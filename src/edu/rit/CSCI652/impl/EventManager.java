package edu.rit.CSCI652.impl;


import edu.rit.CSCI652.demo.Event;
import edu.rit.CSCI652.demo.Subscriber;
import edu.rit.CSCI652.demo.Topic;

import java.io.*;
import java.lang.reflect.Array;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
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
    static private HashMap<String, HashMap<String, Integer>> topicsInfo = new
            HashMap<String, HashMap<String, Integer>>();

    // [Publisher1(IP:port), Publisher2(IP:port)]
    static private ArrayList<String> publishersInfo = new ArrayList<String>();

    // {subscriber1 = [topic1], subscriber2 = [topic1]}
    static private HashMap<String, ArrayList<String>> subscribersInfo = new
            HashMap<String, ArrayList<String>>();

    // {topic1 = [event1, event2, event3], topic2 = [event11, event12, event13]]
    static private HashMap<String, ArrayList<Event>> topicQueue = new
            HashMap<String, ArrayList<Event>>();

    public EventManager() {

    }

    public EventManager(ServerSocket ss) {
        this.ss = ss;
    }

    private void startService() {
        try {
            ss = new ServerSocket(4444);
            for (int i = 0; i < numberOfThreads; i++) {
                EventManager em = new EventManager(ss);
                em.start();
            }
        } catch (IOException ex) {
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
                try {
                    int code = in.readInt();
                    // 1 -> advertise topic, add new topic.
                    if (code == 1) {
                        String publisherId = in.readUTF();
                        Topic newTopic = (Topic) in.readObject();
                        validatePublisher(publisherId);
                        this.addTopic(newTopic);
                    }
                    // 2 -> subscribe to given topic
                    else if (code == 2) {
                        String subscriberId = in.readUTF();
                        String inet = s.getInetAddress().getHostAddress();
                        Topic topic = (Topic) in.readObject();
                        this.addSubscriber(subscriberId, topic.getName());
                    }
                    // 3 -> new event published, notify subscribers
                    else if (code == 3) {
                        Event newEvent = (Event) in.readObject();
                        this.notifySubscribers(newEvent);
                    }

                    break;
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            } catch (IOException ex) {
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
     * check if new publisher is sending a request
     */

    private void validatePublisher(String publisherId) {
        if (publishersInfo.contains(publisherId)) {
            return;
        } else {
            System.out.println("Adding new Publisher: " + publisherId);
            publishersInfo.add(publisherId);
        }
    }

    /*
     * add new topic when received advertisement of new topic
     */
    private void addTopic(Topic topic) {
        // Check if topic already exists
        if (topicsInfo.containsKey(topic.getName())) {
            System.out.println("Topic already exists");
            return;
        }
        // Topic does not exist, create and notify everyone
        else {
            System.out.println("Adding topic: " + topic.getName());
            this.topicsInfo.put(topic.getName(), new HashMap<String, Integer>());
            System.out.println(topicsInfo);
            // TODO: Broadcast to everyone
            Socket sendSocket;
            String destination;
            int port;
            for (String s : publishersInfo) {
                try {
                    destination = s.split(":")[0];
                    port = Integer.parseInt(s.split(":")[1]);
                    sendSocket = new Socket(destination, port);
                    ObjectOutputStream out = new ObjectOutputStream
                            (sendSocket.getOutputStream());
                    out.writeInt(1);
                    out.writeObject(topic);
                    out.close();
                } catch (IOException e) {
                    // TODO: Publisher is offline. Handle it!
                    e.printStackTrace();
                }
            }

            for (String s : subscribersInfo.keySet()) {
                try {
                    destination = s.split(":")[0];
                    port = Integer.parseInt(s.split(":")[1]);
                    sendSocket = new Socket(destination, port);
                    ObjectOutputStream out = new ObjectOutputStream
                            (sendSocket.getOutputStream());
                    out.write(1);
                    out.writeObject(topic);
                } catch (IOException e) {
                    // TODO: Subscriber is offline. Handle it!
                    e.printStackTrace();
                }
            }
        }
    }

    /*
     * add subscriber to the internal list
     */
    private void addSubscriber(String sub, String topicName) {
        // If subscriber exists, just add topic
        if (subscribersInfo.containsKey(sub)) {
            ArrayList<String> tmp = subscribersInfo.get
                    (sub);
            if (tmp.contains(topicName)) {
                System.out.println("Already subscribed");
                return;
            } else {
                System.out.println("Subscribing " + sub + "to " + topicName);
                tmp.add(topicName);
                subscribersInfo.put(sub, tmp);
            }
        }
        // Subscriber does not exist, add to list of subscribers
        else {
            System.out.println("Adding new subscriber: " + sub);
            ArrayList<String> tmp = new ArrayList<String>();
            tmp.add(topicName);
            subscribersInfo.put(sub, tmp);
        }

        HashMap<String, Integer> topicInfo = topicsInfo.get(topicName);
        if (topicInfo.containsKey(sub)) {
            System.out.println("Already subscribed.");
        } else {
            topicInfo.put(sub, 0);
            topicsInfo.put(topicName, topicInfo);
        }

        System.out.println("Current subscribers list: " + subscribersInfo);
        System.out.println("Current subscribers per topic: " + topicsInfo);
    }

    /*
     * remove subscriber from the list
     */
    private void removeSubscriber() {

    }

    /*
     * show the list of subscriber for a specified topic
     */
    private void showSubscribers(Topic topic) {

    }


    public static void main(String[] args) {
        new EventManager().startService();
    }


}
