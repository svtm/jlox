package com.enielsen.lox;

import java.util.List;

abstract class Expr {
  interface Visitor<R> {
    R visitAssignExpr(Assign expr);
    R visitConditionalExpr(Conditional expr);
    R visitBinaryExpr(Binary expr);
    R visitCallExpr(Call expr);
    R visitArrayExpr(Array expr);
    R visitGetExpr(Get expr);
    R visitIndexGetExpr(IndexGet expr);
    R visitIndexSetExpr(IndexSet expr);
    R visitGroupingExpr(Grouping expr);
    R visitLiteralExpr(Literal expr);
    R visitLogicalExpr(Logical expr);
    R visitSetExpr(Set expr);
    R visitThisExpr(This expr);
    R visitSuperExpr(Super expr);
    R visitUnaryExpr(Unary expr);
    R visitVariableExpr(Variable expr);
    R visitFunctionExpr(Function expr);
  }

  static class Assign extends Expr {
    Assign(Token name, Expr value) {
      this.name = name;
      this.value = value;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitAssignExpr(this);
    }

    @Override
    public String toString() {
      return "AssignExpr";
    }

    final Token name;
    final Expr value;
  }

  static class Conditional extends Expr {
    Conditional(Expr condition, Expr thenBranch, Expr elseBranch) {
      this.condition = condition;
      this.thenBranch = thenBranch;
      this.elseBranch = elseBranch;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitConditionalExpr(this);
    }

    @Override
    public String toString() {
      return "ConditionalExpr";
    }

    final Expr condition;
    final Expr thenBranch;
    final Expr elseBranch;
  }

  static class Binary extends Expr {
    Binary(Expr left, Token operator, Expr right) {
      this.left = left;
      this.operator = operator;
      this.right = right;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitBinaryExpr(this);
    }

    @Override
    public String toString() {
      return "BinaryExpr";
    }

    final Expr left;
    final Token operator;
    final Expr right;
  }

  static class Call extends Expr {
    Call(Expr callee, Token paren, List<Expr> arguments) {
      this.callee = callee;
      this.paren = paren;
      this.arguments = arguments;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitCallExpr(this);
    }

    @Override
    public String toString() {
      return "CallExpr";
    }

    final Expr callee;
    final Token paren;
    final List<Expr> arguments;
  }

  static class Array extends Expr {
    Array(Token bracket, List<Expr> elements) {
      this.bracket = bracket;
      this.elements = elements;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitArrayExpr(this);
    }

    @Override
    public String toString() {
      return "ArrayExpr";
    }

    final Token bracket;
    final List<Expr> elements;
  }

  static class Get extends Expr {
    Get(Expr object, Token name) {
      this.object = object;
      this.name = name;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitGetExpr(this);
    }

    @Override
    public String toString() {
      return "GetExpr";
    }

    final Expr object;
    final Token name;
  }

  static class IndexGet extends Expr {
    IndexGet(Expr indexee, Token bracket, Expr index) {
      this.indexee = indexee;
      this.bracket = bracket;
      this.index = index;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitIndexGetExpr(this);
    }

    @Override
    public String toString() {
      return "IndexGetExpr";
    }

    final Expr indexee;
    final Token bracket;
    final Expr index;
  }

  static class IndexSet extends Expr {
    IndexSet(Expr indexee, Token bracket, Expr index, Expr value) {
      this.indexee = indexee;
      this.bracket = bracket;
      this.index = index;
      this.value = value;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitIndexSetExpr(this);
    }

    @Override
    public String toString() {
      return "IndexSetExpr";
    }

    final Expr indexee;
    final Token bracket;
    final Expr index;
    final Expr value;
  }

  static class Grouping extends Expr {
    Grouping(Expr expression) {
      this.expression = expression;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitGroupingExpr(this);
    }

    @Override
    public String toString() {
      return "GroupingExpr";
    }

    final Expr expression;
  }

  static class Literal extends Expr {
    Literal(Object value) {
      this.value = value;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitLiteralExpr(this);
    }

    @Override
    public String toString() {
      return "LiteralExpr";
    }

    final Object value;
  }

  static class Logical extends Expr {
    Logical(Expr left, Token operator, Expr right) {
      this.left = left;
      this.operator = operator;
      this.right = right;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitLogicalExpr(this);
    }

    @Override
    public String toString() {
      return "LogicalExpr";
    }

    final Expr left;
    final Token operator;
    final Expr right;
  }

  static class Set extends Expr {
    Set(Expr object, Token name, Expr value) {
      this.object = object;
      this.name = name;
      this.value = value;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitSetExpr(this);
    }

    @Override
    public String toString() {
      return "SetExpr";
    }

    final Expr object;
    final Token name;
    final Expr value;
  }

  static class This extends Expr {
    This(Token keyword) {
      this.keyword = keyword;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitThisExpr(this);
    }

    @Override
    public String toString() {
      return "ThisExpr";
    }

    final Token keyword;
  }

  static class Super extends Expr {
    Super(Token keyword, Token method) {
      this.keyword = keyword;
      this.method = method;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitSuperExpr(this);
    }

    @Override
    public String toString() {
      return "SuperExpr";
    }

    final Token keyword;
    final Token method;
  }

  static class Unary extends Expr {
    Unary(Token operator, Expr right, boolean postfix) {
      this.operator = operator;
      this.right = right;
      this.postfix = postfix;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitUnaryExpr(this);
    }

    @Override
    public String toString() {
      return "UnaryExpr";
    }

    final Token operator;
    final Expr right;
    final boolean postfix;
  }

  static class Variable extends Expr {
    Variable(Token name) {
      this.name = name;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitVariableExpr(this);
    }

    @Override
    public String toString() {
      return "VariableExpr";
    }

    final Token name;
  }

  static class Function extends Expr {
    Function(List<Token> parameters, List<Stmt> body) {
      this.parameters = parameters;
      this.body = body;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitFunctionExpr(this);
    }

    @Override
    public String toString() {
      return "FunctionExpr";
    }

    final List<Token> parameters;
    final List<Stmt> body;
  }

  abstract <R> R accept(Visitor<R> visitor);
}
