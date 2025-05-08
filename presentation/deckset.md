autoscale: true
slidenumbers: false
build-lists: true
list: alignment(left)
footer: Pierangelo Cecchetto - LambdaConf 2025


# Programs as Values

![right fit](images/cover.jpeg)

### Write code and don't get lost

---

# The Code archeologist problem

![right fit](images/digital-archeologist-problem.jpeg)

- "Investigation requests" _(not "Bug Reports")_
- What is the system _doing_?
- What is the system _supposed to do_?

^ The suggestion for this talk comes from a LinkedIn post a friend posted a few months ago, where he rants that more often than not, in a particularly  core system within the organization, we see "Investigation requests" rather than "Bug Reports". Here are a few examples of how such requests look like:

---

# Investigation Request #1

![right fit](images/investigation_1.png)

  System consumes documents from Kafka and produces processing proofs. We see _100 messages going in_, and only _70 coming out_. What happened to the other 30?

---

# Investigation Request #2

![right fit](images/investigation_2.png)


In a purchase approval system, I see _30 transactions refused out of 100_, why are they refused?

---

# Investigation Request #3

![right fit](images/investigation_3_1.png)

"We were supposed to see 8 shop locations on Google Maps but we see only 6. Why is that?"

---

# Investigation Request #3

![right fit](images/investigation_3_2.png)

"We were supposed to see 8 shop locations on Google Maps but we see only 6. Why is that?"

---

### No errors in the logs

![right fit](images/no_errors_logs_doubt.png)

^
To make things even more complicated, none of these situation results in error in our logs. So why don't things work as expected?

---

# The Promise of Functional Programming

### Focus on the _what_ rather than the _how_

[.code-highlight: none]
[.code-highlight: 1-6]
[.code-highlight: 1-10]

```scala 
// HOW (Procedural)
val x = List(1, 2, 3) 
val y = scala.collection.mutable.ArrayBuffer.empty[Int]
for (i <- 0 until x.size) {
  y += x(i) * 2
}

// WHAT (Functional)
val x = List(1, 2, 3)
val y = x.map(_ * 2)
``` 

^ FP promises to let us focus on intent rather than the internal mechanism, and it succeeds in cases like this. We can appreciate how the functional approach is clear and easy to understand. But sometimes we still miss this goal, for example when we model a solution for a simple technical problem, such as establishing the health state of a running service 

---

#### The Promise of FP in Business Logic Problems
Health Check


![right fit](images/healthcheck.png)

[.code-highlight: 1-4,36]
[.code-highlight: 1-12,36]
```scala 
/**
 * Check db connectivity
 */
def checkErrors(): ZIO[Transactor[Task] & AdminClient & SttpClient, Nothing, List[StatusError]] = {
  def dbCheck(
    dbType: DbType, 
    checkTables: List[TableName]
  ): ZIO[Transactor[Task], Nothing, List[StatusError]] =
    /*
     * Doobie code    
     */

  def kafkaCheck(
    topics: List[Topic]
  ): URIO[AdminClient, Option[StatusError]] =
    /*
     * ZIO-kafka code
     */

  def httpCheck(
    url: Url
  ): URIO[SttpClient, List[StatusError]] =
    /*
     * Sttp code  
     */

  ZIO
    .collectAll(
      List(
        dbCheck(DbType.MySql, List(TableName("table1"), TableName("table2"))),
        kafkaCheck(List(Topic("topic1"), Topic("topic2"))),
        httpCheck(Url("http://localhost:8080"))
      )
    )
    .map(_.flatten)
}
``` 

^ Here is a straightforward implementation for the healthcheck, One function checks the DB, one checks the kafka connection, and one checks the external http connection.
At the end, we collect the error messages produced by each of them into  a single list


---

#### The Promise of FP in Business Logic Problems
Health Check


![right fit](images/healthcheck.png)

```scala 
  def dbCheck(
    dbType: DbType, 
    checkTables: List[TableName]
  ): ZIO[Transactor[Task], Nothing, List[StatusError]] =
    /*
     * Doobie code    
     */
``` 

