package minos.ui.panels;

import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
import minos.ui.models.TreeElement;
import minos.ui.models.TreeElement.TreeElementType;

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
	private static Logger log = LoggerFactory.getLogger( CompetencePanel.class );	
	
	private WebAsyncTree<TreeElement> tree = null;
	private CompetenceDataProvider cdp = null;
	private WebToggleButton clockBtn = null;
	private Window owner = null;

	public CompetencePanel(Window owner) {
		this.owner = owner;
		init();
	}
	
	private void init() {
		setLayout(new BorderLayout());
		clockBtn = new WebToggleButton( null, IconResource.getInstance().getIcon( IconType.CLOCK, 32 ) );
		clockBtn.setShadeToggleIcon ( true );
		clockBtn.setActionCommand( CLOCK_CMD );
		clockBtn.addActionListener( this );
        
        
		WebToolBar tb = new WebToolBar();		
		tb.add( new ActionAdapter("add catalog", IconResource.getInstance().getIcon(IconType.ADD, 24), 
				CATALOG_ADD_CMD, "Добавление нового подкаталога в каталог", this, 0) ); //(ImageIcon)res.get("icon.addFolder.32")
		tb.add( new ActionAdapter("add competence", null, COMPETENCE_ADD_CMD, 
				"Добавление новой компетенции в каталог", this, 0) ); //(ImageIcon)res.get("icon.addCompetence.32")
		tb.add( new ActionAdapter("add indicator", null, INDICATOR_ADD_CMD, 
				"Добавление нового индикатора в компетенцию", this, 0) ); //(ImageIcon)res.get("icon.addIndicator.32")
		tb.add( new ActionAdapter("edit", null, EDIT_CMD, 
				"Редактирование элемента", this, 0) ); //(ImageIcon)res.get("icon.addIndicator.32")
		tb.add( new ActionAdapter("refresh", IconResource.getInstance().getIcon( IconType.REFRESH, 32 ), REFRESH_CMD, 
				"Обновить данные", this, 0) );
		tb.add( clockBtn );

		add( tb, BorderLayout.NORTH );
		
		cdp = new CompetenceDataProvider( new CatalogDataProvider() );
		tree = new WebAsyncTree<>( cdp );
		tree.setRootVisible(false);
		add( new WebScrollPane(tree), BorderLayout.CENTER );
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		switch ( e.getActionCommand() ) {
		case CATALOG_ADD_CMD:	
			addCatalog();			
			break;

		case COMPETENCE_ADD_CMD:
			addCompetence();
			break;

		case INDICATOR_ADD_CMD:
			addIndicator();
			break;

		case REFRESH_CMD:
			tree.reloadRootNode();
			break;
		
		case EDIT_CMD:
			editDispatch();			
			break;

		case CLOCK_CMD:
			if ( clockBtn.isSelected() ) {
				TimePointDlg.showTimePointDlgDlg( owner, "Время в прошлом" );
			}
			break;

		default:
			break;
		}		
	}
	
	private boolean checkHistoryView() {
		if ( cdp.getCurrentTimePoint() != BasisDataProvider.FUTURE_TIMEPOINT ) {
			WebOptionPane.showMessageDialog( owner, "При просмотре истории нельзя вносить изменения",
					"Ошибка", WebOptionPane.ERROR_MESSAGE );
			return true;
		}
		return false;		
	}

	private boolean checkRightSelect(TreeElementType... tets) {
 		if ( ( tree.getSelectionCount() != 1 ) ) {
			WebOptionPane.showMessageDialog( owner, 
					((tree.getSelectionCount() == 0) ? "Не выбран элемент" : "Выбрано много элементов"), 
					"Ошибка", WebOptionPane.ERROR_MESSAGE );
			return false;
		}		

 		if ( ( tets == null ) || ( tets.length == 0 ) ) return true;
 		for ( int i = 0; i < tets.length; i++ ) {
 			if ( tree.getSelectedNode().getType() == tets[i] ) return true;
 		}
		
		WebOptionPane.showMessageDialog( owner, "Выбран некорректный элемент. Ожидалось :" + tets.toString(),
					"Ошибка", WebOptionPane.ERROR_MESSAGE );
		return false;		
	}

	private void editDispatch() {
		if ( checkHistoryView() || !checkRightSelect( TreeElementType.CATALOG, TreeElementType.COMPETENCE, TreeElementType.INDICATOR ) ) return;
		switch( tree.getSelectedNode().getType() ) {
		case CATALOG:
			editCatalog();
			break;
		case COMPETENCE:
			editCompetence();
			break;
		case INDICATOR:
			editIndicator();
			break;
		default:
			break;		
		}		
	}

	
	private void addIndicator() {
		if ( checkHistoryView() || !checkRightSelect( TreeElementType.COMPETENCE ) ) return;
		
		Indicator newIndicator = IndicatorDlg.showIndicatorDlg( owner,  "Новый индикатор", null, false );
		if ( newIndicator == null ) return;
		
		TreeElement te = tree.getSelectedNode();
		Competence competence = (Competence) ORMHelper.findEntity( Competence.class, te.getAncestor() );
		if( competence == null ) {
			if ( log.isErrorEnabled() ) log.error("CompetencePanel.addIndicator(): cannot find competence in db id=" + te.getAncestor());
			return;
		}
		newIndicator.setCompetence( competence );
		IndicatorJPAController.create( newIndicator, true );
	}
	
	private void addCompetence() {
		if ( checkHistoryView() || !checkRightSelect( TreeElementType.CATALOG ) ) return;
		
		Competence newCompetence = CompetenceDlg.showCompetenceDlg( owner,  "Новая компетенция", null, false );
		if ( newCompetence == null ) return;
		
		TreeElement te = tree.getSelectedNode();
		Catalog catalog = (Catalog) ORMHelper.findEntity( Catalog.class, te.getAncestor() );
		if( catalog == null ) {
			if ( log.isErrorEnabled() ) log.error("CompetencePanel.addCompetence(): cannot find parent catalog in db id=" + te.getAncestor());
			return;
		}
		newCompetence.setCatalog( catalog );
		CompetenceJPAController.create( newCompetence, true );
	}
	
	private void addCatalog() {
		if ( checkHistoryView() || !checkRightSelect( TreeElementType.CATALOG ) ) return;

		Catalog newCatalog = CatalogDlg.showCatalogDlg(owner, "Новый каталог", null, false);
		if ( newCatalog == null ) return;

		TreeElement te = tree.getSelectedNode();
		Catalog parentCatalog = (Catalog) ORMHelper.findEntity( Catalog.class, te.getAncestor() );
		if( parentCatalog == null ) {
			if ( log.isErrorEnabled() ) log.error("CompetencePanel.addCatalog(): cannot find parent catalog in db id=" + te.getAncestor());
			return;
		}
		newCatalog.setParentCatalog( parentCatalog );
		CatalogJPAController.create( newCatalog, true );
	}	
	
	private void editCatalog() {
		if ( checkHistoryView() || !checkRightSelect( TreeElementType.CATALOG ) ) return;

		TreeElement te = tree.getSelectedNode();
		if ( te.getCurrent() != te.getAncestor() )  {
			if ( log.isErrorEnabled() ) log.error("CompetencePanel.editCatalog() : текущий и предок разные каталоги ");
			return;
		}

		Catalog current = (Catalog) ORMHelper.findEntity( Catalog.class, te.getCurrent(), "parentCatalog", "journal" );
		if ( current == null ) {
			if ( log.isErrorEnabled() ) log.error("CompetencePanel.editCatalog() : current catalog with id=" + te.getCurrent() + "  cannot find");
			return;
		}
		
		if ( current.getId() < 5 ) { 
			CatalogDlg.showCatalogDlg(owner, "Просмотр каталога", current, true);
			if ( log.isErrorEnabled() ) log.error("CompetencePanel.editCatalog() : cannot edit catalog where id < 5");
			return;
		}		

		Catalog newCatalog = CatalogDlg.showCatalogDlg(owner, "Редактирование каталога", current, false);
		if ( newCatalog == null ) return;
		String str = newCatalog.getName();
		newCatalog.setAncestorCatalog( current );
		CatalogJPAController.edit( newCatalog, true );
		te.setName( str );
	}
	
	private void editCompetence() {
		if ( checkHistoryView() || !checkRightSelect( TreeElementType.COMPETENCE ) ) return;

		TreeElement te = tree.getSelectedNode();
		if ( te.getCurrent() != te.getAncestor() )  {
			if ( log.isErrorEnabled() ) log.error("CompetencePanel.editCompetence() : текущий и предок разные компетенции ");
			return;
		}

		Competence current = ( Competence ) ORMHelper.findEntity( Competence.class, te.getCurrent(), "journal", "description" );
		if ( current == null ) {
			if ( log.isErrorEnabled() ) log.error( "CompetencePanel.editCatalog() : current catalog with id=" + te.getCurrent() + "  cannot find" );
			return;
		}
		
		if ( current.getVariety() != Competence.PROFESSIONAL_COMPETENCE ) { 
			CompetenceDlg.showCompetenceDlg( owner, "Просмотр компетенции", current, true );
			if ( log.isErrorEnabled() ) log.error("CompetencePanel.editCompetence() : cannot edit competence not PROFESSIONAL_COMPETENCE");
			return;
		}		

		Competence newCompetence = CompetenceDlg.showCompetenceDlg(owner, "Редактирование компетенции", current, false);
		if ( newCompetence == null ) return;
		String str = newCompetence.getName();
		newCompetence.setAncestorCompetence( current );
		CompetenceJPAController.edit( newCompetence, true );
		te.setName( str );
	}

	private void editIndicator() {
		if ( checkHistoryView() || !checkRightSelect( TreeElementType.INDICATOR ) ) return;

		TreeElement te = tree.getSelectedNode();
		if ( te.getCurrent() != te.getAncestor() )  {
			if ( log.isErrorEnabled() ) log.error("CompetencePanel.editCompetence() : текущий и предок разные компетенции ");
			return;
		}

		Indicator current = ( Indicator ) ORMHelper.findEntity( Indicator.class, te.getCurrent(), "competence", "journal" );
		if ( current == null ) {
			if ( log.isErrorEnabled() ) log.error( "CompetencePanel.editCatalog() : current catalog with id=" + te.getCurrent() + "  cannot find" );
			return;
		}
		
		if ( current.getCompetence().getVariety() != Competence.PROFESSIONAL_COMPETENCE ) { 
			IndicatorDlg.showIndicatorDlg( owner, "Просмотр индикатора", current, true );
			if ( log.isErrorEnabled() ) log.error("CompetencePanel.editIndicator() : cannot edit indicator not PROFESSIONAL_COMPETENCE");
			return;
		}		

		Indicator newIndicator = IndicatorDlg.showIndicatorDlg(owner, "Редактирование индикатора", current, false);
		if ( newIndicator == null ) return;
		String str = newIndicator.getName();
		newIndicator.setAncestorIndicator( current );
		IndicatorJPAController.edit( newIndicator, true );
		te.setName( str );
	}
}