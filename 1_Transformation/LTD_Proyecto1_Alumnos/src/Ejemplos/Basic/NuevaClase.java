package Ejemplos.Basic;

public class NuevaClase
{
    public static void main(String[] args) throws Exception
    {
        int resultado = sumatorio(1);

        System.out.println(resultado);
    }

    public static int sumatorio(int x) throws Exception
    {
        // BUCLE WHILE (sin anidamiento)
        System.out.println("Empieza bucle WHILE:");

        int y = 2;

        while (x<=10)
        {
            x++;
            if(x == 2) {
                y =2;

                continue;
            }

            y++;

            System.out.println("El valor de Y" + y);

            continue;
        }

            return x;
	}
}
