import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Random;

/** Run with: java -cp src Tests. No external test framework is required. */
public class Tests {
    private static final int LARGE_INPUT_SIZE = 12_000;

    public static void main(String[] args) throws ReflectiveOperationException {
        testDynamicArray();
        testLinkedList();
        testMinHeap();
        System.out.println("All data structure tests passed.");
    }

    private static void testDynamicArray() {
        DynamicArray actual = new DynamicArray(0);
        ArrayList<Integer> expected = new ArrayList<>();
        check(actual.size() == 0 && !actual.contains(7), "array: empty");
        expectIndexError(() -> actual.get(0), "array: get from empty");
        expectIndexError(() -> actual.remove(0), "array: remove from empty");
        expectIndexError(() -> expected.get(0), "ArrayList: get from empty");
        expectIndexError(() -> expected.remove(0), "ArrayList: remove from empty");

        actual.add(0, 5);
        expected.add(0, 5);
        assertArrayContents(actual, expected);
        removeArrayAt(actual, expected, 0);
        check(actual.size() == 0, "array: empty after singleton removal");

        int[] initial = {7, 7, -4, Integer.MIN_VALUE, Integer.MAX_VALUE};
        for (int value : initial) {
            actual.add(value);
            expected.add(value);
        }
        actual.add(0, 9);
        expected.add(0, 9);
        actual.add(3, 7);
        expected.add(3, 7);
        actual.add(actual.size(), -9);
        expected.add(expected.size(), -9);
        assertArrayContents(actual, expected);
        check(actual.contains(7) == expected.contains(7), "array: duplicate search");
        check(actual.contains(123) == expected.contains(123), "array: missing search");
        check(actual.getComparisonCount() > 0, "array: comparisons counted");
        removeArrayAt(actual, expected, 0);
        removeArrayAt(actual, expected, actual.size() / 2);
        removeArrayAt(actual, expected, actual.size() - 1);
        assertArrayContents(actual, expected);
        checkInvalidArrayIndices(actual, expected);

        Random random = new Random(101);
        for (int step = 0; step < 3_000; step++) {
            int value = random.nextInt(41) - 20;
            switch (random.nextInt(5)) {
                case 0:
                    actual.add(value);
                    expected.add(value);
                    break;
                case 1:
                    int insertAt = random.nextInt(actual.size() + 1);
                    actual.add(insertAt, value);
                    expected.add(insertAt, value);
                    break;
                case 2:
                    if (actual.size() > 0) {
                        removeArrayAt(actual, expected, random.nextInt(actual.size()));
                    }
                    break;
                case 3:
                    if (actual.size() > 0) {
                        int index = random.nextInt(actual.size());
                        check(actual.get(index) == expected.get(index), "array: random get");
                    }
                    break;
                default:
                    check(actual.contains(value) == expected.contains(value),
                            "array: random contains");
            }
            check(actual.size() == expected.size(), "array: random size");
            if (step % 29 == 0) {
                assertArrayContents(actual, expected);
            }
        }
        assertArrayContents(actual, expected);

        DynamicArray large = new DynamicArray(1);
        ArrayList<Integer> largeExpected = new ArrayList<>();
        for (int i = 0; i < LARGE_INPUT_SIZE; i++) {
            int value = i % 251 - 125;
            large.add(value);
            largeExpected.add(value);
        }
        assertArrayContents(large, largeExpected);
        for (int i = 0; i < 200; i++) {
            removeArrayAt(large, largeExpected, i % 2 == 0 ? 0 : large.size() - 1);
        }
        assertArrayContents(large, largeExpected);
        check(large.getElementAccessCount() > 0 && large.getMovementCount() > 0,
                "array: accesses and movements counted");
        large.resetCounters();
        check(large.getElementAccessCount() == 0 && large.getComparisonCount() == 0
                && large.getMovementCount() == 0, "array: counter reset");
        System.out.println("DynamicArray tests passed.");
    }

    private static void checkInvalidArrayIndices(DynamicArray actual,
            ArrayList<Integer> expected) {
        int size = actual.size();
        expectIndexError(() -> actual.get(-1), "array: negative get");
        expectIndexError(() -> expected.get(-1), "ArrayList: negative get");
        expectIndexError(() -> actual.get(size), "array: get at size");
        expectIndexError(() -> expected.get(size), "ArrayList: get at size");
        expectIndexError(() -> actual.remove(-1), "array: negative remove");
        expectIndexError(() -> expected.remove(-1), "ArrayList: negative remove");
        expectIndexError(() -> actual.remove(size), "array: remove at size");
        expectIndexError(() -> expected.remove(size), "ArrayList: remove at size");
        expectIndexError(() -> actual.add(-1, 1), "array: negative insert");
        expectIndexError(() -> expected.add(-1, 1), "ArrayList: negative insert");
        expectIndexError(() -> actual.add(size + 1, 1), "array: insert past end");
        expectIndexError(() -> expected.add(size + 1, 1), "ArrayList: insert past end");
        assertArrayContents(actual, expected);
    }

