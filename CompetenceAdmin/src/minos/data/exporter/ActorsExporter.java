package minos.data.exporter;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityNotFoundException;

import minos.data.orm.OrmHelper;
import minos.data.orm.OrmHelper.QueryType;
import minos.data.services.FilialInfo;
import minos.entities.Actors;
import minos.entities.ActorsPerformance;
import minos.entities.Competence;
import minos.entities.Division;
import minos.entities.Level;
import minos.entities.OrgUnit;
import minos.entities.Person;
import minos.entities.Post;
import minos.entities.Profile;
import minos.utils.ResourceKeeper;
import minos.utils.ResourceKeeper.OType;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import ru.gedr.util.tuple.Pair;

public class ActorsExporter {
	// =================================================================================================================
	// Constants
	// =================================================================================================================
	private static final String TEST_RESULT_PATTERN		= "Test_Result";
	private static final String TEST_TYPE_PATTERN		= "Test_Type";
	private static final String TEST_MODE_PATTERN		= "Test_Mode";
	private static final String RESERVE_LEVEL_PATTERN	= "Reserve_level";
	private static final String TEST_FINISH_PATTERN		= "Test_Finish";
	private static final String COMPETENCE1_PATTERN		= "Competence_Type1";
	private static final String COMPETENCE2_PATTERN		= "Competence_Type2";
	private static final String COMPETENCE3_PATTERN		= "Competence_Type3";
	private static final String SINNNER_FIO_PATTERN		= "Person_Name";
	private static final String SINNNER_DIVISION_PATTERN= "Person_Division";
	private static final String SINNNER_POSITION_PATTERN= "Person_Position";	
	private static final String TEST_DIVISION_PATTERN	= "Test_Division";
	private static final String TEST_POSITION_PATTERN	= "Test_Position";
	private static final String PROFILE_LEVEL_PATTERN	= "profile_level";
	private static final String RESULT_LEVEL_PATTERN	= "Result_level";
	private static final String MINOS_FIO_PATTERN		= "MPerson_name";
	private static final String MINOS_POSITION_PATTERN	= "MPerson_position";	
	
	private static final String REPORT_FORM_PATH		= "/forms/ReportForm.docx";
	
	public static final String[] PATTERNS = { TEST_RESULT_PATTERN, TEST_TYPE_PATTERN, TEST_MODE_PATTERN, 
		RESERVE_LEVEL_PATTERN, TEST_FINISH_PATTERN, COMPETENCE1_PATTERN, COMPETENCE2_PATTERN, COMPETENCE3_PATTERN, 
		SINNNER_FIO_PATTERN, SINNNER_DIVISION_PATTERN, SINNNER_POSITION_PATTERN, TEST_DIVISION_PATTERN, 
		TEST_POSITION_PATTERN, MINOS_FIO_PATTERN, MINOS_POSITION_PATTERN };

	private static final Logger log = LoggerFactory.getLogger( ActorsExporter.class );
	
	// =================================================================================================================
	// Fields
	// =================================================================================================================
	private List<Pair<ActorsPerformance, Competence>> lap;
	private List<OrgUnit> lou;
	private List<Profile> lp;

	// =================================================================================================================
	// Constructors
	// =================================================================================================================
	public ActorsExporter() {
		
	}

	// =================================================================================================================
	// Getter & Setter
	// =================================================================================================================

	// =================================================================================================================
	// Methods for/from SuperClass/Interfaces
	// =================================================================================================================

	// =================================================================================================================
	// Methods
	// =================================================================================================================
	public void exportToExcel( List<Actors> la, String outFile ) {
		loadData( la );
		List<Person> lp = new ArrayList<>();
		List<Long> ll = new ArrayList<>();
		for ( Actors a : la ) {
			if ( a.getMinos() != null ) lp.add( a.getMinos() );
			if ( a.getInternalSinner() != null ) lp.add( a.getInternalSinner() );
			if ( a.getProfile() != null ) ll.add( a.getProfile().getId() );
		}
		loadPersonData(lp);
		loadProfileData( ll );

		try {
			Workbook wb = new XSSFWorkbook();
			Sheet sheet = wb.createSheet( "main" );
			makeTitle( sheet );
			int rowInd = 1;
			for ( Actors a : la ) {
				fillSheet( sheet.createRow( rowInd++ ), a );
			}

			FileOutputStream fileOut = new FileOutputStream( outFile );

			wb.write(fileOut);
			fileOut.close();

		} catch ( Exception ex ) {
			// TODO Auto-generated catch block
			ex.printStackTrace();
		}
	}

