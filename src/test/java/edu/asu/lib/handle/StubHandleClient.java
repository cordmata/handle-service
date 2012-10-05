package edu.asu.lib.handle;

import java.net.MalformedURLException;
import java.net.URL;

import net.handle.hdllib.HandleException;

/**
 * This is intended to be wired into the running service for integration testing.
 */
public class StubHandleClient implements HandleClient {
    public static final String RESOLVE_URL = "http://example.com";
    public static final String NOT_FOUND_HANDLE = "0000/foo:0";

    /**
     * Error conditions are tested more comprehensively in the unit tests, 
     * but we'll put this here to test for a non-existent handle. That way we 
     * can ensure that the Servlet container acts as expected.
     */
    @Override
    public URL resolve(String handle) throws HandleException {
        if (handle.equalsIgnoreCase(NOT_FOUND_HANDLE)) {
            throw new HandleException(HandleException.HANDLE_DOES_NOT_EXIST);
        }
        try {
            return new URL(RESOLVE_URL);
        } catch (MalformedURLException e) {
            throw new HandleException(HandleException.INTERNAL_ERROR);
        }
    }

    // Because the integration test is primarily testing auth/auth we'll just
    // no-op the methods below and assume success.
    
    @Override
    public void create(String handle, URL target) throws HandleException {}

    @Override
    public void update(String handle, URL target) throws HandleException {}

    @Override
    public void delete(String handle) throws HandleException {}

}
