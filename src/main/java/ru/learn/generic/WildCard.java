package ru.learn.generic;

import java.util.List;

import ru.learn.generic.dto.Phone;
import ru.learn.generic.dto.Product;
import ru.learn.generic.dto.Samsung;

public class WildCard {
/**
 * НЕ ИСПОЛЬЗОВАТЬ WILD_CARD ЕСЛИ МЕТОД И ПОЛУЧАЕТ И ЗАПИСЫВАЕТ В СТРУКТУРУ
 *
 *

 USE_AN_EXTENDS_WILDCARD_WHEN_YOU_ONLY_GET_VALUES_OUT_OF_A_STRUCTURE
 USE_A_SUPER_WILDCARD_WHEN_YOU_ONLY_PUT_VALUES_INTO_A_STRUCTURE.
 AND_DON'T_USE_A_WILDCARD_WHEN_YOU_BOTH_WANT_TO_GET_AND_PUT_FROM/TO_A_STRUCTURE.


 */

    public <T> List<? super T> copy(List<? extends T> src, List<? super T> dest) {
        dest.addAll(src);
        return dest;
    }

    public void upperBounded(List<? extends Phone> list){
        Phone phone2 = list.get(0);
        System.out.println(phone2);

        Product phone1 = list.get(0);
        System.out.println(phone1);

        Samsung phone = (Samsung) list.get(0);
        System.out.println(phone);


//        Samsung phone1 = list.get(0); COMPILE ERROR
//        list.add(phone);
//        list.add(phone1);
//        list.add(phone2);
    }

    public void lowerBounded(List<? super Phone> list) {
        Object object = list.get(0); //Взять только Object object
        System.out.println(object);

        list.add(new Phone());
        list.add(new Samsung());

//        list.add(new Product()); COMPILE ERROR
    }

    public void unbounded(List<?> list) { //ЛУЧШЕ ВМЕСТО ЭТОГО ИСПОЛЬЗОВАТЬ <T>

    }
}
