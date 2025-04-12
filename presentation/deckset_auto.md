# Straight Implementation of Health Checks

---

## **Problem Statement**

- **Goal**: Check the health of our system.
    - Database
    - Kafka
    - HTTP dependencies

- **Initial solution**: A straightforward implementation leveraging:
    - **Doobie** for database health checks
    - **zio-kafka** for Kafka
    - **Sttp** for HTTP

---

## **Challenges with the Current Approach**

1. **Non-Composability**:
    - Adding a new health check requires modifying the `checkErrors` method.

2. **Technology-Specific**:
    - Tightly coupled with Doobie, zio-kafka, and Sttp.
    - Challenges with shared libraries for varied needs:
        - Legacy services (e.g., Akka-HTTP)
        - Modern services (e.g., Http4s)

3. **Not Testable**:
    - Directly connects to dependencies (e.g., database, Kafka).
    - Mix of **business logic** and **side effects**:
        - Makes unit testing impossible.

---

## **Key Code Overview**

### Main Functionality

```scala
object StraightImplementation {
  def checkErrors() = {
    ZIO.collectAll(
      List(
        dbCheck(...),
        kafkaCheck(...),
        httpCheck(...)
      )
    ).map(_.flatten)
  }
}
```

---

## **Database Health Check**

### Highlights:
- Uses **Doobie**:
    - Supports databases like Postgres and MySQL.
- Encodes the health-check logic in helper methods:
    - `status`, `existsPostgres` and `existsMySql`.

```scala
dbCheck(DbType.MySql, List(TableName("table1"), TableName("table2")))
```

---

## **Kafka Health Check**

### Highlights:
- Uses **zio-kafka** to check if Kafka topics exist.
- Simple API:
    ```scala
    KafkaHealthCheck.checkTopics(topics).map(_.toList)
    ```

---

## **HTTP Health Check**

### Highlights:
- Uses **Sttp** to verify HTTP services.

```scala
SttpHealthCheck.check(Url("http://localhost:8080"))
```

---

## **Downsides Summarized**

1. Modification overhead for new checks.
2. Technology lock-in:
    - Dependency on **Doobie**, **zio-kafka**, and **Sttp**.
3. Lack of testability:
    - Cannot test without real dependencies.

---

## **What's Next?**

- Simplify and enhance composability.
- Introduce abstraction layers for modularity.
- Enable testing by separating side effects from logic.

---

## **Thank You!**

- Questions? 😊