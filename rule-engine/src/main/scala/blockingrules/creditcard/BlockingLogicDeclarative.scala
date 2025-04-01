package blockingrules.creditcard

import blockingrules.creditcard.BlockingLogicDeclarative.Tree
import blockingrules.creditcard.model.{CreditCard, Purchase}
import blockingrules.creditcard.model.basetypes.{CardNumber, Country, Probability, PurchaseCategory}
import cats.Show
import neotype.*
import zio.*

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
   * With a bit of extra boilerplate, we defined a rigourous set of constructors
   * and combinators that we can use to express all the possible complicated
   * rules that our business counterparts might require
   */

  object LiveRuleEvaluator {
    case class Input(
      creditCard: CreditCard,
      purchase: Purchase
    )

    def purchaseOccursInCountry(rule: BlockingRule.PurchaseOccursInCountry)(input: Input): UIO[Boolean] =
      ZIO.succeed(input.purchase.inCountry == rule.country)

    def purchaseCategoryEquals(rule: BlockingRule.PurchaseCategoryEquals)(input: Input): UIO[Boolean] =
      ZIO.succeed(input.purchase.category == rule.purchaseCategory)

    def purchaseAmountExceeds(rule: BlockingRule.PurchaseAmountExceeds)(input: Input): UIO[Boolean] =
      ZIO.succeed(input.purchase.amount.unwrap > rule.amount)

    def creditCardFlagged(ccFlaggedService: CreditCardFlaggedService)(input: Input): UIO[Boolean] =
      ccFlaggedService.isFlagged(input.creditCard.cardNumber)

    def fraudProbability(rule: BlockingRule.FraudProbabilityExceeds, fraudScoreService: FraudScoreService)(
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

  case class Tree[+T](node: T, children: List[Tree[T]]) { self =>

    def isLeaf: Boolean            = children.isEmpty
    def map[S](f: T => S): Tree[S] = Tree[S](f(self.node), self.children.map(_.map(f)))

    def mapZIO[S](f: T => UIO[S]): UIO[Tree[S]] = self match {
      case Tree(n, cs) =>
        for {
          fn <- f(n)
          fs <- ZIO.foreach(cs)(c => c.mapZIO(f))
        } yield Tree(fn, fs)
    }
  }

  object Tree {

    def leaf[T](node: T): Tree[T] = Tree(node, List.empty)

    def traverseSubtrees[T](tree: Tree[T]): List[Tree[T]] =
      if (tree.children.isEmpty) List(tree)
      else tree :: tree.children.flatMap(traverseSubtrees)

    /*
      I want a function that given a tree and a function collapseNodes(main: T, child: T): T gives me another tree where the children of the child tree
      become children of the main, after being collapsed as well
     */
    def collapse[T](tree: Tree[T], collapseNodes: (T, T) => Boolean): Tree[T] = {
      val childrenWithCollapsable = tree.children.map(child => (child, collapseNodes(tree.node, child.node)))
      val newChildren: List[Tree[T]] = childrenWithCollapsable.flatMap {
        case (child, true)  => collapse(child, collapseNodes).children
        case (child, false) => List(collapse(child, collapseNodes))
      }
      Tree(tree.node, newChildren)
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
      case class Render(text: String, style: String = "")

      given MermaidRenderable[BlockingRule] with {
        extension (br: BlockingRule)
          def mermaidRender: MermaidRenderable.Render = br match
            case BlockingRule.And(_, _) => MermaidRenderable.Render(Shape.renderLabel("AND", Shape.Hexagon))
            case BlockingRule.Or(_, _)  => MermaidRenderable.Render(Shape.renderLabel("OR", Shape.Hexagon))
            case BlockingRule.PurchaseOccursInCountry(country) =>
              MermaidRenderable.Render(Shape.renderLabel(s"In Country: ${country.unwrap}", Shape.RoundedSquare))
            case BlockingRule.PurchaseCategoryEquals(purchaseCategory) =>
              MermaidRenderable.Render(
                Shape.renderLabel(s"Category: ${purchaseCategory.toString}", Shape.RoundedSquare)
              )
            case BlockingRule.PurchaseAmountExceeds(amount) =>
              MermaidRenderable.Render(Shape.renderLabel(s"Amount > ${amount}", Shape.RoundedSquare))
            case BlockingRule.FraudProbabilityExceeds(threshold) =>
              MermaidRenderable.Render(Shape.renderLabel(s"P[Fraud] > ${threshold.unwrap}", Shape.RoundedSquare))
            case BlockingRule.CreditCardFlagged() =>
              MermaidRenderable.Render(Shape.renderLabel(s"CC Flagged", Shape.RoundedSquare))
      }

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

    def isNode(b: BlockingRule) = b match {
      case BlockingRule.Or(_, _)  => true
      case BlockingRule.And(_, _) => true
      case _                      => false
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

    import MermaidRenderable.given
    summon[MermaidRenderable[BlockingRule]]
    summon[MermaidRenderable[Labelled[BlockingRule]]].mermaidRender
    def toMermaidCode[A: MermaidRenderable](tree: Tree[Labelled[A]]): UIO[String] =
      for {
        subtrees <- ZIO.succeed(Tree.traverseSubtrees(tree))
        code = subtrees.flatMap { subtree =>
                 subtree.node.mermaidRender.text ::
                   subtree.children.map(child => s"${subtree.node.label} --> ${child.node.label}")
               }.mkString("\n")
      } yield code

  }

}

object TreeExample extends ZIOAppDefault {
  sealed trait Operation

  object Operation {
    case object Addition          extends Operation
    case object Multiplication    extends Operation
    case class Number(value: Int) extends Operation
  }

  import Operation.*
  val arithmetics: Tree[Operation] = Tree(
    Addition,
    List(
      Tree.leaf(Number(4)),
      Tree(
        Addition,
        List(
          Tree.leaf(Number(2)),
          Tree(
            Multiplication,
            List(
              Tree.leaf(Number(3)),
              Tree(
                Addition,
                List(Tree.leaf(Number(4)), Tree(Addition, List(Tree.leaf(Number(6)), Tree.leaf(Number(8)))))
              )
            )
          )
        )
      )
    )
  )

  def collapse(x: Operation, y: Operation) = (x, y) match {
    case (Operation.Addition, Operation.Addition)             => true
    case (Operation.Multiplication, Operation.Multiplication) => true
    case _                                                    => false
  }

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    zio.Console.printLine(s"original: $arithmetics") *>
      zio.Console.printLine(s"Collapsed: ${Tree.collapse(arithmetics, collapse)}") *> {

        for {
          counter      <- zio.Ref.make(0)
          labelledTree <- arithmetics.mapZIO(o => counter.getAndUpdate(_ + 1).map(nr => (o, nr)))
          _            <- zio.Console.printLine(s"original with labels: $labelledTree")
        } yield ()
      }

}
