package com.github.assisstion.DynamicDebugger;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import com.github.assisstion.DynamicDebugger.Debugger.DebuggerUpdatable;

class DebuggerPanel extends JPanel implements DebuggerUpdatable{

	protected Map<String, String> values;
	protected DebuggerTableModel model;
	JScrollPane scrollPane;

	public DebuggerPanel() {
		setLayout(new BorderLayout());
		values = new ConcurrentSkipListMap<String, String>();
		model = new DebuggerTableModel();
		table = new JTable(model);
		scrollPane = new JScrollPane(table);
		scrollPane.add(table);
		add(scrollPane, BorderLayout.CENTER);
	}

	private static final long serialVersionUID = 623716694098971561L;
	private JTable table;

	@Override
	public void update(Map<String, String> map){
		values.clear();
		values.putAll(map);
		EventQueue.invokeLater(() -> {
			model.fireTableDataChanged();
			scrollPane.setViewportView(table);
			table.doLayout();
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

		@Override
		public String getColumnName(int columnIndex){
			if(columnIndex == 0){
				return "Name";
			}
			if(columnIndex == 1){
				return "Value";
			}
			throw new ArrayIndexOutOfBoundsException(columnIndex);
		}

	}
}