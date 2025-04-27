# Programs as values

This talk stems from a LinkedIn post a friend of mine posted a few months ago, where he rants that more often than not, in a particularly notorious system within the organization, we see "Investigation requests" rather than "Bug Reports". The reason why this happens is that nobody really understands what the system is really doing, nor supposed to do. 

Sometimes the investigation request is: _We have Sytem A that is consuming documents from a Kafka topic an should produce processing proofs on the other side. I see  100 messages coming in, and only 70 coming out. What happened to the other 30?_

Or another one: _In a purchase approval system, I see 30 transactions refused out of 100, why are they refused?_

Or in a Shop visualization mobile app displaying shops on Google Maps, _We were supposed to see 10 locations but we see only 8. Why is that?_

### The promises of Functional Programming

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

### Problem nr 1: clarity of intent

Is this focused on the what or on the how? From a FP perspective, it adheres to all the principles of FP:

- No mutability
- Functional effects systems, so no "impure" effects such as `Future`

But to understand what this does, we need to deep dive into the tale given by this piece of code. There's a function that checks the DB, by checking some tables exist (show code). Then a function that checks if the expected Kafka topics are reachable, and a function that checks a given URL.

In other words we have a problem of _readability_ of this function.

Another problem is that this function is unconstrained. We could squeeze in there any computation or effect, making it do more than it should, e.g. we could send some unrelated message at every health check.

### Problem nr 2: divergence between implementation and documentation 
The first version of this function, written 2 years ago, was just checking the DB. Then during time we first added a Kafka check, and lastly an external Http check. But our documentation remaied outdated (show documentation), and that's all we have explaining what our code does.

Now, while this is a pedagogical example, in real business cases with complex business rules, these situations are pretty common

### Problem nr 3: solution bound to a specific technology
Another issue we see here is that this solution is bound to use ZIO, and Doobie. If our team deals with services developed using different technologies, e.g. Slick/Future, we need to reimplement from scratch a totally different function, with no attention guaranteed between the equivalence of the 2 functions. We can easily slip over some details while implementing a Future-based version, vs a ZIO-based one.

### Orthogonality
We want to address the 3 problems we just explained, in an orthogonal way. When we talk about orthogonality we mean "independence of the solutions", or solving one problem should not affect the resolution of the other problems, just like in a vector space with inner product we can't express orthogonal vectors in terms of the others

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

Here the `Or` is a combinator term, while the other 2 are basic terms. If we introduce some constructors to make the creation of the basic terms easier, we can describe our health check where we just check the  existance of 2 DB tables on Postgres, and an http call that must return a Success code. 

```scala
val errorCondition =
    dbErrorCondition(dbType, TableName("user"), TableName("access_token")) ||
      getHttp2xx(Url("https://license-check.org"))
```


So far we didn't think about what we should do to detect such an error condition, but we defined a _precise language_ to describe our problem. This language is made of base types to describe basic situations, and more complex types, including a recursive type, to describe the most complex situations.

### Solution: Solve the problem by attacking the simple parts

Once we have this in place, we need to make this runnable, and this is what goes by as _interpretation_. Interpretation is completely free: we have no constraints coming from our DLS about what we need to do with the data structure we built. 

So if we use ZIO and Doobie, the interpreter that, given our health check definition returns the list of status errors, if any, would be

```scala
object ZIOInterpreter {
  def interpret(
    errorCondition: ErrorCondition
  ): URIO[Transactor[Task] & SttpClient, List[StatusError]] =
    errorCondition match
      case ErrorCondition.DBErrorCondition(DbType.Postgres, checkTables) =>
        DoobieZIOdBHealthcheck.status(DoobieZIOdBHealthcheck.existsPostgres, checkTables)
      case ErrorCondition.DBErrorCondition(DbType.MySql, checkTables) =>
        DoobieZIOdBHealthcheck.status(DoobieZIOdBHealthcheck.existsMySql, checkTables)
      case ErrorCondition.HttpErrorCondition(url) =>
        SttpHealthCheck.check(url)
}
```

