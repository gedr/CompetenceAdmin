package minos;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityNotFoundException;
import javax.persistence.Persistence;
import javax.sql.DataSource;

import minos.data.orm.OrmHelper;
import minos.data.orm.OrmHelper.QueryType;
import minos.data.services.FilialInfo;
import minos.data.services.TablesInfo;
import minos.entities.Level;
import minos.entities.Person;
import minos.entities.Role;
import minos.ui.MainWnd;
import minos.ui.dialogs.DBConnectionDlg;
import minos.ui.dialogs.ErrorDlg;
import minos.utils.DBConnectionConfig;
import minos.utils.Permission;
import minos.utils.ProgramConfig;
import minos.utils.ResourceKeeper;
import minos.utils.ResourceKeeper.IType;
import minos.utils.ResourceKeeper.OType;

import org.apache.commons.lang.NullArgumentException;
import org.apache.log4j.xml.DOMConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.gedr.util.tuple.Pair;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import com.thoughtworks.xstream.InitializationException;

public class Start {
	// =================================================================================================================
	// Constants
	// =================================================================================================================
	private static final Logger log = LoggerFactory.getLogger( Start.class );	
	
	// =================================================================================================================
	// Fields
	// =================================================================================================================
	private Thread thread;
	private boolean wakeupFlag;
	
	// =================================================================================================================
	// Constructors
	// =================================================================================================================
	private Start() { 
		thread = null;
		wakeupFlag = false;
	}

	// =================================================================================================================
	// Methods
	// =================================================================================================================
	public static void main(String[] args) { 		
		String cfgPath = ( ( ( args != null ) && ( args.length > 0 ) ) ? args[0] : "program.cfg" );		
		if ( !getInstance().init( cfgPath ) ) System.exit( -1 );
		getInstance().thread = Thread.currentThread();
		new MainWnd();
		
		while ( !getInstance().wakeupFlag ) {			
			try {			
				Thread.sleep(Long.MAX_VALUE);				
			} catch (InterruptedException e) { 
				if( log.isDebugEnabled() ) log.debug( "Statrt.main() :  wakeup" );
			}			
		}
		if ( OrmHelper.getFactory() != null ) OrmHelper.getFactory().close();
		( ( ProgramConfig ) ResourceKeeper.getObject( OType.PROGRAM_CONFIG ) ).saveConfig( cfgPath );
		if( log.isDebugEnabled() ) log.debug( "Statrt.main() :  bye" );
		System.exit(0);	
	}

	/**
	 * singleton
	 * @return instance of Start object
	 */
	public static Start getInstance() {
		return Holder.KEEPER;
	}

	/**
	 * wake up main thread for ending program
	 */
	public void wakeup() {
		wakeupFlag = true;
		thread.interrupt();
	}
	
	/**
	 * load and initialize all data before run
	 * @param cfgPath - configuration file path
	 * @return true if initialize finish successfully
	 */
	private boolean init( String cfgPath ) {
		try {
			loadConfig( cfgPath );
			initDb( ( ProgramConfig ) ResourceKeeper.getObject( OType.PROGRAM_CONFIG ) );
			initUser();
			initLevelsCache();
			initRoleAndPermission();
			loadCommonCfg();
			initTimePoint();
			return true;
		} catch ( InitializationException iex ) {
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( "db init failed", iex );
		} catch ( Exception ex ) {
			ErrorDlg.show( null, "Error", "The application has failed to initialize properly", ex, 
					ResourceKeeper.getIcon( IType.ERROR, 48 ) );
		}
		return false;
	}

	/**
	 * make key time points
	 */
	private void initTimePoint() {
		Calendar calendar = Calendar.getInstance();
		calendar.set( 1812, Calendar.SEPTEMBER, 1 );
		ResourceKeeper.putObject( OType.WAR, new Timestamp( calendar.getTimeInMillis() ) );
		calendar.set( 7777, Calendar.JULY, 7, 7, 7, 7 );
		ResourceKeeper.putObject( OType.DAMNED_FUTURE, new Timestamp( calendar.getTimeInMillis() ) );
		calendar.set( 9999, Calendar.DECEMBER, 31 );
		ResourceKeeper.putObject( OType.DOOMSDAY, new Timestamp( calendar.getTimeInMillis() ) );
	}	

	/**
	 * load configuration file
	 * @param cfgPath - file path
	 * @throws Exception
	 */
	private void loadConfig( String cfgPath ) throws Exception {
		try {
			if ( cfgPath == null ) throw new NullArgumentException( "Start.loadConfig() : cfgPath is null" );
			ProgramConfig cfg = ProgramConfig.loadConfig( cfgPath );
			DOMConfigurator.configure( cfg.getLogConfigFile() );
			ResourceKeeper.putObject( ResourceKeeper.OType.PROGRAM_CONFIG, cfg );
		} catch ( Exception ex ) {
			String errmsg = "Start.loadConfig() : read ProgramConfig failed";
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( errmsg );
			throw new IllegalStateException( errmsg, ex );
		}
	}

