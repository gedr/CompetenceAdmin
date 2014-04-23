package minos.ui.panels;

import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.activation.DataHandler;
import javax.swing.JComponent;
import javax.swing.TransferHandler;
import javax.swing.TransferHandler.TransferSupport;

import minos.data.services.CatalogJPAController;
import minos.data.services.ORMHelper;
import minos.data.services.ProfilePatternJPAController;
import minos.entities.Catalog;
import minos.entities.ProfilePattern;
import minos.resource.managers.IconResource;
import minos.resource.managers.IconResource.IconType;
import minos.ui.adapters.ActionAdapter;
import minos.ui.dialogs.CatalogDlg;
import minos.ui.dialogs.ProfilePatternDlg;
import minos.ui.models.CatalogDataProvider;
import minos.ui.models.CatalogDataProvider.CatalogType;
import minos.ui.models.CompetenceDataProvider;
import minos.ui.models.MainTreeCellRenderer;
import minos.ui.models.ProfilePatternDataProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alee.extended.tree.AsyncUniqueNode;
import com.alee.extended.tree.WebAsyncTree;
import com.alee.laf.optionpane.WebOptionPane;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.scroll.WebScrollPane;
import com.alee.laf.toolbar.WebToolBar;

public class ProfilePatternPanel extends WebPanel implements ActionListener{
	private static final long serialVersionUID 			= 1L;
	private static final String PP_ADD_CMD				= "1";
	private static final String EDIT_CMD				= "2";
	private static final String DELETE_CMD				= "3";
	private static final String CATALOG_ADD_CMD 		= "4";
	private static final String PP_ACTIVATE_CMD 		= "5";
	private static final String PPE_CHANGE_LEVEL_CMD	= "6";
	
	private static Logger log = LoggerFactory.getLogger( ProfilePatternPanel.class );
	private Window owner;
	private WebAsyncTree<AsyncUniqueNode> tree;

	public ProfilePatternPanel(Window owner) {
		super();
		this.owner = owner;
		init();
	}

	private void init() {
		CatalogDataProvider cdp = new CatalogDataProvider();
		cdp.setCatalogType( CatalogType.PROFILE_PATTERN_CATALOG );
		ProfilePatternDataProvider ppdp = new ProfilePatternDataProvider( cdp );
		CompetenceDataProvider cmdp = new CompetenceDataProvider( ppdp );
		tree = new WebAsyncTree<>( cmdp );
		tree.setCellRenderer( new MainTreeCellRenderer( 24 ) );
		tree.setRootVisible( false );
		tree.setTransferHandler( new TransferHandler() {			
			private static final long serialVersionUID = 1L;			
			private DataFlavor df;
			
			private DataFlavor getDataFlavor() {
				if ( df != null ) return df;
				try {
					df = new DataFlavor( DataFlavor.javaJVMLocalObjectMimeType + ";class=java.lang.Integer" );					
				} catch (Exception e) {
					// TODO: handle exception
					df = null;
				}
				return df;
			}

			
			@Override
			public boolean canImport( TransferSupport support ) {
				try {					
					log.debug( "ProfilePatternPanel.canImport  " + getDataFlavor() );
					return support.isDataFlavorSupported( getDataFlavor() );					
				} catch ( Exception e ) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}						
				return false;
			}

			@Override
			protected Transferable createTransferable( JComponent c ) {
				log.debug( "ProfilePatternPanel.createTransferable  " );
				return new DataHandler(Integer.valueOf( 1 ), DataFlavor.javaJVMLocalObjectMimeType );
			}

			@Override
			public int getSourceActions( JComponent c ) { 
				log.debug( "ProfilePatternPanel   COPY_OR_MOVE " );
				return TransferHandler.COPY_OR_MOVE;
			}

			@Override
			public boolean importData( TransferSupport support ) {
				//((DataHandler)support.getTransferable().get).
				Transferable t = support.getTransferable();
				try {
					log.debug( "ProfilePatternPanel.importData " + t.getTransferData( getDataFlavor() ) );
				} catch (UnsupportedFlavorException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				return super.importData(support);
			}
			
		});
        
		WebToolBar tb = new WebToolBar();		
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

		setLayout( new BorderLayout() );
		add( tb, BorderLayout.NORTH );
		add( new WebScrollPane( tree ), BorderLayout.CENTER );
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		switch ( e.getActionCommand() ) {
		case CATALOG_ADD_CMD:
			if ( addCatalog() ) tree.reloadRootNode();
			break;

		case PP_ADD_CMD:
			addProfilePattern();
			break;
		
		case EDIT_CMD:
			break;
			
		case DELETE_CMD:
			break;

		case PP_ACTIVATE_CMD:
			break;
			
		case PPE_CHANGE_LEVEL_CMD:
			break;

		default:
			break;
		}
	}
	
