# =realtimer=

**Realtimer** is comprised of 2 modules:
 * *Tracker*
 * *CLI client*
 
![Realtimer architecture](realtimer.png)
 
The *Tracker* module is a simple REST API you can call (via GET) to publish events.
The *CLI client* works as subscriber to an event bus.
 
**Realtimer** leverages Vert.x [1] - a distributed event bus backed by a simple concurrency model (node.js-like, non-blocking). 
In the simplest scenario one can have one Tracker instance and multiple clients. Clients are fault-tolerant, meaning they operate regardless of whether the Tracker service is running or not (no restart needed). 

**Realtimer** is scalable. It can be containerized (Docker) and scaled up in a Kubernetes cluster. Multiple *Tracker* instances (running on different nodes) share the exact-same event bus. This is achieved by using a cluster manager (I am using Hazelcast).
These *Tracker* instances can be put behind a load-balancer (provided by k8s), forming a distributed, fault-tolerant high-availability system.
 
[1] https://en.wikipedia.org/wiki/Vert.x 


## Installation

```
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

Performance test (using vegeta):
```
$ echo "GET http://localhost:8080/testUserId?data=testData" | vegeta attack -duration=15s -rate=500 | vegeta report
```

#### 2. CLI client

```
$ java -jar ws-client/build/libs/realtimer-ws.jar testUserId
```

#### 3. Web browser client (web-socket)

```
$ open ws-client/html5client.html
```