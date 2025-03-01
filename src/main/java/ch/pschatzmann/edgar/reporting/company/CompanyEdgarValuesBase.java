package ch.pschatzmann.edgar.reporting.company;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import ch.pschatzmann.common.table.ITable;
import ch.pschatzmann.common.table.ITableEx;
import ch.pschatzmann.common.table.TableCalculated;
import ch.pschatzmann.common.table.TableConsolidated;
import ch.pschatzmann.common.table.TableFilteredOnCol;
import ch.pschatzmann.common.table.TableFilteredOnRow;
import ch.pschatzmann.common.utils.Tuple;
import ch.pschatzmann.edgar.base.errors.DataException;
import ch.pschatzmann.edgar.table.FormulasEdgar;

/**
 * Provides the reported parameter values for a company by date. Per default we
 * use the QuarterlyCumulated filter
 * 
 * @author pschatzmann
 *
 */
public abstract class CompanyEdgarValuesBase implements ICompanyInfo {
	private static final long serialVersionUID = 1L;
	protected ITableEx table;
	protected BiFunction<ITable, Integer, Boolean> filter = new FilterQuarterlyCumulated();
	protected boolean consolidated = true;
	protected String parameterNamesArray[] = null;
	protected String unitRefArray[] = { "USD" };
	private ITableEx filteredTable;
	private boolean addMissingParameters = true;
	private boolean useArrayList = false;
	private List<Tuple<String, String>> calculatedColumns = new ArrayList();
	private List<String> removeParameterNames = new ArrayList();
	private DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
	private boolean addTime = false;
	
	public CompanyEdgarValuesBase() {}
	

	protected abstract void setup() throws DataException;

	public CompanyEdgarValuesBase setFilter(IRowFilter filter) {
		return this.setFilter((BiFunction)filter, true);
	}
	
	public CompanyEdgarValuesBase setFilter(BiFunction<ITable, Integer, Boolean> filter) {
		return this.setFilter(filter, true);
	}

	public CompanyEdgarValuesBase setFilter(IRowFilter filter, boolean consolidated) {
		return this.setFilter((BiFunction)filter, consolidated);
	}
	
	public CompanyEdgarValuesBase setFilter(BiFunction<ITable, Integer, Boolean> filter, boolean consolidated) {
		this.filter = filter;
		this.consolidated = consolidated;
		if (this.table !=null) {
			this.filteredTable = new TableFilteredOnRow(this.table, filter) ;
			if (consolidated) {
				this.filteredTable = new TableConsolidated(this.filteredTable ,Arrays.asList("form","numberOfMonths"));
			}
			if (!calculatedColumns.isEmpty()) {
				filteredTable = new TableCalculated(filteredTable, new FormulasEdgar(), calculatedColumns);
			}
			if (!removeParameterNames.isEmpty()){
				filteredTable = new TableFilteredOnCol(filteredTable).removeParameterNames(removeParameterNames);
			}
		}
		return this;
	}

	public CompanyEdgarValuesBase setParameterNames(String... parameterNames) {
		this.table = null;
		this.parameterNamesArray = parameterNames;
		return this;
	}
	
	
	public CompanyEdgarValuesBase removeParameterNames(String...removeParameterNames ) {
		this.removeParameterNames  = Arrays.asList(removeParameterNames);
		return this;
	}

	public CompanyEdgarValuesBase removeParameterNames(List<String>removeParameterNames ) {
		this.removeParameterNames  = removeParameterNames!=null ? removeParameterNames : new ArrayList();
		return this;
	}

	public CompanyEdgarValuesBase setParameterNames(Collection<String> parameterNames) {
		this.table = null;
		this.parameterNamesArray = parameterNames.toArray(new String[parameterNames.size()]);
		return this;
	}
	
	public CompanyEdgarValuesBase setUnitRef(String... unitRef) {
		this.table = null;
		this.unitRefArray = unitRef;
		return this;
	}

	public boolean isAddMissingParameters() {
		return addMissingParameters;
	}

	public CompanyEdgarValuesBase setAddMissingParameters(boolean addMissingParameters) {
		this.addMissingParameters = addMissingParameters;
		return this;
	}

	public boolean isUseArrayList() {
		return useArrayList;
	}
	
	/**
	 * Informs if the dates are converted to java.util.Date objects.
	 * By default the dates are represented as String and they are not converted to java.util.Date.
	 * @return
	 */
	public boolean isAddTime() {
		return this.addTime;
	}
	
	/**
	 * The date field is usually represented as String. We can convert it to a date instead
	 * @param flag
	 * @return
	 */
	public CompanyEdgarValuesBase setAddTime(boolean flag) {
		this.addTime = flag;
		return this;
	}
	
	/**
	 * Adds a calculated column
	 * 
	 * @param parameterName
	 * @param formula
	 * @return
	 */
	public CompanyEdgarValuesBase addFormula(String parameterName, String formula) {
		calculatedColumns.add(new Tuple(parameterName, formula));
		return this;
	}
	
	/**
	 * Adds many calculated columns
	 * @param formulas
	 * @return
	 */
	public CompanyEdgarValuesBase addFormulas(List<Tuple<String, String>> formulas) {
		if (formulas!=null) {
			formulas.forEach(e -> this.addFormula(e.x, e.y));
		}
		return this;
	}


	public CompanyEdgarValuesBase setUseArrayList(boolean useArrayList) {
		this.useArrayList = useArrayList;
		return this;
	}


	public List<Map<String, ?>> toList() throws DataException {
		setup();
		List<Map<String, ?>>  result = this.isUseArrayList() ? new ArrayList(this.filteredTable.toList()) : this.filteredTable.toList();  
		if (addTime) {
			result = result.stream().map(m -> addTime(m)).collect(Collectors.toList());
		}
		return result;
	}
	
	protected Map<String, ?> addTime(Map<String, ?> m) {
		Map<String,Object> result = new TreeMap(m);
		try {
			Date date = df.parse(m.get("date").toString());
			result.put("time", date);
		} catch (ParseException e) {
		}
		return result;
	}

	public ITable getTable() throws  DataException {
		setup();
		return this.filteredTable;
	}

	public long size() throws  DataException {
		setup();
		return this.filteredTable.getRowCount();
	}
	

	
}
