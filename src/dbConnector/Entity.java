package dbConnector;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import supportedTypes.SupportedExtraTypes;

public class Entity<E extends Entity<?>> {
	
	/**
	 * id to use as primary key in every table. if id is equal to -1 this instance of Entity is not syncronized with the database. to syncronize: save()
	 */
	int id = -1;
	
	private boolean tableExists = false;
	
	
	/**
	 * Constructor
	 * @ToDo
	 */
	public Entity() {//public fragwürdig
//		DBConnector.registerEntity(this);
	}
	
	
	
	
	/**
	 * @ToDo
	 * @return
	 */
	public boolean save() {
		if(!areAllFieldsNotNull())
			return false;
		saveMemberEntities();
		createTableIfNotExists();
		if(id < 0) {//never saved yet
			this.id = DBConnector.getInstance().executeStatement("INSERT INTO " + getClass().getSimpleName() + " (" + getFieldNameString() + ") VALUES (" + getValueString() + ")");
		} else {
			DBConnector.getInstance().executeStatement("UPDATE " + getClass().getSimpleName() + " SET " + getUpdateString() + " WHERE id = " + this.id);
		}
		return true;
	}
	
	/**
	 * Checks if all fields are initialized
	 * @return true if yes, false if not
	 */
	boolean areAllFieldsNotNull() {
		String errors = "";
		Field[] fields = this.getClass().getDeclaredFields();
		for (Field field : fields) {
			if(isFieldSupported(field)) {
				field.setAccessible(true);
				try {
					if(field.get(this) == null) {
						errors += (errors.length() > 0 ? ", " : "") + field.getName();
					}
				} catch (IllegalArgumentException | IllegalAccessException e) {
					System.out.println(e.getMessage());
					errors += (errors.length() > 0 ? ", " : "") + field.getName();
				}
			}
		}
		if(errors.length() > 0) {
			System.out.println("The Fields " + errors + " from \"" + this.getClass().getSimpleName() + "\" are not initialized :( cant save");
		}
		return errors.length() == 0;
	}
	
	String getUpdateString() {
		String out = "";
		for (Field field : getClass().getDeclaredFields()) {
			if(isFieldSupportedForType(field, this.getClass())) {
				out += (out.length() > 0 ? ", " : "") + field.getName() + " = " + getValueOfField(field);
			}
		}
		return out;
	}
	
	/**
	 * @return All names of supported Fields (Comma seperated)
	 */
	String getFieldNameString() {
		String out = "";
		for (Field field : this.getClass().getDeclaredFields()) {
			if(isFieldSupportedForType(field, this.getClass())) {
				out += (out.length() > 0 ? ", " : "") + field.getName();
			}
		}
		return out;
	}
	
	/**
	 * @return link to getNameTypesStringForType()
	 */
	String getNameTypesString() {
		return getNameTypesStringForType(this.getClass());
	}
	
	/**
	 * @return All types in SQL syntax for the create table statement
	 */
	@SuppressWarnings("rawtypes")
	public static String getNameTypesStringForType(Class<? extends Entity> type) {
		Field[] fields = type.getDeclaredFields();
		String out = "id int NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Unuque id for each row'";
		for (int i = 0; i < fields.length; i++) {
			if(isFieldSupportedForType(fields[i], type)) {
				out += ", " + fields[i].getName() + " " + getTypeOfField(fields[i]);
			}
		}
		return out;
	}
	
	/**
	 * @return All values of supported fields & the id (Comma seperated)
	 */
	String getValueString() {
		Field[] fields = this.getClass().getDeclaredFields();
		String out = "";
		for (int i = 0; i < fields.length; i++) {
			if(isFieldSupportedForType(fields[i], this.getClass())) {
				out += (out.length() > 0 ? ", " : "") + getValueOfField(fields[i]);
			}
		}
		return out;
	}
	
	/**
	 * @return link to getForeignKeyStringForType()
	 */
	String getForeignKeyString() {
		return getForeignKeyStringForType(this.getClass());
	}
	
