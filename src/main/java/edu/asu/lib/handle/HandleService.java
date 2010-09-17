package edu.asu.lib.handle;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import net.handle.hdllib.AbstractMessage;
import net.handle.hdllib.AbstractRequest;
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

@Path("/{handle:.*}")
public class HandleService {

	private static final Logger log = Logger.getLogger(HandleService.class);
	private static final URI HANDLE_REGISTRY = UriBuilder.fromUri("http://hdl.handle.net/").build();
	private static final byte[] HS_ADMIN_TYPE = Util.encodeString("HS_ADMIN");
	private static final byte[] URL_TYPE = Util.encodeString("URL");
	private static final String UNAUTH_MSG = "Authentication failure on Handle " +
			"Server. Please check that the supplied private key has " +
			"administration privledges and that the passphrase is correct.";

	private HandleAuthProvider authProvider;
	private HandleResolver handleResolver;
	
	public HandleService() {
		handleResolver = new HandleResolver();
	}

	@GET
	public Response readHandle(@PathParam("handle") String handle) {
		
		log.debug("starting GET");
		if (StringUtils.isBlank(handle)) { return Response.status(Status.BAD_REQUEST).build(); }
		log.debug("Handle = " + handle);
		String errorMsg = "Issue resolving handle: " + handle + "\n";
		ResolutionRequest req = new ResolutionRequest(
				Util.encodeString(handle), null, null, null);
		req.authoritative = true;
		AbstractResponse resp = processRequest(req);
		errorMsg += AbstractMessage.getResponseCodeMessage(resp.responseCode);
		
		if (resp.responseCode == AbstractMessage.RC_SUCCESS) {
			ResolutionResponse rresp = (ResolutionResponse) resp;
			HandleValue[] values;
			try {
				values = rresp.getHandleValues();
			} catch (HandleException e) {
				log.error(errorMsg + ". Error getting handle values.", e);
				return Response.serverError().build();
			}
			URI targetUri = null;
			for (HandleValue val : values) {
				String type = val.getTypeAsString();
				if ("URL".equalsIgnoreCase(type)) {
					String target = val.getDataAsString();
					targetUri = UriBuilder.fromUri(target).build();
				}
			}
			if (targetUri == null) {
				targetUri = UriBuilder.fromUri("NO_URL").build();
			}
			return Response.noContent().location(targetUri).build();
		}
		return returnError(resp.responseCode, errorMsg);
	}

