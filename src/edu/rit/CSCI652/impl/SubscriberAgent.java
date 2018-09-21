package edu.rit.CSCI652.impl;

import edu.rit.CSCI652.demo.Event;
import edu.rit.CSCI652.demo.Subscriber;
import edu.rit.CSCI652.demo.Topic;

import javax.annotation.processing.SupportedSourceVersion;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;
import java.util.Arrays;
import java.util.Scanner;
import java.util.Vector;

public class SubscriberAgent extends Thread implements Subscriber {
    private ServerSocket ss = null;
    static int numberOfThreads = 0;
    private int listeningPort = 0;
    private String sendingAddress = "";
    private int sendingPort = 0;
    static private Vector<Topic> topicList = new Vector<Topic>();

    public SubscriberAgent() {

    }

    public SubscriberAgent(ServerSocket ss) {
        this.ss = ss;
    }

    public void setAddresses(int listeningPort, int threadCount,
                             String sendingAddress, int sendingPort){

        this.listeningPort = listeningPort;
        numberOfThreads = threadCount;
        this.sendingAddress = sendingAddress;
        this.sendingPort = sendingPort;
    }

    public void printAddresses(){
        System.out.println("\tListening Port: " + listeningPort);
        System.out.println("\tThread Count: " + numberOfThreads);
        System.out.println("\tSending Address: " + sendingAddress);
        System.out.println("\tSending Port: " + sendingPort);
    }

    public void printTopicVectors(Vector<Topic> list){

        if(list.size() > 0){
            for(Topic topic: list){
                topic.printAllVariables();
                //System.out.println();
            }
        }
        else {
            System.out.println("\tNo Topic Available");
        }
    }

    public void printEventVectors(Vector<Event> list){

        if(list!=null){
            for(Event topic: list){
                topic.printAllVariables();
                System.out.println();
            }
        }
        else {
            System.out.println("No Pending Events Available at this moment");
        }
    }

