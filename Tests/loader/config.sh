#!/bin/bash

#main class and arguments of the test case
MAIN=bouvlesort.Loader
ARGS="out.txt low"

#### CORTEX INSTRUMENTER ####
#add "--no-init" when we don't want to instrument <init> and <clinit> methods
NO_INIT=


#### CORTEX RUNTIME ####
#output folder to store the production run traces
PRFOLDER=$CORTEX_HOME/Tests/loader/PRuns

#add "-full" to record a full execution trace, instead of just up to the assertion
FULLREC=

#path to the production run output trace
TRACE=$PRFOLDER/loader


#### CORTEX SOLVER ####
#path to program's Java PathFinder configuration file
JPFFILE=$CORTEX_HOME/CortexSE/jpf-symbiosis/src/examples/Loader.jpf

#Symbolic execution timeout (in seconds)
JPFTIMEOUT=10

#cortex search parameters
CORTEX_D=11
CORTEX_N=1

#add "--csr" to apply context switch reduction
CSR="--csr"

