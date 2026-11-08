package ru.learn.patterns.facade;

/**
 * Facade — скрывает последовательность действий, необходимых для запуска фильма.
 * Клиент вызывает watch(), а HomeCinema координирует проектор и звук.
 */
public class Main {
    public static void main(String[] args) {
        var cinema = new HomeCinema(new Projector(), new SoundSystem());
        cinema.watch("Arrival");
    }

    record HomeCinema(Projector projector, SoundSystem soundSystem) {
        void watch(String movie) {
            projector.turnOn();
            soundSystem.setVolume(20);
            projector.play(movie);
        }
    }

    static class Projector {
        void turnOn() {
            System.out.println("Projector on");
        }

        void play(String movie) {
            System.out.println("Playing: " + movie);
        }
    }

    static class SoundSystem {
        void setVolume(int volume) {
            System.out.println("Volume: " + volume);
        }
    }
}