    private static void removeArrayAt(DynamicArray actual, ArrayList<Integer> expected,
            int index) {
        check(actual.remove(index) == expected.remove(index), "array: removed value");
    }

    private static void assertArrayContents(DynamicArray actual,
            ArrayList<Integer> expected) {
        check(actual.size() == expected.size(), "array: size");
        for (int i = 0; i < expected.size(); i++) {
            check(actual.get(i) == expected.get(i), "array: value at " + i);
        }
    }

    private static void testLinkedList() {
        LinkedList actual = new LinkedList();
        java.util.LinkedList<Integer> expected = new java.util.LinkedList<>();
        check(actual.size() == 0 && !actual.contains(7), "list: empty");
        expectIndexError(() -> actual.get(0), "list: get from empty");
        expectIndexError(() -> actual.remove(0), "list: remove from empty");
        expectIndexError(() -> expected.get(0), "LinkedList: get from empty");
        expectIndexError(() -> expected.remove(0), "LinkedList: remove from empty");

        actual.add(0, 5);
        expected.add(0, 5);
        assertListContents(actual, expected);
        removeListAt(actual, expected, 0);
        check(actual.size() == 0, "list: empty after singleton removal");
        int[] initial = {7, 7, -4, Integer.MIN_VALUE, Integer.MAX_VALUE};
        for (int value : initial) {
            actual.add(value);
            expected.add(value);
        }
        actual.add(0, 9);
        expected.add(0, 9);
        actual.add(3, 7);
        expected.add(3, 7);
        actual.add(actual.size(), -9);
        expected.add(expected.size(), -9);
        assertListContents(actual, expected);
        check(actual.contains(7) == expected.contains(7), "list: duplicate search");
        check(actual.contains(123) == expected.contains(123), "list: missing search");
        check(actual.getComparisonCount() > 0, "list: comparisons counted");
        removeListAt(actual, expected, 0);
        removeListAt(actual, expected, actual.size() / 2);
        removeListAt(actual, expected, actual.size() - 1);
        assertListContents(actual, expected);
        checkInvalidListIndices(actual, expected);

        Random random = new Random(202);
        for (int step = 0; step < 2_000; step++) {
            int value = random.nextInt(41) - 20;
            switch (random.nextInt(5)) {
                case 0:
                    actual.add(value);
                    expected.add(value);
                    break;
                case 1:
                    int insertAt = random.nextInt(actual.size() + 1);
                    actual.add(insertAt, value);
                    expected.add(insertAt, value);
                    break;
                case 2:
                    if (actual.size() > 0) {
                        removeListAt(actual, expected, random.nextInt(actual.size()));
                    }
                    break;
                case 3:
                    if (actual.size() > 0) {
                        int index = random.nextInt(actual.size());
                        check(actual.get(index) == expected.get(index), "list: random get");
                    }
                    break;
                default:
                    check(actual.contains(value) == expected.contains(value),
                            "list: random contains");
            }
            check(actual.size() == expected.size(), "list: random size");
            if (step % 31 == 0) {
                assertListContents(actual, expected);
            }
        }
        assertListContents(actual, expected);

        LinkedList large = new LinkedList();
        java.util.LinkedList<Integer> largeExpected = new java.util.LinkedList<>();
        for (int i = 0; i < LARGE_INPUT_SIZE; i++) {
            int value = i % 251 - 125;
            large.add(value);
            largeExpected.add(value);
        }
        check(large.size() == largeExpected.size(), "list: large size");
        for (int i = 0; i < 200; i++) {
            int index = i * (LARGE_INPUT_SIZE - 1) / 199;
            check(large.get(index) == largeExpected.get(index), "list: large get");
        }
        for (int i = 0; i < 200; i++) {
            removeListAt(large, largeExpected, i % 2 == 0 ? 0 : large.size() - 1);
        }
        check(large.size() == largeExpected.size(), "list: large size after removal");
        while (large.size() > 0) {
            removeListAt(large, largeExpected, 0);
        }
        large.add(42);
        largeExpected.add(42);
        assertListContents(large, largeExpected);
        check(large.getNodeVisitCount() > 0, "list: visits counted");
        large.resetCounters();
        check(large.getNodeVisitCount() == 0 && large.getComparisonCount() == 0,
                "list: counter reset");
        System.out.println("LinkedList tests passed.");
    }

    private static void checkInvalidListIndices(LinkedList actual,
            java.util.LinkedList<Integer> expected) {
        int size = actual.size();
        expectIndexError(() -> actual.get(-1), "list: negative get");
        expectIndexError(() -> expected.get(-1), "LinkedList: negative get");
        expectIndexError(() -> actual.get(size), "list: get at size");
        expectIndexError(() -> expected.get(size), "LinkedList: get at size");
        expectIndexError(() -> actual.remove(-1), "list: negative remove");
        expectIndexError(() -> expected.remove(-1), "LinkedList: negative remove");
        expectIndexError(() -> actual.remove(size), "list: remove at size");
        expectIndexError(() -> expected.remove(size), "LinkedList: remove at size");
        expectIndexError(() -> actual.add(-1, 1), "list: negative insert");
        expectIndexError(() -> expected.add(-1, 1), "LinkedList: negative insert");
        expectIndexError(() -> actual.add(size + 1, 1), "list: insert past end");
        expectIndexError(() -> expected.add(size + 1, 1), "LinkedList: insert past end");
        assertListContents(actual, expected);
    }

