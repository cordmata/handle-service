package edu.asu.lib.handle;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.PrivateKey;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.handle.hdllib.PublicKeyAuthenticationInfo;
import net.handle.hdllib.Util;

public class HandleAuthProvider {
	
	private static final Logger log = LoggerFactory.getLogger(HandleAuthProvider.class);
	
	private File privateKey;
	private String passphrase;
	private String authHandle;
	private PublicKeyAuthenticationInfo authInfo;
	
	public File getPrivateKey() {
		return privateKey;
	}
	
	public void setPrivateKey(File privateKey) {
		this.privateKey = privateKey;
	}
	
	public String getPassphrase() {
		return passphrase;
	}
	
	public void setPassphrase(String passphrase) {
		this.passphrase = passphrase;
	}
	
	public String getAuthHandle() {
		return authHandle;
	}

	public void setAuthHandle(String authHandle) {
		this.authHandle = authHandle;
	}

	public PublicKeyAuthenticationInfo getAuthInfo() {
		if (authInfo == null) {
			authInfo = initAuthInfo();
		}
		return authInfo;
	}
	
	private PublicKeyAuthenticationInfo initAuthInfo() {
		log.debug("Initializing PublicKeyAuthenticationInfo.");
		try {
			FileInputStream in = new FileInputStream(getPrivateKey());
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			IOUtils.copy(in, out);
			
			byte[] key = out.toByteArray();

			if (Util.requiresSecretKey(key)) {
				byte[] secKey = getPassphrase().getBytes();
			    key = Util.decrypt(key, secKey);
			}
			PrivateKey privkey = Util.getPrivateKeyFromBytes(key, 0);
			return new PublicKeyAuthenticationInfo(Util.encodeString(getAuthHandle()), 300, privkey);
		} catch (Exception e) {
			String message = "Error constructing PublicKeyAuthenticationInfo.";
			log.error(message, e);
			throw new RuntimeException(message, e);
		}
	}
}