---

#### The Promise of FP in Business Logic Problems
Health Check


![right fit](images/healthcheck.png)


[.code-highlight: 1,10-15,33]
```scala 
def checkErrors(): ZIO[Transactor[Task] & AdminClient & SttpClient, Nothing, List[StatusError]] = {
  def dbCheck(
    dbType: DbType, 
    checkTables: List[TableName]
  ): ZIO[Transactor[Task], Nothing, List[StatusError]] =
    /*
     * Doobie code    
     */

  def kafkaCheck(
    topics: List[Topic]
  ): URIO[AdminClient, Option[StatusError]] =
    /*
     * ZIO-kafka code
     */

  def httpCheck(
    url: Url
  ): URIO[SttpClient, List[StatusError]] =
    /*
     * Sttp code  
     */

  ZIO
    .collectAll(
      List(
        dbCheck(DbType.MySql, List(TableName("table1"), TableName("table2"))),
        kafkaCheck(List(Topic("topic1"), Topic("topic2"))),
        httpCheck(Url("http://localhost:8080"))
      )
    )
    .map(_.flatten)
}
``` 

---

#### The Promise of FP in Business Logic Problems
Health Check


![right fit](images/healthcheck.png)

```scala 

  def kafkaCheck(
    topics: List[Topic]
  ): URIO[AdminClient, Option[StatusError]] =
    /*
     * ZIO-kafka code
     */
```

---

#### The Promise of FP in Business Logic Problems
Health Check


![right fit](images/healthcheck.png)

[.code-highlight: 1,16-22,33]
```scala 
def checkErrors(): ZIO[Transactor[Task] & AdminClient & SttpClient, Nothing, List[StatusError]] = {
  def dbCheck(
    dbType: DbType, 
    checkTables: List[TableName]
  ): ZIO[Transactor[Task], Nothing, List[StatusError]] =
    /*
     * Doobie code    
     */

  def kafkaCheck(
    topics: List[Topic]
  ): URIO[AdminClient, Option[StatusError]] =
    /*
     * ZIO-kafka code
     */

  def httpCheck(
    url: Url
  ): URIO[SttpClient, List[StatusError]] =
    /*
     * Sttp code  
     */

  ZIO
    .collectAll(
      List(
        dbCheck(DbType.MySql, List(TableName("table1"), TableName("table2"))),
        kafkaCheck(List(Topic("topic1"), Topic("topic2"))),
        httpCheck(Url("http://localhost:8080"))
      )
    )
    .map(_.flatten)
}
``` 

---

#### The Promise of FP in Business Logic Problems
Health Check


![right fit](images/healthcheck.png)

```scala

  def httpCheck(
    url: Url
  ): URIO[SttpClient, List[StatusError]] =
    /*
     * Sttp code  
     */
}
``` 

---

#### The Promise of FP in Business Logic Problems
Health Check


![right fit](images/healthcheck.png)

[.code-highlight: 1,23-32,33]
```scala 
def checkErrors(): ZIO[Transactor[Task] & AdminClient & SttpClient, Nothing, List[StatusError]] = {
  def dbCheck(
    dbType: DbType, 
    checkTables: List[TableName]
  ): ZIO[Transactor[Task], Nothing, List[StatusError]] =
    /*
     * Doobie code    
     */

  def kafkaCheck(
    topics: List[Topic]
  ): URIO[AdminClient, Option[StatusError]] =
    /*
     * ZIO-kafka code
     */

  def httpCheck(
    url: Url
  ): URIO[SttpClient, List[StatusError]] =
    /*
     * Sttp code  
     */

  ZIO
    .collectAll(
      List(
        dbCheck(DbType.MySql, List(TableName("table1"), TableName("table2"))),
        kafkaCheck(List(Topic("topic1"), Topic("topic2"))),
        httpCheck(Url("http://localhost:8080"))
      )
    )
    .map(_.flatten)
}
``` 

---

#### The Promise of FP in Business Logic Problems
Health Check

