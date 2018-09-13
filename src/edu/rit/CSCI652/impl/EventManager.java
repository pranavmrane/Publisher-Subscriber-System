package edu.rit.CSCI652.impl;


import edu.rit.CSCI652.demo.Event;
import edu.rit.CSCI652.demo.Subscriber;
import edu.rit.CSCI652.demo.Topic;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class EventManager extends Thread {
	
	/*
	 * Start the repo service
	 */
	private ServerSocket ss = null;
	static int numberOfThreads = 4;

	// {topic1={subscriber1=position, subscriber2: position}, topic2 = {}}
    // subscriber1 is IP:port
	private HashMap topicsInfo = new HashMap();

	// [Publisher1(IP:port), Publisher2(IP:port)]
	private ArrayList<String> publishersInfo = new ArrayList<String>();

	// {subscriber1 = [topic1], subscriber2 = [topic1]}
	private HashMap subscribersInfo = new HashMap();

	// {topic1 = [event1, event2, event3], topic2 = [event11, event12, event13]]
	public EventManager() {

    }

	public EventManager(ServerSocket ss) {
	    this.ss = ss;
    }

	private void startService() {
		try {
            ss = new ServerSocket(4444);
            for (int i=0; i < numberOfThreads; i++) {
                EventManager em = new EventManager(ss);
                em.start();
            }
        }
        catch (IOException ex) {
		    System.err.println(ex);
        }
	}

	public void run() {
	    while (true) {
            try {
                Socket s = ss.accept();
                // OutputStream out = s.getOutputStream();
                // InputStream in = s.getInputStream();
                ObjectInputStream in = new ObjectInputStream(s.getInputStream
                        ());
                while (true) {
                    try {
                        int code = in.readInt();
                        if (code == 1) {
                        	Topic newTopic = (Topic) in.readObject();
                            this.addTopic(newTopic);
                        }

                        break;
                    }
                    catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
            catch (IOException ex) {
                System.out.println(ex);
            }
        }
    }

	/*
	 * notify all subscribers of new event 
	 */
	private void notifySubscribers(Event event) {
		
	}
	
	/*
	 * add new topic when received advertisement of new topic
	 */
	private void addTopic(Topic topic){
		// TODO: Broadcast to everyone
        System.out.println(topic.getName());
        this.topicsInfo.put(topic.getName(), new HashMap());
        System.out.println(topicsInfo);
	}
	
	/*
	 * add subscriber to the internal list
	 */
	private void addSubscriber(){
		
	}
	
	/*
	 * remove subscriber from the list
	 */
	private void removeSubscriber(){
		
	}
	
	/*
	 * show the list of subscriber for a specified topic
	 */
	private void showSubscribers(Topic topic){
		
	}
	
	
	public static void main(String[] args) {
		new EventManager().startService();
	}


}
