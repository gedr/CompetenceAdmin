package minos.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.JTree;
import javax.swing.ListCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.gedr.util.tuple.Pair;
import minos.data.orm.OrmHelper;
import minos.data.orm.OrmHelper.QueryType;
import minos.data.services.FilialInfo;
import minos.data.services.ProfilePatternSummary;
import minos.entities.BasisPost;
import minos.entities.Catalog;
import minos.entities.Competence;
import minos.entities.Faset;
import minos.entities.Indicator;
import minos.entities.Level;
import minos.entities.Measure;
import minos.entities.ProfilePattern;
import minos.entities.ProfilePatternElement;
import minos.ui.adapters.ActionAdapter;
import minos.utils.ResourceKeeper;
import minos.utils.ResourceKeeper.IType;
import minos.utils.ResourceKeeper.OType;
import minos.utils.ResourceKeeper.QType;
import net.miginfocom.swing.MigLayout;

import com.alee.extended.checkbox.CheckState;
import com.alee.extended.date.WebDateField;
import com.alee.extended.panel.CollapsiblePaneListener;
import com.alee.extended.panel.WebCollapsiblePane;
import com.alee.extended.tree.WebCheckBoxTree;
import com.alee.laf.button.WebButton;
import com.alee.laf.combobox.WebComboBox;
import com.alee.laf.label.WebLabel;
import com.alee.laf.list.WebList;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.rootpane.WebDialog;
import com.alee.laf.scroll.WebScrollPane;
import com.alee.laf.splitpane.WebSplitPane;
import com.alee.laf.text.WebTextArea;
import com.alee.laf.text.WebTextField;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class EntitiesDlg extends WebDialog implements ActionListener, CollapsiblePaneListener, TreeCellRenderer, ListCellRenderer<Level> {	
	//=====================================================================================================
	//=                                             Constants                                             =
	//=====================================================================================================
	private static final long serialVersionUID = 1L;

	private static final String CANCEL_CMD 			= "0";
	private static final String CATALOG_OK_CMD 		= "1";
	private static final String COMPETENCE_OK_CMD 	= "2";
	private static final String INDICATOR_OK_CMD 	= "3";
	private static final String PP_OK_CMD			= "4";
	private static final String PPE_OK_CMD			= "5";	
	private static final String MEASURE_OK_CMD		= "6";
	
	private static final String ROOT_BASE_POST_NODE	= "Базовые должности";
	private static final String ROOT_FASET_NODE		= "Фасеты";
	private static final String ROOT_FILIAL_NODE 	= "Филиалы";
	
	//=====================================================================================================
	//=                                            Attributes                                             =
	//=====================================================================================================
	private static Logger log = LoggerFactory.getLogger( EntitiesDlg.class );
	
	private WebTextField txtField = null;
	private WebTextArea txtArea = null;
	private WebComboBox cmb = null;
	private WebLabel renderLabel = null;
	private WebCheckBoxTree<DefaultMutableTreeNode> filterTree = null;
	private WebDateField start = null;
	private WebDateField stop = null;
	
	private Object source = null;
	private Object result = null;
	private Icon[] levelIcons = null; 
	private boolean readOnly = true;
	DefaultMutableTreeNode basePostRootNode = null;
	DefaultMutableTreeNode fasetRootNode 	= null;
	DefaultMutableTreeNode filialRootNode	= null;

	//=====================================================================================================
	//=                                           Constructors                                            =
	//=====================================================================================================
	public EntitiesDlg( Window owner, String title, Catalog catalog, boolean readOnly, ImageIcon icon ) {
		super(owner, title);
		this.source = catalog;
		this.readOnly = readOnly;

		setLayout( new MigLayout( "", "[][grow]", "[][][][][]" ) ); 
		add( new WebLabel( icon ), "cell 0 0 1 3,gapx 0 10,gapy 0 10" );
		add( new WebLabel( "Название каталога" ), "cell 1 0,growx,aligny top" );

		add( initTextField( 30, "Введите название каталога...", catalog == null ? null : catalog.getName() ), 
				"cell 1 1,growx,aligny top" );
		if ( !readOnly ) add( new WebButton( ActionAdapter.build( "OK", null, CATALOG_OK_CMD, "Сохранить", this, KeyEvent.VK_ENTER ) ), 
					"cell 1 2,alignx right" );
		
		add( new WebButton( ActionAdapter.build( "Отмена", null, CANCEL_CMD, "Выйти без сохранения", this, KeyEvent.VK_ESCAPE ) ), 
				"cell 1 2,alignx right" );

		if ( catalog != null )  {
			add( makeCollapsingPane( null, "Детали", makeTechInfo( catalog ), new Dimension( 0, 100 ) ), 
					"cell 0 3, span, growx, wrap" );	
			Component c = makeHistoryInfo( catalog );
			if ( c != null ) add( makeCollapsingPane( null, "История", c, new Dimension( 0, 100 ) ), "cell 0 4, span, growx, wrap" );
		}
	}

	public EntitiesDlg( Window owner, String title, Competence competence, boolean readOnly, ImageIcon icon ) {
		super(owner, title);
		this.source = competence;
		this.readOnly = readOnly;

		setLayout( new MigLayout( "", "[][grow]", "[][][][][]" ) ); 
		add( new WebLabel( icon ), "cell 0 0 1 3,gapx 0 10,gapy 0 10" );
		add( new WebLabel( "Название компетенции" ), "cell 1 0,growx,aligny top" );
		add( initTextField( 30, "Введите название компетенции...", competence == null ? null : competence.getName() ), 
				"cell 1 1,growx,aligny top" );
		add( new WebLabel( "Описание компетенции" ), "cell 1 2,growx,aligny bottom,gapy 5 0" );
		add( new WebScrollPane( initTextArea( competence == null ? null : competence.getDescription() ) ), "cell 0 3 2 1,grow");
		
		if ( !readOnly ) add( new WebButton( ActionAdapter.build( "OK", null, COMPETENCE_OK_CMD, "Сохранить", this, KeyEvent.VK_ENTER ) ), 
					"cell 1 4,alignx right" );
		
		add( new WebButton( ActionAdapter.build( "Отмена", null, CANCEL_CMD, "Выйти без сохранения", this, KeyEvent.VK_ESCAPE ) ), 
				"cell 1 4,alignx right" );

		if ( competence != null )  {
			add( makeCollapsingPane( null, "Детали", makeTechInfo( competence ), new Dimension( 0, 100 ) ), 
					"cell 0 5, span, growx, wrap" );	
			Component c = makeHistoryInfo( competence );
			if ( c != null ) add( makeCollapsingPane( null, "История", c, new Dimension( 0, 100 ) ), "cell 0 6, span, growx, wrap" );
		}
	}

	public EntitiesDlg( Window owner, String title, Indicator indicator, boolean readOnly, ImageIcon icon ) {
		super(owner, title);
		this.source = indicator;
		this.readOnly = readOnly;	
		
		setLayout( new MigLayout( "", "[][grow]", "[][][][][]" + ( indicator == null ? "" : "[][]" ) ) ); // [grow]
		add( new WebLabel( icon ), "cell 0 0 1 3,gapx 0 10,gapy 0 10" );
		add( new WebLabel( "Название индикатора" ), "cell 1 0,growx,aligny top");
		add( initTextField( 30 , "Введите название индикатора...", ( indicator == null ? null : indicator.getName() ) ), 
				"cell 1 1,growx,a ligny top" );
		add( new WebLabel( "Уровень индикатора" ), "cell 1 2,growx,aligny bottom,gapy 5 0" );			
		add( initLevelCombobox( indicator == null ? null : indicator.getLevel() ), "cell 0 3 2 1,grow" );

		if ( !readOnly ) {
			add( new WebButton( ActionAdapter.build( "OK", null, INDICATOR_OK_CMD, "Сохранить ", this, KeyEvent.VK_ENTER ) ), 
					"cell 1 4,alignx right" );
		}			
		add( new WebButton( ActionAdapter.build( "Отмена", null, CANCEL_CMD, "Выйти без сохранения", this, KeyEvent.VK_ESCAPE ) ), 
				"cell 1 4,alignx right" );

		if ( indicator != null )  {
			add( makeCollapsingPane( null, "Детали", makeTechInfo( indicator ), new Dimension( 0, 100 ) ), 
					"cell 0 5, span, growx, wrap" );	
			Component c = makeHistoryInfo( indicator );
			if ( c != null ) add( makeCollapsingPane( null, "История", c, new Dimension( 0, 100 ) ), "cell 0 6, span, growx, wrap" );
		}
	}
	
	public EntitiesDlg( Window owner, String title, ProfilePatternElement ppe, boolean readOnly, ImageIcon icon ) {
		super(owner, title);
		this.source = ppe;
		this.readOnly = readOnly;	
		
		initTextField( 30 , "", ( ( ppe == null ) || ( ppe.getCompetence() == null ) ) 
				? null : ppe.getCompetence().getName() ).setEditable( false );
		initTextArea( ( ( ppe == null ) || ( ppe.getCompetence() == null ) ) 
				? null : ppe.getCompetence().getDescription() ).setEditable( false );
		initLevelCombobox( ppe == null ? null : ppe.getMinLevel() ).setEnabled( !readOnly ); //.setEditable( readOnly );
		
		setLayout( new MigLayout( "", "[][grow]", "[][][][][][]" ) ); 
		add( new WebLabel( icon ), "cell 0 0 1 3,gapx 0 10,gapy 0 10" );
		add( new WebLabel( "Уровень" ), "cell 1 0,growx,aligny" );			
		add( cmb, "cell 1 1,growx,aligny" );
		add( new WebLabel( "Название компетенции" ), "cell 1 2,growx,aligny" );
		add( txtField, "cell 1 3,growx,aligny top" );
		add( new WebLabel( "Описание компетенции" ), "cell 1 4,growx,aligny bottom,gapy 5 0" );
		add( new WebScrollPane( txtArea ), "cell 0 5 2 1,grow");
		
		if ( !readOnly ) add( new WebButton( ActionAdapter.build( "OK", null, PPE_OK_CMD, "Сохранить", this, KeyEvent.VK_ENTER ) ), 
					"cell 1 6,alignx right" );
		
		add( new WebButton( ActionAdapter.build( "Отмена", null, CANCEL_CMD, "Выйти без сохранения", this, KeyEvent.VK_ESCAPE ) ), 
				"cell 1 6,alignx right" );

		if ( ppe.getCompetence() != null )  {
			add( makeCollapsingPane( null, "Детали", makeTechInfo( ppe ), new Dimension( 0, 100 ) ), 
					"cell 0 7, span, growx, wrap" );	
		}
	}

	public EntitiesDlg( Window owner, String title, ProfilePattern pp, boolean readOnly, ImageIcon icon ) {
		super(owner, title);
		this.source = pp;
		this.readOnly = readOnly;
		
		WebPanel lpanel = new WebPanel( new MigLayout( "", "[][grow]", pp == null ? "[][][][grow]" : "[][][][grow][]" ) );
		lpanel.add( new WebLabel( icon ), "cell 0 0 1 3,gapx 0 10,gapy 0 10" );
		lpanel.add( new WebLabel( "Название шаблона профиля" ), "cell 1 0,growx,aligny top" );
		lpanel.add( initTextField( 30, "Введите название шаблона профиля...", pp == null ? null : pp.getName() ), 
				"cell 1 1,growx,aligny top" );
		lpanel.add( new WebLabel( "Описание" ), "cell 1 2,growx,aligny bottom,gapy 5 0" );
		lpanel.add( new WebScrollPane( initTextArea( pp == null ? null : pp.getDescription() ) ), "cell 0 3 2 1,grow");		
		if ( pp != null )  lpanel.add( makeCollapsingPane( null, "Детали", makeTechInfo( pp ), new Dimension( 0, 100 ) ), 
				"cell 0 4, span, growx, wrap" );	

		WebSplitPane splitPane = new WebSplitPane( WebSplitPane.HORIZONTAL_SPLIT, lpanel, new WebScrollPane( makeFilterTree( pp ) ) );
		splitPane.setPreferredSize( new Dimension( 450, 200 ) );
		splitPane.setDividerLocation( 250 );
		splitPane.setOneTouchExpandable( true );		 
		splitPane.setContinuousLayout( true );
		
		WebPanel btnPanel = new WebPanel( new FlowLayout( FlowLayout.RIGHT ) );
		if ( !readOnly ) btnPanel.add( new WebButton( ActionAdapter.build( "OK", ResourceKeeper.getIcon( IType.OK, 24 ), PP_OK_CMD, 
				"Сохранить введеные данные", this, KeyEvent.VK_ENTER ) ), "cell 1 4,alignx right" );
		btnPanel.add( new WebButton( ActionAdapter.build( "Отмена", ResourceKeeper.getIcon( IType.CANCEL, 24 ), CANCEL_CMD, 
				"Выйти без сохранения", this, KeyEvent.VK_ESCAPE ) ), "cell 1 4,alignx right" );
		
		setLayout( new BorderLayout() );
		add( splitPane, BorderLayout.CENTER );
		add( btnPanel, BorderLayout.SOUTH );
	}

	public EntitiesDlg( Window owner, String title, Measure measure, boolean readOnly, ImageIcon icon ) {
		super(owner, title);
		this.source = measure;
		this.readOnly = readOnly;

		setLayout( new MigLayout( "", "[][grow]", "[][][][][]" ) ); 
		add( new WebLabel( icon ), "cell 0 0 1 3,gapx 0 10,gapy 0 10" );
		add( new WebLabel( "Название мероприятия" ), "cell 1 0,growx,aligny top" );
		add( initTextField( 30, "Введите название мероприятия...", measure == null ? null : measure.getName() ), 
				"cell 1 1,growx,aligny top" );
		add( new WebLabel( "Описание мероприятия" ), "cell 1 2,growx,aligny bottom,gapy 5 0" );
		add( new WebScrollPane( initTextArea( measure == null ? null : measure.getDescription() ) ), "cell 0 3 2 1,grow");
		
		Calendar cal = Calendar.getInstance();
		cal.add( Calendar.DAY_OF_YEAR, 1 );
		start = new WebDateField( cal.getTime() );
		cal.add( Calendar.DAY_OF_YEAR, 7 );
		stop = new WebDateField( cal.getTime() );
		add( start, "cell 0 4 2 1,growx, left" );
		add( stop, "cell 0 4 2 1,growx, right" );

		if ( !readOnly ) add( new WebButton( ActionAdapter.build( "OK", null, MEASURE_OK_CMD, "Сохранить", this, KeyEvent.VK_ENTER ) ), 
					"cell 1 5,alignx right" );
		
		add( new WebButton( ActionAdapter.build( "Отмена", null, CANCEL_CMD, "Выйти без сохранения", this, KeyEvent.VK_ESCAPE ) ), 
				"cell 1 5,alignx right" );

		if ( measure != null )  {
			add( makeCollapsingPane( null, "Детали", makeTechInfo( measure ), new Dimension( 0, 100 ) ), 
					"cell 0 6, span, growx, wrap" );	
		}
	}
	//=====================================================================================================
	//=                                                     Methods                                       =
	//=====================================================================================================
	public static Catalog showCatalogDlg( Window owner, String title, Catalog catalog, boolean readOnly, ImageIcon icon ) {
		EntitiesDlg dlg = new EntitiesDlg( owner, title, ( Catalog ) catalog, readOnly, icon );
		initDlg( dlg );
		return ( Catalog ) dlg.getResult();		
	}	

	public static Competence showCompetenceDlg( Window owner, String title, Competence competence, boolean readOnly, ImageIcon icon ) {
		EntitiesDlg dlg = new EntitiesDlg( owner, title, ( Competence ) competence, readOnly, icon );
		initDlg( dlg );
		return ( Competence ) dlg.getResult();		
	}	

	public static Indicator showIndicatorDlg( Window owner, String title, Indicator indicator, boolean readOnly, ImageIcon icon ) {
		EntitiesDlg dlg = new EntitiesDlg( owner, title, ( Indicator ) indicator, readOnly, icon );
		initDlg( dlg );
		return ( Indicator ) dlg.getResult();		
	}	

	public static ProfilePattern showProfilePatternDlg( Window owner, String title, ProfilePattern pp, boolean readOnly, ImageIcon icon ) {
		EntitiesDlg dlg = new EntitiesDlg( owner, title, ( ProfilePattern ) pp, readOnly, icon );
		initDlg( dlg );
		return ( ProfilePattern ) dlg.getResult();		
	}	

	public static ProfilePatternElement showProfilePatternElementDlg( Window owner, String title, ProfilePatternElement ppe, 
			boolean readOnly, ImageIcon icon ) {
		EntitiesDlg dlg = new EntitiesDlg( owner, title, ( ProfilePatternElement ) ppe, readOnly, icon );
		initDlg( dlg );
		return ( ProfilePatternElement ) dlg.getResult();		
	}	

	public static Measure showMeasureDlg( Window owner, String title, Measure measure, boolean readOnly, ImageIcon icon ) {
		EntitiesDlg dlg = new EntitiesDlg( owner, title, ( Measure ) measure, readOnly, icon );
		initDlg( dlg );
		return ( Measure ) dlg.getResult();		
	}	

	private static void initDlg( EntitiesDlg dlg ) {
		if ( dlg == null ) return;
		dlg.setDefaultCloseOperation( DISPOSE_ON_CLOSE );
		dlg.setModal(true);
		dlg.pack();
		dlg.setVisible(true);		
	}

 	private WebTextField initTextField( int columns, String promt, String txt ) {
		txtField = new WebTextField( columns );
		txtField.setInputPrompt( promt );
		txtField.setInputPromptFont( txtField.getFont().deriveFont( Font.ITALIC ) );
		txtField.setEditable( !readOnly );
		if ( txt != null ) txtField.setText( txt );
		return txtField;
 	}
 	
 	private WebTextArea initTextArea( String txt ) {
 		txtArea = new WebTextArea();
 		txtArea.setEditable( !readOnly );
 		txtArea.setLineWrap( true );
 		if ( txt != null ) txtArea.setText( txt );
 		return txtArea;
 	}	
 	
 	@SuppressWarnings("unchecked")
	private WebComboBox initLevelCombobox( Level level  ) {
		List<Level> levels = ResourceKeeper.getObject( OType.LEVELS_CACHE );
		cmb = new WebComboBox( levels.toArray( new Level[0] ) );
		cmb.setRenderer( this );
		if ( level != null ) cmb.setSelectedItem( level ); //SelectedIndex( level.getId() - 1 );
		return cmb;
 	}
	
	private Component makeTechInfo( Catalog catalog ) {
		WebTextArea txtArea = new WebTextArea ();
		txtArea.setEditable( false );
		txtArea.append( "Код: " + catalog.getId() );
		txtArea.append( "\nВерсия: " + catalog.getVersion() );
		txtArea.append( "\nСтатус: " + catalog.getStatus() );
		txtArea.append( "\nСоздан: " + catalog.getJournal().getCreateMoment() );
		if ( catalog.getVersion() > 1 ) txtArea.append( "\nРедактирован: " + catalog.getJournal().getEditMoment() );
		return txtArea;
	}

	private Component makeTechInfo( Indicator indicator ) {
		WebTextArea txtArea = new WebTextArea ();
		txtArea.setEditable( false );
		txtArea.append( "Код: " + indicator.getId() );
		txtArea.append( "\nВерсия: " + indicator.getVersion() );
		txtArea.append( "\nСтатус: " + indicator.getStatus() );
		txtArea.append( "\nСоздан: " + indicator.getJournal().getCreateMoment() );
		if ( indicator.getVersion() > 1 ) txtArea.append( "\nРедактирован: " + indicator.getJournal().getEditMoment() );
		return txtArea;
	}

	private Component makeTechInfo( Competence competence ) {
		WebTextArea txtArea = new WebTextArea ();
		txtArea.setEditable( false );
		txtArea.append( "Код: " + competence.getId() );
		txtArea.append( "\nВерсия: " + competence.getVersion() );
		txtArea.append( "\nСтатус: " + competence.getStatus() );
		txtArea.append( "\nСоздан: " + competence.getJournal().getCreateMoment() );
		if ( competence.getVersion() > 1 ) txtArea.append( "\nРедактирован: " + competence.getJournal().getEditMoment() );
		return txtArea;
	}

	private Component makeTechInfo( ProfilePattern pp ) {
		WebTextArea txtArea = new WebTextArea ();
		txtArea.setEditable( false );
		txtArea.append( "Код: " + pp.getId() );
		txtArea.append( "\nСтатус: " + pp.getStatus() );
		txtArea.append( "\nСоздан: " + pp.getJournal().getCreateMoment() );
		if ( pp.getJournal().getCreateMoment() != pp.getJournal().getEditMoment() ) txtArea.append( "\nРедактирован: " + pp.getJournal().getEditMoment() );
		return txtArea;
	}

	private Component makeTechInfo( ProfilePatternElement ppe ) {
		WebTextArea txtArea = new WebTextArea ();
		txtArea.setEditable( false );
		txtArea.append( "Код: " + ppe.getId() );
		txtArea.append( "\nСтатус: " + ppe.getStatus() );
		txtArea.append( "\nСоздан: " + ppe.getJournal().getCreateMoment() );
		if ( ppe.getJournal().getCreateMoment() != ppe.getJournal().getEditMoment() ) {
			txtArea.append( "\nРедактирован: " + ppe.getJournal().getEditMoment() );
		}
		return txtArea;
	}

	private Component makeTechInfo( Measure measure ) {
		WebTextArea txtArea = new WebTextArea ();
		txtArea.setEditable( false );
		txtArea.append( "Код: " + measure.getId() );
		txtArea.append( "\nСоздан: " + measure.getJournal().getCreateMoment() );
		if ( measure.getJournal().getCreateMoment() != measure.getJournal().getEditMoment() ) txtArea.append( "\nРедактирован: " + measure.getJournal().getEditMoment() );
		if ( measure.getJournal().getDeleteMoment().before( new Date() ) ) txtArea.append( "\nУдален: " + measure.getJournal().getDeleteMoment() );
		return txtArea;
	}

	/**
	 * make component for display catalog's history info
	 * @param catalog - Catalog entity
	 * @return WebList component have catalog's history entity or null otherwise 
	 */
	private Component makeHistoryInfo( Catalog catalog ) {
		if ( catalog == null ) return null;
		Catalog cat = ( Catalog ) OrmHelper.findEntity( Catalog.class, catalog.getId(), "ancestor", "historyList" );
		if ( cat.getAncestor() != null ) cat = ( Catalog ) OrmHelper.findEntity( Catalog.class, cat.getAncestor().getId(), "historyList" );				
		if ( ( cat.getHistoryList() == null ) || ( cat.getHistoryList().size() == 0 ) ) return null;				
		WebList list = new WebList( cat.getHistoryList() );
		list.setEditable( false );
		return list;	
	}

	/**
	 * make component for display indicator's history info
	 * @param indicator - Indicator entity
	 * @return WebList component have indicator's history entity or null otherwise 
	 */
	private Component makeHistoryInfo( Indicator indicator ) {
		if ( indicator == null ) return null;
		Indicator ind = ( Indicator ) OrmHelper.findEntity( Indicator.class, indicator.getId(), "ancestor", "historyList" );
		if ( ind.getAncestor() != null ) ind = ( Indicator ) OrmHelper.findEntity( Indicator.class, ind.getAncestor().getId(), "historyList" );				
		if ( ( ind.getHistoryList() == null ) || ( ind.getHistoryList().size() == 0 ) ) return null;				
		WebList list = new WebList( ind.getHistoryList() );
		list.setEditable( false );
		return list;			
	}

	/**
	 * make component for display competence's history info
	 * @param competence - Competence entity
	 * @return WebList component have competence's history entity or null otherwise 
	 */
	private Component makeHistoryInfo( Competence competence ) {
		if ( competence == null ) return null;
		Competence cmt = ( Competence ) OrmHelper.findEntity( Competence.class, competence.getId(), "ancestor", "historyList" );
		if ( cmt.getAncestor() != null ) cmt = ( Competence ) OrmHelper.findEntity( Competence.class, cmt.getAncestor().getId(), "historyList" );				
		if ( ( cmt.getHistoryList() == null ) || ( cmt.getHistoryList().size() == 0 ) ) return null;				
		WebList list = new WebList( cmt.getHistoryList() );
		list.setEditable( false );
		return list;			
	}

	private WebCollapsiblePane makeCollapsingPane( Icon icon, String title, Component content, Dimension preferedSize ) {
		WebScrollPane scrollPane = new WebScrollPane( content, false );		
		scrollPane.setPreferredSize( preferedSize );
		WebCollapsiblePane pane = new WebCollapsiblePane( icon, title, scrollPane );		
		pane.setExpanded( false ); 
		pane.addCollapsiblePaneListener( this );
		return pane;			
	}	

	public Object getResult() {
		return result;
	}

	/**
	 * save Catalog as new object always
	 */
	private void saveCatalog() {
		result = null;
		if ( readOnly || ( txtField.getText().length() <= 0 ) 
				|| ( ( source != null) && ( ( Catalog ) source).getName().equals( txtField.getText() ) ) ) return;
		Catalog cat = new Catalog( );
		cat.setAncestor( null );
		cat.setCompetences( null );
		result = cat;
	}

	/**
	 * save Indicator as new object always
	 */
	private void saveIndicator() {
		result = null;
		if ( readOnly || ( txtField.getText().length() <= 0 ) 
				|| ( ( source != null) 
						&& ( ( Indicator ) source).getName().equals( txtField.getText() ) 
						&& ( ( Indicator ) source).getLevel().equals( cmb.getSelectedItem() ) ) ) return;
		Indicator indicator = new Indicator();
		indicator.setName( txtField.getText() );
		indicator.setLevel( ( Level ) cmb.getSelectedItem() );
		result = indicator;
	}

	/**
	 * save Competence as new object always
	 */
	private void saveCompetence() {
		result = null;
		if ( readOnly || ( txtField.getText().length() <= 0 ) 
				|| ( ( source != null) 
						&& ( ( Competence ) source).getName().equals( txtField.getText() ) 
						&& ( ( ( Competence ) source).getDescription() != null ) 
						&& ( ( Competence ) source).getDescription().equals( txtArea.getText() ) ) ) return;
		Competence competence = new Competence();
		competence.setName( txtField.getText() );
		competence.setDescription( txtArea.getText() );
		result = competence;
	}
	
	/**
	 * save ProfilePattern as new object or replace existing source object
	 */
	private void savePP() {
		result = null;
		boolean change = false;
		if ( readOnly || ( ( source != null) && !( source instanceof ProfilePattern ) ) ) return;
		ProfilePattern pp = ( source != null ? ( ( ProfilePattern ) source ) : new ProfilePattern() );
		if ( ( ( pp.getName() != null ) && !pp.getName().equals( txtField.getText() ) )
				|| ( ( pp.getName() == null ) && ( txtField.getText() != null ) ) ) {
			change = true;
			pp.setName( txtField.getText() );
		}
		if ( ( ( pp.getDescription() != null ) && !pp.getDescription().equals( txtArea.getText() ) )
				|| ( ( pp.getDescription() == null ) && ( txtArea.getText() != null ) ) ) {
			change = true;
			pp.setDescription( txtArea.getText() );
		}

		ProfilePattern stub = ( change ? pp : new ProfilePattern() );
		saveFilialMask( stub );
		saveSummary( stub );
		if ( change ) {
			result = pp;
			return;
		}
		if ( ( ( pp.getFilialMask() != null ) && ( stub.getFilialMask() != null ) && !Arrays.equals( pp.getFilialMask(), stub.getFilialMask() ) )
				|| ( ( pp.getFilialMask() == null ) && ( stub.getFilialMask() != null ) ) 
				|| ( ( pp.getFilialMask() != null ) && ( stub.getFilialMask() == null ) )) {
			change = true;
			pp.setFilialMask( stub.getFilialMask() );
		}

		if ( ( ( pp.getSummary() != null ) && !pp.getSummary().equals( stub.getSummary() ) )
				|| ( ( pp.getSummary() == null ) && ( stub.getSummary() != null ) ) ) { 
			change = true;
			pp.setSummary( stub.getSummary() );
		}
		if ( change ) result = pp;
	}
	
	private void savePPE() {
		result = null;
		if (readOnly ) return;
		ProfilePatternElement ppe = ( ProfilePatternElement ) source;
		if ( ppe == null ) ppe = new ProfilePatternElement();
		ppe.setMinLevel( ( Level ) cmb.getSelectedItem() );
		result = ppe;
	}

	private void saveMeasure() {
		result = null;
		Measure m = ( Measure ) source;
		if ( readOnly || ( txtField.getText().length() <= 0 ) 
				|| ( ( m != null) 
						&& ( m.getName().equals( txtField.getText() ) ) 
						&& ( m.getDescription() != null ) 
						&& ( m.getDescription().equals( txtArea.getText() ) ) 
						&& ( m.getStart().equals( start.getDate() ) )
						&& ( m.getStop().equals( stop.getDate() ) ) ) ) return;
		Measure measure = new Measure();
		measure.setName( txtField.getText() );
		measure.setDescription( txtArea.getText() );
		measure.setStart( new Timestamp( start.getDate().getTime() ) );
		measure.setStop( new Timestamp( stop.getDate().getTime() ) );
		result = measure;		
	}
		
	@Override
	public void actionPerformed( ActionEvent e ) {
		switch ( e.getActionCommand() ) {
		case CATALOG_OK_CMD:
			saveCatalog();
			break;
		case COMPETENCE_OK_CMD:
			saveCompetence();
			break;
		case INDICATOR_OK_CMD:
			saveIndicator();
			break;
		case PP_OK_CMD:
			savePP();
			break;
		case PPE_OK_CMD:
			savePPE();
			break;
		case MEASURE_OK_CMD:
			saveMeasure();
			break;

		default:
			result = null;
			break;
		}
		setVisible(false);
	}

	@Override
	public void expanding( WebCollapsiblePane pane ) {
		double h = 0;
		if ( pane.getContent() != null ) h = pane.getContent().getPreferredSize().getHeight();
		System.out.println("h=" + h + "    type of WebScrollPane = " + pane.getContent().getClass() );
		Dimension d = this.getSize();
		d.setSize( d.getWidth(), d.getHeight() + h ) ;
		this.setSize(d);		
	}

	@Override
	public void expanded(WebCollapsiblePane pane) {
		// not used		
	}

	@Override
	public void collapsing(WebCollapsiblePane pane) {
		// not used		
	}

	@Override
	public void collapsed( WebCollapsiblePane pane ) {
		double h = 0;
		if ( pane.getContent() != null ) h = pane.getContent().getPreferredSize().getHeight();
		System.out.println("h=" + h + "    type of WebScrollPane = " + pane.getContent().getClass() );
		Dimension d = this.getSize();
		d.setSize(d.getWidth(), d.getHeight() - h ) ;
		this.setSize(d);		
	}		

	@Override
	public Component getListCellRendererComponent( JList<? extends Level> list, Level value, 
			int index, boolean isSelected, boolean cellHasFocus) {
		if ( renderLabel == null ) {
			renderLabel = new WebLabel();
			renderLabel.setOpaque( true );
		}
		if ( levelIcons == null ) {
			levelIcons  = new Icon[] { ResourceKeeper.getIcon( IType.LEVEL0, 24 ),
					ResourceKeeper.getIcon( IType.LEVEL1, 24 ), 
					ResourceKeeper.getIcon( IType.LEVEL2, 24 ),
					ResourceKeeper.getIcon( IType.LEVEL3, 24 ),
					ResourceKeeper.getIcon( IType.LEVEL4, 24 ),
					ResourceKeeper.getIcon( IType.LEVEL5, 24 ) };
		}
		renderLabel.setBackground( isSelected ? Color.LIGHT_GRAY : Color.WHITE );
		renderLabel.setText( value.getName() );
		renderLabel.setIcon( ( ( value.getId() < 1 ) || ( value.getId() > 5 ) ) ? levelIcons[0] : levelIcons[value.getId()] );
		return renderLabel;
	}
	
	@Override
	public Component getTreeCellRendererComponent( JTree tree, Object value, boolean selected, 
			boolean expanded, boolean leaf, int row, boolean hasFocus ) {
		if ( renderLabel == null ) {
			renderLabel = new WebLabel();
			renderLabel.setBorder( null );
			renderLabel.setOpaque( true );
		}		
		DefaultMutableTreeNode node = ( DefaultMutableTreeNode ) value;
		if ( ( node == null ) || ( node.getUserObject() == null ) ) {
			renderLabel.setText( "null" );
			return renderLabel;
		}

		renderLabel.setBackground( selected ? Color.LIGHT_GRAY : Color.WHITE );
		if ( node.getUserObject() instanceof Pair<?,?> ) {
			@SuppressWarnings("unchecked")
			Pair<String, Integer> p = ( Pair<String, Integer> ) node.getUserObject();
			renderLabel.setText( p.getFirst() );
			return renderLabel;
		}
		if ( node.getUserObject() instanceof BasisPost ) {
			BasisPost bp = ( BasisPost ) node.getUserObject();
			renderLabel.setText( bp.getName() );
			return renderLabel;
		}
		if ( node.getUserObject() instanceof FilialInfo ) {
			FilialInfo fi = ( FilialInfo ) node.getUserObject();
			renderLabel.setText( fi.getName() );
			return renderLabel;
		}
		renderLabel.setText( node.getUserObject().toString() );		
		return renderLabel;
	}

	/**
	 * Initialized base post nodes
	 * @return root base post node or null otherwise
	 */
	private DefaultMutableTreeNode initBasePosts() {
		List<Pair<String, Byte>> posts = ResourceKeeper.getObject( ResourceKeeper.OType.BASE_POST_LIST );
		if ( posts == null ) {
			if ( ( log != null ) && log.isWarnEnabled() ) log.warn( "EntitiesDlg.initBasePost() : resourese BASE_POST_LIST not found" );
			return null;
		}		
		
		List<BasisPost> lbp = OrmHelper.findByQueryWithParam( QueryType.JPQL, 
				( String ) ResourceKeeper.getQuery( QType.JPQL_LOAD_BASE_POST ), 
				BasisPost.class );
		if ( ( lbp == null ) || ( lbp.size() == 0 ) ) {
			if ( ( log != null ) && log.isWarnEnabled() ) log.warn( "EntitiesDlg.initBasePost() : not find base post" );
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
		List<Faset> lf = OrmHelper.findByQuery( QueryType.NAMED, "Faset.findAll", Faset.class );
		if ( ( lf == null ) || ( lf.size() == 0 ) ) {
			if ( ( log != null ) && log.isWarnEnabled() ) log.warn( "EntitiesDlg.initBasePost() : not find base post" );
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
		for( FilialInfo fi : lfi ) 
			if ( ( fi.getCode() > 0 ) && ( fi.getCode() < 100 ) ) rootFilial.add( new DefaultMutableTreeNode( fi ) );
		return rootFilial;
	}
	
	/**
	 * make filter tree component
	 * @param pp  
	 * @return WebCheckBoxTree object
	 */
	private Component makeFilterTree( ProfilePattern pp ) {
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
		if ( pp != null ) {
			viewFilialMask( pp );
			viewSummary( pp );
		}
		filterTree.expandAll();
		return filterTree;
	}	
	
	/**
	 * display filial's mask from ProfilePattern entity on tree
	 * @param pp - ProfilePattern entity
	 */
	private void viewFilialMask( ProfilePattern pp ) {
		if ( ( pp == null ) || ( pp.getFilialMask() == null ) 
				|| ( filterTree == null ) || ( filialRootNode == null ) ) return;
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
		if ( ( pp == null ) || ( pp.getSummary() == null ) 
				|| ( filterTree == null ) ) return;
		Gson gson = new Gson();
		ProfilePatternSummary pps = null;
		try {
			pps = gson.fromJson( pp.getSummary(), 
					new TypeToken<ProfilePatternSummary>(){}.getType() );
		} catch ( Exception ex ) {
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( "EntitiesDlg.viewSummary() : ", ex );
			return;
		}
		viewFaset( pps );
		viewBasisPost ( pps );
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
		
		for ( int i = 0; i < filialRootNode.getChildCount(); i++ ) {
			DefaultMutableTreeNode child = ( DefaultMutableTreeNode ) filialRootNode.getChildAt( i );
			if ( ( filterTree.getCheckState( child ) != CheckState.checked ) 
					|| ( child == null ) || ( child.getUserObject() == null ) 
					|| !( child.getUserObject() instanceof FilialInfo ) ) continue;			
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
	 * save basis post and faset data in ProfilePattern as gson string of ProfilePatternSummary objec
	 * @param pp - existing ProfilePattern object
	 * @return ProfilePattern object if success or null otherwise
	 */
	private ProfilePattern saveSummary( ProfilePattern pp ) {
		if ( ( pp == null ) || ( filterTree == null ) 
				|| ( ( basePostRootNode == null ) && ( fasetRootNode == null ) ) ) return null;
		
		ProfilePatternSummary pps = new ProfilePatternSummary(new ArrayList<Integer>(), 
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
			if ( ( filterTree.getCheckState( child ) == CheckState.unchecked )
					|| ( child == null ) || ( child.getUserObject() == null )
					|| !( child.getUserObject() instanceof Pair<?, ?> ) )continue;
			@SuppressWarnings("unchecked")
			Pair<String, Integer> p = ( Pair<String, Integer> ) child.getUserObject();
			List<Integer> li = new ArrayList<Integer>();
			pps.getFasets().put( p.getSecond(), li );
			
			for ( int j = 0; j < child.getChildCount(); j++ ) {
				DefaultMutableTreeNode grandson = ( DefaultMutableTreeNode ) child.getChildAt( j );
				if ( ( filterTree.getCheckState( grandson ) != CheckState.checked ) 
						|| ( grandson == null ) || ( grandson.getUserObject() == null )
						|| !( grandson.getUserObject() instanceof Pair<?, ?> ) ) continue;
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
		if ( ( pps == null ) || ( pps.getBasisPosts() == null ) )  return;
		for ( int i = 0; i < basePostRootNode.getChildCount(); i++ ) {
			DefaultMutableTreeNode child = ( DefaultMutableTreeNode ) basePostRootNode.getChildAt( i );
			if ( filterTree.getCheckState( child ) == CheckState.unchecked ) continue;
			for ( int j = 0; j < child.getChildCount(); j++ ) {
				DefaultMutableTreeNode grandson = ( DefaultMutableTreeNode ) child.getChildAt( j );
				if ( ( filterTree.getCheckState( grandson ) != CheckState.checked )
						|| ( grandson == null ) || ( grandson.getUserObject() == null )
						|| !( grandson.getUserObject() instanceof BasisPost ) ) continue;
				pps.getBasisPosts().add( ( ( BasisPost ) grandson.getUserObject() ).getId() );
			}
		}
	}
}