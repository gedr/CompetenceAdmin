package minos.data.transport;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

import javax.persistence.Query;

import minos.data.orm.OrmCommand;
import minos.data.orm.OrmHelper;
import minos.data.orm.OrmHelper.QueryType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import ru.gedr.util.tuple.Pair;

public class PacketLoader {
	//=====================================================================================================
	//=                                             Constants                                             =
	//=====================================================================================================
	private static final String SCHEMA = "Minos";
	private static final String JPQL_LOAD_TOP_MARKER = "SELECT l FROM Logger l "
			+ " INNER JOIN FETCH l.summary "
			+ " WHERE l.id = (SELECT MAX(l2.id) FROM Logger l2 WHERE l2.operationCode = :code) ";
	private static final String SQL_CHECK_TABLE_ID = " SELECT CONVERT(BIGINT, %s) FROM %s WHERE %s IN ( %s ) ";

	//=====================================================================================================
	//=                                            Attributes                                             =
	//=====================================================================================================
	private static Logger log = LoggerFactory.getLogger( PacketLoader.class );

	//=====================================================================================================
	//=                                           Constructors                                            =
	//=====================================================================================================
	public PacketLoader() { }
	
	//=====================================================================================================
	//=                                                                      Methods                                                      =
	//=====================================================================================================
	public static Packet loadFromBinaryStream( InputStream ism ) throws Exception {
		GZIPInputStream gz = new GZIPInputStream( ism );
		ObjectInputStream ois = new ObjectInputStream( gz );
		Packet pack = ( Packet ) ois.readObject();
		ois.close();
		gz.close();
		return pack;
	}
	
	public static Packet loadFromBinaryFile( String fileName ) throws Exception {
		FileInputStream fis = new FileInputStream( fileName );
		return loadFromBinaryStream( fis );
	}

	public static Packet loadFromJsonStream( InputStream ism ) throws Exception {
		GZIPInputStream gz = new GZIPInputStream( ism );
		InputStreamReader isr = new InputStreamReader( gz, "UTF-8" );
		Gson gson =  new Gson();		
		Packet pack = gson.fromJson( isr, Packet.class );
		isr.close();
		gz.close();
		return pack;
	}

	public static Packet loadFromJsonFile( String fileName ) throws Exception {
		FileInputStream fis = new FileInputStream( fileName );
		return loadFromJsonStream( fis );
	}
	
	/**
	 * load packet to DB
	 * @param packet
	 * @throws Exception
	 */
	public void load( final Packet packet ) throws Exception {		
		if ( packet == null )  throw new IllegalArgumentException( "PacketLoader.load() : packet is null" );

		if ( ( packet.getPreviousUuid() != null ) && !searchPrevLoadMarker( packet ) ) {
			throw new IllegalStateException( "PacketLoader.load() : cannot find previous load marker" );
		}
		if ( packet.getLog() == null ) {
			throw new IllegalStateException( "PacketLoader.load() : cannot find log rows" );
		}

		if ( ( log != null ) && log.isDebugEnabled() ) log.debug( "prepare data for load");
		final Map<Integer, BitSet> map = new TreeMap<>();
		for ( int i = 0; i < packet.getTableDetails().length; i++ ) {
			TableDetail td = packet.getTableDetails()[i];
			if ( ( td == null ) || ( td.getRows() == null ) || ( td.getRows().size() < 1 ) ) continue;
			BitSet bs = checkUpdateOrCreate( null, SCHEMA, packet, td );
			if ( ( bs == null ) || ( bs.size() < 1 ) || bs.isEmpty() ) continue;
			map.put( td.getTableCode(), bs );
		}

		OrmHelper.executeAsTransaction( new OrmCommand() {
			
			@Override
			public void execute(Object obj) throws Exception {
				deleteRows( null, SCHEMA, packet ); 
				updateAndCreateRows( null, SCHEMA, packet, map );		
				insertLogger( packet );
				// throw new IllegalStateException("edik"); //check
			}
		}, this );
	}
	
