package edu.asu.lib.handle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * {@link UserDetailsService} implementation that can be populated with a list
 * of users defined as {@link Properties}. This is intended to be used as an
 * easy way to add a few users via Spring configuration.
 * 
 * @author Matt Cordial <matt.cordial@gmail.com>
 *
 */
public class PropertiesUserDetailsService implements UserDetailsService {
    
    private static Map<String, User> users = new HashMap<String, User>();

    public PropertiesUserDetailsService(){}
    public PropertiesUserDetailsService(Collection<Properties> users) {
        for (Properties user : users) {
            createUser(user); 
        }
    }
    
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (!users.containsKey(username)) { 
            throw new UsernameNotFoundException("No user with the username: " + username);
        }
        return new User(users.get(username));
    }
    
    public void createUser(Properties user) throws IllegalArgumentException {
        String username = user.getProperty("username");
        String password = user.getProperty("password");
        boolean isAdmin = "true".equalsIgnoreCase(user.getProperty("isAdmin"));
        boolean isEnabled = "true".equalsIgnoreCase(user.getProperty("isEnabled", "true"));
        Collection<String> prefixes = splitCommaSeparatedString(user.getProperty("allowedPrefixes"));
        Collection<String> suffixes = splitCommaSeparatedString(user.getProperty("allowedSuffixes"));
        
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            throw new IllegalArgumentException("You must supply both a username and a password.");
        }
        users.put(username, new User(username, password, isAdmin, isEnabled, 
                                     prefixes, suffixes));
    }
    
    private Collection<String> splitCommaSeparatedString(String s) {
        Collection<String> ret = new ArrayList<String>();
        if (StringUtils.isNotBlank(s)) {
            for (String val : s.split(",")) { ret.add(val.trim()); }
        }
        return ret;
    }
    
}
