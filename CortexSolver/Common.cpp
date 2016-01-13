//
//  Common.cpp
//  cortexsolver
//
//  Created by Nuno Machado on 06/05/15.
//  Copyright (c) 2015 Nuno Machado. All rights reserved.
//

#include "Common.h"

std::map<std::string, std::string > solutionValuesFail;
std::map<std::string, std::string > solutionValuesAlt;
std::map<std::string,std::string> solutionValues;
std::vector<Operation*> failScheduleOrd;
std::vector<std::string> altScheduleOrd;
std::map<std::string, std::vector<Operation*> > operationsByThread;    //map thread id -> vector with thread's operations

std::map<std::string, std::vector<RWOperation> > readset;              //map var id -> vector with variable's read operations
std::map<std::string, std::vector<RWOperation> > writeset;             //map var id -> vector with variable's write operations
std::map<std::string, std::vector<LockPairOperation> > lockpairset;    //map object id -> vector with object's lock pair operations
std::map<std::string, SyncOperation> startset;                    //map thread id -> thread's start operation
std::map<std::string, SyncOperation> exitset;                     //map thread id -> thread's exit operation
std::map<std::string, std::vector<SyncOperation> > forkset;            //map thread id -> vector with thread's fork operations
std::map<std::string, std::vector<SyncOperation> > joinset;            //map thread id -> vector with thread's join operations
std::map<std::string, std::vector<SyncOperation> > waitset;            //map object id -> vector with object's wait operations
std::map<std::string, std::vector<SyncOperation> > signalset;          //map object id -> vector with object's signal operations
std::map<std::string, std::vector<SyncOperation> > barrierset;          //map object id -> vector with object's signal operations
std::vector<SyncOperation> syncset;
std::vector<PathOperation> pathset;
std::map<std::string, std::map<std::string, std::stack<LockPairOperation> > > lockpairStack;   //map object id -> (map thread id -> stack with incomplete locking pairs)

std::string unsatCoreStr = "";
std::vector<int> unsatCore;
std::vector<std::string> bugCondOps;

time_t startTime;
time_t endTime;

int numEventsDifDebug = 0;
int numDepFull = 0;
int numDepDifDebug = 0;