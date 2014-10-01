package minos.data.orm.controllers;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.TreeMap;

import javax.persistence.Table;

import minos.data.orm.OrmHelper;
import minos.data.services.TablesInfo;
import minos.entities.Logger;
import minos.utils.ResourceKeeper;
import minos.utils.ResourceKeeper.OType;

public class LoggerJpaController {
	// =================================================================================================================
	// Constants
	// =================================================================================================================
	private static final String METHOD_NAME = "getId";

	// =================================================================================================================
	// Fields
	// =================================================================================================================
	private Map<String, Integer> cache = new TreeMap<String, Integer>();

	// =================================================================================================================
	// Constructors
	// =================================================================================================================
	private LoggerJpaController() { }

	// =================================================================================================================
	// Methods
	// =================================================================================================================
	public static LoggerJpaController getInstance() {
		return Holder.KEEPER;
	}
	
	/**
	 * make new Logger entity and save to DB ( always run in outer transaction )
	 * @param entity - entity for logging
	 * @param opCode - operation code
	 * @param summary -summary information about operation
	 * @return
	 * @throws Exception
	 */
	public Logger create( Object entity, short opCode, String summary ) throws Exception {
		Number id = null;
		for ( Method m : entity.getClass().getMethods() ) {
			if ( METHOD_NAME.equalsIgnoreCase( m.getName() ) ) {
				id = ( Number ) m.invoke( entity );
				break;				
			}
		}
		if ( id == null ) throw new IllegalArgumentException( "LoggerJpaController.create() :"
				+ " method getId() not found. Entity is " + entity ); 

		int tblCode = getTableCode( entity.getClass() );
		Logger l = new Logger( null, id.longValue(), opCode, tblCode, summary );
		return ( Logger ) OrmHelper.create( l );
	}

	/**
	 * get table name from class annotation and return code for logger 
	 * @param cls - entity's class 
	 * @return code table ; otherwise -1   
	 */
	private int getTableCode( Class<?> cls ) {
		Integer i = cache.get( cls.getName() );
		if ( i != null ) return i;
		TablesInfo ti = ResourceKeeper.getObject( OType.TABLES_INFO );
		if ( ti == null ) return -1;
		for ( Annotation a : cls.getAnnotations() ) {
			if ( a instanceof Table ) {
				String tname = ( ( Table ) a ).name();
				if ( tname == null ) return -1;
				i = ti.getCodeByName( tname );
				if ( ( i == null ) || ( ti.getVarietyByName( tname ) == TablesInfo.Variety.LOGGING_DISABLED ) ) i = -1;
				cache.put( cls.getName(), i );
				return i;//( i == null ? -1 : i );			
			}
		}		
		return -1;
	}

	// =================================================================================================================
	// Inner and Anonymous Classes
	// =================================================================================================================
	private static class Holder {
		private static final LoggerJpaController KEEPER = new LoggerJpaController();
	}
}