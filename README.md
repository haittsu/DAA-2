# Assignment 2

## 1. Overview

This project implements three integer data structures from scratch: an array-backed [DynamicArray](src/DynamicArray.java) that doubles its capacity, a singly [LinkedList](src/LinkedList.java) with head and tail pointers, and an array-backed [MinHeap](src/MinHeap.java). The purpose is to analyze their costs, prove two operations correct, and compare theory with four measured workloads. Java's standard collections appear in the [tests](src/Tests.java) as reference implementations, not as the basis of these structures.

The measured data are in [benchmark.csv](results/tables/benchmark.csv): 56 rows covering every applicable structure, operation, and input size. The [plots](results/plots/) show time and operation counts; [plot_benchmarks.ps1](tools/plot_benchmarks.ps1) regenerates them from the CSV.

### Build and run

A JDK with javac and java on PATH is sufficient; no third-party libraries are required. Run from the repository root:

~~~text
javac src/Main.java src/DynamicArray.java src/LinkedList.java src/MinHeap.java src/Tests.java src/Benchmark.java
java -cp src Tests
java -cp src Benchmark
powershell -NoProfile -ExecutionPolicy Bypass -File tools/plot_benchmarks.ps1
~~~

Running Benchmark replaces the CSV with new measurements; the final command then rebuilds the plots from that CSV. Main only confirms that the project starts.

## 2. Complexity Analysis

Here n means the number of elements **before one operation**. The tables assume valid indices; rejecting an invalid index takes Θ(1). Each best, average, and worst cell gives a **tight Θ bound**, which also supplies O and Ω bounds for that particular case. The "overall O / Ω" column gives an upper and lower bound across all valid inputs. When best and worst differ, there is no single tight bound for every input. Auxiliary space is temporary space or a newly allocated node beyond the pre-existing structure; total retained storage is Θ(n).

Average cases use explicit models. For indexed access and indexed insertion/removal, the index is uniform over valid positions. For contains(x), a successful query has a uniformly located first match, or a miss occurs with a constant nonzero probability; either model gives an expected Θ(n) scan. For the heap, newly inserted keys are independent uniform random values. Amortized costs refer to a growing sequence beginning with an empty structure, or a sequence that includes the insertions that filled its current capacity. A short sequence beginning with an already full, pre-populated array can pay Θ(n) on its first insertion.

### DynamicArray

| Operation | Best Θ | Average Θ | Worst Θ | Overall O / Ω | Auxiliary space |
| --- | --- | --- | --- | --- | --- |
| add(x), append | Θ(1) | Θ(1) amortized | Θ(n) | O(n), Ω(1) | Θ(1) without growth; Θ(n) during growth |
| add(index, x) | Θ(1) | Θ(n) | Θ(n) | O(n), Ω(1) | Θ(1) without growth; Θ(n) during growth |
| remove(index) | Θ(1) | Θ(n) | Θ(n) | O(n), Ω(1) | Θ(1) |
| get(index) | Θ(1) | Θ(1) | Θ(1) | O(1), Ω(1) | Θ(1) |
| contains(x) | Θ(1) | Θ(n) | Θ(n) | O(n), Ω(1) | Θ(1) |
| size() | Θ(1) | Θ(1) | Θ(1) | O(1), Ω(1) | Θ(1) |

An append writes one slot if capacity remains; if full, it first copies n elements. With doubling, the capacities copied over a sequence of m insertions from empty form a geometric series bounded by O(m), so the **amortized** append cost is Θ(1). This does not make every append constant time. Indexed insertion shifts n-index elements; indexed removal shifts n-index-1. Uniform indices therefore give Θ(n) expected work, while insertion at the end or removal of the last element can be constant. get reads one slot; contains stops at the first match or scans all n slots. During a resize, the new backing array takes Θ(n) extra space.

### LinkedList

| Operation | Best Θ | Average Θ | Worst Θ | Overall O / Ω | Auxiliary space |
| --- | --- | --- | --- | --- | --- |
| add(x), append | Θ(1) | Θ(1) | Θ(1) | O(1), Ω(1) | Θ(1) |
| add(index, x) | Θ(1) | Θ(n) | Θ(n) | O(n), Ω(1) | Θ(1) |
| remove(index) | Θ(1) | Θ(n) | Θ(n) | O(n), Ω(1) | Θ(1) |
| get(index) | Θ(1) | Θ(n) | Θ(n) | O(n), Ω(1) | Θ(1) |
| contains(x) | Θ(1) | Θ(n) | Θ(n) | O(n), Ω(1) | Θ(1) |
| size() | Θ(1) | Θ(1) | Θ(1) | O(1), Ω(1) | Θ(1) |

