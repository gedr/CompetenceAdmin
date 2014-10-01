package minos.ui.panels;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;

import ru.gedr.util.tuple.Pair;
import ru.gedr.util.tuple.Quartet;
import ru.gedr.util.tuple.Triplet;
import minos.data.orm.OrmHelper;
import minos.data.orm.OrmHelper.QueryType;
import minos.entities.Actors;
import minos.entities.ActorsPerformance;
import minos.entities.Competence;
import minos.entities.Indicator;

import com.alee.laf.label.WebLabel;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.scroll.WebScrollPane;
import com.alee.laf.tree.WebTree;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class ActorsPerfomancePanel extends WebPanel implements ActionListener, TreeCellRenderer {
	private static final long serialVersionUID = 1L;
	
	private static final String PROF = "Профессиональные компетенции";
	private static final String PRIV = "Личностно-деловые компетенции";
	private static final String BSNS = "Управленческие компетенции";

	private static final String jpql1 = " SELECT ap, cmt FROM ActorsPerformance ap, Competence cmt "
			+ " INNER JOIN FETCH ap.attributes "
			+ " INNER JOIN FETCH ap.profilePatternElement "
			+ " INNER JOIN FETCH ap.profilePatternElement.minLevel "
			+ " INNER JOIN FETCH ap.profilePatternElement.competence "			
			+ " WHERE ap.actors IN (:actors)"
			+ " AND ( ( cmt = ap.profilePatternElement.competence "
			+ "         OR cmt.ancestor = ap.profilePatternElement.competence ) "
			+ "     AND ( ap.profilePatternElement.profilePattern.timePoint BETWEEN cmt.journal.editMoment "
			+ "           AND cmt.journal.deleteMoment ) ) ";


	private static final String jpql2 = " select i from Indicator i "
			+ " INNER JOIN FETCH i.competence "
			+ " INNER JOIN FETCH i.level "	
			+ " where i.competence IN (:competences) "
			+ " and :ts between i.journal.editMoment and i.journal.deleteMoment "
			+ " order by i.competence, i.level, i.item ";

	
	private String[] name = { null, PROF, PRIV, BSNS };
	
	private List<Triplet<ActorsPerformance, Competence, Boolean>> lst;
	private List<Indicator> li;
	
	
	@Override
	public Component getTreeCellRendererComponent(JTree arg0, Object arg1,
			boolean arg2, boolean arg3, boolean arg4, int arg5, boolean arg6) {
		// TODO Auto-generated method stub
		return null;
	}

	
	public ActorsPerfomancePanel( Actors a ) {
		java.util.Date now = new java.util.Date(); 
		if ( ( a == null ) || now.before( a.getAssembly() ) ) {
			add( new WebLabel( "Тесты не сформированы" ) );
			return;
		}
		load( a );
		
		
		DefaultMutableTreeNode root = new DefaultMutableTreeNode();
		root.add( makeCompetenceNodes( ( short ) 1 ) );
		root.add( makeCompetenceNodes( ( short ) 2 ));
		root.add( makeCompetenceNodes( ( short ) 3 ) );
		
		WebTree tree = new WebTree( root ) ;
        tree.setEditable( false );
        tree.setRootVisible( false );
        tree.setSelectionMode( WebTree.DISCONTIGUOUS_TREE_SELECTION );
        tree.setCellRenderer( this );
        add( new WebScrollPane( tree ) );
	}
	
	private DefaultMutableTreeNode makeCompetenceNodes( short variety ) {
		DefaultMutableTreeNode node = new DefaultMutableTreeNode( name[variety] );
		for ( Triplet<ActorsPerformance, Competence, Boolean> p : lst )
			if ( variety == p.getSecond().getVariety() ) {
				DefaultMutableTreeNode cn = new DefaultMutableTreeNode( p );
				@SuppressWarnings("unchecked")
				List<Quartet<Long, Short, Double, Date>> lq = ( List<Quartet<Long, Short, Double, Date>> ) 
						( p.getFirst().getAttributes() == null ? null :
					new Gson().fromJson( p.getFirst().getAttributes(), 
						new TypeToken<List<Quartet<Long, Short, Double, Date>>>(){}.getType() ) );
				makeIndicatorNodes( cn, p.getFirst().getProfilePatternElement().getCompetence(), lq );
				
				node.add( cn );				
			}
		
		return node;
	}
	
	private void makeIndicatorNodes( DefaultMutableTreeNode prnt, Competence c, 
			List<Quartet<Long, Short, Double, Date>> lq ) {
		for ( Indicator i : li ) {
			if ( c.equals( i.getCompetence() ) ) {
				if ( lq == null ) {
					prnt.add( new DefaultMutableTreeNode( new Pair<Indicator, Double>( i, 0.0D ) ) );
				} else {
					for ( Quartet<Long, Short, Double, Date> q : lq ) {
						if ( ( q.getFirst() == i.getId() ) && ( q.getSecond() == i.getVersion() ) ) {
							prnt.add( new DefaultMutableTreeNode( new Pair<Indicator, Double>( i, q.getThird() ) ) );
							break;
						}
					}
				}				
			}			
		}
	}

	@Override
	public void actionPerformed( ActionEvent e ) {
		// TODO Auto-generated method stub
		
	}
	
	
	private void load( Actors a ) {
		List<Object[]> lap = OrmHelper.findByQueryWithParam( QueryType.JPQL, 
				jpql1, 
				Object[].class, 
				new Pair<Object, Object>( "actors", Arrays.asList( a ) ) );
		if ( ( lap == null ) || ( lap.size() == 0 ) ) throw new IllegalArgumentException();
		lst = new ArrayList<>();
		for ( Object[] o : lap ) lst.add( new Triplet<>( ( ActorsPerformance ) o[0], ( Competence ) o[1], Boolean.FALSE ) );
		
		List<Competence> lc = new ArrayList<>();
		for ( Triplet<ActorsPerformance, Competence, Boolean> p : lst ) {
			lc.add( p.getFirst().getProfilePatternElement().getCompetence() );			
		}
		li = OrmHelper.findByQueryWithParam( QueryType.JPQL, 
				jpql2, 
				Indicator.class, 
				new Pair<Object, Object>( "competences", lc ),
				new Pair<Object, Object>( "ts", a.getProfile().getProfilePattern().getTimePoint() ) );
		if ( ( li == null ) || ( li.size() == 0 ) ) throw new IllegalArgumentException();

		
	}


}
