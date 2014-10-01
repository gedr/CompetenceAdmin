package minos.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.text.JTextComponent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.gedr.util.tuple.Pair;
import net.miginfocom.swing.MigLayout;

import com.alee.extended.checkbox.CheckState;
import com.alee.extended.tree.WebCheckBoxTree;
import com.alee.laf.button.WebButton;
import com.alee.laf.label.WebLabel;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.scroll.WebScrollPane;
import com.alee.laf.splitpane.WebSplitPane;
import com.alee.laf.text.WebTextArea;
import com.google.gson.Gson;

import minos.data.orm.OrmHelper;
import minos.data.orm.OrmHelper.QueryType;
import minos.data.services.FilialInfo;
import minos.data.services.ProfilePatternSummary;
import minos.entities.BasisPost;
import minos.entities.Faset;
import minos.entities.ProfilePattern;
import minos.ui.ComponentFabrica;
import minos.ui.adapters.ActionAdapter;
import minos.utils.ResourceKeeper;
import minos.utils.ResourceKeeper.IType;
import minos.utils.ResourceKeeper.OType;
import minos.utils.ResourceKeeper.QType;

public class ProfilePatternDlg extends BasisDlg<ProfilePattern> implements TreeCellRenderer {
	// =================================================================================================================
	// Constants
	// =================================================================================================================
	private static final long serialVersionUID = 1L;
	private static final int ICON_SIZE = 48;
	
	private static final String ROOT_BASE_POST_NODE	= "Базовые должности";
	private static final String ROOT_FASET_NODE		= "Фасеты";
	private static final String ROOT_FILIAL_NODE 	= "Филиалы";

	private static final List<Integer> fasetTypes = Arrays.asList( 2, 3, 7, 11, 12, 99 );
	// =================================================================================================================
	// Fields
	// =================================================================================================================
	private static Logger log = LoggerFactory.getLogger( ProfilePatternDlg.class );
	
	private JTextComponent txtName;
	private JTextComponent txtDescr;
	private WebLabel lbl;
	private WebCheckBoxTree<DefaultMutableTreeNode> filterTree;
	
	DefaultMutableTreeNode basePostRootNode;
	DefaultMutableTreeNode fasetRootNode;
	DefaultMutableTreeNode filialRootNode;

	// =================================================================================================================
	// Constructors
	// =================================================================================================================
	public ProfilePatternDlg( Window owner, String title, ProfilePattern source, boolean readOnly ) {
		super( owner, title, source, readOnly );
	}

	// =================================================================================================================
	// Methods for/from SuperClass/Interfaces
	// =================================================================================================================
	@Override
	protected void initUI() {
		WebSplitPane splitPane = new WebSplitPane( WebSplitPane.HORIZONTAL_SPLIT, makeBody(), makeFilter() );
		splitPane.setPreferredSize( new Dimension( 450, 200 ) );
		splitPane.setDividerLocation( 250 );
		splitPane.setOneTouchExpandable( true );		 
		splitPane.setContinuousLayout( true );
		
		setLayout( new BorderLayout() );
		add( splitPane, BorderLayout.CENTER );
		add( makeButtons(), BorderLayout.SOUTH );
	}

	@Override
	protected void save() {
		if ( readOnly || ( txtName.getText() == null ) || ( txtName.getText().trim().length() == 0 ) ) return;
		ProfilePattern pp = new ProfilePattern( txtName.getText(), txtDescr.getText(), null, 1, 
				ProfilePattern.STATUS_BUILDING, null, 
				( Timestamp ) ResourceKeeper.getObject( OType.DAMNED_FUTURE ), 
				source == null ? null : source.getCatalog(), source == null ? null : source.getJournal() );
		if ( source != null ) pp.setId( source.getId() );
		saveFilialMask( pp );
		saveSummary( pp );
		if ( source == null ) { 
			result = pp;
			return;
		}
		if ( !pp.getName().equals( source.getName() )
				|| ( ( pp.getDescription() != null ) && !pp.getDescription().equals( source.getDescription() ) )
				|| ( ( source.getDescription() != null ) && !source.getDescription().equals( pp.getDescription() ) )
				|| ( ( pp.getSummary() != null ) && !pp.getSummary().equals( source.getSummary() ) )
				|| ( ( source.getSummary() != null ) && !source.getSummary().equals( pp.getSummary() ) )
				|| !Arrays.equals( pp.getFilialMask(), source.getFilialMask() )	) {
			result = pp;
		}
	}
	
