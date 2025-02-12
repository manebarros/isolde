package com.github.manebarros;

import java.util.List;
import java.util.function.Function;
import kodkod.ast.Expression;

public class Contextualized<T> {
  private final AbstractHistoryK historyEncoding;
  private final List<Expression> commitOrders;
  private final T content;

  public Contextualized(
      AbstractHistoryK historyEncoding, List<Expression> commitOrders, T content) {
    this.historyEncoding = historyEncoding;
    this.commitOrders = commitOrders;
    this.content = content;
  }

  public AbstractHistoryK getHistoryEncoding() {
    return historyEncoding;
  }

  public List<Expression> getCommitOrders() {
    return commitOrders;
  }

  public T getContent() {
    return content;
  }

  public <S> Contextualized<S> fmap(Function<T, S> f) {
    return new Contextualized<S>(historyEncoding, commitOrders, f.apply(content));
  }

  public <S> Contextualized<S> replace(S newContent) {
    return new Contextualized<S>(historyEncoding, commitOrders, newContent);
  }
}
