//
//  GraphvizGenerator.h
//  symbiosisSolver
//
//  Created by Nuno Machado on 30/07/14.
//  Copyright (c) 2014 Nuno Machado. All rights reserved.
//

#ifndef __symbiosisSolver__GraphvizGenerator__
#define __symbiosisSolver__GraphvizGenerator__

#include <iostream>
#include <stdlib.h>
#include "Types.h"


namespace graphgen{

    std::string cleanCallFunc(std::string funcSign);
    std::string cleanRight(const std::string& op);
    std::string getCodeLine(int line, std::string finename, std::string type);
    void drawAllGraph(const std::map<EventPair, std::vector<std::string>>& altSchedules, const std::vector<std::string>& solution);
    void genAllGraphSchedules(std::vector<std::string> failSchedule, std::map<EventPair, std::vector<std::string> > altSchedules);
    void genGraphSchedule(std::vector<std::string> failSchedule, EventPair invPair, std::vector<std::string> altSchedule);
    void drawGraphviz(const std::vector<ThreadSegment>& segsFail, const std::vector<ThreadSegment>& segsAlt, const std::vector<std::string>& failSchedule, const std::vector<std::string>& altSchedule, const EventPair& invPair);

}

#endif