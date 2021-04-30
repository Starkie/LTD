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
        {
        }
        if (x > 0) {
            return metodo_1(x);
        }
        return new Object[] { x };
    }
}
