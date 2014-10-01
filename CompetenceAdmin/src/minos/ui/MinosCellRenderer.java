package minos.ui;

import java.awt.Color;
import java.awt.Component;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.ListCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.TreeCellRenderer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import minos.entities.ActorsInfo;
import minos.entities.Catalog;
import minos.entities.Competence;
import minos.entities.Division;
import minos.entities.EstablishedPost;
import minos.entities.Indicator;
import minos.entities.Level;
import minos.entities.OrgUnit;
import minos.entities.Person;
import minos.entities.Post;
import minos.entities.Profile;
import minos.entities.ProfilePattern;
import minos.entities.ProfilePatternElement;
import minos.entities.Role;
import minos.ui.models.MainTreeNode;
import minos.ui.models.dataproviders.EPostDataProvider.EPostGroup;
import minos.ui.panels.StringAttrPanel;
import minos.utils.IconJoiner;
import minos.utils.Permission.Operation;
import minos.utils.ResourceKeeper;
import minos.utils.ResourceKeeper.IType;

import com.alee.laf.label.WebLabel;

public class MinosCellRenderer<T> implements TableCellRenderer, TreeCellRenderer, ListCellRenderer<T> {	
	// =================================================================================================================
	// Constants
	// =================================================================================================================
	private static final String NULL_OBJECT			= "null";
	private static final String ROLE_OBJECT			= "Role"; 	// equal Role.class.getSimpleName(); 
	private static final String LEVEL_OBJECT		= "Level";	// equal Level.class.getSimpleName();
	private static final String INDICATOR_OBJECT	= "Indicator";	// equal Indicator.class.getSimpleName();
	private static final String COMPETENCE_OBJECT	= "Competence";	// equal Competence.class.getSimpleName();
	private static final String CATALOG_OBJECT		= "Catalog";	// equal Catalog.class.getSimpleName();
	private static final String ACTORS_INFO_OBJECT	= "ActorsInfo";	// equal ActorsInfo.class.getSimpleName();
	private static final String PERSON_OBJECT		= "Person";		// equal Person.class.getSimpleName();
	private static final String OPERATION_OBJECT	= "Operation";	// equal Permission.Operation.class.getSimpleName();
	private static final String PP_OBJECT			= "ProfilePattern";// equal ProfilePattern.class.getSimpleName();
	private static final String PPE_OBJECT			= "ProfilePatternElement";// equal ProfilePattern.class.getSimpleName();
	private static final String DIVISION_OBJECT		= "Division";// equal Division.class.getSimpleName();
	private static final String EPOST_OBJECT		= "EstablishedPost";// equal EstablishedPost.class.getSimpleName();
	private static final String EPOST_GROUP_OBJECT	= "EPostGroup";// equal EPostGroup.class.getSimpleName();
	private static final String PROFILE_OBJECT		= "Profile"; // equal Profile.class.getSimpleName();
	private static final String ORG_UNIT_OBJECT		= "OrgUnit"; // equal OrgUnit.class.getSimpleName();
	private static final String POST_OBJECT			= "Post"; // equal Post.class.getSimpleName();
	
	private static final Color CBGSE		= new Color( 59, 115, 175 ); 	// color background of selected element
	private static final Color CFGSE		= Color.WHITE;					// color foreground of selected element
	private static final Color CBGUE		= Color.WHITE;				 	// color background of unselected element
	private static final Color CFGUE		= Color.BLACK;					// color foreground of unselected element

	private static final Logger log = LoggerFactory.getLogger( StringAttrPanel.class );
	
	// =================================================================================================================
	// Fields
	// =================================================================================================================
	private JLabel lbl;
	private int size = 0;
	private ImageIcon	updateIcon = null;
	private ImageIcon	deleteIcon = null;
	private ImageIcon	indicatorIcon = null;
	private Icon		divisionIcon = null;
	private Icon 		profileIcon = null;
	private Icon[] indicatorLevelIcon = null;
	private ImageIcon[] levelIcons = null;
	private ImageIcon[] competenceIcons = null;
	private ImageIcon[] catalogIcons = null;
	private Icon[] ppIcon = null; // ProfilePattern icons
	private Icon[] epostIcons = null;
	private Map<Integer, Icon> ppeIcons = null; // ProfilePatternElement icons

	// =================================================================================================================
	// Constructors
	// =================================================================================================================
	public MinosCellRenderer( int iconSize ) {
		lbl = new WebLabel();
		lbl.setOpaque( true );
		size = iconSize;
	}

	// =================================================================================================================
	// Getter & Setter
	// =================================================================================================================

	// =================================================================================================================
	// Methods for/from SuperClass/Interfaces
	// =================================================================================================================
	@Override
	public Component getListCellRendererComponent( JList<? extends T> list,
			T value, int index, boolean isSelected, boolean cellHasFocus ) {
		return getRenderComponentForObject( value, null, isSelected ? CBGSE : CBGUE, isSelected ? CFGSE : CFGUE, false );
	}

