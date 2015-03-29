package skadistats.clarity.processor.runner;

public interface Runner {

    Context getContext();
    int getTick();

    Runner runWith(Object... processors);


}
