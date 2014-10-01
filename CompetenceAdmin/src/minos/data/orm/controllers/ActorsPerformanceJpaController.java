package minos.data.orm.controllers;

import static minos.entities.Logger.OPERATION_CODE_DML_CREATE;
import static minos.entities.Logger.OPERATION_CODE_DML_CREATE_BULK;
import static minos.entities.Logger.OPERATION_CODE_DML_UPDATE;
import static minos.entities.Logger.OPERATION_CODE_DML_UPDATE_BULK;
import ru.gedr.util.tuple.Triplet;
import minos.data.orm.OrmCommand;
import minos.data.orm.OrmHelper;
import minos.entities.ActorsPerformance;

public class ActorsPerformanceJpaController extends BasisJpaController<ActorsPerformance> implements OrmCommand {
	// =================================================================================================================
	// Constructors
	// =================================================================================================================
	private ActorsPerformanceJpaController() { }

	// =================================================================================================================
	// Methods for/from SuperClass/Interfaces
	// =================================================================================================================
	@Override
	public void execute( Object obj ) throws Exception {
		@SuppressWarnings("unchecked")
		Triplet<ActorsPerformance, Short, Boolean> t = ( Triplet<ActorsPerformance, Short, Boolean> ) obj;
		boolean flag = ( ( t.getSecond() == OPERATION_CODE_DML_CREATE_BULK ) 
				|| ( t.getSecond() == OPERATION_CODE_DML_CREATE ) );
		t.setFirst( ( ActorsPerformance ) ( flag ? OrmHelper.create( t.getFirst() ) : OrmHelper.update( t.getFirst() ) ) );
		OrmHelper.getCurrentManager().flush();
		LoggerJpaController.getInstance().create( t.getFirst(), t.getSecond(), null ); 
	}

	@Override
	public ActorsPerformance create( ActorsPerformance entity, boolean saveEntity, boolean bulk, 
			boolean individualTransaction ) throws Exception {
		if ( ( entity == null ) || ( entity.getActor() == null ) || ( entity.getProfilePatternElement() == null ) ) { 
			throw new IllegalArgumentException( "ActorsPerformanceJpaController.create(): illegal entity param" );
		}
		if ( !saveEntity ) return entity;

		Triplet<ActorsPerformance, Short, Boolean> t = new Triplet<>( entity, 
				bulk ? OPERATION_CODE_DML_CREATE_BULK : OPERATION_CODE_DML_CREATE, false );
		startOrmCommand( this, t, individualTransaction );
		return t.getFirst();
	}

	@Override
	public ActorsPerformance update( ActorsPerformance entity, boolean saveEntity, boolean bulk, 
			boolean individualTransaction ) throws Exception {
		if ( entity == null ) {
			throw new IllegalArgumentException( "ActorsPerformanceJpaController.update(): illegal entity param " );
		}		
		if ( !saveEntity ) return entity;
		Triplet<ActorsPerformance, Short, Boolean> t = new Triplet<>( entity, 
				bulk ? OPERATION_CODE_DML_UPDATE_BULK : OPERATION_CODE_DML_UPDATE, true );
		startOrmCommand( this, t, individualTransaction );
		return t.getFirst();

	}

	@Override
	public ActorsPerformance delete( ActorsPerformance entity, boolean saveEntity, boolean bulk, 
			boolean individualTransaction ) throws Exception {
		throw new UnsupportedOperationException( "ActorsInfoJpaControllers.delete(): operation delete not supported" );
	}

	// =================================================================================================================
	// Methods
	// =================================================================================================================
	public static ActorsPerformanceJpaController getInstance() {
		return Holder.KEEPER;
	}

	// =================================================================================================================
	// Inner and Anonymous Classes
	// =================================================================================================================
	private static class Holder {
		private static final ActorsPerformanceJpaController KEEPER = new ActorsPerformanceJpaController();
	}
}
