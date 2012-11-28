package assignment2;

/**
 * Created with IntelliJ IDEA.
 * User: cvyp
 * Date: 11/17/12
 * Time: 2:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class Circle extends Shape {
    private double radius;

    public Circle(int r){
        this.radius = r;
    }

    @Override
    void init() throws invalidDataException {
        super.init();    //To change body of overridden methods use File | Settings | File Templates.
    }



    @Override
    double perimeter() {
        return 2 * Math.PI * radius;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    double area() {
        return radius * radius * Math.PI;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
