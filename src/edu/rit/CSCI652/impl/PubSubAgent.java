package edu.rit.CSCI652.impl;

import edu.rit.CSCI652.demo.Event;
import edu.rit.CSCI652.demo.Publisher;
import edu.rit.CSCI652.demo.Subscriber;
import edu.rit.CSCI652.demo.Topic;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

public class PubSubAgent implements Publisher, Subscriber{

	@Override
	public void subscribe(Topic topic) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void subscribe(String keyword) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void unsubscribe(Topic topic) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void unsubscribe() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void listSubscribedTopics() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void publish(Event event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void advertise(Topic newTopic) {
		// TODO Auto-generated method stub
		// List<String> testList = Arrays.asList("Test", "test");
		// newTopic =  new Topic(1, testList,"Test");

		Socket clientSocket = null;
		try {
			clientSocket = new Socket("localhost", 4444);
			ObjectOutputStream out = new ObjectOutputStream(clientSocket
					.getOutputStream());
			out.writeInt(1);
			out.writeObject(newTopic);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		new PubSubAgent().advertise(new Topic(Arrays.asList("Test", "test"),"Test"));
	}

}
