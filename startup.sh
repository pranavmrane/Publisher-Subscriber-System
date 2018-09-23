docker run --rm --net mynet123 --ip 172.18.0.2 -i -v $PWD:/app -w /app demo/pubsub java -cp "/app/bin" edu.rit.CSCI652.impl.EventManager -port 4444 -threads 4 -eventManagerIP 0.0.0.0 
docker run --rm --net mynet123 -i -v $PWD:/app -w /app demo/pubsub java -cp "/app/bin" edu.rit.CSCI652.impl.PublisherAgent -port 10000 -threads 2 -eventManagerIP 172.18.0.2 -eventManagerPort 4444
docker run --rm --net mynet123 -i -v $PWD:/app -w /app demo/pubsub java -cp "/app/bin" edu.rit.CSCI652.impl.SubscriberAgent -port 11000 -threads 2 -eventManagerIP 172.18.0.2 -eventManagerPort 4444

