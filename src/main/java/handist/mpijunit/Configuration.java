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

/**
 * Contains various {@link String} constants used to define the behavior of the
 * {@link MpiRunner}.
 * <p>
 * Some settings (such as the number of MPI processes to use) are specific to
 * each test class and need to be specified using the {@link MpiConfig}
 * annotations on your individual test classes. They cannot be set using the
 * options presented in this class.
 *
 * @author Patrick Finnerty
 *
 */
public class Configuration {

	/**
	 * Option defining what should be done in case the {@link MpiRunner} encounters
	 * an Exception when running the tests. These usually happen due to classpath
	 * issues or the unavaibility of the <em>mpirun</em> command. This option does
	 * not change the results of the tests that run. It is here to decide what to
	 * present if the {@link MpiRunner} was unable to run the tests successfully.
	 * <p>
	 * This option should be set with <em>-Dmpirunner.actionOnError=action</em>.
	 * Possible actions are:
	 * <ul>
	 * <li>{@value #ON_ERROR_ERROR} which will marks the tests results as "error".
	 * This is the default behavior.
	 * <li>{@value #ON_ERROR_SKIP} which will mark the tests as skipped (as if they
	 * were marked with the @Ignore annotation inside your test class).
	 * <li>{@value #ON_ERROR_SILENT} which does not transmit any result for the
	 * tests
	 * </ul>
	 */
	public static final String ACTION_ON_ERROR = "mpirunner.actionOnError";
	/**
	 * Possible setting for {@link #ACTION_ON_ERROR}, shows tests as "Error"
	 */
	public static final String ON_ERROR_ERROR = "error";
	/**
	 * Possible setting for {@link #ACTION_ON_ERROR}, does not show any result
	 */
	public static final String ON_ERROR_SILENT = "silent";
	/**
	 * Possible setting for {@link #ACTION_ON_ERROR}, shows tests as "Skipped"
	 */
	public static final String ON_ERROR_SKIP = "skip";
	/**
	 * Default action for {@link #ACTION_ON_ERROR}, is {@link #ON_ERROR_ERROR}
	 */
	public static final String ACTION_ON_ERROR_DEFAULT = ON_ERROR_ERROR;

	/**
	 * Command line option to choose whether the <em>mpirun</em> process should
	 * actually be launched. Can be set by the JVM argument
	 * <em>-Dmpirunner.dryRun=true/false</em>. By default, it is set to
	 * <code>false</code>, meaning that the {@link MpiRunner} will try to launch
	 * multiple processes to run the tests.
	 * <p>
	 * However, if this option is set to <code>true</code>, the {@link MpiRunner}
	 * will only attempt to parse the {@link Notification} recordings. In addition,
	 * the recordings will be kept on the file system regardless of what is set for
	 * option {@link #KEEP_NOTIFICATIONS}.
	 * <p>
	 * This option can be useful if you want to parse the test results from a
	 * different system on your local machine which may not be equipped with the
	 * software needed to run the tests.
	 */
	public static final String DRY_RUN = "mpirunner.dryRun";
	/** Default setting for {@link #DRY_RUN} */
	public static final String DRY_RUN_DEFAULT = "false";

	/**
	 * Command line option used to define the <em>-Djava.library.path</em> option of
	 * the mpi process launched by the {@link MpiRunner}. Set it by defining
	 * <em>-Dmpirunner.javaLibraryPath=path/to/native/libs</em>. This setting has no
	 * default value. If it is not set, no particular java library path will be set
	 * for the processes launched by the {@link MpiRunner}.
	 * <p>
	 * If you are using using a native implementation of MPI to run your tests, you
	 * almost certainly need to specify this option.
	 */
	public static final String JAVA_LIBRARY_PATH = "mpirunner.javaLibraryPath";

	/**
	 * Command line option to define the MPI implementation used to launch the mpi
	 * tests. Set this setting with <em>-Dmpirunner.mpiImpl=implementation</em>.
	 * <p>
	 * Currently supported implementations are:
	 * <ul>
	 * <li>{@value #MPI_IMPL_NATIVE} which relies on the
	 * <a href="https://sourceforge.net/projects/mpijava/">mpiJava library</a>
	 * <li>{@value #MPI_IMPL_MPJMULTICORE} which uses the
	 * <a href="http://mpj-express.org/">MPJ-Express</a> "multicore" implementation.
	 * (default value)
	 * <li>{@value #MPI_IMPL_MPJNATIVE} which uses the
	 * <a href="http://mpj-express.org/">MPJ-Express</a> "native" implementation.
	 * </ul>
	 * If you are using the {@link #MPI_IMPL_NATIVE} implementation, you can use the
	 * {@value #MPIRUN_OPTION} to specify options to the <em>mpirun</em> command.
	 */
	public static final String MPI_IMPL = "mpirunner.mpiImpl";

