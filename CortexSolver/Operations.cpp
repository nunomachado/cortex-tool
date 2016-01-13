//
//  Operations.cpp
//  symbiosisSolver
//
//  Created by Nuno Machado on 30/12/13.
//  Copyright (c) 2013 Nuno Machado. All rights reserved.
//

#include <iostream>
#include <stdio.h>
#include "Operations.h"
#include "Util.h"

using namespace std;

//** class Operation ***************
Operation::Operation()
{
    threadId = "";
    var = "";
    id = 0;
    line = 0;
}

Operation::Operation(string tid, string variable, int varid, int srcline, string f)
{
    threadId = tid;
    var = variable;
    id = varid;
    line = srcline;
    filename = f;
}

Operation::Operation(string tid, int idOp)
{
    threadId = tid;
    id = idOp;
};

string Operation::getThreadId(){
    return threadId;
}

string Operation::getVariableName(){
    return var;
}

int Operation::getVariableId(){
    return id;
}

int Operation::getLine(){
    return line;
}

string Operation::getFilename(){
    return filename;
}

void  Operation::setLine(int srcline){
    line = srcline;
}

void Operation::setThreadId(std::string tid){
    threadId = tid;
}

void Operation::setVariableName(std::string variable){
    var = variable;
}

void Operation::setVariableId(int varid){
    id = varid;
}

void Operation::setFilename(string f){
    filename = f;
}

void Operation::print()
{
    cout << "[" << threadId << "] Op-" << var << "-" << id << "&" << filename << "@" << line << endl;
}

string Operation::getOrderConstraintName()
{
    return ("OOp-" + var + "-" + threadId);
}

string Operation::getConstraintName()
{
    return ("Op-" + var + "-" + threadId);
}

//** class CallOperation ***************
CallOperation::CallOperation() : Operation(){};

CallOperation::CallOperation(string tid, int id, int scrLine, int destLine, string scrFilename, string destFilename)
: Operation(tid, id)
{
    _srcLine = scrLine;
    _destLine = destLine;
    _srcFilename = scrFilename;
    _destFilename = destFilename;
}

string CallOperation::getConstraintName(){
    string ret = "FunCall-" + threadId + "-" + util::stringValueOf(id) + "&"+ _srcFilename +"/"+ _destFilename +"@" + util::stringValueOf(_srcLine) + "/" + util::stringValueOf(_destLine);
    return ret;
}

std::string CallOperation::getOrderConstraintName()
{
    string ret = "OC-FunCall-" + threadId + "-" + util::stringValueOf(id) + "&"+ _srcFilename +"/"+ _destFilename +"@" + util::stringValueOf(_srcLine) + "/" + util::stringValueOf(_destLine);
    return ret;
}

void CallOperation::print()
{
    string varList="";
    for(map<string, string>::iterator it = _bindingPair.begin(); it != _bindingPair.end(); it++)
    {
        varList = varList + (*it).first +" to "+(*it).second+" ";
    }
    cout << "[" << threadId << "] " << "FunCall_" << varList << "-" << id << "&" << _srcFilename <<";"<< _destFilename << "@" << _srcLine <<";"<<_destLine<< endl;
}


//** class RWOperation ***************
RWOperation::RWOperation() : Operation(){}

RWOperation::RWOperation(string tid, string variable, int varid, int srcline, string f, string val, bool write)
: Operation(tid, variable, varid, srcline, f)
{
    value = val;
    isWrite = write;
}

string RWOperation::getValue(){
    return value;
}

bool RWOperation::isWriteOp(){
    return isWrite;
}

void RWOperation::setValue(string val){
    value = val;
}

void RWOperation::setIsWrite(bool write){
    isWrite = write;
}

string RWOperation::getConstraintName()
{
    string ret;
    if(isWrite)
    {
        ret = "W-" + var + "-" + threadId + "-" + util::stringValueOf(id) + "&"+ filename +"@" + util::stringValueOf(line);
    }
    else
    {
        ret = "R-" + var + "-" + threadId + "-" + util::stringValueOf(id);  //we must have R-var-id as constraint name in order to conform with the path constraints
    }
    return ret;
}

string RWOperation::getOrderConstraintName()
{
    string ret;
    if(isWrite)
    {
        ret = "OW-" + var + "-" + threadId + "-" + util::stringValueOf(id) + "&"+ filename +"@" + util::stringValueOf(line);
    }
    else
    {
        ret = "OR-" + var + "-" + threadId + "-" + util::stringValueOf(id) + "&"+ filename +"@" + util::stringValueOf(line);
    }
    return ret;
}


//Nuno: does this hold?
string RWOperation::getInitialValueName()
{
    string ret = "InitR-" + var + "-" + threadId; //here we don't consider the var id, because if a read-N reads the initial value, than it is equal to that of the read-0
    
    return ret;
}

bool RWOperation::equals(RWOperation op)
{
    bool ret = op.getConstraintName().compare(getConstraintName());
    //cout << "equals --> " << op.getConstraintName() << " == " << getConstraintName() << " ? " << ret << endl; //debug
    return !ret;
}

