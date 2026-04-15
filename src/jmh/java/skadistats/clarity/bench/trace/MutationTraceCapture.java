package skadistats.clarity.bench.trace;

import skadistats.clarity.event.Insert;
import skadistats.clarity.processor.entities.Entities;
import skadistats.clarity.processor.entities.UsesEntities;
import skadistats.clarity.processor.runner.OnInit;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.processor.sendtables.DTClasses;
import skadistats.clarity.processor.sendtables.OnDTClassesComplete;
import skadistats.clarity.source.MappedFileSource;

import java.nio.file.Path;

/**
 * Runs a {@link SimpleRunner} over a replay with a {@link MutationRecorder} attached to the
 * live {@link Entities} processor, returning the captured trace.
 */
public final class MutationTraceCapture {

    private MutationTraceCapture() {
    }

    public static CapturedTrace capture(Path replay) throws Exception {
        var recorder = new MutationRecorder();
        var installer = new RecorderInstaller(recorder);
        try (var src = new MappedFileSource(replay.toString())) {
            var runner = new SimpleRunner(src);
            runner.runWith(installer);
        }
        return recorder.finish(installer.pointerCount);
    }

    @UsesEntities
    public static class RecorderInstaller {
        private final MutationRecorder recorder;
        int pointerCount = -1;

        @Insert
        private Entities entities;
        @Insert
        private DTClasses dtClasses;

        public RecorderInstaller(MutationRecorder recorder) {
            this.recorder = recorder;
        }

        @OnInit
        public void onInit() {
            entities.setMutationListener(recorder);
        }

        @OnDTClassesComplete
        public void onDTClassesComplete() {
            pointerCount = dtClasses.getPointerCount();
        }
    }
}
