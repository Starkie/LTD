package Ejemplos.Easy;

public class Eval_4 {

    public static void main(String[] args) throws Exception {
        int x = 5;
        if (x > 0) {
            Object[] result = metodo_1(x);
            x = (int) result[0];
        }
    }

    private static Object metodo_1(int x)[] {
        System.out.println("1");
        if (x > 50) {
            x--;
            System.out.println("2");
        } else if (x > 0) {
            System.out.println("3");
            if (x > 100) {
                x++;
                System.out.println("4");
            } else {
                x--;
                System.out.println("5");
                if (x > 1) {
                    Object[] result = metodo_2(x);
                    x = (int) result[0];
                }
                System.out.println("7");
            }
        }
        if (x > 0) {
            return metodo_1(x);
        }
        return new Object[] { x };
    }

    private static Object metodo_2(int x)[] {
        x--;
        System.out.println("6");
        Exception e = new ArrayIndexOutOfBoundsException();
        throw e;
        if (x > 1) {
            return metodo_2(x);
        }
        return new Object[] { x };
    }
}
