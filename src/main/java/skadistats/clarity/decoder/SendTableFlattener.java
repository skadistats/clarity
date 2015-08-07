package skadistats.clarity.decoder;

import skadistats.clarity.model.*;
import skadistats.clarity.processor.sendtables.DTClasses;

import java.util.*;

public class SendTableFlattener {

    private final DTClasses lookup;
    private final SendTable descendant;
    private final Set<SendTableExclusion> exclusions;
    private final List<ReceiveProp> receiveProps;
    private final StringBuffer nameBuf;

    public SendTableFlattener(DTClasses lookup, SendTable descendant) {
        this.lookup = lookup;
        this.descendant = descendant;
        this.exclusions = new HashSet<>();
        this.receiveProps = new ArrayList<>(1024);
        this.nameBuf = new StringBuffer();
        aggregateExclusions(descendant);
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

    private void _flatten(SendTable ancestor, List<SendProp> accumulator, Deque<String> path, String src) {
        _flattenCollapsible(ancestor, accumulator, path, src);
        nameBuf.setLength(0);
        for (String part : path) {
            nameBuf.append(part);
            nameBuf.append('.');
        }
        int l = nameBuf.length();
        for (SendProp sp : accumulator) {
            nameBuf.append(sp.getVarName());
            receiveProps.add(new ReceiveProp(sp, src == null ? sp.getSrc() : src, nameBuf.toString()));
            nameBuf.setLength(l);
        }
    }

    private void _flattenCollapsible(SendTable ancestor, List<SendProp> accumulator, Deque<String> path, String src) {
        for (SendProp sp : ancestor.getSendProps()) {
            if (((PropFlag.EXCLUDE | PropFlag.INSIDE_ARRAY) & sp.getFlags()) != 0) {
                continue;
            }
            if (exclusions.contains(new SendTableExclusion(ancestor.getNetTableName(), sp.getVarName()))) {
                continue;
            }
            if (sp.getType() == PropType.DATATABLE) {
                if ((sp.getFlags() & PropFlag.COLLAPSIBLE) != 0) {
                    _flattenCollapsible(lookup.sendTableForDtName(sp.getDtName()), accumulator, path, src);
                } else {
                    path.offerLast(sp.getVarName());
                    _flatten(lookup.sendTableForDtName(sp.getDtName()), new LinkedList<SendProp>(), path, src == null ? ancestor.getNetTableName() : src);
                    path.removeLast();
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
        _flatten(descendant, new LinkedList<SendProp>(), new LinkedList<String>(), null);
        return sort();
    }
}