	@POST
	@Consumes("application/x-www-form-urlencoded")
	public Response createHandle(
			@FormParam("target") String target,
			@FormParam("prefix") String prefix,
			@FormParam("suffix") String suffix) {

		log.debug("Starting HTTP POST.");
		URL targetUrl = createUrl(target);
		
		// if no target URL was specified or 
		if (targetUrl == null || 
			(StringUtils.isBlank(prefix) && StringUtils.isBlank(suffix))) { 
			return Response.status(Status.BAD_REQUEST).build(); 
		}
		
		String handle;
		boolean autogenHandle = false;
		if (StringUtils.isBlank(suffix)) {
			handle = generateHandle(prefix);
			autogenHandle = true;
		} else {
			handle = prefix + "/" + suffix;
		}
		
		String errorMsg = "Error creating handle: " + handle +
			" with target: " + targetUrl.toString() + "\n";

		// We don't want to create a handle without an admin value-- otherwise
		// we would be locked out. Give ourselves all permissions, even
		// ones that only apply for NA handles.
		AdminRecord admin = new AdminRecord(Util.encodeString(authProvider
				.getAuthHandle()), 300, true, true, true, true, true, true,
				true, true, true, true, true, true);

		int timestamp = getTimestamp();

		/* 
		 * The first argument is the value's index.
		 * 
		 * The second argument is the value's type.
		 * 
		 * The third argument holds the value's data. Since this is binary 
		 * data, not textual data like a URL, we have to encode it first.
		 * 
		 * The fourth argument indicates whether the time to live for the
		 * value is absolute or relative. 
		 * 
		 * The fifth argument is the time to live, 86400 seconds(24 hours) 
		 * in this case.
		 * 
		 * The sixth argument is the timestamp we created earlier.
		 * 
		 * The seventh argument is a ValueReference array. You will almost 
		 * always want to leave this null; read the RFC's for more information. 
		 * 
		 * The last four arguments are the permissions for the value: 
		 * admin read, admin write, public read, and public write.
		 */
		HandleValue[] vals = {
				new HandleValue(100, HS_ADMIN_TYPE,
						Encoder.encodeAdminRecord(admin),
						HandleValue.TTL_TYPE_RELATIVE, 86400, timestamp, null,
						true, true, true, false),
				new HandleValue(1, URL_TYPE, Util.encodeString(targetUrl
						.toString()), HandleValue.TTL_TYPE_RELATIVE, 86400,
						timestamp, null, true, true, true, false) 
				};

		AbstractResponse resp = createHandleRequest(handle, vals);
		errorMsg += AbstractMessage.getResponseCodeMessage(resp.responseCode) + "\n";
		
		if (resp.responseCode == AbstractMessage.RC_HANDLE_ALREADY_EXISTS) {
			if (autogenHandle) { // retry on conflict if we are creating a random unique handle
				while (resp.responseCode == AbstractMessage.RC_HANDLE_ALREADY_EXISTS) {
					resp = createHandleRequest(generateHandle(prefix), vals);
				}
			} else {
				return Response.status(Status.CONFLICT).build();
			}
		}
		
		if (resp.responseCode != AbstractMessage.RC_SUCCESS) {
			return returnError(resp.responseCode, errorMsg);
		}
		
		UriBuilder locBuilder = UriBuilder.fromUri(HANDLE_REGISTRY).path(handle);
		return Response.created(locBuilder.build()).build();
		
	}
	
	private AbstractResponse createHandleRequest(String handle, HandleValue[] vals) {
		CreateHandleRequest req = new CreateHandleRequest(
				Util.encodeString(handle), vals, authProvider.getAuthInfo());
		return processRequest(req);
	}

	@PUT
	public Response updateHandle(
			@PathParam("handle") String handle,
			@QueryParam("target") String target, 
			@QueryParam("create") @DefaultValue("false") Boolean createIfNotFound) {

		log.debug("Starting HTTP PUT.");
		
		URL targetUrl =  createUrl(target);
		if (StringUtils.isBlank(handle) || targetUrl == null) { 
			return Response.status(Status.BAD_REQUEST).build(); 
		}
		
		String errorMsg = "Problem updating handle: " + handle +
			" to target: " + targetUrl.toString() + "\n";
		
		ResolutionRequest req = new ResolutionRequest(
				Util.encodeString(handle), null, null, null);
		req.authoritative = true;
		AbstractResponse resp = processRequest(req);

		errorMsg += "Resolution: " + 
			AbstractMessage.getResponseCodeMessage(resp.responseCode) + "\n";
		
		if (resp.responseCode == AbstractMessage.RC_HANDLE_NOT_FOUND && createIfNotFound) {
			String[] parts = StringUtils.split( handle, "/", 2);
			if (parts.length == 2) {
				return createHandle(target, parts[0], parts[1]);
			}
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

			List<Integer> idxs = new ArrayList<Integer>();
			boolean foundUrl = false;
			for (HandleValue val : values) {
				idxs.add(val.getIndex());
				// check to see if there is a value with type URL
				if ("URL".equalsIgnoreCase(val.getTypeAsString())) {
					val.setData(Util.encodeString(targetUrl.toString()));
					foundUrl = true;
				}
			}
			
			// If there was not an existing URL type then add one at a free index. 
			if (!foundUrl) {
				Integer idx = 1;
				while (idxs.contains(idx)) idx++;
				HandleValue newVal = new HandleValue(idx, URL_TYPE, 
						Util.encodeString(targetUrl.toString()), 
						HandleValue.TTL_TYPE_RELATIVE, 86400,
						getTimestamp(), null, true, true, true, false);
				List<HandleValue> valList = Arrays.asList(values);
				valList.add(newVal);
				values = (HandleValue[]) valList
						.toArray(new HandleValue[valList.size()]);
			}

			ModifyValueRequest modreq = new ModifyValueRequest(rresp.handle,
					values, authProvider.getAuthInfo());
			AbstractResponse modresp = processRequest(modreq);
			errorMsg += "Modification: " + 
				AbstractMessage.getResponseCodeMessage(modresp.responseCode) + "\n";
			
			if (modresp.responseCode != AbstractMessage.RC_SUCCESS) {
				return returnError(modresp.responseCode, errorMsg);
			}
			URI loc = UriBuilder.fromUri(HANDLE_REGISTRY).path(handle).build();
			return Response.noContent().location(loc).build();
		}
		log.debug(errorMsg);
		return returnError(resp.responseCode, errorMsg);

	}

