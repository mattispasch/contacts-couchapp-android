package de.mpasch.p2p_sync.android;

import android.app.Application;
import de.mpasch.p2p_sync.android.db.DatabaseDAO;

public class MyApplication extends Application {
	
	private DatabaseDAO database;
	
	@Override
	public void onCreate() {
		database = new DatabaseDAO();
		database.start(this);
	}
	
	@Override
	public void onTerminate() {
		database.close();
	}

	public DatabaseDAO getDatabase() {
		return database;
	}
	

}
