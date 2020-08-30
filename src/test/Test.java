package test;

import java.util.ArrayList;
import java.util.List;

import dbConnector.Entity;

public class Test extends Entity<Test>{

	int i;
	String s;
	Dozent dozent;
	Test t;
	
//	public Test(int i) {
//		super();
//	}
	
	List<String> testList = new ArrayList<String>();
	
	public void initialize() {
		this.i = 0;
		this.s = "Test String";
		this.dozent = new Dozent();
		testList.add("test1");
		testList.add("test2");
		t = new Test();
	}
}