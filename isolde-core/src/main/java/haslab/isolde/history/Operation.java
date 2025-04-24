package haslab.isolde.history;

public record Operation(Type type, int object, int value) {
  public enum Type {
    WRITE,
    READ,
  }

  public static Operation readOf(int object, int value) {
    return new Operation(Type.READ, object, value);
  }

  public static Operation writeOf(int object, int value) {
    return new Operation(Type.WRITE, object, value);
  }

  public boolean isRead() {
    return this.type().equals(Type.READ);
  }

  public boolean isWrite() {
    return this.type().equals(Type.WRITE);
  }

  @Override
  public final String toString() {
    return (this.isRead() ? "r" : "w") + "(" + object + "," + value + ")";
  }
}