	/**
	 * search in Logger Table previous load marker
	 * @param packet - Packet object
	 * @return true if success
	 * @throws Exception
	 */
	private boolean searchPrevLoadMarker( Packet packet ) throws Exception {
		List<minos.entities.Logger> ll = OrmHelper.findByQueryWithParam( QueryType.JPQL, 
				JPQL_LOAD_TOP_MARKER, 
				minos.entities.Logger.class, 
				new Pair<Object, Object>( "code", minos.entities.Logger.OPERATION_CODE_LOADED_PACKET_MARKER ) );
		if ( ( ll == null )	|| ( ll.size() != 1 ) ) return false;
		
		UUID uuid = UUID.fromString( ll.get( 0 ).getSummary() );
		
		if ( packet.getPreviousUuid().equals( uuid ) ) return true;
		
		List<minos.entities.Logger> llg = packet.getLog();
		for ( minos.entities.Logger lg : llg ) {
			if ( ( lg.getOperationCode() == minos.entities.Logger.OPERATION_CODE_GHOST_PACKET_MARKER ) 
					&& ( UUID.fromString( lg.getSummary() ) == uuid ) ) return true;			
		}
		return false;		
	}
	
	/**
	 * delete rows from tables 
	 * @param con - open connection
	 * @param catalog - DB catalog name 
	 * @param schema - DB schema name
	 * @param packet - Packet object
	 * @throws Exception
	 */
	private void deleteRows( String catalog, String schema, Packet packet ) throws Exception {
		try {
			StringBuilder sb = new StringBuilder();

			for ( int i = 0; i < packet.getTableDetails().length; i++ ) {
				TableDetail td = packet.getTableDetails()[i];
				TableMetaData tmd = td.getTableMetaData();
				if ( ( td.getDeleteKeys() == null ) || ( td.getDeleteKeys().length <= 0 ) ) continue;
				
				if ( ( log != null ) && log.isDebugEnabled() ) log.debug( "delete Rows for " + tmd.getTableName() );
				
				int pkind = tmd.getPrimaryKeyIndex();
				sb.delete( 0, sb.length() );
				sb.append( " DELETE FROM " ).append( TableMetaData.makeFullTableName( catalog, schema, tmd.getTableName() ) )
				.append( " WHERE " ).append( tmd.getColumnNames()[pkind] ).append( " IN ( " );

				if ( ( tmd.getColumnTypes()[pkind] == java.sql.Types.INTEGER ) 
						|| ( tmd.getColumnTypes()[pkind] == java.sql.Types.TINYINT ) 
						|| ( tmd.getColumnTypes()[pkind] == java.sql.Types.SMALLINT ) ) {
					for ( int j = 0; j < td.getDeleteKeys().length; j++ ) {
						sb.append( td.getDeleteKeys()[j] ).append( j == ( td.getDeleteKeys().length - 1 ) ? " ) " : ", " );
					}
				}
				if ( tmd.getColumnTypes()[pkind] == java.sql.Types.BIGINT ) {
					for ( int j = 0; j < td.getDeleteKeys().length; j++ ) {
						Long l = packet.getLongValue( td.getDeleteKeys()[j] );
						if ( l == null ) throw new RuntimeException( "PacketLoader.deleteRows() : cannot find primary key in longs" );
						sb.append( l ).append( j == ( td.getDeleteKeys().length - 1 ) ? " ) " : ", " );
					}
				}			
				OrmHelper.executeQuery( QueryType.SQL, sb.toString() );
			}
		} catch ( Exception ex ) {
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( "PacketLoader.deleteRows() : delete rows error", ex );
		} 
	}
	
	/**
	 * update or create rows from packet data
	 * @param con - open DB connection
	 * @param catalog - DB catalog name
	 * @param schema - DB schema name
	 * @param packet - Packet object
	 * @throws Exception
	 */
	private void updateAndCreateRows( String catalog, String schema, Packet packet, 
			Map<Integer, BitSet> map ) throws Exception {
		for ( int i = 0; i < packet.getTableDetails().length; i++ ) {			
			TableDetail td = packet.getTableDetails()[i];
			if ( ( td == null ) || ( td.getRows() == null ) || ( td.getRows().size() <= 0 ) ) continue;
			
			if ( ( log != null ) && log.isDebugEnabled() ) log.debug( "load Rows for " + td.getTableMetaData().getTableName() );
			
			String sqlInsert = makeInsertRquest( catalog, schema, packet, td );
			String sqlUpdate = makeUpdateRequest( catalog, schema, packet, td );

			BitSet bs = map.get( td.getTableCode() );
			int ind = 0;
			for ( int[] row : td.getRows() ) {	
				setParametersAndExecute( ( ( ( bs != null ) && bs.get( ind++ ) ) ? sqlUpdate : sqlInsert ) , 
						packet, td, row );
			}
		}		
	}
	
