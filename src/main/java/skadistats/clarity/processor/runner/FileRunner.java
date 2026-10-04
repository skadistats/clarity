package skadistats.clarity.processor.runner;

import skadistats.clarity.source.Source;

/**
 * A {@link Runner} that reads its data from a {@link Source}.
 */
public interface FileRunner extends Runner {

    /**
     * @return the source this runner reads from
     */
    Source getSource();

}