	private void makeTitle( Sheet sheet ) {
		 Row row = sheet.createRow( 0 );
		 row.createCell( 0 ).setCellValue( "Код" );
		 row.createCell( 1 ).setCellValue( "Эксперт ФИО" );
		 row.createCell( 2 ).setCellValue( "Эксперт пол" );
		 row.createCell( 3 ).setCellValue( "Эксперт возраст" );
		 row.createCell( 4 ).setCellValue( "Эксперт филиал" );
		 row.createCell( 5 ).setCellValue( "Эксперт подразделение" );
		 row.createCell( 6 ).setCellValue( "Эксперт должность" );
		 row.createCell( 7 ).setCellValue( "Эксперт тип должности" );
		 row.createCell( 8 ).setCellValue( "Оцениваемый ФИО" );
		 row.createCell( 9 ).setCellValue( "Оцениваемый пол" );
		 row.createCell( 10 ).setCellValue( "Оцениваемый возраст" );
		 row.createCell( 11 ).setCellValue( "Оцениваемый филиал" );
		 row.createCell( 12 ).setCellValue( "Оцениваемый подразделение" );
		 row.createCell( 13 ).setCellValue( "Оцениваемый должность" );
		 row.createCell( 14 ).setCellValue( "Оцениваемый тип должности" );
		 row.createCell( 15 ).setCellValue( "Профиль название" );
		 row.createCell( 16 ).setCellValue( "Профиль филиал" );
		 row.createCell( 17 ).setCellValue( "Профиль подразделение" );
		 row.createCell( 18 ).setCellValue( "Профиль должность" );
		 row.createCell( 19 ).setCellValue( "Резерв" );
		 row.createCell( 20 ).setCellValue( "Вид" );
		 row.createCell( 21 ).setCellValue( "Уровень" );
		 row.createCell( 22 ).setCellValue( "Оценка общая" );
		 row.createCell( 23 ).setCellValue( "Оценка общая (max)" );
		 row.createCell( 24 ).setCellValue( "Оценка Профессиональные компетенции" );
		 row.createCell( 25 ).setCellValue( "Оценка Профессиональные компетенции (max)" );
		 row.createCell( 26 ).setCellValue( "Оценка Личностно-деловые компетенции" );
		 row.createCell( 27 ).setCellValue( "Оценка Личностно-деловые компетенции (max)" );
		 row.createCell( 28 ).setCellValue( "Оценка Управленческие компетенции" );
		 row.createCell( 29 ).setCellValue( "Оценка Управленческие компетенции (max)" );	
	}
	
