package minos.data.orm.controllers;

import static minos.entities.Logger.OPERATION_CODE_DML_CREATE;
import static minos.entities.Logger.OPERATION_CODE_DML_CREATE_BULK;
import static minos.entities.Logger.OPERATION_CODE_DML_UPDATE;
import static minos.entities.Logger.OPERATION_CODE_DML_UPDATE_BULK;

import java.util.Date;

import ru.gedr.util.tuple.Triplet;
import minos.data.orm.OrmCommand;
import minos.data.orm.OrmHelper;
import minos.entities.PpeStrAtr;

public class PpeStrAtrJpaController extends BasisJpaController<PpeStrAtr> implements OrmCommand {
	// =================================================================================================================
	// Constructors
	// =================================================================================================================
	private PpeStrAtrJpaController() { }

	// =================================================================================================================
	// Methods for/from SuperClass/Interfaces
	// =================================================================================================================
	@Override
	public void execute( Object obj ) throws Exception {
		@SuppressWarnings("unchecked")
		Triplet<PpeStrAtr, Short, Boolean> t = ( Triplet<PpeStrAtr, Short, Boolean> ) obj;
		boolean flag = ( ( t.getSecond() == OPERATION_CODE_DML_CREATE_BULK ) 
				|| ( t.getSecond() == OPERATION_CODE_DML_CREATE ) );
		t.getFirst().setJournal( makeJournal( t.getFirst().getJournal(), t.getSecond(), t.getThird() ) );
		t.setFirst( ( PpeStrAtr ) ( flag ? OrmHelper.create( t.getFirst() ) : OrmHelper.update( t.getFirst() ) ) );
		OrmHelper.getCurrentManager().flush();
		LoggerJpaController.getInstance().create( t.getFirst(), t.getSecond(), null ); 
	}

	@Override
	public PpeStrAtr create( PpeStrAtr entity, boolean saveEntity, boolean bulk,
			boolean individualTransaction ) throws Exception {
		if ( ( entity == null ) 
				|| ( entity.getStringAttr() == null ) || ( entity.getProfilePatternElement() == null ) ) { 
			throw new IllegalArgumentException( "PpeStrAtrJpaController.create(): illegal entity param" );
		}
		if ( !saveEntity ) {
			JournalJpaController jjc = JournalJpaController.getInstance();
			entity.setJournal( jjc.create( null, false, false, false ) );
			return entity;
		}		
		Triplet<PpeStrAtr, Short, Boolean> t = new Triplet<>( entity, 
				bulk ? OPERATION_CODE_DML_CREATE_BULK : OPERATION_CODE_DML_CREATE, false );
		startOrmCommand( this, t, individualTransaction );
		return t.getFirst();
	}

	@Override
	public PpeStrAtr update( PpeStrAtr entity, boolean saveEntity, boolean bulk,
			boolean individualTransaction ) throws Exception {
		if ( ( entity == null ) || ( entity.getJournal() == null ) ) {
			throw new IllegalArgumentException( "PpeStrAtrJpaController.update(): illegal entity param " );
		}		
		if ( !saveEntity ) {
			JournalJpaController jjc = JournalJpaController.getInstance();
			entity.setJournal( jjc.update( entity.getJournal(), false, false, false ) );
			return entity;
		}		
		Triplet<PpeStrAtr, Short, Boolean> t = new Triplet<>( entity, 
				bulk ? OPERATION_CODE_DML_UPDATE_BULK : OPERATION_CODE_DML_UPDATE, true );
		startOrmCommand( this, t, individualTransaction );
		return t.getFirst();
	}

	@Override
	public PpeStrAtr delete( PpeStrAtr entity, boolean saveEntity, boolean bulk,
			boolean individualTransaction ) throws Exception {
		if ( ( entity == null ) || ( entity.getJournal() == null ) ) {
			throw new IllegalArgumentException( "PpeStrAtrJpaController.delete(): illegal entity param " );
		}		
		if ( entity.getJournal().getDeleteMoment().before( new Date() ) ) return entity;
		if ( !saveEntity ) {
			JournalJpaController jjc = JournalJpaController.getInstance();
			entity.setJournal( jjc.delete( entity.getJournal(), false, false, false ) );
			return entity;
		}		
		Triplet<PpeStrAtr, Short, Boolean> t = new Triplet<>( entity, 
				bulk ? OPERATION_CODE_DML_UPDATE_BULK : OPERATION_CODE_DML_UPDATE, false );
		startOrmCommand( this, t, individualTransaction );
		return t.getFirst();
	}

	// =================================================================================================================
	// Methods
	// =================================================================================================================
	public static PpeStrAtrJpaController getInstance() {
		return Holder.KEEPER;
	}

	// =================================================================================================================
	// Inner and Anonymous Classes
	// =================================================================================================================
	private static class Holder {
		private static final PpeStrAtrJpaController KEEPER = new PpeStrAtrJpaController();
	}
}