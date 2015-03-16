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
        this.exclusions = aggregateExclusions(descendant);
        this.receiveProps = new LinkedList<ReceiveProp>();
        this.nameBuf = new StringBuffer();
    }

    private Set<SendTableExclusion> aggregateExclusions(SendTable table) {
        Set<SendTableExclusion> result = table.getAllExclusions();
        for (SendProp sp : table.getAllRelations()) {
            result.addAll(aggregateExclusions(lookup.sendTableForDtName(sp.getDtName())));
        }
        return result;
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
        for (SendProp sp : ancestor.getAllNonExclusions()) {
            boolean excluded = exclusions.contains(new SendTableExclusion(ancestor.getNetTableName(), sp.getVarName()));
            boolean ineligible = ((sp.getFlags() & PropFlag.INSIDE_ARRAY) != 0);
            if (excluded || ineligible) {
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
        List<ReceiveProp> sorted = new ArrayList<ReceiveProp>(receiveProps);
        Set<Integer> priorities = new TreeSet<Integer>();
        priorities.add(64);
        for (ReceiveProp rp : sorted) {
            priorities.add(rp.getPriority());
        }
        int offset = 0;

        for (Integer priority : priorities) {
            int hole = offset;
            int cursor = offset;
            while (cursor < sorted.size()) {
                ReceiveProp rp = sorted.get(cursor);
                boolean changesOften = (rp.getFlags() & PropFlag.CHANGES_OFTEN) != 0 && priority == 64;
                if (changesOften || rp.getPriority() == priority) {
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