# PubSub

A Publisher Subscriber application with a centralized multi-threaded Event Manager. The Event Manager handles offline Publishers and Subscribers by queueing the events that are to be sent to the offline entity. 

## Running on Linux command line:
Steps to compile code and run single instance of EventManager, PublisherAgent and SubscriberAgent.
```
1) git clone https://gitlab.com/brat197/pubsub.git
2) cd pubsub
3) javac -cp 'src/' src/edu/rit/CSCI652/demo/*
4) javac -cp 'src/' src/edu/rit/CSCI652/impl/*
5) java -cp 'src/' src.edu.rit.CSCI652.impl.EventManager -port 4444 -threads 4 -eventManagerIP 0.0.0.0
6) java -cp 'src/' src.edu.rit.CSCI652.impl.PublisherAgent -port 10000 -threads 2 -eventManagerIP 127.0.0.1 -eventManagerPort 4444
7) java -cp 'src/' src.edu.rit.CSCI652.impl.SubscriberAgent -port 11000 -threads 2 -eventManagerIP 127.0.0.1 -eventManagerPort 4444
```

## Running the application on Docker:
With a working installation of docker. Create a new network so that we can get control on what IP the Event Manager will listen on.
```
1) cd pubsub
2) docker build -f Dockerfile -t demo/pubsub .
3) docker network create --subnet=172.18.0.0/16 mynet123
4) For EventManager run:
docker run --rm --net mynet123 --ip 172.18.0.2 -v $PWD:/app -w /app demo/pubsub java -cp "/app/src" edu.rit.CSCI652.impl.EventManager -port 4444 -threads 4 -eventManagerIP 0.0.0.0 
5) For each PublisherAgent run:
docker run --rm --net mynet123 -i -v $PWD:/app -w /app demo/pubsub java -cp "/app/bin" edu.rit.CSCI652.impl.PublisherAgent -port 10000 -threads 2 -eventManagerIP 172.18.0.2 -eventManagerPort 4444
6) For each SubscriberAgent run:
docker run --rm --net mynet123 -i -v $PWD:/app -w /app demo/pubsub java -cp "/app/bin" edu.rit.CSCI652.impl.SusbcriberAgent -port 11000 -threads 2 -eventManagerIP 172.18.0.2 -eventManagerPort 4444
```

