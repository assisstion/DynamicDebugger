package com.github.assisstion.DynamicDebugger;

import java.awt.EventQueue;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import com.github.assisstion.DynamicDebugger.Debugger.DebuggerUpdatable;

class DebuggerPanel extends JPanel implements DebuggerUpdatable{

	protected Map<String, String> values;

	public DebuggerPanel() {
		values = new ConcurrentSkipListMap<String, String>();
		table = new JTable(new DebuggerTableModel());
		add(table);
	}

	private static final long serialVersionUID = 623716694098971561L;
	private JTable table;

	@Override
	public void update(Map<String, String> table){
		values.clear();
		values.putAll(table);
		EventQueue.invokeLater(() -> {
			revalidate();
			repaint();
		});
	}


	protected class DebuggerTableModel extends AbstractTableModel{

		private static final long serialVersionUID = 679094557703144693L;

		@Override
		public int getRowCount(){
			return values.size();
		}

		@Override
		public int getColumnCount(){
			return 2;
		}

		@Override
		public String getValueAt(int rowIndex, int columnIndex){
			if(columnIndex == 0){
				int counter = 0;
				for(String s : values.keySet()){
					if(counter++ == rowIndex){
						return s;
					}
				}
			}
			if(columnIndex == 1){
				int counter = 0;
				for(String s : values.values()){
					if(counter++ == rowIndex){
						return s;
					}
				}
			}
			throw new ArrayIndexOutOfBoundsException(rowIndex + ", " + columnIndex);
		}


	}
}