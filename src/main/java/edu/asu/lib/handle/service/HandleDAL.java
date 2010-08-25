/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.asu.lib.handle.service;

import java.sql.SQLException;
import java.util.Date;

/**
 *
 * @author dshughe1
 */
public interface HandleDAL {
    long getNextSeed(String serverHandle) throws SQLException;
    void logEvent(String serverHandle, String operation, String originatingIP, String handle, String data);
}
