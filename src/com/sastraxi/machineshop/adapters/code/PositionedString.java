package com.sastraxi.machineshop.adapters.code;


/**
 * Encodes a string that starts at a certain position
 * and goes until the end of the line.
 */
public class PositionedString {

    public String string;
    public Position position;

    public PositionedString(String string, Position position) {
        this.string = string;
        this.position = position;       
    }
    
}
