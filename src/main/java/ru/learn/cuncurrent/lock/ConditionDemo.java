package ru.learn.cuncurrent.lock;

import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Два потока по очереди двигают левую и правую ноги робота.
 * Condition позволяет ждать своей очереди, освобождая блокировку на время ожидания.
 */
public class ConditionDemo {

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition turnChanged = lock.newCondition();
    private boolean leftTurn = true;

    public static void main(String[] args) throws Exception {
        ConditionDemo robot = new ConditionDemo();
        FutureTask<Void> left = new FutureTask<>(() -> {
            robot.walk(true);
            return null;
        });
        FutureTask<Void> right = new FutureTask<>(() -> {
            robot.walk(false);
            return null;
        });
        Thread leftLeg = new Thread(left, "left-leg");
        Thread rightLeg = new Thread(right, "right-leg");
        rightLeg.start();
        leftLeg.start();
        try {
            left.get(5, TimeUnit.SECONDS);
            right.get(5, TimeUnit.SECONDS);
        } finally {
            left.cancel(true);
            right.cancel(true);
        }
    }

    private void walk(boolean leftLeg) throws InterruptedException {
        for (int step = 0; step < 3; step++) {
            lock.lockInterruptibly();
            try {
                // Сигнал означает, что состояние могло измениться. Заново проверяем, чей ход.
                while (leftTurn != leftLeg) {
                    turnChanged.await();
                }
                System.out.println(leftLeg ? "Левый шаг" : "Правый шаг");
                leftTurn = !leftTurn;
                turnChanged.signalAll();
            } finally {
                lock.unlock();
            }
        }
    }
}
