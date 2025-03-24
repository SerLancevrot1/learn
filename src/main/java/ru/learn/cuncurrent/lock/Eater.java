package ru.learn.cuncurrent.lock;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@RequiredArgsConstructor
@Data
public class Eater {

    private final String name;
    private boolean isHungry = true;

    @SneakyThrows
    public void eatWith(Spoon spoon, Eater spouse) {
        while (true) {
            if (spoon.getOwner() != this) {
                Thread.sleep(1);
                continue;
            }

            if (spouse.isHungry) {
                System.out.printf("%s: You eat first my darling %s!%n", name, spouse.getName());
                spoon.serOwner(spouse);
                continue;
            }
            spoon.use();
            isHungry = false;
            System.out.printf("%s: I`m full, my darling %s%n!", name, spouse.getName());
            spoon.serOwner(spouse);
        }


    }
}
