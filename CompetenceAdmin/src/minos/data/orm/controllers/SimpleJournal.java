package minos.data.orm.controllers;

import com.google.gson.Gson;

public class SimpleJournal {
	// =================================================================================================================
	// Fields
	// =================================================================================================================
	private String login;
	private String host;
	private Integer pid; // person id

	// =================================================================================================================
	// Constructors
	// =================================================================================================================
	public SimpleJournal() {
		login = null;
		host = null;
		pid = null;
	}

	public SimpleJournal( String login, String host, Integer pid ) {
		this.login = login;
		this.host = host;
		this.pid = pid;
	}

	// =================================================================================================================
	// Getter & Setter
	// =================================================================================================================
	public String getLogin() {
		return login;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public Integer getPid() {
		return pid;
	}

	public void setPid(Integer pid) {
		this.pid = pid;
	}

	// =================================================================================================================
	// Methods
	// =================================================================================================================
	public String toJson() {
		Gson gson = new Gson();
		return gson.toJson( this );
	}

	static public SimpleJournal fromJson( String str ) {
		if ( str == null ) return null;
		Gson gson = new Gson();
		return gson.fromJson( str, SimpleJournal.class );
	}
}