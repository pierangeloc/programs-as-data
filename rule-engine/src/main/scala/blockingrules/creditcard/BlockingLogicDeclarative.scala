package blockingrules.creditcard

import blockingrules.creditcard.BlockingLogicDeclarative.MermaidInterpreter.{MermaidRenderable, Shape}
import blockingrules.creditcard.BlockingLogicDeclarative.MermaidInterpreter.MermaidRenderable.Style
import blockingrules.creditcard.BlockingLogicDeclarative.{DSLExamples, MermaidInterpreter}
import blockingrules.creditcard.model.{CreditCard, Purchase}
import blockingrules.creditcard.model.basetypes.{CardNumber, Country, Probability, PurchaseCategory}
import cats.Show
import datastructures.Tree
import neotype.*
import zio.*

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.zip.GZIPOutputStream

object BlockingLogicDeclarative {

  sealed trait BlockingRule { self =>
    def &&(other: BlockingRule): BlockingRule = BlockingRule.And(self, other)
    def ||(other: BlockingRule): BlockingRule = BlockingRule.Or(self, other)
  }

  object BlockingRule {
    case class And(l: BlockingRule, r: BlockingRule) extends BlockingRule
    case class Or(l: BlockingRule, r: BlockingRule)  extends BlockingRule

    case class PurchaseOccursInCountry(country: Country)                  extends BlockingRule
    case class PurchaseCategoryEquals(purchaseCategory: PurchaseCategory) extends BlockingRule
    case class PurchaseAmountExceeds(amount: Double)                      extends BlockingRule
    case class FraudProbabilityExceeds(threshold: Probability)            extends BlockingRule
    case class CreditCardFlagged()                                        extends BlockingRule
  }

  object DSL {
    def purchaseInCountry(country: Country): BlockingRule           = BlockingRule.PurchaseOccursInCountry(country)
    def purchaseInOneOfCountries(countries: Country*): BlockingRule = countries.map(purchaseInCountry).reduce(_ || _)
    def purchaseCategoryEquals(purchaseCategory: PurchaseCategory): BlockingRule =
      BlockingRule.PurchaseCategoryEquals(purchaseCategory)
    def purchaseCategoryIsOneOf(purchaseCategories: PurchaseCategory*): BlockingRule =
      purchaseCategories.map(purchaseCategoryEquals).reduce(_ || _)
    def purchaseAmountExceeds(amount: Double): BlockingRule           = BlockingRule.PurchaseAmountExceeds(amount)
    def fraudProbabilityExceeds(threshold: Probability): BlockingRule = BlockingRule.FraudProbabilityExceeds(threshold)
    def creditCardFlagged: BlockingRule                               = BlockingRule.CreditCardFlagged()
  }


  /**
   * With a bit of extra boilerplate, we defined a rigorous set of constructors
   * and combinators that we can use to express all the possible complicated
   * rules that our business counterparts might require
   */
  object LiveRuleEvaluator {
    case class Input(
      creditCard: CreditCard,
      purchase: Purchase
    )

    private def purchaseOccursInCountry(rule: BlockingRule.PurchaseOccursInCountry)(input: Input): UIO[Boolean] =
      ZIO.succeed(input.purchase.inCountry == rule.country)

    private def purchaseCategoryEquals(rule: BlockingRule.PurchaseCategoryEquals)(input: Input): UIO[Boolean] =
      ZIO.succeed(input.purchase.category == rule.purchaseCategory)

    private def purchaseAmountExceeds(rule: BlockingRule.PurchaseAmountExceeds)(input: Input): UIO[Boolean] =
      ZIO.succeed(input.purchase.amount.unwrap > rule.amount)

    private def creditCardFlagged(ccFlaggedService: CreditCardFlaggedService)(input: Input): UIO[Boolean] =
      ccFlaggedService.isFlagged(input.creditCard.cardNumber)

    private def fraudProbability(rule: BlockingRule.FraudProbabilityExceeds, fraudScoreService: FraudScoreService)(
      input: Input
    ): UIO[Boolean] =
      fraudScoreService
        .getFraudScore(input.creditCard, input.purchase)
        .map(score => score.unwrap > rule.threshold.unwrap)

    def evaluate(rule: BlockingRule, ccFlaggedService: CreditCardFlaggedService, fraudScoreService: FraudScoreService)(
      input: Input
    ): UIO[Boolean] = {
      def eval(rule: BlockingRule): UIO[Boolean] = rule match {
        case BlockingRule.PurchaseOccursInCountry(country) =>
          purchaseOccursInCountry(BlockingRule.PurchaseOccursInCountry(country))(input)
        case BlockingRule.PurchaseCategoryEquals(purchaseCategory) =>
          purchaseCategoryEquals(BlockingRule.PurchaseCategoryEquals(purchaseCategory))(input)
        case BlockingRule.PurchaseAmountExceeds(amount) =>
          purchaseAmountExceeds(BlockingRule.PurchaseAmountExceeds(amount))(input)
        case BlockingRule.CreditCardFlagged() => creditCardFlagged(ccFlaggedService)(input)
        case BlockingRule.FraudProbabilityExceeds(threshold) =>
          fraudProbability(BlockingRule.FraudProbabilityExceeds(threshold), fraudScoreService)(input)
        case BlockingRule.And(l, r) => eval(l).zipWith(eval(r))(_ && _)
        case BlockingRule.Or(l, r)  => eval(l).zipWith(eval(r))(_ || _)
      }

      eval(rule)
    }
  }

