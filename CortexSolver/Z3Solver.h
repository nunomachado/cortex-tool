//
//  Z3Solver.h
//  symbiosisSolver
//
//  Created by Nuno Machado on 03/01/14.
//  Copyright (c) 2014 Nuno Machado. All rights reserved.
//

//#ifndef __snorlaxsolver__Z3Solver__
//#define __snorlaxsolver__Z3Solver__

#ifndef __symbiosisSolver__Z3Solver__
#define __symbiosisSolver__Z3Solver__

#include <iostream>
#include <fstream>
#include <vector>
#include "Parameters.h"


class Z3Solver {
    
    protected:
    std::ofstream z3File;                       //z3 output file (containing the symbolic constraint model)
    pid_t z3pid;                                //id of the process running z3
    int procR, procW;                           //pipes to read from and write to the process running z3
    std::vector<std::string> operationsVars;    //vector containing the operation variables
    std::vector<std::string> orderVars;         //vector containing the operation order variables
    int numOps;                             //number of operations to ordered
    
    
    public:
    std::vector<std::string> threadIds;          //vector containing the thread ids
    
    //constructor
    Z3Solver();
    
    //general functions
    bool solve();
    bool solveWithSolution(std::vector<std::string> solution, bool invertBugCond);
    void closeZ3();
    bool checkSat();
    std::string readLinePipe();
    void openOutputFile();
    void openInputFile();
    void printModel();
    void writeLineZ3(std::string content);
    int getNumOps();
    void setNumOps(int n);
    void reset();   //** resets the data structures
    
    //Operations over constraints
    std::string cAnd(std::string exp1, std::string exp2);
    std::string cAnd(std::string exp1);
    std::string cOr(std::string exp1, std::string exp2);
    std::string cOr(std::string exp1);
    std::string cEq(std::string exp1, std::string exp2);
    std::string cNeq(std::string exp1, std::string exp2);
    std::string cGeq(std::string exp1, std::string exp2);
    std::string cGt(std::string exp1, std::string exp2);
    std::string cLeq(std::string exp1, std::string exp2);
    std::string cLt(std::string exp1, std::string exp2);
    std::string cLt(std::string exp1);
    std::string cDiv(std::string exp1, std::string exp2);
    std::string cMod(std::string exp1, std::string exp2);
    std::string cPlus(std::string exp1, std::string exp2);
    std::string cMinus(std::string exp1, std::string exp2);
    std::string cMult(std::string exp1, std::string exp2);
    std::string cSummation(std::vector<std::string> sum);
    std::string declareIntVar(std::string varname);
    std::string declareIntVar(std::string varname, int min, int max);
    std::string declareIntVarAndStore(std::string varname, int min, int max);
    std::string declareRealVar(std::string varname, int min, int max);
    std::string declareIntOrderVar(std::string varname, int min, int max);
    std::string declareIntOrderVarAndStore(std::string varname, int min, int max);
    std::string postAssert(std::string constraint);
    std::string postNamedAssert(std::string constraint, std::string label);
    std::string invertBugCondition(std::string expr);
};

#endif /* defined(__snorlaxsolver__Z3Solver__) */
