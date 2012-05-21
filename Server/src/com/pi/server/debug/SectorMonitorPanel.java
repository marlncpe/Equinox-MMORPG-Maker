package com.pi.server.debug;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import com.pi.common.database.SectorLocation;
import com.pi.server.world.SectorManager;
import com.pi.server.world.SectorManager.SectorStorage;

public class SectorMonitorPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private final SectorManager sm;
	private ThreadTableModel table_model = new ThreadTableModel();
	private JTable tbl;

	public SectorMonitorPanel(SectorManager sm) {
		this.sm = sm;
		setLocation(0, 0);
		setSize(500, 500);
		setLayout(null);
		tbl = new JTable(table_model);
		tbl.setLocation(0, 0);
		tbl.setSize(500, 500);
		tbl.setVisible(true);
		tbl.setFillsViewportHeight(true);
		add(tbl);
		setVisible(true);
	}

	private class ThreadTableModel extends AbstractTableModel {
		private static final long serialVersionUID = 1L;
		String[] colName = { "X", "Plane", "Z", "Last used", "Empty Sector",
				"Revision" };
		Class<?>[] colClass = { String.class, String.class, String.class,
				String.class };

		@Override
		public int getRowCount() {
			return sm.loadedMap().size();
		}

		@Override
		public int getColumnCount() {
			return colName.length;
		}

		@Override
		public Object getValueAt(int row, int col) {
			SectorLocation[] keyArr = sm
					.loadedMap()
					.keySet()
					.toArray(new SectorLocation[sm.loadedMap().keySet().size()]);
			if (row < keyArr.length) {
				SectorLocation key = keyArr[row];
				SectorStorage ss = sm.loadedMap().get(key);
				if (ss != null) {
					switch (col) {
					case 0:
						return key.x + "";
					case 1:
						return key.plane + "";
					case 2:
						return key.z + "";
					case 3:
						return "" + (System.currentTimeMillis() - ss.lastUsed);
					case 4:
						return ss.empty ? "True" : "False";
					case 5:
						return ss.data != null ? ss.data.getRevision() : -1;
					default:
						return "";
					}
				}
			}
			return "";
		}

		@Override
		public String getColumnName(int paramInt) {
			return colName[paramInt];
		}

		@Override
		public Class<?> getColumnClass(int paramInt) {
			return paramInt < colClass.length ? colClass[paramInt]
					: String.class;
		}

		@Override
		public boolean isCellEditable(int paramInt1, int paramInt2) {
			return false;
		}
	}
}