Here the compiler tells us we forgot to cover the recursive term

```scala
[warn]    |    errorCondition match
[warn]    |    ^^^^^^^^^^^^^^
[warn]    |match may not be exhaustive.
[warn]    |
[warn]    |It would fail on pattern case: domainmodeling.healthcheck.ErrorCondition.Or(_, _)
[warn]    |
```

So let's add it
```scala
    errorCondition match
      case ErrorCondition.Or(left, right) =>
        interpret(left).zipPar(interpret(right)).map { case (leftErrors, rightErrors) => leftErrors ++ rightErrors }
      // the basic terms    
```

With this we have a program that we can attach to our service `ready` endpoint, and have k8s querying it to establish the readiness of our service.

Let's focus again on our problems:

1. Clarity of intent
2. Divergence implementation vs documentation
3. Solution bound to a specific technology

Here we improved on 1. Our DSL is succinctly conveying what we want to do.

About point 3, if we have a service that is using Futures and Slick rather than ZIO and Doobie, we could just write another interpreter, while keeping the clarity of intent unaltered (Orthogonality):

```scala
def interpret( db: slick.jdbc.JdbcBackend#Database,
                 jdbcProfile: slick.jdbc.JdbcProfile,
                 httpClient: SttpFutureBackend[Future, Any])(
                 errorCondition: ErrorCondition
               )(implicit
                 ec: ExecutionContext
               ): Future[List[StatusError]] = {
  errorCondition match {
    case ErrorCondition.DBErrorCondition(DbType.Postgres, checkTables) =>
        SlickFutureDbHealthCheck.checkPostgresTables(db,jdbcProfile, checkTables),

    case ErrorCondition.DBErrorCondition(DbType.MySql, checkTables) =>
        SlickFutureDbHealthCheck.checkMySqlTables(db, jdbcProfile, checkTables),
    
    case ErrorCondition.HttpErrorCondition(url) =>
      withTimeout(
        FutureHttpHealthCheck.check(httpClient, url),
        List(StatusError(Source("Http"), Message("Http call timed out")))
      )

    case ErrorCondition.Or(left, right) =>
      // Run both checks in parallel
      val leftFuture = interpret(db, jdbcProfile, kafkaClient, httpClient)(left)
      val rightFuture = interpret(db, jdbcProfile, kafkaClient, httpClient)(right)

      // Combine results
      for {
        leftErrors <- leftFuture
        rightErrors <- rightFuture
      } yield leftErrors ++ rightErrors

  }
}
```

Let's now tackle point 2, the divergence between documentation and implementation. Rather than documenting our code, or writing a Confluence page doomed to oblivion, let's interpret our blocking rule as a String

```scala
def interpret(
                 errorCondition: ErrorCondition
               ): String = //...
```
and we can document the health rules as part of our CI, or when the service bootstraps (SHOW A SCREENSHOT HERE, as we want to see the colors)

```ansi
Either of:
Database Check (Postgres):
  Required tables: customer, access_token
OR
Kafka Check:
  Required topics: events
OR
HTTP Check:
  URL: http://localhost:8080/status/200
```

## Keeping documentation aligned with implementation as code evolves
Now let's suppose we want to add a healthcheck condition, based on the listing of a given list of topics. Our app should be able to reach those topics, this is important e.g. if an ACL policy is in place.

1. We enrich our DSL with an extra term:

```scala
  case class KafkaErrorCondition(topics: List[Topic])                       extends ErrorCondition
```
and we provide with a convenient way to use it within our DSL
```scala
  def kafkaErrorCondition(topics: Topic*): ErrorCondition =
    KafkaErrorCondition(topics.toList)
```

so we can express an evolution of our error condition:

