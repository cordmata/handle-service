package edu.asu.lib.handle;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Spring Securtiy {@link UserDetails} implementation including information
 * needed by the permission evaluator to determine if the authenticated
 * user can modify particular handles.
 * 
 * @author Matt Cordial <matt.cordial@gmail.com>
 *
 */
public class User implements UserDetails {

	private static final long serialVersionUID = 4123856050860359465L;
	private String username;
	private String password;
	private boolean isEnabled = true;
	private boolean isAdmin = false;
	private Collection<String> assignedPrefixes = new ArrayList<String>();
	private Collection<String> suffixNamespaces = new ArrayList<String>();
	private Collection<GrantedAuthority> grantedAuthorities = new ArrayList<GrantedAuthority>();
	
	/**
	 * Full constructor.
	 *  
	 * @param username
	 * @param password
	 * @param isAdmin - Indicates the user can modify any handle with any prefix.
	 * @param isEnabled - Set to false to revoke access.
	 * @param prefixes - A user can only modify handles with these prefixes.
	 * @param suffixes - A user can only modify handles with these suffixes.
	 */
	public User(String username, String password, boolean isAdmin, 
	            boolean isEnabled, Collection<String> prefixes, 
	            Collection<String> suffixes) {
	    this(username, password);
	    this.isEnabled = isEnabled;
	    this.isAdmin = isAdmin;
	    if (isAdmin) {
	        grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
	    }
	    assignedPrefixes.addAll(prefixes);
	    suffixNamespaces.addAll(suffixes);
	}
	
	public User(String username, String password) {
	    this.username = username;
	    this.password = password;
	}
	
	public User(User other) {
        this(other.getUsername(), other.getPassword(), other.isAdmin(),
             other.isEnabled(), other.getAssignedPrefixes(),
             other.getSuffixNamespaces());
    }

	public Collection<String> getAssignedPrefixes() {
        return assignedPrefixes;
    }

    public void setAssignedPrefixes(Collection<String> assignedPrefixes) {
        this.assignedPrefixes = assignedPrefixes;
    }

    public Collection<String> getSuffixNamespaces() {
		return suffixNamespaces;
	}

	public void setHandleNamespace(Collection<String> suffixNamespaces) {
		this.suffixNamespaces = suffixNamespaces;
	}

    public Collection<GrantedAuthority> getAuthorities() {
        return grantedAuthorities;
    }
    
    public void setGrantedAuthorities(Collection<GrantedAuthority> authorities) {
        grantedAuthorities = authorities;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isAccountNonExpired() { return true; }

    public boolean isAccountNonLocked() { return true; }

    public boolean isCredentialsNonExpired() { return true; }

    public boolean isEnabled() { return isEnabled; }
    
    public boolean isAdmin() { return isAdmin; }
    
    public boolean canModifyPrefix(String prefix) {
        return isAdmin
               || assignedPrefixes.contains("*")
               || assignedPrefixes.contains(prefix);
    }
    
    public boolean canModifySuffix(String suffix) {
    	String[] parts = suffix.split("\\.", 2);
    	String ns = "";
    	if (parts.length == 2) ns = parts[0];
        return isAdmin 
               || suffixNamespaces.contains("*")
               || suffixNamespaces.contains(ns);
    }

}