	/**
	 * make UPDATE request as  UPDATE <tablen_name> SET field1 = ?1, field2 = ?2 where <pk_name> = ?<pk_index>
	 * @param catalog - DB catalog name
	 * @param schema - DB schema name
	 * @param packet - Packet object
	 * @param td - current TableDetail object
	 * @return string, containing UPDATE SQL request with parameters 
	 */
	private String makeUpdateRequest( String catalog, String schema, Packet packet, TableDetail td ) {
		TableMetaData tmd = td.getTableMetaData();
		int pkind = tmd.getPrimaryKeyIndex();
		StringBuilder sb = new StringBuilder( " UPDATE " );
		sb.append( TableMetaData.makeFullTableName( catalog, schema, tmd.getTableName() ) ).append( " SET " );
		boolean flagParam = false;
		for ( int k = 0; k < tmd.getColumnNames().length; k++ ) {
			if ( k == pkind ) continue;
			if ( flagParam ) sb.append( ", ");
			sb.append( tmd.getColumnNames()[k] ).append( " = ?" ).append( k + 1 );	
			flagParam = true;
		}
		sb.append( " WHERE " ).append( tmd.getColumnNames()[pkind] ).append( " = ?" ).append( pkind + 1 );
		return sb.toString();		
	}
	
	private void insertLogger( Packet  packet ) {
		if ( ( log != null ) && log.isDebugEnabled() ) log.debug( "load logger row" );
		for ( minos.entities.Logger l : packet.getLog() ) {
			minos.entities.Logger nl = new minos.entities.Logger( l.getMoment(), l.getExternalId(), 
					l.getOperationCode(), l.getTableCode(), l.getSummary() );
			switch ( l.getOperationCode() ) {
			case minos.entities.Logger.OPERATION_CODE_DML_CREATE :
				nl.setOperationCode( minos.entities.Logger.OPERATION_CODE_DML_CREATE_BULK );
				break;
			case minos.entities.Logger.OPERATION_CODE_DML_UPDATE :
				nl.setOperationCode( minos.entities.Logger.OPERATION_CODE_DML_UPDATE_BULK );
				break;
			case minos.entities.Logger.OPERATION_CODE_DML_DELETE :
				nl.setOperationCode( minos.entities.Logger.OPERATION_CODE_DML_DELETE_BULK );
				break;
			}
			OrmHelper.create( nl );
		}
		OrmHelper.create( new minos.entities.Logger( new Timestamp( System.currentTimeMillis() ), 0L, 
				minos.entities.Logger.OPERATION_CODE_LOADED_PACKET_MARKER, 0, 
				packet.getCurrentUuid().toString() ) );
	}
	
	/**
	 * make INSERT request as  INSERT INTO <tablen_name> (field1, field2, ...)  VALUE (?1, ?2, ...)
	 * @param catalog - DB catalog name
	 * @param schema - DB schema name
	 * @param packet - Packet object
	 * @param td - current TableDetail object
	 * @return string, containing INSERT SQL request with parameters 
	 */
	private String makeInsertRquest( String catalog, String schema, Packet packet, TableDetail td ) {
		TableMetaData tmd = td.getTableMetaData();
		StringBuilder sb = new StringBuilder( " INSERT INTO " );
		StringBuilder sb2 = new StringBuilder( " VALUES ( " );
		
		sb.append( TableMetaData.makeFullTableName( catalog, schema, tmd.getTableName() ) ).append( " ( " );					
		for ( int k = 0; k < tmd.getColumnNames().length; k++ ) {				
			sb.append( tmd.getColumnNames()[k] ).append( k == ( tmd.getColumnNames().length - 1 ) ? " ) " : ", " );
			sb2.append( "?" ).append( k + 1 ).append( k == ( tmd.getColumnNames().length - 1 ) ? " ) " : ", " );				
		}
		return sb.append( sb2 ).toString();
	}
	
