//
//  main.cpp
//  cortexsolver
//
//  Created by Nuno Machado on 30/12/13.
//  Copyright (c) 2013 Nuno Machado. All rights reserved.
//
#include <stdio.h>
#include <stdlib.h>
#include <getopt.h>
#include <iostream>
#include <fstream>
#include <map>
#include <set>
#include <vector>
#include <string.h>
#include <stack>
#include <unordered_set>
#include <algorithm>
#include "Operations.h"
#include "Common.h"
#include "ConstraintModelGenerator.h"
#include "Util.h"
#include "Types.h"
#include "Parameters.h"
#include "GraphvizGenerator.h"
#include "Schedule.h"
#include "TraceAnalyzer.h"

using namespace std;

int numIncLockPairs = 0;    //number of incomplete locking pairs, taking into account all objects
vector<string> solution;    //vector that stores a given schedule (i.e. solution) found by the solver (used in --dsp-mode)
//vector<string> solutionAlt;    //vector that stores a given alternate schedule (i.e. solution) found by the solver (used in --dsp-mode)

//data structures for Aviso
AvisoTrace atrace;          //map: thread Id -> vector<avisoEvent>
AvisoEventVector fulltrace; //sorted vector containing all avisoEvents


/**
 *  Parse configuration file.
 */
void parse_configFile()
{
    ifstream fin;
    string path = util::getConfigFile();
    //cout << path << endl;
    fin.open(path);
    if (fin.good())
    {
        while (!fin.eof())
        {
            // read an entire line into memory
            char lineTMP[MAX_LINE_SIZE];
            fin.getline(lineTMP, MAX_LINE_SIZE);
            
            if(lineTMP[0] == '#')
                continue;
            
            string line = lineTMP;
            string key = line.substr(0,line.find("="));
            string value = line.substr(line.find("=")+1);
            //cout << "-> "<< key << ", " << value << endl;
            
            if(key == "trace-folder" && !value.empty()){
                symbFolderPath = value;
            }
            else if(key == "source" && !value.empty()){
                sourceFilePath = value;
            }
            else if(key == "jpf-timeout" && !value.empty()){
                jpftimeout = value;
            }
            else if(key == "jpf-file" && !value.empty()){
                jpfFile = value;
            }
            else if(key == "model" && !value.empty()){
                formulaFile = value;
            }
            else if(key == "solution" && !value.empty()){
                solutionFile = value;
            }
            else if(key == "with-solver" && !value.empty()){
                solverPath = value;
            }
            else if(key == "cortex-n" && !value.empty()){
                cortex_N = util::intValueOf(value);
            }
            else if(key == "cortex-d" && !value.empty()){
                cortex_D = util::intValueOf(value);
            }
            else if(key == "dsp-mode" && value == "true"){
                dspMode = true;
            }
            else if(key == "alt-sch" && !value.empty()){
                solutionAltFile = value;
            }
            else if(key == "debug" && value == "true"){
                debug = true;
            }
            else if(key == "csr" && value == "true"){
                useCSR = true;
            }
        }
        
    }
    else{
        util::print_state(fin);
        cerr << ">> Error opening configuration file: "<< path <<".\n";
        fin.close();
        exit(1);
    }
}

/**
 *  Parse the input arguments.
 */
void parse_args(int argc, char *const* argv)
{
    int c;
    if(argc < 5 && symbFolderPath.empty())
    {
        cerr << ">> Not enough arguments. Usage:\n\n" << usageOpts;
        exit(1);
    }
    
    while(1)
    {
        static struct option long_options[] =
        {
            {"trace-folder", required_argument, 0, 'f'},
            {"aviso-trace", required_argument, 0, 'v'},
            {"with-solver", required_argument, 0, 's'},
            {"model", required_argument, 0, 'm'},
            {"solution", required_argument, 0, 'l'},
            {"alt-sch", required_argument, 0, 'a'},
            {"source", required_argument, 0, 'i'},
            {"debug", no_argument, 0, 'd'},
            {"dsp-mode", no_argument, 0, 'p'},
            {"dsp", required_argument, 0, 'u'},
            {"jpf-file", required_argument, 0, 'j'},
            {"jpf-timeout", required_argument, 0, 't'},
            {"cortex-n", required_argument, 0, 'n'},
            {"cortex-d", required_argument, 0, 'x'},
            {"csr", required_argument, 0, 'c'},
            {"help", no_argument, 0, 'h'},
            {"", }
            
            
        };
        /* getopt_long stores the option index here. */
        int option_index = 0;
        
        c = getopt_long(argc, argv, "", long_options, &option_index);
        
        /* Detect the end of the options. */
        if (c == -1)
            break;
        
        switch (c)
        {
            case 'h':
                cout << ">> Usage:\n\n" << usageOpts;
                exit(EXIT_SUCCESS);
                
            case 'd':
                debug = true;
                break;
                
            case 'v':
                avisoFilePath = optarg;
                break;
                
            case 'f':
                symbFolderPath = optarg;
                break;
            case 'i':
                sourceFilePath = optarg;
                break;
                
            case 's':
                solverPath = optarg;
                break;
                
            case 'm':
                formulaFile = optarg;
                break;
                
            case 'l':
                solutionFile = optarg;
                break;
                
            case 'a':
                solutionAltFile = optarg;
                break;
                
            case 'p':
                dspMode = true;
                break;
            case 'c':
                useCSR = true;
                break;
            case 'u':
                dspFlag = optarg; // extended, short, can be also empty
                break;
            case 'j':
                jpfFile = optarg;
                break;
                /*case 'r':
                 assertThread = optarg;
                 break;
                 */
            case 't':
                jpftimeout = optarg;
                break;
            case 'n':
                cortex_N = util::intValueOf(optarg) - 1; //accounting for case 0
                break;
            case 'x':
                cortex_D = util::intValueOf(optarg);
                break;
            case '?':
                /* getopt_long already printed an error message. */
                break;
                
            default:
                abort ();
        }
    }
    
    /* Print any remaining command line arguments (not options). */
    if (optind < argc)
    {
        printf ("non-option ARGV-elements: ");
        while (optind < argc)
            printf ("%s ", argv[optind++]);
        putchar ('\n');
        exit(1);
    }
    
    if(dspMode && dspFlag!="extended" && dspFlag!="short" && dspFlag!="")
    {
        cerr << "Unknow argument for --dsp\nUsage: --dsp=\"extended\", \"short\" or \"\" to have different DSP views\n";
        exit(1);
    }
    
    if(dspMode && (formulaFile.empty() || solutionFile.empty() || solverPath.empty()))
    {
        cerr << "Not enough arguments for dspMode.\nUsage: --model=/path/to/input/constraint/model\n--solution=/path/to/input/solution\n--with-solver=/path/to/solver/executable\n";
        exit(1);
    }
    else if(!dspMode && (formulaFile.empty() || solutionFile.empty() || solverPath.empty() || symbFolderPath.empty()))
    {
        cerr << "Not enough arguments.\nUsage:\n--trace-folder=/path/to/symbolic/traces/folder\n--aviso-trace=/path/to/aviso/trace\n--with-solver=/path/to/solver/executable\n--model=/path/to/output/constraint/model\n--solution=/path/to/output/solution\n";
        exit(1);
    }
    
    //** pretty print
    if(dspMode)
        cout << "# MODE: FIND BUG'S ROOT CAUSE\n";
    else{
        cout << "# MODE: FIND BUG-TRIGGERING SCHEDULE\n";
        if(useCSR)
            cout << "# CONTEXT SWITH REDUCTION [ON]" << endl;
    }
    
    if(!avisoFilePath.empty()) cout << "# AVISO TRACE: " << avisoFilePath << endl;
    if(!symbFolderPath.empty()) cout << "# SYMBOLIC TRACES: " << symbFolderPath << endl;
    cout << "# SOLVER: " << solverPath << endl;
    cout << "# CONSTRAINT MODEL: " << formulaFile << endl;
    cout << "# SOLUTION: " << solutionFile << endl;
    
    cout << endl;
}

/**
 * Parse the symbolic information contained in a trace and
 * populate the respective data strucutures
 */
