
import java.util.Scanner;

public class Main
{
	public static void main(String[] args)
	{
		Scanner reader = new Scanner(System.in);

		int n = reader.nextInt();
		int mat[][] = new int[n][n];

		for (int i = 0; i < n; ++i)
		{
			for (int j = 0; j < n; ++j)
				mat[i][j] = reader.nextInt();
		}

		String s = String.format("n = %d", n);
		System.out.println(s);

		for (int i = 0; i < n; ++i)
		{
			s = "";
			for (int j = 0; j < n; ++j)
				s += String.format("%d ", mat[i][j]);

			System.out.println(s);
		}
	}
}