![right fit](images/healthcheck.png)

```scala 
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

---

#### The Promise of FP in Business Logic Problems
Health Check


![right fit](images/healthcheck.png)

```scala 
def checkErrors(): ZIO[Transactor[Task] & AdminClient & SttpClient, Nothing, List[StatusError]] = {
  def dbCheck(
    dbType: DbType, 
    checkTables: List[TableName]
  ): ZIO[Transactor[Task], Nothing, List[StatusError]] =
    /*
     * Doobie code    
     */

  def kafkaCheck(
    topics: List[Topic]
  ): URIO[AdminClient, Option[StatusError]] =
    /*
     * ZIO-kafka code
     */

  def httpCheck(
    url: Url
  ): URIO[SttpClient, List[StatusError]] =
    /*
     * Sttp code  
     */

  ZIO
    .collectAll(
      List(
        dbCheck(DbType.MySql, List(TableName("table1"), TableName("table2"))),
        kafkaCheck(List(Topic("topic1"), Topic("topic2"))),
        httpCheck(Url("http://localhost:8080"))
      )
    )
    .map(_.flatten)
}
```

---

### Purely functional code

- Immutable values
- Strongly typed - `neotype`
- Pure functional effect system - `ZIO`


^The code we just showed is purely functional code, written in a functional effect system, but to understand what is being done, the developer must check the implementation of these methods

---

### Purely functional code

Is it focused on the _how_ or on the _what_?

^
This code is Purely functional, but is it really focused on the what rather than the how? I think this suffers from some limitations

---

# Problem #1: No clarity of Intent

[.code-highlight: 0]
[.code-highlight: 1-5]
```scala
def dbCheck(dbType: DbType, checkTables: List[TableName]): ZIO[Transactor[Task], Nothing, List[StatusError]] = /*...*/

def kafkaCheck(topics: List[Topic]): URIO[AdminClient, Option[StatusError]] = /*...*/

def httpCheck(url: Url): URIO[SttpClient, List[StatusError]]  = /*...*/
```

^ What does this code do?
^ What does it mean to check a list of tables? Their existence? Their non-emptiness? Their read access? Or write access? Or both?
^ What does doing an http check mean? getting a 200 code back? Or just checking the connection?

---

# Problem #1: No clarity of Intent

Unconstrained functions

[.code-highlight: 1-6,9-11]
[.code-highlight: 1-7,9-11]
[.code-highlight: 1-8,9-11]
[.code-highlight: 1-13,9-11]

```scala
 def checkErrors() = ZIO
  .collectAll(
    List(
      dbCheck(DbType.MySql, List(TableName("table1"), TableName("table2"))),
      kafkaCheck(List(Topic("topic1"), Topic("topic2"))),
      httpCheck(Url("http://localhost:8080")),
      ZIO.succeed(List(StatusError("Random error")))
      ZIO.die(new RuntimeException("Error in the fifth check"))
    )
  )
  .map(_.flatten)
```

^On top of that, we are using unconstrained functions. Nothing prevents us from putting there one constant error (CLICK), or even letting the whole computation blow up at runtime(CLICK)

---

# Problem #2: Divergence Between Implementation and Documentation

```scala
/**
 * Check db connectivity
 */
  def checkErrors() =
    dbCheck(DbType.MySql, List(TableName("table1"), TableName("table2")))
```
^ this was the first version of our function, with a proper documentation

---


# Problem #2: Divergence Between Implementation and Documentation

[.code-highlight: 1-8,11-14]
[.code-highlight: 1-9,11-14]
[.code-highlight: 1-10,11-14]
```scala
/**
 * Check db connectivity
 */
def checkErrors() = ZIO
  .collectAll(
    List(
      dbCheck(DbType.MySql, List(TableName("table1"), TableName("table2"))),
      kafkaCheck(List(Topic("topic1"), Topic("topic2"))),
      httpCheck(Url("http://localhost:8080"))
    )
  )
  .map(_.flatten)
