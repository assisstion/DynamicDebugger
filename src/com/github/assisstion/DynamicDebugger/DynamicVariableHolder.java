package com.github.assisstion.DynamicDebugger;


public class DynamicVariableHolder<T> extends AbstractDynamicVariable<T>{

	protected T value;

	public DynamicVariableHolder(){
		this(null, null);
	}

	public DynamicVariableHolder(T t){
		this(null, t);
	}

	public DynamicVariableHolder(String name){
		this(name, null);
	}

	public DynamicVariableHolder(String name, T t){
		super(name);
		value = t;
	}

	public synchronized void set(T t){
		long time = System.currentTimeMillis();
		T old = value;
		value = t;
		fireChangeListeners(old, t, time);
	}

	@Override
	public synchronized T get(){
		return value;
	}

}
