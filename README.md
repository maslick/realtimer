# =realtimer=

**Realtimer** is comprised of 2 modules:
 * *Tracker service*
 * *CLI client*
 
![Realtimer architecture](realtimer.png)
 
The *Tracker* module is a non-blocking REST API one can call to publish events (via GET).

The *CLI client* works as subscriber to an event bus. It is using RxJava, generating a stream of Events (can be filtered, transformed, etc).

**Realtimer** leverages Vert.x [1] - a distributed event bus backed by a simple concurrency model.
In the simplest scenario one can have one *Tracker service* instance and multiple websocket clients. Clients are fault-tolerant, meaning they operate regardless of whether the *Tracker service* is running or not (clients reconnect automatically 5 sec after the connection is down).

**Realtimer** is scalable. It can be containerized (Docker) and scaled up in a Kubernetes cluster. Multiple *Tracker* instances (running on different nodes) share the exact-same event bus. This is achieved by using a cluster manager (e.g. Hazelcast, Apache Ignite, etc).

*Tracker* instances can be put behind a load-balancer (provided by k8s), forming a distributed, fault-tolerant and highly available system. Ideally, ``HttpServerVert`` and ``WebsocketVert`` verticles would be deployed to different containers and can be scaled independently.

## TO-DO list

* Add Mongo as persistence layer (implement the ``Repo`` interface)
* Put ``HttpServerVert`` and ``WebsocketVert`` into different modules (jars, Docker containers)
* Use a cluster manager (Hazelcast, Apache Ignite, Zookeeper, Infinispan)
* Add Dockerfile, k8s configuration yaml (service: ``LoadBalancer``)
* Test on minikube [2] (locally)
* Deploy to Google Kubernetes Engine (use gcr.io [3] as container registry)

## Installation

```
$ git clone https://github.com/maslick/realtimer.git
$ ./gradlew clean build
```

## Usage
#### 1. Tracker

Start the server (tracker):
```
$ java -jar tracker/build/libs/realtimer.jar
```

To fire a single GET request run:
```
$ curl http://localhost:8080/testUserId?data=testData
```

Performance test (using vegeta [4]):
```
$ echo "GET http://localhost:8080/testUserId?data=testData" | vegeta attack -duration=15s -rate=500 | vegeta report
```

#### 2. CLI client

```
$ java -DuserId=testUserId -jar ws-client/build/libs/realtimer-ws.jar
$ java -DuserId=testUserId -Daddress=ws://localhost:8081 -jar ws-client/build/libs/realtimer-ws.jar
```

#### 3. Web browser client (web-socket)

```
$ open ws-client/html5client.html
```


[1] https://en.wikipedia.org/wiki/Vert.x

[2] https://github.com/kubernetes/minikube

[3] http://gcr.io

[4] https://github.com/tsenart/vegeta
