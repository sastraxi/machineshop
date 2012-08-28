package com.sastraxi.machineshop.util;

import java.util.Collection;
import java.util.Stack;
import java.util.Vector;

public class AbsolutePath {

    protected final Stack<String> parts;
    
    public static final AbsolutePath ROOT = new AbsolutePath("/");
    
    /**
     * /     --> []         --> /
     * /abc/ --> ['abc']    --> /abc
     * /a/b  --> ['a', 'b'] --> /a/b
     */
    public AbsolutePath(String path) {
        if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
        if (path.startsWith("/")) path = path.substring(1);
        
        parts = new Stack<String>();
        for (String part: path.split("/", -1)) {
            parts.push(part);
        }        
    }
    
    public AbsolutePath(Stack<String> ip) {
        this.parts = ip;
    }
    
    /**
     * The root folder's (/) string representation will end with a trailing
     * slash; nothing else will.
     */
    @Override
    public String toString() {
        if (parts.size() == 0) return "/";
        
        StringBuilder sb = new StringBuilder("");
        for (String part: parts) {
            sb.append("/").append(part);
        }
        return sb.toString();
    }
    
    public int depth() {
        return parts.size();
    }
    
    public AbsolutePath partial(int to) {
        assert(to < depth());
        Stack<String> partialParts = new Stack<String>();
        for (int i = 0; i < to; ++i) {
            partialParts.push(parts.get(i));
        }
        return new AbsolutePath(partialParts);
    }

    public boolean isAncestorOf(AbsolutePath o) {
        if (depth() > o.depth()) return false;
        for (int i = 0; i < depth(); ++i) {
            if (!o.parts.get(i).equals(parts.get(i)))
                return false;
        }
        return true;
    }
    
    /**
     * Returns an inner path (e.g. a/b/c, no slashes at the ends)
     * such that 
     */
    public String getRelativePath(AbsolutePath ancestor) {
        assert(this.isAncestorOf(ancestor));
        String ancestorString = ancestor.toString();
        return this.toString().substring(ancestorString.length());         
    }
    
    public AbsolutePath getParent() {
        return partial(depth() - 1);
    }
    
    @SuppressWarnings("unchecked")
    public AbsolutePath getChild(String name) {
        Stack<String> child = (Stack<String>) parts.clone();
        child.add(name);
        return new AbsolutePath(child);
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (o == this) return true;
        if (o.getClass() != this.getClass()) return false;
        
        AbsolutePath other = (AbsolutePath) o;
        return (other.toString().equals(toString()));
    }
    
    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    public Vector<String> getParts() {
        return parts;
    }
    
}
