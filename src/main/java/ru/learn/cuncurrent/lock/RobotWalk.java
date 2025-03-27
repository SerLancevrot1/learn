package ru.learn.cuncurrent.lock;

import java.util.concurrent.atomic.AtomicBoolean;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RobotWalk implements Runnable {

    private final String name;

    private static final AtomicBoolean b = new AtomicBoolean(false);

    @Override
    public void run() {
        while (true) {
            if ((b.get() && name.equals("Left")) || (!b.get() && name.equals("Right"))) {
                System.out.println("Step by " + name);
                b.set(!b.get());
            }
        }
    }

    public static void main(String[] args) {
        Thread left = new Thread(new RobotWalk("Left"));
        Thread right = new Thread(new RobotWalk("Right"));

        left.start();
        right.start();
    }
}
