//
//  ConstraintModelGenerator.cpp
//  symbiosisSolver
//
//  Created by Nuno Machado on 03/01/14.
//  Copyright (c) 2014 Nuno Machado. All rights reserved.
//

#include "ConstraintModelGenerator.h"
#include "Operations.h"
#include "Util.h"
#include "Z3Solver.h"
#include "KQueryParser.h"
#include "JPFParser.h"
#include "Parameters.h"
#include <string>
#include <set>


using namespace std;
using namespace kqueryparser;

ConstModelGen::ConstModelGen()
{
    
}

void ConstModelGen::createZ3Solver(){
    
    //Z3Solver* z3solver = new Z3Solver();
    numLO = 0;
    numRW = 0;
    numMO = 0;
    numPO = 0;
    numPC = 0;
    numUnkownVars = 0;
};

void ConstModelGen::addMemoryOrderConstraints(map<string, vector<Operation*> > operationsByThread)
{
    
    z3solver.writeLineZ3("(echo \"MEMORY-ORDER CONSTRAINTS -----\")\n");
    
    int intMax = 0; //indicates the max value that an order constraint can have (which corresponds to the number of operations logged during the symb exec)
    int labelCounter = 0; //counter to label each constraint
    
    //count the total number of operations
    for(map<string, vector<Operation*> >::iterator thit = operationsByThread.begin(); thit != operationsByThread.end(); ++thit)
    {
        intMax += (thit->second).size();
    }
    
    string distinct = "(distinct "; //constraint to indicate that we want that each operation to have a unique global order
    for(map<string, vector<Operation*> >::iterator thit = operationsByThread.begin(); thit != operationsByThread.end(); ++thit)
    {
        vector<Operation*> opvec = thit->second;
        
        //if the list of operations has size > 1 then we must generate order constraints
        //for all the operations, otherwise we only need to declare the operation
        if(opvec.size() > 1)
        {
            string conststr = "(<"; //string containing the memory-order constraints
            
            
            for(vector<Operation*>::iterator opit = opvec.begin(); opit != opvec.end(); ++opit)
            {
                string declareVar = z3solver.declareIntOrderVarAndStore((*opit)->getOrderConstraintName(), 0, intMax);
                z3solver.writeLineZ3(declareVar);
                
                conststr.append(" "+(*opit)->getOrderConstraintName());
                distinct.append(" "+(*opit)->getOrderConstraintName());
            }
            conststr.append(")");
            
            string label = "MO" + util::stringValueOf(labelCounter); //** label to uniquely identify this constraint
            labelCounter++;
            z3solver.writeLineZ3(z3solver.postNamedAssert(conststr,label));  //create assert with the corresponding order constraint
            numMO += opvec.size();  //update constraint counter
        }
        else if(opvec.size() == 1)
        {
            string declareVar = z3solver.declareIntOrderVarAndStore(opvec[0]->getOrderConstraintName(), 0, intMax);
            z3solver.writeLineZ3(declareVar);
            numMO++;
        }
        z3solver.threadIds.push_back(thit->first);  //add thread name to the set of threadIds
    }
    distinct.append(")");
    z3solver.writeLineZ3(z3solver.postAssert(distinct)); //indicate that we want that each operation has its own global order
    z3solver.setNumOps(intMax);
    numUnkownVars += numMO; //there is an unknown order variable per event
}

//from a set of write operations, returns the subset of operations that write to a particular variable (being read in readOp)
vector<RWOperation> ConstModelGen::getWritesForRead(RWOperation readOp, std::vector<RWOperation> writeset)
{
    vector<RWOperation> ret;
    
    for(vector<RWOperation>::iterator writeit = writeset.begin(); writeit != writeset.end(); ++writeit)
    {
        RWOperation writeOp = *writeit;
        if(!writeOp.getVariableName().compare(readOp.getVariableName()))
        {
            ret.push_back(writeOp);
        }
    }
    
    return ret;
}


/**
 *  Worker thread to parallelize the creation of read-write constraints.
 */



