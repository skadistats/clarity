package clarity.match;

import clarity.iterator.BidiIterator;
import clarity.parser.Peek;
import clarity.parser.ReplayIndex;

public class Match {

    private final DTClassCollection dtClasses = new DTClassCollection();
    private final StringTableCollection stringTables = new StringTableCollection();
    private final EntityCollection entityCollection = new EntityCollection();
    
    public Match(ReplayIndex idx) {
        for (BidiIterator<Peek> i = idx.prologueIterator(); i.hasNext();) {
            Peek p = i.next();
            p.apply(this);
        }
    }

    public DTClassCollection getDtClasses() {
        return dtClasses;
    }

    public StringTableCollection getStringTables() {
        return stringTables;
    }

    public EntityCollection getEntityCollection() {
        return entityCollection;
    }

}
