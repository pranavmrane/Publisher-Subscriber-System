package edu.rit.CSCI652.demo;

import java.io.Serializable;
import java.util.List;

public class Topic implements Serializable {
	private static final long serialVersionUID = 8309080721495266420L;
	private int id;
	private List<String> keywords;
	private String name;

	public Topic() {

	}

	public Topic(String name) {
	    this.name = name;
    }

	public Topic(int id, List<String> keywords, String name) {
		this.id = id;
		this.keywords = keywords;
		this.name = name;
	}

	public String getName() {
		return this.name;
	}
}