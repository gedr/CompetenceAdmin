package minos.data.orm.controllers;

import static minos.entities.Logger.OPERATION_CODE_DML_CREATE;
import static minos.entities.Logger.OPERATION_CODE_DML_CREATE_BULK;
import static minos.entities.Logger.OPERATION_CODE_DML_UPDATE;
import static minos.entities.Logger.OPERATION_CODE_DML_UPDATE_BULK;
import ru.gedr.util.tuple.Pair;
import ru.gedr.util.tuple.Quartet;
import ru.gedr.util.tuple.Triplet;
import ru.gedr.util.tuple.Tuple;
import ru.gedr.util.tuple.Tuple.TupleType;
import minos.data.orm.OrmCommand;
import minos.data.orm.OrmHelper;
import minos.data.orm.OrmHelper.QueryType;
import minos.entities.Competence;
import minos.entities.Indicator;
import minos.entities.Journal;
import minos.entities.Level;
import minos.utils.ResourceKeeper;
import minos.utils.ResourceKeeper.QType;

public class IndicatorJpaController extends VersionSupportJpaController<Indicator> implements OrmCommand {
	// =================================================================================================================
	// Constructors
	// =================================================================================================================
	private IndicatorJpaController() { }

	// =================================================================================================================
	// Methods for/from SuperClass/Interfaces
	// =================================================================================================================
	@Override
	public void execute( Object obj ) throws Exception {
		Tuple t = ( Tuple ) obj;
		if ( t.getType() == TupleType.TRIPLET ) saveEntity( obj );
		if ( t.getType() == TupleType.QUARTET ) saveAncestorAndHistory( obj );
	}

	@Override
	public Pair<Indicator, Indicator> newVersion( Indicator oldEntity, Indicator newEntity, boolean saveEntity, 
			boolean bulk, boolean individualTransaction ) throws Exception {
		if ( ( oldEntity == null ) || ( newEntity == null )
				|| ( oldEntity.getStatus() != Competence.STATUS_ACTIVE )
				|| ( oldEntity.getJournal() == null ) 
				|| ( oldEntity.getLevel() == null )
				|| ( oldEntity.getCompetence() == null )
 				|| ( newEntity.getLevel() == null )
				|| ( newEntity.getCompetence() == null ) ) {
			throw new IllegalArgumentException( "IndicatorJpaController.newVersion(): illegal param" );
		}
		// fill new version of entity
		newEntity.setId( oldEntity.getId() );
		newEntity.setVersion( ( short ) ( oldEntity.getVersion() + 1 ) );
		newEntity.setStatus( Indicator.STATUS_ACTIVE );
		newEntity.setJournal( oldEntity.getJournal() );

		// make Competence's history entity and Journal entity for them
		JournalJpaController jjc = JournalJpaController.getInstance();
		Journal jd = jjc.copy( oldEntity.getJournal(), false, false, false );
		jd = jjc.delete( jd, false, false, false );
		Indicator history = new Indicator( oldEntity.getName(), oldEntity.getItem(), Indicator.STATUS_HISTORY, 
				oldEntity.getVersion(), oldEntity.getCompetence(), oldEntity.getLevel(), null, jd );
				
		Pair<Indicator, Indicator> p = new Pair<>( history, newEntity );
		if ( !saveEntity ) {
			newEntity.setJournal( jjc.update( newEntity.getJournal(), false, false, false ) );		
			return p;
		}
		Quartet<Indicator, Indicator, Boolean, Boolean> q = new Quartet<>( p.getFirst(), p.getSecond(), bulk, true );
		startOrmCommand( this, q, individualTransaction );
		p.setFirst( q.getFirst() );
		p.setSecond( q.getSecond() );
		return p;
	}

	@Override
	public Indicator create( Indicator entity, boolean saveEntity, boolean bulk,
			boolean individualTransaction ) throws Exception {
		if ( ( entity == null ) || ( entity.getCompetence() == null ) || ( entity.getLevel() == null ) ) {
			throw new IllegalArgumentException( "IndicatorJpaController.create() : illegal entity argument" );
		}
		entity.setStatus( Indicator.STATUS_ACTIVE );
		entity.setVersion( (short) 1 );		
		if ( !saveEntity ) {			
			JournalJpaController jjc = JournalJpaController.getInstance();
			entity.setJournal( jjc.create( null, false, false, false ) );
			return entity;
		}
		Triplet<Indicator, Short, Boolean> t = new Triplet<>( entity, 
				bulk ? OPERATION_CODE_DML_CREATE_BULK : OPERATION_CODE_DML_CREATE, false );
		startOrmCommand( this, t, individualTransaction );
		return t.getFirst();
	}