```scala
val errorCondition =
    dbErrorCondition(dbType, TableName("customer"), TableName("access_token")) ||
      kafkaErrorCondition(kafkaTopic) ||
      getHttp2xx(Url("http://localhost:8080/status/200"))
```

The compiler will tell us immediately that we need to adjust our interpreters, to cover all the cases in the pattern matching

```
[warn] 17 |    errorCondition match
[warn]    |    ^^^^^^^^^^^^^^
[warn]    |match may not be exhaustive.
[warn]    |
[warn]    |It would fail on pattern case: domainmodeling.healthcheck.ErrorCondition.KafkaErrorCondition(_)
```

After some pretty obvious small adjustments we have a health check that is considering also the Kafka situation:

```scala
Either of:
Database Check (Postgres):
  Required tables: customer, access_token
OR
Kafka Check:
  Required topics: events
OR
HTTP Check:
  URL: http://localhost:8080/status/200
```


## A more complex example: a rule engine for payment authorization

The health check example was just useful to learn the technique, and somehow overstretched for a relatively simple task, but this approach shined when we were tasked to implement an authorization engine. 

The problem: We have a payment system and we need to authorize payments based on different rules.

```scala
  case class CreditCard(
    id: Id,
    cardNumber: CardNumber,
    cardHolderName: CardHolderName,
    cardType: CardType,
    expiryDate: ExpiryDate,
    issuedInCountry: Country
  )

  case class Purchase(
    id: Id,
    creditCardId: Id,
    amount: Amount,
    shop: Shop
  )

case class Shop(
    id: ShopId,
    name: ShopName,
    country: Country,
    categories: List[ShopCategory]
  )

```

We want to implement something like 

```scala
def isBlocked(creditCard: CreditCard, purchase: Purchase): Boolean
```

Our Business party (PO) came one day with an initial requirement, like:

> _we want to block all electronic purchases in China_

We could implement this simple rule as:

```scala
 def isBlocked(creditCard: CreditCard, purchase: Purchase):  Boolean =
    purchase.shop.country == Country.China && purchase.shop.categories.contains(ShopCategory.Electronics))
```

but our PO told us that we can expect this rule to become more complex.

So we decide to define a language to cover these evolving requirements. The recipe is the same:     

#### 1. Define the base cases we want to cover as a sealed trait

```scala
sealed trait BlockingRule

case class PurchaseOccursInCountry(country: Country)                  extends BlockingRule
case class PurchaseCategoryEquals(purchaseCategory: ShopCategory) extends BlockingRule
case class PurchaseAmountExceeds(amount: Double)                      extends BlockingRule
case class FraudProbabilityExceeds(threshold: Probability)            extends BlockingRule
case class CreditCardFlagged()                                        extends BlockingRule
case class ShopIsBlacklisted()                                        extends BlockingRule
```
#### 2. Equip our data types with combinators to build more complex data types 
In this case it is sufficient to use recursive data types:

```scala
sealed trait BlockingRule { self =>
    def &&(other: BlockingRule): BlockingRule = BlockingRule.And(self, other)
    def ||(other: BlockingRule): BlockingRule = BlockingRule.Or(self, other)
}
```


#### 3. Build some constructor to make building these cases more user-friendly

```scala
object DSL {
    private def purchaseInCountry(country: Country): BlockingRule           = BlockingRule.PurchaseOccursInCountry(country)
    def purchaseCountryIsOneOf(countries: Country*): BlockingRule = countries.map(purchaseInCountry).reduce(_ || _)
    private def purchaseCategoryEquals(purchaseCategory: ShopCategory): BlockingRule =
      BlockingRule.PurchaseCategoryEquals(purchaseCategory)
    def purchaseCategoryIsOneOf(purchaseCategories: ShopCategory*): BlockingRule =
      purchaseCategories.map(purchaseCategoryEquals).reduce(_ || _)
    def purchaseAmountExceeds(amount: Double): BlockingRule           = BlockingRule.PurchaseAmountExceeds(amount)
    def fraudProbabilityExceeds(threshold: Probability): BlockingRule = BlockingRule.FraudProbabilityExceeds(threshold)
    def creditCardFlagged: BlockingRule                               = BlockingRule.CreditCardFlagged()
    def shopIsBlacklisted: BlockingRule                               = BlockingRule.ShopIsBlacklisted()
  }
```