	@Override
	public Component getTreeCellRendererComponent( JTree tree, Object value, boolean isSelected, 
			boolean expanded, boolean leaf, int row, boolean hasFocus ) {
		Object obj = ( ( value == null ) || ( ( ( MainTreeNode ) value ).getUserObject() == null ) ) 
				? null : ( ( MainTreeNode ) value ).getUserObject();
		boolean visibleOnlyIndicator = ( ( value != null ) 
				&& ( ( ( MainTreeNode ) value ).getUserObject() instanceof Indicator ) 
				&& ( ( ( MainTreeNode ) value ).getParent() != null )
				&& ( ( ( MainTreeNode ) value ).getParent().getUserObject() instanceof Level ) );
		return getRenderComponentForObject( obj, ( ( MainTreeNode ) value ).getAddon(), isSelected ? CBGSE : CBGUE, isSelected ? CFGSE : CFGUE, 
				visibleOnlyIndicator );
	}
	
	@Override
	public Component getTableCellRendererComponent( JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column ) {
		return getRenderComponentForObject( value, null, isSelected ? CBGSE : CBGUE, isSelected ? CFGSE : CFGUE, false );
	}

	// =================================================================================================================
	// Methods
	// =================================================================================================================
	private Component getRenderComponentForObject( Object obj, Object addon, Color bgc, Color fgc, boolean visibleOnlyIndicator ) {
		String name = ( obj == null ? NULL_OBJECT : obj.getClass().getSimpleName() );
		try {
			switch ( name ) {
			case NULL_OBJECT : return fillComponent( "", null, bgc, fgc, JLabel.LEFT  );

			case ROLE_OBJECT : return fillComponent( ( ( Role ) obj ).getName(), null, bgc, fgc, JLabel.LEFT  );

			case LEVEL_OBJECT : return fillComponent( ( ( Level ) obj ).getName(), 
					getLevelIcon( ( ( Level ) obj ).getId() ), bgc, fgc, JLabel.LEFT  );

			case COMPETENCE_OBJECT : return fillComponent( ( ( Competence ) obj ).getName(), 
					getCompetenceIcon( ( ( Competence ) obj ).getVariety() ), bgc, fgc, JLabel.LEFT  );

			case CATALOG_OBJECT : return fillComponent( ( ( Catalog ) obj ).getName(), 
					getCatalogIcon( ( ( Catalog ) obj ).getVariety() ), bgc, fgc, JLabel.LEFT  );

			case INDICATOR_OBJECT : return fillComponent( ( ( Indicator ) obj ).getName(), 
					getIndicatorIcon( visibleOnlyIndicator, ( ( Indicator ) obj ).getLevel() ), bgc, fgc, 
					JLabel.LEFT  );

			case ACTORS_INFO_OBJECT : return fillComponent( ( ( ActorsInfo ) obj ).getName(), null, bgc, fgc, 
					JLabel.LEFT  );
			
			case PERSON_OBJECT : return fillComponent( ( ( Person ) obj ).getFullName(), null, bgc, fgc, 
					JLabel.LEFT  );
			
			case OPERATION_OBJECT : return fillComponent( null, getOperationIcon( ( Operation ) obj ), bgc, fgc, 
					JLabel.CENTER );

			case PP_OBJECT : return fillComponent( ( ( ProfilePattern ) obj ).getName(), 
					getProfilePatternIcon( ( ( ProfilePattern ) obj ).getStatus() ), bgc, fgc, JLabel.CENTER );

			case PPE_OBJECT : 
				ProfilePatternElement ppe = ( ProfilePatternElement ) obj;
				Competence cmtc = ( Competence ) ( addon != null ? addon : ppe.getCompetence() );
				return fillComponent( cmtc.getName(), getProfilePatternElementIcon( ppe ), bgc, fgc, JLabel.CENTER );
				
			case DIVISION_OBJECT : return fillComponent( ( ( Division ) obj ).getFullName(), getDivisionIcon(), 
					bgc, fgc, JLabel.CENTER );

			case EPOST_OBJECT : return fillComponent( ( ( EstablishedPost ) obj ).getName(), 
					getEPostIcon( ( ( EstablishedPost ) obj ).getKpers() ), bgc, fgc, JLabel.CENTER );
			
			case EPOST_GROUP_OBJECT : 
				EPostGroup epg = (EPostGroup) obj;
				return fillComponent( epg.getEstablishedPosts().get( 0 ).getName() + "   [ " 
				+ epg.getEstablishedPosts().size() + " ]", 
					getEPostIcon( epg.getEstablishedPosts().get( 0 ).getKpers() ), bgc, fgc, JLabel.CENTER );
			
			case PROFILE_OBJECT : return fillComponent( ( ( Profile ) obj ).getProfilePattern().getName(), 
					getProfileIcon(), bgc, fgc, 
					JLabel.CENTER );
			
			case ORG_UNIT_OBJECT : return fillComponent( ( ( OrgUnit ) obj ).getPost().getName(), 
					getEPostIcon( ( ( OrgUnit ) obj ).getPost().getKpers() ), bgc, fgc, JLabel.CENTER );
			
			case POST_OBJECT : return fillComponent( ( ( Post ) obj ).getName(), 
					getEPostIcon( ( ( Post ) obj ).getKpers() ), bgc, fgc, JLabel.CENTER );

			default : return fillComponent( obj.toString(), null, bgc, fgc, JLabel.LEFT );
			}
		} catch ( Exception ex ) {
			if ( ( log != null ) && log.isErrorEnabled() ) {
				log.error( "MinosCellRenderer.getRenderComponentForObject() : ", ex );
			}
		}
		return fillComponent( "", null, bgc, fgc, JLabel.LEFT  );
	}

