package assignment2;

/**
 * Created with IntelliJ IDEA.
 * User: cvyp
 * Date: 11/17/12
 * Time: 2:51 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class Shape {
    abstract double perimeter();
    abstract double area();
    void init() throws invalidDataException{}
}
