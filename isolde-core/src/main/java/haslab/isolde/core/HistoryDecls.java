package haslab.isolde.core;

import kodkod.ast.Decls;

@FunctionalInterface
public interface HistoryDecls {
  Decls resolve(AbstractHistoryK historyEncoding);
}
