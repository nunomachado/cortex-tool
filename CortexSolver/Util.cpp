//
//  Util.cpp
//  symbiosisSolver
//
//  Created by Nuno Machado on 03/01/14.
//  Copyright (c) 2014 Nuno Machado. All rights reserved.
//

#include "Util.h"
#include "Parameters.h"
#include <sstream>
#include <string>
//#include <iostream>
#include <sys/types.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <fstream>


#define READ 0
#define WRITE 1
#define LINEMAX 256

using namespace std;

//write map values to a file
void util::saveVarValues2File(std::string filename, std::map<std::string, std::string> mapValues)
{
    ofstream outFile;
    outFile.open(filename, ios::trunc);
    if(!outFile.is_open())
    {
        cerr << " -> Error opening file "<< filename <<".\n";
        outFile.close();
        exit(0);
    }
    cout << "[Analyzer] Saving values to file: " << filename << "\n";
    
    for(map<string,string>::iterator it= mapValues.begin(); it!= mapValues.end();it++)
    {
        outFile << it->first << endl;
        outFile << it->second << endl;
    }    outFile.close();
    
}

//read map values from a file
std::map<std::string, std::string> util::loadVarValuesFromFile(std::string filename)
{
    map<string, string> mapValues;
    string first_var;
    string second_value;
    ifstream inSol(filename);
    string lineSol;
    while (getline(inSol, lineSol))
    {
        first_var = lineSol;
        getline(inSol, lineSol);
        second_value = lineSol;
        mapValues.insert(pair<string,string>(first_var,second_value));
    }
    inSol.close();
    return mapValues;}



//add 3x(thread_ID) to a better Pretty Print
string util::threadTabsPP(int tab)
{
    string str = "";
    for(int j = 0; j < tab ; j++) //print the number of tabs
    {
        str = str+"\t\t\t";
    }
    return str;
}


//fill ScheduleOrd
void util::fillScheduleOrd(string tid, map<string,vector<Operation*>>* op_list, Schedule* sch)
{
    //get the head
    sch->push_back((*op_list)[tid][0]);
    
    //remove the head
    vector<Operation*>::iterator it_erase = (*op_list)[tid].begin();
    (*op_list)[tid].erase(it_erase);
}


//transforms a int into a string
string util::stringValueOf(int i)
{
    stringstream ss;
    ss << i;
    return ss.str();
}

//transforms a string into an int
int util::intValueOf(std::string i)
{
    int res;
    stringstream ss(i);
    ss >> res;
    
    return res;
}

//prints the error flags when an attempt to open a file fails
void util::print_state (const std::ios& stream) {
    cerr << " good()=" << stream.good();
    cerr << " eof()=" << stream.eof();
    cerr << " fail()=" << stream.fail();
    cerr << " bad()=" << stream.bad();
}

//similar to popen, but allows for bidirectional communication
pid_t util::popen2(const char *command, int *infp, int *outfp)
{
    int p_stdin[2], p_stdout[2];
    pid_t pid;
    
    if (pipe(p_stdin) != 0 || pipe(p_stdout) != 0)
    {
        cout << "pipe(p_stdin) = " << pipe(p_stdin) << "\tpipe(p_stdout) = " << pipe(p_stdout) << endl;
        perror(">> Error creating the pipe");
        exit(EXIT_FAILURE);
        return -1;
    }
    
    pid = fork();
    
    if (pid < 0)
        return pid;
    
    else if (pid == 0)  //child process
    {
        close(p_stdin[WRITE]);
        dup2(p_stdin[READ], READ);
        close(p_stdout[READ]);
        dup2(p_stdout[WRITE], WRITE);
        close(p_stdout[READ]);
        close(p_stdin[WRITE]);
        
        execl("/bin/sh", "sh", "-c", command, NULL);
        perror("execl");
        exit(1);
    }
    
    if (infp == NULL)
        close(p_stdin[WRITE]);
    else
        *infp = p_stdin[WRITE];
    
    if (outfp == NULL)
        close(p_stdout[READ]);
    else
        *outfp = p_stdout[READ];
    
    close(p_stdin[READ]);
    close(p_stdout[WRITE]);
    
    return pid;
    
}

