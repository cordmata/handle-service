package edu.asu.lib.handle;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import net.handle.hdllib.HandleException;

import org.apache.log4j.Logger;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * JAX-RS web-service enabling creation/modification/deletion of 
 * CNRI Handles as web-resources.
 * 
 * @author Matt Cordial <matt.cordial@gmail.com>
 *
 */
@Path("/{prefix}/{suffix}")
public class HandleService {

	private static final Logger log = Logger.getLogger(HandleService.class);
	private static final URI HANDLE_REGISTRY = UriBuilder.fromUri("http://hdl.handle.net/").build();
	private static final String AUTHORIZATION_EXPRESSION = "hasRole('ROLE_ADMIN') or " +
												           "(principal.canModifyPrefix(#prefix) and " +
												           "principal.canModifySuffix(#suffix))";
    private HandleClient handleClient;

	@GET
	public Response readHandle(@PathParam("prefix") String prefix, 
							   @PathParam("suffix") String suffix) {
		String handle = buildHandle(prefix, suffix);
		log.debug("Resolving Handle: " + handle);
		String errMsg = "Error getting value for handle: " + handle;
		try {
    		URL target = handleClient.resolve(handle);
    		return Response.noContent().location(target.toURI()).build();
		} catch (HandleException e) {
		    if (e.getCode() == HandleException.HANDLE_DOES_NOT_EXIST) {
		        return Response.status(Status.NOT_FOUND).build();
		    }
			return buildErrorResponse(e, errMsg);
		} catch (URISyntaxException e) {
			return buildErrorResponse(e, errMsg);
		}
		
	}

	@POST
	@Consumes("application/x-www-form-urlencoded")
	@PreAuthorize(AUTHORIZATION_EXPRESSION)
	public Response createHandle(@PathParam("prefix") String prefix,
								 @PathParam("suffix") String suffix,
								 @FormParam("target") String target) {
		
		String handle = buildHandle(prefix, suffix);
		log.debug("Creating handle: " + handle + "to target: " + target);
		
		URL targetUrl = createUrl(target);
		if (targetUrl == null) { 
			return Response.status(Status.BAD_REQUEST).build(); 
		}
		
		try {
			handleClient.create(handle, targetUrl);
		} catch (HandleException e) {
			return buildErrorResponse(e);
		}
		
		URI loc = UriBuilder.fromUri(HANDLE_REGISTRY).path(handle).build();
		return Response.created(loc).build();
		
	}

	@PUT
	@PreAuthorize(AUTHORIZATION_EXPRESSION)
	public Response updateHandle(
			@PathParam("prefix") String prefix,
			@PathParam("suffix") String suffix,
			@QueryParam("target") String target) {

	    String handle = buildHandle(prefix, suffix);
		URL targetUrl =  createUrl(target);
		if (targetUrl == null) { 
			return Response.status(Status.BAD_REQUEST).build(); 
		}
		
		log.debug("Updating " + handle + " to " + target + ".");
		try {
		    handleClient.update(handle, targetUrl);
        } catch (HandleException e) {
            if (e.getCode() == HandleException.HANDLE_DOES_NOT_EXIST) {
                return createHandle(prefix, suffix, target);
            } else {
                return buildErrorResponse(e);
            }
        }
		
		URI loc = UriBuilder.fromUri(HANDLE_REGISTRY).path(handle).build();
    	return Response.noContent().location(loc).build();
	}

	@DELETE
	@PreAuthorize(AUTHORIZATION_EXPRESSION)
	public Response deleteHandle(@PathParam("prefix") String prefix,
								 @PathParam("suffix") String suffix) {
		String handle = buildHandle(prefix, suffix);
		log.debug("Deleting Handle: " + handle);
		try {
			handleClient.delete(handle);
		} catch (HandleException e) {
			return buildErrorResponse(e);
		}
		return Response.noContent().build();
	}
	
	private Response buildErrorResponse(String errorMsg) {
    	return Response.serverError().entity(errorMsg).build();
	}
	
	private Response buildErrorResponse(Exception e, String errorMsg) {
    	return buildErrorResponse(errorMsg + "\n\n" + e.getMessage());
	}
	
	private Response buildErrorResponse(HandleException e) {
	    switch (e.getCode()) {
        case HandleException.UNABLE_TO_AUTHENTICATE:
        case HandleException.SECURITY_ALERT:
            return Response.status(Status.FORBIDDEN)
                           .entity(e.getMessage())
                           .build();
        case HandleException.HANDLE_ALREADY_EXISTS:
            return Response.status(Status.CONFLICT).build();
        default:
    		return buildErrorResponse(e.getMessage());
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
	
	private String buildHandle(String prefix, String suffix) {
		return prefix + "/" + suffix;
	}
	
    public void setHandleClient(HandleClient handleClient) {
        this.handleClient = handleClient;
    }

}
