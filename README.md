# =realtimer=

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
$ java -jar cli/build/libs/realtimer-cli.jar <accountId>
```