package skadistats.clarity.util;

import skadistats.clarity.model.FieldPath;

public class FieldPathUtil {

    public static <T extends FieldPath>  int compare(T fp1, T fp2) {
        return ((Comparable<T>) fp1).compareTo(fp2);
    }

}
