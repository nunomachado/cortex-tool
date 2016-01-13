//
//  Analyzer.cpp
//  cortexsolver
//
//  Created by Nuno Machado on 05/05/15.
//  Copyright (c) 2015 Nuno Machado. All rights reserved.
//


#include <dirent.h>
#include <stdlib.h>
#include <math.h>
#include <algorithm>    // std::max
#include <unistd.h>
#include <signal.h>
#include <cstring>
#include <thread>         // std::this_thread::sleep_for
#include <chrono>         // std::chrono::seconds
#include "TraceAnalyzer.h"
#include "Common.h"
#include "Util.h"
#include "ConstraintModelGenerator.h"

using namespace std;


TraceAnalyzer::TraceAnalyzer()
{
    attempts = 1;
    bflips = 0;

    if(cortex_D != -1){
        MAXDIST = cortex_D;
    }
    if(cortex_N != -1){
        MAXBFS = cortex_N;
    }
    

}


void TraceAnalyzer::loadTraces()
{
    //** find symbolic trace files and populate map pathsByThread
    DIR* dirFile = opendir(symbFolderPath.c_str());
    if ( dirFile )
    {
        struct dirent* hFile;
        while (( hFile = readdir( dirFile )) != NULL )
        {
            if ( !strcmp( hFile->d_name, "."  )) continue;
            if ( !strcmp( hFile->d_name, ".." )) continue;
            
            // in linux hidden files all start with '.'
            if (hFile->d_name[0] == '.' ) continue;
            
            if ( strstr( hFile->d_name, "T" ))
            {
                char filename[250];
                strcpy(filename, hFile->d_name);
                //std::cerr << "found a symbolic trace file: " << filename << endl;
                
                //** parse thread id to serve as key in the map
                string shortname = util::extractFileBasename(filename);
                string tid = shortname.substr(shortname.find("T")+1,shortname.find("_")-1);
                
                //** parse execution id
                size_t init = shortname.find("_")+1;
                size_t end = shortname.find("_",init);
                string execId = shortname.substr(init,end-init);
                
                //** parse thread's path id (which corresponds to the last line in the thread's symbolic trace file)
                ifstream fin;
                fin.open(symbFolderPath+"/"+filename);
                if (!fin.good())
                {
                    string tmpDir = symbFolderPath.substr(0,symbFolderPath.find_last_of("\n"));
                    fin.open(tmpDir+"/"+filename);
                    if (!fin.good())
                    {
                        //check if file is in the original folder (i.e. it wasn't synthesized)
                        util::print_state(fin);
                        cerr << " -> Error opening file "<< symbFolderPath+"/"+filename <<".\n";
                        fin.close();
                        exit(0);
                    }
                }
                char ch;
                string lastLine;
                fin.seekg(-1, std::ios::end);
                fin.get(ch);
                if (ch == '\n')
                {
                    fin.seekg(-2, std::ios::cur); //account for special chars on mac os
                    fin.seekg(-1, std::ios::cur);
                    fin.get(ch);
                    while(ch != '\n' && ch!='I'){ //the 'I' is to indicate the initial event of 'InstrumentationHandler' (BAD: this is not general!)
                        fin.seekg(-2, std::ios::cur);
                        fin.get(ch);
                    }
                    std::getline(fin,lastLine);
                    //cout << "The last line : " << lastLine << '\n';
                }
                
                //check if last line is indeed the path id; if there is no path, consider "-1"
                string tpathid = "-1";
                if(lastLine.find("pathid-")!=string::npos){
                    tpathid = lastLine.substr(lastLine.find("-1"));
                    //cout << "path: "<<tpathid << endl;
                }
                
                //populate data structures
                if(pathsByThread.count(tid))
                {
                    pathsByThread[tid].push_back(tpathid);
                    triesByThread[tid].addPath(tpathid);
                    mapPathToTrace[tid+tpathid] = shortname;
                }
                else
                {
                    vector<string> vec;
                    vec.push_back(tpathid);
                    pathsByThread[tid] = vec;
                    mapPathToTrace[tid+tpathid] = shortname;
                    
                    //create corresponding trie
                    Trie* trie = new Trie();
                    trie->addPath(tpathid);
                    triesByThread[tid] = *trie;
                }
                
                if(initComb.empty()){
                    if(executionIds.count(execId)){
                        executionIds[execId].push_back(tid+tpathid);
                    }
                    else{
                        vector<string> vec;
                        vec.push_back(tid+tpathid);
                        executionIds[execId] = vec;
                    }
                }
            }
        }
        closedir( dirFile );
    }
    
    
    /*for(map<string, Trie>::iterator itt = triesByThread.begin(); itt != triesByThread.end();++itt)
     {
        cout << "\n>>";
        itt->second.print();
     }//debug trie construction */
    
    //** sort files in ascending order of their name (i.e. path length)
    int numThreads = 0;
    for(map<string, vector<string> >:: iterator it = pathsByThread.begin(); it != pathsByThread.end() ; ++it){
        std::sort(pathsByThread[it->first].begin(), pathsByThread[it->first].end(), util::filenameComparator);
        numThreads++;
    }
    
    //we only set the initial combination once
    if(initComb.empty()){
        //** pick initial trace combination
        string minComb = "";
        string minExecId;
        for(map<string, vector<string> >:: iterator it = executionIds.begin(); it != executionIds.end() ; ++it){
            string tmpExecId = it->first;
            vector<string> paths = it->second;
            string tmpExecPath;
            
            //we need to make sure that the combination has traces for all the threads
            if(paths.size() < numThreads){
                it = executionIds.erase(it);
                //continue;
            }
            else{
                //** compute execution length
                for(vector<string>::iterator pit = paths.begin(); pit != paths.end(); ++pit){
                    string pathid = (*pit);
                    tmpExecPath += pathid.substr(pathid.find("-")+1);
                }
                //cout << "EXECUTION "<<tmpExecId << ": "<<tmpExecPath.length() << endl;
                if(tmpExecPath.length() < minComb.length() || minComb.empty()){
                    minComb = tmpExecPath;
                    minExecId = tmpExecId;
                }
            }
        }
        
        cout << "[Analyzer] Initial execution is " << minExecId << ":\n";
        for(vector<string>::iterator pit = executionIds[minExecId].begin(); pit != executionIds[minExecId].end(); ++pit){
            string pathid = (*pit);
            string tid = pathid.substr(0,pathid.find("-"));
            pathid = pathid.substr(pathid.find("-"));
            initComb[tid] = pathid;
            cout << ">> T"<<tid<<pathid << "\n";
        }
    }
    
}