void ConstModelGen::addReadWriteConstraints(map<string, vector<RWOperation> > readSet, map<string, vector<RWOperation> > writeSet, map<string, vector<Operation*> > operationsByThread){
    
    z3solver.writeLineZ3("(echo \"READ-WRITE CONSTRAINTS -----\")\n");
    
    vector<string> orStrSet;    //used to temporary store the read-write constraints (to ensure that all variable declarations are passed to the solver first)
    set<string> initValues;     //used to store the initial values already declared, in order to avoid redundant declarations
    
    numRW = 0;
    int labelCounter = 0; //counter for each RW constraint
    
    //1) declare all read variables
    for(map< string, vector<RWOperation> >::iterator out = readSet.begin(); out != readSet.end(); ++out)
    {
        vector<RWOperation> varvec = out->second;
        for(vector<RWOperation>::iterator readit = varvec.begin(); readit != varvec.end(); ++readit)
        {
            RWOperation readOp = *readit;
            
            numUnkownVars++; //each value returned by a read operation is an unknown variable
            
            //declare the constant representing the value read by the read operation
            string declareVar = z3solver.declareIntVar(readOp.getConstraintName());
            z3solver.writeLineZ3(declareVar);
        }
    }
    
    //2) build the constraints w.r.t RW linkages
    for(map< string, vector<RWOperation> >::iterator out = readSet.begin(); out != readSet.end(); ++out)
    {
        vector<RWOperation> varvec = out->second;
        for(vector<RWOperation>::iterator readit = varvec.begin(); readit != varvec.end(); ++readit)
        {
            RWOperation readOp = *readit;
            vector<RWOperation> writeSetForRead = writeSet[out->first]; //get the write set for a given read
            string orStr;
            string andInitStr;  //for initial values
            
            if(writeSetForRead.empty())
            {
                numRW++; //initial value constraint
            }
            else
            {
                //OPTIMIZATION - remove unnecessary writes (i.e. those belonging to the same thread
                //  that either happen before the closest write to this read, or after the read)
                // 1 - find the closest write operation to the read, from the same thread
                RWOperation *closestWrite = NULL;
                for(vector<Operation*>::iterator thdIt = operationsByThread[readOp.getThreadId()].begin(); thdIt != operationsByThread[readOp.getThreadId()].end();++thdIt)
                {
                    RWOperation* tmpOp = dynamic_cast<RWOperation*>(*thdIt);
                    if(tmpOp!=0){
                        if(tmpOp->isWriteOp() && tmpOp->getVariableName() == readOp.getVariableName()){
                            closestWrite = tmpOp;
                        }
                        else if(tmpOp->getConstraintName() == readOp.getConstraintName()){
                            break;
                        }
                    }
                }
                
                // 2 - delete all the other writes from the same thread
                //cout << "READ OP: " << readOp.getOrderConstraintName() << "\n";
                for(vector<RWOperation>::iterator remIt = writeSetForRead.begin(); remIt!=writeSetForRead.end();)
                {
                    RWOperation tmpOp = *remIt;
                    //cout << "\twrite " << tmpOp.getOrderConstraintName();
                    if(tmpOp.getThreadId() == readOp.getThreadId())
                    {
                        if(closestWrite == NULL || closestWrite->getConstraintName() != tmpOp.getConstraintName()){
                            //cout << " erased\n";
                            writeSetForRead.erase(remIt);
                        }
                        else{
                            //cout << " ok\n";
                            ++remIt;
                        }
                    }
                    else{
                        //cout << " from another thread\n";
                        ++remIt;
                    }
                }
                //------ end of optimization
                
                //build RW constraints
                for(int i = 0; i < writeSetForRead.size(); i++)
                {
                    RWOperation writei = writeSetForRead[i];
                    string andStr;
                    
                    string writtenVal;
                    if(jpfMode){
                        writtenVal = jpfparser::translateExprToZ3(writei.getValue());
                    }
                    else
                        writtenVal = kqueryparser::translateExprToZ3(writei.getValue());
                    
                    andStr.insert(0,z3solver.cEq(readOp.getConstraintName(), writtenVal)); //const: Rx = value written by Wi
                    andStr.append(" "+z3solver.cLt(writei.getOrderConstraintName(), readOp.getOrderConstraintName())); //const: Owi < Or
                    numRW++;
                    
                    for(int j = 0; j < writeSetForRead.size(); j++)
                    {
                        RWOperation writej = writeSetForRead[j];
                        if(!writej.equals(writei))
                        {
                            //const: Owj < Oi || Owj > Or
                            andStr.append(" "+z3solver.cOr(z3solver.cLt(writej.getOrderConstraintName(),writei.getOrderConstraintName()),
                                                           z3solver.cLt(readOp.getOrderConstraintName(),writej.getOrderConstraintName())));
                            numRW += 2;
                        }
                    }
                    //const: for all wi \in W
                    orStr.append("\n "+z3solver.cAnd(andStr));
                    
                    //case where the read is the initial value; const: Or < Owi, for all wi \in W
                    andInitStr.append(" "+z3solver.cLt(readOp.getOrderConstraintName(), writei.getOrderConstraintName()));
                    numRW++;
                }
            }
            //declare the initial value and create the respective constraint; const: Rx = init value
            string initValue = readOp.getInitialValueName();//"init"+readOp.getConstraintName();
            if(!initValues.count(initValue))
            {
                z3solver.writeLineZ3(z3solver.declareIntVar(initValue));
                initValues.insert(initValue);
            }
            andInitStr.insert(0, z3solver.cEq(readOp.getConstraintName(), initValue));
            
            //add the initial value to the global formula
            orStr.insert(0,z3solver.cAnd(andInitStr));
            
            //orStrSet.push_back(orStr); //<-- to temporary store in memory all the constraints, instead of writing directly to file (not being used at this moment)
            
            //write constraint to file
            string label = "RWC" + util::stringValueOf(labelCounter); //** label to uniquely identify this constraint
            labelCounter++;
            z3solver.writeLineZ3(z3solver.postNamedAssert(z3solver.cOr(orStr),label));
        }
    }
    
    //write all read-write constraints previously stored in memory(not being used at this moment)
    /*for(vector<string>::iterator it = orStrSet.begin(); it != orStrSet.end(); ++it)
     {
     string label = "RWC" + util::stringValueOf(labelCounter); // label to uniquely identify this constraint
     labelCounter++;
     z3solver.writeLineZ3(z3solver.postNamedAssert(z3solver.cOr(*it),label));
     }*/
}

