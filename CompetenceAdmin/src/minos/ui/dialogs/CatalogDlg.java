package minos.ui.dialogs;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.KeyEvent;

import javax.swing.ImageIcon;
import javax.swing.text.JTextComponent;

import net.miginfocom.swing.MigLayout;

import com.alee.laf.button.WebButton;
import com.alee.laf.label.WebLabel;
import com.alee.laf.list.WebList;
import com.alee.laf.text.WebTextArea;

import minos.data.orm.OrmHelper;
import minos.entities.Catalog;
import minos.entities.Journal;
import minos.ui.ComponentFabrica;
import minos.ui.adapters.ActionAdapter;
import minos.utils.ResourceKeeper;
import minos.utils.ResourceKeeper.IType;

public class CatalogDlg extends BasisDlg<Catalog> {
	// =================================================================================================================
	// Constants
	// =================================================================================================================
	private static final long serialVersionUID = 1L;
	private static final int ICON_SIZE = 48;

	// =================================================================================================================
	// Fields
	// =================================================================================================================
	private JTextComponent txt;

	// =================================================================================================================
	// Constructors
	// =================================================================================================================
	public CatalogDlg( Window owner, String title, Catalog source, boolean readOnly) {
		super( owner, title, source, readOnly );
	}

	// =================================================================================================================
	// Methods for/from SuperClass/Interfaces
	// =================================================================================================================
	@Override
	protected void initUI() {
		ImageIcon icon = ( readOnly ? ResourceKeeper.getIcon( IType.CATALOG_SIMPLE, ICON_SIZE ) 
				: ( source == null ? ResourceKeeper.getIcon( IType.CATALOG_ADD, ICON_SIZE ) 
						: ResourceKeeper.getIcon( IType.CATALOG_EDIT, ICON_SIZE ) ) ); 

		txt = ComponentFabrica.createOneLineTextEditor( 30, readOnly, "Введите название каталога...", 
				source == null ? null : source.getName() );

		setLayout( new MigLayout( "", "[][grow]", "[][][][][]" ) ); 

		add( new WebLabel( icon ), "cell 0 0 1 3,gapx 0 10,gapy 0 10" );
		add( new WebLabel( "Название" ), "cell 1 0,growx,aligny top" );
		add( txt, "cell 1 1,growx,aligny top" );
		if ( !readOnly ) add( new WebButton( ActionAdapter.build( "OK", ResourceKeeper.getIcon( IType.OK, 24 ), CMD_OK, 
				"Сохранить", this, KeyEvent.VK_ENTER ) ), "cell 1 2,alignx right" );
		add( new WebButton( ActionAdapter.build( "Отмена", ResourceKeeper.getIcon( IType.CANCEL, 24 ), CMD_CANCEL, 
				"Выйти без сохранения", this, KeyEvent.VK_ESCAPE ) ), "cell 1 2,alignx right" );

		if ( source != null )  {
			add( ComponentFabrica.createCollapsingPane( null, "Детали", makeTechInfo(), new Dimension( 0, 100 ) ), 
					"cell 0 3, span, growx, wrap" );	
			Component hic = makeHistoryInfo();
			if ( hic != null ) add( ComponentFabrica.createCollapsingPane( null, "История", hic, 
					new Dimension( 0, 100 ) ), 
					"cell 0 4, span, growx, wrap" );
		}
	}

	@Override
	protected void save() {
		if ( readOnly || ( txt.getText() == null ) || ( txt.getText().trim().length() == 0 ) 
				|| ( ( source != null ) && txt.getText().equals( source.getName() ) ) ) return;
		result = new Catalog( txt.getText(), 
				source == null ? 0 : source.getItem(), 
						Catalog.STATUS_ACTIVE, 
						source == null ? Catalog.EMPTY : source.getVariety(), 
								( short ) 1,
								source == null ? null : source.getParentCatalog(),
										null, null, null, ( Journal ) null );
	}

	// =================================================================================================================
	// Methods
	// =================================================================================================================
	public static Catalog show( Window owner, String title, Catalog catalog, boolean readOnly ) {
		CatalogDlg dlg = new CatalogDlg( owner, title, catalog, readOnly );
		dlg.setDefaultCloseOperation( DISPOSE_ON_CLOSE );
		dlg.setModal( true );
		dlg.pack();
		dlg.setVisible( true );	
		return dlg.getResult();		
	}	

	/**
	 * make component for display catalog's service info
	 * @return WebList component have catalog's service info or null 
	 */
	private Component makeTechInfo() {
		WebTextArea ta = new WebTextArea ();
		ta.setEditable( false );
		ta.append( "Код: " + source.getId() );
		ta.append( "\nВерсия: " + source.getVersion() );
		ta.append( "\nСтатус: " + source.getStatus() );
		ta.append( "\nСоздан: " + source.getJournal().getCreateMoment() );
		if ( source.getVersion() > 1 ) ta.append( "\nРедактирован: " + source.getJournal().getEditMoment() );
		return ta;
	}

	/**
	 * make component for display catalog's history info
	 * @return WebList component have catalog's history entity or null 
	 */
	private Component makeHistoryInfo() {
		Catalog cat = ( Catalog ) OrmHelper.findEntity( Catalog.class, source.getId(), "ancestor", "historyList" );
		if ( cat.getAncestor() != null ) {
			cat = ( Catalog ) OrmHelper.findEntity( Catalog.class, cat.getAncestor().getId(), "historyList" ); 
		}
		if ( ( cat.getHistoryList() == null ) || ( cat.getHistoryList().size() == 0 ) ) return null;				
		WebList list = new WebList( cat.getHistoryList() );
		list.setEditable( false );
		return list;	
	}
}