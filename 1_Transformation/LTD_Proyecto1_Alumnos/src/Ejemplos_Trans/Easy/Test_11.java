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
        {
        }
        if (x > 2) {
            return metodo_1(x);
        }
        return new Object[] { x };
    }
}