void parse_constraints(string symbFilePath)
{
    bool rwconst = false;
    bool pathconst = false;
    int line = 0;
    string filename;
    string syncType;
    string threadId;
    string obj;
    string var;
    int varId = 0;
    int callCounter = 0;
    int branchCounter = 0; //per-thread branch local counter
    
    map<string, int> varIdCounters; //map var name -> int counter to uniquely identify the i-th similar operation
    map<string, int> reentrantLocks; //map lock obj -> counter of reentrant acquisitions (used to differentiate reentrant acquisitions of the same lock)
    
    ifstream fin;
    symbFilePath = symbFolderPath+"/"+symbFilePath;
    fin.open(symbFilePath);
    if (!fin.good())
    {
        //search in the original folder if trace is from production runs
        symbFilePath = symbFilePath.replace(symbFilePath.find("/sts/"), 5, "/");
        fin.open(symbFilePath);
        if (!fin.good())
        {
            //check if file is in the original folder (i.e. it wasn't synthesized)
            util::print_state(fin);
            cerr << " -> Error opening file "<< symbFilePath <<".\n";
            fin.close();
            exit(0);
        }
    }
    
    std::cout << ">> Parsing " << util::extractFileBasename(symbFilePath) << endl;
    
    // read each line of the file
    while (!fin.eof())
    {
        // read an entire line into memory
        char buf[MAX_LINE_SIZE];
        fin.getline(buf, MAX_LINE_SIZE);
        char* token;
        string event = buf;
        //cout << "-> "<< event << endl;
        switch (buf[0]) {
            case '<':
                token = strtok (buf,"<>");
                if(!strcmp(token,"readwrite"))  //is readwrite constraints
                {
                    rwconst = true;
                    if(debug) cout << "parsing readwrite constraints...\n";
                }
                else if(!strcmp(token,"path"))
                {
                    pathconst = true;
                    if(debug) cout << "parsing path constraints...\n";
                }
                else if(!strcmp(token,"pathjpf"))
                {
                    pathconst = true;
                    jpfMode = true;
                    if(debug) cout << "parsing path constraints...\n";
                }
                else if(!strcmp(token,"assertThread_ok"))
                {
                    string tmpfname = util::extractFileBasename(symbFilePath);
                    tmpfname = tmpfname.substr(1,tmpfname.find_first_of("_\n")-1); //extract thread id from file name
                    failedExec = false;
                    assertThread = tmpfname;
                    cout << "# ASSERT THREAD: " << assertThread << " (ok)\n";
                }
                else if(!strcmp(token,"assertThread_fail"))
                {
                    string tmpfname = util::extractFileBasename(symbFilePath);
                    tmpfname = tmpfname.substr(1,tmpfname.find_first_of("_\n")-1); //extract thread id from file name
                    failedExec = true;
                    assertThread = tmpfname;
                    cout << "# ASSERT THREAD: " << assertThread << " (fail)\n";
                }
                break;
                
            case '$':   //indicates that is a write
            {
                string tmp = buf; //tmp is only used to ease the check of the last character
                if(tmp.back() == '$')
                {
                    token = strtok (buf,"$"); //token is the written value
                    if(token == NULL){
                        //token = "0";
                        token[0] = '0';
                        token[1] = '\0';
                    }
                    RWOperation* op;
                    if(!jpfMode){
                         op = new RWOperation(threadId, var, 0, line, filename, token, true);
                        
                        //update variable id
                        string varname = op->getOrderConstraintName();
                        if(!varIdCounters.count(varname))
                        {
                            varIdCounters[varname] = 0;
                        }
                        else
                        {
                            varIdCounters[varname] = varIdCounters[varname] + 1;
                        }
                        op->setVariableId(varIdCounters[varname]);
                    }
                    else{
                        op = new RWOperation(threadId, var, varId, line, filename, token, true);
                    }
                    
                    writeset[var].push_back(*op);
                    operationsByThread[threadId].push_back(op);
                    
                }
                else
                {
                    //cout << tmp << endl;
                    tmp.erase(0,1); //erase first '$'
                    string value = "";
                    while(tmp.back() != '$')
                    {
                        value.append(tmp);
                        fin.getline(buf, MAX_LINE_SIZE);
                        tmp = buf;
                    }
                    tmp.erase(tmp.find('$'),1); //erase last '$'
                    value.append(tmp); //add last expression part
                    
                    RWOperation* op = new RWOperation(threadId, var, 0, line, filename, value, true);
                    
                    //update variable id
                    string varname = op->getOrderConstraintName();
                    if(!varIdCounters.count(varname))
                    {
                        varIdCounters[varname] = 0;
                    }
                    else
                    {
                        varIdCounters[varname] = varIdCounters[varname] + 1;
                    }
                    op->setVariableId(varIdCounters[varname]);
                    
                    writeset[var].push_back(*op);
                    operationsByThread[threadId].push_back(op);
                }
                break;
            }
                
            case 'T':  //indicates that is a path constraint
            {
                string tmp = buf;
                
                //make sure that this is a path condition and not the name of the file
                if(tmp.find("@")==string::npos){
                    threadId = tmp.substr(1,tmp.find(":")-1); //get the thread id
                    
                    string expr;
                    tmp.erase(0,tmp.find(":")+1);
                    
                    while(expr.back() != ')' || !util::isClosedExpression(expr))
                    {
                        expr.append(tmp);
                        if(util::isClosedExpression(expr))
                            break;
                        fin.getline(buf, MAX_LINE_SIZE);
                        tmp = buf;
                    }
                    //expr.append(tmp); //handles the last case in which the last char is ')'
                    
                    //remove unnecessary spaces from expression
                    size_t space = expr.find("  ");
                    while(space!= std::string::npos)
                    {
                        size_t endspace = expr.find_first_not_of(" ",space);
                        expr.replace(space, endspace-space, " ");
                        space = expr.find("  ");
                    }
                    
                    PathOperation* po = new PathOperation(threadId, "", 0, 0, filename, expr);
                    pathset.push_back(*po);
                    break;
                }
            }
                
            case 'b':  //indicates that is a branch
            {
                string tmp = buf;
                //make sure that this is a path condition and not the name of the file
                if(tmp.find("@")==string::npos){
                    string tid = tmp.substr(tmp.find_first_of("-")+1);
                    BranchOperation* bop = new BranchOperation();
                    bop->setThreadId(tid);
                    bop->setVariableId(branchCounter++);
                    operationsByThread[tid].push_back(bop);
                    break;
                }
            }
           
            case 'p':  //indicates that is a path condition
            {
                string tmp = buf;
                //make sure that this is a path condition and not the name of the file
                if(tmp.find("@")==string::npos){
                    break; //the path id is already being read in TraceAnalyzer.loadTraces
                }
            }
                
            case 'c': //indicates that is a BB clock
            {
                string tmp = buf;
                //make sure that this is a path condition and not the name of the file
                if(tmp.find("@")==string::npos){
                    //parse thread id
                    size_t init = tmp.find_first_of("-")+1;
                    size_t end = tmp.find_first_of("-", init);
                    string tid = tmp.substr(init,end-init);
                    //parse bbid (stored as var in Operation)
                    init = tmp.find_first_of("-", end)+1;
                    end = tmp.find_first_of("-", init);
                    string bbid = tmp.substr(init,end-init);
                    //parse var id
                    init = tmp.find_first_of("-", end)+1;
                    end = tmp.find_first_of("-", init);
                    int varid = util::intValueOf(tmp.substr(init,end-init));
                    //parse clock (stored as line in Operation)
                    init = tmp.find_first_of("-", end)+1;
                    end = tmp.find_first_of("-", init);
                    int clock = util::intValueOf(tmp.substr(init,end-init));
                    
                    ClockOperation* cop = new ClockOperation();
                    cop->setThreadId(tid);
                    cop->setVariableName(bbid);
                    cop->setVariableId(varid);
                    cop->setLine(clock);
                    operationsByThread[tid].push_back(cop); 
                    usedBBClocks.insert(cop->getOrderConstraintName());
                    break;
                }
            }
                
            default:  //constraint has form line:constraint
            {
                if(!strcmp(buf,""))
                    break;
                token = strtok (buf,"@");
                filename = token;
                token = strtok (NULL,"-:");
                line = atoi(token);
                
                token = strtok (NULL,"-:"); //token = type (S,R, or W)
                //########## HEAD
                if(!strcmp(token,"CS"))
                {
                    callCounter++;
                    varId = callCounter;
                    
                    token = strtok (NULL,"-\n");
                    threadId = token;
                    string scrFilename = filename;
                    
                    int scrLine = line;
                    
                    //Get CodeDestiny
                    fin.getline(buf, MAX_LINE_SIZE);
                    //char* token2;
                    string event = buf;
                    
                    token = strtok(buf,"@");
                    string destFilename = token;
                    token = strtok(NULL,"-:");
                    int destLine = atoi(token);
                    
                    CallOperation* op = new CallOperation(threadId, varId, scrLine, destLine, scrFilename, destFilename);
                    
                    //operationsByThread[threadId].push_back(op);
                }
                else if(!strcmp(token,"S"))  //handle sync constraints
                {
                    token = strtok (NULL,"-_");
                    syncType = token;
                    if(!strcmp(token,"lock"))
                    {
                        token = strtok (NULL,"-_");
                        obj = token;
                        
                        token = strtok (NULL,"-\n");
                        threadId = token;
                        
                        //check whether it is a reentrant lock
                        //if so, change the name of the object to OBJrN, where N is the number of reentrant acquisitions
                        /*if(reentrantLocks.count(obj) && reentrantLocks[obj] > 0){
                         int rcounter = reentrantLocks[obj];
                         string newobj = obj + "r" + util::stringValueOf(rcounter);
                         rcounter++;
                         reentrantLocks[obj] = rcounter;
                         obj = newobj;
                         }
                         else{
                         reentrantLocks[obj] = 1;
                         }*/
                        
                        //we don't add a constraint for a reentrant lock
                        if(reentrantLocks.count(obj) == 0 || reentrantLocks[obj] == 0){
                            reentrantLocks[obj] = 1;
                        }
                        else if(reentrantLocks[obj] > 0){
                            int rcounter = reentrantLocks[obj];
                            rcounter++;
                            reentrantLocks[obj] = rcounter;
                            continue;
                        }
                        
                        //add the lock operation to the thread memory order set in its correct order
                        SyncOperation* op = new SyncOperation(threadId,obj,0,line,filename,"lock");
                        
                        //update variable id
                        string varname = op->getOrderConstraintName();
                        if(!varIdCounters.count(varname)){
                            varIdCounters[varname] = 0;
                        }
                        else{
                            varIdCounters[varname] = varIdCounters[varname] + 1;
                        }
                        op->setVariableId(varIdCounters[varname]);
                        operationsByThread[threadId].push_back(op);
                        
                        //create new lock pair operation
                        LockPairOperation lo (threadId,obj,varIdCounters[varname],filename,line,-1,"",0);
                        
                        //the locking pair is not complete, so we add it to a temp stack
                        if(lockpairStack.count(obj)){
                            lockpairStack[obj][threadId].push(lo);
                        }
                        else{
                            stack<LockPairOperation> s;
                            s.push(lo);
                            lockpairStack[obj][threadId] = s;
                        }
                        numIncLockPairs++;
                    }
                    else if(!strcmp(token,"unlock"))
                    {
                        token = strtok (NULL,"-_");
                        obj = token;
                        
                        token = strtok (NULL,"-\n");
                        threadId = token;
                        
                        //check whether it is a reentrant unlock
                        /*if(reentrantLocks.count(obj) && reentrantLocks[obj] > 0){
                         int rcounter = reentrantLocks[obj];
                         rcounter--; //we have to decrement before renaming the obj to match the last rcounter
                         string newobj;
                         
                         continue;
                         
                         if(rcounter > 0){
                         newobj = obj + "r" + util::stringValueOf(rcounter);
                         }
                         else{
                         newobj = obj;
                         }
                         reentrantLocks[obj] = rcounter;
                         obj = newobj;
                         }*/
                        //we don't add a constraint for a reentrant lock
                        if(reentrantLocks.count(obj) && reentrantLocks[obj] >= 1){
                            int rcounter = reentrantLocks[obj];
                            rcounter--;
                            reentrantLocks[obj] = rcounter;
                            if(rcounter > 0)
                                continue;
                        }
                        
                        //the unlock completes the locking pair, thus we can add it to the lockpairset
                        LockPairOperation *lo = new LockPairOperation(lockpairStack[obj][threadId].top());
                        lockpairStack[obj][threadId].pop();
                        lo->setUnlockLine(line);
                        lo->setVariableName(obj);
                        lo->setUnlockFile(filename);
                        numIncLockPairs--;
                        
                        //add the unlock operation to the thread memory order set in its correct order
                        SyncOperation* op = new SyncOperation(threadId,obj,0,line,filename,"unlock");
                        
                        //update variable id
                        string varname = lo->getUnlockOrderConstraintName();
                        if(!varIdCounters.count(varname))
                        {
                            varIdCounters[varname] = 0;
                        }
                        else
                        {
                            varIdCounters[varname] = varIdCounters[varname] + 1;
                        }
                        op->setVariableId(varIdCounters[varname]);
                        lo->setUnlockVarId(varIdCounters[varname]);
                                              
                        lockpairset[obj].push_back(*lo);
                        operationsByThread[threadId].push_back(op);
                    }
                    else if(!strcmp(token,"fork"))
                    {
                        token = strtok (NULL,"-_");
                        obj = token;
                        
                        token = strtok (NULL,"-\n");
                        threadId = token;
                        
                        SyncOperation* op = new SyncOperation(threadId,obj,0,line,filename,syncType);
                        
                        if(forkset.count(threadId)){
                            forkset[threadId].push_back(*op);
                        }
                        else{
                            vector<SyncOperation> v;
                            v.push_back(*op);
                            forkset[threadId] = v;
                        }
                        operationsByThread[threadId].push_back(op);
                    }
                    else if(!strcmp(token,"join"))
                    {
                        token = strtok (NULL,"-_");
                        obj = token;
                        
                        token = strtok (NULL,"-\n");
                        if(token == NULL){
                            cout << ">> PARSING ERROR: Missing child thread id in event \""<< event <<"\"! Join event must have format \"S-join_childId-parentId\". Please change file " << util::extractFileBasename(symbFilePath) << " accordingly.\n";
                            exit(EXIT_FAILURE);
                        }
                        threadId = token;
                        
                        SyncOperation* op = new SyncOperation(threadId,obj,0,line,filename,syncType);
                        
                        if(joinset.count(threadId)){
                            joinset[threadId].push_back(*op);
                        }
                        else{
                            vector<SyncOperation> v;
                            v.push_back(*op);
                            joinset[threadId] = v;
                        }
                        operationsByThread[threadId].push_back(op);
                    }
                    else if(!strcmp(token,"wait") || !strcmp(token,"timedwait"))
                    {
                        token = strtok (NULL,"-_");
                        obj = token;
                        
                        token = strtok (NULL,"-\n");
                        threadId = token;
                        
                        SyncOperation* op = new SyncOperation(threadId,obj,0,line,filename,syncType);
                        
                        //update variable id
                        string varname = op->getOrderConstraintName();
                        if(!varIdCounters.count(varname))
                        {
                            varIdCounters[varname] = 0;
                        }
                        else
                        {
                            varIdCounters[varname] = varIdCounters[varname] + 1;
                        }
                        op->setVariableId(varIdCounters[varname]);
                        
                        if(waitset.count(obj)){
                            waitset[obj].push_back(*op);
                        }
                        else{
                            vector<SyncOperation> v;
                            v.push_back(*op);
                            waitset[obj] = v;
                        }
                        
                        //as the wait operations release and acquire locks internally, we also have to account for that behavior
                        Schedule tmpvec = operationsByThread[threadId];
                        for(Schedule::reverse_iterator in = tmpvec.rbegin(); in!=tmpvec.rend(); ++in)
                        {
                            SyncOperation* lop = dynamic_cast<SyncOperation*>(*in);
                            if(lop!=0 && lop->getType() == "lock")
                            {
                                //1 - create an unlock operation and the corresponding locking pair
                                string lockobj = lop->getVariableName();
                                LockPairOperation *lo = new LockPairOperation(lockpairStack[lockobj][threadId].top());
                                lockpairStack[lockobj][threadId].pop();
                                lo->setUnlockLine(line);
                                lo->setUnlockFile(filename);
                                lo->setVariableName(lockobj);
                                lo->setVariableId(0); //we need to set var id to 0 in order to match the correct key for lock operations (see 'lock' case above)
                                lo->setVariableId(varIdCounters[lo->getLockOrderConstraintName()]); //now, set the var id to the correct value
                                
                                //create an extra unlock operation
                                SyncOperation* unlop = new SyncOperation(threadId,lockobj,0,line,filename,"unlock");
                                
                                //update unlock var id
                                string uvarname = lo->getUnlockOrderConstraintName();
                                if(!varIdCounters.count(uvarname)){
                                    varIdCounters[uvarname] = 0;
                                }
                                else{
                                    varIdCounters[uvarname] = varIdCounters[uvarname] + 1;
                                }
                                unlop->setVariableId(varIdCounters[uvarname]);
                                lo->setUnlockVarId(varIdCounters[uvarname]);
                                
                                //add the extra unlock to the thread memory order set
                                lockpairset[lockobj].push_back(*lo);
                                operationsByThread[threadId].push_back(unlop);
                                
                                //2 - insert timedwait in the operations vector
                                operationsByThread[threadId].push_back(op);
                                
                                //3 - create a new lock operation
                                SyncOperation* newlop = new SyncOperation(threadId,lockobj,0,line,filename,"lock");
                                
                                //update lock variable id
                                string newlvarname = newlop->getOrderConstraintName();
                                if(!varIdCounters.count(newlvarname)){
                                    varIdCounters[newlvarname] = 0;
                                }
                                else{
                                    varIdCounters[newlvarname] = varIdCounters[newlvarname] + 1;
                                }
                                newlop->setVariableId(varIdCounters[newlvarname]);
                                
                                //add the lock operation to the thread memory order set in its correct order
                                operationsByThread[threadId].push_back(newlop);
                                
                                //create a new locking pair
                                LockPairOperation newlpair (threadId,lockobj,varIdCounters[newlvarname],filename,line,-1,filename,0);
                                
                                //the locking pair is not complete, so we add it to a temp stack
                                lockpairStack[lockobj][threadId].push(newlpair);
                                
                                break;
                            }
                        }
                    }
                    else if(!strcmp(token,"signal") || !strcmp(token,"signalall"))
                    {
                        token = strtok (NULL,"-_");
                        obj = token;
                        
                        token = strtok (NULL,"-\n");
                        threadId = token;
                        
                        SyncOperation* op = new SyncOperation(threadId,obj,0,line,filename,syncType);
                        
                        //update variable id
                        string varname = op->getOrderConstraintName();
                        if(!varIdCounters.count(varname)){
                            varIdCounters[varname] = 0;
                        }
                        else{
                            varIdCounters[varname] = varIdCounters[varname] + 1;
                        }
                        op->setVariableId(varIdCounters[varname]);
                        
                        if(signalset.count(obj)){
                            signalset[obj].push_back(*op);
                        }
                        else{
                            vector<SyncOperation> v;
                            v.push_back(*op);
                            signalset[obj] = v;
                        }
                        operationsByThread[threadId].push_back(op);
                    }
                    else if(!strcmp(token,"start"))
                    {
                        token = strtok (NULL,"-\n");
                        threadId = token;
                        
                        SyncOperation* op = new SyncOperation(threadId,"",0,line,filename,syncType);
                        startset[threadId] = *op;
                        operationsByThread[threadId].push_back(op);
                    }
                    else if(!strcmp(token,"exit"))
                    {
                        token = strtok (NULL,"-\n");
                        threadId = token;
                        
                        SyncOperation* op = new SyncOperation(threadId,"",0,line,filename,syncType);
                        exitset[threadId] = *op;
                        operationsByThread[threadId].push_back(op);
                    }
                    else if(!strcmp(token,"barrier"))
                    {
                        token = strtok (NULL,"-_");
                        obj = token;
                        
                        token = strtok (NULL,"-\n");
                        threadId = token;
                        
                        SyncOperation* op = new SyncOperation(threadId,obj,0,line,filename,syncType);
                        if(barrierset.count(obj)){
                            barrierset[obj].push_back(*op);
                        }
                        else{
                            vector<SyncOperation> v;
                            v.push_back(*op);
                            barrierset[obj] = v;
                        }
                        operationsByThread[threadId].push_back(op);
                    }
                    else //syncType is unknown
                    {
                        token = strtok (NULL,"-_\n");
                        threadId = token;
                        
                        SyncOperation* op = new SyncOperation(threadId,"",0,line,filename,syncType);
                        syncset.push_back(*op);
                        operationsByThread[threadId].push_back(op);
                    }
                    
                } else if(!strcmp(token,"R"))
                {
                    token = strtok (NULL,"-");
                    var = token;
                    token = strtok (NULL,"-");
                    while(token[0] == '>'){
                        var.append("-");
                        var.append(token);
                        token = strtok (NULL,"-");
                    }
                    threadId = token;
                    token = strtok (NULL,"-\n");
                    varId = atoi(token);
                    
                    RWOperation* op = new RWOperation(threadId, var, varId, line, filename,"", false);
                    readset[var].push_back(*op);
                    operationsByThread[threadId].push_back(op);
                    
                } else if(!strcmp(token,"W"))
                {
                    token = strtok (NULL,"-");
                    var = token;
                    token = strtok (NULL,"-");
                    while(token[0] == '>'){
                        var.append("-");
                        var.append(token);
                        token = strtok (NULL,"-");
                    }
                    threadId = token;
                    //parse varId in case of JPF traces
                    token = strtok (NULL,"-\n");
                    if(token!=NULL){
                        varId = atoi(token);
                        jpfMode = true;
                    }
                }
                break;
            }
        }
    } //end while
    fin.close();
    
    //resolve written values that are references to other writes
    for(map<string, vector<RWOperation> >:: iterator oit = writeset.begin(); oit != writeset.end(); ++oit)
    {
        for(vector<RWOperation>:: iterator iit = oit->second.begin(); iit != oit->second.end(); ++iit)
        {
            RWOperation wOp = *iit;
            string wRef = wOp.getValue(); //check whether the written value is a reference to another write
            if(wRef.substr(0,2) == "W-")
            {
                //find that referenced write and replace the value with the referenced one
                for(vector<RWOperation>:: iterator rit = oit->second.begin(); rit != oit->second.end(); ++rit)
                {
                    RWOperation wOp2 = *rit;
                    if(wRef == wOp2.getConstraintName()){
                        
                    }
                }
            }
        }
    }
    
    //add non-closed locking pairs to lockpairset
    while(numIncLockPairs > 0)
    {
        for(Schedule::reverse_iterator rit = operationsByThread[threadId].rbegin();
            rit != operationsByThread[threadId].rend() && numIncLockPairs > 0; ++rit)
        {
            SyncOperation* tmplop = dynamic_cast<SyncOperation*>(*rit);
            if(tmplop!=0 && tmplop->getType() == "lock"
               && lockpairStack[tmplop->getVariableName()][threadId].size() > 0)
            {
                string tmpobj = tmplop->getVariableName();
                LockPairOperation* lo = new LockPairOperation(lockpairStack[tmpobj][threadId].top());
                int prevLine = operationsByThread[threadId].back()->getLine(); //** line of the last event in thread's trace
                lo->setUnlockLine(prevLine+1);
                lo->setUnlockFile(filename);
                lo->setFakeUnlock(true);
                lo->setVariableId(tmplop->getVariableId());
                
                //** create a fake unlock (placed after the last event in the schedule) and complete the locking pair
                SyncOperation* op = new SyncOperation(threadId,lo->getVariableName(),0,prevLine+1,filename,"unlockFake");
                
                //update var id
                string uvarname = lo->getUnlockOrderConstraintName();
                if(!varIdCounters.count(uvarname)){
                    varIdCounters[uvarname] = 0;
                }
                else{
                    varIdCounters[uvarname] = varIdCounters[uvarname] + 1;
                }
                
                op->setVariableId(varIdCounters[uvarname]);
                lo->setUnlockVarId(varIdCounters[uvarname]);
                
                operationsByThread[threadId].push_back(op);
                lockpairset[lo->getVariableName()].push_back(*lo);
                lockpairStack[tmpobj][threadId].pop();
                numIncLockPairs--;
            }
        }
    }
    lockpairStack.clear(); //** doing this we guarantee that we only add lock constraints only once
    
    //add the exit events...
    int prevLine = operationsByThread[threadId].back()->getLine();
    SyncOperation* op;
    if(assertThread == threadId){
        op = new SyncOperation(threadId, "", 0, prevLine + 1, filename, "Assert");
    }
    else
        op = new SyncOperation(threadId, "", 0, prevLine + 1, filename, "exit");
    operationsByThread[threadId].push_back(op);
    exitset[threadId] = *op;
    
    //delete last branch of the assertThread (which corresponds to the assertion)
    if(assertThread == threadId){
        int bcount = 0;
        for(Schedule::reverse_iterator rit = operationsByThread[threadId].rbegin();
            rit != operationsByThread[threadId].rend(); ++rit)
        {
            bcount++;
            BranchOperation* bop = dynamic_cast<BranchOperation*>(*rit);
            if(bop!=0){
                break;
            }
        }
        operationsByThread[threadId].erase(operationsByThread[threadId].end()-bcount);
    }
}

