
import java.util.Scanner;
import java.lang.Math;

public class Main
{
    public static void main(String[] args)
    {
        Scanner reader = new Scanner(System.in);

        IntMatrix mat = new IntMatrix(reader);

        System.out.println("[INITIAL MATRIX]");
        System.out.print(mat.ToString());

        System.out.println("[FINAL MATRIX]");
        System.out.print(mat.CalcMatrixFloydWarshall().ToString());
    }
}

class IntMatrix
{
    private int _n;
    private int _mat[][];
    private final static int INFINITY = 20000; // Integer.MAX_VALUE leads to overflows

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

    public IntMatrix CalcSimpleFloydWarshall()
    {
        PrepareForFloydWarshall();

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

        PrepareForOutput();
        return this;
    }

    public IntMatrix CalcMatrixFloydWarshall()
    {
        PrepareForFloydWarshall();

        int iterations = (int)Math.ceil(Math.log(_n) / Math.log(2));

        // each iteration will generate paths of length 2^itr
        // last iteration must have paths of length _n
        assert(Math.pow(2, iterations) >= _n);

        for (int itr = 0; itr < iterations; ++itr)
        {
            // just a matrix multiplication by self
            for (int i = 0; i < _n; ++i)
            {
                for (int j = 0; j < _n; ++j)
                {
                    int result = _mat[i][j];
                    for (int rowCol = 0; rowCol < _n; ++rowCol)
                        result = Math.min(result, _mat[rowCol][j] + _mat[i][rowCol]);

                    _mat[i][j] = result;
                }
            }
        }

        PrepareForOutput();
        return this;
    }

    private void PrepareForFloydWarshall()
    {
        // Floyd Warshall requires:
        // _mat[i][i] == 0
        // - infinite cost for unconnected vertexes

        for (int i = 0; i < _n; ++i)
        {
            for (int j = 0; j < _n; ++j)
            {
                if (i == j)
                    assert(_mat[i][j] == 0);
                else if (_mat[i][j] == 0)
                    _mat[i][j] = INFINITY;
            }
        }
    }

    private void PrepareForOutput()
    {
        // turn back 'infinite cost' into 0
        for (int i = 0; i < _n; ++i)
        {
            for (int j = 0; j < _n; ++j)
            {
                if (_mat[i][j] >= INFINITY)
                    _mat[i][j] = 0;
            }
        }
    }
}
