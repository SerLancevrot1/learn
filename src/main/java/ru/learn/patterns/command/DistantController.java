package ru.learn.patterns.command;

public class DistantController {

    Command[] commandsOn;
    Command[] commandsOff;

    public DistantController(int size) {
        commandsOn = new Command[size];
        commandsOff = new Command[size];

        for (int i = 0; i < size; i++) {
            commandsOn[i] = new NoCommand();
            commandsOff[i] = new NoCommand();
        }
    }

    public void setCommand(int buttonNum, Command commandOn, Command commandOff) {
        commandsOn[buttonNum] = commandOn;
        commandsOff[buttonNum] = commandOff;
    }

    public void pressOnButton(int buttonNum) {
        commandsOn[buttonNum].execute();
    }

    public void pressOffButton(int buttonNum) {
        commandsOff[buttonNum].execute();
    }
}



