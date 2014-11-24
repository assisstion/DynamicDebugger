package com.github.assisstion.DynamicDebugger;

import java.util.function.Supplier;

public interface DynamicVariable<T> extends Supplier<T>{
	boolean addChangeListener(VariableListener<? super T> vl);
	boolean removeChangeListener(VariableListener<? super T> vl);
	void clearChangeListeners();
	String getName();
	@Override
	T get();
}
