package com.enielsen.lox;

import java.util.List;

abstract class Stmt {
  interface Visitor<R> {
    R visitBlockStmt(Block stmt);
    R visitClassStmt(Class stmt);
    R visitExpressionStmt(Expression stmt);
    R visitFunctionStmt(Function stmt);
    R visitIfStmt(If stmt);
    R visitPrintStmt(Print stmt);
    R visitReturnStmt(Return stmt);
    R visitVarStmt(Var stmt);
    R visitWhileStmt(While stmt);
    R visitBreakStmt(Break stmt);
  }

  static class Block extends Stmt {
    Block(List<Stmt> statements) {
      this.statements = statements;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitBlockStmt(this);
    }

    @Override
    public String toString() {
      return "BlockStmt";
    }

    final List<Stmt> statements;
  }

  static class Class extends Stmt {
    Class(Token name, Expr superClass, List<Stmt.Function> methods, List<Stmt.Function> classMethods) {
      this.name = name;
      this.superClass = superClass;
      this.methods = methods;
      this.classMethods = classMethods;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitClassStmt(this);
    }

    @Override
    public String toString() {
      return "ClassStmt";
    }

    final Token name;
    final Expr superClass;
    final List<Stmt.Function> methods;
    final List<Stmt.Function> classMethods;
  }

  static class Expression extends Stmt {
    Expression(Expr expression) {
      this.expression = expression;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitExpressionStmt(this);
    }

    @Override
    public String toString() {
      return "ExpressionStmt";
    }

    final Expr expression;
  }

  static class Function extends Stmt {
    Function(Token name, Expr.Function function) {
      this.name = name;
      this.function = function;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitFunctionStmt(this);
    }

    @Override
    public String toString() {
      return "FunctionStmt";
    }

    final Token name;
    final Expr.Function function;
  }

  static class If extends Stmt {
    If(Expr condition, Stmt thenBranch, Stmt elseBranch) {
      this.condition = condition;
      this.thenBranch = thenBranch;
      this.elseBranch = elseBranch;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitIfStmt(this);
    }

    @Override
    public String toString() {
      return "IfStmt";
    }

    final Expr condition;
    final Stmt thenBranch;
    final Stmt elseBranch;
  }

  static class Print extends Stmt {
    Print(Expr expression) {
      this.expression = expression;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitPrintStmt(this);
    }

    @Override
    public String toString() {
      return "PrintStmt";
    }

    final Expr expression;
  }

  static class Return extends Stmt {
    Return(Token keyword, Expr value) {
      this.keyword = keyword;
      this.value = value;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitReturnStmt(this);
    }

    @Override
    public String toString() {
      return "ReturnStmt";
    }

    final Token keyword;
    final Expr value;
  }

  static class Var extends Stmt {
    Var(Token name, Expr initializer) {
      this.name = name;
      this.initializer = initializer;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitVarStmt(this);
    }

    @Override
    public String toString() {
      return "VarStmt";
    }

    final Token name;
    final Expr initializer;
  }

  static class While extends Stmt {
    While(Expr condition, Stmt body) {
      this.condition = condition;
      this.body = body;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitWhileStmt(this);
    }

    @Override
    public String toString() {
      return "WhileStmt";
    }

    final Expr condition;
    final Stmt body;
  }

  static class Break extends Stmt {
    Break() {
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitBreakStmt(this);
    }

    @Override
    public String toString() {
      return "BreakStmt";
    }
  }

  abstract <R> R accept(Visitor<R> visitor);
}
