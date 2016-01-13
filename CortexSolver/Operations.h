//
//  Operations.h
//  symbiosisSolver
//
//  Created by Nuno Machado on 30/12/13.
//  Copyright (c) 2013 Nuno Machado. All rights reserved.
//

//#ifndef snorlaxsolver_Operations_h
//#define snorlaxsolver_Operations_h

#ifndef __symbiosisSolver__Operations__
#define __symbiosisSolver__Operations__

#include <string>
#include <map>

class Operation {
    protected:
    std::string threadId;   //thread id
    std::string var;        //variable on which the operation is applied.
    int id;                 //id used to uniquely identify the i-th read access to the variable
    int line;               //source code line
    std::string filename;
    
    public:
    Operation();
    Operation(std::string tid, std::string variable, int varid, int srcline, std::string filename);
    Operation(std::string tid, int id);
    
    std::string getThreadId();
    std::string getVariableName();
    int getVariableId();
    int getLine();
    std::string getFilename();
    void setThreadId(std::string tid);
    void setVariableName(std::string variable);
    void setVariableId(int varid);
    void setLine(int srcline);
    void setFilename(std::string f);
    
    virtual std::string getConstraintName();
    virtual std::string getOrderConstraintName();
    virtual void print();

};


class CallOperation : public Operation{
    
    protected:
    int _srcLine;
    int _destLine;
    std::string _srcFilename;
    std::string _destFilename;
    std::map< std::string, std::string > _bindingPair;
    
    public:
    CallOperation();
    CallOperation(std::string tid, int id, int srcLine, int destLine, std::string srcFilename, std::string destFilename);
    
    std::string getConstraintName();
    std::string getOrderConstraintName();
    void print();
    
};

class RWOperation : public Operation{
    protected:
    std::string value;   //value written (for writes)
    bool isWrite;   //boolean indicating whether this operation is a write or not
    
    public:
    RWOperation();
    RWOperation(std::string tid, std::string variable, int varid, int srcline, std::string filename, std::string val, bool write);
    
    std::string getValue();
    bool isWriteOp();
    void setValue(std::string val);
    void setIsWrite(bool write);
    
    std::string getConstraintName();
    std::string getOrderConstraintName();
    std::string getInitialValueName();  //for a given read, returns the symbolic name of the initial value
    bool equals(RWOperation op);
    void print();
};

class PathOperation : public Operation{
    protected:
    std::string expr;    //path condition (expression)
    
    public:
    PathOperation();
    PathOperation(std::string tid, std::string variable, int varid, int srcline, std::string filename, std::string exp);
    void print();
    std::string getExpression();
    void setExpression(std::string exp);
};

class LockPairOperation : public Operation{
    protected:
    int unlockLine;          //unlock: source code line
    int unlockVarId;         //unlock: unique variable identifier
    std::string unlockFile;  //unlock: filename
    bool fakeUnlock;      //if true, indicates that the unlock operation was artificially injected to make the model feasible
    
    public:
    LockPairOperation();
    LockPairOperation(std::string tid, std::string variable, int varid, std::string lockfile, int lockLine, int unlockLn, std::string unlockfile, int unlockId);
    
    int getLockLine();
    int getUnlockLine();
    std::string getUnlockFile();
    int getUnlockVarId();
    void setLockLine(int lockLn);
    void setUnlockLine(int unlockLn);
    void setUnlockFile(std::string unlockF);
    void setUnlockVarId(int unlockId);
    bool isFakeUnlock();
    void setFakeUnlock(bool isFake);
    std::string getLockOrderConstraintName();
    std::string getUnlockOrderConstraintName();
    void print();
};

class SyncOperation : public Operation{
    protected:
    std::string type;
    //field var in SyncOperation can be used as child PID (in fork); as locking obj (in lock/unlock)
    
    public:
    SyncOperation();
    SyncOperation(std::string tid, std::string variable, int varid, int srcline, std::string filename, std::string t);
    
    std::string getType();
    void setType(std::string t);
    std::string getConstraintName();
    std::string getOrderConstraintName();
    void print();
};

class BranchOperation : public Operation{
public:
    BranchOperation();
    
    std::string getConstraintName();
    std::string getOrderConstraintName();
    void print();
};

#endif


