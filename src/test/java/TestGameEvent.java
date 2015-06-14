import skadistats.clarity.model.GameEvent;
import skadistats.clarity.processor.gameevents.OnGameEvent;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.MappedFileSource;
import org.junit.Assert;
import org.junit.Test;

public class TestGameEvent {

    @OnGameEvent
    public void onGameEvent(Context ctx, GameEvent event) {
       System.out.println(event.toString());
    }
    
    @Test
    public void test() throws Exception {
        long tStart = System.currentTimeMillis();
        new SimpleRunner(new MappedFileSource("replays/1436943655_684.dem")).runWith(this);
        long tMatch = System.currentTimeMillis() - tStart;
        System.out.format("total time taken: %ss\n", (tMatch) / 1000.0);
    }

}