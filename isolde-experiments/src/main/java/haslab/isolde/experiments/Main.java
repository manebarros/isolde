package haslab.isolde.experiments;

import haslab.isolde.experiments.benchmark.Cli;
import picocli.CommandLine;

public class Main {
  public static void main(String[] args) throws Exception {
    CommandLine cmd = new CommandLine(new Cli()).setCaseInsensitiveEnumValuesAllowed(true);
    int exitCode = cmd.execute(args);
    System.exit(exitCode);

    // Synthesizer synth = new Synthesizer(SATFactory.Glucose);
    // for (int i = 2; i <= 5; i++) {
    //  System.out.println(
    //      String.format(
    //          "Start looking for scope %d at %s",
    //          i, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
    //  SynthesisSolution solution =
    //      synth.synthesize(new Scope(i), AxiomaticDefinitions.ReadAtomic,
    // AxiomaticDefinitions.Ser);
    //  if (solution.history().isPresent()) {
    //    System.out.println(
    //        String.format(
    //            "Found history after %d seconds and %d candidates:\n%s",
    //            solution.time_ms(), solution.candidates(), solution.history().get()));
    //  } else {
    //    System.out.println(
    //        String.format(
    //            "Failed to find a history after %d seconds and %d candidates.",
    //            solution.time_ms(), solution.candidates()));
    //  }
    // }
  }
}
