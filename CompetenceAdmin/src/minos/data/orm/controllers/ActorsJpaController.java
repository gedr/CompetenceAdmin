package minos.data.orm.controllers;

import static minos.entities.Logger.OPERATION_CODE_DML_CREATE;
import static minos.entities.Logger.OPERATION_CODE_DML_CREATE_BULK;
import static minos.entities.Logger.OPERATION_CODE_DML_UPDATE;
import static minos.entities.Logger.OPERATION_CODE_DML_UPDATE_BULK;

import ru.gedr.util.tuple.Triplet;
import minos.data.orm.OrmCommand;
import minos.data.orm.OrmHelper;
import minos.entities.Actors;

public class ActorsJpaController extends BasisJpaController<Actors> implements OrmCommand {
	// =================================================================================================================
	// Constructors
	// =================================================================================================================
	private ActorsJpaController() { }

	// =================================================================================================================
	// Methods for/from SuperClass/Interfaces
	// =================================================================================================================
	@Override
	public void execute( Object obj ) throws Exception {
		@SuppressWarnings("unchecked")
		Triplet<Actors, Short, Boolean> t = ( Triplet<Actors, Short, Boolean> ) obj;
		boolean newEntity = ( ( t.getSecond() == OPERATION_CODE_DML_CREATE_BULK ) 
				|| ( t.getSecond() == OPERATION_CODE_DML_CREATE ) );
		t.getFirst().setJournal( makeJournal( t.getFirst().getJournal(), t.getSecond(), t.getThird() ) );
		t.setFirst( ( Actors ) ( newEntity ? OrmHelper.create( t.getFirst() ) : OrmHelper.update( t.getFirst() ) ) );
		OrmHelper.getCurrentManager().flush();
		LoggerJpaController.getInstance().create( t.getFirst(), t.getSecond(), null ); 
	}
	
	@Override
	public Actors create( Actors entity, boolean saveEntity, boolean bulk, 
			boolean individualTransaction ) throws Exception {
		if ( ( entity == null ) || ( entity.getMeasure() == null ) || ( entity.getTestMode() == null ) 
				|| ( entity.getReserveType() == null ) || ( entity.getReserveLevel() == null ) ) {
			throw new IllegalArgumentException( "ActorsJpaController.create(): illegal entity " );
		}		
		if ( !saveEntity ) {		
			JournalJpaController jjc = JournalJpaController.getInstance();
			entity.setJournal( entity.getJournal() == null ? jjc.create( null, false, false, false ) 
					: jjc.copy( entity.getJournal(), false, false, false )  );
			return entity;
		}
		Triplet<Actors, Short, Boolean> t = new Triplet<>( entity, 
				bulk ? OPERATION_CODE_DML_CREATE_BULK : OPERATION_CODE_DML_CREATE, false );
		startOrmCommand( this, t, individualTransaction );
		return t.getFirst();
	}

	@Override
	public Actors update( Actors entity, boolean saveEntity, boolean bulk,
			boolean individualTransaction ) throws Exception {
		if ( ( entity == null ) || ( entity.getJournal() == null ) ) {
			throw new IllegalArgumentException( "ActorsJpaController.update(): illegal entity " );
		}		
		if ( !saveEntity ) {			
			JournalJpaController jjc = JournalJpaController.getInstance();
			entity.setJournal( jjc.update( entity.getJournal(), false, false, false ) );
			return entity;
		}
		Triplet<Actors, Short, Boolean> t = new Triplet<>( entity, 
				bulk ? OPERATION_CODE_DML_UPDATE_BULK : OPERATION_CODE_DML_UPDATE, true );
		startOrmCommand( this, t, individualTransaction );
		return t.getFirst();
	}

	@Override
	public Actors delete( Actors entity, boolean saveEntity, boolean bulk,
			boolean individualTransaction ) throws Exception {
		if ( ( entity == null ) || ( entity.getJournal() == null ) ) {
			throw new IllegalArgumentException( "ActorsJpaController.delete(): illegal entity" );
		}		
		if ( entity.getStatus() == Actors.STATUS_DELETE ) return entity;

		entity.setStatus( Actors.STATUS_DELETE );
		if ( !saveEntity ) {
			JournalJpaController jjc = JournalJpaController.getInstance();
			entity.setJournal( jjc.delete( entity.getJournal(), false, false, false ) );			
			return entity;	
		}
		Triplet<Actors, Short, Boolean> t = new Triplet<>( entity, 
				bulk ? OPERATION_CODE_DML_UPDATE_BULK : OPERATION_CODE_DML_UPDATE, false );
		startOrmCommand( this, t, individualTransaction );
		return t.getFirst();
	}
	
	// =================================================================================================================
	// Methods
	// =================================================================================================================
	public static ActorsJpaController getInstance() {
		return Holder.KEEPER;
	}

	// =================================================================================================================
	// Inner and Anonymous Classes
	// =================================================================================================================
	private static class Holder {
		private static final ActorsJpaController KEEPER = new ActorsJpaController();
	}
}