package minos.ui.panels;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;

import minos.resource.managers.IconResource;
import minos.resource.managers.IconResource.IconType;
import minos.resource.managers.Resources;
import minos.resource.managers.ResourcesConst;
import minos.ui.adapters.ActionAdapter;
import minos.ui.models.DivisionDataProvider;
import minos.ui.models.EstablishedPostDataProvider;
import net.miginfocom.swing.MigLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alee.extended.button.WebSwitch;
import com.alee.extended.tree.AsyncUniqueNode;
import com.alee.extended.tree.WebAsyncTree;
import com.alee.extended.window.WebPopOver;
import com.alee.laf.button.WebButton;
import com.alee.laf.label.WebLabel;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.scroll.WebScrollPane;
import com.alee.laf.separator.WebSeparator;
import com.alee.laf.toolbar.WebToolBar;

public class PostProfilePanel extends WebPanel implements ActionListener{
	private static final long serialVersionUID 			= 1L;

	private static final String CONFIG_CMD = "a";

	private static Logger log = LoggerFactory.getLogger( PostProfilePanel.class );
	private Window owner;
	private WebAsyncTree<AsyncUniqueNode> tree;
	DivisionDataProvider ddp = null;
	EstablishedPostDataProvider epdp = null;

	public PostProfilePanel(Window owner) {
		super();
		this.owner = owner;
		try {
			init();
		} catch (Exception e) {
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( "PostProfilePanel.PostProfilePanel() : ", e );
		}
	}

	private void init() throws Exception {
		ddp = new DivisionDataProvider( ( int ) Resources.getInstance().get( ResourcesConst.STRUCTURE_ROOT ) );
		epdp = new EstablishedPostDataProvider( ddp );
		//cdp.setCatalogType( CatalogType.PROFILE_PATTERN_CATALOG );
		//ProfilePatternDataProvider ppdp = new ProfilePatternDataProvider( cdp );
		//CompetenceDataProvider cmdp = new CompetenceDataProvider( ppdp );
		tree = new WebAsyncTree<>( epdp );
		//tree.setCellRenderer( new MainTreeCellRenderer( 24 ) );
			       
        
		WebToolBar tb = new WebToolBar();		
	/*
		tb.add( new ActionAdapter("add catalog", IconResource.getInstance().getIcon(IconType.CATALOG_ADD, 32), 
				CATALOG_ADD_CMD, "Добавление нового подкаталога", this, 0) );
		tb.add( new ActionAdapter("add", IconResource.getInstance().getIcon( IconType.PROFILE_PATTERN_ADD, 32 ), 
				PP_ADD_CMD, "Добавление нового шаблона профиля", this, 0) );
		tb.add( new ActionAdapter("add", IconResource.getInstance().getIcon( IconType.PROFILE_PATTERN_ADD, 32 ), 
				PP_ACTIVATE_CMD, "Активировать шаблон профиля", this, 0) );
		tb.add( new ActionAdapter("add", IconResource.getInstance().getIcon( IconType.LEVEL3, 32 ), 
				PPE_CHANGE_LEVEL_CMD, "Активировать шаблон профиля", this, 0) );		
		tb.add( new ActionAdapter("edit", IconResource.getInstance().getIcon(IconType.EDIT, 32), 
				EDIT_CMD, "Редактирование элемента", this, 0) );
		tb.add( new ActionAdapter("edit", IconResource.getInstance().getIcon(IconType.DELETE, 32), 
				DELETE_CMD, "Удаление элемента", this, 0) );
*/
		tb.add( new ActionAdapter("config", null, CONFIG_CMD, "Кофигурация", this, 0) );

		setLayout( new BorderLayout() );
		add( tb, BorderLayout.NORTH );
		add( new WebScrollPane( tree ), BorderLayout.CENTER );
	}

	public void actionPerformed(ActionEvent e) {
		switch ( e.getActionCommand() ) {
		case CONFIG_CMD:
			showConfig( e );
			break;

		default:
			break;
		}
	}

