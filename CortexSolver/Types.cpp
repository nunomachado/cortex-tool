//
//  AvisoUtil.cpp
//  symbiosisSolver
//
//  Created by Nuno Machado on 10/01/14.
//  Copyright (c) 2014 Nuno Machado. All rights reserved.
//

#include "Types.h"



std::string pairToString(EventPair p, std::vector<std::string> solution)
{
    std::string ret;
    
    if(p.first.first!= p.first.second)
        ret.append("(["+solution[p.first.first]+ ".."+solution[p.first.second]+"] ,");
    else
        ret.append("("+solution[p.first.first]+" ,");
    if(p.second.first!=p.second.second)
        ret.append(" ["+ solution[p.second.first] +".."+ solution[p.second.second] +"])\n");
    else
        ret.append(" "+solution[p.second.first] +")\n");
    
    return ret;
}

std::string bugCauseToString(EventPair p, std::vector<std::string> solution)
{
    std::string ret;
    
    if(p.first.first!= p.first.second)
        ret.append("["+solution[p.first.first]+ ".."+solution[p.first.second]+"] should execute after");
    else
        ret.append(solution[p.first.first]+" should execute after");
    if(p.second.first!=p.second.second)
        ret.append(" ["+ solution[p.second.first] +".."+ solution[p.second.second] +"]\n");
    else
        ret.append(" "+solution[p.second.first] +"\n");
    
    return ret;
}

std::string bugCauseToGviz(EventPair p, std::vector<std::string> solution)
{
    std::string ret;
    
    if(p.first.first!= p.first.second)
        ret.append("["+solution[p.first.first]+ ".."+solution[p.first.second]+"]\\n should execute after\\n");
    else
        ret.append(solution[p.first.first]+"\\n should execute after \\n");
    if(p.second.first!=p.second.second)
        ret.append(" ["+ solution[p.second.first] +".."+ solution[p.second.second] +"]\\n\\n\\n");
    else
        ret.append(" "+solution[p.second.first] +"\\n\\n\\n");
    
    return ret;
}