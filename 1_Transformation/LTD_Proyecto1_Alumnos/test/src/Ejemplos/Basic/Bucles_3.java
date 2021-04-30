package Ejemplos.Basic;

public class Bucles_3 {

    public static void main(String[] args) {
        int x;
        System.out.println("Empieza bucle FOR:");
        for (x = 1; x <= 10; x++) {
            System.out.print(" " + x);
        }
        System.out.println();
        System.out.println("Empieza bucle WHILE:");
        x = 1;
        if (x <= 10) {
            Object[] result = metodo_1(x);
            x = (int) result[0];
        }
        System.out.println();
        System.out.println("Empieza bucle DO WHILE:");
        x = 1;
        if (x <= 10) {
            {
                System.out.print(" " + x);
                x++;
            }
            Object[] result = metodo_2(x);
            x = (int) result[0];
        }
        System.out.println();
    }

    private static Object metodo_1(int x)[] {
        System.out.print(" " + x);
        x++;
        if (x <= 10) {
            return metodo_1(x);
        }
        return new Object[] { x };
    }

    private static Object metodo_2(int x)[] {
        System.out.print(" " + x);
        x++;
        if (x <= 10) {
            return metodo_2(x);
        }
        return new Object[] { x };
    }
}