	/**
	 * initializing DB connection (show dialog, make DataSource and pass to openJPA)
	 * @return true if success
	 */
	private void initDb( ProgramConfig cfg ) {
		DBConnectionConfig dbc = ( cfg == null ? null : cfg.getDBConnectionConfig() );
		if ( ( cfg == null ) || ( cfg.getDBConnectionConfig() == null ) || cfg.isShowDBConnectionConfig() ) {
			dbc = DBConnectionDlg.show( null, dbc );
			if ( dbc == null ) {
				String errmsg = "Start.initDb() : DBConnectionDlg return null";
				if ( ( log != null ) && log.isErrorEnabled() ) log.error( errmsg );
				throw new InitializationException( errmsg );
			}
		}
		cfg.setDBConnectionConfig( dbc );
		
		DataSource ds = fillDataSource( dbc );
		if ( ds == null ) {
			String errmsg = "Start.initDb() : DataSource is null";
			if ( ( log != null ) && log.isInfoEnabled() ) log.info( errmsg );
			throw new IllegalStateException( errmsg );
		}

		// create EntityManagerFactory object 
		Properties prop = new Properties();
		prop.put( "openjpa.ConnectionFactory", ds ); // DataSource pass to openJPA
		EntityManagerFactory factory = Persistence.createEntityManagerFactory( "CompetenceEntitiesPU", prop );
		if ( factory == null ) {
			String errmsg = "Start.initDb() : EntityManagerFactory create failed";
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( errmsg );
			throw new IllegalStateException( errmsg );
		}
		OrmHelper.setFactory( factory );
	}

	/**
	 * 
	 * fill DataSource structure for connection with DB 
	 * @param dbcfg - parameters for DataSource
	 * @return complete DataSource's object or null 
	 */
	private DataSource fillDataSource( DBConnectionConfig dbcfg ) { 
		if ( dbcfg == null ) return null;
		
		SQLServerDataSource msds = new SQLServerDataSource(); // driver depended
		msds.setServerName( dbcfg.getServerAddress() );
		msds.setDatabaseName( dbcfg.getDbName() ); 
		msds.setPortNumber( dbcfg.getDbPort() );
		if ( dbcfg.getDbInstance() != null ) msds.setInstanceName( dbcfg.getDbInstance() );
		msds.setIntegratedSecurity( dbcfg.isIntegratedSecurity() );
		if ( !dbcfg.isIntegratedSecurity() ) {
			msds.setUser( dbcfg.getLogin() );
			msds.setPassword( dbcfg.getPassword() );				
		}
		// see http://openjpa.apache.org/builds/1.2.3/apache-openjpa/docs/dbsupport_sqlserver.html			
		msds.setSelectMethod("cursor");
		msds.setSendStringParametersAsUnicode(false);
		return msds;
	}

	/**
	 * caching Level table
	 * @return true if success
	 */
	private void initLevelsCache() throws Exception {
		List<Level> lst = ( List<minos.entities.Level> ) OrmHelper.findByQuery( QueryType.NAMED, "Level.findAll", Level.class );
		if ( ( lst == null ) || ( lst.size() != Level.LEVEL_COUNT ) ) {
			String errmsg = "Start.initLevelsCache() : create cache of Level failed";
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( errmsg );
			throw new IllegalStateException( errmsg );
		}
		List<Level> lvls = new ArrayList<>( lst );
		ResourceKeeper.putObject( ResourceKeeper.OType.LEVELS_CACHE, lvls );
	}
	
	/**
	 * load common configuration data
	 * @return true if load success
	 */
	private void loadCommonCfg() throws Exception {
		String sql = "SELECT name, resourceKod, value FROM minos.ProgramCommonConfig";
		List<Object[]> lst = OrmHelper.findByQuery( QueryType.SQL, sql, Object[].class );
		if ( ( lst == null ) || ( lst.get( 0 ).length != 3 ) ) {
			String errmsg = "Start.initLevelsCache() : load ProgramCommonConfig failed";
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( errmsg );
			throw new IllegalStateException( errmsg );
		}
		Gson gson = new Gson();
		java.lang.reflect.Type t = null;
		
		for ( Object[] objs : lst ) {
			String name = ( String ) objs[0];
			int rkod = ( int ) objs[1];
			String val = ( String ) objs[2];

			if ( ( rkod == OType.DEFAULT_BRANCH_OFFICE_CODE.getIndex() ) && 
					( name.compareToIgnoreCase( OType.DEFAULT_BRANCH_OFFICE_CODE.getName() ) == 0 ) ) {
				Byte b = Byte.valueOf( val );				
				ResourceKeeper.putObject( OType.DEFAULT_BRANCH_OFFICE_CODE, b );
				continue;				
			}
			if ( ( rkod == OType.BASE_POST_LIST.getIndex() )
					&& ( name.compareToIgnoreCase( OType.BASE_POST_LIST.getName() ) == 0 ) ) {
				t = new TypeToken<List<Pair<String, Byte>>>(){}.getType();
				List<Pair<String, Byte>> lpsb = gson.fromJson( val, t );
				ResourceKeeper.putObject( OType.BASE_POST_LIST, lpsb );
				continue;
			}
			if ( ( rkod == OType.TABLES_INFO.getIndex() ) && 
					( name.compareToIgnoreCase( OType.TABLES_INFO.getName() ) == 0 ) ) {
				t = new TypeToken<TablesInfo>(){}.getType();
				TablesInfo ti = gson.fromJson( val, t );
				ResourceKeeper.putObject( OType.TABLES_INFO, ti );				
				continue;
			}
			if ( ( rkod == OType.BRANCH_OFFICES_INFO.getIndex() ) 
					&& ( name.compareToIgnoreCase( OType.BRANCH_OFFICES_INFO.getName() ) == 0 ) ) {				
				t = new TypeToken<List<FilialInfo>>(){}.getType();
				List<FilialInfo> lpsb = gson.fromJson( val , t);
				ResourceKeeper.putObject( OType.BRANCH_OFFICES_INFO, lpsb);
				continue;
			}
		}
		if ( ( ResourceKeeper.getObject( OType.DEFAULT_BRANCH_OFFICE_CODE ) == null )
				|| ( ResourceKeeper.getObject( OType.BASE_POST_LIST ) == null )
				|| ( ResourceKeeper.getObject( OType.TABLES_INFO ) == null )
				|| ( ResourceKeeper.getObject( OType.BRANCH_OFFICES_INFO ) == null ) ) {
			String errmsg = "Start.loadCommonCfg() : load data from ProgramCommonConfig failed";
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( errmsg );
			throw new IllegalStateException( errmsg );
		}
	}

