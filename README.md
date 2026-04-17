
# Kafka setup having two brokers and 3 partitions each with Docker compose

# To set the volumes
- run `docker run --rm apache/kafka:latest /opt/kafka/bin/kafka-storage.sh random-uuid` it will generate the cluster id eg: `5L6g3nShT-eMCtK--X86sw` \
- set it as on all the brokers `KAFKA_CLUSTER_ID: 5L6g3nShT-eMCtK--X86sw`

```yml
#Docker compose for kafka with two brokers 
services:
  broker:
    image: apache/kafka:latest
    container_name: broker
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_CLUSTER_ID: 5L6g3nShT-eMCtK--X86sw
      # ✅ LISTEN on all interfaces
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@broker:9093,2@broker-2:9093
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 2
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 2
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS: 0
      KAFKA_NUM_PARTITIONS: 3
      KAFKA_LOG_DIRS: /var/lib/kafka/data
    volumes:
      - kafka-data-1:/var/lib/kafka/data 
    ports:
      - "9092:9092" 

  broker-2:
    image: apache/kafka:latest
    container_name: broker-2
    environment:
      KAFKA_NODE_ID: 2
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_CLUSTER_ID: 5L6g3nShT-eMCtK--X86sw
      # ✅ LISTEN on all interfaces
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9094
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@broker:9093,2@broker-2:9093
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 2
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 2
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS: 0
      KAFKA_NUM_PARTITIONS: 3
      KAFKA_LOG_DIRS: /var/lib/kafka/data
    volumes:
      - kafka-data-2:/var/lib/kafka/data
    ports:
      - "9094:9092"
volumes:
  kafka-data-1:
  kafka-data-2:
```
- To build and run the docker compose \
  `docker-compose up -d`
- Set the uuid in server.properties [Replace the <UUID> with previously generated UUID] Needed to use the volumes.
  `docker exec -it broker /opt/kafka/bin/kafka-storage.sh format -t <UUID> -c /opt/kafka/config/kraft/server.properties`
- restart the compose
  `docker-compose restart`
- To open the pod bash \
  `docker exec -it broker bash`
- TO list the existing kafka topics \
`kafka-topics.sh --list --bootstrap-server localhost:9092`

- Create the kafka topics \
`kafka-topics.sh --create 
  --topic user-message 
  --bootstrap-server localhost:9092 
  --partitions 3 
  --replication-factor 1
`
# Kafbat UI visulization
https://github.com/kafbat/kafka-ui/releases

- Download the jar and run with
```
# application-local.yml for kafbat ui

logging:
  level:
    root: INFO
    io.kafbat.ui: DEBUG

spring:
  jmx:
    enabled: true

kafka:
  clusters:
    - name: local
      bootstrapServers: localhost:9092,localhost:9094
```
# Run the kafbat ui jar
`java -Dspring.config.additional-location=application-local.yml --add-opens java.rmi/javax.rmi.ssl=ALL-UNNAMED -jar ./api-v1.4.2.jar ` 
## open the kafbat UI
`http://localhost:8080/ui/` 

# For docker version of kafbat visit
https://ui.docs.kafbat.io/configuration/configuration-file

--------------------------------------------------------------

# 🚀 Kafka Setup on WSL (End-to-End Guide)

This guide walks you through:
- Installing WSL
- Installing Kafka
- Configuring networking
- Producing & consuming messages
- Connecting Spring Boot

---

# 🪟 1. Install WSL

    Open **PowerShell (Admin)**:

    ```powershell
    wsl --install
    wsl
--- 

# ☕ 2. Install Java (Required) Inside WSL:

`sudo apt update` \
`sudo apt install openjdk-17-jdk -y` \
`java -version`

`wget https://downloads.apache.org/kafka/3.7.0/kafka_2.13-3.7.0.tgz` \
`tar -xzf kafka_2.13-3.7.0.tgz`         (or manually unzip) \
`cd kafka_2.13-3.7.0 `

## edit server.properties
- Post installing the kafka we need to change some configs in the server.properties

