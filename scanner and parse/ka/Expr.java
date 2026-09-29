package ka;

import java.util.List;

/*
 * ==========================================================================
 *  Expr - Árvore de Sintaxe Abstrata (AST) para EXPRESSÕES
 * ==========================================================================
 * Este arquivo seria gerado automaticamente por GenerateAst.java (ver
 * pasta tool/). Cada classe interna representa um tipo de nó que pode
 * aparecer numa expressão de Ka. Nenhuma classe aqui sabe como se
 * "executar" - isso é responsabilidade de outra camada (o Interpreter,
 * que não faz parte desta versão de apresentação).
 *
 * O padrão Visitor (interface Visitor<R>) é o que permite adicionar
 * novas OPERAÇÕES sobre essas classes (imprimir, interpretar, resolver
 * nomes...) sem precisar tocar nas classes em si.
 * ==========================================================================
 */
abstract class Expr {
  interface Visitor<R> {
    R visitAssignExpr(Assign expr);
    R visitBinaryExpr(Binary expr);
    R visitCallExpr(Call expr);
    R visitFunctionExpr(Function expr);
    R visitGetExpr(Get expr);
    R visitGroupingExpr(Grouping expr);
    R visitLiteralExpr(Literal expr);
    R visitLogicalExpr(Logical expr);
    R visitObjectLiteralExpr(ObjectLiteral expr);
    R visitSetExpr(Set expr);
    R visitThisExpr(This expr);
    R visitUnaryExpr(Unary expr);
    R visitVariableExpr(Variable expr);
  }

  // a = valor
  static class Assign extends Expr {
    Assign(Token name, Expr value) {
      this.name = name;
      this.value = value;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitAssignExpr(this);
    }

    final Token name;
    final Expr value;
  }

  // esquerda OPERADOR direita   (ex: 1 + 2, a == b)
  static class Binary extends Expr {
    Binary(Expr left, Token operator, Expr right) {
      this.left = left;
      this.operator = operator;
      this.right = right;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitBinaryExpr(this);
    }

    final Expr left;
    final Token operator;
    final Expr right;
  }

  // callee(argumentos...)
  static class Call extends Expr {
    Call(Expr callee, Token paren, List<Expr> arguments) {
      this.callee = callee;
      this.paren = paren;
      this.arguments = arguments;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitCallExpr(this);
    }

    final Expr callee;
    final Token paren; // usado para reportar erros de aridade na linha certa
    final List<Expr> arguments;
  }

  // fun (parametros) { corpo }   -- função anônima em posição de expressão
  static class Function extends Expr {
    Function(List<Token> params, List<Stmt> body) {
      this.params = params;
      this.body = body;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitFunctionExpr(this);
    }

    final List<Token> params;
    final List<Stmt> body;
  }

  // objeto.propriedade
  static class Get extends Expr {
    Get(Expr object, Token name) {
      this.object = object;
      this.name = name;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitGetExpr(this);
    }

    final Expr object;
    final Token name;
  }

  // ( expressao )
  static class Grouping extends Expr {
    Grouping(Expr expression) {
      this.expression = expression;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitGroupingExpr(this);
    }

    final Expr expression;
  }

  // numero, string, true, false, nil
  static class Literal extends Expr {
    Literal(Object value) {
      this.value = value;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitLiteralExpr(this);
    }

    final Object value;
  }

  // esquerda (and|or) direita  -- separado de Binary por causa do
  // curto-circuito (a avaliação, quando implementada, não avalia o
  // lado direito se já souber o resultado).
  static class Logical extends Expr {
    Logical(Expr left, Token operator, Expr right) {
      this.left = left;
      this.operator = operator;
      this.right = right;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitLogicalExpr(this);
    }

    final Expr left;
    final Token operator;
    final Expr right;
  }

  // { chave: valor, chave2: valor2 }
  static class ObjectLiteral extends Expr {
    ObjectLiteral(List<Token> keys, List<Expr> values) {
      this.keys = keys;
      this.values = values;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitObjectLiteralExpr(this);
    }

    final List<Token> keys;
    final List<Expr> values;
  }

  // objeto.propriedade = valor
  static class Set extends Expr {
    Set(Expr object, Token name, Expr value) {
      this.object = object;
      this.name = name;
      this.value = value;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitSetExpr(this);
    }

    final Expr object;
    final Token name;
    final Expr value;
  }

  // this
  static class This extends Expr {
    This(Token keyword) {
      this.keyword = keyword;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitThisExpr(this);
    }

    final Token keyword;
  }

  // OPERADOR direita   (ex: -5, !verdadeiro)
  static class Unary extends Expr {
    Unary(Token operator, Expr right) {
      this.operator = operator;
      this.right = right;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitUnaryExpr(this);
    }

    final Token operator;
    final Expr right;
  }

  // referência a uma variável pelo nome
  static class Variable extends Expr {
    Variable(Token name) {
      this.name = name;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitVariableExpr(this);
    }

    final Token name;
  }

  abstract <R> R accept(Visitor<R> visitor);
}
