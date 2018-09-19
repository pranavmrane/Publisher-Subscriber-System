package edu.rit.CSCI652.demo;

import java.io.Serializable;
import java.util.List;

public class Event implements Serializable {

	private static final long serialVersionUID = 8309080721495266421L;
	private int id;
	private Topic topic;
	private String title;
	private String content;

	public Event(int id, Topic topic, String title, String content){
	    this.id = id;
	    this.topic = topic;
	    this.title = title;
	    this.content = content;
    }

	public String getTitle() {
		return this.title;
	}

	public Topic getTopicForEvent() {return this.topic;}
}
