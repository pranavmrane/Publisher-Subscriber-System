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
    static int numberOfThreads = 0;
    private int listeningPort = 0;
    // Add Comment Here
    private String listeningAddress = "0.0.0.0";
    private String sendingAddress = "";
    private int sendingPort = 0;
    static private Vector<Topic> topicList = new Vector<Topic>();

    public PublisherAgent() {

    }

    public PublisherAgent(ServerSocket ss) {
        this.ss = ss;
    }
    /*
     * Starts two threads. One to listen and other to accept orders
     * */

    public void setAddresses(int listeningPort, int threadCount,
                             String sendingAddress, int sendingPort){

        this.listeningPort = listeningPort;
        numberOfThreads = threadCount;
        this.sendingAddress = sendingAddress;
        this.sendingPort = sendingPort;
    }

    public void printAddresses(){
        System.out.println("Listening Port: " + listeningPort);
        System.out.println("Thread Count: " + numberOfThreads);
        System.out.println("Sending Address: " + sendingAddress);
        System.out.println("Sending Port: " + sendingPort);
    }

    public void printTopicVectors(Vector<Topic> list){

        if(list.size() > 0){
            for(Topic topic: list){
                topic.printAllVariables();
            }
        }
        else {
            System.out.println("No Topics Available at this moment");
        }
    }

    public void printEventVectors(Vector<Event> list){

        if(list!=null){
            for(Event topic: list){
                topic.printAllVariables();
            }
        }
        else {
            System.out.println("No Pending Events Available at this moment");
        }
    }

    public void startService() {
        try {
            ss = new ServerSocket(listeningPort, 100, Inet4Address.getByName
                    (listeningAddress));
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
                }
                // 999 -> Receiving topics list
                else if (code == 999) {
                    topicList = (Vector<Topic>) in.readObject();
                    System.out.println("Receiving topics list: ");
                    printTopicVectors(topicList);
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
            clientSocket = new Socket(sendingAddress, sendingPort);
            ObjectOutputStream out = new ObjectOutputStream(clientSocket
                    .getOutputStream());
            out.writeInt(3);
            //out.writeUTF(Inet4Address.getLocalHost().getHostAddress() +
            //        ":" + this.listeningPort);
            out.writeObject(event);
            out.flush();

        } catch (IOException e) {
            System.out.println("EventManager is down!");
        }
    }

    /*Send New Topics Created to Event Manager*/
    @Override
    public void advertise(Topic newTopic) {
        Socket clientSocket = null;
        try {
            System.out.println("Advertising topic: " + newTopic.getName());
            clientSocket = new Socket(sendingAddress, sendingPort);
            ObjectOutputStream out = new ObjectOutputStream(clientSocket
                    .getOutputStream());
            out.writeInt(1);
            out.writeUTF(Inet4Address.getLocalHost().getHostAddress() +
                    ":" + this.listeningPort);
            out.writeObject(newTopic);
            out.flush();

        } catch (IOException e) {
            System.out.println("EventManager is down!");
        }
    }

    /*Connect and register network*/

    public void ping() {
        Socket clientSocket = null;
        try {
            System.out.println("Pinging...");
            clientSocket = new Socket(sendingAddress, sendingPort);
            ObjectOutputStream out = new ObjectOutputStream(clientSocket
                    .getOutputStream());
            out.writeInt(999);
            out.writeUTF(Inet4Address.getLocalHost().getHostAddress() +
                    ":" + this.listeningPort);
            out.flush();
        } catch (IOException e) {
            System.out.println("EventManager is down!");
        }
    }

    /*Displays all the topics ever registered with event manager*/
    private void displayTopicWithNumbers (){
        if(topicList.size() == 0) {
            System.out.println("No Topics Present");
        }
        else {
            System.out.println("The Topics Available are:");
            for (int i=0; i<topicList.size(); i++){
                System.out.println("" + i + ":" + topicList.get(i).getName());
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {

        PublisherAgent pubUI = new PublisherAgent();

        if (args.length==8 && args[0].equals("-port") &&
                args[2].equals("-threads") &&
                args[4].equals("-eventManagerIP") &&
                args[6].equals("-eventManagerPort")) {
            System.out.println("Setting Connection Variables:");
            pubUI.setAddresses(Integer.parseInt(args[1]),
                    Integer.parseInt(args[3]), args[5],
                    Integer.parseInt(args[7]));
            pubUI.printAddresses();

        }
        else {
            System.err.println("Enter Command Like this: " +
                    "PublisherAgent -port 10000 -threads 2 " +
                    "-eventManagerIP localhost -eventManagerPort 4444");
            System.err.println("Using Default Values");
            pubUI.setAddresses(10000, 2,
                    "localhost", 4444);
            pubUI.printAddresses();
        }

        pubUI.startService();
        Scanner sc = new Scanner(System.in);
        int userInput = 0;

        boolean loopStatus = true;
        sleep(2500);
        pubUI.ping();
        while(loopStatus){
            try{

                System.out.println("Press 1 to Advertise New Topic");
                System.out.println("Press 2 to Publish New Event");
                System.out.println("Press 3 to Ping");
                System.out.println("Press 4 to view topics");
                System.out.println("Press 5 to Exit the loop");
                System.out.println("Please select an option: ");

                userInput = sc.nextInt();

                switch (userInput){
                    case 1:
                        // Moving Cursor to next line
                        sc.nextLine();
                        System.out.println("Enter topic name: ");
                        String name = sc.nextLine();
                        System.out.println("Enter keywords, Separate By " +
                                "Commas: ");
                        String keywords = sc.nextLine();
                        String[] keywordsArray = keywords.split(",");
                        for(int i = 0; i<keywordsArray.length; i++){
                            keywordsArray[i] = keywordsArray[i].trim();
                        }
                        List<String> keywordsList =
                                new Vector<String>(Arrays.asList(keywordsArray));
                        System.out.println("Generating new topic -> " + "Name: "
                                + name + ", Keywords: " + keywords);
                        pubUI.advertise(new Topic(keywordsList,name));


                        break;
                    case 2:
                        try {
                            System.out.println("Select Topic Id for The Event");
                            pubUI.displayTopicWithNumbers();
                            int topicId = sc.nextInt();
                            // Moving Cursor to next line
                            sc.nextLine();
                            System.out.println("Enter Event Title");
                            String eventTitle = sc.nextLine();
                            System.out.println("Enter Content(String) for Event");
                            String content = sc.nextLine();
                            System.out.println("Generating new Event: "
                                    + "Title: " + eventTitle +
                                    ", Topic: " +
                                    topicList.get(topicId).getName() +
                                    ", Content: " + content);
                            pubUI.publish(new Event(topicList.get(topicId),
                                    eventTitle, content));
                        }
                        catch (ArrayIndexOutOfBoundsException e){
                            System.out.println("Start Again, Please Select " +
                                    "Topic Id in Range");
                        }

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
                        System.out.println("Please enter a correct value.");
                        break;
                }
            } catch (Exception e){
                System.out.println("Main Loop");
                e.printStackTrace();
            }
        }
        sc.close();
        System.out.println("Publisher Terminated");
        System.exit(0);
    }
}
