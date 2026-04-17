package skadistats.clarity.model;

import skadistats.clarity.model.s1.S1FieldPath;
import skadistats.clarity.model.s2.S2FieldPath;

public sealed interface FieldPath permits S1FieldPath, S2FieldPath {
}
