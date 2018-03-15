package com.adamgaltrey.virtualchests.sql;

public enum SCHEMA {
	
	MySQL,
	SQLite;
	
	@Override
	public String toString(){
		return super.toString().toUpperCase();
	}

}
