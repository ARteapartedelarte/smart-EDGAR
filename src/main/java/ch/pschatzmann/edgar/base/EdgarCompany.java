package ch.pschatzmann.edgar.base;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rometools.rome.io.FeedException;

import ch.pschatzmann.edgar.base.Fact.Type;
import ch.pschatzmann.edgar.base.errors.DataException;
import ch.pschatzmann.edgar.dataload.online.TradingSymbol;
import ch.pschatzmann.edgar.reporting.company.CompanyEdgarValuesBase;
import ch.pschatzmann.edgar.reporting.company.CompanyEdgarValuesFile;
import ch.pschatzmann.edgar.service.EdgarFileService;
import ch.pschatzmann.edgar.utils.Utils;

/**
 * The Standard Industrial Code SIC information and the states information is
 * not available via the SEC filings. We scrape it from the html company
 * information provided by the SEC.
 * 
 * @author pschatzmann
 *
 */
public class EdgarCompany implements ICompany, Serializable, Comparable<EdgarCompany> {
	private static final long serialVersionUID = 1L;
	private static final Logger LOG = Logger.getLogger(EdgarCompany.class);
	private String companyNumber = "";
	private String sicCode = "";
	private String sicDescription = "";
	private String companyName = "";
	private String stateLocation = "";
	private String stateIncorporation = "";
	private String tradingSymbol=null;

	/**
	 * Empty Constructor which gives an empty object
	 */
	public EdgarCompany() {		
	}
	
	/**
	 * We use the cik to look up the information from the Company json file. If the file 
	 * does not exist we create it.
	 * @param cik
	 */
	public EdgarCompany(String cik) {
		this.companyNumber = cik;
		try {
			if (!loadFile()) {
				// update scraped information
				scrape();
				setupTradingSymbol();
				saveFile();
			}
		} catch (Exception ex) {
			LOG.error(ex, ex);
		}
	}

	/**
	 * 
	 * @param xbrl
	 */
	public EdgarCompany(XBRL xbrl) {
		boolean save = false;
		this.companyNumber = xbrl.getCompanyNumber();
		
		if (!Utils.isEmpty(this.companyNumber)) {		
			try {
				if (!Utils.isEmpty(getFolderName())) {
					if (!loadFile()) {
						this.companyNumber = xbrl.getCompanyNumber();
						// update scraped information
						scrape();
						// update ticker
						save = true;
					}
				}
			} catch (Exception ex) {
				LOG.error(ex, ex);
			}
			
			if (Utils.isEmpty(this.getCompanyName())) {
				Iterator it = xbrl.find("EntityRegistrantName", Arrays.asList(Type.value)).iterator();
				if (it.hasNext()) {
					this.setCompanyName(((FactValue) it.next()).getValue());
				}
			}
			
			// we setup the trading symbol
			String current = this.getTradingSymbol();
			if (Utils.isEmpty(this.getTradingSymbol())) {
				Iterator<Fact> it = xbrl.find("TradingSymbol", Arrays.asList(Type.value)).iterator();
				if (it.hasNext()) {
					this.setTradingSymbol(((FactValue) it.next()).getValue());
				}
				this.setupTradingSymbol();
			}
			
			if (!Utils.equals(this.getTradingSymbol(),current)){
				save = true;
			}
			
			if (save) {
				this.saveFile();
			}
		}
	}

	public EdgarCompany(Map<String, ?> m) {
		this.companyNumber = (String)m.get("identifier");
		this.companyName = (String)m.get("companyName");
		this.tradingSymbol = (String)m.get("tradingSymbol");
		this.stateIncorporation = (String)m.get("incorporation");
		this.stateLocation = (String)m.get("location");
		this.sicDescription = (String)m.get("sicDescription");
		this.sicCode = (String) m.get("sicCode");
	}

	public void setupTradingSymbol()  {
		// we get the symbol from the filing
		try {
			if (Utils.isEmpty(this.tradingSymbol)) {
				this.tradingSymbol = this.getTradingSymbolEx();
			}
		} catch(Exception ex) {
			this.tradingSymbol = "";
			LOG.error(ex,ex);
		}
	}

