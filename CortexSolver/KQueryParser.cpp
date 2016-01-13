//
//  KQueryParser.cpp
//  symbiosisSolver
//
//  Created by Nuno Machado on 06/01/14.
//  Copyright (c) 2014 Nuno Machado. All rights reserved.
//

#include "KQueryParser.h"
#include "Util.h"
#include <stdlib.h>
#include <unistd.h>
#include <string>

using namespace std;

void kqueryparser::parseLeftRightExpr(string expr, string &l, string &r)
{
    string left, right;
    std::size_t init = 0;
    std::size_t end = 0;
    
    if(expr[init]!='(')
    {
        end = expr.find_first_of(' ',init);
        left = expr.substr(init, end-init);   //get the left operand value
        init = end+1;
    }
    else
    {
        end = expr.find_first_of(')',init);
        left = expr.substr(init, end-init+1); //get the left operand expression (we want the last ')')
        while(!util::isClosedExpression(left))
        {
            end = expr.find_first_of(')',end+1);
            left = expr.substr(init, end-init+1);
        }
        init = end+1;
    }
    
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
        right = expr.substr(init, end-init);   //get the right operand value
        init = end+1;
    }
    else
    {
        end = expr.find_first_of(')',init);
        right = expr.substr(init, end-init+1); //get the right operand expression (we want the last ')')
        while(!util::isClosedExpression(right))
        {
            end = expr.find_first_of(')',end+1);
            right = expr.substr(init, end-init+1);
        }
        init = end+1;
    }
    
    l.append(left);
    r.append(right);
}


