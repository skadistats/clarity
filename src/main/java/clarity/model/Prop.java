package clarity.model;


public interface Prop {

    boolean isFlagSet(PropFlag flag);

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
