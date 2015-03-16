package skadistats.clarity.model;


public interface Prop {

    SendTableExclusion getExcludeIdentifier();

    PropType getType();

    String getSrc();

    String getDtName();

    String getVarName();

    int getPriority();

    float getLowValue();

    float getHighValue();

    int getNumBits();

    int getNumElements();

    SendProp getTemplate();
    
    int getFlags();

}