	/**
	 * check rows by primary key in TableDetail and DB
	 * @param con - Connection object
	 * @param catalog - catalog DB
	 * @param schema - schema DB
	 * @param packet - Packet object
	 * @param td - TableDetail object
	 * @return BitSet - flags array, where true is row must be update, otherwise row must be create
	 * @throws Exception
	 */
	private BitSet checkUpdateOrCreate( String catalog, String schema, Packet packet, 
			TableDetail td ) throws Exception {
		TableMetaData tmd = td.getTableMetaData();
		int pkind = tmd.getPrimaryKeyIndex();
		BitSet bs = new BitSet( td.getRows().size() );
		bs.clear();
		 
		StringBuilder sb = new StringBuilder();
		//List<Long> ll = new ArrayList<>();
		int ind = 1;
		if ( ( tmd.getColumnTypes()[pkind] == java.sql.Types.INTEGER ) 
				|| ( tmd.getColumnTypes()[pkind] == java.sql.Types.TINYINT ) 
				|| ( tmd.getColumnTypes()[pkind] == java.sql.Types.SMALLINT ) ) {			
			for ( int[] row : td.getRows() ) {
				//if ( !ll.contains( row[pkind] ) ) ll.add( Long.valueOf( row[pkind] ) );
				sb.append( row[pkind] ).append( ind++ == td.getRows().size() ? " " : ", " );			
			}
		}
		if ( tmd.getColumnTypes()[pkind] == java.sql.Types.BIGINT ){
			for ( int[] row : td.getRows() ) {
				sb.append( packet.getLongValue( row[pkind] ) ).append( ind++ == td.getRows().size() ? " " : ", " );
				//if ( !ll.contains( packet.getLongValue( row[pkind] ) ) ) ll.add( packet.getLongValue( row[pkind] ) );
			}
		}
		if ( tmd.getTableName().equals( "ProfilePattern" ) ) {
			System.out.println("hello");
		}

		String sql = String.format( SQL_CHECK_TABLE_ID, // SELECT CONVERT(BIGINT, %s) FROM %s WHERE %s IN ( %s )
				tmd.getColumnNames()[pkind], 
				TableMetaData.makeFullTableName( catalog, schema, tmd.getTableName() ), 
				tmd.getColumnNames()[pkind],
				//Arrays.toString( ll.toArray( new Long[0] ) ) );
				sb.toString() );

		List<Long> ll = OrmHelper.findByQuery( QueryType.SQL, sql, Long.class );
		if ( ( ll == null ) || ( ll.size() == 0 ) ) return bs;
		if ( ll.size() == td.getRows().size() ) {
			bs.set( 0, bs.size() );
			return bs;
		}
		if ( ( tmd.getColumnTypes()[pkind] == java.sql.Types.INTEGER ) 
				|| ( tmd.getColumnTypes()[pkind] == java.sql.Types.TINYINT ) 
				|| ( tmd.getColumnTypes()[pkind] == java.sql.Types.SMALLINT ) ) {			
			for ( Long l : ll ) {
				ind = 0;
				for ( int[] row : td.getRows() ) {
					if ( l.intValue() == row[pkind] ) {
						bs.set( ind );
						break;
					}
					ind++;					
				}
			}
		}
		if ( tmd.getColumnTypes()[pkind] == java.sql.Types.BIGINT ){
			for ( Long l : ll ) {
				ind = 0;
				for ( int[] row : td.getRows() ) {
					if ( l.equals( packet.getLongValue( row[pkind] ) ) ) {
						bs.set( ind );
						break;
					}
					ind++;
				}
			}
		}
		return bs;
	}