/**
 *  Parse the events contained in the Aviso trace.
 */
void parse_avisoTrace()
{
    ifstream fin;
    fin.open(avisoFilePath);
    
    if (!fin.good())
    {
        util::print_state(fin);
        cout << " -> Error opening file "<< avisoFilePath <<".\n";
        fin.close();
        exit(0);
    }
    
    // read each line of the file
    while (!fin.eof())
    {
        // read an entire line into memory
        char buf[MAX_LINE_SIZE];
        fin.getline(buf, MAX_LINE_SIZE);
        
        if(buf[0])
        {
            char* token;
            token = strtok (buf," :"); //token == thread id
            
            AvisoEvent aetmp;
            aetmp.tid = token;
            
            token = strtok (NULL," :"); //token == filename
            aetmp.filename = util::extractFileBasename(token);
            
            token = strtok (NULL," :"); //token == line of code
            aetmp.loc = atoi(token);
            
            //cout << "TID: " << aetmp.tid << " Filename: " << aetmp.filename << " Loc: "<< aetmp.loc << endl;
            
            atrace[aetmp.tid].push_back(aetmp);
            fulltrace.push_back(aetmp);
        }
    }
    fin.close();
    
    
    if(debug)
    {
        cout<< "\n### AVISO TRACE\n";
        
        for (unsigned i = 0; i < fulltrace.size(); i++) {
            cout << "[" << fulltrace[i].tid << "] " << util::extractFileBasename(fulltrace[i].filename) << "@" << fulltrace[i].loc << endl;
        }
        cout << endl;
    }
}


