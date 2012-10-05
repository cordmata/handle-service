package edu.asu.lib.handle;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import javax.annotation.PostConstruct;

import net.handle.api.HSAdapter;
import net.handle.hdllib.HandleException;
import net.handle.hdllib.HandleValue;

import org.apache.log4j.Logger;

public class HandleClientImpl implements HandleClient {
    
    private static final String[] TYPES = {"URL"};
    private static final Logger log = Logger.getLogger(HandleClientImpl.class);
    
    private ServerAdapterFactory serverAdapterFactory;
    private HSAdapter serverAdapter;
    private String adminHandle;
    private String passphrase;
    private File privateKey;
    private int keyIndex;
    
    /**
     * @see edu.asu.lib.handle.HandleClient#resolve(java.lang.String)
     */
    public URL resolve(String handle) throws HandleException {
        HandleValue[] vals = serverAdapter.resolveHandle(handle, TYPES, null);
        if (vals.length > 0) {
            try {
                return new URL(vals[0].getDataAsString());
            } catch (MalformedURLException e) {
                throw new HandleException(HandleException.INVALID_VALUE, 
                                          "The value of " + handle 
                                          + " is a malformed URL.");
            }
        }
        throw new HandleException(HandleException.HANDLE_DOES_NOT_EXIST, 
                                  "Handle not found");
    }

    /**
     * @see edu.asu.lib.handle.HandleClient#create(java.net.URL)
     */
    public void create(String handle, URL target) throws HandleException {
        HandleValue[] vals = {
            serverAdapter.createAdminValue(adminHandle, keyIndex, 100),
            serverAdapter.createHandleValue(1, "URL", target.toString())
        };
        serverAdapter.createHandle(handle, vals);
    }
    
    /**
     * @see edu.asu.lib.handle.HandleClient#update(String, URL)
     */
    public void update(String handle, URL target) throws HandleException {
        HandleValue[] vals = serverAdapter.resolveHandle(handle, TYPES, null);
        HandleValue[] newVals = new HandleValue[vals.length];
        for (int i = 0; i < vals.length; i++) {
            HandleValue oldVal = vals[i];
            newVals[i] = serverAdapter.createHandleValue(
                oldVal.getIndex(), 
                oldVal.getTypeAsString(), 
                target.toString()
            );
            
        }
        serverAdapter.updateHandleValues(handle, newVals);
    }

    /**
     * @see edu.asu.lib.handle.HandleClient#delete(String)
     */
    public void delete(String handle) throws HandleException {
        serverAdapter.deleteHandle(handle); 
    }
    
    public void setPrivateKey(File privateKey) {
        this.privateKey = privateKey;
    }

    public void setPassphrase(String passphrase) {
        this.passphrase = passphrase;
    }

    public void setAdminHandle(String adminHandle) {
        this.adminHandle = adminHandle;
    }
    
    public void setKeyIndex(int keyIndex) {
        this.keyIndex = keyIndex;
    }
    
    public void setServerAdapterFactory(ServerAdapterFactory serverAdapterFactory) {
        this.serverAdapterFactory = serverAdapterFactory;
    }

    @PostConstruct
    private void initServerAdapter() {
        try {
            serverAdapter = serverAdapterFactory.newInstance(
                                   adminHandle, keyIndex, privateKey, passphrase);
        } catch (HandleException e) {
            log.fatal("Can't initialize communication with the Handle Server.", e);
            throw new RuntimeException(e);
        }
    }
    

}
