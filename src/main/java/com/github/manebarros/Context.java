package com.github.manebarros;

import java.util.List;
import java.util.function.Function;
import kodkod.ast.Expression;

public class Context<T> {
  private final List<Expression> commitOrders;
  private final T content;

  public Context(List<Expression> commitOrders, T content) {
    this.commitOrders = commitOrders;
    this.content = content;
  }

  public List<Expression> getCommitOrders() {
    return commitOrders;
  }

  public T getContent() {
    return content;
  }

  public <S> Context<S> fmap(Function<T, S> f) {
    return new Context<S>(commitOrders, f.apply(content));
  }
}
