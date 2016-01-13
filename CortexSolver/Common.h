//
//  Common.h
//  cortexsolver
//
//  Created by Nuno Machado on 06/05/15.
//  Copyright (c) 2015 Nuno Machado. All rights reserved.
//

#ifndef __cortexsolver__Common__
#define __cortexsolver__Common__

#include <stdio.h>
#include <vector>
#include <map>
#include <stack>
#include "Operations.h"

//global vars
typedef std::vector<Operation*> Schedule;
extern std::map<std::string, std::string > solutionValuesFail;
extern std::map<std::string, std::string > solutionValuesAlt;
extern std::map<std::string,std::string> solutionValues; // stores values of SMT solution
extern std::map<std::string, std::vector<Operation*> > operationsByThread;    //map thread id -> vector with thread's operations
extern std::vector<Operation*> failScheduleOrd; // vector to store the fail schedules operations in order
extern std::vector<std::string> altScheduleOrd;    // vector to store the alternate schedules operations in order, TODO std::vector<Operation*>


extern std::map<std::string, std::vector<RWOperation> > readset;              //map var id -> vector with variable's read operations
extern std::map<std::string, std::vector<RWOperation> > writeset;             //map var id -> vector with variable's write operations
extern std::map<std::string, std::vector<LockPairOperation> > lockpairset;    //map object id -> vector with object's lock pair operations
extern std::map<std::string, SyncOperation> startset;                    //map thread id -> thread's start operation
extern std::map<std::string, SyncOperation> exitset;                     //map thread id -> thread's exit operation
extern std::map<std::string, std::vector<SyncOperation> > forkset;            //map thread id -> vector with thread's fork operations
extern std::map<std::string, std::vector<SyncOperation> > joinset;            //map thread id -> vector with thread's join operations
extern std::map<std::string, std::vector<SyncOperation> > waitset;            //map object id -> vector with object's wait operations
extern std::map<std::string, std::vector<SyncOperation> > signalset;          //map object id -> vector with object's signal operations
extern std::map<std::string, std::vector<SyncOperation> > barrierset;          //map object id -> vector with object's signal operations
extern std::vector<SyncOperation> syncset;
extern std::vector<PathOperation> pathset;
extern std::map<std::string, std::map<std::string, std::stack<LockPairOperation> > > lockpairStack;   //map object id -> (map thread id -> stack with incomplete locking pairs)

extern std::string unsatCoreStr; //string containing the whole unsat core expression (used by the trace analyzer to parse the conflicting PCs)
extern std::vector<int> unsatCore;  //vector to store the core (i.e. the constraints) of an unsat model (this is only used in the bug-fixing mode, to store which events of the failing schedule cause the non-bug condition to be unsat)
//std::map<std::string, std::vector<Operation*> > operationsByThread;    //map thread id -> vector with thread's operations
extern std::vector<std::string> bugCondOps; //operations/events that appear in the bug condition

//vars to measure solving time
extern time_t startTime;
extern time_t endTime;

//vars for statistics of differential debugging
extern int numEventsDifDebug;   //number of events in the root-cause
extern int numDepFull;         //number of data-dependencies in the full failing schedule
extern int numDepDifDebug;      //number of data-dependencies in the differential debugging schedule

#endif /* defined(__cortexsolver__Common__) */
