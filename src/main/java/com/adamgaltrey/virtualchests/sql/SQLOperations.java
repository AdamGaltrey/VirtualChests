package com.adamgaltrey.virtualchests.sql;

import java.sql.*;

class SQLOperations {

	synchronized int standardQuery(String query, Connection connection) throws SQLException {
		Statement statement = connection.createStatement();
		int rowsUpdated = statement.executeUpdate(query);
		statement.close();
		return rowsUpdated;
	}

	synchronized ResultSet sqlQuery(String query, Connection connection) throws SQLException {
		Statement statement = connection.createStatement();
		return statement.executeQuery(query);
	}

	synchronized boolean checkTable(String table, Connection connection) throws SQLException {
		DatabaseMetaData dbm;
		dbm = connection.getMetaData();
		ResultSet tables = dbm.getTables(null, null, table, null);
		return tables.next();
	}

}
