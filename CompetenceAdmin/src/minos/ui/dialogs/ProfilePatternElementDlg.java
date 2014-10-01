package minos.ui.dialogs;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.KeyEvent;

import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.text.JTextComponent;

import net.miginfocom.swing.MigLayout;

import com.alee.laf.button.WebButton;
import com.alee.laf.label.WebLabel;
import com.alee.laf.scroll.WebScrollPane;
import com.alee.laf.text.WebTextArea;

import minos.entities.Competence;
import minos.entities.Level;
import minos.entities.ProfilePatternElement;
import minos.ui.ComponentFabrica;
import minos.ui.adapters.ActionAdapter;
import minos.utils.ResourceKeeper;
import minos.utils.ResourceKeeper.IType;

public class ProfilePatternElementDlg extends BasisDlg<ProfilePatternElement> {
	// =================================================================================================================
	// Constants
	// =================================================================================================================
	private static final long serialVersionUID = 1L;
	
	// =================================================================================================================
	// Fields
	// =================================================================================================================
	private JComboBox<Level> cmb;
	
	// =================================================================================================================
	// Constructors
	// =================================================================================================================
	public ProfilePatternElementDlg(Window owner, String title, ProfilePatternElement source, boolean readOnly) {
		super(owner, title, source, readOnly);
	}

	// =================================================================================================================
	// Methods for/from SuperClass/Interfaces
	// =================================================================================================================
	@Override
	protected void initUI() {
		Icon icon = ResourceKeeper.getIcon( readOnly ? IType.COMPETENCE : IType.COMPETENCE_EDIT, 48 );
		Competence c = ( source == null ? null : source.getCompetence() );
		JTextComponent cn = ComponentFabrica.createOneLineTextEditor( 30, true, "", ( c == null ? "" : c.getName() ) );  
		JTextComponent cd = ComponentFabrica.createMultiLineTextEditor( true, ( c == null ? "" : c.getDescription() ) );
		cmb = ComponentFabrica.createLevelComboBox( source == null ? null : source.getMinLevel() );

		setLayout( new MigLayout( "", "[][grow]", "[][][][][][grow][][]" ) ); 
		add( new WebLabel( icon ), "cell 0 0 1 3,gapx 0 10,gapy 0 10" );
		add( new WebLabel( "Минимальный уровень компетенции" ), "cell 1 0,growx,aligny" );			
		add( cmb, "cell 1 1,growx,aligny" );
		add( new WebLabel( "Название компетенции" ), "cell 1 2,growx,aligny" );
		add( cn, "cell 1 3,growx,aligny top" );
		add( new WebLabel( "Описание компетенции" ), "cell 1 4,growx,aligny bottom,gapy 5 0" );
		add( new WebScrollPane( cd ), "cell 0 5 2 1,grow" );
		
		if ( !readOnly ) add( new WebButton( ActionAdapter.build( "OK", ResourceKeeper.getIcon( IType.OK, 24 ), 
				CMD_OK, "Сохранить", this, KeyEvent.VK_ENTER ) ), "cell 1 6,alignx right" );
		add( new WebButton( ActionAdapter.build( "Отмена", ResourceKeeper.getIcon( IType.CANCEL, 24 ), 
				CMD_CANCEL, "Выйти без сохранения", this, KeyEvent.VK_ESCAPE ) ),	"cell 1 6,alignx righ" );
		if ( source != null ) {
			add( ComponentFabrica.createCollapsingPane( null, "Детали", makeTechInfo(), new Dimension( 0, 100 ) ),
					 "cell 0 7 2 1,grow" );
		}
	}

	@Override
	protected void save() {
		if ( readOnly ) return;
		ProfilePatternElement ppe = ( ProfilePatternElement ) source;
		if ( ppe == null ) ppe = new ProfilePatternElement();
		ppe.setMinLevel( ( Level ) cmb.getSelectedItem() );
		result = ppe;
	}

	// =================================================================================================================
	// Methods
	// =================================================================================================================
	public static ProfilePatternElement show( Window owner, String title, ProfilePatternElement entity, boolean readOnly ) {
		ProfilePatternElementDlg dlg = new ProfilePatternElementDlg( owner, title, entity, readOnly );
		dlg.setDefaultCloseOperation( DISPOSE_ON_CLOSE );
		dlg.setModal( true );
		dlg.pack();
		dlg.setVisible( true );	
		return dlg.getResult();		
	}	

	private Component makeTechInfo() {
		WebTextArea txtArea = new WebTextArea ();
		txtArea.setEditable( false );
		txtArea.append( "Код: " + source.getId() );
		txtArea.append( "\nСтатус: " + source.getStatus() );
		txtArea.append( "\nСоздан: " + source.getJournal().getCreateMoment() );
		if ( !source.getJournal().getCreateMoment().equals( source.getJournal().getEditMoment() ) ) {
			txtArea.append( "\nРедактирован: " + source.getJournal().getEditMoment() );
		}
		if ( source.getCompetence() != null ) {
			txtArea.append( "\nКод компетенции: " + source.getCompetence().getId() );
			txtArea.append( "\nВерсия компетенции: " + source.getCompetence().getVersion() );
		}
		return txtArea;
	}
}