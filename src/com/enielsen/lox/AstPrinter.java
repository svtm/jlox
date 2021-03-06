package com.enielsen.lox;


// This class should return a string representation of AST nodes, but is currently pretty bad
//TODO implement
class AstPrinter implements Expr.Visitor<String> {
    String print(Expr expr) {
        return expr.accept(this);
    }

    private String parenthesize(String name, Expr... exprs) {
        StringBuilder builder = new StringBuilder();

        builder.append("(").append(name);
        for (Expr expr : exprs) {
            builder.append(" ");
            builder.append(expr.accept(this));
        }
        builder.append(")");

        return builder.toString();
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return parenthesize("group", expr.expression);
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        return expr.value == null
                ? "nil"
                : expr.value.toString();
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return parenthesize(expr.operator.lexeme, expr.right);
    }

    @Override
    public String visitVariableExpr(Expr.Variable expr) {
        return parenthesize("var", expr);
    }

    @Override
    public String visitAssignExpr(Expr.Assign expr) {
        return parenthesize("assign", expr);
    }

    @Override
    public String visitLogicalExpr(Expr.Logical expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitCallExpr(Expr.Call expr) {
        return ""; // TODO
    }

    @Override
    public String visitFunctionExpr(Expr.Function expr) {
        return ""; //TODO
    }

    @Override
    public String visitGetExpr(Expr.Get expr) {
        return ""; //TODO
    }

    @Override
    public String visitSetExpr(Expr.Set expr) {
        return ""; //TODO
    }

    @Override
    public String visitThisExpr(Expr.This expr) {
        return ""; //TODO
    }

    @Override
    public String visitConditionalExpr(Expr.Conditional expr) {
        return ""; //TODO
    }

    @Override
    public String visitSuperExpr(Expr.Super expr) {
        return ""; //TODO
    }

    @Override
    public String visitArrayExpr(Expr.Array expr) {
        return ""; //TODO
    }

    @Override
    public String visitIndexGetExpr(Expr.IndexGet expr) {
        return ""; //TODO
    }

    @Override
    public String visitIndexSetExpr(Expr.IndexSet expr) {
        return ""; //TODO
    }
}
