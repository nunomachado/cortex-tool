#!/bin/bash

#############
# VARIABLES #
#############
#test case name (there must exist a folder with this name under /cortex/Tests/)
PROG=$2 
CONFIG=~/work/cortex/Tests/$PROG/config.sh
. $CONFIG
#echo $CONFIG

#path to Cortex Instrumenter
CORTEX_INST=~/work/cortex/CortexTransformer/
INST_CP=./bin:~/work/cortex/Tests/$PROG/bin:./lib/jasminclasses-2.4.0.jar:./lib/sootclasses-2.4.0.mine.jar:./lib/polyglotclasses-1.3.5.jar #java classpath 

#path to folder containing the instrumented classes
INST_RT=$CORTEX_INST/CortexRuntime
INST_JPF=$CORTEX_INST/CortexJPF

#path to Cortex Runtime
CORTEX_RT=~/work/cortex/CortexRuntime
RT_CP=./bin:$INST_RT:. #java classpath 

#number of production runs to record
RUNS=100

#path to Cortex Symbolic Execution Engine (Java PathFinder)
CORTEX_SE=~/work/cortex/CortexSE/jpf-symbiosis
TMPFILE=~/work/cortex/Tests/TMP.jpf

#path to Cortex Solver
CORTEX_SOLVER=~/work/cortex/CortexSolver
SYMB_FOLDER=/home/symbiosis/work/cortex/Tests/$PROG/Symbolic
MODEL=$CORTEX_SOLVER/tmp/model_$PROG.txt
FAILSCH=$CORTEX_SOLVER/tmp/fail_$PROG.txt
SRC_FOLDER=/home/symbiosis/work/cortex/Tests/$PROG/src

#path to SMT solver
SMTSOLVER=~/work/z3-4.3.2/bin/z3

##############
# EXEC MODES #
##############
#USAGE: 
#  "-i" for instrumentation; 
#  "-r" for production run collection; 
#  "-s" for symbolic trace generation; 
#  "-e" for exposing concurrency bugs via path and schedule exploration; 
#  "-d" for DSP generation;
while getopts "irsed" opt; do

    case "$opt" in
    i)  #INSTRUMENTATION
	echo $'\n'=== INSTRUMENTING PROGRAM - $MAIN
	cd $CORTEX_INST
	java -Xmx1500m -cp $INST_CP pt.tecnico.symbiosis.transformer.SymbiosisTransformer $MAIN $NO_INIT
	;;

    r)  #PRODUCTION RUN COLLECTION
        echo $'\n'=== COLLECTING $RUNS PRODUCTION RUNS - $MAIN
	cd $CORTEX_RT
        for i in `seq 1 $RUNS`; do
            time java -ea -cp $RT_CP pt.tecnico.symbiosis.runtime.Main --main-class $MAIN $ARGS --bb-trace $TRACE $FULLREC
        done
        ;;

    s) 	#SYMBOLIC TRACE GENERATION
        cp $JPFFILE $TMPFILE
        cd $PRFOLDER
        sed -i "s/^cortex.flipfile=/#cortex.flipfile=/" $TMPFILE
        for f in *.ok; do
	    echo $'\n'=== SYMBOLIC EXECUTION - $f
            #change the trace used by JPF
            sed -i "s/.*symbiosis.bbtrace.*/symbiosis.bbtrace=\/home\/symbiosis\/work\/cortex\/Tests\/$PROG\/PRuns\/$f/" $TMPFILE
            #run Cortex SE (JPF)
            cd $CORTEX_SE
            java -Xmx1500m -jar ~/work/cortex/CortexSE/jpf-core/build/RunJPF.jar +shell.port=4242 $TMPFILE
        done
        sed -i "s/^#cortex.flipfile=/cortex.flipfile=/" $TMPFILE
        ;;

    e)	#EXPLORATION OF PATH AND SCHEDULE DEPENDENT BUGS 
	cd $CORTEX_SOLVER
	./cortexSolver --trace-folder=$SYMB_FOLDER --jpf-timeout=$JPFTIMEOUT --jpf-file=$TMPFILE --model=$MODEL --solution=$FAILSCH --with-solver=$SMTSOLVER --source=$SRC_FOLDER --cortex-n=$CORTEX_N --cortex-d=$CORTEX_D $CSR
        ;;
    d)	#DSP GENERATION 
	cd $CORTEX_SOLVER
	./cortexSolver --trace-folder=$SYMB_FOLDER --model=$MODEL --solution=$FAILSCH --with-solver=$SMTSOLVER --source=$SRC_FOLDER --dsp-mode --cortex-n=$CORTEX_N --cortex-d=$CORTEX_D
	xdot $CORTEX_SOLVER/tmp/DSP/dsp_fail_$PROG\_Alt0.gv &
        ;; 
    esac
done

