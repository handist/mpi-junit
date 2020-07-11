# mpi-junit

This project contains a Junit4 Runner implementation which allows you to run
Junit tests for Java programs that use MPI calls. Simply add this project as a
dependency and annotate your test class with the @RunWith and @MpiConfig annotations:

```java
@RunWith(MpiRunner.class)  // Use the MpiRunner to run the test
@MpiConfig(ranks=4)        // Specify how many ranks you want your test to run with
public class MyTestClass {
	@Test
	public void testMultipleHosts() {
		assertTrue(MPI.COMM_WORLD.Size() > 1);
	}
}
```

In normal Junit test execution, the test methods are only run once. With the
`MpiRunner however`, each mpi process execute the test methods. You will therefore
obtain as many test results as there are ranks for each of your test methods.

# Releases / Documentation

|Version|Changes|
|-|-|
| master-latest | Development head<br>[(Javadoc)](master-latest/apidocs/index.html)<br>[(Maven Report)](master-latest/index.html) |
| v1.2 | Released July 6th 2020<br>Added support for MPJ-multicore environment as well as a number of useful customization options<br>[(Javadoc)](v1.2/apidocs/index.html)<br>[(Maven Report)](v1.2/index.html)<br>[(Download mpi-junit-1.2.jar)](https://github.com/handist/mpi-junit/releases/download/v1.2/mpi-junit-1.2.jar) |
| v1.0 | Original release limited to mpiJava v1.2.7 native Mpi bindings. Use is not recommended due to issues in the project configuration. |

# How it works

To run the tests with MPI, the MpiRunner spawns a `mpirun` process with the
user-specified number of ranks. Each mpi process will write the results of its
test methods to a dedicated file. When the `mpirun` process terminates, the
`MpiRunner` will parse these files, aggregate the results and return them to
the "normal" Junit runtime.

As the use of custom Runners is an integral feature of the Junit4 framework,
using the MpiRunner should integrate seamlessly with the system you use to run
your tests, whether it be the Eclipse Junit launchers / Maven ...

[(click here for more detailed information)](HowItWorks.md)

# Dependencies

All the dependencies for this project are included with the "provided" maven
scope, meaning that when you use this project, it will use your Junit and MPI
for Java dependencies.

As of now, the `MpiRunner` supports two MPI implementations:

+ the [mpiJava project (v1.2.7)](https://sourceforge.net/projects/mpijava/) as a
supplier of the MPI calls to Java
+ the [MPJ-Express](http://mpj-express.org/) "multicore" environment

Be aware that some runtime environment cannot use the MPJ-Express "multicore" configuration.
This is the case when mixing the APGAS for Java runtime with MPI for instance.
However, if your programs can run with the MPJ-Express "multicore" implementation,
you should be able to use this project to run tests.

# These projects use *mpi-junit* to run their tests

+ [Handist Collections](https://handist.github.io/handistCollections/) a distributed
collection library
