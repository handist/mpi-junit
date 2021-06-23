package handist.mpijunit;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;

import org.junit.runner.Runner;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.Parameterized;
import org.junit.runners.Suite;

import mpi.MPI;

public class ParameterizedMpiTestLauncher {
	/** Identifier of this place */
	static int commRank;
	/** Number of places in the system */
	static int commSize;

	/**
	 * Main method of MpiTestLauncher
	 * <p>
	 * Sets up the MPI environment by calling Init, Rank, and Size. Initializes a
	 * file in which the result of the Junit tests are going to be written. Runs the
	 * tests of the class provided using the normal Junit4 Runner
	 * {@link BlockJUnit4ClassRunner}.
	 * <p>
	 * As all ranks do the same, all the ranks will proceed to call the same test
	 * methods in the same order.
	 *
	 * @param args Fully qualified name of the test class whose tests need to be
	 *             run, second argument: the index of the Parameter which needs to
	 *             be run, third argument: the directory under which the test
	 *             results file should be written (optional)
	 * @throws Throwable if thrown during main
	 */
	public static void main(String[] args) throws Throwable {
		MPI.Init(args);
		commRank = MPI.COMM_WORLD.Rank();
		commSize = MPI.COMM_WORLD.Size();

		// Discard the 3 first arguments of the MPJ runtime if necessary
		if (args.length > 3) {
			final String[] newArgs = new String[args.length - 3];
			for (int i = 3, j = 0; i < args.length; i++, j++) {
				newArgs[j] = args[i];
			}
			args = newArgs;
		}

		// Obtain the class to test given as an argument
		final Class<?> testClass = Class.forName(args[0]);
		final int configurationIndex = Integer.parseInt(args[1]);

		// Create a Parameterized runner to extract the specific block runner we want to
		// run
		Parameterized runner = new Parameterized(testClass);

		// Code below allows us to extract the successive "Runner" instances
		// prepared by Parameterized. They are in member "runners" of the parent
		// class Suite.
		Field runnersField = Suite.class.getDeclaredField("runners");
		runnersField.setAccessible(true);
		@SuppressWarnings("unchecked")
		Runner runnerForSpecifiedConfig = ((List<Runner>) runnersField.get(runner)).get(configurationIndex);
		final String notificationFileName = testClass.getCanonicalName() + "_" + configurationIndex + "_" + commRank;

		String directory = null;
		if (args.length > 2) {
			directory = args[2];
		}
		final File f = new File(directory, notificationFileName);

		final ToFileRunNotifier notifier = new ToFileRunNotifier(f);
		runnerForSpecifiedConfig.run(notifier);
		notifier.close();

		MPI.Finalize();
		System.exit(0);
	}
}
