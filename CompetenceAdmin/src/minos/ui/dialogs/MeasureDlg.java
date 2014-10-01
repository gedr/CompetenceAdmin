package minos.ui.dialogs;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

import javax.swing.ImageIcon;
import javax.swing.text.JTextComponent;

import net.miginfocom.swing.MigLayout;

import com.alee.extended.date.WebDateField;
import com.alee.laf.button.WebButton;
import com.alee.laf.label.WebLabel;
import com.alee.laf.scroll.WebScrollPane;
import com.alee.laf.text.WebTextArea;

import minos.entities.Measure;
import minos.ui.ComponentFabrica;
import minos.ui.adapters.ActionAdapter;
import minos.utils.ResourceKeeper;
import minos.utils.ResourceKeeper.IType;

public class MeasureDlg extends BasisDlg<Measure> {
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
	private WebDateField start;
	private WebDateField stop;

	// =================================================================================================================
	// Constructors
	// =================================================================================================================
	public MeasureDlg( Window owner, String title, Measure source, boolean readOnly ) {
		super( owner, title, source, readOnly );
	}

	// =================================================================================================================
	// Methods for/from SuperClass/Interfaces
	// =================================================================================================================
	@Override
	protected void initUI() {
		ImageIcon icon = ( readOnly ? ResourceKeeper.getIcon( IType.MEASURE, ICON_SIZE ) 
				: ( source == null ? ResourceKeeper.getIcon( IType.MEASURE_ADD, ICON_SIZE ) 
						: ResourceKeeper.getIcon( IType.MEASURE_EDIT, ICON_SIZE ) ) ); 
		name = ComponentFabrica.createOneLineTextEditor( 30, readOnly, "Введите название мероприятия...", 
				source == null ? null : source.getName() );
		descr = ComponentFabrica.createMultiLineTextEditor(readOnly, source == null ? "" : source.getDescription() );
		Calendar cal = Calendar.getInstance();
		cal.add( Calendar.DAY_OF_YEAR, 1 );
		start = new WebDateField( source == null ? cal.getTime() : source.getStart());
		cal.add( Calendar.DAY_OF_YEAR, 7 );
		stop = new WebDateField( source == null ? cal.getTime() : source.getStop() );

		setLayout( new MigLayout( "", "[][grow]", "[][][][grow][]" ) ); 
		add( new WebLabel( icon ), "cell 0 0 1 3,gapx 0 10,gapy 0 10" );
		add( new WebLabel( "Название мероприятия" ), "cell 1 0,growx,aligny top" );
		add( name, "cell 1 1,growx,aligny top" );
		add( new WebLabel( "Описание мероприятия" ), "cell 1 2,growx,aligny bottom,gapy 5 0" );
		add( new WebScrollPane( descr ), "cell 0 3 2 1,grow");
		add( new WebLabel( "Начало", WebLabel.CENTER ), "cell 0 4 2 1,growx, left" );
		add( new WebLabel( "Завершение", WebLabel.CENTER ), "cell 0 4 2 1,growx, right" );
		add( start, "cell 0 5 2 1,growx, left" );
		add( stop, "cell 0 5 2 1,growx, right" );
		if ( !readOnly ) add( new WebButton( ActionAdapter.build( "OK", ResourceKeeper.getIcon( IType.OK, 24 ), 
				CMD_OK, "Сохранить", this, KeyEvent.VK_ENTER ) ), "cell 1 6,alignx right" );
		add( new WebButton( ActionAdapter.build( "Отмена", ResourceKeeper.getIcon( IType.CANCEL, 24 ), CMD_CANCEL, 
				"Выйти без сохранения", this, KeyEvent.VK_ESCAPE ) ), "cell 1 6,alignx right" );
		if ( ( source != null ) && ( source.getId() != 0 ) ) add( ComponentFabrica.createCollapsingPane( null, "Детали", 
				makeTechInfo(), new Dimension( 0, 100 ) ), "cell 0 7, span, growx, wrap" );	
	}

	@Override
	protected void save() {
		if ( readOnly || ( name.getText() == null ) || name.getText().trim().isEmpty() 
				|| ( ( source != null) && name.getText().trim().equals( source.getName() )
						&& ( ( ( source.getDescription() == null ) && ( descr.getText() == null ) )
								|| ( ( source.getDescription() != null ) 
										&& source.getDescription().equals( descr.getText() ) ) )
										&& ( source.getStart().equals( start.getDate() ) )
										&& ( source.getStop().equals( stop.getDate() ) ) ) ) return;
		result = ( source != null ? source : new Measure() );
		result.setName( name.getText() );
		result.setDescription( descr.getText() );
		result.setStart( new Timestamp( start.getDate().getTime() ) );
		result.setStop( new Timestamp( stop.getDate().getTime() ) );
	}

	// =================================================================================================================
	// Methods
	// =================================================================================================================
	public static Measure show( Window owner, String title, Measure entity, boolean readOnly ) {
		MeasureDlg dlg = new MeasureDlg( owner, title, entity, readOnly );
		dlg.setDefaultCloseOperation( DISPOSE_ON_CLOSE );
		dlg.setModal(true);
		dlg.pack();
		dlg.setVisible(true);	
		return dlg.getResult();		
	}	

	/**
	 * make component for display measure's service info
	 * @return WebList component have measure's service info or null 
	 */
	private Component makeTechInfo() {
		WebTextArea ta = new WebTextArea ();
		ta.setEditable( false );
		ta.append( "Код: " + source.getId() );
		if ( source.getJournal() == null ) return ta;
		
		ta.append( "\nСоздан: " + source.getJournal().getCreateMoment() );
		if ( !source.getJournal().getCreateMoment().equals( source.getJournal().getEditMoment() ) ) {
			ta.append( "\nРедактирован: " + source.getJournal().getEditMoment() );
		}
		if ( source.getJournal().getDeleteMoment().before( new Date() ) ) {
			ta.append( "\nУдален: " + source.getJournal().getDeleteMoment() );
		}
		return ta;
	}
}
