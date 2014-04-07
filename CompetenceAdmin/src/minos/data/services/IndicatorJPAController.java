package minos.data.services;

import javax.persistence.Query;

import minos.entities.Catalog;
import minos.entities.Indicator;
import minos.entities.Journal;
import minos.entities.StatusConst;

public class IndicatorJPAController {
	private static String jpqlMaxItem = "select max(i.item) from Indicator i where i.competence.id = :cid and i.level.id = :lid";

	/**
	 * create new indicator 
	 * @param indicator is not null reference. ParentCatalog fields must be filled 
	 * @param flagSaveEntity is not null reference. Level and Competence fields must be filled
	 * @return
	 */
	public static Indicator create( Indicator indicator, boolean flagSaveEntity ) {
		if ( ( indicator == null ) || ( indicator.getCompetence() == null ) || ( indicator.getLevel() == null ) ) return null;
		
		ORMHelper.openManager();
		Query q = ORMHelper.getCurrentManager().createQuery( jpqlMaxItem );
		q.setParameter( "cid", indicator.getCompetence().getId() );
		q.setParameter( "lid", indicator.getLevel().getId() );
		Short item = (Short) q.getSingleResult();
		ORMHelper.closeManager();
		
		indicator.setAncestorIndicator( null );
		indicator.setItem( ++item );
		indicator.setStatus( StatusConst.STATUS_ACTIVE );
		indicator.setVersion( (short) 1 );
		indicator.setJournal( JournalJPAController.create( false ) );
		if ( flagSaveEntity ) indicator = (Indicator) ORMHelper.createEntity( indicator );
		return indicator;
	}


	/**
	 * create history copy of indicator
	 * @param indicator  is not null reference. AncestorIndicator, Competence and Journal of AncestorIndicator fields must be filled 
	 * @param flagSaveEntity
	 * @return
	 */
	public static Indicator edit( Indicator indicator, boolean flagSaveEntity ) {
		if ( ( indicator == null ) || ( indicator.getAncestorIndicator() == null ) || 
				( indicator.getAncestorIndicator().getCompetence() == null ) ||
				( indicator.getAncestorIndicator().getJournal() == null ) ) return null;

		Indicator basis = indicator.getAncestorIndicator();		
		// change name
		String tmp = basis.getName();
		basis.setName( indicator.getName() );
		indicator.setName( tmp );				
		// change version
		indicator.setVersion( basis.getVersion() );
		basis.setVersion( (short) (basis.getVersion() + 1) );
		// make journal
		Journal j = JournalJPAController.copy( basis.getJournal(), false );
		JournalJPAController.delete( j, false );
		indicator.setJournal( j );		
		basis.setJournal( JournalJPAController.edit( basis.getJournal(), false ) );
		// fill over fields
		indicator.setItem( basis.getItem() );
		indicator.setStatus( Catalog.STATUS_HISTORY );
		indicator.setCompetence( basis.getCompetence() );
		if ( flagSaveEntity ) {
			ORMHelper.updateEntity( basis );		
			indicator = (Indicator) ORMHelper.createEntity( indicator );
		}
		return indicator;
	}
	
	/**
	 *  mark catalog as deleted
	 * @param indicator is not null reference. Journal fields must be filled 
	 * @param flagSaveEntity
	 * @return
	 */
	public static Indicator delete( Indicator indicator, boolean flagSaveEntity ) {
		if ( ( indicator == null ) || ( indicator.getJournal() == null ) )  return null;
		
		indicator.setStatus( StatusConst.STATUS_DELETE );		
		indicator.setJournal( JournalJPAController.delete( indicator.getJournal(), false ) );
		if ( flagSaveEntity ) indicator = (Indicator) ORMHelper.updateEntity( indicator );		
		return indicator;
	}
}