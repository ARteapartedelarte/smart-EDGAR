package ch.pschatzmann.edgar.reporting.company;

import java.util.List;
import java.util.Map;

import ch.pschatzmann.common.table.ITable;
import ch.pschatzmann.edgar.utils.Utils;

/**
 * Returns all 10-K entries for 12 months or 0 months. We need to filter out all
 * balance sheet values which have been reported by 10-K for the quarters
 * because we just want to have the end of year values
 * 
 * @author pschatzmann
 *
 */
public class FilterYearly implements IRowFilter {
	private List<Map<String, ?>> list;
	private PeriodsDetermination periods;
	
	public FilterYearly() {
	}

	@Override
	public Boolean apply(ITable table, Integer row) {
		setup(table);
		Map<String,?> record = list.get(row);
		return periods.isValidYearly(Utils.str(record.get("date")), Utils.str(record.get("form")), Utils.str(record.get("numberOfMonths")));
	}

	protected void setup(ITable table) {
		if (this.list != table) {
			this.list = table.toList();
			periods = new PeriodsDetermination(table);
		}
	}
	
	public String getFileNameRegex() {
		return ".*10-K.*";
	}

	@Override
	public String getRestKey() {
		return "Y";
	}



}
