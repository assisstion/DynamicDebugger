package com.github.assisstion.DynamicDebugger;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

import javax.swing.JPanel;

public class Debugger<T> implements Closeable{

	protected Object doneLock = new Object();
	protected ReadWriteLock variableLock = new ReentrantReadWriteLock();
	protected List<Supplier<? extends T>> suppliers;
	protected List<DynamicVariable<? extends T>> dynamicVariables;
	protected long delay;
	protected boolean done = false;
	protected ReadWriteLock resourceLock = new ReentrantReadWriteLock();
	protected List<DebuggerUpdatable> updatables;
	protected boolean init = false;
	protected int lastHash = -1;

	public Debugger(){
		this(100);
	}

	//In milliseconds
	public Debugger(long updateDelay){
		suppliers = new LinkedList<Supplier<? extends T>>();
		dynamicVariables = new LinkedList<DynamicVariable<? extends T>>();
		updatables = Collections.synchronizedList(new LinkedList<DebuggerUpdatable>());
		delay = updateDelay;
	}

	/*
	 * Supplier pattern syntax
	 * If the supplier returns an object with a String representation that begins
	 * with "%$" and the String representation of the object contains an "=", then
	 * the String representation is split by its first "=" and the value before becomes
	 * the key of the map, while the value after becomes the value of the map.
	 * This only applies for suppliers (and not DynamicVariables, for ease of usage).
	 * e.g. a value of:
	 * %$counter=3
	 * would give the key of "counter" and the value of "3"
	 */
	public boolean attachSupplier(Supplier<? extends T> supplier){
		Lock lock = variableLock.writeLock();
		lock.lock();
		boolean b;
		try{
			b = suppliers.add(supplier);
		}
		finally{
			lock.unlock();
		}
		return b;
	}

	public static Supplier<String> formatSupplier(Supplier<?> key, Supplier<?> value){
		return () -> "%$" + key.get().toString() + "=" + value.get().toString();
	}

	public boolean attach(DynamicVariable<? extends T> dynamicVariable){
		Lock lock = variableLock.writeLock();
		lock.lock();
		boolean b;
		try{
			b = dynamicVariables.add(dynamicVariable);
		}
		finally{
			lock.unlock();
		}
		return b;
	}

	public boolean attachAllSuppliers(Collection<? extends Supplier<? extends T>> supplier){
		Lock lock = variableLock.writeLock();
		lock.lock();
		boolean b;
		try{
			b = suppliers.addAll(supplier);
		}
		finally{
			lock.unlock();
		}
		return b;
	}

	public boolean attachAll(Collection<? extends
			DynamicVariable<? extends T>> dynamicVariable){
		Lock lock = variableLock.writeLock();
		lock.lock();
		boolean b;
		try{
			b = dynamicVariables.addAll(dynamicVariable);
		}
		finally{
			lock.unlock();
		}
		return b;
	}

	public boolean unattachSupplier(Supplier<? extends T> supplier){
		Lock lock = variableLock.writeLock();
		lock.lock();
		boolean b;
		try{
			b = suppliers.remove(supplier);
		}
		finally{
			lock.unlock();
		}
		return b;
	}

	public boolean unattach(DynamicVariable<? extends T> dynamicVariable){
		Lock lock = variableLock.writeLock();
		lock.lock();
		boolean b;
		try{
			b = dynamicVariables.remove(dynamicVariable);
		}
		finally{
			lock.unlock();
		}
		return b;
	}

	public boolean unattachAllSuppliers(Collection<? extends
			Supplier<? extends T>> supplier){
		Lock lock = variableLock.writeLock();
		lock.lock();
		boolean b;
		try{
			b = suppliers.removeAll(supplier);
		}
		finally{
			lock.unlock();
		}
		return b;
	}

	public boolean unattachAll(Collection<? extends DynamicVariable<
			? extends T>> dynamicVariable){
		Lock lock = variableLock.writeLock();
		lock.lock();
		boolean b;
		try{
			b = dynamicVariables.removeAll(dynamicVariable);
		}
		finally{
			lock.unlock();
		}
		return b;
	}

	public void clearVariables(){
		Lock lock = variableLock.writeLock();
		try{
			dynamicVariables.clear();
		}
		finally{
			lock.unlock();
		}
	}

	public void clearSuppliers(){
		Lock lock = variableLock.writeLock();
		try{
			suppliers.clear();
		}
		finally{
			lock.unlock();
		}
	}

	public void clear(){
		Lock lock = variableLock.writeLock();
		try{
			clearVariables();
			clearSuppliers();
		}
		finally{
			lock.unlock();
		}
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
		try{
			done = true;
			synchronized(doneLock){
				doneLock.notifyAll();
			}
		}
		finally{
			writeLock.unlock();
		}
	}

	protected class DebuggerUpdater implements Runnable{

		protected Timer timer;

		@Override
		public void run(){
			timer = new Timer();
			timer.scheduleAtFixedRate(new DebugUpdaterTask(), 0, delay);
			synchronized(doneLock){
				while(!done){
					try{
						doneLock.wait();
					}
					catch(InterruptedException e){
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			timer.cancel();
		}

		protected class DebugUpdaterTask extends TimerTask{

			@Override
			public void run(){
				Lock vrLock = variableLock.readLock();
				vrLock.lock();
				try{
					//lazy hash code checking

					//can fail to update in case of collisions
					int supplierHash = 0;
					for(Supplier<? extends T> supplier : suppliers){
						T t = supplier.get();
						supplierHash = 31*supplierHash + (t == null ? 0 : t.hashCode());
					}
					int varHash = 0;
					for(DynamicVariable<? extends T> var : dynamicVariables){
						T t = var.get();
						varHash = 31*varHash + (t == null ? 0 : t.hashCode());
					}
					int hash = varHash ^ supplierHash;
					if(hash == lastHash){
						return;
					}
					else{
						lastHash = hash;
					}
				}
				finally{
					vrLock.unlock();
				}
				synchronized(updatables){
					ExecutorService executor = DynamicVariableHolder.getExecutor();
					for(DebuggerUpdatable updater : updatables){
						executor.execute(DebuggerUpdater.this.new
								DebuggerUpdateDispatcher(updater));
					}
				}
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
					timer.cancel();
					return;
				}
				try{
					Map<String, String> map = new HashMap<String, String>();
					Lock vrLock = variableLock.readLock();
					vrLock.lock();
					try{
						for(DynamicVariable<? extends T> dv : dynamicVariables){
							map.put(dv.getName(), dv.get().toString());
						}
						for(Supplier<? extends T> dv : suppliers){
							String key = dv.toString();
							String value = dv.get().toString();

							if(value.startsWith("%$")){
								if(value.endsWith("=")){
									key = value.substring(0, value.length() - 1);
									value = "";
								}
								else{
									int i = value.indexOf("=");
									if(i != -1){
										key = value.substring(2, i);
										value = value.substring(i+1);
									}
								}
							}
							map.put(key, value);
						}
					}
					finally{
						vrLock.unlock();
					}
					updatable.update(map);
				}
				finally{
					readLock.unlock();
				}
			}

		}

	}

	protected interface DebuggerUpdatable{
		void update(Map<String, String> table);
	}

	public boolean isDone(){
		return done;
	}

	public boolean isInit(){
		return init;
	}
}
