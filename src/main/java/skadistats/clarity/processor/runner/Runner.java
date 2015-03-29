package skadistats.clarity.processor.runner;

public interface Runner<T extends Runner> {

    Context getContext();
    int getTick();

    T runWith(Object... processors);


}
