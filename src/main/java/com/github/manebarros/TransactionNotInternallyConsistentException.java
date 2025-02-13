package com.github.manebarros;

/**
 * Thrown when attempting to calculate the abstract version of an internally inconsistent
 * transaction.
 */
public final class TransactionNotInternallyConsistentException extends RuntimeException {

  private static String buildMessage(String tid, Object object, Object expected, Object actual) {
    StringBuilder sb = new StringBuilder();
    sb.append("\nTransaction \"")
        .append(tid)
        .append("\" is not internally consistent. In a read of object ")
        .append(object)
        .append(",\nexpected: ")
        .append(expected)
        .append(" but got: ")
        .append(actual);
    return sb.toString();
  }

  private static String buildMessage(Transaction t, Object object, Object expected, Object actual) {
    StringBuilder sb = new StringBuilder();
    sb.append("\nTransaction [")
        .append(t)
        .append("] is not internally consistent. In a read of object ")
        .append(object)
        .append(",\nexpected: ")
        .append(expected)
        .append(" but got: ")
        .append(actual);
    return sb.toString();
  }

  public <K, V> TransactionNotInternallyConsistentException(
      String tid, K object, V expected, V actual) {
    super(buildMessage(tid, object, expected, actual));
  }

  public <K, V> TransactionNotInternallyConsistentException(
      Transaction t, K object, V expected, V actual) {
    super(buildMessage(t, object, expected, actual));
  }
}