	private void showConfig( ActionEvent e) {
		if ( ddp == null ) return;
		final WebPopOver popOver = new WebPopOver ( owner );

		final WebSwitch switchDeletedDivision = makeSwitchBtn( ddp.isVisibleDeletedDivisions() );
		final WebSwitch switchOverdueDivision = makeSwitchBtn( ddp.isVisibleOverdueDivisions() );
		final WebSwitch switchDisapproveDivision = makeSwitchBtn( ddp.isVisibleDisapproveDivisions() );

		final WebSwitch switchDeletedPost = makeSwitchBtn( epdp.isVisibleDeletedEstablishedPosts() );
		final WebSwitch switchOverduePost = makeSwitchBtn( epdp.isVisibleOverdueEstablishedPosts() );
		final WebSwitch switchDisapprovePost = makeSwitchBtn( epdp.isVisibleDisapproveEstablishedPosts() );
		final WebSwitch switchGroupPost = makeSwitchBtn( epdp.isVisibleGroupEstablishedPost() );
/*

		final WebCheckBox switchDeletedDivision = new WebCheckBox( ddp.isVisibleDeletedDivisions() );
		final WebCheckBox switchOverdueDivision = new WebCheckBox( ddp.isVisibleOverdueDivisions() );
		final WebCheckBox switchDisapproveDivision = new WebCheckBox( ddp.isVisibleDisapproveDivisions() );

		final WebCheckBox switchDeletedPost = new WebCheckBox( epdp.isVisibleDeletedEstablishedPosts() );
		final WebCheckBox switchOverduePost = new WebCheckBox( epdp.isVisibleOverdueEstablishedPosts() );
		final WebCheckBox switchDisapprovePost = new WebCheckBox( epdp.isVisibleDisapproveEstablishedPosts() );
		final WebCheckBox switchGroupPost = new WebCheckBox( epdp.isVisibleGroupEstablishedPost() );
*/
		
		popOver.setCloseOnFocusLoss ( true );
	    // popOver.setModal ( true );
	    popOver.setMargin ( 10 );
	    popOver.setMovable ( false );
	    popOver.setLayout( new MigLayout( "", "[][]", "[][][][]" ));

	    WebLabel l1 = new WebLabel( "Отображение отделов" );
	    l1.setFont( l1.getFont().deriveFont( Font.BOLD ) );
	    WebLabel l2 = new WebLabel( "Отображение штатных должностей" );
	    l2.setFont( l1.getFont() );
	    
	    popOver.add( l1, "split 2, span" );
	    popOver.add( new WebSeparator(), "growx, wrap" );
	    popOver.add( new WebLabel( "Показывать просроченые отделы" ));
	    popOver.add( switchOverdueDivision, "wrap" );
	    popOver.add( new WebLabel( "Показывать удаленые отделы" ) );
	    popOver.add( switchDeletedDivision, "wrap" );
	    popOver.add( new WebLabel( "Показывать отделы, не согласовнае ОТИЗом" ) );
	    popOver.add( switchDisapproveDivision, "wrap" );
	    popOver.add( l2, "split 2, span" );
	    popOver.add( new WebSeparator(), "growx, wrap" );
	    popOver.add( new WebLabel( "Показывать просроченые должности" ));
	    popOver.add( switchOverduePost, "wrap" );
	    popOver.add( new WebLabel( "Показывать удаленые должности" ) );
	    popOver.add( switchDeletedPost, "wrap" );
	    popOver.add( new WebLabel( "Показывать должности, не согласовнае ОТИЗом" ) );
	    popOver.add( switchDisapprovePost, "wrap" );
	    popOver.add( new WebLabel( "Группировка должностей" ) );
	    popOver.add( switchGroupPost, "wrap" );

	    popOver.add( new WebSeparator(), "split 2, span, growx, wrap" );	    
	    popOver.add( new WebButton( "Применить", new ActionListener ()
	    {
	        @Override
	        public void actionPerformed ( final ActionEvent e )
	        {
	        	boolean fUpdate = false;
	        	if ( switchDeletedDivision.isSelected() != ddp.isVisibleDeletedDivisions() ) {
	        		ddp.setVisibleDeletedDivisions( switchDeletedDivision.isSelected() );
	        		fUpdate = true;
	        	}
	        	if ( switchOverdueDivision.isSelected() != ddp.isVisibleOverdueDivisions() ) {
	        		ddp.setVisibleOverdueDivisions( switchOverdueDivision.isSelected() );
	        		fUpdate = true;
	        	}
	        	if ( switchDisapproveDivision.isSelected() != ddp.isVisibleDisapproveDivisions() ) {
	        		ddp.setVisibleDisapproveDivisions( switchDisapproveDivision.isSelected() );
	        		fUpdate = true;
	        	}
	        	if ( switchDeletedPost.isSelected() != epdp.isVisibleDeletedEstablishedPosts() ) {
	        		epdp.setVisibleDeletedEstablishedPosts( switchDeletedPost.isSelected() );
	        		fUpdate = true;
	        	}	        	
	        	if ( switchDeletedPost.isSelected() != epdp.isVisibleDeletedEstablishedPosts() ) {
	        		epdp.setVisibleDeletedEstablishedPosts( switchDeletedPost.isSelected() );
	        		fUpdate = true;
	        	}	        	
	        	if ( switchOverduePost.isSelected() != epdp.isVisibleOverdueEstablishedPosts() ) {
	        		epdp.setVisibleOverdueEstablishedPosts( switchOverduePost.isSelected() );
	        		fUpdate = true;
	        	}	        	
	        	if ( switchDisapprovePost.isSelected() != epdp.isVisibleDisapproveEstablishedPosts() ) {
	        		epdp.setVisibleDisapproveEstablishedPosts( switchDisapprovePost.isSelected() );
	        		fUpdate = true;
	        	}	        	
	        	if ( switchGroupPost.isSelected() != epdp.isVisibleGroupEstablishedPost() ) {
	        		epdp.setVisibleGroupEstablishedPost( switchGroupPost.isSelected() );
	        		fUpdate = true;
	        	}      	
	        	
	        	popOver.setVisible( false );
	        	popOver.dispose();
	        	if ( fUpdate ) {
	        		
	        		tree.reloadRootNode();
	        	}
	        }
	    } ), "growx, span" );
	    popOver.show( ( JButton )  e.getSource() );	    
	}
	
	private WebSwitch makeSwitchBtn(boolean selected) {		
        WebSwitch ws = new WebSwitch();
        ws.setRound ( 10 );
        WebLabel lbl = new WebLabel( IconResource.getInstance().getIcon( IconType.OK, 16 ), WebLabel.CENTER );
        lbl.setMargin(2, 2, 2, 2);
        ws.setLeftComponent ( lbl );
        lbl = new WebLabel( IconResource.getInstance().getIcon( IconType.NO, 16 ), WebLabel.CENTER );
        lbl.setMargin(2, 2, 2, 2);
        ws.setRightComponent ( lbl );
        ws.setSelected( selected );
        return ws;
	}
	
	
	

}