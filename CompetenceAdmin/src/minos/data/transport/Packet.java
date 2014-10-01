package minos.data.transport;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class Packet implements Serializable {	
	private static final long serialVersionUID = 1L;
	
	private short version;
	private UUID currentUuid;
	private UUID previousUuid;
	
	private TableDetail[] tableDetails;
	private List<minos.entities.Logger> log;

	private List<Long> longs;
	private List<Double> doubles;
	private List<String> strings;
	private List<byte[]> bytes;
	private List<BigDecimal> bigDecimals;
	private List<Timestamp> timestamps;
	private List<Date> dates;
	private List<Clob> clobs;
	private List<Blob> blobs;

	public Long getLongValue( int ind ) {
		if ( ( longs == null ) || ( ind < 0 ) || ( ind >= longs.size() ) ) return null;
		return longs.get( ind ); 
	}
	
	public int addLongValue( Long val ) {
		if ( val == null ) return -1;
		if ( longs == null ) longs = new ArrayList<>();
		if ( longs.indexOf( val ) == -1 ) longs.add( val );
		return longs.indexOf( val );
	}

	public Double getDoubleValue( int ind ) {
		if ( ( doubles == null ) || ( ind < 0 ) || ( ind >= doubles.size() ) )  return null;
		return doubles.get( ind ); 
	}

	public int addDoubleValue( Double val ) {
		if ( val == null ) return -1;
		if ( doubles == null ) doubles = new ArrayList<>();
		if ( doubles.indexOf( val ) == -1 ) doubles.add( val );
		return doubles.indexOf( val );
	}

	public String getStringValue( int ind ) {
		if ( ( strings == null ) || ( ind < 0 ) || ( ind >= strings.size() ) ) return null;
		return strings.get( ind ); 
	}

	public int addStringValue( String val ) {
		if ( val == null ) return -1;
		if ( strings == null ) strings = new ArrayList<>();
		if ( strings.indexOf( val ) == -1 ) strings.add( val );
		return strings.indexOf( val );
	}

	public byte[] getBytesValue( int ind ) {
		if ( ( bytes == null ) || ( ind < 0 ) || ( ind >= bytes.size() ) ) return null;
		return bytes.get( ind ); 
	}

	public int addBytesValue( byte[] val ) {
		if ( val == null ) return -1;
		if ( bytes == null ) bytes = new ArrayList<>();
		bytes.add( val );
		return bytes.size() - 1;
	}

	public BigDecimal getBigDecimalValue( int ind ) {
		if ( ( bigDecimals == null ) || ( ind < 0 ) || ( ind >= bigDecimals.size() ) ) return null;
		return bigDecimals.get( ind ); 
	}

	public int addBigDecimalValue( BigDecimal val ) {
		if ( val == null ) return -1;
		if ( bigDecimals == null ) bigDecimals = new ArrayList<>();
		if ( bigDecimals.indexOf( val ) == -1 ) bigDecimals.add( val );
		return bigDecimals.indexOf( val );
	}

	public Timestamp getTimestampValue( int ind ) {
		if ( ( timestamps == null ) || ( ind < 0 ) || ( ind >= timestamps.size() ) ) return null;
		return timestamps.get( ind ); 
	}

	public int addTimestampValue( Timestamp val ) {
		if ( val == null ) return -1;
		if ( timestamps == null ) timestamps = new ArrayList<>();
		if ( timestamps.indexOf( val ) == -1 ) timestamps.add( val );
		return timestamps.indexOf( val );
	}

	public Date getDateValue( int ind ) {
		if ( ( dates == null ) || ( ind < 0 ) || ( ind >= dates.size() ) ) return null;
		return dates.get( ind ); 
	}

	public int addDateValue( Date val ) {
		if ( val == null ) return -1;
		if ( dates == null ) dates = new ArrayList<>();
		if ( dates.indexOf( val ) == -1 ) dates.add( val );
		return dates.indexOf( val );
	}

	public Clob getClobValue( int ind ) {
		if ( ( clobs == null ) || ( ind < 0 ) || ( ind >= clobs.size() ) ) return null;
		return clobs.get( ind ); 
	}

	public int addClobValue( Clob val ) {
		if ( val == null ) return -1;
		if ( clobs == null ) clobs = new ArrayList<>();
		clobs.add( val );
		return clobs.size() - 1;
	}

	public Blob getBlobValue( int ind ) {
		if ( ( blobs == null ) || ( ind < 0 ) || ( ind >= blobs.size() ) ) return null;
		return blobs.get( ind ); 
	}

	public int addBlobValue( Blob val ) {
		if ( val == null ) return -1;
		if ( blobs == null ) blobs = new ArrayList<>();
		blobs.add( val );
		return blobs.size() - 1;
	}

	public List<int[]> data;

	public Packet() { }

	public List<int[]> getData() {
		return data;
	}

	public void setData( List<int[]> data ) {
		this.data = data;
	}

	public UUID getCurrentUuid() {
		return currentUuid;
	}

	public void setCurrentUuid( UUID uuid ) {
		this.currentUuid = uuid;
	}

	public UUID getPreviousUuid() {
		return previousUuid;
	}

	public void setPreviousUuid( UUID uuid ) {
		this.previousUuid = uuid;
	}

	public short getVersion() {
		return version;
	}

	public void setVersion( short version ) {
		this.version = version;
	}

	public TableDetail[] getTableDetails() {
		return tableDetails;
	}

	public void setTableDetails( TableDetail[] tableDetails ) {
		this.tableDetails = tableDetails;
	}

	public List<minos.entities.Logger> getLog() {
		return log;
	}

	public void setLog( List<minos.entities.Logger> log ) {
		this.log = log;
	}
}