void ConstModelGen::addPathConstraints(vector<PathOperation> pathset, bool invertBugCond){
    
    z3solver.writeLineZ3("(echo \"PATH CONSTRAINTS -----\")\n");
    int labelCounter = 0;
    string lastTid = pathset.begin()->getThreadId(); //used to reset the label counter (we only want count the number of PCs per thread)
    
    for(vector<PathOperation>::iterator it = pathset.begin(); it != pathset.end(); ++it)
    {
        PathOperation pathcond = *it;
        numPC++;
        string label;
        string curTid = pathcond.getThreadId();
        
        if(lastTid != curTid){
            labelCounter = 0;
            lastTid = curTid;
        }
        
        if(pathcond.getThreadId()==assertThread)
        {
            //if last path condition of assert thread, then label it as bug condition
            if(it+1 != pathset.end())
            {
                PathOperation nextcond = *(it+1);
                if(nextcond.getThreadId()==assertThread){
                    label = "PC" + util::stringValueOf(labelCounter) + "_T" + pathcond.getThreadId();
                }
                else{
                    label = "PC" + util::stringValueOf(labelCounter) + "_T" + pathcond.getThreadId() +"_BUGCOND";
                }
            }
            else{
                label = "PC" + util::stringValueOf(labelCounter) + "_T" + pathcond.getThreadId() +"_BUGCOND";
            }
        }
        else{
            label = "PC" + util::stringValueOf(labelCounter) + "_T" + pathcond.getThreadId(); //** label to uniquely identify this constraint
        }
        
        labelCounter++;
        
        string expr;
        if(jpfMode){
            expr = jpfparser::translateExprToZ3(pathcond.getExpression());
        }
        else
            expr = kqueryparser::translateExprToZ3(pathcond.getExpression());
        
        //invert bug condition if traces from successful execution
        if(invertBugCond && label.find("BUGCOND")!=string::npos){
            cout << ">> Inverting assert condition to force failure!\n";
            expr = z3solver.invertBugCondition(expr);
        }
        
        z3solver.writeLineZ3(z3solver.postNamedAssert(expr,label));
        if(!util::isClosedExpression(expr))
        {
            cout << expr;
        }
    }
}

