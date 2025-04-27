package blockingrules.creditcard.declarative

import zio.{Scope, ZIO, ZIOAppArgs, ZIOAppDefault}

object ExampleApp extends ZIOAppDefault {

  import MermaidInterpreter.Instances.given
  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    for {
//      mermaidCode <- MermaidInterpreter.toMermaidCode(DSLExamples.blockingRule1)
      mermaidCode <- MermaidInterpreter.toMermaidCode(DSLExamples.Evolution.br1)
      _            <- zio.Console.printLine("mermaid code: \n\n" + mermaidCode)
      mermaidLink   = MermaidInterpreter.mermaidLink(mermaidCode)
      _            <- zio.Console.printLine("mermaid link: \n\n" + mermaidLink)
    } yield ()

}