/**
 * Clears the data structures containing the operations from the symbolic traces.
 *
 */
void cleanDataStructures()
{
    readset.clear();
    writeset.clear();
    lockpairset.clear();
    startset.clear();
    exitset.clear();
    forkset.clear();
    joinset.clear();
    waitset.clear();
    signalset.clear();
    syncset.clear();
    pathset.clear();
    usedBBClocks.clear();
    lockpairStack.clear();
    operationsByThread.clear();
}

/**
 * Print data structures for debugging purposes
 *
 */
void printDataStructures()
{
    cout<< "\n-- READ SET\n";
    for(map< string, vector<RWOperation> >::iterator out = readset.begin(); out != readset.end(); ++out)
    {
        vector<RWOperation> tmpvec = out->second;
        for(vector<RWOperation>::iterator in = tmpvec.begin() ; in != tmpvec.end(); ++in)
        {
            in->print();
        }
    }
    
    cout<< "\n-- WRITE SET\n";
    for(map< string, vector<RWOperation> >::iterator out = writeset.begin(); out != writeset.end(); ++out)
    {
        vector<RWOperation> tmpvec = out->second;
        for(vector<RWOperation>::iterator in = tmpvec.begin() ; in != tmpvec.end(); ++in)
            in->print();
    }
    
    cout<< "\n-- LOCKPAIR SET\n";
    for(map<string, vector<LockPairOperation> >::iterator out = lockpairset.begin(); out != lockpairset.end(); ++out)
    {
        vector<LockPairOperation> tmpvec = out->second;
        for(vector<LockPairOperation>::iterator in = tmpvec.begin() ; in!=tmpvec.end(); ++in)
            in->print();
    }
    
    cout<< "\n-- WAIT SET\n";
    for(map<string, vector<SyncOperation> >::iterator out = waitset.begin(); out != waitset.end(); ++out)
    {
        vector<SyncOperation> tmpvec = out->second;
        for(vector<SyncOperation>::iterator in = tmpvec.begin() ; in!=tmpvec.end(); ++in)
            in->print();
    }
    
    cout<< "\n-- SIGNAL SET\n";
    for(map<string, vector<SyncOperation> >::iterator out = signalset.begin(); out != signalset.end(); ++out)
    {
        vector<SyncOperation> tmpvec = out->second;
        for(vector<SyncOperation>::iterator in = tmpvec.begin() ; in!=tmpvec.end(); ++in)
            in->print();
    }
    
    cout<< "\n-- BARRIER SET\n";
    for(map<string, vector<SyncOperation> >::iterator out = barrierset.begin(); out != barrierset.end(); ++out)
    {
        vector<SyncOperation> tmpvec = out->second;
        for(vector<SyncOperation>::iterator in = tmpvec.begin() ; in!=tmpvec.end(); ++in)
            in->print();
    }
    
    cout<< "\n-- FORK SET\n";
    for (map<string, vector<SyncOperation> >::iterator it=forkset.begin(); it!=forkset.end(); ++it)
    {
        vector<SyncOperation> tmpvec = it->second;
        for(vector<SyncOperation>::iterator in = tmpvec.begin(); in!=tmpvec.end(); ++in)
            in->print();
    }
    
    cout<< "\n-- JOIN SET\n";
    for (map<string, vector<SyncOperation> >::iterator it=joinset.begin(); it!=joinset.end(); ++it)
    {
        vector<SyncOperation> tmpvec = it->second;
        for(vector<SyncOperation>::iterator in = tmpvec.begin(); in!=tmpvec.end(); ++in)
            in->print();
    }
    
    cout<< "\n-- START SET\n";
    for (map<string, SyncOperation>::iterator it=startset.begin(); it!=startset.end(); ++it)
    {
        it->second.print();
    }
    
    cout<< "\n-- EXIT SET\n";
    for (map<string, SyncOperation>::iterator it=exitset.begin(); it!=exitset.end(); ++it)
    {
        it->second.print();
    }
    
    if(!syncset.empty())
    {
        cout<< "\n-- OTHER SYNC SET\n";
        for(vector<SyncOperation>::iterator it = syncset.begin(); it!=syncset.end(); ++it)
            it->print();
    }
    
    cout<< "\n-- PATH SET\n";
    for(vector<PathOperation>::iterator it = pathset.begin(); it!=pathset.end(); ++it)
        it->print();
    
    cout<< "\n### OPERATIONS BY THREAD\n";
    for (map<string, Schedule >::iterator it=operationsByThread.begin(); it!=operationsByThread.end(); ++it)
    {
        cout << "-- Thread " << it->first << endl;
        Schedule tmpvec = it->second;
        for(Schedule::iterator in = tmpvec.begin(); in!=tmpvec.end(); ++in)
        {
            (*in)->print();
        }
    }
}