```

^
Later in time we kept adding functionality, but documentation remained the same
- Function was initially just checking the DB
- Later added Kafka check
- Then added HTTP check
- Documentation remained outdated

^ This is a common issue in complex systems - implementation evolves but documentation lags

---

# Problem #3: Solution Bound to ZIO

- Legacy services use `Future` and Slick
- No guarantee of equivalence between implementations

^ Changing tech stack means rewriting from scratch with no guarantee of correctness

---

# Orthogonality

### Address all three problems independently:
1. Make intent clear
2. Keep documentation _always_ aligned with code
3. Make solution stack-agnostic

^ Orthogonal is a language mutuated from Algebra/Geometry, where in the same way orthogonal vector or spaces don't possibly affect each other, here we mean the solving one problem should not affect the solution of the other ones

---

### Define a Language that describes the Problem

^The solution is: Start with a language that accurately describes the problem
---

# Model the problem - not the solution

```scala 
sealed trait ErrorCondition

object ErrorCondition { 
  
  case class DBErrorCondition(dbType: DbType, checkTables: List[TableName]) extends ErrorCondition 
  
  case class KafkaErrorCondition(topics: List[Topic]) extends ErrorCondition 
  
  case class HttpErrorCondition(url: Url) extends ErrorCondition 
}
``` 

^ We use algebraic data types to create a language that describes our problem domain

---

# Add Combinators

```scala 
sealed trait ErrorCondition

object ErrorCondition { 
  case class Or(left: ErrorCondition, right: ErrorCondition) extends ErrorCondition

  case class DBErrorCondition(dbType: DbType, checkTables: List[TableName]) extends ErrorCondition 

  case class KafkaErrorCondition(topics: List[Topic]) extends ErrorCondition 
  case class HttpErrorCondition(url: Url) extends ErrorCondition }
``` 

^ Now we can combine error conditions using logical operators. Notice we use an `||` because our system fails if one of the conditions is met

---

# Build data structures that model our problem

E.g. Error if there is a DB error _or_ an http error

```scala
Or(DBErrorCondition(DbType.Postgres, List(TableName("user", "access_token"))), HttpErrorCondition(Url("https://license-check.org/check")))
```

---

# Make it ergonomic

```scala
sealed trait ErrorCondition { self =>
  def ||(other: ErrorCondition): ErrorCondition = Or(self, other)
}
``` 

^ By adding this combinator and some constructors we can make the expression of an error condition much easier and constrained

---

# Make it ergonomic

```scala
val errorCondition =
  dbErrorCondition(dbType, TableName("user"), TableName("access_token")) ||
    getHttp2xx(Url("https://license-check.org"))
    
``` 

---

# Flexibility

```scala
val errorCondition =
  dbErrorCondition(dbType, TableName("user"), TableName("access_token")) ||
    getHttp2xx(Url("https://license-check.org")) ||
    kafkaErrorCondition("messages-topic")
    
``` 

---

# Make a `checkErrors()`

Interpret the data structure into a function 

```scala 
object ZIOInterpreter { 
  def checkErrors(errorCondition: ErrorCondition) = 
    
    errorCondition match
    
      case ErrorCondition.DBErrorCondition(DbType.Postgres, checkTables) =>
        DoobieZIOdBHealthcheck.status(DoobieZIOdBHealthcheck.existsPostgres, checkTables) 
    
      case ErrorCondition.DBErrorCondition(DbType.MySql, checkTables) => 
        DoobieZIOdBHealthcheck.status(DoobieZIOdBHealthcheck.existsMySql, checkTables) 
    
      case ErrorCondition.HttpErrorCondition(url) => 
        SttpHealthCheck.check(url)
    
      case ErrorCondition.Or(left, right) => 
        interpret(left).zipPar(interpret(right)).map { 
          case (leftErrors, rightErrors) => leftErrors ++ rightErrors 
        } 
}
``` 

^
Once we have the data structure definition in place, we need to make this runnable, and this is what goes by as _interpretation_. Interpretation is completely free: we have no constraints coming from our DLS about what we need to do with the data structure we built.
So if we use ZIO and Doobie, the interpreter that, given our health check definition returns the list of status errors, if any
Note that The compiler will help us ensure we've covered all cases.

---

# Interpreter: the bare minimum

```scala
sealed trait ErrorCondition

