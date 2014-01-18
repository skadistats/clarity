package clarity.decoder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import clarity.match.DTClassCollection;
import clarity.model.PropFlag;
import clarity.model.PropType;
import clarity.model.ReceiveProp;
import clarity.model.SendProp;
import clarity.model.SendTable;
import clarity.model.SendTableExclusion;

public class SendTableFlattener {

    private final DTClassCollection lookup;
    private final SendTable descendant;
    private final Set<SendTableExclusion> exclusions;
    private final List<ReceiveProp> receiveProps;

    public SendTableFlattener(DTClassCollection lookup, SendTable descendant) {
        this.lookup = lookup;
        this.descendant = descendant;
        this.exclusions = aggregateExclusions(descendant);
        this.receiveProps = new LinkedList<ReceiveProp>();
    }

    private Set<SendTableExclusion> aggregateExclusions(SendTable table) {
        Set<SendTableExclusion> result = table.getAllExclusions();
        for (SendProp sp : table.getAllRelations()) {
            result.addAll(aggregateExclusions(lookup.sendTableForDtName(sp.getDtName())));
        }
        return result;
    }

    private void _flatten(SendTable ancestor, List<SendProp> accumulator, String proxy) {
        _flattenCollapsible(ancestor, accumulator);
        for (SendProp sp : accumulator) {
            String n = sp.getVarName();
            String s = sp.getSrc();
            if (proxy != null) {
                n = s + "." + n;
                s = proxy;
            }
            receiveProps.add(new ReceiveProp(sp, s, n));
        }
    }

    private void _flattenCollapsible(SendTable ancestor, List<SendProp> accumulator) {
        for (SendProp sp : ancestor.getAllNonExclusions()) {
            boolean excluded = exclusions.contains(new SendTableExclusion(ancestor.getNetTableName(), sp.getVarName()));
            boolean ineligible = (sp.isFlagSet(PropFlag.INSIDE_ARRAY));
            if (excluded || ineligible) {
                continue;
            }
            if (sp.getType() == PropType.DATATABLE) {
                if (sp.isFlagSet(PropFlag.COLLAPSIBLE)) {
                    _flattenCollapsible(lookup.sendTableForDtName(sp.getDtName()), accumulator);
                } else {
                    _flatten(lookup.sendTableForDtName(sp.getDtName()), new LinkedList<SendProp>(), ancestor.getNetTableName());
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
                boolean changesOften = rp.isFlagSet(PropFlag.CHANGES_OFTEN) && priority == 64;
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
        _flatten(descendant, new LinkedList<SendProp>(), null);
        return sort();
    }
}