package minos.ui.dialogs;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.text.JTextComponent;

import ru.gedr.util.tuple.Unit;
import net.miginfocom.swing.MigLayout;

import com.alee.laf.button.WebButton;
import com.alee.laf.label.WebLabel;
import com.alee.laf.text.WebTextArea;

import minos.entities.Actors;
import minos.entities.ActorsInfo;
import minos.entities.Person;
import minos.entities.Profile;
import minos.ui.ComponentFabrica;
import minos.ui.adapters.ActionAdapter;
import minos.ui.panels.PersonPanel;
import minos.ui.panels.PostProfilePanel;
import minos.utils.AuxFunctions;
import minos.utils.ResourceKeeper;
import minos.utils.ResourceKeeper.IType;

public class ActorsDlg extends BasisDlg<List<Actors>> {
	// =================================================================================================================
	// Constants
	// =================================================================================================================
	private static final long serialVersionUID = 1L;
	private static final int ICON_SIZE = 48;
	private static final String CMD_SELECT_MINOS 	= "A";
	private static final String CMD_CLEAR_MINOS 	= "B";
	private static final String CMD_SELECT_SINNER	= "C";
	private static final String CMD_CLEAR_SINNER 	= "D";
	private static final String CMD_SELECT_PROFILE	= "E";
	private static final String CMD_CLEAR_PROFILE 	= "F";

	// =================================================================================================================
	// Fields
	// =================================================================================================================
	private Actors actors;
	private JTextComponent minosField;
	private JTextComponent sinnerField;
	private JTextComponent profileField;
	private JComboBox<ActorsInfo> lvl;
	private JComboBox<ActorsInfo> mode;
	private JComboBox<ActorsInfo> type;
	
	private PersonPanel personPanel;
	private Unit<PersonPanel> upp;
	private PostProfilePanel profilePanel;

	// =================================================================================================================
	// Constructors
	// =================================================================================================================
	public ActorsDlg( Window owner, String title, List<Actors> source, boolean readOnly, Unit<PersonPanel> upp ) {
		super( owner, title );
		if ( ( source == null ) || ( source.size() == 0 ) ) throw new NullPointerException( "ActorsDlg() :"
				+ " param source is null" );
		this.source = source;
		this.readOnly = readOnly;
		this.upp = upp;
		this.personPanel = ( upp == null ? null : upp.getFirst() );
		initActors();
		initUI();
	}

