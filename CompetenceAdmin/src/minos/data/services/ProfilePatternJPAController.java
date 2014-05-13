package minos.data.services;

import java.sql.Timestamp;
import java.util.Calendar;

import javax.persistence.Query;

import minos.entities.Catalog;
import minos.entities.Journal;
import minos.entities.ProfilePattern;
import minos.entities.StatusConst;

public class ProfilePatternJPAController {
	private static String jpqlMaxItem = "select max(pp.item) from ProfilePattern pp where pp.catalog.id = :cid";
	
	public static final Timestamp TIMEPOINT;
	static {
		Calendar cal = Calendar.getInstance();
		cal.set(3333, 3, 3);
		TIMEPOINT = new Timestamp( cal.getTimeInMillis() );		
	}
	
	/**
	 * create new profile pattern 
	 * @param pp is not null reference. Catalog field must be filled 
	 * @return
	 */
	public static ProfilePattern create( ProfilePattern pp, boolean flagSaveEntity ) {
		if ( ( pp == null ) || ( pp.getCatalog() == null ) ) return null;
		
		ORMHelper.openManager();
		Query q = ORMHelper.getCurrentManager().createQuery( jpqlMaxItem );
		q.setParameter( "cid", pp.getCatalog().getId() );
		Short item = (Short) q.getSingleResult();
		ORMHelper.closeManager();
		
		pp.setAncestor( null );
		pp.setItem( ++item );
		pp.setJournal( JournalJPAController.create( false ) );
		pp.setProfilePatternElements( null );
		pp.setVersion( (short) 1 );
		pp.setProfilePatternElements( null );
		pp.setStatus( ProfilePattern.STATUS_BUILDING );
		pp.setTimePoint( TIMEPOINT );		
		if ( flagSaveEntity ) pp = ( ProfilePattern ) ORMHelper.createEntity( pp );
		return pp;
	}

	/**
	 * create history copy of catalog
	 * @param catalog is not null reference. Catalog and journal of Ancestor fields must be filled 
	 * @return history copy of catalog
	 */
	public static ProfilePattern edit( ProfilePattern pp, boolean saveEntity, boolean saveHistory ) {
		if ( ( pp == null ) || ( pp.getAncestor() == null ) || 
				( pp.getAncestor().getCatalog() == null ) ||
				( pp.getAncestor().getJournal() == null ) ) return null;
		
		ProfilePattern basis = pp.getAncestor();
		if ( !basis.getName().equals( pp.getName() ) ) { // change name
			String tmp = basis.getName();
			basis.setName( pp.getName() );
			pp.setName( tmp );				
		}
		if ( !basis.getDescription().equals( pp.getDescription() ) ) { // change description
			String tmp = basis.getDescription();
			basis.setDescription( pp.getDescription() );
			pp.setDescription( tmp );
		}
		if ( basis.getPostMask() != pp.getPostMask() ) { // change postMask
			short pm = basis.getPostMask();
			basis.setPostMask( pp.getPostMask() );
			pp.setPostMask( pm );
		}
		if ( basis.getFilialMask() != pp.getFilialMask() ) { // change filial mask
			long fm = basis.getFilialMask();
			basis.setFilialMask( pp.getFilialMask() );
			pp.setFilialMask( fm );		
		}
		if ( basis.getStatus() != pp.getStatus() ) { // change status
			short stat = basis.getStatus();
			basis.setStatus( pp.getStatus() );
			pp.setStatus( stat );
		}
		if ( basis.getItem() != pp.getItem() ) { // change item
			int i = basis.getItem();
			basis.setItem( pp.getItem() );
			pp.setItem( i );			
		}
		if ( ( pp.getCatalog() != null ) && ( !pp.getCatalog().equals( basis.getCatalog() ) ) ) {
			Catalog tmp = basis.getCatalog();
			basis.setCatalog( pp.getCatalog() );
			pp.setCatalog( tmp );
		}
		if ( pp.getCatalog() == null ) pp.setCatalog( basis.getCatalog() );
		// change version
		pp.setVersion( basis.getVersion() );
		basis.setVersion( ( short ) ( basis.getVersion() + 1 ) );		
		// make journal
		Journal j = JournalJPAController.copy( basis.getJournal(), false );
		JournalJPAController.delete( j, false );
		pp.setJournal( j );		
		basis.setJournal( JournalJPAController.edit( basis.getJournal(), false ) );		
		// fill over fields
		pp.setProfilePatternElements( null );

		if ( saveEntity ) {
			ORMHelper.updateEntity( basis );		
			if ( saveHistory ) pp = ( ProfilePattern ) ORMHelper.createEntity( pp );
		}
		return ( saveHistory ? pp : basis );
	}

	/**
	 * mark ProfilePattern as deleted 
	 * @param pp is not null reference. Journal fields must be filled 
	 * @return history copy of catalog
	 */
	public static ProfilePattern delete( ProfilePattern pp, boolean saveEntity ) {
		if ( ( pp == null ) || ( pp.getJournal() == null ) )  return null;
		
		pp.setStatus( StatusConst.STATUS_DELETE );		
		pp.setJournal( JournalJPAController.delete( pp.getJournal(), false ) );
		if ( saveEntity ) pp = ( ProfilePattern ) ORMHelper.updateEntity( pp );		
		return pp;
	}
}