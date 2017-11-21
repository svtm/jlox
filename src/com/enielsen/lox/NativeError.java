package com.enielsen.lox;

class NativeError extends RuntimeException {
    public NativeError(String s) {
        super(s);
    }
}
