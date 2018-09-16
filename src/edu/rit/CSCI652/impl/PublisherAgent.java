package edu.rit.CSCI652.impl;

import edu.rit.CSCI652.demo.Event;
import edu.rit.CSCI652.demo.Publisher;
import edu.rit.CSCI652.demo.Topic;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;

public class PublisherAgent extends Thread implements Publisher {

    private ServerSocket ss = null;
    static int numberOfThreads = 2;
    private int port = 10000;
    static private ArrayList<Topic> topicList = new ArrayList<Topic>();

    public PublisherAgent() {

    }

    public PublisherAgent(ServerSocket ss) {
        this.ss = ss;
    }

    public void startService() {
        try {
            ss = new ServerSocket(port);
            PublisherAgent pub = new PublisherAgent(ss);
            pub.start();

        } catch (IOException ex) {
            System.out.println(ex);
        }
    }

    public void run() {
        System.out.println("Publisher listening.");
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
                // 999 -> Receiving topics list
                else if (code == 999) {
                    topicList = (ArrayList<Topic>) in.readObject();
                    System.out.println("Receiving topics list: " + topicList);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    public void publish(Event event) {
        Socket clientSocket = null;
        try {
            System.out.println("Publishing event: " + event.getTitle());
            clientSocket = new Socket("localhost", 4444);
            ObjectOutputStream out = new ObjectOutputStream(clientSocket
                    .getOutputStream());
            out.writeInt(3);
            out.writeUTF(Inet4Address.getLocalHost().getHostAddress() +
                    ":" + this.port);
            out.flush();

            out.writeObject(event);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void advertise(Topic newTopic) {
        Socket clientSocket = null;
        try {
            System.out.println("Advertising topic: " + newTopic.getName());
            clientSocket = new Socket("localhost", 4444);
            ObjectOutputStream out = new ObjectOutputStream(clientSocket
                    .getOutputStream());
            out.writeInt(1);
            out.writeUTF(Inet4Address.getLocalHost().getHostAddress() +
                    ":" + this.port);
            out.flush();
            out.writeObject(newTopic);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void ping() {
        Socket clientSocket = null;
        try {
            System.out.println("Pinging.");
            clientSocket = new Socket("localhost", 4444);
            ObjectOutputStream out = new ObjectOutputStream(clientSocket
                    .getOutputStream());
            out.writeInt(999);
            out.writeUTF(Inet4Address.getLocalHost().getHostAddress() +
                    ":" + this.port);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        PublisherAgent pubUI = new PublisherAgent();
        pubUI.startService();
        // TODO: while loop for Command Line interface

        sleep(2000);
        pubUI.ping();
        sleep(2000);
        Topic newTopic = new Topic(1, Arrays.asList("Test", "test"),
                "Test");
        pubUI.advertise(newTopic);
        while (true) {
            sleep(1000);
        }
    }

}
