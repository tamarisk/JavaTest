package asmtools.cfg;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.AbstractVisitor;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * This class extracts a control flow graph in .dot format from the byte code of a specified Java method.
 *
 * @author Anna.Yudina@usi.ch
 */
public final class ControlFlowGraphExtractor {

    static List<Integer> blockSizePerClass;

    final static int returnBlock = -1;

    /**
     * @return the blockSizePerClass
     */
    public List<Integer> getBlockSizePerClass() {
        return blockSizePerClass;
    }

    public static void main(final String[] args) throws IOException {

        String classFileName = null;
        String archiveName = null;
        String methodNameAndDescriptor = "all";
        boolean exFlag = false;

        int argLen = args.length;
        for (int i = 0; i < argLen; i++) {
            final String opName = args[i];
            if (opName.equals("-h")) {
                usage();
                return;
            } else if (opName.equals("-c")) {
                if (++i < argLen) {
                    classFileName = args[i];
                } else {
                    usage();
                    return;
                }
            } else if (opName.equals("-j")) {
                if (++i < argLen) {
                    archiveName = args[i];
                } else {
                    usage();
                    return;
                }
            } else if (opName.equals("-e")) {
                exFlag = true;
            } else if (opName.equals("-m")) {
                if (++i < argLen) {
                    methodNameAndDescriptor = args[i];
                } else {
                    usage();
                    return;
                }
            } else {
                usage();
                return;
            }
        }

        if (archiveName != null) {
            final ZipFile zipFile = new ZipFile(archiveName);
            final Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                final ZipEntry zipEntry = entries.nextElement();
                if (zipEntry.getName().toLowerCase().endsWith(".class")) {
                    final ClassReader cr = new ClassReader(zipFile.getInputStream(zipEntry));
                    final ClassNode classNode = new ClassNode();
                    cr.accept(classNode, 0);
                    final ControlFlowGraphExtractor extractor = new ControlFlowGraphExtractor();
                    extractor.processClass(classNode, methodNameAndDescriptor, exFlag, true);
                }
            }
        } else if (classFileName != null) {
            final ClassReader cr;
            cr = new ClassReader(new FileInputStream(classFileName));
            final ClassNode classNode = new ClassNode();
            cr.accept(classNode, 0);
            final ControlFlowGraphExtractor extractor = new ControlFlowGraphExtractor();
            extractor.processClass(classNode, methodNameAndDescriptor, exFlag, true);
        } else {
            System.out.println("Classes to analyze are not specified.");
        }
    }

    static void usage() {
        System.out.println("Usage: java ControlFlowGraphExtractor <options>");
        System.out.println();
        System.out.println("where options include:");
        System.out.println("-c <path>    path to class");
        System.out.println("-j <path>    path to jar");
        System.out.println("-e           consider exceptions handling");
        System.out.println("-m <name>    method name");
        System.out.println("-h           help");
    }

    public ControlFlowGraphExtractor() {
        blockSizePerClass = new ArrayList<Integer>();
    }

    /**
     * Adds edges to the CFG.
     */
    void addConnections(HashMap<Integer, Block> blocks) {
        for (Block currentBlock : blocks.values()) {
            if (currentBlock.getBranches().isEmpty()) {
                if (!currentBlock.getInsnRecords().isEmpty()) {
                    currentBlock
                            .getBranches()
                            .put(currentBlock
                                    .getInsnRecords()
                                    .get(currentBlock.getInsnRecords().size() - 1)
                                    .getNum() + 1, "");
                }
            } else if (currentBlock.getNeedFalseBranch()) {
                currentBlock.getBranches().put(
                        currentBlock.getInsnRecords()
                                .get(currentBlock.getInsnRecords().size() - 1)
                                .getNum() + 1, "F");
            }
        }
    }

    /**
     * Collect some statistics.
     */
    void blocksStat(HashMap<Integer, Block> blocks) {
        for (Block currentBlock : blocks.values()) {
            blockSizePerClass.add((currentBlock.getInsnRecords().size()));
        }
    }

    /**
     * Creates basic blocks for the CFG.
     */
    HashMap<Integer, Block> createBlocks(MethodNode method, List<InsnRecord> insnRecords, boolean exFlag) {
        InsnList instructions = method.instructions;
        HashMap<Integer, Block> blocks = new HashMap<Integer, Block>();
        for (int i = 0; i < instructions.size(); i++) {
            if (i == 0) {
                Block block = new Block();
                block.setNum(0);
                blocks.put(block.getNum(), block);
            }
            final AbstractInsnNode insnNode = instructions.get(i);
            InsnRecord insnRecord = processInstruction(insnNode, i, instructions);
            if (insnRecord.getDescription() != null) {
                insnRecords.add(insnRecord);
            }
            if (exFlag) {
                if (!insnRecord.getExClasses().isEmpty()) {
                    if (!blocks.keySet().contains(i + 1)) {
                        Block block = new Block();
                        block.setNum(i + 1);
                        blocks.put(block.getNum(), block);
                    }
                }
            }
            if (!insnRecord.getBranches().isEmpty()) {
                for (Integer currentBranch : insnRecord.getBranches().keySet()) {
                    if (currentBranch != -1) {
                        if (blocks.keySet().contains(currentBranch)) {
                        } else {
                            Block block = new Block();
                            block.setNum(currentBranch);
                            blocks.put(block.getNum(), block);
                        }
                    }
                }
                if (!blocks.keySet().contains(i + 1)) {
                    Block block = new Block();
                    block.setNum(i + 1);
                    blocks.put(block.getNum(), block);
                }
            }
        }
        return blocks;
    }

    /**
     * Creates in memory representation of the exceptions table.
     */
    List<ExceptionsTableEntry> createExceptionsTable(MethodNode method) {
        InsnList instructions = method.instructions;
        List<ExceptionsTableEntry> exTable = new ArrayList<ExceptionsTableEntry>();
        @SuppressWarnings("unchecked")
        List<TryCatchBlockNode> tcBlocks = method.tryCatchBlocks;
        for (TryCatchBlockNode tcBlock : tcBlocks) {
            exTable.add(new ExceptionsTableEntry(
                    instructions.indexOf(tcBlock.start),
                    instructions.indexOf(tcBlock.end),
                    instructions.indexOf(tcBlock.handler),
                    tcBlock.type));
        }
        return exTable;
    }

    /**
     * Fills basic blocks with the instructions. Exception handling is conidered.
     */
    void fillBlocks(HashMap<Integer, Block> blocks, List<ExceptionsTableEntry> exTable, List<InsnRecord> insnRecords) {
        for (InsnRecord currentInsnRecord : insnRecords) {
            boolean put = false;
            int dec = 0;
            while (!put) {
                if (blocks.containsKey(currentInsnRecord.getNum() - dec)) {
                    blocks.get(currentInsnRecord.getNum() - dec).getInsnRecords().add(currentInsnRecord);
                    if (!currentInsnRecord.getExClasses().isEmpty()) {
                        blocks.get(currentInsnRecord.getNum() - dec).getExBranches().add(returnBlock);
                        final List<Integer> exHandlers = findExceptionHandlers(
                                currentInsnRecord.getNum(),
                                currentInsnRecord.getExClasses(),
                                exTable);
                        if (!exHandlers.isEmpty()) {
                            for (Integer exHandler : exHandlers) {
                                blocks.get(currentInsnRecord.getNum() - dec).getExBranches().add(exHandler);
                            }
                        }
                    }
                    for (Integer currentBranch : currentInsnRecord.getBranches().keySet()) {
                        blocks.get(currentInsnRecord.getNum() - dec)
                                .getBranches()
                                .put(currentBranch,
                                        currentInsnRecord.getBranches()
                                                .get(currentBranch)
                                                .replace("_only", ""));
                        if (currentInsnRecord.getBranches().get(currentBranch).equals("T")) {
                            blocks.get(currentInsnRecord.getNum() - dec).setNeedFalseBranch(true);
                        }
                    }
                    put = true;
                } else {
                    dec++;
                }
            }
        }
    }

    /**
     * Fills basic blocks with the instructions. Exception handling is not considered.
     */
    void fillBlocks(HashMap<Integer, Block> blocks, List<InsnRecord> insnRecords) {
        for (InsnRecord currentInsnRecord : insnRecords) {
            boolean put = false;
            int dec = 0;
            while (!put) {
                if (blocks.containsKey(currentInsnRecord.getNum() - dec)) {
                    blocks.get(currentInsnRecord.getNum() - dec).getInsnRecords().add(currentInsnRecord);

                    for (Integer currentBranch : currentInsnRecord.getBranches().keySet()) {
                        blocks.get(currentInsnRecord.getNum() - dec)
                                .getBranches()
                                .put(currentBranch,
                                        currentInsnRecord.getBranches().get(currentBranch).replace("_only", ""));
                        if (currentInsnRecord.getBranches().get(currentBranch).equals("T")) {
                            blocks.get(currentInsnRecord.getNum() - dec).setNeedFalseBranch(true);
                        }
                    }
                    put = true;
                } else {
                    dec++;
                }
            }
        }
    }

    /**
     * Finds location of proper exception handlers if any.
     */
    List<Integer> findExceptionHandlers(int insnNum, List<String> exClasses, List<ExceptionsTableEntry> exTable) {
        List<Integer> result = new ArrayList<Integer>();
        if (!exClasses.isEmpty()) {
            for (ExceptionsTableEntry exTableEntry : exTable) {
                if (insnNum >= exTableEntry.getStart() && insnNum < exTableEntry.getEnd()) {
                    result.add(exTableEntry.getHandler());
                }

            }
        }
        return result;
    }

    /**
     * Checks if the instruction is PEI and returns a list of exception class names if any of them can be thrown.
     */
    List<String> getPossibleExceptions(AbstractInsnNode insnNode) {

        List<String> exClasses = new ArrayList<String>();

        switch (insnNode.getOpcode()) {
            case Opcodes.AALOAD:
                exClasses.add("java.lang.NullPointerException");
                exClasses.add("java.lang.ArrayIndexOutOfBoundsException");
                break;
            case Opcodes.AASTORE:
                exClasses.add("java.lang.NullPointerException");
                exClasses.add("java.lang.ArrayIndexOutOfBoundsException");
                exClasses.add("java.lang.ArrayStoreException");
                break;
            case Opcodes.ANEWARRAY:
                exClasses.add("java.lang.NegativeArraySizeException");
                // + linking
                break;
            case Opcodes.ARETURN:
                // if synchronized
                exClasses.add("java.lang.IllegalMonitorStateException");
                break;
            case Opcodes.ARRAYLENGTH:
                exClasses.add("java.lang.NullPointerException");
                break;
            case Opcodes.ATHROW:
                exClasses.add("java.lang.NullPointerException");
                // if synchronized
                exClasses.add("java.lang.IllegalMonitorStateException");
                break;
            case Opcodes.BALOAD:
                exClasses.add("java.lang.NullPointerException");
                exClasses.add("java.lang.ArrayIndexOutOfBoundsException");
                break;
            case Opcodes.BASTORE:
                exClasses.add("java.lang.NullPointerException");
                exClasses.add("java.lang.ArrayIndexOutOfBoundsException");
                break;
            case Opcodes.CALOAD:
                exClasses.add("java.lang.NullPointerException");
                exClasses.add("java.lang.ArrayIndexOutOfBoundsException");
                break;
            case Opcodes.CASTORE:
                exClasses.add("java.lang.NullPointerException");
                exClasses.add("java.lang.ArrayIndexOutOfBoundsException");
                break;
            case Opcodes.CHECKCAST:
                exClasses.add("ClassCastException");
                // + linking
                break;
            case Opcodes.DALOAD:
                exClasses.add("java.lang.NullPointerException");
                exClasses.add("java.lang.ArrayIndexOutOfBoundsException");
                break;
            case Opcodes.DASTORE:
                exClasses.add("java.lang.NullPointerException");
                exClasses.add("java.lang.ArrayIndexOutOfBoundsException");
                break;
            case Opcodes.DRETURN:
                // if synchronized
                exClasses.add("java.lang.IllegalMonitorStateException");
                break;
            case Opcodes.FALOAD:
                exClasses.add("java.lang.NullPointerException");
                exClasses.add("java.lang.ArrayIndexOutOfBoundsException");
                break;
            case Opcodes.FASTORE:
                exClasses.add("java.lang.NullPointerException");
                exClasses.add("java.lang.ArrayIndexOutOfBoundsException");
                break;
            case Opcodes.FRETURN:
                // if synchronized
                exClasses.add("java.lang.IllegalMonitorStateException");
                break;
            case Opcodes.GETFIELD:
                exClasses.add("java.lang.NullPointerException");
                // + linking
                break;
            case Opcodes.GETSTATIC:
                exClasses.add("Error*");
                // + linking
                break;
            case Opcodes.IALOAD:
                exClasses.add("java.lang.NullPointerException");
                exClasses.add("java.lang.ArrayIndexOutOfBoundsException");
                break;
            case Opcodes.IASTORE:
                exClasses.add("java.lang.NullPointerException");
                exClasses.add("java.lang.ArrayIndexOutOfBoundsException");
                break;
            case Opcodes.IDIV:
                exClasses.add("java.lang.ArithmeticException");
                break;
            case Opcodes.INSTANCEOF:
                exClasses.add("Error");
                // + linking
                // case Opcodes.INVOKEDYNAMIC: -- ?
                break;
            case Opcodes.INVOKEINTERFACE:
                exClasses.add("java.lang.NullPointerException");
                exClasses.add("IncompatibleClassChangeError");
                exClasses.add("AbstractMethodError");
                exClasses.add("IllegalAccessError");
                exClasses.add("UnsatisfiedLinkError");
                // + linking
                break;
            case Opcodes.INVOKESPECIAL:
                exClasses.add("java.lang.NullPointerException");
                exClasses.add("UnsatisfiedLinkError");
                // + linking
                break;
            case Opcodes.INVOKESTATIC:
                exClasses.add("UnsatisfiedLinkError");
                exClasses.add("Error*");
                // + linking
                break;
            case Opcodes.INVOKEVIRTUAL:
                exClasses.add("java.lang.NullPointerException");
                exClasses.add("AbstractMethodError");
                exClasses.add("UnsatisfiedLinkError");
                break;
            // + linking
            case Opcodes.IREM:
                exClasses.add("java.lang.ArithmeticException");
                break;
            case Opcodes.IRETURN:
                // if synchronized
                exClasses.add("java.lang.IllegalMonitorStateException");
                break;
            case Opcodes.LALOAD:
                exClasses.add("java.lang.NullPointerException");
                exClasses.add("java.lang.ArrayIndexOutOfBoundsException");
                break;
            case Opcodes.LASTORE:
                exClasses.add("java.lang.NullPointerException");
                exClasses.add("java.lang.ArrayIndexOutOfBoundsException");
                break;
            case Opcodes.LDIV:
                exClasses.add("java.lang.ArithmeticException");
                break;
            case Opcodes.LREM:
                exClasses.add("java.lang.ArithmeticException");
                break;
            case Opcodes.LRETURN:
                // if synchronized
                exClasses.add("java.lang.IllegalMonitorStateException");
                break;
            case Opcodes.MONITORENTER:
                exClasses.add("java.lang.NullPointerException");
                break;
            case Opcodes.MONITOREXIT:
                exClasses.add("java.lang.NullPointerException");
                exClasses.add("java.lang.IllegalMonitorStateException");
                break;
            case Opcodes.MULTIANEWARRAY:
                exClasses.add("java.lang.NegativeArraySizeException");
                // + linking
                break;
            case Opcodes.NEW:
                exClasses.add("Error*");
                // + linking
                break;
            case Opcodes.NEWARRAY:
                exClasses.add("java.lang.NegativeArraySizeException");
                break;
            case Opcodes.PUTFIELD:
                exClasses.add("java.lang.NullPointerException");
                // + linking
                break;
            case Opcodes.PUTSTATIC:
                exClasses.add("Error*");
                // + linking
                break;
            case Opcodes.RETURN:
                // if synchronized
                exClasses.add("java.lang.IllegalMonitorStateException");
                break;
            case Opcodes.SALOAD:
                exClasses.add("java.lang.NullPointerException");
                exClasses.add("java.lang.ArrayIndexOutOfBoundsException");
                break;
            case Opcodes.SASTORE:
                exClasses.add("java.lang.NullPointerException");
                exClasses.add("java.lang.ArrayIndexOutOfBoundsException");
                break;
        }
        return exClasses;
    }

    /**
     * Creates .dot file with the graph that represents blocks.
     *
     * @param blocks     {@link java.util.HashMap} of blocks
     * @param className  name of the class
     * @param methodName name of the method
     * @param exFlag     true if exception handling should be considered
     */
    void output(HashMap<Integer, Block> blocks, final String className, final String methodName, boolean exFlag) {

        if (!(new File("asm-out")).exists()) {
            //noinspection ResultOfMethodCallIgnored
            new File("asm-out").mkdir();
        }

        if (!(new File("asm-out/cfg")).exists()) {
            //noinspection ResultOfMethodCallIgnored
            new File("asm-out/cfg").mkdir();
        }

        final String outputClassName;
        if (className.contains("/")) {
            outputClassName = className.substring(className.lastIndexOf('/') + 1);
        } else {
            outputClassName = className;
        }
        final String pathToOutputFile = "asm-out/cfg/" + outputClassName + "_" + methodName + ".dot";
        final FileWriter outputFile;

        try {
            outputFile = new FileWriter(pathToOutputFile);
            final BufferedWriter out = new BufferedWriter(outputFile);
            out.write("digraph " + outputClassName + "_"
                    + methodName.replace("<", "").replace(">", "") + " {\n"
                    + "S [label=\"S\"]\n" + "E [label=\"E\"]\n");

            Iterator<Block> itBlock = blocks.values().iterator();
            while (itBlock.hasNext()) {
                final Block currentBlock = itBlock.next();
                out.write(currentBlock.getNum() + " [shape=record, label=\"(B" + currentBlock.getNum() + ")|{");
                Iterator<InsnRecord> itInsn = currentBlock.getInsnRecords().iterator();
                while (itInsn.hasNext()) {
                    final InsnRecord insn = itInsn.next();
                    out.write(insn.getDescription().replace('>', ']').replace('<', '['));
                    if (itInsn.hasNext()) {
                        out.write("|");
                    }
                }
                out.write("}\"]\n");
            }

            // add arrows
            out.write("S -> 0\n");
            itBlock = blocks.values().iterator();
            while (itBlock.hasNext()) {
                final Block currentBlock = itBlock.next();
                for (Integer currentBranch : currentBlock.getBranches().keySet()) {
                    if (currentBranch == returnBlock) {
                        out.write(currentBlock.getNum() + " -> E [label=\""
                                + currentBlock.getBranches().get(currentBranch)
                                + "\"]\n");
                    } else {
                        out.write(currentBlock.getNum() + " -> "
                                + currentBranch + " [label=\""
                                + currentBlock.getBranches().get(currentBranch)
                                + "\"]\n");
                    }
                }
            }

            if (exFlag) {
                // add exceptions arrows
                itBlock = blocks.values().iterator();
                while (itBlock.hasNext()) {
                    final Block currentBlock = itBlock.next();
                    for (Integer currentBranch : currentBlock.getExBranches()) {
                        if (currentBranch == returnBlock) {
                            out.write(currentBlock.getNum() + " -> E [label=\" \", style=dotted]\n");
                        } else {
                            out.write(currentBlock.getNum() + " -> " + currentBranch +
                                    " [label=\" \", style=dotted]\n");
                        }
                    }
                }
            }

            out.write("}");
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param classNode               class node
     * @param methodNameAndDescriptor {@link String} that contains concatenated name and
     *                                descriptor of the method or the value "all" to create a graph
     *                                of all methods of the class
     * @param exFlag                  true if exception handling should be considered
     * @param outputFlag              true if .dot output is needed
     */
    public void processClass(final ClassNode classNode,
                             final String methodNameAndDescriptor, boolean exFlag,
                             boolean outputFlag) {

        @SuppressWarnings("unchecked")
        final List<MethodNode> methods = classNode.methods;
        for (final MethodNode methodNode : methods) {
            if (methodNameAndDescriptor.equals("all")
                    || methodNameAndDescriptor.equals(methodNode.name + methodNode.desc)) {
                processMethod(classNode.name, methodNode, exFlag, outputFlag);
            }
        }
    }

    /**
     * Processes a single instruction.
     *
     * @param insnNode     � instruction node
     * @param insnNumber   � instruction number
     * @param instructions � list of instructions of the same method
     * @return parsed instruction record
     */
    InsnRecord processInstruction(final AbstractInsnNode insnNode, final int insnNumber, final InsnList instructions) {

        InsnRecord insnRecord = new InsnRecord();
        insnRecord.setNum(insnNumber);

        final int opcode = insnNode.getOpcode();
        final String mnemonic = opcode == -1 ? "" : AbstractVisitor.OPCODES[insnNode.getOpcode()];

        insnRecord.setExClasses(getPossibleExceptions(insnNode));

        switch (insnNode.getType()) {
            case AbstractInsnNode.LABEL:
                // pseudo-instruction (branch or exception target)
                break;
            case AbstractInsnNode.FRAME:
                // pseudo-instruction (stack frame map)
                break;
            case AbstractInsnNode.LINE:
                // pseudo-instruction (line number information)
                break;
            case AbstractInsnNode.INSN:
                // Opcodes: NOP, ACONST_NULL, ICONST_M1, ICONST_0, ICONST_1, ICONST_2,
                // ICONST_3, ICONST_4, ICONST_5, LCONST_0, LCONST_1, FCONST_0,
                // FCONST_1, FCONST_2, DCONST_0, DCONST_1, IALOAD, LALOAD, FALOAD,
                // DALOAD, AALOAD, BALOAD, CALOAD, SALOAD, IASTORE, LASTORE, FASTORE,
                // DASTORE, AASTORE, BASTORE, CASTORE, SASTORE, POP, POP2, DUP,
                // DUP_X1, DUP_X2, DUP2, DUP2_X1, DUP2_X2, SWAP, IADD, LADD, FADD,
                // DADD, ISUB, LSUB, FSUB, DSUB, IMUL, LMUL, FMUL, DMUL, IDIV, LDIV,
                // FDIV, DDIV, IREM, LREM, FREM, DREM, INEG, LNEG, FNEG, DNEG, ISHL,
                // LSHL, ISHR, LSHR, IUSHR, LUSHR, IAND, LAND, IOR, LOR, IXOR, LXOR,
                // I2L, I2F, I2D, L2I, L2F, L2D, F2I, F2L, F2D, D2I, D2L, D2F, I2B,
                // I2C, I2S, LCMP, FCMPL, FCMPG, DCMPL, DCMPG, IRETURN, LRETURN,
                // FRETURN, DRETURN, ARETURN, RETURN, ARRAYLENGTH, ATHROW,
                // MONITORENTER, or MONITOREXIT.
                // zero operands, nothing to print
                insnRecord.setDescription(insnNumber + ": " + mnemonic);
                if (mnemonic.contains("RETURN")) {
                    insnRecord.getBranches().put(returnBlock, "");
                }
                break;
            case AbstractInsnNode.INT_INSN:
                // Opcodes: NEWARRAY, BIPUSH, SIPUSH.

                if (insnNode.getOpcode() == Opcodes.NEWARRAY) {
                    // NEWARRAY
                    insnRecord
                            .setDescription(insnNumber
                                    + ": "
                                    + mnemonic
                                    + " "
                                    + AbstractVisitor.TYPES[((IntInsnNode) insnNode).operand]);
                } else {
                    // BIPUSH or SIPUSH
                    insnRecord.setDescription(insnNumber + ": " + mnemonic + " " + ((IntInsnNode) insnNode).operand);
                }
                break;
            case AbstractInsnNode.JUMP_INSN:
                // Opcodes: IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE, IF_ICMPEQ,
                // IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ,
                // IF_ACMPNE, GOTO, JSR, IFNULL or IFNONNULL.
            {
                final LabelNode targetInstruction = ((JumpInsnNode) insnNode).label;
                final int targetId = instructions.indexOf(targetInstruction);
                insnRecord.setDescription(insnNumber + ": " + mnemonic + " (B" + targetId + ")");

                if (insnNode.getOpcode() == Opcodes.GOTO || insnNode.getOpcode() == Opcodes.JSR) {
                    insnRecord.getBranches().put(targetId, "T_only");
                } else {
                    insnRecord.getBranches().put(targetId, "T");
                }
                break;
            }
            case AbstractInsnNode.LDC_INSN:
                // Opcodes: LDC.
                insnRecord.setDescription(insnNumber + ": " + mnemonic + " " + ((LdcInsnNode) insnNode).cst);
                break;
            case AbstractInsnNode.IINC_INSN:
                // Opcodes: IINC.
                insnRecord.setDescription(insnNumber + ": " + mnemonic + " "
                        + ((IincInsnNode) insnNode).var + " "
                        + ((IincInsnNode) insnNode).incr);
                break;
            case AbstractInsnNode.TYPE_INSN:
                // Opcodes: NEW, ANEWARRAY, CHECKCAST or INSTANCEOF.
                insnRecord.setDescription(insnNumber + ": " + mnemonic + " " + ((TypeInsnNode) insnNode).desc);
                break;
            case AbstractInsnNode.VAR_INSN:
                // Opcodes: ILOAD, LLOAD, FLOAD, DLOAD, ALOAD, ISTORE,
                // LSTORE, FSTORE, DSTORE, ASTORE or RET.
                insnRecord.setDescription(insnNumber + ": " + mnemonic + " " + ((VarInsnNode) insnNode).var);
                break;
            case AbstractInsnNode.FIELD_INSN:
                // Opcodes: GETSTATIC, PUTSTATIC, GETFIELD or PUTFIELD.
                insnRecord.setDescription(insnNumber + ": " + mnemonic + " "
                        + ((FieldInsnNode) insnNode).owner + "."
                        + ((FieldInsnNode) insnNode).name + " "
                        + ((FieldInsnNode) insnNode).desc);
                break;
            case AbstractInsnNode.METHOD_INSN:
                // Opcodes: INVOKEVIRTUAL, INVOKESPECIAL, INVOKESTATIC,
                // INVOKEINTERFACE or INVOKEDYNAMIC.
                insnRecord.setDescription(insnNumber + ": " + mnemonic + " "
                        + ((MethodInsnNode) insnNode).owner + "."
                        + ((MethodInsnNode) insnNode).name + " "
                        + ((MethodInsnNode) insnNode).desc);
                break;
            case AbstractInsnNode.MULTIANEWARRAY_INSN:
                // Opcodes: MULTIANEWARRAY.
                insnRecord.setDescription(insnNumber + ": " + mnemonic + " "
                        + ((MultiANewArrayInsnNode) insnNode).desc + " "
                        + ((MultiANewArrayInsnNode) insnNode).dims);
                break;
            case AbstractInsnNode.LOOKUPSWITCH_INSN:
                // Opcodes: LOOKUPSWITCH.
            {
                insnRecord.setDescription(insnNumber + ": " + mnemonic);
                @SuppressWarnings("rawtypes")
                final List keys = ((LookupSwitchInsnNode) insnNode).keys;
                @SuppressWarnings("rawtypes")
                final List labels = ((LookupSwitchInsnNode) insnNode).labels;
                for (int t = 0; t < keys.size(); t++) {
                    final int key = (Integer) keys.get(t);
                    final LabelNode targetInstruction = (LabelNode) labels.get(t);
                    final int targetId = instructions.indexOf(targetInstruction);
                    insnRecord.getBranches().put(targetId, Integer.toString(key));
                }
                final LabelNode defaultTargetInstruction = ((LookupSwitchInsnNode) insnNode).dflt;
                final int defaultTargetId = instructions.indexOf(defaultTargetInstruction);
                insnRecord.getBranches().put(defaultTargetId, "default");
                break;
            }
            case AbstractInsnNode.TABLESWITCH_INSN:
                // Opcodes: TABLESWITCH.
            {
                insnRecord.setDescription(insnNumber + ": " + mnemonic + " ");
                final int minKey = ((TableSwitchInsnNode) insnNode).min;
                @SuppressWarnings("rawtypes")
                final List labels = ((TableSwitchInsnNode) insnNode).labels;
                for (int t = 0; t < labels.size(); t++) {
                    final int key = minKey + t;
                    final LabelNode targetInstruction = (LabelNode) labels.get(t);
                    final int targetId = instructions.indexOf(targetInstruction);
                    insnRecord.getBranches().put(targetId, Integer.toString(key));
                }
                final LabelNode defaultTargetInstruction = ((TableSwitchInsnNode) insnNode).dflt;
                final int defaultTargetId = instructions.indexOf(defaultTargetInstruction);
                insnRecord.getBranches().put(defaultTargetId, "default");
                break;
            }
        }

        return insnRecord;
    }

    /**
     * @param className  name of the class
     * @param method     name of the method
     * @param exFlag     true if exception handling should be considered
     * @param outputFlag true if .dot output is needed
     */
    void processMethod(final String className, final MethodNode method, boolean exFlag, boolean outputFlag) {

        List<InsnRecord> insnRecords = new ArrayList<InsnRecord>();
        HashMap<Integer, Block> blocks = createBlocks(method, insnRecords, exFlag);
        if (exFlag) {
            List<ExceptionsTableEntry> exTable = createExceptionsTable(method);
            fillBlocks(blocks, exTable, insnRecords);
        } else {
            fillBlocks(blocks, insnRecords);
        }

        addConnections(blocks);
        if (outputFlag) {
            output(blocks, className, method.name, exFlag);
        }
        blocksStat(blocks);
    }

}