package com.github.assisstion.DynamicDebugger;

import java.util.function.Supplier;

//Does not follow change listener rules
//DynamicVariableHolder recommended over this
public class DynamicSupplier<T> extends AbstractDynamicVariable<T>{

	protected Supplier<? extends T> supplier;

	public DynamicSupplier(String name, Supplier<? extends T> supplier){
		super(name);
		this.supplier = supplier;
	}

	@Override
	public T get(){
		return supplier.get();
	}

	//Manually fire changed values
	public void fireValueChanged(T oldValue, T newValue, long timeStamp){
		fireChangeListeners(oldValue, newValue, timeStamp);
	}

}