	private boolean checkRightSelect( boolean emptySelection, Class<?>... clss) {
		if ( emptySelection && (tree.getSelectionCount() == 0) ) return true;
 		if ( ( tree.getSelectionCount() > 1 ) || 
 				( !emptySelection && ( tree.getSelectionCount() == 0 ) ) ) {
			WebOptionPane.showMessageDialog( owner, 
					((tree.getSelectionCount() == 0) ? "Не выбран элемент" : "Выбрано много элементов"), 
					"Ошибка", WebOptionPane.ERROR_MESSAGE );
			return false;
		}		

 		if ( ( clss == null ) || ( clss.length == 0 ) ) return true;
 		for ( int i = 0; i < clss.length; i++ ) { 			
 			if ( tree.getSelectedNode().getUserObject().getClass().equals( clss[i] ) ) return true;
 		}
		
		WebOptionPane.showMessageDialog( owner, "Выбран некорректный элемент.",
					"Ошибка", WebOptionPane.ERROR_MESSAGE );
		return false;		
	}

	
	
	private boolean addProfilePattern() {
		if ( !checkRightSelect( true, Catalog.class ) ) return false;
		ProfilePattern pp = ProfilePatternDlg.showProfilePatternDlg(owner, "Новый шаблон профиля", null, false, 
				IconResource.getInstance().getIcon( IconType.PROFILE_PATTERN_ADD, 64 ) );
		if ( pp == null ) return false;

		int id = ( tree.getSelectionCount() == 0 ? 1 : ( ( Catalog ) tree.getSelectedNode().getUserObject() ).getId() );
		Catalog parentCatalog = (Catalog) ORMHelper.findEntity( Catalog.class, id, "ancestorCatalog" );
		if ( parentCatalog == null ) {
			if ( log.isErrorEnabled() ) log.error( "ProfilePatternPanel.addCatalog(): cannot find parent catalog in db id=" + id );
			return false;
		}
		if ( parentCatalog.getAncestorCatalog() != null ) {
			WebOptionPane.showMessageDialog( this, "Отключите режим просмотра истории", "Ошибка", WebOptionPane.ERROR_MESSAGE );
			return false;
		}
		pp.setCatalog( parentCatalog );
		ProfilePatternJPAController.create( pp, true  );		
		return true;
	}
	
	private boolean addCatalog() {
		if ( !checkRightSelect( true, Catalog.class ) ) return false;

		Catalog newCatalog = CatalogDlg.showCatalogDlg(owner, "Новый каталог", null, false, 
				IconResource.getInstance().getIcon( IconType.CATALOG_ADD, 64 ));
		if ( newCatalog == null ) return false;
		
		int id = ( tree.getSelectionCount() == 0 ? 1 : ( ( Catalog ) tree.getSelectedNode().getUserObject() ).getId() );
		Catalog parentCatalog = (Catalog) ORMHelper.findEntity( Catalog.class, id, "ancestorCatalog" );
		if ( parentCatalog == null ) {
			if ( log.isErrorEnabled() ) log.error( "ProfilePatternPanel.addCatalog(): cannot find parent catalog in db id=" + id );
			return false;
		}
		if ( parentCatalog.getAncestorCatalog() != null ) {
			WebOptionPane.showMessageDialog( this, "Отключите режим просмотра истории", "Ошибка", WebOptionPane.ERROR_MESSAGE );
			return false;
		}
		newCatalog.setParentCatalog( parentCatalog );
		CatalogJPAController.create( newCatalog, true );
		return true;
	}
}