	// =================================================================================================================
	// Methods for/from SuperClass/Interfaces
	// =================================================================================================================
	@Override
	protected void initUI() {
		ImageIcon icon = ResourceKeeper.getIcon( readOnly ? IType.USERS :  IType.USERS_EDIT, ICON_SIZE );

		minosField = ComponentFabrica.createOneLineTextEditor( 30, true, "", 
				( ( ( actors == null ) || ( actors.getMinos() == null ) ) ? "" : actors.getMinos().getFullName() ) );
		profileField = ComponentFabrica.createOneLineTextEditor( 30, true, "", 
				( ( ( actors == null ) || ( actors.getProfile() == null ) 
						|| ( actors.getProfile().getProfilePattern() == null ) ) ? "" 
								: actors.getProfile().getProfilePattern().getName() ) );

		sinnerField = ComponentFabrica.createOneLineTextEditor( 30, true, "", "" );
		if ( ( actors != null ) && ( ( actors.getInternalSinner() != null ) || ( actors.getAlienSinner() != null ) ) ) {
			sinnerField.setText( actors.getAlienSinner() != null ? actors.getAlienSinner().getName() 
					: actors.getInternalSinner().getFullName() );
		}
		lvl = ComponentFabrica.createActorsInfoComboBox( false, true, ActorsInfo.VARIETY_SPEC, ActorsInfo.VARIETY_LEVEL );
		lvl.setSelectedItem( actors.getReserveLevel() );
		mode = ComponentFabrica.createActorsInfoComboBox( false, true, ActorsInfo.VARIETY_SPEC, ActorsInfo.VARIETY_MODE );
		mode.setSelectedItem( actors.getTestMode() );
		type = ComponentFabrica.createActorsInfoComboBox( false, true, ActorsInfo.VARIETY_SPEC, ActorsInfo.VARIETY_TYPE );
		type.setSelectedItem( actors.getReserveType() );

		setLayout( new MigLayout( "", "[][grow][][]", "[][][][][][][][][][]" ) ); 
		add( new WebLabel( icon ), "cell 0 0 1 3,gapx 0 10,gapy 0 10" );
		
		add( new WebLabel( "Эксперт" ), "cell 1 0 3 1,growx" );
		add( minosField, "cell 1 1,growx" );
		add( new WebButton( ActionAdapter.build( "...", null, CMD_SELECT_MINOS, "Выбор эксперта из справочника", 
				this, 0 ) ), "cell 2 1"  );
		add( new WebButton( ActionAdapter.build( "x", null, CMD_CLEAR_MINOS, "Очистить эксперта", 
				this, 0 ) ), "cell 3 1"  );
		
		add( new WebLabel( "Оцениваемый" ), "cell 1 2 3 1,growx" );
		add( sinnerField, "cell 1 3,growx" );
		add( new WebButton( ActionAdapter.build( "...", null, CMD_SELECT_SINNER, "Выбор эксперта из справочника", 
				this, 0 ) ), "cell 2 3"  );
		add( new WebButton( ActionAdapter.build( "x", null, CMD_CLEAR_SINNER, "Очистить эксперта", 
				this, 0 ) ), "cell 3 3"  );
		
		add( new WebLabel( "Уровень" ), "cell 0 4 4 1,growx" );
		add( lvl, "cell 0 5 4 1,growx" );
		
		add( new WebLabel( "Резерв" ), "cell 0 6 4 1,growx" );
		add( type, "cell 0 7 4 1,growx" );

		add( new WebLabel( "Вид" ), "cell 0 8 4 1,growx" );
		add( mode, "cell 0 9 4 1,growx" );

		add( new WebLabel( "Профиль" ), "cell 1 2 3 1,growx" );
		add( profileField, "cell 0 10 2 1,growx" );
		add( new WebButton( ActionAdapter.build( "...", null, CMD_SELECT_PROFILE, "Выбор профиля из справочника", 
				this, 0 ) ), "cell 2 10"  );
		add( new WebButton( ActionAdapter.build( "x", null, CMD_CLEAR_PROFILE, "Очистить профиль", 
				this, 0 ) ), "cell 3 10"  );

		add( ComponentFabrica.createCollapsingPane( null, "Детали", makeTechInfo(), new Dimension( 0, 100 ) ), 
				"cell 0 11 4 1, growx" );	
		
		if ( !readOnly ) add( new WebButton( ActionAdapter.build( "OK", ResourceKeeper.getIcon( IType.OK, 24 ), CMD_OK, 
				"Сохранить", this, KeyEvent.VK_ENTER ) ), "cell 1 12,alignx right" );
		add( new WebButton( ActionAdapter.build( "Отмена", ResourceKeeper.getIcon( IType.CANCEL, 24 ), CMD_CANCEL, 
				"Выйти без сохранения", this, KeyEvent.VK_ESCAPE ) ), "cell 1 12, alignx right" );
	}
	
	@Override
	public void actionPerformed( ActionEvent e ) {
		switch( e.getActionCommand() ) {

		case CMD_CLEAR_MINOS :
			clearHuman( true );
			break;

		case CMD_CLEAR_SINNER :
			clearHuman( false );
			break;

		case CMD_CLEAR_PROFILE :
			clearProfile();
			break;

		case CMD_SELECT_PROFILE : 
			setProfile();
			break;

		case CMD_SELECT_MINOS : 
			setHuman( true );
			break;

		case CMD_SELECT_SINNER : 
			setHuman( false );
			break;
			
		case CMD_CANCEL : 
			if ( upp != null ) upp.setFirst( personPanel );
			super.actionPerformed( e );
			break;

		default :
			super.actionPerformed( e );
			break;
		}
	}

	@Override
	protected void save() {
		if ( upp != null ) upp.setFirst( personPanel );
		if ( readOnly ) return;
		actors.setReserveLevel( ( ActorsInfo ) lvl.getSelectedItem() );
		actors.setReserveType( ( ActorsInfo ) type.getSelectedItem() ); 
		actors.setTestMode( ( ActorsInfo ) mode.getSelectedItem() );
		result = Arrays.asList( actors );	
	}	

	// =================================================================================================================
	// Methods
	// =================================================================================================================
	public static List<Actors> show( Window owner, String title, List<Actors> source, boolean readOnly,
			Unit<PersonPanel> upp ) {
		ActorsDlg dlg = new ActorsDlg( owner, title, source, readOnly, upp );
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
		for ( Actors a : source ) {
			ta.append( "\nКод: " + a.getId() );
			ta.append( "\nСтатус: " + a.getStatus() );
			if ( a.getJournal() != null ) ta.append( "\nСоздан: " + a.getJournal().getCreateMoment() );
			ta.append( "\n=====================================" );
		}
		return ta;
	}
	
