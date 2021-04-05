package Ejemplos.Easy;

public class Eval_4 {

	public static void main(String[] args) throws Exception
	{		
		int x=5;
		while (x>0)
		{
			System.out.println("1");
			if (x>50)
			{
				x--;
				System.out.println("2");
			}
			else if (x>0)
			{
				System.out.println("3");
				if (x>100)
				{
					x++;
					System.out.println("4");
				}
				else
				{
					x--;
					System.out.println("5");
					while (x>1)
					{
						x--;
						System.out.println("6");
						Exception e = new ArrayIndexOutOfBoundsException();
						throw e;
						//x--;
					}
					System.out.println("7");
				}	
			}
		}
	}
}
