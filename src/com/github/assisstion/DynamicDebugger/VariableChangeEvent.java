package com.github.assisstion.DynamicDebugger;

public class VariableChangeEvent<T>{
	protected T oldValue;
	protected T newValue;
	protected long timeStamp;
	protected DynamicVariable<T> source;

	public VariableChangeEvent(T oldValue, T newValue, long timeStamp, DynamicVariable<T> source){
		this.oldValue = oldValue;
		this.newValue = newValue;
		this.timeStamp = timeStamp;
		this.source = source;
	}

	public T getOldValue(){
		return oldValue;
	}

	public T getNewValue(){
		return newValue;
	}

	public long getTimeStamp(){
		return timeStamp;
	}

	public DynamicVariable<T> getSource(){
		return source;
	}
}