/**
 *  Generate and solve the contraint model
 *  for a given set of symbolic traces
 */
bool verifyConstraintModel(ConstModelGen *cmgen, const map<string, string>& traceComb)
{
    bool success = false;
    size_t hasUnsatClocks = 1; //used to check whether the model was refined due to conflicting clock constraints (if so, repeat the solving)
    int conflictClocks = 0;
    
    while(!success && hasUnsatClocks > 0){
        
        cout << "\n### GENERATING CONSTRAINT MODEL\n";
        cmgen->openOutputFile(); //** opens a new file to store the model
        
        cout << "[Solver] Adding memory-order constraints...\n";
        cmgen->addMemoryOrderConstraints(operationsByThread);
        
        cout << "[Solver] Adding read-write constraints...\n";
        cmgen->addReadWriteConstraints(readset,writeset,operationsByThread);
        
        cout << "[Solver] Adding path constraints...\n";
        cmgen->addPathConstraints(pathset, !failedExec); //invert bug condition if the traces are from a successful execution
        
        cout << "[Solver] Adding locking-order constraints...\n";
        cmgen->addLockingConstraints(lockpairset);
        
        cout << "[Solver] Adding fork/start constraints...\n";
        cmgen->addForkStartConstraints(forkset, startset);
        
        cout << "[Solver] Adding join/exit constraints...\n";
        cmgen->addJoinExitConstraints(joinset, exitset);
        
        cout << "[Solver] Adding wait/signal constraints...\n";
        cmgen->addWaitSignalConstraints(waitset, signalset);
        
        cout << "[Solver] Adding barrier constraints...\n";
        cmgen->addBarrierConstraints(barrierset,operationsByThread);
        
        cout << "[Solver] Adding clock constraints...\n";
        cmgen->addBBClockConstraints();
        
        /* cout << "[Solver] Adding Aviso constraints...\n";
         cmgen->addAvisoConstraints(operationsByThread, fulltrace);
         //*/
        
        hasUnsatClocks = satClocks.size();
        cout << "\n### SOLVING CONSTRAINT MODEL: Z3\n";
        success = cmgen->solve();
        hasUnsatClocks = hasUnsatClocks - satClocks.size();
        if(hasUnsatClocks > 0){
            conflictClocks += hasUnsatClocks;
            cout << "[Solver] Unsat Core has conflicting clock constraints -> refine the model and repeat solving (conflicting constraints: "<< conflictClocks <<" of "<< (conflictClocks+satClocks.size()) <<")" << endl;
        }
        
        if(success)
        {
            cout<< "\n\n>> FAILING SCHEDULE:" << endl;
            scheduleLIB::printSch(failScheduleOrd);
            
            Schedule simpleSch;
            if(useCSR){
                simpleSch = scheduleLIB::scheduleSimplify(failScheduleOrd,cmgen);
                cout<< "\n\n>> NEW SCHEDULE (with Context Switch Reduction):" << endl;
                scheduleLIB::printSch(failScheduleOrd);
            }
            else
                simpleSch = failScheduleOrd;
            scheduleLIB::saveScheduleFile(solutionFile,scheduleLIB::schedule2string(simpleSch),traceComb);
        }
        
        //** clean data structures
        cmgen->resetSolver();
    }
    
    return success;
}





