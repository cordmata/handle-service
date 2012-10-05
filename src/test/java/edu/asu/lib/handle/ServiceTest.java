package edu.asu.lib.handle;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.net.URL;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import net.handle.hdllib.HandleException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ServiceTest {

    private static final String HDL_BASE = "http://hdl.handle.net/";
    private static final String GOOD_PREFIX = "0000";
    private static final String BAD_PREFIX = "0001";
    private static final String GOOD_SUFFIX = "test";
    private static final String NOT_FOUND_SUFFIX = "test-not-found";
    private static final String GOOD_TARGET = "http://example.com";
    private static final String MALFORMED_TARGET = "I'm gonna blow up";
    
	@Mock private HandleClient client;
	@InjectMocks private HandleService service = new HandleService();
	
	
	@Test
	public void testCreateHandle() throws Exception {
	    Response resp = service.createHandle(GOOD_PREFIX, GOOD_SUFFIX, GOOD_TARGET);
	    assertEquals(resp.getStatus(), Status.CREATED.getStatusCode());
	    String loc = resp.getMetadata().get("Location").get(0).toString();
	    assertEquals(loc, HDL_BASE + buildHandle(GOOD_PREFIX, GOOD_SUFFIX));
	}
	
	@Test
	public void testBadPrefix() throws Exception {
	    doThrow(new HandleException(HandleException.UNABLE_TO_AUTHENTICATE))
	        .when(client).update(eq(buildHandle(BAD_PREFIX, GOOD_SUFFIX)), 
	                             any(URL.class));
	    
        Response resp = service.updateHandle(BAD_PREFIX, GOOD_SUFFIX, GOOD_TARGET);
        assertEquals(resp.getStatus(), Status.FORBIDDEN.getStatusCode());
	}
	
	@Test
    public void testHandleResolution() throws Exception {
	    String handle = buildHandle(GOOD_PREFIX, GOOD_SUFFIX);
	    when(client.resolve(handle)).thenReturn(new URL(GOOD_TARGET));
        Response resp = service.readHandle(GOOD_PREFIX, GOOD_SUFFIX);
        assertEquals(resp.getStatus(), Status.NO_CONTENT.getStatusCode());
	    String loc = resp.getMetadata().get("Location").get(0).toString();
	    assertEquals(loc, GOOD_TARGET);
    }
	
	@Test
    public void testHandleNotFound() throws Exception {
	    String handle = buildHandle(GOOD_PREFIX, NOT_FOUND_SUFFIX);
	    when(client.resolve(handle))
	        .thenThrow(new HandleException(HandleException.HANDLE_DOES_NOT_EXIST));
        Response resp = service.readHandle(GOOD_PREFIX, NOT_FOUND_SUFFIX);
        assertEquals(resp.getStatus(), Status.NOT_FOUND.getStatusCode());
    }
	
	@Test
    public void testHandleConflict() throws Exception {
        String handle = buildHandle(GOOD_PREFIX, GOOD_SUFFIX);
        doThrow(new HandleException(HandleException.HANDLE_ALREADY_EXISTS))
            .when(client).create(eq(handle), any(URL.class));
        Response resp = service.createHandle(GOOD_PREFIX, GOOD_SUFFIX, GOOD_TARGET);
        assertEquals(resp.getStatus(), Status.CONFLICT.getStatusCode());
    }
	
	@Test
    public void testHandleDeletion() {
        Response resp = service.deleteHandle(GOOD_PREFIX, GOOD_SUFFIX);
        assertEquals(resp.getStatus(), Status.NO_CONTENT.getStatusCode());
    }
	
	@Test
    public void testBadTarget() {
	    Response resp = service.updateHandle(GOOD_PREFIX, GOOD_SUFFIX, MALFORMED_TARGET);
        assertEquals(resp.getStatus(), Status.BAD_REQUEST.getStatusCode());
    }
	
	@Test
	public void testUpdateNew() throws Exception {
	    String handle = buildHandle(GOOD_PREFIX, GOOD_SUFFIX);
	    doThrow(new HandleException(HandleException.HANDLE_DOES_NOT_EXIST))
	        .when(client).update(handle, new URL(GOOD_TARGET));
	    Response resp = service.updateHandle(GOOD_PREFIX, GOOD_SUFFIX, GOOD_TARGET);
	    assertEquals(resp.getStatus(), Status.CREATED.getStatusCode());
	}
	
	@Test
	public void testUpdateExisting() throws Exception {
	    Response resp = service.updateHandle(GOOD_PREFIX, GOOD_SUFFIX, GOOD_TARGET);
	    assertEquals(resp.getStatus(), Status.NO_CONTENT.getStatusCode());
	    String hdl = resp.getMetadata().get("Location").get(0).toString();
	    assertEquals(hdl, HDL_BASE + buildHandle(GOOD_PREFIX, GOOD_SUFFIX));
	}
	
	private String buildHandle(String prefix, String suffix) {
	    return prefix + "/" + suffix;
	}
}