bool TraceAnalyzer::hasNext()
{
    //if(flipMap.empty() || combCount < pow(2,MAXDIST)) //FOR SOLUTION WITH BITS
    if(!combSet.empty() || flipMap.empty())
        return true;
    
    cout << ">> No more combinations left.\n";
    return false;
}

int getBit(int value, int position)
{
    int bit = value & (int) pow(2, position);
    return (bit > 0 ? 1 : 0);
}


/*
 *  Function used to update the counters used to generate all combinations
 *  of symbolic trace files. Corresponds to computing all the possible subsets for
 *  the number of branches to flip.
 */
void TraceAnalyzer::updateFlipMap()
{
    if(combSet.empty()){
        vector<int> set;
        for(int i = 0; i < flipMap.size(); i++){
            set.push_back(i);
        }
        vector<int> empty;
        combSet.push_back( empty );
        
        for (int i = 0; i < set.size(); i++)
        {
            vector< vector<int> > subsetTemp = combSet;
            
            for (int j = 0; j < subsetTemp.size(); j++)
                subsetTemp[j].push_back( set[i] );
            
            for (int j = 0; j < subsetTemp.size(); j++)
                combSet.push_back( subsetTemp[j] );
        }
        std::sort(combSet.begin(), combSet.end(), util::subsetComparator);
        combSet.erase(combSet.begin());
        cout << endl;
    }
    else{
        if(combSet.size() > 0 && bfs < MAXBFS){
            bfs++;
            return;
        }
         else{
            bflips++;
            bfs = 0;
        }
    }
    
    for(int i = 0; i < flipMap.size(); i++){
        flipMap[i] = false;
    }
    
    
    for(int i = 0; i < combSet[0].size(); i++){
        int b = combSet[0][i];
        flipMap[b] = true;
        
    }
    combSet.erase(combSet.begin()); //advance to the next combination
    
    /*cout << "COMBINATION: ";
    for(int i = 0; i < flipMap.size(); i++){
        cout << flipMap[i];
    }
    cout << endl;//*/
    
    
    //SOLUTION USING BITS
    //first case
   /* if(combCount == 0){
        combCount = 1;
    }
    else{
        if(combCount > 0 && bfs < MAXBFS){
            bfs++;
            return;
        }
        else
            bfs = 0;
    }
    
    //cout << "COMBINATION: ";
    for(int d = 0; d < flipMap.size(); d++){
        flipMap[d] = getBit(combCount, d);
        //cout << flipMap[d];
    }
    //cout << endl;
    combCount++;//*/
}

