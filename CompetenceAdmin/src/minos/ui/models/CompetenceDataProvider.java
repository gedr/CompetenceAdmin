package minos.ui.models;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import minos.data.services.ORMHelper;
import minos.entities.Level;
import minos.ui.models.TreeElement.TreeElementType;

import com.alee.extended.tree.ChildsListener;

public class CompetenceDataProvider extends BasisDataProvider<TreeElement> {
	private static Logger log = LoggerFactory.getLogger( CompetenceDataProvider.class );
	private BasisDataProvider<TreeElement> bdp;	

	public CompetenceDataProvider(BasisDataProvider<TreeElement> bdp) {		
		super();
		this.bdp = bdp;
		setCurrentTimePoint( null );
	}
	
	public CompetenceDataProvider(Timestamp timepoint, BasisDataProvider<TreeElement> bdp) {
		super();
		this.bdp = bdp;
		setCurrentTimePoint( timepoint );		
	}
	
	@Override
	public TreeElement getRoot() {		
		return bdp.getRoot();
	}

	@Override
	public boolean isLeaf(TreeElement node) {
		return ( node.getType() == TreeElementType.INDICATOR ? true : false );
	}

	@Override
	public void loadChilds(TreeElement node, ChildsListener<TreeElement> listener) {
		if ( node.getType() == TreeElementType.CATALOG ) {		
		
			// load sub catalog
			@SuppressWarnings("unchecked")
			final List<TreeElement>[] siblings = ( List<TreeElement>[] ) new List[] { null };
			bdp.loadChilds( node, new ChildsListener<TreeElement>() {

				@Override
				public void childsLoadFailed(Throwable cause) {
					if( (log != null) && log.isErrorEnabled() ) log.error( "CompetenceDataProvider.loadChilds()", cause );				
				}

				@Override
				public void childsLoadCompleted(List<TreeElement> childs) {
					siblings[0] = childs;				
				}
			} );
			
			// load competence for catalog 
			ORMHelper.openManager();
			Query q = ORMHelper.getCurrentManager().createQuery("select c.id, c.ancestorCompetence, c.variety, c.name from Competence c "
					+ " where ( (c.journal.editMoment <= :ts and :ts < c.journal.deleteMoment)  or (c.journal.deleteMoment < :ts and c.status = 2) ) "
					+ " and c.catalog.id = :pid order by c.item");
			q.setParameter( "ts", getCurrentTimePoint() );
			q.setParameter( "pid", node.getCurrent() );
			@SuppressWarnings("unchecked")
			List<Object[]> res = q.getResultList();
			ORMHelper.closeManager();

			if ( (res == null) || (res.size() == 0) ) {
				listener.childsLoadCompleted( siblings[0] );
				return ;
			}
			
			List<TreeElement> lst = new ArrayList<>( siblings[0] );
			for ( Object[] objs : res ) { 
				lst.add( new TreeElement( TreeElementType.COMPETENCE, (Integer) objs[0], (Integer) ( objs[1] == null ? objs[0] : objs[1] ), 
						(Short) objs[2], (String) objs[3] ) ) ;
			}
			listener.childsLoadCompleted( lst );
		}
		
		if ( node.getType() == TreeElementType.COMPETENCE ) {
			List<TreeElement> lvl = new ArrayList<>( Level.LEVEL_COUNT );
			for( int i = 1; i <= Level.LEVEL_COUNT; i++ ) lvl.add( new TreeElement( TreeElementType.LEVEL, i, node.getAncestor(), ( short ) 0, null ) );
			listener.childsLoadCompleted( lvl );
		}

		if ( node.getType() == TreeElementType.LEVEL ) {
			// load indicator for competence level
			ORMHelper.openManager();
			Query q = ORMHelper.getCurrentManager().createQuery("select i.id, i.ancestorIndicator, 0, i.name from Indicator i "
					+ " where ( (i.journal.editMoment <= :ts and :ts < i.journal.deleteMoment) or (i.journal.deleteMoment < :ts and i.status = 2) )"
					+ " and i.competence.id = :pid "
					+ " and i.level.id = :lid order by i.item");
			q.setParameter( "ts", getCurrentTimePoint() );
			q.setParameter( "pid", node.getAncestor() );
			q.setParameter( "lid", node.getCurrent() );
			@SuppressWarnings("unchecked")
			List<Object[]> res = q.getResultList();
			ORMHelper.closeManager();

			
			if ( (res == null) || (res.size() == 0) ) {
				listener.childsLoadCompleted( emptyList );
				return ;
			}
			
			List<TreeElement> lst = new ArrayList<>();
			for ( Object[] objs : res ) { 
				lst.add( new TreeElement( TreeElementType.INDICATOR, (Integer) objs[0], (Integer) ( objs[1] == null ? objs[0] : objs[1] ), 
						( short ) 0, (String) objs[3] ) ) ;
			}
			listener.childsLoadCompleted( lst );			
		}
	}
}