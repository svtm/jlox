package com.enielsen.lox;

import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

abstract class NativeFunctions {

    static void defineNatives(Environment environment) {
        environment.define("clock", clock);
        environment.define("print", print);
        environment.define("prompt", prompt);
    }

    private static final LoxCallable clock = new LoxCallable() {
        @Override
        public int arity() {
            return 0;
        }

        @Override
        public Object call(Interpreter interpreter, List<Object> arguments) {
            return (double)System.currentTimeMillis() / 1000.0;
        }
    };

    private static final LoxCallable print = new LoxCallable() {
        @Override
        public int arity() {
            return 0;
        }

        @Override
        public boolean variadic() {
            return true;
        }

        @Override
        public Object call(Interpreter interpreter, List<Object> arguments) {
            System.out.println(arguments.stream()
                    .map(interpreter::stringify)
                    .collect(Collectors.joining(" ")));
            return null;
        }
    };

    private static final LoxCallable prompt = new LoxCallable() {
        @Override
        public int arity() {
            return 0;
        }

        @Override
        public boolean variadic() {
            return true;
        }

        @Override
        public Object call(Interpreter interpreter, List<Object> arguments) {
            System.out.print(arguments.stream()
                    .map(interpreter::stringify)
                    .collect(Collectors.joining(" ")));
            java.util.Scanner sc = new Scanner(System.in);
            return sc.nextLine();
        }
    };
}
