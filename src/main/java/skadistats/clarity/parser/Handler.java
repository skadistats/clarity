package skadistats.clarity.parser;

import skadistats.clarity.match.Match;

public interface Handler<T> {

    void apply(int peekTick, T message, Match match);
}
