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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;

import org.junit.runners.BlockJUnit4ClassRunner;

import apgas.Configuration;
import apgas.GlobalRuntime;
import apgas.impl.Config;
import apgas.impl.Launcher;
import mpi.MPI;
import mpi.MPIException;

/**
 * Launcher class for tests that rely on a combination of MPI and APGAS for Java
 * calls. 
 * This class sets up the MPI and APGAS runtime before launching the Junit tests
 * on every rank (or place in APGAS terminology). To use this class over the 
 * default {@link MpiTestLauncher} to run your tests, you need to specify this
 * class in the {@link MpiConfig} annotation of your test class. 
 * 
 * @author Patrick Finnerty
 *
 */
final public class MpiApgasTestLauncher implements Launcher {

	/** Identifier of this place */
	static int commRank;
	/** Number of places in the system */
	static int commSize;

	static boolean finalizeCalled = false;

	/**
	 * Set in the main method according to the value set by {@link Configuration#APGAS_VERBOSE_LAUNCHER}
	 */
	static boolean verboseLauncher;

	/**
	 * Constructs a new {@link MpiApgasTestLauncher} instance.
	 */
	public MpiApgasTestLauncher() {
	}

	/**
	 * Launches one process with the given command line at the specified host.
	 * <p>
	 * Should not be called in practice as every java processes are supposed to be launched using the `mpirun` command.
	 * Implementation will print an "Internal error" on the {@link System#err} before calling for the program's
	 * termination and exiting with return code -1.
	 *
	 * @param  command   command line
	 * @param  host      host
	 * @param  verbose   dumps the executed commands to stderr
	 * @return           the process object
	 * @throws Exception if launching fails
	 */
	@Override
	public Process launch(List<String> command, String host, boolean verbose) throws Exception {

		System.err.println("[MPILauncher] Internal Error");
		MPI.Finalize();
		System.exit(-1);

		return null;
	}

	/**
	 * Launches n processes with the given command line and host list. The first host of the list is skipped. If the list
	 * is incomplete, the last host is repeated.
	 * <p>
	 * In this particular launcher implementation, this method is used by rank 0 
	 * to broadcast setup information to every other process which are expecting 
	 * it in the {@link #slave()} method. 
	 *
	 * @param  n         number of processes to launch
	 * @param  command   command line
	 * @param  hosts     host list (not null, not empty, but possibly incomplete)
	 * @param  verbose   dumps the executed commands to stderr
	 * @throws Exception if launching fails
	 */
	@Override
	public void launch(int n, List<String> command, List<String> hosts, boolean verbose) throws Exception {

		if (n + 1 != commSize) {
			System.err.println(
					"[MPILauncher] " + Configuration.APGAS_PLACES + " should be equal to number of MPI processes " + commSize);
			MPI.Finalize();
			System.exit(-1);
		}

		final byte[] baCommand = serializeToByteArray(command.toArray(new String[command.size()]));
		final int[] msglen = new int[1];
		msglen[0] = baCommand.length;
		MPI.COMM_WORLD.Bcast(msglen, 0, 1, MPI.INT, 0);
		MPI.COMM_WORLD.Bcast(baCommand, 0, msglen[0], MPI.BYTE, 0);
	}

	/**
	 * Waits to receive some information from rank 0 to setup the APGAS runtime. 
	 * Returns when the APGAS has been launched and is ready to run distributed
	 * programs. 
	 *  
	 * @throws Exception if some problem occurs with the MPI runtime
	 */
	static void slave() throws Exception {

		final int[] msglen = new int[1];
		MPI.COMM_WORLD.Bcast(msglen, 0, 1, MPI.INT, 0);
		final byte[] msgbuf = new byte[msglen[0]];
		MPI.COMM_WORLD.Bcast(msgbuf, 0, msglen[0], MPI.BYTE, 0);
		final String[] command = (String[]) deserializeFromByteArray(msgbuf);

		for (int i = 1; i < command.length; i++) {
			final String term = command[i];
			if (term.startsWith("-D")) {
				final String[] kv = term.substring(2).split("=", 2);
				if (verboseLauncher) {
					System.err.println("[" + commRank + "] setProperty \"" + kv[0] + "\" = \"" + kv[1] + "\"");
				}

				System.setProperty(kv[0], kv[1]);
			}
		}

		GlobalRuntime.getRuntime();
	}

	/**
	 * Shuts down the local process launched using MPI.
	 */
	@Override
	public void shutdown() {
		if (verboseLauncher) {
			System.err.println("[MPILauncherNoExit2] About to call MPI.Finalize on rank" + commRank);
		}
		try {
			if (!finalizeCalled) {
				finalizeCalled = true;
				MPI.Finalize();
			}
		} catch (final MPIException e) {
			System.err.println("[MPILauncher] Error on Shutdown at rank " + commRank);
			e.printStackTrace();
		}
		System.exit(0);
	}