	@Override
	public Component getTreeCellRendererComponent( JTree tree, Object value, boolean selected, 
			boolean expanded, boolean leaf, int row, boolean hasFocus ) {
		if ( lbl == null ) {
			lbl = new WebLabel();
			lbl.setBorder( null );
			lbl.setOpaque( false );
		}
		lbl.setBackground( selected ? Color.LIGHT_GRAY : Color.WHITE );
		if ( ( value == null ) || !(value instanceof DefaultMutableTreeNode ) 
				|| ( ( ( DefaultMutableTreeNode ) value ).getUserObject() == null ) ) {
			lbl.setText( "" );
			return lbl;
		}
		DefaultMutableTreeNode node = ( DefaultMutableTreeNode ) value;
		if ( node.getUserObject() instanceof Pair<?,?> ) {
			@SuppressWarnings("unchecked")
			Pair<String, Integer> p = ( Pair<String, Integer> ) node.getUserObject();
			lbl.setText( p.getFirst() );
			return lbl;
		}
		if ( node.getUserObject() instanceof BasisPost ) {
			BasisPost bp = ( BasisPost ) node.getUserObject();
			lbl.setText( bp.getName() );
			return lbl;
		}
		if ( node.getUserObject() instanceof FilialInfo ) {
			FilialInfo fi = ( FilialInfo ) node.getUserObject();
			lbl.setText( fi.getName() );
			return lbl;
		}
		lbl.setText( node.getUserObject().toString() );		
		return lbl;
	}

	// =================================================================================================================
	// Methods
	// =================================================================================================================
	public static ProfilePattern show( Window owner, String title, ProfilePattern catalog, boolean readOnly ) {
		ProfilePatternDlg dlg = new ProfilePatternDlg( owner, title, catalog, readOnly );
		dlg.setDefaultCloseOperation( DISPOSE_ON_CLOSE );
		dlg.setModal( true );
		dlg.pack();
		dlg.setVisible( true );	
		return dlg.getResult();		
	}	

	private Component makeBody() {
		ImageIcon icon = ( readOnly ? ResourceKeeper.getIcon( IType.PROFILE_PATTERN, ICON_SIZE ) 
				: ( source == null ? ResourceKeeper.getIcon( IType.PROFILE_PATTERN_ADD, ICON_SIZE ) 
						: ResourceKeeper.getIcon( IType.PROFILE_PATTERN_EDIT, ICON_SIZE ) ) ); 
		txtName = ComponentFabrica.createOneLineTextEditor( 30, readOnly, "Введите название шаблона профиля...", 
				source == null ? null : source.getName() );
		txtDescr = ComponentFabrica.createMultiLineTextEditor( readOnly, source == null ? null : source.getDescription() );

		WebPanel pnl = new WebPanel( new MigLayout( "", "[][grow]", source == null ? "[][][][grow]" : "[][][][grow][]" ) );
		pnl.add( new WebLabel( icon ), "cell 0 0 1 3,gapx 0 10,gapy 0 10" );
		pnl.add( new WebLabel( "Название шаблона профиля" ), "cell 1 0,growx,aligny top" );
		pnl.add( txtName, "cell 1 1,growx,aligny top" );
		pnl.add( new WebLabel( "Описание" ), "cell 1 2,growx,aligny bottom,gapy 5 0" );
		pnl.add( new WebScrollPane( txtDescr ), "cell 0 3 2 1,grow");		
		if ( source != null )  pnl.add( ComponentFabrica.createCollapsingPane( null, "Детали", makeTechInfo(), 
				new Dimension( 0, 100 ) ), "cell 0 4, span, growx, wrap" );	
		return pnl;
	}

