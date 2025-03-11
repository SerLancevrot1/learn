package ru.learn.patterns.command;

public class NoCommand implements Command {
    @Override
    public void execute() {
        System.out.println("Do nothing");
    }
}
