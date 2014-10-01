package minos.data.importer;

import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.gedr.util.tuple.Pair;
import minos.entities.Catalog;
import minos.entities.Competence;
import minos.entities.Indicator;
import minos.entities.Level;
import minos.entities.PpeStrAtr;
import minos.entities.ProfilePattern;
import minos.entities.ProfilePatternElement;
import minos.entities.StringAttr;
import minos.utils.ResourceKeeper;
import minos.utils.ResourceKeeper.OType;

public class FileImporter {
	// =================================================================================================================
	// Constants
	// =================================================================================================================
	public static final String COMPETENCE_START = "$";
	public static final String PROFILE_START = "%";

	// =================================================================================================================
	// Fields
	// =================================================================================================================
	private static Logger log = LoggerFactory.getLogger( FileImporter.class );
	private List<Level> levels;
	private String competenceStart = COMPETENCE_START;
	private String profileStart = PROFILE_START;

	// =================================================================================================================
	// Constructors
	// =================================================================================================================
	public FileImporter() {
		levels = ResourceKeeper.getObject( OType.LEVELS_CACHE );
		if ( levels == null ) throw new NullPointerException( "FileImporter() : not find LEVELS_CACHE resource" );
		competenceStart = COMPETENCE_START;
		profileStart = PROFILE_START;
	}

	// =================================================================================================================
	// Getter & Setter
	// =================================================================================================================
	public String getCompetenceStart() {
		return competenceStart;
	}

	public void setCompetenceStart( String competenceStart ) {
		if ( ( competenceStart == null ) || competenceStart.trim().isEmpty()  ) {
			throw new IllegalArgumentException( "FileImporter.setCompetenceStart()" ); 
		}
		this.competenceStart = competenceStart;
	}

	public String getProfileStart() {
		return profileStart;
	}

	public void setProfileStart( String profileStart ) {
		if ( ( profileStart == null ) || profileStart.trim().isEmpty()  ) {
			throw new IllegalArgumentException( "FileImporter.setProfileStart()" ); 
		}
		this.profileStart = profileStart;
	}

