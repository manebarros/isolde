package haslab.isolde.history;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AbstractHistory {
  private final List<AbstractSession> sessions;

  public AbstractHistory(AbstractSession session) {
    this.sessions = Collections.singletonList(session);
  }

  public AbstractHistory(AbstractSession session, AbstractSession... s) {
    this.sessions = new ArrayList<>();
    this.sessions.add(session);
    this.sessions.addAll(Arrays.asList(s));
  }

  public AbstractHistory(List<AbstractSession> sessions) {
    this.sessions = sessions;
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

  public List<AbstractSession> getSessions() {
    return sessions;
  }
}