	/**
	 * make filter tree component
	 * @param pp  
	 * @return WebCheckBoxTree object
	 */
	private Component makeFilter() {
		basePostRootNode= initBasePosts();
		fasetRootNode 	= initFasets();
		filialRootNode	= initFilials();
		DefaultMutableTreeNode root = new DefaultMutableTreeNode();
		if ( basePostRootNode != null ) root.add( basePostRootNode );
		if ( fasetRootNode != null ) root.add( fasetRootNode );
		if ( filialRootNode != null ) root.add( filialRootNode );
		
		filterTree = new WebCheckBoxTree<>( root );	
		filterTree.setEditable( false );
		filterTree.setCheckingEnabled( !readOnly );
		filterTree.setRootVisible( false );
		filterTree.setCellRenderer( this );
		filterTree.setPaintLines( false );
		viewFilialMask( source );
		viewSummary( source );
		// filterTree.expandAll();
		return new WebScrollPane( filterTree );
	}	

	private Component makeButtons() {
		WebPanel pnl = new WebPanel( new FlowLayout( FlowLayout.RIGHT ) );
		if ( !readOnly ) pnl.add( new WebButton( ActionAdapter.build( "OK", ResourceKeeper.getIcon( IType.OK, 24 ), CMD_OK, 
				"Сохранить введеные данные", this, KeyEvent.VK_ENTER ) ), "cell 1 4,alignx right" );
		pnl.add( new WebButton( ActionAdapter.build( "Отмена", ResourceKeeper.getIcon( IType.CANCEL, 24 ), CMD_CANCEL, 
				"Выйти без сохранения", this, KeyEvent.VK_ESCAPE ) ), "cell 1 4,alignx right" );
		return pnl;
	}
	
	private Component makeTechInfo() {
		if ( source == null ) return null;
		WebTextArea txtArea = new WebTextArea();
		txtArea.setEditable( false );
		txtArea.append( "Код: " + source.getId() );
		txtArea.append( "\nСтатус: " + source.getStatus() );
		txtArea.append( "\nСоздан: " + source.getJournal().getCreateMoment() );
		if ( source.getJournal().getCreateMoment() != source.getJournal().getEditMoment() ) {
			txtArea.append( "\nРедактирован: " + source.getJournal().getEditMoment() );
		}
		return txtArea;
	}

	
	/**
	 * Initialized base post nodes
	 * @return root base post node or null otherwise
	 */
	private DefaultMutableTreeNode initBasePosts() {
		List<Pair<String, Byte>> posts = ResourceKeeper.getObject( ResourceKeeper.OType.BASE_POST_LIST );
		if ( posts == null ) {
			if ( ( log != null ) && log.isWarnEnabled() ) log.warn( "ProfilePatternDlg.initBasePost() : "
					+ "resourese BASE_POST_LIST not found" );
			return null;
		}		
		List<BasisPost> lbp = OrmHelper.findByQuery( QueryType.JPQL, 
				ResourceKeeper.getQuery( QType.JPQL_LOAD_BASE_POST ), BasisPost.class );
		if ( ( lbp == null ) || ( lbp.size() == 0 ) ) {
			if ( ( log != null ) && log.isWarnEnabled() ) log.warn( "ProfilePatternDlg.initBasePost() : not found base post" );
			return null;			
		}
		DefaultMutableTreeNode rootPost = new DefaultMutableTreeNode( ROOT_BASE_POST_NODE );
		DefaultMutableTreeNode subPost = null;
		BasisPost prev = null;
		
		for( BasisPost bp : lbp ) {
			byte kpers = ( byte)  ( bp.getKpers() / 10 ) ;
			if ( ( prev == null ) || ( ( ( byte ) ( prev.getKpers() / 10 ) ) !=  kpers ) ) {
				subPost = null;
				for( Pair<String, Byte> p : posts ) {
					if ( p.getSecond() == kpers ) {
						subPost = new DefaultMutableTreeNode( p.getFirst() );
						break;
					}
				}
				if ( subPost != null ) rootPost.add( subPost );				
			}
			prev = bp;
			if ( subPost != null ) subPost.add( new DefaultMutableTreeNode( bp ) );			
		}
		return rootPost;
	}
	
