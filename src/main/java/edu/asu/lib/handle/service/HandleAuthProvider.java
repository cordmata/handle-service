/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.asu.lib.handle.service;

import javax.servlet.http.HttpServletRequest;
import net.handle.hdllib.PublicKeyAuthenticationInfo;

/**
 *
 * @author dshughe1
 */
public interface HandleAuthProvider {
    PublicKeyAuthenticationInfo getAuthenticationInfo(HttpServletRequest req);
}
