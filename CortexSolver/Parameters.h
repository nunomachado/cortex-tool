//
//  Parameters.h
//  symbiosisSolver
//
//  Created by Nuno Machado on 21/05/14.
//  Copyright (c) 2014 Nuno Machado. All rights reserved.
//

#ifndef __symbiosisSolver__Parameters__
#define __symbiosisSolver__Parameters__

#include <iostream>

//program inputs
extern const int MAX_LINE_SIZE;
extern bool debug;                  //prints debug info
extern bool dspMode;             //run in bug fixing mode
extern bool jpfMode;                //parse symbolic traces with Java Path Finder syntax
extern std::string symbFolderPath;  //path to the folder containing the symbolic event traces
extern std::string avisoFilePath;   //path to the aviso event trace
extern std::string solverPath;      //path to the solver executable
extern std::string formulaFile;     //path to the output file of the generated constraint formula
extern std::string solutionFile;    //path to the output file of the generated schedule
extern std::string solutionAltFile; //path to file with the alternate schedule
extern std::string sourceFilePath;  //path to the source code
extern std::string assertThread;    //id of the thread that contains the assertion
extern std::string dspFlag;         //define which view the user wants in the result: "extended" "short" or default
extern std::string jpfFile;         //path to JPF file to run symbolically
extern std::string jpftimeout;      //timeout for symbolic execution in JPF 
extern bool failedExec;             //indicates whether the traces correspond to a failing or successful execution
extern bool useCSR;                 //apply context switch reduction to the full failing schedule  
extern int cortex_D;    //Cortex: maximum number of branches away from the assertion that are allowed to be flipped for exploration purposes
extern int cortex_N;    //Cortex: maximum number of shortest paths (according to BFS) to be tested with a prefix corresponding to the flipped branch

#endif /* defined(__symbiosisSolver__Parameters__) */