def checkErrors(ec: ErrorCondition): UIO[List[Errors]] =
  ec match {
    //cover all cases
  }
  
```

---

# Interpreter: optimize before running

```scala
sealed trait ErrorCondition

private def optimize(ec: ErrorCondition): Algebra = ???

def checkErrors(ec: ErrorCondition): UIO[List[Errors]] =
  optimize(ec) match {
    //cover all cases
  }
  
```

^
one possible optimization in our case would be to traverse the data structure, collect all the conditions of the same type in a monoidal way and run just one call to the targeted platform 

---
# Benefits: Clarity of Intent


```scala 
val errorCondition =
  dbErrorCondition(dbType, TableName("user"), TableName("access_token")) ||
    getHttp2xx(Url("https://license-check.org")) ||
    kafkaErrorCondition("messages-topic")

``` 

TODO: Make it even better with some constructors such as `checkDb(type).tablesExist(tables...)`

^ This reads almost like natural language, making the intent clear. Our DSL is now clearly conveying what we want to do:

---

# Benefits: Technology Independence

For a Future/Slick implementation:

```scala 
  def checkErrors(
    db: slick.jdbc.JdbcBackend#Database, 
    jdbcProfile: slick.jdbc.JdbcProfile, 
    httpClient: SttpFutureBackend[Future, Any]
  )(
    errorCondition: ErrorCondition )(implicit ec: ExecutionContext): Future[List[StatusError]] = { 
      // Different implementation, same logic 
    }
``` 

^ We can create different interpreters for different tech stacks

---

# Benefits: Documentation from Code

```scala 
def interpret(errorCondition: ErrorCondition): String = ...
``` 

Output:

```
Either of:
Database Check (Postgres):
  Required tables: customer, access_token
OR
Kafka Check:
  Required topics: events
OR
HTTP Check:
  URL: http://localhost:8080/status/200
are detected for failure
``` 

^ Generate documentation directly from the code structure

---

# Evolution of the Language

New requirement? -> New term in the algebra

```scala 
case class RabbitMQErrorCondition(exchanges: List[Exchange]) extends ErrorCondition
def rabbitMQErrorCondition(topics: Exchange*): ErrorCondition = RabbitMQErrorCondition(topics.toList)
``` 

^ The language evolves as our requirements evolve

---

# Compiler-Enforced Consistency
```
[warn] 17 |    errorCondition match
[warn]    |    ^^^^^^^^^^^^^^
[warn]    |match may not be exhaustive.
[warn]    |
[warn]    |It would fail on pattern case: domainmodeling.healthcheck.ErrorCondition.RabbitMQErrorCondition(_)
``` 

^ The compiler tells us when our interpreters need updating

---

# A More Complex Example
## Payment Authorization Rules

The problem: Authorizing payments based on complex, evolving rules
```
scala def isBlocked(creditCard: CreditCard, purchase: Purchase): Boolean
``` 

^ Let's look at a more complex real-world example

---

# Domain Model
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

^ Our domain contains credit cards, purchases, and shops

---

# Payment Approval system

```scala
def isBlocked(creditCard: CreditCard, purchase: Purchase): Boolean
```

---

# First Requirement

> _"Block all electronic purchases in China"_

Naive implementation:

```scala
 def isBlocked(creditCard: CreditCard, purchase: Purchase):  Boolean =
  purchase.shop.country == Country.China && purchase.shop.categories.contains(ShopCategory.Electronics))
```

^ But our PO told us that this rule would become more complex over time

---

# Building the basic rules

```scala
sealed trait BlockingRule

