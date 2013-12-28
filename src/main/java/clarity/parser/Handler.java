package clarity.parser;

import clarity.match.Match;

public interface Handler<T> {

    void apply(int peekTick, T message, Match match);
}