	/**
	 * Possible option for the MPI implementation used to launch the tests. When
	 * using the <em>native</em> implementation, be mindful of the fact you will
	 * need to provide the path to the shared objects bindings to the MPI library
	 * you are using with {@link #JAVA_LIBRARY_PATH}. You can also give additional
	 * arguments to the <em>mpirun</em> command by setting {@link #MPIRUN_OPTION}.
	 */
	public static final String MPI_IMPL_NATIVE = "native";

	/**
	 * Possible option for the MPI implementation used to launch the tests.
	 * <p>
	 * When using the MPJ multicore configuration, you will need to set
	 * <em>MPJ_HOME</em> as an environment variable as this is needed by the MPJ
	 * library to run correctly. We refer you to
	 * <a href="http://mpj-express.org/">the MPJ Express website</a> for downloads
	 * and documentation about this library.
	 */
	public static final String MPI_IMPL_MPJMULTICORE = "mpj-multicore";

	/**
	 * Possible option for the MPI implementation used to launch the tests.
	 * <p>
	 * When using the MPJ native configuration, you will need to set
	 * <em>MPJ_HOME</em> as an environment variable. It is also expected that you
	 * have library <em>mpj.jar</em> and <em>libnativempjdev.so</em> in directory
	 * <em>MPJ_HOME/lib</em>. We refer you to <a href="http://mpj-express.org/">the
	 * MPJ Express website</a> for downloads and documentation about this library.
	 */
	public static final String MPI_IMPL_MPJNATIVE = "mpj-native";

	/** Default setting for {@link #MPI_IMPL} : {@value #MPI_IMPL_DEFAULT} */
	public static final String MPI_IMPL_DEFAULT = MPI_IMPL_MPJMULTICORE;

	/**
	 * Settings to give options to the <em>mpirun</em> command when a
	 * <em>native</em> MPI implementation id used. Use
	 * <em>-Dmpirunner.mpirunOptions=some options</em> to set it.
	 * <p>
	 * By default, this option is not set (by default, no options are passed to the
	 * <em>mpirun</em> command except <em>-np X</em> where X is the number of
	 * processes requested by your test class, as defined in the {@link MpiConfig}
	 * annotation). You should not set option <em>-np X</em> using this option, it
	 * is done by the {@link MpiRunner}.
	 */
	public static final String MPIRUN_OPTION = "mpirunner.mpirunOptions";

	/**
	 * Option to specify the directory in which the notifications of each rank will
	 * be placed. Set it using
	 * <em>-Dmpirunner.notificationsPath=directory/to/notifications</em>. If you do
	 * not set this option, the working directory will be used.
	 * <p>
	 * If the directory does not exist, it will be created. However, note that the
	 * recording files are deleted by default by the {@link MpiRunner} after they
	 * are parsed. To keep the recordings, you also need to set
	 * {@link #KEEP_NOTIFICATIONS} to true.
	 */
	public static final String NOTIFICATIONS_PATH = "mpirunner.notificationsPath";

	/**
	 * Command line option used to choose whether the files containing the
	 * {@link Notification}s that are made to the RunNotifier in each of the MPI
	 * processes should be kept after the tests' execution. Set it by defining
	 * <em>-Dmpirunner.keepNotifications=true/false</em>. This option is set to
	 * <code>false</code> by default, meaning the notification files will deleted
	 * after they are parsed by the {@link MpiRunner}.
	 * <p>
	 * This can be useful if you want to parse the results on a different system
	 * then the one they were executed on. For instance, you may find it convenient
	 * to run your tests on a server with this option set to <code>true</code>,
	 * download the notification recordings and parse the results on your personal
	 * machine using the {@link #DRY_RUN} option.
	 */
	public static final String KEEP_NOTIFICATIONS = "mpirunner.keepNotifications";
	/**
	 * Default setting for {@link #KEEP_NOTIFICATIONS}, is
	 * {@value #KEEP_NOTIFICATIONS_DEFAULT} .
	 */
	public static final String KEEP_NOTIFICATIONS_DEFAULT = "false";

	/**
	 * Option used to parse only the results of a single rank. By default, the test
	 * results of all the processes are processed. By setting this option with a
	 * particular rank number, only the test results of this process will be parsed.
	 * <p>
	 * For instance, <em>-Dmpirunner.parseNotifications=0</em> will make the
	 * {@link MpiRunner} only look for the file with the test results of rank 0.
	 * <p>
	 * <em>NOTA BENE</em>: although a single notification file will ever be
	 * processed by the {@link MpiRunner} when using this option, the usual
	 * notification file naming convention (with the rank of the process as a
	 * suffix) still applies. If you are implementing your own Launcher and using
	 * this option, you still need to observe this naming convention.
	 */
	public static final String PARSE_NOTIFICATIONS = "mpirunner.parseNotifications";

	/**
	 * Enables verbose output of the {@link MpiRunner}. Verbose output is disabled
	 * by default, it can be activated by setting this property to "true".
	 */
	public static final String VERBOSE = "mpirunner.verbose";

	/**
	 * Default value for property {@link #VERBOSE}. Is "false".
	 */
	public static final String VERBOSE_DEFAULT = "false";
}
