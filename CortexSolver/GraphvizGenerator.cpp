//
//  GraphvizGenerator.cpp
//  symbiosisSolver
//
//  Created by Nuno Machado on 30/07/14.
//  Copyright (c) 2014 Nuno Machado. All rights reserved.
//

#include <iostream>
#include <fstream>
#include <sstream>
#include <set>
#include <algorithm>
#include <unistd.h>

#include <fcntl.h> //for read
#include <iostream>
#include <fstream>
#include <vector>
#include "Z3Solver.h"
#include "GraphvizGenerator.h"
#include "Util.h"
#include "Parameters.h"
#include <string>
#include <iostream>
#include <string>
#include <algorithm>

#define LINEMAX 256


using namespace std;

map<string,string> threadColors; //map: thread id -> banner color (used to distinguish threads in the graph)

map<string,string> readDependFail;  //[failing schedule] map: R operation -> last W operation on the same var (this means that R is data dependent on W)
map<string,string> readDependAlt;   //[alternate schedule] map: R operation -> last W operation on the same var

map<string,vector<string>> writeDependFail; //[failing schedule] map: W operation -> R operations on the same var affected by this W
map<string,vector<string>> writeDependAlt; //[alternate schedule] map: W operation -> R operations on the same var affected by this W

map<string,string> exclusiveFail;   //same as above, but only for the unique dependencies on failing schedule
map<string,string> exclusiveAlt;    //same as above, but only for the unique dependencies on failing schedule

map<string,int> dependIdsFail; //map: operation -> position the failing schedule
map<string,int> dependIdsAlt;  //map: operation -> position the alt schedule

map<int,string> lockVariables; //map: lockID -> lockVariable

set<string> relevantThreads; //set containing the threads that are relevant (i.e. that have operations with exclusive data-dependencies)

int altCounter = 0; //counts the number of alternate schedules (used to name the output files)
pid_t lineCode_pid;                          //id of the process running getCodeLine()
int procR, procW;                           //pipes to read from and write to the process running getCodeLine()
std::vector<std::string> operationsVars;    //vector containing the operation variables
std::vector<std::string> orderVars;         //vector containing the operation order variables
int numOps;                             //number of operations to ordered


/*
 * Returns a port name if the operation is involved in a dependence
 * in the failing schedule. Otherwise, returns empty string.
 */
string getDependencePort(string op, string schType)
{
    map<string,string> exclusiveAux;
    if (schType=="fail")
        exclusiveAux = exclusiveFail;
    
    else
        exclusiveAux = exclusiveAlt;
    
    int counter = 1;
    for(map<string,string>::iterator it = exclusiveAux.begin(); it!=exclusiveAux.end();++it)
    {
        if(!op.compare(it->first))
            return (util::stringValueOf(counter)+"1");
        
        if(!op.compare(it->second))
            return (util::stringValueOf(counter)+"2");;
        
        counter++;
    }
    
    return "";
}


/*
 * Replaces CHAR "specialChar" (unsupported by graphviz) to "htmlCode"
 *
 * Example:
 *            ">" to "&gt"
 *            "&" to "&amp"
 */
string changeChar2graphviz(string specialChar, string htmlCode ,string op){
    
    size_t pos = op.find(specialChar);
    while(pos != string::npos)
    {
        op.replace(pos,1,htmlCode);
        pos++;
        pos = op.find(specialChar,pos);
    }
    return op;
};



// Remove line comments from an operation
string removeComment(string op)
{
    size_t pos = op.find("//");
    if(pos!= string::npos)
        op.resize(pos);
    
    return op;
}


// Changes from the operation name the characters unsupported by graphviz
string cleanOperation(string op)
{
    op = removeComment(op);
    op = changeChar2graphviz("&","&amp;",op);
    op = changeChar2graphviz(">","&gt;",op);
    op = changeChar2graphviz("<","&lt;",op);
    return op;
}


/*
 * return filename from an operation
 *
 * Example: Retrive "SimpleAssertKLEE.c" from "[18] OS-lock_416680994-2-0&SimpleAssertKLEE.c@45"
 */
string getFilenameOp(string op)
{
    int filenameInitP = (int)op.find("&")+1;
    int filenameEndP = (int)op.find("@");
    return op.substr(filenameInitP,filenameEndP-filenameInitP);
}


/*
 * return operation line in de source code
 *
 * Example: "45", expecting to receive something like: [18] OS-lock_416680994-2-0&SimpleAssertKLEE.c@45
 */
int getLineOp(string op)
{
    int lineInitP = (int)op.find("@")+1;
    return util::intValueOf(op.substr(lineInitP,op.length()));
}


/*
 * return operation variable id
 *
 * Example: "416680994", expecting to receive something like: [18] OS-lock_416680994-2-0&SimpleAssertKLEE.c@45
 */
int getVarID(string op)
{
    int initP = (int)op.find("_")+1;
    op = op.substr(initP,op.length());
    int endP = (int)op.find("-");
    return util::intValueOf(op.substr(0,endP));
}



/*
 * return variable name from a code line
 *
 * Example: "&lock", from "pthread_mutex_lock(&lock);"
 */
string getVarNameFromCodeLine(string codeLine)
{
    int init = (int)codeLine.find("(")+1;
    int end = (int)codeLine.find(")");
    return codeLine.substr(init,end-init);
}


/*
 * return variable name from source code
 *
 * Example: return "&lock", from fileName in line 43
 */
string getLockVarName(string filename, int line)
{
    string codeLine = "lock";
    //if(!sourceFilePath.empty())
       // codeLine = graphgen::getCodeLine(line, filename,"lock"); //Nuno: search for actual line of code
    return getVarNameFromCodeLine(codeLine);
}


