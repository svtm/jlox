package com.enielsen.lox;

import java.util.List;

interface LoxCallable {
    int arity();
    default boolean variadic() {
        return false;
    }
    Object call(Interpreter interpreter, List<Object> arguments);
}
