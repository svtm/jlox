package com.enielsen.lox;

import java.util.List;

class LoxFunction implements LoxCallable {

    private final String name;
    private final Expr.Function function;
    private final Environment closure;
    private final boolean isInitializer;

    LoxFunction(String name, Expr.Function function, Environment closure, boolean isInitializer) {
        this.name = name;
        this.function = function;
        this.closure = closure;
        this.isInitializer = isInitializer;
    }

    LoxFunction bind(LoxInstance instance) {
        Environment environment = new Environment(closure);
        environment.define("this", instance);
        return new LoxFunction(name, function, environment, isInitializer);
    }

    @Override
    public int arity() {
        return function.parameters.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Environment environment = new Environment(closure);
        for (int i = 0; i < function.parameters.size(); i++) {
            environment.define(function.parameters.get(i).lexeme, arguments.get(i));
        }
        try {
            interpreter.executeBlock(function.body, environment);
        } catch (ReturnJump returnValue) {
            return returnValue.value;
        }

        if (isInitializer) {
            return closure.getAt(0, "this");
        }
        return null;
    }

    @Override
    public String toString() {
        return "<fn" +
                (name == null
                        ? ">"
                        : (" " + name + ">")
                );
    }
}