  object MermaidInterpreter {

    enum Shape {
      case Square
      case RoundedSquare
      case Circle
      case Bang
      case Cloud
      case Hexagon
      case Default
    }

    object Shape {
      def renderLabel(l: String, shape: Shape): String = shape match {
        case Shape.Square        => s"[$l]"
        case Shape.RoundedSquare => s"($l)"
        case Shape.Circle        => s"(($l))"
        case Shape.Bang          => s"))$l(("
        case Shape.Cloud         => s")$l("
        case Shape.Hexagon       => s"{{$l}}"
        case Shape.Default       => l
      }
    }

    trait MermaidRenderable[A] {
      extension (a: A) def mermaidRender: MermaidRenderable.Render
    }
    object MermaidRenderable {
      enum Style { self =>
        case Default
        case Or
        case And

        def toStyleString: String = self match {
          case Style.Default => ""
          case Style.Or      => "fill:#b36,stroke:#666,stroke-width:4px"
          case Style.And     => "fill:#693,stroke:#666,stroke-width:4px"
//          case Style.And  => "fill:#63b,stroke:#f66,stroke-width:2px,color:#fff,stroke-dasharray: 5 5"
        }
      }

      case class Render(text: String, style: Style = Style.Default)



      given [A](using ra: MermaidRenderable[A]): MermaidRenderable[Labelled[A]] with {
        extension (la: Labelled[A])
          def mermaidRender: MermaidRenderable.Render = {
            val renderA = ra.mermaidRender(la.a)
            MermaidRenderable.Render(
              text = s"${la.label}${renderA.text}",
              style = renderA.style
            )
          }

      }
    }

    def makeTree(rule: BlockingRule): Tree[BlockingRule] = rule match {
      case BlockingRule.And(l, r) => Tree(rule, List(makeTree(l), makeTree(r)))
      case BlockingRule.Or(l, r)  => Tree(rule, List(makeTree(l), makeTree(r)))
      case _                      => Tree(rule, List.empty)

    }

    case class Labelled[A](a: A, label: Int)

    def label(blockingRule: BlockingRule): UIO[Tree[Labelled[BlockingRule]]] = {
      val tree = makeTree(blockingRule)
      def canCollapse(br1: BlockingRule, br2: BlockingRule) = (br1, br2) match
        case (BlockingRule.And(_, _), BlockingRule.And(_, _)) => true
        case (BlockingRule.Or(_, _), BlockingRule.Or(_, _))   => true
        case _                                                => false

      val collapsed = Tree.collapse(tree, canCollapse)
      for {
        counter      <- zio.Ref.make(0)
        labelledTree <- collapsed.mapZIO(br => counter.getAndUpdate(_ + 1).map(label => Labelled(br, label)))
      } yield labelledTree
    }


    def toMermaidCode[A: MermaidRenderable](tree: Tree[Labelled[A]]): UIO[String] = {
      import MermaidRenderable.given

      for {
        subtrees <- ZIO.succeed(Tree.traverseSubtrees(tree))
        code = subtrees.flatMap { subtree =>
                 subtree.node.mermaidRender.text ::
                   (if (subtree.node.mermaidRender.style != Style.Default)
                      s"style ${subtree.node.label} ${subtree.node.mermaidRender.style.toStyleString}"
                    else "") ::
                   subtree.children.map(child => s"${subtree.node.label} --> ${child.node.label}")
               }.mkString("\n")
      } yield s"""flowchart LR
                 |${code}""".stripMargin
    }

    // this for some reason don't work, probably streams get closed prematurely
    def mermaidLink(mermaidCode: String): UIO[String] = {
      val escapedCode   = mermaidCode.replace("\"", "\\\"").replace("\n", "\\n")
      val mermaidGraph  = s"""{"code": "$escapedCode"}", "mermaid": {"theme": "default"} }"""
      val inflatedBytes = mermaidGraph.getBytes(StandardCharsets.UTF_8)
      println(s"inflated bytes: ${inflatedBytes.length}")
      ZIO.scoped {
        ZIO
          .acquireRelease(for {
            byteOutputStream <- ZIO.succeed(new ByteArrayOutputStream())
            zipOutputStream  <- ZIO.succeed(new GZIPOutputStream(byteOutputStream))
          } yield (byteOutputStream, zipOutputStream))((byteOutputStream, zipOutputStream) =>
            ZIO.attempt {
              println("Closing streams")
//          byteOutputStream.close()
              zipOutputStream.close()
              println("closed streams")
            }.orDie
          )
          .flatMap { case (byteOS, zipOS) =>
            for {
              _          <- ZIO.logInfo(s"Original MermaidGraph: $mermaidGraph")
              _          <- ZIO.succeed(zipOS.write(inflatedBytes))
              _          <- ZIO.logInfo("Written bytes into zipstream")
              deflated   <- ZIO.succeed(byteOS.toByteArray)
              _          <- ZIO.logInfo(s"Extracted bytes from underlying bytestream: ${deflated.length}")
              encoded     = new String(Base64.getEncoder.encode(deflated), StandardCharsets.UTF_8)
              _          <- ZIO.logInfo(s"Encoded bytes B64: $encoded")
              mermaidLink = s"https://mermaid.live/edit#peko:$encoded"
            } yield mermaidLink

          }
      }

    }

