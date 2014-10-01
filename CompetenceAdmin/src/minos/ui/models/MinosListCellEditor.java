package minos.ui.models;

import java.awt.Font;

import javax.swing.JList;
import javax.swing.JTextField;

import com.alee.laf.list.WebListStyle;
import com.alee.laf.list.editor.AbstractListCellEditor;
import com.alee.laf.text.WebTextField;

public class  MinosListCellEditor extends AbstractListCellEditor<JTextField, String> {
	//=====================================================================================================
	//=                                            Attributes                                             =
	//=====================================================================================================
	private String promt;

	//=====================================================================================================
	//=                                           Constructors                                            =
	//=====================================================================================================
	public MinosListCellEditor( String promt ) {
		this.promt = promt;
	}

	//=====================================================================================================
	//=                                                     Methods                                       =
	//=====================================================================================================
	@Override
	public String getCellEditorValue( @SuppressWarnings("rawtypes") JList list,
			int index, String oldValue ) {
		return ( ( WebTextField ) editor ).getText();
	}

	@Override
	protected JTextField createCellEditor( @SuppressWarnings("rawtypes")  JList list,
			int index, String value ) {
		if ( editor == null ) {
			WebTextField field = WebTextField.createWebTextField( true, 
					WebListStyle.selectionRound, 
					WebListStyle.selectionShadeWidth );
			field.setInputPrompt( promt );
			field.setInputPromptFont( field.getFont().deriveFont( Font.ITALIC ) );
			field.setDrawFocus( false );
			editor = field;
		}
		editor.setText( value != null ? value : "" );
		editor.selectAll();
		return editor;
	}
}
