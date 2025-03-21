package com.github.manebarros.generic.cerone;

import com.github.manebarros.generic.ExecutionFormula;
import com.github.manebarros.generic.HistoryAtoms;
import com.github.manebarros.generic.SynthesisEncoder;
import com.github.manebarros.generic.SynthesisModule;
import java.util.List;

public class CeroneSynthesisEncoder implements SynthesisEncoder<CeroneExecution> {

  @Override
  public SynthesisModule<CeroneExecution> getModule(
      List<ExecutionFormula<CeroneExecution>> formulas, HistoryAtoms atoms) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getModule'");
  }
}