case class PurchaseOccursInCountry(country: Country)                  extends BlockingRule
case class PurchaseCategoryEquals(purchaseCategory: ShopCategory) extends BlockingRule
case class PurchaseAmountExceeds(amount: Double)                      extends BlockingRule
case class FraudProbabilityExceeds(threshold: Probability)            extends BlockingRule
case class CreditCardFlagged()                                        extends BlockingRule
case class ShopIsBlacklisted()                                        extends BlockingRule
```

^ We define the base cases for our blocking rules

---

# Adding Combinators

```scala
sealed trait BlockingRule { self =>
    def &&(other: BlockingRule): BlockingRule = BlockingRule.And(self, other)
    def ||(other: BlockingRule): BlockingRule = BlockingRule.Or(self, other)
}
```

^ We add logical operators to combine rules

---

# User-Friendly Constructors

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

^ Helper methods make our DSL read naturally

---

# Rule Interpreter

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

^ Our interpreter evaluates the rule against actual services

---

# Documentation from code: Mermaid

In such a  case a graph is better than a long Confluence page

```
scala def toMermaidCode(blockingRule: BlockingRule): UIO[String] = // ...
``` 

^ We can also interpret our rules as visual diagrams

---

# Rule Evolution: V1

```scala
val br1 = 
  purchaseCountryIsOneOf(Country.China) && 
    purchaseCategoryIsOneOf(ShopCategory.Electronics)
```

![right fit](images/rule_v1.png)

^ Our first rule blocks electronics purchases in China
After a while our PO comes with a new requirement, we want to block also gambling transactions in UK

---

# Rule Evolution: V2

```scala
val br1 =
  purchaseCountryIsOneOf(Country.China) &&
    purchaseCategoryIsOneOf(ShopCategory.Electronics)
    
val br2 = br1 || 
  (purchaseCountryIsOneOf(Country.UK) && 
    purchaseCategoryIsOneOf(ShopCategory.Gambling))
```

![right fit](images/rule_v2.png)

^ Now we also block gambling purchases in UK
he third evolution of our rule will be more complex, and it will combine country, category, amount and a fraud probability threshold

---

# Rule Evolution: V3

```scala
val br1 =
  purchaseCountryIsOneOf(Country.China) &&
    purchaseCategoryIsOneOf(ShopCategory.Electronics)

val br2 = br1 ||
  (purchaseCountryIsOneOf(Country.UK) &&
    purchaseCategoryIsOneOf(ShopCategory.Gambling))

val br3 = br2 || 
  (
    purchaseCountryIsOneOf(Country.Italy) &&
      purchaseCategoryIsOneOf(
        ShopCategory.Gambling,
        ShopCategory.Adult
      ) && 
      purchaseAmountExceeds(1000) && 
      fraudProbabilityExceeds(Probability(0.8))
  )
```
![right fit](images/rule_v3.png)


^ Our third version adds complex rules for Italy

---

# Benefits
- **Clear Intent**: Rules express what we want, not how to calculate it
- **Self-Documenting**: Visual representation directly from code
- **Technology Independent**: Same rules, different implementations
- **Evolving Safely**: Compiler catches missing cases
- **Optimization**: We can transform the rule tree before execution

^ These are the key advantages of treating programs as values

---

# ...and there is more!
Not only algorithmic problems:

- ETL processing pipelines
- Workflows
- Authorization rules
- API requests _(Tapir, ZIO-http)_
- Domain-specific calculations

^ This approach works for many different problem domains

---

# Scala featues shine...
- Compiler exhaustive checks
- Phantom types to make unwanted constructions impossible

---

# ...but all you need is data
- Languages with good sum and product types
- Pattern matching (data de-construction)
- And capability to add methods to types
 
---

# Keep Things Simple
- No need for higher-kinded types (HKTs)
- Restrict operations to a limited set
- Avoid arbitrary functions or map/flatMap
- (Free) monads are necessary at low level

^ Simplicity is a feature, not a limitation

---

#Thank You!

Questions?


^
TODO's:
mention Runar's talk
Introduce a conservation principle: Expressivity + Reasonability = Constant. The more I can express with a language, the more difficult it is to reason about what I'm doing with that language.