package edu.asu.lib.handle;

import java.io.File;

import net.handle.api.HSAdapter;
import net.handle.hdllib.HandleException;

public interface ServerAdapterFactory {

    /**
     * Create an adapter to communicate with the Handle Server using the 
     * supplied credentials. 
     * 
     * @param adminHandle - The registered server's ADMIN Handle.
     * @param keyIndex - Index of the Handle Value (usually 300)
     * @param privateKey - The registered server's administrative private key.
     * @param passphrase - The passphrase used to encrypt the private key (null if unencrypted).
     * @return
     * @throws HandleException
     */
    public HSAdapter newInstance(String adminHandle, int keyIndex, 
                                 File privateKey, String passphrase) throws HandleException;
}