	/**
	 * @return all Foreign Keys seperyted by comma to be appended at the end of the create table statement(all elements have a comma before f.e(,1,2,3,4))
	 */
	@SuppressWarnings("rawtypes")
	static String getForeignKeyStringForType(Class<? extends Entity> type) {
		String out = "";
		Field[] fields = type.getDeclaredFields();
		for (int i = 0; i < fields.length; i++) {
			if(isFieldSubClass(fields[i]) && isFieldSupportedForType(fields[i], type)) {
				out += ", " + getForeignKeyForField(fields[i]);
			}
		}
		return out;
	}
	
	/**
	 * Returns the FOREIGN KEY Constraint as SQL Statement for the Field f
	 * @param f
	 * @return null if no matching procedure for that field is found
	 * @ToDo FOREIGN KEY for Supported special types
	 */
	static String getForeignKeyForField(Field f) {
		if(isFieldSubClass(f)) {
			return "FOREIGN KEY (" + f.getName() + ") REFERENCES " + f.getType().getSimpleName() + " (id)";
		}
		return null;
	}
	
	/**
	 * @return true if all fields have successfully saved, false if not
	 */
	boolean saveMemberEntities() {
		for (Field field : this.getClass().getDeclaredFields()) {
			if(SupportedExtraTypes.isFieldSupported(field)) { //Is part of the supported Extra Types
				try {
					SupportedExtraTypes.invokeBeforeSave(field, this);
				} catch(Exception e) {
					System.out.println(e.getMessage());
					return false;
				}
			} else if(isFieldSubClass(field) && isFieldSupportedForType(field, this.getClass())) { // is subclass of Entity, so can be saved just as this instance
				try {
					field.setAccessible(true);
					((Entity<?>) field.get(this)).save();
				} catch (IllegalArgumentException | IllegalAccessException e) {
					e.printStackTrace();
				} catch(NullPointerException e) {
					System.out.println("Field \"" + field.getName() + "\" in " + this.getClass().getName() + " Is Null!");
				}
			}
		}
		return true;
	}
	
	/**
	 * @param field Field to be checked
	 * @return true if the specified Field is currently supported for SQL
	 */
	static boolean isFieldSupported(Field field) {
		return isFieldSupportedForType(field, Entity.class);
	}
	
	/**
	 * @param f Field to be checked
	 * @return true if the specified Field is currently supported for SQL
	 */
	static boolean isFieldSupportedForType(Field f, Class<?> type) {
		return (isFieldPrimitive(f) || SupportedExtraTypes.isFieldSupported(f) || isFieldSubClass(f)) && type != f.getType();
	}
	
	/**
	 * @param f Field to be checked
	 * @return true if the specified Field is of type primitive
	 */
	public static boolean isFieldPrimitive(Field f) {
		return f.getType().isPrimitive() || f.getType() == String.class;
	}
	
	/**
	 * @param f Field to be checked
	 * @return true if the type of the specified Field is a subtype of Entity but not an Entity itself
	 */
	public static boolean isFieldSubClass(Field f) {
		return Entity.class.isAssignableFrom(f.getType()) && f.getType() != Entity.class;
	}
	
	/**
	 * 
	 * @param f Field to be checked
	 * @return SQL compatible type from the specified Field or null if no matching type was found
	 */
	public static String getTypeOfField(Field f) {
		if(isFieldPrimitive(f)) {								//Primitive
			String out = "";
			Class<?> c = f.getType();
			if(c == Short.TYPE || c == Short.TYPE || c == Integer.TYPE) {
				out = SupportedExtraTypes.SQL_TYPE_INT;
			} else  if(c == Long.TYPE) {
				out = SupportedExtraTypes.SQL_TYPE_LONG;;
			}else if(c == Float.TYPE) {
				out = SupportedExtraTypes.SQL_TYPE_FLOAT;
			} else if(c == Double.TYPE) {
				out = SupportedExtraTypes.SQL_TYPE_DOUBLE;
			} else if(c == Character.TYPE) {
				out = SupportedExtraTypes.SQL_TYPE_CHAR;
			} else if(c == Boolean.TYPE) {
				out = SupportedExtraTypes.SQL_TYPE_BOOLEAN;
			} else if(c == String.class) {
				out = SupportedExtraTypes.SQL_TYPE_STRING;
			}
			return out + " NOT NULL";
		} else if(SupportedExtraTypes.isFieldSupported(f)) {					//Supported extra type
			try {
				SupportedExtraTypes.getSQLType(f);
			} catch(InvalidParameterException e) {
				System.out.println(e.getMessage());
			}
		} else if (isFieldSubClass(f)) {						//Subclass to be saved as foreign key
			return SupportedExtraTypes.SQL_TYPE_FOREIGN_KEY + " NOT NULL";
		}
		return null;
	}
	