	private void fillSheet( Row row, Actors a ) {
		if ( a == null ) return;
		row.createCell( 0 ).setCellValue( a.getId() );
		row.createCell( 1 ).setCellValue( a.getMinos() == null ? "null" : a.getMinos().getFullName() );
		row.createCell( 2 ).setCellValue( a.getMinos() == null ? "null" : a.getMinos().getSex() );
		row.createCell( 3 ).setCellValue( a.getMinos() == null ? 0 : a.getMinos().getAge( null ) );
		row.createCell( 4 ).setCellValue( getFilial( a.getMinos() ) );
		
		OrgUnit ou = findPerson( a.getMinos() );

		row.createCell( 5 ).setCellValue( ( ( ou == null ) || ( ou.getDivision() == null ) ) ? "null" 
				: ou.getDivision().getFullName() );
		row.createCell( 6 ).setCellValue( ( ( ou == null ) || ( ou.getPost() == null ) ) ? "null" 
				: ou.getPost().getName() );
		row.createCell( 7 ).setCellValue( ( ( ou == null ) || ( ou.getPost() == null ) ) ? "null" 
				: getPostType( ou.getPost() ) );

		row.createCell( 8 ).setCellValue( a.getInternalSinner() == null ? "null" : a.getInternalSinner().getFullName() );
		row.createCell( 9 ).setCellValue( a.getInternalSinner() == null ? "null" : a.getInternalSinner().getSex() );
		row.createCell( 10 ).setCellValue( a.getInternalSinner() == null ? 0 : a.getInternalSinner().getAge( null ) );
		row.createCell( 11 ).setCellValue( getFilial( a.getInternalSinner() ) );

		ou = findPerson( a.getInternalSinner() );

		row.createCell( 12 ).setCellValue( ( ( ou == null ) || ( ou.getDivision() == null ) ) ? "null" 
				: ou.getDivision().getFullName() );
		row.createCell( 13 ).setCellValue( ( ( ou == null ) || ( ou.getPost() == null ) ) ? "null" 
				: ou.getPost().getName() );
		row.createCell( 14 ).setCellValue( ( ( ou == null ) || ( ou.getPost() == null ) ) ? "null" 
				: getPostType( ou.getPost() ) );
		
		Profile prfl = ( !lp.contains( a.getProfile() ) ? null : lp.get( lp.indexOf( a.getProfile() ) ) );

		row.createCell( 15 ).setCellValue( ( ( prfl == null ) || ( prfl.getProfilePattern() == null ) ) ? "null" 
				: prfl.getProfilePattern().getName() );
		row.createCell( 16 ).setCellValue( prfl == null ? "null" 
				: ( ( prfl.getDivision() != null ) ? getFilial( prfl.getDivision() )
						: ( ( prfl.getEstablishedPost() == null ) ? "null" 
								: getFilial( prfl.getEstablishedPost().getDivision() ) ) ) );
		row.createCell( 17 ).setCellValue( prfl == null ? "null" 
				: ( ( prfl.getDivision() != null ) ? prfl.getDivision().getFullName()  
						: ( ( prfl.getEstablishedPost() == null ) ? "null" 
								: ( ( prfl.getEstablishedPost().getDivision() == null ) ? "null" 
										: prfl.getEstablishedPost().getDivision().getFullName() ) ) ) );
		row.createCell( 18 ).setCellValue( prfl == null ? "null" 
				: ( ( prfl.getPost() != null ) ? prfl.getPost().getName()  
						: ( ( prfl.getEstablishedPost() == null ) ? "null" 
								: prfl.getEstablishedPost().getName() ) ) );
		row.createCell( 19 ).setCellValue( a.getReserveType() == null ? "null" : a.getReserveType().getName() );
		row.createCell( 20 ).setCellValue( a.getTestMode() == null ? "null" : a.getTestMode().getName() );
		row.createCell( 21 ).setCellValue( a.getReserveLevel() == null ? "null" : a.getReserveLevel().getName() );

		double[] res = calcTestResult( a );
		double[] max = calcMaxTestResult( a );
		row.createCell( 22 ).setCellValue( res[0] );
		row.createCell( 23 ).setCellValue( max[0] );
		row.createCell( 24 ).setCellValue( res[1] );
		row.createCell( 25 ).setCellValue( max[1] );
		row.createCell( 26 ).setCellValue( res[2] );
		row.createCell( 27 ).setCellValue( max[2] );
		row.createCell( 28 ).setCellValue( res[3] );
		row.createCell( 29 ).setCellValue( max[3] );
	}
	
	private String getFilial( Person p ) {
		if ( p == null ) return "null";
		List<FilialInfo> lfi = ResourceKeeper.getObject( OType.BRANCH_OFFICES_INFO );
		for ( FilialInfo fi : lfi ) {
			if ( ( 0 < fi.getCode() ) && ( fi.getCode() < 100 ) && ( fi.getPersonCodeMin() <= p.getId() ) 
					&& ( p.getId() <= fi.getPersonCodeMax() ) ) return fi.getName();
		}
		return "unknown";
	}

