package ru.learn.patterns.state;

/**
 * State — меняет действие кнопки в зависимости от режима плеера.
 * Текущее состояние обрабатывает нажатие и определяет, в какое состояние перейти.
 */
public class Main {
    public static void main(String[] args) {
        var player = new Player();
        player.pressPlay();
        player.pressPlay();
        player.pressPlay();
    }

    static class Player {
        private PlaybackState state = new Paused();

        void pressPlay() {
            // Объект состояния выполняет действие и возвращает режим для следующего нажатия.
            state = state.onPlayPressed();
        }
    }

    interface PlaybackState {
        PlaybackState onPlayPressed();
    }

    static class Paused implements PlaybackState {
        @Override
        public PlaybackState onPlayPressed() {
            System.out.println("Playing");
            return new Playing();
        }
    }

    static class Playing implements PlaybackState {
        @Override
        public PlaybackState onPlayPressed() {
            System.out.println("Paused");
            return new Paused();
        }
    }
}
