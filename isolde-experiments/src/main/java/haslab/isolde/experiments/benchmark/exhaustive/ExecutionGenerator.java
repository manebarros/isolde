package haslab.isolde.experiments.benchmark.exhaustive;

import haslab.isolde.core.synth.Scope;
import haslab.isolde.history.AbstractHistory;
import haslab.isolde.history.AbstractTransaction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

public class ExecutionGenerator {
  private final Scope scope;

  public ExecutionGenerator(Scope scope) {
    this.scope = scope;
  }

  /**
   * Computes the powerset of integers from 0 to size-1.
   *
   * @param size the size of the base set
   * @return a set containing all subsets of {0, 1, 2, ..., size-1}
   */
  public static Set<Set<Integer>> powerSet(int size) {
    Set<Set<Integer>> result = new HashSet<>();

    // The powerset has 2^size elements
    int powerSetSize = 1 << size; // equivalent to Math.pow(2, size)

    // Iterate through all possible subsets using bit manipulation
    for (int i = 0; i < powerSetSize; i++) {
      Set<Integer> subset = new HashSet<>();

      // Check each bit position
      for (int j = 0; j < size; j++) {
        // If the j-th bit is set in i, include j in the subset
        if ((i & (1 << j)) != 0) {
          subset.add(j);
        }
      }

      result.add(subset);
    }

    return result;
  }

  public Iterator<List<Set<Integer>>> allLists() {
    return allLists(
        new ArrayList<>(powerSet(this.scope.getObjects())), this.scope.getTransactions());
  }

