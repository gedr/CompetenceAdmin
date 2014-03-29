package minos.ui.panels;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import minos.resource.managers.IconResource;
import minos.resource.managers.IconResource.IconType;
import minos.ui.adapters.ActionAdapter;
import minos.ui.models.CatalogTreeModel;

import com.alee.laf.panel.WebPanel;
import com.alee.laf.scroll.WebScrollPane;
import com.alee.laf.toolbar.WebToolBar;
import com.alee.laf.tree.WebTree;

public class CompetencePanel extends WebPanel implements ActionListener{
	private static final long serialVersionUID = 1L;
	private static final String CATALOG_ADD_CMD 	= "a";
	private static final String COMPETENCE_ADD_CMD 	= "b";
	private static final String INDICATOR_ADD_CMD 	= "c";
	private static final String REFRESH_CMD 		= "d";
	
	
	private WebTree tree = null;

	public CompetencePanel() {
		init();
	}
	
	private void init() {
		setLayout(new BorderLayout());
		WebToolBar tb = new WebToolBar();
		
		tb.add( new ActionAdapter("add catalog", IconResource.getInstance().getIcon(IconType.ADD, 24), 
				CATALOG_ADD_CMD, "Добавление нового подкаталога в каталог", this, 0) ); //(ImageIcon)res.get("icon.addFolder.32")
		tb.add( new ActionAdapter("add competence", null, COMPETENCE_ADD_CMD, 
				"Добавление новой компетенции в каталог", this, 0) ); //(ImageIcon)res.get("icon.addCompetence.32")
		tb.add( new ActionAdapter("add indicator", null, INDICATOR_ADD_CMD, 
				"Добавление нового индикатора в компетенцию", this, 0) ); //(ImageIcon)res.get("icon.addIndicator.32")
		tb.add( new ActionAdapter("refresh", null, REFRESH_CMD, 
				"Обновить данные", this, 0) ); //(ImageIcon)res.get("icon.refresh.32")

		add( tb, BorderLayout.NORTH );
		
		tree = new WebTree<>( new CatalogTreeModel() );
		tree.setRootVisible(false);
		add( new WebScrollPane(tree), BorderLayout.CENTER );
/*						
		treeCatalogAndCompetence = new JTree((TreeModel)res.get("catalog.competence.TreeModel"));
		treeCatalogAndCompetence.setRootVisible(false);
		treeCatalogAndCompetence.setCellRenderer((TreeCellRenderer) res.get("tree.renderer"));
*/
		
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		switch ( e.getActionCommand() ) {
		case CATALOG_ADD_CMD:			
			break;

		case COMPETENCE_ADD_CMD:			
			break;

		case INDICATOR_ADD_CMD:			
			break;

		case REFRESH_CMD:			
			break;

		default:
			break;
		}
		
		
	}

}