- If using WINDOWS then install the WSL get the IP of the WSL by
hostname -I        eg.172.20.117.7

```
process.roles=broker,controller
node.id=1

listeners=PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
advertised.listeners=PLAINTEXT://172.20.117.7:9092

controller.listener.names=CONTROLLER
controller.quorum.voters=1@localhost:9093

inter.broker.listener.name=PLAINTEXT
```

# start kafka
`bin/kafka-server-start.sh config/server.properties`

# 🧪 7. Test from Windows
`Test-NetConnection -ComputerName 172.20.117.7 -Port 9092`

# produce message
`bin/kafka-console-producer.sh
--topic rushi-events
--bootstrap-server localhost:9092
`

# consume message via cli
`
bin/kafka-console-consumer.sh 
--topic rushi-events 
--from-beginning 
--bootstrap-server localhost:9092
`
# Kafbat UI visulization
https://github.com/kafbat/kafka-ui/releases

- Download the jar and run with
```
# application-local.yml for kafbat ui

logging:
  level:
    root: INFO
    io.kafbat.ui: DEBUG

spring:
  jmx:
    enabled: true

kafka:
  clusters:
    - name: local
      bootstrapServers: localhost:9092,localhost:9094
```
# Run the kafbat ui jar
`java -Dspring.config.additional-location=application-local.yml --add-opens java.rmi/javax.rmi.ssl=ALL-UNNAMED -jar ./api-v1.4.2.jar ` 
## open the kafbat UI
`http://localhost:8080/ui/` 

# For docker version of kafbat
https://ui.docs.kafbat.io/configuration/configuration-file



