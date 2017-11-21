package com.enielsen.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


// TODO: native "classes" should maybe be handled more generically
class LoxArray implements LoxIndexable {

    final List<Object> elements;
    private final Map<String, LoxCallable> methods;

    private final static String BOUNDS_ERROR_MSG = "Array index out of bounds.";

    LoxArray(List<Object> elements) {
        this.elements = elements;
        methods = createMethods(this);
    }


    private static Map<String, LoxCallable> createMethods(LoxArray array) {
        Map<String, LoxCallable> methods = new HashMap<>();
        methods.put("add", new LoxCallable() {
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
                array.elements.addAll(arguments);
                return null;
            }
        });
        methods.put("pop", new LoxCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                try {
                    return array.elements.remove(0);
                } catch (IndexOutOfBoundsException e) {
                    throw new NativeError("Array is empty.");
                }
            }
        });
        methods.put("remove", new LoxCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                try {
                    int idx = ((Double) arguments.get(0)).intValue();
                    return array.elements.remove(idx);
                } catch (NumberFormatException e) {
                    throw new NativeError("Index must be an integer.");
                } catch (IndexOutOfBoundsException e) {
                    throw new NativeError(BOUNDS_ERROR_MSG);
                }
            }
        });
        methods.put("length", new LoxCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return array.length();
            }
        });
        methods.put("isEmpty", new LoxCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return array.length() == 0;
            }
        });
        return methods;
    }

    LoxCallable getMethod(Token name) {
        if (methods.containsKey(name.lexeme)) {
            return methods.get(name.lexeme);
        }
        throw new RuntimeError(name, "No such method.");
    }

    @Override
    public Object get(Token token, Object index) {
        int i = indexToInteger(token, index);
        try {
            return elements.get(i);
        } catch (IndexOutOfBoundsException e) {
            throw new RuntimeError(token, BOUNDS_ERROR_MSG);
        }
    }

    // array[x:y] like in Python. //TODO: hook up in parser/interpreter
    private List<Object> slice(Token token, Object fromIndex, Object toIndex) {
        int from = indexToInteger(token, fromIndex);
        int to = indexToInteger(token, toIndex);
        try {
            List<Object> res = new ArrayList<>();
            for (int i = from; i < to; i++) {
                res.add(elements.get(i));
            }
            return res;
        } catch (IndexOutOfBoundsException e) {
            throw new RuntimeError(token, BOUNDS_ERROR_MSG);
        }
    }

    @Override
    public void set(Token token, Object index, Object item) {
        int i = indexToInteger(token, index);
        try {
            elements.set(i, item);
        } catch (IndexOutOfBoundsException e) {
            throw new RuntimeError(token, BOUNDS_ERROR_MSG);
        }
    }

    @Override
    public int length() {
        return elements.size();
    }

    private int indexToInteger(Token token, Object index) {
        if (index instanceof Double) {
            double idx = ((Double) index).doubleValue();
            // All number literals in Lox are doubles, have to do a little hack
            if (idx == Math.floor(idx)) {
                // Allow negative indexing like Python
                return (idx < 0) ? Math.floorMod((int)idx, elements.size()) : (int)idx;
            }
        }
        throw new RuntimeError(token, "Array index must be an integer.");
    }

}