	private void copyFrom(EdgarCompany ci) {
		if (!Utils.isEmpty(ci.companyNumber)) {
			this.companyNumber = ci.companyNumber;
		}
		this.sicCode = ci.sicCode;
		this.sicDescription = ci.sicDescription;
		this.companyName = ci.companyName;
		this.stateLocation = ci.stateLocation;
		this.stateIncorporation = ci.stateIncorporation;
		this.tradingSymbol = ci.tradingSymbol;
	}

	private String fileName(String name) {
		return Utils.getDestinationFolder()+"/" + name + "/company-" + name + ".json";
	}

	public String getCompanyName() {
		return this.companyName;
	}

	public String getCompanyNumber() {
		return companyNumber;
	}

	@JsonIgnore
	public String getFolderName() {
		return companyNumber.replaceFirst("^0+(?!$)", "");
	}

	public String getIncorporationState() {
		return stateIncorporation;
	}

	public String getLocationState() {
		return this.stateLocation;
	}


	public String getSICCode() {
		return this.sicCode;
	}

	public String getSICDescription() {
		if (!this.sicDescription.isEmpty()) {
			return Utils.cleanHtmlEscapes(sicDescription);
		}
		if (this.sicCode.equals("3949")) {
			return "Sporting and Athletic Goods, not elsewhere classified";
		}
		if (this.sicCode.equals("6221")) {
			return "Commodity Contracts Brokers and Dealers";

		}
		if (this.sicCode.equals("9995")) {
			return "Non-operating establishments";
		}
		return "";
	}


	public String getTradingSymbol() {
		return this.tradingSymbol;
	}
	
	@JsonIgnore
	public String getTradingSymbolEx() throws MalformedURLException, ClassNotFoundException, FeedException, IOException, DataException, SQLException {
		return !Utils.isEmpty(this.tradingSymbol) ? this.tradingSymbol : new TradingSymbol(this.companyNumber).getTradingSymbol();
	}

	public boolean loadFile() {
		boolean result = false;
		try {
			ObjectMapper mapper = new ObjectMapper();
			String name = getFolderName();
			String filePath = fileName(name);
			LOG.info("Reading "+filePath);
			EdgarCompany ci = mapper.readValue(new File(filePath),EdgarCompany.class);
			copyFrom(ci);
			result = true;
		} catch (Exception ex) {
			LOG.warn(ex);
		}
		return result;
	}

	public void saveFile() {
		try {
			ObjectMapper mapper = new ObjectMapper();
			String name = getFolderName();
			if (new File(Utils.getDestinationFolder()).exists()) {
				mapper.writeValue(new File(fileName(name)), this);
			}
		} catch (Exception ex) {
			LOG.warn(ex, ex);
		}
	}