// store a new pair<variable id, variable name>
void storeLockPair(int lockVarID,string lockVarName)
{
    lockVariables.insert(pair<int,string>(lockVarID,lockVarName));
}


// get the variable name from a map using its id
string getVarName(int varID)
{
    return lockVariables[varID];
}


// store a lockVarName with its ID
void fillMaplockVariables(string op)
{
    //expecting something like: [18] OS-lock_416680994-2-0&SimpleAssertKLEE.c@45
    string filename = getFilenameOp(op); // SimpleAssertKLEE.c
    int line = getLineOp(op);          //45
    int lockVarID = getVarID(op);  //416680994
    
    string lockVarName = getLockVarName(filename,line);
    storeLockPair(lockVarID,lockVarName);
}




/**
 *  For each alternate schedule, draws a graphviz graph of the thread segments that differ
 *  between the failing schedule and the alternate schedule. Moreover, it highlights the
 *  the segments that became atomic and the data-flow dependencies that changed.
 */
void graphgen::genAllGraphSchedules(vector<string> failSchedule, map<EventPair, vector<string> > altSchedules)
{
    //** define thread colors
    threadColors["0"] = "salmon";
    threadColors["1"] = "lightsteelblue2";
    threadColors["1.1"] = "cyan4";
    threadColors["1.2"] = "dodgerblue";
    threadColors["2"] = "darkolivegreen3";
    threadColors["3"] = "mediumpurple";
    threadColors["4"] = "darkgoldenrod1";
    threadColors["5"] = "khaki1";
    threadColors["6"] = "darkviolet";
    threadColors["7"] = "blue";
    threadColors["8"] = "firebrick1";
    threadColors["9"] = "chocolate4";
    threadColors["10"] = "red";
    
    //** compute data-dependencies for failSchedule
    for(int oit = (int)failSchedule.size()-1; oit >= 0;oit--)
    {
        string opA = failSchedule[oit];
        
        //fill map lockVariables
        if(opA.find("OS-lock")!= string::npos)
            fillMaplockVariables(opA);
        
        if(opA.find("OR-") == string::npos)
            continue;
        
        string varA = util::parseVar(opA);
        for(int iit = oit; iit >= 0; iit--)
        {
            string opB = failSchedule[iit];
            string varB = util::parseVar(opB);
            
            if(varA==varB && opB.find("OW")!=string::npos)
            {
                //cout << "   debug: varA " << varA << " == varB " << varB << "\n";
                //cout << "   debug: " << opA << " <-- " << opB << "\n";
                readDependFail[opA] = opB; //add dependence "A is data dependent on B"
                if(writeDependFail.count(opB))
                    writeDependFail[opB].push_back(opA);
                
                else{
                    vector<string> tmpRs;
                    tmpRs.push_back(opA);
                    writeDependFail[opB] = tmpRs;
                }
                
                dependIdsFail[opA] = oit; //add A's position in the schedule
                dependIdsFail[opB] = iit; //add B's position in the schedule
                numDepFull++;
                break;
            }
        }
    }
    
    for(map<EventPair, vector<string> >::iterator it = altSchedules.begin(); it!=altSchedules.end(); ++it)
    {
        genGraphSchedule(failSchedule, it->first, it->second);
        altCounter++;
    }
}



//** compute data-dependencies for Schedule
void computeDataDependencies(vector<string> schedule){
    
    for(int oit = (int)schedule.size()-1; oit >= 0; oit--)
    {
        string opA = schedule[oit];
        
        if(opA.find("OR-") == string::npos)
            continue;
        
        string varA = util::parseVar(opA);
        
        for(int iit = oit; iit >= 0; iit--)
        {
            string opB = schedule[iit];
            string varB = util::parseVar(opB);
            
            if(varA==varB && opB.find("OW")!=string::npos)
            {
                readDependAlt[opA] = opB; //add dependence "A is data dependent on B"
                if(writeDependAlt.count(opB)){
                    writeDependAlt[opB].push_back(opA);
                }
                else{
                    vector<string> tmpRs;
                    tmpRs.push_back(opA);
                    writeDependAlt[opB] = tmpRs;
                }
                dependIdsAlt[opA] = oit; //add A's position in the schedule
                dependIdsAlt[opB] = iit; //add B's position in the schedule
                break;
            }
        }
    }
}


// check whether operations in the read-dependencies have the same variable as those of the unsat core
bool isUnsatCoreOp(string varFail)
{
    bool sameVar = false;
    for(vector<string>::iterator it = bugCondOps.begin(); it!=bugCondOps.end();++it)
    {
        string varBCO = util::parseVar(*it);
        if(varFail==varBCO)
            return true;
    }
    return sameVar;
}


