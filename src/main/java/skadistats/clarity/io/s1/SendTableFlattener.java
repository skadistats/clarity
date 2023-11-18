package skadistats.clarity.io.s1;

import skadistats.clarity.model.s1.PropFlag;
import skadistats.clarity.model.s1.PropType;
import skadistats.clarity.processor.sendtables.DTClasses;

import java.util.*;

public class SendTableFlattener {

    private final DTClasses dtClasses;
    private final SendTable table;
    private final Set<SendTableExclusion> exclusions;
    private final List<ReceiveProp> receiveProps;

    public SendTableFlattener(DTClasses dtClasses, SendTable table) {
        this.dtClasses = dtClasses;
        this.table = table;
        this.exclusions = new HashSet<>();
        this.receiveProps = new ArrayList<>(1024);
    }

    private SendTable sendTableForDtName(String dtName) {
        var dtClass = (S1DTClass) dtClasses.forDtName(dtName);
        return dtClass.getSendTable();
    }

    private void aggregateExclusions(SendTable table) {
        for (var sp : table.getSendProps()) {
            if ((sp.getFlags() & PropFlag.EXCLUDE) != 0) {
                exclusions.add(sp.getExcludeIdentifier());
            } else if (sp.getType() == PropType.DATATABLE) {
                aggregateExclusions(sendTableForDtName(sp.getDtName()));
            }
        }
    }

    private void gather(SendTable ancestor, List<SendProp> accumulator, StringBuilder nameBuf) {
        gatherCollapsible(ancestor, accumulator, nameBuf);
        var l = nameBuf.length();
        for (var sp : accumulator) {
            nameBuf.append(sp.getVarName());
            receiveProps.add(new ReceiveProp(sp, nameBuf.toString()));
            nameBuf.setLength(l);
        }
    }

    private void gatherCollapsible(SendTable ancestor, List<SendProp> accumulator, StringBuilder nameBuf) {
        for (var sp : ancestor.getSendProps()) {
            if (((PropFlag.EXCLUDE | PropFlag.INSIDE_ARRAY) & sp.getFlags()) != 0) {
                continue;
            }
            if (exclusions.contains(new SendTableExclusion(ancestor.getNetTableName(), sp.getVarName()))) {
                continue;
            }
            if (sp.getType() == PropType.DATATABLE) {
                if ((sp.getFlags() & PropFlag.COLLAPSIBLE) != 0) {
                    gatherCollapsible(sendTableForDtName(sp.getDtName()), accumulator, nameBuf);
                } else {
                    var l = nameBuf.length();
                    nameBuf.append(sp.getVarName());
                    nameBuf.append('.');
                    gather(sendTableForDtName(sp.getDtName()), new LinkedList<>(), nameBuf);
                    nameBuf.setLength(l);
                }
            } else {
                accumulator.add(sp);
            }
        }
    }

    private int[] computeIndexMapping() {
        var mapping =  new int[receiveProps.size()];
        for (var i = 0; i < mapping.length; i++) {
            mapping[i] = i;
        }

        Set<Integer> priorities = new TreeSet<>();
        priorities.add(64);
        for (var rp : receiveProps) {
            priorities.add(rp.getSendProp().getPriority());
        }
        var hole = 0;

        for (var priority : priorities) {
            var cursor = hole;
            while (cursor < mapping.length) {
                var rp = receiveProps.get(mapping[cursor]);
                var sp = rp.getSendProp();
                var changesOften = (sp.getFlags() & PropFlag.CHANGES_OFTEN) != 0 && priority == 64;
                if (changesOften || sp.getPriority() == priority) {
                    var temp = mapping[cursor];
                    mapping[cursor] = mapping[hole];
                    mapping[hole] = temp;
                    hole++;
                }
                cursor++;
            }
        }
        return mapping;
    }

    public Result flatten() {
        aggregateExclusions(table);
        gather(table, new LinkedList<>(), new StringBuilder());
        var r = new Result();
        r.receiveProps = receiveProps.toArray(new ReceiveProp[] {});
        r.indexMapping = computeIndexMapping();
        return r;
    }

    public static class Result {
        public ReceiveProp[] receiveProps;
        public int[] indexMapping;
    }

}