	/**
	 * Initialized faset nodes
	 * @return root faset or null otherwise
	 */
	private DefaultMutableTreeNode initFasets() {
		List<Faset> lf = OrmHelper.findByQueryWithParam( QueryType.JPQL, 
				ResourceKeeper.getQuery( QType.JPQL_LOAD_FASET ),
				Faset.class,
				new Pair<Object, Object>( "types", fasetTypes ) );
		if ( ( lf == null ) || ( lf.size() == 0 ) ) {
			if ( ( log != null ) && log.isWarnEnabled() ) log.warn( "ProfilePatternDlg.initBasePost() : not found faset" );
			return null;			
		}		
		DefaultMutableTreeNode rootFaset = new DefaultMutableTreeNode( ROOT_FASET_NODE );
		DefaultMutableTreeNode subFaset = null;
		Faset prevFaset = null;
		StringBuilder sb = new StringBuilder();
		Pair<String, Integer> emptyFaset = new Pair<String, Integer>( "Пустой фасет", 0 );
		for ( Faset f : lf ) {
			if ( ( prevFaset == null ) || ( prevFaset.getType() != f.getType() ) ) {
				sb.delete( 0, sb.length() ).append( "Фасет [" ).append( f.getType() ).append( "]" );
				subFaset = new DefaultMutableTreeNode( new Pair<String, Integer>( sb.toString(), f.getType() ) );				
				rootFaset.add( subFaset );
				subFaset.add( new DefaultMutableTreeNode( emptyFaset ) );
			}
			prevFaset = f;
			if ( f.getCode() != -1 ) {
				subFaset.add( new DefaultMutableTreeNode( new Pair<String, Integer>( f.getName(), f.getCode() ) ) );
				continue;
			}
			@SuppressWarnings("unchecked")
			Pair<String, Integer> p = ( Pair<String, Integer> ) subFaset.getUserObject() ;
			sb.delete( 0, sb.length() ).append( p.getFirst() ).append( " - " ).append( f.getName() );
			p.setFirst( sb.toString() );
		}
		return rootFaset;
	}

	/**
	 * Initialized filial nodes
	 * @return root filial or null otherwise
	 */
	private DefaultMutableTreeNode initFilials() {
		List<FilialInfo> lfi = ResourceKeeper.getObject( ResourceKeeper.OType.BRANCH_OFFICES_INFO );
		if ( lfi == null ) {
			if ( ( log != null ) && log.isWarnEnabled() ) log.warn( "EntitiesDlg.initBasePost() : resourese BRANCH_OFFICES_INFO not found" );
			return null;
		}		
		DefaultMutableTreeNode rootFilial = new DefaultMutableTreeNode( ROOT_FILIAL_NODE );			
		for( FilialInfo fi : lfi ) {
			if ( ( fi.getCode() > 0 ) && ( fi.getCode() < 100 ) ) rootFilial.add( new DefaultMutableTreeNode( fi ) );
		}
		return rootFilial;
	}
	
	
	/**
	 * display filial's mask from ProfilePattern entity on tree
	 * @param pp - ProfilePattern entity
	 */
	private void viewFilialMask( ProfilePattern pp ) {
		if ( ( pp == null ) || ( pp.getFilialMask() == null ) || ( filterTree == null ) 
				|| ( filialRootNode == null ) ) return;
		for ( int i = 0; i < filialRootNode.getChildCount(); i++ ) {
			DefaultMutableTreeNode child = ( DefaultMutableTreeNode ) filialRootNode.getChildAt( i );
			if ( ( child == null ) || ( child.getUserObject() == null ) 
					|| !( child.getUserObject() instanceof FilialInfo ) ) continue;
			FilialInfo fi = ( FilialInfo ) child.getUserObject();
			int cell = fi.getShift() / 8;
			int diff = fi.getShift() % 8;
			if ( cell >= pp.getFilialMask().length ) continue;
			filterTree.setChecked( child, ( pp.getFilialMask()[cell] & ( 1 << diff ) ) != 0 ? true : false );
		}
	}