	// =================================================================================================================
	// Methods
	// =================================================================================================================
	/**
	 * This function load TXT file. The TXT file contain competence's names, description, indicators, 
	 * and may contain a command instruction for initialize profiles
	 * @param file - java.nio.Pah for load file (required)
	 * @param prntCatalog - is parent catalog for new competence (required)
	 * @return Pair<Catalog, Catalog> - first catalog contain hierarchy catalogs and competences
	 * 									second catalog contain hierarchy catalogs and ProfilePatterns  
	 */
	public Pair<Catalog, Catalog> loadTXTFile( Path file, Catalog prntCatalog ) throws Exception {		
		if( ( file == null ) || ( prntCatalog == null ) || ( prntCatalog.getVariety() == Catalog.EMPTY )  
				|| ( prntCatalog.getStatus() != Catalog.STATUS_ACTIVE ) ) {
			throw new IllegalArgumentException( "FileImporter.loadTXTFile() : params have illegal value" );
		}		
		try ( BufferedReader reader = Files.newBufferedReader( file, Charset.forName( "UTF-8" ) ) ) {
			String line = null;
			short item = 0;
			int step = -1; // state-machine
			Catalog rootCC = new Catalog( prntCatalog.getName(), prntCatalog.getItem(), prntCatalog.getStatus(), 
					prntCatalog.getVariety(), prntCatalog.getVersion(), null, null, null, ( List<Competence> )null, null );
			rootCC.setId( prntCatalog.getId() );
			Catalog rootPPC = new Catalog( "", ( short ) 1, Catalog.STATUS_ACTIVE, Catalog.EMPTY, ( short ) 1, null, 
					null, null, null, ( List<ProfilePattern> )null );
			rootPPC.setId( 1 );
			
			Catalog competenceCatalog = null; // current competence catalog
			Competence competence = null;
			ProfilePatternElement ppe = null;

			while ( ( line = reader.readLine() ) != null ) {
				if ( line.contains( competenceStart ) ) {
					competenceCatalog = parseCompetenceCmdLine( rootCC, line );
					competence = null;
					ppe = null;
					step = 1;
					continue;
				}
				if ( line.contains( profileStart ) ) {
					ppe = parseProfilePatternCmdLine( rootPPC, line );
					if ( ( ppe != null ) && ( ppe.getCompetence() == null ) 
							&& ( competence != null ) ) ppe.setCompetence( competence );
					continue;
				}
				if ( line.trim().isEmpty() ) { 
					step++;
					item = 1;
					continue;
				}
				if ( step == 1 ) {
					competence = new Competence( line, null, ( short ) 0, Competence.STATUS_ACTIVE, 
							competenceCatalog.getVariety(), ( short ) 1, competenceCatalog, null, null, null );
					competenceCatalog.addCompetence( competence );
					if ( ( ppe != null ) && ( ppe.getCompetence() == null ) ) ppe.setCompetence( competence );
					continue;
				}
				if ( ( step == 2 ) && ( competence != null ) ) {
					if ( ( competence.getDescription() != null ) && !competence.getDescription().trim().isEmpty() ) {
						line = competence.getDescription() + "\n" + line;
					}
					competence.setDescription( line );
					continue;					
				}
				if ( ( 3 <= step ) && ( step < 3 + Level.LEVEL_COUNT ) && ( competence != null ) ) {
					Indicator ind = new Indicator( line, item++, Indicator.STATUS_ACTIVE, ( short ) 1, 
							competence, levels.get( step - 3 ), null, null );
					competence.addIndicator( ind );
					continue;					
				}
				if ( ( step == ( 3 + Level.LEVEL_COUNT ) ) && ( ppe != null )  && ( competence != null ) ) {	
					StringAttr strAttr = new StringAttr( null, line, 1, 
							StringAttr.VARIETY_POFILE_PATTERN_ELEMENT_ATTRIBUTE, null, null );
					PpeStrAtr psa = new PpeStrAtr( strAttr, ppe, item++, null );
					ppe.addAttributes( psa );
				}
			}
			return new Pair<Catalog, Catalog>( rootCC, rootPPC );
		} catch( Exception ex ) {
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( "FileImporter.loadTXTFile() : ", ex );
			throw ex;
		} 
	}

	/**
	 * parse Competence cmd line ( sample "$ catalog/subcatalog/subsubcatalog" )
	 * @param root is catalog for insert competence ( when user select in JTree component)
	 * @param line is string ( may contain hierarchy of catalog as "catalog/catalog/catalog"
	 * @return parent catalog for competence 
	 */
	private Catalog parseCompetenceCmdLine( Catalog root, String line ) {
		int offs = line.indexOf( competenceStart ) + competenceStart.length();
		List<String> ls = ( offs >= line.length() ? Collections.<String>emptyList() 
				: betokenBySlash( line.substring( offs ) ) ); 
		return makeSubCatalogs( root, ls );
	}
	
	/**
	 * parse ProfilePattern cmd line ( sample "% catalog/subcatalog/subsubcatalog/ProfilePatternName:MinLevel" )
	 * @param root is catalog for insert element
	 * @param line is string line for parse
	 * @return ProfilePatternElement object
	 */
	private ProfilePatternElement parseProfilePatternCmdLine( Catalog root, String line ) {
		int offs = line.indexOf( profileStart ) + profileStart.length();

		List<String> ls = ( offs >= line.length() ? Collections.<String>emptyList() : 
			betokenBySlash( line.substring( offs ) ) );
		if ( ls.size() == 0 ) return null;
		if ( ls.size() > 1 ) root = makeSubCatalogs( root, ls.subList( 0, ls.size() - 1 ) );
		List<String> lss = betokenByColon( ls.get( ls.size() - 1 ) );
		return makePPE( root, lss );
	}
	