	private String getFilial( Division d ) {
		if ( d == null ) return "null";
		List<FilialInfo> lfi = ResourceKeeper.getObject( OType.BRANCH_OFFICES_INFO );
		for ( FilialInfo fi : lfi ) {
			if ( ( 0 < fi.getCode() ) && ( fi.getCode() < 100 ) && ( fi.getDivisionCodeMin() <= d.getId() ) 
					&& ( d.getId() <= fi.getDivisionCodeMax() ) ) return fi.getName();
		}
		return "unknown";
	}

	private String getPostType( Post p ) {
		if ( p == null ) return "null";
		List<Pair<String, Byte>> lpsb = ResourceKeeper.getObject( OType.BASE_POST_LIST );
		for ( Pair<String, Byte> psb : lpsb ) {
			byte b = ( byte ) ( p.getKpers() / 10 );
			if ( b == psb.getSecond().byteValue() ) return psb.getFirst();
		}
		return "unknown";
	}
	
	
	public void exportToWord( List<Actors> la, String outdir ) {
		loadData( la );
		List<Person> lp = new ArrayList<>();
		List<Long> ll = new ArrayList<>();
		for ( Actors a : la ) {
			if ( a.getMinos() != null ) lp.add( a.getMinos() );
			if ( a.getInternalSinner() != null ) lp.add( a.getInternalSinner() );
			if ( a.getProfile() != null ) ll.add( a.getProfile().getId() );
		}
		loadPersonData(lp);
		loadProfileData( ll );

		try {
			if ( ( log != null ) && log.isDebugEnabled() ) log.debug( "open and save pattern file" );
			XWPFDocument patternDoc = new XWPFDocument( getClass().getResourceAsStream( REPORT_FORM_PATH ) );
			
			java.util.Date now = new java.util.Date(); 
			for ( Actors a : la ) {
				if ( ( a == null ) || ( a.getAssembly() == null ) || now.before( a.getAssembly() ) ) continue;
				
				String outputFileName = outdir + a.getId() + "-" + a.getMinos().getSurnameAndInitials( false, false ) 
						+ "-" + ( a.getSinnerType() == Actors.SINNER_TYPE_INNER 
						? a.getInternalSinner().getSurnameAndInitials( false, false ) : "other" ) + "-" 
						+ System.currentTimeMillis() + ".docx";				
				patternDoc.write( new FileOutputStream( outputFileName ) );
				if ( ( log != null ) && log.isDebugEnabled() ) log.debug( "open export's file" );			
				XWPFDocument workDoc = new XWPFDocument( new FileInputStream( outputFileName ) );
				List<XWPFTableRow> rows = getWordTableRows( workDoc );
				Preconditions.checkNotNull( rows, "Word Table Rows not found" );

				fillTable( rows, a );
				removesSuperfluous( workDoc.getTables().get( 0 ) );
				
				workDoc.write( new FileOutputStream( outputFileName ) );
				if ( ( log != null ) && log.isDebugEnabled() ) log.debug( "file unload successfully" );
			}
		} catch ( Exception ex ) {
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( "printToWord()", ex );
		}		
	}
	
	/**
	 * поиск в строке ¤чейки с полем содержащим маркер pattern
	 * @return
	 */
	private int findCompetenceTestResult( XWPFTableRow row, String pattern ) {
		List<XWPFTableCell> cells = row.getTableCells();
		for ( int i = 0; i < cells.size(); i++ ) {
			if ( getTextFromCell( cells.get( i ) ).equalsIgnoreCase( pattern ) ) return i;
		}
		return -1;
	}
	
	private String getTextFromCell( XWPFTableCell cell ) {
		if ( cell == null ) return "";
		try {
			String s = cell.getText();
			return s;
		} catch ( Exception ex ) { 
			// if ( ( log != null ) && log.isErrorEnabled() ) log.error( "getTextFromCell()" );
		}
		return "";
	}
	