/**
 *  Identify the files containing symbolic traces pick
 *  a set of traces to generate the constraint model
 */
bool findFailingSchedule()
{
    string symbolicFile = "";
    bool foundBug = false;  //boolean var indicating whether the solver found a model that triggers the bug or not
    
    //** instatiate a constrain model generator object
    ConstModelGen* cmgen = new ConstModelGen();
    cmgen->createZ3Solver();
    
    //create TraceAnalyzer and load traces
    TraceAnalyzer* analyzer = new TraceAnalyzer();
    analyzer->loadTraces();
    map<string, string> traceComb; //combination of symbolic traces to test
    
    
    //1) test all production runs
    std::cout << "\n[Analyzer] Test first with all collected production runs.\n";
    for(map<string, vector<string> >:: iterator it = analyzer->executionIds.begin(); it != analyzer->executionIds.end() ; ++it){
        string execId = it->first;
        vector<string> paths = it->second;
        
        std::cout << "\n+-------------------------------+";
        std::cout << "\n| PRODUCTION RUN " << execId << "\t|";
        std::cout << "\n+-------------------------------+" << endl;
        
        //create combination of traces
        for(vector<string>::iterator pit = paths.begin(); pit != paths.end(); ++pit){
            string pathid = (*pit);
            string tid = pathid.substr(0,pathid.find("-"));
            pathid = pathid.substr(pathid.find("-"));
            traceComb[tid] = pathid;
            cout << ">> T"<<tid<<pathid << "\n";
        }
        
        //** parse contraints from thread symbolic traces
        cleanDataStructures();
        for(map<string,string>::iterator it = traceComb.begin(); it != traceComb.end(); ++it)
        {
            string key = it->first+it->second; //key is "tid-pathid"
            parse_constraints(analyzer->mapPathToTrace[key]);
        }
        
        //debug: print constraints
        if(debug){
            printDataStructures();
        }
        
        //** generate the constraint model and try to solve it
        foundBug = verifyConstraintModel(cmgen,traceComb);
        
        if(foundBug){
            cout << "\n[Analyzer] #Attempts to find failing schedule: 0 (bug is schedule-dependent)" << endl;
            return true;
        }
    }
    
    //2) flip branches
    //set combination to the initial one
    std::cout << "\n[Analyzer] Start path and schedule exploration (D = "<< analyzer->MAXDIST << "; N = "<< analyzer->MAXBFS << ")\n";
    unsatCoreStr = "";
    do
    {
        std::cout << "\n+--------------+";
        std::cout << "\n| ATTEMPT " << analyzer->attempts << "\t|";
        std::cout << "\n+--------------+" << endl;
        
        analyzer->getNextTraceCombination(&traceComb);
        
        //** parse contraints from thread symbolic traces
        cleanDataStructures();
        for(map<string,string>::iterator it = traceComb.begin(); it != traceComb.end(); ++it)
        {
            string key = it->first+it->second; //key is "tid-pathid"
            parse_constraints(analyzer->mapPathToTrace[key]);
        }
        
        //debug: print constraints
        if(debug){
            printDataStructures();
        }
        
        //** generate the constraint model and try to solve it
        foundBug = verifyConstraintModel(cmgen,traceComb);
        
    }while(!foundBug && analyzer->hasNext());
    
    cmgen->closeSolver();
    
    cout << "\n=======================================\n";
    cout << "STATISTICS: \n";
    cout << ">> Cortex search parameters (D: "<< cortex_D <<", N:"<< (cortex_N+1) <<")" << endl; //add 1 to cortex_N to account for the initial decrement
    cout << ">> #Attempts to find failing schedule: " << (analyzer->attempts-1) << endl;
    cout << ">> #Branch flips: " << (analyzer->bflips) << "\n\n";
    
    return foundBug;
}


/* Generate pairs of events to be inverted, between a given set of operations
 * and the operations in the unsat core.
 * A pair is comprised of two segments, where each segment is itself a pair
 * indicating the init and the end positions in the schedule
 *
 *  mapOpToId -> maps events to its id in the array containing the failing schedule
 *  opsToInvert -> vector of events to be inverted in the new schedule
 */
vector<EventPair> generateEventPairs(map<string, int> mapOpToId, vector<string> opsToInvert)
{
    vector<EventPair> eventPairs;
    for(vector<string>::iterator it = opsToInvert.begin(); it!=opsToInvert.end();++it)
    {
        string op1 = *it;
        string tid1 = util::parseThreadId(op1);
        string var1 = util::parseVar(op1);
        Segment seg1 = std::make_pair(mapOpToId[op1],mapOpToId[op1]);
        
        /* if the operation is not wrapped by a lock, the segment will be a pair
         * with the position of the operation in the schedule array.
         * Otherwise, it is a pair with the positions of the lock/unlock operations in the array.*/
        int i;
        for(i = mapOpToId[op1]; i > 0; i--)
        {
            string op2 = solution[i];
            string tid2 = util::parseThreadId(op2);
            
            if(op2.find("-lock")!=std::string::npos && tid2 == tid1)
            {
                //the operation is wrapped by a lock
                seg1.first = i;
                break;
            }
            else if (op2.find("-unlock")!=std::string::npos)
            {
                //the operation is not wrapped by a lock
                i = 0;
            }
        }
        
        if(i > 0) //if wrapped by a lock, find the corresponding unlock
        {
            for(i = mapOpToId[op1]; i < solution.size(); i++)
            {
                string op2 = solution[i];
                if(op2.find("-unlock")!=std::string::npos)
                {
                    seg1.second = i;
                    break;
                }
            }
        }
        
        /* generate pairs for the operation in the bug condition and
         the operations in the unsatCore (from other threads)*/
        cout << "\n>> Event Pairs for '"<< op1 <<"':\n";
        for(int pos = unsatCore[0]; pos < solution.size(); pos++)
        {
            //int pos = unsatCore[i];
            string op2 = solution[pos];
            string tid2 = util::parseThreadId(op2);
            
            //** disregard events of the same thread, as well as exits/joins
            if(tid1 == tid2
               || op2.find("branch-")!=string::npos
               || op2.find("-exit-")!=string::npos
               || op2.find("-FAILURE-")!=string::npos
               || op2.find("-AssertOK-")!=string::npos
               || op2.find("-AssertFAIL-")!=string::npos
               || op2.find("-Assert-")!=string::npos
               || op2.find("-join_")!=string::npos
               || op2.find("-fork_")!=string::npos
               || op2.find("-start_")!=string::npos)
                continue;
            
            //** disregard read operations and events (which are not locks) on different variables
            string var2 = util::parseVar(op2);
            if(op2.find("R-")!=string::npos)
                //|| (op2.find("lock")==string::npos && var1 != var2))
                continue;
            
            Segment seg2 = std::make_pair(pos, pos);
            EventPair p;
            
            //** check whether the operation to be inverted occurs before or after the segment containing the bug condition operation
            if(seg1.first < seg2.first){
                
                //** if the op occurs concurrently with the critical section, than it is not wrapped by locks and thus can be directly inverted with the bug condition operation
                if(seg1.second > seg2.first){
                    Segment segR = std::make_pair(mapOpToId[op1],mapOpToId[op1]);
                    p = std::make_pair(seg2, segR);
                }
                else
                    p = std::make_pair(seg1, seg2);
            }
            else
                p = std::make_pair(seg2, seg1);
            eventPairs.insert(eventPairs.begin(), p);
            
            cout << pairToString(p, solution);
        }
    }
    return eventPairs;
}

