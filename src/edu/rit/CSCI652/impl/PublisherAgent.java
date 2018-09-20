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
import java.util.Vector;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class PublisherAgent extends Thread implements Publisher {

    private ServerSocket ss = null;
    static int numberOfThreads = 2;
    private int port = 10000;
    static private Vector<Topic> topicList = new Vector<Topic>();

    public PublisherAgent() {

    }

    public PublisherAgent(ServerSocket ss) {
        this.ss = ss;
    }
    /*
    * Starts two threads. One to listen and other to accept orders
    * */
    public void startService() {
        try {
            ss = new ServerSocket(port);
            PublisherAgent pub = new PublisherAgent(ss);
            pub.start();

        } catch (IOException ex) {
            ex.printStackTrace();
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
                    displayTopicWithNumbers();
                }
                // 999 -> Receiving topics list
                else if (code == 999) {
                    topicList = (Vector<Topic>) in.readObject();
                    System.out.println("Receiving topics list: " + topicList);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /*Send info about new event*/
    @Override
    public void publish(Event event) {
        Socket clientSocket = null;
        try {
            System.out.println("Publishing event: " + event.getTitle());
            clientSocket = new Socket("localhost", 4444);
            ObjectOutputStream out = new ObjectOutputStream(clientSocket
                    .getOutputStream());
            out.writeInt(3);
            //out.writeUTF(Inet4Address.getLocalHost().getHostAddress() +
            //        ":" + this.port);
            out.writeObject(event);
            out.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*Send New Topics Created to Event Manager*/
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
            out.writeObject(newTopic);
            out.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*Connect and register network*/

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

    /*Displays all the topics ever registered with event manager*/
    private void displayTopicWithNumbers (){
        if(topicList.size() == 0) System.out.println("No Topics Present");
        for (int i=0; i<topicList.size(); i++){
            System.out.println(i + ":" + topicList.get(i).getName());
        }
    }

    public static void main(String[] args) throws InterruptedException {

        PublisherAgent pubUI = new PublisherAgent();
        pubUI.startService();
        Scanner sc = new Scanner(System.in);
        int userInput = 0;

        boolean loopStatus = true;
        sleep(2500);
        try{
            pubUI.ping();
            pubUI.advertise(new Topic(Arrays.asList("India", "USA"),
                    "World"));
            pubUI.advertise(new Topic(Arrays.asList("Asia", "Europe"),
                    "Continent"));
            while(loopStatus){
                // Temporary codes Used: 54, 55
                // All other codes used to identify operations
                sleep(900);
                System.out.println("Press 1 to Advertise New Topic");
                System.out.println("Press 2 to Publish New Event");
                System.out.println("Press 3 to Ping");
                System.out.println("Press 4 to view topics");
                System.out.println("Press 5 to Exit the loop");
                System.out.print("Please select an option: ");

                userInput = sc.nextInt();

                switch (userInput){
                    case 1:
                        System.out.println("Enter topic name");
                        String name = sc.next();
//                        System.out.println("Enter Topic Id");
//                        int tid = sc.nextInt();
                        System.out.println("Enter keywords, Separate By Commas");
                        String keywords = sc.next();
                        String[] keywordsArray = keywords.split(",");
                        List<String> keywordsList =
                                new Vector<String>(Arrays.asList(keywordsArray));
                        System.out.println("Generating new topic" + " Name: " +name+
                                " keywords: " + keywords);
                         pubUI.advertise(new Topic(keywordsList,name));




                        break;
                    case 2:
                        System.out.println("Select Topic Id for The Event");
                        pubUI.displayTopicWithNumbers();
                        int topicId = sc.nextInt();
                        System.out.println("Enter Event Title");
                        String eventTitle = sc.next();
                        System.out.println("Enter Event Id");
                        int eid = sc.nextInt();
                        System.out.println("Enter Content(String) for Event");
                        String content = sc.next();
                        System.out.println("Generating new Event"
                                + " Title: " + eventTitle + " id: " + eid +
                                " Topic: " + topicList.get(topicId).getName() +
                                " content: " + content);
                        pubUI.publish(new Event(eid, topicList.get(topicId), eventTitle, content));
                        break;
                    case 3:
                        pubUI.ping();
                        break;
                    case 4:
                        pubUI.displayTopicWithNumbers();
                        break;
                    case 5:
                        loopStatus = false;
                        break;
                    default:
                        System.out.println("Please enter a correct value");
                        break;
                }// End of Switch Case
            }// End of While Loop
            sc.close();
            System.out.println("Publisher Terminated");
            System.exit(0);
        }// End of Try
        catch (Exception e){
            System.out.println("Main Loop");
            e.printStackTrace();
        }
    }
}
