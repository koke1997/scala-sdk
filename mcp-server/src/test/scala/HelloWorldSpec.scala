object HelloWorldSpec extends org.scalatest.flatspec.AnyFlatSpec {
  "HelloWorld" should "return the correct greeting" in {
    assert(HelloWorld.greet() == "Hello, World!")
  }
}