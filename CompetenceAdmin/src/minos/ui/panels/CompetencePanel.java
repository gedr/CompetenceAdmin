package minos.ui.panels;

import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Timestamp;

import javax.activation.DataHandler;
import javax.swing.JComponent;
import javax.swing.TransferHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import minos.data.services.CatalogJPAController;
import minos.data.services.CompetenceJPAController;
import minos.data.services.IndicatorJPAController;
import minos.data.services.ORMHelper;
import minos.entities.Catalog;
import minos.entities.Competence;
import minos.entities.Indicator;
import minos.resource.managers.IconResource;
import minos.resource.managers.IconResource.IconType;
import minos.ui.adapters.ActionAdapter;
import minos.ui.dialogs.CatalogDlg;
import minos.ui.dialogs.CompetenceDlg;
import minos.ui.dialogs.IndicatorDlg;
import minos.ui.dialogs.TimePointDlg;
import minos.ui.models.BasisDataProvider;
import minos.ui.models.CatalogDataProvider;
import minos.ui.models.CompetenceDataProvider;
import minos.ui.models.MainTreeCellRenderer;
import minos.ui.models.MainTreeNode;

import com.alee.extended.tree.WebAsyncTree;
import com.alee.laf.button.WebToggleButton;
import com.alee.laf.optionpane.WebOptionPane;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.scroll.WebScrollPane;
import com.alee.laf.toolbar.WebToolBar;

public class CompetencePanel extends WebPanel implements ActionListener{
	private static final long serialVersionUID = 1L;
	private static final String CATALOG_ADD_CMD 	= "a";
	private static final String COMPETENCE_ADD_CMD 	= "b";
	private static final String INDICATOR_ADD_CMD 	= "c";
	private static final String REFRESH_CMD 		= "d";
	private static final String EDIT_CMD 			= "e";
	private static final String CLOCK_CMD 			= "f";
	private static final String DELETE_CMD 			= "g";
	private static Logger log = LoggerFactory.getLogger( CompetencePanel.class );	
	
	private WebAsyncTree<MainTreeNode> tree = null;
	private CompetenceDataProvider cdp = null;
	private WebToggleButton clockBtn = null;
	private Window owner = null;

	public CompetencePanel(Window owner) {
		this.owner = owner;
		init();
	}
	
