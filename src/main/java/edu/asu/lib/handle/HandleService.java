package edu.asu.lib.handle;

import java.net.URI;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.handle.hdllib.AbstractMessage;
import net.handle.hdllib.AbstractResponse;
import net.handle.hdllib.AdminRecord;
import net.handle.hdllib.CreateHandleRequest;
import net.handle.hdllib.DeleteHandleRequest;
import net.handle.hdllib.Encoder;
import net.handle.hdllib.HandleException;
import net.handle.hdllib.HandleResolver;
import net.handle.hdllib.HandleValue;
import net.handle.hdllib.ModifyValueRequest;
import net.handle.hdllib.ResolutionRequest;
import net.handle.hdllib.ResolutionResponse;
import net.handle.hdllib.Util;

@Path("/handleservice/{handle}")
public class HandleService {

	private static final Logger log = LoggerFactory
			.getLogger(HandleService.class);
	private static final byte[] HS_ADMIN_TYPE = Util.encodeString("HS_ADMIN");
	private static final byte[] URL_TYPE = Util.encodeString("URL");

	private HandleAuthProvider authProvider;

	@GET
	public Response readHandle(@PathParam("handle") String handle) {
		
		log.debug("starting GET");
		String errorMsg = "Issue resolving handle: " + handle;
		
		HandleResolver res = new HandleResolver();
		ResolutionRequest req = new ResolutionRequest(
				Util.encodeString(handle), null, null, null);
		AbstractResponse resp;
		try {
			resp = res.processRequest(req);
		} catch (HandleException e) {
			log.error(errorMsg + ". Error communicating with Handle Server.", e);
			return Response.serverError().build();
		}
		String mess = AbstractMessage.getResponseCodeMessage(resp.responseCode);
		switch (resp.responseCode) {
		case AbstractMessage.RC_HANDLE_NOT_FOUND:
			return Response.status(Status.NOT_FOUND).build();
		case AbstractMessage.RC_SUCCESS:
			ResolutionResponse rresp = (ResolutionResponse) resp;
			HandleValue[] values;
			try {
				values = rresp.getHandleValues();
			} catch (HandleException e) {
				log.error(errorMsg + ". Error getting handle values.", e);
				return Response.serverError().build();
			}
			for (HandleValue val : values) {
				String type = val.getTypeAsString();
				if ("URL".equalsIgnoreCase(type)) {
					String target = val.getDataAsString();
					URI targetUri = UriBuilder.fromUri(target).build();
					return Response.noContent().location(targetUri).build();
				}
			}
			return Response.status(Status.NOT_FOUND).build();
		default:
			return Response.serverError().entity(mess).build();
		}
	}

	@POST
	public Response createHandle(@PathParam("handle") String handle,
			@QueryParam("target") URI target) {

		log.debug("Starting HTTP POST.");

		String errorMsg = "Error creating handle: " + handle +
			" with target: " + target.toString();
		
		HandleResolver resolver = new HandleResolver();

		// We don't want to create a handle without an admin value-- otherwise
		// we would be locked out. Give ourselves all permissions, even
		// ones that only apply for NA handles.
		AdminRecord admin = new AdminRecord(Util.encodeString(authProvider
				.getAuthHandle()), 300, true, true, true, true, true, true,
				true, true, true, true, true, true);

		// All handle values need a timestamp, so get the current time in
		// seconds since the epoch
		int timestamp = (int) (System.currentTimeMillis() / 1000);

		// argument is the value's index, 100 in this case. The second
		// argument is the value's type. The third argument holds the value's
		// data. Since this is binary data, not textual data like a URL, we have
		// to encode it first.
		//
		// The fourth argument indicates whether the time to live for the
		// value is absolute or relative. The fifth argument is the time to
		// live, 86400 seconds(24 hours) in this case. The sixth argument is
		// the timestamp we created earlier. The seventh argument is a
		// ValueReference array. You will almost always want to leave this
		// null; read the RFC's for more information. The last four arguments
		// are the permissions for the value: admin read, admin write, public
		// read, and public write.
		//
		// whew!

		HandleValue[] vals = {
				new HandleValue(100, HS_ADMIN_TYPE,
						Encoder.encodeAdminRecord(admin),
						HandleValue.TTL_TYPE_RELATIVE, 86400, timestamp, null,
						true, true, true, false),
				new HandleValue(101, URL_TYPE, Util.encodeString(target
						.toString()), HandleValue.TTL_TYPE_RELATIVE, 86400,
						timestamp, null, true, true, true, false) };

		// Now we can build our CreateHandleRequest object. As its first
		// parameter it takes the handle we are going to create. The second
		// argument is the array of initial values the handle should have.
		// The final argument is the authentication object that should be
		// used to gain permission to perform the creation.
		CreateHandleRequest req = new CreateHandleRequest(
				Util.encodeString(handle), vals, authProvider.getAuthInfo());

		// Finally, we are ready to send the message. We do this by calling
		// the processRequest method of the resolver object with the request
		// object as an argument. The result is returned as either a
		// GenericResponse or ErrorResponse object. It is important to note that
		// a failed resolution will not throw an exception, only return a
		// ErrorResponse.
		AbstractResponse resp;
		try {
			resp = resolver.processRequest(req);
		} catch (HandleException e) {
			log.error(errorMsg, e);
			return Response.serverError().build();
		}

		// The responseCode value for a response indicates the status of
		// the request. A successful resolution will always return
		// RC_SUCCESS. Failed resolutions could return one of several
		// response codes, including RC_ERROR, RC_INVALID_ADMIN, and
		// RC_INSUFFICIENT_PERMISSIONS.
		String mess = AbstractMessage.getResponseCodeMessage(resp.responseCode);
		if (resp.responseCode != AbstractMessage.RC_SUCCESS) {
			log.warn(errorMsg + "Response code from Handle Server: " + mess);
			return Response.serverError().entity(mess).build();
		}
		UriBuilder locBuilder = UriBuilder.fromUri("http://hdl.handle.net/")
				.path(handle);
		return Response.created(locBuilder.build()).build();
	}

