package skadistats.clarity.processor.entities;

import com.dota2.proto.Netmessages;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterators;
import skadistats.clarity.decoder.PacketEntitiesDecoder;
import skadistats.clarity.event.Provides;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.Handle;
import skadistats.clarity.model.PVS;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.sendtables.DTClasses;
import skadistats.clarity.processor.sendtables.UsesDTClasses;
import skadistats.clarity.processor.stringtables.StringTables;
import skadistats.clarity.processor.stringtables.UsesStringTable;

import java.util.Iterator;

@Provides({UsesEntities.class})
@UsesDTClasses
@UsesStringTable("instancebaseline")
public class Entities {

    private final Entity[] entities = new Entity[1 << Handle.INDEX_BITS];

    @OnMessage(Netmessages.CSVCMsg_PacketEntities.class)
    public void onPacketEntities(Context ctx, Netmessages.CSVCMsg_PacketEntities message) {
        new PacketEntitiesDecoder(
            message.getEntityData().toByteArray(),
            message.getUpdatedEntries(),
            message.getIsDelta(),
            ctx.getProcessor(DTClasses.class),
            ctx.getProcessor(StringTables.class).forName("instancebaseline")
        ).decodeAndApply(this);
    }

    public void add(int index, int serial, DTClass dtClass, PVS pvs, Object[] state) {
        entities[index] = new Entity(index, serial, dtClass, pvs, state);
    }

    public Entity getByIndex(int index) {
        return entities[index];
    }

    public Entity getByHandle(int handle) {
        Entity e = entities[Handle.indexForHandle(handle)];
        return e == null || e.getSerial() != Handle.serialForHandle(handle) ? null : e;
    }

    public void remove(int index) {
        entities[index] = null;
    }

    public Iterator<Entity> getAllByPredicate(Predicate<Entity> predicate) {
        return Iterators.filter(
            Iterators.forArray(entities),
            Predicates.and(
                Predicates.notNull(),
                predicate
            ));
    }

    public Entity getByPredicate(Predicate<Entity> predicate) {
        Iterator<Entity> iter = getAllByPredicate(predicate);
        return iter.hasNext() ? iter.next() : null;
    }

    public Iterator<Entity> getAllByDtName(final String dtClassName) {
        return getAllByPredicate(
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
