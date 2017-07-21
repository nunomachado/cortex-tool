#!/bin/bash

#main class and arguments of the test case
MAIN=bank.Bank
ARGS=

#### CORTEX INSTRUMENTER ####
#add "--no-init" when we don't want to instrument <init> and <clinit> methods
NO_INIT=


#### CORTEX RUNTIME ####
#output folder to store the production run traces
PRFOLDER=$CORTEX_HOME/Tests/account/PRuns

#add "-full" to record a full execution trace, instead of just up to the assertion
FULLREC=

#path to the production run output trace
TRACE=$PRFOLDER/account


#### CORTEX SOLVER ####
#path to program's Java PathFinder configuration file
JPFFILE=$CORTEX_HOME/CortexSE/jpf-symbiosis/src/examples/Account.jpf

#Symbolic execution timeout (in seconds)
JPFTIMEOUT=5

#cortex search parameters
CORTEX_D=1
CORTEX_N=1

#add "--csr" to apply context switch reduction
CSR='--csr=true' 

