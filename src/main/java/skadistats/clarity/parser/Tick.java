package skadistats.clarity.parser;

import java.util.List;

import skadistats.clarity.match.Match;

public class Tick {
    
    private final List<Peek> peeks;
    
    public Tick(List<Peek> peeks) {
        this.peeks = peeks;
    }
    
    public void apply(Match match) {
        for (Peek p : peeks) {
            p.apply(match);
        }
    }
    
}
