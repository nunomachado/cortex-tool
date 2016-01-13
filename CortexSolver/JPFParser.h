//
//  JPFParser.h
//  symbiosisSolver
//
//  Created by Nuno Machado on 17/09/14.
//  Copyright (c) 2014 Nuno Machado. All rights reserved.
//

#ifndef __symbiosisSolver__JPFParser__
#define __symbiosisSolver__JPFParser__

#include <iostream>

namespace jpfparser{
    
    void parseLeftRightExpr(std::string expr, std::string &l, std::string &r, std::string &o);
    std::string translateExprToZ3(std::string expr);
}

#endif /* defined(__symbiosisSolver__JPFParser__) */
