package assighnment1eq;


/**
 * Created with IntelliJ IDEA.
 * User: cvyp
 * Date: 11/11/12
 * Time: 9:19 PM
 * To change this template use File | Settings | File Templates.
 */
public class SquareRoots {
    public void printRoots(int a, int b, int c){
        double x1, x2;
        int d;
        if (a != 0){
            d = b * b - 4 * a * c;
            if (d >= 0){
                x1 = (- b + Math.sqrt(d))/(2 * a);
                x2 = (- b - Math.sqrt(d))/(2 * a);
                System.out.print("x1 = " + x1 + "; x2 = " + x2);
            } else System.out.print("There's no solution on R");
        } else System.out.print("It's not a quadratic equation");
    }
}
