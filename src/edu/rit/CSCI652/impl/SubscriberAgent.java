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
import java.util.Vector;
import java.util.Scanner;

public class SubscriberAgent extends Thread implements Subscriber {

    // Server parameters
    private ServerSocket ss = null;
    static int numberOfThreads = 0;
    private int listeningPort = 0;
    private String listeningAddress = "0.0.0.0";
    private String sendingAddress = "";
    private int sendingPort = 0;


    static private Vector<Topic> topicList = new Vector<Topic>();

    public SubscriberAgent() {

    }

    public SubscriberAgent(ServerSocket ss) {
        this.ss = ss;
    }

    // Sets server parameters.
    public void setAddresses(int listeningPort, int threadCount,
                             String sendingAddress, int sendingPort){

        this.listeningPort = listeningPort;
        numberOfThreads = threadCount;
        this.sendingAddress = sendingAddress;
        this.sendingPort = sendingPort;
    }

    // Prints server parameters.
    public void printAddresses(){
        System.out.println("Listening Port: " + listeningPort);
        System.out.println("Thread Count: " + numberOfThreads);
        System.out.println("Sending Address: " + sendingAddress);
        System.out.println("Sending Port: " + sendingPort);
    }

    /*
     * Prints topic information.
     */
    public void printTopicVectors(Vector<Topic> list){

        if(list.size() > 0){
            for(Topic topic: list){
                topic.printAllVariables();
            }
        }
        else {
            System.out.println("No Topic Available.");
        }
    }

    /*
     * Prints events information
     */
    public void printEventVectors(Vector<Event> list){
        if (list == null) {
            System.out.println("No Pending Events Available at this moment.");
        }
        else if (list.size() == 0) {
            System.out.println("No Pending Events Available at this moment.");
        }
        else {
            for(Event topic: list){
                topic.printAllVariables();
            }
        }
    }

    /*
     * Starts new thread to listen for communication from Event Manager.
     * Main thread runs the UI.
     * */
    public void startService() {
        try {
            ss = new ServerSocket(listeningPort, 100, Inet4Address.getByName
                    (listeningAddress));
            SubscriberAgent pub = new SubscriberAgent(ss);
            pub.start();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /*
     * Listening for connections from EventManager.
     */
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
                e.printStackTrace();
            }
        }
    }

    /*
     * Connect to Event Manager and subscribe to topic
     */
    @Override
    public void subscribe(Topic topic) {
        Socket clientSocket = null;
        try {
            System.out.println("Subscribing to topic: " + topic.getName());
            clientSocket = new Socket(sendingAddress, 4444);
            ObjectOutputStream out = new ObjectOutputStream(clientSocket
                    .getOutputStream());
            // 2 -> subscribe to topic
            out.writeInt(2);
            out.writeUTF(Inet4Address.getLocalHost().getHostAddress() +
                    ":" + this.listeningPort);
            out.writeObject(topic);
            out.flush();
        } catch (IOException e) {
            System.out.println("EventManager is down!");
        }

    }

    /*
     * Iterate through every keyword for every topic
     * If keyword matches with request, subscribe to that topic
     */
    public void subscribe(String keyword) {
        boolean foundStatus = false;
        for(Topic consideration: topicList){
            if (consideration.checkIfKeyWordExists(keyword)){
                foundStatus = true;
                this.subscribe(consideration);
            }
        }
        if(!foundStatus){
            System.out.println("Value entered NOT found, stick to original list");
        }
    }

    /* Unsubscribe with topic */
    @Override
    public void unsubscribe(Topic topic) {
        Socket clientSocket = null;
        try {
            System.out.println("Unsubscribing from topic: " + topic.getName());
            clientSocket = new Socket(sendingAddress, 4444);
            ObjectOutputStream out = new ObjectOutputStream(clientSocket
                    .getOutputStream());
            // 6 -> unsubscribe a topic
            out.writeInt(6);
            out.writeUTF(Inet4Address.getLocalHost().getHostAddress() +
                    ":" + this.listeningPort);
            out.writeObject(topic);
            out.flush();
        } catch (IOException e) {
            System.out.println("EventManager is down!");
        }
    }

    /* Unsubscribe from all topics */
    @Override
    public void unsubscribe() {
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
            System.out.println("EventManager is down!");
        }
    }

    /*
     * The implementation is different for getting subscribed Topics.
     * We are asking for and receiving data on the same port
     */
    public Vector<String> getSubscribedTopics() {
        Vector<String> subscribedTopicsList = null;
        Socket clientSocket = null;
        try {
            System.out.println("Getting Subscribed Topics...");
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
            System.out.println("EventManager is down!");
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
            System.out.println("EventManager is down!");
        }
    }

    /*
     * Ping EventManager and fetch latest information.
     */
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
            System.out.println("EventManager is down!");
        }
    }

    /* Simple Function to print a List of Strings */
    private void printList(Vector<String> contentList){
        int i = 0;
        for(String content : contentList) {
            System.out.println("" + i + ":" + content);
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
                System.out.println("" + i + ":" + topicList.get(i).getName());
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
            System.out.println("Setting Connection Variables:");
            subUI.setAddresses(Integer.parseInt(args[1]),
                    Integer.parseInt(args[3]), args[5],
                    Integer.parseInt(args[7]));
            subUI.printAddresses();
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

        subUI.ping();
        while (loopStatus) {
            try {
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
                    // Subsribe to new topic
                    case 1:
                        System.out.println("Select Topic Id from list below:");
                        if(topicList.size()>0){
                            subUI.displayTopicWithNumbers();
                            int topicId0 = sc.nextInt();
                            try {
                                subUI.subscribe(topicList.get(topicId0));
                            }
                            catch (ArrayIndexOutOfBoundsException e){
                                System.out.println("Please enter a value in range");
                            }
                        }
                        else {
                            System.out.println("No Topics Can be Selected Now");
                        }
                        break;
                    // Subscribe to new topic with keyword
                    case 2:
                        System.out.println("Write a keyword from list displayed " +
                                "below");
                        if(topicList.size()>0){
                            subUI.displayKeyWordsForTopics();
                            String keyWordValue = sc.next();
                            subUI.subscribe(keyWordValue);
                        }
                        else {
                            System.out.println("No KeyWords Can be Selected Now");
                        }
                        break;
                    // Fetch list of subscribed topics
                    case 3:
                        subUI.listSubscribedTopics();
                        break;
                    // Unsubscribe all topics
                    case 4:
                        subUI.unsubscribe();
                        break;
                    // Unsubscribe from given topic
                    case 5:
                        System.out.println("Select Topic Id for The Event");
                        Vector<String> subscribedTopics = subUI.getSubscribedTopics();
                        subUI.printList(subscribedTopics);
                        Topic topic = new Topic(subscribedTopics.get(sc.nextInt()));
                        subUI.unsubscribe(topic);
                        break;
                    // Ping event manager
                    case 6:
                        subUI.ping();
                    // Display list of topics
                    case 7:
                        subUI.displayTopicWithNumbers();
                        break;
                    // Exit
                    case 8:
                        loopStatus = false;
                        break;
                    default:
                        System.out.println("Please enter a correct value");
                        break;
                }
            }catch (Exception e){
                System.out.println("Invalid Input");
                sc.nextLine();
            }
        }
        sc.close();
        System.out.println("Subscriber Terminated");
        System.exit(0);
    }
}
