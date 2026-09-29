package ka;

import java.util.List;

/*
 * ==========================================================================
 *  Stmt - Árvore de Sintaxe Abstrata (AST) para COMANDOS (statements)
 * ==========================================================================
 * Diferença conceitual entre Expr e Stmt:
 *   - Expr (expressão)  -> PRODUZ um valor.          Ex: 2 + 3, a.b, f(x)
 *   - Stmt (comando)     -> PRODUZ um EFEITO, não um valor.
 *                           Ex: "var x = 1;", "print x;", um laço "while"
 *
 * Um programa em Ka é, no nível mais alto, uma lista de Stmt.
 * ==========================================================================
 */
abstract class Stmt {
  interface Visitor<R> {
    R visitBlockStmt(Block stmt);
    R visitExpressionStmt(Expression stmt);
    R visitFunctionStmt(Function stmt);
    R visitIfStmt(If stmt);
    R visitPrintStmt(Print stmt);
    R visitReturnStmt(Return stmt);
    R visitVarStmt(Var stmt);
    R visitWhileStmt(While stmt);
  }

  // { comando1; comando2; ... }
  static class Block extends Stmt {
    Block(List<Stmt> statements) {
      this.statements = statements;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitBlockStmt(this);
    }

    final List<Stmt> statements;
  }

  // expressao ;   (uma expressão usada só pelo efeito colateral, ex: chamada de função)
  static class Expression extends Stmt {
    Expression(Expr expression) {
      this.expression = expression;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitExpressionStmt(this);
    }

    final Expr expression;
  }

  // fun nome(parametros) { corpo }   -- declaração de função nomeada
  static class Function extends Stmt {
    Function(Token name, List<Token> params, List<Stmt> body) {
      this.name = name;
      this.params = params;
      this.body = body;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitFunctionStmt(this);
    }

    final Token name;
    final List<Token> params;
    final List<Stmt> body;
  }

  // if (condicao) entao [else senao]
  static class If extends Stmt {
    If(Expr condition, Stmt thenBranch, Stmt elseBranch) {
      this.condition = condition;
      this.thenBranch = thenBranch;
      this.elseBranch = elseBranch;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitIfStmt(this);
    }

    final Expr condition;
    final Stmt thenBranch;
    final Stmt elseBranch; // pode ser null (sem "else")
  }

  // print expressao ;
  static class Print extends Stmt {
    Print(Expr expression) {
      this.expression = expression;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitPrintStmt(this);
    }

    final Expr expression;
  }

  // return [expressao] ;
  static class Return extends Stmt {
    Return(Token keyword, Expr value) {
      this.keyword = keyword;
      this.value = value;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitReturnStmt(this);
    }

    final Token keyword;
    final Expr value; // pode ser null ("return;" sem valor)
  }

  // var nome [= inicializador] ;
  static class Var extends Stmt {
    Var(Token name, Expr initializer) {
      this.name = name;
      this.initializer = initializer;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitVarStmt(this);
    }

    final Token name;
    final Expr initializer; // pode ser null ("var x;" sem valor inicial)
  }

  // while (condicao) corpo
  // (o "for" do Parser é desnaturado em um "while" equivalente - não
  // existe Stmt.For separado; veja o comentário em Parser.forStatement())
  static class While extends Stmt {
    While(Expr condition, Stmt body) {
      this.condition = condition;
      this.body = body;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitWhileStmt(this);
    }

    final Expr condition;
    final Stmt body;
  }

  abstract <R> R accept(Visitor<R> visitor);
}