	private void init() {
		setLayout( new BorderLayout() );
		clockBtn = new WebToggleButton( null, IconResource.getInstance().getIcon( IconType.CLOCK, 32 ) );
		clockBtn.setShadeToggleIcon ( true );
		clockBtn.setActionCommand( CLOCK_CMD );
		clockBtn.addActionListener( this );
        
		WebToolBar tb = new WebToolBar();		
		tb.add( new ActionAdapter("add catalog", IconResource.getInstance().getIcon(IconType.CATALOG_ADD, 32), 
				CATALOG_ADD_CMD, "Добавление нового подкаталога в каталог", this, 0) ); 
		tb.add( new ActionAdapter("add competence", IconResource.getInstance().getIcon( IconType.COMPETENCE_ADD, 32 ), COMPETENCE_ADD_CMD, 
				"Добавление новой компетенции в каталог", this, 0) ); 
		tb.add( new ActionAdapter("add indicator", IconResource.getInstance().getIcon( IconType.INDICATOR_ADD, 32 ), INDICATOR_ADD_CMD, 
				"Добавление нового индикатора в компетенцию", this, 0) ); 
		tb.add( new ActionAdapter("edit", IconResource.getInstance().getIcon( IconType.EDIT, 32 ), EDIT_CMD, 
				"Редактирование элемента", this, 0) ); 
		tb.add( new ActionAdapter("delete", IconResource.getInstance().getIcon( IconType.DELETE, 32 ), DELETE_CMD, 
				"Удаление элемента", this, 0) );
		tb.add( new ActionAdapter("refresh", IconResource.getInstance().getIcon( IconType.RELOAD, 32 ), REFRESH_CMD, 
				"Обновить данные", this, 0) );
		tb.add( clockBtn );

		add( tb, BorderLayout.NORTH );
		
		cdp = new CompetenceDataProvider( new CatalogDataProvider() );
		tree = new WebAsyncTree<>( cdp );
		tree.setRootVisible(false);
		tree.setCellRenderer( new MainTreeCellRenderer( 24 ) );
		tree.setDragEnabled( true );
		
		tree.setTransferHandler( new TransferHandler() {			
			private static final long serialVersionUID = 1L;
			private DataFlavor df;
			
			private DataFlavor getDataFlavor() {
				if ( df != null ) return df;
				try {
					df = new DataFlavor( DataFlavor.javaJVMLocalObjectMimeType + ";class=java.lang.Integer" );					
				} catch (Exception e) {
					if ( ( log != null ) && log.isErrorEnabled() ) log.error(" CompetencePanel.getDataFlavor() ", e );
					df = null;
				}
				return df;
			}
			
			@Override
			public boolean canImport( TransferSupport support ) {
				try {
					if ( ( support == null ) ||  
							( !( support.getComponent() instanceof WebAsyncTree ) ) ||
							( ( ( WebAsyncTree<?> ) support.getComponent() ).
									getNodeForLocation( support.getDropLocation().getDropPoint() ) == null ) ){
						return false;
					}
					return support.isDataFlavorSupported( getDataFlavor() );					
				} catch (Exception e) {
					if ( ( log != null ) && log.isErrorEnabled() ) log.error(" CompetencePanel.canImport() ", e );
				}						
				return false;
			}

			@Override
			protected Transferable createTransferable( JComponent c ) {
				log.debug( "CompetencePanel.createTransferable  " + (c instanceof WebAsyncTree) );
				return new DataHandler(Integer.valueOf( 1234567 ), DataFlavor.javaJVMLocalObjectMimeType );
			}

			@Override
			public int getSourceActions( JComponent c ) {
				log.debug( "CompetencePanel   COPY_OR_MOVE " );
				return TransferHandler.COPY_OR_MOVE;
			}

			@Override
			public boolean importData( TransferSupport support ) {
				try {
					Transferable t = support.getTransferable();
					log.debug( "CompetencePanel.importData " + t.getTransferData( getDataFlavor() ) );
				} catch (Exception e) {
					if ( ( log != null ) && log.isErrorEnabled() ) log.error(" CompetencePanel.importData() ", e );
				} 
				
				return super.importData(support);
			}
			
		});
		add( new WebScrollPane( tree ), BorderLayout.CENTER );
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		switch ( e.getActionCommand() ) {
 		case CATALOG_ADD_CMD:	
 			if ( addCatalog() ) tree.reloadNode( tree.getSelectedNode() );						
			break;

		case COMPETENCE_ADD_CMD:
			if ( addCompetence() ) tree.reloadNode( tree.getSelectedNode() );
			break;

		case INDICATOR_ADD_CMD:
			if ( addIndicator() ) tree.reloadNode( tree.getSelectedNode() );			
			break;

		case REFRESH_CMD:
			tree.reloadRootNode();
			break;
		
		case EDIT_CMD:
			editDispatch();			
			break;

		case CLOCK_CMD:
			viewHistory();
			break;

		default:
			break;
		}		
	}
	
	private void viewHistory() {
		if ( clockBtn.isSelected() ) {
			Timestamp tp = TimePointDlg.showTimePointDlgDlg( owner, "Время в прошлом" );
			if (tp == null ) {
				clockBtn.setSelected( false );
				return;
			}
			cdp.setCurrentTimePoint( tp );
			tree.reloadRootNode();
			return;
		}
		cdp.setCurrentTimePoint( null );
		tree.reloadRootNode();
	}
	
	private boolean checkHistoryView() {
		if ( cdp.getCurrentTimePoint() != BasisDataProvider.FUTURE_TIMEPOINT ) {
			WebOptionPane.showMessageDialog( owner, "При просмотре истории нельзя вносить изменения",
					"Ошибка", WebOptionPane.ERROR_MESSAGE );
			return true;
		}
		return false;		
	}

