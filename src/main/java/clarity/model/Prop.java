package clarity.model;

import org.javatuples.Pair;

public interface Prop {

    boolean isFlagSet(PropFlag flag);

    Pair<String, String> getExcludeIdentifier();

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
