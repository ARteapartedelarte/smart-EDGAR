package ch.pschatzmann.edgar.test;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import ch.pschatzmann.common.table.FormatException;
import ch.pschatzmann.common.table.ITable;
import ch.pschatzmann.common.table.TableCalculated;
import ch.pschatzmann.common.table.TableFormatterCSV;
import ch.pschatzmann.common.table.Value;
import ch.pschatzmann.common.utils.Tuple;
import ch.pschatzmann.edgar.base.EdgarCompany;
import ch.pschatzmann.edgar.base.errors.DataException;
import ch.pschatzmann.edgar.dataload.online.CompanyInformation;
import ch.pschatzmann.edgar.reporting.company.CompanyEdgarValuesBase;
import ch.pschatzmann.edgar.reporting.company.CompanyEdgarValuesDB;
import ch.pschatzmann.edgar.reporting.company.CompanyEdgarValuesEdgar;
import ch.pschatzmann.edgar.reporting.company.CompanyEdgarValuesFile;
import ch.pschatzmann.edgar.reporting.company.CompanyEdgarValuesRest;
import ch.pschatzmann.edgar.reporting.company.CompanySelection;
import ch.pschatzmann.edgar.reporting.company.FilterQuarterlyCumulated;
import ch.pschatzmann.edgar.reporting.company.FilterYearly;
import ch.pschatzmann.edgar.reporting.company.ICompanyInfo;
import ch.pschatzmann.edgar.table.forecast.ForecastQuartersCumulatedARIMA;
import ch.pschatzmann.edgar.utils.Utils;

public class TestCompanyValues {
	
	@BeforeClass
	public static void setup() {
		if (!new File(Utils.getProperty("destinationFolder","/data/SmartEdgar")).exists()) {
			String path = System.getProperty("user.dir")+"/src/test/resources";
			System.setProperty("destinationFolder", path);
		}
	}

	
	@Test
	public void testCompanyEdgarValuesDB() throws Exception {
		CompanyEdgarValuesBase values = new CompanyEdgarValuesDB(new CompanySelection().setTradingSymbol("AAPL"))
			.setParameterNames("NetIncomeLoss");
		
		System.out.println(values.size());
		Assert.assertTrue(values.size()>0);
		Assert.assertTrue(((Number) values.toList().get(10).get("NetIncomeLoss")).doubleValue() > 0.0);

	}
	
	@Test
	public void testCompanyEdgarValuesFile() throws Exception {
		CompanyEdgarValuesBase values = new CompanyEdgarValuesFile(new EdgarCompany("0000320193"))
			.setParameterNames("NetIncomeLoss");
		
		System.out.println(values.size());
		Assert.assertTrue(values.size()>0);
		Assert.assertTrue(((Number) values.toList().get(4).get("NetIncomeLoss")).doubleValue() > 0.0);
	}
	
	@Test
	public void testCompanyEdgarValuesEdgar() throws Exception {
		CompanyEdgarValuesBase values = new CompanyEdgarValuesEdgar(new EdgarCompany("0000320193"))
			.setFilter(new FilterYearly())
			.setParameterNames("NetIncomeLoss");
		
		System.out.println(values.size());
		Assert.assertTrue(values.size()>0);
		
		List<String> years = (List<String>) values.getTable().getColumnKeyValues("date").stream()
				.map(s -> s.toString().substring(0,4))
				.collect(Collectors.toList());
		System.out.println(years);
		Assert.assertTrue(years.contains("2008"));
		Assert.assertTrue(years.contains("2009"));
		Assert.assertTrue(years.contains("2010"));

		Assert.assertTrue(((Number) values.toList().get(4).get("NetIncomeLoss")).doubleValue() > 0.0);
		
	}
	
	@Test
	public void testCompanyEdgarValuesEdgarIntl() throws Exception {
		CompanyEdgarValuesBase values = new CompanyEdgarValuesEdgar(new EdgarCompany("INTL"))
			.setFilter(new FilterYearly())
			.setParameterNames("NetIncomeLoss");
		
		System.out.println(values.size());
		Assert.assertTrue(values.size()>0);
		
		List<String> years = (List<String>) values.getTable().getColumnKeyValues("date").stream()
				.map(s -> s.toString().substring(0,4))
				.collect(Collectors.toList());
		System.out.println(years);
		Assert.assertTrue(years.contains("2009"));
		Assert.assertTrue(years.contains("2010"));
		Assert.assertTrue(years.contains("2011"));
		Assert.assertTrue(years.contains("2012"));
		Assert.assertTrue(years.contains("2013"));

		Assert.assertTrue(((Number) values.toList().get(4).get("NetIncomeLoss")).doubleValue() > 0.0);
		
	}


