package com.craftinginterpreters.ka;

import java.util.List;

interface KaCallable {
    int arity();
    Object call(Interpreter interpreter, List<Object> arguments);
}
