package ch.pschatzmann.edgar.reporting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import ch.pschatzmann.edgar.base.errors.DataException;

/**
 * Implementation which supports the proper handling of alternative parameter
 * names as specified by priority in the SQL mapping table
 * 
 * @author pschatzmann
 *
 */

public class PostgresSQLModelExt extends PostgresSQLModel {
	private static final Logger LOG = Logger.getLogger(PostgresSQLModelExt.class);

	/**
	 * Default Constructor
	 */
	public PostgresSQLModelExt(AbstractModel model) {
		super(model);
	}

	/**
	 * Generate SQL command based on the selected rows, column and value field
	 * When the standardparameter is used we execute the query in two steps.
	 * First we get the valid parameters per compny and then we consolidate the
	 * values
	 * 
	 * @return
	 * @throws DataException 
	 */
	public String toSQL(Table table) throws DataException {
		StringBuffer sb = new StringBuffer();
		// use standard logic of not joined with grouping table
		if (!withStandardParameter(table)) {
			return super.toSQL(table);
		}
		
		// extended logic using the priorities in the mapping table
		sb.append("WITH companyValues as ( ");
		List<NavigationField> group = new ArrayList(table.getGroupFields());
		NavigationField identifier = this.getModel().getNavigationField("values", "identifier", null);
		NavigationField priority = this.getModel().getNavigationField("mappings", "priority", null);
		List<NavigationField> identifierAndGroups = new ArrayList<NavigationField>() {
			{
				add(identifier);
				addAll(group);
			}
		};
		List<NavigationField> identifierAndGroupsAndPriority = new ArrayList<NavigationField>() {
			{
				addAll(identifierAndGroups);
				add(priority);
			}
		};

		sqlSelectFields(table, sb, identifierAndGroups, identifierAndGroups);
		sqlFrom(table, sb);
		sqlJoin(table, sb);
		sqlWhere(sb);
		sqlGroupBy(table, identifierAndGroupsAndPriority, sb, true);

		sqlOrderBy(identifierAndGroupsAndPriority, sb);
		sb.append(" ) ");
		sqlSelectFields(table, sb, table.getGroupFields(), new ArrayList(), false);
		sb.append(" FROM companyValues ");
		sqlGroupBy(table, table.getGroupFields(), sb, false);

		String result = sb.toString();
		LOG.info(result);
		return result;
	}

	protected boolean withStandardParameter(Table table) {
		return getGroupAndFilterFieldNames(table).contains("standardparameter");
	}

	protected Collection<String> getGroupAndFilterFieldNames(Table table) {
		Collection<String> result = new TreeSet();
		for (NavigationField fld : this.getModel().getFilterFields()) {
			result.add(fld.getFieldName());
		}
		for (NavigationField fld : table.getGroupFields()) {
			result.add(fld.getFieldName());
		}
		return result;
	}

	protected void sqlOrderBy(List<NavigationField> fields, StringBuffer sb) {
		if (!fields.isEmpty()) {
			sb.append(" ORDER BY ");
			boolean first = true;
			for (NavigationField fld : fields) {
				if (!first)
					sb.append(", ");
				first = false;
				sb.append(fld.isCalculated() ? fld.getFieldName() : fld.getFieldNameExt());
			}
		}
	}
}
