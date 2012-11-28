package assignment2;

/**
 * Created with IntelliJ IDEA.
 * User: cvyp
 * Date: 11/17/12
 * Time: 2:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class MyApp {
    public static void main(String[] args) {
        Shape s = null;
        if (args[1].equals("triangle")) {
            int a = Integer.parseInt(args[2]);
            int b = Integer.parseInt(args[3]);
            int c = Integer.parseInt(args[4]);
            s = new Triangle(a, b, c);
        }
        else if (args[1].equals("circle")) {
            int r = Integer.parseInt(args[2]);
            s = new Circle(r);
        }

        if (s != null) {
         if (args[0].equals("area")) {
            System.out.println(s.area());
         }
         else if (args[0].equals("perimeter")){
             System.out.println(s.perimeter());
         }

       }


    }
}
