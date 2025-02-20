package com.github.manebarros;

import java.util.List;
import java.util.function.Function;

public class Contextualized<T> {
  private final AbstractHistoryK historyEncoding;
  private final List<BiswasExecutionK> biswasExecutions;
  private final List<CeroneExecutionK> ceroneExecutions;
  private final T content;

  public AbstractHistoryK getHistoryEncoding() {
    return historyEncoding;
  }

  public List<BiswasExecutionK> getBiswasExecutions() {
    return biswasExecutions;
  }

  public List<CeroneExecutionK> getCeroneExecutions() {
    return ceroneExecutions;
  }

  public T getContent() {
    return content;
  }

  public Contextualized(
      AbstractHistoryK historyEncoding,
      List<BiswasExecutionK> biswasExecutions,
      List<CeroneExecutionK> ceroneExecutions,
      T content) {
    this.historyEncoding = historyEncoding;
    this.biswasExecutions = biswasExecutions;
    this.ceroneExecutions = ceroneExecutions;
    this.content = content;
  }

  public <S> Contextualized<S> fmap(Function<T, S> f) {
    return new Contextualized<S>(
        historyEncoding, biswasExecutions, ceroneExecutions, f.apply(content));
  }

  public <S> Contextualized<S> replace(S newContent) {
    return new Contextualized<S>(historyEncoding, biswasExecutions, ceroneExecutions, newContent);
  }
}
