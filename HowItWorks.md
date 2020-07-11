# How the MpiRunner works

This page presents the general behavior of class `MpiRunner`. Class `MpiRunner` extends the Junit4 `Runner` class and as such, is responsible for calling the test methods of the test class is receives and transmitting the results to the `Notifier` it is given in [method run](https://github.com/handist/mpi-junit/blob/d622b22c0496824933f60f08aa3930bc3f6804c6/src/main/java/handist/mpijunit/MpiRunner.java#L348).

Unlike most Runner implementation, the test class is never called directly in the process running the MpiRunner. Instead, the tests are executed in 3 phases.

1. A MPI command is prepared and launched by the `MpiRunner`
2. The tests are run on each rank with potential communication between these ranks. The results of the tests for each MPI rank are written to a separate file.
3. Back in the `MpiRunner`, the test results of each rank are parsed and transmitted back to the normal Junit framework

The code details of steps 1. and 3. can be read in [method `run` of class `MpiRunner`](https://github.com/handist/mpi-junit/blob/d622b22c0496824933f60f08aa3930bc3f6804c6/src/main/java/handist/mpijunit/MpiRunner.java#L348). Step 2. is performed by the launcher used by the test class. You may refer to the default implementation, [class `MpiTestLauncher`](https://github.com/handist/mpi-junit/blob/d622b22c0496824933f60f08aa3930bc3f6804c6/src/main/java/handist/mpijunit/MpiTestLauncher.java#L30).

The following sections try to provide some additional details on how these three steps are performed. They may provide the reader with some additional understanding of some customizing options that the MpiRunner offers. For a complete list of options, we refer the reader [to this page](ConfiguringMpiRunner.md)

## 1. Preparing the command

There are currently two implementations available.

+ native (C/C++) Mpi accesses using the mpiJava java library
+ MPJ-Express "multicore" implementation

Depending on the MPI implementation you choose, the command that the MpiRunner will build and launch.

### 1.1 Native implementation command

The command built by the MpiRunner in the "native" implementation is the following:

```
mpirun [<mpirun options>] -np <ranks> java [<MpiRunner's javaagents>] -Duser.dir=<directory> [-Djava.library.path=<path>] -cp <classpath> <launcher> <class under test> [<path to notification files>]
```
where:
+ *`<mpirun options>`* is set using the `mpirunner.mpirunOptions` property (if not set, is omitted in the command)
+ *`<ranks>`* is the number of ranks specified by the test class with the `@MpiConfig` annotation.
+ *`<MpiRunner's javaagents>`* are the `-javaaent::...` arguments the process that launched the MpiRunner used. If there are any, they are transmitted to the processes that run the tests. One use case of this feature is code coverage using Jacoco.
+ *`<directory>`* is populated using `System.getProperty("user.dir")`
+ *`-Djava.library.path=<path>`* is set in the command if property `mpirunner.javaLibraryPath` is set. *`<path>`* will be populated with what this option was set with.
+ *`<classpath>`* is populated with `System.getProperty("java.class.path")`
+ *`<launcher>`* is the fully qualified name of the main class. It is populated with the launcher specified in the test's `@MpiConfig` annotation (default is `handist.mpijunit.MpiTestLauncher`)
+ *`<class under test>`* is the fully qualified name of the class under test
+ *`<path to notification files>`* is added to the command if option `mpirunner.notificationsPath` is set. It is omitted if the option is not set.

This places a number of requirements on the system running the tests:

1. A native MPI implementation must be installed on the system
2. Command `mpirun` must be available in the PATH
3. the native bindings of the mpiJava library needs to be compiled on the system and the location of the shared object library (libmpijava.so) needs to be specified with the `mpirunner.javaLibraryPath` property

### 1.2 MPJ Express "multicore" configuration

## 2. Running the tests

The tests contained in the test class are run by the launcher on each rank using the regular Junit4 framework but using a custom Notification implementation.

Rather than directly transmitting the test results with notifications directly to the Junit framework (from which each rank is disconnected anyway), the notifications made by each rank are stored in a file.

The key part of the default launcher ([MpiTestLauncher](https://github.com/handist/mpi-junit/blob/master/src/main/java/handist/mpijunit/MpiTestLauncher.java)) implementation we provide is shown below.

```java
public class MpiTestLauncher {
    public static void main(String[] args) {
        MPI.Init(args);
        // Setup specific to your environmnent
        // ...

        // Use a method that assigns a unique int to each rank
        int commRank = MPI.COMM_WORLD.Rank();

        // Obtain the class to test given as an argument
		Class<?> testClass = Class.forName(args[0]);
        // Open a file in which the notifications to Junit will be written.
        // Make sure to use the same naming convention!
        String notificationFileName = testClass.getCanonicalName() + "_" + commRank;

        // Handle the optional directory argument
        String directory = null;
		if (args.length > 1) {
			directory = args[1];
		}
		File f = new File(directory, notificationFileName);

        // Prepare the custom Notifier with the File created
        ToFileRunNotifier notifier = new ToFileRunNotifier(f);

        // Use the default Junit4 test runner to launch the tests.
        // It may be possible to use another implementation.
        BlockJUnit4ClassRunner junitDefaultRunner = new BlockJUnit4ClassRunner(testClass);

        // Use the custom notifier when calling the Junit test runner
        junitDefaultRunner.run(notifier);

        // Call this method to flush the contents of the notifier to the file before exiting.
		notifier.close();    

        // Shutdown / Teardown specific to your environment
        // ...
    }
}
```


### Implementing a custom launcher

You are able to specify the specific launcher you want to use in the `@MpiConfig` annotation of your test class. Nothing prevents you from implementing your own test launcher. This can be particularly useful if you need to make some special runtime preparations that cannot be done in the "normal" Junit @BeforeClass methods.

All that matters is that your launcher takes one arguments and an optional second which specifies in which directory the files that contain the notifications to Junit of each rank will be kept. If you want to use your launcher with MPJ, you will also need to ignore the first three arguments that are automatically added by the MPJ launcher after calling `MPI.init();`.

After your setup between your nodes, you need to write something similar to the code shown above. In particular, we strongly encourage you to reuse the `ToFileRunNotifier` class and keeping the same naming convention for the files containing the notifications of each rank.

## 3. Parsing the results

After the tests have run on each host, the notifications that would have normally been transferred directly to the Junit framework are parsed back from the files of each rank and made on the "normal" notifier that the MpiRunner was supplied with in its `run(RunNotifier notifier)` method.

In this stage, the tests are identified by the name of the test method. The parser currently implemented will add a `[x] ` prefix to the method name (where `x` corresponds to the rank number) so that the result of each rank are clearly identifiable.
