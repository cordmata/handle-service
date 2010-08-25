/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.asu.lib.handle.service;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import javax.sql.DataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;

/**
 *
 * @author dshughe1
 */
public class HandleDALImpl implements HandleDAL {
    private Log log = LogFactory.getLog(HandleDALImpl.class);
    private DataSource _datasource;
    private final static long _firstSeed = 0;
    public HandleDALImpl() {}

    public synchronized long getNextSeed(String serverHandle) throws SQLException {

        long result = -1;
        Connection conn = null;
        try {
            conn = _datasource.getConnection();
            conn.setAutoCommit(true);

            PreparedStatement selectSeed = conn.prepareStatement("select seed from handle_seeds where server=?");
            PreparedStatement updateSeed = conn.prepareStatement("update handle_seeds set seed=? where server=?");
            PreparedStatement initSeed = conn.prepareStatement("insert into handle_seeds (server, seed) values (?, ?)");

            // attempt to retrieve the current seed for the server handle
            selectSeed.setString(1, serverHandle);
            ResultSet seedResult = selectSeed.executeQuery();
            if(seedResult.next()) {
                // since a seed exists, use it as the result and increment the seed in the database
                result = seedResult.getLong("seed");
                seedResult.close();
                updateSeed.setLong(1, result + 1);
                updateSeed.setString(2, serverHandle);
                updateSeed.executeUpdate();
            }
            else {
                // since no seed exists, use zero as the current result and create
                // a row for this server handle in the seeds table
                seedResult.close();
                result = _firstSeed;
                initSeed.setString(1, serverHandle);
                initSeed.setLong(2, _firstSeed + 1);
                initSeed.executeUpdate();
            }
        } finally {
            if(conn != null) conn.close();
        }
        return result;
    }

    public void logEvent(String server, String operation, String originatingIP, String handle, String target) {
        try {
            Connection conn = _datasource.getConnection();
            conn.setAutoCommit(true);
            String insertStmt = "insert into handle_events (server, operation, timestamp, ip, handle, target) " +
                                "values (?, ?, ?, ?, ?, ?)";
            PreparedStatement insertEventLog = conn.prepareStatement(insertStmt);
            insertEventLog.setString(1, server);
            insertEventLog.setString(2, operation);
            insertEventLog.setDate(3, new Date(new java.util.Date().getTime()));
            insertEventLog.setString(4, originatingIP);
            insertEventLog.setString(5, handle);
            insertEventLog.setString(6, target);
            insertEventLog.executeUpdate();
        }
        catch(SQLException ex) {
            log.error(ex);
        }
    }

    public void setDataSource(DataSource ds) {
        _datasource = ds;
    }
}