	/**
	 * display summary from ProfilePattern entity on tree
	 * @param pp - ProfilePattern entity
	 */
	private void viewSummary( ProfilePattern pp ) {
		if ( ( pp == null ) || ( pp.getSummary() == null ) || ( filterTree == null ) ) return;
		try {
			Gson gson = new Gson();
			ProfilePatternSummary pps = gson.fromJson( pp.getSummary(), ProfilePatternSummary.class );
			viewFaset( pps );
			viewBasisPost ( pps );
		} catch ( Exception ex ) {
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( "ProfilePatternDlg.viewSummary() : ", ex );
		}
	}

	/** 
	 * display basis post information from ProfilePatternSummary object 
	 * @param pps - existing ProfilePatternSummary object
	 */
	private void viewBasisPost( ProfilePatternSummary pps ) {
		if ( ( pps == null ) || ( pps.getBasisPosts() == null ) )  return;
		for ( int i = 0; i < basePostRootNode.getChildCount(); i++ ) {
			DefaultMutableTreeNode child = ( DefaultMutableTreeNode ) basePostRootNode.getChildAt( i );
			if ( child == null ) continue;
			for ( int j = 0; j < child.getChildCount(); j++ ) {
				DefaultMutableTreeNode grandson = ( DefaultMutableTreeNode ) child.getChildAt( j );
				if ( ( grandson == null ) || ( grandson.getUserObject() == null )
						|| !( grandson.getUserObject() instanceof BasisPost ) ) continue;
				filterTree.setChecked( grandson, 
						pps.getBasisPosts().contains( ( ( BasisPost ) grandson.getUserObject() ).getId() ) );
			}
		}
	}

	/**
	 * display faset information from ProfilePatternSummary object
	 * @param pps - existint ProfilePatternSummary object
	 */
	private void viewFaset( ProfilePatternSummary pps ) {
		if ( ( pps == null ) || ( pps.getFasets() == null )|| ( fasetRootNode == null ) ) return;

		for ( int i = 0; i < fasetRootNode.getChildCount(); i++ ) {
			DefaultMutableTreeNode child = ( DefaultMutableTreeNode ) fasetRootNode.getChildAt( i );			
			if ( ( child == null ) || ( child.getUserObject() == null ) 
					|| !( child.getUserObject() instanceof Pair<?, ?> ) )continue;
			@SuppressWarnings("unchecked")
			Pair<String, Integer> p = ( Pair<String, Integer> ) child.getUserObject();
			List<Integer> li = pps.getFasets().get( p.getSecond() );
			if ( ( li == null ) || ( li.size() == 0 ) ) continue;
			
			for ( int j = 0; j < child.getChildCount(); j++ ) {
				DefaultMutableTreeNode grandson = ( DefaultMutableTreeNode ) child.getChildAt( j );
				if ( ( grandson == null ) || ( grandson.getUserObject() == null ) 
						|| !( grandson.getUserObject() instanceof Pair<?, ?> ) ) continue;
				@SuppressWarnings("unchecked")
				Pair<String, Integer> p2 = ( Pair<String, Integer> ) grandson.getUserObject();
				if ( li.contains( p2.getSecond() ) ) filterTree.setChecked( grandson, true );
			}			
		}
	}

	/**
	 * save checked filial's list as byte array 
	 * @param pp - ProfilePattern entity
	 * @return ProfilePattern entity with fill filial mask if success , otherwise null
	 */
	private ProfilePattern saveFilialMask( ProfilePattern pp ) {
		if ( ( pp == null ) || ( filterTree == null ) || ( filialRootNode == null ) ) return null;

		byte[] arr = new byte[ProfilePattern.MAX_FILIAL_MASK_SIZE];
		int maxCell = -1;
		
		Arrays.fill( arr, ( byte ) 0 ); 
		for ( int i = 0; i < filialRootNode.getChildCount(); i++ ) {
			DefaultMutableTreeNode child = ( DefaultMutableTreeNode ) filialRootNode.getChildAt( i );
			if ( ( child == null ) || ( child.getUserObject() == null ) 
					|| !( child.getUserObject() instanceof FilialInfo ) 
					|| ( filterTree.getCheckState( child ) != CheckState.checked ) ) continue;			
			FilialInfo fi = ( FilialInfo ) child.getUserObject();
			int cell = fi.getShift() / 8;
			int diff = fi.getShift() % 8;
			if ( cell >= arr.length ) continue;
			if ( cell > maxCell ) maxCell = cell;
			arr[cell] |= ( 1 << diff );			
		}
		if ( maxCell == -1 ) return null;
		pp.setFilialMask( Arrays.copyOfRange( arr, 0, maxCell + 1 ) );
		return pp;
	}

