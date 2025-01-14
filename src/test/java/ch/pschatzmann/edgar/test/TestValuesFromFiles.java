package ch.pschatzmann.edgar.test;

import java.io.File;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import ch.pschatzmann.common.table.ITable;
import ch.pschatzmann.common.table.TableFormatterCSV;
import ch.pschatzmann.common.table.Value;
import ch.pschatzmann.edgar.base.EdgarFiling;
import ch.pschatzmann.edgar.base.FactValue;
import ch.pschatzmann.edgar.base.XBRL;
import ch.pschatzmann.edgar.reporting.ValueField;
import ch.pschatzmann.edgar.utils.Utils;

public class TestValuesFromFiles {
	
	@BeforeClass
	public static void setup() {
		if (!new File(Utils.getProperty("destinationFolder","/data/SmartEdgar")).exists()) {
			String path = System.getProperty("user.dir")+"/src/test/resources";
			System.setProperty("destinationFolder", path);
		}
	}

	
	@Test
	public void testList() throws Exception {
		List<EdgarFiling> list = EdgarFiling.list(".*10-K.*");
		Assert.assertFalse(list.isEmpty());
		EdgarFiling filing = list.get(0);
		Assert.assertNotNull(filing.getCompanyInfo());
		System.out.println("date:"+filing.getDate());
		Assert.assertNotNull(filing.getDate());
		System.out.println("form:"+filing.getForm());
		Assert.assertEquals("10-K",filing.getForm());
		Assert.assertNotNull(filing.getXBRL());
	}
	
	
	@Test
	public void testText() throws Exception {
		List<EdgarFiling> list = EdgarFiling.list(".*10-K.*");
		Assert.assertFalse(list.isEmpty());
		EdgarFiling filing = list.get(0);
		
		List<FactValue> texts = filing.getXBRL().getCombinedTextValues();

		System.out.println("text:"+texts.get(0));
		Assert.assertTrue(texts.get(0).getValue()!=null);
	}
	
	@Test
	public void testText1() throws Exception {
		XBRL xbrl = new XBRL(true,true,false);
		xbrl.setConvertHtmlToText(true);
		File file = new File("./src/test/resources/ste-20170331.xml");
		xbrl.load(file);
		List<FactValue> texts = xbrl.getCombinedTextValues();

		System.out.println("text:"+texts.get(0).getValue());
		Assert.assertTrue(texts.get(0).getValue()!=null);
		Assert.assertFalse(texts.get(0).getValue().contains("<"));

	}
	
	
	
	@Test
	public void testTable() throws Exception {
		XBRL xbrl = new XBRL(true,true,false);
		File file = new File("./src/test/resources/ste-20170331.xml");
		xbrl.load(file);

		ITable<Value> table = xbrl.toTable();
		System.out.println(new TableFormatterCSV().format(table));
		Assert.assertTrue(table.getRowCount()>0);
		Assert.assertTrue(table.getColumnCount()>0);
		
	}

	

}
