package minos.data.orm.controllers;

import static minos.entities.Logger.OPERATION_CODE_DML_CREATE;
import static minos.entities.Logger.OPERATION_CODE_DML_CREATE_BULK;
import static minos.entities.Logger.OPERATION_CODE_DML_UPDATE;
import static minos.entities.Logger.OPERATION_CODE_DML_UPDATE_BULK;
import static minos.entities.Logger.OPERATION_CODE_DML_DELETE;
import static minos.entities.Logger.OPERATION_CODE_DML_DELETE_BULK;

import org.apache.commons.lang.NullArgumentException;

import ru.gedr.util.tuple.Pair;
import minos.data.orm.OrmCommand;
import minos.data.orm.OrmHelper;
import minos.entities.Person;
import minos.entities.PersonAddon;
import minos.utils.ResourceKeeper;
import minos.utils.ResourceKeeper.OType;

public class PersonAddonJpaController extends BasisJpaController<PersonAddon> implements OrmCommand {
	// =================================================================================================================
	// Constructors
	// =================================================================================================================
	private PersonAddonJpaController() { }

	// =================================================================================================================
	// Methods for/from SuperClass/Interfaces
	// =================================================================================================================
	@Override
	public PersonAddon create( PersonAddon entity, boolean saveEntity, boolean bulk, 
			boolean individualTransaction ) throws Exception {
		if ( entity == null ) throw new NullArgumentException( "PersonAddonJpaController.create(): illegal entity" );
		if ( !saveEntity ) return entity;			
		Pair<PersonAddon, Short> p = new Pair<>( entity, 
				bulk ? OPERATION_CODE_DML_CREATE_BULK : OPERATION_CODE_DML_CREATE );
		startOrmCommand( this, p, individualTransaction );
		return p.getFirst();
	}

	@Override
	public PersonAddon update( PersonAddon entity, boolean saveEntity, boolean bulk, 
			boolean individualTransaction ) throws Exception {
		if ( entity == null ) throw new NullArgumentException( "PersonAddonJpaController.create(): illegal entity" );
		if ( !saveEntity ) return entity;			
		Pair<PersonAddon, Short> p = new Pair<>( entity, 
				bulk ? OPERATION_CODE_DML_UPDATE_BULK : OPERATION_CODE_DML_UPDATE );
		startOrmCommand( this, p, individualTransaction );
		return p.getFirst();
	}

	@Override
	public PersonAddon delete( PersonAddon entity, boolean saveEntity, boolean bulk, 
			boolean individualTransaction ) throws Exception {
		if ( entity == null ) throw new NullArgumentException( "PersonAddonJpaController.create(): illegal entity" );
		if ( !saveEntity ) return entity;			
		Pair<PersonAddon, Short> p = new Pair<>( entity, 
				bulk ? OPERATION_CODE_DML_DELETE_BULK : OPERATION_CODE_DML_DELETE );
		startOrmCommand( this, p, individualTransaction );
		return p.getFirst();
	}

	@Override
	public void execute( Object obj ) throws Exception {
		@SuppressWarnings("unchecked")
		Pair<PersonAddon, Short> p = ( Pair<PersonAddon, Short> ) obj;
		SimpleJournal j = new SimpleJournal( ( String ) ResourceKeeper.getObject( OType.CURRENT_LOGIN ),				 
				( String ) ResourceKeeper.getObject( OType.CURRENT_HOST ), 
				( ( Person ) ResourceKeeper.getObject( OType.CURRENT_PERSON ) ).getId() );
		boolean flag = ( ( p.getSecond() == OPERATION_CODE_DML_CREATE ) 
				|| ( p.getSecond() == OPERATION_CODE_DML_CREATE_BULK ) );
		p.setFirst( ( PersonAddon ) ( flag ? OrmHelper.create( p.getFirst() ) : OrmHelper.update( p.getFirst() ) ) );
		OrmHelper.getCurrentManager().flush();			
		LoggerJpaController.getInstance().create( p.getFirst(), p.getSecond(), j.toJson() );
		if ( ( p.getSecond() == OPERATION_CODE_DML_DELETE ) 
				|| ( p.getSecond() == OPERATION_CODE_DML_DELETE_BULK ) ) OrmHelper.delete( p.getFirst() );
	}

	// =================================================================================================================
	// Methods
	// =================================================================================================================
	public static PersonAddonJpaController getInstance() {
		return Holder.KEEPER;
	}

	// =================================================================================================================
	// Inner and Anonymous Classes
	// =================================================================================================================
	private static class Holder {
		private static final PersonAddonJpaController KEEPER = new PersonAddonJpaController();
	}
}