# Functional and Reactive Programming

## Get the project up and running

### Prerequisites
- Install [IntelliJ IDEA](https://www.jetbrains.com/idea/download/)
- Install [Scala 3.3](https://www.scala-lang.org/download/) or the Scala plugin for IntelliJ IDEA

### Running the project
- Open the project in IntelliJ IDEA
- Run the `Main` class of your chosen module
- The output will be displayed in the console

### Running the tests
- Open the project in IntelliJ IDEA
- Update sbt dependencies by reloading all sbt projects  (In IntelliJ IDEA, click on the `sbt` tab on the right side of the window and click on the refresh icon)
- Open the test folder
- Right-click on the `scala` folder and select `Run ScalaTests in 'scala'`
- To generate a test report, check the `use sbt` checkbox in the test configuration

You can also run the tests using the following command in the terminal, if you have sbt installed locally:

```bash
sbt test
```

Caution: The tests may not be exhaustive and are only meant to demonstrate the basic functionality of the code based on the exercise sheet.