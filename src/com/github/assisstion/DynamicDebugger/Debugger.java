package com.github.assisstion.DynamicDebugger;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class Debugger<T> implements Closeable, DebugInformationReceiver, VariableListener<T>{

	protected Object doneLock = new Object();
	protected ReadWriteLock variableLock = new ReentrantReadWriteLock();
	protected Set<Supplier<? extends T>> suppliers;
	protected Set<DynamicVariable<? extends T>> dynamicVariables;
	protected long delay;
	protected boolean done = false;
	protected ReadWriteLock resourceLock = new ReentrantReadWriteLock();
	protected Set<DebuggerUpdatable> updatables;
	protected boolean init = false;
	protected int lastHash = -1;

	public Debugger(){
		this(100);
	}

	//In milliseconds
	public Debugger(long updateDelay){
		suppliers = new HashSet<Supplier<? extends T>>();
		dynamicVariables = new HashSet<DynamicVariable<? extends T>>();
		updatables = Collections.synchronizedSet(new HashSet<DebuggerUpdatable>());
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
			if(b){
				dynamicVariable.addChangeListener(this);
				this.fireUpdate(dynamicVariable, dynamicVariable.get());
			}
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
			b = dynamicVariable.stream().map(dv -> attach(dv)).allMatch(bx -> bx);
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
			dynamicVariable.removeChangeListener(this);
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
			b = dynamicVariable.stream().map(dv -> unattach(dv)).allMatch(bx -> bx);
		}
		finally{
			lock.unlock();
		}
		return b;
	}

	public void clearVariables(){
		Lock lock = variableLock.writeLock();
		try{
			List<? extends DynamicVariable<? extends T>> dvclone = new
					ArrayList<DynamicVariable<? extends T>>(dynamicVariables);
			dvclone.forEach((dv) -> unattach(dv));
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
		DebuggerPanel panel = new DebuggerPanel(this);
		updatables.add(panel);
		return panel;
	}

	public static <T> Debugger<T> getDebugger(){
		return getDebugger(new WindowAdapter(){
			@Override
			public void windowClosed(WindowEvent e){
				System.exit(0);
			}
		});
	}

	public static <T> Debugger<T> getDebugger(WindowListener closeListener){
		Debugger<T> debugger = new Debugger<T>();
		JFrame frame = new JFrame();
		frame.setTitle("Dynamic Debugger Test");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setBounds(100, 100, 400, 300);
		frame.addWindowListener(new WindowAdapter(){
			@Override
			public void windowClosed(WindowEvent e){
				try{
					debugger.close();
					closeListener.windowClosed(e);
				}
				catch(IOException e1){
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
		frame.setLayout(new BorderLayout());
		frame.add(debugger.init(), BorderLayout.CENTER);
		frame.setVisible(true);
		return debugger;
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
			timer.schedule(new DebugUpdaterTask(), 0, delay);
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
				if(updateLast){
					updateLast = false;
				}
				else if(!update){
					return;
				}
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
					//int varHash = 0;
					//for(DynamicVariable<? extends T> var : dynamicVariables){
					//	T t = var.get();
					//	varHash = 31*varHash + (t == null ? 0 : t.hashCode());
					//}
					int hash = supplierHash;
					//int hash = varHash ^ supplierHash;
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
						//for(DynamicVariable<? extends T> dv : dynamicVariables){
						//	map.put(dv.getName(), dv.get().toString());
						//}
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
		void push(String key, String value);
		void setExecutionState(String state);
	}

	public boolean isDone(){
		return done;
	}

	public boolean isInit(){
		return init;
	}

	protected Object pauseLock = new Object();
	protected volatile boolean paused = false;
	protected volatile boolean held = false;
	protected volatile boolean update = true;
	protected volatile boolean updateLast = false;
	protected volatile int skips = 0;
	protected volatile boolean skipForever = false;

	//False if the pause has been skipped, true otherwise;
	//If hold, stop updating
	public boolean pause(boolean hold){
		synchronized(pauseLock){
			if(skipForever){
				return false;
			}
			if(skips > 0){
				skips--;
				return false;
			}
			paused = true;
			if(hold){
				held = true;
				update = false;
				updateLast = true;
			}
			synchronized(updatables){
				for(DebuggerUpdatable du : updatables){
					du.setExecutionState("Paused");
				}
			}
			while(paused){
				try{
					pauseLock.wait();
				}
				catch(InterruptedException e){
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return true;
		}
	}

	public void setUpdate(boolean update){
		synchronized(pauseLock){
			this.update = update;
			if(held && update){
				held = false;
			}
		}
	}

	public void resume(){
		synchronized(pauseLock){
			paused = false;
			pauseLock.notifyAll();
			if(held){
				update = true;
				held = false;
			}
			synchronized(updatables){
				for(DebuggerUpdatable du : updatables){
					du.setExecutionState("Running");
				}
			}
		}
	}

	public boolean isPaused(){
		return paused;
	}

	@Override
	public void resumeExecution(int count){
		synchronized(pauseLock){
			if(count < 0){
				skipForever = true;
				skips = 0;
			}
			else{
				skipForever = false;
				skips += count;
			}
			if(isPaused()){
				resume();
			}
		}
	}

	//Returns -1 on skipping forever
	@Override
	public int getSkipCount(){
		return skipForever ? -1 : skips;
	}

	@Override
	public void changeOccured(VariableChangeEvent<? extends T> e){
		Objects.requireNonNull(e);
		fireUpdate(e.getSource(), e.getNewValue());
	}

	protected void fireUpdate(DynamicVariable<? extends T> source, T newValue){
		//Depends on updateLast to be updated by traditional updating methods
		if(!updateLast && !update){
			return;
		}
		synchronized(updatables){
			ExecutorService executor = DynamicVariableHolder.getExecutor();
			String key;
			String value;
			if(newValue == null){
				value = "null";
			}
			else{
				value = newValue.toString();
			}
			String sourceName = source.getName();
			if(source == null || sourceName == null){
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
						else{
							key = "null";
						}
					}
				}
				else{
					key = "null";
				}
			}
			else{
				key = sourceName;
			}
			String finalValue = value;
			for(DebuggerUpdatable updater : updatables){
				executor.execute(() -> updater.push(
						key, finalValue));
			}
		}
	}
}

interface DebugInformationReceiver{
	void resumeExecution(int count);
	int getSkipCount();
}
