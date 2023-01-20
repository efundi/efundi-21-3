package edu.nwu.sakaistudentlink.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;

/**
 * This class handles all the connections to the different databases.<br/>
 * If no properties are specified in sakai.properties, the default jndi settings are used.
 */
public class ConnectionManager {

    private static final Logger log = LoggerFactory.getLogger(ConnectionManager.class);

    public Connection getCourseManagementConnection()
            throws ConnectionNotEstablishedException {
        Connection connection = null;
        WebApplicationContext context = ContextLoader.getCurrentWebApplicationContext();
		DataSource datasource  = (DataSource) context.getBean("eFundiDataSource");    

		try {
			connection = datasource.getConnection();
		} catch (SQLException e1) {

            throw new ConnectionNotEstablishedException(
                    "Could not establish Course Management JDBC connection.", e1);
		}
        return connection;
    }

    public static void close(ResultSet rset, PreparedStatement pstmt, Connection connection) {
        try {
            if (rset != null) {
                rset.close();
            }
        }
        catch (Exception e) {
            log.error("Error closing sql resultset", e);
        }
        try {
            if (pstmt != null) {
                pstmt.close();
            }
        }
        catch (Exception e) {
            log.error("Error closing sql statement", e);
        }
        try {
            if (connection != null) {
                connection.close();
            }
        }
        catch (Exception e) {
            log.error("Error closing sql connection", e);
        }
    }
}