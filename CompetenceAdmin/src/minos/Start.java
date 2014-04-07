package minos;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.sql.DataSource;
import javax.swing.JOptionPane;

import minos.data.services.ORMHelper;
import minos.data.services.ORMHelper.QueryType;
import minos.entities.Person;
import minos.entities.Role;
import minos.resource.managers.Resources;
import minos.resource.managers.ResourcesConst;
import minos.ui.frames.MainWnd;

import org.apache.log4j.xml.DOMConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.gedr.util.tuple.Pair;

import com.google.common.base.Joiner;
import com.microsoft.sqlserver.jdbc.SQLServerDataSource;

public class Start {
	private static Logger log = LoggerFactory.getLogger(Start.class);	
	private static Thread thread = null;
	private static boolean wakeupFlag = false;
	
	public static void wakeup() {
		wakeupFlag = true;
		thread.interrupt();
	}
	
	private static DataSource fillDataSource() {   //(DBConnectionConfig dbcfg) {
		// if(dbcfg == null) return null;
		
		SQLServerDataSource msds = new SQLServerDataSource();
		msds.setServerName("192.168.56.2"); // (dbcfg.getServerAddress());
		msds.setDatabaseName("gedr"); // (dbcfg.getDbName());
		msds.setPortNumber(1433); // (dbcfg.getDbPort());

		msds.setUser("sa");
		msds.setPassword("Q11W22e33");				
		
		/*
		if(dbcfg.getDbInstance() != null)
			msds.setInstanceName(dbcfg.getDbInstance());
		if(dbcfg.isIntegratedSecurity()) {
			msds.setIntegratedSecurity(true);
		} else {
			msds.setIntegratedSecurity(false);
			msds.setUser(dbcfg.getLogin());
			msds.setPassword(dbcfg.getPassword());				
		}
*/

		// see http://openjpa.apache.org/builds/1.2.3/apache-openjpa/docs/dbsupport_sqlserver.html			
		msds.setSelectMethod("cursor");
		msds.setSendStringParametersAsUnicode(false);
		return msds;
	}

	private static Pair<String, String> getCurrentUser() {
		try {
			List<Object[]> lst = ORMHelper.executeQuery( QueryType.SQL, "SELECT HOST_NAME(), SUSER_NAME()", null );
			if( (lst == null) || (lst.size() != 1) || (lst.get(0).length != 2) ) {
				if( log.isErrorEnabled() ) log.error("Start.getCurrentUser() : request return incorrect value");
				return null;
			}
			Resources.getInstance().put( ResourcesConst.CURRENT_HOST, (String) lst.get(0)[0] );
			Resources.getInstance().put( ResourcesConst.CURRENT_LOGIN, (String) lst.get(0)[1] );
			return new Pair<>( (String) lst.get(0)[0], (String) lst.get(0)[1] );
		} catch (Exception e) {
			if( log.isErrorEnabled() ) log.error("Start.getCurrentUser() ", e);
		}
		return null;
	}

	private static Pair<Integer, Integer> getPerson(String login) {
		if( login == null ) return null;
		try {
			String sql = Joiner.on("'").join("select person_id, role_id from minos.PersonAddon where logins = ", login, " ").toString();
			List<Object[]> lst = ORMHelper.executeQuery( QueryType.SQL, sql, null);
			if( (lst == null) || (lst.size() != 1) || (lst.get(0).length != 2) ) {
				if( log.isErrorEnabled() ) log.error("Start.getPerson() : request return incorrect value");
				return null;
			}
			Person person = (Person) ORMHelper.findEntity( Person.class, (Integer) lst.get(0)[0] );
			Role role = (Role) ORMHelper.findEntity( Role.class, (Integer) lst.get(0)[1] );			

			Resources.getInstance().put( ResourcesConst.CURRENT_PERSON, person );
			Resources.getInstance().put( ResourcesConst.CURRENT_ROLE, role );
			return new Pair<>( (Integer) lst.get(0)[0], (Integer) lst.get(0)[1] );
		} catch (Exception e) {
			if( log.isErrorEnabled() ) log.error("Start.getPerson() ", e);
		}
		return null;
	}