/*
 *  Checks whether a given combination of traces was already
 *  tested or not.
 */
bool TraceAnalyzer::isNewCombination(map<string, string> *traceComb)
{
    string combHash;
    for(map<string,string>::iterator it = traceComb->begin(); it != traceComb->end();++it)
    {
        combHash += it->second;
    }
    
    size_t h = std::hash<std::string>()(combHash);
    if(testedCombs.find(h)!=testedCombs.end()){
        return false;
    }
    
    testedCombs.insert(h);
    return true;
}

/*
 *  Computes a non-failing schedule and searches for the closest branch in the schedule
 *  to flip. If there is already a trace with that branch flipped, choose that trace.
 *  Otherwise run symbolic execution to obtain a new trace.
 */
void TraceAnalyzer::flipBranch(map<string,string> *traceComb)
{
    // generate a non-failing schedule to pick the closest branches,
    // when we are interested in finding the failing schedule
    // using traces from a failing original execution
    if(failScheduleOrd.empty() && !dspMode){
        
        cout << "[Analyzer] Compute non-failing schedule.\n";
        ConstModelGen* cmgen = new ConstModelGen();
        cmgen->createZ3Solver();
        
        bool success = false;
        
        cmgen->openOutputFile(); //** opens a new file to store the model
        cmgen->addMemoryOrderConstraints(operationsByThread);
        cmgen->addReadWriteConstraints(readset,writeset,operationsByThread);
        cmgen->addPathConstraints(pathset, false); //don't invert bug condition to get a satisfiable model
        cmgen->addLockingConstraints(lockpairset);
        cmgen->addForkStartConstraints(forkset, startset);
        cmgen->addJoinExitConstraints(joinset, exitset);
        cmgen->addWaitSignalConstraints(waitset, signalset);
        cmgen->addBarrierConstraints(barrierset,operationsByThread);
        cout << "\n### SOLVING CONSTRAINT MODEL: Z3\n";
        success = cmgen->solve();
        
        if(!success){
            cout << "[Solver] Unable to find feasible schedule.\n";
            exit(EXIT_FAILURE);
        }
            
        
        //** clean data structures
        cmgen->resetSolver();
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
        lockpairStack.clear();
        operationsByThread.clear();
        cmgen->closeSolver();
        
        scheduleLIB::printSch(failScheduleOrd);
        //save solution schedule
        //solutionFile.insert(solutionFile.find(".txt"),"ALT");
        map<string,string> tmp;
        scheduleLIB::saveScheduleFile(solutionFile,scheduleLIB::schedule2string(failScheduleOrd), tmp); 
        
        //find assertion event
        vector<Operation*>::reverse_iterator tmpit = failScheduleOrd.rbegin();
       /* for(tmpit = failScheduleOrd.rbegin(); tmpit!=failScheduleOrd.rend();++tmpit)
        {
            SyncOperation* op = dynamic_cast<SyncOperation*>(*tmpit);
            if(op!=0 && op->getType()== "Assert")
                break;
        }*/
        
        //fill closestBranches structure with the MAXDIST branches in the schedule closest to the assertion
        for(vector<Operation*>::reverse_iterator it = tmpit; it!=failScheduleOrd.rend();++it)
        {
            BranchOperation* tmpOp = dynamic_cast<BranchOperation*>(*it);
            if(tmpOp!=0){
                //store all branches, then use MAXDIST to constrain the number of branches that are actually flipped
                if(closestBranches.size() < MAXDIST){
                    closestBranches.push_back(tmpOp);
                    flipMap.push_back(false);
                }
                else
                    break;
            }
        }
        updateFlipMap();
    }
    
    //set the current trace combination to the initial one
    if(bfs == 0){
        for(map<string,string>::iterator it = initComb.begin(); it!=initComb.end();++it){
            (*traceComb)[it->first] = it->second;
        }
    }
    
    int minFlipPos = -1; //used to indicate the more general path condition that we can flip (necessary when we flip more than one branch in the schedule for the same thread)
    //flip the branches marked as 'true' in flipMap
    map<string,string> cacheFlip; //map: tid -> previous path prefix; used to cache previous branches of the same thread, when we flip combinations of branches simultaneously
    for(int i = flipMap.size()-1; i >= 0; i--)
    {
        if(!flipMap[i])
            continue;
        
        BranchOperation* br = closestBranches[i];
        string tid = br->getThreadId();
        int flipPos = br->getVariableId()+2; //we need to add 2 due to the root of the traces (i.e. "-1")
        if(flipPos < minFlipPos || minFlipPos == -1){
            minFlipPos = flipPos;
        }
        
        string curPath;
        if(cacheFlip.count(tid)){
            curPath = cacheFlip[tid];
        }
        else{
            curPath = initComb[tid];
        }
        cout << "[Analyzer] Next branch to flip is "<< br->getConstraintName() <<" (from trace T"<< tid << "_" << curPath<<").\n";
        curPath = curPath.substr(curPath.find_first_of("_")+1);
        
        //flip branch
        (curPath[flipPos] == '0') ? curPath[flipPos] = '1' : curPath[flipPos] = '0';
        cacheFlip[tid] = curPath;
        curPath = curPath.substr(0,flipPos+1); //trim the rest of the trace
        
        //check whether we already have paths with that branch flipped
        bool useSynthesis = false;
        if(triesByThread[tid].searchPathPrefix(curPath)){
            string newTrace = triesByThread[tid].getPathByPrefix(curPath, bfs);
            string prevTrace = (*traceComb)[tid];
            
            if(newTrace!=prevTrace)
            {
                //check wether traces have a loop, and if newTrace has simply more iterations than prevTrace
                int matches = 0;
                int count = 0;
                for(string::reverse_iterator ritP=prevTrace.rbegin(); ritP!=prevTrace.rend()-1; ++ritP)
                {
                    string::reverse_iterator ritN = newTrace.rbegin()+count;
                    if(*ritP == *ritN){
                        matches++;
                    }
                    count++;
                }
                
                if(matches == prevTrace.length()-1 && bfs <= 1){
                    cout << "[Analyzer] Found trace "<< ("T"+tid+"_"+newTrace) <<" with prefix "<<curPath<<", but appears to be a loop. Run symbolic execution to try generating shorter traces.\n";
                    useSynthesis = true;
                }
                else {
                    cout << "[Analyzer] Found trace with prefix "<< curPath <<".\n";
                    
                    if((*traceComb)[tid]!=newTrace){
                        cout << "[Analyzer] Picked trace "<< ("T"+tid+"_"+newTrace) <<" (#"<< bfs <<").\n";
                        (*traceComb)[tid] = newTrace;
                    }
                    else{
                        cout << "[Analyzer] The trace "<< ("T"+tid+"_"+newTrace) <<" is the only one for this prefix.\n";
                        bfs = MAXBFS;
                    }
                }
            }
        }
        else{
            cout << "[Analyzer] There is no trace in the trie with prefix "<< curPath <<". Run symbolic execution to generate new traces.\n";
            useSynthesis = true;
        }
        
        //synthesize new traces if necessary
        if(useSynthesis){
            generateFlipBranchFile(tid, (minFlipPos-2)); //subtract 2 to account for the inital chars "-1"
            synthesizeNewSymbolicTraces();
            
            if(triesByThread[tid].searchPathPrefix(curPath)){
                cout << "[Analyzer] Found trace with prefix "<< curPath <<".\n";
                string newTrace = triesByThread[tid].getPathByPrefix(curPath, bfs);
                cout << "[Analyzer] Picked trace "<< ("T"+tid+"_"+newTrace) <<" (#"<< bfs <<").\n";
                (*traceComb)[tid] = newTrace;
            }
            else{
                cout << "[Analyzer] No trace found with prefix "<< curPath <<".\n";
                bfs = MAXBFS;
            }
        }
        
    }
    updateFlipMap();
}


