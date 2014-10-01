package minos.data.transport;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.gedr.util.tuple.Triplet;

public class TableMetaData implements Serializable {
	//=====================================================================================================
	//=                                             Constants                                             =
	//=====================================================================================================
	private static final long serialVersionUID = 1763021799637795048L;

	//=====================================================================================================
	//=                                            Attributes                                             =
	//=====================================================================================================
	private static Logger log = LoggerFactory.getLogger( TableMetaData.class );
	private String tableName = null;	
	private String[] columnNames = null;
	private BitSet columnNullable = null;
	private int[] columnTypes = null;
	private int   primaryKeyIndex = -1;

	//=====================================================================================================
	//=                                           Constructors                                            =
	//=====================================================================================================
	public TableMetaData() { }

	//=====================================================================================================
	//=                                          Getters & Setters                                        =
	//=====================================================================================================
	public String getTableName() {
		return tableName;
	}

	public void setTableName( String tableName ) {
		this.tableName = tableName;
	}

	public String[] getColumnNames() {
		return columnNames;
	}

	public void setColumnNames( String[] columnNames ) {
		this.columnNames = columnNames;
	}

	public BitSet getColumnNullable() {
		return columnNullable;
	}

	public void setColumnNullable( BitSet columnNullable ) {
		this.columnNullable = columnNullable;
	}

	public int[] getColumnTypes() {
		return columnTypes;
	}

	public void setColumnTypes( int[] columnTypes ) {
		this.columnTypes = columnTypes;
	}

	public int getPrimaryKeyIndex() {
		return primaryKeyIndex;
	}

	public void setPrimaryKeyIndex(int primaryKeyIndex) {
		this.primaryKeyIndex = primaryKeyIndex;
	}

	//=====================================================================================================
	//=                                                     Methods                                       =
	//=====================================================================================================
	/**
	 * fill meta data for table  
	 * @param con - open connection
	 * @param catalog - DB catalog name
	 * @param schema - DB schema name
	 * @param tblName - DB table name
	 * @return set primary key columns of table
	 * @throws SQLException
	 */
	public static TableMetaData build( Connection con, String catalog, String schema, 
			String tableName ) throws Exception {
		if ( ( con == null ) || ( tableName == null ) ) throw new IllegalArgumentException( "TableMetaData.build()" );		

		List<Triplet<String, Integer, Boolean>> lt = null;
		Set<String> pkeys = null;
		ResultSet rs = null;
		try {
			DatabaseMetaData dbmd = con.getMetaData();
			pkeys = getPrimaryKeys( dbmd, catalog, schema, tableName );
			rs = dbmd.getColumns( catalog, schema, tableName, null );		
			lt = new ArrayList<Triplet<String, Integer, Boolean>>();
			while ( rs.next() ) {
				Triplet<String, Integer, Boolean> t = new Triplet<>( rs.getString( 4 ), // column name
						rs.getInt( 5 ), // data type code
						( rs.getString(18).equalsIgnoreCase( "YES" ) ? Boolean.TRUE : Boolean.FALSE ) ); // column nullable
				lt.add( t );		
			}
			rs.close();
		} catch ( Exception ex ) {
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( "TableMetaData.build() : retrive DatabaseMetaData ", ex );
			throw ex;
		} finally {
			if ( rs != null )
				try {
					rs.close();
				} catch ( Exception e ) {
					if ( ( log != null ) && log.isErrorEnabled() ) log.error( "TableMetaData.build() : ResultSet close() error ", e );
				}
		}
		if ( ( lt == null ) || ( lt.size() == 0 ) ) throw new RuntimeException( "TableMetaData.build() : table [" + tableName + "] have no metadata" );
		TableMetaData tmd = new TableMetaData();
		tmd.setTableName( tableName );
		tmd.setColumnNames( new String[lt.size()] );
		tmd.setColumnTypes( new int[lt.size()] );
		tmd.setColumnNullable( new BitSet( lt.size() + 1 ) );
		tmd.getColumnNullable().clear();
		tmd.getColumnNullable().set( lt.size() );

		int ind = 0;
		for ( Triplet<String, Integer, Boolean> t : lt ) {
			tmd.getColumnNames()[ind] = t.getFirst();
			tmd.getColumnTypes()[ind] = t.getSecond();
			if ( t.getThird() ) tmd.getColumnNullable().set( ind );			
			ind++;			
		}		

		if ( ( pkeys == null ) || ( pkeys.size() != 1 ) ) 
			throw new RuntimeException( "TableMetaData.build() : table [" + tableName + "] have illegal primary key" );

		String pkname = pkeys.toArray( new String[0] )[0];

		for ( int i = 0; i < tmd.getColumnNames().length; i++ ) {
			if ( pkname.equalsIgnoreCase( tmd.getColumnNames()[i] ) ) {
				tmd.setPrimaryKeyIndex( i ); 
				break;
			}
		}
		return tmd;
	}

	/**
	 * get set of table's primary key column names  
	 * @param con - open connection
	 * @param catalog - DB catalog name
	 * @param schema - DB schema name
	 * @param tblName - DB table name
	 * @return set primary key columns of table
	 * @throws SQLException
	 */
	private static Set<String> getPrimaryKeys( DatabaseMetaData dbmd, String catalog, 
			String schema, String tblName ) throws SQLException {
		ResultSet rs = null;
		try {
			rs = dbmd.getPrimaryKeys( catalog, schema, tblName );			
			Set<String> set = new TreeSet<String>();
			while ( rs.next() ) {
				String s = rs.getString( 4 ); // 1-TABLE_CATALOG, 2-TABLE_SCHEMA, 3-TABLE_NAME, 4-COLUMN_NAME, 5-KEY_SEQ, 6-PK_NAME
				if ( s != null ) set.add( s );
			}
			return set;
		} catch ( Exception ex ) {
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( "TableMetaData.getPrimaryKeys() : Primary Key retrieve error ", ex );
			throw ex;
		} finally {
			if ( rs != null )
				try {
					rs.close();
				} catch ( Exception e ) {
					if ( ( log != null ) && log.isErrorEnabled() ) log.error( "TableMetaData.getPrimaryKeys() : ResultSet close() error ", e );
				}
		}		
	}
	
	/**
	 * make full table name as schema.table_name or catalog.table_name
	 * @param catalog - catalog name
	 * @param schema - schema name
	 * @param tblName - table name
	 * @return full table name
	 */
	public static String makeFullTableName( String catalog, String schema, String tblName ) {
		String space = " ";
		String dbSpace = ( schema != null ? schema : ( catalog != null ? catalog : space ) );
		return dbSpace.equals( space ) ? tblName : ( dbSpace + "." + tblName );
	}


	//=====================================================================================================
	//=                                          hashCode, equals & toString                              =
	//=====================================================================================================
	@Override
	public int hashCode() {
		return tableName == null ? 0 : tableName.hashCode();
	}

	@Override
	public boolean equals( Object obj ) {
		if ( this == obj ) return true;
		if ( obj == null ) return false;        
		if ( !( obj instanceof TableMetaData ) ) return false;
		TableMetaData other = ( TableMetaData) obj;
		if ( !this.tableName.equals( other.tableName ) 
				|| ( this.primaryKeyIndex != other.primaryKeyIndex ) 
				|| !columnNullable.equals( other.columnNullable ) 
				|| !Arrays.equals( this.columnTypes, other.columnTypes )
				|| !Arrays.equals( this.columnNames, other.columnNames ) ) return false;
		return true;
	}

	@Override
	public String toString() {
		return "TableMetaData: [" + tableName + " ] ";
	}
}