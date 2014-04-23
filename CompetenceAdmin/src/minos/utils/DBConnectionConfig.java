package minos.utils;

import java.io.Serializable;

public class DBConnectionConfig implements Serializable {
	private static final long serialVersionUID = 1;
	
	private String serverAddress = null; // ip or dns name
	private String dbName = null;
	private String dbInstance = null;
	private int dbPort = 0;
	private boolean integratedSecurity = false;
	private String login = null;
	private String password = null; 
	
	public DBConnectionConfig() {
		super();
	}

	public DBConnectionConfig(String serverAddress, String dbName, String dbInstance, int dbPort, 
			boolean integratedSecurity, String login, String password) {
		super();
		this.serverAddress = serverAddress;
		this.dbName = dbName;
		this.dbInstance = dbInstance;
		this.dbPort = dbPort;
		this.integratedSecurity = integratedSecurity;
		if(!integratedSecurity) {
			this.login = login;
			this.password = password;
		}
	}

	public String getServerAddress() {
		return serverAddress;
	}
	public void setServerAddress(String serverAddress) {
		this.serverAddress = serverAddress;
	}
	public String getDbName() {
		return dbName;
	}
	public void setDbName(String dbName) {
		this.dbName = dbName;
	}
	public String getDbInstance() {
		return dbInstance;
	}
	public void setDbInstance(String dbInstance) {
		this.dbInstance = dbInstance;
	}
	public int getDbPort() {
		return dbPort;
	}
	public void setDbPort(int dbPort) {
		this.dbPort = dbPort;
	}
	public boolean isIntegratedSecurity() {
		return integratedSecurity;
	}
	public void setIntegratedSecurity(boolean integratedSecurity) {
		this.integratedSecurity = integratedSecurity;
	}
	public String getLogin() {
		return login;
	}
	public void setLogin(String login) {
		this.login = login;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
}