//** from a path to file like a/b/c.txt, extracts the basename c.txt
string util::extractFileBasename(char* path)
{
    string name = path;
    // Remove directory if present.
    // Do this before extension removal incase directory has a period character.
    const size_t last_slash_idx = name.find_last_of("\\/");
    if (std::string::npos != last_slash_idx)
    {
        name.erase(0, last_slash_idx + 1);
    }
    
    // Remove extension if present.
    /*const size_t period_idx = filename.rfind('.');
     if (std::string::npos != period_idx)
     {
     filename.erase(period_idx);
     }*/
    
    return name;
}

//from a path to file like a/b/c.txt, extracts the basename c.txt
string util::extractFileBasename(string name)
{
    // Remove directory if present.
    // Do this before extension removal incase directory has a period character.
    const size_t last_slash_idx = name.find_last_of("\\/");
    if (std::string::npos != last_slash_idx)
    {
        name.erase(0, last_slash_idx + 1);
    }
    
    // Remove extension if present.
    /*const size_t period_idx = filename.rfind('.');
     if (std::string::npos != period_idx)
     {
     filename.erase(period_idx);
     }*/
    
    return name;
}

bool util::isClosedExpression(string expr)
{
    int numOpen = 0;
    int numClose = 0;
    
    std::size_t found = expr.find_first_of("(");
    while (found!=std::string::npos)
    {
        numOpen++;
        found=expr.find_first_of("(",found+1);
    }
    
    found = expr.find_first_of(")");
    while (found!=std::string::npos)
    {
        numClose++;
        found=expr.find_first_of(")",found+1);
    }
    
    return (numOpen == numClose);
    
}

std::string util::parseThreadId(std::string op)
{
    //for branch operations, we parse the thread id after the first "-"
    if(op.find("branch")!=string::npos){
        int posBegin = (int)op.find_first_of("-")+1;
        int posEnd = (int)op.find_first_of("-", posBegin);
        string tid = op.substr(posBegin, posEnd-posBegin);
        return op.substr(posBegin, posEnd-posBegin);
    }
    
    //for branch operations, we parse the thread id after the second "-"
    int posBegin = (int)op.find_first_of("-", op.find_first_of("-") + 1)+1;
    while(op.at(posBegin) == '>')
        posBegin = (int)op.find_first_of("-", posBegin) + 1;
    
    //for read operations, we have to consider the readId as well
    int posEnd = (int)op.find_first_of("-", posBegin);
    int posEnd2 = (int)op.find_first_of("&", posBegin);
    if(posEnd != string::npos)
        return op.substr(posBegin, posEnd-posBegin);
    
    return op.substr(posBegin, posEnd2-posBegin);
    
}

/*
 //returne ThreadID from a string
 string util::getTid(std::string op)
 {
 size_t init, end;
 end = op.find_last_of("-");
 init = op.find_last_of("-",end-1)+1;
 string tid = op.substr(init,end-init);
 
 //account for operations without variable id
 if(tid.length() > 3)
 {
 end = op.find_first_of("&");
 init = op.find_last_of("-")+1;
 tid = op.substr(init,end-init);
 }
 
 return tid;
 }*/

std::string util::parseVar(std::string op)
{
    size_t init, end;
    string var = "";
    
    init = op.find_last_of("_")+1;
    end = op.find_first_of("-",init);
    var = op.substr(init,end-init);
    //cout << "1: " << var << endl;
    
    //for JPF, the address might be the same for different variables (?), so we have to parse the var name as well
    //naive way of checking whether we are in jpfmode: count the length of the var
    if(var.length() < 4)
    {
        init = op.find_first_of("-")+1;
        
        end = op.find_first_of("-",init);
        //** uncomment this in order not to parse the var id
        /*end = op.find_first_of("_",init);
         if(init == end){
         end = op.find_first_of("_",init+1);
         }*/
        
        var = op.substr(init,end-init);
    }
    //cout << "2: " << var << endl;
    return var;
}

/**
 *  Comparator to sort filenames in ascending order of their length
 *
 */
bool util::filenameComparator(string a, string b)
{
    return (a.length() < b.length());
}

bool util::subsetComparator(vector<int> a, vector<int> b)
{
    if(a.size()!=b.size()){
        return (a.size() < b.size());
    }
    else{
        int i = 0;
        while(a[i]==b[i] && (i+1 < a.size())){
            i++;
        }
        return (a[i] < b[i]);
    }
}

/**
 * Read line from pipe.
 */
string util::readLinePipe(int procR)
{
    string ret;
    char c[LINEMAX];
    read(procR,c,1);
    while(c[0]!='\n')
    {
        ret = ret + c[0];
        read(procR,c,1);
    }
    
    return ret;
}

