package asmtools.classhierarchy;

import asmtools.framework.ArchiveScanner;
import asmtools.framework.TypeInconsistencyException;

import java.io.File;
import java.io.IOException;

/**
 * Main class.
 *
 * @author Anna.Yudina@usi.ch
 */
public final class ClassHierarchyMain {

    public static void main(final String[] args) throws IOException, TypeInconsistencyException {
        final ArchiveScanner scanner = new ArchiveScanner();

        // phase 1: build inheritance hierarchy
        final ClassHierarchyBuilder classHierarchyBuilder = new ClassHierarchyBuilder();
        scanner.addAnalyzer(classHierarchyBuilder);
        for (String arg : args) {
            scanner.scan(arg);
        }
        scanner.removeAnalyzer(classHierarchyBuilder);

        // phase 2: add call sites and edges
        final CallGraphBuilder callGraphBuilder =
                new CallGraphBuilder(classHierarchyBuilder.getClassHierarchy());
        scanner.addAnalyzer(callGraphBuilder);
        for (String arg : args) {
            scanner.scan(arg);
        }

        // dump info about structure
        if (!(new File("asm-out")).exists()) {
            //noinspection ResultOfMethodCallIgnored
            new File("asm-out").mkdir();
        }

        new Dumper().dumpDot(classHierarchyBuilder.getClassHierarchy(), "asm-out/class_hierarchy.dot");

        // print statistics
        // stats.printStatistics();
    }

}