	/**
	 * betoken by splash '/'
	 * @param str input sting as "element1/element2/element3"
	 * @return list as List( element1, element2, element3 )
	 */
	private List<String> betokenBySlash( String str ) {
		if ( ( str == null ) || str.trim().isEmpty() ) return Collections.emptyList();
		List<String> ls = Collections.emptyList();
		StringTokenizer st = new StringTokenizer( str, "/" );
		while ( st.hasMoreTokens() ){
			String s = st.nextToken();
			if ( ( s == null ) || s.trim().isEmpty() ) continue;
			if ( ls.size() == 0 ) ls = new ArrayList<>();
			ls.add( s );
		}
		return ls;
	}

	/**
	 * betoken by colon ':'
	 * @param str input sting as "element1:element2:element3"
	 * @return list as List( element1, element2, element3 )
	 */
	private List<String> betokenByColon( String str ) {
		List<String> ls = Collections.emptyList();
		StringTokenizer st = new StringTokenizer( str, ":" );

		while ( st.hasMoreTokens() ){
			String s = st.nextToken();
			if ( ( s == null ) || s.trim().isEmpty() ) continue;
			if ( ls.size() == 0 ) ls = new ArrayList<>();
			ls.add( s.trim() );
		}
		return ls;
	}
	
	/**
	 * make hierarchy subcatalogs for root catalog
	 * @param root is root catalog element
	 * @param names - list names subcatalogs
	 * @return
	 */
	private Catalog makeSubCatalogs( Catalog root, List<String> names ) {
		if ( ( names == null ) || ( names.size() == 0 ) ) return root;
		Catalog top = root;
		for ( String s : names ) {
			if ( ( s == null ) || s.trim().isEmpty() ) continue;
			List<Catalog> sc = top.getSubCatalogs();
			if ( ( sc != null ) && ( sc.size() > 0 ) ) {
				boolean found = false;
				for ( Catalog c : sc ) {
					if ( c.getName().equals( s ) ) {
						found = true;
						top = c;
						break;
					}
				}
				if ( found ) continue;
			}
			Catalog nc = new Catalog( s, ( short ) 1, Catalog.STATUS_ACTIVE, top.getVariety(), ( short ) 1, top, null, 
					null, null, ( List<ProfilePattern> ) null );
			top.addSubCatalog( nc );
			top = nc;
		}
		return top;
	}
	
	/**
	 * make ProfilePatternElement
	 * @param parent is parent catalog for ProfilePattern object
	 * @param lst - first element is name of ProfilePattern, 
	 * 				second element must be number - for min level ProfilePattern
	 * @return
	 */
	private ProfilePatternElement makePPE( Catalog parent, List<String> lst ) {
		if ( ( parent == null ) || ( lst == null ) || ( lst.size() == 0 ) || ( lst.size() > 2 ) 
				|| ( lst.get( 0 ) == null ) || lst.get( 0 ).trim().isEmpty() ) return null;
		Level l = levels.get( 0 );
		try {
			int i = Integer.valueOf( lst.get( 1 ) );
			l = levels.get( i < 0 ? 0 : ( i >= Level.LEVEL_COUNT ? Level.LEVEL_COUNT - 1 : ( i - 1 ) ) );
		} catch ( Exception ex ) {
			if ( ( log != null ) && log.isErrorEnabled() ) log.error( "FileImporter.makePP() : ", ex );
		}
		List<ProfilePattern> lpp = parent.getProfilePatterns();
		ProfilePattern pp = null;
		if ( ( lpp != null ) && ( lpp.size() > 0 ) ) {
			for ( ProfilePattern p : lpp ) {
				if ( p.getName().equals( lst.get( 0 ) ) ) {
					pp = p;
					break;
				}
			}
		}
		if ( pp == null ) {
			pp = new ProfilePattern( lst.get( 0 ), null, new byte[] { 2 }, 1, ProfilePattern.STATUS_BUILDING, null, 
					( Timestamp ) ResourceKeeper.getObject( OType.DAMNED_FUTURE ), parent, null );
			parent.addProfilePattern( pp );
		}
		ProfilePatternElement ppe = new ProfilePatternElement( ( short ) 0, ProfilePatternElement.STATUS_BUILDING, 
				null, l, pp, null );
		pp.addProfilePatternElement( ppe );
		return ppe;
	}
}