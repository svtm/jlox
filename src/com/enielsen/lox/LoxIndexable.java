package com.enielsen.lox;

import java.util.ArrayList;
import java.util.Collection;

interface LoxIndexable {

    Object get(Token token, Object index);

    void set(Token token, Object index, Object item);

    int length();
}