Here we can see how e.g. we implement the exclusion of countries from the list based on the chaining of base cases . 

#### 1. Authorization Rule: Business interpreter

```scala
  def isBlocked(rule: BlockingRule, ccFlaggedService: CreditCardFlaggedService, fraudScoreService: FraudScoreService, shopRepository: ShopRepository)(
      cc: CreditCard, p: Purchase
  ): UIO[Boolean] = {
    def eval(rule: BlockingRule): UIO[Boolean] = rule match {
      case BlockingRule.PurchaseOccursInCountry(country) =>
        purchaseOccursInCountry(BlockingRule.PurchaseOccursInCountry(country))(input)
      case BlockingRule.PurchaseCategoryEquals(purchaseCategory) =>
        purchaseCategoryEquals(BlockingRule.PurchaseCategoryEquals(purchaseCategory))(cc, p)
      case BlockingRule.PurchaseAmountExceeds(amount) =>
        purchaseAmountExceeds(BlockingRule.PurchaseAmountExceeds(amount))(input)
      case BlockingRule.CreditCardFlagged() => creditCardFlagged(ccFlaggedService)(cc, p)
      case BlockingRule.FraudProbabilityExceeds(threshold) =>
        fraudProbability(BlockingRule.FraudProbabilityExceeds(threshold), fraudScoreService)(cc, p)
      case BlockingRule.And(l, r) => eval(l).zipWith(eval(r))(_ && _)
      case BlockingRule.Or(l, r)  => eval(l).zipWith(eval(r))(_ || _)
      case BlockingRule.ShopIsBlacklisted() => shopRepository.isBlacklisted(p.shop.id)
    }

    eval(rule)
  }
```

I'll spare you the details of this implementation that you can find one the repository. However, once we carefully implemented these defining cases, we are basically able to cover all possible rules defined through our DSL, and this occurs only through the intervention on the rule defined through our DSL.

#### 2. Authorization Rule: Mermaid interpreter
We define also an interpreter that renders the rule as a mermaid graph, so that developers and business people can reason about the applied rules in a visual way, and observe a visual documentation always aligned with the actual code.

```scala
def toMermaidCode(blockingRule: BlockingRule): UIO[String] = // ...
```

With this in place we can monitor the evolution of our requirements for the blocking rule:





[//]: # (TODO: show how we defined a mermaid interpreter, and show how the implmeented rule evolved during time, showing how complicated the mermaid graph can become)


## Does this only work for algorithmic problems?
No, we can model pretty much any problem in this way. For example we could model an ETL processing pipeline by defining a totally generic stream with pipes, maps and sinks. Then we interpret this and make it a ZIO stream, or an FS2 stream, or a mermaid graph that faithfully represents the processing steps.

### Keep things simple and constrained. 
In designing this solution we didn't require understanding HKT (like tagless final does), nor Free Monads, which share some aspects of our solution. We actually restricted what we can do to a very limited set of operations. We didn't allow introducing generic functions, nor map/flatMap, as this gives us the ability to interpret properly the program we build.

The language properties we are leveraging are very basic, and this approach can be replicated almost seamlessly in any language having a decent support for product/sum types, ideally with some sort of pattern matching. 

One more thing to notice is that not all DSLs need to be monadic. Monads are a powerful way to combine computations, but not all computations require the power of a monad. Definitely not at high level. When we need to build a low-level language, monads come naturally, and so come applicatives. For high-level languages, such as those used to describe most of our business needs, monads are overkill, and all you need is some simple constructors and combinators.