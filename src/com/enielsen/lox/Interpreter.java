package com.enielsen.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void>{
    final Environment globals = new Environment();
    private Environment environment = globals;
    private final Map<Expr, Integer> locals = new HashMap<>();
    private Object prevResult = null;

    Interpreter() {
        NativeFunctions.defineNatives(globals);
    }

    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    void resolve(Expr expr, int depth) {
        locals.put(expr, depth);
    }

    void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;
        try{
            this.environment = environment;

            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }

    void printExpressionResult() {
        if (prevResult != null) {
            System.out.println(stringify(prevResult));
        }
    }

    /* Expression visitor implementations */

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);

        if (expr.operator.type == TokenType.OR) {
            if (isTruthy(left)) return left;
        } else {
            if (!isTruthy(left)) return left;
        }

        return evaluate(expr.right);
    }

    @Override
    public Object visitSetExpr(Expr.Set expr) {
        Object object = evaluate(expr.object);

        if (!(object instanceof LoxInstance)) {
            throw new RuntimeError(expr.name, "Only instances have fields.");
        }

        Object value = evaluate(expr.value);
        ((LoxInstance) object).set(expr.name, value);
        return value;
    }

    @Override
    public Object visitIndexSetExpr(Expr.IndexSet expr) {
        Object indexee = evaluate(expr.indexee);

        if (!(indexee instanceof LoxIndexable)) {
            throw new RuntimeError(expr.bracket, "Variable is not indexable.");
        }

        Object index = evaluate(expr.index);
        Object value = evaluate(expr.value);
        ((LoxIndexable) indexee).set(expr.bracket, index, value);
        return value;
    }

    @Override
    public Object visitThisExpr(Expr.This expr) {
        return lookupVariable(expr.keyword, expr);
    }

    @Override
    public Object visitSuperExpr(Expr.Super expr) {
        int distance = locals.get(expr);
        LoxClass superClass = ((LoxClass) environment.getAt(distance, "super"));

        // "this" is always one level nearer than "super"'s environment.
        LoxInstance receiver = (LoxInstance)environment.getAt(distance - 1, "this");

        LoxFunction method = superClass.findMethod(receiver, expr.method.lexeme);
        if (method == null) {
            throw new RuntimeError(expr.method,
                    "Undefined property '" + expr.method.lexeme + "'.");
        }

        return method;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }


    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case BANG:
                return !isTruthy(right);
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double)right;
            case PLUS_PLUS: {
                if (!(expr.right instanceof Expr.Variable)) {
                    throw new RuntimeError(expr.operator, "Operand of increment operation must be a variable");
                }
                checkNumberOperand(expr.operator, right);
                double value = (double) right;
                Expr.Variable var = ((Expr.Variable) expr.right);
                environment.assign(var.name, value + 1);
                if (expr.postfix) {
                    return value;
                } else {
                    return value + 1;
                }
            }
            case MINUS_MINUS: {
                // TODO: Dont Repeat Yourself
                if (!(expr.right instanceof Expr.Variable)) {
                    throw new RuntimeError(expr.operator, "Operand of decrement operation must be a variable");
                }
                checkNumberOperand(expr.operator, right);
                double value = (double) right;
                Expr.Variable var = ((Expr.Variable) expr.right);
                environment.assign(var.name, value - 1);
                if (expr.postfix) {
                    return value;
                } else {
                    return value - 1;
                }
            }
        }

        // Unreachable
        return null;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return lookupVariable(expr.name, expr);
    }

    private Object lookupVariable(Token name, Expr expr) {
        Integer distance = locals.get(expr);
        if (distance != null) {
            return environment.getAt(distance, name.lexeme);
        } else {
            return globals.get(name);
        }
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double)left > (double)right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left >= (double)right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left < (double)right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left <= (double)right;
            case BANG_EQUAL:
                return !isEqual(left, right);
            case EQUAL_EQUAL:
                return isEqual(left, right);

            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left - (double)right;
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double)left + (double)right;
                }

                if (left instanceof String) {
                    return (String)left + stringify(right);
                } else if (right instanceof String) {
                    return stringify(left) + (String)right;
                }

                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                checkDivisionByZero(expr.operator, right);
                return (double)left / (double)right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double)left * (double)right;
        }

        // Unreachable
        return null;
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);

        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) {
            arguments.add(evaluate(argument));
        }

        if (!(callee instanceof LoxCallable)) {
            throw new RuntimeError(expr.paren, "Can only call functions and classes.");
        }

        LoxCallable function = (LoxCallable) callee;
        if (arguments.size() != function.arity() && !function.variadic()) {
            throw new RuntimeError(expr.paren,
                    "Expected " + function.arity() + " arguments but got" + arguments.size() + ".");
        }
        try {
            return function.call(this, arguments);
        } catch (NativeError e) {
            throw new RuntimeError(expr.paren, e.getMessage());
        }
    }

    @Override
    public Object visitIndexGetExpr(Expr.IndexGet expr) {
        Object indexee = evaluate(expr.indexee);
        Object index = evaluate(expr.index);
        if (indexee instanceof LoxIndexable) {
            return ((LoxIndexable) indexee).get(expr.bracket, index);
        }
        return null;
    }

    @Override
    public Object visitGetExpr(Expr.Get expr) {
        Object object = evaluate(expr.object);
        if (object instanceof LoxInstance) {
            Object result = ((LoxInstance) object).get(expr.name);
            if (result instanceof LoxFunction && ((LoxFunction) result).isGetter()) {
                result = ((LoxFunction) result).call(this, null);
            }
            return result;
        }
        if (object instanceof LoxArray) {
            return ((LoxArray) object).getMethod(expr.name);
        }

        throw new RuntimeError(expr.name, "Only instances have properties.");
    }

    @Override
    public Object visitFunctionExpr(Expr.Function expr) {
        return new LoxFunction(null, expr, environment, false);
    }

    @Override
    public Object visitArrayExpr(Expr.Array expr) {
        return new LoxArray(expr.elements.stream()
                .map(this::evaluate)
                .collect(Collectors.toList()));
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);

        Integer distance = locals.get(expr);
        if (distance != null) {
            environment.assignAt(distance, expr.name, value);
        } else {
            globals.assign(expr.name, value);
        }
        return value;
    }

    @Override
    public Object visitConditionalExpr(Expr.Conditional expr) {
        if (isTruthy(evaluate(expr.condition))) {
            return evaluate(expr.thenBranch);
        } else {
            return evaluate(expr.elseBranch);
        }
    }

    /* Statement Visitor implementations */

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        prevResult = evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        LoxFunction function = new LoxFunction(stmt.name.lexeme, stmt.function, environment, false);
        environment.define(stmt.name.lexeme, function);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        prevResult = null;
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null) value = evaluate(stmt.value);

        throw new ReturnJump(value);
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }

        environment.define(stmt.name.lexeme, value);
        prevResult = null;
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (isTruthy(evaluate(stmt.condition))) {
            try {
                execute(stmt.body);
            } catch (BreakJump jump) {
                break;
            }
        }
        return null;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        prevResult = null;
        return null;
    }

    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        throw new BreakJump();
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        environment.define(stmt.name.lexeme, null);

        Object superClass = null;
        if (stmt.superClass != null) {
            superClass = evaluate(stmt.superClass);
            if (!(superClass instanceof LoxClass)) {
                throw new RuntimeError(stmt.name, "Superclass must be a class");
            }

            environment = new Environment(environment);
            environment.define("super", superClass);
        }

        Map<String, LoxFunction> classMethods = new HashMap<>();
        for (Stmt.Function method : stmt.classMethods) {
            LoxFunction function = new LoxFunction(
                    stmt.name.lexeme +"." + method.name.lexeme,
                    method.function, environment,
                    method.name.lexeme.equals("init"));
            classMethods.put(method.name.lexeme, function);
        }

        LoxClass metaclass = new LoxClass(null, ((LoxClass) superClass),stmt.name.lexeme + " metaclass", classMethods);

        Map<String, LoxFunction> methods = new HashMap<>();
        for (Stmt.Function method : stmt.methods) {
            LoxFunction function = new LoxFunction(
                    stmt.name.lexeme +"." + method.name.lexeme,
                    method.function, environment,
                    method.name.lexeme.equals("init"));
            methods.put(method.name.lexeme, function);
        }
        LoxClass klass = new LoxClass(metaclass, ((LoxClass) superClass), stmt.name.lexeme, methods);

        if (superClass != null) {
            environment = environment.enclosing;
        }

        environment.assign(stmt.name, klass);
        return null;
    }

    /* Helper methods */

    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean)object;

        return true;
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;

        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;

        throw new RuntimeError(operator, "Operators must be numbers.");
    }

    private void checkDivisionByZero(Token operator, Object denominator) {
        if ((double)denominator == 0) {
            throw new RuntimeError(operator, "Cannot divide by zero.");
        }
    }

    private Object evaluate(Expr expr) {
        Object res = expr.accept(this);
        this.prevResult = res;
        return res;
    }


    private boolean isEqual(Object a, Object b) {
        // nil is only equal to nil
        if (a == null && b == null) return true;
        if (a == null) return false;

        return a.equals(b);
    }

    /*package private*/ String stringify(Object object) {
        if (object == null) return "nil";

        // Hack. Work around Java adding ".0" to integer-valued doubles.
        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        if (object instanceof LoxArray) {
            List<String> elementStrings = ((LoxArray) object).elements
                    .stream()
                    .map(this::stringify)
                    .collect(Collectors.toList());
            return "[" + String.join(", ", elementStrings) + "]";
        }

        return object.toString();
    }
}
