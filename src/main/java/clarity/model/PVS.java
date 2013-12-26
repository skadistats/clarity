package clarity.model;

public enum PVS {

    PRESERVE(0),
    ENTER(1),
    LEAVE(2),
    LEAVE_AND_DELETE(3);

    private final int code;

    private PVS(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

}
