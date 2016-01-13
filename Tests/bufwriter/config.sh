#!/bin/bash

#main class and arguments of the test case
MAIN=bufwriter.BufWriter
ARGS="out.txt little"

#### CORTEX INSTRUMENTER ####
#add "--no-init" when we don't want to instrument <init> and <clinit> methods
NO_INIT=


#### CORTEX RUNTIME ####
#output folder to store the production run traces
PRFOLDER=~/work/cortex/Tests/bufwriter/PRuns

#add "-full" to record a full execution trace, instead of just up to the assertion
FULLREC=

#path to the production run output trace
TRACE=$PRFOLDER/bufwriter


#### CORTEX SOLVER ####
#path to program's Java PathFinder configuration file
JPFFILE=~/work/cortex/CortexSE/jpf-symbiosis/src/examples/BufWriter.jpf

#Symbolic execution timeout (in seconds)
JPFTIMEOUT=10

#cortex search parameters
CORTEX_D=1
CORTEX_N=0

#add "--csr" to apply context switch reduction
CSR= 