	private boolean checkRightSelect( Class<?>... clss) {
 		if ( ( tree.getSelectionCount() != 1 ) ) {
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

	private boolean addCatalog() {
		if ( checkHistoryView() || !checkRightSelect( Catalog.class ) ) return false;

		Catalog newCatalog = CatalogDlg.showCatalogDlg(owner, "Новый каталог", null, false, 
				IconResource.getInstance().getIcon( IconType.CATALOG_ADD, 64 ));
		if ( newCatalog == null ) return false;

		int id = ( ( Catalog ) tree.getSelectedNode().getUserObject() ).getId();
		Catalog parentCatalog = (Catalog) ORMHelper.findEntity( Catalog.class, id, "ancestorCatalog" );
		if ( parentCatalog == null ) {
			if ( log.isErrorEnabled() ) log.error( "CompetencePanel.addCatalog(): cannot find parent catalog in db id=" + id );
			return false;
		}
		if ( parentCatalog.getAncestor() != null ) {
			WebOptionPane.showMessageDialog( this, "Отключите режим просмотра истории", "Ошибка", WebOptionPane.ERROR_MESSAGE );
			return false;
		}
		newCatalog.setParentCatalog( parentCatalog );
		CatalogJPAController.create( newCatalog, true );
		return true;
	}	

	private boolean addCompetence() {
		if ( checkHistoryView() || !checkRightSelect( Catalog.class ) ) return false;
		
		Competence newCompetence = CompetenceDlg.showCompetenceDlg( owner,  "Новая компетенция", null, false );
		if ( newCompetence == null ) return false;
		
		MainTreeNode te = tree.getSelectedNode();
		int id = ( ( Catalog ) te.getUserObject() ).getId();
		Catalog catalog = (Catalog) ORMHelper.findEntity( Catalog.class, id, "ancestorCompetence" );
		if ( catalog == null ) {
			if ( log.isErrorEnabled() ) log.error( "CompetencePanel.addCompetence(): cannot find catalog in db id=" + id );
			return false;
		}
		if ( catalog.getAncestor() != null ) {
			WebOptionPane.showMessageDialog( this, "Отключите режим просмотра истории", "Ошибка", WebOptionPane.ERROR_MESSAGE );
			return false;
		}		
		newCompetence.setCatalog( catalog );
		CompetenceJPAController.create( newCompetence, true );
		return true;
	}

	private boolean addIndicator() {
		if ( checkHistoryView() || !checkRightSelect( Competence.class ) ) return false;
		
		Indicator newIndicator = IndicatorDlg.showIndicatorDlg( owner,  "Новый индикатор", null, false );
		if ( newIndicator == null ) return false;
		
		MainTreeNode te = tree.getSelectedNode();
		int id = ( ( Competence ) te.getUserObject() ).getId();
		Competence competence = (Competence) ORMHelper.findEntity( Competence.class, id, "ancestorCompetence" );
		if( competence == null ) {
			if ( log.isErrorEnabled() ) log.error( "CompetencePanel.addIndicator(): cannot find competence in db id=" + id );
			return false;
		}
		if ( competence.getAncestor() != null ) {
			WebOptionPane.showMessageDialog( this, "Отключите режим просмотра истории", "Ошибка", WebOptionPane.ERROR_MESSAGE );
			return false;
		}
		newIndicator.setCompetence( competence );
		IndicatorJPAController.create( newIndicator, true );
		return true;
	}

	private void editDispatch() {
		if ( checkHistoryView() || !checkRightSelect( Catalog.class, Competence.class, Indicator.class ) ) return;
		if ( tree.getSelectedNode().getUserObject().getClass().equals( Catalog.class ) ) editCatalog();
		if ( tree.getSelectedNode().getUserObject().getClass().equals( Competence.class ) ) editCompetence();
		if ( tree.getSelectedNode().getUserObject().getClass().equals( Indicator.class ) ) editIndicator();
	}

	private void editCatalog() {
		if ( checkHistoryView() || !checkRightSelect( Catalog.class ) ) return;

		int id = ( ( Catalog ) tree.getSelectedNode().getUserObject() ).getId();
		Catalog catalog = (Catalog) ORMHelper.findEntity( Catalog.class, id, "parentCatalog", "journal", "ancestorCatalog" );
		if ( catalog == null ) {
			if ( log.isErrorEnabled() ) log.error("CompetencePanel.editCatalog() : current catalog with id=" + id + "  cannot find");
			return;
		}		
		if ( ( catalog.getId() < 5 ) || ( catalog.getAncestor() != null ) ) { 
			CatalogDlg.showCatalogDlg(owner, "Просмотр каталога", catalog, true, 
					IconResource.getInstance().getIcon( IconType.CATALOG_EDIT, 64 ) );
			return;
		}
		Catalog newCatalog = CatalogDlg.showCatalogDlg(owner, "Редактирование каталога", catalog, false,
				IconResource.getInstance().getIcon( IconType.CATALOG_EDIT, 64 ) );
		if ( newCatalog == null ) return;
		newCatalog.setAncestor( catalog );
		CatalogJPAController.edit( newCatalog, true );
		tree.getSelectedNode().setUserObject( ORMHelper.findEntity( Catalog.class, id ) );
		tree.updateUI();
	}
	
	private void editCompetence() {
		if ( checkHistoryView() || !checkRightSelect( Competence.class ) ) return;

		MainTreeNode te = tree.getSelectedNode();
		int id = ( ( Competence ) te.getUserObject() ).getId();
		Competence competence = ( Competence ) ORMHelper.findEntity( Competence.class, id, 
				"journal", "description", "ancestorCompetence" );
		if ( competence == null ) {
			if ( log.isErrorEnabled() ) log.error( "CompetencePanel.editCompetence() : current competence with id=" + id + "  cannot find" );
			return;
		}		
		if ( ( competence.getVariety() != Competence.PROFESSIONAL_COMPETENCE ) || ( competence.getAncestor() != null ) ){ 
			CompetenceDlg.showCompetenceDlg( owner, "Просмотр компетенции", competence, true );
			return;
		}		

		Competence newCompetence = CompetenceDlg.showCompetenceDlg(owner, "Редактирование компетенции", competence, false);
		if ( newCompetence == null ) return;
		newCompetence.setAncestorCompetence( competence );
		CompetenceJPAController.edit( newCompetence, true );
		tree.getSelectedNode().setUserObject( ORMHelper.findEntity( Competence.class, id ) );
		tree.updateUI();
	}

	private void editIndicator() {
		if ( checkHistoryView() || !checkRightSelect( Indicator.class ) ) return;

		MainTreeNode te = tree.getSelectedNode();
		Long id = ( ( Indicator ) te.getUserObject() ).getId();
		Indicator indicator = ( Indicator ) ORMHelper.findEntity( Indicator.class, id, 
				"competence", "journal", "ancestorIndicator" );
		if ( indicator == null ) {
			if ( log.isErrorEnabled() ) log.error( "CompetencePanel.editIndicator() : current indicator with id=" + id + "  cannot find" );
			return;
		}		
		if ( ( indicator.getCompetence().getVariety() != Competence.PROFESSIONAL_COMPETENCE ) || ( indicator.getAncestor() != null ) ){ 
			IndicatorDlg.showIndicatorDlg( owner, "Просмотр индикатора", indicator, true );
			return;
		}		

		Indicator newIndicator = IndicatorDlg.showIndicatorDlg(owner, "Редактирование индикатора", indicator, false);
		if ( newIndicator == null ) return;
		newIndicator.setAncestor( indicator );
		IndicatorJPAController.edit( newIndicator, true );
		tree.getSelectedNode().setUserObject( ORMHelper.findEntity( Indicator.class, id ) );
		tree.updateUI();
	}
}