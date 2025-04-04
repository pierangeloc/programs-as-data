package datastructures

import zio.{Scope, ZIO, ZIOAppArgs, ZIOAppDefault}


////////////////// 
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
