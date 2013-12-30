package clarity.model;

import clarity.parser.Peek;

import com.google.common.base.Predicate;

public enum PacketType {
    
    DELTA(new Predicate<Peek>() {
        @Override
        public boolean apply(Peek peek) {
            return !peek.isFull();
        }
    }),
    FULL(new Predicate<Peek>() {
        @Override
        public boolean apply(Peek peek) {
            return peek.isFull();
        }
    }),
    BOTH(new Predicate<Peek>() {
        @Override
        public boolean apply(Peek peek) {
            return true;
        }
    });
    
    private final Predicate<Peek> predicate;

    private PacketType(Predicate<Peek> predicate) {
        this.predicate = predicate;
    }

    public Predicate<Peek> getPredicate() {
        return predicate;
    }

}
