package edu.rit.CSCI652.impl;

import edu.rit.CSCI652.demo.Event;
import edu.rit.CSCI652.demo.Subscriber;
import edu.rit.CSCI652.demo.Topic;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;

public class SubscriberAgent extends Thread implements Subscriber {
    private ServerSocket ss = null;
    static int numberOfThreads = 2;
    private int port = 11000;
    static private ArrayList<Topic> topicList = new ArrayList<Topic>();

    public SubscriberAgent() {

    }

    public SubscriberAgent(ServerSocket ss) {
        this.ss = ss;
    }

    public void startService() {
        try {
            ss = new ServerSocket(port);
            SubscriberAgent pub = new SubscriberAgent(ss);
            pub.start();

        } catch (IOException ex) {
            System.out.println(ex);
        }
    }

    public void run() {
        System.out.println("Subscriber listening.");
        while (true) {
            try {
                Socket s = ss.accept();
                ObjectInputStream in = new ObjectInputStream(s.getInputStream());
                int code = in.readInt();

                // 1 -> EventManager is sending an advertise for a
                // newTopic.
                if (code == 1) {
                    Topic newTopic = (Topic) in.readObject();
                    topicList.add(newTopic);
                    System.out.println("Received new topic: " +
                            newTopic.getName());
                    System.out.println(topicList);
                }
            } catch (IOException e) {
                System.out.println(e);
            } catch (ClassNotFoundException e) {
                System.out.println(e);
            }

        }

    }

    @Override
    public void subscribe(Topic topic) {
        Socket clientSocket = null;
        try {
            System.out.println("Subscribing to topic: " + topic.getName());
            clientSocket = new Socket("localhost", 4444);
            ObjectOutputStream out = new ObjectOutputStream(clientSocket
                    .getOutputStream());
            // 2 -> subscribe to topic
            out.writeInt(2);
            out.writeUTF(Inet4Address.getLocalHost().getHostAddress() +
                    ":" + this.port);
            out.flush();
            out.writeObject(topic);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void subscribe(String keyword) {

    }

    @Override
    public void unsubscribe(Topic topic) {

    }

    @Override
    public void unsubscribe() {

    }

    @Override
    public void listSubscribedTopics() {

    }

    public static void main(String[] args) throws InterruptedException {
        SubscriberAgent subUI = new SubscriberAgent();
        subUI.startService();
        // TODO: while loop for Command Line interface
        Topic topic = new Topic("Test");
        subUI.subscribe(topic);
        while (true) {
            sleep(1000);
        }
    }
}
