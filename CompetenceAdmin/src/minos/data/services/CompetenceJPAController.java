package minos.data.services;

import javax.persistence.Query;

import minos.entities.Catalog;
import minos.entities.Competence;
import minos.entities.Journal;
import minos.entities.StatusConst;

public class CompetenceJPAController {
	private static String jpqlMaxItem = "select max(c.item) from Competence c where c.catalog.id = :cid";
	
	/**
	 * create new competence
	 * @param competence is not null reference. Catalog fields must be filled 
	 * @param flagSaveEntity is flag saving entity in db
	 * @return new competence
	 */
	public static Competence create( Competence competence, boolean flagSaveEntity ) {
		if ( ( competence == null ) || ( competence.getCatalog() == null ) ) return null;

		ORMHelper.openManager();
		Query q = ORMHelper.getCurrentManager().createQuery( jpqlMaxItem );
		q.setParameter( "cid", competence.getCatalog().getId() );
		Short item = (Short) q.getSingleResult();
		ORMHelper.closeManager();

		competence.setAncestorCompetence( null );		
		competence.setIndicators( null );
		competence.setItem( ++item );		
		competence.setStatus( Competence.STATUS_ACTIVE );
		competence.setVariety( competence.getCatalog().getVariety() );
		competence.setVersion( (short) 1 );
		competence.setJournal( JournalJPAController.create( false ) );
		if ( flagSaveEntity ) competence = (Competence) ORMHelper.createEntity( competence );
		return competence;		
	}

	/**
	 * create history copy of competence
	 * @param competence is not null reference. getAncestorCompetence and journal of getAncestorCompetence fields must be filled 
	 * @param flagSaveEntity
	 * @return history competence
	 */
	public static Competence edit( Competence competence, boolean flagSaveEntity ) {
		if ( ( competence == null ) || ( competence.getAncestorCompetence() == null ) 
				|| ( competence.getAncestorCompetence().getJournal() == null ) ) return null;
		
		Competence basis = competence.getAncestorCompetence();		
		// change name and description
		String name = basis.getName();
		String dscr = basis.getDescription();
		basis.setName( competence.getName() );
		basis.setDescription( competence.getDescription() );
		competence.setName( name );
		competence.setDescription( dscr );
		// change version
		competence.setVersion( basis.getVersion() );
		basis.setVersion( (short) (basis.getVersion() + 1) );
		// make journal
		Journal j = JournalJPAController.copy( basis.getJournal(), false );
		JournalJPAController.delete( j, false );
		competence.setJournal( j );		
		basis.setJournal( JournalJPAController.edit( basis.getJournal(), false ) );
		// fill over fields
		competence.setItem( basis.getItem() );
		competence.setStatus( Catalog.STATUS_HISTORY );
		competence.setVariety( basis.getVariety() );
		competence.setCatalog( basis.getCatalog() );
		competence.setIndicators( null );
		if ( flagSaveEntity ) {
			ORMHelper.updateEntity( basis );		
			competence = (Competence) ORMHelper.createEntity( competence );
		}
		return competence;		
	}
	
	/**
	 * mark competence as deleted 
	 * @param competence is not null reference. Journal fields must be filled
	 * @param flagSaveEntity
	 * @return
	 */
	public static Competence delete( Competence competence, boolean flagSaveEntity ) {
		if ( ( competence == null ) || ( competence.getJournal() == null ) )  return null;
		
		competence.setStatus( StatusConst.STATUS_DELETE );		
		competence.setJournal( JournalJPAController.delete( competence.getJournal(), false ) );
		if ( flagSaveEntity ) competence = (Competence) ORMHelper.updateEntity( competence );		
		return competence;
	}	
}