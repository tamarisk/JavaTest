package asmtools.classhierarchy;

import asmtools.framework.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.AbstractVisitor;

import java.util.Iterator;
import java.util.List;

/**
 * Build a call graph (as part of the class hierarchy) consisting of CallSite
 * nodes pointing to Method nodes.
 *
 * @author Anna.Yudina@usi.ch
 */
public final class CallGraphBuilder implements ClassAnalyzer {

    private final ClassHierarchy hierarchy;

    public CallGraphBuilder(final ClassHierarchy hierarchy) {
        this.hierarchy = hierarchy;
    }

    public void analyze(final String location, final ClassNode classNode) {
        try {
            final ClassType classType = hierarchy.getOrCreateClass(classNode.name);

            @SuppressWarnings("unchecked")
            final List<MethodNode> methodNodes = (List<MethodNode>) classNode.methods;
            for (final MethodNode methodNode : methodNodes) {
                final Method method = classType.getMethod(methodNode.name, methodNode.desc);
                if (method != null) {
                    final InsnList instructions = methodNode.instructions;
                    for (int i = 0; i < instructions.size(); i++) {
                        final AbstractInsnNode insnNode = instructions.get(i);
                        final int opcode = insnNode.getOpcode();
                        final String mnemonic = opcode == -1 ? "" : AbstractVisitor.OPCODES[insnNode.getOpcode()];

                        if (insnNode.getType() == AbstractInsnNode.METHOD_INSN) {
                            // Opcodes: INVOKEVIRTUAL, INVOKESPECIAL, INVOKESTATIC, INVOKEINTERFACE or INVOKEDYNAMIC.
                            final String owner = ((MethodInsnNode) insnNode).owner;
                            final String name = ((MethodInsnNode) insnNode).name;
                            final String desc = ((MethodInsnNode) insnNode).desc;
                            final CallSite callSite = new CallSite(opcode, owner, name, desc);

                            if (mnemonic.equals("INVOKEVIRTUAL")) {
                                chaVirtual(
                                        hierarchy.getOrCreateClass(callSite.getDeclaredTargetClassName()),
                                        name, desc, callSite);
                            } else if (mnemonic.equals("INVOKESPECIAL")) {
                                chaUp(
                                        hierarchy.getOrCreateClass(callSite.getDeclaredTargetClassName()),
                                        name, desc, callSite);
                            } else if (mnemonic.equals("INVOKESTATIC")) {
                                ClassType targetType =
                                        hierarchy.getOrCreateClass(callSite.getDeclaredTargetClassName());
                                if (targetType.isResolved()) {
                                    callSite.addPossibleTargetClass(targetType);
                                }
                            } else if (mnemonic.equals("INVOKEINTERFACE")) {
                                chaInterface(
                                        hierarchy.getOrCreateClass(callSite.getDeclaredTargetClassName()),
                                        name, desc, callSite);
                            } else if (mnemonic.equals("INVOKEDYNAMIC")) {
                            }
                            method.addCallSite(callSite);
                        }
                    }
                }
            }
        } catch (final TypeInconsistencyException ex) {
            System.err.println(ex);
        }
    }

    private boolean containsMethod(final ClassType classType,
                                   final String name,
                                   final String desc) {
        boolean found = false;
        Iterator<Method> itMethods = classType.getMethods().iterator();
        while (itMethods.hasNext() && !found) {
            final Method method = itMethods.next();
            if (method.getName().equals(name)
                    && method.getDescriptor().equals(desc)
                    && !method.isAbstract()) {
                found = true;
            }
        }
        return found;
    }

    private void chaVirtual(final ClassType classType,
                            final String name,
                            final String desc, final CallSite callSite) {
        chaUp(classType, name, desc, callSite);
        chaVirtualDown(classType, name, desc, callSite);
    }

    private void chaUp(final ClassType classType,
                       final String name,
                       final String desc,
                       final CallSite callSite) {
        if (containsMethod(classType, name, desc)) {
            callSite.addPossibleTargetClass(classType);
        } else {
            if (!(classType.getSuperClass() == null)) {
                chaUp(classType.getSuperClass(), name, desc, callSite);
            }
        }
    }

    private void chaVirtualDown(final ClassType classType,
                                final String name,
                                final String desc,
                                final CallSite callSite) {
        for (ClassType subType : classType.getSubTypes()) {
            if (containsMethod(subType, name, desc)) {
                callSite.addPossibleTargetClass(subType);
                chaVirtualDown(subType, name, desc, callSite);
            }
        }
    }

    private void chaInterface(final ClassType classType,
                              final String name,
                              final String desc,
                              final CallSite callSite) {
        if (classType.isInterface()) {
            for (ClassType classType1 : classType.getSubTypes()) {
                chaInterface(classType1, name, desc, callSite);
            }
        } else {
            chaVirtual(classType, name, desc, callSite);
        }
    }

}
