package domainmodeling.healthcheck

object SimpleCmdCheck:
  
  //create a main method (we are in Scala 3). Just println a message. Use annotation to determine the main function
  
    @main def fn(args: String*): Unit = 
        println("Hello, world!")
  
