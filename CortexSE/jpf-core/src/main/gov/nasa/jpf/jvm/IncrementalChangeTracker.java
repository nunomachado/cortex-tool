package gov.nasa.jpf.jvm;

/**
 * This should be implemented by classes that read & reset "hasChanged"-type
 * information in the KernelState.  Example: CollapsingSerializer
 * 
 * @author peterd
 */
public interface IncrementalChangeTracker extends KernelState.ChangeListener {

}