	public void scrape() throws MalformedURLException, IOException {
		if (!Utils.isEmpty(this.companyNumber)) {
			try {
				LOG.info("scrape "+this.companyNumber);
				String url = "https://www.sec.gov";
				String urlPath = "/cgi-bin/browse-edgar?CIK=%1&Find=Search&output=atom&owner=exclude&action=getcompany&type=NA&count=0"
						.replaceAll("%1", this.companyNumber);
				String input = url + urlPath;
				LOG.info("parsing " + input);
				DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = domFactory.newDocumentBuilder();
				Document doc = builder.parse(input);
				XPath xpath = XPathFactory.newInstance().newXPath();

				// XPath Query for showing all nodes value
				companyName = xpath.compile("//conformed-name").evaluate(doc);
				companyName = Utils.cleanHtmlEscapes(companyName);
				companyName = companyName.replaceAll("'", "");
				sicCode = xpath.compile("//assigned-sic").evaluate(doc);
				sicDescription = xpath.compile("//assigned-sic-desc").evaluate(doc);
				sicDescription = Utils.cleanHtmlEscapes(sicDescription);
				sicDescription = sicDescription.replaceAll("'", "");
				
				stateLocation = xpath.compile("//state-location").evaluate(doc);
				stateIncorporation = xpath.compile("//state-of-incorporation").evaluate(doc);

			} catch (Exception ex) {
				throw new IOException(ex);
			}
		} else {
			LOG.info("CompanyIformation not determineded because the companyNumber is not defined");
		}
	}

	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}
	
	public void setIncorporationState(String stateIncorporation) {
		this.stateIncorporation = stateIncorporation;
	}

	public void setLocationState(String stateLocation) {
		this.stateLocation = stateLocation;
	}
	
	public void setCompanyNumber(String cik) {
		this.companyNumber = cik;
	}
	
	public void setSICCode(String c) {
		this.sicCode = c;
	}
	
	public void setSICDescription(String d) {
		this.sicDescription = d;
	}
	
	public void setTradingSymbol(String ts) {
		this.tradingSymbol = ts;
	}
 
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(this.getCompanyName());
		sb.append(" (");
		sb.append(this.getCompanyNumber());
		sb.append(")");
		return sb.toString();
	}
	
	/**
	 * Lists all entries
	 * @return
	 */
	public static List<EdgarCompany> list (){
		return stream().sorted().collect(Collectors.toList());
	}
	
	/**
	 * Returns stream of all EdgarCompany objects
	 * @return
	 */
	public static Stream<EdgarCompany> stream () {
		return Arrays.asList(new File(Utils.getDestinationFolder()).listFiles()).stream()
		.filter(f -> f.isDirectory())
		.map(f -> new EdgarCompany(f.getName()));
	}
	
	/**
	 * List all filings for the company
	 * @return
	 */
	@JsonIgnore
	public List<EdgarFiling> getFilings() {
		return EdgarFileService.getFilings(this.getFolderName()).stream()
			.map(f -> new EdgarFiling(f))
			.collect(Collectors.toList());
	}
	
	/**
	 * Loads all filings for a company to one combined XBRL
	 * @return
	 */

	@JsonIgnore
	public XBRL getXBRL() {
		return getXBRL(".*");
	}
	
	/**
	 * Returns the last filed XBRL document
	 * @return
	 * @throws ParserConfigurationException 
	 * @throws IOException 
	 * @throws SAXException 
	 */
	@JsonIgnore
	public XBRL getLastXBRL(String regex) throws SAXException, IOException, ParserConfigurationException {
		List<String> result = EdgarFileService.getFilings(this.getFolderName()).stream()
				.filter(filing -> filing.matches(regex)).collect(Collectors.toList());
		XBRL xbrl = new XBRL();
		if (!result.isEmpty()) {
			xbrl.tryLoad(EdgarFileService.getFile(result.get(result.size()-1)),null);
		}
		return xbrl;
	}

	
	/**
	 * Loads all filings for a company to one combined XBRL
	 * @param regex
	 * @return
	 */
	
	public XBRL getXBRL(String regex) {
		XBRL xbrl = new XBRL();
		xbrl.setConvertHtmlToText(true);
		EdgarFileService.getFilings(this.getFolderName()).stream()
			.filter(filing -> filing.matches(regex))
			.forEach(filing -> xbrl.tryLoad(EdgarFileService.getFile(filing),null));
		return xbrl;
	}

	/**
	 * Access to extended table functionality with calculations 
	 * @return
	 * @throws ClassNotFoundException
	 * @throws DataException
	 * @throws SQLException
	 */
	@JsonIgnore
	public CompanyEdgarValuesBase getCompanyEdgarValues() throws ClassNotFoundException, DataException, SQLException {
		return new CompanyEdgarValuesFile(this);
	}
	
	/**
	 * Access to extended table functionality with calculations 
	 * @param predicate
	 * @return
	 * @throws ClassNotFoundException
	 * @throws DataException
	 * @throws SQLException
	 */
	public CompanyEdgarValuesBase getCompanyEdgarValues(Predicate<FactValue> predicate) throws ClassNotFoundException, DataException, SQLException {
		return new CompanyEdgarValuesFile(this, predicate);
	}

	/**
	 * Compare based on the company number
	 */
	@Override
	public int compareTo(EdgarCompany o) {
		return ((Long)Long.parseLong(this.getCompanyNumber())).compareTo(Long.parseLong(o.getCompanyNumber()));
	}
	
	/**
	 * Two companies are equal if they share the same company number
	 */
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof EdgarCompany)) {
			return false;
		}
		return compareTo((EdgarCompany)obj)==0;
	}


}
