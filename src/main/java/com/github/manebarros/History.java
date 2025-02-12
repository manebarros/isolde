package com.github.manebarros;

import java.util.ArrayList;
import java.util.List;
import kodkod.instance.Instance;

public class History {
  private final List<Session> sessions;

  public History(List<Session> sessions) {
    this.sessions = sessions;
  }

  public History(History h) {
    this(new ArrayList<>(h.getSessions()));
  }

  public History(AbstractHistoryK encoding, Instance instance) {
    this(new HistoryIntermediateRepresentation(encoding, instance).buildHistory());
  }

  public List<Session> getSessions() {
    return sessions;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (!sessions.isEmpty()) {
      sb.append(sessions.get(0));

      for (var s : sessions.subList(1, sessions.size())) {
        sb.append("\n\n").append(s);
      }
    }
    return sb.toString();
  }
}