	/**
	 * fill Label component for display as Cell Component Renderer
	 * @param txt - text for label
	 * @param icon - icon for label
	 * @param bg - background color
	 * @param fg - foreground color
	 * @return filled Label component
	 */
	private Component fillComponent( String txt, Icon icon, Color bg, Color fg, int horizontalAlignment ) {
		lbl.setBackground( bg );
		lbl.setForeground( fg );
		lbl.setHorizontalAlignment( horizontalAlignment );
		lbl.setText( txt );
		lbl.setIcon( icon );
		return lbl;
	}	
	
	/**
	 * initialize icons for level entities and return right icon
	 * @param id of Level entity
	 * @return icon for Level entity or null if id not found
	 */
	private Icon getLevelIcon( int id ) {
		if ( levelIcons == null ) {
			levelIcons  = new ImageIcon[] { ResourceKeeper.getIcon( IType.LEVEL0, size ),
					ResourceKeeper.getIcon( IType.LEVEL1, size ), 
					ResourceKeeper.getIcon( IType.LEVEL2, size ),
					ResourceKeeper.getIcon( IType.LEVEL3, size ),
					ResourceKeeper.getIcon( IType.LEVEL4, size ),
					ResourceKeeper.getIcon( IType.LEVEL5, size ) };
		}
		return ( ( ( id < 0 ) || ( id > 5 ) ) ? null : levelIcons[id] ); 
	}

	/**
	 * initialize icons for Competence entities and return right icon
	 * @param variety of Competence entity
	 * @return icon for Competence entity or null if variety is wrong
	 */
	private Icon getCompetenceIcon( short variety ) {
		if ( competenceIcons == null ) {
			competenceIcons = new ImageIcon[] { null, 
					ResourceKeeper.getIcon( IType.COMPETENCE_PROF, size ),
					ResourceKeeper.getIcon( IType.COMPETENCE_PERS, size ),
					ResourceKeeper.getIcon( IType.COMPETENCE_ADM, size ) };
		}
		return ( ( ( variety < 1 ) || ( variety > 3 ) ) ? null : competenceIcons[variety] ); 
	}

	/**
	 * initialize icons for Catalog entities and return right icon
	 * @param variety of Catalog entity
	 * @return icon for Catalog entity or null if variety is wrong
	 */
	private Icon getCatalogIcon( short variety ) {
		if ( catalogIcons == null ) {
			catalogIcons = new ImageIcon[] { 
					ResourceKeeper.getIcon( IType.CATALOG_SIMPLE, size ), 
					ResourceKeeper.getIcon( IType.CATALOG_PROF, size ),
					ResourceKeeper.getIcon( IType.CATALOG_PERS, size ),
					ResourceKeeper.getIcon( IType.CATALOG_ADM, size ) };
		}
		return ( ( ( variety < 0 ) || ( variety > 3 ) ) ? null : catalogIcons[variety] ); 
	}

