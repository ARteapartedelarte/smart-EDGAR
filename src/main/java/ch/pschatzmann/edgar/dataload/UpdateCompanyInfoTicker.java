package ch.pschatzmann.edgar.dataload;

import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.SQLException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import com.rometools.rome.io.FeedException;

import ch.pschatzmann.edgar.base.EdgarCompany;
import ch.pschatzmann.edgar.base.errors.DataException;
import ch.pschatzmann.edgar.dataload.online.TradingSymbol;
import ch.pschatzmann.edgar.reporting.company.CompanySearch;
import ch.pschatzmann.edgar.utils.Utils;

/**
 * Update the ticker symbol from the ownership filings
 * 
 * @author pschatzmann
 *
 */

public class UpdateCompanyInfoTicker {
	private static final Logger LOG = Logger.getLogger(DownloadProcessorJDBC.class);
	private static TableFactory tableFactory = new TableFactory();

	public static void main(String[] args) {
		try {
			update();
		} catch (Exception ex) {
			LOG.error(ex, ex);
		}
	}

	public static void update() throws ClassNotFoundException, SQLException, SAXException, IOException, ParserConfigurationException, IllegalArgumentException, FeedException, DataException {
		tableFactory.openConnection();

		java.util.List<EdgarCompany> companies = new CompanySearch("tradingSymbol","").toListOfEdgarCompany();				
		for (EdgarCompany c : companies) {
			update(c);
		}
	}

		
	public static boolean update(EdgarCompany c) throws MalformedURLException, FeedException, IOException,
			ClassNotFoundException, DataException, SQLException {
		
		boolean result = false;
		String tradingSymbol = c.getTradingSymbol();
		if (Utils.isEmpty(tradingSymbol)) {
			tradingSymbol = new TradingSymbol(c.getCompanyNumber()).getTradingSymbol();
		}

		EdgarCompany ci = new EdgarCompany(c.getCompanyNumber());
		if (!Utils.equals(tradingSymbol, ci.getTradingSymbol())) {
			ci.setTradingSymbol(tradingSymbol);
			ci.saveFile();
			result = true;
		}
					
		if (!Utils.isEmpty(tradingSymbol) && !tradingSymbol.equals(c.getTradingSymbol())) {
			String sql = "update company set tradingSymbol='%ts' where identifier = '%id' ";
			sql = sql.replace("%id", c.getCompanyNumber());
			sql = sql.replace("%ts", tradingSymbol);
			LOG.info(sql);
			tableFactory.execute(sql);
			tableFactory.commit();
			result = true;
		}
		
		return result;
	}



}
