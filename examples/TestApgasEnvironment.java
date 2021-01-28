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
import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;

import apgas.Constructs;
import apgas.MultipleException;
import handist.mpijunit.MpiApgasTestLauncher;
import handist.mpijunit.MpiConfig;
import handist.mpijunit.MpiRunner;

import static apgas.Constructs.*;

/**
 * Test class that checks how the Junit assertions behave with the APGAS
 * remote activities framework and illustrates how to handle them. 
 * <p>
 * A specific launcher included in this library is used: (MpiApgasTestLauncher.class). 
 * This launcher will initialize both the APGAS and the MPI environment before
 * launching the tests. 
 */
@RunWith(MpiRunner.class)
@MpiConfig(ranks=2, launcher=MpiApgasTestLauncher.class)
public class TestApgasEnvironment {

	/**
	 * Checks that the test is indeed run with multiple APGAS places
	 */
	@Test
	public void testMultipleHosts() {
		int numberOfPlaces = places().size();
		assertTrue("This test should be run with multiple hosts", numberOfPlaces > 1);
	}

	/**
	 * Tests the failure of an assertEquals call in an asynchronous remote
	 * activity.
	 */
	@Test(expected=java.lang.AssertionError.class)
	public void testRemoteAssertEqualsFailure() throws Throwable {
		int here = here().id;
		final int targetPlace = here == 0 ? Constructs.places().size() - 1: here-1; 
			try {
				finish(()-> {
					final int rootId = here().id;
					asyncAt(place(targetPlace), () -> {
						assertEquals(rootId, here().id); // Expect failure
					});
				});
			} catch (MultipleException e) {
				Throwable[] suppressed = e.getSuppressed();
				throw(suppressed[0]);
			}
	}
}
