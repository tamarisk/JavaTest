package asmtools.cfg;

/**
 * This class represents an entry in the method's exceptions table.
 *
 * @author Anna.Yudina@usi.ch
 */
public final class ExceptionsTableEntry {

    private int end;

    private int handler;

    private int start;

    private String type;

    public ExceptionsTableEntry(int start, int end, int handler, String type) {
        this.start = start;
        this.end = end;
        this.handler = handler;
        this.type = type;
    }

    /**
     * @return the end
     */
    public int getEnd() {
        return end;
    }

    /**
     * @return the handler
     */
    public int getHandler() {
        return handler;
    }

    /**
     * @return the start
     */
    public int getStart() {
        return start;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

}
