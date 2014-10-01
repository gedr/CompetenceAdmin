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
import com.alee.laf.scroll.WebScrollPane;
import com.alee.laf.text.WebTextArea;

import minos.data.orm.OrmHelper;
import minos.entities.Competence;
import minos.ui.ComponentFabrica;
import minos.ui.adapters.ActionAdapter;
import minos.utils.ResourceKeeper;
import minos.utils.ResourceKeeper.IType;

public class CompetenceDlg extends BasisDlg<Competence> {
	// =================================================================================================================
	// Constants
	// =================================================================================================================
	private static final long serialVersionUID = 1L;
	private static final int ICON_SIZE = 48;

	// =================================================================================================================
	// Fields
	// =================================================================================================================
	private JTextComponent name;
	private JTextComponent descr;

	// =================================================================================================================
	// Constructors
	// =================================================================================================================
	public CompetenceDlg( Window owner, String title, Competence source, boolean readOnly) {
		super(owner, title, source, readOnly);
		// TODO Auto-generated constructor stub
	}

	// =================================================================================================================
	// Methods for/from SuperClass/Interfaces
	// =================================================================================================================
	@Override
	protected void initUI() {
		ImageIcon icon = ResourceKeeper.getIcon( readOnly ? IType.COMPETENCE 
				: ( source == null ? IType.COMPETENCE_ADD : IType.COMPETENCE_EDIT ), ICON_SIZE );
		name = ComponentFabrica.createOneLineTextEditor( 30, readOnly, "Введите название компетенции...", 
				source == null ? null : source.getName() );
		descr = ComponentFabrica.createMultiLineTextEditor(readOnly, source == null ? null : source.getDescription() );

		setLayout( new MigLayout( "", "[][grow]", "[][][][grow][]" ) ); 
		add( new WebLabel( icon ), "cell 0 0 1 3,gapx 0 10,gapy 0 10" );
		add( new WebLabel( "Название компетенции" ), "cell 1 0,growx,aligny top" );
		add( name, "cell 1 1,growx,aligny top" );
		add( new WebLabel( "Описание компетенции" ), "cell 1 2,growx,aligny bottom,gapy 5 0" );
		add( new WebScrollPane( descr ), "cell 0 3 2 1,grow");
		if ( !readOnly ) add( new WebButton( ActionAdapter.build( "OK", null, CMD_OK, "Сохранить", this, 
				KeyEvent.VK_ENTER ) ), "cell 1 4,alignx right" );
		add( new WebButton( ActionAdapter.build( "Отмена", null, CMD_CANCEL, "Выйти без сохранения", this, 
				KeyEvent.VK_ESCAPE ) ), "cell 1 4,alignx right" );
		
		if ( source != null )  {
			add( ComponentFabrica.createCollapsingPane( null, "Детали", makeTechInfo(), new Dimension( 0, 100 ) ), 
					"cell 0 5, span, growx, wrap" );	
			Component hic = makeHistoryInfo();
			if ( hic != null ) add( ComponentFabrica.createCollapsingPane( null, "История", hic, 
					new Dimension( 0, 100 ) ), 
					"cell 0 6, span, growx, wrap" );
		}
	}

	@Override
	protected void save() {
		if ( readOnly || ( name.getText() == null ) || ( name.getText().trim().length() == 0 )
				|| ( ( source != null) && name.getText().equals( source.getName() )
						&& ( ( ( source.getDescription() == null ) && ( descr.getText() == null ) )
								|| ( ( source.getDescription() != null ) 
										&& source.getDescription().equals( descr.getText() ) ) ) ) ) return;
		result = new Competence( name.getText(), descr.getText(), ( source == null ? ( short ) 1 : source.getItem() ), 
				Competence.STATUS_ACTIVE, ( source == null ? Competence.ADMINISTRATIVE_COMPETENCE : source.getVariety() ),
				( short ) 1, ( source == null ? null : source.getCatalog() ), 
				null, null, null );
	}

	// =================================================================================================================
	// Methods
	// =================================================================================================================
	public static Competence show( Window owner, String title, Competence entity, boolean readOnly ) {
		CompetenceDlg dlg = new CompetenceDlg( owner, title, entity, readOnly );
		dlg.setDefaultCloseOperation( DISPOSE_ON_CLOSE );
		dlg.setModal(true);
		dlg.pack();
		dlg.setVisible(true);	
		return dlg.getResult();		
	}	

	/**
	 * make component for display competence's service info
	 * @return WebList component have competence's service info or null 
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
	 * make component for display competence's history info
	 * @return WebList component have competence's history entity or null 
	 */
	private Component makeHistoryInfo() {
		Competence cat = ( Competence ) OrmHelper.findEntity( Competence.class, source.getId(), "ancestor", "historyList" );
		if ( cat.getAncestor() != null ) {
			cat = ( Competence ) OrmHelper.findEntity( Competence.class, cat.getAncestor().getId(), "historyList" ); 
		}
		if ( ( cat.getHistoryList() == null ) || ( cat.getHistoryList().size() == 0 ) ) return null;				
		WebList list = new WebList( cat.getHistoryList() );
		list.setEditable( false );
		return list;	
	}
}