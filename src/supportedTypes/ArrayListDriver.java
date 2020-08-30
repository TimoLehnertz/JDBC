package supportedTypes;

import java.lang.reflect.Field;
import java.util.ArrayList;

public class ArrayListDriver extends ExtraType {

	@Override
	boolean isSupported(Field field) {
		return field.getType() == ArrayList.class;
	}

	@Override
	String getSQLType() {
		return SupportedExtraTypes.SQL_TYPE_FOREIGN_KEY;
	}

	@Override
	void beforeSave(Object context) {
		
	}

	@Override
	protected String getStringValue(Object owner) {
		// TODO Auto-generated method stub
		return null;
	}
}