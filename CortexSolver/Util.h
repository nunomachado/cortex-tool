//
//  Util.h
//  symbiosisSolver
//
//  Created by Nuno Machado on 03/01/14.
//  Copyright (c) 2014 Nuno Machado. All rights reserved.
//


#ifndef __symbiosisSolver__Util__
#define __symbiosisSolver__Util__

#include <string>
#include <sys/types.h>
#include "Parameters.h"
#include "Operations.h"
#include "Schedule.h"

namespace util{
    
    void saveVarValues2File(std::string filename, std::map<std::string, std::string> mapValues); // store a map in a file
    std::map<std::string, std::string> loadVarValuesFromFile(std::string filename); // load map from a file
    std::string threadTabsPP(int tab);               //add 3x(thread_ID) to a better PP
    void fillScheduleOrd(std::string tid, std::map< std::string, std::vector<Operation*>> *op_list , Schedule* schedule);
    std::string stringValueOf(int i);                           //transforms an int into a string
    int intValueOf(std::string i);                           //transforms a string into an int
    pid_t popen2(const char *command, int *infp, int *outfp);   //similar to popen, but allows for bidirectional communication
    void print_state (const std::ios& stream);                  //prints the error flags when an attempt to open a file fails
    
    std::string extractFileBasename(std::string name);          //from a path to file like a/b/c.txt, extracts the basename c.txt
    std::string extractFileBasename(char* path);                //from a path to file like a/b/c.txt, extracts the basename c.txt
    bool isClosedExpression(std::string expr);                  //returns true if an expression is closed, i.e. its number of '(' == number of ')'
    const std::string getcwd();                           //returns the path of the current working directory
    
    std::string parseThreadId(std::string operation);       //parses the threadId of an operation
    std::string parseVar(std::string operation);       //parses the variable of an operation
    bool filenameComparator(std::string a, std::string b);   //comparator to sort filenames in ascending order of their length
    std::string readLinePipe(int procR);                //read line from pipe
    bool subsetComparator(std::vector<int> a, std::vector<int> b); //comparator to sort subsets of values in ascending order of their size
    
}


#endif
