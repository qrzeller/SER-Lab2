package ch.heigvd.iict.ser.imdb;

import java.text.DecimalFormat;

import ch.heigvd.iict.ser.imdb.db.MySQLAccess;
import ch.heigvd.iict.ser.imdb.models.Data;

public class Worker {

	private int step = -1;
	
	public Worker(int step) {
		this.step = step;
	}
	
	public Data run() {
		//we notify ui that the thread is running
		System.out.println("Starting dataset "+ step + "...");

		//log
		long s = System.currentTimeMillis();
		
		//we connect to DB
		MySQLAccess db = new MySQLAccess();
		db.connect();
		
		Data data = db.getData(this.step);
		
		//log
		System.out.println("Done dataset "+ step +" [" + displaySeconds(s, System.currentTimeMillis()) + "]");
		
		//we disconnect from database
		db.disconnect();
		
		return data;
	}
	
	private static final DecimalFormat doubleFormat = new DecimalFormat("#.#");
	private static final String displaySeconds(long start, long end) {
		long diff = Math.abs(end - start);
		double seconds = ((double) diff) / 1000.0;
		return doubleFormat.format(seconds) + " s";
	}
	
}
