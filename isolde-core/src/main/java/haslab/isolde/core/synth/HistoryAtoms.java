package haslab.isolde.core.synth;

import haslab.isolde.core.general.AtomsContainer;
import haslab.isolde.kodkod.Atom;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class HistoryAtoms implements AtomsContainer {
  private List<Atom<Integer>> txnAtoms;
  private List<Atom<Integer>> objAtoms;
  private List<Atom<Integer>> valAtoms;
  private List<Atom<Integer>> sessionAtoms;

  public HistoryAtoms(Scope scope) {
    this.txnAtoms =
        IntStream.rangeClosed(0, scope.getTransactions())
            .mapToObj(i -> new Atom<>("t", i))
            .collect(Collectors.toList());

    this.objAtoms =
        IntStream.range(0, scope.getObjects())
            .mapToObj(i -> new Atom<>("x", i))
            .collect(Collectors.toList());

    this.valAtoms =
        IntStream.range(0, scope.getValues())
            .mapToObj(i -> new Atom<>("v", i))
            .collect(Collectors.toList());

    this.sessionAtoms =
        IntStream.range(0, scope.getSessions())
            .mapToObj(i -> new Atom<>("s", i))
            .collect(Collectors.toList());
  }

  public Atom<Integer> initialTxn() {
    return this.txnAtoms.get(0);
  }

  public List<Atom<Integer>> normalTxns() {
    return txnAtoms.subList(1, txnAtoms.size());
  }

  public List<Atom<Integer>> normalValues() {
    return valAtoms.subList(1, valAtoms.size());
  }

  @Override
  public List<Object> atoms() {
    List<Object> atoms = new ArrayList<>();
    atoms.addAll(this.txnAtoms);
    atoms.addAll(this.objAtoms);
    atoms.addAll(this.valAtoms);
    atoms.addAll(this.sessionAtoms);
    return atoms;
  }

  public List<Atom<Integer>> getTxnAtoms() {
    return txnAtoms;
  }

  public List<Atom<Integer>> getObjAtoms() {
    return objAtoms;
  }

  public List<Atom<Integer>> getValAtoms() {
    return valAtoms;
  }

  public List<Atom<Integer>> getSessionAtoms() {
    return sessionAtoms;
  }
}
