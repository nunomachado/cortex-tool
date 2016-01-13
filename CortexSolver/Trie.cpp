//
//  Trie.cpp
//  cortexsolver
//
//  Created by Nuno Machado on 04/05/15.
//  Copyright (c) 2015 Nuno Machado. All rights reserved.
//

#include "Trie.h"
#include <stack>
#include <queue>
#include <map>
using namespace std;

Node* Node::findChild(char c)
{
    for ( int i = 0; i < mChildren.size(); i++ )
    {
        Node* tmp = mChildren.at(i);
        if ( tmp->content() == c )
        {
            return tmp;
        }
    }
    
    return NULL;
}

Trie::Trie()
{
    root = new Node();
}

Trie::~Trie()
{
    // Free memory
}

void Trie::addPath(string s)
{
    Node* current = root;
    
    if ( s.length() == 0 )
    {
        current->setPathMarker(); // an empty path
        return;
    }
    
    for ( int i = 0; i < s.length(); i++ )
    {
        Node* child = current->findChild(s[i]);
        if ( child != NULL )
        {
            current = child;
        }
        else
        {
            Node* tmp = new Node();
            tmp->setContent(s[i]);
            current->appendChild(tmp);
            current = tmp;
        }
        if ( i == s.length() - 1 )
            current->setPathMarker();
    }
}


bool Trie::searchPath(string s)
{
    Node* current = root;
    
    while ( current != NULL )
    {
        for ( int i = 0; i < s.length(); i++ )
        {
            Node* tmp = current->findChild(s[i]);
            if ( tmp == NULL )
                return false;
            current = tmp;
        }
        
        if ( current->pathMarker() )
            return true;
        else
            return false;
    }
    
    return false;
}

bool Trie::searchPathPrefix(string s)
{
    Node* current = root;
    
    while ( current != NULL )
    {
        for ( int i = 0; i < s.length(); i++ )
        {
            Node* tmp = current->findChild(s[i]);
            if ( tmp == NULL )
                return false;
            current = tmp;
        }

        return true;
    }
    
    return false;
}

/*
 * Return the n-th shortest path for a given prefix (uses BFS).
 */
string Trie::getPathByPrefix(string s, int n)
{
    Node* current = root;
    string ret = "";
    int tmpN = 0; //used to find the n-th shortest path
    
    for ( int i = 0; i < s.length(); i++ )
    {
        Node* tmp = current->findChild(s[i]);
        
        if(tmp==NULL){
            break;
        }
        ret += tmp->content();
        current = tmp;
    }
    
    //BFS to find the shortest path for this prefix
    map<Node*,Node*> parentMap;
    parentMap[current] = NULL;
    queue<Node*> q;
    q.push(current);
    
    while(!q.empty())
    {
        current = q.front();
        q.pop();
        
        if(current->pathMarker()){
            if(tmpN < n)
                tmpN++;
            else
                break;
        }
        
        for ( int i = 0; i < current->children().size(); i++ )
        {
            q.push(current->children().at(i));
            parentMap[current->children().at(i)] = current;
        }
    }
    
    string tmp = "";
    while(parentMap[current]!=NULL){
        tmp += current->content();
        current = parentMap[current];
    }
    
    for (string::reverse_iterator rit=tmp.rbegin(); rit!=tmp.rend(); ++rit)
        ret += *rit;
    
    
    return ret;

}

//print the Trie using DFS
void Trie::print()
{
    Node* current = root;
    stack<Node*> s;
    s.push(current);
    
    while(!s.empty())
    {
        current = s.top();
        s.pop();
        cout << current->content() <<".";
        if(current->pathMarker())
            cout << endl;
        
        for ( int i = 0; i < current->children().size(); i++ )
        {
            s.push(current->children().at(i));
        }
    }
}
