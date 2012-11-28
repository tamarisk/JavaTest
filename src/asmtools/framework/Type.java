package asmtools.framework;

/**
 * A (primitive, array, or class) type in Java.
 * 
 * @author Matthias.Hauswirth@usi.ch
 */
public interface Type {

	public String getInternalName();

	public boolean isResolved();

	public String getSimpleName();

}