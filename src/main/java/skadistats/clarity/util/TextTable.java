package skadistats.clarity.util;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class TextTable {

    public static final char[] FRAME_UNICODE = { '│', '─', '┌', '├', '└', '┬', '┼', '┴', '┐', '┤', '┘'};
    public static final char[] FRAME_COMPAT  = { '|', '-', '+', '+', '+', '+', '+', '+', '+', '+', '+'};

    public enum Alignment {
        LEFT, RIGHT
    }

    private static class ColDef {
        private String header;
        private Alignment alignment;
        private String conversion = "s";
        public ColDef(String header, Alignment alignment) {
            this.header = header;
            this.alignment = alignment;
        }
    }

    public static class Builder {
        private String title;
        private List<ColDef> columns = new ArrayList<>();
        private int paddingLeft = 1;
        private int paddingRight = 1;
        private boolean framed = true;
        private char[] frame = FRAME_UNICODE;
        public Builder setPadding(int paddingLeft, int paddingRight) {
            this.paddingLeft = paddingLeft;
            this.paddingRight = paddingRight;
            return this;
        }
        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }
        public Builder setFramed(boolean framed) {
            this.framed = framed;
            return this;
        }
        public Builder setFrame(char[] frame) {
            this.frame = frame;
            return this;
        }
        public Builder addColumn() {
            return addColumn("");
        }
        public Builder addColumn(String header) {
            return addColumn(header, Alignment.LEFT);
        }
        public Builder addColumn(String header, Alignment alignment) {
            columns.add(new ColDef(header, alignment));
            return this;
        }
        public TextTable build() {
            return new TextTable(title, columns, paddingLeft, paddingRight, framed, frame);
        }
    }

    private final String title;
    private final ColDef[] columns;
    private final String paddingLeft;
    private final String paddingRight;
    private final boolean framed;
    private final char[] frame;
    private final TreeMap<Integer, TreeMap<Integer, Object>> data = new TreeMap<>();

    private TextTable(String title, List<ColDef> columns, int paddingLeft, int paddingRight, boolean framed, char[] frame) {
        this.title = title;
        this.columns = columns.toArray(new ColDef[] {});
        this.paddingLeft = repeat(paddingLeft, ' ');
        this.paddingRight = repeat(paddingRight, ' ');
        this.framed = framed;
        this.frame = frame;
    }

    public void setData(int row, int column, Object value) {
        if (column >= columns.length) {
            throw new IndexOutOfBoundsException("column must be smaller than " + columns.length);
        }
        TreeMap<Integer, Object> rowMap = data.get(row);
        if (rowMap == null) {
            rowMap = new TreeMap<>();
            data.put(row, rowMap);
        }
        rowMap.put(column, value);
    }

    public Object getData(int row, int column) {
        if (column >= columns.length) {
            throw new IndexOutOfBoundsException("column must be smaller than " + columns.length);
        }
        TreeMap<Integer, Object> rowMap = data.get(row);
        return rowMap != null ? rowMap.get(column) : null;
    }

    public int getColumnCount() {
        return columns.length;
    }

    public int getRowCount() {
        return data.size() == 0 ? 0 : data.lastKey() + 1;
    }

    public void print(PrintWriter writer) {
        int[] widths = calculateColumnWidths();
        String f = calcFormatString(widths);
        if (title != null) {
            if (framed) {
                writer.println(calcTitleSeparatorString(widths, 0));
            }
            writer.format(calcTitleFormatString(widths), title);
            writer.println();
            if (framed) {
                writer.println(calcTitleSeparatorString(widths, 1));
            }
        } else {
            if (framed) {
                writer.println(calcSeparatorString(widths, 0));
            }
        }
        writer.format(f, getHeaders());
        writer.println();
        if (framed) {
            writer.println(calcSeparatorString(widths, 1));
        }
        for (int r = 0; r < getRowCount(); r++) {
            writer.format(f, getObjects(r));
            writer.println();
            if (framed) {
                writer.println(calcSeparatorString(widths, r + 1 == getRowCount() ? 2 : 1));
            }
        }
    }

    public void print(PrintStream stream) {
        print(new PrintWriter(stream, true));
        stream.flush();
    }

    public String toString() {
        StringWriter w = new StringWriter();
        print(new PrintWriter(w));
        return w.toString();
    }

    public String repeat(int n, char ch) {
        return CharBuffer.allocate(n).toString().replace('\0', ch);
    }

    private String calcFormatString(int[] widths) {
        StringBuffer buf = new StringBuffer();
        if (framed) {
            buf.append(frame[0]);
        }
        for (int i = 0; i < columns.length; i++) {
            buf.append(paddingLeft);
            buf.append('%');
            if (columns[i].alignment == Alignment.LEFT) {
                buf.append('-');
            }
            buf.append(widths[i]);
            buf.append(columns[i].conversion);
            buf.append(paddingRight);
            if (framed) {
                buf.append(frame[0]);
            }
        }
        return buf.toString();
    }

    private String calcTitleFormatString(int[] widths) {
        StringBuffer buf = new StringBuffer();
        if (framed) {
            buf.append(frame[0]);
        }
        buf.append(paddingLeft);
        buf.append('%');
        buf.append('-');
        buf.append(widths[widths.length - 1] - (framed ? 2 : 0) - paddingLeft.length() - paddingRight.length());
        buf.append('s');
        buf.append(paddingRight);
        if (framed) {
            buf.append(frame[0]);
        }
        return buf.toString();
    }


    private String calcSeparatorString(int[] widths, int pos) {
        StringBuffer buf = new StringBuffer();
        buf.append(frame[2 + pos]);
        for (int i = 0; i < columns.length; i++) {
            buf.append(repeat(paddingLeft.length() + widths[i] + paddingRight.length(), frame[1]));
            buf.append(frame[(i + 1 == columns.length ? 8 : 5) + pos]);
        }
        return buf.toString();
    }

    private String calcTitleSeparatorString(int[] widths, int pos) {
        StringBuffer buf = new StringBuffer();
        buf.append(frame[2 + pos]);
        for (int i = 0; i < columns.length; i++) {
            buf.append(repeat(paddingLeft.length() + widths[i] + paddingRight.length(), frame[1]));
            if (i + 1 == columns.length) {
                buf.append(frame[8 + pos]);
            } else {
                buf.append(frame[pos == 0 ? 1 : 5]);
            }
        }
        return buf.toString();
    }


    private int[] calculateColumnWidths() {
        int[] widths = new int[columns.length + 1];
        for (int i = 0; i < columns.length; i++) {
            widths[i] = columns[i].header.length();
        }
        for (TreeMap<Integer, Object> rowMap : data.values()) {
            for (Map.Entry<Integer, Object> entry : rowMap.entrySet()) {
                if (entry.getValue() != null) {
                    widths[entry.getKey()] = Math.max(widths[entry.getKey()], convertToString(entry.getKey(), entry.getValue()).length());
                }
            }
        }
        int complete = framed ? 1 : 0;
        for (int i = 0; i < columns.length; i++) {
            complete += widths[i] + paddingLeft.length() + paddingRight.length() + (framed ? 1 : 0);
        }
        widths[columns.length] = complete;
        return widths;
    }

    private String convertToString(int i, Object value) {
        return String.format("%" + columns[i].conversion, value);
    }

    private Object[] getHeaders() {
        Object[] result = new Object[columns.length];
        for (int i = 0; i < columns.length; i++) {
            result[i] = columns[i].header;
        }
        return result;
    }

    private Object[] getObjects(int row) {
        Object[] result = new Object[columns.length];
        for (int i = 0; i < columns.length; i++) {
            result[i] = getData(row, i);
        }
        return result;
    }

}
