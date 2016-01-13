//
//  Analyzer.h
//  cortexsolver
//
//  Created by Nuno Machado on 05/05/15.
//  Copyright (c) 2015 Nuno Machado. All rights reserved.
//

#ifndef __cortexsolver__Analyzer__
#define __cortexsolver__Analyzer__

#include <stdio.h>
#include <map>
#include <set>
#include "Trie.h"
#include "Operations.h"

//implements the methods responsible for analyzing the symbolic traces and
//decide which combination of traces should be tested by the solver at each moment
class TraceAnalyzer {

public:
    std::map<std::string, std::vector<std::string> > pathsByThread;   //map: thread id -> vector with the paths ids collected for each thread
    std::map<std::string, Trie> triesByThread;                        //map: thread id -> trie with thread's execution paths
    std::map<std::string, std::string> mapPathToTrace;                //map "tid_path id" -> name of a symbolic trace that follows that path
    std::map<std::string, std::vector<std::string> > executionIds;    //map execution id -> vector with path ids per thread (e.g. "0-1", "1-100101",..)
    int attempts;                                                     //number of attempts to obtain the buggy interleaving tested so far
    int bflips;                                                       //number of branch flips performed
    std::set<std::size_t> testedCombs;                                //set containing the hashes of the combinations of traces already tested
    std::vector<BranchOperation*> closestBranches;                    //stores the MAXDIST branches away from the assertion in the successful schedule
    std::vector<bool> flipMap;                                        //vector that indicates which of the MAXDIST closest branches were flipped to test a given trace combination
    std::map<std::string, std::string> initComb;                      //stores the initial trace combination tested, which will serve as basis to flip branches
    std::vector< std::vector<int> > combSet; //set with possible combinations of branches
    //int combCount = 0;    //current combination of branches to attempt
    int bfs = 0;          //number of different traces tested so far for the same combination of branches
    int MAXDIST = 4; //maximum number of branches away from the assertion that are allowed to be flipped for exploration purposes
    int MAXBFS = 0;  //maximum number of shortest paths (according to BFS) to be tested with a prefix corresponding to the flipped branch
    
    TraceAnalyzer();
    ~TraceAnalyzer() {}
    void loadTraces();
    bool hasNext();   //indicates whether there are non-attempted combinations of per-thread left to test
    void getNextTraceCombination(std::map<std::string, std::string> *traceComb); //returns a combination of per-thread paths to test in the solver (one execution path per thread)
    
private:
    void updateFlipMap(); //updates the flip map for a new combination of flipped branches
    bool isNewCombination(std::map<std::string, std::string> *traceComb);
    void flipBranch(std::map<std::string, std::string> *traceComb);
    void synthesizeNewSymbolicTraces();
    void generateFlipBranchFile(std::string tid, int flipPos);
};
#endif /* defined(__cortexsolver__Analyzer__) */
