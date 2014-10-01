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
import minos.entities.Catalog;
import minos.entities.Competence;
import minos.entities.Journal;
import minos.utils.ResourceKeeper;
import minos.utils.ResourceKeeper.QType;

public class CompetenceJpaController extends VersionSupportJpaController<Competence> implements OrmCommand {
	// =================================================================================================================
	// Constructors
	// =================================================================================================================
	private CompetenceJpaController() { }

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
	public Pair<Competence, Competence> newVersion( Competence oldEntity, Competence newEntity, boolean saveEntity, 
			boolean bulk, boolean individualTransaction ) throws Exception {
		if ( ( oldEntity == null ) || ( newEntity == null )
				|| ( oldEntity.getStatus() != Competence.STATUS_ACTIVE )
				|| ( oldEntity.getJournal() == null ) 
				|| ( oldEntity.getCatalog() == null ) 
				|| ( newEntity.getCatalog() == null ) ) {
			throw new IllegalArgumentException( "CompetenceJpaController.newVersion(): illegal param" );
		}
		// fill new version of entity
		newEntity.setId( oldEntity.getId() );
		newEntity.setVersion( ( short ) ( oldEntity.getVersion() + 1 ) );
		newEntity.setStatus( Catalog.STATUS_ACTIVE );
		newEntity.setJournal( oldEntity.getJournal() );

		// make Competence's history entity and Journal entity for them
		JournalJpaController jjc = JournalJpaController.getInstance();
		Journal jd = jjc.copy( oldEntity.getJournal(), false, false, false );
		jd = jjc.delete( jd, false, false, false );
		Competence history = new Competence( oldEntity.getName(), oldEntity.getDescription(), oldEntity.getItem(), 
				Competence.STATUS_HISTORY, oldEntity.getVariety(), oldEntity.getVersion(), oldEntity.getCatalog(), 
				null, null, jd );

		Pair<Competence, Competence> p = new Pair<>( history, newEntity );
		if ( !saveEntity ) {
			newEntity.setJournal( jjc.update( newEntity.getJournal(), false, false, false ) );		
			return p;
		}
		Quartet<Competence, Competence, Boolean, Boolean> q = new Quartet<>( p.getFirst(), p.getSecond(), bulk, true );
		startOrmCommand( this, q, individualTransaction );
		p.setFirst( q.getFirst() );
		p.setSecond( q.getSecond() );
		return p;
	}

	@Override
	public Competence create( Competence entity, boolean saveEntity, boolean bulk, 
			boolean individualTransaction ) throws Exception {
		if ( ( entity == null ) || ( entity.getCatalog() == null ) ) {
			throw new IllegalArgumentException( "CompetenceJpaController.create() : illeagl entity argument" );
		}
		entity.setStatus( Competence.STATUS_ACTIVE );		
		entity.setVersion( (short) 1 );		
		if ( !saveEntity ) {			
			JournalJpaController jjc = JournalJpaController.getInstance();
			entity.setJournal( jjc.create( null, false, false, false ) );
			return entity;
		}
		Triplet<Competence, Short, Boolean> t = new Triplet<>( entity, 
				bulk ? OPERATION_CODE_DML_CREATE_BULK : OPERATION_CODE_DML_CREATE, false );
		startOrmCommand( this, t, individualTransaction );
		return t.getFirst();
	}

	@Override
	public Competence update( Competence entity, boolean saveEntity, boolean bulk, 
			boolean individualTransaction ) throws Exception {
		if ( ( entity == null ) || ( entity.getJournal() == null ) ) { 
			throw new IllegalArgumentException( "CompetenceJpaController.edit() : illegal entity argument" );
		}
		if ( !saveEntity ) {		
			JournalJpaController jjc = JournalJpaController.getInstance();
			entity.setJournal( jjc.update( entity.getJournal(), false, false, false ) );
			return entity;
		}
		Triplet<Competence, Short, Boolean> t = new Triplet<>( entity, 
				 bulk ? OPERATION_CODE_DML_UPDATE_BULK : OPERATION_CODE_DML_UPDATE, true );
		startOrmCommand( this, t, individualTransaction );
		return t.getFirst();
	}

	@Override
	public Competence delete( Competence entity, boolean saveEntity, boolean bulk, 
			boolean individualTransaction ) throws Exception {
		if ( ( entity == null ) || ( entity.getJournal() == null ) ) {
			throw new IllegalArgumentException( "CompetenceJpaController.delete() : illegal entity argument" );
		}
		if ( entity.getStatus() != Competence.STATUS_ACTIVE ) return entity;

		entity.setStatus( Competence.STATUS_DELETE );
		if ( !saveEntity ) {
			JournalJpaController jjc = JournalJpaController.getInstance();
			entity.setJournal( jjc.delete( entity.getJournal(), false, false, false ) );			
			return entity;	
		}
		Triplet<Competence, Short, Boolean> t = new Triplet<>( entity, 
				 bulk ? OPERATION_CODE_DML_UPDATE_BULK : OPERATION_CODE_DML_UPDATE, false );
		startOrmCommand( this, t, individualTransaction );
		return t.getFirst();
	}
	
	// =================================================================================================================
	// Methods
	// =================================================================================================================
	public static CompetenceJpaController getInstance() {
		return Holder.KEEPER;
	}
	
	public short getMaxItem( Catalog catalog ) {
		Number num = ( Number ) OrmHelper.executeQueryWithParam( QueryType.JPQL, 
				ResourceKeeper.getQuery( QType.JPQL_MAX_COMPETENCE_ITEM ), 
				new Pair<Object, Object>( "catalog", catalog ) );
		return num == null ? ( short ) 0 : num.shortValue();
	}
	
	private void saveEntity( Object obj ) throws Exception {
		@SuppressWarnings("unchecked")
		Triplet<Competence, Short, Boolean> t = ( Triplet<Competence, Short, Boolean> ) obj;
		boolean newEntity = ( ( t.getSecond() == OPERATION_CODE_DML_CREATE_BULK ) 
				|| ( t.getSecond() == OPERATION_CODE_DML_CREATE ) );
		t.getFirst().setJournal( makeJournal( t.getFirst().getJournal(), t.getSecond(), t.getThird() ) );
		t.setFirst( ( Competence ) ( newEntity ? OrmHelper.create( t.getFirst() ) : OrmHelper.update( t.getFirst() ) ) );
		OrmHelper.getCurrentManager().flush();
		LoggerJpaController.getInstance().create( t.getFirst(), t.getSecond(), null ); 
	}
	
	private void saveAncestorAndHistory( Object obj ) throws Exception {
		@SuppressWarnings("unchecked")
		Quartet<Competence, Competence, Boolean, Boolean> q = ( Quartet<Competence, Competence, Boolean, Boolean> ) obj;
		q.setSecond( update( q.getSecond(), true, q.getThird(), false ) );
		q.getFirst().setAncestor( q.getSecond() );
		q.setFirst( create( q.getFirst(), true, q.getThird(), false ) );
	}

	// =================================================================================================================
	// Inner and Anonymous Classes
	// =================================================================================================================
	private static class Holder {
		private static final CompetenceJpaController KEEPER = new CompetenceJpaController();
	}
}