	/**
	 * save basis post and faset data in ProfilePattern as GSON string of ProfilePatternSummary object
	 * @param pp - existing ProfilePattern object
	 * @return ProfilePattern object if success or null otherwise
	 */
	private ProfilePattern saveSummary( ProfilePattern pp ) {
		if ( ( pp == null ) || ( filterTree == null ) 
				|| ( ( basePostRootNode == null ) && ( fasetRootNode == null ) ) ) return null;
		
		ProfilePatternSummary pps = new ProfilePatternSummary( new ArrayList<Integer>(), 
				new TreeMap<Integer, List<Integer>>() );
		saveBasisPost( pps );
		saveFaset( pps );
		Gson gson = new Gson();
		pp.setSummary( gson.toJson( pps ) );		
		return pp;
	}
	
	/**
	 * save checked faset in list
	 * @param pps - existing ProfilePatternSummary object
	 */
	private void saveFaset( ProfilePatternSummary pps ) {
		if ( ( pps == null ) || ( pps.getFasets() == null ) )  return;
		for ( int i = 0; i < fasetRootNode.getChildCount(); i++ ) {
			DefaultMutableTreeNode child = ( DefaultMutableTreeNode ) fasetRootNode.getChildAt( i );
			if ( ( child == null ) || ( child.getUserObject() == null )
					|| !( child.getUserObject() instanceof Pair<?, ?> )
					|| ( filterTree.getCheckState( child ) == CheckState.unchecked ) ) continue;
			@SuppressWarnings("unchecked")
			Pair<String, Integer> p = ( Pair<String, Integer> ) child.getUserObject();
			List<Integer> li = new ArrayList<Integer>();
			pps.getFasets().put( p.getSecond(), li );
			
			for ( int j = 0; j < child.getChildCount(); j++ ) {
				DefaultMutableTreeNode grandson = ( DefaultMutableTreeNode ) child.getChildAt( j );
				if ( ( grandson == null ) || ( grandson.getUserObject() == null )
						|| !( grandson.getUserObject() instanceof Pair<?, ?> )
						||( filterTree.getCheckState( grandson ) != CheckState.checked ) ) continue;
				@SuppressWarnings("unchecked")
				Pair<String, Integer> p2 = ( Pair<String, Integer> ) grandson.getUserObject();
				li.add( p2.getSecond() );
			}			
		}
	}
	
	/** 
	 * save checked basis post in list of IDs 
	 * @param pps - existing ProfilePatternSummary object
	 */
	private void saveBasisPost( ProfilePatternSummary pps ) {
		if ( ( pps == null ) || ( pps.getBasisPosts() == null ) ) return;
		for ( int i = 0; i < basePostRootNode.getChildCount(); i++ ) {
			DefaultMutableTreeNode child = ( DefaultMutableTreeNode ) basePostRootNode.getChildAt( i );
			if ( filterTree.getCheckState( child ) == CheckState.unchecked ) continue;
			for ( int j = 0; j < child.getChildCount(); j++ ) {
				DefaultMutableTreeNode grandson = ( DefaultMutableTreeNode ) child.getChildAt( j );
				if ( ( grandson == null ) || ( grandson.getUserObject() == null )
						|| !( grandson.getUserObject() instanceof BasisPost )
						|| ( filterTree.getCheckState( grandson ) != CheckState.checked ) ) continue;
				pps.getBasisPosts().add( ( ( BasisPost ) grandson.getUserObject() ).getId() );
			}
		}
	}
}