	@PUT
	public Response updateHandle(@PathParam("handle") String handle,
			@QueryParam("target") URI target) {

		log.debug("Starting HTTP PUT.");
		String errorMsg = "Issue updating handle: " + handle +
			" to target: " + target.toString();
		
		HandleResolver resolver = new HandleResolver();

		// first handle the removal requests
		ResolutionRequest req = new ResolutionRequest(
				Util.encodeString(handle), null, null, null);

		AbstractResponse resp;
		try {
			resp = resolver.processRequest(req);
		} catch (HandleException e1) {
			log.error(errorMsg, e1);
			return Response.serverError().build();
		}
		String resolutionMessage = AbstractMessage.getResponseCodeMessage(resp.responseCode);
		
		// The responseCode value for a response indicates the status of the
		// request. A successful resolution will always return RC_SUCCESS.
		// Failed resolutions could return one of several response codes,
		// including RC_ERROR, RC_SERVER_TOO_BUSY, and RC_HANDLE_NOT_FOUND.
		if (resp.responseCode == AbstractMessage.RC_HANDLE_NOT_FOUND) {
			return createHandle(handle, target);
		}
		
		if (resp.responseCode == AbstractMessage.RC_SUCCESS) {

			ResolutionResponse rresp = (ResolutionResponse) resp;
			HandleValue[] values;
			try {
				values = rresp.getHandleValues();
			} catch (HandleException e) {
				log.error(errorMsg + ". Error getting Handle values.", e);
				return Response.serverError().build();
			}

			for (HandleValue val : values) {
				// check to see if there is a value with type URL
				if ("URL".equalsIgnoreCase(val.getTypeAsString())) {
					val.setData(Util.encodeString(target.toString()));
				}
			}

			ModifyValueRequest modreq = new ModifyValueRequest(rresp.handle,
					values, authProvider.getAuthInfo());
			AbstractResponse modresp;
			try {
				modresp = resolver.processRequest(modreq);
			} catch (HandleException e) {
				log.error(errorMsg, e);
				return Response.serverError().build();
			}
			String modRespMessage = AbstractMessage.getResponseCodeMessage(modresp.responseCode);
			if (modresp != null && modresp.responseCode == AbstractMessage.RC_SUCCESS) {
				return Response.noContent().build();
			} else {
				log.error(errorMsg + ". message: " + modRespMessage);
				return Response.serverError().entity(modRespMessage).build();
			}
		} else {
			log.info("Something's wonky with the supplied Handle: " + handle + 
					" message: " + resolutionMessage);
			return Response.serverError().entity(resolutionMessage).build();
		}
	}

	@DELETE
	public Response deleteHandle(@PathParam("handle") String handle) {
		
		log.debug("Starting DELETE.");
		String errorMsg = "Error deleting handle: " + handle;
		
		HandleResolver resolver = new HandleResolver();
		DeleteHandleRequest req = new DeleteHandleRequest(
				Util.encodeString(handle), authProvider.getAuthInfo());
		AbstractResponse resp;
		try {
			resp = resolver.processRequest(req);
		} catch (HandleException e) {
			log.error("Problem communicating with the Handle Server", e);
			return Response.serverError().build();
		}

		if (resp.responseCode != AbstractMessage.RC_SUCCESS
				|| resp.responseCode != AbstractMessage.RC_HANDLE_NOT_FOUND) {
			String mess = AbstractMessage
					.getResponseCodeMessage(resp.responseCode);
			log.error(errorMsg + ". Response code: " + mess);
			return Response.serverError().entity(mess).build();
		}

		return Response.noContent().build();
	}

	public void setAuthProvider(HandleAuthProvider authProvider) {
		this.authProvider = authProvider;
	}

}
