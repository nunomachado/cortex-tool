//
//  Trie.h
//  cortexsolver
//
//  Created by Nuno Machado on 04/05/15.
//  Copyright (c) 2015 Nuno Machado. All rights reserved.
//

#ifndef __cortexsolver__Trie__
#define __cortexsolver__Trie__

#include <stdio.h>
#include <iostream>
#include <vector>

#endif /* defined(__cortexsolver__Trie__) */

class Node {
public:
    Node() { mContent = ' '; mMarker = false; }
    ~Node() {}
    char content() { return mContent; }
    void setContent(char c) { mContent = c; }
    bool pathMarker() { return mMarker; }
    void setPathMarker() { mMarker = true; }
    Node* findChild(char c);
    void appendChild(Node* child) { mChildren.push_back(child); }
    std::vector<Node*> children() { return mChildren; }
    
private:
    char mContent;
    bool mMarker;
    std::vector<Node*> mChildren;
};

// Implements a trie of paths, where each path is a sequence of 0s and 1s indicating whether
// the false or the true branch were respectively taken for a given path condition
class Trie {
public:
    Trie();
    ~Trie();
    void addPath(std::string s);
    bool searchPath(std::string s);
    bool searchPathPrefix(std::string s);
    void deletePath(std::string s);
    std::string getPathByPrefix(std::string s, int n);
    void print();
private:
    Node* root;
};