void ConstModelGen::addLockingConstraints(map<string, vector<LockPairOperation> > lockpairset){
    
    z3solver.writeLineZ3("(echo \"LOCKING CONSTRAINTS -----\")\n");
    int labelCounter = 0;
    for(map<string, vector<LockPairOperation> >::iterator it = lockpairset.begin(); it != lockpairset.end(); ++it)
    {
        vector<LockPairOperation> lockvec = it->second;
        size_t lockSize = lockvec.size();
        
        if(lockSize > 1)
        {
            numLO++; //account for constraint Oau < Oal'
            for(int i=0; i < lockSize; i++)
            {
                vector<LockPairOperation> lockpairPrime = lockvec;
                LockPairOperation main = lockpairPrime[i];
                lockpairPrime.erase(lockpairPrime.begin()+i);
                
                //OPTIMIZATION - remove unnecessary lock pairs (i.e. those belonging to the same thread
                //  that either happen before the closest pair to this one, or after this pair)
                // 1 - find the closest lock pair to the main, from the same thread
                // 2 - remove all other locking pairs from the same thread
                //cout << "MAIN: "; main.print();
                if(i == 0){
                    //erase all subsequent locking pairs from the same thread
                    for(vector<LockPairOperation>::iterator remIt = lockpairPrime.begin(); remIt!=lockpairPrime.end();)
                    {
                        if(remIt->getThreadId() == main.getThreadId()){
                            //cout << "\terase "; remIt->print();
                            lockpairPrime.erase(remIt);
                        }
                        else{
                            //cout << "\tbreak\n";
                            break;
                        }
                    }
                }
                else{
                    LockPairOperation closestPair = lockpairPrime[i-1];
                    
                    bool flag = false; //flag indicating that we reached the subset of locking pairs of the same thread (this avoids iterating over unncessary cases)
                    for(vector<LockPairOperation>::iterator remIt = lockpairPrime.begin(); remIt!=lockpairPrime.end();)
                    {
                        if(remIt->getThreadId() == main.getThreadId())
                        {
                            flag = true;
                            if(remIt->getLockOrderConstraintName() != closestPair.getLockOrderConstraintName()
                               || remIt->getUnlockOrderConstraintName() != closestPair.getUnlockOrderConstraintName())
                            {
                                //cout << "\terase "; remIt->print();
                                //this already accounts for the cases where closestPair is from another thread
                                lockpairPrime.erase(remIt);
                            }
                            else{
                                //cout << "\tok "; remIt->print();
                                ++remIt;
                            }
                        }
                        else if(flag){
                            //cout << "\tbreak\n";
                            break;
                        }
                        else{
                            //cout << "\tok "; remIt->print();
                            ++remIt;
                        }
                    }
                }
                // ----- end of optimization
                
                //if there are no remaning locks to generate constraints continue to next locking pair
                if(lockpairPrime.size() == 0){
                    continue;
                }
                
                numLO = numLO + 1 + 2*lockpairPrime.size(); //account for constraints Oal > Oau' && Oal'' > Oau || Oau'' < Oal'
                string globalOr = "";
                string firstAnd = "";
                
                for(int j = 0; j < lockpairPrime.size(); j++)
                {
                    vector<LockPairOperation> lockpairDoublePrime = lockpairPrime;
                    LockPairOperation mainPrime = lockpairDoublePrime[j];
                    
                    //if(main.getThreadId() == mainPrime.getThreadId())
                    //  continue;
                    
                    lockpairDoublePrime.erase(lockpairDoublePrime.begin()+j);
                    string secondAnd = "";
                    
                    
                    if(mainPrime.getUnlockLine() != -1){
                        //const: Oal > Oau'
                        secondAnd.insert(0, z3solver.cGt(main.getLockOrderConstraintName(),mainPrime.getUnlockOrderConstraintName()));
                    }
                    
                    for(int u = 0; u < lockpairDoublePrime.size(); u++)
                    {
                        LockPairOperation doublePrime = lockpairDoublePrime[u];
                        
                        if(main.getUnlockLine() != -1 && doublePrime.getUnlockLine() != -1){
                            //const: Oal'' > Oau || Oau'' < Oal'
                            secondAnd.append(z3solver.cOr(z3solver.cGt(doublePrime.getLockOrderConstraintName(),main.getUnlockOrderConstraintName()),
                                                          z3solver.cLt(doublePrime.getUnlockOrderConstraintName(),mainPrime.getLockOrderConstraintName())));
                        }
                        else if(main.getUnlockLine() != -1){ //discard the constraint Oau'' < Oal'
                            //const: Oal'' > Oau
                            secondAnd.append(z3solver.cGt(doublePrime.getLockOrderConstraintName(),main.getUnlockOrderConstraintName()));
                        }
                        else if(doublePrime.getUnlockLine() != -1){  //discard the constraint Oal'' > Oau
                            //const: Oau'' < Oal'
                            secondAnd.append(z3solver.cLt(doublePrime.getUnlockOrderConstraintName(),mainPrime.getLockOrderConstraintName()));
                        }
                    }
                    
                    if(main.getUnlockLine() != -1){
                        //const: Oau < Oal'
                        firstAnd.append(z3solver.cLt(main.getUnlockOrderConstraintName(), mainPrime.getLockOrderConstraintName()));
                    }
                    
                    globalOr.append("\n"+z3solver.cAnd(secondAnd));
                }
                
                //if(!firstAnd.empty()) //remove case where there are no unlock operations
                globalOr.insert(0, z3solver.cAnd(firstAnd));
                
                string label = "LC"+util::stringValueOf(labelCounter); //** label to uniquely identify this constraint
                labelCounter++;
                z3solver.writeLineZ3(z3solver.postNamedAssert(z3solver.cOr(globalOr),label));
            }
        }
    }
}


