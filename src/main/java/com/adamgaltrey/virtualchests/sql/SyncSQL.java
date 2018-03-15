package com.adamgaltrey.virtualchests.sql;

import com.adamgaltrey.virtualchests.VirtualChests;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;


public class SyncSQL extends SQLOperations {

	private String host, database, username, password;
	private SCHEMA schema;
	private Connection connection;
	private File databaseFile;

	private boolean active = false;

	SyncSQL(String host, String database, String username, String password) {
		this.host = host;
		this.database = database;
		this.username = username;
		this.password = password;
		this.schema = SCHEMA.MySQL;
	}

	/**
	 * Reopens the SQL connection if it is closed. This is invoked upon every
	 * query.
	 */
	public void refreshConnection() {
		if (connection == null) {
			initialise();
		}
	}
	
	public boolean isActive(){
		return active;
	}

	/**
	 * Manually close the connection.
	 */
	public void closeConnection() {
		try {
			this.connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Initialise a new connection. This will automatically create the database
	 * file if you are using SQLite and it doesn't already exist.
	 * 
	 * @return
	 */
	public boolean initialise() {
		if (schema == SCHEMA.MySQL) {
			try {
				Class.forName("com.mysql.jdbc.Driver");
				connection = DriverManager.getConnection("jdbc:mysql://" + host + "/" + database, username, password);
				active = true;
				return true;
			} catch (ClassNotFoundException ex) {
				VirtualChests.logger.info("Could not find MySQL driver class!");
				ex.printStackTrace();
			} catch (SQLException ex) {
                VirtualChests.logger.info("SQL Exception!");
				//ex.printStackTrace();
			}
		} else {
			if (!databaseFile.exists()) {
				try {
					databaseFile.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			try {
				Class.forName("org.sqlite.JDBC");
				connection = DriverManager.getConnection("jdbc:sqlite:" + this.databaseFile.getAbsolutePath());
				active = true;
				return true;
			} catch (SQLException ex) {
                VirtualChests.logger.info("SQL Exception!");
				ex.printStackTrace();
			} catch (ClassNotFoundException ex) {
                VirtualChests.logger.info("Could not find SQLite driver class!");
				ex.printStackTrace();
			}
		}
		active = false;
		return false;
	}

	public Connection getConnection() {
		return this.connection;
	}

	/**
	 * Any query which does not return a ResultSet object. Such as : INSERT,
	 * UPDATE, CREATE TABLE...
	 * 
	 * @param query
	 */
	public void standardQuery(String query) throws SQLException {
		if (active) {
			this.refreshConnection();
			super.standardQuery(query, this.connection);
		}
	}

	/**
	 * Check whether a field/entry exists in a database.
	 * 
	 * @param query
	 * @return Whether or not a result has been found in the query.
	 * @throws java.sql.SQLException
	 */
	public boolean existanceQuery(String query) throws SQLException {
		this.refreshConnection();
		return super.sqlQuery(query, this.connection).next();
	}

	/**
	 * Any query which returns a ResultSet object. Such as : SELECT Remember to
	 * close the ResultSet object after you are done with it to free up
	 * resources immediately. ----- ResultSet set =
	 * sqlQuery("SELECT * FROM sometable;"); set.doSomething(); set.close();
	 * -----
	 * 
	 * @param query
	 * @return ResultSet
	 */
	public ResultSet sqlQuery(String query) throws SQLException {
		this.refreshConnection();
		return super.sqlQuery(query, this.connection);
	}

	/**
	 * Check whether the table name exists.
	 * 
	 * @param table
	 * @return
	 */
	public boolean doesTableExist(String table) throws SQLException {
		this.refreshConnection();
		return super.checkTable(table, this.connection);
	}

}
