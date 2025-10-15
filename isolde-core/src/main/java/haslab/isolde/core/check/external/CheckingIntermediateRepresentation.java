package haslab.isolde.core.check.external;

import haslab.isolde.core.general.AtomsContainer;
import haslab.isolde.history.History;
import haslab.isolde.kodkod.Atom;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CheckingIntermediateRepresentation implements AtomsContainer {
  private List<Atom<Integer>> sessAtoms;
  private List<List<Atom<Integer>>> txnAtoms;
  private Map<Integer, Atom<Integer>> keyAtoms;
  private Map<Integer, Atom<Integer>> valAtoms;
  private Atom<Integer> initialTxnAtom;
  private History history;

  public History getHistory() {
    return history;
  }

  public List<Atom<Integer>> normalTxnAtoms() {
    return txnAtoms.stream().flatMap(s -> s.stream()).collect(Collectors.toList());
  }

  public CheckingIntermediateRepresentation(History history) {
    this.sessAtoms = new ArrayList<>();
    this.txnAtoms = new ArrayList<>();
    this.keyAtoms = new LinkedHashMap<>();
    this.valAtoms = new LinkedHashMap<>();
    this.initialTxnAtom = new Atom<Integer>("t", 0);
    this.history = history;
    valAtoms.put(0, new Atom<>("v", 0));

    int nextTid = 1;
    int nextSid = 0;
    for (var session : history.getSessions()) {
      sessAtoms.add(new Atom<>("s", nextSid++));
      List<Atom<Integer>> sessTxnAtoms = new ArrayList<>();
      txnAtoms.add(sessTxnAtoms);
      for (var txn : session.transactions()) {
        sessTxnAtoms.add(new Atom<>("t", nextTid++));
        for (var op : txn.operations()) {
          var key = op.object();
          var val = op.value();
          if (!keyAtoms.containsKey(key)) {
            keyAtoms.put(key, new Atom<>("x", key));
          }
          if (!valAtoms.containsKey(val)) {
            valAtoms.put(val, new Atom<>("v", val));
          }
        }
      }
    }
  }

  @Override
  public List<Object> atoms() {
    List<Object> allAtoms = new ArrayList<>();
    allAtoms.addAll(normalTxnAtoms());
    allAtoms.add(initialTxnAtom);
    allAtoms.addAll(keyAtoms.values());
    allAtoms.addAll(valAtoms.values());
    allAtoms.addAll(sessAtoms);
    return allAtoms;
  }

  public List<Atom<Integer>> getSessAtoms() {
    return sessAtoms;
  }

  public List<List<Atom<Integer>>> getTxnAtoms() {
    return txnAtoms;
  }

  public Map<Integer, Atom<Integer>> getKeyAtoms() {
    return keyAtoms;
  }

  public Map<Integer, Atom<Integer>> getValAtoms() {
    return valAtoms;
  }

  public Atom<Integer> getInitialTxnAtom() {
    return initialTxnAtom;
  }
}
