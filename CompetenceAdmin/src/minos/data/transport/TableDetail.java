package minos.data.transport;

import java.io.Serializable;
import java.util.List;

public class TableDetail implements Serializable {
	//=====================================================================================================
	//=                                             Constants                                             =
	//=====================================================================================================
	private static final long serialVersionUID = 1L;
	
	//=====================================================================================================
	//=                                            Attributes                                             =
	//=====================================================================================================
	private TableMetaData tableMetaData;
	private int tableCode;
	private int[] deleteKeys = null;
	private List<int[]> rows;
	
	//=====================================================================================================
	//=                                           Constructors                                            =
	//=====================================================================================================
	public TableDetail() { }

	//=====================================================================================================
	//=                                          Getters & Setters                                        =
	//=====================================================================================================
	public int getTableCode() {
		return tableCode;
	}

	public void setTableCode( int tableCode ) {
		this.tableCode = tableCode;
	}

	public List<int[]> getRows() {
		return rows;
	}

	public void setRows( List<int[]> data ) {
		this.rows = data;
	}

	public int[] getDeleteKeys() {
		return deleteKeys;
	}

	public void setDeleteKeys( int[] deleteKeys ) {
		this.deleteKeys = deleteKeys;
	}

	public TableMetaData getTableMetaData() {
		return tableMetaData;
	}

	public void setTableMetaData( TableMetaData tableMetaData ) {
		this.tableMetaData = tableMetaData;
	}
}