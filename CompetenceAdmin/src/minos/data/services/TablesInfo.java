package minos.data.services;

import java.util.ArrayList;
import java.util.List;

import ru.gedr.util.tuple.Triplet;

public class TablesInfo {
	private List<Triplet <String, Integer, Variety>> lst = new ArrayList<>();	
	
	public enum Variety { LOGGING_DISABLED, LOGGING_ONLY, LOGGING_AND_TRANSPORT_SOMETIMES, LOGGING_AND_TRANSPORT_ALWAYS } 
	
	public TablesInfo() { }
	
	public void addInfo( String name, int code, Variety variety ) {
		if ( ( name == null ) || ( variety == null ) ) throw new IllegalArgumentException();
		lst.add( new Triplet<>(name, code, variety ) );		
	}
	
	public String getNameByCode( int code ) {
		for ( Triplet <String, Integer, Variety> t : lst) 
			if ( t.getSecond() == code ) return t.getFirst();
		return null;
	}

	public int getCodeByName( String name ) {
		if ( name == null ) throw new IllegalArgumentException();
		for ( Triplet <String, Integer, Variety> t : lst) 
			if ( t.getFirst().equals( name ) ) return t.getSecond();
		return -1;
	}
	
	public Variety getVarietyByCode( int code ) {
		for ( Triplet <String, Integer, Variety> t : lst) 
			if ( t.getSecond() == code ) return t.getThird();
		return null;
	}

	public Variety getVarietyByName( String name ) {
		if ( name == null )
		for ( Triplet <String, Integer, Variety> t : lst) 
			if ( t.getFirst().equals( name ) ) return t.getThird();
		return null;
	}

	public String[] getNamesByVarieties( Variety... varieties ) {		
		if ( ( varieties == null ) || ( varieties.length == 0 ) ) throw new IllegalArgumentException();
		List<String> str = new ArrayList<>();
		for ( Triplet <String, Integer, Variety> t : lst) {
			for( Variety v : varieties )
				if ( t.getThird() == v ) str.add( t.getFirst() );
		}
		return ( str.size() == 0 ? null : str.toArray( new String[0] ) );
	}

	public Integer[] getCodesByVarieties( Variety... varieties ) {
		if ( ( varieties == null ) || ( varieties.length == 0 ) ) throw new IllegalArgumentException();
		List<Integer> li = new ArrayList<>();
		for ( Triplet <String, Integer, Variety> t : lst) {
			for( Variety v : varieties )
				if ( t.getThird() == v ) li.add( t.getSecond() );
		}
		return li.toArray( new Integer[0] );
	}
	
	public String[] getNames() {
		if ( lst.size() == 0 ) return null;
		String[] arrs = new String[lst.size()];
		int ind = 0;
		for ( Triplet <String, Integer, Variety> t : lst) arrs[ind++] = t.getFirst();
		return arrs;
	}

	public int[] getCodes() {
		if ( lst.size() == 0 ) return null;
		int[] arrs = new int[lst.size()];
		int ind = 0;
		for ( Triplet <String, Integer, Variety> t : lst) arrs[ind++] = t.getSecond();
		return arrs;
	}
}
