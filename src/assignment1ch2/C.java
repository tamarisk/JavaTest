package assignment1ch2;

/**
 * Created with IntelliJ IDEA.
 * User: cvyp
 * Date: 11/11/12
 * Time: 7:56 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class C {
    public void m(){}
    void n(){
        C c = new A();
        c.m();
    }
}
