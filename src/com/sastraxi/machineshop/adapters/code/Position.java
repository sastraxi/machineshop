package com.sastraxi.machineshop.adapters.code;

/**
 * Encapsulates a line/character combo.
 */
public class Position implements Comparable<Position> {
    public int character;
    public int line;
    
    public Position(int line, int character) {
        this.line = line;
        this.character = character;             
    }
    
    public int compareTo(Position another) {
        if (line < another.line) return -1;
        if (line > another.line) return 1;
        if (character < another.character) return -1;
        if (character > another.character) return 1;
        return 0;
    }
    
    @Override
    public String toString() {
        return (this.line+1) + ":" + this.character;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (!(o instanceof Position)) return false;
        Position other = (Position) o;
        return (other.line == this.line && other.character == this.character);
    }

    public Position withCharacter(int character) {
        return new Position(line, character);
    }         
    
    public Position withLine(int line) {
        return new Position(line, character);
    }

    public Position clone() {
        return new Position(line, character);
    }
    
}
