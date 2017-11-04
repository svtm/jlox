package com.enielsen.lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;

class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void>{
    private final Interpreter interpreter;
    private final Stack<Map<String, Variable>> scopes = new Stack<>();
    private FunctionType currentFunction = FunctionType.NONE;
    private ClassType currentClass = ClassType.NONE;

    Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    private enum FunctionType {
        NONE,
        FUNCTION,
        INITIALIZER,
        METHOD
    }

    private enum ClassType {
        NONE,
        CLASS
    }

    private static class Variable {
        final Token name;
        VariableState state;

        public Variable(Token name, VariableState state) {
            this.name = name;
            this.state = state;
        }
    }

    private enum VariableState {
        DECLARED,
        DEFINED,
        READ
    }

    void resolve(List<Stmt> statements) {
        for (Stmt statement : statements) {
            resolve(statement);
        }
    }


    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        beginScope();
        resolve(stmt.statements);
        endScope();
        return null;
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        declare(stmt.name);
        define(stmt.name);

        ClassType enclosingClass = currentClass;
        currentClass = ClassType.CLASS;

        beginScope();
        // sortof a hack, "this" has no token, assume always READ to prevent warnings
        scopes.peek().put("this", new Variable(null, VariableState.READ));
        for (Stmt.Function method : stmt.methods) {
            FunctionType declaration = FunctionType.METHOD;
            if (method.name.lexeme.equals("init")) {
                declaration = FunctionType.INITIALIZER;
            }
            resolveFunction(method.function, declaration);
        }

        for (Stmt.Function method : stmt.classMethods) {
            beginScope();
            scopes.peek().put("this", new Variable(null, VariableState.READ));
            resolveFunction(method.function, FunctionType.METHOD);
            endScope();
        }
        endScope();

        currentClass = enclosingClass;
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        resolve(stmt.condition);
        resolve(stmt.thenBranch);
        if(stmt.elseBranch != null) resolve(stmt.elseBranch);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        if (currentFunction == FunctionType.NONE) {
            Lox.error(stmt.keyword, "Cannot return from top-level code");
        }
        if (stmt.value != null) {
            if (currentFunction == FunctionType.INITIALIZER) {
                Lox.error(stmt.keyword, "Cannot return a value from an initializer");
            }

            resolve(stmt.value);
        }
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        resolve(stmt.condition);
        resolve(stmt.body);
        return null;
    }

    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        // No resolution needed
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        declare(stmt.name);
        define(stmt.name);

        resolve(stmt.function);
        //resolveFunction(stmt.function, FunctionType.FUNCTION);
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        declare(stmt.name);
        if (stmt.initializer != null) {
            resolve(stmt.initializer);
            define(stmt.name);
        }
        return null;
    }

    @Override
    public Void visitAssignExpr(Expr.Assign expr) {
        resolve(expr.value);
        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitConditionalExpr(Expr.Conditional expr) {
        resolve(expr.condition);
        resolve(expr.thenBranch);
        resolve(expr.elseBranch);
        return null;
    }

    @Override
    public Void visitVariableExpr(Expr.Variable expr) {
        if (!scopes.isEmpty()
                && scopes.peek().containsKey(expr.name.lexeme)
                && scopes.peek().get(expr.name.lexeme).state == VariableState.DECLARED) {
            Lox.error(expr.name, "Cannot read local variable in its own initializer.");
        }

        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitBinaryExpr(Expr.Binary expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitCallExpr(Expr.Call expr) {
        resolve(expr.callee);

        for (Expr argument : expr.arguments) {
            resolve(argument);
        }
        return null;
    }

    @Override
    public Void visitGetExpr(Expr.Get expr) {
        resolve(expr.object);
        return null;
    }

    @Override
    public Void visitGroupingExpr(Expr.Grouping expr) {
        resolve(expr.expression);
        return null;
    }

    @Override
    public Void visitLiteralExpr(Expr.Literal expr) {
        // No resolution needed
        return null;
    }

    @Override
    public Void visitLogicalExpr(Expr.Logical expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitSetExpr(Expr.Set expr) {
        // TODO: (nicetohave) warn if setting field shadows class method
        resolve(expr.value);
        resolve(expr.object);
        return null;
    }

    @Override
    public Void visitThisExpr(Expr.This expr) {
        if (currentClass == ClassType.NONE) {
            Lox.error(expr.keyword, "Cannot use 'this' outside of a class.");
            return null;
        }

        resolveLocal(expr, expr.keyword);
        return null;
    }

    @Override
    public Void visitUnaryExpr(Expr.Unary expr) {
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitFunctionExpr(Expr.Function expr) {

        resolveFunction(expr, FunctionType.FUNCTION);
       /* beginScope();
        for (Token param : expr.parameters) {
            declare(param);
            define(param);
        }
        resolve(expr.body);
        endScope();*/
        return null;
    }

    private void resolve(Stmt stmt) {
        stmt.accept(this);
    }

    private void resolve(Expr expr) {
        expr.accept(this);
    }

    private void beginScope() {
        scopes.push(new HashMap<>());
    }

    private void endScope() {
        Map<String, Variable> scope = scopes.pop();
        List<Variable> unread = scope.entrySet()
                .stream()
                .filter(entry -> entry.getValue().state != VariableState.READ)
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
        unread.stream()
                .filter(var -> var.state == VariableState.DECLARED)
                .forEach(var -> Lox.warning(var.name, "Local variable never defined."));
        unread.stream()
                .filter(var -> var.state == VariableState.DEFINED)
                .forEach(var -> Lox.warning(var.name, "Local variable never used."));
                //.forEach(var -> Lox.warning(var.name, "Local variable is not used."));
    }

    private void declare(Token name) {
        if (scopes.isEmpty()) return;

        Map<String, Variable> scope = scopes.peek();
        if (scope.containsKey(name.lexeme)) {
            Lox.error(name, "Variable with this name already declared in this scope.");
        }
        scope.put(name.lexeme, new Variable(name, VariableState.DECLARED));
    }

    private void define(Token name) {
        if (scopes.isEmpty()) return;

        scopes.peek().get(name.lexeme).state = VariableState.DEFINED;
    }

    private void resolveLocal(Expr expr, Token name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size() - 1 - i);
                scopes.get(i).get(name.lexeme).state = VariableState.READ;
                return;
            }
        }
        // Not found, assume it's global
    }

    private void resolveFunction(Expr.Function function, FunctionType type) {
        FunctionType enclosingFunction = currentFunction;
        currentFunction = type;

        beginScope();
        if (function.parameters != null) {
            for (Token param : function.parameters) {
                declare(param);
                define(param);
            }
        }
        resolve(function.body);
        endScope();
        currentFunction = enclosingFunction;
    }
}

