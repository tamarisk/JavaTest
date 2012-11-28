package assignment2;

/**
 * Created with IntelliJ IDEA.
 * User: cvyp
 * Date: 11/17/12
 * Time: 3:00 PM
 * To change this template use File | Settings | File Templates.
 */
public
class Triangle extends Shape{
    private int a;
    private int b;
    private int c;

    public Triangle(int a, int b, int c){
        if ((a + b > c) && (a + c > b) && (b + c > a)){
            this.a = a;
            this.b = b;
            this.c = c;
        } else System.out.println("Not triangle");

    }


    @Override
    double perimeter() {
        return a + b + c;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    double area() {
        double p = this.perimeter() / 2;
        return Math.sqrt(p * (p - a)*(p - b)*(p - c));  //To change body of implemented methods use File | Settings | File Templates.
    }
}
