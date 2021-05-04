package ejemplos;

public class Bucles_7 {
	
	public static void main(String[] args) 
	{
		int x=0;
		
		while (x<=3) {
			switch (x) {
				case 1: System.out.println("1");
			        	break;
				case 2: if (x>1) System.out.println("2");
						break;
				case 3: while (x<1) System.out.println("3");
						break;
			}
			
			x++;
		}
	}
}