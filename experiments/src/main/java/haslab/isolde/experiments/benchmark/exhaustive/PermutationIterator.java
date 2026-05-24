package haslab.isolde.experiments.benchmark.exhaustive;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

public class PermutationIterator implements Iterator<List<Integer>> {
  private final List<Integer> elements;
  private final int[] indices;
  private final int[] cycles;
  private boolean hasNext;

  public PermutationIterator(List<Integer> elements) {
    this.elements = new ArrayList<>(elements);
    int n = elements.size();
    this.indices = new int[n];
    this.cycles = new int[n];

    for (int i = 0; i < n; i++) {
      indices[i] = i;
      cycles[i] = n - i;
    }

    this.hasNext = n > 0;
  }

  @Override
  public boolean hasNext() {
    return hasNext;
  }

  @Override
  public List<Integer> next() {
    if (!hasNext) {
      throw new NoSuchElementException();
    }

    // Build current permutation
    List<Integer> result = new ArrayList<>(indices.length);
    for (int i : indices) {
      result.add(elements.get(i));
    }

    // Compute next permutation (using algorithm similar to Python's itertools)
    int n = indices.length;
    for (int i = n - 1; i >= 0; i--) {
      cycles[i]--;
      if (cycles[i] == 0) {
        // Rotate indices from i to end
        int temp = indices[i];
        System.arraycopy(indices, i + 1, indices, i, n - i - 1);
        indices[n - 1] = temp;
        cycles[i] = n - i;
      } else {
        // Swap
        int j = n - cycles[i];
        int temp = indices[i];
        indices[i] = indices[j];
        indices[j] = temp;
        return result;
      }
    }

    hasNext = false;
    return result;
  }

  public static Iterator<List<Integer>> allPermutations(Set<Integer> values) {
    return new PermutationIterator(new ArrayList<>(values));
  }
}
