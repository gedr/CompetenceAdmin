package minos.data.orm.controllers;

import static minos.entities.Logger.OPERATION_CODE_DML_CREATE;
import static minos.entities.Logger.OPERATION_CODE_DML_CREATE_BULK;
import static minos.entities.Logger.OPERATION_CODE_DML_UPDATE;
import static minos.entities.Logger.OPERATION_CODE_DML_UPDATE_BULK;


import minos.data.orm.OrmCommand;
import minos.data.orm.OrmHelper;
import minos.entities.Journal;

public abstract class BasisJpaController<T> {
	// =================================================================================================================
	// Methods
	// =================================================================================================================
	public abstract T create( T entity, boolean saveEntity, boolean bulk, 
			boolean individualTransaction ) throws Exception;

	public abstract T update( T entity, boolean saveEntity, boolean bulk, 
			boolean individualTransaction ) throws Exception;

	public abstract T delete( T entity, boolean saveEntity, boolean bulk, 
			boolean individualTransaction ) throws Exception;
	
	protected void startOrmCommand( OrmCommand cmd, Object param, 
			boolean individualTransaction ) throws Exception {
		if ( individualTransaction ) {
			OrmHelper.executeAsTransaction( cmd, param );
		} else {
			cmd.execute( param );;
		}
	}
	
	/**
	 * create or update Journal entity ( always run in outer transaction )
	 * @param j - existing Journal entity or null 
	 * @param cmd - DML command
	 * @param update - DML command is update editMoment if true; otherwise update DeleteMoment 
	 * @return changed Journal entity
	 * @throws Exception
	 */
	public Journal makeJournal( Journal j, short cmd, boolean update ) throws Exception {
		boolean bulk = ( ( cmd == OPERATION_CODE_DML_CREATE_BULK ) || ( cmd == OPERATION_CODE_DML_UPDATE_BULK ) );
		JournalJpaController jjc = JournalJpaController.getInstance();
		switch ( cmd ) {
		case OPERATION_CODE_DML_CREATE_BULK : 
		case OPERATION_CODE_DML_CREATE :
			j = ( j == null ? jjc.create( null, true, bulk, false ) : jjc.copy( j, true, bulk, false ) ); 
			break;

		case OPERATION_CODE_DML_UPDATE_BULK : 
		case OPERATION_CODE_DML_UPDATE :
			j = ( update ? jjc.update( j, true, bulk, false ) : jjc.delete( j, true, bulk, false ) );
			break;

		default :
			throw new IllegalArgumentException( "BasisJpaController.makeJournal() : illegal cmd parameter" );
		}
		return j;		
	}

}