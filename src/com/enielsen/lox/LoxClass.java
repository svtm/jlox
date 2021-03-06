package com.enielsen.lox;

import java.util.List;
import java.util.Map;

class LoxClass extends LoxInstance implements LoxCallable {
    final String name;
    final LoxClass superClass;
    private final Map<String, LoxFunction> methods;

    LoxClass(LoxClass metaclass, LoxClass superClass, String name, Map<String, LoxFunction> methods) {
        super(metaclass);
        this.superClass = superClass;
        this.name = name;
        this.methods = methods;
    }

    LoxFunction findMethod(LoxInstance instance, String name) {
        if (methods.containsKey(name)) {
            return methods.get(name).bind(instance);
        }
        if (superClass != null) {
            return superClass.findMethod(instance, name);
        }

        return null;
    }


    @Override
    public int arity() {
        LoxFunction initalizer = methods.get("init");
        if (initalizer == null) return 0;
        return initalizer.arity();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        LoxInstance instance = new LoxInstance(this);
        LoxFunction initializer = methods.get("init");
        if (initializer != null) {
            initializer.bind(instance).call(interpreter, arguments);
        }
        return instance;
    }

    @Override
    public String toString() {
        return "<class " + name +">";
    }
}
