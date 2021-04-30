package Ejemplos.Basic;

public class Bucles_4 {

    public static void main(String[] args) {
        int x = 1;
        if (x < 10) {
            Object[] result = metodo_1(x);
            x = (int) result[0];
        }
        int suma = 0;
        int y = 1;
        if (y < 10) {
            Object[] result = metodo_2(suma, y);
            suma = (int) result[0];
            y = (int) result[1];
        }
        System.out.println(suma);
        int sumatorio = 0;
        int min = 10;
        int max = 100;
        for (int num = min; num <= max; num++) {
            sumatorio += num;
        }
        System.out.println(sumatorio);
        int count = 0;
        if (count < 10) {
            Object[] result = metodo_3(count);
            count = (int) result[0];
        }
        System.out.println(count);
    }

    private static Object metodo_1(int x)[] {
        {
        }
        if (x < 10) {
            return metodo_1(x);
        }
        return new Object[] { x };
    }

    private static Object metodo_2(int suma, int y)[] {
        {
        }
        if (y < 10) {
            return metodo_2(suma, y);
        }
        return new Object[] { suma, y };
    }

    private static Object metodo_3(int count)[] {
        {
        }
        if (count < 10) {
            return metodo_3(count);
        }
        return new Object[] { count };
    }
}
