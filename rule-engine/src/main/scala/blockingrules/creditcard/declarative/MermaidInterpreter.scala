package blockingrules.creditcard.declarative

import blockingrules.creditcard.declarative.BlockingLogicDeclarative.BlockingRule
import blockingrules.creditcard.declarative.MermaidInterpreter.MermaidRenderable.Style
import datastructures.Tree
import neotype.*
import zio.{UIO, ZIO}

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.zip.GZIPOutputStream

object MermaidInterpreter {

  object MermaidCode extends Newtype[String]
  type MermaidCode = MermaidCode.Type

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
          case blockingrules.creditcard.declarative.BlockingLogicDeclarative.BlockingRule.ShopIsBlacklisted() =>
            MermaidRenderable.Render(Shape.renderLabel(s""""Shop blacklisted"""", Shape.RoundedSquare))
    }

  def makeTree(rule: BlockingRule): Tree[BlockingRule] = rule match {
    case BlockingRule.And(l, r) => Tree(rule, List(makeTree(l), makeTree(r)))
    case BlockingRule.Or(l, r)  => Tree(rule, List(makeTree(l), makeTree(r)))
    case _                      => Tree(rule, List.empty)

  }

  case class Labelled[A](a: A, label: Int)

  private def label(blockingRule: BlockingRule): UIO[Tree[Labelled[BlockingRule]]] = {
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

  def toMermaidCode(blockingRule: BlockingRule): UIO[MermaidCode] = {
    import MermaidInterpreter.Instances.given
    for {
      labelledTree <- label(blockingRule)
      mermaidCode  <- labelledToMermaidCode(labelledTree)
    } yield MermaidCode(mermaidCode)
  }

  private def labelledToMermaidCode[A: MermaidRenderable](tree: Tree[Labelled[A]]): UIO[String] = {
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
  def mermaidLinkNotWorking(mermaidCode: String): UIO[String] = {
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

  def mermaidLink(mermaidCode: MermaidCode): String = {
    val escapedCode  = mermaidCode.unwrap.replace("\"", "\\\"").replace("\n", "\\n")
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
