package ch.pschatzmann.edgar.reporting;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import ch.pschatzmann.common.table.ITable;
import ch.pschatzmann.common.table.ITableEx;
import ch.pschatzmann.edgar.base.errors.DataException;
import ch.pschatzmann.edgar.table.CombinedKey;
import ch.pschatzmann.edgar.table.Key;
import ch.pschatzmann.edgar.table.KeyComparator;

/**
 * Table representation of data where the numerical values are split up in
 * columns and rows.
 * 
 * @author pschatzmann
 *
 */
public class Table implements ITableEx {
	private static final Logger LOG = Logger.getLogger(Table.class);
	private static final long serialVersionUID = 1L;
	private List<NavigationField> rows = new ArrayList<NavigationField>();
	private List<NavigationField> columns = new ArrayList<NavigationField>();
	private NavigationField valueField;
	private List<Key> dataRows = new ArrayList<Key>();
	private List<Key> dataColumns = new ArrayList<Key>();
	private Map<Key, Key> rowKeyMap = new HashMap<Key, Key>();
	private Map<Key, Key> colKeyMap = new HashMap<Key, Key>();
	private Map<CombinedKey, Number> valueMap = new HashMap<CombinedKey, Number>();
	private BiFunction<Integer, Integer, Number> valueFunction = ((row, col) -> {
		CombinedKey key = new CombinedKey(dataColumns.get(col), dataRows.get(row));
		return getValue(key);
	});

	
	public Table() {
		LOG.info("table");
	}

	/**
	 * Determines the value field (= source of numerical values)
	 * 
	 * @return
	 */
	public NavigationField getValueField() {
		return valueField;
	}

	/**
	 * Defines the value field
	 * 
	 * @param fld
	 */
	public void setValueField(NavigationField fld) {
		this.valueField = fld;
	}

	/**
	 * Determine the rows of the result table
	 * 
	 * @return
	 */
	public List<NavigationField> getRows() {
		return this.rows;
	}

	/**
	 * Determine the columns of the result table
	 * 
	 * @return
	 */
	public List<NavigationField> getColumns() {
		return this.columns;
	}

	/**
	 * Determines the group by fields
	 * 
	 * @return
	 */
	protected Collection<NavigationField> getGroupFields() {
		Collection<NavigationField> result = new ArrayList<NavigationField>();
		result.addAll(this.getColumns());
		result.addAll(this.getRows());
		return result;
	}

	/**
	 * Defines a new model row
	 * 
	 * @param row
	 */

	public void addRow(NavigationField row) {
		this.rows.add(row);
	}

	/**
	 * Defines a new model column
	 * 
	 * @param col
	 */
	public void addColumn(NavigationField col) {
		this.columns.add(col);
	}

	/**
	 * Clears the rows and columns
	 */
	public void clear() {
		this.getRows().clear();
		this.getColumns().clear();
	}

	/**
	 * Determines the field names of the header column
	 * 
	 * @return
	 */
	public List<String> getColumnFieldNames() {
		return getFieldNames(this.columns);
	}

	public String getColumnFieldName(int col) {
		return getFieldNames(this.columns).get(col);
	}

	public int getColumnFieldCount() {
		return getFieldNames(this.columns).size();
	}

	public List<String> getRowFieldNames() {
		return getFieldNames(this.rows);
	}

	public String getRowFieldName(int row) {
		return getFieldNames(this.rows).get(row);
	}

	public int getRowFieldCount() {
		return getFieldNames(this.rows).size();
	}

	protected List<String> getFieldNames(List<NavigationField> fields) {
		return fields.stream().map(f -> f.getFieldName()).collect(Collectors.toList());
	}

	public int getColumnCount() {
		return this.getValueField() == null ? 0 : this.dataColumns.size();
	}

	public int getRowCount() {
		return this.dataRows.size();
	}

	public Number getValue(int col, int row) {
		return valueFunction.apply(row, col);
	}
	
