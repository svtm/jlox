# jlox
My implementation of jlox, a Lox interpreter written in Java. Implemented after following the book http://www.craftinginterpreters.com

### Project status
Completed chapter 12

### Extra features:
* Break out of loops with ```break;```
* Unary prefix and postfix increment/decrement operators (```i++, ++i, i--, --i```)
* Multiline/inline comments with ```/* this is a comment */```
* Native variadic functions:
  * ```print(*args)``` prints space separated ```args```
  * ```prompt(*args)``` presents user with text in ```args``` and reads user input (like Python's ```input()```)
* Lambda functions
* Ternary conditional operator
* Static methods (denoted by keyword ```class``` in before method name)
* Class getter-methods (denoted by method without parens in class declaration)
* lists/arrays