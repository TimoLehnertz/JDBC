package test;

import dbConnector.Entity;

public class Schueler extends Entity<Schueler> {

	public Schueler() {
		this("testName");
	}
	
	public Schueler(String name) {
		super();
		this.name = name;
	}
	
	String name = "test schueler";
}