    private static void removeListAt(LinkedList actual,
            java.util.LinkedList<Integer> expected, int index) {
        check(actual.remove(index) == expected.remove(index), "list: removed value");
    }

    private static void assertListContents(LinkedList actual,
            java.util.LinkedList<Integer> expected) {
        check(actual.size() == expected.size(), "list: size");
        for (int i = 0; i < expected.size(); i++) {
            check(actual.get(i) == expected.get(i), "list: value at " + i);
        }
    }

    private static void testMinHeap() throws ReflectiveOperationException {
        // Inspect the private storage without adding test-only methods to MinHeap.
        Field storage = MinHeap.class.getDeclaredField("elements");
        storage.setAccessible(true);
        MinHeap actual = new MinHeap(0);
        PriorityQueue<Integer> expected = new PriorityQueue<>();
        assertHeapState(actual, expected, storage);
        expectEmptyError(() -> actual.peekMin(), "heap: peek from empty");
        expectEmptyError(() -> actual.extractMin(), "heap: extract from empty");
        expectEmptyError(() -> expected.element(), "PriorityQueue: element from empty");
        expectEmptyError(() -> expected.remove(), "PriorityQueue: remove from empty");

        int[] initial = {7, 7, -4, Integer.MIN_VALUE, Integer.MAX_VALUE, 7};
        for (int value : initial) {
            actual.insert(value);
            expected.add(value);
            assertHeapState(actual, expected, storage);
        }
        check(actual.getComparisonCount() > 0, "heap: comparisons counted");
        long comparisonsBeforePeek = actual.getComparisonCount();
        actual.peekMin();
        check(actual.getComparisonCount() == comparisonsBeforePeek,
                "heap: peek does not compare");
        drainHeap(actual, expected, storage);
        actual.insert(42);
        expected.add(42);
        assertHeapState(actual, expected, storage);
        extractHeap(actual, expected);
        assertHeapState(actual, expected, storage);

        Random random = new Random(303);
        for (int step = 0; step < 4_000; step++) {
            if (expected.isEmpty() || random.nextInt(3) != 0) {
                int value = random.nextInt(101) - 50;
                actual.insert(value);
                expected.add(value);
            } else {
                extractHeap(actual, expected);
            }
            assertHeapState(actual, expected, storage);
        }
        drainHeap(actual, expected, storage);

        MinHeap large = new MinHeap(1);
        PriorityQueue<Integer> largeExpected = new PriorityQueue<>();
        for (int i = 0; i < LARGE_INPUT_SIZE; i++) {
            int value = random.nextInt(2_001) - 1_000;
            large.insert(value);
            largeExpected.add(value);
            assertHeapState(large, largeExpected, storage);
        }
        drainHeap(large, largeExpected, storage);
        check(large.getComparisonCount() > 0, "heap: large comparisons counted");
        large.resetCounters();
        check(large.getComparisonCount() == 0, "heap: counter reset");
        System.out.println("MinHeap tests passed.");
    }

    private static void drainHeap(MinHeap actual, PriorityQueue<Integer> expected,
            Field storage) throws IllegalAccessException {
        Integer previous = null;
        while (!expected.isEmpty()) {
            int extracted = extractHeap(actual, expected);
            check(previous == null || previous <= extracted, "heap: extraction order");
            previous = extracted;
            assertHeapState(actual, expected, storage);
        }
        expectEmptyError(() -> actual.extractMin(), "heap: extract after drain");
    }

    private static int extractHeap(MinHeap actual, PriorityQueue<Integer> expected) {
        int extracted = actual.extractMin();
        check(extracted == expected.remove(), "heap: extracted value");
        return extracted;
    }

    private static void assertHeapState(MinHeap actual, PriorityQueue<Integer> expected,
            Field storage) throws IllegalAccessException {
        check(actual.size() == expected.size(), "heap: size");
        if (!expected.isEmpty()) {
            check(actual.peekMin() == expected.element(), "heap: minimum");
        }
        assertHeapInvariant(actual, storage);
    }

    private static void assertHeapInvariant(MinHeap heap, Field storage)
            throws IllegalAccessException {
        int[] elements = (int[]) storage.get(heap);
        check(heap.size() <= elements.length, "heap: storage capacity");
        for (int child = 1; child < heap.size(); child++) {
            if (elements[(child - 1) / 2] > elements[child]) {
                throw new AssertionError("heap: parent greater than child at " + child);
            }
        }
    }

    private static void expectIndexError(Runnable action, String label) {
        try {
            action.run();
            throw new AssertionError(label + " did not throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException expected) {
            // Expected.
        }
    }

    private static void expectEmptyError(Runnable action, String label) {
        try {
            action.run();
            throw new AssertionError(label + " did not throw NoSuchElementException");
        } catch (NoSuchElementException expected) {
            // Expected.
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
