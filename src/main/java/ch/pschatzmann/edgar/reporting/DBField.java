package ch.pschatzmann.edgar.reporting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a single Table field.
 * 
 * @author pschatzmann
 *
 */
public class DBField {
	private DBTable table;
	private String fieldName;
	private String fieldNameExt;
	private List<String> filterValues = new ArrayList<String>();
	private boolean filterEquals = true;
	private boolean isCalculated = false;
	private String fieldGroup;
	private boolean supportWildCardFilter = false;

	public DBField() {}
	
	/**
	 * Default constructor for regular database fields
	 * @param t
	 * @param fieldName
	 */
	public DBField(DBTable t, String fieldName) {
		this.table = t;
		this.fieldName = fieldName;
		this.fieldNameExt = fieldName;
		this.fieldGroup = t.getTableName();
	}

	/**
	 * Constructor to create a field based on an sql expression 
	 * @param t
	 * @param fieldName
	 * @param sqlExpression
	 */
	public DBField(DBTable t, String fieldName, String sqlExpression) {
		this.table = t;
		this.fieldName = fieldName;
		this.fieldNameExt = sqlExpression;
		this.isCalculated = !fieldName.equals(sqlExpression);
		this.fieldGroup = t.getTableName();
	}

	/**
	 * Returns the table 
	 * @return
	 */
	public DBTable getTable() {
		return table;
	}

	/**
	 * Returns the field name
	 * @return
	 */
	public String getFieldName() {
		return fieldName;
	}

	/**
	 * Returns the name in the form table name.field name
	 * @return
	 */
	public String getFieldNameExt() {
		return this.getTable().getTableName()+"."+fieldName;
	}

	/**
	 * Returns the sql expression which is used to determine the field value
	 * @return
	 */
	public String getFieldNameSQL() {
		return fieldNameExt;
	}
	
	/**
	 * Returns true if there is a sql expression for the calculaiton of the field
	 * @return
	 */
	public boolean isCalculated() {
		return this.isCalculated;
	}
	
	/**
	 * Determines the values which should be used to limit the result set
	 * @return
	 */
	public List<String> getFilterValues() {
		return filterValues;
	}

	/**
	 * Defines the values which are used to restrict the result via a where condition
	 * @param filterValues
	 */
	public DBField setFilterValues(List<String> filterValues) {
		this.filterValues = filterValues;
		return this;
	}

	/**
	 * Defines multiple filter values
	 * @param filterValues
	 * @return
	 */
	public DBField setFilterValues(String ... filterValues) {
		this.filterValues = Arrays.asList(filterValues);
		return this;
	}

	/**
	 * Determines the reporting group. 
	 * @return
	 */
	public String getGroup() {
		return this.fieldGroup;
	}

	/**
	 * Defines the reporting group
	 * @param grp
	 */
	public DBField setGroup(String grp) {
		this.fieldGroup = grp;
		return this;
	}
	
	@Override
	public String toString() {
		return this.getFieldNameExt();
	}
	
	protected void setCalculated(boolean calculated) {
		this.isCalculated = calculated;
	}

	public boolean isFilterEquals() {
		return filterEquals;
	}

	public void setFilterEquals(boolean filterEquals) {
		this.filterEquals = filterEquals;
	}

	public boolean isSupportWildCardFilter() {
		return supportWildCardFilter;
	}

	public void setSupportWildCardFilter(boolean supportWildFilter) {
		this.supportWildCardFilter = supportWildFilter;
	}

}