	/**
	 * initialize icon for Indicator entity and return right icon
	 * @param variety of Catalog entity
	 * @return icon for Catalog entity or null if variety is wrong
	 */
	private Icon getIndicatorIcon( boolean visibleOnlyIndicator, Level level ) {
		if ( indicatorIcon == null ) indicatorIcon = ResourceKeeper.getIcon( IType.INDICATOR, size );
		if ( visibleOnlyIndicator ) return indicatorIcon;

		if ( indicatorLevelIcon == null ) {
			indicatorLevelIcon = new Icon[] { 					
					IconJoiner.HJoiner( 0, 0, 1, indicatorIcon, ResourceKeeper.getIcon( IType.LEVEL0, size ) ),
					IconJoiner.HJoiner( 0, 0, 1, indicatorIcon, ResourceKeeper.getIcon( IType.LEVEL1, size ) ),
					IconJoiner.HJoiner( 0, 0, 1, indicatorIcon, ResourceKeeper.getIcon( IType.LEVEL2, size ) ),
					IconJoiner.HJoiner( 0, 0, 1, indicatorIcon, ResourceKeeper.getIcon( IType.LEVEL3, size ) ),
					IconJoiner.HJoiner( 0, 0, 1, indicatorIcon, ResourceKeeper.getIcon( IType.LEVEL4, size ) ),
					IconJoiner.HJoiner( 0, 0, 1, indicatorIcon, ResourceKeeper.getIcon( IType.LEVEL5, size ) ) };
		}
		return indicatorLevelIcon[level == null ? 0 : level.getId()];
	}
	
	/**
	 * initialize icon for Operation enum and return right icon
	 * @param variety of Catalog entity
	 * @return icon for Catalog entity or null if variety is wrong
	 */
	private Icon getOperationIcon( Operation op ) {
		if ( ( op == Operation.UPDATE ) || ( op == Operation.CREATE ) ) {
			if ( updateIcon == null ) updateIcon = ResourceKeeper.getIcon( IType.PENCIL, size );
			return updateIcon;
		}
		if ( op == Operation.DELETE ) {
			if ( deleteIcon == null ) deleteIcon = ResourceKeeper.getIcon( IType.DELETE, size );
			return deleteIcon;
		}
		return null;
	}
	
	/**
	 * initialize icon for ProfilePattern entity and return right icon
	 * @param status of ProfilePattern entity
	 * @return icon for ProfilePattern entity or null if status is wrong
	 */
	private Icon getProfilePatternIcon( short status ) {
		if ( ppIcon == null ) {
			ImageIcon ppi = ResourceKeeper.getIcon( ResourceKeeper.IType.PROFILE_PATTERN, size );
			ImageIcon flgi = ResourceKeeper.getIcon( ResourceKeeper.IType.FORK, size );
			ImageIcon dlti = ResourceKeeper.getIcon( ResourceKeeper.IType.DELETE, size );
			ImageIcon crni = ResourceKeeper.getIcon( ResourceKeeper.IType.HAMMER, size );
			ppIcon = new Icon[] { IconJoiner.HJoiner( 0, 0, 1, ppi, flgi ), 
					null,
					IconJoiner.HJoiner( 0, 0, 1, ppi, dlti ),
					IconJoiner.HJoiner( 0, 0, 1, ppi, crni ) };
		}
		if ( ( status < 0 ) || ( status >= ppIcon.length ) ) return null;
		return ppIcon[status];
	}
		
	/**
	 * initialize icon for ProfilePatternElement entity and return right icon
	 * @param status of ProfilePatternElement entity
	 * @return icon for ProfilePatternElement entity or null if status is wrong
	 */
	private Icon getProfilePatternElementIcon( ProfilePatternElement ppe ) {
		if ( ( ppe == null ) || ( ppe.getMinLevel() == null ) || ( ppe.getCompetence() == null ) ) return null;
		if ( ppeIcons == null ) ppeIcons = new TreeMap<>();		

		int code = ( ppe.getMinLevel().getId() << 16 ) + ppe.getCompetence().getVariety();
		Icon icon = ppeIcons.get( code );
		if ( icon == null ) {
			icon = IconJoiner.HJoiner(0, 0, 2, getCompetenceIcon( ppe.getCompetence().getVariety() ), 
					getLevelIcon( ppe.getMinLevel().getId() ) );
			ppeIcons.put( code, icon );
		}
		return icon;
	}

	/**
	 * initialize icon for Division entity and return right icon
	 * @return icon for Division entity
	 */
	private Icon getDivisionIcon() {
		if ( divisionIcon == null ) divisionIcon = ResourceKeeper.getIcon( IType.OFFICE, size );
		return divisionIcon;
	}
	
	private Icon getEPostIcon( int kpers ) {
		if ( epostIcons == null ) {
			epostIcons = new Icon [] { ResourceKeeper.getIcon( IType.WORKER0, size ), 
                    ResourceKeeper.getIcon( IType.WORKER1, size ), 
                    ResourceKeeper.getIcon( IType.WORKER2, size ), 
                    ResourceKeeper.getIcon( IType.WORKER3, size ), 
                    ResourceKeeper.getIcon( IType.WORKER4, size ) };
		}
		kpers /= 10;
		return ( kpers < 1 ? epostIcons[0] : ( kpers > 4 ? epostIcons[4] : epostIcons[kpers] ) );
	}
	
	private Icon getProfileIcon() {
		if ( profileIcon == null ) profileIcon = ResourceKeeper.getIcon( IType.PROFILE, size );
		return profileIcon;
	}
}