void ConstModelGen::addForkStartConstraints(map<string, vector<SyncOperation> > forkset, map<string, SyncOperation> startset)
{
    z3solver.writeLineZ3("(echo \"FORK-START CONSTRAINTS -----\")\n");
    int labelCounter = 0;
    for (map<string, vector<SyncOperation> >::iterator it=forkset.begin(); it!=forkset.end(); ++it)
    {
        string constraint;
        vector<SyncOperation> tmpvec = it->second;
        for(vector<SyncOperation>::iterator in = tmpvec.begin(); in!=tmpvec.end(); ++in)
        {
            SyncOperation parent = *in;
            string childTid = parent.getVariableName(); //get the forked thread id
            SyncOperation child = startset[childTid];
            
            //account for possible missing thread traces
            if(child.getOrderConstraintName() == "OS--@0"){
                continue;
            }
            
            constraint = z3solver.cLt(parent.getOrderConstraintName(),child.getOrderConstraintName()); //const: fork-parent < start-child
            
            string label = "FSC"+util::stringValueOf(labelCounter); //** label to uniquely identify this constraint
            labelCounter++;
            numPO++; //increase number of partial-order constraints
            z3solver.writeLineZ3(z3solver.postNamedAssert(constraint,label));
        }
    }
}

void ConstModelGen::addJoinExitConstraints(std::map<string, vector<SyncOperation> > joinset, map<string, SyncOperation> exitset)
{
    z3solver.writeLineZ3("(echo \"JOIN-EXIT CONSTRAINTS -----\")\n");
    int labelCounter = 0;
    for (map<string, vector<SyncOperation> >::iterator it=joinset.begin(); it!=joinset.end(); ++it)
    {
        string constraint;
        vector<SyncOperation> tmpvec = it->second;
        for(vector<SyncOperation>::iterator in = tmpvec.begin(); in!=tmpvec.end(); ++in)
        {
            SyncOperation parent = *in;
            string childTid = parent.getVariableName(); //get the joined thread id
            SyncOperation child = exitset[childTid];
            
            //account for possible missing thread traces
            if(child.getOrderConstraintName() == "OS--@0"){
                continue;
            }
            
            constraint = z3solver.cGt(parent.getOrderConstraintName(),child.getOrderConstraintName()); //const: join-parent > exit-child
            
            string label = "JEC"+util::stringValueOf(labelCounter); //** label to uniquely identify this constraint
            labelCounter++;
            numPO++; //increase number of partial-order constraints
            z3solver.writeLineZ3(z3solver.postNamedAssert(constraint, label));
        }
    }
}