The tail pointer makes append constant time. Insertion or removal at index 0 changes a fixed number of references; indexed insertion at size also uses the tail. Other indexed changes walk from the head to the predecessor. In particular, removing the last node takes Θ(n), because a singly linked list must find its predecessor despite having a tail pointer. get visits index+1 nodes; contains compares values until a match or the end. Each added node uses Θ(1) extra space.

### MinHeap

| Operation | Best Θ | Average Θ | Worst Θ | Overall O / Ω | Auxiliary space |
| --- | --- | --- | --- | --- | --- |
| insert(x) | Θ(1) | expected/amortized Θ(1) | Θ(n) on growth; Θ(log n) without growth | O(n), Ω(1) | Θ(1) without growth; Θ(n) during growth |
| peekMin() | Θ(1) | Θ(1) | Θ(1) | O(1), Ω(1) | Θ(1) |
| extractMin() | Θ(1) | expected Θ(log n) | Θ(log n) | O(log n), Ω(1) | Θ(1) |
| size() | Θ(1) | Θ(1) | Θ(1) | O(1), Ω(1) | Θ(1) |

siftUp and siftDown each traverse at most the heap height, O(log n). Under the stated independent-key model, a new key usually rises only a few levels, giving expected Θ(1) siftUp work; an unfavorable key can rise Θ(log n). insert doubles a full backing array, so that individual call can cost Θ(n), even though resizing is amortized Θ(1) per insertion across a growing sequence. peekMin reads the root. extractMin can stop immediately, but normally sifts the replacement down Θ(log n) levels; extraction does not shrink the array. peekMin and extractMin throw NoSuchElementException when the heap is empty.

## 3. Correctness: Two Loop-Invariant Proofs

### 3.1 DynamicArray.add(index, x): right-shift loop

Let the original size be s, the valid insertion index be k in [0, s], and the original values be A[0], …, A[s-1]. ensureCapacity preserves that prefix and makes slot s available. The loop assigns elements[i] = elements[i-1] for i = s, s-1, …, k+1.

1. **Invariant.** At the start of an iteration with index i, slots [i+1, s] hold the original values A[i], …, A[s-1] in order, while slots [0, i-1] still hold A[0], …, A[i-1]. Thus the next source element has not been overwritten.
2. **Initialization.** At i=s, the shifted suffix is empty and the entire original prefix remains intact after ensureCapacity. The invariant holds.
3. **Maintenance.** The assignment copies A[i-1] into its new slot i. It does not change slots [0, i-2]. After decrementing i, the shifted suffix and untouched prefix have exactly the ranges required by the invariant.
4. **Termination.** The nonnegative quantity i-k decreases on each iteration, so the loop ends at i=k. Then slots [k+1, s] contain the original suffix [k, s-1], and slots [0, k-1] are unchanged.
5. **Conclusion.** Writing x at k and increasing size to s+1 yields the original prefix, x, and the original suffix. Every former element occurs once in its original relative order. If k=s, the loop is empty and the operation is append; if k=0, every old element shifts.

### 3.2 MinHeap.extractMin(): siftDown loop

Before extraction every parent is no greater than its children, so the root is the minimum. extractMin saves that root, replaces it with the last element, and decreases size. The remaining values form the correct multiset; only the replacement at the root may violate the heap order.

1. **Invariant.** At the start of a siftDown iteration at position p, every heap edge is ordered except possibly the edges from p to its children, and the multiset of remaining values is unchanged. If p has a parent, that parent is no greater than either child of p, as inherited from the previously ordered subtree.
2. **Initialization.** Replacing the root can affect only its edges to its children. The root has no parent, and all lower subtrees retain their order. The invariant holds at p=0.
3. **Maintenance.** Choose the smaller child c. If elements[p] ≤ elements[c], both child edges are ordered and no swap is needed. Otherwise swap p and c. The value moved up is no greater than its sibling and is less than the displaced value; its edge to the parent of p remains ordered by the invariant. A violation can now occur only below the displaced value at the new p=c. A swap preserves the multiset, so the invariant holds for the next iteration. Equality does not require a swap.
4. **Termination.** Each swap moves p strictly down a finite tree. The loop stops at a leaf or when p is no greater than its smaller child.
5. **Conclusion.** At termination, the possible offending edges are ordered too, so all heap edges satisfy the min-heap property. The saved original root was the smallest original value and is returned. For a one-element heap, siftDown is skipped and that sole value is returned.