//** compute exclusive dependencies (i.e. dependencies that appear only in the failing
//** schedule or in the alternate schedule)
void computeExclusiveDependencies(vector<int>* exclusiveFailIds, vector<int>* exclusiveAltIds)
{
    map<string,string> readDependCommon; //used to compute dependencies when there are control-flow diffs between the failing and the non-failing schedules
    for(map<string,string>::iterator dit = readDependFail.begin(); dit!=readDependFail.end(); ++dit)
    {
        string writeFail = dit->second;
        string readOp = dit->first;
        string varFail = util::parseVar(writeFail);
        
        // check whether operations in the read-dependencies have the same variable as those of the unsat core
        if(dspFlag=="short" && !isUnsatCoreOp(varFail))
            continue;
        
        string writeAlt = "";
        if(readDependAlt.find(readOp)!=readDependAlt.end()){
            writeAlt = readDependAlt[readOp];
            readDependCommon[readOp] = writeAlt;
        }
        
        if(writeFail!=writeAlt)
        {
            //FAILING SCHEDULE
            if(writeFail!=""){
                exclusiveFail[readOp] = writeFail;
                exclusiveFailIds->push_back(dependIdsFail[readOp]);
                exclusiveFailIds->push_back(dependIdsFail[writeFail]);
                cout << "Exclusive Fail:\t" << readOp << " <-- " << writeFail << "\n";
                
                //add relevant threads
                relevantThreads.insert(util::parseThreadId(readOp));
                relevantThreads.insert(util::parseThreadId(writeFail));
            }
            
            //ALT SCHEDULE
            if(writeAlt!=""){
                exclusiveAlt[readOp] = writeAlt;
                exclusiveAltIds->push_back(dependIdsAlt[readOp]);
                exclusiveAltIds->push_back(dependIdsAlt[writeAlt]);
                cout << "Exclusive Alt:\t" << readOp << " <-- " << writeAlt << "\n";
                
                //add relevant threads
                relevantThreads.insert(util::parseThreadId(writeAlt));
            }
            numDepDifDebug++;
        }
    }
    
    //compute dependencies for different operations (if there are any)
    for(map<string,string>::iterator dit = readDependAlt.begin(); dit!=readDependAlt.end(); ++dit)
    {
        string readOp = dit->first;
        string writeAlt = dit->second;
        if(readDependCommon.find(readOp)==readDependCommon.end()){ //this operation appears only on the alternate schedule
            exclusiveAlt[readOp] = writeAlt;
            exclusiveAltIds->push_back(dependIdsAlt[readOp]);
            exclusiveAltIds->push_back(dependIdsAlt[writeAlt]);
            cout << "Exclusive Alt:\t" << readOp << " <-- " << writeAlt << "\n";
            
            //add relevant threads
            relevantThreads.insert(util::parseThreadId(writeAlt));
        }
    }
    
    //** sort ids in ascending order
    sort(exclusiveFailIds->begin(),exclusiveFailIds->end());
    sort(exclusiveAltIds->begin(),exclusiveAltIds->end());
}


//adding adjacents locks and unlocks from an exclusive ScheduleID vector
void addLockOp2Dependencies(const vector<string>& schedule, ThreadSegment* tseg, int i, vector<int>* exclusiveSchIds)
{
    int j,k;
    //find potential wrapping locking region
    for(j = i-1; j >= tseg->initPos; j--)
    {
        if(schedule[j].find("S-unlock")!= string::npos){
            break;
        }
        else if(schedule[j].find("S-lock")!= string::npos){
            
            //add new dependencies, if necessary
            if(find(exclusiveSchIds->begin(),exclusiveSchIds->end(),j) == exclusiveSchIds->end()){
                //cout << "addlockdependencies: "<< schedule[j] << endl;
                (tseg->dependencies).push_back(j);
                exclusiveSchIds->push_back(j); //add lock id to exclusive
            }
            
            //we've found a lock; find the corresponding unlock
            for(k = i+1; k <= tseg->endPos; k++)
            {
                if(schedule[k].find("S-unlock")!= string::npos){
                    
                    //add new dependencies, if necessary
                    if(find(exclusiveSchIds->begin(),exclusiveSchIds->end(),k) == exclusiveSchIds->end()){
                        //cout << "addlockdependencies: "<< schedule[k] << endl;
                        (tseg->dependencies).push_back(k);
                        exclusiveSchIds->push_back(k); //add unlock id to exclusive
                    }
                    break; //found corresponding unlock
                }
            }
        }
    }
    
}


//Build segment struct from a schedule
void computeSingleSegment(const vector<string>& schedule, vector<ThreadSegment>* segsList, vector<int>* exclusiveSchIds, int initSeg, int oit, string prevTid)
{
    int dependIt = 0; //iterator for exclusiveFail/AltIds; allows to check if a given block encompasses operations with dependencies or not
    
    ThreadSegment tseg;
    tseg.initPos = initSeg;
    tseg.endPos = oit-1; //the endPos is the last operation of the previous segment
    tseg.markAtomic = false;
    tseg.hasDependencies = false;
    tseg.tid = prevTid;
    
    //check if the current block comprises operations with dependencies
    int exsize = exclusiveSchIds->size();
    for(int dit = dependIt; dit < exsize; dit++) // check if there are exclusive operations (position) that compreend the segment borders (init and end)
    {
        dependIt = dit;
        
        if(tseg.endPos < (*exclusiveSchIds)[dit])
            break;
        
        if(tseg.initPos > (*exclusiveSchIds)[dit])
            continue;
        
        tseg.hasDependencies = true;
        tseg.dependencies.push_back((*exclusiveSchIds)[dit]); // add new position to segment.dependencies
        addLockOp2Dependencies(schedule,&tseg,(*exclusiveSchIds)[dit], exclusiveSchIds); //add lock and unlock to seg.dependencies
        
    }
    //only add segments of relevant threads
    if(relevantThreads.find(tseg.tid)!=relevantThreads.end())
        segsList->push_back(tseg);
    
}


// Compute thread segments for both schedules
vector<ThreadSegment> computeSegments( const vector<string>& schedule, vector<int>* exclusiveSchIds)
{
    vector<ThreadSegment> segsList;
    string op = schedule[0];
    string prevTid = util::parseThreadId(op); //indicates the last thread id observed
    
    int initSeg = 0;  //indicates the start of the current segment
    
    int oit;
    for(oit = 1; oit < schedule.size(); oit++)
    {
        op = schedule[oit];
        string tid = util::parseThreadId(op);
        
        if(tid != prevTid)
        {
            computeSingleSegment(schedule, &segsList, exclusiveSchIds, initSeg, oit, prevTid);
            prevTid = tid;
            initSeg = oit;
        }
    }
    //** handle last case
    computeSingleSegment(schedule, &segsList, exclusiveSchIds, initSeg, oit, prevTid);
    
    return segsList;
}


