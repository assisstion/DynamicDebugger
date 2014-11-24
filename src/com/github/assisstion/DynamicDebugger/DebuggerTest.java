package com.github.assisstion.DynamicDebugger;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JFrame;

public class DebuggerTest{

	protected static int time = 0;

	private static Debugger<Object> debugger;

	public static void main(String[] args){
		try{
			EventQueue.invokeAndWait(() -> {
				debugger = new Debugger<Object>();
				JFrame frame = new JFrame();
				frame.setTitle("Dynamic Debugger Test");
				frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				frame.setBounds(100, 100, 400, 300);
				frame.addWindowListener(new WindowAdapter(){
					@Override
					public void windowClosed(WindowEvent e){
						try{
							debugger.close();
							System.exit(0);
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
			});
			DynamicVariableHolder<Integer> points = new DynamicVariableHolder<Integer>(
					"points", 21);
			DynamicVariableHolder<String> name = new DynamicVariableHolder<String>(
					"name", "ping pong");
			DynamicVariableHolder<Integer> matches = new DynamicVariableHolder<Integer>(
					"matches", 3);
			debugger.attach(points);
			debugger.attach(name);
			Thread.sleep(2000);
			name.set("table tennis");
			debugger.attach(matches);
			Thread.sleep(2000);
			points.set(11);
			matches.set(3);
			debugger.attachSupplier(Debugger.formatSupplier(() -> "time", () -> time));
			while(!debugger.isDone()){
				time++;
				Thread.sleep(1000);
			}
		}
		catch(InvocationTargetException
				| InterruptedException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
