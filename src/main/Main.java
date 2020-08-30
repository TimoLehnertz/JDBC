package main;

import dbConnector.DBConnector;
import test.Dozent;
import test.Schueler;
import test.Test;

public class Main {

	public static void main(String[] args) {
		DBConnector.getInstance().begin();
		
		Test t = new Test();
		Dozent d = new Dozent();
		Schueler s = new Schueler();
		
		t.dropTableIfExists();
		d.dropTableIfExists();
		s.dropTableIfExists();

		for (int i = 0; i < 3; i++) {
			Test t1 = new Test();
			t1.initialize();
			t1.save();
		}
		
		t.printTable();
		
		new Dozent().printTable();
		System.out.println(new Test().getAllEntities());
	}
}