//for each exclusive write in the alt schedule, add all data-dependencies from
//reads in the same thread of the exclusive read (i.e. mark all reads that are affected by that particular write)
void addAllReadDependencies(vector<int>* exclusiveFailIds,vector<int>* exclusiveAltIds)
{
    for(map<string,string>::iterator ait = exclusiveAlt.begin(); ait!=exclusiveAlt.end();++ait)
    {
        string writeAlt = ait->second;
        string readAlt = ait->first;
        string tid = util::parseThreadId(readAlt);
        
        for(vector<string>::iterator rit = writeDependAlt[writeAlt].begin();
            rit != writeDependAlt[writeAlt].end();++rit)
        {
            string tmpR = *rit;
            if(tmpR.compare(readAlt) && tid==util::parseThreadId(tmpR))
            {
                exclusiveAlt[tmpR] = writeAlt;
                exclusiveAltIds->push_back(dependIdsAlt[tmpR]);
                cout << "Relevant Alt:\t" << tmpR << " <-- " << writeAlt << "\n";
            }
        }
        
        for(vector<string>::iterator rit = writeDependFail[writeAlt].begin();
            rit != writeDependFail[writeAlt].end();++rit)
        {
            string tmpR = *rit;
            if(!exclusiveFail.count(tmpR) && tid==util::parseThreadId(tmpR))
            {
                exclusiveFail[tmpR] = writeAlt;
                exclusiveFailIds->push_back(dependIdsFail[tmpR]);
                cout << "Relevant Fail:\t" << tmpR << " <-- " << writeAlt << "\n";
            }
        }
    }
}


//**    2) cut-off irrelevant, common prefix
void cutOffPrefix( vector<ThreadSegment>* segsFail, vector<ThreadSegment>* segsAlt, vector<string>* failSchedule, vector<string>* altSchedule)
{
    vector<ThreadSegment>::iterator fit = segsFail->begin();
    
    //for each segment check if the the corresponding segment in alt schedule is of the same size of not
    while(fit != segsFail->end())
    {
        vector<ThreadSegment>::iterator ait = segsAlt->begin();
        while(ait != segsAlt->end() && fit != segsFail->end())
        {
            ThreadSegment fseg = *fit;
            ThreadSegment aseg = *ait;
            
            string fOp = (*failSchedule)[fseg.initPos]; //first operation of the fail segment
            string aOp = (*altSchedule)[aseg.initPos]; //first operation of the alt segment
            
            if(fOp.compare(aOp)){   //if operations are different, then proceed
                
                ait++;
            }
            else
            {
                int fsize = fseg.endPos - fseg.initPos;
                int asize = aseg.endPos - aseg.initPos;
                
                if(!aseg.hasDependencies &&
                   !fseg.hasDependencies &&
                   asize == fsize)
                {
                    fit = segsFail->erase(fit);
                    ait = segsAlt->erase(ait);
                    
                    if(fit==segsFail->end())
                        break;
                    else
                        continue;
                }
                else if(asize > fsize && aseg.hasDependencies)
                    ait->markAtomic = true;
                
                //move forward in the failing segments and restart iterating over the alt segments
                fit++;
                ait = segsAlt->begin();
            }
        }
        if(fit==segsFail->end())
            break;
        
        else
            fit++;
    }
}



//new - cutoff identical events within thread segments (this is not optimized, as it could have been done in the previous cycle..)
void cutOffIdenticalEvents(vector<ThreadSegment>* segsFail, vector<ThreadSegment>* segsAlt, vector<string>* failSchedule, vector<string>* altSchedule)
{
    for(int i = 0; i < 2; i++){
        
        vector<ThreadSegment>::iterator fit = segsFail->begin();
        while(fit != segsFail->end())
        {
            vector<ThreadSegment>::iterator ait = segsAlt->begin();
            while(ait != segsAlt->end() && fit != segsFail->end())
            {
                //prune common prefix within the segments, until operations are different or one of them has a dependency
                string fOp = (*failSchedule)[fit->initPos]; //first operation of the fail segment
                string aOp = (*altSchedule)[ait->initPos]; //first operation of the alt segment
                
                if(fOp == aOp){
                    bool isDependency = false;
                    while(fOp == aOp && !isDependency && fit->initPos <= fit->endPos && ait->initPos <= ait->endPos)
                    {
                        //check whether the head operations are involved in a dependency or not; stop pruning if so
                        for(vector<int>::iterator tmpit = fit->dependencies.begin(); tmpit != fit->dependencies.end(); ++tmpit){
                            if(fit->initPos == *tmpit){
                                isDependency = true;
                                break;
                            }
                        }
                        
                        for(vector<int>::iterator tmpit = ait->dependencies.begin(); tmpit != ait->dependencies.end() && !isDependency; ++tmpit){
                            if(ait->initPos == *tmpit){
                                isDependency = true;
                                break;
                            }
                        }
                        
                        if(!isDependency){
                            if(fit->initPos <= fit->endPos)
                                fit->initPos++;
                            if(ait->initPos <= ait->endPos)
                                ait->initPos++;
                            fOp = (*failSchedule)[fit->initPos];
                            aOp = (*altSchedule)[ait->initPos];
                        }
                        
                        //check if, after pruning the common suffix, there are other blocks
                        //that start by the same operation (this is useful when the block atomicity
                        //is broken from the failing to the alternate schedule)
                        /*if(fOp != aOp && ait != segsAlt->end()){
                         vector<ThreadSegment>::iterator tmpait = ait;
                         while(fOp != aOp && tmpait != segsAlt->end()){
                         tmpait++;
                         aOp = (*altSchedule)[tmpait->initPos];
                         }
                         }*/
                    }
                    
                    //prune common suffix within the segments, until operations are different or one of them has a dependency
                    fOp = (*failSchedule)[fit->endPos]; //last operation of the fail segment
                    aOp = (*altSchedule)[ait->endPos]; //last operation of the alt segment
                    
                    if(fOp == aOp){
                        bool isDependency = false;
                        while(fOp == aOp && !isDependency)
                        {
                            //check whether the head operations are involved in a dependency or not; stop pruning if so
                            for(vector<int>::iterator tmpit = fit->dependencies.begin(); tmpit != fit->dependencies.end(); ++tmpit){
                                if(fit->endPos == *tmpit){
                                    isDependency = true;
                                    break;
                                }
                            }
                            
                            for(vector<int>::iterator tmpit = ait->dependencies.begin(); tmpit != ait->dependencies.end() && !isDependency; ++tmpit){
                                if(ait->endPos == *tmpit){
                                    isDependency = true;
                                    break;
                                }
                            }
                            
                            if(!isDependency){
                                fit->endPos--;
                                ait->endPos--;
                                fOp = (*failSchedule)[fit->endPos];
                                aOp = (*altSchedule)[ait->endPos];
                            }
                        }
                    }
                    
                    //erase potential empty segments
                    if(fit->initPos > fit->endPos)
                        fit = segsFail->erase(fit);
                    
                    if(ait->initPos > ait->endPos)
                        ait = segsAlt->erase(ait);
                    
                    break;
                }
                else{
                    ait++;
                }
            }
            fit++;
        }
    }
}


