package minos.utils;

import java.util.Map;
import java.util.TreeMap;

import javax.swing.ImageIcon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceKeeper {
	private static Logger log = LoggerFactory.getLogger( ResourceKeeper.class );
	private static Map<Integer, Object> map = new TreeMap<Integer, Object>();

	static {
		map.put( QType.JPQL_LOAD_BASE_POST.getIndex(), " SELECT bp FROM BasisPost bp "
				+ " WHERE EXISTS ( SELECT ep FROM  EstablishedPost ep WHERE ep.basisPost = bp ) "
				+ " ORDER BY bp.kpers, bp.name"  );

		map.put( QType.JPQL_LOAD_CATALOGS.getIndex(), " SELECT entity FROM Catalog entity "
				+ " INNER JOIN FETCH entity.name "
				+ " WHERE entity.variety IN (:variety) "
				+ " AND ( (:ts BETWEEN entity.journal.editMoment AND entity.journal.deleteMoment) "
				+ " OR ( entity.journal.deleteMoment < :dts AND entity.status = 2)  ) "
				+ " AND entity.parentCatalog.id = :pcid "
				+ " ORDER BY entity.item " );
		
		map.put( QType.JPQL_LOAD_COMPETENCES.getIndex(), " SELECT entity FROM Competence entity "
				+ " INNER JOIN FETCH entity.name "
				+ " INNER JOIN FETCH entity.journal "
				+ " WHERE ( entity.catalog IN (:catalogs) ) "
				+ " AND ( ( :ts BETWEEN entity.journal.editMoment AND entity.journal.deleteMoment) "
				+ " OR ( entity.journal.deleteMoment < :dts AND entity.status = 2)  ) "
				+ " ORDER BY entity.item " );

		map.put( QType.JPQL_LOAD_DIVISIONS.getIndex(), " SELECT entity FROM Division entity "
				+ " JOIN FETCH entity.fullName "
				+ " WHERE ( entity.parentDivision IN (:divisions) )"
				+ " AND ( ( :ts BETWEEN entity.beginDate AND entity.endDate )  OR ( entity.endDate < :overdue ) ) "
				+ " AND ( entity.isdelete IN (:del) ) "
				+ " AND ( entity.otizOk IN (:disaprv) ) "
				+ " ORDER BY entity.fullName" );

		map.put( QType.JPQL_LOAD_DIVISIONS_BY_ID.getIndex(), " SELECT entity FROM Division entity "
				+ " WHERE entity.id IN (:ids)" );
		
		map.put( QType.JPQL_LOAD_EPOSTS.getIndex(), " SELECT entity FROM EstablishedPost entity "
				+ " JOIN FETCH entity.name "
				+ " JOIN FETCH entity.basisPost "
				+ " WHERE ( entity.division.id = :did )"
				+ " AND ( ( :ts between entity.beginDate AND entity.endDate )  OR ( entity.endDate < :overdue ) ) "
				+ " AND ( entity.isdelete IN (:del) ) "
				+ " AND ( entity.otizOk IN (:disaprv) ) "
				+ " ORDER BY entity.name " );

		map.put( QType.JPQL_LOAD_INDICATORS.getIndex(), " SELECT entity FROM Indicator entity "
				+ " INNER JOIN FETCH entity.name "
				+ " INNER JOIN FETCH entity.level "
				+ " INNER JOIN FETCH entity.journal "
				+ " INNER JOIN FETCH entity.competence "
				+ " WHERE entity.competence IN (:competences) "
				+ " AND entity.level IN (:levels) "
				+ " AND ( ( :ts between entity.journal.editMoment AND entity.journal.deleteMoment) "
				+ " OR ( entity.journal.deleteMoment < :dts AND entity.status = 2) )"
				+ " ORDER BY entity.level, entity.item " );

		map.put( QType.JPQL_LOAD_PROFILE_BY_EPOST.getIndex(), " SELECT entity FROM Profile entity "
				+ " INNER JOIN FETCH entity.profilePattern "
				+ " INNER JOIN FETCH entity.journal "
				+ " WHERE entity.establishedPost IN (:eposts) "
				+ " AND :ts BETWEEN entity.journal.editMoment AND entity.journal.deleteMoment" );

		map.put( QType.JPQL_LOAD_PROFILE_BY_POST.getIndex(), " SELECT entity FROM Profile entity "
				+ " INNER JOIN FETCH entity.profilePattern "
				+ " INNER JOIN FETCH entity.journal "
				+ " WHERE entity.post IN (:posts) "
				+ "    AND entity.division IN (:divisions)"
				+ "    AND :ts BETWEEN entity.journal.editMoment AND entity.journal.deleteMoment" );

		map.put( QType.JPQL_LOAD_PROFILE_FOR_TEST_ASSEMBLER.getIndex(), " SELECT p FROM Profile p "
				+ " INNER JOIN FETCH p.profilePattern "
				+ " INNER JOIN FETCH p.profilePattern.profilePatternElements "
				+ " WHERE p.id IN (:profile_ids)" );

		map.put( QType.JPQL_LOAD_MEASURES.getIndex(), " SELECT entity FROM Measure entity "
				+ " INNER JOIN FETCH entity.journal "
				+ " INNER JOIN FETCH entity.branchOffice "
				+ " WHERE entity.branchOffice IN (:boffice) "
				+ " AND ( (entity.start BETWEEN :intervalBegin AND :intervalEnd) "
				+ " OR (entity.stop BETWEEN :intervalBegin AND :intervalEnd) "
				+ " OR (entity.journal.deleteMoment > :dts) ) " );

		map.put( QType.JPQL_LOAD_PP.getIndex(), " SELECT entity FROM ProfilePattern entity "
				+ " INNER JOIN FETCH entity.name "
				+ " WHERE entity.catalog IN (:catalogs) "
				+ " AND ( ( :ts BETWEEN entity.journal.editMoment AND entity.journal.deleteMoment) "
				+ " OR (entity.journal.deleteMoment < :dts AND entity.status = 2) ) "
				+ " ORDER BY entity.item " );

		map.put( QType.JPQL_LOAD_PPE_AND_COMPETENCE.getIndex(), " SELECT ppe, cmt "
				+ " FROM ProfilePatternElement ppe, Competence cmt "
				+ " INNER JOIN FETCH ppe.minLevel "
				+ " INNER JOIN FETCH ppe.competence "
				+ " INNER JOIN FETCH ppe.journal "
				+ " INNER JOIN FETCH ppe.profilePattern "
				+ " WHERE  ppe.profilePattern IN (:pps) "
				+ " AND ppe.competence.variety IN (:vrt) "
				+ " AND ( ( :ts BETWEEN ppe.journal.editMoment AND ppe.journal.deleteMoment) "
				+ " OR ( ppe.journal.deleteMoment < :dts AND ppe.status = 2 )  ) "
				+ " AND ( ( cmt.id = ppe.competence.id OR cmt.ancestor.id = ppe.competence.id ) "
				+ "   AND ( ppe.profilePattern.timePoint BETWEEN cmt.journal.editMoment AND cmt.journal.deleteMoment ) )" );

		map.put( QType.JPQL_LOAD_PPE_ATTRS.getIndex(), " SELECT entity FROM PpeStrAtr entity "
				+ " INNER JOIN FETCH entity.journal "
				+ " INNER JOIN FETCH entity.stringAttr "
				+ " INNER JOIN FETCH entity.stringAttr.key "
				+ " INNER JOIN FETCH entity.stringAttr.value "
				+ " WHERE ( entity.ppe IN (:ppes) ) " 
				+ " AND ( ( :ts BETWEEN entity.journal.editMoment AND entity.journal.deleteMoment ) "
				+ " OR ( entity.journal.deleteMoment < :dts ) ) "
				+ " ORDER BY entity.item " );
		
		map.put( QType.JPQL_LOAD_ROLE.getIndex(), " SELECT entity FROM Role entity "
				+ " INNER JOIN FETCH entity.name "
				+ " INNER JOIN FETCH entity.description "
				+ " INNER JOIN FETCH entity.flag "
				+ " WHERE NOT entity.id IN (1,2)" );		

		map.put( QType.JPQL_LOAD_ACTORS.getIndex(), " SELECT entity FROM Actors entity "
				+ " JOIN FETCH entity.measure "
				+ " JOIN FETCH entity.measure.branchOffice "
				+ " LEFT JOIN FETCH entity.minos "
				+ " LEFT JOIN FETCH entity.internalSinner "
				+ " JOIN FETCH entity.journal "
				+ " JOIN FETCH entity.testMode "
				+ " JOIN FETCH entity.reserveLevel "
				+ " JOIN FETCH entity.reserveType "
				+ " LEFT JOIN FETCH entity.profile "
				+ " LEFT JOIN FETCH entity.profile.profilePattern "
				+ " WHERE entity.measure IN (:measures) "
				+ " AND entity.journal.deleteMoment > :dts "
				+ " ORDER BY entity.measure.id " );

		map.put( QType.JPQL_LOAD_PERSONS_AND_PERSON_ADDON.getIndex(), " SELECT p, ep, d "
				+ " FROM Person p, PersonPostRelation ppr, EstablishedPost ep, Division d "
				+ " LEFT JOIN FETCH p.personAddon "
				+ " LEFT JOIN FETCH p.personAddon.logins "
				+ " LEFT JOIN FETCH p.personAddon.role "
				+ " WHERE p.id BETWEEN :startPersonID AND :stopPersonID "
				+ " AND p.status IN (:stats) "
				+ " AND p = ppr.person "
				+ " AND ppr.epost = ep "
				+ " AND ep.division = d "
				+ " ORDER BY p.surname " );

		map.put( QType.JPQL_LOAD_PERSONS_ONLY.getIndex(), " SELECT p, ep, d "
				+ " FROM Person p, PersonPostRelation ppr, EstablishedPost ep, Division d "
				+ " WHERE p.id BETWEEN :startPersonID AND :stopPersonID "
				+ " AND p.status IN (:stats) "
				+ " AND p = ppr.person "
				+ " AND ppr.epost = ep "
				+ " AND ep.division = d "
				+ " ORDER BY p.surname " );
		
		map.put( QType.JPQL_LOAD_PERSON_ADDON_BY_ROLES.getIndex(), " SELECT entity FROM PersonAddon entity "
				+ " LEFT JOIN FETCH entity.role "
				+ " WHERE entity.role IN (:roles) " );
		
		map.put( QType.JPQL_LOAD_ACTORS_INFO.getIndex(), " SELECT entity FROM ActorsInfo entity "
				+ " INNER JOIN FETCH entity.name "
				+ " INNER JOIN FETCH entity.journal "
				+ " WHERE entity.variety IN (:varieties) "
				+ " AND ( (entity.journal.editMoment < :ts AND :ts < entity.journal.deleteMoment) "
				+ " OR (entity.journal.deleteMoment < :dts) ) " );
		
		map.put( QType.JPQL_LOAD_PERSON_BY_EPOST.getIndex(), "SELECT entity.person "
				+ " FROM PersonPostRelation entity "
				+ " WHERE entity.epost IN (:epost) "
				+ " AND entity.type IN (:types) "
				+ " AND entity.state IN (:state) "
				+ " AND ( (:ts BETWEEN entity.beginDate AND entity.endDate) "
				+ " OR (:dts > entity.endDate) )"
				+ " AND ( (:ts BETWEEN entity.person.beginDate AND entity.person.endDate) "
				+ " OR (:dts > entity.person.endDate) )" );

		map.put( QType.JPQL_LOAD_FASET.getIndex(), " SELECT entity FROM Faset entity "
				+ " WHERE entity.type IN (:types) "  );

		
		
		map.put( QType.JPQL_REFRESH_CATALOGS_BEFORE_DELETE.getIndex(), " SELECT entity FROM Catalog entity "
				+ " INNER JOIN FETCH entity.journal "
				+ " WHERE entity.id IN (:catalog_ids) AND entity.status IN (:status) " );

		map.put( QType.JPQL_REFRESH_COMPETENCE_BEFORE_DELETE.getIndex(), " SELECT entity FROM Competence entity "
				+ " INNER JOIN FETCH entity.journal "
				+ " WHERE entity.id IN (:competence_ids) AND entity.status IN (:status) " );

		map.put( QType.JPQL_REFRESH_INDICATOR_BEFORE_DELETE.getIndex(), " SELECT entity FROM Indicator entity "
				+ " INNER JOIN FETCH entity.journal "
				+ " INNER JOIN FETCH entity.competence "
				+ " WHERE entity.id IN (:indicator_ids) AND entity.status IN (:status) " );

		map.put( QType.JPQL_REFRESH_PP_BEFORE_DELETE.getIndex(), " SELECT entity FROM ProfilePattern entity "
				+ " INNER JOIN FETCH entity.journal "
				+ " WHERE entity.id IN (:pp_ids) AND entity.status IN (:status) " );

		map.put( QType.JPQL_REFRESH_PPE_BEFORE_DELETE.getIndex(), " SELECT entity FROM ProfilePatternElement entity "
				+ " INNER JOIN FETCH entity.journal "
				+ " WHERE entity.id IN (:ppe_ids) AND entity.status IN (:status) " );

		map.put( QType.JPQL_REFRESH_MEASURE_BEFORE_DELETE.getIndex(), " SELECT entity FROM Measure entity "
				+ " INNER JOIN FETCH entity.journal "
				+ " WHERE entity.id IN (:msrs) AND entity.journal.deleteMoment > CURRENT_TIMESTAMP " );

		map.put( QType.JPQL_REFRESH_ACTORS_BEFORE_DELETE_FOR_MEASURE.getIndex(), " SELECT entity FROM Actors entity "
				+ " INNER JOIN FETCH entity.journal "
				+ " WHERE entity.measure IN (:measures) AND entity.status IN (:status) " );

		map.put( QType.JPQL_REFRESH_ACTORS_BEFORE_DELETE_FOR_ACTORS.getIndex(), " SELECT entity FROM Actors entity "
				+ " INNER JOIN FETCH entity.journal "
				+ " WHERE entity IN (:actors) AND entity.status IN (:status) " );
		
		map.put( QType.JPQL_REFRESH_CURRENT_PROFILE_BEFORE_DELETE_BY_EPOST.getIndex(), " SELECT entity FROM Profile entity "
				+ " INNER JOIN FETCH entity.journal "
				+ " WHERE CURRENT_TIMESTAMP BETWEEN entity.journal.editMoment AND entity.journal.deleteMoment "
				+ " AND entity.establishedPost IN (:eposts) " );

		map.put( QType.JPQL_REFRESH_CURRENT_PROFILE_BEFORE_DELETE_BY_POST.getIndex(), " SELECT entity FROM Profile entity "
				+ " INNER JOIN FETCH entity.journal "
				+ " INNER JOIN FETCH entity.division "
				+ " INNER JOIN FETCH entity.post "
				+ " WHERE CURRENT_TIMESTAMP BETWEEN entity.journal.editMoment AND entity.journal.deleteMoment "
				+ " AND entity.division IN (:divisions) "
				+ " AND entity.post IN (:posts) " );

		map.put( QType.JPQL_FIND_PPE_STRING_ATTR.getIndex(), " SELECT entity FROM StringAttr entity "
				+ " WHERE entity.value=:value AND entity.variety IN (:variety) " );		
		
		map.put( QType.JPQL_FIND_CATALOG.getIndex(), " SELECT c FROM Catalog c "
				+ " WHERE c.name=:name AND c.variety=:variety AND c.parentCatalog=:parent AND c.status=:status" );
				
		
		map.put( QType.JPQL_MAX_COMPETENCE_ITEM.getIndex(), "SELECT MAX(c.item) FROM Competence c WHERE c.catalog = :catalog" );
		map.put( QType.JPQL_MAX_CATALOG_ITEM.getIndex(), "SELECT MAX(c.item) FROM Catalog c WHERE c.parentCatalog = :parent" );				
		map.put( QType.JPQL_MAX_INDICATOR_ITEM.getIndex(), "SELECT MAX(i.item) FROM Indicator i WHERE i.competence = :competence AND i.level = :level" );
		map.put( QType.JPQL_MAX_PP_ITEM.getIndex(), "SELECT MAX(p.item) FROM ProfilePattern p WHERE p.catalog = :catalog" );
		map.put( QType.JPQL_MAX_PPE_ITEM.getIndex(), "SELECT MAX(p.item) FROM ProfilePatternElement p WHERE p.competence.variety = :variety AND p.profilePattern = :pp" );
		map.put( QType.JPQL_COUNT_PERSON_ADDON_BY_ROLES.getIndex(), "SELECT COUNT(entity) FROM PersonAddon entity WHERE entity.role IN (:roles) " );
		
		
		map.put( QType.SQL_LOAD_ALL_SUB_CATALOGS.getIndex(), " WITH SubCatalogs( rootid, subid ) AS ( "
				+ " SELECT parent_id, id FROM Minos.Catalog WHERE parent_id = ?1 "
				+ " UNION ALL "
				+ " SELECT parent_id, id  FROM Minos.Catalog c INNER JOIN SubCatalogs c2 ON c.parent_id = c2.subid ) "
				+ " SELECT * FROM Minos.Catalog WHERE status = ?2 AND id IN ( SELECT DISTINCT(subid) FROM SubCatalogs )" );

		map.put( QType.SQL_LOAD_ESTIMATION.getIndex(), " SELECT ap.actors_id, c.variety, "
				+ " SUM(ap.cost) res, SUM(l.price) doc FROM Minos.ActorsPerformance ap "
				+ " INNER JOIN Minos.ProfilePatternElement ppe on ppe.id = ap.ppe_id "
				+ " INNER JOIN Minos.Level l on l.id = ppe.minLevel_id "
				+ " INNER JOIN Minos.ProfilePattern pp on pp.id = ppe.profilePattern_id "
				+ " INNER JOIN Minos.Competence c on ( c.id = ppe.competence_id or c.ancestor_id = ppe.competence_id ) "
				+ " INNER JOIN Minos.Journal j on j.id = c.journal_id "
				+ " WHERE (ap.actors_id IN (%s)) AND (pp.timePoint BETWEEN j.editMoment AND j.deleteMoment)"
				+ " GROUP BY ap.actors_id, c.variety "
				+ " ORDER BY ap.actors_id asc, c.variety asc " );		
		
		map.put( QType.SQL_LOAD_STRING_ATTR.getIndex(), "SELECT TOP 10 * FROM Minos.StringAttr WHERE variety = ?1" );
	}

	public static enum QType {
		JPQL_LOAD_BASE_POST( 1_000_000 ),
		JPQL_LOAD_CATALOGS( 1_000_002 ),
		JPQL_LOAD_COMPETENCES( 1_000_003 ),		
		JPQL_LOAD_DIVISIONS( 1_000_004 ),
		JPQL_LOAD_DIVISIONS_BY_ID( 1_000_005 ),
		JPQL_LOAD_EPOSTS( 1_000_006 ),
		JPQL_LOAD_INDICATORS( 1_000_007 ),
		JPQL_LOAD_PROFILE_BY_EPOST( 1_000_008 ),
		JPQL_LOAD_MEASURES( 1_000_009 ),
		JPQL_LOAD_PP( 1_000_010 ),
		JPQL_LOAD_PPE_AND_COMPETENCE( 1_000_011 ),
		JPQL_LOAD_PPE_ATTRS( 1_000_012 ),
		JPQL_LOAD_ROLE( 1_000_013 ),
		JPQL_LOAD_ACTORS( 1_000_014 ),
		JPQL_LOAD_PERSONS_ONLY( 1_000_015 ),
		JPQL_LOAD_PERSONS_AND_PERSON_ADDON( 1_000_016 ),
		JPQL_LOAD_PERSON_ADDON_BY_ROLES( 1_000_017 ),
		JPQL_LOAD_ACTORS_INFO( 1_000_018 ),
		JPQL_LOAD_PERSON_BY_EPOST( 1_000_019 ),
		JPQL_LOAD_FASET( 1_000_020 ),
		JPQL_LOAD_PROFILE_FOR_TEST_ASSEMBLER( 1_000_021 ),
		JPQL_LOAD_PROFILE_BY_POST( 1_000_022 ),
				
		JPQL_FIND_PPE_STRING_ATTR( 1_000_040 ),
		JPQL_FIND_CATALOG( 1_000_041 ),
		
		JPQL_REFRESH_CATALOGS_BEFORE_DELETE( 1_000_050 ),
		JPQL_REFRESH_COMPETENCE_BEFORE_DELETE( 1_000_051 ),
		JPQL_REFRESH_INDICATOR_BEFORE_DELETE( 1_000_052 ),
		JPQL_REFRESH_PP_BEFORE_DELETE( 1_000_053 ),
		JPQL_REFRESH_PPE_BEFORE_DELETE( 1_000_054 ),
		JPQL_REFRESH_MEASURE_BEFORE_DELETE( 1_000_055 ),
		JPQL_REFRESH_ACTORS_BEFORE_DELETE_FOR_MEASURE( 1_000_056 ),
		JPQL_REFRESH_ACTORS_BEFORE_DELETE_FOR_ACTORS( 1_000_057 ),
		JPQL_REFRESH_CURRENT_PROFILE_BEFORE_DELETE_BY_EPOST( 1_000_058 ),
		JPQL_REFRESH_CURRENT_PROFILE_BEFORE_DELETE_BY_POST( 1_000_059 ),
		
		JPQL_MAX_COMPETENCE_ITEM( 1_000_100 ),
		JPQL_MAX_CATALOG_ITEM( 1_000_101 ),
		JPQL_MAX_INDICATOR_ITEM( 1_000_102 ),
		JPQL_MAX_PP_ITEM( 1_000_103 ),
		JPQL_MAX_PPE_ITEM( 1_000_104 ),
		
		JPQL_COUNT_PERSON_ADDON_BY_ROLES( 1_000_154 ),
		
		SQL_LOAD_ALL_SUB_CATALOGS( 1_000_200 ),
		SQL_LOAD_STRING_ATTR( 1_000_201 ),
		SQL_LOAD_ESTIMATION( 1_000_202 );

		
		private int ind;
		QType( int ind ) { 
			this.ind = ind;
		}
		public int getIndex() { return ind; }
	};
	
	public static enum OType {
		BASE_POST_LIST( 11, "BASE_POST_LIST" ),
		BRANCH_OFFICES_INFO( 12, "BRANCH_OFFICES_INFO" ),
		DEFAULT_BRANCH_OFFICE_CODE( 13, "DEFAULT_BRANCH_OFFICE_CODE" ),
		BRANCH_OFFICE_ROLE_MASK( 14, "BRANCH_OFFICE_ROLE_MASK" ),
		TABLES_INFO( 15, "TABLES_INFO" ),				
		PERMISSION_CONSTRAINT( 16, null ), 
		PERMISSION_DEFAULT( 17, null ),
		

		PROGRAM_CONFIG( 1, null ), 
		CURRENT_PERSON( 2, null ), 
		CURRENT_ROLE( 3, null ),
		PERMISSION_CURRENT( 4, null ), 
		CURRENT_LOGIN( 5, null ), 
		CURRENT_HOST( 6, null ),		
		DOOMSDAY( 7, null ), 
		DAMNED_FUTURE( 8, null ),  
		WAR( 9, null ),  

		LEVELS_CACHE( 100, null );

		private int ind;
		private String name;;
		OType( int ind, String name ) { 
			this.ind = ind;
			this.name = name;
		}
		public int getIndex() { return ind; }
		public String getName() { return name; }
	};

	public static enum IType { 
		OK( 1_000_000_000, "ok.png" ),
		CANCEL( 1_001_000_000, "cancel.png" ),
		NEW( 1_002_000_000, "new.png" ),
		EDIT( 1_003_000_000, "edit.png" ), 
		REMOVE( 1_004_000_000, "remove.png" ),
		DELETE( 1_005_000_000, "delete.png" ), 
		REFRESH( 1_006_000_000, "refresh.png" ), 
		CLOCK( 1_007_000_000, "chronometer.png" ), 
		FILTER( 1_008_000_000, "filter.png" ), 
		FORK( 1_009_000_000, "fork.png" ),
		HAMMER( 1_010_000_000, "hammer.png" ), 
		RUN( 1_011_000_000, "run.png" ),
		
		PENCIL( 1_012_000_000, "pencil.png" ), 
		KEY( 1_013_000_000, "key.png" ),
		PAPER_CLIP( 1_014_000_000, "paperclip.png" ),		
		SAVE_ALL( 1_015_000_000, "save_all.png" ),
		CHECKERED_FLAG( 1_016_000_000, "checkered_flag.png" ),
		ERROR( 1_017_000_000, "error.png" ),
		
		PREFERENCES( 1_018_000_000, "preferences.png" ),		
		
		LOCK( 1_019_000_000, "lock.png" ),
		UNLOCK( 1_020_000_000, "unlock.png" ),
		
		ENTERPRISE( 1_021_000_000, "enterprise.png" ),
		OFFICE( 1_022_000_000, "office.png" ),

		BULB_ON( 1_023_000_000, "bulb_on.png"), 
		BULB_OFF( 1_024_000_000, "bulb_off.png" ),
		ADD( 1_025_000_000, "add.png" ),
		COPY( 1_026_000_000, "copy.png" ),
		SAVE( 1_027_000_000, "save.png" ),
		DOWN( 1_028_000_000, "down.png" ),
		WORD( 1_029_000_000, "word.png" ),
		EXCEL( 1_030_000_000, "excel.png" ),
		
		CATALOG( 1_050_000_000, "folder.png" ),
		CATALOG_ADD( 1_051_000_000, "folder_add.png" ),  
		CATALOG_EDIT( 1_052_000_000, "folder_edit.png" ),		
		CATALOG_DOWNLOAD( 1_053_000_000, "folder_download.png" ),
		CATALOG_SIMPLE( 1_054_000_000, "folder_yellow.png" ), 
		CATALOG_PROF( 1_055_000_000, "folder_green.png" ), 
		CATALOG_PERS( 1_056_000_000, "folder_brown.png" ), 
		CATALOG_ADM( 1_057_000_000, "folder_red.png" ),

		COMPETENCE( 1_100_000_000, "book.png" ),
		COMPETENCE_ADD( 1_101_000_000, "book_add.png" ),
		COMPETENCE_EDIT( 1_102_000_000, "book_edit.png" ),
		COMPETENCE_DOWNLOAD( 1_103_000_000, "book_download.png" ),
		COMPETENCE_PROF( 1_104_000_000, "book_green.png" ), 
		COMPETENCE_PERS( 1_105_000_000, "book_brown.png" ), 
		COMPETENCE_ADM( 1_106_000_000, "book_red.png" ),
		
		LEVEL0( 1_150_000_000, "level0.png" ), 
		LEVEL1( 1_151_000_000, "level1.png" ), 
		LEVEL2( 1_152_000_000, "level2.png" ), 
		LEVEL3( 1_153_000_000, "level3.png" ), 
		LEVEL4( 1_154_000_000, "level4.png" ), 
		LEVEL5( 1_155_000_000, "level5.png" ),  
		
		INDICATOR( 1_160_000_000, "indicator.png"),
		INDICATOR_ADD( 1_161_000_000, "indicator_add.png"), 
		INDICATOR_EDIT( 1_162_000_000, "indicator_edit.png"),
		
		PROFILE_PATTERN( 1_170_000_000, "books.png" ), 
		PROFILE_PATTERN_ADD( 1_171_000_000, "books_add.png" ), 
		PROFILE_PATTERN_EDIT( 1_172_000_000, "books_edit.png" ),

		PROFILE( 1_180_000_000, "profile.png" ), 
		PROFILE_ADD( 1_181_000_000, "profile_add.png" ), 
		PROFILE_DELETE( 1_182_000_000, "profile_del.png" ),

		
		WORKER0( 1_190_000_000, "worker0.png" ), 
		WORKER1( 1_191_000_000, "worker1.png" ), 
		WORKER2( 1_192_000_000, "worker2.png" ), 
		WORKER3( 1_193_000_000, "worker3.png" ),   
		WORKER4( 1_194_000_000, "worker4.png" ),	
		
		MEASURE( 1_200_000_000, "measure.png" ),
		MEASURE_ADD( 1_201_000_000, "measure_add.png" ),
		MEASURE_EDIT( 1_202_000_000, "measure_edit.png" ),
		MEASURE_DELETE( 1_203_000_000, "measure_del.png" ),

		USERS( 1_210_000_000, "users.png" ),
		USERS_ADD( 1_211_000_000, "users_add.png" ),
		USERS_EDIT( 1_212_000_000, "users_edit.png" ),
		USERS_COPY( 1_213_000_000, "users_copy.png" ),
		USERS_DELETE( 1_214_000_000, "users_del.png" ),

		ALIEN( 1_220_000_000, "alien.png" ),
		ALIEN_ADD( 1_221_000_000, "alien_add.png" ),
		ALIEN_EDIT( 1_222_000_000, "alien_edit.png" ),
		ALIEN_DELETE( 1_223_000_000, "alien_del.png" ),
		
		
		ANIMATED_GEARS( 1_900_000_000, "gears_animated.gif" );
		
		
		private int ind;
		private String name;
		IType( int ind, String name ) { 
			this.ind = ind;
			this.name = name;
		}
		public int getIndex() { return ind; }
		public String getName() { return name; }
	};

	public static ImageIcon getIcon( IType icon, int size ) {
		int ind = size  + icon.getIndex();				
		StringBuilder path = new StringBuilder();
		ImageIcon img = null;
		try {
			img = ( ImageIcon ) map.get( ind );
			if ( img != null ) return img;
			path = new StringBuilder();
			path.append( "/img/" );
			if ( icon.getIndex() < 1_900_000_000 ) path.append( size ).append( "/" );
			path.append( icon.getName() );
			img = new ImageIcon( ResourceKeeper.class.getResource( path.toString() ) );	
			if ( img != null ) map.put(ind, img);			
		} catch ( Exception ex ) {				
			if ( path.length() > 0 ) path.delete( 0, path.length() );
			path.append( "ResourceKeeper.getIcon() : Icon not found : " ).append( icon.getName() )
			.append( " [size=" ).append( size ).append( "]" );
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( path.toString() );
			img = null;
		}
		return img;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T getObject( OType key ) {
		return ( T ) map.get( key.getIndex() );
	}

	public static String getQuery( QType key ) {
		return ( String ) map.get( key.getIndex() );
	}

	public static <T> void putObject( OType key, T res ) {
		map.put( key.getIndex(), res );
	}

	public static void clearAll() {
		map.clear();		
	}
	
	public static boolean containKey( OType key ) {
		return map.containsKey( key.getIndex() );
	}

	public static boolean containKey( QType key ) {
		return map.containsKey( key.getIndex() );
	}

	public static boolean containKey( IType icon, int size ) {
		return map.containsKey( icon.getIndex() + size );
	}
}