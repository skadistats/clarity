package skadistats.clarity.bench;

import skadistats.clarity.bench.trace.BirthMaterializer;
import skadistats.clarity.bench.trace.MutationTraceCapture;
import skadistats.clarity.model.state.S2EntityStateType;

import java.nio.file.Files;
import java.nio.file.Path;

public class SmokeTraceMain {

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("usage: SmokeTraceMain <replay>");
            System.exit(2);
        }
        var replay = Path.of(args[0]);
        if (!Files.isRegularFile(replay)) {
            System.err.println("replay not found: " + replay);
            System.exit(2);
        }

        System.out.println("capturing trace from " + replay);
        var t0 = System.nanoTime();
        var trace = MutationTraceCapture.capture(replay);
        var captureMs = (System.nanoTime() - t0) / 1_000_000;
        System.out.printf("  captured %d births, %d update mutations in %d ms%n",
            trace.births().size(), trace.updateMutations().length, captureMs);

        for (var impl : S2EntityStateType.values()) {
            var t1 = System.nanoTime();
            var states = BirthMaterializer.materialize(trace, impl);
            var matMs = (System.nanoTime() - t1) / 1_000_000;

            var t2 = System.nanoTime();
            boolean acc = false;
            for (var m : trace.updateMutations()) {
                acc ^= states[m.stateId()].applyMutation(m.fp(), m.mutation());
            }
            var replayMs = (System.nanoTime() - t2) / 1_000_000;
            System.out.printf("  %-13s materialize=%d ms, replay=%d ms, acc=%s%n",
                impl, matMs, replayMs, acc);
        }
        System.out.println("OK");
    }
}
