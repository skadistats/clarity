package skadistats.clarity.processor.runner;

public interface Runner<I> {

    Context getContext();
    int getTick();

    Runner runWith(I input, Object...processors);


}
