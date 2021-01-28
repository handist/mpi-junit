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
 * Custom implementation of the Junit {@link RunNotifier}. Instead of using the
 * subscriber-based system of the standard Junit framework, this implementation
 * stores the calls it receives as instances of class {@link Notification} and
 * writes these calls to a {@link File} specified as a parameter of its
 * constructor.
 *
 * @author Patrick Finnerty
 * @see Notification
 */
public class ToFileRunNotifier extends RunNotifier {

	private static String testAssumptionFailedMethod = "fireTestAssumptionFailed";
	private static String testFailureMethod = "fireTestFailure";
	private static String testFinishedMethod = "fireTestFinished";
	private static String testIgnoredMethod = "fireTestIgnored";
	private static String testRunFinishedMethod = "fireTestRunFinished";
	private static String testRunStartedMethod = "fireTestRunStarted";
	private static String testStartedMethod = "fireTestStarted";
	private static String testSuiteFinishedMethod = "fireTestSuiteFinished";
	private static String testSuiteStartedMethod = "fireTestSuiteStarted";

	/**
	 * Accumulator of the calls made to this instance. This member is the one which
	 * is serialized and written to {@link #outputFile} in method {@link #close()}.
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
	 *          written
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
	 * Writes all the notifications received to the output stream {@link #objOut},
	 * flushes it, and closes it.
	 * <p>
	 * This method should be called after all the tests have been run and the JVM
	 * running the tests for this particular place have terminated.
	 *
	 * @throws IOException if such an Exception is thrown when manipulating
	 *                     {@link #objOut}
	 */
	public void close() throws IOException {
		objOut.writeObject(notifiedEvents);
		objOut.flush();
		objOut.close();
	}

	@Override
	public void fireTestAssumptionFailed(final Failure failure) {
		final Object[] args = new Object[1];
		args[0] = failure;
		notifiedEvents.add(new Notification(testAssumptionFailedMethod, args));
	}

	@Override
	public void fireTestFailure(Failure failure) {
		final Object[] args = new Object[1];
		args[0] = failure;
		notifiedEvents.add(new Notification(testFailureMethod, args));
	}

	@Override
	public void fireTestFinished(final Description description) {
		final Object[] args = new Object[1];
		args[0] = description;
		notifiedEvents.add(new Notification(testFinishedMethod, args));
	}

	@Override
	public void fireTestIgnored(final Description description) {
		final Object[] args = new Object[1];
		args[0] = description;
		notifiedEvents.add(new Notification(testIgnoredMethod, args));
	}

	@Override
	public void fireTestRunFinished(final Result result) {
		final Object[] args = new Object[1];
		args[0] = result;
		notifiedEvents.add(new Notification(testRunFinishedMethod, args));
	}

	@Override
	public void fireTestRunStarted(final Description description) {
		final Object[] args = new Object[1];
		args[0] = description;
		notifiedEvents.add(new Notification(testRunStartedMethod, args));
	}

	@Override
	public void fireTestStarted(final Description description) throws StoppedByUserException {
		final Object[] args = new Object[1];
		args[0] = description;
		notifiedEvents.add(new Notification(testStartedMethod, args));
	}

	@Override
	public void fireTestSuiteFinished(final Description description) {
		final Object[] args = new Object[1];
		args[0] = description;
		notifiedEvents.add(new Notification(testSuiteFinishedMethod, args));
	}

	@Override
	public void fireTestSuiteStarted(final Description description) {
		final Object[] args = new Object[1];
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
