/*******************************************************************************
 * Copyright (c) 2020 Handy Tools for Distributed Computing (HanDist) project.
 *
 * This program and the accompanying materials are made available to you under 
 * the terms of the Eclipse Public License 1.0 which accompanies this 
 * distribution, and is available at https://www.eclipse.org/legal/epl-v10.html
 *
 * SPDX-License-Identifier: EPL-1.0
 *******************************************************************************/
package handist.mpijunit;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.lang.ProcessBuilder.Redirect;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

/**
 * Custom Junit {@link Runner} which uses MPI to run the test class it receives. 
 * This implementation relies on a {@link ProcessBuilder} to launch a <em>mpirun
 * </em> process which will run the tests with the specified number of ranks. 
 * Test methods are therefore executed in parallel and can communicate using 
 * MPI.
 * <p>
 * After the test methods have run, the results of the tests of each mpi rank
 * are written to files on the system. This class then parses these files and
 * aggregates the test results before transmitting them to the normal Junit4
 * notification mechanism. 
 * <p>
 * General configuration options for the {@link MpiRunner} can be found in 
 * {@link Configuration}. For configuration specific to individual test classes,
 * refer to the {@link MpiConfig} annotation. 
 *  
 * @author Patrick Finnerty
 *
 */
public class MpiRunner extends Runner {

	/** Class used as the main class for the child MPI processes */
	String launcherClass;

	/** Directory path to the notification */
	String pathToNotifications;

	/** Number of ranks desired */
	int processCount;

	/** Class under test */
	Class<?> testClass;

	/**
	 * Constructor with the class to test provided as parameter
	 * 
	 * @param klass the class to test
	 * @throws InitializationError if the given test class cannot be run by the 
	 * 	MpiRunner, i.e. if it does not have a {@link MpiConfig} annotation for 
	 *  instance
	 */
	public MpiRunner(Class<?> klass) throws InitializationError {
		super();
		testClass = klass;
		MpiConfig [] configs = testClass.getAnnotationsByType(MpiConfig.class);
		if (configs.length > 0) {
			setupConfiguration(configs[0]);
		} else {
			throw new InitializationError("The test class " + testClass + 
					" is missing a @MpiConfig annotation.");
		}
	}

	/**
	 * Creates a tree description of the tests to run. As per normal Junit 
	 * tests, a branch is created for each test method in the test class. 
	 * Usually, these "test" descriptions are the leaves of the test class 
	 * description. However with the {@link MpiRunner}, we add a description to
	 * each test method description for each mpi process. This groups the 
	 * results from each mpi process by test methods.
	 */
	@Override
	public Description getDescription() {
		if (System.getProperty(Configuration.PARSE_NOTIFICATIONS) != null) {
			try {
				return new BlockJUnit4ClassRunner(testClass).getDescription();
			} catch (InitializationError e) {
				e.printStackTrace();
			}
			return null;
		} else {
			Description toReturn = Description.createSuiteDescription(testClass);

			for (Method m : testClass.getMethods()) {
				if (m.isAnnotationPresent(Test.class)) {
					Description methodDescription = Description.createTestDescription(testClass, m.getName());
					for (int i = 0; i < processCount; i++) {
						Description leafDescription = Description.createTestDescription(testClass, "[" + i + "] " + m.getName()); 
						methodDescription.addChild(leafDescription);

					}
					toReturn.addChild(methodDescription);
				}
			}

			return toReturn;
		}
	}

	/**
	 * Attempts to open the file that should have been created by the 
	 * {@link ToFileRunNotifier} of the specified rank. 
	 *  
	 * @param rank the integer indicating the process whose notifications
	 * 	need to be retrieved
	 * @return an {@link ArrayList} containing the {@link Notification} that 
	 *  were made by the target rank during the test execution
	 */
	@SuppressWarnings("unchecked")
	private ArrayList<Notification> getNotifications(int rank)
			throws Exception {
		String notificationFileName = testClass.getCanonicalName() + "_" + 
				rank;
		File toOpen = new File(pathToNotifications, notificationFileName);

		boolean keepFile = Boolean.parseBoolean(System.getProperty(
				Configuration.KEEP_NOTIFICATIONS, 
				Configuration.KEEP_NOTIFICATIONS_DEFAULT));
		if (!keepFile) {
			toOpen.deleteOnExit();
		}
		ObjectInputStream inStream = 
				new ObjectInputStream(new FileInputStream(toOpen));
		Object o = inStream.readObject();
		inStream.close();
		return (ArrayList<Notification>) o;
	}

