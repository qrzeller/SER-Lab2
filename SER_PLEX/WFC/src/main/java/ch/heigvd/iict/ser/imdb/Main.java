package ch.heigvd.iict.ser.imdb;

import java.util.Scanner;

import ch.heigvd.iict.ser.imdb.db.MySQLAccess;
import ch.heigvd.iict.ser.imdb.models.Data;

public class Main {

	static {
		// this will load the MySQL driver, each DB has its own driver
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			System.err.println("MySQL drivers not found !");
			System.exit(1);
		}

		//database configuration
		MySQLAccess.MYSQL_URL 		= "docr.iict.ch";
		MySQLAccess.MYSQL_DBNAME 	= "imdb";
		MySQLAccess.MYSQL_USER 		= "imdb";
		MySQLAccess.MYSQL_PASSWORD 	= "imdb";
	}

	private static Scanner scanner = new Scanner(System.in);

	public static void main(String[] args) {
		Main main = new Main();
		main.run();
	}

	private Data lastData = null;

	private void run() {

		boolean continuer = true;		
		while(continuer) {
			System.out.print("Select the data version to download [1/2/3/0=quit]: ");
			int choice = -1;
			try {
				choice = scanner.nextInt();
			} catch(Exception e) {
				e.printStackTrace();
			}
			
			if(choice == 0) continuer = false;
			else if(choice >= 1 && choice <= 3) {
				Worker worker = new Worker(choice);
				this.lastData = worker.run();

				//TODO notify client
				
			}
		}
	}

}
