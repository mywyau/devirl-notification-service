# 📨 devirl-notification-service

**Notification microservice for the Dev IRL platform.**

This service handles **real-time** and **persistent notifications**, integrating **Kafka**, **PostgreSQL**, **Redis**, and **WebSockets** using the **Typelevel stack** — including Cats Effect, FS2, Http4s, and Doobie.

---

## 🚀 Overview

The `devirl-notification-service` is responsible for receiving, storing, and broadcasting notifications triggered by events from other microservices (e.g., quest completion, payments, profile updates).

It listens to Kafka topics, stores notifications in Postgres for history, and streams them live via WebSockets to connected frontend users.

---

## 🧩 Architecture Summary

### 1️⃣ Kafka Consumer
- Subscribes to the `notifications` topic.
- Decodes incoming JSON messages into `NotificationEvent`s.
- Maps each event into a domain-level `Notification`.
- Persists the notification into **PostgreSQL**.
- Publishes it into an **FS2 Topic** for in-memory pub/sub distribution.

### 2️⃣ FS2 Topic (Pub/Sub Layer)
- Acts as an in-memory message bus.
- Broadcasts new notifications to all connected WebSocket clients.
- Each client receives only their own relevant messages.

### 3️⃣ WebSocket Endpoint
- Exposes `/ws/:userId`.
- Streams new notifications in real time to the frontend (e.g. toast popups).
- Powered by FS2 + Http4s WebSocket streams.

### 4️⃣ HTTP Endpoints
- Provide REST access to stored notifications for history or pagination.

### 5️⃣ PostgreSQL
- Stores notifications persistently.
- Each notification has an auto-incremented `id` and a unique `notification_id` (UUID).

### 6️⃣ Redis
- Used for caching or future scaling features (e.g., deduplication or user session state).

---

## ⚙️ Data Flow

Kafka Topic ---> NotificationConsumer ---> PostgreSQL
|
v
FS2 Topic
|
v
WebSocket Clients


---

## 🧪 Testing

Integration tests run using **Weaver + Cats Effect** with Kafka and Postgres:

- Each test creates a unique Kafka topic (e.g., `notifications.test.<timestamp>`).
- The consumer listens for events, persists them to Postgres, and publishes to the FS2 Topic.
- The test asserts that the record exists in the database after consumption.

---

## 🧰 Tech Stack

| Component | Technology |
|------------|-------------|
| Language | Scala 3 |
| Concurrency / Effects | Cats Effect 3, FS2 |
| Web Framework | Http4s |
| Database | PostgreSQL (via Doobie) |
| Messaging | Kafka (via fs2-kafka) |
| Cache | Redis (via redis4cats) |
| Logging | Log4Cats (Slf4j + Logback) |

---

## 🧑‍💻 Local Development

### 🐳 Start dependencies

```bash
docker-compose up -d postgres redis kafka
```


### Kafka commands

#### List topics
```
docker exec -it kafka-container-redpanda-1 rpk topic list --brokers localhost:9092
```


#### Describe a topic
```
docker exec -it kafka-container-redpanda-1 rpk topic describe notifications --brokers localhost:9092
```

#### Delete a topic
```
docker exec -it kafka-container-redpanda-1 rpk topic delete notifications --brokers localhost:9092
```

#### Connect to a websocket
```
websocat ws://localhost:8080/ws/test-user
```

#### You should see a response like this:
```
{
  "id": "e82b2d91-2d6c-4d2c-bd7f-0cfa3a3f5a7e",
  "userId": "test-user",
  "title": "Quest Completed!",
  "message": "Your quest has been verified and completed.",
  "eventType": "quest.completed",
  "createdAt": "2025-11-13T01:01:41.602Z",
  "read": false
}
```