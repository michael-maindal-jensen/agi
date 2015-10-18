/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package io.agi.core.orm;

/**
 * Something that has a name. It is assumed (for this class to be useful) that 
 * names are unique. Therefore, object identity now rests on the name. Further,
 * the name cannot be null.
 * 
 * @author dave
 */
public class AbstractNamed< T > {
    
    protected String _name;
    public T _named;

    public AbstractNamed( String name ) {
        _name  = name;
        _named = null;
    }

    public AbstractNamed( String name, T named ) {
        _name  = name;
        _named = named;
    }

    public String getName() {
        return _name;
    }

    public void setName( String name ) {
        _name = name;
    }
    
    @Override public int hashCode() {
        return _name.hashCode();
    }

    @Override public boolean equals( Object o ) {
        if( !( o instanceof AbstractNamed ) ) {
            return false;
        }

        AbstractNamed an = (AbstractNamed)o;

        // evaluate _first:
        return _name.equals( an._name );
    }
    
}