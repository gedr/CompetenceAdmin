package minos.data.orm.controllers;

import static minos.entities.Logger.OPERATION_CODE_DML_CREATE;
import static minos.entities.Logger.OPERATION_CODE_DML_CREATE_BULK;
import static minos.entities.Logger.OPERATION_CODE_DML_UPDATE;
import static minos.entities.Logger.OPERATION_CODE_DML_UPDATE_BULK;

import java.sql.Timestamp;

import ru.gedr.util.tuple.Pair;
import minos.data.orm.OrmCommand;
import minos.data.orm.OrmHelper;
import minos.entities.Journal;
import minos.entities.Person;
import minos.utils.ResourceKeeper;
import minos.utils.ResourceKeeper.OType;

public class JournalJpaController extends BasisJpaController<Journal> implements OrmCommand {
	// =================================================================================================================
	// Constructors
	// =================================================================================================================
	private JournalJpaController() { }

	// =================================================================================================================
	// Methods for/from SuperClass/Interfaces
	// =================================================================================================================
	@Override
	public void execute( Object obj ) throws Exception {
		@SuppressWarnings("unchecked")
		Pair<Journal, Short> t = ( Pair<Journal, Short> ) obj;

		boolean flag = ( (t.getSecond() == OPERATION_CODE_DML_CREATE_BULK ) 
				|| ( t.getSecond() == OPERATION_CODE_DML_CREATE ) );
		t.setFirst( ( Journal ) ( flag ? OrmHelper.create( t.getFirst() ) : OrmHelper.update( t.getFirst() ) ) );
		OrmHelper.getCurrentManager().flush();
		LoggerJpaController.getInstance().create( t.getFirst(), t.getSecond(), null );
	}

	@Override
	public Journal create( Journal entity, boolean saveEntity, boolean bulk,
			boolean individualTransaction ) throws Exception {
		Timestamp now = new Timestamp( System.currentTimeMillis() );
		Person person = ( Person ) ResourceKeeper.getObject( OType.CURRENT_PERSON );
		String host = ( String ) ResourceKeeper.getObject( OType.CURRENT_HOST );
		String login = ( String ) ResourceKeeper.getObject( OType.CURRENT_LOGIN );
		if ( entity == null ) {
			entity = new Journal( now, person.getId(), host, login, 
					now, person.getId(), null, null, 
					( Timestamp ) ResourceKeeper.getObject( OType.DOOMSDAY ), person.getId(), null, null);
		} else {
			entity.setCreateMoment( now );
			entity.setCreatorHost( host );
			entity.setCreatorID( person.getId() );
			entity.setCreatorLogin( login );
			entity.setEditMoment( now );
			entity.setEditorHost( null );
			entity.setEditorID( person.getId() );
			entity.setEditorLogin( null );
			entity.setDeleteMoment( ( Timestamp ) ResourceKeeper.getObject( OType.DOOMSDAY ) );
			entity.setDeleterHost( null );
			entity.setDeleterID( person.getId() );
			entity.setDeleterLogin( null );
		}
		if ( saveEntity ) startOrmCommand( this, 
				new Pair<Journal, Short>( entity, bulk ? OPERATION_CODE_DML_CREATE_BULK : OPERATION_CODE_DML_CREATE ), 
				individualTransaction );
		return entity;
	}

	@Override
	public Journal update( Journal entity, boolean saveEntity, boolean bulk,
			boolean individualTransaction ) throws Exception {
		if ( entity == null ) throw new IllegalArgumentException( "JournalJpaController.update() : entity is null" ); 
		entity.setEditMoment( new Timestamp( System.currentTimeMillis() ) );
		entity.setEditorID( ( ( Person ) ResourceKeeper.getObject( ResourceKeeper.OType.CURRENT_PERSON ) ).getId() );
		entity.setEditorHost( ( String ) ResourceKeeper.getObject( ResourceKeeper.OType.CURRENT_HOST ) );
		entity.setEditorLogin( ( String ) ResourceKeeper.getObject( ResourceKeeper.OType.CURRENT_LOGIN ) );	
		if ( saveEntity ) startOrmCommand( this, 
				new Pair<Journal, Short>( entity, bulk ? OPERATION_CODE_DML_UPDATE_BULK : OPERATION_CODE_DML_UPDATE ), 
				individualTransaction );
		return entity;
	}

	@Override
	public Journal delete( Journal entity, boolean saveEntity, boolean bulk,
			boolean individualTransaction ) throws Exception {
		if ( entity == null ) throw new IllegalArgumentException( "JournalJpaController.delete() : entity is null" ); 
		entity.setDeleteMoment( new Timestamp( System.currentTimeMillis() ) );
		entity.setDeleterID( ( ( Person ) ResourceKeeper.getObject( ResourceKeeper.OType.CURRENT_PERSON ) ).getId() );
		entity.setDeleterHost( ( String ) ResourceKeeper.getObject( ResourceKeeper.OType.CURRENT_HOST ) );
		entity.setDeleterLogin( ( String ) ResourceKeeper.getObject( ResourceKeeper.OType.CURRENT_LOGIN ) );
		if ( saveEntity ) startOrmCommand( this, 
				new Pair<Journal, Short>( entity, bulk ? OPERATION_CODE_DML_UPDATE_BULK : OPERATION_CODE_DML_UPDATE ), 
				individualTransaction );
		return entity;
	}
	
	// =================================================================================================================
	// Methods
	// =================================================================================================================
	public static JournalJpaController getInstance() {
		return Holder.KEEPER;
	}

	/**
	 * make copy from other journal
	 * @param journal
	 * @param saveEntity
	 * @return copy of journal
	 */
	public Journal copy( Journal journal, boolean saveEntity, boolean bulk, 
			boolean individualTransaction ) throws Exception { 
		if ( journal == null ) throw new IllegalArgumentException( "JournalJpaController.copy() : journal is null" ); 
		Journal entity = new Journal( journal.getCreateMoment(), journal.getCreatorID(), 
				journal.getCreatorHost(), journal.getCreatorLogin(), 
				journal.getEditMoment(), journal.getEditorID(), 
				journal.getEditorHost(), journal.getEditorLogin(), 
				journal.getDeleteMoment(), journal.getDeleterID(), 
				journal.getDeleterHost(), journal.getDeleterLogin() );
		if ( saveEntity ) startOrmCommand( this, 
				new Pair<Journal, Short>( entity, bulk ? OPERATION_CODE_DML_UPDATE_BULK : OPERATION_CODE_DML_UPDATE ), 
				individualTransaction );
		return entity;
	}
	
	// =================================================================================================================
	// Inner and Anonymous Classes
	// =================================================================================================================
	private static class Holder {
		private static final JournalJpaController KEEPER = new JournalJpaController();
	}
}
