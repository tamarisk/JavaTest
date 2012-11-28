package asmtools.classhierarchy;

import asmtools.framework.*;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;

/**
 * Dump out information about the given ClassHierarchy.
 *
 * @author Anna.Yudina@usi.ch
 */
public final class Dumper {

    public void dumpDot(final ClassHierarchy hierarchy, final String fileName)
            throws IOException, TypeInconsistencyException {
        final PrintWriter pw = new PrintWriter(new FileWriter(fileName));
        pw.println("digraph types {");
        pw.println("  rankdir=\"BT\"");

        // add classes
        for (final Type type : hierarchy.getTypes()) {
            if (type instanceof ClassType) {
                final ClassType classType = (ClassType) type;
                pw.print("  \"" + classType.getInternalName());
                if (classType.isInterface()) {
                    pw.print("\" [shape=record, style=dotted, label=\"{"
                            + ouputClassName(classType.getInternalName()) + "|");
                } else if (classType.isAbstract()) {
                    pw.print("\" [shape=record, style=dashed, label=\"{"
                            + ouputClassName(classType.getInternalName()) + "|");
                } else if (classType.isFinal()) {
                    pw.print("\" [shape=record, style=bold, label=\"{"
                            + ouputClassName(classType.getInternalName()) + "|");
                } else if (classType.isResolved()) {
                    pw.print("\" [shape=record, style=solid, label=\"{"
                            + ouputClassName(classType.getInternalName()) + "|");
                }
                if (!classType.isResolved()) {
                    pw.println("\" [shape=ellipse, style=dotted, label=\""
                            + ouputClassName(classType.getInternalName())
                            + "\"]");
                } else {
                    final Collection<Method> methods = classType.getMethods();
                    for (Method method : methods) {
                        if (method.isPrivate()) {
                            pw.print("private ");
                        } else if (method.isPublic()) {
                            pw.print("public ");
                        } else if (method.isProtected()) {
                            pw.print("protected ");
                        }
                        if (method.isFinal()) {
                            pw.print("final ");
                        }
                        if (method.isStatic()) {
                            pw.print("static ");
                        }
                        if (method.isAbstract()) {
                            pw.print("abstract ");
                        }
                        pw.print(method.getName().replaceAll("<", "[")
                                .replaceAll(">", "]")
                                + method.getDescriptor() + "\\n");
                    }
                    pw.println("}\"]");
                }

            }
        }

        // add CH arrows
        for (final Type type : hierarchy.getTypes()) {
            if (type instanceof ClassType) {
                final ClassType classType = (ClassType) type;
                for (ClassType iface : classType.getInterfaces()) {
                    pw.println("  \"" + classType.getInternalName()
                            + "\" -> \"" + iface.getInternalName()
                            + "\" [style=dashed, arrowhead=empty]");
                }

                ClassType superClassType = classType.getSuperClass();
                if (!(superClassType == null)) {
                    pw.println("  \"" + classType.getInternalName()
                            + "\" -> \"" + superClassType.getInternalName()
                            + "\" [style=solid, arrowhead=empty]");
                }
            }
        }

        // add method calls
        for (final Type type : hierarchy.getTypes()) {
            if (type instanceof ClassType) {
                final ClassType classType = (ClassType) type;
                if (classType.isResolved()) {
                    for (Method method : classType.getMethods()) {
                        pw.print("  \""
                                + method.getDeclaringClassName()
                                + "."
                                + method.getName()
                                + method.getDescriptor()
                                + "\" [shape=rectangle, label=\""
                                + method.getDeclaringClassName()
                                + "\\n"
                                + method.getName()
                                + method.getDescriptor()
                                + "\", fillcolor=darkolivegreen1 style=\"filled,");
                        if (method.isAbstract()) {
                            pw.print(",dashed");
                        }
                        pw.println("\"]");

                        pw.println("  \""
                                + method.getDeclaringClassName()
                                + "\" -> \""
                                + method.getDeclaringClassName()
                                + "."
                                + method.getName()
                                + method.getDescriptor()
                                + "\" [arrowhead=none, style=bold, color=darkolivegreen1]");
                    }
                }
            }
        }

        for (final Type type : hierarchy.getTypes()) {
            if (type instanceof ClassType) {
                final ClassType classType = (ClassType) type;
                if (classType.isResolved()) {
                    for (Method method : classType.getMethods()) {
                        for (CallSite callSite : method.getCallSites()) {
                            if (hierarchy.getOrCreateClass(callSite.getDeclaredTargetClassName()).isResolved()) {
                                pw.print("  \""
                                        + method.getDeclaringClassName() + "."
                                        + method.getName()
                                        + method.getDescriptor() + "\" -> \""
                                        + callSite.getDeclaredTargetClassName()
                                        + "." + callSite.getTargetMethodName()
                                        + callSite.getTargetMethodDescriptor()
                                        + "\" [color=blue");
                                if (hierarchy.getOrCreateClass(callSite.getDeclaredTargetClassName()).isInterface()) {
                                    pw.print(", style=dotted");
                                }
                                pw.println("]");
                            }

                            for (ClassType targetType : callSite.getPossibleTargetClasses()) {
                                pw.print("  \""
                                        + method.getDeclaringClassName() + "."
                                        + method.getName()
                                        + method.getDescriptor() + "\" -> \""
                                        + targetType.getInternalName() + "."
                                        + callSite.getTargetMethodName()
                                        + callSite.getTargetMethodDescriptor()
                                        + "\" [color=red");

                                if (hierarchy.getOrCreateClass(callSite.getDeclaredTargetClassName()).isInterface()) {
                                    pw.print(", style=dotted");
                                }
                                pw.println("]");
                            }
                        }
                    }
                }
            }
        }
        pw.println("}");
        pw.close();
    }

    final String ouputClassName(final String internalName) {
        int lastSlash = internalName.lastIndexOf('/');
        String resultName;
        if (internalName.contains("/")) {
            resultName = internalName.substring(0, lastSlash).replace('/', '.')
                    + "\\n" + internalName.substring(lastSlash + 1);
        } else {
            resultName = internalName;
        }
        return resultName;
    }

}