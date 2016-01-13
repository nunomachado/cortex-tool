//
//  Schedule.cpp
//  SymbiosisSolverXcode
//
//  Created by Daniel Ribeiro Quinta on 20/03/15.
//  Copyright (c) 2015 Daniel Ribeiro Quinta. All rights reserved.
//

#include "Schedule.h"
#include "ConstraintModelGenerator.h"
#include "Util.h"
#include <algorithm>

using namespace std;


//print Schedule
void scheduleLIB::printSch(const Schedule& sch)
{
    map<string,int> tabs;
    int t = 0 ; //counter for the number of tabs
    int i = 0; //global order iterator
    cout << "Schedule size: " << sch.size()<< endl;
    cout << "Schedule contextSwitches: " << getContextSwitchNum(sch) << endl;
   
    for(Schedule ::const_iterator it = sch.begin(); it != sch.end(); ++it) {
        
        //get the number of tabs for the thread id
        int ntabs;
        string tid = (*it)->getThreadId();
        if(tabs.count(tid)){
            ntabs = tabs[tid];
        }
        else{
            tabs[tid] = t;
            ntabs = t;
            t++;
        }
        string tabs = util::threadTabsPP(ntabs);
        cout << tabs <<"["<< i <<"] ";//<<  (*it)->getThreadId();
        cout << (*it)->getOrderConstraintName() << endl;
        i++;
    }
    cout << endl;
}


//remove empty operations from vector<string (operations)>
vector<string> cleanEmpty(const vector<string>& globalOrderTmp)
{
    vector<string> globalOrder;
    for(int i = 0; i < globalOrderTmp.size(); i++)
    {
        string op = globalOrderTmp[i];
        if(!op.empty())
            globalOrder.push_back(op);
    }
    return globalOrder;
}


//load fail Schedule or if in fixmode load alternative
void scheduleLIB::loadSchedule(const vector<string>& globalOrderTmp)
{
    map<string,vector<Operation*> > t2op = operationsByThread;
    //clean empty positions in globalOrder
    vector<string> globalOrder = cleanEmpty(globalOrderTmp);
    Schedule scheduleTmp;
    scheduleTmp.clear();
    scheduleTmp.reserve(globalOrder.size());
    
    if(!dspMode)
    {
        for(int i = 0; i < globalOrder.size(); i++)
        {
            string op = globalOrder[i];
            
            string tid = util::parseThreadId(op);
            
            //fill failScheduleOrd
            util::fillScheduleOrd(tid, &t2op, &scheduleTmp);
        }
        failScheduleOrd.clear();
        failScheduleOrd.reserve(globalOrder.size());
        failScheduleOrd = scheduleTmp;
    }
    else
        altScheduleOrd = globalOrder;
}


// transform a given schedule in a strings' vector
std::vector<std::string> scheduleLIB::schedule2string(const Schedule& schedule)
{
    vector<string> listOp;
    for(Schedule::const_iterator it = schedule.begin() ; it != schedule.end(); it ++)
        listOp.push_back((*it)->getOrderConstraintName());
    return listOp;
}


//save a schedule to a file
void scheduleLIB::saveScheduleFile(string filename, const vector<string>& listOp, const map<string, string>& traceComb){
    
    string solConst;
    std::ofstream solFile;
    solFile.open(filename, ios::trunc); // open the output file to store the solution
    if(!solFile.is_open())
    {
        cerr << " -> Error opening file " << formulaFile << ".\n";
        solFile.close();
        exit(0);
    }
    cout << "Saving solution to file: " << filename << endl;
    
    if(!traceComb.empty()){
        for(map<string,string>::const_iterator it = traceComb.begin(); it != traceComb.end();++it){
            solFile << "<" << it->first << it->second << ">\n";
        }
    }
    
    //write failing schedule to file
    for(int i = 0; i < listOp.size()-1; i++){
        solFile << listOp[i] << endl;
    }
    solFile.close();
}


//return the number of context switches in a schedule
int scheduleLIB::getContextSwitchNum(const Schedule& sch){
    int count = 0;
    string oldTid = sch[0]->getThreadId();
    for(Schedule::const_iterator it = sch.begin(); it != sch.end()-1; it++)
    {
        string nextTid = (*(it+1))->getThreadId();
        if (nextTid != oldTid)
            count++;
        oldTid = nextTid;
    }
    return count;
}


//return the operation thread ID
string scheduleLIB::getTidOperation(Operation op)
{
    return op.getThreadId();
}


// return TID size with the TID start position
int scheduleLIB::getTEIsize(Schedule schedule, int initPosition)
{
    string tid = getTidOperation(*schedule[initPosition]); //(schedule,initPosition);
    int size = 1;
    
    //size incrementation until the thread ID of the next action is different
    for(Schedule::iterator it = schedule.begin()+initPosition+1; it != schedule.end(); it++){
        if (tid == getTidOperation(**it))
            size++;
        else break;
    }
    return size;
}


//insert TEI in a schedule in a given position
Schedule scheduleLIB::insertTEI(Schedule schedule, int newPosition, Schedule tei)
{
    Schedule::iterator newPositionIt = schedule.begin()+newPosition;
    schedule.insert(newPositionIt,tei.begin(),tei.end());
    return schedule;
}