## 4. Experimental Setup

[Benchmark.java](src/Benchmark.java) uses **n = 100, 1,000, 10,000, 100,000**, where n is the initial number of elements, and **m** is the number of timed operations. The fixed workloads are:

| Workload | Structures | Timed operations m per repetition |
| --- | --- | --- |
| Random access | DynamicArray, LinkedList | 10,000 get calls at pre-generated random indices |
| Search | DynamicArray, LinkedList | 1,000 contains calls with pre-generated values |
| Insertion/removal | DynamicArray, LinkedList | 1,000 calls for each of insert front, remove front, insert middle, and remove middle |
| Priority processing | MinHeap | n inserts into an empty heap, followed by n extractMin calls |

Every applicable experiment has **five repetitions** with Random(42). Values are uniformly generated integers in [-1,000,000, 1,000,000]. For search, half the queries are selected from stored values and half are independently generated; accidental matches and duplicates remain possible. Data, indices, and expected answers are prepared before timing.

System.nanoTime() measures one complete get/contains loop or one heap phase. For insertion and removal, durations of individual calls are summed. Structure construction, result checks, counter resets/reads, and printing occur outside the timed intervals. Extraction results are compared with a separately sorted copy. Each CSV value is the mean of the **total time or count for one full repetition** over five repetitions, not the mean time of one call. The CSV time unit is nanoseconds; the report converts it to milliseconds and rounds time to 0.001 ms. Reported counts retain their exact five-run averages. Empty CSV cells mean that a metric does not apply.

Insertion begins with n stored values. At step i, middle insertion uses index (n+i)/2 so that the insertion point follows the growing middle. **Removal uses a restored original state:** after each timed remove(index), add(index, removed) restores the element outside the timed interval. The counters are reset before the next removal, and only the remove duration and counters enter the CSV. Middle removal always uses index n/2. This permits 1,000 removals even for n=100; the measurements do not include the cost of the restore operation.

DynamicArray counts backing-array reads/writes and movements of existing elements during shifts or growth. LinkedList counts visits to existing nodes; front insertion records zero visits while still doing work and allocating a node. Value comparisons for contains and heap sifting are counted separately. The saved CSV was produced on Windows with OpenJDK 25.0.1. There was no separate warm-up phase, variance report, or system-load control, so small timing differences should be treated cautiously.

## 5. Results

All times below are **mean total milliseconds for the stated m operations**, not per-call times. "Accesses" refers to DynamicArray element accesses, "visits" to LinkedList node visits, "moves" to DynamicArray element movements, and "comparisons" to value comparisons. The [CSV](results/tables/benchmark.csv) holds unrounded times and all applicable metrics.

### Workload 1: Random access — 10,000 get calls

| n | DynamicArray time (ms) | DynamicArray accesses | LinkedList time (ms) | LinkedList visits |
| ---: | ---: | ---: | ---: | ---: |
| 100 | 0.160 | 10000 | 0.709 | 506978.6 |
| 1000 | 0.033 | 10000 | 6.527 | 5021542.2 |
| 10000 | 0.035 | 10000 | 66.701 | 49925301.8 |
| 100000 | 0.011 | 10000 | 728.324 | 500098218.4 |

Plots: [time versus n](results/plots/time_random_access.png), [accesses and visits versus n](results/plots/counts_random_access.png).

### Workload 2: Search — 1,000 contains calls

For these implementations and identical queries, each comparison is also one DynamicArray access or one LinkedList visit. Thus the comparison column gives all three counts.

| n | DynamicArray time (ms) | LinkedList time (ms) | Comparisons per structure |
| ---: | ---: | ---: | ---: |
| 100 | 0.397 | 0.271 | 75639 |
| 1000 | 1.348 | 1.813 | 751298.2 |
| 10000 | 3.766 | 15.512 | 7520374.6 |
| 100000 | 27.166 | 154.802 | 73143943.8 |

Plots: [time versus n](results/plots/time_search.png), [comparisons versus n](results/plots/counts_search.png).

### Workload 3: Insertion and removal — 1,000 calls per operation

**Front, index=0.**

| Operation | n | DynamicArray time (ms) | Accesses | Moves | LinkedList time (ms) | Visits |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| insert_front | 100 | 1.361 | 1203000 | 601000 | 0.097 | 0 |
| insert_front | 1000 | 0.256 | 3002000 | 1500500 | 0.040 | 0 |
| insert_front | 10000 | 1.195 | 21020000 | 10509500 | 0.066 | 0 |
| insert_front | 100000 | 11.364 | 201200000 | 100599500 | 0.084 | 0 |
| remove_front | 100 | 0.368 | 199000 | 99000 | 0.076 | 1000 |
| remove_front | 1000 | 0.092 | 1999000 | 999000 | 0.049 | 1000 |
| remove_front | 10000 | 0.532 | 19999000 | 9999000 | 0.083 | 1000 |
| remove_front | 100000 | 9.097 | 199999000 | 99999000 | 0.051 | 1000 |

