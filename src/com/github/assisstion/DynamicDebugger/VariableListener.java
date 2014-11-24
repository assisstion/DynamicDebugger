package com.github.assisstion.DynamicDebugger;

public interface VariableListener<T>{
	//TimeStamp in ms
	void changeOccured(T oldValue, T newValue, long timeStamp);
}
