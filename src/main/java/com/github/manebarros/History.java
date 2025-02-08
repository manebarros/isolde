package com.github.manebarros;

import java.util.ArrayList;
import java.util.List;
import kodkod.instance.Instance;

public class History {
  private final int initialValue;
  private final List<Session> sessions;

  public History(int initialValue, List<Session> sessions) {
    this.initialValue = initialValue;
    this.sessions = sessions;
  }

  public History(History h) {
    this(h.getInitialValue(), new ArrayList<>(h.getSessions()));
  }

  public History(Instance instance) {
    this(new HistoryIntermediateRepresentation(instance).buildHistory());
  }

  public int getInitialValue() {
    return initialValue;
  }

  public List<Session> getSessions() {
    return sessions;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("initial value: " + initialValue + "\n");
    if (!sessions.isEmpty()) {
      sb.append(sessions.get(0));
    }
    for (var s : sessions.subList(1, sessions.size())) {
      sb.append("\n\n").append(s);
    }
    return sb.toString();
  }
}
