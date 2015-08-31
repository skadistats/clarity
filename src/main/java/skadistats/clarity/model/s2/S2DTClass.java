package skadistats.clarity.model.s2;

import skadistats.clarity.model.DTClass;

public class S2DTClass implements DTClass {


    private final Serializer serializer;
    private int classId = -1;

    public S2DTClass(Serializer serializer) {
        this.serializer = serializer;
    }

    @Override
    public int getClassId() {
        return classId;
    }

    @Override
    public void setClassId(int classId) {
        this.classId = classId;
    }

    @Override
    public String getDtName() {
        return serializer.getId().getName();
    }

    @Override
    public Integer getPropertyIndex(String property) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object[] getEmptyStateArray() {
        return serializer.getEmptyStateArray();
    }

    public Field getNameForSerializer(StringBuilder name, Serializer s, FieldPath fp, int i) {
        Field f = s.getFields()[fp.path[i]];
        if (f == null) {
            throw new RuntimeException("wtf1");
        }

        if (name.length() != 0) {
            name.append('.');
        }
        if (!"(root)".equals(f.getSendNode())) {
            name.append(f.getSendNode());
            name.append('.');
        }
        name.append(f.getName());

        if (fp.last == i) {
            return f;
        }

        if (f.getType().isFixedArray() || f.getType().isVariableArray()) {
            // array
            name.append('[');
            name.append(fp.path[++i]);
            name.append(']');
            if (fp.last == i) {
                return f;
            }
        }

        if (f.getSerializer() != null) {
            return getNameForSerializer(name, f.getSerializer(), fp, ++i);
        }

        return f;
    }


    public String getNameForFieldPath(FieldPath fp) {
        StringBuilder name = new StringBuilder();
        getNameForSerializer(name, serializer, fp, 0);
        return name.toString();
    }

    public Field getFieldForFieldPath(FieldPath fp) {
        StringBuilder name = new StringBuilder();
        return getNameForSerializer(name, serializer, fp, 0);
    }
}
