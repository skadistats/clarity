package skadistats.clarity.model.s1;

public class SendTableExclusion {

    private final String dtName;
    private final String varName;
    
    public SendTableExclusion(String dtName, String varName) {
        this.dtName = dtName;
        this.varName = varName;
    }

    public String getDtName() {
        return dtName;
    }

    public String getVarName() {
        return varName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((dtName == null) ? 0 : dtName.hashCode());
        result = prime * result + ((varName == null) ? 0 : varName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SendTableExclusion other = (SendTableExclusion) obj;
        if (dtName == null) {
            if (other.dtName != null)
                return false;
        } else if (!dtName.equals(other.dtName))
            return false;
        if (varName == null) {
            if (other.varName != null)
                return false;
        } else if (!varName.equals(other.varName))
            return false;
        return true;
    }
    
}
