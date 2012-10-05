package edu.asu.lib.handle;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class PropertiesUserDetailsServiceTest {

    PropertiesUserDetailsService service;
    Properties billProp;
    
    @Before
    public void instantiateService() {
        billProp = new Properties();
        billProp.setProperty("username", "bill");
        billProp.setProperty("password", "foo");
        service = new PropertiesUserDetailsService();
    }
    
    @Test
    public void testUserRetrieval() {
        service.createUser(billProp);
        assertNotNull(service.loadUserByUsername("bill"));
        try {
            service.loadUserByUsername("absent");
            fail("Should have thrown an exception.");
        } catch (UsernameNotFoundException e) { }
    }
    
    @Test
    public void testConstructor() {
        Properties janeProp = new Properties();
        janeProp.setProperty("username", "jane");
        janeProp.setProperty("password", "bar");
        
        Collection<Properties> users = new ArrayList<Properties>();
        users.add(billProp);
        users.add(janeProp);
        
        PropertiesUserDetailsService serv = new PropertiesUserDetailsService(users);
        
        assertTrue(serv.loadUserByUsername("bill") instanceof UserDetails);
        assertTrue(serv.loadUserByUsername("jane") instanceof User);
    }
    
    @Test
    public void testDefaultProperties() {
        service.createUser(billProp); // minimum default user props
        User bill = (User) service.loadUserByUsername("bill");
        assertTrue(bill.isEnabled());
        assertTrue(!bill.isAdmin());
        assertTrue(bill.getAssignedPrefixes().isEmpty());
        assertTrue(bill.getSuffixNamespaces().isEmpty());
        assertTrue(bill.getAuthorities().isEmpty());
    }
    
    @Test
    public void testUserCreation() {
        billProp.setProperty("isAdmin", "true");
        billProp.setProperty("isEnabled", "false");
        billProp.setProperty("allowedPrefixes", "0000.0,0000.1");
        billProp.setProperty("allowedSuffixes", "abcd, efgh");
        service.createUser(billProp);
        User bill = (User) service.loadUserByUsername("bill");
        assertTrue(!bill.isEnabled());
        assertTrue(bill.isAdmin());
        assertTrue(bill.getAssignedPrefixes().size() == 2);
        assertTrue(bill.getAssignedPrefixes().contains("0000.0"));
        assertTrue(bill.getSuffixNamespaces().size() == 2);
        assertTrue(bill.getSuffixNamespaces().contains("efgh"));
        assertTrue(bill.getAuthorities().iterator().next().getAuthority() == "ROLE_ADMIN");
    }
    
    @Test
    public void testBadProperties() {
        Properties bad = new Properties();
        bad.setProperty("username", "bad");
        bad.setProperty("isAdmin", "borked");
        User badUser = null;
        try {
            service.createUser(bad);
        } catch (IllegalArgumentException e) {}
        assertNull(badUser);
    }
    
}