# server.properties example
```yml
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

############################# Server Basics #############################

# The role of this server. Setting this puts us in KRaft mode
process.roles=broker,controller

# The node id associated with this instance's roles
node.id=1

# List of controller endpoints used connect to the controller cluster
# controller.quorum.bootstrap.servers=0.0.0.0:9093

# controller.quorum.voters=1@localhost:9093
controller.quorum.voters=1@127.0.0.1:9093

############################# Socket Server Settings #############################

# The address the socket server listens on.
# Combined nodes (i.e. those with `process.roles=broker,controller`) must list the controller listener here at a minimum.
# If the broker listener is not defined, the default listener will use a host name that is equal to the value of java.net.InetAddress.getCanonicalHostName(),
# with PLAINTEXT listener name, and port 9092.
#   FORMAT:
#     listeners = listener_name://host_name:port
#   EXAMPLE:
#     listeners = PLAINTEXT://your.host.name:9092
listeners=PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093


# Name of listener used for communication between brokers.
inter.broker.listener.name=PLAINTEXT

# Listener name, hostname and port the broker or the controller will advertise to clients.
# If not set, it uses the value for "listeners".
# advertised.listeners=PLAINTEXT://localhost:9092
# advertised.listeners=PLAINTEXT://127.0.0.1:9092
advertised.listeners=PLAINTEXT://172.20.117.7:9092


# A comma-separated list of the names of the listeners used by the controller.
# If no explicit mapping set in `listener.security.protocol.map`, default will be using PLAINTEXT protocol
# This is required if running in KRaft mode.
controller.listener.names=CONTROLLER

# Maps listener names to security protocols, the default is for them to be the same. See the config documentation for more details
listener.security.protocol.map=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,SSL:SSL,SASL_PLAINTEXT:SASL_PLAINTEXT,SASL_SSL:SASL_SSL

# The number of threads that the server uses for receiving requests from the network and sending responses to the network
num.network.threads=3

# The number of threads that the server uses for processing requests, which may include disk I/O
num.io.threads=8

# The send buffer (SO_SNDBUF) used by the socket server
socket.send.buffer.bytes=102400

# The receive buffer (SO_RCVBUF) used by the socket server
socket.receive.buffer.bytes=102400

# The maximum size of a request that the socket server will accept (protection against OOM)
socket.request.max.bytes=104857600


############################# Log Basics #############################

# A comma separated list of directories under which to store log files
log.dirs=/tmp/kraft-combined-logs

# The default number of log partitions per topic. More partitions allow greater
# parallelism for consumption, but this will also result in more files across
# the brokers.
num.partitions=3

# The number of threads per data directory to be used for log recovery at startup and flushing at shutdown.
# This value is recommended to be increased based on the installation resources.
num.recovery.threads.per.data.dir=2

############################# Internal Topic Settings  #############################
# The replication factor for the group metadata internal topics "__consumer_offsets", "__share_group_state" and "__transaction_state"
# For anything other than development testing, a value greater than 1 is recommended to ensure availability such as 3.
offsets.topic.replication.factor=1
share.coordinator.state.topic.replication.factor=1
share.coordinator.state.topic.min.isr=1
transaction.state.log.replication.factor=1
transaction.state.log.min.isr=1

############################# Log Flush Policy #############################

# Messages are immediately written to the filesystem but by default we only fsync() to sync
# the OS cache lazily. The following configurations control the flush of data to disk.
# There are a few important trade-offs here:
#    1. Durability: Unflushed data may be lost if you are not using replication.
#    2. Latency: Very large flush intervals may lead to latency spikes when the flush does occur as there will be a lot of data to flush.
#    3. Throughput: The flush is generally the most expensive operation, and a small flush interval may lead to excessive seeks.
# The settings below allow one to configure the flush policy to flush data after a period of time or
# every N messages (or both). This can be done globally and overridden on a per-topic basis.

# The number of messages to accept before forcing a flush of data to disk
#log.flush.interval.messages=10000

# The maximum amount of time a message can sit in a log before we force a flush
#log.flush.interval.ms=1000

############################# Log Retention Policy #############################

# The following configurations control the disposal of log segments. The policy can
# be set to delete segments after a period of time, or after a given size has accumulated.
# A segment will be deleted whenever *either* of these criteria are met. Deletion always happens
# from the end of the log.

# The minimum age of a log file to be eligible for deletion due to age
log.retention.hours=168

# A size-based retention policy for logs. Segments are pruned from the log unless the remaining
# segments drop below log.retention.bytes. Functions independently of log.retention.hours.
#log.retention.bytes=1073741824

# The maximum size of a log segment file. When this size is reached a new log segment will be created.
log.segment.bytes=1073741824

# The interval at which log segments are checked to see if they can be deleted according
# to the retention policies
log.retention.check.interval.ms=300000

```

# Types of the acks (acknowledgements for the kafka message consumers)

--------------------------------------------------------------
| Ack Mode         | Commit Timing     | Use Case            |
| ---------------- | ----------------- | ------------------- |
| RECORD           | After each record | Default safe        |
| BATCH            | After batch       | High throughput     |
| TIME             | Time-based        | Scheduled commit    |
| COUNT            | After N records   | Controlled batching |
| COUNT_TIME       | Hybrid            | Balanced load       |
| MANUAL           | You control       | Complex logic       |
| MANUAL_IMMEDIATE | Immediate commit  | strict control      |
--------------------------------------------------------------


# Spring Boot Producer (Windows)

- Add dependency to pom
```xml
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-kafka-test</artifactId>
			<scope>test</scope>
		</dependency>
```

- application.yml
```yml
spring:
  kafka:
    bootstrap-servers: 172.20.117.7:9092
```

# spring boot producer
```java
@PostMapping("/{message}")
public ResponseEntity<String> sendMessage(@PathVariable String message) throws Exception {
    kafkaTemplate.send("rushi-events", message).get();
    return ResponseEntity.ok("Message sent: " + message);
}
```


```java
# Spring Boot Consumer
@KafkaListener(topics = "rushi-events", groupId = "group-1")
public void consume(String message) {
    System.out.println("Received: " + message);
}
```
---

# 🧠 Apache Kafka – Core Concepts

## 📌 What is Kafka?

**Apache Kafka** is a **distributed event streaming platform** used to:

