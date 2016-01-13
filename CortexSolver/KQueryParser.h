//
//  KQueryParser.h
//  symbiosisSolver
//
//  Created by Nuno Machado on 06/01/14.
//  Copyright (c) 2014 Nuno Machado. All rights reserved.
//

//#ifndef __snorlaxsolver__KQueryParser__
//#define __snorlaxsolver__KQueryParser__

#ifndef __symbiosisSolver__KQueryParser__
#define __symbiosisSolver__KQueryParser__



#include <iostream>
#include <string>

namespace kqueryparser{
    void parseLeftRightExpr(std::string expr, std::string &l, std::string &r);
    std::string translateExprToZ3(std::string writeVal);
}

#endif /* defined(__snorlaxsolver__KQueryParser__) */