	/**
	 * Builds the command and launches a process that will run the test class.
	 * 
	 * @throws Exception if an exception occurs when launching the process
	 */
	private void launchMpiProcess()	throws Exception {
		final ArrayList<String> command = new ArrayList<>();

		String mpiImplementation = System.getProperty(Configuration.MPI_IMPL, 
				Configuration.MPI_IMPL_DEFAULT);

		String mpirunOptions = System.getProperty(Configuration.MPIRUN_OPTION);
		String mpjHome = System.getenv("MPJ_HOME");
		String sep = File.separator;

		// Depending on the implementation, build a different command
		switch (mpiImplementation) {
		case Configuration.MPI_IMPL_NATIVE:
			command.add("mpirun");
			if (mpirunOptions!=null) {
				command.add(mpirunOptions);
			}
			command.add("-np");
			command.add(String.valueOf(processCount));
			command.add("java");
			// Transmit the potential java agents
			for ( String s :ManagementFactory.getRuntimeMXBean().getInputArguments() ) {
				if (s.startsWith("-javaagent")) {
					s = s.replace("\\\\", "\\");
					command.add(s);
				}	
			}
			command.add("-Duser.dir=" + System.getProperty("user.dir"));
			String javaLibraryPath = System.getProperty(Configuration.JAVA_LIBRARY_PATH);
			if (javaLibraryPath != null) {
				command.add("-Djava.library.path="+ javaLibraryPath);
			}
			command.add("-cp");
			command.add(System.getProperty("java.class.path"));
			break;
		case Configuration.MPI_IMPL_MPJNATIVE:
			if (mpjHome == null) {
				throw new Exception("MPJ_HOME was not set. Cannot run the tests");
			}
			command.add("mpirun");
			if (mpirunOptions!=null) {
				command.add(mpirunOptions);
			}
			command.add("-np");
			command.add(String.valueOf(processCount));
			command.add("java");
			// Transmit the potential java agents
			for ( String s :ManagementFactory.getRuntimeMXBean().getInputArguments() ) {
				if (s.startsWith("-javaagent")) {
					s = s.replace("\\\\", "\\");
					command.add(s);
				}	
			}
			command.add("-Duser.dir=" + System.getProperty("user.dir"));

			if (!mpjHome.endsWith(sep)) {
				mpjHome += sep;
			}
			String pathToNativeLib= mpjHome + "lib";
			command.add("-Djava.library.path="+pathToNativeLib);
			command.add("-cp");
			command.add(System.getProperty("java.class.path"));
			break;
		case Configuration.MPI_IMPL_MPJMULTICORE:
			command.add("java");
			// 			Transmit the potential java agents
			//==============================================================================			
			//			Due to the implementation of MPJ-Express (which internally also 
			//			launches a Process), the java agents are not transmitted to the 
			//			process that actually runs the tests. It is therefore pointless to
			//			transmit the java agents here. 
			//==============================================================================
			//			for ( String s :ManagementFactory.getRuntimeMXBean().getInputArguments() ) {
			//				if (s.startsWith("-javaagent")) {
			//					s = s.replace("\\\\", "\\");
			//					command.add(s);
			//				}	
			//			}
			command.add("-Duser.dir=" + System.getProperty("user.dir"));
			command.add("-jar");

			// Assemble the path to starter.jar of the MPJ library
			if (mpjHome == null) {
				throw new Exception("MPJ_HOME was not set. Cannot run the tests");
			}
			if (!mpjHome.endsWith(sep)) {
				mpjHome += sep;
			}
			String pathToStarterJar = mpjHome + "lib" + sep + "starter.jar";
			command.add(pathToStarterJar);

			command.add("-cp");
			command.add(System.getProperty("java.class.path"));
			command.add("-np");
			command.add(String.valueOf(processCount));
			break;
		default:
			throw new Exception("Unknown MPI implementation <" + 
					mpiImplementation + ">");
		}
		// Common to all configurations are the two last parameters(three 
		// parameters if the optional path to the notification files was set)
		command.add(launcherClass);

		// In the case of MPJ native implementation, insert 3 arguments
		if (Configuration.MPI_IMPL_MPJNATIVE.equals(mpiImplementation)) {
			// Three parameters for the mpj native configuration
			command.add("0");
			command.add("0");
			command.add("native");
		}

		command.add(testClass.getCanonicalName());
		pathToNotifications = System.getProperty(Configuration.NOTIFICATIONS_PATH);
		if (pathToNotifications != null) {
			new File(pathToNotifications).mkdirs();
			command.add(pathToNotifications);
		}

		//System.out.println("[MpiRunner] Effective Command " + String.join(" ", command));

		ProcessBuilder pb = new ProcessBuilder(command);
		pb.redirectOutput(Redirect.INHERIT);
		pb.redirectError(Redirect.INHERIT);

		Process p = pb.start();
		p.waitFor();
	}

