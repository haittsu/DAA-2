/** A resizable array of integers with counters for later experiments. */
public class DynamicArray {
    private static final int DEFAULT_CAPACITY = 10;

    private int[] elements;
    private int size;
    private long elementAccessCount;
    private long comparisonCount;
    private long movementCount;

    public DynamicArray() {
        this(DEFAULT_CAPACITY);
    }

    public DynamicArray(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("initialCapacity must be non-negative");
        }
        elements = new int[initialCapacity];
    }

    public void add(int value) {
        add(size, value);
    }

    public void add(int index, int value) {
        checkPositionIndex(index);
        ensureCapacity();

        for (int i = size; i > index; i--) {
            elements[i] = elements[i - 1];
            elementAccessCount += 2;
            movementCount++;
        }
        elements[index] = value;
        elementAccessCount++;
        size++;
    }

    public int remove(int index) {
        checkElementIndex(index);
        int removed = elements[index];
        elementAccessCount++;

        for (int i = index; i < size - 1; i++) {
            elements[i] = elements[i + 1];
            elementAccessCount += 2;
            movementCount++;
        }
        size--;
        return removed;
    }

    public int get(int index) {
        checkElementIndex(index);
        elementAccessCount++;
        return elements[index];
    }

    public boolean contains(int value) {
        for (int i = 0; i < size; i++) {
            elementAccessCount++;
            comparisonCount++;
            if (elements[i] == value) {
                return true;
            }
        }
        return false;
    }

    public int size() {
        return size;
    }

    /** Counts reads and writes of array slots, including during resizing. */
    public long getElementAccessCount() {
        return elementAccessCount;
    }

    /** Counts value equality checks performed by contains. */
    public long getComparisonCount() {
        return comparisonCount;
    }

    /** Counts copies of existing elements during shifts and resizing. */
    public long getMovementCount() {
        return movementCount;
    }

    public void resetCounters() {
        elementAccessCount = 0;
        comparisonCount = 0;
        movementCount = 0;
    }

    private void ensureCapacity() {
        if (size < elements.length) {
            return;
        }
        if (elements.length == Integer.MAX_VALUE) {
            throw new OutOfMemoryError("DynamicArray cannot grow further");
        }

        int newCapacity = elements.length == 0 ? 1
                : elements.length > Integer.MAX_VALUE / 2
                        ? Integer.MAX_VALUE : elements.length * 2;
        int[] expanded = new int[newCapacity];
        for (int i = 0; i < size; i++) {
            expanded[i] = elements[i];
            elementAccessCount += 2;
            movementCount++;
        }
        elements = expanded;
    }

    private void checkElementIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("index: " + index + ", size: " + size);
        }
    }

    private void checkPositionIndex(int index) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException("index: " + index + ", size: " + size);
        }
    }
}
