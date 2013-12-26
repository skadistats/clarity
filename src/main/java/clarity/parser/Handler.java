package clarity.parser;

import clarity.match.Match;

public interface Handler<T> {

    void apply(T message, Match match);
}
