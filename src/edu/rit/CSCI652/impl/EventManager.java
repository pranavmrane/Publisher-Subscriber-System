package edu.rit.CSCI652.impl;


import edu.rit.CSCI652.demo.Event;
import edu.rit.CSCI652.demo.Topic;

import java.io.*;
import java.net.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class EventManager extends Thread {

    // Server setup attributes
    private ServerSocket ss = null;
    static int numberOfThreads = 0;
    private int listeningPort = 0;
    private String listeningAddress = "";

    // {topic1={subscriber1, subscriber2}, topic2 = {}}
    // subscriber1 is IP:port
    static private ConcurrentHashMap<String, Vector<String>> topicsInfo = new
            ConcurrentHashMap<>();

    // [Publisher1(IP:port), Publisher2(IP:port)]
    static private Vector<String> publishersInfo = new Vector<>();

    // {subscriber1 = [topic1], subscriber2 = [topic1]}
    static private ConcurrentHashMap<String, Vector<String>> subscribersInfo =
            new ConcurrentHashMap<>();

    // {SubscriberName = {EventName, Time}}
    static public ConcurrentHashMap<String, ConcurrentHashMap<Event,
            LocalDateTime>> pendingEvents = new ConcurrentHashMap<>();

    // [topic1, topic2];
    static private Vector<Topic> topicList = new Vector<>();

    // Maintain a list of offline subscribers
    static private Vector<String> offlineSubscribers = new Vector<>();

    // Use to assign id's to topics.
    // Obtain topicIndexLock before using it
    static private int topicIndex = 1;
    private final Lock topicIndexLock = new ReentrantLock();

    // Use to assign id's to events.
    // Obtain eventIndexLock before using this.
    static private ConcurrentHashMap<String, Integer> eventIndex = new
            ConcurrentHashMap<>();

    // TTL for event expiry.
    static private int ttl = 2;

    public EventManager() {

    }

    public EventManager(ServerSocket ss) {
        this.ss = ss;
    }

    /*
     * Set server attributes from Command line.
     */
    public void setAddresses(int listeningPort, int threadCount,
                             String listeningAddress){

        this.listeningPort = listeningPort;
        numberOfThreads = threadCount;
        this.listeningAddress = listeningAddress;
    }

    /*
     * Print server attributes.
     */
    public void printAddresses(){
        System.out.println("Listening Port: " + listeningPort);
        System.out.println("Thread Count: " + numberOfThreads);
        System.out.println("Sending Address: " + listeningAddress);
    }

    /*
     * Start threads to accept connections on the given IP and port.
     */
    private void startService() {
        try {
            ss = new ServerSocket(listeningPort, 100, Inet4Address.getByName
                    (listeningAddress));
            for (int i = 0; i < numberOfThreads; i++) {
                EventManager em = new EventManager(ss);
                em.start();
            }
            // Start a thread for event expiry after TTL minutes.
            (new GarbageCollector(ttl)).start();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /*
     * Infinite loop for accepting connections and serving requests from
     * Publishers and Subscribers.
     */
    public void run() {
        while (true) {
            try {
                Socket s = ss.accept();
                ObjectInputStream in = new ObjectInputStream(s.getInputStream());
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
                        Topic topic = (Topic) in.readObject();
                        this.addSubscriber(subscriberId, topic.getName());
                    }
                    // 3 -> new event published, notify subscribers
                    else if (code == 3) {
                        Event newEvent = (Event) in.readObject();
                        this.notifySubscribers(newEvent);
                    }
                    // 4 -> supply subscribed topic list for Subscriber
                    else if (code == 4) {
                        String subscriberId = in.readUTF();
                        Vector<String> subscribedTopicsList = subscribersInfo.get(subscriberId);
                        ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
                        out.writeObject(subscribedTopicsList);
                        out.flush();
                    }
                    // 5 -> unsubscribe everything
                    else if (code == 5) {
                        // System.out.println("code" + 5);
                        String subscriberId = in.readUTF();
                        subscribersInfo.put(subscriberId,
                                new Vector<String>());
                        for(String topicName: topicsInfo.keySet()){
                            Vector<String> subscriberNamesForTopic =
                                    topicsInfo.get(topicName);
                            subscriberNamesForTopic.remove(subscriberId);
                            topicsInfo.put(topicName, subscriberNamesForTopic);
                        }
                    }
                    // 6 -> unsubscribe a specific topic
                    else if (code == 6) {
                        // System.out.println("code" + 6);
                        String subscriberId = in.readUTF();
                        // String inet = s.getInetAddress().getHostAddress();
                        Topic topic = (Topic) in.readObject();
                        this.unsubscribeTopic(subscriberId, topic);
                    }
                    // 999 -> Publisher came online or New Publisher
                    else if (code == 999) {
                        String publisherId = in.readUTF();
                        validatePublisher(publisherId);
                        pingPublisher(publisherId);
                    }
                    // 1000 -> Subscriber came online or New Subscriber
                    else if (code == 1000) {
                        String subscriberId = in.readUTF();
                        validateSubscriber(subscriberId);
                        pingSubscriber(subscriberId);
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /*
     * notify all subscribers of new event
     * Offline Subscribers will also be dealt here
     */
    private void notifySubscribers(Event event) {
        // Find index for event
        System.out.println("Received new event: " + event.getTitle());
        String topicName = event.getTopicName();
        int index = eventIndex.get(topicName);
        event.setId(index++);
        eventIndex.put(topicName, index);

        String destination;
        int port;
        Socket sendSocket = null;
        ConcurrentHashMap<Event, LocalDateTime> tmpPendingEvents = new ConcurrentHashMap<Event, LocalDateTime>();
        for (String s : topicsInfo.get(topicName)) {
            try {
                // Subscriber is already known to be offline.
                if (offlineSubscribers.contains(s)) {
                    LocalDateTime currentTime = null;
                    tmpPendingEvents = pendingEvents.get(s);
                    currentTime = LocalDateTime.now();
                    tmpPendingEvents.put(event, currentTime);
                    pendingEvents.put(s, tmpPendingEvents);
                }
                else {
                    // Subscriber should be online, try notifying.
                    destination = s.split(":")[0];
                    port = Integer.parseInt(s.split(":")[1]);
                    sendSocket = new Socket(destination, port);
                    ObjectOutputStream out = new ObjectOutputStream
                            (sendSocket.getOutputStream());
                    // 3 -> Advertising new event
                    out.writeInt(3);
                    out.writeObject(event);
                }
            } catch (IOException e) {
                // Subscriber is offline. Handle it!
                LocalDateTime currentTime  = null;
                offlineSubscribers.add(s);
                // Subscriber is already known to be offline.
                if(pendingEvents.contains(s)){
                    tmpPendingEvents = pendingEvents.get(s);
                    currentTime = LocalDateTime.now();
                    tmpPendingEvents.put(event, currentTime);
                    pendingEvents.put(s, tmpPendingEvents);
                }
                else{
                    // Dealing with newly discovered offline subscriber
                    System.out.println("Subscriber " + s + " is offline.");
                    ConcurrentHashMap<Event, LocalDateTime> dataForNewSubs = new ConcurrentHashMap<Event, LocalDateTime>();
                    currentTime = LocalDateTime.now();
                    dataForNewSubs.put(event, currentTime);
                    pendingEvents.put(s, dataForNewSubs);
                }

            }
        }
    }

    /*
     * Provide Publisher with Topics list
     * Might be offline or new Publisher
     */
    private void pingPublisher(String publisherId) {
        System.out.println("Publisher " + publisherId + " is Pinging. " +
                "Sending" +
                " list of topics.");
        String destination = publisherId.split(":")[0];
        int port = Integer.parseInt(publisherId.split(":")[1]);
        try {
            Socket sendSocket = new Socket(destination, port);
            ObjectOutputStream out = new ObjectOutputStream(sendSocket
                    .getOutputStream());
            // 999 -> Sending Publisher list of topics
            out.writeInt(999);
            out.writeObject(topicList);
            out.close();
        } catch (IOException e) {
            System.out.println("Publisher " + publisherId + " is offline.");
        }
    }

    /*
     * Provide Subscriber with Topics list
     * Might be offline or new Subscriber
     * Send pending events
     */
    private void pingSubscriber(String subscriberId) {
        System.out.println("Subscriber " + subscriberId + " is Pinging. " +
                "Sending list of topics.");
        String destination = subscriberId.split(":")[0];
        int port = Integer.parseInt(subscriberId.split(":")[1]);
        try {
            Socket sendSocket = new Socket(destination, port);
            ObjectOutputStream out = new ObjectOutputStream(sendSocket
                    .getOutputStream());
            // Sending subscriber list of topics and any pending events
            out.writeInt(1000);
            out.writeObject(topicList);
            ConcurrentHashMap <Event, LocalDateTime> tmpPendingEvents =
                    pendingEvents.get(subscriberId);

            Vector<Event> eventList = null;
            if(tmpPendingEvents!=null){
                // There is definitely something inside pending events
                // Initialise Vector of Events to be passed
                eventList = new Vector<Event>();

                // Save all events with Pending List
                for(Event pendingEvent: tmpPendingEvents.keySet()){
                    eventList.add(pendingEvent);
                }
                out.writeObject(eventList);

                // Remove Subscriber for Pending List
                pendingEvents.remove(subscriberId);
                offlineSubscribers.remove(subscriberId);
            }
            else {
                // Subscriber has no pending data
                out.writeObject(eventList);
            }
        }
        catch (IOException e) {
            System.out.println("Subscriber " + subscriberId + " is offline.");
        }
    }


    /*
     * Check if new publisher is sending a request
     */
    private void validatePublisher(String publisherId) {
        // Just a returning Publisher, return
        if (publishersInfo.contains(publisherId)) {
            return;
        }
        // New publisher, add to list
        else {
            System.out.println("Adding new Publisher: " + publisherId);
            publishersInfo.add(publisherId);
        }
    }

    /*
     * Check if new subscriber is sending a request
     */
    private void validateSubscriber(String subscriberId) {
        // Just a returning subscriber, return
        if (subscribersInfo.containsKey(subscriberId)) {
            offlineSubscribers.remove(subscriberId);
            return;
        } // New subscriber, update data structures
        else {
            System.out.println("Adding new Subscriber: " + subscriberId);
            subscribersInfo.put(subscriberId, new Vector<String>());
        }
    }

    /*
     * Remove particular topic for a certain subscriber
     * */
    private void unsubscribeTopic(String subscriberName, Topic topicName) {
        if (!subscribersInfo.containsKey(subscriberName)) {
            System.out.println("Subscriber Not Present");
            return;
        }
        if (!topicsInfo.containsKey(topicName.getName())) {
            System.out.println("Topic Not Present");
            return;
        }

        // Remove Information from SubscriberInfo
        Vector<String> subscriptions = subscribersInfo.get(subscriberName);
        subscriptions.remove(topicName.getName());
        subscribersInfo.put(subscriberName, subscriptions);

        // Remove Information from topicsInfo
        Vector<String> subscriberNames = topicsInfo.get(topicName.getName());
        subscriberNames.remove(subscriberName);
        topicsInfo.put(topicName.getName(), subscriberNames);
    }

    /*
     * Add new topic when received advertisement of new topic
     */
    private void addTopic(Topic topic) {
        // Check if topic already exists
        if (topicsInfo.containsKey(topic.getName())) {
            System.out.println("Topic already exists");
            return;
        }
        // Topic does not exist, create and notify everyone
        else {
            // Get index for topic
            topicIndexLock.lock();
            topic.setId(topicIndex++);
            topicIndexLock.unlock();

            System.out.println("Adding topic: " + topic.getName());
            this.topicsInfo.put(topic.getName(), new Vector<String>());
            this.topicList.add(topic);
            this.eventIndex.put(topic.getName(), 1);

            Socket sendSocket;
            String destination;
            int port;

            // Send Information to online Publishers
            for (String publisher : publishersInfo) {
                try {
                    destination = publisher.split(":")[0];
                    port = Integer.parseInt(publisher.split(":")[1]);
                    sendSocket = new Socket(destination, port);
                    ObjectOutputStream out = new ObjectOutputStream
                            (sendSocket.getOutputStream());

                    // 1 -> Advertising new topic
                    out.writeInt(1);
                    out.writeObject(topic);
                    out.flush();
                    out.close();
                } catch (IOException e) {
                    // Publisher is offline.
                    System.out.println("Publisher " + publisher + " is offline.");
                }
            }

            // Send Information to online Subscribers
            for (String subscriber : subscribersInfo.keySet()) {
                if (!offlineSubscribers.contains(subscriber)) {
                    try {
                        destination = subscriber.split(":")[0];
                        port = Integer.parseInt(subscriber.split(":")[1]);
                        sendSocket = new Socket(destination, port);
                        ObjectOutputStream out = new ObjectOutputStream
                                (sendSocket.getOutputStream());

                        // 1 -> Advertising new topic
                        out.writeInt(1);
                        out.writeObject(topic);
                        out.flush();
                        out.close();
                    } catch (IOException e) {
                        // Subscriber is offline.
                        System.out.println("Subscriber " + subscriber + " is " +
                                "offline.");
                        if(!pendingEvents.contains(subscriber)){
                            offlineSubscribers.add(subscriber);
                            ConcurrentHashMap<Event, LocalDateTime> dataForNewSubs = new ConcurrentHashMap<Event, LocalDateTime>();
                            pendingEvents.put(subscriber, dataForNewSubs);
                        }
                    }
                }
            }
        }
    }

    /*
     * Add subscriber information.
     * Update relevant subscribersInfo and topicsInfo with relevant information.
     */
    private void addSubscriber(String sub, String topicName) {
        // If subscriber exists, just add topic
        if (subscribersInfo.containsKey(sub)) {
            Vector<String> tmp = subscribersInfo.get(sub);
            if (tmp.contains(topicName)) {
                System.out.println("Already subscribed");
                return;
            } else {
                System.out.println("Subscribing " + sub + " to " + topicName);
                tmp.add(topicName);
                subscribersInfo.put(sub, tmp);
            }
        }
        // Subscriber does not exist, add to list of subscribers
        else {
            System.out.println("Adding new subscriber: " + sub);
            Vector<String> tmp = new Vector<String>();
            tmp.add(topicName);
            subscribersInfo.put(sub, tmp);
            pendingEvents.put(sub, new ConcurrentHashMap<Event, LocalDateTime>());
        }

        Vector<String> topicInfo = topicsInfo.get(topicName);
        if (topicInfo.contains(sub)) {
            System.out.println("Already subscribed.");
        } else {
            topicInfo.add(sub);
            topicsInfo.put(topicName, topicInfo);
        }
    }

    /*
     * Remove subscriber from the system and from all subscriptions.
     */
    private void removeSubscriber(String droppedSubscriber) {
        subscribersInfo.remove(droppedSubscriber);
        for(String topicName: topicsInfo.keySet()){
            Vector<String> subscriberNamesForTopic =
                    topicsInfo.get(topicName);
            subscriberNamesForTopic.remove(droppedSubscriber);
            topicsInfo.put(topicName, subscriberNamesForTopic);
        }
        System.out.println(droppedSubscriber + " is removed");
    }

    /*
     * Show the list of subscriber for a specified topic
     */
    private void showSubscribers(Topic topic) {
        // HashMap will contain Subscribers and Positions
        // We need only Subscriber names
        Vector<String> subscribersForTopic =
                topicsInfo.get(topic.getName());

        if (subscribersForTopic.size() == 0) {
            System.out.println("There are no subscribers for this topic.");
        }
        else {
            for (String key : subscribersForTopic) {
                System.out.println(key);
            }
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
                System.out.println(i + ":" + topicList.get(i).getName());
            }
        }
    }


    public static void main(String[] args) {
        EventManager eventManager = new EventManager();

        if (args.length==6 && args[0].equals("-port") &&
                args[2].equals("-threads") &&
                args[4].equals("-eventManagerIP")) {
            System.out.println("Setting Connection Variables:");
            eventManager.setAddresses(Integer.parseInt(args[1]),
                    Integer.parseInt(args[3]), args[5]);
            eventManager.printAddresses();
        }
        else {
            System.err.println("Enter Command Like this: " +
                    "EventManager -port 4444 -threads 4 " +
                    "-eventManagerIP localhost");
            System.out.println("Using Default Values");
            eventManager.setAddresses(4444, 4, "localhost");
            eventManager.printAddresses();
        }


        eventManager.startService();

        Scanner sc = new Scanner(System.in);
        int userInput = 0;
        boolean loopStatus = true;

        while (loopStatus) {
            try {

                System.out.println("Press 1 to remove Subscribers");
                System.out.println("Press 2 to show Subscriber for a topic");
                System.out.println("Press 3 to view topics");
                System.out.println("Press 4 to view Publishers");
                System.out.println("Press 5 to view Pending Events ");
                System.out.println("Press 6 to Exit the loop");
                System.out.println("Please select an option: ");

                userInput = sc.nextInt();

                switch (userInput) {
                    // Remove subscriber
                    case 1:
                        if(subscribersInfo.size()>0){
                            System.out.println("Select Index of Subscriber to " +
                                    "be Removed, No Spaces Allowed");
                            int index = 0;
                            ArrayList<String> subscriberNames =
                                    new ArrayList<String>();

                            for(String subscriberName: subscribersInfo.keySet()){
                                System.out.println("" + index++ + ":"
                                        + subscriberName);
                                subscriberNames.add(subscriberName);
                            }
                            int subscriberIndex = sc.nextInt();
                            try {
                                eventManager.removeSubscriber(subscriberNames.
                                        get(subscriberIndex));
                            }
                            catch (ArrayIndexOutOfBoundsException e){
                                System.out.println("Please enter a value in range");
                            }
                        }
                        else {
                            System.out.println("No Subscribers Present");
                        }

                        break;
                    // Show subscribers for a topic
                    case 2:
                        System.out.println("Select Topic Id for List");
                        if(topicList.size() == 0) {
                            System.out.println("No Topics Present");
                        }
                        else {
                            eventManager.displayTopicWithNumbers();
                            int topicId = sc.nextInt();
                            try {
                                System.out.println("Subscribers for the topic " +
                                    "are:");
                                eventManager.showSubscribers(topicList.get(topicId));
                            }
                            catch (ArrayIndexOutOfBoundsException e){
                                System.out.println("Please enter a value in range");
                            }
                        }
                        break;
                    // View list of topics
                    case 3:
                        eventManager.displayTopicWithNumbers();
                        break;
                    // View list of publishers
                    case 4:
                        System.out.println("List of publishers: ");
                        System.out.println(publishersInfo);
                        break;
                    // View pending events
                    case 5:
                        System.out.println("Pending Events data dump: ");
                        System.out.println(pendingEvents);
                        break;
                    // Exit
                    case 6:
                        loopStatus = false;
                        break;
                }
            } catch (Exception e){
                System.out.println("Invalid input.");
            }
        }
        sc.close();
        System.out.println("Event Manager Terminated");
        System.exit(0);
    }
}

/*
 * GarbageCollector runs in a minute interval and deletes events that are n
 * minutes old.
 */
class GarbageCollector extends Thread{
    private static int ttl;

    public GarbageCollector() {

    }

    public GarbageCollector(int ttl) {
        this.ttl = ttl;
    }

    public void run(){
        LocalDateTime timeRightNow = null;
        try {
            while (true){
                // Garbage Collector runs every minute.
                sleep(60*1000);
                System.out.println("Garbage Collector Active");
                for(String subscriberName : EventManager.pendingEvents.keySet()){
                    ConcurrentHashMap<Event, LocalDateTime> eventHistory =
                            EventManager.pendingEvents.get(subscriberName);
                    for(Event historicalEvent: eventHistory.keySet()){
                        timeRightNow = LocalDateTime.now();
                        // Remove an event that is more than 5 minutes old
                        if(Duration.between(eventHistory.get(historicalEvent),
                                timeRightNow).toMillis()>(ttl*60*1000)){
                            System.out.println("Removed: " + historicalEvent.getTitle());
                            eventHistory.remove(historicalEvent);
                        }
                    }
                    EventManager.pendingEvents.put(subscriberName, eventHistory);
                }
            }
        }
        catch (InterruptedException e){
            e.printStackTrace();
        }
    }
}