//cuts out the segments containing only one operation, which is irrelevant to the DSP (e.g. exit or AssertFail)
void cutOffOrphans( vector<ThreadSegment>* segsFail, vector<ThreadSegment>* segsAlt, vector<string>* failSchedule, vector<string>* altSchedule)
{
    vector<ThreadSegment>::iterator fit = segsFail->begin();
    while(fit != segsFail->end())
    {
        if(fit->initPos == fit->endPos){
            string op = (*failSchedule)[fit->initPos];
            bool isDependency = false;
            
            //check whether the head operations are involved in a dependency or not; stop pruning if so
            for(vector<int>::iterator tmpit = fit->dependencies.begin(); tmpit != fit->dependencies.end(); ++tmpit){
                if(fit->initPos == *tmpit){
                    isDependency = true; // <<<< CHECK ON ALT DEPENDENCIES AS WELL
                    break;
                }
            }
            
            //we don't want to print the failure and exit operations
            if(!isDependency &&
               (op.find("Assert")!=string::npos
               || op.find("exit")!=string::npos)){
                fit = segsFail->erase(fit);
            }
            else
                fit++;
        }
        else
            fit++;
    }
    
    fit = segsAlt->begin();
    while(fit != segsAlt->end())
    {
        if(fit->initPos == fit->endPos){
            string op = (*altSchedule)[fit->initPos];
            bool isDependency = false;
            
            //check whether the head operations are involved in a dependency or not; stop pruning if so
            for(vector<int>::iterator tmpit = fit->dependencies.begin(); tmpit != fit->dependencies.end(); ++tmpit){
                if(fit->initPos == *tmpit){
                    isDependency = true;
                    break;
                }
            }
            
            //we don't want to print the failure and exit operations
            if(!isDependency &&
               (op.find("Assert")!=string::npos
                || op.find("exit")!=string::npos)){
                fit = segsAlt->erase(fit);
            }
            else
                fit++;
        }
        else
            fit++;
    }
}


//present fail and alternative schedules in a graph in graphviz format
void graphgen::genGraphSchedule(vector<string> failSchedule, EventPair invPair, vector<string> altSchedule)
{
    readDependAlt.clear();
    dependIdsAlt.clear();
    
    /**
     compute data-dependencies for AltSchedule
     fill global vars: writeDependAlt,readDependAlt and dependIdsAlt
     */
    computeDataDependencies(altSchedule);
    
    vector<int> exclusiveFailIds;
    vector<int> exclusiveAltIds;
    exclusiveFail.clear();
    exclusiveAlt.clear();
    computeExclusiveDependencies(&exclusiveFailIds, &exclusiveAltIds); //(i.e. dependencies that appear only in the failing schedule or in the alternate schedule)
    
    
    //for each exclusive write in the alt schedule, add all data-dependencies from
    //reads in the same thread of the exclusive read (i.e. mark all reads that are affected by that particular write
    if(dspFlag=="extended")
        addAllReadDependencies(&exclusiveFailIds,&exclusiveAltIds);
    
    //**    1) compute thread segments for both schedules
    //**    2) cut-off irrelevant, common prefix
    //**    3) cut-off threads that don't exhibit dependence changes in their segments
    //**    4) mark as "bold" the segments that became bigger in the alt schedule
    
    // Compute thread segments for both schedules
    vector<ThreadSegment> segsFail = computeSegments(failSchedule, &exclusiveFailIds);    // ---- segments for failing schedule
    vector<ThreadSegment> segsAlt = computeSegments(altSchedule, &exclusiveAltIds);       // ---- segments for alt schedule
    
    if(dspFlag!="noCuts")
    {
        // Cutoff common prefix
        cutOffPrefix( &segsFail, &segsAlt, &failSchedule, &altSchedule);
        
        //new - cutoff identical events within thread segments (this is not optimized, as it could have been done in the previous cycle..)
        cutOffIdenticalEvents(&segsFail, &segsAlt, &failSchedule, &altSchedule);
        
        //cut out orphan segments (i.e. that contain only irrelevant operations to the DSP)
        cutOffOrphans(&segsFail, &segsAlt, &failSchedule, &altSchedule);
    }
    
    //draw graphviz file
    drawGraphviz(segsFail, segsAlt, failSchedule, altSchedule, invPair);
}


