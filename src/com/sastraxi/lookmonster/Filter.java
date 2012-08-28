package com.sastraxi.lookmonster;

public interface Filter<T> {
	public boolean accept(T item);
}
