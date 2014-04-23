package minos.ui.dialogs;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;

import net.miginfocom.swing.MigLayout;

import com.alee.extended.date.WebCalendar;
import com.alee.laf.button.WebButton;
import com.alee.laf.optionpane.WebOptionPane;
import com.alee.laf.rootpane.WebDialog;
import com.alee.laf.spinner.WebSpinner;

public class TimePointDlg extends WebDialog implements ActionListener {
	private static final long serialVersionUID = 1L;
	
	private static final String OK_CMD = "1";
	private static final String CANCEL_CMD = "2";	
	
	private Timestamp result = null; 
	
	private WebCalendar cal = null;
	private WebSpinner spin = null;
		
	public TimePointDlg( Window owner, String title ) {		
		super(owner, title);
		initUI();		
	}
	
	public static Timestamp showTimePointDlgDlg( Window owner, String title ) {
		TimePointDlg dlg = new TimePointDlg(owner, title);
		dlg.setDefaultCloseOperation( DISPOSE_ON_CLOSE );
		dlg.setModal(true);
		dlg.pack();
		dlg.setResizable( false );
		dlg.setVisible(true);
		return dlg.getResult();		
	}	

	private void initUI() {		
		Date now = new Date();		
		cal = new WebCalendar( now );

        SpinnerDateModel model = new SpinnerDateModel ();
        model.setCalendarField ( Calendar.MINUTE );

        spin = new WebSpinner ();
        spin.setModel ( model );
        spin.setValue ( now );	
        spin.setEditor(new JSpinner.DateEditor(spin, "HH:mm:ss"));

		WebButton okBtn = new WebButton( "OK" );
		okBtn.setActionCommand( OK_CMD );		
		okBtn.addActionListener( this );		
		
		WebButton cancelBtn = new WebButton( "Отмена" );
		cancelBtn.setActionCommand( CANCEL_CMD );
		cancelBtn.addActionListener( this );
		
		setLayout( new MigLayout( "", "[]", "[][][]") );
		add( cal, "wrap" );
		add( spin, "grow, wrap" );
		add( okBtn, "align right" );
		add( cancelBtn, "cell 0 2, align right" );		
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if ( ( e.getActionCommand() == OK_CMD ) && ( cal.getDate() != null ) ){
			Calendar res = Calendar.getInstance();
			res.setTime( cal.getDate() );
			Calendar tmp = Calendar.getInstance();
			tmp.setTime( ( Date ) spin.getValue() );
			res.set( Calendar.HOUR_OF_DAY, tmp.get( Calendar.HOUR_OF_DAY ) );
			res.set( Calendar.MINUTE, tmp.get( Calendar.MINUTE ) );
			res.set( Calendar.SECOND, tmp.get( Calendar.SECOND ) );
			res.set( Calendar.MILLISECOND, 0 );			
			Date now = new Date();
			if ( now.before( res.getTime() ) ) {
				WebOptionPane.showMessageDialog( this, "Нельзя указывать время в будущем", "Ошибка", WebOptionPane.ERROR_MESSAGE);
				return;
			}				
			result = new Timestamp( res.getTimeInMillis() );					
		}
		setVisible( false );		
	}
	
	private Timestamp getResult() {		
		return result;
	}
}
