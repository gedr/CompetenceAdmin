package minos.ui.dialogs;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.text.JTextComponent;


import ru.gedr.util.tuple.Pair;
import net.miginfocom.swing.MigLayout;

import com.alee.laf.button.WebButton;
import com.alee.laf.label.WebLabel;
import com.alee.laf.text.WebTextArea;

import minos.data.orm.OrmHelper;
import minos.data.orm.OrmHelper.QueryType;
import minos.entities.EstablishedPost;
import minos.entities.Person;
import minos.entities.PersonPostRelation;
import minos.ui.ComponentFabrica;
import minos.ui.adapters.ActionAdapter;
import minos.ui.models.dataproviders.EPostDataProvider.EPostGroup;
import minos.utils.ResourceKeeper;
import minos.utils.ResourceKeeper.IType;
import minos.utils.ResourceKeeper.OType;
import minos.utils.ResourceKeeper.QType;

public class EPostDlg extends BasisDlg<EstablishedPost> {
	// =================================================================================================================
	// Constants
	// =================================================================================================================
	private static final long serialVersionUID = 1L;
	private static final int ICON_SIZE = 48;
	
	// =================================================================================================================
	// Fields
	// =================================================================================================================
	private EPostGroup epg;
	private JTextComponent txt;
	
	// =================================================================================================================
	// Constructors
	// =================================================================================================================
	public EPostDlg( Window owner, String title, EstablishedPost source, EPostGroup epostGr ) {
		super( owner, title );
		this.epg = epostGr;
		if ( ( source == null ) && ( ( epg == null ) || ( epg.getEstablishedPosts() == null ) 
				|| ( epg.getEstablishedPosts().size() < 1 ) ) ) {
			throw new IllegalArgumentException( "EPostDlg() : illegal param" );
		}		
		if ( source == null ) source = epg.getEstablishedPosts().get( 0 );
		initUI();
	}

	// =================================================================================================================
	// Methods for/from SuperClass/Interfaces
	// =================================================================================================================
	@Override
	protected void initUI() {
		ImageIcon icon = ( readOnly ? ResourceKeeper.getIcon( IType.WORKER0, ICON_SIZE ) 
				: ( source == null ? ResourceKeeper.getIcon( IType.WORKER0, ICON_SIZE ) 
						: ResourceKeeper.getIcon( IType.WORKER0, ICON_SIZE ) ) ); 
		txt = ComponentFabrica.createOneLineTextEditor( 30, readOnly, "", source.getName() );

		setLayout( new MigLayout( "", "[][grow]", "[][][][][]" ) ); 

		add( new WebLabel( icon ), "cell 0 0 1 3,gapx 0 10,gapy 0 10" );
		add( new WebLabel( "Название" ), "cell 1 0,growx,aligny top" );
		add( txt, "cell 1 1,growx,aligny top" );
		add( new WebButton( ActionAdapter.build( "Отмена", ResourceKeeper.getIcon( IType.CANCEL, 24 ), CMD_CANCEL, 
				"Выйти без сохранения", this, KeyEvent.VK_ESCAPE ) ), "cell 1 2,alignx right" );
		add( ComponentFabrica.createCollapsingPane( null, "Детали", makeTechInfo(), new Dimension( 0, 100 ) ), 
				"cell 0 3, span, growx, wrap" );			
	}

	@Override
	protected void save() {
		// TODO Auto-generated method stub
		
	}
	
	// =================================================================================================================
	// Methods
	// =================================================================================================================
	public static EstablishedPost show( Window owner, String title, EstablishedPost source, EPostGroup epostGr ) {
		EPostDlg dlg = new EPostDlg( owner, title, source, epostGr );
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
		List<EstablishedPost> lep = ( epg != null ? epg.getEstablishedPosts() :  Arrays.asList( source ) );

		List<Person> lp = OrmHelper.findByQueryWithParam( QueryType.JPQL, 
				ResourceKeeper.getQuery( QType.JPQL_LOAD_PERSON_BY_EPOST), Person.class, 
				new Pair<Object, Object>( "epost", lep ), 
				new Pair<Object, Object>( "types", Arrays.asList( PersonPostRelation.TYPE_TRANSIENT )  ), 
				new Pair<Object, Object>( "state", Arrays.asList( PersonPostRelation.STATE_ACTIVE )  ),
				new Pair<Object, Object>( "ts", new Timestamp( System.currentTimeMillis() ) ),
				new Pair<Object, Object>( "dts", ResourceKeeper.getObject( OType.WAR ) ) );

		WebTextArea ta = new WebTextArea ();
		ta.setEditable( false );
		ta.append( "Код: " + source.getId() );
		ta.append( "\nВремя начала: " + source.getBeginDate() );
		ta.append( "\nВремя завершения: " + source.getEndDate() );
		ta.append( "\nОдобрен ОТИЗом: " + ( source.getOtizOK() > 0 ? "да" : "нет" ) );
		ta.append( "\nЯвляется удаленым: " + ( source.getIsdelete() > 0 ? "да" : "нет" ) );
		ta.append( "\nKPERS : " + source.getKpers() );
		ta.append( "\nFaset 2 : " + source.getFaset2() );
		ta.append( "\nFaset 3 : " + source.getFaset3() );
		ta.append( "\nFaset 7 : " + source.getFaset7() );
		ta.append( "\nFaset 11 : " + source.getFaset11() );
		ta.append( "\nFaset 12 : " + source.getFaset12() );
		ta.append( "\nFaset 99 : " + source.getFaset99() );
		if ( lp != null ) {
			for ( Person p : lp ) ta.append( "\nФИО : " + p.getSurnameAndInitials( true, true ) );
		}
		return ta;
	}
}