	@Test
	public void testCompanyEdgarValuesRest() throws Exception {
		ICompanyInfo values = new CompanyEdgarValuesRest(new CompanySelection().setCompanyNumber("0000320193"))
			.setParameterNames("NetIncomeLoss");
		
		System.out.println(values.size());
		Assert.assertTrue(values.size()>0);
		Assert.assertTrue(((Number) values.toList().get(4).get("NetIncomeLoss")).doubleValue() > 0.0);
	}

	
	@Test
	public void testCompanyEdgarValuesYearly() throws Exception {
		CompanyEdgarValuesBase values = new CompanyEdgarValuesDB(new CompanySelection().setTradingSymbol("AAPL"))
			.setFilter(new FilterYearly());

		System.out.println(values.size());
		Assert.assertTrue(values.size()>0);
		Assert.assertTrue(((Number) values.toList().get(5).get("NetIncomeLoss")).doubleValue() > 0.0);

	}

	@Test
	public void testCompanyEdgarValuesDBQuarterlyCumulated() throws Exception {
		CompanyEdgarValuesBase values = new CompanyEdgarValuesDB(new CompanySelection().setTradingSymbol("AAPL"))
			.setFilter(new FilterQuarterlyCumulated())
			.setAddTime(true)
			.setParameterNames("NetIncomeLoss","OperatingIncomeLoss","EarningsPerShareBasic");

		System.out.println(values.size());
		Assert.assertTrue(values.size()>0);
		List<Map<String,?>> list = new ArrayList(values.toList());
		Assert.assertTrue(((Number) list.get(1).get("NetIncomeLoss")).doubleValue() > 0.0);
		
		System.out.println(new TableFormatterCSV().format(values.getTable()));
		System.out.println("--");
		System.out.println(list);
		
	}
	
	
	@Test
	public void testCompanyEdgarValuesFilter() throws ClassNotFoundException, DataException, SQLException {
		CompanyEdgarValuesBase test = new CompanyEdgarValuesDB(new CompanySelection().setTradingSymbol("AAPL"))
			    .setFilter(new FilterYearly(),false)
			    .setParameterNames("NetIncomeLoss","OperatingIncomeLoss","Test");
		
		List<Map<String, ?>> list = test.toList();
		Assert.assertTrue(((Number) list.get(1).get("NetIncomeLoss")).doubleValue() > 0.0);
		Assert.assertTrue(((Number)test.getTable().getValue(0, 0)).doubleValue() > 0);
		Assert.assertTrue(((Number)test.getTable().getValue(0, 1)).doubleValue() > 0);
		Assert.assertTrue(((Number)test.getTable().getValue(0, 2)).doubleValue() > 0);

	}


	
	@Test
	public void testFormulas() throws ClassNotFoundException, DataException, SQLException, FormatException {
		CompanyEdgarValuesBase test = new CompanyEdgarValuesDB(new CompanySelection().setTradingSymbol("AAPL"))
			    .setFilter(new FilterYearly(),false)
			    .setParameterNames("NetIncomeLoss","OperatingIncomeLoss")
			    .addFormula("NetIncome", "Edgar.coalesce('NetIncomeLoss','OperatingIncomeLoss')")
			    .addFormula("NetIncomeMio", "NetIncomeLoss / 1000000.0")
			    .addFormula("NetIncomeLoss-1", "Edgar.lag('NetIncomeLoss',-1)");
		

		System.out.println(new TableFormatterCSV().format(test.getTable()));
		
		List<Map<String, ?>> list = test.toList();

		Assert.assertTrue(((Number) list.get(1).get("NetIncomeLoss")).doubleValue() > 0.0);
		Assert.assertTrue(((Number) list.get(1).get("NetIncome")).doubleValue() > 0.0);
		Assert.assertTrue(((Number) list.get(1).get("NetIncomeMio")).doubleValue() > 0.0);
	}
	
