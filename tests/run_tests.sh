
# exit on error
set -e

javac ../src/Main.java

for TEST in 6 12 60 300;
do
    echo "Using test matrix size $TEST"
    java -ea -classpath ../src/ Main input${TEST} --algorithm 0 \
        --test output${TEST} --no-matrix-print
    java -ea -classpath ../src/ Main input${TEST} --algorithm 1 \
        --test output${TEST} --no-matrix-print
    # 4 threads, works with all the example sizes
    java -ea -classpath ../src/ Main input${TEST} --algorithm 2 4 \
        --test output${TEST} --no-matrix-print
done

