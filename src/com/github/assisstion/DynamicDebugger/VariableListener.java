package com.github.assisstion.DynamicDebugger;

public interface VariableListener<T>{
	void changeOccured(VariableChangeEvent<? extends T> e);
}