void Tokenize(const string& str,
              vector<string>& tokens,
              const string& delimiters = " ")
{
    // Skip delimiters at beginning.
    string::size_type lastPos = str.find_first_not_of(delimiters, 0);
    // Find first "non-delimiter".
    string::size_type pos     = str.find_first_of(delimiters, lastPos);
    
    while (string::npos != pos || string::npos != lastPos)
    {
        // Found a token, add it to the vector.
        tokens.push_back(str.substr(lastPos, pos - lastPos));
        // Skip delimiters.  Note the "not_of"
        lastPos = str.find_first_not_of(delimiters, pos);
        // Find next "non-delimiter"
        pos = str.find_first_of(delimiters, lastPos);
    }
}


vector<string> splitVars(string vars)
{
    vector<string> varList;
    Tokenize(vars, varList,",");
    return varList;
    
}


//string scrLine = "bounded_buf_init(&buffer, 3);";
vector<string> getVarCall(string lineC)
{
    vector<string> varList;
    int startP = (int)lineC.find("(")+1;
    int endP = (int)lineC.find(")");
    string vars = lineC.substr(startP,endP- startP);
    varList = splitVars(vars);
    
    return varList;
}


//string destLine = "int bounded_buf_init(bounded_buf_t * bbuf, size_t sz)";
vector<string> getVarSignature(string lineC)
{
    vector<string> listVarsType;
    vector<string> vectorPair;
    vector<string> listVars;
    
    
    int startP = (int)lineC.find("(")+1;
    int endP = (int)lineC.find(")");
    string vars = lineC.substr(startP,endP- startP);
    
    Tokenize(vars, listVarsType,",");
    for(vector<string>::iterator it = listVarsType.begin(); it != listVarsType.end(); it++)
    {
        vectorPair.clear();
        Tokenize((*it),vectorPair," ");
        listVars.push_back(vectorPair[vectorPair.size()-1]);
    }
    return listVars;
}


string getVarValue(string op, string schType)
{
    map<string,string> solution;
    if(schType!="fail")
        solution = solutionValuesAlt;
    else
        solution = solutionValuesFail;
    
    for(map<string,string>::const_iterator it = solution.begin(); it != solution.end(); it++)
    {
        if (op.find(it->first)!= string::npos)
            return it->second;
    }
    return "";
    
}

string getVarBind(string srcLine, string destLine)
{
    vector<string> varListCall = getVarCall(srcLine);
    vector<string> varListSign = getVarSignature(destLine);
    if (varListSign.size() != varListCall.size())
    {
        cerr << "Error when binding variables, different sizes" << endl;
        exit(0);
    }
    string bindBuff = "( ";
    string separator = " , ";
    int i;
    for(i=0; i < varListCall.size(); i++)
    {
        if(i == varListCall.size()-1)
            separator = "";
        
        bindBuff = bindBuff + varListCall[i] + " to " + varListSign[i] + separator;
    }
    bindBuff = bindBuff + " )";
    return bindBuff;
}


//receive a function signature and return just its name. i.e. reveives void name(); and return "name"
string graphgen::cleanCallFunc(string funcSign)
{ //"54  \tvoid StringBuffer::getChars(...) {" returns "getChars"
    funcSign = graphgen::cleanRight(funcSign);
    int classSymb = (int)funcSign.find("::");
    if(classSymb!= string::npos)
        funcSign = funcSign.substr(classSymb+2);
    
    funcSign = funcSign.substr(0,funcSign.find("("));
    vector<string> funcType_funcName= splitVars(funcSign);
    string funcName = funcType_funcName.back();
    return funcSign;
}

//return a friendly string with parameters binding
string getFunCallFriendlyOp(string instrCall)
{//OC-FunCall-0-1&boundedBufferKLEE.c_boundedBufferKLEE.c@350_41
    vector<string> filesLines;
    Tokenize(instrCall,filesLines,"&@");
    vector<string> files;
    Tokenize(filesLines[1],files,"/");
    vector<string> lines;
    Tokenize(filesLines[2],lines,"/");
    string filenameScr = files[0];
    string filenameDest = files[1];
    int lineSrc = util::intValueOf(lines[0]);
    int lineDest = util::intValueOf(lines[1]);
    
    string srcOp = graphgen::getCodeLine(lineSrc, filenameScr, "call");
    sleep(1);  //system("someMagic!");
    string destOp = graphgen::getCodeLine(lineDest, filenameDest,"signature");
    
    if (srcOp == "" ||  destOp == "")
    {
        cerr << "Error when binding variables, str = empty" << endl;
        cerr << "func call: "<< srcOp << "!" << endl;
        cerr << "func signature: " << destOp << "!" << endl;
        exit(0);
    }
    
    string funcSign = graphgen::cleanCallFunc(destOp);
    string vars = getVarBind(srcOp, destOp);
    funcSign = "CALL "+funcSign + vars;
    return funcSign;
}

//turn operation in a pretty line of code
string makeInstrFriendly(string instruction){
    
    if (instruction.find("OC-FunCall-") != string::npos)
        return getFunCallFriendlyOp(instruction);
    
    int filenameP =  (int)instruction.find("&");
    int lineP =      (int)instruction.find("@");
    int isOSlock = (int)instruction.find("OS-lock");
    int isOSunlock = (int)instruction.find("OS-unlock");
    string friendlyInstr = instruction;
    
    if(string::npos != filenameP && string::npos != lineP)
    {
        string filename = getFilenameOp(friendlyInstr);
        int line = getLineOp(friendlyInstr);
        
        if (string::npos != isOSlock || string::npos != isOSunlock)
        {
            int varID = getVarID(friendlyInstr);
            string lockVarName = getVarName(varID);
            string lockStr = " lock(";
            if (string::npos != isOSunlock)
                lockStr = " unlock(";
            
            friendlyInstr = filename + " L"+ to_string(line) + lockStr + lockVarName+");";
            return friendlyInstr;
        }
        string codeLine = instruction;
        //if(!sourceFilePath.empty())
           // codeLine = graphgen::getCodeLine(line, filename, ""); //Nuno: search for actual line of code
        friendlyInstr = filename+" L"+codeLine;
    }
    //cout << friendlyInstr << endl;
    return friendlyInstr;
}