	@Test
	public void testFormulas1() throws ClassNotFoundException, DataException, SQLException, FormatException {
		CompanyEdgarValuesBase values = new CompanyEdgarValuesDB(new CompanySelection().setTradingSymbol("AAPL"))
			    .setFilter(new FilterYearly())
			    .setUseArrayList(true)
			    .setParameterNames("NetIncomeLoss","OperatingIncomeLoss","ResearchAndDevelopmentExpense",
			        "CashAndCashEquivalentsAtCarryingValue","AvailableForSaleSecuritiesCurrent","AccountsReceivableNetCurrent",
			        "Revenues","SalesRevenueNet","InventoryNet","AssetsCurrent","LiabilitiesCurrent","Assets","EarningsPerShareBasic",
			        "StockholdersEquity")
			    .addFormula("Revenue","Edgar.coalesce('Revenues', 'SalesRevenueNet')")
			    .addFormula("QuickRatio","(CashAndCashEquivalentsAtCarryingValue + AccountsReceivableNetCurrent + AvailableForSaleSecuritiesCurrent) / LiabilitiesCurrent")
			    .addFormula("CurrentRatio","AssetsCurrent / LiabilitiesCurrent")
			    .addFormula("InventoryTurnover","Revenue / InventoryNet")
			    .addFormula("NetProfitMargin","NetIncomeLoss / Revenue")
			    .addFormula("SalesResearchRatio%","ResearchAndDevelopmentExpense / Revenue *100")
			    .addFormula("NetIncomeResearchRatio%","ResearchAndDevelopmentExpense / NetIncomeLoss * 100")
			    .addFormula("NetIncomeChange%", "NetIncomeLoss - Edgar.lag('NetIncomeLoss', -1) / Edgar.lag('NetIncomeLoss', -1) * 100 ")  
			    .addFormula("RevenueChange%", "Edgar.percentChange('Revenue')" )  
			    .addFormula("ResearchAndDevelopmentChange%","Edgar.percentChange('ResearchAndDevelopmentExpense')" )
			    .removeParameterNames("Revenues","SalesRevenueNet");
		
		System.out.println(new TableFormatterCSV<Number>().format(values.getTable()));
		List<Map<String, Number>> list = values.getTable().toList();
		
		
		Assert.assertNotNull(list.get(3).get("Assets"));
		Assert.assertNotNull(list.get(3).get("ResearchAndDevelopmentChange%"));
		Assert.assertNotNull(list.get(3).get("NetProfitMargin"));

		
	}
	
	
	@Test
	public void testFormulasFile() throws ClassNotFoundException, DataException, SQLException, FormatException {
		CompanyEdgarValuesBase values = new EdgarCompany("320193").getCompanyEdgarValues()
			    .setFilter(new FilterYearly())
			    .setUseArrayList(true)
			    .setAddTime(true)
			    .setParameterNames("NetIncomeLoss","OperatingIncomeLoss","ResearchAndDevelopmentExpense",
			        "CashAndCashEquivalentsAtCarryingValue","AvailableForSaleSecuritiesCurrent","AccountsReceivableNetCurrent",
			        "Revenues","SalesRevenueNet","InventoryNet","AssetsCurrent","LiabilitiesCurrent","Assets","EarningsPerShareBasic",
			        "StockholdersEquity")
			    .addFormula("Revenue","Edgar.coalesce('Revenues', 'SalesRevenueNet')")
			    .addFormula("QuickRatio","(CashAndCashEquivalentsAtCarryingValue + AccountsReceivableNetCurrent + AvailableForSaleSecuritiesCurrent) / LiabilitiesCurrent")
			    .addFormula("CurrentRatio","AssetsCurrent / LiabilitiesCurrent")
			    .addFormula("InventoryTurnover","Revenue / InventoryNet")
			    .addFormula("NetProfitMargin","NetIncomeLoss / Revenue")
			    .addFormula("SalesResearchRatio%","ResearchAndDevelopmentExpense / Revenue *100")
			    .addFormula("NetIncomeResearchRatio%","ResearchAndDevelopmentExpense / NetIncomeLoss * 100")
			    .addFormula("NetIncomeChange%", "NetIncomeLoss - Edgar.lag('NetIncomeLoss', -1) / Edgar.lag('NetIncomeLoss', -1) * 100 ")  
			    .addFormula("RevenueChange%", "Edgar.percentChange('Revenue')" )  
			    .addFormula("ResearchAndDevelopmentChange%","Edgar.percentChange('ResearchAndDevelopmentExpense')" )
			    .removeParameterNames("Revenues","SalesRevenueNet");
		
		System.out.println(values.getTable().getRowValue(2));		
		System.out.println(new TableFormatterCSV<Number>().format(values.getTable()));
		
		Assert.assertFalse(Utils.isEmpty(values.getTable().getRowValue(2).get(4).toString()));
		Assert.assertFalse(Utils.isEmpty(values.getTable().getRowValue(2).get(5).toString()));
		Assert.assertFalse(Utils.isEmpty(values.getTable().getRowValue(2).get(6).toString()));
		List<Map<String, Number>> list = values.getTable().toList();
		
		Assert.assertFalse(list.isEmpty());
		Assert.assertNotNull(list.get(2).get("Assets"));
		Assert.assertNotNull(list.get(2).get("ResearchAndDevelopmentChange%"));
		Assert.assertNotNull(list.get(2).get("NetProfitMargin"));

		
	}
	
	
	@Test
	public void testForecast() throws ClassNotFoundException, DataException, SQLException, FormatException {
		CompanyEdgarValuesBase test = new CompanyEdgarValuesDB(new CompanySelection().setTradingSymbol("AAPL"))
			    .setFilter(new FilterQuarterlyCumulated(),true)
			    .setParameterNames("NetIncomeLoss")
			    .addFormula("NetIncomeForecast", "Edgar.forecast('NetIncomeLoss')");
		
		System.out.println(new TableFormatterCSV().format(test.getTable()));
		
		List<Tuple> values =  (List<Tuple>) test.toList().stream()
			.map(r -> ((Map<String,Number>)r))
			.map(r -> new Tuple(r.get("NetIncomeLoss").doubleValue(),r.get("NetIncomeForecast").doubleValue()))
			.filter(t -> Double.isFinite((double)t.x) && Double.isFinite((double)t.y) && (double)t.y > 0.0)
			.collect(Collectors.toList());
		
		Assert.assertTrue(!values.isEmpty());
		
		double rsme = Math.sqrt(values.stream().map(t -> Math.pow(((double)t.y -(double)t.x),2)).mapToDouble(d -> d).sum() / (double) values.size());
		System.out.println("rsmd (with regression): "+rsme);
		
	}
	
