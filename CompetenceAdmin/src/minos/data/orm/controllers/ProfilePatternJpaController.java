package minos.data.orm.controllers;

import static minos.entities.Logger.OPERATION_CODE_DML_CREATE;
import static minos.entities.Logger.OPERATION_CODE_DML_CREATE_BULK;
import static minos.entities.Logger.OPERATION_CODE_DML_UPDATE;
import static minos.entities.Logger.OPERATION_CODE_DML_UPDATE_BULK;
import minos.data.orm.OrmCommand;
import minos.data.orm.OrmHelper;
import minos.data.orm.OrmHelper.QueryType;
import minos.entities.Catalog;
import minos.entities.ProfilePattern;
import minos.utils.ResourceKeeper;
import minos.utils.ResourceKeeper.QType;
import ru.gedr.util.tuple.Pair;
import ru.gedr.util.tuple.Triplet;

public class ProfilePatternJpaController extends BasisJpaController<ProfilePattern> implements OrmCommand {
	// =================================================================================================================
	// Constructors
	// =================================================================================================================
	private ProfilePatternJpaController() { }

	// =================================================================================================================
	// Methods for/from SuperClass/Interfaces
	// =================================================================================================================
	@Override
	public void execute( Object obj ) throws Exception {
		@SuppressWarnings("unchecked")
		Triplet<ProfilePattern, Short, Boolean> t = ( Triplet<ProfilePattern, Short, Boolean> ) obj;
		boolean newEntity = ( ( t.getSecond() == OPERATION_CODE_DML_CREATE_BULK ) 
				|| ( t.getSecond() == OPERATION_CODE_DML_CREATE ) );
		t.getFirst().setJournal( makeJournal( t.getFirst().getJournal(), t.getSecond(), t.getThird() ) );
		t.setFirst( ( ProfilePattern ) ( newEntity ? OrmHelper.create( t.getFirst() ) 
				: OrmHelper.update( t.getFirst() ) ) );
		OrmHelper.getCurrentManager().flush();
		LoggerJpaController.getInstance().create( t.getFirst(), t.getSecond(), null ); 
	}

	@Override
	public ProfilePattern create( ProfilePattern entity, boolean saveEntity, boolean bulk, 
			boolean individualTransaction ) throws Exception {
		if ( ( entity == null ) || ( entity.getFilialMask() == null ) || ( entity.getCatalog() == null ) ) {
			throw new IllegalArgumentException( "ProfilePatternJpaController.create(): illegal entity param " );
		}
		entity.setStatus( ProfilePattern.STATUS_BUILDING );
		if ( !saveEntity ) {
			JournalJpaController jjc = JournalJpaController.getInstance();
			entity.setJournal( jjc.create( null, false, false, false ) );
			return entity;
		}		
		Triplet<ProfilePattern, Short, Boolean> t = new Triplet<>( entity, 
				bulk ? OPERATION_CODE_DML_CREATE_BULK : OPERATION_CODE_DML_CREATE, false );
		startOrmCommand( this, t, individualTransaction );
		return t.getFirst();
	}

	@Override
	public ProfilePattern update( ProfilePattern entity, boolean saveEntity, boolean bulk, 
			boolean individualTransaction ) throws Exception {
		if ( ( entity == null ) || ( entity.getJournal() == null ) ) {
			throw new IllegalArgumentException( "ProfilePatternJpaController.update(): illegal entity param " );
		}		
		if ( !saveEntity ) {
			JournalJpaController jjc = JournalJpaController.getInstance();
			entity.setJournal( jjc.update( entity.getJournal(), false, false, false ) );
			return entity;
		}		
		Triplet<ProfilePattern, Short, Boolean> t = new Triplet<>( entity, 
				bulk ? OPERATION_CODE_DML_UPDATE_BULK : OPERATION_CODE_DML_UPDATE, true );
		startOrmCommand( this, t, individualTransaction );
		return t.getFirst();
	}

	@Override
	public ProfilePattern delete( ProfilePattern entity, boolean saveEntity, boolean bulk, 
			boolean individualTransaction ) throws Exception {
		if ( ( entity == null ) || ( entity.getJournal() == null ) ) {
			throw new IllegalArgumentException( "ProfilePatternJpaController.delete(): illegal entity param " );
		}		
		if ( entity.getStatus() == ProfilePattern.STATUS_DELETE ) return entity;

		entity.setStatus( ProfilePattern.STATUS_DELETE );
		if ( !saveEntity ) {
			JournalJpaController jjc = JournalJpaController.getInstance();
			entity.setJournal( jjc.delete( entity.getJournal(), false, false, false ) );			
			return entity;	
		}
		Triplet<ProfilePattern, Short, Boolean> t = new Triplet<>( entity, 
				 bulk ? OPERATION_CODE_DML_UPDATE_BULK : OPERATION_CODE_DML_UPDATE, false );
		startOrmCommand( this, t, individualTransaction );
		return t.getFirst();
	}

	// =================================================================================================================
	// Methods
	// =================================================================================================================
	public static ProfilePatternJpaController getInstance() {
		return Holder.KEEPER;
	}
	
	public short getMaxItem( Catalog catalog ) {
		Number num  = ( Number ) OrmHelper.executeQueryWithParam( QueryType.JPQL, 
				ResourceKeeper.getQuery( QType.JPQL_MAX_PP_ITEM ), 
				new Pair<Object, Object>( "catalog", catalog ) ); 
		return num == null ? ( short ) 0 : num.shortValue();
	}

	// =================================================================================================================
	// Inner and Anonymous Classes
	// =================================================================================================================
	private static class Holder {
		private static final ProfilePatternJpaController KEEPER = new ProfilePatternJpaController();
	}
}