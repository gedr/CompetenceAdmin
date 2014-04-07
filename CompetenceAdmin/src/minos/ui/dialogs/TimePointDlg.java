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
import com.alee.laf.rootpane.WebDialog;
import com.alee.laf.spinner.WebSpinner;

public class TimePointDlg extends WebDialog implements ActionListener {
	private static final long serialVersionUID = 1L;

	private static final String OK_CMD = "1";
	private static final String CANCEL_CMD = "2";
	
	
	
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
		dlg.setVisible(true);
		//return dlg.getResult();		
		return null;
	}	

	private void initUI() {
		setLayout( new MigLayout( "", "[grow]", "[][][]") );
		Date now = new Date();		
		cal = new WebCalendar( now );

        SpinnerDateModel model = new SpinnerDateModel ();
        model.setCalendarField ( Calendar.MINUTE );

        spin = new WebSpinner ();
        spin.setModel ( model );
        spin.setValue ( now );	
        spin.setEditor(new JSpinner.DateEditor(spin, "HH:mm:ss"));
		
		add( cal, "wrap" );
		add( spin, "grow, wrap" );
		
		
		WebButton okBtn = new WebButton("OK");
		okBtn.setActionCommand(OK_CMD);		
		okBtn.addActionListener(this);
		add(okBtn, "alignx right");
		
		WebButton cancelBtn = new WebButton("Отмена");
		cancelBtn.setActionCommand(CANCEL_CMD);
		cancelBtn.addActionListener(this);		
		add(cancelBtn, "alignx right");		
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
			
			System.out.println( new Timestamp( res.getTimeInMillis() ) );
					
		}

		
	}
}
