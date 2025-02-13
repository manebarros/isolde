package com.github.manebarros;

import java.util.Objects;

public record Atom<T>(String type, T value) {

  @Override
  public final String toString() {
    return type + value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Atom<?> that = (Atom<?>) o;
    return type.equals(that.type) && value.equals(that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, value);
  }
}
