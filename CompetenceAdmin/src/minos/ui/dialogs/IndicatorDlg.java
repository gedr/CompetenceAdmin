package minos.ui.dialogs;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.KeyEvent;

import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.text.JTextComponent;

import net.miginfocom.swing.MigLayout;

import com.alee.laf.button.WebButton;
import com.alee.laf.label.WebLabel;
import com.alee.laf.list.WebList;
import com.alee.laf.text.WebTextArea;

import minos.data.orm.OrmHelper;
import minos.entities.Indicator;
import minos.entities.Level;
import minos.ui.ComponentFabrica;
import minos.ui.adapters.ActionAdapter;
import minos.utils.ResourceKeeper;
import minos.utils.ResourceKeeper.IType;

public class IndicatorDlg extends BasisDlg<Indicator> {
	// =================================================================================================================
	// Constants
	// =================================================================================================================
	private static final long serialVersionUID = 1L;
	private static final int ICON_SIZE = 48;

	// =================================================================================================================
	// Fields
	// =================================================================================================================
	private JTextComponent txt;
	private JComboBox<Level> cmb;

	// =================================================================================================================
	// Constructors
	// =================================================================================================================
	public IndicatorDlg(Window owner, String title, Indicator source, boolean readOnly) {
		super( owner, title, source, readOnly );
	}

	// =================================================================================================================
	// Methods for/from SuperClass/Interfaces
	// =================================================================================================================
	@Override
	protected void initUI() {
		ImageIcon icon = ( readOnly ? ResourceKeeper.getIcon( IType.INDICATOR, ICON_SIZE ) 
				: ( source == null ? ResourceKeeper.getIcon( IType.INDICATOR_ADD, ICON_SIZE ) 
						: ResourceKeeper.getIcon( IType.INDICATOR_ADD, ICON_SIZE ) ) ); 
		txt = ComponentFabrica.createOneLineTextEditor( 30, readOnly, "Введите название индикатора...", 
				source == null ? null : source.getName() );
		cmb = ComponentFabrica.createLevelComboBox( source == null ? null : source.getLevel() );

		setLayout( new MigLayout( "", "[][grow]", "[][][][][]" + ( source == null ? "" : "[][]" ) ) ); // [grow]
		add( new WebLabel( icon ), "cell 0 0 1 3,gapx 0 10,gapy 0 10" );
		add( new WebLabel( "Название индикатора" ), "cell 1 0,growx,aligny top");
		add( txt, "cell 1 1,growx,a ligny top" );
		add( new WebLabel( "Уровень индикатора" ), "cell 1 2,growx,aligny bottom,gapy 5 0" );			
		add( cmb, "cell 0 3 2 1,grow" );
		if ( !readOnly ) add( new WebButton( ActionAdapter.build( "OK", null, CMD_OK, "Сохранить ", this, 
				KeyEvent.VK_ENTER ) ), "cell 1 4,alignx right" );
		add( new WebButton( ActionAdapter.build( "Отмена", null, CMD_CANCEL, "Выйти без сохранения", this, 
				KeyEvent.VK_ESCAPE ) ), "cell 1 4,alignx right" );
		if ( source != null )  {
			add( ComponentFabrica.createCollapsingPane( null, "Детали", makeTechInfo(), new Dimension( 0, 100 ) ), 
					"cell 0 5, span, growx, wrap" );	
			Component c = makeHistoryInfo();
			if ( c != null ) add( ComponentFabrica.createCollapsingPane( null, "История", c, new Dimension( 0, 100 ) ), 
					"cell 0 6, span, growx, wrap" );
		}
	}

	@Override
	protected void save() {
		if ( readOnly || ( txt.getText() == null ) || ( txt.getText().trim().length() == 0 ) 
				|| ( ( source != null ) && txt.getText().equals( source.getName() ) 
						&& cmb.getSelectedItem().equals( source.getLevel() ) ) ) return;
		result = new Indicator( txt.getText(), ( source == null ? ( short ) 1 : source.getItem() ), 
				Indicator.STATUS_ACTIVE, ( short ) 1, ( source == null ? null : source.getCompetence() ), 
				( Level ) cmb.getSelectedItem(), null, null );
	}

	// =================================================================================================================
	// Methods
	// =================================================================================================================
	public static Indicator show( Window owner, String title, Indicator entity, boolean readOnly ) {
		IndicatorDlg dlg = new IndicatorDlg( owner, title, entity, readOnly );
		dlg.setDefaultCloseOperation( DISPOSE_ON_CLOSE );
		dlg.setModal(true);
		dlg.pack();
		dlg.setVisible(true);	
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
		Indicator ind = ( Indicator ) OrmHelper.findEntity( Indicator.class, source.getId(), "ancestor", "historyList" );
		if ( ind.getAncestor() != null ) {
			ind = ( Indicator ) OrmHelper.findEntity( Indicator.class, ind.getAncestor().getId(), "historyList" ); 
		}
		if ( ( ind.getHistoryList() == null ) || ( ind.getHistoryList().size() == 0 ) ) return null;				
		WebList list = new WebList( ind.getHistoryList() );
		list.setEditable( false );
		return list;	
	}
}