	@Override
	public Indicator update( Indicator entity, boolean saveEntity, boolean bulk,
			boolean individualTransaction ) throws Exception {
		if ( ( entity == null ) || ( entity.getJournal() == null ) ) { 
			throw new IllegalArgumentException( "IndicatorJpaController.edit() : illegal entity argument" );
		}
		if ( !saveEntity ) {			
			JournalJpaController jjc = JournalJpaController.getInstance();
			entity.setJournal( jjc.update( entity.getJournal(), false, false, false ) );
			return entity;
		}
		Triplet<Indicator, Short, Boolean> t = new Triplet<>( entity, 
				 bulk ? OPERATION_CODE_DML_UPDATE_BULK : OPERATION_CODE_DML_UPDATE, true );
		startOrmCommand( this, t, individualTransaction );
		return t.getFirst();
	}

	@Override
	public Indicator delete( Indicator entity, boolean saveEntity, boolean bulk,
			boolean individualTransaction ) throws Exception {
		if ( ( entity == null ) || ( entity.getJournal() == null ) ) { 
			throw new IllegalArgumentException( "IndicatorJpaController.edit() : illegal entity argument" );
		}
		if ( entity.getStatus() != Indicator.STATUS_ACTIVE ) return entity;

		entity.setStatus( Indicator.STATUS_DELETE );
		if ( !saveEntity ) {
			JournalJpaController jjc = JournalJpaController.getInstance();
			entity.setJournal( jjc.delete( entity.getJournal(), false, false, false ) );			
			return entity;	
		}
		Triplet<Indicator, Short, Boolean> t = new Triplet<>( entity, 
				 bulk ? OPERATION_CODE_DML_UPDATE_BULK : OPERATION_CODE_DML_UPDATE, false );
		startOrmCommand( this, t, individualTransaction );
		return t.getFirst();
	}
	
	// =================================================================================================================
	// Methods
	// =================================================================================================================
	public static IndicatorJpaController getInstance() {
		return Holder.KEEPER;
	}
	
	public short getMaxItem( Competence competence, Level level ) {
		Number num = ( Number ) OrmHelper.executeQueryWithParam( QueryType.JPQL, 
				ResourceKeeper.getQuery( QType.JPQL_MAX_INDICATOR_ITEM ), 
				new Pair<Object, Object>( "competence", competence ),  
				new Pair<Object, Object>( "level", level ) );
		return num == null ? ( short ) 0 : num.shortValue();
	}
	
	private void saveEntity( Object obj ) throws Exception {
		@SuppressWarnings("unchecked")
		Triplet<Indicator, Short, Boolean> t = ( Triplet<Indicator, Short, Boolean> ) obj;
		boolean newEntity = ( ( t.getSecond() == OPERATION_CODE_DML_CREATE_BULK ) 
				|| ( t.getSecond() == OPERATION_CODE_DML_CREATE ) );
		t.getFirst().setJournal( makeJournal( t.getFirst().getJournal(), t.getSecond(), t.getThird() ) );
		t.setFirst( ( Indicator ) ( newEntity ? OrmHelper.create( t.getFirst() ) : OrmHelper.update( t.getFirst() ) ) );
		OrmHelper.getCurrentManager().flush();
		LoggerJpaController.getInstance().create( t.getFirst(), t.getSecond(), null ); 
	}
	
	private void saveAncestorAndHistory( Object obj ) throws Exception {
		@SuppressWarnings("unchecked")
		Quartet<Indicator, Indicator, Boolean, Boolean> q = ( Quartet<Indicator, Indicator, Boolean, Boolean> ) obj;
		q.setSecond( update( q.getSecond(), true, q.getThird(), false ) );
		q.getFirst().setAncestor( q.getSecond() );
		q.setFirst( create( q.getFirst(), true, q.getThird(), false ) );
	}

	// =================================================================================================================
	// Inner and Anonymous Classes
	// =================================================================================================================
	private static class Holder {
		private static final IndicatorJpaController KEEPER = new IndicatorJpaController();
	}
}