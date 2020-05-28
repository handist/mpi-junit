package handist.mpijunit;

/**
 * Contains various {@link String} constants used to define the behavior of the
 * {@link MpiRunner}.  
 * <p>
 * Some settings (such as the number of mpi processes to use) are specific to 
 * each test class and need to be specified using the {@link MpiConfig} on your
 * test class. They cannot be set using the options presented in this class.
 * 
 * @author Patrick Finnerty
 *
 */
public class Configuration {

	/** 
	 * Option defining what should be done in case the MpiRunner encounters an 
	 * Exception when running the tests. Should be set with command-line option
	 * <em>-Dmpirunner.actionOnFAilure=action</em>. Possible actions are: 
	 * <ul>
	 * <li>{@value #ON_ERROR_ERROR} which marks the tests results as errors. 
	 * 	This is the default behavior.  
	 * <li>{@value #ON_ERROR_SKIP} which marks the tests as skipped as if they 
	 * were marked with the @Ignore annotation.
	 * <li>{@value #ON_ERROR_SILENT} which does not transmit any result for the 
	 * tests   
	 * </ul>
	 */
	public static final String ACTION_ON_ERROR = "mpirunner.actionOnError";
	/** Possible setting for {@link #ACTION_ON_ERROR}, show tests as "Error" */
	public static final String ON_ERROR_ERROR = "error";
	/** Possible setting for {@link #ACTION_ON_ERROR}, do not show any result */
	public static final String ON_ERROR_SILENT = "silent";
	/** Possible setting for {@link #ACTION_ON_ERROR}, show tests as "Skipped" */
	public static final String ON_ERROR_SKIP = "skip";
	/** Default action for {@link #ACTION_ON_ERROR}, is {@link #ON_ERROR_ERROR} */
	public static final String ACTION_ON_ERROR_DEFAULT = ON_ERROR_ERROR;
	/**
	 * Command line option to choose whether the <em>mpirun</em> process should
	 * actually be launched. Can be set by the JVM argument <em>-Dmpirunner.dryRun=true/false</em>. 
	 * By default, it is set to <code>false</code>, meaning that the  
	 * {@link MpiRunner} will try to launch a <em>mpirun</em> process. 
	 * <p>
	 * However, if this option is set to <code>true</code>, the 
	 * {@link MpiRunner} will only try to parse the {@link Notification} 
	 * recordings. In addition, the recordings will be kept on the system 
	 * regardless of what is set for option {@link #KEEP_NOTIFICATIONS}.   
	 * <p>
	 * This can be useful if you want to parse the test results from a different 
	 * system on your local machine. 
	 */
	public static final String DRY_RUN = "mpirunner.dryRun";
	/** Default setting for {@link #DRY_RUN} */
	public static final String DRY_RUN_DEFAULT = "false";
	/**
	 * Command line option used to define the <em>-Djava.library.path</em> 
	 * option of the mpi process launched by the {@link MpiRunner}. Set it by 
	 * defining <em>-Dmpirunner.javaLibraryPath=path/to/native/libs</em>.  
	 * This setting  has no default value. If it is not set, no particular 
	 * java library path will be set for the processes launched by the 
	 * {@link MpiRunner}.  
	 */
	public static final String JAVA_LIBRARY_PATH = "mpirunner.javaLibraryPath";
			
	/**
	 * Command line option to define the MPI implementation used to launch the 
	 * mpi tests. This can be used to run a script rather than the "mpirun" 
	 * command. Set this setting with <em>-Dmpirunner.mpiImpl=implementation</em>.
	 * <p>
	 * Current supported implementations are:
	 * <ul>
	 * <li>{@value #MPI_IMPL_NATIVE}
	 * <li>{@value #MPI_IMPL_MPJMULTICORE}
	 * </ul>
	 * In the {@link #MPI_IMPL_NATIVE} implementation, the <em>mpirun</em> command will be used. 
	 * You can set the options of that command by setting the {@value #MPIRUN_OPTION}
	 * variable. 
	 */
	public static final String MPI_IMPL = "mpirunner.mpiImpl";
	
	/** 
	 * Possible option for the MPI implementation used to launch the tests. 
	 * When using the <em>native</em> implementation, be careful of the fact you
	 * will need to provide the path to the shared objects bindings to the MPI
	 * library you are using. You can do this by setting option 
	 * {@link #JAVA_LIBRARY_PATH}. You can also give additional arguments to the
	 * <em>mpirun</em> command by setting {@link #MPIRUN_OPTION}.  
	 */ 
	public static final String MPI_IMPL_NATIVE = "native";
	/** 
	 * Possible option for the MPI implementation used to launch the tests.
	 * <p>
	 * When using the MPJ multicore configuration, you will need to set 
	 * <em>MPJ_HOME</em> as an environment variable as this is needed by the MPJ
	 * library to run correctly. We refer you to 
	 * <a href="http://mpj-express.org/">the MPJ Express website</a> for
	 * downloads and documentation.   
	 */
	public static final String MPI_IMPL_MPJMULTICORE = "mpj-multicore";
	
	/** Default setting for {@link #MPI_IMPL} : {@value #MPI_IMPL_DEFAULT} */
	public static final String MPI_IMPL_DEFAULT = MPI_IMPL_MPJMULTICORE;
	
	/**
	 * Settings to give options to the <em>mpirun</em> command when a 
	 * <em>native</em> MPI implementation id used. Is not set by default (by 
	 * default, no options are passed to the <em>mpirun</em> command except 
	 * <em>-np X</em> where X is the number of processes requested by your test 
	 * class). 
	 */
	public static final String MPIRUN_OPTION = "mpirunner.mpirunOptions";
	
	/**
	 * Option to specify the directory in which the notifications of each mpi 
	 * process will be placed. Set it using <em>-Dmpirunner.notificationsPath=direcotryToNotifications</em>.
	 * If the directory does not exist, it will be created. However, note that
	 * the recording files are deleted by default by the {@link MpiRunner}
	 * after they are parsed. To keep the recordings, set {@link #KEEP_NOTIFICATIONS}
	 * to true. 
	 */
	public static final String NOTIFICATIONS_PATH = "mpirunner.notificationsPath";
	
	/**
	 * Command line option used to choose whether the files containing the 
	 * {@link Notification}s that are made to the RunNotifier in each MPI 
	 * process should be kept after the tests' execution. Set it by defining 
	 * <em>-Dmpirunner.keepNotifications=true/false</em>. This option is 
	 * <code>false</code> by default, meaning these files will deleted after 
	 * they are parsed.
	 * <p>
	 * This can be useful if you want to parse the results on a different system
	 * then the one they were executed on. For instance, I find it convenient to
	 * run the tests on a server with this option set to <code>true</code>, 
	 * download the notification recordings and parse the results on my personal
	 * machine using the {@link #DRY_RUN} option.  
	 */
	public static final String KEEP_NOTIFICATIONS = "mpirunner.keepNotifications";
	/** 
	 * Default setting for {@link #KEEP_NOTIFICATIONS}, is 
	 * {@value #KEEP_NOTIFICATIONS_DEFAULT} 
	 */
	public static final String KEEP_NOTIFICATIONS_DEFAULT = "false";
}
