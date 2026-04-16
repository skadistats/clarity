package skadistats.clarity.bench;

import skadistats.clarity.event.Insert;
import skadistats.clarity.io.MutationListener;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.Vector;
import skadistats.clarity.model.state.EntityState;
import skadistats.clarity.model.state.S1EntityStateType;
import skadistats.clarity.model.state.StateMutation;
import skadistats.clarity.processor.entities.Entities;
import skadistats.clarity.processor.entities.OnEntityCreated;
import skadistats.clarity.processor.entities.OnEntityDeleted;
import skadistats.clarity.processor.entities.OnEntityUpdated;
import skadistats.clarity.processor.entities.UsesEntities;
import skadistats.clarity.processor.runner.OnInit;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.MappedFileSource;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class S1ParityCaptureMain {

    @UsesEntities
    public static class CaptureProcessor {
        private final BufferedWriter out;
        private final boolean forceMaterialize;
        @Insert
        private Entities entities;

        public CaptureProcessor(BufferedWriter out, boolean forceMaterialize) {
            this.out = out;
            this.forceMaterialize = forceMaterialize;
        }

        @OnInit
        public void onInit() {
            if (forceMaterialize) {
                entities.setMutationListener(new MutationListener() {
                    @Override public void onBirthEmpty(EntityState s, DTClass c) {}
                    @Override public void onBirthCopy(EntityState n, EntityState s) {}
                    @Override public void onSetupMutation(EntityState t, FieldPath fp, StateMutation m) {}
                    @Override public void onUpdateMutation(EntityState t, FieldPath fp, StateMutation m) {}
                });
            }
        }

        @OnEntityCreated
        public void onCreated(Entity e) throws IOException {
            out.write("C\t");
            out.write(Integer.toString(e.getIndex()));
            out.write('\t');
            out.write(e.getDtClass().getDtName());
            out.write('\n');
        }

        @OnEntityDeleted
        public void onDeleted(Entity e) throws IOException {
            out.write("D\t");
            out.write(Integer.toString(e.getIndex()));
            out.write('\t');
            out.write(e.getDtClass().getDtName());
            out.write('\n');
        }

        @OnEntityUpdated
        public void onUpdated(Entity e, FieldPath[] fps, int n) throws IOException {
            for (var i = 0; i < n; i++) {
                var fp = fps[i];
                out.write("U\t");
                out.write(Integer.toString(e.getIndex()));
                out.write('\t');
                out.write(fp.toString());
                out.write('\t');
                var name = e.getNameForFieldPath(fp);
                out.write(name != null ? name : "<no-name>");
                out.write('\t');
                out.write(repr(e.getPropertyForFieldPath(fp)));
                out.write('\n');
            }
        }

        private static String repr(Object v) {
            if (v == null) return "null";
            if (v instanceof Object[] arr) return Arrays.toString(arr);
            if (v instanceof Vector vec) return vec.toString();
            return v.toString();
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2 || args.length > 4) {
            System.err.println("usage: S1ParityCaptureMain <replay> <out.txt> [OBJECT_ARRAY|FLAT] [--materialize]");
            System.exit(2);
        }
        var replay = Path.of(args[0]);
        var outFile = Path.of(args[1]);
        var stateType = S1EntityStateType.OBJECT_ARRAY;
        var forceMaterialize = false;
        for (var i = 2; i < args.length; i++) {
            if (args[i].equals("--materialize")) {
                forceMaterialize = true;
            } else {
                stateType = S1EntityStateType.valueOf(args[i]);
            }
        }
        if (!Files.isRegularFile(replay)) {
            System.err.println("replay not found: " + replay);
            System.exit(2);
        }
        Files.createDirectories(outFile.toAbsolutePath().getParent());

        var t0 = System.nanoTime();
        try (var src = new MappedFileSource(replay);
             var out = Files.newBufferedWriter(outFile, StandardCharsets.UTF_8)) {
            var runner = new SimpleRunner(src);
            runner.withS1EntityState(stateType);
            runner.runWith(new CaptureProcessor(out, forceMaterialize));
        }
        var ms = (System.nanoTime() - t0) / 1_000_000;
        System.out.printf("OK %s [%s%s] -> %s in %d ms%n", replay, stateType,
            forceMaterialize ? " materialize=true" : "", outFile, ms);
    }
}
