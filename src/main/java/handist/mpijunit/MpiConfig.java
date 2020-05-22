package handist.mpijunit;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation used to set some environment settings for test classes that use 
 * the {@link MpiRunner}. At a minimum, test classes have to specify how many 
 * processes are desired for the test.  
 * 
 * @author Patrick Finnerty
 *
 */
@Documented
@Retention(RUNTIME)
@Target(TYPE)
public @interface MpiConfig {
	/**
	 * Indicates how many mpi processes are desired to run the test class. This
	 * setting needs to be set, there are no default value for this setting.  
	 * @return number of MPI processes to be launched
	 */
	int ranks();
	
	/**
	 * Indicates which launcher should be used in each mpiprocess. The launcher 
	 * is the class whose main method is responsible for launching the tests in 
	 * each MPI process. Using a different class than the default may be useful 
	 * if you have some specific setup to do before launching the normal Junit 
	 * runtime.
	 * <p>
	 * By default, class {@link MpiTestLauncher} is used.  
	 * 
	 * @return the class whose main will be launched by all the MPI processess
	 */
	Class<?> launcher() default MpiTestLauncher.class;
}
