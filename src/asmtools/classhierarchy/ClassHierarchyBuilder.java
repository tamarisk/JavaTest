package asmtools.classhierarchy;

import asmtools.framework.*;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;

/**
 * Build a class hierarchy (including methods).
 *
 * @author Anna.Yudina@usi.ch
 */
public final class ClassHierarchyBuilder implements ClassAnalyzer {

    private final ClassHierarchy classHierarchy;

    public ClassHierarchyBuilder() {
        this.classHierarchy = new ClassHierarchy();
    }

    public ClassHierarchy getClassHierarchy() {
        return classHierarchy;
    }

    public void analyze(final String location, final ClassNode clazz) {
        try {
            ClassType classType = classHierarchy.getOrCreateClass(clazz.name);
            if (classType.isResolved()) {
                return;
            }
            classType.setLocation(location);

            // extract modifiers
            final int modifiers = clazz.access;
            classType.setModifiers(modifiers);

            // extract superclass
            final String superClassName = clazz.superName;
            ClassType superClassType = classHierarchy.getOrCreateClass(superClassName);
            classType.setSuperClass(superClassType);

            // extract interfaces
            @SuppressWarnings("unchecked")
            final List<String> interfaces = clazz.interfaces;
            for (String interfaceName : interfaces) {
                ClassType interfaceClassType = classHierarchy.getOrCreateClass(interfaceName);
                classType.addInterface(interfaceClassType);
            }

            // extract methods
            @SuppressWarnings("unchecked")
            final List<MethodNode> methods = clazz.methods;
            for (MethodNode methodNode : methods) {
                Method method = new Method(clazz.name, methodNode.name, methodNode.desc, methodNode.access);
                classType.addMethod(method);
            }

            classType.setResolved();
        } catch (final TypeInconsistencyException ex) {
            System.err.println(ex);
        }
    }

}
