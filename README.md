# cortex-tool
Cortex -- Production-guided Concurrency Debugging


Cortex is a tool to help developers expose and diagnose concurrency bugs in Java multithreaded applications. For more information check our [PPoPP'16 paper](http://www.gsd.inesc-id.pt/~nmachado/papers/ppopp16-nmachado.pdf).

#### Download our VM image with Cortex ready to use: http://www.gsd.inesc-id.pt/~nmachado/cortex-vm.ova  (username: cortex ; password: cortex2015)
    

### Folder Description 

This repository contains all the necessary code and scripts to run Cortex:

* **CortexRuntime** contains the code responsible for recording path profiles during production runs.

* **CortexSE** contains the code of the symbolic execution engine, consisting of Cortex’s extensions to Java PathFinder.

* **CortexTransformer** contains the code to identify shared variables (via static analysis) and instrument the program under test.

* **CortexSolver** contains the code responsible for building and solving the SMT formulae during Cortex’s production-guided search.

* **Tests** contains several benchmarks. Each benchmark program has its own folder containing: a configuration file (named config.sh), the program’s source code, the Java compiled classes, production run traces, and symbolic traces.

* **z3-4.3.2** contains two executables (for Mac OS and Linux, respectively) of the Z3 SMT Solver.

* **runCortex.sh** is the main script used to run Cortex.


### Usage 

Our Cortex prototypes operates in five steps: i) instrumentation, ii) production run trace generation, iii) symbolic trace generation, iv) production-guided exploration for failing schedules, and v) root cause isolation via DSP generation.
To test Cortex with a given benchmark bench within .../Tests, run the
command:
```
./runCortex.sh OPTION bench
```
where ```OPTION``` corresponds to one of the aforementioned execution steps, and ```bench``` is the name of the benchmark folder. In particular, the possible values of OPTION are the following:

* **-i** instruments the program and performs a static analysis to identify the shared variables in the code. 
(E.g. ```./runCortex.sh -i airline```)

* **-r** runs the instrumented version of the program a number RUNS of times (the value of RUNS is set to 100 by default, but it can be changed by editing the respective variable in runCortex.sh), recording an execution path profile per production run. The generated traces will be placed in .../Tests/bench/PRuns. Traces from correct runs will have the extension **.ok**, whereas traces from failing runs will have the extension **.fail**. 
(E.g. ```./runCortex.sh -r airline```)

* **-s** generates the corresponding per-thread symbolic traces for each of the **.ok** traces recorded in the previous step. The symbolic traces will be stored under .../Tests/bench/Symbolic. (E.g. ```./runCortex.sh -s airline```)

* **-e** performs the production-guided search to find a failing schedule. Here, Cortex uses the symbolic traces obtained before to guide the exploration of the space of possible paths and schedules. Cortex also synthesizes new symbolic traces if necessary.
The failing schedule (when found) will be output as a file named **fail_bench.txt** under .../CortexSolver/tmp. In turn, the data regarding the number of attempts and the number of branch conditions flipped required to expose the concurrency bug will be output in the console. 
(E.g. ```./runCortex.sh -r airline```)

* **-d** produces an alternate non-failing schedule via event pair reordering and computes the corresponding DSP, which helps isolating the bug’s root cause. The alternate schedule will be output as a file named **fail_benchALT.txt** under .../Cortex- Solver/tmp, whereas the DSP will be output as a file named **dsp_fail_bench Alt0.gv** under .../CortexSolver/tmp/DSP.
(E.g. ```./runCortex.sh -d airline```)


As a remark, note that the name of the benchmark passed as parameter to runCortex.sh must match the name of the corresponding folder in .../Tests/. Also, as our Cortex prototype uses Soot (for instrumentation) and Java PathFinder (for symbolic execution), it requires Java 6.











