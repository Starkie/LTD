package Ejemplos.Easy;

public class Eval_3 {

    public static void main(String[] args) {
        int x = 5;
        System.out.println("1");
        if (x > 0) {
            Object[] result = metodo_1(x);
            x = (int) result[0];
        }
    }

    private static Object metodo_1(int x)[] {
        System.out.println("2");
        if (x <= 1) {
            System.out.println("3");
            x--;
        } else if (x <= 0) {
            System.out.println("4");
            if (x <= 3) {
                System.out.println("5");
                x++;
            } else {
                System.out.println("6");
                x--;
                if (x > 1) {
                    Object[] result = metodo_2(x);
                    x = (int) result[0];
                }
            }
        }
        if (x > 0) {
            return metodo_1(x);
        }
        return new Object[] { x };
    }

    private static Object metodo_2(int x)[] {
        System.out.println("7");
        x--;
        if (x > 1) {
            return metodo_2(x);
        }
        return new Object[] { x };
    }
}
