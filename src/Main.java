
import java.util.Scanner;
import java.lang.Math;

public class Main
{
    public static void main(String[] args)
    {
        Scanner reader = new Scanner(System.in);

        IntMatrix mat = new IntMatrix(reader);

        System.out.print(mat.ToString());

        System.out.print(mat.CalcFloydWarshallSimple().ToString());
    }
}

class IntMatrix
{
    private int _n;
    private int _mat[][];

    public IntMatrix(Scanner scanner)
    {
        _n = scanner.nextInt();
        _mat = new int[_n][_n];

        for (int i = 0; i < _n; ++i)
        {
            for (int j = 0; j < _n; ++j)
                _mat[i][j] = scanner.nextInt();
        }
    }


    public String ToString()
    {
        String s = String.format("n = %d\n", _n);

        for (int i = 0; i < _n; ++i)
        {
            for (int j = 0; j < _n; ++j)
                s += String.format("%d ", _mat[i][j]);

            s += "\n";
        }

        return s;
    }

    public IntMatrix CalcFloydWarshallSimple()
    {
        // non-matrix Floyd Warshall requires:
        // _mat[i][i] == 0
        // - infinite cost for unconnected vertexes
        int infinity = 20000; // Integer.MAX_VALUE leads to overflows

        for (int i = 0; i < _n; ++i)
        {
            for (int j = 0; j < _n; ++j)
            {
                if (i == j)
                    assert(_mat[i][j] == 0);
                else if (_mat[i][j] == 0)
                    _mat[i][j] = infinity;
            }
        }

        for (int k = 0; k < _n; ++k)
        {
            for (int i = 0; i < _n; ++i)
            {
                for (int j = 0; j < _n; ++j)
                {
                    if (_mat[i][j] > _mat[i][k] + _mat[k][j])
                        _mat[i][j] = _mat[i][k] + _mat[k][j];
                }
            }
        }

        // turn back 'infinite cost' into 0
        for (int i = 0; i < _n; ++i)
        {
            for (int j = 0; j < _n; ++j)
            {
                if (_mat[i][j] >= infinity)
                    _mat[i][j] = 0;
            }
        }

        return this;
    }
}
