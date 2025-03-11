package ru.learn.generic;

import java.math.BigDecimal;

import ru.learn.generic.dto.Camera;
import ru.learn.generic.dto.Product;

public class Main {

    public static void main(String[] args) {

        Camera camera = new Camera("Camera", 100, 13);

        CommonClassWithGenericMethods commonClassWithGenericMethods = new CommonClassWithGenericMethods();
        commonClassWithGenericMethods.printAny(camera);
        commonClassWithGenericMethods.printAny("STRING IN <T>");
        var res = commonClassWithGenericMethods.printAnyAndReturn(camera);
        assert res != null;

        GenericClass<Product> genericClass = new GenericClass<>();
        genericClass.printAny(camera);
        genericClass.printAnyAndReturn(camera);
        System.out.println(genericClass.returnString(camera));

        GenericClassPair<String, Camera> genericClassPair = new GenericClassPair<>();
        genericClassPair.printAny("Nicon", camera);
        System.out.println(genericClassPair.printAnyAndReturn("Nicon", camera));

        CommonClassConstraint commonClassConstraint = new CommonClassConstraint();
        commonClassConstraint.printWithConstraint(1);
        commonClassConstraint.printWithConstraint(BigDecimal.valueOf(1));

    }
}