* Publish messages (Producers)
* Store them durably (Brokers)
* Consume/process them (Consumers)

### ✅ Key Features

* Distributed
* Scalable
* Fault-tolerant
* High throughput
* Persistent storage

---

# 🏢 What are Brokers?

A **Broker** is a Kafka server.

👉 A Kafka cluster consists of multiple brokers.

## 🔧 Responsibilities

* Store messages on disk
* Handle producer write requests
* Serve consumer read requests
* Manage replication
* Perform leader election

---

## 🧠 Leader & Follower Concept

* **Leader** → Handles all reads/writes
* **Followers** → Replicate data

👉 If leader fails → follower becomes leader

---

# 📦 What are Partitions?

A **Topic** is divided into multiple **Partitions**.

👉 Each partition is an **ordered, immutable log**

---

## 🔥 Why Partitions?

* Enable parallel processing
* Improve scalability
* Distribute load across brokers
* Maintain ordering (within partition)

---

## ⚠️ Partition Limitations

### 1. Consumer Limit

```text
Max consumers in a group = number of partitions
```

Example:

```text
Partitions = 3
Consumers = 5
```

👉 3 consumers active, 2 idle ❌

---

### 2. One Consumer per Partition

* A partition is consumed by **only one consumer per group**

---

### 3. Ordering Limitation

* Ordering guaranteed **only within a partition**
* Not across partitions

---

# 👥 What is a Consumer?

A **Consumer** reads messages from Kafka topics.

* Pull-based system
* Reads using offsets

---

# 👨‍👩‍👧‍👦 What is a Consumer Group?

A **Consumer Group** is a set of consumers working together.

👉 Kafka distributes partitions among them.

---

## 📊 Example

```text
Topic: 4 partitions
Consumers: 2
```

👉 Each consumer gets 2 partitions

---

## ⚠️ Consumer Group Limitations

### 1. Parallelism Limit

```text
Max consumers = number of partitions
```

---

### 2. Rebalancing

When a consumer:

* joins
* leaves

👉 Kafka redistributes partitions → temporary delay

---

### 3. Offset Management

Offsets are stored in:

```text
__consumer_offsets
```

---

# 💀 What is Dead Letter Queue (DLQ / DLT)?

A **Dead Letter Topic (DLT)** stores messages that **failed processing**.

---

## 🧠 Why DLT?

* Prevent infinite retries
* Avoid blocking the system
* Enable debugging of failed messages

---

## 🔁 Flow

```text
Main Topic → Consumer → Failure
            ↓
        Retry (e.g., 3 times)
            ↓
        Still fails
            ↓
        Sent to DLT
```

---

## ⚙️ In Spring Kafka

Handled using:

* `DefaultErrorHandler`
* `DeadLetterPublishingRecoverer`

---

## ⚠️ Important Rules

### 1. Not built into Kafka

👉 DLT is implemented via frameworks (like Spring Kafka)

---

### 2. Must consume separately

```java
@KafkaListener(topics = "topic.DLT")
```

---

### 3. Avoid infinite retries

👉 Best practice:

* Log error
* Send alert
* Fix manually

---

# 🔥 Complete Flow

```text
Producer → Topic (Partitions)
              ↓
         Broker (Leader/Follower)
              ↓
        Consumer Group
              ↓
         Success ✅

         OR

         Failure ❌
              ↓
         Retry
              ↓
         DLT (Dead Letter Topic)
```

---

# 🧾 Quick Summary

| Concept        | Description                          |
| -------------- | ------------------------------------ |
| Kafka          | Distributed event streaming platform |
| Broker         | Kafka server                         |
| Partition      | Unit of parallelism & ordering       |
| Consumer       | Reads messages                       |
| Consumer Group | Enables parallel processing          |
| DLT            | Stores failed messages               |


---

# 🔷 1. Kafka Architecture Overview

