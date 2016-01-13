//
//  Schedule.h
//  SymbiosisSolverXcode
//
//  Created by Daniel Ribeiro Quinta on 20/03/15.
//  Copyright (c) 2015 Daniel Ribeiro Quinta. All rights reserved.
//

#ifndef __SymbiosisSolverXcode__Schedule__
#define __SymbiosisSolverXcode__Schedule__

#include <stdio.h>
#include "Common.h"
#include "ConstraintModelGenerator.h"


namespace scheduleLIB{

    int getContextSwitchNum(const Schedule& sch);     //return the number of context switches
    void printSch (const Schedule& sch);     //Print Schedule using OrderConstraintName
    void saveScheduleFile(std::string filename, const std::vector<std::string>& listOperation, const std::map<std::string, std::string>& traceComb);  //save a given operation's vector and store it in a given filename
    void loadSchedule(const std::vector<std::string>& globalOrderTmp); //fill failScheduleOrd and altScheduleOrd
    std::string getTidOperation(Operation op);     //return the operation/action thread ID
    int getTEIsize(Schedule schedule, int initPosition);     // receive TID start position and return TID size with the
    bool isLastActionTEI(Schedule sch, int pos);    //cheeck if action in a given position is the last one in its TEI
    int hasNextTEI(Schedule sch, int pos);     //return next action positon within the same thread: < 0 false | >= 0 next action position
    
    std::vector<std::string> schedule2string(const Schedule& schedule); // transform a given schedule do a string's vector
    std::vector<std::string> getSolutionStr(Schedule schedule);  //create a string vector of actions, e.i. used in solver.
    
    Schedule insertTEI(Schedule schedule, int newPosition, Schedule tei); //insert TEI in a schedule
    Schedule removeTEI(Schedule schedule, int initPosition); //removeTEI from a schedule
    Schedule getTEI(Schedule schedule, int startPostion);
    
    //functions for the Simplification Algorithm
    Schedule moveTEISch(Schedule list,int newPositon, int oldPosition);    //change TEI block to another location (TEI - thread execution interval)
    Schedule moveUpTEI(Schedule schedule,ConstModelGen *cmgen, bool isReverse); // return a new valid Schedule with a shorter or equal solution using a moveUpTei algorithm
    Schedule moveDownTEI(Schedule schedule,ConstModelGen *cmgen); // return a new valid schedule with a shorter or equal using a reverse moveUpTei algorithm
    Schedule scheduleSimplify(Schedule schedule,ConstModelGen *cmgen); // return a new valid schedule with a shorter or equal using moveUpTei and moveDownTei algorithms
}

#endif /* defined(__SymbiosisSolverXcode__Schedule__) */
