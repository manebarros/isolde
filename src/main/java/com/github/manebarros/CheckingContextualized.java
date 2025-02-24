package com.github.manebarros;

import java.util.function.Function;

public class CheckingContextualized<E, T> {
  private final AbstractHistoryK historyEncoding;
  private final E execution;
  private final T content;

  public CheckingContextualized(AbstractHistoryK historyEncoding, E execution, T content) {
    this.historyEncoding = historyEncoding;
    this.execution = execution;
    this.content = content;
  }

  public AbstractHistoryK getHistoryEncoding() {
    return historyEncoding;
  }

  public E getExecution() {
    return execution;
  }

  public T getContent() {
    return content;
  }

  public <S> CheckingContextualized<E, S> fmap(Function<T, S> f) {
    return new CheckingContextualized<>(
        this.historyEncoding, this.execution, f.apply(this.content));
  }

  public <S> CheckingContextualized<E, S> replace(S newContent) {
    return new CheckingContextualized<>(this.historyEncoding, this.execution, newContent);
  }
}
