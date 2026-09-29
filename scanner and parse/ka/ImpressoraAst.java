package ka;

import java.util.List;

/*
 * ==========================================================================
 *  ImpressoraAst - visualização da árvore de sintaxe para depuração
 * ==========================================================================
 * Implementa TANTO Expr.Visitor<Void> QUANTO Stmt.Visitor<Void> - ela
 * sabe "visitar" qualquer nó da AST inteira do programa (comandos e
 * expressões), e para cada um imprime uma linha indentada proporcional
 * à profundidade na árvore. O resultado lembra a exibição de uma árvore
 * de diretórios: cada nível de aninhamento ganha mais indentação.
 *
 * Essa é a peça que comprova, visualmente, que o Parser está
 * respeitando a precedência de operadores e a estrutura da gramática:
 * basta olhar QUEM está mais acima na árvore (a raiz de cada
 * subárvore é o que "executa por último" / "tem menor precedência").
 * ==========================================================================
 */
class ImpressoraAst implements Expr.Visitor<Void>, Stmt.Visitor<Void> {

    private int depth = 0;

    private void printLine(String texto) {
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < depth; i++) indent.append("  ");
        System.out.println(indent + texto);
    }

    // Ponto de entrada: imprime uma lista de comandos de topo (o programa inteiro).
    void print(List<Stmt> statements) {
        System.out.println("=== ARVORE DE SINTAXE (AST) ===");
        for (Stmt stmt : statements) {
            if (stmt == null) continue; // pode ser null se houve erro de sintaxe recuperado
            stmt.accept(this);
        }
        System.out.println("================================");
    }

    void print(Expr expr) {
        expr.accept(this);
    }

    private void child(String rotulo, Object node) {
        depth++;
        printLine(rotulo + ":");
        depth++;
        if (node instanceof Expr) ((Expr) node).accept(this);
        else if (node instanceof Stmt) ((Stmt) node).accept(this);
        depth -= 2;
    }

    private void childList(String rotulo, List<?> nodes) {
        depth++;
        printLine(rotulo + " (" + nodes.size() + "):");
        depth++;
        for (Object node : nodes) {
            if (node instanceof Expr) ((Expr) node).accept(this);
            else if (node instanceof Stmt) ((Stmt) node).accept(this);
            else if (node instanceof Token) printLine("- " + ((Token) node).lexeme);
        }
        depth -= 2;
    }

    // ---------------- Stmt ----------------

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        printLine("Bloco {");
        depth++;
        for (Stmt s : stmt.statements) s.accept(this);
        depth--;
        printLine("}");
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        printLine("ComandoExpressao");
        child("expressao", stmt.expression);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        StringBuilder params = new StringBuilder();
        for (int i = 0; i < stmt.params.size(); i++) {
            if (i > 0) params.append(", ");
            params.append(stmt.params.get(i).lexeme);
        }
        printLine("DeclaracaoFuncao " + stmt.name.lexeme + "(" + params + ")");
        depth++;
        for (Stmt s : stmt.body) s.accept(this);
        depth--;
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        printLine("If");
        child("condicao", stmt.condition);
        child("entao", stmt.thenBranch);
        if (stmt.elseBranch != null) child("senao", stmt.elseBranch);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        printLine("Print");
        child("valor", stmt.expression);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        printLine("Return");
        if (stmt.value != null) child("valor", stmt.value);
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        printLine("DeclaracaoVar " + stmt.name.lexeme);
        if (stmt.initializer != null) child("inicializador", stmt.initializer);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        printLine("While");
        child("condicao", stmt.condition);
        child("corpo", stmt.body);
        return null;
    }

    // ---------------- Expr ----------------

    @Override
    public Void visitAssignExpr(Expr.Assign expr) {
        printLine("Atribuicao " + expr.name.lexeme + " =");
        child("valor", expr.value);
        return null;
    }

    @Override
    public Void visitBinaryExpr(Expr.Binary expr) {
        printLine("Binaria '" + expr.operator.lexeme + "'");
        child("esquerda", expr.left);
        child("direita", expr.right);
        return null;
    }

    @Override
    public Void visitCallExpr(Expr.Call expr) {
        printLine("Chamada");
        child("callee", expr.callee);
        childList("argumentos", expr.arguments);
        return null;
    }

    @Override
    public Void visitFunctionExpr(Expr.Function expr) {
        StringBuilder params = new StringBuilder();
        for (int i = 0; i < expr.params.size(); i++) {
            if (i > 0) params.append(", ");
            params.append(expr.params.get(i).lexeme);
        }
        printLine("FuncaoAnonima (" + params + ")");
        depth++;
        for (Stmt s : expr.body) s.accept(this);
        depth--;
        return null;
    }

    @Override
    public Void visitGetExpr(Expr.Get expr) {
        printLine("AcessoPropriedade ." + expr.name.lexeme);
        child("objeto", expr.object);
        return null;
    }

    @Override
    public Void visitGroupingExpr(Expr.Grouping expr) {
        printLine("Agrupamento ( )");
        child("expressao", expr.expression);
        return null;
    }

    @Override
    public Void visitLiteralExpr(Expr.Literal expr) {
        printLine("Literal " + (expr.value == null ? "nil" : expr.value));
        return null;
    }

    @Override
    public Void visitLogicalExpr(Expr.Logical expr) {
        printLine("Logica '" + expr.operator.lexeme + "'");
        child("esquerda", expr.left);
        child("direita", expr.right);
        return null;
    }

    @Override
    public Void visitObjectLiteralExpr(Expr.ObjectLiteral expr) {
        printLine("LiteralObjeto {");
        depth++;
        for (int i = 0; i < expr.keys.size(); i++) {
            printLine(expr.keys.get(i).lexeme + ":");
            depth++;
            expr.values.get(i).accept(this);
            depth--;
        }
        depth--;
        printLine("}");
        return null;
    }

    @Override
    public Void visitSetExpr(Expr.Set expr) {
        printLine("Atribuicao ." + expr.name.lexeme + " =");
        child("objeto", expr.object);
        child("valor", expr.value);
        return null;
    }

    @Override
    public Void visitThisExpr(Expr.This expr) {
        printLine("This");
        return null;
    }

    @Override
    public Void visitUnaryExpr(Expr.Unary expr) {
        printLine("Unaria '" + expr.operator.lexeme + "'");
        child("operando", expr.right);
        return null;
    }

    @Override
    public Void visitVariableExpr(Expr.Variable expr) {
        printLine("Variavel " + expr.name.lexeme);
        return null;
    }
}
