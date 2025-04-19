package blockingrules.creditcard.declarative

import blockingrules.creditcard.declarative
import zio.{Scope, ZIO, ZIOAppArgs, ZIOAppDefault}

object ExampleApp extends ZIOAppDefault {

  import MermaidInterpreter.Instances.given
  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    for {
      labelledTree <- MermaidInterpreter.label(DSLExamples.blockingRule1)
      mermaidCode  <- MermaidInterpreter.toMermaidCode(labelledTree)
      _            <- zio.Console.printLine("mermaid code: \n\n" + mermaidCode)
      mermaidLink   = MermaidInterpreter.mermaidLink2(mermaidCode)
      _            <- zio.Console.printLine("mermaid link: \n\n" + mermaidLink)
    } yield ()

}
