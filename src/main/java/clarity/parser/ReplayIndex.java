package clarity.parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import clarity.model.PacketType;

import com.dota2.proto.Demo.CDemoStop;
import com.dota2.proto.Demo.CDemoStringTables;
import com.dota2.proto.Demo.CDemoSyncTick;
import com.dota2.proto.Netmessages.CSVCMsg_PacketEntities;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.google.protobuf.GeneratedMessage;

public class ReplayIndex {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private final List<Peek> index = new ArrayList<Peek>();
    private int lastTick = 0; // the number of ticks in this replay
    private int syncIdx = 0; // the index of the sync packet
    
    public ReplayIndex(DemoInputStreamIterator iter) throws IOException {
        int skew = 0;
        boolean sync = false;
        while (iter.hasNext()) {
            Peek p = iter.next();
            if (sync) {
                skew = p.getTick() - p.getPeekTick();
                if (p.getMessage() instanceof CSVCMsg_PacketEntities) {
                    sync = false;
                }
            }
            p.applySkew(skew);
            lastTick = p.getTick();
            index.add(p);
            if (p.getMessage() instanceof CDemoSyncTick) {
                syncIdx = p.getId();
                sync = true;
            } else if (p.getMessage() instanceof CDemoStop) {
                break;
            }
        }
    }
        
    public Peek get(int i) {
        return index.get(i);
    }

    public int size() {
        return index.size();
    }
    
    public int getLastTick() {
        return lastTick;
    }
    
    private int indexForTick(List<Peek> list, int tick) {
        int a = -1; // lower bound 
        int b = list.size(); // upper bound
        while (a + 1 != b) {
            int  m = (a + b) >>> 1;
            if (list.get(m).getTick() < tick) {
                a = m;
            } else {
                b = m;
            }
        }
        return b;
    }

    public int nextIndexOf(Class<? extends GeneratedMessage> clazz, int pos) {
        for (int i = pos; i < index.size(); i++) {
            if (clazz.isAssignableFrom(index.get(i).getMessage().getClass())) {
                return i;
            }
        }
        return -1;
    }
    
    private List<Peek> prologueList() {
        return index.subList(0, syncIdx + 1);
    }

    private List<Peek> matchList() {
        return index.subList(syncIdx + 1, index.size());
    }
    
    public Iterator<Peek> prologueIterator() {
        return prologueList().iterator();
    }

    public Iterator<Peek> matchIterator() {
        return matchList().iterator();
    }
    
    public Iterator<Peek> matchIteratorForTicks(final int startTick, final int endTick, final PacketType packetType) {
        List<Peek> match = matchList();
        return 
            Iterators.filter(
                match.subList(indexForTick(match, startTick), indexForTick(match, endTick + 1)).iterator(), 
                packetType.getPredicate()
            );
    }
    
    public Iterator<Peek> filteringIteratorForTicks(final int startTick, final int endTick, final PacketType packetType, final Class<? extends GeneratedMessage> clazz) {
        return Iterators.filter(
            matchIteratorForTicks(startTick, endTick, packetType),
            new Predicate<Peek>() {
                @Override
                public boolean apply(Peek p) {
                    return clazz.isAssignableFrom(p.getMessage().getClass());
                }
            }
        );
    }
    
    public Iterator<Peek> skipToIterator(final int tick) {
        final Peek p = Iterators.getLast(filteringIteratorForTicks(0, tick, PacketType.FULL, CDemoStringTables.class)); 
        return Iterators.concat(
            filteringIteratorForTicks(0, p.getTick() - 1, PacketType.FULL, CDemoStringTables.class),
            matchIteratorForTicks(p.getTick(), p.getTick(), PacketType.FULL),
            matchIteratorForTicks(p.getTick() + 1, tick, PacketType.DELTA)
        );
    }

}
