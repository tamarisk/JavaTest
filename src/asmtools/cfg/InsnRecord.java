package asmtools.cfg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This class represents a bytecode instruction.
 *
 * @author Anna.Yudina@usi.ch
 */
public final class InsnRecord {

    private HashMap<Integer, String> branches;

    private String description;

    private List<String> exClasses;

    private int num;

    public InsnRecord() {
        branches = new HashMap<Integer, String>();
        exClasses = new ArrayList<String>();
    }

    /**
     * @return the branches
     */
    public HashMap<Integer, String> getBranches() {
        return branches;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the exClasses
     */
    public List<String> getExClasses() {
        return exClasses;
    }

    /**
     * @return the num
     */
    public int getNum() {
        return num;
    }

    public void setBranches(HashMap<Integer, String> branches) {
        this.branches = branches;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setExClasses(List<String> exClasses) {
        this.exClasses = exClasses;
    }

    public void setNum(int num) {
        this.num = num;
    }

}