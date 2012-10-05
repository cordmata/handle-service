package edu.asu.lib.handle;

import java.net.URL;

import net.handle.hdllib.HandleException;

public interface HandleClient {

    /**
     * Get the first HandleValue with type URL and return a URL Object.
     * 
     * @param handle - The handle to lookup, in the form of "prefix/suffix". 
     * @return The target URL
     * @throws HandleException
     */
    public abstract URL resolve(String handle) throws HandleException;

    /**
     * Create a new Handle with the supplied target URL.
     * 
     * @param handle - The Handle to create.
     * @param target - The URL target for the new Handle.
     * @throws HandleException
     */
    public abstract void create(String handle, URL target) throws HandleException;
    
    /**
     * Update the Handle with the new supplied target URL.
     * 
     * @param handle - The Handle to update.
     * @param target - URL to replace the existing target.
     * @throws HandleException
     */
    public abstract void update(String handle, URL target) throws HandleException;
    
    /**
     * Purge the supplied Handle.
     * 
     * @param handle - The Handle to purge.
     * @throws HandleException
     */
    public abstract void delete(String handle) throws HandleException;

}