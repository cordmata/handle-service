package edu.asu.lib.handle;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import net.handle.api.HSAdapter;
import net.handle.api.HSAdapterFactory;
import net.handle.hdllib.HandleException;

/**
 * {@link ServerAdapterFactory} implementation that uses CNRI's suppled adapter
 * factory which is a final class, otherwise we could just sub-class it 
 * eliminating the need to specify our own...such is life. 
 * 
 * @author Matt Cordial <matt.cordial@gmail.com>
 *
 */
public class CNRIGenericAdapterFactory implements ServerAdapterFactory {
    
    private static final Logger log = Logger.getLogger(CNRIGenericAdapterFactory.class);
    
    public HSAdapter newInstance(String adminHandle, int keyIndex,
                                 File privateKey, String passphrase) throws HandleException {
        byte[] pass = (passphrase != null) ? passphrase.getBytes() : null;
        byte[] key = readKey(privateKey);
        return HSAdapterFactory.newInstance(adminHandle, keyIndex, key, pass);
    }
    
    private byte[] readKey(File key) {
        try {
            FileInputStream in = new FileInputStream(key);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            IOUtils.copy(in, out);        
            return out.toByteArray();
        } catch (Exception e) {
            log.fatal("Service mis-configuration, can't find or read the private key.", e);
            throw new RuntimeException(e);
        }   
    }
}
