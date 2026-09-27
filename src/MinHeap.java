import java.util.NoSuchElementException;

/** An array-backed min-heap of integers. */
public class MinHeap {
    private static final int DEFAULT_CAPACITY = 10;

    private int[] elements;
    private int size;
    private long comparisonCount;

    public MinHeap() {
        this(DEFAULT_CAPACITY);
    }

    public MinHeap(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("initialCapacity must be non-negative");
        }
        elements = new int[initialCapacity];
    }

    public void insert(int value) {
        ensureCapacity();
        int index = size++;
        elements[index] = value;
        siftUp(index);
    }

    public int peekMin() {
        checkNotEmpty();
        return elements[0];
    }

    public int extractMin() {
        checkNotEmpty();
        int minimum = elements[0];
        int last = elements[--size];
        if (size > 0) {
            elements[0] = last;
            siftDown(0);
        }
        return minimum;
    }

    public int size() {
        return size;
    }

    /** Counts comparisons between stored values during heap restoration. */
    public long getComparisonCount() {
        return comparisonCount;
    }

    public void resetCounters() {
        comparisonCount = 0;
    }

    private void siftUp(int index) {
        while (index > 0) {
            int parent = (index - 1) / 2;
            comparisonCount++;
            if (elements[parent] <= elements[index]) {
                break;
            }
            swap(parent, index);
            index = parent;
        }
    }

    private void siftDown(int index) {
        while (index < size / 2) {
            int left = 2 * index + 1;
            int right = left + 1;
            int smallerChild = left;
            if (right < size) {
                comparisonCount++;
                if (elements[right] < elements[left]) {
                    smallerChild = right;
                }
            }
            comparisonCount++;
            if (elements[index] <= elements[smallerChild]) {
                break;
            }
            swap(index, smallerChild);
            index = smallerChild;
        }
    }

    private void swap(int first, int second) {
        int temporary = elements[first];
        elements[first] = elements[second];
        elements[second] = temporary;
    }

    private void ensureCapacity() {
        if (size < elements.length) {
            return;
        }
        if (elements.length == Integer.MAX_VALUE) {
            throw new OutOfMemoryError("MinHeap cannot grow further");
        }

        int newCapacity = elements.length == 0 ? 1
                : elements.length > Integer.MAX_VALUE / 2
                        ? Integer.MAX_VALUE : elements.length * 2;
        int[] expanded = new int[newCapacity];
        System.arraycopy(elements, 0, expanded, 0, size);
        elements = expanded;
    }

    private void checkNotEmpty() {
        if (size == 0) {
            throw new NoSuchElementException("Heap is empty");
        }
    }
}
