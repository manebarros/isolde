package haslab.isolde.core.general;

import haslab.isolde.core.HistoryDecls;
import java.util.Collection;
import java.util.Optional;

public interface Input {
  Collection<Object> atoms();

  Optional<HistoryDecls> decls();
}
