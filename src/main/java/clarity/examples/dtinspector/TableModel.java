package clarity.examples.dtinspector;

import javax.swing.table.AbstractTableModel;

import clarity.model.DTClass;
import clarity.model.PropFlag;
import clarity.model.ReceiveProp;

public class TableModel extends AbstractTableModel {
    private static final long serialVersionUID = 2946867068203801119L;

    private final DTClass dtClass;

    public TableModel(DTClass dtClass) {
        this.dtClass = dtClass;
    }

    @Override
    public int getRowCount() {
        return dtClass.getReceiveProps().size();
    }

    @Override
    public int getColumnCount() {
        return 5;
    }

    @Override
    public String getColumnName(int column) {
        switch (column) {
        case 0:
            return "Name";
        case 1:
            return "Type";
        case 2:
            return "Source";
        case 3:
            return "Priority";
        case 4:
            return "Flags";
        default:
            return "";
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        ReceiveProp p = dtClass.getReceiveProps().get(rowIndex);
        switch (columnIndex) {
        case 0:
            return p.getVarName();
        case 1:
            return p.getType();
        case 2:
            return p.getSrc();
        case 3:
            return p.getPriority();
        case 4:
            StringBuffer buf = new StringBuffer();
            for (PropFlag f : PropFlag.values()) {
                if (p.isFlagSet(f)) {
                    if (buf.length() > 0) {
                        buf.append(", ");
                    }
                    buf.append(f.name());
                }
            }
            return buf.toString();
        default:
            return "";
        }
    }
}