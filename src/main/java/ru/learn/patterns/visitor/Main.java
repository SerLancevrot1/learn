package ru.learn.patterns.visitor;

import java.util.List;

/**
 * Visitor — выносит разные операции над фигурами в отдельные классы.
 * Один посетитель вычисляет площадь, другой формирует описание. Фигура выбирает нужную перегрузку visit().
 */
public class Main {
    public static void main(String[] args) {
        List<Shape> shapes = List.of(new Circle(2), new Rectangle(3, 4));

        var area = new AreaVisitor();
        var description = new DescriptionVisitor();
        for (Shape shape : shapes) {
            System.out.println(shape.accept(description) + ": " + shape.accept(area));
        }
    }

    interface Shape {
        <T> T accept(ShapeVisitor<T> visitor);
    }

    interface ShapeVisitor<T> {
        T visit(Circle circle);
        T visit(Rectangle rectangle);
    }

    record Circle(double radius) implements Shape {
        @Override
        public <T> T accept(ShapeVisitor<T> visitor) {
            // Здесь this имеет тип Circle, поэтому выбирается перегрузка visit(Circle).
            return visitor.visit(this);
        }
    }

    record Rectangle(double width, double height) implements Shape {
        @Override
        public <T> T accept(ShapeVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    static class AreaVisitor implements ShapeVisitor<Double> {
        @Override
        public Double visit(Circle circle) {
            return Math.PI * circle.radius() * circle.radius();
        }

        @Override
        public Double visit(Rectangle rectangle) {
            return rectangle.width() * rectangle.height();
        }
    }

    static class DescriptionVisitor implements ShapeVisitor<String> {
        @Override
        public String visit(Circle circle) {
            return "Circle with radius " + circle.radius();
        }

        @Override
        public String visit(Rectangle rectangle) {
            return "Rectangle " + rectangle.width() + " x " + rectangle.height();
        }
    }
}
