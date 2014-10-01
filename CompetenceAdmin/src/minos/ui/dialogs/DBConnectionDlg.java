package minos.ui.dialogs;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import net.miginfocom.swing.MigLayout;

import com.alee.laf.button.WebButton;
import com.alee.laf.checkbox.WebCheckBox;
import com.alee.laf.label.WebLabel;
import com.alee.laf.rootpane.WebDialog;
import com.alee.laf.spinner.WebSpinner;
import com.alee.laf.text.WebPasswordField;
import com.alee.laf.text.WebTextField;

import minos.utils.DBConnectionConfig;

public class DBConnectionDlg extends WebDialog implements ActionListener {
	private static final long serialVersionUID = 1L;
	
	private static final String OK_CMD		= "1"; 
	private static final String CANCEL_CMD	= "2";

	private WebTextField[] txt = null;
	private WebPasswordField psw = null;
	private WebSpinner port = null;
	private WebCheckBox wa = null;
	private DBConnectionConfig config = null;
	private DBConnectionConfig res = null;

	public DBConnectionDlg( Window owner, DBConnectionConfig config ) {
		super(owner, "Подключение к БД");
		this.config = config;
		initUI();
	}	
	
	public static DBConnectionConfig show( Window owner, DBConnectionConfig config ) {
		DBConnectionDlg dlg = new DBConnectionDlg(owner, config );
		dlg.setDefaultCloseOperation( DISPOSE_ON_CLOSE );
		dlg.setModal(true);
		dlg.pack();
		dlg.setVisible(true);
		return dlg.getResult();		
	}	

	private void initUI() {		
		txt = new WebTextField [4];
		for( int i = 0; i < 4; i++ )  txt[i] = new WebTextField( 30 );
		psw = new WebPasswordField();

		port = new WebSpinner();
		port.setValue( 1433 );		
		wa = new WebCheckBox( "Проверка подлиности Windows" );
		wa.addActionListener( this );

		if(config != null) {
			txt[0].setText( config.getServerAddress() );
			txt[1].setText( config.getDbName() );
			txt[2].setText( config.getDbInstance() );
			txt[3].setText( config.getLogin() );
			psw.setText( config.getPassword() );
			port.setValue( config.getDbPort() );
			wa.setSelected( config.isIntegratedSecurity() );
		} 
		txt[3].setEnabled( !wa.isSelected() );
		psw.setEnabled( !wa.isSelected() );

		WebButton okButton = new WebButton( "OK", this );
		okButton.setActionCommand( OK_CMD );
		WebButton cancelButton = new WebButton( "Отмена", this );
		cancelButton.setActionCommand( CANCEL_CMD );

		setLayout( new MigLayout( "", "[][fill, grow]", "[][][][][][][][]" ) );		
		add( new WebLabel( "Сервер" ) );
		add( txt[0], "wrap" );
		add( new WebLabel( "Название БД" ) );
		add( txt[1], "wrap" );
		add( new WebLabel( "Экземпляр БД" ) );
		add( txt[2], "wrap" );
		add( new WebLabel( "Порт подключения" ) );
		add( port, "wrap" );
		add( wa, "span, wrap" );
		add( new WebLabel( "Логин" ) );
		add( txt[3], "wrap" );
		add( new WebLabel( "Пароль" ) );
		add( psw, "wrap" );
		add(okButton, "flowx,cell 1 8,alignx right");
		add(cancelButton, "cell 1 8,alignx right");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == wa) {
			txt[3].setEnabled( !wa.isSelected() );
			psw.setEnabled( !wa.isSelected() );
			return;
		}
		
		if( e.getActionCommand() == OK_CMD ) {
			res = new DBConnectionConfig( txt[0].getText(), txt[1].getText(), txt[2].getText(), 
					( Integer ) port.getValue(), wa.isSelected(), 
					( wa.isSelected() ? null : txt[3].getText() ), 
					( wa.isSelected() ? null : String.valueOf( psw.getPassword() ) ) );				
		}
		setVisible(false);
	}
	
	public DBConnectionConfig getResult() {
		return res;
	}
}