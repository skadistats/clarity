package skadistats.clarity.platform.buffer;

public interface Buffer {

    interface B32 {
        int get(int n);
    }

    interface B64 {
        long get(int n);
    }

}
