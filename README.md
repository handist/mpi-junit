# mpi-junit

This project contains a Junit4 Runner implementation which allows you to run
Junit tests for Java programs that use MPI calls. Simply add this project as a 
dependency and annotate your test class with the following: 

```java
@RunWith(MpiRunner.class)   // Use the MpiRunner to run the test
@MpiConfig(ranks=4)	        // Specify how many ranks you want your test to run with
public class MyTestClass {
	@Test
	public void testMultipleHosts() {
		assertTrue(MPI.COMM_WORLD.Size() > 1);
	}
}
```

In normal Junit test execution, the test methods are only run once. With the 
MpiRunner however, each mpi process execute the test methods. You will therefore 
obtain as many test results as there are ranks for each of your test methods. 

## How it works

To run the tests with MPI, the MpiRunner spawns a `mpirun` process with the 
user-specified number of ranks. Each mpi process will write the results of its 
test methods to a seperate file. When the `mpirun` processes terminate, the 
MpiRunner will parse these files and aggregate the results and return them to
the "normal" Junit runtime. 

As the use of custom Runners is an integral feature of the Junit4 framework, 
using the MpiRunner should integrate seamlessly with the system you use to run
your tests, whether it be the Eclipse Junit launchers or Maven. 

## Dependencies

All the dependencies for this project are included with the "provided" maven 
scope, meaning that when you use this project, it will use your Junit and MPI 
for Java dependencyies. 

As of now, the MpiRunner was only tested with the mpiJava project  (v1.2.7) as a 
supplier of the MPI calls to Java. If you are using the MPJ Express library, you 
should be able to adapt the command launched by the MpiRunner to use the launch
scripts included with the MPJ-Express library. A more simple command-line 
setting to change the command used to launch the mpi processes is an idea for 
future development.