    public void startService() {
        try {
            ss = new ServerSocket(listeningPort);
            SubscriberAgent pub = new SubscriberAgent(ss);
            pub.start();

        } catch (IOException ex) {
            ex.printStackTrace();
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
                    // Topic newTopic = (Topic) in.readObject();
                    Topic newTopic = (Topic) in.readObject();
                    topicList.add(newTopic);
                    System.out.println("Received new topic named: " +
                            newTopic.getName());
                    displayTopicWithNumbers();
                }
                // 1000 -> Receiving topics list and pending events if any.
                else if (code == 1000) {
                    topicList = (Vector<Topic>)in.readObject();
                    System.out.println("Received topics list: ");
                    printTopicVectors(topicList);
                    Vector<Event> pendingEvents = (Vector<Event>) in.readObject();
                    System.out.println("Received pending events: ");
                    printEventVectors(pendingEvents);
                }
                // 4-> Receiving subscribed topic list
                else if (code == 4){
                    // System.out.println("code" + 4);
                    Vector<String> subscribedTopicsList =
                            (Vector<String>)in.readObject();
                    if (subscribedTopicsList.size() == 0){
                        System.out.println("No Topics Subscribed");
                    }
                    else {
                        System.out.println("The Subscribed Topic List is:");
                        this.printList(subscribedTopicsList);
                    }


                }
                // 3-> Receiving Events
                else if (code == 3){
                    Event newEvent = (Event) in.readObject();
                    System.out.println("Received New Event: "
                            + newEvent.getTitle());
                    newEvent.printAllVariables();
                }
            } catch (Exception e) {
                System.out.println("Error Being Handled");
                e.printStackTrace();
            }
        }

    }
    /*Connect to Event Manager and specify topic for subscription*/
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
                    ":" + this.listeningPort);
            out.writeObject(topic);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    /*
    * The assumption here is that Subscriber will always have list of active
    * topics
    * This function uses the same numeric code as subscribe i.e. 2
    * */
    public void subscribe(String keyword) {
        for(Topic consideration: topicList){
            if (consideration.checkIfKeyWordExists(keyword)){
                this.subscribe(consideration);
                break;
            }
        }
    }

    /*Unsubscribe with topic*/

    @Override
    public void unsubscribe(Topic topic) {
        // System.out.println("unsubscribe:topic");
        Socket clientSocket = null;
        try {
            System.out.println("Unsubscribing from topic: " + topic.getName());
            clientSocket = new Socket("localhost", 4444);
            ObjectOutputStream out = new ObjectOutputStream(clientSocket
                    .getOutputStream());
            // 6 -> unsubscribe a topic
            out.writeInt(6);
            out.writeUTF(Inet4Address.getLocalHost().getHostAddress() +
                    ":" + this.listeningPort);
            out.writeObject(topic);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*Unsubscribe from all topics*/

    @Override
    public void unsubscribe() {
        // System.out.println("unsubscribe");
        Socket clientSocket = null;
        try {
            System.out.println("Unsubscribing from all topics ");
            clientSocket = new Socket(sendingAddress, sendingPort);
            ObjectOutputStream out = new ObjectOutputStream(clientSocket
                    .getOutputStream());
            // 5 -> Unsubscribe All
            out.writeInt(5);
            out.writeUTF(Inet4Address.getLocalHost().getHostAddress() +
                    ":" + this.listeningPort);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Vector<String> getSubscribedTopics() {
        Vector<String> subscribedTopicsList = null;
        Socket clientSocket = null;
        try {
            System.out.println("Getting Subscribed Topics ");
            clientSocket = new Socket(sendingAddress, sendingPort);
            ObjectOutputStream out = new ObjectOutputStream(clientSocket
                    .getOutputStream());
            // 4 -> supply subscribed topic list for Subscriber
            out.writeInt(4);
            out.writeUTF(Inet4Address.getLocalHost().getHostAddress() +
                    ":" + this.listeningPort);
            out.flush();

            ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
            subscribedTopicsList = (Vector<String>) in.readObject();
        }
        catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return subscribedTopicsList;
    }

    @Override
    /*
    * This will access the copy out of subscribersInfo
    * */
    public void listSubscribedTopics() {
        // System.out.println("listSubscribedTopics");
        Vector<String> subscribedTopicsList = this.getSubscribedTopics();

        try {
            if(subscribedTopicsList == null){
                System.out.println("Subscriber Removed from Server");
                System.out.println("Pinging to Register...");
                this.ping();
                System.out.println("No Topics Subscribed");
            }
            else if (subscribedTopicsList.size() == 0){
                System.out.println("No Topics Subscribed");
            }
            else {
                System.out.println("Received Subscribed Topic List");
                this.printList(subscribedTopicsList);
            }
        }
        catch (Exception e){
            e.printStackTrace();
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
            out.writeInt(1000);
            out.writeUTF(Inet4Address.getLocalHost().getHostAddress() +
                    ":" + this.listeningPort);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*Simple Function to print a List of Strings*/

    private void printList(Vector<String> contentList){
        // System.out.println("printList");
        int i = 0;
        for(String content : contentList) {
            System.out.println("\t" + i + ":" + content);
            i++;
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
                System.out.println("\t" + i + ":" + topicList.get(i).getName());
            }
        }
    }

    /*Display KeyWords for every topic*/
    private void displayKeyWordsForTopics (){
        for (Topic consideration: topicList)
        {
            consideration.printKeyWordsWithTopicName();
        }
    }

    public static void main(String[] args) throws InterruptedException {

        SubscriberAgent subUI = new SubscriberAgent();

        if (args.length==8 && args[0].equals("-port") &&
                args[2].equals("-threads") &&
                args[4].equals("-eventManagerIP") &&
                args[6].equals("-eventManagerPort")) {
            try {
                System.out.println("Setting Connection Variables:");
                subUI.setAddresses(Integer.parseInt(args[1]),
                        Integer.parseInt(args[3]), args[5],
                        Integer.parseInt(args[7]));
                subUI.printAddresses();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else {
            System.err.println("Enter Command Like this: " +
                    "SubscriberAgent -port 11001 -threads 2 " +
                    "-eventManagerIP localhost -eventManagerPort 4444");
            System.out.println("Using Default Values");
            subUI.setAddresses(11001, 2,
                    "localhost", 4444);
            subUI.printAddresses();
        }

        subUI.startService();
        Scanner sc = new Scanner(System.in);
        int userInput = 0;
        boolean loopStatus = true;
        sleep(2500);
        try {
            subUI.ping();
            while (loopStatus) {
                sleep(900);
                System.out.println("Press 1 to Subscribe to New Topic");
                System.out.println("Press 2 to Subscribe to New Topic using " +
                        "Keyword");
                System.out.println("Press 3 to view subscribed topics");
                System.out.println("Press 4 to unsubscribe all topics");
                System.out.println("Press 5 to unsubscribe by topics");
                System.out.println("Press 6 to Ping");
                System.out.println("Press 7 to view topics");
                System.out.println("Press 8 to Exit the loop");
                System.out.println("Please select an option: ");

                userInput = sc.nextInt();

                switch (userInput) {
                    case 1:
                        System.out.println("Select Topic Id from list below:");
                        subUI.displayTopicWithNumbers();
                        int topicId0 = sc.nextInt();
                        subUI.subscribe(topicList.get(topicId0));
                        break;
                    case 2:
                        System.out.println("Write a keyword from list displayed " +
                                "below");
                        subUI.displayKeyWordsForTopics();
                        String keyWordValue = sc.next();
                        subUI.subscribe(keyWordValue);
                        break;
                    case 3:
                        subUI.listSubscribedTopics();
                        break;
                    case 4:
                        subUI.unsubscribe();
                        break;
                    case 5:
                        System.out.println("Select Topic Id for The Event");
                        Vector<String> subscribedTopics = subUI.getSubscribedTopics();
                        subUI.printList(subscribedTopics);
                        Topic topic = new Topic(subscribedTopics.get(sc.nextInt()));
                        subUI.unsubscribe(topic);
                        break;
                    case 6:
                        subUI.ping();
                    case 7:
                        subUI.displayTopicWithNumbers();
                        break;
                    case 8:
                        loopStatus = false;
                        break;
                    default:
                        System.out.println("Please enter a correct value");
                        break;
                }// Switch End
            }// While End
            sc.close();
            System.out.println("Subscriber Terminated");
            System.exit(0);
        }// Try End

        catch (Exception e){
            e.printStackTrace();
        }
    }
}
