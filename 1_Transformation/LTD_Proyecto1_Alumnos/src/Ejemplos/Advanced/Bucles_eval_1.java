package Ejemplos.Advanced;

public class Bucles_eval_1 {
	
	public static void main(String[] args)
	{
		int resultado = sumatorio(1);
		System.out.println(resultado);
	}
	
	public static int sumatorio(int x)
	{
		// BUCLE WHILE (sin anidamiento)
		System.out.println("Empieza bucle WHILE:");		
		while (x<=10)
		{
			x++;
		}
		return x;
	}	
}