Plots: [front-operation time](results/plots/time_front.png), [front-insertion counts](results/plots/counts_insert_front.png), [front-removal counts](results/plots/counts_remove_front.png).

**Middle.** Insertion follows the current middle; each removal is from the original middle after restoration.

| Operation | n | DynamicArray time (ms) | Accesses | Moves | LinkedList time (ms) | Visits |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| insert_middle | 100 | 0.537 | 604000 | 301500 | 0.537 | 299500 |
| insert_middle | 1000 | 0.188 | 1503000 | 751000 | 1.258 | 749500 |
| insert_middle | 10000 | 0.649 | 10521000 | 5260000 | 8.234 | 5249500 |
| insert_middle | 100000 | 5.321 | 100701000 | 50350000 | 80.120 | 50249500 |
| remove_middle | 100 | 0.193 | 99000 | 49000 | 0.177 | 51000 |
| remove_middle | 1000 | 0.065 | 999000 | 499000 | 0.769 | 501000 |
| remove_middle | 10000 | 0.228 | 9999000 | 4999000 | 7.434 | 5001000 |
| remove_middle | 100000 | 3.819 | 99999000 | 49999000 | 78.671 | 50001000 |

Plots: [middle-operation time](results/plots/time_middle.png), [middle-insertion counts](results/plots/counts_insert_middle.png), [middle-removal counts](results/plots/counts_remove_middle.png).

### Workload 4: Priority processing — n insert and n extractMin calls

| n | Insert time (ms) | Insert comparisons | Extract time (ms) | Extract comparisons |
| ---: | ---: | ---: | ---: | ---: |
| 100 | 0.044 | 207.8 | 0.065 | 848.8 |
| 1000 | 0.079 | 2234 | 0.170 | 14987.6 |
| 10000 | 0.430 | 22577.4 | 0.945 | 216589.4 |
| 100000 | 2.029 | 227781.8 | 11.327 | 2831578 |

Plots: [time versus n](results/plots/time_priority.png), [comparisons versus n](results/plots/counts_priority.png). All 12 plots name the workload and display units and legends; their n axes use a logarithmic scale.

## 6. Discussion: Answers to the Nine Design Questions

1. **How does increasing n affect each workload?** Random access keeps DynamicArray at exactly 10,000 accesses, while LinkedList visits rise from 506,978.6 to 500,098,218.4 and time from 0.709 to 728.324 ms. Search comparisons rise from 75,639 to 73,143,943.8 per structure, and both times increase overall. Front changes keep LinkedList visits constant (0 for insertion, 1,000 for removal), while DynamicArray moves rise to 100,599,500 for insertion and 99,999,000 for removal at n=100,000. Middle changes raise both array movements and list visits roughly linearly with n. For the heap, insert comparisons rise from 207.8 to 227,781.8 across n inserts, while extract comparisons rise from 848.8 to 2,831,578 across n extractions.

2. **Which results agree with theoretical complexity?** The flat access count for DynamicArray get matches Θ(1) per call; roughly n/2 LinkedList visits per uniformly random get match Θ(n). Both contains implementations perform the same linear number of comparisons, about 0.75n per query in this query mix. Constant LinkedList front-operation visits versus growing array shifts match their Θ(1) and Θ(n) costs. Heap insert comparisons stay near a constant number per random insertion (about 2.28 at n=100,000), whereas extraction comparisons grow with the heap height, as expected for Θ(log n) per extraction.

3. **Where do measurements differ from a simple theoretical prediction?** DynamicArray random-access time falls from 0.160 ms at n=100 to 0.011 ms at n=100,000 even though its 10,000 accesses are unchanged. Front insertion takes 1.361 ms at n=100 but 0.256 ms at n=1,000 despite fewer moves at n=100. LinkedList search is slightly faster at n=100 (0.271 versus 0.397 ms), then slower at larger n. Big-O predicts growth bounds, not monotonic time or a winner at each finite n. Capacity growth, JIT compilation, cache state, GC, timer overhead, and the lack of a separate warm-up can influence these short measurements; the data do not isolate which factor caused each anomaly.

