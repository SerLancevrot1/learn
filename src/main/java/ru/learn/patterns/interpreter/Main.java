package ru.learn.patterns.interpreter;

import java.util.Map;

/**
 * Interpreter — вычисляет выражения небольшого языка по дереву операций.
 * Каждый узел вычисляет свою часть выражения. Значения переменных берутся из переданного контекста.
 */
public class Main {
    public static void main(String[] args) {
        // Дерево для (x + 2) * 3 собрано вручную, чтобы показать вычисление по узлам.
        Expression expression = new Multiply(new Add(new Variable("x"), new Number(2)), new Number(3));
        System.out.println(expression.interpret(Map.of("x", 4)));
    }

    interface Expression {
        int interpret(Map<String, Integer> variables);
    }

    record Number(int value) implements Expression {
        @Override
        public int interpret(Map<String, Integer> variables) {
            return value;
        }
    }

    record Variable(String name) implements Expression {
        @Override
        public int interpret(Map<String, Integer> variables) {
            Integer value = variables.get(name);
            if (value == null) {
                throw new IllegalArgumentException("Unknown variable: " + name);
            }
            return value;
        }
    }

    record Add(Expression left, Expression right) implements Expression {
        @Override
        public int interpret(Map<String, Integer> variables) {
            return left.interpret(variables) + right.interpret(variables);
        }
    }

    record Multiply(Expression left, Expression right) implements Expression {
        @Override
        public int interpret(Map<String, Integer> variables) {
            return left.interpret(variables) * right.interpret(variables);
        }
    }
}
