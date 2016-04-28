#!/bin/bash

#main class and arguments of the test case
MAIN=critical.Critical
ARGS=

#### CORTEX INSTRUMENTER ####
#add "--no-init" when we don't want to instrument <init> and <clinit> methods
NO_INIT=--no-init


#### CORTEX RUNTIME ####
#output folder to store the production run traces
PRFOLDER=$CORTEX_HOME/Tests/critical/PRuns

#add "-full" to record a full execution trace, instead of just up to the assertion
FULLREC=-full

#path to the production run output trace
TRACE=$PRFOLDER/critical


#### CORTEX SOLVER ####
#path to program's Java PathFinder configuration file
JPFFILE=$CORTEX_HOME/CortexSE/jpf-symbiosis/src/examples/Critical.jpf

#Symbolic execution timeout (in seconds)
JPFTIMEOUT=10

#cortex search parameters
CORTEX_D=2
CORTEX_N=3

#add "--csr" to apply context switch reduction
CSR=
