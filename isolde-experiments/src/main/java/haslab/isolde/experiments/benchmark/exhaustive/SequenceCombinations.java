package haslab.isolde.experiments.benchmark.exhaustive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class SequenceCombinations {

  /**
   * Returns an iterator over all possible ways to organize values [1..size] into different
   * sequences (set partitions with internal permutations).
   */
  public static Iterator<Set<List<Integer>>> allSequenceCombinations(int size) {
    if (size <= 0) {
      return Collections.emptyIterator();
    }

    List<Integer> elements = new ArrayList<>();
    for (int i = 1; i <= size; i++) {
      elements.add(i);
    }

    List<Set<List<Integer>>> allCombinations = new ArrayList<>();

    // Generate all partitions of the elements
    List<List<Set<Integer>>> partitions = generatePartitions(elements);

    // For each partition, generate all permutations within each subset
    for (List<Set<Integer>> partition : partitions) {
      List<Set<List<Integer>>> partitionCombinations = generatePermutationsForPartition(partition);
      allCombinations.addAll(partitionCombinations);
    }

    return allCombinations.iterator();
  }

  /** Generate all set partitions of the given elements. */
  private static List<List<Set<Integer>>> generatePartitions(List<Integer> elements) {
    List<List<Set<Integer>>> result = new ArrayList<>();

    if (elements.isEmpty()) {
      result.add(new ArrayList<>());
      return result;
    }

    Integer first = elements.get(0);
    List<Integer> rest = elements.subList(1, elements.size());

    // Get all partitions of the rest
    List<List<Set<Integer>>> restPartitions = generatePartitions(rest);

    for (List<Set<Integer>> partition : restPartitions) {
      // Add first element to each existing subset
      for (int i = 0; i < partition.size(); i++) {
        List<Set<Integer>> newPartition = new ArrayList<>();
        for (int j = 0; j < partition.size(); j++) {
          Set<Integer> subset = new HashSet<>(partition.get(j));
          if (i == j) {
            subset.add(first);
          }
          newPartition.add(subset);
        }
        result.add(newPartition);
      }

      // Create a new subset with just the first element
      List<Set<Integer>> newPartition = new ArrayList<>(partition);
      Set<Integer> newSubset = new HashSet<>();
      newSubset.add(first);
      newPartition.add(newSubset);
      result.add(newPartition);
    }

    return result;
  }

  /**
   * For a given partition, generate all combinations where each subset is permuted in all possible
   * ways.
   */
  private static List<Set<List<Integer>>> generatePermutationsForPartition(
      List<Set<Integer>> partition) {

    List<Set<List<Integer>>> result = new ArrayList<>();

    // Convert each set to all its permutations
    List<List<List<Integer>>> allSubsetPermutations = new ArrayList<>();
    for (Set<Integer> subset : partition) {
      List<Integer> subsetList = new ArrayList<>(subset);
      List<List<Integer>> permutations = generatePermutations(subsetList);
      allSubsetPermutations.add(permutations);
    }

    // Generate cartesian product of all subset permutations
    generateCartesianProduct(allSubsetPermutations, 0, new ArrayList<>(), result);

    return result;
  }

  /** Generate all permutations of a list. */
  private static List<List<Integer>> generatePermutations(List<Integer> elements) {
    List<List<Integer>> result = new ArrayList<>();

    if (elements.isEmpty()) {
      result.add(new ArrayList<>());
      return result;
    }

    for (int i = 0; i < elements.size(); i++) {
      Integer current = elements.get(i);
      List<Integer> remaining = new ArrayList<>();
      remaining.addAll(elements.subList(0, i));
      remaining.addAll(elements.subList(i + 1, elements.size()));

      List<List<Integer>> subPermutations = generatePermutations(remaining);
      for (List<Integer> subPerm : subPermutations) {
        List<Integer> perm = new ArrayList<>();
        perm.add(current);
        perm.addAll(subPerm);
        result.add(perm);
      }
    }

    return result;
  }

  /** Generate cartesian product of permutations to create all combinations. */
  private static void generateCartesianProduct(
      List<List<List<Integer>>> allSubsetPermutations,
      int index,
      List<List<Integer>> current,
      List<Set<List<Integer>>> result) {

    if (index == allSubsetPermutations.size()) {
      result.add(new HashSet<>(current));
      return;
    }

    for (List<Integer> permutation : allSubsetPermutations.get(index)) {
      List<List<Integer>> newCurrent = new ArrayList<>(current);
      newCurrent.add(permutation);
      generateCartesianProduct(allSubsetPermutations, index + 1, newCurrent, result);
    }
  }

  // Test method
  public static void main(String[] args) {
    Iterator<Set<List<Integer>>> iter = allSequenceCombinations(3);
    int count = 0;

    System.out.println("All sequence combinations for size 3:");
    while (iter.hasNext()) {
      Set<List<Integer>> combination = iter.next();
      count++;
      System.out.println(count + ": " + combination);
    }

    System.out.println("\nTotal combinations: " + count);
  }
}
