package pt.tecnico.jpf.symbiosis.util;

public enum Type {

	INT(1),
	REAL(2),
	STRING(3),
	BOOLEAN(4),
	SHORT(5),
	BYTE(6),
	LONG(7),
	CHAR(8),
	FLOAT(9),
	REF(10),
	REFERENCE(0),
	SYMINT(-1),
	SYMREAL(-2),
	SYMSTRING(-3),
	SYMREF(-4);
	
	private int code;
	
	Type(int c){
		code = c;
	}
	
	int getCode(){
		return code;
	}
}

