package generics;

import com.sun.management.OSMBeanFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: cvyp
 * Date: 11/21/12
 * Time: 9:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class A {

    static List<String> list = new ArrayList<String>();

    Map<Integer, String> m = new HashMap();

    public static void main(String[] args) {
        list.add("aaa");

        GenA<String> g1 = new GenA<String>();

        GenA<? extends A> g2 = new GenA<D>();
        GenA<? super A> g3 = new GenA<Object>();
        GenA<?> g4;

    }

    public class D extends A {

    };
}
