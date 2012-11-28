package asmtools.framework;

/**
 * Thrown in cases of type inconsistencies while building the ClassHierarchy
 *
 * @author Matthias.Hauswirth@usi.ch
 */
@SuppressWarnings("serial")
public class TypeInconsistencyException extends Exception {

    public TypeInconsistencyException(final String message) {
        super(message);
    }

}