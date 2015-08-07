package skadistats.clarity.decoder;

import skadistats.clarity.model.*;
import skadistats.clarity.processor.sendtables.DTClasses;

import java.util.*;

public class SendTableFlattener {

    private final DTClasses lookup;
    private final SendTable descendant;
    private final Set<SendTableExclusion> exclusions;
    private final List<ReceiveProp> receiveProps;

    public SendTableFlattener(DTClasses lookup, SendTable descendant) {
        this.lookup = lookup;
        this.descendant = descendant;
        this.exclusions = new HashSet<>();
        this.receiveProps = new ArrayList<>(1024);
    }

    private void aggregateExclusions(SendTable table) {
        for (SendProp sp : table.getSendProps()) {
            if ((sp.getFlags() & PropFlag.EXCLUDE) != 0) {
                exclusions.add(sp.getExcludeIdentifier());
            } else if (sp.getType() == PropType.DATATABLE) {
                aggregateExclusions(lookup.sendTableForDtName(sp.getDtName()));
            }
        }
    }

    private void gather(SendTable ancestor, List<SendProp> accumulator, StringBuilder nameBuf) {
        gatherCollapsible(ancestor, accumulator, nameBuf);
        int l = nameBuf.length();
        for (SendProp sp : accumulator) {
            nameBuf.append(sp.getVarName());
            receiveProps.add(new ReceiveProp(sp, nameBuf.toString()));
            nameBuf.setLength(l);
        }
    }

    private void gatherCollapsible(SendTable ancestor, List<SendProp> accumulator, StringBuilder nameBuf) {
        for (SendProp sp : ancestor.getSendProps()) {
            if (((PropFlag.EXCLUDE | PropFlag.INSIDE_ARRAY) & sp.getFlags()) != 0) {
                continue;
            }
            if (exclusions.contains(new SendTableExclusion(ancestor.getNetTableName(), sp.getVarName()))) {
                continue;
            }
            if (sp.getType() == PropType.DATATABLE) {
                if ((sp.getFlags() & PropFlag.COLLAPSIBLE) != 0) {
                    gatherCollapsible(lookup.sendTableForDtName(sp.getDtName()), accumulator, nameBuf);
                } else {
                    int l = nameBuf.length();
                    nameBuf.append(sp.getVarName());
                    nameBuf.append('.');
                    gather(lookup.sendTableForDtName(sp.getDtName()), new LinkedList<SendProp>(), nameBuf);
                    nameBuf.setLength(l);
                }
            } else {
                accumulator.add(sp);
            }
        }
    }

    private List<ReceiveProp> sort() {
        List<ReceiveProp> sorted = new ArrayList<>(receiveProps);
        Set<Integer> priorities = new TreeSet<>();
        priorities.add(64);
        for (ReceiveProp rp : sorted) {
            priorities.add(rp.getSendProp().getPriority());
        }
        int offset = 0;

        for (Integer priority : priorities) {
            int hole = offset;
            int cursor = offset;
            while (cursor < sorted.size()) {
                ReceiveProp rp = sorted.get(cursor);
                SendProp sp = rp.getSendProp();
                boolean changesOften = (sp.getFlags() & PropFlag.CHANGES_OFTEN) != 0 && priority == 64;
                if (changesOften || sp.getPriority() == priority) {
                    Collections.swap(sorted, cursor, hole);
                    hole++;
                    offset++;
                }
                cursor++;
            }
        }
        return sorted;
    }

    public List<ReceiveProp> flatten() {
        aggregateExclusions(descendant);
        gather(descendant, new LinkedList<SendProp>(), new StringBuilder());
        return sort();
    }
}