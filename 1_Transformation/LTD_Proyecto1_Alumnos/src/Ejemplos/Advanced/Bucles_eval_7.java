package Ejemplos.Advanced;

public class Bucles_eval_7 {
	public static void main(String[] args) throws Exception
	{
		int resultado = sumatorio(1);
		System.out.println(resultado);
	}
	
	public static int sumatorio(int x)
	{
		// BUCLE WHILE (sin anidamiento)
		System.out.println("Empieza bucle WHILE:");		
		
		try {  // EL BUCLE ESTÁ DENTRO DE UN TRY-CATCH
			while (x<=10)
			{
				x++; 
				x/=0;
			}
		}catch(Exception E) {System.out.println("Ha habido una excepción");}
		return 42;
	}	
}
