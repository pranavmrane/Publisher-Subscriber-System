package edu.rit.CSCI652.impl;

import edu.rit.CSCI652.demo.Event;
import edu.rit.CSCI652.demo.Subscriber;
import edu.rit.CSCI652.demo.Topic;

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
    static int numberOfThreads = 2;
    private int port = 11001;
    static private Vector<Topic> topicList = new Vector<Topic>();

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
                    System.out.println("Received new topic: " +
                            newTopic.getName());
                    displayTopicWithNumbers();
                }
                // 1000 -> Receiving topics list and pending events if any.
                else if (code == 1000) {
                    topicList = (Vector<Topic>)in.readObject();
                    System.out.println("Received topics list: " + topicList);
                    Vector<Event> pendingEvents = (Vector<Event>) in.readObject();
                    System.out.println("Received pending events: " +
                            pendingEvents);
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
                        System.out.println("Received Subscribed Topic List");
                    }
                    this.printList(subscribedTopicsList);

                }
                // 3-> Receiving Events
                else if (code == 3){
                    System.out.println("code" + 3);
                    Event newEvent = (Event) in.readObject();
                    System.out.println("Received New Event: "
                            + newEvent.getTitle());
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
                    ":" + this.port);
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
        System.out.println("unsubscribe:topic");
        Socket clientSocket = null;
        try {
            System.out.println("Unsubscribing topic: " + topic.getName());
            clientSocket = new Socket("localhost", 4444);
            ObjectOutputStream out = new ObjectOutputStream(clientSocket
                    .getOutputStream());
            // 6 -> unsubscribe a topic
            out.writeInt(6);
            out.writeUTF(Inet4Address.getLocalHost().getHostAddress() +
                    ":" + this.port);
            out.writeObject(topic);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*Unsubscribe from all topics*/

    @Override
    public void unsubscribe() {
        System.out.println("unsubscribe");
        Socket clientSocket = null;
        try {
            System.out.println("Unsubscribing all topics ");
            clientSocket = new Socket("localhost", 4444);
            ObjectOutputStream out = new ObjectOutputStream(clientSocket
                    .getOutputStream());
            // 5 -> Unsubscribe All
            out.writeInt(5);
            out.writeUTF(Inet4Address.getLocalHost().getHostAddress() +
                    ":" + this.port);
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
            clientSocket = new Socket("localhost", 4444);
            ObjectOutputStream out = new ObjectOutputStream(clientSocket
                    .getOutputStream());
            // 4 -> supply subscribed topic list for Subscriber
            out.writeInt(4);
            out.writeUTF(Inet4Address.getLocalHost().getHostAddress() +
                    ":" + this.port);
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
        if (subscribedTopicsList.size() == 0){
            System.out.println("No Topics Subscribed");
        }
        else {
            System.out.println("Received Subscribed Topic List");
        }
        this.printList(subscribedTopicsList);

    }

    /*Connect and register network*/

    public void ping() {
        Socket clientSocket = null;
        try {
            System.out.println("Pinging.");
            clientSocket = new Socket("localhost", 4444);
            ObjectOutputStream out = new ObjectOutputStream(clientSocket
                    .getOutputStream());
            out.writeInt(1000);
            out.writeUTF(Inet4Address.getLocalHost().getHostAddress() +
                    ":" + this.port);
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
            System.out.println("" + i + ":" + content);
            i++;
        }
    }

    /*Displays all the topics ever registered with event manager*/
    private void displayTopicWithNumbers (){
        if(topicList.size() == 0) System.out.println("No Topics Present");
        for (int i=0; i<topicList.size(); i++){
            System.out.println(i + ":" + topicList.get(i).getName());
        }
    }

    /*Display KeyWords for every topic*/
    private void displayKeyWordsForTopics (){
        for (Topic consideration: topicList) consideration.printKeyWords();
    }

    public static void main(String[] args) throws InterruptedException {

        SubscriberAgent subUI = new SubscriberAgent();
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
                System.out.print("Please select an option: ");

                userInput = sc.nextInt();

                switch (userInput) {
                    case 1:
                        System.out.println("Select Topic Id from list below:");
                        subUI.displayTopicWithNumbers();
                        int topicId0 = sc.nextInt();
                        subUI.subscribe(topicList.get(topicId0));
                        break;
                    case 2:
                        System.out.println("Select a keyword from list displayed " +
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
