package minos.data.orm.controllers;

import static minos.entities.Logger.OPERATION_CODE_DML_CREATE;
import static minos.entities.Logger.OPERATION_CODE_DML_CREATE_BULK;
import static minos.entities.Logger.OPERATION_CODE_DML_UPDATE;
import static minos.entities.Logger.OPERATION_CODE_DML_UPDATE_BULK;
import ru.gedr.util.tuple.Pair;
import ru.gedr.util.tuple.Triplet;
import minos.data.orm.OrmCommand;
import minos.data.orm.OrmHelper;
import minos.data.orm.OrmHelper.QueryType;
import minos.entities.ProfilePattern;
import minos.entities.ProfilePatternElement;
import minos.utils.ResourceKeeper;
import minos.utils.ResourceKeeper.QType;

public class ProfilePatternElementJpaController extends BasisJpaController<ProfilePatternElement> 
implements OrmCommand {
	// =================================================================================================================
	// Constructors
	// =================================================================================================================
	private ProfilePatternElementJpaController() { }

	// =================================================================================================================
	// Methods for/from SuperClass/Interfaces
	// =================================================================================================================
	@Override
	public void execute( Object obj ) throws Exception {
		@SuppressWarnings("unchecked")
		Triplet<ProfilePatternElement, Short, Boolean> t = ( Triplet<ProfilePatternElement, Short, Boolean> ) obj;
		boolean newEntity = ( ( t.getSecond() == OPERATION_CODE_DML_CREATE_BULK ) 
				|| ( t.getSecond() == OPERATION_CODE_DML_CREATE ) );
		t.getFirst().setJournal( makeJournal( t.getFirst().getJournal(), t.getSecond(), t.getThird() ) );
		t.setFirst( ( ProfilePatternElement ) ( newEntity ? OrmHelper.create( t.getFirst() ) 
				: OrmHelper.update( t.getFirst() ) ) );
		OrmHelper.getCurrentManager().flush();
		LoggerJpaController.getInstance().create( t.getFirst(), t.getSecond(), null ); 
	}

	@Override
	public ProfilePatternElement create( ProfilePatternElement entity, boolean saveEntity, boolean bulk, 
			boolean individualTransaction ) throws Exception {
		if ( ( entity == null ) || ( entity.getCompetence() == null ) || ( entity.getMinLevel() == null ) 
				|| ( entity.getProfilePattern() == null ) ) {
			throw new IllegalArgumentException( "ProfilePatternElementJpaController.create(): illegal entity param" );
		}
		/*
		System.out.println( "entity.getCompetence().getVariety() : " + entity.getCompetence().getVariety());
		System.out.println( "entity.getProfilePattern() : " + entity.getProfilePattern() );
		Object o = OrmHelper.executeQueryWithParam( QueryType.JPQL, 
				ResourceKeeper.getQuery( QType.JPQL_MAX_PPE_ITEM ), 
				new Pair<Object, Object>( "variety", entity.getCompetence().getVariety() ), 
				new Pair<Object, Object>( "pp", entity.getProfilePattern() ) );  
		System.out.println( " 0 = " + o );
		short item = ( (Number)o).shortValue();
		entity.setItem( ++item );
		entity.setStatus( ProfilePatternElement.STATUS_BUILDING );
*/
		if ( !saveEntity ) {
			JournalJpaController jjc = JournalJpaController.getInstance();
			entity.setJournal( jjc.create( null, false, false, false ) );
			return entity;
		}		
		Triplet<ProfilePatternElement, Short, Boolean> t = new Triplet<>( entity, 
				bulk ? OPERATION_CODE_DML_CREATE_BULK : OPERATION_CODE_DML_CREATE, false );
		startOrmCommand( this, t, individualTransaction );
		return t.getFirst();
	}

	@Override
	public ProfilePatternElement update( ProfilePatternElement entity, boolean saveEntity, boolean bulk, 
			boolean individualTransaction ) throws Exception {
		if ( ( entity == null ) || ( entity.getJournal() == null ) ) { 
			throw new IllegalArgumentException( "ProfilePatternElementJpaController.update(): illegal entity param" );
		}		
		if ( !saveEntity ) {
			JournalJpaController jjc = JournalJpaController.getInstance();
			entity.setJournal( jjc.update( entity.getJournal(), false, false, false ) );
			return entity;
		}		
		Triplet<ProfilePatternElement, Short, Boolean> t = new Triplet<>( entity, 
				bulk ? OPERATION_CODE_DML_UPDATE_BULK : OPERATION_CODE_DML_UPDATE, true );
		startOrmCommand( this, t, individualTransaction );
		return t.getFirst();
	}

	@Override
	public ProfilePatternElement delete( ProfilePatternElement entity, boolean saveEntity, boolean bulk, 
			boolean individualTransaction ) throws Exception {
		if ( ( entity == null ) || ( entity.getJournal() == null ) ) { 
			throw new IllegalArgumentException( "ProfilePatternElementJpaController.delete(): illegal entity param" );
		}		
		if ( entity.getStatus() != ProfilePatternElement.STATUS_BUILDING ) return entity;
		
		if ( !saveEntity ) {
			JournalJpaController jjc = JournalJpaController.getInstance();
			entity.setJournal( jjc.delete( entity.getJournal(), false, false, false ) );
			return entity;
		}		
		entity.setStatus( ProfilePatternElement.STATUS_DELETE );
		Triplet<ProfilePatternElement, Short, Boolean> t = new Triplet<>( entity, 
				bulk ? OPERATION_CODE_DML_UPDATE_BULK : OPERATION_CODE_DML_UPDATE, false );
		startOrmCommand( this, t, individualTransaction );
		return t.getFirst();
	}
	
	// =================================================================================================================
	// Methods
	// =================================================================================================================
	public static ProfilePatternElementJpaController getInstance() {
		return Holder.KEEPER;
	}
	
	public short getMaxItem( short variety, ProfilePattern pp ) {
		Number num = ( Number ) OrmHelper.executeQueryWithParam( QueryType.JPQL, 
				ResourceKeeper.getQuery( QType.JPQL_MAX_PPE_ITEM ), 
				new Pair<Object, Object>( "variety", variety ), 
				new Pair<Object, Object>( "pp", pp ) ); 
		return num == null ? ( short ) 0 : num.shortValue();
	}

	// =================================================================================================================
	// Inner and Anonymous Classes
	// =================================================================================================================
	private static class Holder {
		private static final ProfilePatternElementJpaController KEEPER = new ProfilePatternElementJpaController();
	}
}