	@Test
	public void testForecastARIMA() throws ClassNotFoundException, DataException, SQLException, FormatException {
		CompanyEdgarValuesBase test = new CompanyEdgarValuesDB(new CompanySelection().setTradingSymbol("AAPL"))
			    .setFilter(new FilterQuarterlyCumulated(),true)
			    .setParameterNames("NetIncomeLoss")
			    .addFormula("NetIncomeForecast", "Edgar.forecast('NetIncomeLoss')");
		
		TableCalculated tc = (TableCalculated) test.getTable().getBaseTableOfClass("TableCalculated");
		tc.getFormulas().setForecast(new ForecastQuartersCumulatedARIMA());
		System.out.println(new TableFormatterCSV().format(test.getTable()));
		
		List<Tuple> values =  (List<Tuple>) test.toList().stream()
			.map(r -> ((Map<String,Number>)r))
			.map(r -> new Tuple(r.get("NetIncomeLoss").doubleValue(),r.get("NetIncomeForecast").doubleValue()))
			.filter(t -> Double.isFinite((double)t.x) && Double.isFinite((double)t.y) && (double)t.y > 0.0)
			.collect(Collectors.toList());
		
		Assert.assertTrue(!values.isEmpty());
		
		double rsme = Math.sqrt(values.stream().map(t -> Math.pow(((double)t.y -(double)t.x),2)).mapToDouble(d -> d).sum() / (double) values.size());
		System.out.println("rsmd (with regression): "+rsme);
		
	}
	
	@Test
	public void testForecastAvg() throws ClassNotFoundException, DataException, SQLException, FormatException {
		CompanyEdgarValuesBase test = new CompanyEdgarValuesDB(new CompanySelection().setTradingSymbol("AAPL"))
			    .setFilter(new FilterQuarterlyCumulated(),true)
			    .setParameterNames("NetIncomeLoss")
			    .addFormula("NetIncomeForecast", "Edgar.forecast('NetIncomeLoss')");
		
		System.out.println(new TableFormatterCSV().format(test.getTable()));
		
		List<Tuple> values =  (List<Tuple>) test.toList().stream()
			.map(r -> ((Map<String,Number>)r))
			.map(r -> new Tuple(r.get("NetIncomeLoss").doubleValue(),r.get("NetIncomeForecast").doubleValue()))
			.filter(t -> Double.isFinite((double)t.x) && Double.isFinite((double)t.y) && (double)t.y > 0.0)
			.collect(Collectors.toList());
		
		Assert.assertTrue(!values.isEmpty());

		
		double rsme = Math.sqrt(values.stream().map(t -> Math.pow(((double)t.y -(double)t.x),2)).mapToDouble(d -> d).sum() / (double)values.size());
		System.out.println("rsmd - w/o regression: "+rsme);
		
	}

	
	@Test
	public void testCalculatedMarketShare() throws ClassNotFoundException, DataException, SQLException, FormatException {
		CompanyEdgarValuesBase test = new CompanyEdgarValuesDB(new CompanySelection().setTradingSymbol("AAPL"))
			    .setFilter(new FilterQuarterlyCumulated(),true)
			    .setParameterNames("NetIncomeLoss")
			    .addFormula("MarketShare", "Edgar.marketShare()");
		
		System.out.println(new TableFormatterCSV().format(test.getTable()));

		Assert.assertEquals(38.01, ((Value)test.toList().get(1).get("MarketShare")).doubleValue(),0.1);
		
	}
	

}
