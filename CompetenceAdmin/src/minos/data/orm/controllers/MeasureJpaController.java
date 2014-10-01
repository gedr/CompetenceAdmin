package minos.data.orm.controllers;

import static minos.entities.Logger.OPERATION_CODE_DML_CREATE;
import static minos.entities.Logger.OPERATION_CODE_DML_CREATE_BULK;
import static minos.entities.Logger.OPERATION_CODE_DML_UPDATE;
import static minos.entities.Logger.OPERATION_CODE_DML_UPDATE_BULK;

import java.util.Date;

import minos.data.orm.OrmCommand;
import minos.data.orm.OrmHelper;
import minos.entities.Measure;
import ru.gedr.util.tuple.Triplet;

public class MeasureJpaController extends BasisJpaController<Measure> implements OrmCommand  {
	// =================================================================================================================
	// Constructors
	// =================================================================================================================
	private MeasureJpaController() { }

	// =================================================================================================================
	// Methods for/from SuperClass/Interfaces
	// =================================================================================================================
	@Override
	public void execute( Object obj ) throws Exception {
		@SuppressWarnings("unchecked")
		Triplet<Measure, Short, Boolean> t = ( Triplet<Measure, Short, Boolean> ) obj;
		boolean newEntity = ( ( t.getSecond() == OPERATION_CODE_DML_CREATE_BULK ) 
				|| ( t.getSecond() == OPERATION_CODE_DML_CREATE ) );
		t.getFirst().setJournal( makeJournal( t.getFirst().getJournal(), t.getSecond(), t.getThird() ) );
		t.setFirst( ( Measure ) ( newEntity ? OrmHelper.create( t.getFirst() ) : OrmHelper.update( t.getFirst() ) ) );
		OrmHelper.getCurrentManager().flush();
		LoggerJpaController.getInstance().create( t.getFirst(), t.getSecond(), null ); 
	}

	@Override
	public Measure create( Measure entity, boolean saveEntity, boolean bulk, 
			boolean individualTransaction ) throws Exception {
		if ( ( entity == null ) || ( entity.getBranchOffice() == null ) 
				|| ( entity.getStart() == null ) || ( entity.getStop() == null ) ) {
			throw new IllegalArgumentException( "MeasureJpaController.create(): illegal entity param" );
		}
		if ( !saveEntity ) {
			JournalJpaController jjc = JournalJpaController.getInstance();
			entity.setJournal( jjc.create( null, false, false, false ) );
			return entity;
		}		
		Triplet<Measure, Short, Boolean> t = new Triplet<>( entity, 
				bulk ? OPERATION_CODE_DML_CREATE_BULK : OPERATION_CODE_DML_CREATE, false );
		startOrmCommand( this, t, individualTransaction );
		return t.getFirst();
	}

	@Override
	public Measure update( Measure entity, boolean saveEntity, boolean bulk, 
			boolean individualTransaction ) throws Exception {
		if ( ( entity == null ) || ( entity.getJournal() == null ) ) {
			throw new IllegalArgumentException( "MeasureJpaController.create(): illegal entity param " );
		}		
		if ( !saveEntity ) {
			JournalJpaController jjc = JournalJpaController.getInstance();
			entity.setJournal( jjc.update( entity.getJournal(), false, false, false ) );
			return entity;
		}		
		Triplet<Measure, Short, Boolean> t = new Triplet<>( entity, 
				bulk ? OPERATION_CODE_DML_UPDATE_BULK : OPERATION_CODE_DML_UPDATE, true );
		startOrmCommand( this, t, individualTransaction );
		return t.getFirst();
	}

	@Override
	public Measure delete( Measure entity, boolean saveEntity, boolean bulk, 
			boolean individualTransaction ) throws Exception {
		if ( ( entity == null ) || ( entity.getJournal() == null ) ) {
			throw new IllegalArgumentException( "MeasureJpaController.delete(): illegal entity param " );
		}
		if ( entity.getJournal().getDeleteMoment().before( new Date() ) ) return entity;

		if ( !saveEntity ) {
			JournalJpaController jjc = JournalJpaController.getInstance();
			entity.setJournal( jjc.delete( entity.getJournal(), false, false, false ) );			
			return entity;	
		}
		Triplet<Measure, Short, Boolean> t = new Triplet<>( entity, 
				 bulk ? OPERATION_CODE_DML_UPDATE_BULK : OPERATION_CODE_DML_UPDATE, false );
		startOrmCommand( this, t, individualTransaction );
		return t.getFirst();
	}
	
	// =================================================================================================================
	// Methods
	// =================================================================================================================
	public static MeasureJpaController getInstance() {
		return Holder.KEEPER;
	}

	// =================================================================================================================
	// Inner and Anonymous Classes
	// =================================================================================================================
	private static class Holder {
		private static final MeasureJpaController KEEPER = new MeasureJpaController();
	}
}