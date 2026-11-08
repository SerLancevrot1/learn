package ru.learn.patterns.command;

/**
 * Command — позволяет пульту хранить действия для кнопок и выполнять их позже.
 * Пульт вызывает Command.execute(), а привязанная команда обращается к нужному устройству.
 */
public class Main {

    public static void main(String[] args) {

        DistantController distantController = new DistantController(4);

        LightInRoom lightInRoom = new LightInRoom();
        GarageDoor garageDoor = new GarageDoor();
        Curtains curtains = new Curtains();

        // Ссылка на метод реализует единственный абстрактный метод Command.execute().
        distantController.setCommand(0, lightInRoom::lightOn, lightInRoom::lightOff);
        distantController.setCommand(1, curtains::open, curtains::close);
        distantController.setCommand(2, garageDoor::open, garageDoor::close);

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
