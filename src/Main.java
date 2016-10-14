
import java.util.Scanner;
import java.lang.Math;
import java.lang.Thread;
import java.lang.System;
import java.io.File;
import java.io.FileNotFoundException;

public class Main
{
    public static final int ALGO_SIMPLE_FLOYD_WARSHALL = 0;
    public static final int ALGO_MATRIX_FLOYD_WARSHALL = 1;
    public static final int ALGO_PARALLEL_MATRIX_FLOYD_WARSHALL = 2;

    public static void main(String[] args)
    {
        // missing matrix file arg
        if (args.length == 0)
            PrintUsageAndExit();

        // parse command line arguments
        String inMatFileStr = args[0];
        String testMatFileStr = "";
        boolean matrixPrint = true;
        int algorithm = ALGO_SIMPLE_FLOYD_WARSHALL;
        int threadCount = 1;

        // options
        for (int i = 1; i < args.length; ++i)
        {
            String s = args[i];
            if (s.startsWith("--algorithm"))
            {
                i += 1; // get chosen algorithm
                if (i >= args.length) // must have a chosen algorithm
                    PrintUsageAndExit();

                try
                {
                    algorithm = Integer.parseInt(args[i]);
                }
                catch (Exception e)
                {
                    System.err.println("Exception: " + e.getMessage());
                    PrintUsageAndExit();
                }

                // parallel Floyd-Warshall has another param, the number of threads
                if (algorithm == ALGO_PARALLEL_MATRIX_FLOYD_WARSHALL)
                {
                    i += 1;
                    if (i >= args.length)
                        PrintUsageAndExit();

                    try
                    {
                        threadCount = Integer.parseInt(args[i]);
                    }
                    catch (Exception e)
                    {
                        System.err.println("Exception: " + e.getMessage());
                        PrintUsageAndExit();
                    }

                    assert(threadCount > 0 && threadCount <= 1000);
                }
            }
            else if (s.startsWith("--test"))
            {
                i += 1; // get test file from next arg
                if (i >= args.length) // must have another argument
                    PrintUsageAndExit();

                testMatFileStr = args[i];
            }
            else if (s.startsWith("--no-matrix-print"))
                matrixPrint = false;
            else
                PrintUsageAndExit();
        }

        IntMatrix mat = LoadMatrix(inMatFileStr);

        // compute Floyd-Warshall
        long timeAtStart = System.currentTimeMillis();

        switch (algorithm)
        {
            case ALGO_SIMPLE_FLOYD_WARSHALL:
                mat.CalcSimpleFloydWarshall();
                break;
            case ALGO_MATRIX_FLOYD_WARSHALL:
                mat.CalcMatrixFloydWarshall();
                break;
            case ALGO_PARALLEL_MATRIX_FLOYD_WARSHALL:
                mat.CalcParallelMatrixFloydWarshall(threadCount);
                break;
            default:
                System.err.println(String.format("Invalid algorithm %d", algorithm));
                PrintUsageAndExit();
                break;
        }

        long timeAtEnd = System.currentTimeMillis();

        if (matrixPrint)
            System.out.println(mat.ToString());

        System.out.println(String.format("Matrix compute time: %dms", timeAtEnd - timeAtStart));

        // compare with test file if needed
        if (!testMatFileStr.isEmpty())
        {
            IntMatrix testMat = LoadMatrix(testMatFileStr);
            if (mat.equals(testMat))
            {
                System.out.println("Test passed!");
                System.exit(0);
            }
            else
            {
                System.err.println("Computed matrix differs from test output matrix!");
                System.exit(1);
            }
        }
    }

    public static void PrintUsageAndExit()
    {
        System.out.println("Usage: java Main in_file [options]");
        System.out.println("Options:");
        System.out.println("'--test test_file' compares result matrix to matrix in test_file, exit code 1 if fail");
        System.out.println("'--no-matrix-print' removes result matrix output");
        System.out.println("'--algorithm algorithm' uses one of the possible algorithms:");
        System.out.println(" '0' - Simple Floyd-Warshall, O(n^3)");
        System.out.println(" '1' - Matrix Floyd-Warshall, O(n^3 log(n))");
        System.out.println(" '2 threadCount' - Parallel Matrix Floyd-Warshall, depends on thread count - thread count must be a square multiple of n, in order to split the matrix");
        System.exit(1);
    }

    public static IntMatrix LoadMatrix(String fileStr)
    {
        File file = new File(fileStr);
        Scanner scanner = null;

        try
        {
            scanner = new Scanner(file);
        }
        catch (FileNotFoundException e)
        {
            System.err.println("FileNotFoundException: " + e.getMessage());
            System.exit(1);
        }

        int n = scanner.nextInt();
        int mat[][] = new int[n][n];

        for (int i = 0; i < n; ++i)
        {
            for (int j = 0; j < n; ++j)
                mat[i][j] = scanner.nextInt();
        }

        IntMatrix intMat = new IntMatrix(n, mat);

        return intMat;
    }
}

class IntMatrix
{
    private final int _n;
    private int _mat[][];
    private final static int INFINITY = 20000; // Integer.MAX_VALUE leads to overflows

    public IntMatrix(int n, int mat[][])
    {
        _n = n;
        _mat = mat;
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

    @Override
    public boolean equals(Object obj)
    {
        if ((obj instanceof IntMatrix) == false)
            return false;

        IntMatrix intMat = (IntMatrix)obj;

        if (_n != intMat._n)
            return false;

        for (int i = 0; i < _n; ++i)
        {
            for (int j = 0; j < _n; ++j)
            {
                if (_mat[i][j] != intMat._mat[i][j])
                    return false;
            }
        }

        return true;
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
        // don't need to initialize the copy, it will be filled in the multiplication

        for (int itr = 0; itr < iterations; ++itr)
        {
            // just a matrix multiplication by self
            for (int i = 0; i < _n; ++i)
            {
                for (int j = 0; j < _n; ++j)
                {
                    int result = _mat[i][j];
                    for (int rowCol = 0; rowCol < _n; ++rowCol)
                        result = Math.min(result, _mat[i][rowCol] + _mat[rowCol][j]);

                    matCopy[i][j] = result;
                }
            }

            // need to update after *EACH* iteration
            CopyFrom(matCopy); // update our internal matrix
        }

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
