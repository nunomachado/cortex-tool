#!/bin/bash

#main class and arguments of the test case
MAIN=GarageManager.GarageManager
ARGS="out.txt little"

#### CORTEX INSTRUMENTER ####
#add "--no-init" when we don't want to instrument <init> and <clinit> methods
NO_INIT=


#### CORTEX RUNTIME ####
#output folder to store the production run traces
PRFOLDER=$CORTEX_HOME/Tests/garage/PRuns

#add "-full" to record a full execution trace, instead of just up to the assertion
FULLREC=

#path to the production run output trace
TRACE=$PRFOLDER/garage


#### CORTEX SOLVER ####
#path to program's Java PathFinder configuration file
JPFFILE=$CORTEX_HOME/CortexSE/jpf-symbiosis/src/examples/Garage.jpf

#Symbolic execution timeout (in seconds)
JPFTIMEOUT=20

#cortex search parameters
CORTEX_D=3
CORTEX_N=4

#add "--csr" to apply context switch reduction
CSR=
