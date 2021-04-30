package Ejemplos.Advanced;

public class Bucles_eval_7 {

    public static void main(String[] args) throws Exception {
        int resultado = sumatorio(1);
        System.out.println(resultado);
    }

    public static int sumatorio(int x) {
        System.out.println("Empieza bucle WHILE:");
        try {
            if (x <= 10) {
                Object[] result = metodo_1(x);
                x = (int) result[0];
            }
        } catch (Exception E) {
            System.out.println("Ha habido una excepción");
        }
        return 42;
    }

    private static Object metodo_1(int x)[] {
        x++;
        x /= 0;
        if (x <= 10) {
            return metodo_1(x);
        }
        return new Object[] { x };
    }
}
