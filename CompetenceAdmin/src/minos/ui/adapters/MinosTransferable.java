package minos.ui.adapters;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

import org.apache.commons.lang.NullArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinosTransferable implements Transferable {
	// =================================================================================================================
	// Fields
	// =================================================================================================================
	private static Logger log = LoggerFactory.getLogger( MinosTransferable.class );

	private Object obj;

	// =================================================================================================================
	// Constructors
	// =================================================================================================================
	public MinosTransferable( Object obj ) {
		if ( obj == null ) throw new NullArgumentException( "MinosTransferable() : argument is null" );
		this.obj = obj;
	}
	
	// =================================================================================================================
	// Methods for/from SuperClass/Interfaces
	// =================================================================================================================
	@Override
	public Object getTransferData( DataFlavor df ) throws UnsupportedFlavorException {
		if ( !isDataFlavorSupported( df ) ) throw new UnsupportedFlavorException( df );
		return obj;
	}

	@Override
	public DataFlavor[] getTransferDataFlavors() {
		try {
			return new DataFlavor[] { 
					new DataFlavor( DataFlavor.javaJVMLocalObjectMimeType + ";class=" + obj.getClass().getName() )
			};
		} catch (ClassNotFoundException e) {
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( "MinosTransferable.getTransferDataFlavors() :"
					+ " DataFlavor create error" );
			return new DataFlavor[0];
		}
	}

	@Override
	public boolean isDataFlavorSupported( DataFlavor df ) {
		DataFlavor[] cur = getTransferDataFlavors();
		if ( ( df == null ) || ( cur == null ) || ( cur.length != 1 ) || ( cur[0] == null ) 
				|| ( cur[0].getPrimaryType() == null ) || !cur[0].getPrimaryType().equals( df.getPrimaryType() )
				|| ( cur[0].getSubType() == null ) || !cur[0].getSubType().equals( df.getSubType() )
				|| ( df.getRepresentationClass() == null ) || ( cur[0].getRepresentationClass() == null )
				|| !df.getRepresentationClass().isAssignableFrom( cur[0].getRepresentationClass() ) ) return false;
		return true;
	}
}