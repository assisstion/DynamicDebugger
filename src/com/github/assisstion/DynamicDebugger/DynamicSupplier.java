package com.github.assisstion.DynamicDebugger;

import java.util.function.Supplier;

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

}