  public Iterator<List<Set<Integer>>> allLists(List<Set<Integer>> powerset, int size) {
    return new Iterator<List<Set<Integer>>>() {
      private final int n = powerset.size();
      private final int[] indices = new int[size];
      private boolean hasNext = size > 0 && n > 0;

      @Override
      public boolean hasNext() {
        return hasNext;
      }

      @Override
      public List<Set<Integer>> next() {
        if (!hasNext) {
          throw new NoSuchElementException();
        }

        // Build current sequence
        List<Set<Integer>> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
          result.add(powerset.get(indices[i]));
        }

        // Advance to next combination (like incrementing a base-n number)
        int pos = size - 1;
        while (pos >= 0) {
          indices[pos]++;
          if (indices[pos] < n) {
            break;
          }
          indices[pos] = 0;
          pos--;
        }

        // If we carried past the first position, we're done
        if (pos < 0) {
          hasNext = false;
        }

        return result;
      }
    };
  }

  public Iterator<AbstractHistory> allHistories() {
    Iterator<List<AbstractTransaction>> txnIterator = allTransactionCombinations();

    assert txnIterator.hasNext();

    return new Iterator<AbstractHistory>() {
      private List<AbstractTransaction> currentTxnCombination = txnIterator.next();
      private Iterator<Set<List<Integer>>> soIterator =
          SequenceCombinations.allSequenceCombinations(scope.getTransactions());

      @Override
      public boolean hasNext() {
        return soIterator.hasNext() || txnIterator.hasNext();
      }

      @Override
      public AbstractHistory next() {
        if (!soIterator.hasNext()) {
          this.currentTxnCombination = txnIterator.next();
          this.soIterator = SequenceCombinations.allSequenceCombinations(scope.getTransactions());
        }
        Set<List<Integer>> currentSo = soIterator.next();
        return new AbstractHistory(currentTxnCombination, currentSo);
      }
    };
  }

  private Iterator<List<AbstractTransaction>> allTransactionCombinations() {
    Iterator<List<Map<Integer, Integer>>> readsIterator = allMappings();

    assert readsIterator.hasNext();

    return new Iterator<List<AbstractTransaction>>() {
      private List<Map<Integer, Integer>> currentReads = readsIterator.next();
      private Iterator<List<Map<Integer, Integer>>> writesIterator = allMappings();

      @Override
      public boolean hasNext() {
        return writesIterator.hasNext() || readsIterator.hasNext();
      }

      @Override
      public List<AbstractTransaction> next() {
        if (!writesIterator.hasNext()) {
          this.currentReads = readsIterator.next();
          this.writesIterator = allMappings();
        }
        List<Map<Integer, Integer>> currWrites = writesIterator.next();
        List<AbstractTransaction> transactions = new ArrayList<>();
        for (int i = 0; i < scope.getTransactions(); i++) {
          transactions.add(new AbstractTransaction(currentReads.get(i), currWrites.get(i)));
        }
        return transactions;
      }
    };
  }

  public Iterator<AbstractExecution> allExecutions() {
    Iterator<AbstractHistory> allHistories = allHistories();

    assert allHistories.hasNext();

    return new Iterator<AbstractExecution>() {
      private AbstractHistory currentHistory = allHistories.next();
      private Iterator<AbstractExecution> executionIterator = allExecutions(this.currentHistory);

      @Override
      public boolean hasNext() {
        return executionIterator.hasNext() || allHistories.hasNext();
      }

      @Override
      public AbstractExecution next() {
        if (!executionIterator.hasNext()) {
          this.currentHistory = allHistories.next();
          this.executionIterator = allExecutions(this.currentHistory);
        }
        return this.executionIterator.next();
      }
    };
  }

  public Iterator<AbstractExecution> allExecutions(AbstractHistory history) {
    Set<Integer> transactions = transactions();

    return new Iterator<AbstractExecution>() {
      private Iterator<List<Integer>> commitOrderIterator =
          PermutationIterator.allPermutations(transactions);

      @Override
      public boolean hasNext() {
        return commitOrderIterator.hasNext();
      }

      @Override
      public AbstractExecution next() {
        var co = commitOrderIterator.next();
        return new AbstractExecution(history, co);
      }
    };
  }

  public Iterator<List<Map<Integer, Integer>>> allMappings() {
    Iterator<List<Set<Integer>>> readObjectsIterator = allLists();

    assert readObjectsIterator.hasNext();

    return new Iterator<List<Map<Integer, Integer>>>() {
      private List<Set<Integer>> currentReadObjects = readObjectsIterator.next();
      private Iterator<List<Map<Integer, Integer>>> readOperationsIterator =
          allMappings(currentReadObjects, values());

      @Override
      public boolean hasNext() {
        return readOperationsIterator.hasNext() || readObjectsIterator.hasNext();
      }

      @Override
      public List<Map<Integer, Integer>> next() {
        if (!readOperationsIterator.hasNext()) {
          this.currentReadObjects = readObjectsIterator.next();
          this.readOperationsIterator = allMappings(currentReadObjects, values());
        }
        return this.readOperationsIterator.next();
      }
    };
  }

  private Set<Integer> values() {
    Set<Integer> values = new HashSet<>();
    for (int i = 0; i < this.scope.getValues(); i++) {
      values.add(i);
    }
    return values;
  }

  private Set<Integer> transactions() {
    Set<Integer> transactions = new HashSet<>();
    for (int i = 0; i < this.scope.getTransactions(); i++) {
      transactions.add(i);
    }
    return transactions;
  }

  public Iterator<List<Map<Integer, Integer>>> allMappings(
      List<Set<Integer>> keys, Set<Integer> possibleValues) {
    return new Iterator<List<Map<Integer, Integer>>>() {
      private final int numMaps = keys.size();
      private final List<Integer> valuesList = new ArrayList<>(possibleValues);
      private final int numValues = valuesList.size();

      // For each map, store the list of keys and current value indices
      private final List<List<Integer>> keyLists = new ArrayList<>();
      private final List<int[]> valueIndices = new ArrayList<>();
      private boolean hasNext;

      {
        // Initialize
        hasNext = numValues > 0;
        for (Set<Integer> keySet : keys) {
          List<Integer> keyList = new ArrayList<>(keySet);
          keyLists.add(keyList);

          if (keyList.isEmpty()) {
            // Empty key set contributes only one empty map
            valueIndices.add(new int[0]);
          } else {
            valueIndices.add(new int[keyList.size()]);
            if (numValues == 0) {
              hasNext = false; // Can't map keys to empty value set
            }
          }
        }

        if (numMaps == 0) {
          hasNext = false;
        }
      }

      @Override
      public boolean hasNext() {
        return hasNext;
      }

      @Override
      public List<Map<Integer, Integer>> next() {
        if (!hasNext) {
          throw new NoSuchElementException();
        }

        // Build current list of maps
        List<Map<Integer, Integer>> result = new ArrayList<>(numMaps);
        for (int i = 0; i < numMaps; i++) {
          Map<Integer, Integer> map = new HashMap<>();
          List<Integer> keyList = keyLists.get(i);
          int[] indices = valueIndices.get(i);

          for (int j = 0; j < keyList.size(); j++) {
            map.put(keyList.get(j), valuesList.get(indices[j]));
          }
          result.add(map);
        }

        // Advance to next combination
        // We treat this as a multi-dimensional counter where each map
        // has its own base-numValues counter for its keys
        boolean carry = true;
        for (int mapIdx = numMaps - 1; mapIdx >= 0 && carry; mapIdx--) {
          int[] indices = valueIndices.get(mapIdx);
          if (indices.length == 0) {
            continue; // Skip empty maps
          }

          // Increment this map's counter
          int pos = indices.length - 1;
          while (pos >= 0) {
            indices[pos]++;
            if (indices[pos] < numValues) {
              carry = false;
              break;
            }
            indices[pos] = 0;
            pos--;
          }

          if (pos < 0) {
            // This map overflowed, continue to next map
            carry = true;
          } else {
            carry = false;
          }
        }

        // If we still have carry, we've exhausted all combinations
        if (carry) {
          hasNext = false;
        }

        return result;
      }
    };
  }
}
