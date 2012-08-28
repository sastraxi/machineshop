package com.sastraxi.lookmonster;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Observable;

import org.simpleframework.xml.ElementList;

public class SmartList<T> extends Observable implements List<T> {

    @ElementList
	List<T> delegate;

	public SmartList(List<T> delegate) {
		this.delegate = delegate;
	}

	public SmartList() {
		this.delegate = new ArrayList<T>();
	}

	@Override
	public String toString() {
		return delegate.toString();
	}

	public boolean add(T object) {
		boolean result = delegate.add(object);
		if (result) {
			setChanged();
		}
		notifyObservers();
		return result;
	}

	public void add(int position, T object) {
		delegate.add(position, object);
		setChanged();
		notifyObservers();
	}

	public boolean addAll(Collection<? extends T> c) {
		boolean result = delegate.addAll(c);
		if (result) {
			setChanged();
		}
		notifyObservers();
		return result;
	}

	public boolean addAll(int position, Collection<? extends T> c) {
		boolean result = delegate.addAll(position, c);
		if (result) {
			setChanged();
		}
		notifyObservers();
		return result;
	}

	public void clear() {
		delegate.clear();
		setChanged();
		notifyObservers();
	}

	public boolean contains(Object object) {
		return delegate.contains(object);
	}

	public boolean containsAll(Collection<?> c) {
		return delegate.containsAll(c);
	}

	public T get(int position) {
		return delegate.get(position);
	}

	public int indexOf(Object object) {
		return delegate.indexOf(object);
	}

	@Override
	public boolean equals(Object object) {
		return delegate.equals(object);
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

	public boolean isEmpty() {
		return delegate.isEmpty();
	}

	public Iterator<T> iterator() {
		return delegate.iterator();
	}

	public int lastIndexOf(Object object) {
		return delegate.lastIndexOf(object);
	}

	public ListIterator<T> listIterator() {
		return delegate.listIterator();
	}

	public ListIterator<T> listIterator(int position) {
		return delegate.listIterator(position);
	}

	public T remove(int position) {
		T result = delegate.remove(position);
		setChanged();
		notifyObservers();
		return result;
	}

	public boolean remove(Object object) {
		boolean result = delegate.remove(object);
		if (result) {
			setChanged();
		}
		notifyObservers();
		return result;
	}

	public boolean removeAll(Collection<?> c) {
		boolean result = delegate.removeAll(c);
		if (result) {
			setChanged();
		}
		notifyObservers();
		return result;
	}

	public boolean retainAll(Collection<?> c) {
		boolean result = delegate.retainAll(c);
		if (result) {
			setChanged();
		}
		notifyObservers();
		return result;
	}

	public T set(int position, T object) {
		T result = delegate.set(position, object);
		setChanged();
		notifyObservers();
		return result;
	}

	public int size() {
		return delegate.size();
	}

	public List<T> subList(int fromPosition, int toPosition) {
		return delegate.subList(fromPosition, toPosition);
	}

	public Object[] toArray() {
		return delegate.toArray();
	}

	@SuppressWarnings("hiding")
	public <T> T[] toArray(T[] array) {
		return delegate.toArray(array);
	}

}