void ConstModelGen::addWaitSignalConstraints(map<string, vector<SyncOperation> > waitset, map<string, vector<SyncOperation> > signalset)
{
    z3solver.writeLineZ3("(echo \"WAIT-SIGNAL CONSTRAINTS -----\")\n");
    
    int totalVars = 0;
    int labelCounter = 0;
    map<string,vector<string> > signalBinaryVars;   //map signal id -> vector of all binary vars corresponding to this signal operation
    //map<string,string > waitConditions;             //map wait id (W-obj-tid) -> string corresponding to the constraints for all wait operations on the same object
    
    for(map<string, vector<SyncOperation> >::iterator allWaitIt = waitset.begin(); allWaitIt != waitset.end(); ++allWaitIt)
    {
        vector<SyncOperation> signals = signalset[allWaitIt->first];
        totalVars += signals.size();
        
        for(vector<SyncOperation>::iterator waitIt = allWaitIt->second.begin(); waitIt != allWaitIt->second.end(); ++waitIt)
        {
            SyncOperation wait = *waitIt;
            string globalOr;
            
            for(vector<SyncOperation>::iterator signalIt = signals.begin(); signalIt!= signals.end(); ++signalIt)
            {
                SyncOperation signal = *signalIt;
                
                string labelSig = "@S";
                
                //account for signalall
                if(signal.getType().find("signalall")!=string::npos)
                {
                    labelSig = "@SALL";
                }
                
                //binVar -> binary var used to indicate whether the signal operation is mapped to a wait operation or not
                string binVar = "b" + wait.getVariableName() + "@W" + util::stringValueOf(wait.getLine()) + "-" + wait.getThreadId() + "-" + util::stringValueOf(wait.getVariableId()) + labelSig + util::stringValueOf(signal.getLine()) + "-" + signal.getThreadId() + "-" + util::stringValueOf(signal.getVariableId());
                
                string signalId = signal.getConstraintName();
                signalBinaryVars[signalId].push_back(binVar);
                
                //const: Oa_sg < Oa_wt && b^{a_sg}_{a_wt} = 1
                globalOr.append(z3solver.cAnd(z3solver.cLt(signal.getOrderConstraintName(), wait.getOrderConstraintName()), z3solver.cEq(binVar,util::stringValueOf(1))));
                numPO += 2;
                z3solver.writeLineZ3(z3solver.declareIntVarAndStore(binVar, 0, 1));
            }
            string label = "WSC"+util::stringValueOf(labelCounter); //** label to uniquely identify this constraint
            labelCounter++;
            z3solver.writeLineZ3(z3solver.postNamedAssert(z3solver.cOr(globalOr),label));
        }
    }
    
    //add constraints stating that a given signal can only be mapped to a wait operation
    for(map<string, vector<string> >::iterator binSetIt = signalBinaryVars.begin(); binSetIt != signalBinaryVars.end(); ++binSetIt)
    {
        string signalId = binSetIt->first;
        
        //** if is signalAll, we don't constrain the number of waits that can be matched with this signal
        if(signalId.find("signalall")!=string::npos)
        {
            //const: Sum_{x \in WT} b^{a_sg}_{x} >= 0
            z3solver.writeLineZ3(z3solver.postAssert(z3solver.cGeq(z3solver.cSummation(binSetIt->second), util::stringValueOf(0))));
        }
        else{
            //const: Sum_{x \in WT} b^{a_sg}_{x} <= 1
            z3solver.writeLineZ3(z3solver.postAssert(z3solver.cLeq(z3solver.cSummation(binSetIt->second), util::stringValueOf(1))));
        }
        numPO++; //account for the constraint above in the partial-order constraints
    }
}


