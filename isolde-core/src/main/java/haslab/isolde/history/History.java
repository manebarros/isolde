package haslab.isolde.history;

import haslab.isolde.core.AbstractHistoryK;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import kodkod.instance.Instance;

public class History {
  private final List<Session> sessions;

  public History(Session session) {
    this.sessions = Collections.singletonList(session);
  }

  public History(Session session, Session... s) {
    this.sessions = new ArrayList<>();
    this.sessions.add(session);
    this.sessions.addAll(Arrays.asList(s));
  }

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
