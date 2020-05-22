package handist.mpijunit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runner.notification.StoppedByUserException;

/**
 * Custom implementation of the Junit {@link RunNotifier}. 
 * Instead of using the subscriber-based system of the standard Junit
 * framework, this implementation writes the calls it receives received to a 
 * {@link File} specified as a parameter of its constructor. 
 * 
 * @author Patrick Finnerty
 *
 */
public class ToFileRunNotifier extends RunNotifier {
	
	public static String testAssumptionFailedMethod = "fireTestAssumptionFailed";
	
	public static String testFailureMethod = "fireTestFailure";
	
	public static String testFinishedMethod = "fireTestFinished";
	public static String testIgnoredMethod= "fireTestIgnored";
	public static String testRunFinishedMethod = "fireTestRunFinished";
	public static String testRunStartedMethod = "fireTestRunStarted";
	public static String testStartedMethod = "fireTestStarted";
	public static String testSuiteFinishedMethod = "fireTestSuiteFinished";
	public static String testSuiteStartedMethod = "fireTestSuiteStarted";
	/** 
	 * Accumulator of the calls made to this instance. 
	 * This member is the one which is serialized and written to 
	 * {@link #outputFile} in method {@link #close()}.  
	 */
	ArrayList<Notification> notifiedEvents;
	/** 
	 * OutputStream used to write the {@link Notification} objects to the file 
	 */
	transient ObjectOutputStream objOut;
	
	/** File object to which the calls received to this instance are written */
	transient File outputFile;
	
	/**
	 * Constructor
	 * 
	 * @param f the file to which the events notified to this instance are to be
	 * 	written
	 * 
	 * @throws IOException if something goes wrong when attempting to open the file
	 */
	public ToFileRunNotifier(File f) throws IOException {
		outputFile = f;
		if (!outputFile.exists()) {
			outputFile.createNewFile();
		}
		objOut = new ObjectOutputStream(new FileOutputStream(outputFile));
		notifiedEvents = new ArrayList<>();
	}

    /**
     * Unused in this implementation
     */
    @Override
	public void addFirstListener(RunListener listener) {
    }
    
	/**
     * Unused in this implementation
     */
    @Override
	public void addListener(RunListener listener) {
    }
	
	/**
	 * Writes all the notifications received to the output stream 
	 * {@link #objOut},flushes it, and closes it.
	 * <p>
	 * This method should be called after all the tests have been run and the 
	 * JVM running the tests for this particular place have terminated.
	 * 
	 * @throws IOException if such an Exception is thrown when manipulating 
	 * 	{@link #objOut}
	 */
	public void close() throws IOException {
		objOut.writeObject(notifiedEvents);
		objOut.flush();
		objOut.close();
	}
	
	/**
     * Invoke to tell listeners that an atomic test flagged that it assumed
     * something false.
     *
     * @param failure the description of the test that failed and the
     * {@link org.junit.AssumptionViolatedException} thrown
     */
    @Override
	public void fireTestAssumptionFailed(final Failure failure) {
    	Object [] args = new Object[1];
    	args[0] = failure;
    	notifiedEvents.add(new Notification(testAssumptionFailedMethod, args));
    }

    /**
     * Invoke to tell listeners that an atomic test failed.
     *
     * @param failure the description of the test that failed and the exception thrown
     */
    @Override
	public void fireTestFailure(Failure failure) {
    	Object [] args = new Object[1];
    	args[0] = failure;
    	notifiedEvents.add(new Notification(testFailureMethod, args));
    }

    /**
     * Invoke to tell listeners that an atomic test finished. Always invoke
     * this method if you invoke {@link #fireTestStarted(Description)}
     * as listeners are likely to expect them to come in pairs.
     *
     * @param description the description of the test that finished
     */
    @Override
	public void fireTestFinished(final Description description) {
    	Object [] args = new Object[1];
    	args[0] = description;
    	notifiedEvents.add(new Notification(testFinishedMethod, args));
    }

    /**
     * Invoke to tell listeners that an atomic test was ignored.
     *
     * @param description the description of the ignored test
     */
    @Override
	public void fireTestIgnored(final Description description) {
    	Object [] args = new Object[1];
    	args[0] = description;
    	notifiedEvents.add(new Notification(testIgnoredMethod, args));
    }

    /**
     * Do not invoke.
     */
    @Override
	public void fireTestRunFinished(final Result result) {
    	Object [] args = new Object[1];
    	args[0] = result;
    	notifiedEvents.add(new Notification(testRunFinishedMethod, args));
    }

    /**
     * Do not invoke.
     */
    @Override
	public void fireTestRunStarted(final Description description) {
    	Object [] args = new Object[1];
    	args[0] = description;
    	notifiedEvents.add(new Notification(testRunStartedMethod, args));
    }

    /**
     * Invoke to tell listeners that an atomic test is about to start.
     *
     * @param description the description of the atomic test (generally a class and method name)
     * @throws StoppedByUserException thrown if a user has requested that the test run stop
     */
    @Override
	public void fireTestStarted(final Description description) throws StoppedByUserException {
    	Object [] args = new Object[1];
    	args[0] = description;
    	notifiedEvents.add(new Notification(testStartedMethod, args));
    }

    /**
     * Invoke to tell listeners that a test suite is about to finish. Always invoke
     * this method if you invoke {@link #fireTestSuiteStarted(Description)}
     * as listeners are likely to expect them to come in pairs.
     *
     * @param description the description of the suite test (generally a class name)
     * @since 4.13
     */
    @Override
	public void fireTestSuiteFinished(final Description description) {
    	Object [] args = new Object[1];
    	args[0] = description;
    	notifiedEvents.add(new Notification(testSuiteFinishedMethod, args));
    }

    /**
     * Invoke to tell listeners that a test suite is about to start. Runners are
     * strongly encouraged--but not required--to call this method. If this 
     * method is called for a given {@link Description} then 
     * {@link #fireTestSuiteFinished(Description)} MUST be called for the same 
     * {@code Description}.
     *
     * @param description the description of the suite test (generally a class 
     * 	name)
     * @since 4.13
     */
    @Override
	public void fireTestSuiteStarted(final Description description) {
    	Object [] args = new Object[1];
    	args[0] = description;
    	notifiedEvents.add(new Notification(testSuiteStartedMethod, args));
    }

    /**
     * Unused in this implementation
     */
    @Override
	public void removeListener(RunListener listener) {
    }
}