	private static void initDoomsday() {
		Calendar calendar = Calendar.getInstance();
		calendar.set(9999, 11, 30);
		Resources.getInstance().put(ResourcesConst.DOOMSDAY, new Timestamp( calendar.getTimeInMillis() ) );
	}
	
	public static void main(String[] args) {
		thread = Thread.currentThread();
		
		
		DOMConfigurator.configure(System.getProperty("user.dir") + "\\log4j.xml");		
		
		DataSource ds = fillDataSource(); // (dbconfig);		
		if(ds == null) {
			JOptionPane.showMessageDialog(null, "�� ������� ���������� � ��", "������", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		// create EntityManagerFactory object 
		Properties prop = new Properties();
		//Set the DataSource into the map passed to Persistence.createEntityManagerFactory under the openjpa.ConnectionFactory key.
		prop.put("openjpa.ConnectionFactory", ds);
		EntityManagerFactory factory = Persistence.createEntityManagerFactory("CompetenceEntitiesPU", prop);
		ORMHelper.setFactory(factory);

		Pair<String, String> p1 = getCurrentUser();
		if( log.isInfoEnabled() ) log.info( "host:" + p1.getFirst() + "   | login:" + p1.getSecond() );
		Pair<Integer, Integer> p2 = getPerson( p1.getSecond() );
		if( log.isInfoEnabled() ) log.info( "person_id:" + p2.getFirst() + "   | role_id:" + p2.getSecond() );
		Person person = (Person) ORMHelper.findEntity( Person.class, p2.getFirst() );
		Role role = (Role) ORMHelper.findEntity( Role.class, p2.getSecond() );
		
		
		if( log.isInfoEnabled() ) log.info( role.toString() );
		if( log.isInfoEnabled() ) log.info( person.toString() );
		
		initDoomsday();
		
		if( log.isInfoEnabled() ) log.info("start main windows");
		new MainWnd();
				
		while ( !wakeupFlag ) {			
			try {			
				if( log.isInfoEnabled() ) log.info( "Statrt.main() :  it's bedtime" );
				Thread.sleep(Long.MAX_VALUE);				
			} catch (InterruptedException e) { 
				if( log.isInfoEnabled() ) log.info( "Statrt.main() :  wakeup" );
			}			
		}
		
		factory.close();
		// programConfig.saveConfig("program.cfg");
		if( log.isInfoEnabled() ) log.info( "Statrt.main() :  bye" );
		System.exit(0);
	}
}


/*
private DBConnectionConfig makeDBConnectionConfig(ProgramConfig config) {
		DBConnectionConfig dbconfig = (config == null ? null : config.getDBConnectionConfig());
		if( (config != null) && (dbconfig != null) && !config.isShowDBConnectionConfig() )
			return dbconfig;

		DBConnectionConfig dbconfig2 = DBConnectionDlg.showDBConnectionDlg(null, dbconfig);

		return dbconfig2;
	}
	
	private SQLServerDataSource fillDataSource(DBConnectionConfig dbcfg) {
		if(dbcfg == null)
			return null;
		
		SQLServerDataSource msds = new SQLServerDataSource();
		msds.setServerName(dbcfg.getServerAddress());
		msds.setDatabaseName(dbcfg.getDbName());
		msds.setPortNumber(dbcfg.getDbPort());		
		if(dbcfg.getDbInstance() != null)
			msds.setInstanceName(dbcfg.getDbInstance());
		if(dbcfg.isIntegratedSecurity()) {
			msds.setIntegratedSecurity(true);
		} else {
			msds.setIntegratedSecurity(false);
			msds.setUser(dbcfg.getLogin());
			msds.setPassword(dbcfg.getPassword());				
		}

		// see http://openjpa.apache.org/builds/1.2.3/apache-openjpa/docs/dbsupport_sqlserver.html			
		msds.setSelectMethod("cursor");
		msds.setSendStringParametersAsUnicode(false);
		return msds;
	}

	
	private void loadFont(Map<String, Object> resources) {
		Font font1 = new Font("Tahoma", Font.PLAIN, 12);		
		Font font2 = new Font("Tahoma", Font.BOLD, 12);
		Font font3 = new Font("Tahoma", Font.ITALIC, 12);
		
		Map<TextAttribute, Object> m = new HashMap<>();
		m.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
		Font font4 = font3.deriveFont(m);		
		
		resources.put("button.font", font1);		
		resources.put("primary.button.font", font2);
		resources.put("label.font", font1);
		resources.put("division.normal", font1);
		resources.put("division.notOTIZ_OK", font1);
		resources.put("division.delete", font4);
	}
	
	private void loadSQL(Map<String, Object> resources) {
		Resources.getInstance().putResource(ResourcesConst.JPQL_LOAD_ACTORS_FOR_ROUND, 
				"SELECT ra FROM RoundActors ra WHERE ra.round.id = ?1");

		Resources.getInstance().putResource(ResourcesConst.JPQL_LOAD_TEST_TYPE, 
				"SELECT tt FROM TestType tt WHERE tt.variety = ?1");

		
		resources.put("sql.canAlienEdit", "SELECT COUNT(*) FROM MinosRoundActors where alienSinner_id=?1");
		resources.put("sql.canAlienDelete", "SELECT COUNT(*) FROM MinosRoundActors where alienSinner_id=?1");
		resources.put("sql.profileForPerson", " SELECT COUNT(mp.id) FROM MinosProfile mp INNER JOIN MinosSupport ms ON ms.id = mp.support_id WHERE mp.division_id = ?1 AND mp.position_id = ?2 AND GETDATE() <  ms.removeDT");
		resources.put("sql.allProfileForPerson", "SELECT mp.id, mp.item, mp.division_id, mp.position_id, mp.competence_id, mp.minLevel_id, mp.support_id "
				+ " FROM MinosProfile mp " 
				+ " inner join MinosSupport ms on ms.id = mp.support_id "
				+ " where mp.division_id = ?1 and mp.position_id = ?2 and GETDATE() <  ms.removeDT");

		resources.put("sql.profilePositionForPerson", "	SELECT ds.* FROM tStatDolSP ds "
				+ "WHERE ds.tStatDolSPId IN (SELECT DISTINCT(mp.position_id) FROM MinosProfile mp "
				+ "INNER join MinosSupport ms ON ms.id = mp.support_id "
				+ "WHERE mp.division_id = ?1 AND GETDATE() <  ms.removeDT)"); 
		
		resources.put("sql.maxItemForProfile", "SELECT ISNULL(MAX(item), 0) FROM MinosProfile WHERE division_id = ?1 AND position_id = ?2");
	}
	
	
	private void loadIcon(Map<String, Object> resources) {
		ImageIcon icon1 = new ImageIcon(getClass().getResource("/img/32/page_edit.png"));
		resources.put("icon.add.32", new ImageIcon(getClass().getResource("/img/32/add.png")));
		resources.put("icon.edit.32", icon1);
		resources.put("icon.delete.32", new ImageIcon(getClass().getResource("/img/32/delete.png")));
		resources.put("icon.up.32", new ImageIcon(getClass().getResource("/img/32/up.png")));
		resources.put("icon.next.32", new ImageIcon(getClass().getResource("/img/32/next.png")));
		resources.put("icon.down.32", new ImageIcon(getClass().getResource("/img/32/down.png")));
		resources.put("icon.refresh.32", new ImageIcon(getClass().getResource("/img/32/refresh.png")));
		resources.put("icon.round.64", new ImageIcon(getClass().getResource("/img/64/up.png")));

		resources.put("icon.addFolder.32", new ImageIcon(getClass().getResource("/img/32/folder_add.png")));
		resources.put("icon.loadFolder.32", new ImageIcon(getClass().getResource("/img/32/folder_down.png")));
		resources.put("icon.addCompetence.32", new ImageIcon(getClass().getResource("/img/32/book_add.png")));
		resources.put("icon.loadCompetence.32", new ImageIcon(getClass().getResource("/img/32/page_down.png")));
		resources.put("icon.addIndicator.32", icon1);		
				
		resources.put("icon.addAlien.32", new ImageIcon(getClass().getResource("/img/32/business_user_add.png")));
		resources.put("icon.editAlien.32", new ImageIcon(getClass().getResource("/img/32/business_user_edit.png")));
		resources.put("icon.deleteAlien.32", new ImageIcon(getClass().getResource("/img/32/business_user_delete.png")));

		resources.put("icon.level3.32", new ImageIcon(getClass().getResource("/img/32/level3.png")));
		
		resources.put("icon.addActors.32", new ImageIcon(getClass().getResource("/img/32/users_add.png")));
		resources.put("icon.deleteActors.32", new ImageIcon(getClass().getResource("/img/32/users_delete.png")));
	}	
	
	public static void main(String[] args) throws Exception{
		test t = new test();
		
		// load configuration file or create new 
		ProgramConfig programConfig = ProgramConfig.loadConfig("program.cfg");
		if(programConfig == null) {
			programConfig = new ProgramConfig();
			//programConfig.saveConfig("program.cfg");
		}
		
		// create DataSource and load current user id and login
		DBConnectionConfig dbconfig = t.makeDBConnectionConfig(programConfig);
		if(dbconfig == null) {
				JOptionPane.showMessageDialog(null, "�� ������� ������������ ���������� � ��", "������", JOptionPane.ERROR_MESSAGE);
				return;
		}
		programConfig.setDBConnectionConfig(dbconfig);
		SQLServerDataSource ds = t.fillDataSource(dbconfig);		
		if(ds == null) {
			JOptionPane.showMessageDialog(null, "�� ������� ���������� � ��", "������", JOptionPane.ERROR_MESSAGE);
			return;
		}
		Pair<Integer, String> user = t.getCurrentUser(ds);
		if(user == null) {
			JOptionPane.showMessageDialog(null, "�� ������� �������� ����� � ��� ������������", "������", JOptionPane.ERROR_MESSAGE);
			return;			
		}

		// create Person Entity object
		EntityManager em = factory.createEntityManager();
		Person person = em.find(Person.class, user.getFirst());
		em.close();

		// make models for ui elements
		CatalogTreeModel catalogTM = new CatalogTreeModel(factory);
		CompetenceTreeModel competenceTM = new CompetenceTreeModel(factory, true);
		CatalogAndCompetenceTreeModel ctalogAndCompetenceTM = new CatalogAndCompetenceTreeModel(factory, catalogTM, competenceTM, true);

		DivisionTreeModel divisionTM = new DivisionTreeModel(factory, programConfig.getDivisionRootID(), false, false, false, DivisionOrder.BY_NAME);
		PositionInDivisionTreeModel positionInDivisionTM = new PositionInDivisionTreeModel(factory, divisionTM, true, true);		
		CompetenceAndPositionInDivisionTreeModel competenceAndPositionInDivisioTM = new CompetenceAndPositionInDivisionTreeModel(factory, positionInDivisionTM, competenceTM);
		
		PersonInDivisionTreeModel personInDivisionTM = new PersonInDivisionTreeModel(factory, divisionTM, true);

		// resources initialize and fill 
		Map<String, Object> resources = new HashMap<>();		
		t.loadIcon(resources);
		t.loadSQL(resources);
		t.loadFont(resources);
		Resources.getInstance().putResource(ResourcesConst.CURRENT_PERSON, person);
		Resources.getInstance().putResource(ResourcesConst.CURRENT_LOGIN, user.getSecond());
		Resources.getInstance().putResource(ResourcesConst.CATALOG_COMPETENCE_TREEMODEL, ctalogAndCompetenceTM);
		Resources.getInstance().putResource(ResourcesConst.COMPETENCE_POSITION_DIVISION_TREEMODEL, competenceAndPositionInDivisioTM);
		Resources.getInstance().putResource(ResourcesConst.PERSON_DIVISION_TREEMODEL, personInDivisionTM);
		Resources.getInstance().putResource(ResourcesConst.TREE_RENDERER, new MinosTreeRenderer());
		
		resources.put("program.config", programConfig);
		resources.put("current.person", person);
		resources.put("current.user.id", user.getFirst());
		resources.put("current.user.login", user.getSecond());
		resources.put("catalog.competence.TreeModel", ctalogAndCompetenceTM);
		resources.put("competence.position.division.TreeModel", competenceAndPositionInDivisioTM);
		resources.put("person.division.TreeModel", personInDivisionTM);
		resources.put("tree.renderer", new MinosTreeRenderer());
				
	*/