	private void loadData( List<Actors> la ) {
		if ( ( la == null ) || ( la.size() == 0 ) ) return;
			
		
		String jpql1 = " SELECT ap, cmt FROM ActorsPerformance ap, Competence cmt "
				+ " INNER JOIN FETCH ap.actors "
				+ " INNER JOIN FETCH ap.profilePatternElement "
				+ " INNER JOIN FETCH ap.profilePatternElement.minLevel "
				+ " WHERE ap.actors IN (:actors)"
				+ " AND ( ( cmt = ap.profilePatternElement.competence "
				+ "         OR cmt.ancestor = ap.profilePatternElement.competence ) "
				+ "     AND ( ap.profilePatternElement.profilePattern.timePoint BETWEEN cmt.journal.editMoment "
				+ "           AND cmt.journal.deleteMoment ) ) ";
		
		List<Object[]> los = OrmHelper.findByQueryWithParam( QueryType.JPQL, jpql1, Object[].class, 
				new Pair<Object, Object>( "actors", la ) );
		if ( ( los == null ) || ( los.size() == 0 ) ) throw new EntityNotFoundException( "ActorsPerformance not found" );
		
		lap = new ArrayList<>();
		for ( Object[] o : los ) lap.add( new Pair<>( ( ActorsPerformance ) o[0], ( Competence ) o[1] ) );

	}
	
	private void loadPersonData( List<Person> lp ) {
		String jpql = " SELECT entity "
				+ " FROM OrgUnit entity "
				+ " INNER JOIN FETCH entity.person "
				+ " INNER JOIN FETCH entity.division "
				+ " INNER JOIN FETCH entity.post "
				+ " WHERE entity.person IN (:persons) "
				+ " AND entity.person.status IN (:stats) "
				+ " AND ( CURRENT_TIMESTAMP BETWEEN entity.person.beginDate AND entity.person.endDate) ";

		lou = OrmHelper.findByQueryWithParam( QueryType.JPQL, jpql, OrgUnit.class, 
				new Pair<Object, Object>( "persons", lp ), 
				new Pair<Object, Object>( "stats", Arrays.asList( Person.STATUS_ACTIVE ) ) );		
		if ( ( lou == null ) || ( lou.size() == 0 ) ) throw new EntityNotFoundException( "ActorsPerformance not found" );
		/*
		for ( OrgUnit ou : lou ) {
			if ( ou.getPerson() != null ) System.out.println( ou.getPerson().getFullName() );
			if ( ou.getDivision() != null ) System.out.println( ou.getDivision().getFullName() );
			if ( ou.getPost() != null ) System.out.println( ou.getPost().getName() );
		}
		*/
	}
	
	private void loadProfileData( List<Long> ll ) {
		String jpql = " SELECT entity "
				+ " FROM Profile entity "
				+ " INNER JOIN FETCH entity.profilePattern "
				+ " LEFT  JOIN FETCH entity.establishedPost "
				+ " LEFT  JOIN FETCH entity.establishedPost.division "
				+ " LEFT  JOIN FETCH entity.post "
				+ " LEFT  JOIN FETCH entity.division "
				+ " WHERE entity.id IN (:pids) ";

		lp = OrmHelper.findByQueryWithParam( QueryType.JPQL, jpql, Profile.class, 
				new Pair<Object, Object>( "pids", ll ) ); 
		if ( ( lp == null ) || ( lp.size() == 0 ) ) throw new EntityNotFoundException( "ActorsPerformance not found" );
		/*
		for ( Profile p : lp ) {
			if ( p.getEstablishedPost() != null ) System.out.println( "profile for " + 
					p.getEstablishedPost().getName() + " : " + 
					p.getEstablishedPost().getDivision().getFullName() );
		}
		*/
	}
	
	private OrgUnit findPerson( Person p ) {
		if ( p == null ) return null;
		for ( OrgUnit ou : lou ) {
			if ( ou.getPerson().equals( p ) ) return ou;
		}
		return null;
	}
	