	/**
	 * receive info about current user
	 * @return true if success
	 */
	private void initUser() throws Exception {
		List<Object[]> lst = OrmHelper.findByQuery( QueryType.SQL, "SELECT HOST_NAME(), SUSER_NAME()", Object[].class );
		if( ( lst == null ) || ( lst.size() != 1 ) || ( lst.get(0).length != 2 ) 
				|| ( lst.get(0)[0] == null ) || ( lst.get(0)[1] == null ) ) {
			String errmsg = "Start.initUser() : load HOST_NAME and|or SUSER_NAME failed";
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( errmsg );
			throw new IllegalStateException( errmsg );
		}
		String host = ( String ) lst.get(0)[0];
		String login = ( String ) lst.get(0)[1];

		String sql = "SELECT id, role_id FROM Minos.PersonAddon WHERE upper(logins) LIKE upper('%\"" + login + "\"%') ";

		lst = OrmHelper.findByQuery( QueryType.SQL, sql, Object[].class );
		if ( ( lst == null ) || ( lst.size() != 1 ) || ( lst.get(0).length != 2 ) 
				|| ( lst.get(0)[0] == null ) || ( lst.get(0)[1] == null ) ) {
			String errmsg = "Start.initUser() : load PersonAddon failed login=" + login + 
					"\nquery result = " + ( lst == null ? "null" : String.valueOf( lst.size() ) );
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( errmsg );
			throw new IllegalStateException( errmsg );
		}
		Person person = ( Person ) OrmHelper.findEntity( Person.class, ( Integer ) lst.get( 0 )[0] );
		Role role = (Role) OrmHelper.findEntity( Role.class, ( Integer ) lst.get( 0 )[1] );
		if ( ( person == null ) || ( role == null ) || ( role.getId() == 1 ) || ( role.getId() == 2 ) ) {
			String errmsg = "Start.initUser() : load Person and|or Role failed or Role have incorrect id";
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( errmsg );
			throw new IllegalStateException( errmsg );
		}
		ResourceKeeper.putObject( OType.CURRENT_HOST, host );
		ResourceKeeper.putObject( OType.CURRENT_LOGIN, login );
		ResourceKeeper.putObject( OType.CURRENT_PERSON, person );
		ResourceKeeper.putObject( OType.CURRENT_ROLE, role );			
		ResourceKeeper.putObject( OType.PERMISSION_CURRENT, new Permission( role ) );
	}

	/**
	 * load base roles (CONSTRAINT and DEFAULT) and make permissions
	 * @throws Exception
	 */
	private void initRoleAndPermission() throws Exception {
		Role rc = ( Role ) OrmHelper.findEntity( Role.class, 1 );
		Role rd = ( Role ) OrmHelper.findEntity( Role.class, 2 );
		if ( ( rc == null ) || !rc.getName().equals( "ROLE_CONSTRAINT" )
				|| ( rd == null ) || !rd.getName().equals( "ROLE_DEFAULT" ) ) {
			String errmsg = "Start.initRoleAndPermission() : base roles not found";
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( errmsg );
			throw new EntityNotFoundException( errmsg );
		}
		ResourceKeeper.putObject( OType.BRANCH_OFFICE_ROLE_MASK, new Role[] {rc, rd} );
		ResourceKeeper.putObject( OType.PERMISSION_CONSTRAINT, new Permission( rc ) );
		ResourceKeeper.putObject( OType.PERMISSION_DEFAULT, new Permission( rd ) );				
	}

	// =================================================================================================================
	// Inner and Anonymous Classes
	// =================================================================================================================
	private static class Holder {
		private static final Start KEEPER = new Start();
	}
}