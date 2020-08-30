package test;

import dbConnector.Entity;

public class Dozent extends Entity<Dozent> {

	String name;
	Schueler schueler = new Schueler("testSchueler");
	
	public Dozent() {
		super();
		this.name = "Mustermann";
	}
}