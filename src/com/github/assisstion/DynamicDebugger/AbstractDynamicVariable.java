package com.github.assisstion.DynamicDebugger;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public abstract class AbstractDynamicVariable<T> implements DynamicVariable<T>{

	protected List<VariableListener<? super T>> listeners =
			Collections.synchronizedList(new LinkedList<VariableListener<? super T>>());
	protected String name;

	private static ExecutorService service = new ThreadPoolExecutor(4,
			Integer.MAX_VALUE, 1000, TimeUnit.MILLISECONDS,
			new ArrayBlockingQueue<Runnable>(1024));

	public AbstractDynamicVariable(String name){
		this.name = name == null ? toString() : name;
	}

	protected static ExecutorService getExecutor(){
		return service;
	}

	@Override
	public boolean addChangeListener(VariableListener<? super T> vl){
		return listeners.add(vl);
	}

	@Override
	public boolean removeChangeListener(VariableListener<? super T> vl){
		return listeners.remove(vl);
	}

	@Override
	public void clearChangeListeners(){
		listeners.clear();
	}

	protected void fireChangeListeners(T oldValue, T newValue, long timeStamp){
		synchronized(listeners){
			ExecutorService es = getExecutor();
			for(VariableListener<? super T> vl : listeners){
				es.execute(this.new ListenerExecutionRunnable(vl,
						oldValue, newValue, timeStamp));
			}
		}
	}

	protected class ListenerExecutionRunnable implements Runnable{

		protected VariableListener<? super T> vl;
		protected T oldValue;
		protected T newValue;
		protected long timeStamp;

		public ListenerExecutionRunnable(VariableListener<? super T> vl,
				T oldValue, T newValue, long timeStamp){
			this.vl = vl;
			this.oldValue = oldValue;
			this.newValue = newValue;
			this.timeStamp = timeStamp;
		}

		@Override
		public void run(){
			vl.changeOccured(new VariableChangeEvent<T>(
					oldValue, newValue, timeStamp, AbstractDynamicVariable.this));
		}

	}

	@Override
	public String getName(){
		return name;
	}
}