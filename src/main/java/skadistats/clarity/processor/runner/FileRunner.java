package skadistats.clarity.processor.runner;

import skadistats.clarity.source.Source;

public interface FileRunner<T extends FileRunner<? super T>> extends Runner<T> {

    Source getSource();

}
