package supportedTypes;

import java.lang.reflect.Field;

public abstract class ExtraType {
	
	/**
	 * @param field
	 * @return true if the specified Field is supported by this Supported Type
	 */
	abstract boolean isSupported(Field field);
	
	/**
	 * Should return the String representation of this types SQL Type
	 * Should be On of the specified Strings in SupportedTypes
	 * @return
	 */
	abstract String getSQLType();
	
	/**
	 * gets invoked before this type is getting saved in the database
	 * useful f.e. for lists that need to save sub classes first
	 */
	abstract void beforeSave(Object context);
	
	boolean doesNeedExtraTable() {
		return false;
	}
	
	void createExtraTable() {
		System.out.println("Please implement me");
	}

	protected abstract String getStringValue(Object owner);
}