
import java.util.Scanner;
import java.lang.Math;
import java.lang.Thread;
import java.lang.System;

public class Main
{
    public static void main(String[] args)
    {
        Scanner reader = new Scanner(System.in);

        IntMatrix mat = new IntMatrix(reader);

        System.out.println("[INITIAL MATRIX]");
        System.out.print(mat.ToString());

        System.out.println("[FINAL MATRIX]");
        System.out.print(mat.CalcParallelMatrixFloydWarshall(4).ToString());
        // System.out.println("[CORRECT MATRIX, 1 itr]");
        // System.out.print(mat.CalcMatrixFloydWarshall().ToString());
        // System.out.println("[CORRECT SIMPLE METHOD MATRIX]");
        // System.out.print(mat.CalcSimpleFloydWarshall().ToString());
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

        // new copy to store results
        int matCopy[][] = new int[_n][_n];
        CopyTo(matCopy);

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

                    matCopy[i][j] = result;
                }
            }
        }

        CopyFrom(matCopy); // update our internal matrix
        PrepareForOutput();
        return this;
    }

    public IntMatrix CalcParallelMatrixFloydWarshall(int threads)
    {
        PrepareForFloydWarshall();

        int q = (int)Math.sqrt(threads);
        assert(q * q == threads);
        assert(_n % q == 0); // we're splitting the matrix[n][n], so q must divide n

        int sn = _n / q;

        int iterations = (int)Math.ceil(Math.log(_n) / Math.log(2));

        // each iteration will generate paths of length 2^itr
        // last iteration must have paths of length _n
        assert(Math.pow(2, iterations) >= _n);

        for (int itr = 0; itr < iterations; ++itr)
        {
            ParallelIntMatrixTask tasks[][] = new ParallelIntMatrixTask[q][q];
            for (int row = 0; row < q; ++row)
            {
                for (int col = 0; col < q; ++col)
                    tasks[row][col] = new ParallelIntMatrixTask(_n, _mat, row, col, q, sn);
            }

            // start all threads
            for (int row = 0; row < q; ++row)
            {
                for (int col = 0; col < q; ++col)
                    tasks[row][col].start();
            }

            // wait for all threads to finish
            for (int row = 0; row < q; ++row)
            {
                for (int col = 0; col < q; ++col)
                {
                    try
                    {
                        tasks[row][col].join();
                    }
                    catch (InterruptedException ex)
                    {
                        System.err.println("InterruptedException: " + ex.getMessage());
                        System.exit(1);
                    }
                }
            }

            for (int row = 0; row < q; ++row)
            {
                for (int col = 0; col < q; ++col)
                {
                    // got our result, copy to the original matrix
                    int subMat[][] = tasks[row][col].GetSubMatrix();

                    for (int i = 0; i < sn; ++i)
                    {
                        for (int j = 0; j < sn; ++j)
                            _mat[row * sn + i][col * sn + j] = subMat[i][j];
                    }
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

    // copy to matrix
    private void CopyTo(int mat[][])
    {
        for (int i = 0; i < _n; ++i)
        {
            for (int j = 0; j < _n; ++j)
                mat[i][j] = _mat[i][j];
        }
    }

    // copy from matrix
    private void CopyFrom(int mat[][])
    {
        for (int i = 0; i < _n; ++i)
        {
            for (int j = 0; j < _n; ++j)
                _mat[i][j] = mat[i][j];
        }
    }
}

class ParallelIntMatrixTask extends Thread
{
    // references to the 'full' matrix
    private final int _n;
    private final int _matRef[][];

    // inner matrix coords/private work copy of the matrix block we need to compute
    private final int _row;
    private final int _col;
    private final int _q;
    private final int _sn; // sub matrix size
    private int _subMat[][];

    public ParallelIntMatrixTask(int n, int matRef[][], int row, int col, int q, int sn)
    {
        _n = n;
        _matRef = matRef; // just a reference, don't deep copy

        _row = row;
        _col = col;
        _q = q;
        _sn = sn;

        _subMat = new int[sn][sn];
        for (int i = 0; i < _sn; ++i)
        {
            for (int j = 0; j < _sn; ++j)
                _subMat[i][j] = _matRef[_row * sn + i][_col * sn + j];
        }
    }

    public void run()
    {
        // compute for each value in the sub matrix
        for (int i = 0; i < _sn; ++i)
        {
            for (int j = 0; j < _sn; ++j)
            {
                // no synchronization/thread communication needed since we're
                //  in shared memory, and we have access to the entire matrix
                for (int k = 0; k < _n; ++k)
                {
                    int aIdxI = _row * _sn + i;
                    int aIdxJ = ((_row * _sn + i) + k) % _n;
                    int bIdxI = ((_row * _sn + i) + k) % _n;
                    int bIdxJ = _col * _sn + j;

                    // Fox's algorithm, in A * B = C, move A to left and
                    //  shift B 'up' with each iteration
                    _subMat[i][j] = Math.min(_subMat[i][j],
                        (_matRef[aIdxI][aIdxJ] +
                         _matRef[bIdxI][bIdxJ]));
                }
            }
        }
    }

    public int[][] GetSubMatrix() { return _subMat; }
}
