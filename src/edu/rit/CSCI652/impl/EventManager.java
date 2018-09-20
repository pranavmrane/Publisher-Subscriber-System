package edu.rit.CSCI652.impl;


import com.sun.org.apache.xpath.internal.SourceTree;
import edu.rit.CSCI652.demo.Event;
import edu.rit.CSCI652.demo.Subscriber;
import edu.rit.CSCI652.demo.Topic;

import java.io.*;
import java.lang.reflect.Array;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.StandardSocketOptions;
import java.util.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class EventManager extends Thread {

    /*
     * Start the repo service
     */
    private ServerSocket ss = null;
    static int numberOfThreads = 4;

    // {topic1={subscriber1:position, subscriber2: position}, topic2 = {}}
    // subscriber1 is IP:port
    static private ConcurrentHashMap<String, Vector<String>> topicsInfo = new
            ConcurrentHashMap<>();

    // [Publisher1(IP:port), Publisher2(IP:port)]
    static private Vector<String> publishersInfo = new Vector<>();

    // {subscriber1 = [topic1], subscriber2 = [topic1]}
    static private ConcurrentHashMap<String, Vector<String>> subscribersInfo =
            new ConcurrentHashMap<>();

    // {topic1 = [event1, event2, event3], topic2 = [event11, event12, event13]]
    static private ConcurrentHashMap<String, Vector<Event>> topicQueue = new
            ConcurrentHashMap<>();

    static private ConcurrentHashMap<String, Vector<Event>> pendingEvents =
            new ConcurrentHashMap<>();

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
    private final Lock eventIndexLock = new ReentrantLock();

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
            ex.printStackTrace();
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
                    // 4 -> supply subscribed topic list for Subscriber
                    else if (code == 4) {
                        // System.out.println("code" + 4);
                        String subscriberId = in.readUTF();
                        // String inet = s.getInetAddress().getHostAddress();
                        Vector<String> subscribedTopicsList =
                                subscribersInfo.get(subscriberId);
                        System.out.println(subscribedTopicsList);
                        ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
                        out.writeObject(subscribedTopicsList);
                        out.flush();
                    }
                    // 5 -> unsubscribe everything
                    else if (code == 5) {
                        // System.out.println("code" + 5);
                        String subscriberId = in.readUTF();
                        // TODO: Decipher future impact of removal on topicInfo
                        // Presently no action taken with position of subscriber
                        subscribersInfo.put(subscriberId,
                                new Vector<String>());
                    }
                    // 6 -> unsubscribe a specific topic
                    else if (code == 6) {
                        System.out.println("code" + 6);
                        String subscriberId = in.readUTF();
                        // String inet = s.getInetAddress().getHostAddress();
                        Topic topic = (Topic) in.readObject();
                        // TODO: Decipher future impact of removal on topicInfo
                        // Presently no action taken with position of subscriber
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
     * Questions: What to do with topic queue?
     */
    private void notifySubscribers(Event event) {
        // Find index for event
        String topicName = event.getTopicName();
        eventIndexLock.lock();
        int index = eventIndex.get(topicName);
        event.setId(index++);
        eventIndex.put(topicName, index);
        eventIndexLock.unlock();
        System.out.println(index);
        System.out.println(eventIndex);
        String destination;
        int port;
        Socket sendSocket = null;
        Vector<Event> tmpPendingEvents;
        for (String s : topicsInfo.get(topicName)) {
            try {
                if (offlineSubscribers.contains(s)) {
                    tmpPendingEvents = pendingEvents.get(s);
                    tmpPendingEvents.add(event);
                    pendingEvents.put(s, tmpPendingEvents);
                }
                else {
                    destination = s.split(":")[0];
                    port = Integer.parseInt(s.split(":")[1]);
                    sendSocket = new Socket(destination, port);
                    ObjectOutputStream out = new ObjectOutputStream
                            (sendSocket.getOutputStream());
                    // 3 -> Advertising new topic
                    out.writeInt(3);
                    out.writeObject(event);
                }
            } catch (IOException e) {
                // Subscriber is offline. Handle it!
                e.printStackTrace();
                offlineSubscribers.add(s);
                tmpPendingEvents = pendingEvents.get(s);
                tmpPendingEvents.add(event);
                pendingEvents.put(s, tmpPendingEvents);
            }
        }
    }

    /*
     * Send Back Subscription List Back to Subscriber
     */
    private void transmitSubscribedTopics(String recipient,
                                          ArrayList<String> SubsTopicList) {
        // System.out.println("transmitSubscribedTopics");
        int port = 0;
        Socket sendSocket;
        String destination = "";
        try {
            destination = recipient.split(":")[0];
            port = Integer.parseInt(recipient.split(":")[1]);
            sendSocket = new Socket(destination, port);
            ObjectOutputStream out = new ObjectOutputStream
                    (sendSocket.getOutputStream());
            // 4 -> supply subscribed topic list for Subscriber
            out.writeObject(SubsTopicList);
            out.close();
        } catch (IOException e) {
            // Subscriber is not expected to be offline
            // Not sure if Subscriber can collapse just after sending request
            e.printStackTrace();
        }
     }

    /*
     * Provide Publisher with Topics list
     * Might be offline or new Publisher
     */
    private void pingPublisher(String publisherId) {
        System.out.println("Publisher " + publisherId + " came online. " +
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
            e.printStackTrace();
        }
    }

    /*
     * Provide Subscriber with Topics list
     * Might be offline or new Subscriber
     * Send pending events
     */
    private void pingSubscriber(String subscriberId) {
        System.out.println("Subscriber " + subscriberId + " came online. " +
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

            // TODO: Send pending events when this subscriber was offline
            out.writeObject(pendingEvents.get(subscriberId));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }


    /*
     * check if new publisher is sending a request
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
     * check if new subscriber is sending a request
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

        System.out.println("<->: " + topicList);
        System.out.println("<-1->: " + topicName);

        System.out.println("unsubscribeTopic");
        if (!subscribersInfo.containsKey(subscriberName)) {
            System.out.println("Subscriber Not Present");
            return;
        }

        if (!topicsInfo.containsKey(topicName.getName())) {
            System.out.println("Topic Not Present");

            return;
        }

        Vector<String> subscriptions = subscribersInfo.get(subscriberName);
        subscriptions.remove(topicName.getName());
        subscribersInfo.put(subscriberName, subscriptions);
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
            // Get index for topic
            topicIndexLock.lock();
            topic.setId(topicIndex++);
            topicIndexLock.unlock();

            System.out.println("Adding topic: " + topic.getName());
            this.topicsInfo.put(topic.getName(), new Vector<String>());
            this.topicList.add(topic);
            this.eventIndex.put(topic.getName(), 1);
            System.out.println(topicsInfo);
            // TODO: Broadcast to everyone
            Socket sendSocket;
            String destination;
            int port;
            // The code below works as expected
            for (String s : publishersInfo) {
                try {
                    destination = s.split(":")[0];
                    port = Integer.parseInt(s.split(":")[1]);
                    sendSocket = new Socket(destination, port);
                    ObjectOutputStream out = new ObjectOutputStream
                            (sendSocket.getOutputStream());
                    // 1 -> Advertising new topic
                    out.writeInt(1);
                    out.writeObject(topic);
                    out.flush();
                    out.close();
                } catch (IOException e) {
                    // TODO: Publisher is offline. Handle it!
                    e.printStackTrace();
                }
            }

            // There is a definite issue in the code below
            // Information doesn't get transmitted
            // The most common solution online is adding flush
            for (String s : subscribersInfo.keySet()) {
                try {
                    destination = s.split(":")[0];
                    port = Integer.parseInt(s.split(":")[1]);
                    sendSocket = new Socket(destination, port);
                    ObjectOutputStream out = new ObjectOutputStream
                            (sendSocket.getOutputStream());
                    // 1 -> Advertising new topic
                    out.writeInt(1);
                    out.writeObject(topic);
                    out.flush();
                    out.close();
                } catch (IOException e) {
                    // TODO: Subscriber is offline. Handle it!
                    e.printStackTrace();
                    offlineSubscribers.add(s);
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
            pendingEvents.put(sub, new Vector<Event>());
        }

        Vector<String> topicInfo = topicsInfo.get(topicName);
        if (topicInfo.contains(sub)) {
            System.out.println("Already subscribed.");
        } else {
            topicInfo.add(sub);
            topicsInfo.put(topicName, topicInfo);
        }

        System.out.println("Current subscribers list: " + subscribersInfo);
        System.out.println("Current subscribers per topic: " + topicsInfo);

    }

    /*
     * remove subscriber from the list
     * The original function dMap.Entry<String, Integer> nameEntryefinition did not contain parameter:droppedSubscriber
     * In the original function{removeSubscriber()}, It wasn't clear which
     * subscriber is to be removed
     */
    private void removeSubscriber(String droppedSubscriber) {
        // System.out.println("removeSubscriber");
        subscribersInfo.remove(droppedSubscriber);
        topicsInfo.remove(droppedSubscriber);
        System.out.println(droppedSubscriber + " is removed");
    }

    /*
     * show the list of subscriber for a specified topic
     */
    private void showSubscribers(Topic topic) {
        System.out.println("showSubscribers");
        // Hashmap will contain Subscribers and Positions
        // We need only Subscriber names
        Vector<String> subscribersForTopic =
                topicsInfo.get(topic.getName());

        for (String key : subscribersForTopic) {
            System.out.println(key);
        }
    }

    /*Displays all the topics ever registered with event manager*/
    private void displayTopicWithNumbers (){
        if(topicList.size() == 0) System.out.println("No Topics Present");
        for (int i=0; i<topicList.size(); i++){
            System.out.println(i + ":" + topicList.get(i).getName());
        }
    }


    public static void main(String[] args) {
        EventManager eventManager = new EventManager();
        eventManager.startService();

        Scanner sc = new Scanner(System.in);
        int userInput = 0;
        boolean loopStatus = true;

        try {
            while (loopStatus) {
                System.out.println("Press 1 to remove Subscribers");
                System.out.println("Press 2 to Show Subscriber for a topic");

                System.out.println("Press 3 to view topics");
                System.out.println("Press 4 to Exit the loop");
                System.out.print("Please select an option: ");

                userInput = sc.nextInt();

                switch (userInput) {
                    case 1:
                        System.out.println("Type Subscriber to be Removed, " +
                                "No Spaces Allowed");
                        String subscriberName = sc.next();
                        eventManager.removeSubscriber(subscriberName);
                        break;
                    case 2:
                        System.out.println("Select Topic Id for List");
                        eventManager.displayTopicWithNumbers();
                        int topicId = sc.nextInt();
                        eventManager.showSubscribers(topicList.get(topicId));
                        break;
                    case 3:
                        eventManager.displayTopicWithNumbers();
                        break;
                    case 4:
                        loopStatus = false;
                        break;
                }// End of Switch
            }// End Of While
            sc.close();
            System.out.println("Event Manager Terminated");
            System.exit(0);
        }// End of Try
        catch (Exception e){

        }




    }
}