//remove TEI from a schedule with a given position
Schedule scheduleLIB::removeTEI(Schedule schedule, int initPosition)
{
    int size = getTEIsize(schedule,initPosition);
    Schedule::iterator it = schedule.begin()+initPosition;
    schedule.erase(it,it+size);
    return schedule;
}


Schedule scheduleLIB::getTEI(Schedule schedule, int startPostion){
    int size = getTEIsize(schedule, startPostion);
    
    //Schedule tei = schedule; if erase deletes the main struct
    Schedule::iterator it = schedule.begin();
    schedule.erase(it+startPostion+size, schedule.end());     //erase TAIL
    it = schedule.begin();
    schedule.erase(it,it+startPostion);     //erase HEAD
    return schedule;
}


//change TEI block to another location (TEI - thread execution interval)
Schedule scheduleLIB::moveTEISch(Schedule list,int newPositon, int oldPosition)
{
    Schedule tei = getTEI(list,oldPosition);
    return insertTEI(removeTEI(list,oldPosition),newPositon+1, tei);
}


//check if action in a given position is the last one in its TEI
bool scheduleLIB::isLastActionTEI(Schedule sch, int pos)
{
    string Tid = getTidOperation(*sch[pos]);
    //cout << (*sch[pos]).getConstraintName() << endl;
    if(pos < sch.size()-1)
    {
        string nextTid = getTidOperation(*sch[pos+1]);
        return (Tid != nextTid);
    }
    else
        return false;
}


//return next action positon within the same thread: < 0 false | >= 0 next action position
int scheduleLIB::hasNextTEI(Schedule sch, int pos)
{
    int nextTEIPosition = -1;
    string Tid = getTidOperation(*sch[pos]);
    string nextTid = "";
    for (Schedule::iterator it= sch.begin()+pos+1; it != sch.end(); it ++)
    {
        nextTid = getTidOperation(**it);
        if(Tid == nextTid)
            return (int) distance(sch.begin(),it);
    }
    return nextTEIPosition;
    
}

//create a string vector of actions, e.i. used in solver.
vector<string> scheduleLIB::getSolutionStr(Schedule schedule){
    vector<string> actionsList;
    int i=0 ;
    
    for(Schedule::iterator it = schedule.begin(); it != schedule.end(); it++)
    {
        actionsList.push_back((*it)->getOrderConstraintName());
        i++;
    }
    return actionsList;
}


// return a new valid Schedule with a shorter or equal solution using a moveUpTei algorithm
Schedule scheduleLIB::moveUpTEI(Schedule schedule,ConstModelGen *cmgen, bool isReverse)
{
    int i=0;
    int prox = -1; // < 0 false ; >= 0 represents the action posion
    
    Schedule currentSch = schedule;
    Schedule oldSch = currentSch;
    Schedule reverseSch;
    bool valid;
    
    for(Schedule::iterator it = schedule.begin(); it != schedule.end();it++)
    {
        if(isLastActionTEI(currentSch,i))
        {
            prox = hasNextTEI(currentSch,i);
            if(prox != -1)
            {
                currentSch = moveTEISch(currentSch,i,prox);
                if(isReverse) //moveDownTEI uses also this function but with isReverse=true
                {
                    reverseSch = currentSch;
                    reverse(reverseSch.begin(),reverseSch.end());
                    valid = cmgen->solveWithSolution(getSolutionStr(reverseSch), false);
                }
                else
                    valid = cmgen->solveWithSolution(getSolutionStr(currentSch), false);
                
                if (valid)
                    oldSch = currentSch; // save the new solution in oldSch
                else
                    currentSch = oldSch; // return to the last valid solution
            }
        }
        i++;
    }
    return oldSch;
}


// return a new valid schedule with a shorter or equal using a reverse moveUpTei algorithm
Schedule scheduleLIB::moveDownTEI(Schedule schedule, ConstModelGen *cmgen)
{
    reverse(schedule.begin(),schedule.end());
    
    Schedule sch = moveUpTEI(schedule, cmgen, true);
    
    reverse(sch.begin(),sch.end());
    
    return sch;
}



// return a new valid schedule with a shorter or equal using moveUpTei and moveDownTei algorithms
Schedule scheduleLIB::scheduleSimplify(Schedule schedule, ConstModelGen *cmgen)
{
    Schedule currentSch = schedule;
    Schedule oldSch = schedule;
    bool continueS = true ;
    int count = 0;
    bool notReverse = false;
    
    cout << "##### CONTEXT SWITCH REDUCTION ALGORITHM\n";
    
    while(continueS)
    {
        cout << "\n> Iteration " << count << endl;
        //removeLastTEI
        //### NOT IMPLEMENTED ###
        
        //move-Up-TEI
        currentSch = scheduleLIB::moveUpTEI(currentSch, cmgen, notReverse);
        
        //move-Down-TEI
        currentSch = scheduleLIB::moveDownTEI(currentSch, cmgen); //calls moveUpTEI but with NotReverse = true!
        
        if(getContextSwitchNum(currentSch) < getContextSwitchNum(oldSch))
        {
            oldSch = currentSch;    //simplification continues
            count++;
            cout << count << " simplifications." << endl;
        }
        else
            continueS = false; //simplification without effect, exit cycle.
    }
    return oldSch;
}