	/**	
	 * set parameters in SQL query and execute  
	 * @param packet - open result set having table data
	 * @param td - TableDetail object
	 * @param row - current row for UPADTE or INSERT
	 * @throws Exception
	 */
	private void setParametersAndExecute( String sql, Packet packet, TableDetail td, int[] row  ) throws Exception {
		if ( ( row == null ) || ( row.length < 1 ) ) {
			throw new IllegalArgumentException( "setParametersAndExecute() : input parameters is null" );
		}
		/*
		if ( ( log != null ) && log.isDebugEnabled() ) log.debug( "setParametersAndExecute " + sql );
		if ( ( log != null ) && log.isDebugEnabled() ) log.debug( "setParametersAndExecute " + Arrays.toString( td.getTableMetaData().getColumnNames() ) );
		if ( ( log != null ) && log.isDebugEnabled() ) log.debug( "setParametersAndExecute " + Arrays.toString( row ) );
		if ( ( log != null ) && log.isDebugEnabled() ) log.debug( "setParametersAndExecute " + packet.getLongValue( row[0] ) );
		*/
		TableMetaData tmd = td.getTableMetaData();
		Query q = OrmHelper.getCurrentManager().createNativeQuery( sql );
		for ( int col = 0; col < row.length; col++ ) {			
			switch( tmd.getColumnTypes()[col] ) {					
			case java.sql.Types.CLOB:
				if ( row[col] == -1 ) q.setParameter( col + 1, null );
				else q.setParameter( col + 1, packet.getClobValue( row[col] ) ); 
				break;

			case java.sql.Types.BLOB:
				if ( row[col] == -1 ) q.setParameter( col + 1, null );
				else q.setParameter( col + 1, packet.getBlobValue( row[col] ) ); 
				break;

			case java.sql.Types.TIMESTAMP:
				if ( row[col] == -1 ) q.setParameter( col + 1, null );
				else q.setParameter( col + 1, packet.getTimestampValue( row[col] ) ); 
				break;					

			case java.sql.Types.DATE:		
				if ( row[col] == -1 ) q.setParameter( col + 1, null );
				else q.setParameter( col + 1, packet.getDateValue( row[col] ) ); 
				break;					

			case java.sql.Types.DECIMAL:
			case java.sql.Types.NUMERIC:
				if ( row[col] == -1 ) q.setParameter( col + 1, null );
				else q.setParameter( col + 1, packet.getBigDecimalValue( row[col] ) ); 
				break;					

			case java.sql.Types.FLOAT:
			case java.sql.Types.DOUBLE:
				if ( row[col] == -1 ) q.setParameter( col + 1, null );
				else q.setParameter( col + 1, packet.getDoubleValue( row[col] ) ); 
				break;					

			case java.sql.Types.REAL:
				if ( row[col] == -1 ) q.setParameter( col + 1, null );
				else q.setParameter( col + 1, packet.getDoubleValue( row[col] ).floatValue() ); 
				break;

			case java.sql.Types.BIGINT:
				if ( row[col] == -1 ) q.setParameter( col + 1, null );
				else q.setParameter( col + 1, packet.getLongValue( row[col] ) ); 
				break;

			case java.sql.Types.INTEGER:
				if ( !tmd.getColumnNullable().get(col) ) {
					q.setParameter( col + 1, Integer.valueOf( row[col] ) );
				} else {
					if ( row[col] == -1 ) q.setParameter( col + 1, null );
					else q.setParameter( col + 1, packet.getLongValue( row[col] ).intValue() ); 
				}
				break;

			case java.sql.Types.TINYINT:
			case java.sql.Types.SMALLINT:
				if ( !tmd.getColumnNullable().get(col) ) {
					q.setParameter( col + 1, Short.valueOf( ( short ) row[col] ) );					
				} else {
					q.setParameter( col + 1, row[col] == -1 ? null : packet.getLongValue( row[col] ).shortValue() );
				}
				break;

			case java.sql.Types.BIT:
			case java.sql.Types.BOOLEAN:
				if ( row[col] == -1 ) q.setParameter( col + 1, null );
				else q.setParameter( col + 1, ( row[col] == 0 ? Boolean.FALSE : Boolean.TRUE ) );
				break;

			case java.sql.Types.BINARY:
			case java.sql.Types.VARBINARY:
			case java.sql.Types.LONGVARBINARY:
				if ( row[col] == -1 ) q.setParameter( col + 1, null );
				else  q.setParameter( col + 1, packet.getBytesValue( row[col] ) );
				break;

			case java.sql.Types.CHAR:
			case java.sql.Types.VARCHAR:
			case java.sql.Types.LONGVARCHAR:
			case java.sql.Types.NCHAR:
			case java.sql.Types.NVARCHAR:
			case java.sql.Types.LONGNVARCHAR:					
				if ( row[col] == -1 ) q.setParameter( col + 1, null );
				else  q.setParameter( col + 1, packet.getStringValue( row[col] ) );
				break;

			default:
				throw new RuntimeException( "unknown sql type : " + tmd.getColumnTypes()[col] );  
			}
		}
		q.executeUpdate();
	}
}
