package com.github.assisstion.DynamicDebugger;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.AbstractTableModel;

import com.github.assisstion.DynamicDebugger.Debugger.DebuggerUpdatable;

class DebuggerPanel extends JPanel implements DebuggerUpdatable{

	protected DebugInformationReceiver dir;
	protected Map<String, String> values;
	protected DebuggerTableModel model;
	protected JScrollPane scrollPane;

	public DebuggerPanel(DebugInformationReceiver dir) {
		this.dir = dir;
		setLayout(new BorderLayout());
		values = new ConcurrentSkipListMap<String, String>();
		model = new DebuggerTableModel();
		table = new JTable(model);
		scrollPane = new JScrollPane(table);
		scrollPane.add(table);
		add(scrollPane, BorderLayout.CENTER);

		JPanel panel = new JPanel();
		add(panel, BorderLayout.SOUTH);

		JLabel lblExecution = new JLabel("Execution: ");
		panel.add(lblExecution);

		executionState = new JLabel("Running");
		panel.add(executionState);

		btnResume = new JButton("Resume");
		btnResume.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String s = textField.getText();
				int i;
				if(s.length() == 0){
					if(executionState.getText().equals("Paused") ||
							dir.getSkipCount() < 0){
						i = 0;
					}
					else{
						i = 1;
					}
				}
				else{
					try{
						i = Integer.parseInt(s);
					}
					catch(NumberFormatException nfe){
						i = 0;
					}
				}
				dir.resumeExecution(i);
			}
		});
		panel.add(btnResume);

		lblSkipPauses = new JLabel("Skip Pauses:");
		panel.add(lblSkipPauses);

		textField = new JTextField();
		panel.add(textField);
		textField.setColumns(2);
	}

	private static final long serialVersionUID = 623716694098971561L;
	private JTable table;
	private JTextField textField;
	private JLabel executionState;
	private JButton btnResume;
	private JLabel lblSkipPauses;

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

	@Override
	public void setExecutionState(String state){
		EventQueue.invokeLater(() -> {
			executionState.setText(state);
			if(state.equals("Paused")){
				btnResume.setText("Resume");
			}
			else if(state.equals("Running")){
				btnResume.setText("Skip");
			}
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