	/**
	 * Checks that all the processes launched are healthy.
	 * <p>
	 * Current implementation of {@link MpiApgasTestLauncher} always returns true.
	 *
	 * @return true if all subprocesses are healthy
	 */
	@Override
	public boolean healthy() {
		return true;
	}

	/**
	 * Converts a String to an int array
	 *
	 * @param  src String to be converted
	 * @return     array of integer corresponding to the given parameter
	 * @see        #intArrayToString(int[])
	 */
	static int[] stringToIntArray(String src) {
		final char[] charArray = src.toCharArray();
		final int[] intArray = new int[charArray.length];
		for (int i = 0; i < charArray.length; i++) {
			intArray[i] = charArray[i];
		}
		return intArray;
	}

	/**
	 * Converts an int array back into the String it represents
	 *
	 * @param  src the integer array to be converted back into a String
	 * @return     the constructed String
	 * @see        #stringToIntArray(String)
	 */
	static String intArrayToString(int[] src) {
		final char[] charArray = new char[src.length];
		for (int i = 0; i < src.length; i++) {
			charArray[i] = (char) src[i];
		}
		final String str = new String(charArray);
		return str;
	}

	/**
	 * Serializer method
	 *
	 * @param  obj         Object to be serialized
	 * @return             array of bytes
	 * @throws IOException if an I/O exception occurs
	 * @see                #deserializeFromByteArray(byte[])
	 */
	static byte[] serializeToByteArray(Serializable obj) throws IOException {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(obj);
		return baos.toByteArray();
	}

	/**
	 * Deserializer method
	 *
	 * @param  barray                 byte array input
	 * @return                        Object constructed from the input
	 * @throws IOException            if an I/O occurs
	 * @throws ClassNotFoundException if the class could not be identified
	 * @see                           #serializeToByteArray(Serializable)
	 */
	static Object deserializeFromByteArray(byte[] barray) throws IOException, ClassNotFoundException {
		final ByteArrayInputStream bais = new ByteArrayInputStream(barray);
		final ObjectInputStream ois = new ObjectInputStream(bais);
		return ois.readObject();
	}

	/**
	 * Main method of the {@link MpiApgasTestLauncher} class.
	 * <p>
	 * Sets up the APGAS environment using MPI. Rank 0 of the processes will launch the main method of the
	 * class specified as parameter with the arguments specified afterward. 
	 * <p>
	 * This main method takes at least one argument, the fully qualified name of the class whose main method is to be run.
	 * Arguments for that class' main need to to follow that first argument.
	 *
	 * @param  args  first argument is the fully qualified name of the class whose
	 * 	tests are to launch, the second argument is the path under which the 
	 * 	{@link Notification}s sent to Junit are to be 
	 * 	written.  
	 * @throws Exception if an MPI exception occur
	 */
	public static void main(String[] args) throws Exception {
		MPI.Init(args);
		commRank = MPI.COMM_WORLD.Rank();
		commSize = MPI.COMM_WORLD.Size();

		verboseLauncher = Boolean.parseBoolean(System.getProperty(Configuration.APGAS_VERBOSE_LAUNCHER, "false"));
		if (verboseLauncher) {
			System.err.println("[MPILauncher] rank = " + commRank);
		}

		// Discard the 3 arguments of the MPJ runtime if necessary
		if (args.length > 2) {
			String [] newArgs = new String[args.length -3];
			for (int i = 3, j = 0; i < args.length; i++, j++) {
				newArgs[j] = args[i];
			}
			args = newArgs;
		}
		
		if (args.length < 1) {
			System.err.println("[MPILauncher] Error Main Class Required");
			MPI.Finalize();
			System.exit(0);
		}

		// Sets the number of places according to the size of the WORLD communicator
		System.setProperty(Configuration.APGAS_PLACES, Integer.toString(commSize));
		// Sets the launcher to be of MPILauncher class. This will make the apgas
		// runtime use the MPILauncher shutdown method when the apgas shutdown
		// method is launched
		System.setProperty(Config.APGAS_LAUNCHER, MpiApgasTestLauncher.class.getCanonicalName());

		/*
		 * If this place is the "master", i.e. rank 0, indireclty broadcasts connection
		 * information to every other hosts by setting up the runtime with this launcher.
		 * If this is not the master, receives that information and uses it to setup 
		 * the runtime.  
		 */
		if (commRank == 0) {
			GlobalRuntime.getRuntime();
		} else {
			slave();
		}

		// Obtain the class to test as an argument
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

		// Initiating shutdown
		if (verboseLauncher) {
			System.err.println("[MPILauncher] MPI rank" + commRank + " is initiating shutdown");
		}
		try {
			if (!finalizeCalled) {
				finalizeCalled = true;
				MPI.Finalize();
			}
			System.exit(0);
		} catch (final MPIException e) {
			System.err.println("[MPILauncher] Error on Finalize - main method at rank " + commRank);
			e.printStackTrace();
		}
	}
}
