//
//  ConstraintModelGenerator.h
//  symbiosisSolver
//
//  Created by Nuno Machado on 03/01/14.
//  Copyright (c) 2014 Nuno Machado. All rights reserved.
//

//#ifndef __snorlaxsolver__ConstraintModelGenerator__
//#define __snorlaxsolver__ConstraintModelGenerator__

#ifndef __symbiosisSolver__ConstraintModelGenerator__
#define __symbiosisSolver__ConstraintModelGenerator__

#include <iostream>
#include <map>
#include <vector>
#include <string>
#include "Z3Solver.h"
#include "Operations.h"
#include "Types.h"

class ConstModelGen {
protected:
    Z3Solver z3solver;
    double numMO; //number of memory order constraints
    double numRW; //number of read-write constraints
    double numLO; //number of locking order constraints
    double numPO; //number of partial order synchronization constraints
    double numPC; //number of path constraints
    double numUnkownVars; //number of unknown variables
    
private:
    std::vector<RWOperation> getWritesForRead(RWOperation readop, std::vector<RWOperation> writeset);
    
public:
    ConstModelGen();
    void createZ3Solver();  //creates an instance of the z3 solver
    void addMemoryOrderConstraints(std::map<std::string, std::vector<Operation*> > operationsByThread);
    void addReadWriteConstraints(std::map<std::string, std::vector<RWOperation> > readset, std::map<std::string, std::vector<RWOperation> > writeset, std::map<std::string, std::vector<Operation*> > operationsByThread);
    void addPathConstraints(std::vector<PathOperation> pathset, bool invertBugCond);
    void addLockingConstraints(std::map<std::string, std::vector<LockPairOperation> > lockpairset);
    void addForkStartConstraints(std::map<std::string, std::vector<SyncOperation> > forkset, std::map<std::string, SyncOperation> startset);
    void addJoinExitConstraints(std::map<std::string, std::vector<SyncOperation> > joinset, std::map<std::string, SyncOperation> exitset);
    void addWaitSignalConstraints(std::map<std::string, std::vector<SyncOperation> > waitset, std::map<std::string, std::vector<SyncOperation> > signalset);
    void addBarrierConstraints(std::map<std::string, std::vector<SyncOperation> > barrierset, std::map<std::string, std::vector<Operation*> > operationsByThread);
    void addAvisoConstraints(std::map<std::string, std::vector<Operation*> > operationsByThread, AvisoEventVector fulltrace);
    void openOutputFile(); //opens a file used to store the generated model
    bool solve();   //tries to solve the model and returns true if the model has a solution
    bool solveWithSolution(std::vector<std::string> solution, bool invertBugCond);   //solve the model with the bug condition inverted and a solution, in order to find the buggy constraints
    void closeSolver();  //close the output file and the pipes, and kills the process running z3
    void resetSolver();  //resets the data structures in z3solver
};

#endif /* defined(__snorlaxsolver__ConstraintModelGenerator__) */

