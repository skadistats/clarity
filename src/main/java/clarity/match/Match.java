package clarity.match;

import clarity.model.DTClassCollection;
import clarity.model.EntityCollection;
import clarity.model.StringTableCollection;

public class Match {

    private DTClassCollection dtClasses = new DTClassCollection();
    private final StringTableCollection stringTables = new StringTableCollection();
    private final EntityCollection entityCollection = new EntityCollection();

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
