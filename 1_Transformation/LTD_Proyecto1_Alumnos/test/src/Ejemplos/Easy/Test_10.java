package Ejemplos.Easy;

public class Test_10 {

    public static void main(String[] args) {
        String[] fruits = new String[] { "Orange", "Apple", "Pear", "Strawberry" };
        String[] animals = new String[] { "Bear", "Monkey" };
        int x = 1;
        if (x <= 3) {
            Object[] result = metodo_1(fruits, animals, x);
            fruits = (String[]) result[0];
            animals = (String[]) result[1];
            x = (int) result[2];
        }
        if (x <= 6) {
            Object[] result = metodo_2(animals, x);
            animals = (String[]) result[0];
            x = (int) result[1];
        }
        System.out.println();
    }

    private static Object metodo_1(String[] fruits, String[] animals, int x)[] {
        System.out.println(" " + x);
        for (String fruit : fruits) {
            System.out.println(fruit);
            for (String animal : animals) {
                System.out.println(animal);
            }
        }
        x++;
        if (x <= 3) {
            return metodo_1(fruits, animals, x);
        }
        return new Object[] { fruits, animals, x };
    }

    private static Object metodo_2(String[] animals, int x)[] {
        for (String animal : animals) {
            System.out.println(animal);
            x++;
        }
        if (x <= 6) {
            return metodo_2(animals, x);
        }
        return new Object[] { animals, x };
    }
}
