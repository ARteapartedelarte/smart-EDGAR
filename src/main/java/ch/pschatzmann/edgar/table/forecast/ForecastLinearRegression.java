package ch.pschatzmann.edgar.table.forecast;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import ch.pschatzmann.common.table.IForecast;
import ch.pschatzmann.common.table.TableCalculated;

/**
 * Calculates the forecasted value for the indicated row.
 * We calculate the trend with a linear regression and predict the value with 
 * the average seasonal factors
 * 
 * @author pschatzmann
 *
 */
public class ForecastLinearRegression implements IForecast {
	private TableCalculated table;
	private List<String> parameterNames;
	
	public ForecastLinearRegression() {
	}

	public Double forecast(String parameter, int row) {
		Double result = null;
		if (row>1) {
			// get history
			List<ForecastValue> past = getHistory(parameter, row-1);

			// Execute regression for each step to calculate the trend
			List<Double> pastValues = past.stream().map(v -> v.value).collect(Collectors.toList());
			past.stream().forEach(fv -> fv.valueRegression =  linearRegressionFor(fv.index, pastValues));

			ForecastValue current = past.get(past.size()-1);
						
			// Calculate  predicted value
			result = predict(current);	
		}
		
		return result;
	}

	protected List<ForecastValue> getHistory(String parameterName, int toRow) {
		int col = this.getParameterNames().indexOf(parameterName);
		List<ForecastValue> values = new ArrayList();
		// history
		for (int row = 0; row <= toRow; row++) {
			Number value = table.eval(row, table.getColumnTitle(col));
			values.add(setValue(new ForecastValue(row),values, value));
		}
		// forecasted current value
		values.add(setValue(new ForecastValue(values.size()),values, null));
		return values;
	}
	
	protected List<String> getParameterNames() {
		if (this.parameterNames==null) {
			this.parameterNames = IntStream.range(0, table.getColumnCount())
				    .mapToObj(col -> table.getColumnTitle(col))
				    .collect(Collectors.toList());
		}
		return this.parameterNames;
	}

	
	protected ForecastValue setValue(ForecastValue v,List<ForecastValue> list, Number valueNumber){
		v.prior = list.get(v.index-1);
		if (valueNumber!=null) {
			try {
				v.value = valueNumber.doubleValue();
				v.seasonIndex = v.index % 4;
			} catch(Exception ex) {
				ex.printStackTrace();
			}
		} else {
			v.seasonIndex = (v.prior.seasonIndex + 1);
			if (v.seasonIndex==4) v.seasonIndex = 0;
		}
		return v;		
	}
	
	protected Double predict(ForecastValue v) {
		Double result = null;
		if (v.prior!=null) {
			result = v.valueRegression ;			
		}
		return result;
	}

	protected double linearRegressionFor(int row, List<Double> past) {
		double regressionResult = 0;
		if (past.size()>1) {
			SimpleRegression regression = new SimpleRegression();
			for (int j=0;j<past.size();j++) {
				regression.addData(j, past.get(j));
			}
			regressionResult = regression.predict(row);
		} else if (past.size()==1){
			regressionResult = past.get(0);
		}
		return regressionResult;
	}

	@Override
	public void setTable(TableCalculated table) {
		this.table = table;
	}	
	
}
