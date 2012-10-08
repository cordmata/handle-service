package edu.asu.lib.handle;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;

public class UserTest {

    @Test
    public void testDefault() {
    	User user = new User("test", "fake");
    	assertTrue(!user.isAdmin());
    	assertTrue(user.isEnabled());
    	assertTrue(user.getAssignedPrefixes().isEmpty());
    	assertTrue(user.getSuffixNamespaces().isEmpty());
    }

    @Test
    public void testAdminAuthority() {
    	User user = new User("test", "fake", true, true, 
    						 new ArrayList<String>(), new ArrayList<String>());
        assertTrue(user.getAuthorities()
        			   .iterator().next()
        			   .getAuthority() == "ROLE_ADMIN");
    }

    @Test
    public void testAuthorization() {
    	String prefix = "0000.0";
    	String suffix = "foo.bar";
    	
    	User admin = new User("test", "fake", true, true, 
    						  new ArrayList<String>(), new ArrayList<String>());
    	assertTrue(admin.getAssignedPrefixes().isEmpty());
    	assertTrue(admin.getSuffixNamespaces().isEmpty());
    	assertTrue(admin.canModifyPrefix(prefix));
    	assertTrue(admin.canModifySuffix(suffix));
    	
    	Collection<String> prefixes = new ArrayList<String>();
    	prefixes.add(prefix);
    	Collection<String> suffixes = new ArrayList<String>();
    	suffixes.add("foo");
    	
    	User user = new User("test2", "fake2", false, true, prefixes, suffixes);
    	
        assertTrue(user.getAssignedPrefixes().size() == 1);
        assertTrue(user.getSuffixNamespaces().size() == 1);
    	assertTrue(user.canModifyPrefix(prefix));
    	assertTrue(user.canModifySuffix(suffix));
    	assertFalse(user.canModifyPrefix("0000"));
    	assertFalse(user.canModifyPrefix("bar"));
    }

}
