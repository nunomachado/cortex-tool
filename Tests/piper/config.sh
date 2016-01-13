#!/bin/bash

#main class and arguments of the test case
MAIN=Piper.IBM_Airlines
ARGS="out.txt 2"

#### CORTEX INSTRUMENTER ####
#whether to instrument <init> and <clinit> methods or not
NO_INIT=


#### CORTEX RUNTIME ####
#output folder to store the production run traces
PRFOLDER=~/work/cortex/Tests/piper/PRuns

#add "-full" to record a full execution trace, instead of just up to the assertion
FULLREC=

#path to the production run output trace
TRACE=$PRFOLDER/piper


#### CORTEX SOLVER ####
#path to program's Java PathFinder configuration file
JPFFILE=~/work/cortex/CortexSE/jpf-symbiosis/src/examples/Piper.jpf

#Symbolic execution timeout (in seconds)
JPFTIMEOUT=20

#cortex search parameters
CORTEX_D=1
CORTEX_N=1

#add "--csr" to apply context switch reduction
CSR=
