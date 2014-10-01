package minos.data.transport;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import javax.sql.DataSource;

import minos.data.orm.OrmHelper;
import minos.data.orm.OrmHelper.QueryType;
import minos.data.services.TablesInfo;
import minos.utils.ResourceKeeper;
import minos.utils.ResourceKeeper.OType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.gedr.util.tuple.Pair;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class PacketUnloader {
	//=====================================================================================================
	//=                                             Constants                                             =
	//=====================================================================================================
	private static final String SCHEMA = "Minos";
	private static final String JPQL_SEARCH_PREV_MARKER = " SELECT l FROM Logger l INNER JOIN FETCH l.summary"
			+ " WHERE l.id = (SELECT MAX(l2.id) FROM Logger l2 WHERE l2.id < :lid AND l2.operationCode = :code) ";
	private static final String JPQL_LOAD_LOGGER_ROWS = "SELECT l FROM Logger l "
			+ " WHERE l.id > :minid AND l.id <= :maxid AND l.operationCode IN (:codes) "
			+ " AND l.tableCode IN (:tbls) ORDER BY l.id";

	//=====================================================================================================
	//=                                            Attributes                                             =
	//=====================================================================================================
	private static Logger log = LoggerFactory.getLogger( PacketUnloader.class );
	private TablesInfo tablesInfo;

	//=====================================================================================================
	//=                                           Constructors                                            =
	//=====================================================================================================
	public PacketUnloader() { 
		tablesInfo = ResourceKeeper.getObject( OType.TABLES_INFO );
	}

	//=====================================================================================================
	//=                                                     Methods                                       =
	//=====================================================================================================
	/**
	 * unload tables rows 
	 * @param tblCodes - list of tables for unload
	 * @throws Exception 
	 */
	public Packet unload( List<Integer> tblCodes ) throws Exception {
		DataSource ds = (DataSource) OrmHelper.getFactory().getProperties().get( "openjpa.ConnectionFactory" );
		UUID uuid = UUID.randomUUID();

		// make GHOST MARKER
		if ( ( log != null ) && log.isDebugEnabled() ) log.debug( "make GHOST MARKER" );		
		minos.entities.Logger lgr = new minos.entities.Logger( null, 0L, 
				minos.entities.Logger.OPERATION_CODE_GHOST_PACKET_MARKER, 
				0, uuid.toString() );
		OrmHelper.createEntity( lgr );

		// search previous BUILD MARKER		
		if ( ( log != null ) && log.isDebugEnabled() ) log.debug( "search previous BUILD MARKER" );
		List<minos.entities.Logger> ll = OrmHelper.findByQueryWithParam( QueryType.JPQL, 
				JPQL_SEARCH_PREV_MARKER, 
				minos.entities.Logger.class, 
				new Pair<Object, Object>( "code", minos.entities.Logger.OPERATION_CODE_BUILD_PACKET_MARKER ),
				new Pair<Object, Object>( "lid", lgr.getId() ) );
		long minid = ( ( ( ll == null ) || ( ll.size() == 0 ) ) ? 0 : ll.get(0).getId() );
		UUID prevUuid = ( minid == 0 ? null : UUID.fromString( ll.get(0).getSummary() ) );

		// load all Logger rows between 2 markers
		if ( ( log != null ) && log.isDebugEnabled() ) log.debug( "load all Logger rows between 2 markers" );
		List<Short> codes = Arrays.asList( minos.entities.Logger.OPERATION_CODE_DML_CREATE, 
				minos.entities.Logger.OPERATION_CODE_DML_UPDATE,
				minos.entities.Logger.OPERATION_CODE_DML_DELETE,
				minos.entities.Logger.OPERATION_CODE_GHOST_PACKET_MARKER );
		
		List<minos.entities.Logger> operations = OrmHelper.findByQueryWithParam( QueryType.JPQL, 
				JPQL_LOAD_LOGGER_ROWS, 
				minos.entities.Logger.class,
				new Pair<Object, Object>( "minid", minid ),
				new Pair<Object, Object>( "maxid", lgr.getId() ),
				new Pair<Object, Object>( "codes", codes ),
				new Pair<Object, Object>( "tbls", tblCodes ) );

		Map<Integer, Pair<List<Long>, List<Long>>> tbls = getMapChangedIds( operations );

		Packet packet = new Packet();
		packet.setCurrentUuid( uuid );
		packet.setPreviousUuid( prevUuid );
		packet.setLog( operations );
		packet.setTableDetails( new TableDetail [tbls.keySet().size()] );		

		Connection con = null;
		try {
			con = ds.getConnection();
			int ind = 0;
			for ( Integer i : tbls.keySet() ) {		
				TableMetaData tmd = TableMetaData.build( con, null, SCHEMA, tablesInfo.getNameByCode( i ) );
				TableDetail td = new  TableDetail();
				packet.getTableDetails()[ind] = td;
				packet.getTableDetails()[ind].setTableCode( i );
				packet.getTableDetails()[ind].setTableMetaData( tmd );

				if ( ( log != null ) && log.isDebugEnabled() ) log.debug( "unload Rows for " + tmd.getTableName() );
				
				Pair<List<Long>, List<Long>> p = tbls.get( i );
				if ( ( p != null ) && ( p.getFirst() != null ) ) unloadExistingRows( con, null, SCHEMA, packet, td, p.getFirst() );
				if ( ( p != null ) && ( p.getSecond() != null ) ) unloadDeleteRows( packet, td, p.getSecond() );			
				ind++;
			}
		} catch ( Exception ex ) {
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( "PacketUnloader.unload() : ", ex );
			throw ex;
		} finally {
			if ( con != null )
				try {
					con.close();
				} catch ( Exception e ) {
					if ( ( log != null ) && log.isErrorEnabled() ) log.error( "PacketUnloader.unload() : connection close() error ", e );
				}
		}

		// change marker status
		lgr.setOperationCode( minos.entities.Logger.OPERATION_CODE_BUILD_PACKET_MARKER );
		OrmHelper.updateEntity( lgr );
		return packet;
	}

	/**
	 * save packet to stream in JSON format
	 * @param packet - Packet object
	 * @param os - open output stream
	 * @throws IOException 
	 */
	public static void saveToStreamAsJson( Packet packet, OutputStream os ) throws IOException {
		if ( ( packet == null ) || ( os == null ) ) throw new IllegalArgumentException();
		GZIPOutputStream gz = new GZIPOutputStream( os );
		PrintStream ps = new PrintStream( gz, true, "UTF-8" );
		Gson gson =  new GsonBuilder()
		.setDateFormat( DateFormat.LONG )
		//.setFieldNamingPolicy( FieldNamingPolicy.UPPER_CAMEL_CASE )
		//.setPrettyPrinting()
		//.setVersion(1.0)
		.create();
		
		gson.toJson( packet, ps );
		ps.flush();
		ps.close();
		gz.flush();
		gz.close();		
	}

	/**
	 * save packet to file in JSON format
	 * @param packet - Packet object
	 * @param fileName - full filename
	 * @throws IOException 
	 */
	public static void saveToFileAsJson( Packet packet, String fileName ) throws IOException {
		if ( ( packet == null ) || ( fileName == null ) ) throw new IllegalArgumentException();
		FileOutputStream fos = new FileOutputStream( fileName );
		saveToStreamAsJson( packet, fos );
		fos.flush();
		fos.close();		
	}

	/**
	 * save packet to stream in BINARY format
	 * @param packet - existing Packet object
	 * @param os - open output stream
	 * @throws IOException 
	 */
	public static void saveToStreamAsBin( Packet packet, OutputStream os ) throws IOException {
		if ( ( packet == null ) || ( os == null ) ) throw new IllegalArgumentException();
		GZIPOutputStream gz = new GZIPOutputStream( os );
		ObjectOutputStream oos = new ObjectOutputStream( gz );
		oos.writeObject( packet );
		oos.flush();
		oos.close();
		gz.flush();
		gz.close();		
	}

	/**
	 * save packet to file in BINARY format
	 * @param packet - existing Packet object
	 * @param fileName - full filename
	 * @throws IOException 
	 */
	public static void saveToFileAsBin( Packet packet, String fileName ) throws IOException {
		if ( ( packet == null ) || ( fileName == null ) ) throw new IllegalArgumentException();
		FileOutputStream fos = new FileOutputStream( fileName );
		saveToStreamAsBin( packet, fos );
		fos.flush();
		fos.close();		
	}

	/**
	 * create map for changed rows 
	 * @param operations - list logger operations
	 * @return Map - key is table code; value is pair<create_and_update_ids, delete_ids>
	 */
	private Map<Integer, Pair<List<Long>, List<Long>>> getMapChangedIds( List<minos.entities.Logger> operations ) {		
		Map<Integer, Pair<List<Long>, List<Long>>> tbls = new TreeMap<>(); 
		for ( minos.entities.Logger l : operations ) {
			if ( ( tablesInfo.getVarietyByCode( l.getTableCode() ) == TablesInfo.Variety.LOGGING_AND_TRANSPORT_ALWAYS ) 
					|| ( tablesInfo.getVarietyByCode( l.getTableCode() ) == TablesInfo.Variety.LOGGING_AND_TRANSPORT_SOMETIMES ) ) {
				Pair<List<Long>, List<Long>> p = tbls.get( l.getTableCode() );
				if ( p == null ) {
					p = new Pair<List<Long>, List<Long>>( null, null );
					tbls.put( l.getTableCode(), p );
				}

				if ( l.getOperationCode() == minos.entities.Logger.OPERATION_CODE_DML_DELETE ) {
					if ( p.getSecond() == null ) p.setSecond( new ArrayList<Long>() );
					p.getSecond().add( l.getExternalId() );					
				}
				if ( ( l.getOperationCode() == minos.entities.Logger.OPERATION_CODE_DML_CREATE )
						|| ( l.getOperationCode() == minos.entities.Logger.OPERATION_CODE_DML_UPDATE ) ) {
					if ( p.getFirst() == null ) p.setFirst( new ArrayList<Long>() );
					p.getFirst().add( l.getExternalId() );				
				}
			}
		}
		return tbls;
	}

	/**
	 * unload delete table rows
	 * @param packet - Packet object
	 * @param tind - index of TableDetail' object in Packet
	 * @param pkeys - list of primary keys for unload
	 * @throws Exception
	 */
	private void unloadDeleteRows( Packet packet, TableDetail td, List<Long> pkeys ) throws Exception {
		if ( ( packet == null ) || ( td == null ) || ( pkeys == null ) 
				|| ( pkeys.size() <= 0 ) ) throw new IllegalArgumentException( "PacketUnloader.unloadDeleteRows() : wrong arguments" );

		TableMetaData tmd = td.getTableMetaData();
		int pkind = tmd.getPrimaryKeyIndex();
		if ( ( tmd.getColumnTypes()[pkind] == java.sql.Types.INTEGER )
				|| ( tmd.getColumnTypes()[pkind] == java.sql.Types.TINYINT ) 
				|| ( tmd.getColumnTypes()[pkind] == java.sql.Types.SMALLINT ) ) {
			td.setDeleteKeys( new int[pkeys.size()] );
			int ind = 0;
			for ( Long l : pkeys ) td.getDeleteKeys()[ind++] = l.intValue();
			return;
		}
		if ( tmd.getColumnTypes()[pkind] == java.sql.Types.BIGINT ) {
			td.setDeleteKeys( new int[pkeys.size()] );
			int ind = 0;
			for ( Long l : pkeys ) td.getDeleteKeys()[ind++] = packet.addLongValue( l );
			return;
		}
	}

	/**
	 * unload existing table rows
	 * @param con - connection object
	 * @param catalog - catalog DB name 
	 * @param schema - schema DB
	 * @param packet - Packet object
	 * @param tind - index of TableDetail' object in Packet
	 * @param pkeys - list of primary keys for unload
	 * @throws Exception
	 */
	private void unloadExistingRows( Connection con, String catalog, String schema, 
			Packet packet, TableDetail td, List<Long> pkeys ) throws Exception {
		if ( ( pkeys == null ) || ( pkeys.size() == 0 ) ) return;
		if ( ( con == null ) || ( td == null ) || ( packet == null ) ) throw new IllegalArgumentException( "TableUnloader.unloadExistingRows() : wrong arguments " ); 

		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = con.createStatement();
			rs = stmt.executeQuery( makeUnloadQuery( catalog, schema, td, pkeys ) );
			
			td.setRows( new ArrayList<int[]>() );
			while ( rs.next() ) {
				int[] row = unloadTableRow( rs, packet, td );
				if ( ( row == null ) 
						|| ( row.length != td.getTableMetaData().getColumnTypes().length ) ) throw new RuntimeException();
				td.getRows().add( row );
			}
		} catch ( Exception ex ) {
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( "TableUnloader.unloadExistingRows() : ResultSet close() error ", ex );
			throw ex;			
		} finally {
			if ( rs != null )
				try {
					rs.close();
				} catch ( Exception e ) {
					if ( ( log != null ) && log.isErrorEnabled() ) log.error( "TableUnloader.unloadExistingRows() : ResultSet close() error ", e );
				}

			if ( stmt != null ) 
				try {
					stmt.close();
				} catch ( Exception e ) {
					if ( ( log != null ) && log.isErrorEnabled() ) log.error( "TableUnloader.unloadExistingRows() : Statement close() error ", e );
				}
		}
	}

	/**
	 * make unload SELECT query for TableDetail
	 * @param catalog - DB catalog name or null , if unused
	 * @param schema -  DB schema name or null , if unused
	 * @param td - TableDetail object
	 * @param pkeys - list of IDs for unload
	 * @return right select query
	 */
	private String makeUnloadQuery( String catalog, String schema, TableDetail td, List<Long> pkeys  ) {
		StringBuilder sb = new StringBuilder();
		TableMetaData tmd = td.getTableMetaData();
		sb.append( " SELECT " );
		for ( int i = 0; i < tmd.getColumnNames().length; i++ ) {
			sb.append( tmd.getColumnNames()[i] ).append( i == ( tmd.getColumnNames().length -1 ) ? " " : ", " );
		}
		sb.append( " FROM " ).append( TableMetaData.makeFullTableName( null, SCHEMA, tmd.getTableName() ) )
		.append( " WHERE " ).append( tmd.getColumnNames()[tmd.getPrimaryKeyIndex()] ).append( " IN ( " );

		int ind = 1;
		for ( Long l : pkeys ) {
			sb.append( l.toString() ).append( ind++ == pkeys.size() ? " ) " : ", " );
		}
		return sb.toString();
	}



	/**	
	 * save table's row in data area  
	 * @param rs - open result set having table data
	 * @param pts - current object
	 * @throws Exception
	 */
	private int[] unloadTableRow( ResultSet rs, Packet packet, TableDetail td ) throws Exception {
		if ( ( rs == null ) || ( packet == null ) ) throw new IllegalArgumentException( "unloadTableRow() : input parameters is null" );
		TableMetaData tmd = td.getTableMetaData();
		int colCount = tmd.getColumnTypes().length;		
		int[] ref = new int[colCount];

		for ( int col = 0; col < colCount; col++ ) {			
			switch( tmd.getColumnTypes()[col] ) {					
			case java.sql.Types.CLOB:
				ref[col] = packet.addClobValue( rs.getClob( col + 1 ) );
				break;

			case java.sql.Types.BLOB:
				ref[col] = packet.addBlobValue( rs.getBlob( col + 1 ) );
				break;

			case java.sql.Types.TIMESTAMP:
				ref[col] = packet.addTimestampValue( rs.getTimestamp( col + 1 ) );
				break;					

			case java.sql.Types.DATE:		
				ref[col] = packet.addDateValue( rs.getDate( col + 1 ) );
				break;					

			case java.sql.Types.DECIMAL:
			case java.sql.Types.NUMERIC:
				ref[col] = packet.addBigDecimalValue( rs.getBigDecimal( col + 1 ) );													
				break;					

			case java.sql.Types.FLOAT:
			case java.sql.Types.DOUBLE:
				ref[col] = packet.addDoubleValue( rs.getDouble( col + 1 ) );
				break;					

			case java.sql.Types.REAL:
				ref[col] = packet.addDoubleValue( ( double ) rs.getFloat( col + 1 ) );
				break;

			case java.sql.Types.BIGINT:
				ref[col] = packet.addLongValue( rs.getLong( col + 1 ) );
				break;

			case java.sql.Types.INTEGER:
				Integer i = rs.getInt( col + 1 );
				if ( tmd.getColumnNullable().get( col ) ) {
					ref[col] = ( i == null ? -1 : packet.addLongValue( ( long ) i ) );	
				} else {
					ref[col] = i;
				}				
				break;

			case java.sql.Types.TINYINT:
			case java.sql.Types.SMALLINT:
				Short sh = rs.getShort( col + 1 );
				if ( tmd.getColumnNullable().get( col ) ) {
					ref[col] = ( sh == null ? -1 : packet.addLongValue( ( long ) sh ) );	
				} else {
					ref[col] = sh;
				}				
				break;

			case java.sql.Types.BIT:
			case java.sql.Types.BOOLEAN:
				Boolean bool = rs.getBoolean( col + 1 );
				ref[col] = (  bool == null ? -1 : ( bool == Boolean.TRUE ? 1 : 0 ) ); 
				break;

			case java.sql.Types.BINARY:
			case java.sql.Types.VARBINARY:
			case java.sql.Types.LONGVARBINARY:
				ref[col] = packet.addBytesValue( rs.getBytes( col + 1 ) );
				break;

			case java.sql.Types.CHAR:
			case java.sql.Types.VARCHAR:
			case java.sql.Types.LONGVARCHAR:
			case java.sql.Types.NCHAR:
			case java.sql.Types.NVARCHAR:
			case java.sql.Types.LONGNVARCHAR:					
				ref[col] = packet.addStringValue( rs.getString( col + 1 ) );
				break;

			default:
				throw new RuntimeException( "unknown sql type : " + tmd.getColumnTypes()[col] );  
			}
		}
		return ref;		
	}
}
