package skadistats.clarity.io.s2;

public class SerializerId {

    private final String name;
    private final int version;

    public SerializerId(String name, int version) {
        this.name = name;
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        var that = (SerializerId) o;

        if (version != that.version) return false;
        return name.equals(that.name);

    }

    @Override
    public int hashCode() {
        var result = name.hashCode();
        result = 31 * result + version;
        return result;
    }

    @Override
    public String toString() {
        final var sb = new StringBuilder(name);
        sb.append('(');
        sb.append(version);
        sb.append(')');
        return sb.toString();
    }

}
