package com.adamgaltrey.virtualchests.sql;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class SQLConfig {
	
	private final File f;
	private FileConfiguration io;
	
	public SQLConfig(File f){
		this.f = f;
		this.io = YamlConfiguration.loadConfiguration(f);
	}
	
	public boolean createDefault(){
		if(!f.exists()){
			try {
				f.createNewFile();
				io.set("host", "localhost");
				io.set("username", "admin");
				io.set("password", "password");
				io.set("database", "minecraft");
				io.save(f);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return true;
		} else {
			return false;
		}
	}
	
	public SyncSQL load(){
		return new SyncSQL(io.getString("host"), io.getString("database"), io.getString("username"), io.getString("password"));
	}

}
