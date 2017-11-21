package com.enielsen.lox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import static com.enielsen.lox.TokenType.*;

class Parser {
    private static class ParseError extends RuntimeException {}

    private static final int MAX_ARGS = 32;

    private final List<Token> tokens;
    private int current = 0;
    private int loopLevel = 0; // keep track of how deep our loops are for break statements
    private boolean inClass = false; // keep track of if we are currently parsing a class declaration

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            try {
                statements.add(declaration());
            } catch (ParseError error) {
                return Collections.emptyList();
            }
        }

        return statements;
    }

    private Stmt declaration() {
        try {
            if (match(CLASS)) return classDeclaration();

            if (check(FUN) && checkNext(IDENTIFIER)) {
                consume(FUN, null);
                return function("function");
            }
            if (match(VAR)) return varDeclaration();

            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt classDeclaration() {
        Token name = consume(IDENTIFIER, "Expect class name.");
        // Inheritance
        Expr superClass = null;
        if (match(LESS)) {
            consume(IDENTIFIER, "Expect class name");
            superClass = new Expr.Variable(previous());
        }
        consume(LEFT_BRACE, "Expect '{' before class body.");

        List<Stmt.Function> methods = new ArrayList<>();
        List<Stmt.Function> classMethods = new ArrayList<>();
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            boolean isClassMethod = match(CLASS);
            (isClassMethod ? classMethods : methods).add(function("method"));
        }

        consume(RIGHT_BRACE, "Expect '}' after class body.");

        return new Stmt.Class(name, superClass, methods, classMethods);
    }

    private Stmt statement() {
        if (match(FOR)) return forStatement();
        if (match(IF)) return ifStatement();
        if (match(PRINT)) return printStatement();
        if (match(RETURN)) return returnStatement();
        if (match(WHILE)) return whileStatement();
        if (match(BREAK)) return breakStatement();
        if (match(LEFT_BRACE)) return new Stmt.Block(block());

        return expressionStatement();
    }

    private Stmt forStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'for'.");

        Stmt initalizer;
        if (match(SEMICOLON)) {
            initalizer = null;
        } else if (match(VAR)) {
            initalizer = varDeclaration();
        } else {
            initalizer = expressionStatement();
        }

        Expr condition = null;
        if (!check(SEMICOLON)) {
            condition = expression();
        }
        consume(SEMICOLON, "Expect ';' after loop condition");

        Expr increment = null;
        if (!check(RIGHT_PAREN)) {
            increment = expression();
        }
        consume(RIGHT_PAREN, "Expect ')' after 'for'-clauses");

        loopLevel++;
        Stmt body = statement();
        loopLevel--;
        if (increment != null) {
            body = new Stmt.Block(Arrays.asList(
                    body,
                    new Stmt.Expression(increment)
            ));
        }

        if (condition == null) condition = new Expr.Literal(true);
        body = new Stmt.While(condition, body);

        if (initalizer != null) {
            body = new Stmt.Block(Arrays.asList(initalizer, body));
        }
        return body;
    }

    private Stmt ifStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'if'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after if condition");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(ELSE)) {
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    private Stmt returnStatement() {
        Token keyword = previous();
        Expr value = null;
        if (!check(SEMICOLON)) {
            value = expression();
        }
        consume(SEMICOLON, "Expect ';' after return value.");
        return new Stmt.Return(keyword, value);
    }

    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Expect variable name.");

        Expr initalizer = null;
        if (match(EQUAL)) {
            initalizer = expression();
        }

        consume(SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Var(name, initalizer);
    }

    private Stmt whileStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'if'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after if condition");

        loopLevel++;
        Stmt body = statement();
        loopLevel--;
        return new Stmt.While(condition, body);
    }

    private Stmt breakStatement() {
        if (!(loopLevel > 0)) {
            throw error(previous(), "'break' used outside loop.");
        }
        consume(SEMICOLON, "Expect ';' after 'break'.");
        return new Stmt.Break();
    }

    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }

    private Stmt.Function function(String kind) {
        Token name = consume(IDENTIFIER, "Expect " + kind + " name.");
        return new Stmt.Function(name, functionBody(kind));
    }

    private Expr.Function functionBody(String kind) {
        List<Token> parameters = null;

        if (!kind.equals("method") || check(LEFT_PAREN)) {
            consume(LEFT_PAREN, "Expect '(' after " + kind + " name");
            parameters = new ArrayList<>();
            if (!check(RIGHT_PAREN)) {
                do {
                    if (parameters.size() >= MAX_ARGS) {
                        error(peek(), "Cannot have more than " + MAX_ARGS + " parameters.");
                    }

                    parameters.add(consume(IDENTIFIER, "Expect parameter name."));
                } while (match(COMMA));
            }
            consume(RIGHT_PAREN, "Expect ')' after parameters.");
        }

        consume(LEFT_BRACE, "Expect '{' before " + kind);
        List<Stmt> body = block();
        return new Expr.Function(parameters, body);
    }

    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }

    private Expr assignment() {
        Expr expr = conditional();

        if (match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable) expr).name;
                return new Expr.Assign(name, value);
            } else if (expr instanceof Expr.Get) {
                Expr.Get get = ((Expr.Get) expr);
                return new Expr.Set(get.object, get.name, value);
            } else if (expr instanceof Expr.IndexGet) {
                Expr.IndexGet get = ((Expr.IndexGet) expr);
                return new Expr.IndexSet(get.indexee, get.bracket, get.index, value);
            }

            error(equals, "Invalid assignment target.");
        }
        return expr;
    }

    private Expr array() {
        List<Expr> elements = new ArrayList<>();
        if (!check(RIGHT_BRACKET)) {
            do {
                elements.add(expression());
            } while (match(COMMA));
        }

        Token bracket = consume(RIGHT_BRACKET, "Expect ']' after list initializer.");

        return new Expr.Array(bracket, elements);
    }

    private Expr conditional() {
        Expr expr = or();

        if (match(QUESTION_MARK)) {
            Expr thenBranch = expression();
            consume(COLON, "Expect ':' after then-branch of conditional expression.");
            Expr elseBranch = conditional();
            expr = new Expr.Conditional(expr, thenBranch, elseBranch);
        }
        return expr;
    }

    private Expr or() {
        Expr expr = and();

        while (match(OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Expr and() {
        Expr expr = equality();

        while (match(AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Expr expression() {
        return assignment();
    }

    private Expr leftAssociative(Supplier<Expr> operand, TokenType... matches) {
        Expr expr = operand.get();

        while(match(matches)) {
            Token operator = previous();
            Expr right = operand.get();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr equality() {
        return leftAssociative(this::comparison, BANG_EQUAL, EQUAL_EQUAL);
        /*Expr expr = comparison();

        while(match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;*/
    }

    private Expr comparison() {
        return leftAssociative(this::addition, GREATER, GREATER_EQUAL, LESS, LESS_EQUAL);
/*        Expr expr = addition();int i = 0

        while(match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = addition();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;*/
    }

    private Expr addition() {
        return leftAssociative(this::multiplication, MINUS, PLUS);
        /*Expr expr = multiplication();

        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = multiplication();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;*/
    }

    private Expr multiplication() {
        return leftAssociative(this::unary, SLASH, STAR);
        /*Expr expr = unary();

        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;*/
    }

    private Expr unary() {
        if (match(BANG, MINUS, PLUS_PLUS, MINUS_MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right, false);
        }

        // Unary prefix increment and decrement does not allow chaining
        if (match(PLUS_PLUS, MINUS_MINUS)) {
            Token operator = previous();
            Expr right = primary();
            return new Expr.Unary(operator, right, false);
        }

        return postfix();
    }

    private Expr postfix() {
        if (matchNext(PLUS_PLUS, MINUS_MINUS)) {
            Token operator = peek();
            current--;
            Expr left = primary();
            advance();
            return new Expr.Unary(operator, left, true);
        }
        return call();
    }

    private Expr call() {
        Expr expr = primary();
        while (true) {
            if (match(LEFT_PAREN)) {
                expr = finishCall(expr);
            } else if (match(DOT)) {
                Token name = consume(IDENTIFIER, "Expect property name after '.'.");
                expr = new Expr.Get(expr, name);
            } else if (match(LEFT_BRACKET)) {
                expr = finishIndexGet(expr);
            } else {
                break;
            }
        }

        return expr;
    }

    private Expr finishIndexGet(Expr indexee) {
        Expr index = expression();
        Token bracket = consume(RIGHT_BRACKET, "Expect ']' after index.");
        return new Expr.IndexGet(indexee, bracket, index);
    }

    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (arguments.size() >= MAX_ARGS) {
                    error(peek(), "Cannot have more than " + MAX_ARGS + " arguments");
                }
                arguments.add(expression());
            } while (match(COMMA));
        }

        Token paren = consume(RIGHT_PAREN, "Expect ')' after arguments.");

        return new Expr.Call(callee, paren, arguments);
    }

    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);
        if (check(FUN) && !checkNext(IDENTIFIER)) {
            advance();
            return functionBody("function");
        }
        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(SUPER)) {
            Token keyword = previous();
            consume(DOT, "Expect '.' after 'super'.");
            Token method = consume(IDENTIFIER, "Expect superclass method name.");
            return new Expr.Super(keyword, method);
        }

        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
        }

        if (match(THIS)) return new Expr.This(previous());

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        if (match(LEFT_BRACKET)) {
            return array();
        }

        throw error(peek(), "Expect expression");
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }

        return false;
    }

    private boolean matchNext(TokenType... types) {
        for (TokenType type : types) {
            if (checkNext(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();

        throw error(peek(), message);
    }

    private boolean check(TokenType tokenType) {
        if (isAtEnd()) {
            return false;
        }
        return peek().type == tokenType;
    }

    private boolean checkNext(TokenType tokenType) {
        if (isAtEnd()) {
            return false;
        }
        if (tokens.get(current + 1).type == EOF) {
            return false;
        }
        return tokens.get(current + 1).type == tokenType;
    }

    private Token advance() {
        if (!isAtEnd()) {
            current++;
        }
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return;

            switch (peek().type) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }
            advance();
        }
    }
}
