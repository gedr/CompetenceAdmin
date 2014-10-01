package minos.data.services;

import java.io.Serializable;
import java.util.Arrays;

public class FilialInfo implements Serializable {
	private static final long serialVersionUID = 4602549973662593883L;

	private byte code;
	private byte shift;
	private String name;
	private byte[] prefixes;
	private int rootDivisionCode;
	private int divisionCodeMin;
	private int divisionCodeMax;
	private int personCodeMin;
	private int personCodeMax;
	
	public FilialInfo() { 
		this.code = -1;
		this.shift = 0;
		this.name = null;
		this.prefixes = null;
		this.rootDivisionCode = -1;
		this.personCodeMin = this.personCodeMax = 0;
		this.divisionCodeMin = this.divisionCodeMax = 0;
	}

	public FilialInfo( byte code, byte shift, String name, byte[] prefixes, int rootDivisionCode, int divisionCodeMin, 
			int divisionCodeMax, int personCodeMin, int personCodeMax ) { 
		this.code = code;
		this.shift = shift;
		this.name = name;
		this.prefixes = prefixes;
		this.rootDivisionCode = rootDivisionCode;
		this.setDivisionCodeMin(divisionCodeMin);
		this.setDivisionCodeMax(divisionCodeMax);
		this.personCodeMin = personCodeMin;
		this.personCodeMax = personCodeMax;
	}

	public byte getCode() {
		return code;
	}
	
	public void setCode( byte code ) {
		this.code = code;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName( String name ) {
		this.name = name;
	}
	
	public byte[] getPrefixes() {
		return prefixes;
	}
	
	public void setPrefixes( byte[] prefixes ) {
		this.prefixes = prefixes;
	}
	
	public int getRootDivisionCode() {
		return rootDivisionCode;
	}
	
	public void setRootDivisionCode( int rootDivisionCode ) {
		this.rootDivisionCode = rootDivisionCode;
	}
	
	public int getPersonCodeMin() {
		return personCodeMin;
	}
	
	public void setPersonCodeMin( int personCodeMin)  {
		this.personCodeMin = personCodeMin;
	}
	
	public int getPersonCodeMax() {
		return personCodeMax;
	}
	
	public void setPersonCodeMax( int personCodeMax ) {
		this.personCodeMax = personCodeMax;
	}

	public byte getShift() {
		return shift;
	}

	public void setShift(byte shift) {
		this.shift = shift;
	}

	public int getDivisionCodeMax() {
		return divisionCodeMax;
	}

	public void setDivisionCodeMax( int divisionCodeMax ) {
		this.divisionCodeMax = divisionCodeMax;
	}

	public int getDivisionCodeMin() {
		return divisionCodeMin;
	}

	public void setDivisionCodeMin( int divisionCodeMin ) {
		this.divisionCodeMin = divisionCodeMin;
	}

	@Override
	public boolean equals( Object obj ) { // deep equals
		if ( obj == this ) return true;
		if ( obj == null ) return false;
		if ( !( obj instanceof FilialInfo ) ) return false;  
		FilialInfo fi = ( FilialInfo ) obj;
		if ( ( fi.code != this.code ) || ( fi.shift != this.shift ) || ( fi.rootDivisionCode != this.rootDivisionCode ) 
				|| ( fi.divisionCodeMin != this.divisionCodeMin ) || ( fi.divisionCodeMax != this.divisionCodeMax ) 
				|| ( fi.personCodeMin != this.personCodeMin ) || ( fi.personCodeMax != this.personCodeMax ) 
				|| !fi.name.equals( name ) ) return false;
		if ( ( fi.prefixes == null ) && ( this.prefixes == null ) ) return true;
		if ( ( fi.prefixes == null ) && ( this.prefixes != null ) 
				|| ( fi.prefixes != null ) && ( this.prefixes == null ) ) return false;
		return Arrays.equals( fi.prefixes, this.prefixes );
	}

	@Override
	public String toString() {		
		return name;
	}
}