//remove special caracteres and white spaces between line number and operation
string cleanInitSpacesOp(string ret)
{
    if(ret == "")
        return "";
    
    //for some reason (probably to allow flushing the buffer read from the grep process), this is necessary to avoid misreading the line
    sleep(1);
    
    ret = ret.substr(ret.find_first_of("1234567890")); //remove de /x01 caracter
    
    //find line number and store it in lineN
    int endNum = 0;
    for(string::iterator itC= ret.begin(); itC != ret.end(); itC++)
    {
        if(isdigit(*itC)) //isnumber(*itC)
            endNum++;
        
        else
            break;
    }
    string lineN = ret.substr(0,endNum);
    
    //find operation initial position and store operation in strOp
    int initCode = endNum;
    for(string::iterator itC= ret.begin()+endNum+1; itC != ret.end(); itC++)
    {
        if((*itC)==' ')
            initCode++;
        else
            break;
    }
    string strOp = ret.substr(initCode);
    
    //built pretty line + operation
    ret = lineN + " " + strOp;
    
    //remove potential comments (with '//') in source code
    size_t pos = ret.find("//");
    if(pos !=string::npos){
        ret = ret.substr(0,pos);
    }
    else {
        pos = ret.find("/*");
        if(pos!=string::npos){
            ret = ret.substr(0,pos);
        }
    }
    
    return ret;
}


string look4LineWith(string token, int line, string filename)
{
    string signature = graphgen::getCodeLine(line, filename,"");
    if(signature.find(token) == string::npos)
        return look4LineWith(token, line-1, filename);
    
    return signature;
}

string graphgen::cleanRight(const string& op)
{//"    55\t                            char *dst, int dstBegin) {"
    int i = 0;
    string cleanStr = "";
    string x = op;
    for (i = 0 ; i < op.size()-1; i++)
    {
        char c = op[i];
        if(isalpha(c))
            break;
    }
    cleanStr = op.substr(i);
    return cleanStr;
}


//get operation from file using system call
string graphgen::getCodeLine(int line, string filename, string type)
{
    numOps = 0;
    
    if(filename.find("InstrumentationHandler.java")!=string::npos){
        return "0 thread-start";
    }
    
    string opendir = "cd "+ sourceFilePath;
    string line2code = "nl -ba "+ filename+" | grep  \"  "+ util::stringValueOf(line) +  "\\t\"";
    string sysExeGetCodeLine = (opendir+"; "+line2code);
    char *command = (char *)sysExeGetCodeLine.c_str();
    
    lineCode_pid = util::popen2(command, &procW, &procR);
    if (!lineCode_pid)
    {
        perror("Problems with getCodeLine pipe!");
        exit(1);
    }
    
    string ret;
    char c[LINEMAX];
    read(procR,c,1);
    while(c[0]!='\n')
    {
        ret = ret + c[0];
        read(procR,c,1);
    }
    
    if(type == "signature")
    {
        if(ret.find(")")==string::npos)
            return getCodeLine(line-1, filename, "signature");
        else if (ret.find("(")==string::npos)
            ret = look4LineWith("(", line-1,filename) + cleanRight(ret) ;
    }
    
    string op = cleanInitSpacesOp(ret);
    return op;  //remove special caracteres and white spaces between line number and operation
}


//checks if the segment contains the root cause, if in "fail" print table in red otherwise green
bool containsBugCauseOp(ThreadSegment fseg, vector<string> sch, string bugCauseStr, string type)
{
    string firstPart = bugCauseStr.substr(0, bugCauseStr.find(" should")-2);
    
    int i;
    for(i = fseg.initPos; i <= fseg.endPos; i++)
        if (sch[i].find(firstPart) != string::npos) // || sch[i].find(secondPart) != string::npos) Mark just the segment that contains the first part of the bug cause!
            return true;
    
    return false;
}


// draw/write header in graphfiz format in file
void drawHeader(ofstream &outFile, string bugSolution)
{
    outFile << "digraph G {\n\tcenter=1;\n\tranksep=.25; size = \"7.5,10\";\n\tnode [shape=record]\n\n";
    outFile << "labelloc=top;\n";
    outFile << "labeljust=left;\n";
    outFile << "label=\"FOUND BUG AVOIDING SCHEDULE:\\n" << bugSolution << "\"\n\n";
}


