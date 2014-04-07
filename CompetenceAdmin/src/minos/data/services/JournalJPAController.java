package minos.data.services;

import java.sql.Timestamp;
import java.util.Calendar;

import minos.entities.Journal;
import minos.entities.Person;
import minos.resource.managers.Resources;
import minos.resource.managers.ResourcesConst;

public class JournalJPAController {
	public static final Timestamp DOOMSDAY;
	static { 
		Calendar calendar = Calendar.getInstance();
		calendar.set(9999, 11, 30);
		DOOMSDAY = new Timestamp( calendar.getTimeInMillis() );
	}

	public static Journal create( boolean flagSaveEntity ) {
		Timestamp now = new Timestamp( System.currentTimeMillis() );
		Person person = ( Person ) Resources.getInstance().get( ResourcesConst.CURRENT_PERSON );
		String host = ( String ) Resources.getInstance().get( ResourcesConst.CURRENT_HOST );
		String login = ( String ) Resources.getInstance().get( ResourcesConst.CURRENT_LOGIN );
		Journal j = new Journal( now, person.getId(), host, login, now, person.getId(), null, null, DOOMSDAY, person.getId(), null, null);
		if ( flagSaveEntity ) j = (Journal) ORMHelper.createEntity( j );			
		return j;
	}
	
	public static Journal edit( Journal journal, boolean flagSaveEntity ) {
		if ( journal == null ) return null;
		journal.setEditMoment( new Timestamp( System.currentTimeMillis() ) );
		journal.setEditorID( ( ( Person ) Resources.getInstance().get( ResourcesConst.CURRENT_PERSON ) ).getId() );
		journal.setEditorHost( ( String ) Resources.getInstance().get( ResourcesConst.CURRENT_HOST ) );
		journal.setEditorLogin( ( String ) Resources.getInstance().get( ResourcesConst.CURRENT_LOGIN ) );	
		if ( flagSaveEntity ) journal = (Journal) ORMHelper.updateEntity( journal );
		return journal;
	}
	
	public static Journal delete( Journal journal, boolean flagSaveEntity ) {
		if ( journal == null ) return null;
		journal.setDeleteMoment( new Timestamp( System.currentTimeMillis() ) );
		journal.setDeleterID( ( ( Person ) Resources.getInstance().get( ResourcesConst.CURRENT_PERSON ) ).getId() );
		journal.setDeleterHost( ( String ) Resources.getInstance().get( ResourcesConst.CURRENT_HOST ) );
		journal.setDeleterLogin( ( String ) Resources.getInstance().get( ResourcesConst.CURRENT_LOGIN ) );
		if ( flagSaveEntity ) journal = (Journal) ORMHelper.updateEntity( journal );
		return journal;
	}
	
	public static Journal copy( Journal journal, boolean flagSaveEntity ) { 
		if ( journal == null ) return null;		
		Journal j = new Journal( journal.getCreateMoment(), journal.getCreatorID(), journal.getCreatorHost(), journal.getCreatorLogin(), 
				journal.getEditMoment(), journal.getEditorID(), journal.getEditorHost(), journal.getEditorLogin(), 
				journal.getDeleteMoment(), journal.getDeleterID(), journal.getDeleterHost(), journal.getDeleterLogin() );
		if ( flagSaveEntity ) j = (Journal) ORMHelper.createEntity( j );
		 return j;
	}
}