	@DELETE
	public Response deleteHandle(@PathParam("handle") String handle) {
		log.debug("Starting DELETE.");
		if (StringUtils.isBlank(handle)) { return Response.status(Status.BAD_REQUEST).build(); }
		String errorMsg = "Error deleting handle: " + handle + "\n";
		DeleteHandleRequest req = new DeleteHandleRequest(
				Util.encodeString(handle), authProvider.getAuthInfo());
		AbstractResponse resp = processRequest(req);
		errorMsg += AbstractMessage.getResponseCodeMessage(resp.responseCode);
		switch (resp.responseCode) {
			case AbstractMessage.RC_SUCCESS:
			case AbstractMessage.RC_HANDLE_NOT_FOUND:
				return Response.noContent().build();
			default:
				return returnError(resp.responseCode, errorMsg);
		}
	}

	private AbstractResponse processRequest(AbstractRequest req) {
		AbstractResponse resp;
		try {
			resp = handleResolver.processRequest(req);
		} catch (HandleException e) {
			log.error("Problem communicating with the Handle Server", e);
			throw new WebApplicationException();
		}
		return resp;
	}
	
	private Response returnError(int handleResponseCode, String errorMsg) {
		switch (handleResponseCode) {
			case AbstractMessage.RC_HANDLE_NOT_FOUND:
				return Response.status(Status.NOT_FOUND).build();
			case AbstractMessage.RC_AUTHEN_ERROR:
			case AbstractMessage.RC_AUTHENTICATION_FAILED:
			case AbstractMessage.RC_AUTHENTICATION_NEEDED:
			case AbstractMessage.RC_INSUFFICIENT_PERMISSIONS:
			case AbstractMessage.RC_INVALID_CREDENTIAL:
				log.error(UNAUTH_MSG);
			default:
				ResponseBuilder r = Response.serverError();
				if (StringUtils.isNotBlank(errorMsg)) {
					log.error(errorMsg);
					r.entity(errorMsg);
				}
				return r.build();
		}
	}
	
	private URL createUrl(String urlString) {
		URL url = null;
		try {
			url = new URL(urlString);
		} catch (MalformedURLException e) {
			log.warn("Supplied url is malformed: " + urlString);
		}
		return url;
	}
	
	private int getTimestamp() {
		// All handle values need a timestamp, so get the current time in
		// seconds since the epoch
		return (int) (System.currentTimeMillis() / 1000);
	}
	
	private String generateHandle(String prefix) {
		String str = RandomStringUtils.randomNumeric(2) + 
			Long.toString(System.currentTimeMillis()) + 
			RandomStringUtils.randomNumeric(2);
		String suffix = Long.toString(Long.parseLong(str), 36);
		return prefix + "/" + suffix;
	}
	
	public void setAuthProvider(HandleAuthProvider authProvider) {
		this.authProvider = authProvider;
	}

}
