package ru.learn.patterns.proxy;

import java.util.function.Supplier;

/**
 * Proxy — откладывает создание изображения до первого показа.
 * Клиент работает с Image, а LazyImage контролирует момент загрузки и переиспользует результат.
 */
public class Main {
    public static void main(String[] args) {
        Image image = new LazyImage(() -> new DiskImage("cover.png"));
        System.out.println("Proxy created");
        image.display();
        image.display();
    }

    interface Image {
        void display();
    }

    record DiskImage(String filename) implements Image {
        DiskImage {
            System.out.println("Loading " + filename);
        }

        @Override
        public void display() {
            System.out.println("Displaying " + filename);
        }
    }

    static class LazyImage implements Image {
        private final Supplier<Image> loader;
        private Image image;

        LazyImage(Supplier<Image> loader) {
            this.loader = loader;
        }

        @Override
        public void display() {
            // Пример рассчитан на один поток. Первое обращение создаёт изображение, остальные используют готовое.
            if (image == null) {
                image = loader.get();
            }
            image.display();
        }
    }
}
