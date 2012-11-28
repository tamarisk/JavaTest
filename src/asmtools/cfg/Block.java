package asmtools.cfg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This class represents a basic block in he CFG.
 *
 * @author Anna.Yudina@usi.ch
 */
public final class Block {

    private HashMap<Integer, String> branches;

    private List<Integer> exBranches;

    private List<InsnRecord> insnRecords;

    private boolean needFalseBranch;

    private int num;

    public Block() {
        branches = new HashMap<Integer, String>();
        exBranches = new ArrayList<Integer>();
        insnRecords = new ArrayList<InsnRecord>();
        needFalseBranch = false;
    }

    /**
     * @return the branches
     */
    public HashMap<Integer, String> getBranches() {
        return branches;
    }

    /**
     * @return the exBranches
     */
    public List<Integer> getExBranches() {
        return exBranches;
    }

    /**
     * @return the insnRecords
     */
    public List<InsnRecord> getInsnRecords() {
        return insnRecords;
    }

    /**
     * @return the num
     */
    public int getNum() {
        return num;
    }

    /**
     * @return the needFalseBranch
     */
    public boolean getNeedFalseBranch() {
        return needFalseBranch;
    }

    public void setBranches(HashMap<Integer, String> branches) {
        this.branches = branches;
    }

    public void setExBranches(List<Integer> exBranches) {
        this.exBranches = exBranches;
    }

    public void setInsnRecords(List<InsnRecord> insnRecords) {
        this.insnRecords = insnRecords;
    }

    public void setNeedFalseBranch(boolean needFalseBranch) {
        this.needFalseBranch = needFalseBranch;
    }

    public void setNum(int num) {
        this.num = num;
    }

}