package skadistats.clarity.model.s2;

import skadistats.clarity.decoder.unpacker.Unpacker;
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

    public Unpacker getUnpackerForFieldPath(FieldPath fp)  {
        Serializer s = serializer;
        Field f = null;
        Unpacker u = null;
        boolean generic = false;
        boolean fixed = false;
        for (int i = 0; i <= fp.last; i++) {
            if (generic || fixed) {
                if (generic) {
                    u = f.getElementUnpacker();
                }
                generic = false;
                fixed = false;
                continue;
            }
            if (f != null) {
                s = f.getSerializer();
            }
            f = s.getFields()[fp.path[i]];
            u = f.getBaseUnpacker();
            if (f.getType().isGenericArray()) {
                generic = true;
            } else if (f.getType().isFixedArray()) {
                fixed = true;
            }
        }
        return u;
    }

    public FieldType getTypeForFieldPath(FieldPath fp)  {
        Serializer s = serializer;
        Field f = null;
        FieldType t = null;
        boolean generic = false;
        boolean fixed = false;
        for (int i = 0; i <= fp.last; i++) {
            if (generic || fixed) {
                if (generic) {
                    t = f.getType().getGenericType();
                }
                generic = false;
                fixed = false;
                continue;
            }
            if (f != null) {
                s = f.getSerializer();
            }
            f = s.getFields()[fp.path[i]];
            t = f.getType();
            if (f.getType().isGenericArray()) {
                generic = true;
            } else if (f.getType().isFixedArray()) {
                fixed = true;
            }
        }
        return t;
    }

    public Field getFieldForFieldPath(FieldPath fp)  {
        Serializer s = serializer;
        Field f = null;
        boolean array = false;
        for (int i = 0; i <= fp.last; i++) {
            if (array) {
                array = false;
                continue;
            }
            if (f != null) {
                s = f.getSerializer();
            }
            f = s.getFields()[fp.path[i]];
            if (f.getType().isGenericArray() || f.getType().isFixedArray()) {
                array = true;
            }
        }
        return f;
    }

    public String getNameForFieldPath(FieldPath fp)  {
        StringBuilder name = new StringBuilder();
        Serializer s = serializer;
        Field f = null;
        boolean array = false;
        for (int i = 0; i <= fp.last; i++) {
            if (array) {
                name.append('[');
                name.append(fp.path[i]);
                name.append(']');
                array = false;
                continue;
            }
            if (f != null) {
                s = f.getSerializer();
            }
            f = s.getFields()[fp.path[i]];
            if (name.length() != 0) {
                name.append('.');
            }
            if (!"(root)".equals(f.getSendNode())) {
                name.append(f.getSendNode());
                name.append('.');
            }
            name.append(f.getName());
            if (f.getType().isGenericArray() || f.getType().isFixedArray()) {
                array = true;
            }
        }
        return name.toString();
    }

}