string kqueryparser::translateExprToZ3(std::string expr){
    
    //in case its a number, there is no need to parse
    if(expr.front()!='(')
        return expr;
    
    string expOperator = expr.substr(1,expr.find_first_of(' ')-1);                  //expression operator
    
    //check if operator is an arithmetic operation
    if(!expOperator.compare("Add") || !expOperator.compare("Sub") || !expOperator.compare("Mul") || !expOperator.compare("UDiv")
       || !expOperator.compare("SDiv") || !expOperator.compare("URem") || !expOperator.compare("SRem")
       || !expOperator.compare("And"))
    {
        std::size_t pos = expr.find_first_of('w');
        string type = expr.substr(pos,expr.find_first_of(' ',pos)-pos); //type of form w[0-9]+
        pos = expr.find(type) + type.size() + 1;
        
        string left,right;
        string tmp = expr.substr(pos);
        parseLeftRightExpr(tmp, left, right);
        
        if(!expOperator.compare("Add"))
        {
            //handle cases where klee does subtractions using overflow tricks
            if(left.find("4294967295")!=string::npos || right.find("4294967295")!=string::npos)
            {
                string changeexpr;
                if(left.find("4294967295")!=string::npos){
                    changeexpr = ("(- "+translateExprToZ3(right)+" "+translateExprToZ3("1")+")");
                }
                else{
                    changeexpr = ("(- "+translateExprToZ3(left)+" "+translateExprToZ3("1")+")");
                }
                return changeexpr;
            }
            
            return ("(+ "+translateExprToZ3(left)+" "+translateExprToZ3(right)+")");
        }
        else if(!expOperator.compare("Sub"))
        {
            return ("(- "+translateExprToZ3(left)+" "+translateExprToZ3(right)+")");
        }
        else if(!expOperator.compare("Mul"))
        {
            return ("(* "+translateExprToZ3(left)+" "+translateExprToZ3(right)+")");
        }
        else if(!expOperator.compare("UDiv") || !expOperator.compare("SDiv"))
        {
            return ("(div "+translateExprToZ3(left)+" "+translateExprToZ3(right)+")");
        }
        else if(!expOperator.compare("URem") || !expOperator.compare("SRem"))
        {
            return ("(rem "+translateExprToZ3(left)+" "+translateExprToZ3(right)+")");
        }
        else if(!expOperator.compare("And")) //note: we are assuming the and is being used with Extract to represent (x % 2) cases
        {
            return ("(rem "+translateExprToZ3(left)+" 2)");
        }
        
    }
    //check whether the operator is a macro expression
    else if (!expOperator.compare("ReadLSB") || !expOperator.compare("ReadMSB"))
    {
        std::size_t pos = expr.find_first_of('w');
        
        string type = expr.substr(pos,expr.find_first_of(' ',pos)-pos); //type of form w[0-9]+
        pos = pos + type.size() + 1;
        string index = expr.substr(pos,expr.find_first_of(' ',pos)-pos); //index of form [0-9]+
        
        //check if index is an expression instead of a integer
        if(index.find("(")!=string::npos)
        {
            index = expr.substr(pos,expr.find(") ",pos)-pos);
        }
        pos = pos + index.size() + 1;
        string var = expr.substr(pos,expr.find_first_of(')',pos)-pos); //variable reference
        
        //erase potential spaces in the beginning of the variable
        while(var[0] == ' '){
            var = var.substr(1);
        }
        
        return ("R-"+var);
    }
    //check whether the operator is a comparison
    else if (!expOperator.compare("Eq") || !expOperator.compare("Ne") || !expOperator.compare("Ult") || !expOperator.compare("Ule")
             || !expOperator.compare("Ugt") || !expOperator.compare("Uge") || !expOperator.compare("Slt")
             || !expOperator.compare("Sgt") || !expOperator.compare("Sle") || !expOperator.compare("Sge"))
    {
        std::size_t pos = expr.find_first_not_of(' ',expOperator.size()+1);
        
        string left,right;
        string tmp = expr.substr(pos);
        parseLeftRightExpr(tmp, left, right);
        
        if(!expOperator.compare("Eq"))
        {
            return ("(= "+translateExprToZ3(left)+" "+translateExprToZ3(right)+")");
        }
        if(!expOperator.compare("Ne"))
        {
            return ("(not (= "+translateExprToZ3(left)+" "+translateExprToZ3(right)+"))");
        }
        if(!expOperator.compare("Ult") || !expOperator.compare("Slt"))
        {
            return ("(< "+translateExprToZ3(left)+" "+translateExprToZ3(right)+")");
        }
        if(!expOperator.compare("Ule") || !expOperator.compare("Sle"))
        {
            return ("(<= "+translateExprToZ3(left)+" "+translateExprToZ3(right)+")");
        }
        if(!expOperator.compare("Ugt") || !expOperator.compare("Sgt"))
        {
            return ("(> "+translateExprToZ3(left)+" "+translateExprToZ3(right)+")");
        }
        if(!expOperator.compare("Uge") || !expOperator.compare("Sge"))
        {
            return ("(>= "+translateExprToZ3(left)+" "+translateExprToZ3(right)+")");
        }
        
    }
    //check whether the operator is a bit vector -> we're not handling these...
    else if(!expOperator.compare("SExt") || !expOperator.compare("ZExt"))
    {
        std::size_t pos = expr.find_first_of('w');
        
        string type = expr.substr(pos,expr.find_first_of(' ',pos)-pos); //type of form w[0-9]+
        pos = pos + type.size() + 1;
        
        //get the input expression
        pos = expr.find_first_not_of(' ',pos);
        
        string inputexpr;
        if(expr[pos]!='(')
        {
            inputexpr = expr.substr(pos, expr.find_first_of(')',pos)-pos);   //get the left operand value
        }
        else
        {
            inputexpr = expr.substr(pos,expr.find_first_of(')',pos)-pos +1 ); //get the left operand expression (we want the last ')')
        }
        
        return translateExprToZ3(inputexpr);
    }
    else if(!expOperator.compare("Extract"))
    {
        std::size_t pos = expr.find_first_of('w');
        
        string type = expr.substr(pos,expr.find_first_of(' ',pos)-pos); //type of form w[0-9]+
        pos = pos + type.size() + 1;
        
        //get the offset
        string offset = expr.substr(pos,expr.find_first_of(' ',pos)-pos);
        pos = pos + offset.size() + 1;
        
        //get the input expression
        pos = expr.find_first_not_of(' ',pos);
        
        string inputexpr;
        if(expr[pos]!='(')
        {
            inputexpr = expr.substr(pos, expr.find_first_of(')',pos)-pos);   //get the left operand value
        }
        else
        {
            inputexpr = expr.substr(pos, expr.find_last_of(')',pos)-pos ); //get the left operand expression (we want the last ')')
        }
        
        return translateExprToZ3(inputexpr);
    }
    
   
    
    return expr;
}