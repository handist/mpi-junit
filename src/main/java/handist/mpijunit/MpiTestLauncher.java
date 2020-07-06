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

import org.junit.runners.BlockJUnit4ClassRunner;

import mpi.MPI;

/**
 * Default launcher for the MpiRunner. This is main class of each rank in the 
 * MPI process. It calls MPI.Init before launching the test class using the
 * standard {@link BlockJUnit4ClassRunner}. The results of the tests are 
 * intercepted using the {@link ToFileRunNotifier} and written to a file when 
 * this process ends. This file will then be parsed by the {@link MpiRunner} to 
 * aggregate the test results of each rank and report the global test results of
 * the test class.
 *   
 * @author Patrick Finnerty
 *
 */
final public class MpiTestLauncher {

	/** Identifier of this place */
	static int commRank;
	/** Number of places in the system */
	static int commSize;


	/**
	 * Main method of MpiTestLauncher
	 * <p>
	 * Sets up the MPI environment by calling Init, Rank, and Size. Initializes a 
	 * file in which the result of the Junit tests are going to be written. 
	 * Runs the tests of the class provided using the normal Junit4 Runner 
	 * {@link BlockJUnit4ClassRunner}.
	 * <p>
	 * As all ranks do the same, all the ranks will proceed to call the same 
	 * test methods in the same order. 
	 * 
	 * @param  args Fully qualified name of the test class whose tests need to be 
	 * 	run, second argument: the directory under which the test results file should 
	 * be written (optional)
	 * 
	 * @throws Exception if an MPI exception occurs
	 */
	public static void main(String[] args) throws Exception {
		MPI.Init(args);
		commRank = MPI.COMM_WORLD.Rank();
		commSize = MPI.COMM_WORLD.Size();

		// Discard the 3 first arguments of the MPJ runtime if necessary
		if (args.length > 2) {
			String [] newArgs = new String[args.length -3];
			for (int i = 3, j = 0; i < args.length; i++, j++) {
				newArgs[j] = args[i];
			}
			args = newArgs;
		}

		// Obtain the class to test given as an argument
		Class<?> testClass = Class.forName(args[0]);
		BlockJUnit4ClassRunner junitDefaultRunner = new BlockJUnit4ClassRunner(testClass);
		String notificationFileName = testClass.getCanonicalName() + "_" + 
				commRank;

		String directory = null;
		if (args.length > 1) {
			directory = args[1];
		}
		File f = new File(directory, notificationFileName);

		ToFileRunNotifier notifier = new ToFileRunNotifier(f);
		junitDefaultRunner.run(notifier);
		notifier.close();    

		MPI.Finalize();
		System.exit(0);
	}
}