// draw schedule segments from a given schedule, can be fail or alternate schedule
void drawAllSegments(ofstream &outFile, vector<ThreadSegment> segsSch, vector<string> schedule, string schType, string bugSolution)
{
    numEventsDifDebug = 0;
    
    map<string,string> opToPort; //for a given operation, indicates its port label of form "tableId:port"
    string nextOp ="", previousOp ="";
    string friendlyOpNext = "";
    
    //differences between fail and alternate, by DEFAULT FAIL
    map<string,string> exclusiveAux = exclusiveFail;
    string colorBug = "\"red\"";
    string nodeType = "f";
    string lineColor = "\"red\"";
    string gridColor = "\"#A00000\"";
    if(schType!="fail") //redefine variables to fit alternate needs
    {
        exclusiveAux = exclusiveAlt;
        colorBug = "\"green\"";
        nodeType = "a";
        lineColor = "\"darkgreen\"";
        gridColor = "\"darkgreen\"";
    }
    
    //** draw all segments for the schedule
    for(int i = 0; i < segsSch.size(); i++)
    {
        ThreadSegment seg = segsSch[i];
        
        outFile << nodeType << i << " [fontname=\"Helvetica\", fontsize=\"11\", shape=none, margin=0,\n";
        if(containsBugCauseOp(seg, schedule, bugSolution, schType))
            outFile << "\tlabel=<<table border=\"2\" color=" + gridColor + " cellspacing=\"0\">\n";
        else
        {
            if(schType!="fail")
            {
                outFile << "\tlabel=<<table border=\"";
                if(seg.markAtomic)
                    outFile << "4\" cellspacing=\"0\">\n";
                else
                    outFile << "0\" cellspacing=\"0\">\n";
            }
            else
                outFile << "\tlabel=<<table border=\"0\" cellspacing=\"0\">\n";
        }
        outFile << "\t\t<tr><td border=\"1\" bgcolor=\""<< threadColors[seg.tid]<<"\"><font point-size=\"14\">T"<<seg.tid<<"</font></td></tr>\n";
        
        for(int j = seg.initPos; j <= seg.endPos; j++)
        {
            string op = schedule[j];
            //cout << op << endl;
            //we don't want to print the failure and exit operations
            int finalPositionOp = (int) op.find("&");
            if(op.substr(0,finalPositionOp).find ("Assert")!=string::npos // a problem occurred when e.g.: the filename was "simpleAssert"
               || op.substr(0,finalPositionOp).find("exit")!=string::npos
               || (!sourceFilePath.empty() && op.substr(0,finalPositionOp).find("branch")!=string::npos)){
                continue;
            }
            if(j < seg.endPos-1)
            {
                nextOp = schedule[j+1];
                friendlyOpNext = cleanOperation(makeInstrFriendly(nextOp));
            }
            string port = getDependencePort(op,schType);
            string friendlyOp = cleanOperation(makeInstrFriendly(op));
            
            if(port.empty())
            {
                if(friendlyOp == previousOp || (friendlyOp == friendlyOpNext && !(getDependencePort( nextOp, schType).empty()))) // jump write if 1) == previous, or 2) next operation is equal and special (with a port)
                    continue;
                else
                {
                    outFile << "\t\t<tr><td align=\"left\" border=\"1\">" << friendlyOp<< "</td></tr>\n";
                    previousOp = friendlyOp;
                }
            }
            else
            {
                outFile << "\t\t<tr><td align=\"left\" border=\"1\" port=\""<<port<<"\" bgcolor="+ colorBug+">" << friendlyOp << "</td></tr>\n";
                opToPort[(nodeType+op)] = nodeType+util::stringValueOf(i)+":"+port+":e";
                previousOp = friendlyOp;
            }
            
            numEventsDifDebug++;
        }
        outFile << "\t</table>>\n]\n\n";
    }
    
    //** draw edges
    for(int i = 0; i < segsSch.size()-1; i++)
        outFile << nodeType << i <<" -> "<< nodeType << i+1 <<";\n";
    
    //** draw data-dependence edges
    for(map<string,string>::const_iterator it = exclusiveAux.begin(); it!=exclusiveAux.end(); ++it)
    {
        outFile << opToPort[(nodeType + it->second)] <<" -> "<< opToPort[(nodeType + it->first)] <<" [color=" + lineColor
        + ", fontcolor="+ lineColor + ", style=bold, label=\"" << getVarValue(it->first, schType) << "\"] ;\n\n\n";
    }
}



/*
 * Draw failing schedule and alternate schedule in graphviz format
 *
 */
void graphgen::drawGraphviz(const vector<ThreadSegment>& segsFail, const vector<ThreadSegment>& segsAlt, const vector<string>& failSchedule, const vector<string>& altSchedule, const EventPair& invPair)
{
    ofstream outFile;
    
    string path = solutionFile.substr(0, solutionFile.find_last_of("/"));
    string appname = util::extractFileBasename(solutionFile);
    
    if(appname.find("solution")!=string::npos)
        appname.erase(appname.find("solution"),8); //try to parse app name for files
    
    if(appname.find("ALT")!=string::npos)
        appname.erase(appname.find("ALT"),3);
    
    if(appname.find(".txt")!=string::npos)
        appname.erase(appname.find(".txt"));
    
    path.append("/DSP/dsp_"+appname+"_Alt"+util::stringValueOf(altCounter)+".gv");
    
    outFile.open(path, ios::trunc);
    cout << "Saving graph to file: " << path << "\n";
    if(!outFile.is_open())
    {
        cerr << " -> Error opening file "<< path <<".\n";
        outFile.close();
        exit(0);
    }
    
    string bugSolution = bugCauseToGviz(invPair, failSchedule);
    string previousOp = "", nextOp = "";
    
    //draw file header
    drawHeader(outFile, bugSolution);
    
    //drawAllSegments
    drawAllSegments(outFile, segsFail, failSchedule, "fail", bugSolution);
    drawAllSegments(outFile, segsAlt, altSchedule, "alternate", bugSolution);
    
    outFile << "}\n";
    outFile.close();
}


void graphgen::drawAllGraph(const map<EventPair, vector<string>>& altSchedules, const vector<string>& solution)
{
    
    //** compute data dependencies
    cout << "=======================================\n";
    cout << "DATA DEPENDENCIES: \n\n";
    graphgen::genAllGraphSchedules(solution, altSchedules);
    cout << "=======================================\n";
    cout << "STATISTICS: \n";
    cout << "\n#Events in the full failing schedule: " << solution.size();
    cout << "\n#Events in the unsat core: " << unsatCore.size();
    cout << "\n#Events in the diff-debug schedule: " << numEventsDifDebug;
    cout << "\n#Data-dependencies in the full failing schedule: " << numDepFull;
    cout << "\n#Data-dependencies in the diff-debug schedule: " << numDepDifDebug << endl;
}
