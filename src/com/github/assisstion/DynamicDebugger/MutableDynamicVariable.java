package com.github.assisstion.DynamicDebugger;

public interface MutableDynamicVariable<T> extends DynamicVariable<T>{
	void set(T t);
}
