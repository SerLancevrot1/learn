package ru.learn.patterns.command;

public class Main {

    public static void main(String[] args) {

        DistantController distantController = new DistantController(4);
        //пульт управления умным домом

        LightInRoom lightInRoom = new LightInRoom();
        GarageDoor garageDoor = new GarageDoor();
        Curtains curtains = new Curtains();

        // в лямбдах создаю анонимный класс. Если в интерфейсе больше одного абстрактного метода - нужно делать реализацию классов самому
        distantController.setCommand(0, () -> lightInRoom.lightOn(), () -> lightInRoom.lightOff());
        distantController.setCommand(1, () -> curtains.open(), () -> curtains.close());
        distantController.setCommand(2, () -> garageDoor.open(), () -> garageDoor.close());

        distantController.pressOnButton(0);
        distantController.pressOffButton(0);

        distantController.pressOnButton(1);
        distantController.pressOffButton(1);

        distantController.pressOnButton(2);
        distantController.pressOffButton(2);

        distantController.pressOnButton(3);
        distantController.pressOffButton(3);
    }
}
