package minos.data.services;

import javax.persistence.Query;

import minos.entities.Catalog;
import minos.entities.Journal;
import minos.entities.StatusConst;

public class CatalogJPAController {
	private static String jpqlMaxItem = "select max(c.item) from Catalog c where c.parentCatalog.id = :pid";
	
	/**
	 * create new catalog 
	 * @param catalog is not null reference. ParentCatalog fields must be filled 
	 * @return
	 */
	public static Catalog create( Catalog catalog, boolean flagSaveEntity ) {
		if ( ( catalog == null ) || ( catalog.getParentCatalog() == null ) ) return null;
		
		ORMHelper.openManager();
		Query q = ORMHelper.getCurrentManager().createQuery( jpqlMaxItem );
		q.setParameter( "pid", catalog.getParentCatalog().getId() );
		Short item = (Short) q.getSingleResult();
		ORMHelper.closeManager();		

		catalog.setAncestorCatalog( null );
		catalog.setSubCatalogs( null );
		catalog.setCompetences( null );
		catalog.setVariety( catalog.getParentCatalog().getVariety() );
		catalog.setStatus( Catalog.STATUS_ACTIVE );
		catalog.setVersion( (short) 1 ); 
		catalog.setItem( ++item );
		catalog.setJournal( JournalJPAController.create( false ) );
		if ( flagSaveEntity ) catalog = (Catalog) ORMHelper.createEntity( catalog );
		return catalog;
	}

	/**
	 * create history copy of catalog
	 * @param catalog is not null reference. AncestorCatalog and journal of AncestorCatalog fields must be filled 
	 * @return history copy of catalog
	 */
	public static Catalog edit( Catalog catalog, boolean flagSaveEntity ) {
		if ( ( catalog == null ) || ( catalog.getAncestorCatalog() == null ) || 
				( catalog.getAncestorCatalog().getJournal() == null ) ) return null;
		
		Catalog basis = catalog.getAncestorCatalog();		
		// change name
		String tmp = basis.getName();
		basis.setName( catalog.getName() );
		catalog.setName( tmp );				
		// change version
		catalog.setVersion( basis.getVersion() );
		basis.setVersion( (short) (basis.getVersion() + 1) );
		// make journal
		Journal j = JournalJPAController.copy( basis.getJournal(), false );
		JournalJPAController.delete( j, false );
		catalog.setJournal( j );		
		basis.setJournal( JournalJPAController.edit( basis.getJournal(), false ) );
		// fill over fields
		catalog.setItem( basis.getItem() );
		catalog.setStatus( Catalog.STATUS_HISTORY );
		catalog.setVariety( basis.getVariety() );
		catalog.setParentCatalog( basis.getParentCatalog() );
		catalog.setSubCatalogs( null );
		catalog.setCompetences( null );
		if ( flagSaveEntity ) {
			ORMHelper.updateEntity( basis );		
			catalog = (Catalog) ORMHelper.createEntity( catalog );
		}
		return catalog;
	}

	/**
	 * mark catalog as deleted 
	 * @param catalog is not null reference. Journal fields must be filled 
	 * @return history copy of catalog
	 */
	public static Catalog delete( Catalog catalog, boolean flagSaveEntity ) {
		if ( ( catalog == null ) || ( catalog.getJournal() == null ) )  return null;
		
		catalog.setStatus( StatusConst.STATUS_DELETE );		
		catalog.setJournal( JournalJPAController.delete( catalog.getJournal(), false ) );
		if ( flagSaveEntity ) catalog = (Catalog) ORMHelper.updateEntity( catalog );		
		return catalog;
	}
}