void TraceAnalyzer::generateFlipBranchFile(string tid, int flipPos)
{
    //generate file with flipped branches
    std::ofstream flipFile;
    string filename = "/home/symbiosis/work/cortex/CortexSolver/tmp/flipFile.txt";
    flipFile.open(filename, ios::trunc);
    cout << "Creating file: " << filename << endl;
    if(!flipFile.is_open())
    {
        cerr << " -> Error opening file "<< filename <<".\n";
        flipFile.close();
        exit(EXIT_FAILURE);
    }
    flipFile << tid << " " << flipPos << endl;
    flipFile.close();
}

/*
 * Calls the Symbolic Execution Engine and generates symbolic traces for the flipped branch.
 * Adds the traces to the Trie afterwards.
 */
void TraceAnalyzer::synthesizeNewSymbolicTraces()
{
    //check if we have already synthesized any new traces - used for debugging
    /*if(symbFolderPath.find("/sts") == string::npos){
        symbFolderPath = symbFolderPath+"/sts";
        loadTraces();
        return;
    }*/
    
    int procR, procW;
    string seExePath = "cd /home/symbiosis/work/cortex/CortexSE/jpf-symbiosis/bin; java -Xmx1500m -jar /home/symbiosis/work/cortex/CortexSE/jpf-core/build/RunJPF.jar +shell.port=4242 "+jpfFile+" 2>&1 & sleep "+jpftimeout+"; kill $!; echo \"endSE\"";
    char *command = (char *)seExePath.c_str();
    
    int sepid = util::popen2(command, &procW, &procR);
    
    cout << "[SE Engine] Init symbolic execution...\n";
    cout << "[SE Engine] Created SE process " << sepid << endl;
    if (!sepid)
    {
        perror("Problems with pipe");
        exit(1);
    }
    
    //Create log file
    std::ofstream logFile;
    string filename = "/home/symbiosis/work/cortex/SE_LOGFILE.txt";
    logFile.open(filename, ios::trunc);
    //cout << "Creating logfile: " << filename << endl;
    if(!logFile.is_open())
    {
        cerr << " -> Error opening file "<< filename <<".\n";
        logFile.close();
        exit(EXIT_FAILURE);
    }
    
    std::this_thread::sleep_for (std::chrono::seconds(5));
    string line = "";
    while(line.find("endSE")==string::npos)
    {
        line = util::readLinePipe(procR);
        logFile << line << endl;
        
        if(line.find("=== error")!=string::npos){
            cout << "[SE Engine] No feasible execution found for this branch flip." << endl;
            bfs = MAXBFS; //indicates that there is no need to test this branch flip anymore
            break;
        }
        if(line.find("endSE")!=string::npos){
            cout << "[SE Engine] Symbolic execution ended." << endl;
            cerr << ">> Killing SE process "<< sepid <<" and "<< (sepid+1) << endl;
            kill(sepid,SIGKILL);
            kill(sepid+1,SIGKILL);//** Nuno: apparently, the process created has always PID=z3pid+1, so we must increment the pid in order to kill it properly
        }
    }
    
    close(procR);
    close(procW);
    logFile.close();
    //cerr << ">> Killing SE process "<< sepid <<" and "<< (sepid+1) << endl;
    //kill(sepid,SIGKILL);
    //kill(sepid+1,SIGKILL);//** Nuno: apparently, the process created has always PID=z3pid+1, so we must increment the pid in order to kill it properly
    
    if(symbFolderPath.find("/sts") == string::npos)
       symbFolderPath = symbFolderPath+"/sts";
    loadTraces();
}


