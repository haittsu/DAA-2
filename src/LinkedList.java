/** A singly linked list of integers with counters for later experiments. */
public class LinkedList {
    private static final class Node {
        private final int value;
        private Node next;

        private Node(int value) {
            this.value = value;
        }
    }

    private Node head;
    private Node tail;
    private int size;
    private long nodeVisitCount;
    private long comparisonCount;

    public void add(int value) {
        Node appended = new Node(value);
        if (tail == null) {
            head = appended;
        } else {
            tail.next = appended;
            nodeVisitCount++;
        }
        tail = appended;
        size++;
    }

    public void add(int index, int value) {
        checkPositionIndex(index);
        if (index == size) {
            add(value);
        } else if (index == 0) {
            Node inserted = new Node(value);
            inserted.next = head;
            head = inserted;
            size++;
        } else {
            Node previous = nodeAt(index - 1);
            Node inserted = new Node(value);
            inserted.next = previous.next;
            previous.next = inserted;
            size++;
        }
    }

    public int remove(int index) {
        checkElementIndex(index);
        Node removed;
        if (index == 0) {
            removed = head;
            nodeVisitCount++;
            head = removed.next;
            size--;
            if (size == 0) {
                tail = null;
            }
        } else {
            Node previous = nodeAt(index - 1);
            removed = previous.next;
            nodeVisitCount++;
            previous.next = removed.next;
            size--;
            if (removed == tail) {
                tail = previous;
            }
        }
        return removed.value;
    }

    public int get(int index) {
        checkElementIndex(index);
        return nodeAt(index).value;
    }

    public boolean contains(int value) {
        for (Node current = head; current != null; current = current.next) {
            nodeVisitCount++;
            comparisonCount++;
            if (current.value == value) {
                return true;
            }
        }
        return false;
    }

    public int size() {
        return size;
    }

    /** Counts existing nodes visited by traversal or direct access. */
    public long getNodeVisitCount() {
        return nodeVisitCount;
    }

    /** Counts value equality checks performed by contains. */
    public long getComparisonCount() {
        return comparisonCount;
    }

    public void resetCounters() {
        nodeVisitCount = 0;
        comparisonCount = 0;
    }

    private Node nodeAt(int index) {
        Node current = head;
        nodeVisitCount++;
        for (int i = 0; i < index; i++) {
            current = current.next;
            nodeVisitCount++;
        }
        return current;
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
