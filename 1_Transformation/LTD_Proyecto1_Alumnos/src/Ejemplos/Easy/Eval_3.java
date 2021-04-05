package Ejemplos.Easy;

public class Eval_3 {

	public static void main(String[] args)
	{		
		int x=5;
		System.out.println("1");
		while (x>0)
		{
			System.out.println("2");
			if (x<=1)
			{
				System.out.println("3");
				x--;
			}
			else if (x<=0)
			{
				System.out.println("4");
				if (x<=3)
				{
					System.out.println("5");
					x++;
				}
				else
				{
					System.out.println("6");
					x--;
					while (x>1)
					{
						System.out.println("7");
						x--;
					}
				}	
			}
		}
	}
}
