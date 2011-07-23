package com.pi.server.database;

import java.io.IOException;

import com.pi.server.Server;

public class ServerDatabase {
    // private final Server server;
    private AccountDatabase accountsDB;

    public ServerDatabase(final Server server) {
	// this.server = server;
	try {
	    this.accountsDB = new AccountDatabase();
	} catch (IOException e) {
	    e.printStackTrace();
	    this.accountsDB = null;
	} catch (ClassNotFoundException e) {
	    e.printStackTrace();
	    this.accountsDB = null;
	}
    }

    public AccountDatabase getAccounts() {
	return this.accountsDB;
    }
    
    public void save(){
	try {
	    accountsDB.writeData();
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }
}