```text
        +------------------+
        |    Producer      |
        +--------+---------+
                 |
                 v
        +------------------+
        |      Topic       |
        |  (Partitions)    |
        +--------+---------+
                 |
                 v
        +------------------+
        |     Broker       |
        | (Kafka Server)   |
        +--------+---------+
                 |
                 v
        +------------------+
        |   Consumer(s)    |
        +------------------+
```

---

# 🔷 2. Topic with Partitions

```text
Topic: user-created-event-topic

+-------------------------------+
|         PARTITION 0           |
|  msg1 → msg2 → msg3 → msg4    |
+-------------------------------+

+-------------------------------+
|         PARTITION 1           |
|  msg5 → msg6 → msg7 → msg8    |
+-------------------------------+

+-------------------------------+
|         PARTITION 2           |
|  msg9 → msg10 → msg11         |
+-------------------------------+
```

👉 Order is guaranteed **only inside each partition**

---

# 🔷 3. Broker with Leader & Followers

```text
            Kafka Cluster

     +-----------+   +-----------+   +-----------+
     | Broker 1  |   | Broker 2  |   | Broker 3  |
     +-----------+   +-----------+   +-----------+

     Partition 0 → Leader
     Partition 1 → Follower
     Partition 2 → Follower

     (Replication across brokers)
```

---

## Leader-Follower Example

```text
Partition 0

Leader (Broker 1)
   ↓
Follower (Broker 2)
   ↓
Follower (Broker 3)
```

👉 Writes & reads happen via **Leader only**

---

# 🔷 4. Consumer Group Distribution

```text
Topic: 3 Partitions
Consumers: 3

Partition 0 → Consumer 1
Partition 1 → Consumer 2
Partition 2 → Consumer 3
```

---

## ⚠️ More Consumers than Partitions

```text
Topic: 3 Partitions
Consumers: 5

Partition 0 → Consumer 1
Partition 1 → Consumer 2
Partition 2 → Consumer 3

Consumer 4 → Idle ❌
Consumer 5 → Idle ❌
```

👉 Max parallelism = **number of partitions**

---

# 🔷 5. Consumer Group Scaling

```text
Same Topic, Different Groups

        Topic
          |
   -----------------
   |               |
   v               v

Group A         Group B

C1   C2         C3   C4
```

👉 Each group gets **full copy of data**

---

# 🔷 6. Offset Concept

```text
Partition 0:

[0] msg1 → [1] msg2 → [2] msg3 → [3] msg4

Consumer Offset = 2
```

👉 Consumer will read from:

```text
msg3 onwards
```

---

# 🔷 7. Failure + Retry + DLT Flow

```text
        +------------------+
        |    Producer      |
        +--------+---------+
                 |
                 v
        +------------------+
        |   Main Topic     |
        +--------+---------+
                 |
                 v
        +------------------+
        |    Consumer      |
        +--------+---------+
                 |
         ---------------------
         |                   |
         v                   v
     Success ✅         Failure ❌
                             |
                             v
                    Retry (3 times)
                             |
                             v
                    Still Fails ❌
                             |
                             v
        +-----------------------------+
        | Dead Letter Topic (DLT)     |
        +-----------------------------+
```

---

# 🔷 8. DLT Consumption

```text
Main Topic → Consumer → ❌ Failure
                         |
                         v
                  topic.DLT
                         |
                         v
                DLT Consumer (log/debug)
```

---

# 🔷 9. Full End-to-End Flow

```text
Producer
   |
   v
Topic (Partitions)
   |
   v
Broker (Leader/Follower)
   |
   v
Consumer Group
   |
   +-------> Success ✅
   |
   +-------> Failure ❌
                |
                v
            Retry
                |
                v
             DLT Topic
                |
                v
         DLT Consumer
```

---

# 🧠 Quick Visual Memory Trick

```text
Kafka = PIPELINE

Producer → Topic → Partition → Broker → Consumer → DLT (if fail)
```

---

<img width="1536" height="1024" alt="kafka workflow" src="https://github.com/user-attachments/assets/939eb1ce-28c3-4e5c-8b4c-47ee8faa638d" />
