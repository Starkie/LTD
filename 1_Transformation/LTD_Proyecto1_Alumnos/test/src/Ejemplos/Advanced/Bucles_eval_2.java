package Ejemplos.Advanced;

public class Bucles_eval_2 {

    public static void main(String[] args) {
        int resultado = sumatorio(1);
        System.out.println(resultado);
    }

    public static int sumatorio(int x) {
        System.out.println("Empieza bucle WHILE:");
        if (x <= 10) {
            Object[] result = metodo_1(x);
            x = (int) result[0];
        }
        return x;
    }

    private static Object metodo_1(int x)[] {
        {
        }
        if (x <= 10) {
            return metodo_1(x);
        }
        return new Object[] { x };
    }
}