/* Generate a new schedule by inverting the event pairs in the original failing schedule.
 */
vector<string> generateNewSchedule(EventPair invPair)
{
    vector<string> bugCore; //vector used to store the events that compose the 'bug core'; if the event pair produces a sat schedule, then we save the bugCore in the map altSchedules
    
    //** generate the new schedule by copying the original schedule, apart from the events to be reordered
    int i = 0;
    vector<string> newSchedule;
    
    //** for a pair (A,B) to be inverted, add events from init to A
    for(i = 0; i < solution.size(); i++)
    {
        if(i == invPair.first.first){
            break;
        }
        newSchedule.push_back(solution[i]);
    }
    
    //** from A to B add all events that not belong to A's thread
    int j;
    vector<string> aThreadOps; //vector containing the operations after A that belong to A's thread
    //parse A's thread id
    string opA = solution[i];
    size_t init, end;
    string tidA = util::parseThreadId(opA);
    
    //parse B's thread id
    string opB = solution[invPair.second.first];
    string tidB = util::parseThreadId(opB);
    
    for(j = i; j < solution.size(); j++)
    {
        //stop if we hit the first event of segment B
        if(j == invPair.second.first)
        {
            break;
        }
        
        //parse op thread id
        string opC = solution[j];
        string tidC = util::parseThreadId(opC);
        
        //** we don't want to reorder the events of the other threads (Nuno: we're not doing this at the moment)
        if(tidC!=tidB)//if(tidA == tidB)
            aThreadOps.push_back(opC);
        else
            newSchedule.push_back(opC);
    }
    
    //** add all events of segment B before A
    //** (there might be some events belonging to other threads in seg B,
    //** so add them to aThreadOps)
    // (NOTE: at the end of this loop, 'i' will point to the operation right after segment B's last operation)
    for(i = invPair.second.first; i <= invPair.second.second; i++)
    {
        //parse op thread id
        string opC = solution[i];
        end = opC.find_last_of("-");
        if(opC.find("exit")!=string::npos)
        {
            init = opC.find_last_of("-")+1;
            end = opC.find_last_of("@");
        }
        else
            init = opC.find_last_of("-",end-1)+1;
        string tidC = opC.substr(init,end-init);
        
        if(tidC==tidB)
        {
            newSchedule.push_back(opC);
            bugCore.push_back(opC);
        }
        else
        {
            aThreadOps.push_back(opC);
        }
    }
    
    //** add events of segment of A, and all the other of A's thread that
    //** occurred between A and B
    for(j = 0; j < aThreadOps.size(); j++)
    {
        newSchedule.push_back(aThreadOps[j]);
        bugCore.push_back(aThreadOps[j]);
    }
    
    //** finally, add the remaining events from B to the end
    for(j = i; j < solution.size(); j++)
    {
        newSchedule.push_back(solution[j]);
    }
    
    return newSchedule;
}

void loadScheduleFromFile(string filepath, vector<string> *sch, map<string,string> *tracecomb)
{
    ifstream inSol(filepath);
    string lineSol;
    
    //** store in 'solutionAlt' the alternate schedule
    while (getline(inSol, lineSol))
    {
        //if line has form <T1..> parse the trace name, otherwise parse the constraint
        if(lineSol.front()=='<')
        {
            size_t init = 1;
            size_t end = lineSol.find_first_of(">",init);
            string tracename = lineSol.substr(init,end-init);
            //parse thread id
            init = tracename.find("-");
            string tid = tracename.substr(0,init);
            (*tracecomb)[tid] = tracename.substr(init);
        }
        else{
            (*sch).push_back(lineSol);
        }
    }
    inSol.close();
    
}


/**
 *  Algorithm to find the root cause of a given concurrency bug.
 *
 *  Outline:
 *  1) solve constraint model with the bug-inducing schedule and
 *  invert the bug condition to find the constraints that
 *  make the model to be unsat.
 *
 *  2) generate event pairs for the operations in the buggy schedule
 *  that appear in the unsat core and are part of the bug condition
 *
 *  3) for each pair, invert the event order in the buggy schedule
 *  and solve the constraint model with the no-bug constraint again. If
 *  the new model is sat, then it means that we found the root cause.
 *  Otherwise, simply attempt with another pair.
 */
