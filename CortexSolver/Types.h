//
//  AvisoUtil.h
//  symbiosisSolver
//
//  Created by Nuno Machado on 10/01/14.
//  Copyright (c) 2014 Nuno Machado. All rights reserved.
//

#ifndef __symbiosisSolver__AvisoUtil__
#define __symbiosisSolver__AvisoUtil__

#include <iostream>
#include <map>
#include <vector>
#include <string>


struct AvisoEvent {
    std::string tid;
    int loc;
    std::string filename;
    
} ;

typedef std::vector<AvisoEvent> AvisoEventVector;
typedef std::map<std::string, AvisoEventVector > AvisoTrace;

typedef std::pair<int,int> Segment;
typedef std::pair<Segment,Segment> EventPair;

std::string pairToString(EventPair p, std::vector<std::string> solution); //EventPair pretty print
std::string bugCauseToString(EventPair p, std::vector<std::string> solution); //pretty print when bug root cause found
std::string bugCauseToGviz(EventPair p, std::vector<std::string> solution); //print bug root cause to graphviz


/* struct representing a segment of contiguous instructions
 * executed by the same thread in a given execution schedule
 */
struct ThreadSegment {
    int initPos;    //initial position of the segment in the vector representing the schedule
    int endPos;     //end position
    bool hasDependencies; //indicates if this segment contains an operation that is involved in a data-dependence
    bool markAtomic; //indicates if this segment should be marked as amotic in the graphviz file
    std::string tid;
    std::vector<int> dependencies; //positions of the operations referring to dependencies
} ;

#endif /* defined(__symbiosisSolver__AvisoUtil__) */
