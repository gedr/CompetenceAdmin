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
import minos.entities.Journal;
import minos.utils.ResourceKeeper;
import minos.utils.ResourceKeeper.QType;

public class CatalogJpaController extends VersionSupportJpaController<Catalog> implements OrmCommand {
	// =================================================================================================================
	// Constructors
	// =================================================================================================================
	private CatalogJpaController() { 
		super();
	}

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
	public Catalog create( Catalog entity, boolean saveEntity, boolean bulk,
			boolean individualTransaction ) throws Exception {
		if ( ( entity == null ) || ( entity.getParentCatalog() == null ) ) {
			throw new IllegalArgumentException( "CatalogJpaController.create(): illegal entity " );
		}		
		entity.setVariety( entity.getParentCatalog().getVariety() );

		if ( !saveEntity ) {			
			JournalJpaController jjc = JournalJpaController.getInstance();
			entity.setJournal( entity.getJournal() == null ? jjc.create( null, false, false, false ) 
					: jjc.copy( entity.getJournal(), false, false, false )  );
			return entity;
		}
		Triplet<Catalog, Short, Boolean> t = new Triplet<>( entity, 
				bulk ? OPERATION_CODE_DML_CREATE_BULK : OPERATION_CODE_DML_CREATE, false );
		startOrmCommand( this, t, individualTransaction );
		return t.getFirst();
	}

	@Override
	public Catalog update( Catalog entity, boolean saveEntity, boolean bulk,
			boolean individualTransaction ) throws Exception {
		if ( ( entity == null ) || ( entity.getJournal() == null ) ) {
			throw new IllegalArgumentException( "CatalogJpaController.update(): illegal entity" );
		}		
		if ( !saveEntity ) {			
			JournalJpaController jjc = JournalJpaController.getInstance();
			entity.setJournal( jjc.update( entity.getJournal(), false, false, false ) );
			return entity;
		}
		Triplet<Catalog, Short, Boolean> t = new Triplet<>( entity, 
				 bulk ? OPERATION_CODE_DML_UPDATE_BULK : OPERATION_CODE_DML_UPDATE, true );
		startOrmCommand( this, t, individualTransaction );
		return t.getFirst();
	}

	@Override
	public Catalog delete( Catalog entity, boolean saveEntity, boolean bulk,
			boolean individualTransaction ) throws Exception {
		if ( ( entity == null ) || ( entity.getJournal() == null ) ) {
			throw new IllegalArgumentException( "CatalogJpaController.delete(): illegal entity" );
		}		
		if ( entity.getStatus() != Catalog.STATUS_ACTIVE ) return entity;

		entity.setStatus( Catalog.STATUS_DELETE );
		if ( !saveEntity ) {
			JournalJpaController jjc = JournalJpaController.getInstance();
			entity.setJournal( jjc.delete( entity.getJournal(), false, false, false ) );			
			return entity;	
		}
		Triplet<Catalog, Short, Boolean> t = new Triplet<>( entity, 
				 bulk ? OPERATION_CODE_DML_UPDATE_BULK : OPERATION_CODE_DML_UPDATE, false );
		startOrmCommand( this, t, individualTransaction );
		return t.getFirst();
	}

	@Override
	public Pair<Catalog, Catalog> newVersion( Catalog oldEntity, Catalog newEntity, boolean saveEntity,
			boolean bulk, boolean individualTransaction ) throws Exception {
		if ( ( oldEntity == null ) || ( newEntity == null )
				|| ( oldEntity.getStatus() != Catalog.STATUS_ACTIVE )
				|| ( oldEntity.getJournal() == null ) 
				|| ( oldEntity.getParentCatalog() == null ) 
				|| ( newEntity.getParentCatalog() == null ) ) {
			throw new IllegalArgumentException( "CatalogJpaController.newVersion(): illegal param" );
		}
		// fill new version of entity
		newEntity.setId( oldEntity.getId() );
		newEntity.setVersion( ( short ) ( oldEntity.getVersion() + 1 ) );
		newEntity.setStatus( Catalog.STATUS_ACTIVE );
		newEntity.setJournal( oldEntity.getJournal()  );

		JournalJpaController jjc = JournalJpaController.getInstance();
		
		// make Catalog's history entity and Journal entity for them
		Journal jd = jjc.copy( oldEntity.getJournal(), false, false, false );
		jd = jjc.delete( jd, false, false, false );
		Catalog history = new Catalog( oldEntity.getName(), oldEntity.getItem(), 
				Catalog.STATUS_HISTORY, oldEntity.getVariety(), oldEntity.getVersion(), 
				oldEntity.getParentCatalog(), null, null, jd, null );

		final Pair<Catalog, Catalog> p = new Pair<>( history, newEntity );
		if ( !saveEntity ) {
			newEntity.setJournal( jjc.update( newEntity.getJournal(), false, false, false ) );		
			return p;
		}
		Quartet<Catalog, Catalog, Boolean, Boolean> q = new Quartet<>( p.getFirst(), p.getSecond(), bulk, true );
		startOrmCommand( this, q, individualTransaction );
		p.setFirst( q.getFirst() );
		p.setSecond( q.getSecond() );
		return p;
	}

	// =================================================================================================================
	// Methods
	// =================================================================================================================
	public static CatalogJpaController getInstance() {
		return Holder.KEEPER;
	}
	
	public short getMaxItem( Catalog parent ) {
		Number num = ( Number ) OrmHelper.executeQueryWithParam( QueryType.JPQL, 
				ResourceKeeper.getQuery( QType.JPQL_MAX_CATALOG_ITEM ), 
				new Pair<Object, Object>( "parent", parent ) );
		return num == null ? ( short ) 0 : num.shortValue();
	}

	private void saveEntity( Object obj ) throws Exception {
		@SuppressWarnings("unchecked")
		Triplet<Catalog, Short, Boolean> t = ( Triplet<Catalog, Short, Boolean> ) obj;
		boolean newEntity = ( ( t.getSecond() == OPERATION_CODE_DML_CREATE_BULK ) 
				|| ( t.getSecond() == OPERATION_CODE_DML_CREATE ) );
		t.getFirst().setJournal( makeJournal( t.getFirst().getJournal(), t.getSecond(), t.getThird() ) );
		t.setFirst( ( Catalog ) ( newEntity ? OrmHelper.create( t.getFirst() ) : OrmHelper.update( t.getFirst() ) ) );
		OrmHelper.getCurrentManager().flush();
		LoggerJpaController.getInstance().create( t.getFirst(), t.getSecond(), null ); 
	}
	
	private void saveAncestorAndHistory( Object obj ) throws Exception {
		@SuppressWarnings("unchecked")
		Quartet<Catalog, Catalog, Boolean, Boolean> q = ( Quartet<Catalog, Catalog, Boolean, Boolean> ) obj;
		q.setSecond( update( q.getSecond(), true, q.getThird(), false ) );
		q.getFirst().setAncestor( q.getSecond() );
		q.setFirst( create( q.getFirst(), true, q.getThird(), false ) );
	}

	// =================================================================================================================
	// Inner and Anonymous Classes
	// =================================================================================================================
	private static class Holder {
		private static final CatalogJpaController KEEPER = new CatalogJpaController();
	}
}
