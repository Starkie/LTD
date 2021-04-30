package Ejemplos.Easy;

public class Test_11 {

    public static void main(String[] args) {
        int x = 0;
        if (x > 1) {
            x = 1;
            if (x > 2) {
                Object[] result = metodo_1(x);
                x = (int) result[0];
            }
        }
    }

    private static Object metodo_1(int x)[] {
        x = 2;
        if (x > 3) {
            Object[] result = metodo_2(x);
            x = (int) result[0];
        }
        if (x > 2) {
            return metodo_1(x);
        }
        return new Object[] { x };
    }

    private static Object metodo_2(int x)[] {
        x = 3;
        if (x > 4) {
            x = 4;
            if (x > 5) {
                x = 5;
            }
        }
        if (x > 3) {
            return metodo_2(x);
        }
        return new Object[] { x };
    }
}
