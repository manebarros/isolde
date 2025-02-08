package com.github.manebarros;

public record Atom<T>(String type, T value) {
  @Override
  public final String toString() {
    return type + value;
  }
}