	private int findCompetence( Actors a, short variety, int pos ) {
		int offs = 0;
		for ( int i = 0; i < lap.size(); i++ ) {
			Pair<ActorsPerformance, Competence> ap = lap.get( i );
			if ( ap.getFirst().getActor().equals( a ) && ( ap.getSecond().getVariety() == variety ) ) {
				if ( offs == pos ) return i;
				offs++;
			}
		}
		return -1;			
	}
	
	
	
	/**
	 * clear cell, use only one Paragraph and one Run in them 
	 * @param cell
	 */
	private void clearParagraphsAndRuns( XWPFTableCell cell ) {
		List<XWPFParagraph> cellParagraphs = cell.getParagraphs();		
		//while ( cellParagraphs.get( 0 ).getRuns().size() > 1 ) cellParagraphs.get( 0 ).removeRun (  1) ;
		while ( cellParagraphs.size() > 1 ) cellParagraphs.remove( 1 );

		List<XWPFRun> runs = cellParagraphs.get( 0 ).getRuns();
		while ( runs.size() > 1 ) cellParagraphs.get( 0 ).removeRun( runs.size() - 1 );
	}
	
	/**
	 * get rows of table[0] in word pattern
	 * @param outFileName - output report file 
	 * @return list of XWPFTableRow or null
	 */
	private List<XWPFTableRow> getWordTableRows( XWPFDocument doc ) {
		try {			
			List<XWPFTable> tbls = doc.getTables();
			return tbls.get(0).getRows();			
		} catch ( Exception ex ) {
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( "getWordTableRows", ex );
			return null;
		}
	}

	
	private void setTextInWordTableCell( XWPFTableCell cell, String str ) {
		try {
			cell.getParagraphs().get( 0 ).getRuns().get( 0 ).setText( str, 0 );
		} catch ( Exception ex ) {
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( "setTextInWordTableCell() : " + str, ex );
		}
	}
	
	
	private void paintCell( XWPFTableRow row, String pattern, int count ) {
		if( (row == null) || (pattern == null) ) return;
		int pos  = findCompetenceTestResult( row, pattern );
		if ( pos < 0 ) return;
		
		clearParagraphsAndRuns(row.getCell(pos));
		XWPFRun run = row.getCell(pos).getParagraphs().get(0).getRuns().get(0);
		String color = run.getColor();
		run.setText(" ", 0);
		
		for(int i = 0; i < count; i++)
			row.getCell(pos + i).setColor(color);
	}

	private double[] calcTestResult( Actors a ) {
		double[] min = { 0.0D, 0.0D, 0.0D, 0.0D };
		double[] res = { 0.0D, 0.0D, 0.0D, 0.0D };
		for ( Pair<ActorsPerformance, Competence> p : lap ) {
			if( ( p == null ) || ( p.getFirst() == null ) || ( p.getFirst().getProfilePatternElement() == null ) 
					|| ( p.getFirst().getProfilePatternElement().getMinLevel() == null ) ) continue;
			if ( p.getFirst().getActor().equals( a ) ) {
				res[0] += p.getFirst().getCost();
				min[0] += p.getFirst().getProfilePatternElement().getMinLevel().getPrice();
				res[p.getSecond().getVariety()] += p.getFirst().getCost();
				min[p.getSecond().getVariety()] += p.getFirst().getProfilePatternElement().getMinLevel().getPrice();

			}
		}
		for ( int i = 0; i < res.length; i++ ) res[i] = ( ( min[i] == 0.0D ) ? 0.0D : res[i] / min[i] * 100 );
		return res;
	}