	public Number getValue(CombinedKey key) {
		return valueMap.get(key);
	}


	public String getValueFieldName() {
		return this.valueField.getFieldName();
	}

	/**
	 * Determines the value of the indicated row
	 * 
	 * @param row
	 * @return
	 */
	public List<String> getRowValue(int row) {
		return this.dataRows.get(row).getKeyValues();
	}

	/**
	 * Determines the value of the indicated column
	 * 
	 * @param col
	 * @return
	 */
	public List<String> getColumnValue(int col) {
		return this.dataColumns.get(col).getKeyValues();
	}

	/**
	 * Adds the record from the result set to the table
	 * 
	 * @param values
	 * @throws SQLException
	 */
	public void putRecord(ResultSet values) throws SQLException {
		Number value = null;
		Key rowKey = addRowKey(values);
		Key colKey = addColumnKey(values);
		if (this.getValueField() != null) {
			value = values.getBigDecimal(this.getValueFieldName());
			if (value != null) {
				if (LOG.isDebugEnabled())
					LOG.debug(colKey + "/" + rowKey + ": " + value);
				this.valueMap.put(new CombinedKey(colKey, rowKey), value);
			}
		} else {
			this.valueMap.put(new CombinedKey(colKey, rowKey), null);
		}
	}

	protected Key addRowKey(ResultSet values) throws SQLException {
		List<String> row = new ArrayList<String>();
		for (int j = 0; j < this.getRowFieldCount(); j++) {
			row.add(values.getString(this.getRowFieldName(j)));
		}
		Key rowKeySearch = new Key(row);
		Key rowKey = this.rowKeyMap.get(rowKeySearch);
		if (rowKey == null) {
			rowKey = rowKeySearch;
			this.dataRows.add(rowKey);
			this.rowKeyMap.put(rowKey, rowKey);
		}
		return rowKey;
	}

	protected Key addColumnKey(ResultSet values) throws SQLException {
		List<String> col = new ArrayList<String>();
		for (int j = 0; j < this.getColumnFieldCount(); j++) {
			col.add(values.getString(this.getColumnFieldName(j)));
		}
		return addColumnKey(col);
	}

	/**
	 * Eg. add a parameter which e.g. is not in the database
	 * 
	 * @param parameter
	 */
	public Table addColumnKey(String parameter) {
		 addColumnKey(Arrays.asList(parameter));
		 return this;
	}

	protected Key addColumnKey(List<String> parameter) {
		Key colKeySearch = new Key(parameter);
		Key colKey = this.colKeyMap.get(colKeySearch);
		if (colKey == null) {
			colKey = colKeySearch;
			this.dataColumns.add(colKey);
			this.colKeyMap.put(colKey, colKey);
		}
		return colKey;
	}

	/**
	 * Executes the SQL command against the database and adds all found records
	 * 
	 * @param model
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 * @throws DataException
	 */
	public void execute(EdgarModel model) throws SQLException, ClassNotFoundException, DataException {
		String sql = model.toSQL(this);
		DBMS db = DBMS.getInstance();
		db.execute(sql, this);
		Collections.sort(dataRows, new KeyComparator());
		Collections.sort(dataColumns, new KeyComparator());
	}

	public String getColumnTitle(int col) {
		StringBuffer sb = new StringBuffer();
		boolean firstTitle = true;
		for (String f : this.getColumnValue(col)) {
			if (!firstTitle)
				sb.append(" ");
			firstTitle = false;
			sb.append(f);
		}
		if (firstTitle) {
			sb.append("Total");
		}
		return sb.toString();
	}

	protected List<Key> getDataRows() {
		return this.dataRows;
	}

	public BiFunction<Integer, Integer, Number> getValueFunction() {
		return valueFunction;
	}

	public void setValueFunction(BiFunction<Integer, Integer, Number> valueFunction) {
		LOG.info("setValueFunction");
		this.valueFunction = valueFunction;
	}
	
	@Override
	public ITable getBaseTable() {
		return this;
	}
}

