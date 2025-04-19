# Programs as values

This talk stems from a Linkedin post a friend posted a few months ago, where he rants that more often than not, in a particularly notorious system within the organization, we see
"Investigation requests" rather than "Bug Reports". The reason why this happens is that nobody really understands what the system is really doing, nor supposed to do. 

Sometimes the investigation request is: _We have Sytem A that is consuming documents from a Kafka topic an should produce processing proofs on the other side.
I see  100 messages coming in, and only 70 coming out. What happened to the other 30?_

Or another one: _In a purchase approval system, I see 30 transactions refused out of 100, why are they refused?_

Or in a Shop visualization mobile app displaying shops on Google Maps, _We were supposed to see 10 locations but we see only 8. Why is that?_

### Problem nr 1: clarity of intent

When I started learning FP with Scala, one selling point was "I want to focus on the _what_ rather than the _how_ ". And for sure

```scala
val x = List(1, 2, 3)
val y = x.map(_ * 2) 
```

is a very big improvement vs its procedural counterpart

```scala
val x = List(1, 2, 3)
val y = scala.collection.mutable.ArrayBuffer.empty[Int]
for (i <- 0 until x.size) {
  y += x(i) * 2
}
```

but what about a simple problem like establishing if our system is healthy?

```scala
def checkErrors() = 
  def dbCheck(dbType: DbType, checkTables: List[TableName]): ZIO[Transactor[Task], Nothing, List[StatusError]] =
    for {
      res <- dbType match
               case DbType.Postgres =>
                 DoobieZIOdBHealthcheck.status(DoobieZIOdBHealthcheck.existsPostgres, checkTables)
               case DbType.MySql =>
                 DoobieZIOdBHealthcheck.status(DoobieZIOdBHealthcheck.existsMySql, checkTables)
  
    } yield res
  
  def kafkaCheck(topics: List[Topic]) =
    KafkaHealthCheck.checkTopics(topics).map(_.toList)
  
  def httpCheck(url: Url) = SttpHealthCheck.check(url)
  
  ZIO
    .collectAll(
      List(
        dbCheck(DbType.MySql, List(TableName("table1"), TableName("table2"))),
        kafkaCheck(List(Topic("topic1"), Topic("topic2"))),
        httpCheck(Url("http://localhost:8080"))
      )
    )
    .map(_.flatten)
```

Is this focused on the what or on the how? From a FP perspective, it adheres to all the principles of FP:

- No mutability
- Functional effects systems, so no "impure" effects such as `Future`

But to understand what this does, we need to deep dive into the tale given by this piece of code. There's a function that checks the DB, by checking some tables exist (show code). Then a function that checks if the expected Kafka topics are reachable, and a function that checks a given URL.

So we have a problem of _readability_ of this function.

Another problem is that this function is unconstrained. We could squeeze in there any computation or effect, making it do more than it should, e.g. we could send some unrelated message at every health check.

### Problem nr 2: divergence between implementation and documentation 
The first version of this function, written 2 years ago, was just checking the DB. Then during time we first added a Kafka check, and lastly an external Http check. But our documentation remaied outdated (show documentation)

Now, while this is a pedagogical example, in real business casees with complex business rules, these situations are pretty common