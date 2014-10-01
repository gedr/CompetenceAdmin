package minos.data.orm.controllers;

import static minos.entities.Logger.OPERATION_CODE_DML_CREATE;
import static minos.entities.Logger.OPERATION_CODE_DML_CREATE_BULK;
import static minos.entities.Logger.OPERATION_CODE_DML_UPDATE;
import static minos.entities.Logger.OPERATION_CODE_DML_UPDATE_BULK;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import ru.gedr.util.tuple.Pair;
import ru.gedr.util.tuple.Triplet;
import minos.data.orm.OrmCommand;
import minos.data.orm.OrmHelper;
import minos.data.orm.OrmHelper.QueryType;
import minos.entities.StringAttr;
import minos.utils.ResourceKeeper;
import minos.utils.ResourceKeeper.QType;

public class StringAttrJpaController extends BasisJpaController<StringAttr> implements OrmCommand {
	// =================================================================================================================
	// Constructors
	// =================================================================================================================
	private StringAttrJpaController() { }

	// =================================================================================================================
	// Methods for/from SuperClass/Interfaces
	// =================================================================================================================
	@Override
	public void execute( Object obj ) throws Exception {
		@SuppressWarnings("unchecked")
		Triplet<StringAttr, Short, Boolean> t = ( Triplet<StringAttr, Short, Boolean> ) obj;
		boolean flag = ( ( t.getSecond() == OPERATION_CODE_DML_CREATE_BULK ) 
				|| ( t.getSecond() == OPERATION_CODE_DML_CREATE ) );
		t.getFirst().setJournal( makeJournal( t.getFirst().getJournal(), t.getSecond(), t.getThird() ) );
		t.setFirst( ( StringAttr ) ( flag ? OrmHelper.create( t.getFirst() ) : OrmHelper.update( t.getFirst() ) ) );
		OrmHelper.getCurrentManager().flush();
		LoggerJpaController.getInstance().create( t.getFirst(), t.getSecond(), null ); 
	}

	@Override
	public StringAttr create( StringAttr entity, boolean saveEntity, boolean bulk, 
			boolean individualTransaction ) throws Exception {
		if ( entity == null ) { 
			throw new IllegalArgumentException( "StringAttrJpaController.create(): illegal entity param" );
		}
		if ( !saveEntity ) {
			JournalJpaController jjc = JournalJpaController.getInstance();
			entity.setJournal( jjc.create( null, false, false, false ) );
			return entity;
		}		
		Triplet<StringAttr, Short, Boolean> t = new Triplet<>( entity, 
				bulk ? OPERATION_CODE_DML_CREATE_BULK : OPERATION_CODE_DML_CREATE, false );
		startOrmCommand( this, t, individualTransaction );
		return t.getFirst();
	}

	@Override
	public StringAttr update( StringAttr  entity, boolean saveEntity, boolean bulk, 
			boolean individualTransaction ) throws Exception {
		if ( ( entity == null ) || ( entity.getJournal() == null ) ) {
			throw new IllegalArgumentException( "StringAttrJpaController.update(): illegal entity param " );
		}		
		if ( !saveEntity ) {
			JournalJpaController jjc = JournalJpaController.getInstance();
			entity.setJournal( jjc.update( entity.getJournal(), false, false, false ) );
			return entity;
		}		
		Triplet<StringAttr, Short, Boolean> t = new Triplet<>( entity, 
				bulk ? OPERATION_CODE_DML_UPDATE_BULK : OPERATION_CODE_DML_UPDATE, true );
		startOrmCommand( this, t, individualTransaction );
		return t.getFirst();
	}

	@Override
	public StringAttr delete( StringAttr entity, boolean saveEntity, boolean bulk, 
			boolean individualTransaction ) throws Exception {
		if ( ( entity == null ) || ( entity.getJournal() == null ) ) {
			throw new IllegalArgumentException( "StringAttrJpaController.delete(): illegal entity param " );
		}		
		if ( entity.getJournal().getDeleteMoment().before( new Date() ) ) return entity;
		if ( !saveEntity ) {
			JournalJpaController jjc = JournalJpaController.getInstance();
			entity.setJournal( jjc.delete( entity.getJournal(), false, false, false ) );
			return entity;
		}		
		Triplet<StringAttr, Short, Boolean> t = new Triplet<>( entity, 
				bulk ? OPERATION_CODE_DML_UPDATE_BULK : OPERATION_CODE_DML_UPDATE, false );
		startOrmCommand( this, t, individualTransaction );
		return t.getFirst();
	}

	// =================================================================================================================
	// Methods
	// =================================================================================================================
	public static StringAttrJpaController getInstance() {
		return Holder.KEEPER;
	}
	
	/**
	 * search in DB
	 * @param sa - fresh object
	 * @return list of StringAttr's entities if sa.value and sa.variety existing in DB; otherwise null
	 */
	public List<StringAttr> searchPpeStringAttr( StringAttr sa ) {
		if ( ( sa == null ) || ( sa.getValue() == null ) 
				|| sa.getValue().trim().isEmpty() ) return Collections.emptyList();
		// lookup changed StringAttr in DB
		List<StringAttr> lsa = OrmHelper.findByQueryWithParam( QueryType.JPQL, 
				ResourceKeeper.getQuery( QType.JPQL_FIND_PPE_STRING_ATTR ), 
				StringAttr.class, 
				new Pair<Object, Object>( "value", sa.getValue().trim() ), 
 				new Pair<Object, Object>( "variety", Arrays.asList( StringAttr.VARIETY_POFILE_PATTERN_ELEMENT_ATTRIBUTE ) ) );
		return ( ( lsa != null ) && ( lsa.size() > 0 ) ) ? lsa : Collections.<StringAttr>emptyList();		
	}


	// =================================================================================================================
	// Inner and Anonymous Classes
	// =================================================================================================================
	private static class Holder {
		private static final StringAttrJpaController KEEPER = new StringAttrJpaController();
	}
}