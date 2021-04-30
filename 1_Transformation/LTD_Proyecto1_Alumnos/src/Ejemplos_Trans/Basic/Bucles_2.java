package Ejemplos.Basic;

public class Bucles_2 {

    public static void main(String[] args) {
        System.out.println("Empieza bucle WHILE anidado a otro WHILE:");
        int x = 1;
        char y = 'a';
        if (x <= 10) {
            Object[] result = metodo_1(x, y);
            x = (int) result[0];
            y = (char) result[1];
        }
        System.out.println();
    }

    private static Object metodo_1(int x, char y)[] {
        {
        }
        if (x <= 10) {
            return metodo_1(x, y);
        }
        return new Object[] { x, y };
    }
}