	private double[] calcMaxTestResult( Actors a ) {
		double[] max = { 0.0D, 0.0D, 0.0D, 0.0D };
		double[] res = { 0.0D, 0.0D, 0.0D, 0.0D };
		
		List<Level> ll = ResourceKeeper.getObject( OType.LEVELS_CACHE );
		double maxPrice = 0.0D;
		for ( Level l : ll )  {
			if ( l.getPrice() > maxPrice ) maxPrice = l.getPrice();
		}
		for ( Pair<ActorsPerformance, Competence> p : lap ) {
			if( ( p == null ) || ( p.getFirst() == null ) || ( p.getFirst().getProfilePatternElement() == null ) 
					|| ( p.getFirst().getProfilePatternElement().getMinLevel() == null ) ) continue;
			if ( p.getFirst().getActor().equals( a ) ) {
				res[0] += p.getFirst().getCost();
				max[0] += maxPrice;
				res[p.getSecond().getVariety()] += p.getFirst().getCost();
				max[p.getSecond().getVariety()] += maxPrice;

			}
		}
		for ( int i = 0; i < res.length; i++ ) res[i] = ( ( max[i] == 0.0D ) ? 0.0D : res[i] / max[i] * 100 );
		return res;
	}

	
	private void fillTable( List<XWPFTableRow> rows, Actors a ) {
		if ( ( rows == null ) || ( a == null ) ) return;
		double commonResult = calcTestResult( a )[0];

		int competence1Number = 0;
		int competence2Number = 0;
		int competence3Number = 0;
		
		OrgUnit sou = ( a.getSinnerType() == Actors.SINNER_TYPE_ALIEN ? null : findPerson( a. getInternalSinner() ) );
		OrgUnit mou = findPerson( a. getMinos() );
		Profile prfl = ( !lp.contains( a.getProfile() ) ? null : lp.get( lp.indexOf( a.getProfile() ) ) );

		for ( int i= 0; i < rows.size(); i++ ) {
			boolean found = false;

			for ( String s : PATTERNS ) {				
				int num = findCompetenceTestResult( rows.get( i ), s );
				String txt;
				if ( num < 0 ) continue;
				
				clearParagraphsAndRuns( rows.get( i ).getCell( num ) );
				switch(s) {
				case SINNNER_FIO_PATTERN:
					if ( a.getSinnerType() == Actors.SINNER_TYPE_INNER ) {
						txt = ( a.getInternalSinner() == null ? "null" : a.getInternalSinner().getFullName() ); 
					} else {
						txt = ( a.getAlienSinner() == null ? "null" : a.getAlienSinner().getName() );
					}
					setTextInWordTableCell( rows.get(i).getCell( num ), txt );
					found = true;
					break;							

				case SINNNER_DIVISION_PATTERN:
					if( a.getSinnerType() == Actors.SINNER_TYPE_ALIEN ) {
						txt = "Неизвестно";
					} else {
						txt = ( ( sou == null ) || ( sou.getDivision() == null ) ? "null" 
								: sou.getDivision().getFullName() );
					}
					setTextInWordTableCell( rows.get(i).getCell( num ), txt );				
					found = true;
					break;							

				case SINNNER_POSITION_PATTERN:				
					if( a.getSinnerType() == Actors.SINNER_TYPE_ALIEN ) {
						txt = "Неизвестно";
					} else {
						txt = ( ( sou == null ) || ( sou.getPost() == null ) ? "null" : sou.getPost().getName() );
					}
					setTextInWordTableCell( rows.get(i).getCell( num ), txt );	
					found = true;
					break;							

				case TEST_DIVISION_PATTERN:
					txt = ( prfl == null ? "null" 
							: ( ( prfl.getDivision() != null ) ? prfl.getDivision().getFullName()  
									: ( ( prfl.getEstablishedPost() == null ) ? "null" 
											: ( ( prfl.getEstablishedPost().getDivision() == null ) ? "null" 
													: prfl.getEstablishedPost().getDivision().getFullName() ) ) ) ); 
					setTextInWordTableCell( rows.get(i).getCell( num ), txt );
					found = true;
					break;							

				case TEST_POSITION_PATTERN:
					txt = ( prfl == null ? "null" 
							: ( ( prfl.getPost() != null ) ? prfl.getPost().getName()  
									: ( ( prfl.getEstablishedPost() == null ) ? "null" 
											: prfl.getEstablishedPost().getName() ) ) );
					setTextInWordTableCell( rows.get(i).getCell( num ), txt );
					found = true;
					break;							

				case TEST_TYPE_PATTERN:
					txt = a.getReserveType().getName();
					setTextInWordTableCell( rows.get(i).getCell( num ), txt );
					found = true;
					break;
					
				case TEST_MODE_PATTERN:
					txt = a.getTestMode().getName();
					setTextInWordTableCell( rows.get(i).getCell( num ), txt );
					found = true;
					break;

				case RESERVE_LEVEL_PATTERN:
					txt = a.getReserveLevel().getName();
					setTextInWordTableCell( rows.get(i).getCell( num ), txt );
					found = true;
					break;

				case COMPETENCE1_PATTERN:
					outCompetence(rows, i, num, a, ( short ) 1, competence1Number++ );
					found = true;
					break;

				case COMPETENCE2_PATTERN:
					outCompetence(rows, i, num, a, ( short ) 2, competence2Number++ );
					found = true;
					break;

				case COMPETENCE3_PATTERN:
					outCompetence(rows, i, num, a, ( short ) 3, competence3Number++ );
					found = true;
					break;

				case TEST_RESULT_PATTERN:
					setTextInWordTableCell( rows.get(i).getCell( num ), String.format( "%4.1f", commonResult ) + " %" );
					found = true;
					break;
					
				case TEST_FINISH_PATTERN :
					java.util.Date dt = new java.util.Date();
					setTextInWordTableCell( rows.get(i).getCell( num ), String.format( "%1$td .%1$tm.%1$tY", 
							( dt.before( a.getFinish() ) ? dt : a.getFinish() ) ) );
					found = true;
					break;
					
				case MINOS_FIO_PATTERN :
					txt = ( a.getMinos() == null ? "null" : a.getMinos().getFullName() );
					setTextInWordTableCell( rows.get(i).getCell( num ), txt );
					found = false;
					break;

				case MINOS_POSITION_PATTERN :
					txt = ( ( mou == null ) || ( mou.getPost() == null ) ? "null" : mou.getPost().getName() );
					setTextInWordTableCell( rows.get(i).getCell( num ), txt );	
					found = false;
					break;							
				}
				if ( found ) break;
			}
		}
	}
	