    def mermaidLink2(mermaidCode: String): String = {
      val escapedCode  = mermaidCode.replace("\"", "\\\"").replace("\n", "\\n")
      val mermaidGraph = s"""{"code": "$escapedCode", "mermaid": {"theme": "default"} }"""
      println(s"mermaidGraph:\n$mermaidGraph\n")
      val inflatedBytes = mermaidGraph.getBytes(StandardCharsets.UTF_8)
      println(s"inflated bytes: ${inflatedBytes.length}")
      val byteOutputStream = new ByteArrayOutputStream()
      val zipOutputStream  = new GZIPOutputStream(byteOutputStream)
      zipOutputStream.write(inflatedBytes)
      zipOutputStream.close()
      val deflated = byteOutputStream.toByteArray
      println(s"Extracted bytes from underlying bytestream: ${deflated.length}")
      val encoded = new String(Base64.getEncoder.encode(deflated), StandardCharsets.UTF_8)
      println(s"Encoded bytes B64: $encoded")
      val mermaidLink = s"https://mermaid.live/edit#pako:$encoded"
      mermaidLink

    }

  }

  object DSLExamples {
    import DSL.*
    val example1 = (purchaseInCountry(Country.UK) && purchaseCategoryEquals(PurchaseCategory.Crypto)) ||
      (purchaseInCountry(Country.China) && purchaseCategoryEquals(PurchaseCategory.Electronics)) ||
      (purchaseInCountry(Country.Italy) && purchaseCategoryEquals(PurchaseCategory.Weapons) && purchaseAmountExceeds(
        1000
      )) ||
      ((purchaseInCountry(Country.Netherlands) && purchaseAmountExceeds(500)) || (purchaseInCountry(
        Country.US
      ) && purchaseAmountExceeds(1000))) && fraudProbabilityExceeds(Probability(0.8))
  }


  object Instances:
    given MermaidRenderable[BlockingRule] with {
      extension (br: BlockingRule)
        def mermaidRender: MermaidRenderable.Render = br match
          case BlockingRule.And(_, _) =>
            MermaidRenderable.Render(Shape.renderLabel(""""AND"""", Shape.Hexagon), Style.And)
          case BlockingRule.Or(_, _) =>
            MermaidRenderable.Render(Shape.renderLabel(""""OR"""", Shape.Hexagon), Style.Or)
          case BlockingRule.PurchaseOccursInCountry(country) =>
            MermaidRenderable.Render(Shape.renderLabel(s""""In Country: ${country.unwrap}"""", Shape.RoundedSquare))
          case BlockingRule.PurchaseCategoryEquals(purchaseCategory) =>
            MermaidRenderable.Render(
              Shape.renderLabel(s""""Category: ${purchaseCategory.toString}"""", Shape.RoundedSquare)
            )
          case BlockingRule.PurchaseAmountExceeds(amount) =>
            MermaidRenderable.Render(Shape.renderLabel(s""""Amount > ${amount}"""", Shape.RoundedSquare))
          case BlockingRule.FraudProbabilityExceeds(threshold) =>
            MermaidRenderable.Render(Shape.renderLabel(s""""P[Fraud] > ${threshold.unwrap}"""", Shape.RoundedSquare))
          case BlockingRule.CreditCardFlagged() =>
            MermaidRenderable.Render(Shape.renderLabel(s""""CC Flagged"""", Shape.RoundedSquare))
    }
}

object MermaidExample extends ZIOAppDefault {

  import BlockingLogicDeclarative.Instances.given
  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    for {
      labelledTree <- MermaidInterpreter.label(DSLExamples.example1)
      mermaidCode  <- blockingrules.creditcard.BlockingLogicDeclarative.MermaidInterpreter.toMermaidCode(labelledTree)
      _            <- zio.Console.printLine("mermaid code: \n\n" + mermaidCode)
      mermaidLink   = blockingrules.creditcard.BlockingLogicDeclarative.MermaidInterpreter.mermaidLink2(mermaidCode)
      _            <- zio.Console.printLine("mermaid link: \n\n" + mermaidLink)
    } yield ()

}
