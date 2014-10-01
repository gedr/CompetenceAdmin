 package minos.utils;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import minos.entities.Person;
import minos.entities.Role;
import minos.ui.panels.ProfilePatternPanel;
import minos.utils.ResourceKeeper.OType;

public class Permission {	
	private static final Logger log = LoggerFactory.getLogger( ProfilePatternPanel.class );
	
	private Role role;
	private boolean changeFlag;

	public static enum Block { 
		COMMON_CATALOG( 4 * 0 ), PROFESSIONAL_CATALOG( 4 * 1 ), 
		PERSONALITY_CATALOG( 4 * 2 ), ADMINISTRATIVE_CATALOG( 4* 3 ),
		
		PROFESSIONAL_COMPETENCE( 4 * 4 ), PERSONALITY_BUSINESS_COMPETENCE( 4 * 5 ),  
		ADMINISTRATIVE_COMPETENCE( 4 * 6 ),

		PROFILE_PATTERN( 4 * 7 ), PROFILE_PATTERN_ELEMENT( 4 * 8 ), STR_ATTR( 4 * 9 ), 
		PROFILE( 4 * 10 ),
		
		MEASURE_INNER( 4 * 11 ), MEASURE_OUTER( 4 * 12), ACTORS_INNER( 4 * 13 ), ACTORS_OUTER( 4 * 14 ),
		PERFORMANCE_INNER( 4 * 15 ), PERFORMANCE_OUTER( 4 * 16 ), 
		
		ROLE( 4 * 17 ), LOGIN( 4 * 18 ), ACTORS_INFO( 4 * 19 ), DIVISION( 4 * 20 ), EPOST( 4 * 21 ),
		ACTIVATE_PROFILE_PATTERN( 4 * 22 ); 
		
		private int value;
		Block( int value ) { this.value = value; }
		public int getValue() { return value; }
	};
	
	public static enum Operation { 
		CREATE( 3 ), READ( 2 ), UPDATE( 1 ), DELETE( 0 );
		private int value;
		Operation( int value ) { this.value = value ; }
		public int getValue() { return value; }
	};
	
	public Permission( Role role ) {
		if ( ( role == null ) || ( role.getFlag() == null ) 
				|| (role.getFlag().length != Role.FLAG_LENGTH ) ) {
			throw new IllegalArgumentException( "Permission() : invalid role " );
		}
		this.role = role;
		changeFlag = false;
	}

	public boolean isEnabled( Block block, Operation op ) {		
		int cell = ( block.getValue() + op.getValue() ) / 8;
		int offs = ( block.getValue() + op.getValue() ) % 8;
		return ( ( role.getFlag()[cell] & ( 1 << offs ) ) == 0  ? false : true );		
	}
	
	public void setEnabled( Block block, Operation op, boolean enabled ) {
		int cell = ( block.getValue() + op.getValue() ) / 8;
		int offs = ( block.getValue() + op.getValue() ) % 8;
		// make copy for successful execute EntityManager.merge()
		byte[] arr = changeFlag ? role.getFlag() : Arrays.copyOfRange( role.getFlag(), 0, Role.FLAG_LENGTH );
		arr[cell] = ( enabled ? ( byte ) ( arr[cell] | ( 1 << offs ) ) 
				: ( byte ) ( arr[cell] - ( arr[cell] & ( 1 << offs ) ) ) );
		role.setFlag( arr );
		changeFlag = true;
	}
	
	public static boolean combinePermission( Permission constr, Permission curr, Permission def, 
			Block block, Operation op ) {
		if ( ( constr == null ) || ( curr == null ) || ( def == null ) ) {
			StringBuilder errmsg = new StringBuilder();
			errmsg.append( "constr = " ).append( constr == null ? "null" : "not null " ).append( " \ncurr = " )
			.append( curr == null ? "null" : "not null " ).append( " \ndef = " )
			.append( def == null ? "null" : "not null " );
			if ( curr == null ) {
				String host = ResourceKeeper.getObject( OType.CURRENT_HOST );
				String login = ResourceKeeper.getObject( OType.CURRENT_LOGIN );
				Person person = ResourceKeeper.getObject( OType.CURRENT_PERSON );
				Role role = ResourceKeeper.getObject( OType.CURRENT_ROLE );
				Permission perm = ResourceKeeper.getObject( OType.PERMISSION_CURRENT );

				errmsg.append( "\nhost = " ).append( host == null ? "null" : host )
				.append( "\nlogin = " ).append( login == null ? "null" : login )
				.append( "\nuser = " ).append( person == null ? "null" : person )
				.append( "\nrole = " ).append( role == null ? "null" : role )
				.append( "\npermission = " ).append( perm == null ? "null" : "not null " );
			}

			if ( ( log != null ) && log.isDebugEnabled() ) log.debug( errmsg.toString() );
			throw new IllegalArgumentException( errmsg.toString() );
		}
		return ( constr.isEnabled( block, op ) ? curr.isEnabled( block, op ) : def.isEnabled( block, op ) );		
	}
}