void ConstModelGen::addBarrierConstraints(map<string, vector<SyncOperation> > barrierset, map<string,vector<Operation*> > operationsByThread)
{
    z3solver.writeLineZ3("(echo \"BARRIER CONSTRAINTS -----\")\n");
    int labelCounter = 0;
    
    for(map< string, vector<SyncOperation> >::iterator out = barrierset.begin(); out != barrierset.end(); ++out)
    {
        vector<SyncOperation> varvec = out->second;
        vector<Operation*> nextOps; //vector containing the operations happening after barrier operations
        for(vector<SyncOperation>::iterator barit = varvec.begin(); barit != varvec.end(); ++barit)
        {
            //for each barrier operation, find the subsequent operation in the thread traces
            for(vector<Operation*>::iterator thdIt = operationsByThread[barit->getThreadId()].begin(); thdIt != operationsByThread[barit->getThreadId()].end();++thdIt)
            {
                SyncOperation* tmpOp = dynamic_cast<SyncOperation*>(*thdIt);
                if(tmpOp!=0 && tmpOp->getConstraintName() == barit->getConstraintName()){
                    thdIt++;
                    nextOps.push_back(*thdIt);
                    break;
                }
            }
        }
        
        //add constraint stating that all next operations should occur after all the barrier operations
        for(vector<SyncOperation>::iterator barit = varvec.begin(); barit != varvec.end(); ++barit)
        {
            for(vector<Operation*>::iterator nextit = nextOps.begin(); nextit != nextOps.end(); ++nextit)
            {
                string label = "BC"+util::stringValueOf(labelCounter); //** label to uniquely identify this constraint
                string constraint = z3solver.postNamedAssert(z3solver.cLt(barit->getOrderConstraintName(), (*nextit)->getOrderConstraintName()),label);
                z3solver.writeLineZ3(constraint);
                labelCounter++;
            }
        }
    }
    
}


void ConstModelGen::addAvisoConstraints(std::map<std::string, std::vector<Operation*> > operationsByThread, AvisoEventVector fulltrace)
{
    z3solver.writeLineZ3("(echo \"AVISO CONSTRAINTS -----\")\n");
    int labelCounter = 0;
    map<string, vector<Operation*> > tmpOperationsByThread = operationsByThread; //we create a tmp copy in order to freely erase entries, thus speeding up the search
    
    string constraint;
    //bool found = false; //indicates whether a given event was already found in the operations' set
    for(AvisoEventVector::iterator avisoit = fulltrace.begin(); avisoit != fulltrace.end(); ++avisoit)
    {
        string tid = (*avisoit).tid;
        vector<Operation*> tmpvec = tmpOperationsByThread[tid];
        
        //** search for the operation corresponding to the aviso event
        for(int i = 0; i < tmpvec.size(); i++)
        {
            Operation* op = tmpvec[i];
            string avisoLine = (*avisoit).filename + "@" + util::stringValueOf((*avisoit).loc);
            string opLine = op->getFilename() + "@" + util::stringValueOf(op->getLine());
            if(!avisoLine.compare(opLine) && op->getConstraintName().find("unlock")==std::string::npos)
            {
                constraint.append(op->getOrderConstraintName() + " ");
                tmpvec.erase(tmpvec.begin(),tmpvec.begin()+i+1);
                tmpOperationsByThread[tid].erase(tmpOperationsByThread[tid].begin(),tmpOperationsByThread[tid].begin()+i+1);
                break;
            }
        }
    }
    string label = "AvisoC"+util::stringValueOf(labelCounter); //** label to uniquely identify this constraint
    z3solver.writeLineZ3(z3solver.postNamedAssert(z3solver.cLt(constraint),label));
}

void ConstModelGen::openOutputFile(){
    z3solver.openOutputFile();
}

bool ConstModelGen::solve(){
    double total = numMO + numLO + numPC + numPO + numRW;
    cout << "   TYPE\t| #CONSTRAINTS\n";
    cout << "--------------------------\n";
    cout << "Memory\t|\t" << numMO << "\t("<< (numMO/total)<<"%)\n";
    cout << "ReadWrite\t|\t" << numRW << "\t("<< (numRW/total)<<"%)\n";
    cout << "Locking\t|\t" << numLO << "\t("<< (numLO/total)<<"%)\n";
    cout << "PartialOrd\t|\t" << numPO << "\t("<< (numPO/total)<<"%)\n";
    cout << "Path\t\t|\t" << numPC << "\t("<< (numPC/total)<<"%)\n";
    cout << "\n>> #CONSTRAINTS: " << total << "\n";
    cout << ">> #UNKNOWN VARS: " << numUnkownVars << "\n\n";
    return z3solver.solve();
}

bool ConstModelGen::solveWithSolution(vector<string> solution, bool invertBugCond){
    return z3solver.solveWithSolution(solution,invertBugCond);
}

void ConstModelGen::resetSolver(){
    z3solver.reset();
}

void ConstModelGen::closeSolver(){
    z3solver.closeZ3();
}