	private void outCompetence( List<XWPFTableRow> rows, int startRow, int cellName, Actors a, 
			short variety, int offs ) {
		int pos = findCompetence( a, variety, offs );
		if ( pos < 0 ) return;
		Pair<ActorsPerformance, Competence> ap = lap.get( pos );
		setTextInWordTableCell( rows.get( startRow ).getCell( cellName ), ap.getSecond().getName() );
		paintCell(rows.get( startRow ), RESULT_LEVEL_PATTERN, ( int ) ap.getFirst().getCost() );
		paintCell(rows.get( startRow ), PROFILE_LEVEL_PATTERN, ( int ) ap.getFirst().getProfilePatternElement()
				.getMinLevel().getPrice() );
		paintCell(rows.get( startRow + 1 ), RESULT_LEVEL_PATTERN, ( int ) ap.getFirst().getCost() );
		paintCell(rows.get( startRow + 1 ), PROFILE_LEVEL_PATTERN, ( int ) ap.getFirst().getProfilePatternElement()
				.getMinLevel().getPrice() );
	}

	private void removesSuperfluous( XWPFTable tbl ) {
		List<XWPFTableRow> rows = tbl.getRows();
		if ( rows == null ) return;
		
		int rowCount = rows.size();
		int pos = 0;
		boolean fRemove = false;
		
		String[] ptrnsFull = Arrays.copyOf( PATTERNS, PATTERNS.length + 2 );
		ptrnsFull[PATTERNS.length] = RESULT_LEVEL_PATTERN;
		ptrnsFull[PATTERNS.length + 1] = PROFILE_LEVEL_PATTERN;
				
		while ( pos < rowCount ) {
			fRemove = false;
		
			for( String s : ptrnsFull ) {
				if ( findCompetenceTestResult( rows.get( pos ), s ) >= 0 ) {
					fRemove = true;
					break;
				}
			}
			if ( fRemove ) {
				tbl.removeRow( pos );
				rowCount--;
				pos = 0;
				continue;
			}
			pos++;		
		}
	}

}
