package haslab.isolde.core.check.candidate;

import haslab.isolde.core.general.DirectHistoryConstraintProblem;
import haslab.isolde.core.general.HistoryEncoder;

public class CandidateCheckProblem extends DirectHistoryConstraintProblem<Candidate> {

  public CandidateCheckProblem(Candidate input) {
    this(input, CandidateHistoryEncoder.instance());
  }

  public CandidateCheckProblem(Candidate input, HistoryEncoder<Candidate> encoder) {
    super(input, encoder);
  }
}
