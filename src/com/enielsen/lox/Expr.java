package com.enielsen.lox;

import java.util.List;

abstract class Expr {
  interface Visitor<R> {
    R visitAssignExpr(Assign expr);
    R visitBinaryExpr(Binary expr);
    R visitCallExpr(Call expr);
    R visitGroupingExpr(Grouping expr);
    R visitLiteralExpr(Literal expr);
    R visitLogicalExpr(Logical expr);
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
