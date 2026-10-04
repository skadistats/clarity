package skadistats.clarity.model;

import skadistats.clarity.state.EntityState;

/**
 * A live entity in the replay: an instance of a {@link DTClass} identified by
 * index and serial, carrying its current property values in an
 * {@link EntityState}.
 *
 * <p>Instances are created and updated by the parser; the same {@code Entity}
 * object stays valid for as long as the entity exists. Properties are read by
 * name or by {@link FieldPath}; resolve the {@link FieldPath} once with
 * {@link #getFieldPathForName} when reading repeatedly.
 */
public class Entity {

    private final int index;
    private final int serial;
    private final int handle;
    private final DTClass dtClass;

    private boolean existent;
    private boolean active;
    private int spawnGroupHandle;
    private EntityState state;

    /** Created by the parser; user code receives entities from {@link skadistats.clarity.processor.entities.Entities} and entity events. */
    public Entity(int index, int serial, int handle, DTClass dtClass) {
        this.index = index;
        this.serial = serial;
        this.handle = handle;
        this.dtClass = dtClass;
    }

    /**
     * @return the entity's slot index, unique among entities existing at the same time
     */
    public int getIndex() {
        return index;
    }

    /**
     * @return the serial number distinguishing successive entities that occupied the same index
     */
    public int getSerial() {
        return serial;
    }

    /**
     * @return the entity handle, combining index and serial as defined by the
     * replay's {@link skadistats.clarity.engine.EngineType#handleForIndexAndSerial}
     */
    public int getHandle() {
        return handle;
    }

    /**
     * @return the entity's class
     */
    public DTClass getDtClass() {
        return dtClass;
    }

    /**
     * Whether the entity has been created and not yet deleted. Set when the
     * entity is created, cleared when it is deleted.
     *
     * @return true while the entity exists in the replay's entity list
     * @see #isActive()
     */
    public boolean isExistent() {
        return existent;
    }

    /** Internal to the parser. */
    public void setExistent(boolean existent) {
        this.existent = existent;
    }

    /**
     * Whether the entity is currently active, i.e. has entered and not left
     * the client's view. Set when the entity is created or re-created and when
     * it enters again, cleared when it leaves (as signalled by the packet
     * entities PVS bits). An entity can exist without being active.
     *
     * @return true while the entity is active
     * @see #isExistent()
     */
    public boolean isActive() {
        return active;
    }

    /** Internal to the parser. */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Get the spawn group handle this entity belongs to. Source 2 only;
     * always 0 for Source 1 engines.
     *
     * <p>Spawn groups are the Source 2 mechanism for batch-loading and
     * unloading entities. Handle 0 is the system spawn group containing
     * always-resident resource entities (CWorld, player controllers, team,
     * game rules, observer pawns). Higher handles correspond to map sections
     * loaded later (main map, sub-areas, mod content).
     */
    public int getSpawnGroupHandle() {
        return spawnGroupHandle;
    }

    /** Internal to the parser. */
    public void setSpawnGroupHandle(int spawnGroupHandle) {
        this.spawnGroupHandle = spawnGroupHandle;
    }

    /**
     * @return the entity's current property state
     */
    public EntityState getState() {
        return state;
    }

    /** Internal to the parser. */
    public void setState(EntityState state) {
        this.state = state;
    }

    /**
     * Resolves a {@link FieldPath} to the property name.
     *
     * @return the property name; for Source 2 entities {@code null} if the path
     * does not resolve against the entity's current state
     */
    public String getNameForFieldPath(FieldPath fp) {
        return DTClass.getNameForFieldPath(dtClass, state, fp);
    }

    /**
     * Resolves a property name to its {@link FieldPath}. For Source 2 entities
     * the result depends on the entity's current state (vector sizes, pointer
     * targets).
     *
     * @return the field path, or {@code null} if the entity has no such property
     */
    public FieldPath getFieldPathForName(String property) {
        return DTClass.getFieldPathForName(dtClass, state, property);
    }

    /**
     * Check if this entity contains the given property.
     *
     * @param property Name of the property
     * @return True, if and only if the given property is present in this entity
     */
    public boolean hasProperty(String property) {
        return getFieldPathForName(property) != null;
    }

