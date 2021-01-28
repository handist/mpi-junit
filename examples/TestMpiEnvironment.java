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

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import handist.mpijunit.MpiConfig;
import handist.mpijunit.MpiRunner;
import mpi.MPI;

/**
 * Test class that demonstrates how the tests are run and
 * how the Junit assertions / failure behave in case they 
 * fail.  
 */
@RunWith(MpiRunner.class)
@MpiConfig(ranks=4)
public class TestMpiEnvironment {

	static int rank = -1;
	static int size = -1;
	
	@BeforeClass
	public static void beforeClass() throws Exception {
		rank = MPI.COMM_WORLD.Rank();
		size = MPI.COMM_WORLD.Size();
	}
	
  /**
	 * Checks that the test was indeed run with multiple hosts
	 */
	@Test
	public void testMultipleHosts() {
		assertEquals(4, size);
		assertTrue("Rank shoud be stricly postive", rank >= 0);
		System.out.println("Here is a message from [" + rank + "]");
	}

	@Test
	public void testFailOnFirstHost() throws Throwable {
		if (rank == 0) {
			fail("Failure on host 0 only");
		}
	}
	
	@Test
	public void testFailOnSecondHost() throws Throwable {
		if (rank == 1) {
			fail("Failure on host 1 only");
		}
	}
	
	@Test
	public void testFailOnTwoAndThree() throws Throwable {
		if (rank == 2 || rank == 3) {
			fail("Failure on " + rank);
		}
	}
	
	@Test
	@Ignore
	public void testSkipped() throws Throwable {
	}
	
}