bool findBugRootCause(map<EventPair, vector<string>>* altSchedules)
{
    map<string, int> mapOpToId; //map: operation name -> id in 'solution' array
    bool success= false; //indicates whether we have found a bug-avoiding schedule
    int numAttempts = 0; //counts the number of attempts to find a sat alternate schedule
    map<string, string> traceComb; //combination of symbolic traces that produced the failing schedule
    
    //** search directly in symbolic folder (this may not be the way
    //** to handle the case where the traces used for the failing schedule were synthesized)
    //check if we have already synthesized any new traces
    //if(symbFolderPath.find("/sts") == string::npos){
    //    symbFolderPath = symbFolderPath+"/sts";
    //}
    
    ConstModelGen* cmgen = new ConstModelGen();
    cmgen->createZ3Solver();
    
    loadScheduleFromFile(solutionFile,&solution,&traceComb);
    
    cmgen->solveWithSolution(solution,true); //** solve the model with the bug condition inverted in order to get the unsat core
    
    //** sort because the values in unsatCore are often in descending order (which the opposite of the memory-order of the program)
    std::sort(unsatCore.begin(),unsatCore.end());
    
    //** check if the unsat core begins within a region wrapped by a lock; if so, fetch all the operations until the locking operation
    for(int i = 0; i<unsatCore.size();i++)
    {
        string op = solution[unsatCore[i]];
        if(op.find("-lock")!=std::string::npos)
            break;
        else if (op.find("-unlock")!=std::string::npos) //we are missing a lock, search backwards in the solution schedule for it
        {
            for(int j = unsatCore[0]-1; j>0; j--)
            {
                string op2 = solution[j];
                unsatCore.insert(unsatCore.begin(), j);
                if(op2.find("-lock")!=std::string::npos)
                    break;
            }
            break;
        }
    }
    
    cout << "\n>> Operations in unsat core ("<< unsatCore.size() <<"):\n";
    for(int i = 0; i<unsatCore.size();i++)
    {
        string op = solution[unsatCore[i]];
        cout << op << endl;
        //mapOpToId[op] = unsatCore[i];
    }
    
    cout << "\n>> Operations in bug condition ("<< bugCondOps.size() <<"):\n";
    for(int i = 0; i < bugCondOps.size(); i++)
    {
        string bop = bugCondOps[i];
        cout << bop << endl;
        
    }
    
    //fill mapOpToId: operation name -> position in the failing schedule
    for(int j = 0; j<solution.size();j++)
    {
        string op = solution[j];
        mapOpToId[op] = j;
        
        //find the position of each op in the bug condition in the schedule
        //(take into account that operations in the condition start by "R-" instead of "OR-")
        for(int i = 0; i < bugCondOps.size(); i++)
        {
            string bop = bugCondOps[i];
            if(op.find(bop)!=string::npos)
            {
                mapOpToId[bop] = j;
                break;
            }
        }
    }

    
    //fill mapOpToId: operation name -> position in the failing schedule
    for(int j = 0; j<solution.size();j++)
    {
        string op = solution[j];
        mapOpToId[op] = j;
    }
    
    //generate event pairs with the operations from the bug condition
    vector<EventPair> eventPairs = generateEventPairs(mapOpToId, bugCondOps);
    
    /* Generate a new schedule by inverting the event pairs and try to solve the original constraint model.
     * If it is sat with the bug condition inverted, than we found the root cause of the bug;
     * otherwise, attempt again with a new pair
     */
    for(vector<EventPair>::iterator it = eventPairs.begin(); it!=eventPairs.end();++it)
    {
        EventPair invPair; //the pair to be inverted
        invPair = *it;
        vector<string> newSchedule = generateNewSchedule(invPair);
        
        cout << "\n------------------------\n";
        cout << "["<< ++numAttempts <<"] Attempt by inverting pair:\n" << pairToString(invPair, solution) << endl;
        
        bugCondOps.clear(); //clear bugCondOps to avoid getting repeated operations
        
        success = cmgen->solveWithSolution(newSchedule,true);
        if(success)
        {
            cout << "\n>> FOUND BUG AVOIDING SCHEDULE:\n" << bugCauseToString(invPair, solution);
            //altSchedules[invPair] = bugCore;
            (*altSchedules)[invPair] = newSchedule;
            break;
        }
    }
    
    //2) if we haven't found any alternate schedule by manipulating the events in the bug condition
    //let's broad the search scope to consider all reads on variables that appear in the bug condition
    if(!success)
    {
        cout << "\n\n>> No alternate schedule found! Increase search scope to consider other read operations on the variables contained in the bug condition.\n";
        
        //find the new operations ops to be inverted
        vector<string> opsToInvert;
        for(int i = 0; i < bugCondOps.size(); i++)
        {
            string bop = bugCondOps[i];
            string bvar = util::parseVar(bop);
            cout << "> For "<< bvar <<":\n";
            for(int j = 0; j < mapOpToId[bop]; j++)
            {
                string sop = solution[j];
                string svar = util::parseVar(sop);
                
                //store operation if its a read on the same var that of the Op in the bug condition
                if(svar == bvar && sop.find("R-")!=string::npos)
                {
                    cout << sop << endl;
                    opsToInvert.push_back(sop);
                    mapOpToId[sop] = j;
                }
            }
        }
        
        //generate event pairs with the new set of operations
        eventPairs.clear();
        eventPairs = generateEventPairs(mapOpToId, opsToInvert); //NOTE: the unsat at this point might be different from the first one.. this might cut-off some events (?)
        
        //generate the respective new schedule and attempt to solve the model again
        for(vector<EventPair>::iterator it = eventPairs.begin(); it!=eventPairs.end();++it)
        {
            EventPair invPair; //the pair to be inverted
            invPair = *it;
            vector<string> newSchedule = generateNewSchedule(invPair);
            
            cout << "\n------------------------\n";
            cout << "["<< ++numAttempts <<"] Attempt by inverting pair:\n" << pairToString(invPair, solution) << endl;
            
            bugCondOps.clear(); //clear bugCondOps to avoid getting repeated operations
            
            success = cmgen->solveWithSolution(newSchedule,true);
            if(success)
            {
                cout << "\n>> FOUND BUG AVOIDING SCHEDULE:\n" << bugCauseToString(invPair, solution);
                (*altSchedules)[invPair] = newSchedule;
                break;
            }
        }
    }
    
    //3) Explore different paths if we haven't found a non-failing schedule by reordering event pairs
    if(!success)
    {
        cout << "\n\n>> No alternate schedule found! Explore different paths.\n";
        
        //create TraceAnalyzer in case it's necessary to explore different paths
        TraceAnalyzer* analyzer = new TraceAnalyzer();
        analyzer->loadTraces();
        
        //mark the trace combination of the failing schedule as already tested
        string combHash;
        for(map<string, string >::iterator cit = traceComb.begin(); cit!=traceComb.end();++cit){
            combHash += cit->second;
        }
        analyzer->testedCombs.insert(std::hash<std::string>()(combHash));
        
        //find the MAXDIST closest branches
        for(vector<string>::reverse_iterator it = solution.rbegin(); it!=solution.rend();++it)
        {
            if(it->find("branch")!=string::npos){
                size_t init, end;
                init = it->find_first_of("-")+1;
                end = it->find_last_of("-");
                string tid = it->substr(init, end-init);
                int varid = util::intValueOf(it->substr(end+1));
                BranchOperation* bop = new BranchOperation();
                bop->setThreadId(tid);
                bop->setVariableId(varid);
                analyzer->closestBranches.push_back(bop);
                analyzer->flipMap.push_back(false);
            }
            
            if(analyzer->closestBranches.size() == analyzer->MAXDIST)
                break;
        }
        
        do
        {
            cout << "\n------------------------\n";
            cout << "["<< ++numAttempts <<"] Attempt by flipping branch.\n";
            
            analyzer->getNextTraceCombination(&traceComb);
            
            //** parse contraints from thread symbolic traces
            cleanDataStructures();
            for(map<string,string>::iterator it = traceComb.begin(); it != traceComb.end(); ++it)
            {
                string key = it->first+it->second; //key is "tid-pathid"
                parse_constraints(analyzer->mapPathToTrace[key]);
            }
            
            //debug: print constraints
            if(debug){
                printDataStructures();
            }
            
            
            //** generate the constraint model and try to solve it
            cmgen->openOutputFile();
            cmgen->addMemoryOrderConstraints(operationsByThread);
            cmgen->addReadWriteConstraints(readset,writeset,operationsByThread);
            cmgen->addPathConstraints(pathset, false); //do not invert the assert condition
            cmgen->addLockingConstraints(lockpairset);
            cmgen->addForkStartConstraints(forkset, startset);
            cmgen->addJoinExitConstraints(joinset, exitset);
            cmgen->addWaitSignalConstraints(waitset, signalset);
            cmgen->addBarrierConstraints(barrierset,operationsByThread);
            cmgen->addBBClockConstraints();
            cout << "\n### SOLVING CONSTRAINT MODEL: Z3\n";
            success = cmgen->solve();
            
        }while(!success && analyzer->hasNext());
        
        if(success)
        {
            cout << "\n>> FOUND BUG AVOIDING SCHEDULE\n";
            EventPair ep;//create dummy event pair
            Segment seg1 = std::make_pair(0,0);
            Segment seg2 = std::make_pair(0,0);
            ep = std::make_pair(seg1, seg2);
            (*altSchedules)[ep] = altScheduleOrd;
        }
        
    }
    
    return success;
}


int main(int argc, char *const* argv)
{
    //parse parameters
    parse_configFile();
    parse_args(argc, argv);
    
    map<EventPair, vector<string> > altSchedules; //set used to store the event pairs that yield a sat non-failing alternative schedule
    
    if(!dspMode) //find failing schedule
    {
        //parse_avisoTrace();
        bool foundBug = findFailingSchedule();
        /*for(map<string,string>::iterator it = solutionValues.begin();it!= solutionValues.end();it++){
         cout << it->first << " : " << it-> second << endl;
         }*/
        
        
        if(foundBug){
            //save variable values to file
            string fname = solutionFile.substr(0,solutionFile.find_last_of("."))+"_values.txt";
            util::saveVarValues2File(fname,solutionValues);
        }
    }
    else //find alternate schedule and generate DSP
    {
        //load variable values for failing schedule
        string valFile = solutionFile.substr(0,solutionFile.find_last_of("."))+"_values.txt";
        solutionValuesFail = util::loadVarValuesFromFile(valFile);
        
        bool success;
        
        if(solutionAltFile.empty()){
            //compute alternate schedule
            success = findBugRootCause(&altSchedules);
            
            if(success){
                //save alternate schedule
                solutionFile.insert(solutionFile.find(".txt"),"ALT");
                map<string,string> tmp;
                scheduleLIB::saveScheduleFile(solutionFile,altScheduleOrd, tmp); //TODO: is tracecomb null in here?
                
                //save variable values to file
                string fname = solutionFile.substr(0,solutionFile.find_last_of("."))+"_values.txt";
                util::saveVarValues2File(fname,solutionValues);
                solutionValuesAlt = solutionValues;
            }
        }
        else{
            //load failing and alternate schedules from file
            success = true;
            map<string,string> tmp;
            cout << "\n>> Loading failing schedule from file: "<< solutionAltFile <<"\n";
            loadScheduleFromFile(solutionFile, &solution, &tmp);
            cout << ">> Loading alternate schedule from file: "<< solutionAltFile <<"\n";
            loadScheduleFromFile(solutionAltFile,&altScheduleOrd,&tmp);
            
            //load variable values for failing schedule
            valFile = solutionAltFile.substr(0,solutionAltFile.find_last_of("."))+"_values.txt";
            solutionValuesAlt = util::loadVarValuesFromFile(valFile);
            
            //create dummy event pair
            EventPair ep;
            Segment seg1 = std::make_pair(0,0);
            Segment seg2 = std::make_pair(0,0);
            ep = std::make_pair(seg1, seg2);
            altSchedules[ep] = altScheduleOrd;
        }
        
        //print data-dependencies and stats only when Symbiosis has found an alternate schedule
        if(success)
        {
            graphgen::drawAllGraph(altSchedules, solution);
        }
    }
    return 0;
}

