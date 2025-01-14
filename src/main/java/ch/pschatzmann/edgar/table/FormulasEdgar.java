package ch.pschatzmann.edgar.table;

import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.log4j.Logger;

import ch.pschatzmann.common.table.IForecast;
import ch.pschatzmann.common.table.IFormulas;
import ch.pschatzmann.common.table.ITable;
import ch.pschatzmann.common.table.TableCalculated;
import ch.pschatzmann.edgar.reporting.marketshare.MarketShare;
import ch.pschatzmann.edgar.table.forecast.ForecastLinearRegression;
import ch.pschatzmann.edgar.table.forecast.ForecastQuarters;
import ch.pschatzmann.edgar.table.forecast.ForecastQuartersCumulated;

/**
 * Support for Forecasts, Surprises and Market Share calculations
 * 
 * @author pschatzmann
 *
 */
public class FormulasEdgar extends ch.pschatzmann.common.table.Formulas implements IFormulas {
	private static final Logger LOG = Logger.getLogger(FormulasEdgar.class);
	private static MarketShare marketShare = null;
	
	
	public FormulasEdgar() {
	}

	protected FormulasEdgar(TableCalculated table, int row) {
		super(table, row);
	}
	

	@Override
	public FormulasEdgar create (TableCalculated table, int row) {
		FormulasEdgar result = new FormulasEdgar(table, row);
		// setup the forecoast in the template
		if (this.getForecast()==null) {
			this.setForecast(result.createDefaultForecast());
		} else {
			// we copy the forecast from the template
			result.setForecast(this.getForecast());
		}
		return result;
	}

	protected IForecast createDefaultForecast() {
		Set<String> months = getMonths(this.getTable().getBaseTableWithFields("numberOfMonths"));
		if(months.contains("6")) {
			setForecast(new ForecastQuartersCumulated(false));
		} else if (months.contains("3")){
			setForecast(new ForecastQuarters(false));				
		} else {
			setForecast(new ForecastLinearRegression());
		}
		return this.getForecast();
	}

	protected Set<String> getMonths(ITable baseTableWithFields) {
		Set<String> result = new TreeSet();
		int monthsIndex = baseTableWithFields.getRowFieldNames().indexOf("numberOfMonths");
		if (monthsIndex>=0) {
			result = IntStream.range(0, baseTableWithFields.getRowCount())
			.mapToObj(row -> (String)baseTableWithFields.getRowValue(row).get(monthsIndex))
			.collect(Collectors.toSet());
		}
		return result;
	}
	/**
	 * Forecasts the indicated parameter
	 * @param parName
	 * @return
	 */
	public Number forecast(String parName) {
		IForecast forecast = this.getForecast();
		forecast.setTable(this.getTable());
		return forecast.forecast(parName, super.row());
	}


	/**
	 * Calculate % difference to forecast
	 * @param parName
	 * @return
	 */
	public Number surprisePercent(String parName) {
		Number result = null;
		try {
			Number forecast = forecast(parName);
			if (forecast != null) {
				double forecastDouble = forecast.doubleValue();
				result = (value(parName).doubleValue() - forecastDouble) / forecastDouble * 100;
			}
		} catch (Exception ex) {
			LOG.error(ex,ex);
		}
		return result;
	}

	/**
	 * Calculates the market share of the current company for the indicated year
	 * @return
	 */
	public Number marketShare()  {
		Number result = null;
		try {
			if (marketShare == null) {
				marketShare = new MarketShare();
			}
			String companyNr = this.getTable().getRowValue(this.row(),"identifier");
			String year = this.getTable().getRowValue(this.row(),"date").substring(0, 4);
			result = marketShare.getMarketShare(companyNr, year);
		} catch(Exception ex) {
			LOG.error(ex,ex);
		};
		return result;
	}


}