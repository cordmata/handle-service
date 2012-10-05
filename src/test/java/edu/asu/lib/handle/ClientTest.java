package edu.asu.lib.handle;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import java.io.File;
import java.net.URL;

import net.handle.api.HSAdapter;
import net.handle.hdllib.HandleException;
import net.handle.hdllib.HandleValue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ClientTest {

    private static final String NOT_FOUND_HANDLE = "0000/test-not-found";
    private static final String GOOD_HANDLE = "0000/test";
    private static final String GOOD_HANDLE_URL = "http://example.com";
    
	@Mock private ServerAdapterFactory serverAdapterFactory;
	@Mock private HSAdapter adapter;
	@InjectMocks private HandleClientImpl client;
	
	@Before
	public void mockAdapter() throws HandleException {
	    
	    // Set up what we want our mock adapter to do.
	    doThrow(new HandleException(HandleException.HANDLE_DOES_NOT_EXIST))
	        .when(adapter)
	        .resolveHandle(eq(NOT_FOUND_HANDLE), 
            	           any(String[].class), 
            	           any(int[].class));
	    
	    HandleValue[] good = { 
	        new HandleValue(1, "URL".getBytes(), GOOD_HANDLE_URL.getBytes()) 
	    };
	    when(adapter.resolveHandle(eq(GOOD_HANDLE), 
	                               any(String[].class), 
	                               any(int[].class))).thenReturn(good);
	    
	    // Ensure our mocked/stubbed adapter gets returned from the factory.
	    when(serverAdapterFactory
	            .newInstance(anyString(), anyInt(), 
	                         any(File.class), anyString()))
	            .thenReturn(adapter);
	}
	
	@Test
	public void testCreateHandle() throws Exception {
	    client.create(GOOD_HANDLE, new URL(GOOD_HANDLE_URL));
	    verify(adapter).createAdminValue(anyString(), anyInt(), eq(100));
	    verify(adapter).createHandleValue(1, "URL", GOOD_HANDLE_URL);
	}
	
	@Test
	public void testHandleNotFound() {
        try {
            client.resolve(NOT_FOUND_HANDLE);
            fail("Should've thrown HandleException!");
        } catch (HandleException e) {
            assertTrue(e.getCode() == HandleException.HANDLE_DOES_NOT_EXIST);
        }
	}
	
	@Test
    public void testHandleResolution() throws HandleException {
        URL target = client.resolve(GOOD_HANDLE);
        assertTrue(target.toString().equals(GOOD_HANDLE_URL));
    }
	
	@Test
    public void testHandleDeletion() throws HandleException {
        client.delete(GOOD_HANDLE);
        verify(adapter).deleteHandle(GOOD_HANDLE);
    }
	
	@Test
    public void testHandleUpdate() throws Exception {
	    client.update(GOOD_HANDLE, new URL(GOOD_HANDLE_URL));
	    verify(adapter).resolveHandle(
	                    anyString(), any(String[].class), isNull(int[].class));
        verify(adapter).createHandleValue(
                        anyInt(), eq("URL"), eq(GOOD_HANDLE_URL));
        verify(adapter).updateHandleValues(
                        eq(GOOD_HANDLE), any(HandleValue[].class));	    
    }
}
