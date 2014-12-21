package skadistats.clarity.match;

import com.google.common.base.Predicate;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.Handle;
import skadistats.clarity.model.PVS;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class EntityCollection extends BaseCollection<EntityCollection, Entity> {

    @Override
    protected List<Entity> initialValues() {
        List<Entity> result = new ArrayList<Entity>(1 << Handle.INDEX_BITS);
        for (int i = 0; i < 1 << Handle.INDEX_BITS; i++) {
            result.add(null);
        }
        return result;
    }

    public void add(int index, int serial, DTClass dtClass, PVS pvs, Object[] state) {
        values.set(index, new Entity(index, serial, dtClass, pvs, state));
    }

    public Entity getByHandle(int handle) {
        Entity e = getByIndex(Handle.indexForHandle(handle));
        return e == null || e.getSerial() != Handle.serialForHandle(handle) ? null : e;
    }

    public void remove(int index) {
        values.set(index, null);
    }

    public Iterator<Entity> getAllByDtName(final String dtClassName) {
        return iteratorForPredicate(
            new Predicate<Entity>() {
                @Override
                public boolean apply(Entity e) {
                    return dtClassName.equals(e.getDtClass().getDtName());
                }
            });
    }

    public Entity getByDtName(final String dtClassName) {
        Iterator<Entity> iter = getAllByDtName(dtClassName);
        return iter.hasNext() ? iter.next() : null;
    }

}
