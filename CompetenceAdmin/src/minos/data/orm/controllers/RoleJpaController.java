package minos.data.orm.controllers;

import org.apache.commons.lang.NullArgumentException;

import ru.gedr.util.tuple.Pair;
import static minos.entities.Logger.OPERATION_CODE_DML_CREATE;
import static minos.entities.Logger.OPERATION_CODE_DML_CREATE_BULK;
import static minos.entities.Logger.OPERATION_CODE_DML_UPDATE;
import static minos.entities.Logger.OPERATION_CODE_DML_UPDATE_BULK;
import static minos.entities.Logger.OPERATION_CODE_DML_DELETE;
import static minos.entities.Logger.OPERATION_CODE_DML_DELETE_BULK;
import minos.data.orm.OrmCommand;
import minos.data.orm.OrmHelper;
import minos.entities.Role;
import minos.entities.Person;
import minos.utils.ResourceKeeper;
import minos.utils.ResourceKeeper.OType;

public class RoleJpaController extends BasisJpaController<Role> implements OrmCommand {
	// =================================================================================================================
	// Constructors
	// =================================================================================================================
	private RoleJpaController() { }

	// =================================================================================================================
	// Methods for/from SuperClass/Interfaces
	// =================================================================================================================
	@Override
	public Role create( Role entity, boolean saveEntity, boolean bulk,
			boolean individualTransaction ) throws Exception {
		if ( entity == null ) throw new NullArgumentException( "RoleJpaController.create(): illegal entity" );
		if ( !saveEntity ) return entity;			
		Pair<Role, Short> p = new Pair<>( entity, bulk ? OPERATION_CODE_DML_CREATE_BULK : OPERATION_CODE_DML_CREATE );
		startOrmCommand( this, p, individualTransaction );
		return p.getFirst();
	}

	@Override
	public Role update( Role entity, boolean saveEntity, boolean bulk,
			boolean individualTransaction ) throws Exception {
		if ( entity == null ) throw new NullArgumentException( "RoleJpaController.create(): illegal entity" );
		if ( !saveEntity ) return entity;			
		Pair<Role, Short> p = new Pair<>( entity, bulk ? OPERATION_CODE_DML_UPDATE_BULK : OPERATION_CODE_DML_UPDATE );
		startOrmCommand( this, p, individualTransaction );
		return p.getFirst();
	}

	@Override
	public Role delete( Role entity, boolean saveEntity, boolean bulk,
			boolean individualTransaction ) throws Exception {
		if ( entity == null ) throw new NullArgumentException( "RoleJpaController.create(): illegal entity" );
		if ( !saveEntity ) return entity;		
		Pair<Role, Short> p = new Pair<>( entity, bulk ? OPERATION_CODE_DML_DELETE_BULK : OPERATION_CODE_DML_DELETE );
		startOrmCommand( this, p, individualTransaction );
		return p.getFirst();
	}

	@Override
	public void execute( Object obj ) throws Exception {
		@SuppressWarnings("unchecked")
		Pair<Role, Short> p = ( Pair<Role, Short> ) obj;
		SimpleJournal j = new SimpleJournal( ( String ) ResourceKeeper.getObject( OType.CURRENT_LOGIN ),				 
				( String ) ResourceKeeper.getObject( OType.CURRENT_HOST ), 
				( ( Person ) ResourceKeeper.getObject( OType.CURRENT_PERSON ) ).getId() );
		boolean newEntity = ( ( p.getSecond() == OPERATION_CODE_DML_CREATE ) 
				|| ( p.getSecond() == OPERATION_CODE_DML_CREATE_BULK ) );

		p.setFirst( ( Role ) ( newEntity ? OrmHelper.create( p.getFirst() ) : OrmHelper.update( p.getFirst() ) ) );
		OrmHelper.getCurrentManager().flush();			
		LoggerJpaController.getInstance().create( p.getFirst(), p.getSecond(), j.toJson() );
		
		if ( ( p.getSecond() == OPERATION_CODE_DML_DELETE ) 
				|| ( p.getSecond() == OPERATION_CODE_DML_DELETE_BULK ) ) OrmHelper.delete( p.getFirst() );
	}
	
	// =================================================================================================================
	// Methods
	// =================================================================================================================
	public static RoleJpaController getInstance() {
		return Holder.KEEPER;
	}

	// =================================================================================================================
	// Inner and Anonymous Classes
	// =================================================================================================================
	private static class Holder {
		private static final RoleJpaController KEEPER = new RoleJpaController();
	}
}