package ru.learn.patterns.mediator;

import java.util.ArrayList;
import java.util.List;

/**
 * Mediator — сосредоточивает правила общения участников в одном месте.
 * Участник отправляет сообщение через посредника, а ChatRoom определяет получателей.
 */
public class Main {
    public static void main(String[] args) {
        var room = new ChatRoom();
        Participant anna = room.join("Anna");
        room.join("Boris");
        Participant vera = room.join("Vera");

        anna.send("Hello!");
        vera.send("Hi!");
    }

    interface Mediator {
        void broadcast(Participant sender, String message);
    }

    static class ChatRoom implements Mediator {
        private final List<Participant> participants = new ArrayList<>();

        Participant join(String name) {
            var participant = new Participant(name, this);
            participants.add(participant);
            return participant;
        }

        @Override
        public void broadcast(Participant sender, String message) {
            for (Participant participant : participants) {
                // Правило комнаты исключает отправителя из получателей рассылки.
                if (participant != sender) {
                    participant.receive(sender.name(), message);
                }
            }
        }
    }

    record Participant(String name, Mediator mediator) {
        void send(String message) {
            mediator.broadcast(this, message);
        }

        void receive(String from, String message) {
            System.out.println(name + " received from " + from + ": " + message);
        }
    }
}