void RWOperation::print()
{
    if(isWrite)
    {
        cout << "[" << threadId << "] W-" << var << "-" << id <<" = $" << value << "$&" << filename << "@" << line << endl;
    }
    else
    {
        cout << "[" << threadId << "] R-" << var << "-" << id << "&" << filename << "@" << line << endl;
    }
}


//** class PathOperation ***************
PathOperation::PathOperation() : Operation(){}

PathOperation::PathOperation(string tid, string variable, int varid, int srcline, string f, string exp)
: Operation(tid, variable, varid, srcline, f)
{
    expr = exp;
}

string PathOperation::getExpression(){
    return expr;
}

void PathOperation::setExpression(string exp){
    expr = exp;
}

void PathOperation::print(){
    cout << "[" << threadId << "]: " << expr << endl;
}

//** class LockPairOperation ***************
LockPairOperation::LockPairOperation() : Operation(){}

LockPairOperation::LockPairOperation(string tid, string variable, int varid, string f, int lockLn, int unlockLn, string ufile, int unlockId)
: Operation(tid, variable, varid, lockLn, f)
{
    unlockLine = unlockLn;
    fakeUnlock = false;
    unlockVarId = unlockId;
    unlockFile = ufile;
}

int LockPairOperation::getLockLine(){
    return line;
}

int LockPairOperation::getUnlockLine(){
    return unlockLine;
}


string LockPairOperation::getUnlockFile(){
    return unlockFile;
}

int LockPairOperation::getUnlockVarId(){
    return unlockVarId;
}

string LockPairOperation::getLockOrderConstraintName()
{
    string ret;
    ret = "OS-lock_" + var + "-" + threadId + "-" + util::stringValueOf(id) + "&"+ filename + "@" + util::stringValueOf(line);
    //OS-lock_       416680994     -1                  -0                      &SimpleAssertKLEE.c     @16
    return ret;
}

string LockPairOperation::getUnlockOrderConstraintName()
{
    string ret;
    if(fakeUnlock==true)
        ret = "OS-unlockFake_" + var + "-" + threadId + "-" + util::stringValueOf(unlockVarId) + "&" + unlockFile + "@" + util::stringValueOf(unlockLine);
    else
        ret = "OS-unlock_" + var + "-" + threadId + "-" + util::stringValueOf(unlockVarId) + "&" + unlockFile + "@" + util::stringValueOf(unlockLine);
    
    return ret;
}

void LockPairOperation::setLockLine(int lockLn){
    line = lockLn;
}

void LockPairOperation::setUnlockLine(int unlockLn){
    unlockLine = unlockLn;
}

void LockPairOperation::setUnlockFile(string unlockF){
    unlockFile = unlockF;
}

void LockPairOperation::setUnlockVarId(int unlockId){
    unlockVarId = unlockId;
}

void LockPairOperation::setFakeUnlock(bool isFake){
    fakeUnlock = isFake;
}
bool LockPairOperation::isFakeUnlock(){
    return fakeUnlock;
}

void LockPairOperation::print()
{
    cout << "[" << threadId << "] " << var << "-" << id << "/"<< unlockVarId << ": Lock&" << filename << "@" << line << " Unlock&" << unlockFile<< "@" <<unlockLine << endl;
}

//** class SyncOperation ***************
SyncOperation::SyncOperation() : Operation(){}

SyncOperation::SyncOperation(string tid, string variable, int varid, int srcline, string f, string t)
: Operation(tid, variable, varid, srcline, f)
{
    type = t;
}

string SyncOperation::getType(){
    return type;
}


void SyncOperation::setType(string t){
    type = t;
}


string SyncOperation::getConstraintName()
{
    string ret;
    //ret = "S-" + type + "-" + threadId;
    if(var.empty()){
        ret = "S-" + type + "-" + threadId + "&" +filename + "@" + util::stringValueOf(line);
    }
    else{
        ret = "S-" + type + "_" + var + "-" + threadId + "-" + util::stringValueOf(id) + "&" +filename + "@" + util::stringValueOf(line);
    }
    return ret;
}

string SyncOperation::getOrderConstraintName()
{
    string ret;
    
    if(var.empty()){
        ret = "OS-" + type + "-" + threadId + "&" +filename + "@" + util::stringValueOf(line);
    }
    else{
        ret = "OS-" + type + "_" + var + "-" + threadId + "-" + util::stringValueOf(id) + "&" +filename + "@" + util::stringValueOf(line);
    }
    
    return ret;
}

void SyncOperation::print()
{
    if(var.empty())
        cout << "[" << threadId << "] " << type << "&" << filename << "@" << line << endl;
    else
        cout << "[" << threadId << "] " << type << "_" << var << "-" << id << "&" << filename << "@" << line << endl;
}

//** class SyncOperation ***************
BranchOperation::BranchOperation() : Operation(){}

string BranchOperation::getConstraintName()
{
    return ("branch-"+threadId+"-"+util::stringValueOf(id));
}

string BranchOperation::getOrderConstraintName()
{
   return ("branch-"+threadId+"-"+util::stringValueOf(id));
}

void BranchOperation::print()
{
    cout << "[" << threadId << "] branch-" << id << endl;
}
