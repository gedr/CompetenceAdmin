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
import com.alee.laf.text.WebTextArea;

import minos.entities.Division;
import minos.ui.ComponentFabrica;
import minos.ui.adapters.ActionAdapter;
import minos.utils.ResourceKeeper;
import minos.utils.ResourceKeeper.IType;

public class DivisionDlg extends BasisDlg<Division> {
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
	public DivisionDlg( Window owner, String title, Division source, boolean readOnly ) {		
		super( owner, title, source, readOnly );
		if ( !readOnly || ( source == null ) ) throw new IllegalArgumentException( "DivisionDlg() : illegal param" );
	}

	// =================================================================================================================
	// Methods for/from SuperClass/Interfaces
	// =================================================================================================================
	@Override
	protected void initUI() {
		ImageIcon icon = ( readOnly ? ResourceKeeper.getIcon( IType.OFFICE, ICON_SIZE ) 
				: ( source == null ? ResourceKeeper.getIcon( IType.CATALOG_ADD, ICON_SIZE ) 
						: ResourceKeeper.getIcon( IType.CATALOG_EDIT, ICON_SIZE ) ) ); 
		txt = ComponentFabrica.createOneLineTextEditor( 30, readOnly, "", source.getFullName() );

		setLayout( new MigLayout( "", "[][grow]", "[][][][][]" ) ); 

		add( new WebLabel( icon ), "cell 0 0 1 3,gapx 0 10,gapy 0 10" );
		add( new WebLabel( "��������" ), "cell 1 0,growx,aligny top" );
		add( txt, "cell 1 1,growx,aligny top" );
		add( new WebButton( ActionAdapter.build( "������", ResourceKeeper.getIcon( IType.CANCEL, 24 ), CMD_CANCEL, 
				"����� ��� ����������", this, KeyEvent.VK_ESCAPE ) ), "cell 1 2,alignx right" );
		add( ComponentFabrica.createCollapsingPane( null, "������", makeTechInfo(), new Dimension( 0, 100 ) ), 
				"cell 0 3, span, growx, wrap" );	
	}

	@Override
	protected void save() { /* not used */ }

	// =================================================================================================================
	// Methods
	// =================================================================================================================
	public static Division show( Window owner, String title, Division source, boolean readOnly ) {
		DivisionDlg dlg = new DivisionDlg( owner, title, source, readOnly );
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
		ta.append( "���: " + source.getId() );
		ta.append( "\n����� ������: " + source.getBeginDate() );
		ta.append( "\n����� ����������: " + source.getEndDate() );
		ta.append( "\n������� ������: " + ( source.getOtizOk()  > 0 ? "��" : "���" ) );
		ta.append( "\n�������� ��������: " + ( source.getIsdelete() > 0 ? "��" : "���" ) );
		return ta;
	}
}