    /**
     * Check if this entity contains all of the given properties.
     *
     * @param properties Names of the properties
     * @return True, if and only if the given properties are present in this entity
     */
    public boolean hasProperties(String... properties) {
        for (var property : properties) {
            if (!hasProperty(property)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Reads a property by name, boxing primitives. The value is cast
     * unchecked to {@code T}; a wrong {@code T} fails with a
     * {@link ClassCastException} at the call site. Prefer the primitive
     * getters ({@link #getInt(String)} etc.) where the type is known.
     *
     * @throws IllegalArgumentException if the entity has no such property
     */
    @SuppressWarnings("unchecked")
    public <T> T getProperty(String property) {
        var fp = getFieldPathForName(property);
        if (fp == null) {
            throw new IllegalArgumentException(String.format("property %s not found on entity of class %s", property, getDtClass().getDtName()));
        }
        return getPropertyForFieldPath(fp);
    }

    /**
     * Like {@link #getProperty(String)}, for an already resolved
     * {@link FieldPath}. The value is cast unchecked to {@code T}.
     */
    public <T> T getPropertyForFieldPath(FieldPath fp) {
        return EntityState.getValueForFieldPath(getState(), fp);
    }

    /**
     * Reads an {@code int} property without boxing; see
     * {@link EntityState#getInt(EntityState, FieldPath)} for the default-value
     * contract. The {@code long}, {@code float} and {@code Object} variants
     * below follow the same pattern.
     */
    public int getInt(FieldPath fp) {
        return EntityState.getInt(getState(), fp);
    }

    /**
     * Like {@link #getInt(FieldPath)}, resolving the property by name. Resolve
     * the {@link FieldPath} once with {@link #getFieldPathForName} when
     * reading repeatedly.
     *
     * @throws IllegalArgumentException if the entity has no such property
     */
    public int getInt(String property) {
        var fp = getFieldPathForName(property);
        if (fp == null) throw new IllegalArgumentException("property " + property + " not found on entity of class " + getDtClass().getDtName());
        return EntityState.getInt(getState(), fp);
    }

    /** Like {@link #getInt(FieldPath)}, for {@code long}-typed properties. */
    public long getLong(FieldPath fp) {
        return EntityState.getLong(getState(), fp);
    }

    /**
     * Like {@link #getLong(FieldPath)}, resolving the property by name.
     *
     * @throws IllegalArgumentException if the entity has no such property
     */
    public long getLong(String property) {
        var fp = getFieldPathForName(property);
        if (fp == null) throw new IllegalArgumentException("property " + property + " not found on entity of class " + getDtClass().getDtName());
        return EntityState.getLong(getState(), fp);
    }

    /** Like {@link #getInt(FieldPath)}, for {@code float}-typed properties. */
    public float getFloat(FieldPath fp) {
        return EntityState.getFloat(getState(), fp);
    }

    /**
     * Like {@link #getFloat(FieldPath)}, resolving the property by name.
     *
     * @throws IllegalArgumentException if the entity has no such property
     */
    public float getFloat(String property) {
        var fp = getFieldPathForName(property);
        if (fp == null) throw new IllegalArgumentException("property " + property + " not found on entity of class " + getDtClass().getDtName());
        return EntityState.getFloat(getState(), fp);
    }

    /** See {@link EntityState#getObject(EntityState, FieldPath)}. */
    public Object getObject(FieldPath fp) {
        return EntityState.getObject(getState(), fp);
    }

    /**
     * Like {@link #getObject(FieldPath)}, resolving the property by name.
     *
     * @throws IllegalArgumentException if the entity has no such property
     */
    public Object getObject(String property) {
        var fp = getFieldPathForName(property);
        if (fp == null) throw new IllegalArgumentException("property " + property + " not found on entity of class " + getDtClass().getDtName());
        return EntityState.getObject(getState(), fp);
    }

    /**
     * @return a table dump of the entity's index, serial, class and all property values
     */
    @Override
    public String toString() {
        var title = "idx: " + getIndex() + ", serial: " + getSerial() + ", class: " + getDtClass().getDtName();
        return getState().dump(title, this::getNameForFieldPath);
    }

    /**
     * @return a unique id combining class id and handle, see {@link #uid(int, int)}
     */
    public long getUid() {
        return uid(dtClass.getClassId(), handle);
    }

    /**
     * Computes the value {@link #getUid()} returns for an entity.
     *
     * @return {@code dtClassId} in the upper 32 bits, {@code handle} in the lower 32
     */
    public static long uid(int dtClassId, int handle) {
        return (long) dtClassId << 32 | handle;
    }

}
