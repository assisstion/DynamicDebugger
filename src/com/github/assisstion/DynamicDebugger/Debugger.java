package com.github.assisstion.DynamicDebugger;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

import javax.swing.JPanel;

public class Debugger<T> implements Closeable{

	protected ReadWriteLock variableLock = new ReentrantReadWriteLock();
	protected List<Supplier<? extends T>> suppliers;
	protected List<DynamicVariable<? extends T>> dynamicVariables;
	protected long delay;
	protected boolean done = false;
	protected ReadWriteLock resourceLock = new ReentrantReadWriteLock();
	protected List<DebuggerUpdatable> updatables;
	protected boolean init;

	public Debugger(){
		this(10);
	}

	//In milliseconds
	public Debugger(long updateDelay){
		suppliers = new LinkedList<Supplier<? extends T>>();
		dynamicVariables = new LinkedList<DynamicVariable<? extends T>>();
		updatables = Collections.synchronizedList(new LinkedList<DebuggerUpdatable>());
		delay = updateDelay;
	}

	public boolean attachSupplier(Supplier<? extends T> supplier){
		Lock lock = variableLock.writeLock();
		lock.lock();
		boolean b = suppliers.add(supplier);
		lock.unlock();
		return b;
	}

	public boolean attach(DynamicVariable<? extends T> dynamicVariable){
		Lock lock = variableLock.writeLock();
		lock.lock();
		boolean b = dynamicVariables.add(dynamicVariable);
		lock.unlock();
		return b;
	}

	public boolean attachAllSuppliers(Collection<? extends Supplier<? extends T>> supplier){
		Lock lock = variableLock.writeLock();
		lock.lock();
		boolean b = suppliers.addAll(supplier);
		lock.unlock();
		return b;
	}

	public boolean attachAll(Collection<? extends
			DynamicVariable<? extends T>> dynamicVariable){
		Lock lock = variableLock.writeLock();
		lock.lock();
		boolean b = dynamicVariables.addAll(dynamicVariable);
		lock.unlock();
		return b;
	}

	public boolean unattachSupplier(Supplier<? extends T> supplier){
		Lock lock = variableLock.writeLock();
		lock.lock();
		boolean b = suppliers.remove(supplier);
		lock.unlock();
		return b;
	}

	public boolean unattach(DynamicVariable<? extends T> dynamicVariable){
		Lock lock = variableLock.writeLock();
		lock.lock();
		boolean b = dynamicVariables.remove(dynamicVariable);
		lock.unlock();
		return b;
	}

	public boolean unattachAllSuppliers(Collection<? extends
			Supplier<? extends T>> supplier){
		Lock lock = variableLock.writeLock();
		lock.lock();
		boolean b = suppliers.removeAll(supplier);
		lock.unlock();
		return b;
	}

	public boolean unattachAll(Collection<? extends DynamicVariable<
			? extends T>> dynamicVariable){
		Lock lock = variableLock.writeLock();
		lock.lock();
		boolean b = dynamicVariables.removeAll(dynamicVariable);
		lock.unlock();
		return b;
	}

	public void clearVariables(){
		Lock lock = variableLock.writeLock();
		dynamicVariables.clear();
		lock.unlock();
	}

	public void clearSuppliers(){
		Lock lock = variableLock.writeLock();
		suppliers.clear();
		lock.unlock();
	}

	public void clear(){
		Lock lock = variableLock.writeLock();
		clearVariables();
		clearSuppliers();
		lock.unlock();
	}

	public synchronized JPanel init(){
		if(init){
			return null;
		}
		init = true;
		new Thread(this.new DebuggerUpdater()).start();
		DebuggerPanel panel = new DebuggerPanel();
		updatables.add(panel);
		return panel;
	}

	@Override
	public void close() throws IOException{
		Lock writeLock = resourceLock.writeLock();
		writeLock.lock();
		done = true;
		writeLock.unlock();
	}

	protected class DebuggerUpdater implements Runnable{

		@Override
		public void run(){
			while(!done){
				Lock readLock = resourceLock.readLock();
				if(!readLock.tryLock()){
					break;
				}
				try{
					synchronized(updatables){
						ExecutorService executor = DynamicVariableHolder.getExecutor();
						for(DebuggerUpdatable updater : updatables){
							executor.execute(this.new
									DebuggerUpdateDispatcher(updater));
						}
					}
					Thread.sleep(10);
				}
				catch(InterruptedException e){
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				readLock.unlock();
			}
		}

		protected class DebuggerUpdateDispatcher implements Runnable{

			protected DebuggerUpdatable updatable;

			public DebuggerUpdateDispatcher(DebuggerUpdatable updatable){
				this.updatable = updatable;
			}

			@Override
			public void run(){
				Lock readLock = resourceLock.readLock();
				if(!readLock.tryLock()){
					return;
				}
				Map<String, String> map = new HashMap<String, String>();
				Lock vrLock = variableLock.readLock();
				vrLock.lock();
				for(DynamicVariable<? extends T> dv : dynamicVariables){
					map.put(dv.getName(), dv.get().toString());
				}
				for(Supplier<? extends T> dv : suppliers){
					map.put(dv.toString(), dv.get().toString());
				}
				vrLock.unlock();
				updatable.update(map);
				readLock.unlock();
			}

		}

	}

	protected interface DebuggerUpdatable{
		void update(Map<String, String> table);
	}
}
