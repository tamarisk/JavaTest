package assignment1ch1;

/**
 * Created with IntelliJ IDEA.
 * User: cvyp
 * Date: 11/11/12
 * Time: 6:10 PM
 * To change this template use File | Settings | File Templates.
 */
abstract class C {
    public abstract void m();

    void n(){
        //System.out.println("C.n");
        C c = new A();
        c.m();

    }

}