/*
 * Returns the next combination of thread execution paths to test, if there exists one
 */
void TraceAnalyzer::getNextTraceCombination(map<string,string> *traceComb)
{
    /* Pick one symbolic trace per thread.
     * Algorithm:
     * 1) return the combination of traces of the shortest execution for the first attempt
     * 2) check unsat core for path conditions (PCs) to flip
     * 3) if there is no PCs in the unsat core (rather than the bug condition),
     *    flip the PC closer to the assertion in the non-failing trace
     */
    
    string combHash = ""; //hash representing a given combination of traces
    
    if(unsatCoreStr.empty()){ // 1) first attempt
        
        for(map<string, string>::iterator cit = initComb.begin(); cit!=initComb.end();++cit){
            string tid = cit->first;
            string pathid = cit->second;
            (*traceComb)[tid] = pathid;
            combHash += pathid;
        }
        
        testedCombs.insert(std::hash<std::string>()(combHash));
        
        return;
    }
    
    vector<string> pconds;//candidate path conditions to be flipped (because they conflict in the unsat core)
    
    size_t init = unsatCoreStr.find("PC");
    bool confBug = false; //flag indicating whether the path conditions in the unsat are conflicting with the bug condition
    while(init != string::npos){
        size_t end = unsatCoreStr.find_first_of(" ",init);
        string pc = unsatCoreStr.substr(init,end-init);
        if(pc.find("BUG")==string::npos){
            pconds.push_back(pc);
        }else
            confBug = true;
        init = unsatCoreStr.find("PC",end);
    }
    
    // 3) if we don't have any PCs to flip, return the next combination of traces;
    //    otherwise, return a combination with the conflicting PC flipped
    if(pconds.empty() || pconds.size() >= 2 || (pconds.size() <= 2 && !confBug)){
        if(pconds.empty()){
            cout << "[Analyzer] Unsat core does not have PCs. Flip the branch closest to the assertion.\n";
        }
        else if(pconds.size() >= 2){
            cout << "[Analyzer] Unsat core has too many PCs. Flip the branch closest to the assertion instead.\n";
        }
        else{
            cout << "[Analyzer] The PCs in the unsat core are not conflicting with the bug condition. Flip the branch closest to the assertion instead.\n";
        }
        
        //get the trace corresponding to the flip of the branch closest to the assertion
        flipBranch(traceComb);
        
        //get a new combination of traces if this one was already tested
        while(!isNewCombination(traceComb) && hasNext()){
            cout << "[Analyzer] Combination was already tested! Pick another one.\n\n";
            flipBranch(traceComb);
        }
        
        attempts++;
        return;
    }
    
    //2) check unsat core for path conditions (PCs) to flip
    //parse thread id
    string tid = pconds[0].substr(pconds[0].find("T")+1);
    
    //parse the n-th branch to flip
    init = pconds[0].find("PC")+2;
    size_t end = pconds[0].find("_");
    int flipPos = util::intValueOf(pconds[0].substr(init,end-init)) + 2; //we need to add 2 due to the root of the traces (i.e. "-1")
    
    //check whether we already have paths with the that branch flipped
    string curPath = (*traceComb)[tid];
    curPath = curPath.substr(curPath.find_first_of("_")+1);
    
    cout << "[Analyzer] Found PC "<< pconds[0] <<" in unsat core. Flip branch "<<flipPos<<" in trace T"<< tid << curPath <<".\n";
    
    //flip branch
    curPath = curPath.substr(0,flipPos+1); //trim the rest of the trace
    (curPath[flipPos] == '0') ? curPath[flipPos] = '1' : curPath[flipPos] = '0';
    
    if(triesByThread[tid].searchPathPrefix(curPath)){
        cout << "[Analyzer] Found trace with prefix "<< curPath <<".\n";
        string newTrace = triesByThread[tid].getPathByPrefix(curPath,bfs);
        cout << "[Analyzer] Picked trace "<< ("T"+tid+"_"+newTrace) <<" (#"<< bfs <<").\n";
        (*traceComb)[tid] = newTrace;
    }
    else{
        cout << "[Analyzer] There is no trace in the trie with prefix "<< curPath <<". Run symbolic execution to generate new traces.\n";
        generateFlipBranchFile(tid, flipPos);
        synthesizeNewSymbolicTraces();
        if(triesByThread[tid].searchPathPrefix(curPath)){
            cout << "[Analyzer] Found trace with prefix "<< curPath <<".\n";
            string newTrace = triesByThread[tid].getPathByPrefix(curPath, bfs);
            cout << "[Analyzer] Picked trace "<< ("T"+tid+"_"+newTrace) <<" (#"<< bfs <<").\n";
            (*traceComb)[tid] = newTrace;
        }
        else{
            cout << "[Analyzer] No trace found with prefix "<< curPath <<".\n";
        }
    }
    
    //get a new combination of traces if this one was already tested
    while(!isNewCombination(traceComb)){
       cout << "[Analyzer] Combination was already tested! Pick another one.\n\n";
       flipBranch(traceComb);
    }//*/
    attempts++;
}

