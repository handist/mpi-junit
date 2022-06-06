/*******************************************************************************
 * Copyright (c) 2021 Handy Tools for Distributed Computing (HanDist) project.
 *
 * This program and the accompanying materials are made available to you under
 * the terms of the Eclipse Public License 1.0 which accompanies this
 * distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 *
 * SPDX-License-Identifier: EPL-1.0
 ******************************************************************************/
package handist.mpijunit;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.lang.ProcessBuilder.Redirect;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.Parameterized;
import org.junit.runners.Suite;
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
 * General configuration options for the {@link ParameterizedMpi} can be found
 * in {@link Configuration}. For configuration specific to individual test
 * classes, refer to the {@link MpiConfig} annotation.
 * 
 * @author Patrick Finnerty
 *
 */
public class ParameterizedMpi extends Runner {

	/**
	 * Annotation used to set some environment settings for test classes that use
	 * the {@link ParameterizedMpi} runner. At a minimum, test classes have to
	 * specify how many processes are desired for the test. You can also specify a
	 * specific launcher if you need some specific setup or initialization that
	 * involves multiple hosts before launching the tests.
	 *
	 * @author Patrick Finnerty
	 *
	 */
	@Documented
	@Retention(RUNTIME)
	@Target(TYPE)
	public @interface ParameterizedMpiConfig {
		/**
		 * Indicates which launcher should be used. The launcher is the class whose main
		 * method is responsible for launching the tests in each MPI process. By
		 * default, class {@link ParameterizedMpiTestLauncher} is used.
		 * <p>
		 * Using a different class than the default may be useful if you have some
		 * specific setup to do before launching the Junit tests runtime. One such
		 * specific launcher we provide in this library is the
		 * {@link MpiApgasTestLauncher} which sets up the APGAS runtime before launching
		 * the tests.
		 * <p>
		 * Of course you can also use your own custom launcher. If you choose to do so,
		 * make sure that your implementation takes the same arguments as the
		 * implementations we provide and creates notification files with the same name.
		 *
		 * @return the class whose main will be launched by all the parallel processes
		 */
		Class<?> launcher() default ParameterizedMpiTestLauncher.class;

		/**
		 * Indicates how many ranks (parallel processes) are desired to run the test
		 * class. There are no default value for this setting and it needs to be set by
		 * the user.
		 * 
		 * @return number of MPI processes to be launched
		 */
		int ranks();

		/**
		 * Timeout after which the spawned MPI process will be killed. The default time
		 * unit is the second. This can be changed by also indicating
		 * {@link #timeUnit()}. By default returns {@value 0l} disabling the timeout
		 * feature. Users should be careful to allocate enough time for all their tests
		 * to run. This is only meant to kill a process which has trouble terminating
		 * 
		 * @return the timeout after which the spawned MPI process should be killed.
		 */
		long timeout() default 0l;

		/**
		 * Unit used to describe the timeout of {@link #timeout()}. By default is
		 * {@link TimeUnit#SECONDS}. Setting this parameter without setting
		 * {@link #timeout()} has no effect.
		 * 
		 * @return
		 */
		TimeUnit timeUnit() default TimeUnit.SECONDS;
	}

	/** Field describing the private Field "runners" of class {@link Suite} */
	private static final Field runnersField;

