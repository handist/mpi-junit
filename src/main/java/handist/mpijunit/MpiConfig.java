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

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * Annotation used to set some environment settings for test classes that use
 * the {@link MpiRunner}. At a minimum, test classes have to specify how many
 * processes are desired for the test. You can also specify a specific launcher
 * if you need some specific setup or initialization that involves multiple
 * hosts before launching the tests.
 *
 * @author Patrick Finnerty
 *
 */
@Documented
@Retention(RUNTIME)
@Target(TYPE)
public @interface MpiConfig {
	/**
	 * Indicates which launcher should be used. The launcher is the class whose main
	 * method is responsible for launching the tests in each MPI process. By
	 * default, class {@link MpiTestLauncher} is used.
	 * <p>
	 * Using a different class than the default may be useful if you have some
	 * specific setup to do before launching the Junit tests runtime. One such
	 * specific launcher we provide in this library is the
	 * {@link MpiApgasTestLauncher} which sets up the APGAS runtime before launching
	 * the tests. Of course you can also use your own custom launcher.
	 *
	 * @return the class whose main will be launched by all the parallel processes
	 */
	Class<?> launcher() default MpiTestLauncher.class;

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