	/**
	 * Posts dummy test results to the notifier in case something went wrong in
	 * this class's runtime when trying to run the tests or when parsing and 
	 * combining the test results of each rank.
	 * <p>
	 * Users can choose the behavior of this method by defining the variable 
	 * {@link MpiApgasRunner#ifRuntimeProblemShow}.
	 * 
	 * @param notifier the notifier to which the test results need to be reported
	 * @param e the exception that caused the issue
	 */
	private void notificationsForRuntimeProblem(RunNotifier notifier, Exception e) {
		String action = System.getProperty(Configuration.ACTION_ON_ERROR, Configuration.ACTION_ON_ERROR_DEFAULT);
		if (action.equals(Configuration.ON_ERROR_SILENT)) {
			return;
		}

		Exception failureCause = new Exception("Unable to produce results for this test");
		failureCause.initCause(e);
		notifier.fireTestSuiteStarted(getDescription());
		for (Method m : testClass.getMethods()) {
			if (m.isAnnotationPresent(Test.class)) {
				Description testDescription = Description.
						createTestDescription(testClass, m.getName());

				switch (action) {
				case Configuration.ON_ERROR_ERROR:
					notifier.fireTestStarted(testDescription);
					notifier.fireTestFailure(new Failure(testDescription, 
							failureCause));
					notifier.fireTestFinished(testDescription);
					break;
				case Configuration.ON_ERROR_SKIP:
				default:
					notifier.fireTestIgnored(testDescription);
					break;
				}

			}
		}
		notifier.fireTestSuiteFinished(getDescription());
	}

	/**
	 * Parses the test results of each rank and transmits them to the provided 
	 * {@link RunNotifier}.  
	 *  
	 * @param notifier the notifier to which the aggregated test results need to
	 * 	be transmitted
	 */
	private void parseTestResults(RunNotifier notifier) throws Exception {
		// Obtain the notifications of every process
		// If the mpirunner.parseNotifications was defined, restrict to the 
		// given rank
		List<List<Notification>> processesNotifications = new ArrayList<>(processCount);
		int firstRankToProcess = 0;
		int lastRankToProcess = processCount-1;

		final boolean SINGLE_HOST_PARSING = System.getProperty(Configuration.PARSE_NOTIFICATIONS) != null;

		if (SINGLE_HOST_PARSING) {
			firstRankToProcess = Integer.parseInt(System.getProperty(Configuration.PARSE_NOTIFICATIONS));
			lastRankToProcess = firstRankToProcess;
		} 

		for (int i = firstRankToProcess; i <= lastRankToProcess; i++ ) {
			processesNotifications.add(i, getNotifications(i));
		}

		// Process the notifications of each list in FIFO order

		// The first notification on each host should be a TestSuiteStarted
		// We can pop it and put our own TestSuiteDescription instead
		for (List<Notification> l : processesNotifications) {
			l.remove(0);
			l.remove(l.size()-1);
		}

		for (int i = firstRankToProcess; i <= lastRankToProcess; i++ ) {
			List<Notification> processNotifications = processesNotifications.get(i);

			for (Notification n : processNotifications) {
				Class<?> paramClass = n.parameters[0].getClass();
				//Reconstitute the method call
				Method m = RunNotifier.class.getDeclaredMethod(n.method, paramClass);

				if (!SINGLE_HOST_PARSING) { // We modify the calls to distinguish results of != places
					switch (n.method) {
					case "fireTestFinished":
					case "fireTestStarted":
					case "fireTestIgnored": 
						Description d = (Description) n.parameters[0];
						Description toUse = Description.createTestDescription(testClass, "[" + i + "] " + d.getMethodName());
						n.parameters[0] = toUse;
						break;
					case "fireTestAssumptionFailed":
					case "fireTestFailure":
						Failure f = (Failure) n.parameters[0];
						Description failDescription = f.getDescription();
						Description descriptionToUse = Description.
								createTestDescription(testClass, 
										"[" + i + "] " + failDescription.getMethodName());

						Failure failureToUse = new Failure(descriptionToUse , f.getException());
						n.parameters[0] = failureToUse;
						break;
					default:
					}
				}
				m.invoke(notifier, n.parameters);
			}
		}
	}

	/**
	 * Launches a MPI process with the test class to run the tests. Then parses
	 * the results of each rank and transmits them to the given 
	 * {@link RunNotifier}. 
	 */
	@Override
	public void run(RunNotifier notifier) {
		pathToNotifications = System.getProperty(Configuration.NOTIFICATIONS_PATH);
		boolean isDryRun = Boolean.parseBoolean(System.getProperty(Configuration.DRY_RUN, Configuration.DRY_RUN_DEFAULT));

		if (isDryRun) {
			// Override the value KEEP_NOTIFICATIONS to keep the notifications files
			System.setProperty(Configuration.KEEP_NOTIFICATIONS, "true");
		} else {
			// Launch the mpirun process for the test class
			try {
				launchMpiProcess();			
			} catch (Exception e) {
				notificationsForRuntimeProblem(notifier, e);
				return;
			}
		}

		try {
			parseTestResults(notifier);
		} catch (Exception e) {
			// Something went wrong during the parsing
			notificationsForRuntimeProblem(notifier, e);
		}	
	}

	/**
	 * Helper method that uses the provided {@link MpiConfig} to set some 
	 * members of this class. 
	 * 
	 * @param cfg configuration to use to run the tests (provided by the test 
	 * class) 
	 */
	private void setupConfiguration(MpiConfig cfg) {
		processCount = cfg.ranks();
		launcherClass = cfg.launcher().getCanonicalName();
	}
}