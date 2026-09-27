import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Reproducible workloads for the three custom data structures. */
public final class Benchmark {
    private static final int[] SIZES = {100, 1_000, 10_000, 100_000};
    private static final int REPETITIONS = 5;
    private static final int ACCESS_OPERATIONS = 10_000;
    private static final int SEARCH_OPERATIONS = 1_000;
    private static final int CHANGE_OPERATIONS = 1_000;
    private static final long SEED = 42L;
    private static final Path OUTPUT = Paths.get("results", "tables", "benchmark.csv");

    private Benchmark() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 0) {
            throw new IllegalArgumentException("Benchmark takes no arguments");
        }
        List<Row> rows = new ArrayList<>();
        runRandomAccess(rows);
        runSearch(rows);
        runInsertionAndRemoval(rows);
        runPriorityProcessing(rows);
        writeCsv(rows);
        System.out.println("Saved measured averages to " + OUTPUT);
        System.out.println("Seed: 42; repetitions: 5; sizes: 100, 1000, 10000, 100000.");
        System.out.println("Removal method: after each timed remove(index), the removed value is "
                + "reinserted at the same index outside the timed interval. Only the 1000 "
                + "remove() durations are summed, so this also works when n=100.");
    }

    private static void runRandomAccess(List<Row> rows) {
        Random random = new Random(SEED);
        for (int n : SIZES) {
            Stats arrayStats = new Stats();
            Stats listStats = new Stats();
            for (int repeat = 0; repeat < REPETITIONS; repeat++) {
                int[] values = randomValues(random, n);
                int[] indices = new int[ACCESS_OPERATIONS];
                int[] expected = new int[ACCESS_OPERATIONS];
                for (int i = 0; i < ACCESS_OPERATIONS; i++) {
                    indices[i] = random.nextInt(n);
                    expected[i] = values[indices[i]];
                }

                DynamicArray array = buildArray(values);
                int[] observed = new int[ACCESS_OPERATIONS];
                array.resetCounters();
                long start = System.nanoTime();
                for (int i = 0; i < ACCESS_OPERATIONS; i++) {
                    observed[i] = array.get(indices[i]);
                }
                long elapsed = System.nanoTime() - start;
                arrayStats.add(elapsed, array.getElementAccessCount(), 0, 0, 0);
                require(Arrays.equals(observed, expected), "random access: DynamicArray");

                LinkedList list = buildList(values);
                observed = new int[ACCESS_OPERATIONS];
                list.resetCounters();
                start = System.nanoTime();
                for (int i = 0; i < ACCESS_OPERATIONS; i++) {
                    observed[i] = list.get(indices[i]);
                }
                elapsed = System.nanoTime() - start;
                listStats.add(elapsed, 0, list.getNodeVisitCount(), 0, 0);
                require(Arrays.equals(observed, expected), "random access: LinkedList");
            }
            rows.add(new Row("random_access", "get", "DynamicArray", n,
                    ACCESS_OPERATIONS, arrayStats, true, false, false, false));
            rows.add(new Row("random_access", "get", "LinkedList", n,
                    ACCESS_OPERATIONS, listStats, false, true, false, false));
        }
    }

    private static void runSearch(List<Row> rows) {
        Random random = new Random(SEED);
        for (int n : SIZES) {
            Stats arrayStats = new Stats();
            Stats listStats = new Stats();
            for (int repeat = 0; repeat < REPETITIONS; repeat++) {
                int[] values = randomValues(random, n);
                int[] queries = new int[SEARCH_OPERATIONS];
                boolean[] expected = new boolean[SEARCH_OPERATIONS];
                Set<Integer> present = new HashSet<>();
                for (int value : values) {
                    present.add(value);
                }
                for (int i = 0; i < SEARCH_OPERATIONS; i++) {
                    queries[i] = i % 2 == 0 ? values[random.nextInt(n)] : randomValue(random);
                    expected[i] = present.contains(queries[i]);
                }

                DynamicArray array = buildArray(values);
                boolean[] observed = new boolean[SEARCH_OPERATIONS];
                array.resetCounters();
                long start = System.nanoTime();
                for (int i = 0; i < SEARCH_OPERATIONS; i++) {
                    observed[i] = array.contains(queries[i]);
                }
                long elapsed = System.nanoTime() - start;
                arrayStats.add(elapsed, array.getElementAccessCount(), 0, 0,
                        array.getComparisonCount());
                require(Arrays.equals(observed, expected), "search: DynamicArray");

                LinkedList list = buildList(values);
                observed = new boolean[SEARCH_OPERATIONS];
                list.resetCounters();
                start = System.nanoTime();
                for (int i = 0; i < SEARCH_OPERATIONS; i++) {
                    observed[i] = list.contains(queries[i]);
                }
                elapsed = System.nanoTime() - start;
                listStats.add(elapsed, 0, list.getNodeVisitCount(), 0,
                        list.getComparisonCount());
                require(Arrays.equals(observed, expected), "search: LinkedList");
            }
            rows.add(new Row("search", "contains", "DynamicArray", n,
                    SEARCH_OPERATIONS, arrayStats, true, false, false, true));
            rows.add(new Row("search", "contains", "LinkedList", n,
                    SEARCH_OPERATIONS, listStats, false, true, false, true));
        }
    }

    private static void runInsertionAndRemoval(List<Row> rows) {
        Random random = new Random(SEED);
        for (int n : SIZES) {
            Stats arrayInsertFront = new Stats();
            Stats listInsertFront = new Stats();
            Stats arrayRemoveFront = new Stats();
            Stats listRemoveFront = new Stats();
            Stats arrayInsertMiddle = new Stats();
            Stats listInsertMiddle = new Stats();
            Stats arrayRemoveMiddle = new Stats();
            Stats listRemoveMiddle = new Stats();
            int[] frontIndices = new int[CHANGE_OPERATIONS];
            int[] middleIndices = new int[CHANGE_OPERATIONS];
            for (int i = 0; i < CHANGE_OPERATIONS; i++) {
                middleIndices[i] = (n + i) / 2;
            }
            for (int repeat = 0; repeat < REPETITIONS; repeat++) {
                int[] initial = randomValues(random, n);
                int[] inserted = randomValues(random, CHANGE_OPERATIONS);
                measureArrayInsertion(initial, inserted, frontIndices, arrayInsertFront);
                measureListInsertion(initial, inserted, frontIndices, listInsertFront);
                measureArrayRemoval(initial, 0, arrayRemoveFront);
                measureListRemoval(initial, 0, listRemoveFront);
                measureArrayInsertion(initial, inserted, middleIndices, arrayInsertMiddle);
                measureListInsertion(initial, inserted, middleIndices, listInsertMiddle);
                measureArrayRemoval(initial, n / 2, arrayRemoveMiddle);
                measureListRemoval(initial, n / 2, listRemoveMiddle);
            }
            rows.add(new Row("insertion_removal", "insert_front", "DynamicArray", n,
                    CHANGE_OPERATIONS, arrayInsertFront, true, false, true, false));
            rows.add(new Row("insertion_removal", "insert_front", "LinkedList", n,
                    CHANGE_OPERATIONS, listInsertFront, false, true, false, false));
            rows.add(new Row("insertion_removal", "remove_front", "DynamicArray", n,
                    CHANGE_OPERATIONS, arrayRemoveFront, true, false, true, false));
            rows.add(new Row("insertion_removal", "remove_front", "LinkedList", n,
                    CHANGE_OPERATIONS, listRemoveFront, false, true, false, false));
            rows.add(new Row("insertion_removal", "insert_middle", "DynamicArray", n,
                    CHANGE_OPERATIONS, arrayInsertMiddle, true, false, true, false));
            rows.add(new Row("insertion_removal", "insert_middle", "LinkedList", n,
                    CHANGE_OPERATIONS, listInsertMiddle, false, true, false, false));
            rows.add(new Row("insertion_removal", "remove_middle", "DynamicArray", n,
                    CHANGE_OPERATIONS, arrayRemoveMiddle, true, false, true, false));
            rows.add(new Row("insertion_removal", "remove_middle", "LinkedList", n,
                    CHANGE_OPERATIONS, listRemoveMiddle, false, true, false, false));
        }
    }

    private static void measureArrayInsertion(int[] initial, int[] inserted,
            int[] indices, Stats stats) {
        DynamicArray array = buildArray(initial);
        ArrayList<Integer> expected = copyToArrayList(initial, inserted.length);
        long elapsed = 0;
        long accesses = 0;
        long movements = 0;
        for (int i = 0; i < inserted.length; i++) {
            array.resetCounters();
            long start = System.nanoTime();
            array.add(indices[i], inserted[i]);
            elapsed += System.nanoTime() - start;
            accesses += array.getElementAccessCount();
            movements += array.getMovementCount();
            expected.add(indices[i], inserted[i]);
            require(array.size() == initial.length + i + 1
                    && array.get(indices[i]) == inserted[i], "array insertion at step " + i);
        }
        verifyArraySamples(array, expected);
        stats.add(elapsed, accesses, 0, movements, 0);
    }

    private static void measureListInsertion(int[] initial, int[] inserted,
            int[] indices, Stats stats) {
        LinkedList list = buildList(initial);
        ArrayList<Integer> expected = copyToArrayList(initial, inserted.length);
        long elapsed = 0;
        long visits = 0;
        for (int i = 0; i < inserted.length; i++) {
            list.resetCounters();
            long start = System.nanoTime();
            list.add(indices[i], inserted[i]);
            elapsed += System.nanoTime() - start;
            visits += list.getNodeVisitCount();
            expected.add(indices[i], inserted[i]);
            require(list.size() == initial.length + i + 1
                    && list.get(indices[i]) == inserted[i], "list insertion at step " + i);
        }
        verifyListSamples(list, expected);
        stats.add(elapsed, 0, visits, 0, 0);
    }

    private static void measureArrayRemoval(int[] initial, int index, Stats stats) {
        DynamicArray array = buildArray(initial);
        int expected = initial[index];
        long elapsed = 0;
        long accesses = 0;
        long movements = 0;
        for (int i = 0; i < CHANGE_OPERATIONS; i++) {
            array.resetCounters();
            long start = System.nanoTime();
            int removed = array.remove(index);
            elapsed += System.nanoTime() - start;
            accesses += array.getElementAccessCount();
            movements += array.getMovementCount();
            require(removed == expected && array.size() == initial.length - 1,
                    "array removal at step " + i);
            array.add(index, removed);
            require(array.size() == initial.length && array.get(index) == expected,
                    "array restoration at step " + i);
        }
        stats.add(elapsed, accesses, 0, movements, 0);
    }

    private static void measureListRemoval(int[] initial, int index, Stats stats) {
        LinkedList list = buildList(initial);
        int expected = initial[index];
        long elapsed = 0;
        long visits = 0;
        for (int i = 0; i < CHANGE_OPERATIONS; i++) {
            list.resetCounters();
            long start = System.nanoTime();
            int removed = list.remove(index);
            elapsed += System.nanoTime() - start;
            visits += list.getNodeVisitCount();
            require(removed == expected && list.size() == initial.length - 1,
                    "list removal at step " + i);
            list.add(index, removed);
            require(list.size() == initial.length && list.get(index) == expected,
                    "list restoration at step " + i);
        }
        stats.add(elapsed, 0, visits, 0, 0);
    }

    private static void runPriorityProcessing(List<Row> rows) {
        Random random = new Random(SEED);
        for (int n : SIZES) {
            Stats insertStats = new Stats();
            Stats extractStats = new Stats();
            for (int repeat = 0; repeat < REPETITIONS; repeat++) {
                int[] values = randomValues(random, n);
                int[] expected = values.clone();
                Arrays.sort(expected);
                MinHeap heap = new MinHeap();
                heap.resetCounters();
                long start = System.nanoTime();
                for (int value : values) {
                    heap.insert(value);
                }
                long elapsed = System.nanoTime() - start;
                insertStats.add(elapsed, 0, 0, 0, heap.getComparisonCount());
                require(heap.size() == n && heap.peekMin() == expected[0],
                        "priority processing: insertion");

                int[] observed = new int[n];
                heap.resetCounters();
                start = System.nanoTime();
                for (int i = 0; i < n; i++) {
                    observed[i] = heap.extractMin();
                }
                elapsed = System.nanoTime() - start;
                extractStats.add(elapsed, 0, 0, 0, heap.getComparisonCount());
                require(heap.size() == 0 && Arrays.equals(observed, expected),
                        "priority processing: extraction order");
            }
            rows.add(new Row("priority_processing", "insert", "MinHeap", n,
                    n, insertStats, false, false, false, true));
            rows.add(new Row("priority_processing", "extract_min", "MinHeap", n,
                    n, extractStats, false, false, false, true));
        }
    }

    private static DynamicArray buildArray(int[] values) {
        DynamicArray array = new DynamicArray(values.length);
        for (int value : values) {
            array.add(value);
        }
        require(array.size() == values.length, "DynamicArray preparation");
        return array;
    }

    private static LinkedList buildList(int[] values) {
        LinkedList list = new LinkedList();
        for (int value : values) {
            list.add(value);
        }
        require(list.size() == values.length, "LinkedList preparation");
        return list;
    }

    private static ArrayList<Integer> copyToArrayList(int[] values, int extraCapacity) {
        ArrayList<Integer> copy = new ArrayList<>(values.length + extraCapacity);
        for (int value : values) {
            copy.add(value);
        }
        return copy;
    }

    private static void verifyArraySamples(DynamicArray array, ArrayList<Integer> expected) {
        require(array.size() == expected.size(), "DynamicArray final insertion size");
        for (int i = 0; i < 64; i++) {
            int index = (int) ((long) i * (expected.size() - 1) / 63);
            require(array.get(index) == expected.get(index),
                    "DynamicArray final insertion value at " + index);
        }
    }

    private static void verifyListSamples(LinkedList list, ArrayList<Integer> expected) {
        require(list.size() == expected.size(), "LinkedList final insertion size");
        for (int i = 0; i < 64; i++) {
            int index = (int) ((long) i * (expected.size() - 1) / 63);
            require(list.get(index) == expected.get(index),
                    "LinkedList final insertion value at " + index);
        }
    }

    private static int[] randomValues(Random random, int count) {
        int[] values = new int[count];
        for (int i = 0; i < count; i++) {
            values[i] = randomValue(random);
        }
        return values;
    }

    private static int randomValue(Random random) {
        return random.nextInt(2_000_001) - 1_000_000;
    }

    private static void writeCsv(List<Row> rows) throws IOException {
        Files.createDirectories(OUTPUT.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(OUTPUT,
                StandardCharsets.UTF_8)) {
            writer.write("workload,operation,structure,n,operations,seed,repetitions,avg_time_ns,"
                    + "avg_element_accesses,avg_node_visits,avg_movements,avg_comparisons");
            writer.newLine();
            for (Row row : rows) {
                writer.write(row.toCsv());
                writer.newLine();
            }
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static final class Stats {
        private long timeNs;
        private long elementAccesses;
        private long nodeVisits;
        private long movements;
        private long comparisons;

        private void add(long timeNs, long elementAccesses, long nodeVisits,
                long movements, long comparisons) {
            this.timeNs += timeNs;
            this.elementAccesses += elementAccesses;
            this.nodeVisits += nodeVisits;
            this.movements += movements;
            this.comparisons += comparisons;
        }
    }

    private static final class Row {
        private final String workload;
        private final String operation;
        private final String structure;
        private final int n;
        private final int operations;
        private final Stats stats;
        private final boolean showAccesses;
        private final boolean showVisits;
        private final boolean showMovements;
        private final boolean showComparisons;

        private Row(String workload, String operation, String structure, int n,
                int operations, Stats stats, boolean showAccesses, boolean showVisits,
                boolean showMovements, boolean showComparisons) {
            this.workload = workload;
            this.operation = operation;
            this.structure = structure;
            this.n = n;
            this.operations = operations;
            this.stats = stats;
            this.showAccesses = showAccesses;
            this.showVisits = showVisits;
            this.showMovements = showMovements;
            this.showComparisons = showComparisons;
        }

        private String toCsv() {
            return workload + "," + operation + "," + structure + "," + n + ","
                    + operations + "," + SEED + "," + REPETITIONS + ","
                    + average(stats.timeNs) + ","
                    + metric(showAccesses, stats.elementAccesses) + ","
                    + metric(showVisits, stats.nodeVisits) + ","
                    + metric(showMovements, stats.movements) + ","
                    + metric(showComparisons, stats.comparisons);
        }

        private static String metric(boolean applicable, long total) {
            return applicable ? average(total) : "";
        }

        private static String average(long total) {
            return BigDecimal.valueOf(total)
                    .divide(BigDecimal.valueOf(REPETITIONS)).toPlainString();
        }
    }
}
