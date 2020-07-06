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

import java.io.Serializable;
import java.lang.reflect.Method;

import org.junit.runner.notification.RunNotifier;

/**
 * Class used to capture the fact the {@link ToFileRunNotifier} was called. 
 * An instance of this class represents a single call to the notifier. When the
 * tests have completed and the calls to the notifier of each rank have been 
 * written to a file, the {@link MpiRunner} parses these files and reproduces 
 * the calls made to the Junit notifier on its own {@link RunNotifier} for each
 * rank, effectively transmitting the test results of rank used to run the
 * tests.
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
	
	@Override
	public String toString() {
		return method + "(" + parameters[0].toString() + ")";
	}
}