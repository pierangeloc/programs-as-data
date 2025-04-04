package datastructures

import zio.{UIO, ZIO}

/**
 * A Simple Tree with map function and mapZIO for stateful transformations
 */

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

  /**
   * Produces a list of subtrees obtained by visiting the nodes in a depth-first fashion
   */
  def traverseSubtrees[T](tree: Tree[T]): List[Tree[T]] =
    if (tree.children.isEmpty) List(tree)
    else tree :: tree.children.flatMap(traverseSubtrees)

  /**
   * Collapses adjacent nodes of a tree into the parent, provided the nodes satisfies a given condition
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
