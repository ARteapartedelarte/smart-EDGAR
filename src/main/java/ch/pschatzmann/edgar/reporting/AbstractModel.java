package ch.pschatzmann.edgar.reporting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import ch.pschatzmann.common.table.ITable;
import ch.pschatzmann.edgar.base.errors.DataException;
import ch.pschatzmann.edgar.utils.Utils;

/**
 * We can define the row fields, column fields and the value field. In addition
 * we represent the database tables and their join relationships. This
 * information is used to generate the SQL select command
 * 
 * @author pschatzmann
 *
 */

public abstract class AbstractModel {
	private Collection<DBTable> tables = new ArrayList<DBTable>();
	private ISQLModel sqlModel = new PostgresSQLModelPriorities(this);
	private List<String> groupings = null;
	private boolean parameterAsPriorityAlternatives = true;

	/**
	 * Setup of the model
	 * 
	 * @throws DataException
	 */
	public abstract AbstractModel create() throws DataException;

	/**
	 * Determines the database tables
	 * 
	 * @return
	 */
	protected Collection<DBTable> getTables() {
		return this.tables;
	}

	/**
	 * Add a table to the model
	 * 
	 * @param table
	 * @return
	 */
	public DBTable addTable(DBTable table) {
		tables.add(table);
		return table;
	}

	/**
	 * Determines a tyble by name
	 * 
	 * @param name
	 * @return
	 */
	public DBTable getTable(String name) {
		for (DBTable t : this.tables) {
			if (t.getTableName().equalsIgnoreCase(name)) {
				return t;
			}
		}
		return null;
	}

	/**
	 * Determines a field by table name and field name
	 * 
	 * @param table
	 * @param field
	 * @return
	 * @throws DataException
	 */
	public DBField getTableField(String table, String field) throws DataException {
		for (DBTable t : this.tables) {
			if (t.getTableName().equalsIgnoreCase(table) || table == null) {
				for (DBField fld : t.getFields()) {
					if (fld.getFieldName().equalsIgnoreCase(field)) {
						return fld;
					}
				}
			}
		}
		throw new DataException("The table does not exist: " + table);
	}

	/**
	 * Determines the field values for the indicated table field
	 * @param tableName
	 * @param fieldName
	 * @param like
	 * @return
	 * @throws DataException
	 */
	public List<String> getFieldValues(String tableName, String fieldName, String like) throws DataException {
		DBField fld = this.getTableField(tableName, fieldName);
		DBMS sql = DBMS.getInstance();
		try {
			return sql.getFieldValues(fld.getTable().getTableName(), fld.getFieldNameSQL(), like);
		} catch (Exception e) {
			throw new DataException(e);
		}
	}

	/**
	 * Determines a navigation field with the help of the field name and table name
	 * 
	 * @param table
	 * @param field
	 * @return
	 * @throws DataException
	 */
	public NavigationField getNavigationField(String table, String field) throws DataException {
		return getNavigationField(table, field, null);
	}

	/**
	 * Determines a navigation field with the help of the field name and table name
	 * 
	 * @param table
	 * @param field
	 * @param relationFromField
	 * @return
	 * @throws DataException
	 */
	public NavigationField getNavigationField(String table, String field, String relationFromField)
			throws DataException {
		for (DBTable t : this.tables) {
			if (t.getTableName().equalsIgnoreCase(table) || Utils.isEmpty(table)) {
				NavigationField fld = t.getNavigationField(field, relationFromField);
				if (fld != null) {
					return fld;
				}
			}
		}
		throw new DataException("The field '" + field + "' does not exist");
	}

	/**
	 * Determines the model which is used to generate the join condition
	 * 
	 * @return
	 */
	public ISQLModel getSQLModel() {
		return sqlModel;
	}

	/**
	 * Defines the model which is used to generate the join condition
	 * 
	 * @param model
	 */
	public void setSQLModel(ISQLModel model) {
		this.sqlModel = model;
	}

	/**
	 * Returns all groupings which are used by the navigation fields
	 * 
	 * @return
	 */
	public List<String> getGroupings() {
		if (groupings == null) {
			Set<String> set = new TreeSet();
			for (DBTable t : this.getTables()) {
				for (NavigationField nf : t.getNavigationFields()) {
					set.add(nf.getGroup());
				}
			}
			groupings = new ArrayList(set);
		}
		return groupings;
	}

	/**
	 * Finds all NavigationField for the indicated grouping
	 * 
	 * @param grouping
	 * @return
	 */
	public List<NavigationField> getNavigationFieldForGrouping(String grouping) {
		List<NavigationField> result = new ArrayList();
		for (DBTable t : this.getTables()) {
			for (NavigationField nf : t.getNavigationFields()) {
				if (Utils.isEmpty(grouping) || grouping.equalsIgnoreCase(nf.getGroup())) {
					result.add(nf);
				}
			}
		}
		return result;
	}

	/**
	 * Determines the fields with filter values
	 * 
	 * @return
	 */
	public Collection<NavigationField> getFilterFields() {
		List<NavigationField> result = new ArrayList();
		for (DBTable t : this.getTables()) {
			for (NavigationField nf : t.getNavigationFields()) {
				if (!nf.getFilterValues().isEmpty()) {
					result.add(nf);
				}
			}
		}
		return result;
	}

	/**
	 * Create SQL command via the actual SQL model
	 * 
	 * @param table
	 * @return
	 * @throws DataException
	 */

	public String toSQL(ITable table) throws DataException {
		return this.getSQLModel().toSQL(table);
	}

	/**
	 * The list of parameters is treated as list of alternatives where the first
	 * value is used which occurs per reporting
	 * 
	 * @return
	 */
	public boolean isParameterAsPriorityAlternatives() {
		return this.parameterAsPriorityAlternatives;
	}

	public void setParameterAsPriorityAlternatives(boolean flag) {
		this.parameterAsPriorityAlternatives = flag;
	}

}
