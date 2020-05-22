package handist.mpijunit;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.junit.runner.notification.RunNotifier;

/**
 * Class used to capture the fact the {@link ToFileRunNotifier} was called. 
 * An instance of this class represents a single call to the notifier. 
 * When each mpi process has terminated, the {@link MpiRunner} gathers the 
 * calls made to the {@link ToFileRunNotifier} in each rank and reproduces 
 * these calls on its own {@link RunNotifier}, effectively transmitting the results
 * of every mpi process.
 *  
 * @author Patrick Finnerty
 * @see ToFileRunNotifier
 *
 */
public class Notification implements Serializable {
	
	/** Serial Version UID */
	private static final long serialVersionUID = -420188700639629567L;

	/** Method that was called */
	public String method;
	
	/** 
	 * Parameters that were used when making the call to the notifier. 
	 * <p>
	 * In practice, there is always a single parameter in the methods of the 
	 * notifier. But it is easier to carry the array of objects as it matches
	 * the expected format of the {@link Method#invoke(Object, Object...)} 
	 * method used when re-making the calls to the notifier in 
	 * {@link MpiRunner}.  
	 * */
	public Object[] parameters;

	/**
	 * Constructor. 
	 * 
	 * Builds a Notification object that represents the fact that a method 
	 * of the class {@link ToFileRunNotifier} was called.
	 * 
	 * @param m the name of the method that was called
	 * @param params parameters with which the method was called 
	 */
	public Notification (String m, Object[] params) {
		method = m;
		parameters = params;
	}
	
	public String toString() {
		return method + "(" + parameters[0].toString() + ")";
	}
}