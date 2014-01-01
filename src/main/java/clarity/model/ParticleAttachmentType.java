package clarity.model;

public enum ParticleAttachmentType {

    ABSORIGIN,              // Create at absorigin, but don't follow
    ABSORIGIN_FOLLOW,       // Create at absorigin, and update to follow the entity
    CUSTOMORIGIN,           // Create at a custom origin, but don't follow
    CUSTOMORIGIN_FOLLOW,    // Create at a custom origin, follow relative position to specified entity
    POINT,                  // Create on attachment point, but don't follow
    POINT_FOLLOW,           // Create on attachment point, and update to follow the entity
    EYES_FOLLOW,            // Create on eyes of the attached entity, and update to follow the entity
    WORLDORIGIN,            // Used for control points that don't attach to an entity
    MAX;
    
    
    public static ParticleAttachmentType forId(int id) {
        for (ParticleAttachmentType t : values()) {
            if (t.ordinal() == id) {
                return t;
            }
        }
        return null;
    }
}
