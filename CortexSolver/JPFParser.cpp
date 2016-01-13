//
//  JPFParser.cpp
//  symbiosisSolver
//
//  Created by Nuno Machado on 17/09/14.
//  Copyright (c) 2014 Nuno Machado. All rights reserved.
//

#include "JPFParser.h"
#include "Util.h"
#include <stdlib.h>
#include <unistd.h>
#include <string>

using namespace std;

void jpfparser::parseLeftRightExpr(string expr, string &l, string &r, string &o)
{
    string left, right, op;
    std::size_t init = 0;
    std::size_t end = 0;
    
    if(expr[init]!='(')
    {
        end = expr.find_first_of(' ',init);
        left = expr.substr(init, end-init);   //get left expression
        init = end+1;
    }
    else
    {
        end = expr.find_first_of(')',init);
        left = expr.substr(init, end-init+1); //get left expression (we want the last ')')
        while(!util::isClosedExpression(left))
        {
            end = expr.find_first_of(')',end+1);
            left = expr.substr(init, end-init+1);
        }
        init = end+1;
    }
    
    init = expr.find_first_not_of(' ',init);
    end = expr.find_first_of(' ',init);
    op = expr.substr(init, end-init); //get operator value
    
    init = end; //move init after operand
    init = expr.find_first_not_of(" )",init);
    if(expr[init]!='(')
    {
        if(expr.find_first_of(' ',init)!=string::npos)
        {
            end = expr.find_first_of(' ',init);
        }
        else if(expr.find_first_of(')',init)!=string::npos)
        {
            end = expr.find_first_of(')',init);
        }
        else
            end = expr.size();
        right = expr.substr(init, end-init);   //get right expression
        init = end+1;
    }
    else
    {
        end = expr.find_first_of(')',init);
        right = expr.substr(init, end-init+1); //get right expression (we want the last ')')
        while(!util::isClosedExpression(right))
        {
            end = expr.find_first_of(')',end+1);
            right = expr.substr(init, end-init+1);
        }
        init = end+1;
    }
    
    
    //handle constants of format CONST_val
    if(left.find("CONST_")!=string::npos)
    {
        left = left.replace(left.find("CONST_"),6,"");//left.substr(6,left.size()-6);
    }
    if(right.find("CONST_")!=string::npos)
    {
        right = right.replace(right.find("CONST_"),6,"");//right.substr(6,right.size()-6);
    }
    
    l.append(left);
    r.append(right);
    o.append(op);
}

string jpfparser::translateExprToZ3(std::string expr){

    //in case its a number, there is no need to parse
    if(expr.find(' ')==string::npos)
        return expr;
    
    string left,right,expOperator; //left expression, right expression, operator
    
    if(expr.front()=='('){
        expr = expr.substr(1,expr.find_last_of(")")-1); //remove first parenthesis
    }
    parseLeftRightExpr(expr, left, right, expOperator);
    
    
    //check if operator is an arithmetic operation
    if(!expOperator.compare("+") || !expOperator.compare("-") || !expOperator.compare("*") || !expOperator.compare("/")
       || !expOperator.compare("%"))
    {
        if(!expOperator.compare("+"))
        {
            return ("(+ "+translateExprToZ3(left)+" "+translateExprToZ3(right)+")");
        }
        else if(!expOperator.compare("-"))
        {
            return ("(- "+translateExprToZ3(left)+" "+translateExprToZ3(right)+")");
        }
        else if(!expOperator.compare("*"))
        {
            return ("(* "+translateExprToZ3(left)+" "+translateExprToZ3(right)+")");
        }
        else if(!expOperator.compare("/"))
        {
            return ("(div "+translateExprToZ3(left)+" "+translateExprToZ3(right)+")");
        }
        else if(!expOperator.compare("%"))
        {
            return ("(mod "+translateExprToZ3(left)+" "+translateExprToZ3(right)+")");
        }
    }
    //check whether the operator is a comparison
    else if (!expOperator.compare("==") || !expOperator.compare("!=") || !expOperator.compare("<") || !expOperator.compare("<=")
             || !expOperator.compare(">") || !expOperator.compare(">="))
    {
        if(!expOperator.compare("=="))
        {
            return ("(= "+translateExprToZ3(left)+" "+translateExprToZ3(right)+")");
        }
        if(!expOperator.compare("!="))
        {
            return ("(not (= "+translateExprToZ3(left)+" "+translateExprToZ3(right)+"))");
        }
        if(!expOperator.compare("<"))
        {
            return ("(< "+translateExprToZ3(left)+" "+translateExprToZ3(right)+")");
        }
        if(!expOperator.compare("<="))
        {
            return ("(<= "+translateExprToZ3(left)+" "+translateExprToZ3(right)+")");
        }
        if(!expOperator.compare(">"))
        {
            return ("(> "+translateExprToZ3(left)+" "+translateExprToZ3(right)+")");
        }
        if(!expOperator.compare(">="))
        {
            return ("(>= "+translateExprToZ3(left)+" "+translateExprToZ3(right)+")");
        }
        
    }
    
    return expr;
    
}