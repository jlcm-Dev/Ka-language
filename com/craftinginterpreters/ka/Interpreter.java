package com.craftinginterpreters.ka;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    final Environment globals = new Environment();
    private Environment environment = globals;
    private final Map<Expr, Integer> locals = new HashMap<>();

    Interpreter() {
        globals.define("clock", new KaCallable() {
            @Override
            public int arity() { return 0; }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return (double) System.currentTimeMillis() / 1000.0;
            }

            @Override
            public String toString() { return "<native fn>"; }
        });

        // "Object" e um objeto global comum (sem prototipo), com um
        // campo "create" que e uma funcao nativa. Isso e bem
        // idiomatico de linguagem prototipica: nao existe uma
        // "classe Object" especial, so um objeto de conveniencia.
        KaObject objectGlobal = new KaObject(null);
        objectGlobal.fields.put("create", new KaCallable() {
            @Override
            public int arity() { return 1; }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                Object protoArg = arguments.get(0);
                if (protoArg != null && !(protoArg instanceof KaObject)) {
                    // Sem um Token específico aqui (é nativo) - usamos
                    // um token sintético só pra ter uma linha pra reportar.
                    throw new RuntimeError(new Token(TokenType.NIL, "create", null, 0),
                        "O prototipo passado para Object.create deve ser um objeto ou nil.");
                }
                KaObject proto = (protoArg instanceof KaObject) ? (KaObject) protoArg : null;
                return new KaObject(proto);
            }

            @Override
            public String toString() { return "<native fn create>"; }
        });
        globals.define("Object", objectGlobal);
    }

    void resolve(Expr expr, int depth) {
        locals.put(expr, depth);
    }

    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Ka.runtimeError(error);
        }
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null) value = evaluate(stmt.value);
        throw new Return(value);
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }
        environment.define(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        KaFunction function = new KaFunction(stmt.name.lexeme, stmt.params, stmt.body, environment);
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
    public Void visitWhileStmt(Stmt.While stmt) {
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body);
        }
        return null;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;
            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);
        switch (expr.operator.type) {
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double) right;
            case BANG:
                return !isTruthy(right);
        }
        return null;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return lookUpVariable(expr.name, expr);
    }

    private Object lookUpVariable(Token name, Expr expr) {
        Integer distance = locals.get(expr);
        if (distance != null) {
            return environment.getAt(distance, name.lexeme);
        } else {
            return globals.get(name);
        }
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
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left - (double) right;
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                return (double) left / (double) right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double) left * (double) right;
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                }
                if (left instanceof String && right instanceof String) {
                    return (String) left + (String) right;
                }
                throw new RuntimeError(expr.operator,
                    "Os operandos devem ser dois numeros ou duas strings.");
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double) left > (double) right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left >= (double) right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left < (double) right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left <= (double) right;
            case BANG_EQUAL:
                return !isEqual(left, right);
            case EQUAL_EQUAL:
                return isEqual(left, right);
        }
        return null;
    }

    // ---------------------------------------------------------------
    // Chamadas: aqui mora a diferença central do Ka em relação ao
    // jlox. Se o "callee" é uma expressão Get (objeto.propriedade),
    // fazemos o binding de "this" DINAMICAMENTE, nesta chamada
    // específica - baseado no objeto usado NESTA sintaxe, não em
    // onde a função foi originalmente declarada.
    // ---------------------------------------------------------------
    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee;
        Object receiver = null;

        if (expr.callee instanceof Expr.Get) {
            Expr.Get get = (Expr.Get) expr.callee;
            Object object = evaluate(get.object);
            if (!(object instanceof KaObject)) {
                throw new RuntimeError(get.name, "Somente objetos tem propriedades.");
            }
            receiver = object;
            callee = ((KaObject) object).get(get.name);
        } else {
            callee = evaluate(expr.callee);
        }

        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) {
            arguments.add(evaluate(argument));
        }

        if (!(callee instanceof KaCallable)) {
            throw new RuntimeError(expr.paren, "So e possivel chamar funcoes e objetos chamaveis.");
        }

        KaCallable function = (KaCallable) callee;

        if (arguments.size() != function.arity()) {
            throw new RuntimeError(expr.paren, "Esperado " +
                function.arity() + " argumentos mas recebeu " + arguments.size() + ".");
        }

        // Só "amarra" this se a função for uma KaFunction de verdade
        // (funções nativas, tipo clock/Object.create, ignoram isso).
        if (receiver != null && function instanceof KaFunction) {
            function = ((KaFunction) function).bind((KaObject) receiver);
        }

        return function.call(this, arguments);
    }

    // "objeto.propriedade" LIDO SEM CHAMAR - devolve o valor cru,
    // SEM vincular "this" (diferente de visitCallExpr). É assim que
    // um método "solto" de seu objeto perde a referência a "this",
    // igual acontece de verdade em JavaScript.
    @Override
    public Object visitGetExpr(Expr.Get expr) {
        Object object = evaluate(expr.object);
        if (object instanceof KaObject) {
            return ((KaObject) object).get(expr.name);
        }
        throw new RuntimeError(expr.name, "Somente objetos tem propriedades.");
    }

    @Override
    public Object visitSetExpr(Expr.Set expr) {
        Object object = evaluate(expr.object);
        if (!(object instanceof KaObject)) {
            throw new RuntimeError(expr.name, "Somente objetos tem campos.");
        }
        Object value = evaluate(expr.value);
        ((KaObject) object).set(expr.name, value);
        return value;
    }

    @Override
    public Object visitThisExpr(Expr.This expr) {
        return lookUpVariable(expr.keyword, expr);
    }

    @Override
    public Object visitFunctionExpr(Expr.Function expr) {
        // Função anônima ("fun (a,b) {...}"): captura o ambiente
        // ATUAL como closure, igual uma declaração normal, só que
        // sem nome (usado em toString como "<fn anonima>").
        return new KaFunction(null, expr.params, expr.body, environment);
    }

    @Override
    public Object visitObjectLiteralExpr(Expr.ObjectLiteral expr) {
        // "{ chave: valor, ... }" -> cria um KaObject NOVO, sem
        // protótipo, com cada campo avaliado e atribuído na hora.
        KaObject object = new KaObject(null);
        for (int i = 0; i < expr.keys.size(); i++) {
            Object value = evaluate(expr.values.get(i));
            object.fields.put(expr.keys.get(i).lexeme, value);
        }
        return object;
    }

    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean) object;
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;
        return a.equals(b);
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "O operando deve ser um numero.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator, "Os operandos devem ser numeros.");
    }

    private String stringify(Object object) {
        if (object == null) return "nil";
        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }
        return object.toString();
    }
}
