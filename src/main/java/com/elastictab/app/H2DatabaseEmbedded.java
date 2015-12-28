package com.elastictab.app;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.h2.tools.Server;

import com.elastictab.util.Constants;

public class H2DatabaseEmbedded {

	static Properties H2DBProperties = new Properties();

	H2DatabaseEmbedded(String configPath) {
		try {
			InputStream input = new FileInputStream(configPath);
			H2DBProperties.load(input);
		} catch (IOException e) {
			e.printStackTrace();
		}

		startTCPServer();
		if (H2DBProperties.getProperty(Constants.runWebServer) == null || Boolean.valueOf(H2DBProperties.getProperty(Constants.runWebServer))) {
			startWebServer();
		}
		createDB();
	}

	private void createDB() {
		if (!tableExist()) {
			createDBTables();
		}
	}

	private boolean tableExist() {
		String query = "SELECT count(1) FROM information_schema.tables where table_NAME='QRTZ_CALENDARS'";
		PreparedStatement selectPreparedStatement;
		try {
			selectPreparedStatement = getDBConnection().prepareStatement(query);
			ResultSet rs = selectPreparedStatement.executeQuery();
			while (rs.next()) {
				if (rs.getInt(1) > 0) {
					return true;
				} else {
					return false;
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return true;
		}
		return true;
	}

	private void createDBTables() {
		String[] queries = Constants.queries;
		Connection connection = getDBConnection();
		for (int i = 0; i < queries.length; i++) {
			PreparedStatement preparedStatement;
			try {
				preparedStatement = connection.prepareStatement(queries[i]);
				preparedStatement.execute();
				preparedStatement.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		try {
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void startWebServer() {
		Thread t = new Thread() {
			public void run() {
				try {
					Server.createWebServer(H2DBProperties.getProperty(Constants.webServerConfig).split(",")).start();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		};
		t.start();
	}

	private void startTCPServer() {
		Thread t = new Thread() {
			public void run() {
				try {
					Server.createTcpServer(H2DBProperties.getProperty(Constants.TCPServerConfig).split(",")).start();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		};
		t.start();
	}

	private static Connection getDBConnection() {
		Connection dbConnection = null;
		try {
			Class.forName(H2DBProperties.getProperty(Constants.DB_DRIVER));
		} catch (ClassNotFoundException e) {
			System.out.println(e.getMessage());
		}
		try {
			dbConnection = DriverManager.getConnection(H2DBProperties.getProperty(Constants.DB_CONNECTION), H2DBProperties.getProperty(Constants.DB_USER),
					H2DBProperties.getProperty(Constants.DB_PASSWORD));
			return dbConnection;
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		return dbConnection;
	}
}
