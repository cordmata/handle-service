package edu.asu.lib.handle;

import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.form.Form;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Primarily tests that the Spring Security machinery works as expected in
 * a running Servlet container. The expectation is that the service has been
 * deployed and a system property of "service.url" exists indicating where
 * to address the service (maven takes care of this automatically). It also
 * assumes HandleService was initialized with {@link StubHandleClient} and 
 * that the UserDetailsService has been initialized with specific users with
 * specific permissions (see src/test/context/beans.xml).
 * 
 * @author Matt Cordial <matt.cordial@gmail.com>
 *
 */
public class HandleServiceIT extends Assert {

	private static final String TARGET = StubHandleClient.RESOLVE_URL;
    private static String endpointUrl;
    
    @BeforeClass
    public static void initialize() throws Exception {
        endpointUrl = System.getProperty("service.url");
    }
    
    @Test
    public void testAdminUser() throws Exception {
        assertAllMethodsAuthorized(
            createClient("handleAdmin", "blah").path("/0000/foo")); 
    }

    @Test
    public void testUnauthenticatedUser() throws Exception {
        assertAllMethodsUnauthorized(WebClient.create(endpointUrl + "/0000/foo"));
    }
    
    @Test
    public void testBadCredentials() throws Exception {
        assertAllMethodsUnauthorized(
            createClient("handleAdmin", "blab").path("0000/foo"));
    }    
    
    @Test
    public void testDisabledUser() throws Exception {
        assertAllMethodsUnauthorized(
        	createClient("disabledUser", "noaccess").path("/0000/foo"));
    }
    
    @Test
    public void testUnauthorizedSuffix() throws Exception { 
        assertAllMethodsUnauthorized( 
            createClient("johnny", "johnboy").path("/1234.5/nope:1"));
    }
    
    @Test
    public void testUnauthorizedPrefix() throws Exception { 
        assertAllMethodsUnauthorized( 
            createClient("johnny", "johnboy").path("/1234/repo:1"));
    }
    
    @Test
    public void testWildcardPrefix() throws Exception { 
        WebClient client = createClient("prefixTest", "blah");
        assertAllMethodsAuthorized(client.path("/1234.4/foo:1"));
        assertAllMethodsAuthorized(client.replacePath("/1234.0/foo:1"));
        assertModifyingMethodsForbidden(client.replacePath("/1234/repo:1"));
    }
    
    @Test
    public void testWildcardSuffix() throws Exception { 
        WebClient client = createClient("suffixTest", "blah");
        assertAllMethodsAuthorized(client.path("/1234.5/repo:1"));
        assertAllMethodsAuthorized(client.replacePath("/1234.5/anything"));
        assertModifyingMethodsForbidden(client.replacePath("/1234.0/repo:1"));
    }
    
    @Test
    public void testBlankEntries() throws Exception { 
        WebClient client = createClient("blankTest", "blah");
        assertModifyingMethodsForbidden(client.path("/1234.0/repo:1"));
        assertModifyingMethodsForbidden(client.replacePath("/1234/any"));
    }
    
    @Test
    public void testAuthorizedNonAdminUser() throws Exception { 
        assertAllMethodsAuthorized( 
            createClient("johnny", "jonboy").path("/1234.5/repo:1"));
    }
    
    @Test
    public void testHandleNotFound() throws Exception { 
    	WebClient client = createClient("handleAdmin", "blah");
    	client.path("/" + StubHandleClient.NOT_FOUND_HANDLE); 
        Response r = client.get();
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), r.getStatus());
    }
    
    private void assertModifyingMethodsStatus(WebClient client, int statusCode) {
        // POST
        Response r = client.form(new Form().set("target", TARGET));
        assertEquals(statusCode, r.getStatus());
        
        // PUT
        r = client.query("target", TARGET).put(null);
        assertEquals(statusCode, r.getStatus());
        
        // DELETE
        r = client.delete();
        assertEquals(statusCode, r.getStatus());
    }
    
    private void assertModifyingMethodsForbidden(WebClient client) {
        assertModifyingMethodsStatus(client, Response.Status.FORBIDDEN.getStatusCode());
    }
    
    private void assertAllMethodsUnauthorized(WebClient client) {
        int unauthStatus = Response.Status.UNAUTHORIZED.getStatusCode();
        // GET
        Response r = client.get();
        assertEquals(unauthStatus, r.getStatus());
        assertModifyingMethodsStatus(client, unauthStatus);
    }
    
    private void assertAllMethodsAuthorized(WebClient client) {
        // GET
        Response r = client.get();
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), r.getStatus());
        assertEquals(TARGET, r.getMetadata().get("Location").get(0).toString());
       
        // POST
        r = client.form(new Form().set("target", TARGET));
        assertEquals(Response.Status.CREATED.getStatusCode(), r.getStatus());
        
        // PUT
        r = client.query("target", TARGET).put(null);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), r.getStatus());
        
        // DELETE
        r = client.delete();
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), r.getStatus());
    }
    
    private WebClient createClient(String username, String password) {
        return WebClient.create(endpointUrl, username, password, null);
    }
    
}
