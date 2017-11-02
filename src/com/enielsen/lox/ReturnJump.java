package com.enielsen.lox;

public class ReturnJump extends RuntimeException {
    final Object value;

    ReturnJump(Object value) {
        super(null, null, false, false);
        this.value = value;
    }
}
