package supportedTypes;

import java.lang.reflect.Field;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;

public class SupportedExtraTypes {

//	List of all Supported Types need to be updated manually
	static final ArrayList<ExtraType> supportedTypes = new ArrayList<ExtraType>(Arrays.asList(/*@ToDo List*/));
	
//	invalidParameterException to be thrown if an Information requested involved an unsupported Type
	private static InvalidParameterException invalidParameterException = new InvalidParameterException("The type of this Field is not supported Yet :(");
	
	/**
	 * Constants for conversion from java types to SQL Compatible types
	 */
	public static final String SQL_TYPE_INT = "INT";
	
	public static final String SQL_TYPE_FOREIGN_KEY = SQL_TYPE_INT;
	
	public static final String SQL_TYPE_LONG = "INT(20)";
	
	public static final String SQL_TYPE_FLOAT = "FLOAT(24)";
	
	public static final String SQL_TYPE_DOUBLE = "DOUBLE(12, 12)";
	
	public static final String SQL_TYPE_BOOLEAN = "BOOL";
	
	public static final String SQL_TYPE_CHAR = "CHAR(1)";
	
	public static final int DEFAULT_VARCHAR_SIZE = 50;
	public static final String SQL_TYPE_STRING = "VARCHAR(" + DEFAULT_VARCHAR_SIZE + ")";
	
	
	/**
	 * private Constructor
	 */
	private SupportedExtraTypes() {
		super();
	}
	
	/**
	 * @param field
	 * @return true if Field is supported
	 */
	public static boolean isFieldSupported(Field field) {
		try {
			getSupportedTypeForField(field);
			return true;
		} catch (InvalidParameterException e) {
			return false;
		}
	}
	
	/**
	 * searches the supportedTypes List for a fitting SupportedType
	 * @param field to be searched for
	 * @return the SupportedType 
	 * @throws InvalidParameterException if Field is incompatible
	 */
	private static ExtraType getSupportedTypeForField(Field field) throws InvalidParameterException {
		for (ExtraType supportedType : supportedTypes) {
			if(supportedType.isSupported(field)) {
				return supportedType;
			}
		}
		throw invalidParameterException;
	}
	
	/**
	 * @param field to get the SQL Type for
	 * @return  SQL Type in String format
	 * @throws InvalidParameterException
	 */
	public static String getSQLType(Field field) throws InvalidParameterException {
		return getSupportedTypeForField(field).getSQLType();
	}
	
	public static void invokeBeforeSave(Field field, Object context) throws IllegalArgumentException, IllegalAccessException {
		getSupportedTypeForField(field).beforeSave(field.get(context));
	}
	
	public static boolean doesNeedExtraTable(Field field) throws IllegalArgumentException {
		return getSupportedTypeForField(field).doesNeedExtraTable();
	}
	
	public static void createExtraTables(Field field) {
		getSupportedTypeForField(field).createExtraTable();
	}

	public static String getStringValueForField(Field field, Object owner) throws IllegalArgumentException {
		return getSupportedTypeForField(field).getStringValue(owner);
	}
}