	/**
	 * 
	 * @param field Field to use
	 * @return the String representation of this fields value in the context of "this" instance or null in case of an error
	 */
	String getValueOfField(Field field) {
		if(isFieldSupported(field)) {
			try {
				field.setAccessible(true);
				Object o = field.get(this);
				if(o == null)
					return null;
				if(isFieldPrimitive(field)) {// is primitive
					if(field.getType() == String.class)
						return "\"" + o + "\"";
					else
						return o + "";
				} else if(isFieldSubClass(field)) {
					return ((Entity<?>)o).id + "";
				} else if(SupportedExtraTypes.isFieldSupported(field)) {
					return SupportedExtraTypes.getStringValueForField(field, this);
				}
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	/**
	 * goes through all fields and checkes if any tables need to be created in order for this instance to work
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	static void createMissingForeignKeyTablesForType(Class<? extends Entity> type) {
		List<Class<?>> checked = new ArrayList<Class<?>>();
		for (Field field : type.getDeclaredFields()) {
			if(isFieldSubClass(field)) {
				if(!checked.contains(field.getType()) && field.getType() != type) {
					createTableForType((Class<? extends Entity>) field.getType());
					checked.add(field.getType());
				}
			} else if(SupportedExtraTypes.isFieldSupported(field)) {
				if(SupportedExtraTypes.doesNeedExtraTable(field)) {
					SupportedExtraTypes.createExtraTables(field);
				}
			}
		}
	}
	
	static boolean createTableForClassIfNotExists(Class<? extends Entity<?>> type) {
		if(!DBConnector.getInstance().doesTableExist(type.getSimpleName())) {
			return createTableForType(type);
		}
		return false;
	}
	
	/**
	 * checks if a suitable Table for this class instance does exist and if not it creates one via createTable()
	 * @return true if table was created, false if not
	 */
	boolean createTableIfNotExists() {
		if(!DBConnector.getInstance().doesTableExist(this.getClass().getSimpleName())) {
			return createTableForType(this.getClass());
		}
		alterTableIfNeeded();
		tableExists = true;
		return false;
	}
	
	/**
	 * @ToDo
	 */
	void alterTableIfNeeded() {
		Field[] fields = this.getClass().getDeclaredFields();
	}
	
	static void warnUncompatibleFieldsForType(Class<?> type) {
		String incompatible = "";
		for (Field field : type.getDeclaredFields()) {
			if(!isFieldSupportedForType(field, type)) {
				incompatible += (incompatible.length() > 0 ? ", " : "") + field.getName();
			}
		}
		if(incompatible.length() > 0) {
			System.out.println("the following Fields are not compatible with Entity \"" + type.getSimpleName() + "\" and will be ignored: " + incompatible);
		}
	}
	
	/**
	 * @ToDo
	 * Attemps to create a suitable Table for this instance
	 * @return true for success, false for error
	 */
	@SuppressWarnings("rawtypes")
	private static boolean createTableForType(Class<? extends Entity> type) {
		warnUncompatibleFieldsForType(type);
		if(DBConnector.getInstance().doesTableExist(type.getSimpleName())) {
			return false;
		}
		createMissingForeignKeyTablesForType(type);
		String createTableString = "CREATE TABLE IF NOT EXISTS " + type.getSimpleName();
		createTableString += " (" + getNameTypesStringForType(type);
		createTableString += getForeignKeyStringForType(type) + ")";
		boolean success = DBConnector.getInstance().executeStatement(createTableString) > -1;
		return success;
	}
	
	/**
	 * 
	 * @param name of own Field
	 * @return true if this field needs a foreignKey
	 */
	static boolean isFieldForeignKey(Class<?> type, String name) {
		try {
			return isFieldSubClass(type.getDeclaredField(name));
		} catch (NoSuchFieldException | SecurityException e) {
			return false;
		}
	}
	
	@SuppressWarnings({ "unchecked" })
	static Entity<?> getEntityFromMap(HashMap<String, Object> attributes, Class<?> type){
		Entity<?> entity = (Entity<?>) getEntityFromClassname(type.getName());
		if(entity != null) {
			for (Map.Entry<String, Object> attribute : attributes.entrySet()) {
				if(isFieldForeignKey(entity.getClass(), attribute.getKey())) { 	//Needs foreign Key
					try {
						Field memberField = entity.getClass().getDeclaredField(attribute.getKey());
						memberField.setAccessible(true);
						Class<?> memberType = memberField.getType();
						List<HashMap<String, Object>> rs = DBConnector.executeQuery("SELECT * FROM " + memberType.getSimpleName() + " WHERE id = " + attribute.getValue());
						Entity<?> memberEntity = getEntityFromMap(rs.get(0), memberType);
						memberField.set(entity, memberEntity);
					} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
						e.printStackTrace();
					}
				} else {								//simple value
					try {
						if(attribute.getKey().contentEquals("id")) {
							entity.id = (int) attribute.getValue();
						} else {
							Field field = type.getDeclaredField(attribute.getKey());
							field.setAccessible(true);
							field.set(entity, attribute.getValue());
						}
					} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
						e.printStackTrace();
					}
				}
			}
			return entity;
		} else {
			return null;
		}
	}
	
	/**
	 * @return A List of generic type E wich is holding all Entities of this kind saved in the database
	 */
	public List<E> getAllEntities(){
		List<E> out = new ArrayList<E>();
		DBConnector.getInstance();
		List<HashMap<String, Object>> rs = DBConnector.executeQuery("SELECT * FROM " + this.getClass().getSimpleName());
		for (HashMap<String, Object> entries : rs) {
			Entity<?> e = getEntityFromMap(entries, this.getClass());
			E type = null;
			if(e != null)
				out.add((E) e);
		}
		return out;
	}
	
	static Entity<?> getEntityFromClassname(String className) {
		try {
			Class<? extends Entity<?>> clazz = (Class<? extends Entity<?>>) Class.forName(className);
			Constructor<?> ctor = clazz.getConstructors()[0];
			return (Entity<?>) ctor.newInstance();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException | ClassNotFoundException e) {
			System.out.print(e.getMessage());
			System.out.println("! Failed to construct \"" + className + "\". Did you implemented a default Constructor with no arguments?");
			return null;
		}
	}
	
	/**
	 * deletes the table of this instances class
	 * @return
	 */
	public boolean dropTableIfExists() {
		String dropTableString = "DROP TABLE IF EXISTS " + this.getClass().getSimpleName();
		tableExists = false;
		return DBConnector.getInstance().executeStatement(dropTableString) > -1;
	}
	
	public boolean doesTableExist() {
		return tableExists;
	}


	public void printTable() {
		createTableIfNotExists();
		DBConnector.getInstance().printSelectAll(getClass().getSimpleName());
	}

	public int getId() {
		return id;
	}
	
	/**
	 * 
	 * @param n
	 * @return n tabs as String
	 */
	String getnTabs(int n) {
		String out = "";
		for (int i = 0; i < n; i++) {
			out += "\t";
		}
		return out;
	}
	
	/**
	 * 
	 * @param tabs initial tabs used for recursive calls
	 * @return
	 */
	public String toStringJson(int tabs) {
		tabs++;
		String out = "\"" + this.getClass().getSimpleName() + "\": {\n " + getnTabs(tabs) + " \"ID\":\"" + id + "\"";
		Field[] fields = this.getClass().getDeclaredFields();
		for (Field field : fields) {
			try {
				field.setAccessible(true);
				out +="\n" + getnTabs(tabs) +  " \"" + field.getName() + "\": " + (isFieldSubClass(field) && field.get(this) != null ? ((Entity<?>) field.get(this)).toStringJson(tabs) : "\"" + field.get(this) + "\"") + ",";
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		return out + "\n" + getnTabs(tabs - 1) + "}";
	}
	
	@Override
	public String toString() {
		return toStringJson(0);
	}
}