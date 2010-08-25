/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.asu.lib.handle.service;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.security.PrivateKey;
import javax.servlet.http.HttpServletRequest;
import net.handle.hdllib.PublicKeyAuthenticationInfo;
import net.handle.hdllib.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author dshughe1
 */
public class ZeroSecurityAuthProvider implements HandleAuthProvider {

    private static Log log = LogFactory.getLog(ZeroSecurityAuthProvider.class);
    private PublicKeyAuthenticationInfo _authInfo;
    private String _passphrase;
    private String _keyfile;
    private String _authHandle;
    private int _authIndex;

    public ZeroSecurityAuthProvider() {}

    public void setPassphrase(String passphrase) { _passphrase = passphrase; }
    public void setKeyFile(String keyfile) { _keyfile = keyfile; }
    public void setAuthHandle(String authHandle) { _authHandle = authHandle; }
    public void setAuthIndex(int authIndex) { _authIndex = authIndex; }

    public PublicKeyAuthenticationInfo getAuthenticationInfo(HttpServletRequest request) {
        if(_authInfo == null)
            initializeAuthInfo();
        return _authInfo;
    }

    private void initializeAuthInfo() {
        log.debug("Attempting to prepare authentication.");
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
            byte[] bytes = new byte[512];

            // Read bytes from the input stream in bytes.length-sized chunks and write
            // them into the output stream
            FileInputStream keystream = new FileInputStream(_keyfile);
            int readBytes;
            while ((readBytes = keystream.read(bytes)) != -1) {
                out.write(bytes, 0, readBytes);
            }

            // Convert the contents of the output stream into a byte array
            byte[] key = out.toByteArray();

            // Check to see if the private key is encrypted.  If so, read in the
            // user's passphrase and decrypt.  Finally, convert the byte[]
            // representation of the private key into a PrivateKey object.
            PrivateKey privkey = null;
            byte[] secKey = null;
            if (Util.requiresSecretKey(key)) {
                secKey = _passphrase.getBytes();
            }

            key = Util.decrypt(key, secKey);
            privkey = Util.getPrivateKeyFromBytes(key, 0);
            log.debug("Algorithm = " + privkey.getAlgorithm().trim());
            _authInfo = new PublicKeyAuthenticationInfo(_authHandle.getBytes("UTF8"), _authIndex, privkey);
        } catch (Exception ex) {
            String msg = "Error preparing authentication.";
            log.error(msg, ex);
        }
        log.debug("Prepare authentication successful.");
    }
}
