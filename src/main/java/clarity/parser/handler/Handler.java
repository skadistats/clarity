package clarity.parser.handler;

import clarity.match.Match;

public interface Handler<T> {

	void apply(T message, Match match);
}
