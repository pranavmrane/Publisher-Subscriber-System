package edu.rit.CSCI652.demo;

import java.io.Serializable;

public class Event implements Serializable {

	private static final long serialVersionUID = 8309080721495266421L;
	private int id;
	private Topic topic;
	private String title;
	private String content;

	public Event() {

	}

    public Event(Topic topic, String title, String content){
        this.topic = topic;
        this.title = title;
        this.content = content;
    }

	public int getId() {
		return this.id;
	}

	public void setId(int newId) {
		this.id = newId;
	}

	public String getTitle() {
		return this.title;
	}

	public Topic getTopicForEvent() {
		return this.topic;
	}

	public String getTopicName() {
		return this.topic.getName();
	}

	public Topic getTopic() {
		return this.topic;
	}

	public void printAllVariables(){
		System.out.println("EVENT-> Title: " + this.title + ", Topic Name: "
				+ this.getTopicName() + ", Content: " + this.content);
	}

	public String toString() {
		return getTitle();
	}
}