	private void initActors() {
		int num = 0;
		Actors a0 = source.get( 0 );
		if ( a0.getSinnerType() == Actors.SINNER_TYPE_ALIEN ) {
			actors = new Actors( a0.getMinos(), a0.getAlienSinner(), a0.getAlienSinnerVersion(), a0.getGauge(), 
					a0.getFinish(), a0.getStatus(), a0.getMeasure(), a0.getProfile(), a0.getJournal(), a0.getTestMode(), 
					a0.getReserveLevel(), a0.getReserveType(), null );
		} else {
			actors = new Actors( a0.getMinos(), a0.getInternalSinner(), a0.getGauge(), a0.getFinish(), a0.getStatus(), 
					a0.getMeasure(), a0.getProfile(), a0.getJournal(), a0.getTestMode(), a0.getReserveLevel(), 
					a0.getReserveType(), null );
		}
		ActorsInfo nullai = new ActorsInfo( ComponentFabrica.NULL_ACTORS_INFO_NAME, ActorsInfo.VARIETY_SPEC, null );
		nullai.setId( ComponentFabrica.NULL_ACTORS_INFO_ID );
		for ( Actors a : source ) {
			if ( a.getStatus() != Actors.STATUS_BUILDING ) num++;
			if ( !AuxFunctions.equals( actors.getMinos(), a.getMinos(), true )  ) actors.setMinos( null );
			if ( ( actors.getSinnerType() != a.getSinnerType() ) 
					|| !AuxFunctions.equals( actors.getInternalSinner(), a.getInternalSinner(), true )  
					|| !AuxFunctions.equals( actors.getAlienSinner(), a.getAlienSinner(), true )
					|| (  actors.getAlienSinnerVersion() != a.getAlienSinnerVersion() ) ) {
				actors.setInternalSinner( null );
				actors.setAlienSinner( null );
				actors.setAlienSinnerVersion( ( short ) 0 );
			}
			if ( !AuxFunctions.equals( actors.getTestMode(), a.getTestMode(), true ) ) {
				actors.setTestMode( nullai );
			}
			if ( !AuxFunctions.equals( actors.getReserveLevel(), a.getReserveLevel(), true ) ) {
				actors.setReserveLevel( nullai );
			}
			if ( !AuxFunctions.equals( actors.getReserveType(), a.getReserveType(), true )  ) {
				actors.setReserveType( nullai );
			}	
		}
		this.readOnly = ( num == source.size() ); 
	}
	
	private void clearHuman( boolean minos ) {
		if ( minos ) {
			actors.setMinos( null );
			minosField.setText( "" );
		} else {
			actors.setSinnerType( Actors.SINNER_TYPE_UNKNOWN );
			actors.setInternalSinner( null );
			actors.setAlienSinner( null );
			actors.setAlienSinnerVersion( ( short ) 0 );
			sinnerField.setText( "" );
		}
	}
	
	private void setHuman( boolean minos ) {
		if ( personPanel == null ) {
			personPanel = new PersonPanel( this, true );
			personPanel.setPreferredSize( new Dimension( 500, 300 ) );
		}
		if ( JOptionPane.OK_OPTION == ComponentDlg.show( this, "Выбор " + ( minos ? "эксперта" : "оцениваемого" ), 
				personPanel, null ) ) {
			Person p = personPanel.getSeleсtedPerson();
			if ( p == null ) return;
			if ( minos ) {
				actors.setMinos( p );
				minosField.setText( p.getFullName() );
			} else {
				actors.setSinnerType( Actors.SINNER_TYPE_INNER );
				actors.setInternalSinner( p );
				actors.setAlienSinner( null );
				actors.setAlienSinnerVersion( ( short ) 0 );
				sinnerField.setText( p.getFullName() );
			}
		}
	}
	
	private void clearProfile() {
		actors.setProfile( null );
		profileField.setText( "" );
	}

	private void setProfile() {
		if ( profilePanel == null ) {
			profilePanel = new PostProfilePanel( this, true );
			profilePanel.setPreferredSize( new Dimension( 400, 300 ) );
		}
		if ( JOptionPane.OK_OPTION == ComponentDlg.show( this, "Выбор профиля", profilePanel, null ) ) {
			Profile p = profilePanel.getSelectedProfile();
			if ( p == null ) return;
			actors.setProfile( p );
			profileField.setText( p.getProfilePattern() == null ? "???" : p.getProfilePattern().getName() );
		}
	}
}