	static {
		Field runnersFieldOfClassSuite;
		try {
			runnersFieldOfClassSuite = Suite.class.getDeclaredField("runners");
			runnersFieldOfClassSuite.setAccessible(true);
			runnersField = runnersFieldOfClassSuite;
		} catch (NoSuchFieldException | SecurityException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	/** Successive parameterized configurations to test */
	final List<Runner> configurations;

	/** Class used as the main class for the child MPI processes */
	String launcherClass;

	/** Directory path to the notification */
	String pathToNotifications;

	/** Number of ranks desired */
	int processCount;
	/** Class under test */
	final Class<?> testClass;
	/**
	 * Timeout after which the spawned process should be killed. Is disabled if the
	 * value is negative or null.
	 */
	long timeout;
	/** Time unit for {@link #timeout} */
	TimeUnit timeUnit;

	/**
	 * Constructor with the class to test provided as parameter
	 *
	 * @param klass the class to test
	 * @throws InitializationError if the given test class cannot be run by the
	 *                             MpiRunner, i.e. if it does not have a
	 *                             {@link MpiConfig} annotation for instance
	 */
	@SuppressWarnings("unchecked")
	public ParameterizedMpi(Class<?> klass) throws InitializationError {
		super();
		testClass = klass;
		final ParameterizedMpiConfig[] configs = testClass.getAnnotationsByType(ParameterizedMpiConfig.class);
		if (configs.length > 0) {
			setupConfiguration(configs[0]);
		} else {
			throw new InitializationError(
					"The test class " + testClass + " is missing a @ParameterizedMpiConfig annotation.");
		}
		try {
			Parameterized parameterizedRunner = new Parameterized(klass);
			configurations = (List<Runner>) runnersField.get(parameterizedRunner);
		} catch (Throwable t) {
			throw new InitializationError(t);
		}
	}

	/**
	 * Creates a tree description of the tests to run. As per normal Junit tests, a
	 * branch is created for each test method in the test class. Usually, these
	 * "test" descriptions are the leaves of the test class description. However
	 * with the {@link ParameterizedMpi}, we add a description to each test method
	 * description for each mpi process. This groups the results from each mpi
	 * process by test methods.
	 */
	@Override
	public Description getDescription() {
		if (System.getProperty(Configuration.PARSE_NOTIFICATIONS) != null) {
			try {
				return new Parameterized(testClass).getDescription();
			} catch (final Throwable e) {
				e.printStackTrace();
			}
			return null;
		} else {
			final Description toReturn = Description.createSuiteDescription(testClass);

			for (Runner r : configurations) {
				Description parameterDescription = Description.createSuiteDescription(r.getDescription().toString());

				for (final Method m : testClass.getMethods()) {
					if (m.isAnnotationPresent(Test.class)) {
						final Description methodDescription = Description.createSuiteDescription(m.getName());
						for (int i = 0; i < processCount; i++) {
							final Description leafDescription = Description.createTestDescription(testClass,
									"[" + i + "] " + m.getName());
							methodDescription.addChild(leafDescription);
						}
						parameterDescription.addChild(methodDescription);
					}
				}

				toReturn.addChild(parameterDescription);
			}

			return toReturn;
		}
	}

	/**
	 * Attempts to open the file that should have been created by the
	 * {@link ToFileRunNotifier} of the specified rank.
	 * 
	 * @param rank   the integer indicating the process whose notifications need to
	 *               be retrieved
	 * @param config the index of the parameter whose test result needs to be
	 *               retrived
	 * @return an {@link ArrayList} containing the {@link Notification} that were
	 *         made by the target rank during the test execution
	 */
	@SuppressWarnings("unchecked")
	private ArrayList<Notification> getNotifications(int rank, int config) throws Exception {
		final String notificationFileName = testClass.getCanonicalName() + "_" + config + "_" + rank;
		final File toOpen = new File(pathToNotifications, notificationFileName);

		final boolean keepFile = Boolean.parseBoolean(
				System.getProperty(Configuration.KEEP_NOTIFICATIONS, Configuration.KEEP_NOTIFICATIONS_DEFAULT));
		if (!keepFile) {
			toOpen.deleteOnExit();
		}
		final ObjectInputStream inStream = new ObjectInputStream(new FileInputStream(toOpen));
		final Object o = inStream.readObject();
		inStream.close();
		return (ArrayList<Notification>) o;
	}

	/**
	 * Builds the command and launches a process that will run the test class.
	 *
	 * @param i the index of the configuration to launch
	 * @throws Exception if an exception occurs when launching the process
	 */
	private void launchMpiProcess(int i) throws Exception {
		final ArrayList<String> command = new ArrayList<>();

		final String mpiImplementation = System.getProperty(Configuration.MPI_IMPL, Configuration.MPI_IMPL_DEFAULT);

		final String mpirunOptions = System.getProperty(Configuration.MPIRUN_OPTION);
		String mpjHome = System.getenv("MPJ_HOME");
		final String sep = File.separator;

		// Depending on the implementation, build a different command
		switch (mpiImplementation) {
		case Configuration.MPI_IMPL_OPENMPI:
		case Configuration.MPI_IMPL_NATIVE:
			command.add("mpirun");
			if (mpirunOptions != null) {
				// We need to split the mpirun options at each space and add the
				// resulting strings one by one
				for (final String s : mpirunOptions.split(" ")) {
					command.add(s);
				}
			}
			command.add("-np");
			command.add(String.valueOf(processCount));
			command.add("java");
			// Transmit the potential java agents
			for (String s : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
				if (s.startsWith("-javaagent")) {
					s = s.replace("\\\\", "\\");
					command.add(s);
				}
			}
			command.add("-Duser.dir=" + System.getProperty("user.dir"));
			final String javaLibraryPath = System.getProperty(Configuration.JAVA_LIBRARY_PATH);
			if (javaLibraryPath != null) {
				command.add("-Djava.library.path=" + javaLibraryPath);
			}
			command.add("-cp");
			command.add(System.getProperty("java.class.path"));
			break;
		case Configuration.MPI_IMPL_MPJNATIVE:
			if (mpjHome == null) {
				throw new Exception("MPJ_HOME was not set. Cannot run the tests");
			}
			command.add("mpirun");
			if (mpirunOptions != null) {
				// We need to split the mpirun options at each space and add the
				// resulting strings one by one
				for (final String s : mpirunOptions.split(" ")) {
					command.add(s);
				}
			}
			command.add("-np");
			command.add(String.valueOf(processCount));
			command.add("java");
			// Transmit the potential java agents
			for (String s : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
				if (s.startsWith("-javaagent")) {
					s = s.replace("\\\\", "\\");
					command.add(s);
				}
			}
			command.add("-Duser.dir=" + System.getProperty("user.dir"));

			if (!mpjHome.endsWith(sep)) {
				mpjHome += sep;
			}
			final String pathToNativeLib = mpjHome + "lib";
			command.add("-Djava.library.path=" + pathToNativeLib);
			command.add("-cp");
			command.add(System.getProperty("java.class.path"));
			break;
		case Configuration.MPI_IMPL_MPJMULTICORE:
			command.add("java");
			// Transmit the potential java agents
			// ==============================================================================
			// Due to the implementation of MPJ-Express (which internally also
			// launches a Process), the java agents are not transmitted to the
			// process that actually runs the tests. It is therefore pointless to
			// transmit the java agents here.
			// ==============================================================================
			// for ( String s :ManagementFactory.getRuntimeMXBean().getInputArguments() ) {
			// if (s.startsWith("-javaagent")) {
			// s = s.replace("\\\\", "\\");
			// command.add(s);
			// }
			// }
			command.add("-Duser.dir=" + System.getProperty("user.dir"));
			command.add("-jar");

			// Assemble the path to starter.jar of the MPJ library
			if (mpjHome == null) {
				throw new Exception("MPJ_HOME was not set. Cannot run the tests");
			}
			if (!mpjHome.endsWith(sep)) {
				mpjHome += sep;
			}
			final String pathToStarterJar = mpjHome + "lib" + sep + "starter.jar";
			command.add(pathToStarterJar);

			command.add("-cp");
			command.add(System.getProperty("java.class.path"));
			command.add("-np");
			command.add(String.valueOf(processCount));
			break;
		default:
			throw new Exception("Unknown MPI implementation <" + mpiImplementation + ">");
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

		// First argument to MPI process main: the class under test
		command.add(testClass.getCanonicalName());
		// Second argument to MPI process main: the index of the @Parameter to run
		command.add(Integer.toString(i));

		// Third and optional argument to the MPI process main: directory in which
		// to place the notification file
		pathToNotifications = System.getProperty(Configuration.NOTIFICATIONS_PATH);
		if (pathToNotifications != null) {
			new File(pathToNotifications).mkdirs();
			command.add(pathToNotifications);
		}

		if (Boolean.parseBoolean(System.getProperty(Configuration.VERBOSE, Configuration.VERBOSE_DEFAULT))) {
			System.out.println("[MpiRunner] Launching Command: " + String.join(" ", command));
		}

		final ProcessBuilder pb = new ProcessBuilder(command);
		pb.redirectOutput(Redirect.INHERIT);
		pb.redirectError(Redirect.INHERIT);

		final Process p = pb.start();
		if (0 < timeout) {
			boolean cleanTermination = p.waitFor(timeout, timeUnit);
			if (!cleanTermination) {
				System.err.println(
						"Spawned MPI process did not terminate within the " + timeout + " " + timeUnit + " allocated");
				System.err.println("Killing process");
				p.destroyForcibly();
			}
		} else {
			p.waitFor();
		}
	}

	/**
	 * Posts dummy test results to the notifier in case something went wrong in this
	 * class's runtime when trying to run the tests or when parsing and combining
	 * the test results of each rank.
	 * <p>
	 * Users can choose the behavior of this method by defining the variable
	 * {@link MpiApgasRunner#ifRuntimeProblemShow}.
	 *
	 * @param notifier the notifier to which the test results need to be reported
	 * @param e        the exception that caused the issue
	 * @param i        the index of the configuration for which a problem occurred
	 */
	private void notificationsForRuntimeProblem(RunNotifier notifier, Exception e, int i) {
		final String action = System.getProperty(Configuration.ACTION_ON_ERROR, Configuration.ACTION_ON_ERROR_DEFAULT);
		if (action.equals(Configuration.ON_ERROR_SILENT)) {
			return;
		}

		final Exception failureCause = new Exception("Unable to produce results for this test");
		failureCause.initCause(e);
		notifier.fireTestSuiteStarted(configurations.get(i).getDescription());
		for (final Method m : testClass.getMethods()) {
			if (m.isAnnotationPresent(Test.class)) {
				final Description testDescription = Description.createTestDescription(testClass.getName(),
						m.getName() + configurations.get(i).getDescription().getDisplayName()); // Description.createTestDescription(testClass,
																								// m.getName());

				switch (action) {
				case Configuration.ON_ERROR_ERROR:
					notifier.fireTestStarted(testDescription);
					notifier.fireTestFailure(new Failure(testDescription, failureCause));
					notifier.fireTestFinished(testDescription);
					break;
				case Configuration.ON_ERROR_SKIP:
				default:
					notifier.fireTestIgnored(testDescription);
					break;
				}

			}
		}
		notifier.fireTestSuiteFinished(configurations.get(i).getDescription());
	}

	/**
	 * Parses the test results of each rank and transmits them to the provided
	 * {@link RunNotifier}.
	 * 
	 * @param notifier the notifier to which the aggregated test results need to be
	 *                 transmitted
	 * @param config   the index of the configuration considered for parsing
	 */
	private void parseTestResults(RunNotifier notifier, int config) throws Exception {
		// Obtain the notifications of every process
		// If the mpirunner.parseNotifications was defined, restrict to the given rank
		final List<List<Notification>> processesNotifications = new ArrayList<>(processCount);
		int firstRankToProcess = 0;
		int lastRankToProcess = processCount - 1;

		final boolean SINGLE_HOST_PARSING = System.getProperty(Configuration.PARSE_NOTIFICATIONS) != null;

		if (SINGLE_HOST_PARSING) {
			firstRankToProcess = Integer.parseInt(System.getProperty(Configuration.PARSE_NOTIFICATIONS));
			lastRankToProcess = firstRankToProcess;
		}

		for (int i = firstRankToProcess; i <= lastRankToProcess; i++) {
			processesNotifications.add(i, getNotifications(i, config));
		}

		// Process the notifications of each list in FIFO order
		for (int i = firstRankToProcess; i <= lastRankToProcess; i++) {
			final List<Notification> processNotifications = processesNotifications.get(i);

			for (final Notification n : processNotifications) {
				final Class<?> paramClass = n.parameters[0].getClass();
				// Reconstitute the method call
				final Method m = RunNotifier.class.getDeclaredMethod(n.method, paramClass);

				if (!SINGLE_HOST_PARSING) { // We modify the calls to distinguish results of != places
					switch (n.method) {
					case "fireTestFinished":
					case "fireTestStarted":
					case "fireTestIgnored":
						final Description d = (Description) n.parameters[0];
						final Description toUse = Description.createTestDescription(testClass,
								"[" + i + "] " + d.getMethodName());
						n.parameters[0] = toUse;
						break;
					case "fireTestAssumptionFailed":
					case "fireTestFailure":
						final Failure f = (Failure) n.parameters[0];
						final Description failDescription = f.getDescription();
						final Description descriptionToUse = Description.createTestDescription(testClass,
								"[" + i + "] " + failDescription.getMethodName());

						final Failure failureToUse = new Failure(descriptionToUse, f.getException());
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
	 * Launches a MPI process with the test class to run the tests. Then parses the
	 * results of each rank and transmits them to the given {@link RunNotifier}.
	 */
	@Override
	public void run(RunNotifier notifier) {
		pathToNotifications = System.getProperty(Configuration.NOTIFICATIONS_PATH);
		final boolean isDryRun = Boolean
				.parseBoolean(System.getProperty(Configuration.DRY_RUN, Configuration.DRY_RUN_DEFAULT));

		if (isDryRun) {
			// Override the value KEEP_NOTIFICATIONS to keep the notifications files
			System.setProperty(Configuration.KEEP_NOTIFICATIONS, "true");
		}

		// for each configuration (we use the int index to designate a configuration)
		for (int i = 0; i < configurations.size(); i++) {

			try {
				if (!isDryRun) {
					// Launch the mpirun process for the test class
					launchMpiProcess(i);
				}

				parseTestResults(notifier, i);
			} catch (final Exception e) {
				// Something went wrong during the parsing
				notificationsForRuntimeProblem(notifier, e, i);
			}
		}
	}

	/**
	 * Helper method that uses the provided {@link MpiConfig} to set some members of
	 * this class.
	 *
	 * @param cfg configuration to use to run the tests (provided by the test class)
	 */
	private void setupConfiguration(ParameterizedMpiConfig cfg) {
		processCount = cfg.ranks();
		launcherClass = cfg.launcher().getCanonicalName();
		timeout = cfg.timeout();
		timeUnit = cfg.timeUnit();
	}
}
