package skadistats.clarity.model.s2.field;

import skadistats.clarity.decoder.unpacker.Unpacker;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.s2.Serializer;

import java.util.List;

public abstract class Field {

    protected final FieldProperties properties;

    public Field(FieldProperties properties) {
        this.properties = properties;
    }

    public abstract Object getInitialState();
    public abstract void accumulateName(List<String> parts, FieldPath fp, int pos);
    public abstract Unpacker queryUnpacker(FieldPath fp, int pos);
    public abstract Field queryField(FieldPath fp, int pos);
    public abstract FieldType queryType(FieldPath fp, int pos);
    public abstract void setValueForFieldPath(FieldPath fp, Object[] state, Object data, int pos);

    protected void assertFieldPathEnd(FieldPath fp, int pos) {
        if (fp.last != pos) {
            throw new RuntimeException(String.format("Assert failed: FieldPath %s not at end at position %s", fp, pos));
        }
    }

    protected void addBasePropertyName(List<String> parts) {
        if (properties.getSendNode() != null) {
            parts.add(properties.getSendNode());
        }
        parts.add(properties.getName());
    }


    public FieldProperties getProperties() {
        return properties;
    }



    public FieldType getType() {
        return properties.getType();
    }

    public String getSendNode() {
        return properties.getSendNode();
    }

    public Integer getBitCount() {
        return properties.getBitCount();
    }

    public String getName() {
        return properties.getName();
    }

    public int getBitCountOrDefault(int defaultValue) {
        return properties.getBitCountOrDefault(defaultValue);
    }

    public Float getLowValue() {
        return properties.getLowValue();
    }

    public int getEncodeFlagsOrDefault(int defaultValue) {
        return properties.getEncodeFlagsOrDefault(defaultValue);
    }

    public float getHighValueOrDefault(float defaultValue) {
        return properties.getHighValueOrDefault(defaultValue);
    }

    public Float getHighValue() {
        return properties.getHighValue();
    }

    public Serializer getSerializer() {
        return properties.getSerializer();
    }

    public String getEncoder() {
        return properties.getEncoder();
    }

    public float getLowValueOrDefault(float defaultValue) {
        return properties.getLowValueOrDefault(defaultValue);
    }

    public Integer getEncodeFlags() {
        return properties.getEncodeFlags();
    }

}