4. **Why can algorithms with the same Big-O have different times?** Indexed middle insertion is Θ(n) for both structures, yet at n=100,000 it takes 5.321 ms in DynamicArray and 80.120 ms in LinkedList. The array shifts contiguous cells; the list follows references through nodes. The same distinction appears in middle removal (3.819 versus 78.671 ms). Big-O omits memory locality and the cost of each elementary step.

5. **How do constant factors and implementation details matter?** DynamicArray performs consecutive reads and writes; LinkedList allocates nodes and dereferences pointers. Both search algorithms make exactly 73,143,943.8 comparisons at n=100,000, but take 27.166 and 154.802 ms respectively. The list's counter records zero visits for front insertion because it traverses no existing node, although allocation and pointer updates still cost time. Heap growth copies an entire backing array on some inserts, even when the comparison count remains small. These details change elapsed time without changing the stated asymptotic bounds.

6. **Why prefer DynamicArray for some workloads?** get is Θ(1) and makes one counted access per call; LinkedList get is Θ(n). At n=100,000, 10,000 get calls take 0.011 versus 728.324 ms. Array search also wins at that size despite equal comparison counts (27.166 versus 154.802 ms), and its contiguous shifts beat indexed middle traversal for the measured insertion/removal workload. Append is amortized Θ(1) over a growth sequence, though any one resize can cost Θ(n).

7. **When is LinkedList useful?** For frequent changes at the head, add(0, x) and remove(0) change a fixed number of links rather than shifting n array elements. At n=100,000, 1,000 front insertions take 0.084 ms in LinkedList versus 11.364 ms in DynamicArray; front removals take 0.051 versus 9.097 ms. Appending is also Θ(1) via the tail. An already available predecessor node would allow constant-time relinking, but this implementation's indexed middle API must first traverse from the head.

8. **Why is a heap suitable for priority processing?** The smallest item is at the root, so peekMin is Θ(1), and extractMin restores order in O(log n) rather than scanning all values for every minimum. At n=100,000, the measured heap performs 100,000 inserts in 2.029 ms and 100,000 extractions in 11.327 ms, with 227,781.8 and 2,831,578 comparisons respectively. The benchmark checks that extraction exactly matches the sorted input, including duplicates. A heap does not make arbitrary indexed access or full sorting constant time; it supports repeated minimum retrieval efficiently.

9. **How does workload determine the choice?** Choose DynamicArray when indexed reads, scans, or indexed middle changes dominate; choose LinkedList when frequent front changes dominate; choose MinHeap when the next minimum repeatedly determines the next action. The measured n=100,000 results show these distinct advantages. The recommendation is for these implementations and operations: a mixed workload should weigh how often each operation occurs, the cost of occasional array growth, and memory use, rather than select a structure from one Big-O label alone.

Finite n, five repetitions, and no independent warm-up cannot establish an asymptotic law from time alone. Counters support the predicted mechanisms, while timings also reflect the Java runtime and this machine. The removal results isolate remove cost because restoration occurs outside the timer; an application that performs both removal and restoration would have to include both costs.

## 7. Design Recommendations

| Workload or use case | Recommended structure | Reason from theory and measured results |
| --- | --- | --- |
| Random indexed access | DynamicArray | Θ(1) get and 10,000 accesses at every n; LinkedList takes Θ(n) traversal and reaches 500,098,218.4 visits at n=100,000. |
| Search without an index | DynamicArray for medium and large n | Both scans are Θ(n), but contiguous array access is faster for n≥1,000 in this CSV. |
| Frequent front insertion/removal | LinkedList | Θ(1) link changes versus Θ(n) array shifts; it wins at every measured n. |
| Indexed middle insertion/removal | DynamicArray in this implementation | Both are Θ(n), but contiguous shifts are faster than list traversal for n≥1,000 in the measured runs. |
| Append followed by indexed reads | DynamicArray | Amortized Θ(1) append across growth, then Θ(1) get; allow for occasional Θ(n) resize. |
| Repeated minimum-priority processing | MinHeap | Θ(1) peekMin and O(log n) extractMin; extracted values matched sorted input. |

## 8. Conclusion

Theory and counters identify the main mechanisms: array indexing is constant time, linked traversal and linear search grow with n, front shifts make array changes costly, and heap extraction follows tree height. The measured times show the same large-scale tradeoffs while also revealing effects of memory layout, resizing, and runtime behavior at finite sizes. The appropriate structure depends on the operations that dominate the workload: DynamicArray for indexed access and contiguous scans, LinkedList for changes at the head, and MinHeap for repeated minimum retrieval.
