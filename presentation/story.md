# Programs as values

This talk stems from a LinkedIn post a friend of mine posted a few months ago, where he rants that more often than not, in a particularly notorious system within the organization, we see "Investigation requests" rather than "Bug Reports". The reason why this happens is that nobody really understands what the system is really doing, nor supposed to do. 

Sometimes the investigation request is: _We have Sytem A that is consuming documents from a Kafka topic an should produce processing proofs on the other side. I see  100 messages coming in, and only 70 coming out. What happened to the other 30?_

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
/**
 * Health check ensures the expected tables are accessible in the DB
 */
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
The first version of this function, written 2 years ago, was just checking the DB. Then during time we first added a Kafka check, and lastly an external Http check. But our documentation remaied outdated (show documentation), and that's all we have explaining what our code does.

Now, while this is a pedagogical example, in real business cases with complex business rules, these situations are pretty common

### Problem nr 3: solution bound to a specific technology
Another issue we see here is that this solution is bound to use ZIO, and Doobie. If our team deals with services developed using different technologies, e.g. Slick/Future, we need to reimplement from scratch a totally different function, with no attention guaranteed between the equivalence of the 2 functions. We can easily slip over some details while implementing a Future-based version, vs a ZIO-based one.

### Solution: Define a language to specify the problem
The first step is to start from the definition of our problem, rather than from its solution.
We use algebraic data types (sum types, product types) to describe our problem. Here we want to describe the ways in which our application can fail, or rather the possible ways in which an `ErrorCondition` can arise. An `ErrorCondition` can arise if one of these simple error situations occur

```scala
sealed trait ErrorCondition

object ErrorCondition {
  case class DBErrorCondition(dbType: DbType, checkTables: List[TableName]) extends ErrorCondition
  case class KafkaErrorCondition(topics: List[Topic])                       extends ErrorCondition
  case class HttpErrorCondition(url: Url)                                   extends ErrorCondition
}
```
[highlight them one-by-one]

These define the basic error condition. But an error condition can occur if at least one of these error conditions occur, and we want to express that by combining different error conditions into one. Given the combination here is shared among all the error  conditions, we can add a combinator as part of the base trait:

```scala
sealed trait ErrorCondition { self =>
  def ||(other: ErrorCondition): ErrorCondition = Or(self, other)
}

object ErrorCondition {
  case class Or(left: ErrorCondition, right: ErrorCondition) extends ErrorCondition

  case class DBErrorCondition(dbType: DbType, checkTables: List[TableName]) extends ErrorCondition
  case class KafkaErrorCondition(topics: List[Topic])                       extends ErrorCondition
  case class HttpErrorCondition(url: Url)                                   extends ErrorCondition
}
```

In this way we have all we need to combine basic error conditions into more complex ones, for example one error condition could be:

```scala
Or(DBErrorCondition(DbType.Postgres, List(TableName("user", "access_token"))), HttpErrorCondition(Url("https://license-check.org/check")))
```

Here the `Or` is a combinator term, while the other 2 are basic terms. It is handy to introduce some constructors to make the creation of the basic terms. We could add an extension method or just add a method to tbe base trait to facilitate the combination of such terms:

```scala
val errorCondition =
    dbErrorCondition(dbType, TableName("user"), TableName("access_token")) ||
      getHttp2xx(Url("https://license-check.org"))
```


So far we didn't think about what we should do to detect such an error condition, but we defined a _precise language to describe our problem. This language is made of base types to describe basic situations, and more complex types, possibly recursive types, to describe the most complex situations.


### Approachable approach
In designing this solution we didn't require understanding HKT (like tagless final does), nor Free Monads, which share some aspects of our solution. We actually restricted what we can do to a very limited set of operations. We didn't allow introducing generic functions, nor map/flatMap, as this gives us the ability to interpret properly the program we build.

The language properties we are leveraging are very basic, and this approach can be replicated almost seamlessly in any language having a decent support for product/sum types, ideally with some sort of pattern matching.