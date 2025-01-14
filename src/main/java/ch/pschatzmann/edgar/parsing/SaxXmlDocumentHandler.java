package ch.pschatzmann.edgar.parsing;

import java.io.Serializable;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import ch.pschatzmann.edgar.base.Fact;
import ch.pschatzmann.edgar.base.Fact.Attribute;
import ch.pschatzmann.edgar.base.Fact.Type;
import ch.pschatzmann.edgar.base.FactValue;
import ch.pschatzmann.edgar.base.IndexAPI;
import ch.pschatzmann.edgar.base.XBRL;
import ch.pschatzmann.edgar.utils.Utils;

/**
 * Sax Parser which loads the XML or XLS data
 * 
 * @author pschatzmann
 *
 */

public class SaxXmlDocumentHandler extends DefaultHandler implements Serializable {
	private static final Logger LOG = Logger.getLogger(SaxXmlDocumentHandler.class);
	private StringBuffer value = new StringBuffer();
	private Stack<Fact> factStack = new Stack();
	private IndexAPI index;
	private Fact fact;
	private int level = 0;
	private boolean isFact = false;
	private XBRL xbrl;
	private long line = 0;

	/**
	 * Constructor
	 * 
	 * @param xbrl
	 * @param factRoot
	 * @param isFactFile
	 */
	public SaxXmlDocumentHandler(XBRL xbrl, Fact factRoot, boolean isFactFile) {
		this.xbrl = xbrl;
		this.fact = factRoot;
		this.index = xbrl.getIndex();
		this.isFact = isFactFile;
		factStack.push(factRoot);
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		line++;
		value.setLength(0);
		Type type = getType(localName, level, isFact);
		Fact newfact = type == Type.value ? new FactValue(xbrl, type, level, line) : new Fact(xbrl, type, level, line);

		for (int i = 0; i < attributes.getLength(); i++) {
			String name = attributes.getLocalName(i);
			String value = attributes.getValue(i);
			if (!Utils.isEmpty(value)) {
				newfact.put(name, value);
			}
		}

		if (type == Type.value) {
			newfact.put(Attribute.parameterName.name(), localName);
			newfact.put("uri", uri);
			newfact.put("prefix", qName.contains(":") ? qName.substring(0, qName.indexOf(":")) : qName);
		}

		createRelationship(newfact, fact);
		factStack.push(newfact);
		fact = newfact;
		level++;

	}

	private Type getType(String localName, int level, boolean isFact) {
		Type result = null;
		// import is reserved, we map it to importX
		localName = "import".equals(localName) ? "importX" : localName;
		try {
			result = Type.valueOf(localName);
		} catch (IllegalArgumentException ex) {
			// facts are in the fact xml on level 1
			if (isFact && level == 1) {
				result = Type.value;
			} else {
				LOG.warn("The type '" + localName + "' was not recognised -> UNDEFINED");
				result = Type.UNDEFINED;
			}
		}
		// assert(result!=Type.UNDEFINED);
		return result;
	}

	private void createRelationship(Fact fact, Fact parent) {
		parent.addChild(fact);
		fact.addParent(parent);
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		level--;
		String str = value.toString().trim();
		if (!str.isEmpty()) {
			String name = localName;
			if (fact.getType() == Type.value) {
				name = Attribute.value.name();
			}
			
			// reformat
			IValueFormatter f = this.xbrl.getValueFormatter(fact.getDataType(str));
			fact.put(name, f.format(str));
			fact.index();
			
			// Unfortunatly this is necessary here. In the postprocessing it might be too late.
			if (name.equalsIgnoreCase("value") && fact.getParameterName().equalsIgnoreCase("DocumentType")) {
				xbrl.getFilingInfo().setForm(str);
			}

		}
		factStack.pop();
		fact = factStack.peek();
		value = new StringBuffer();

	}

	@Override
	public void characters(char ch[], int start, int length) throws SAXException {
		if (value.length()<xbrl.getMaxFieldSize()) {
			value.append(new String(ch, start, length));
		}
		if (value.length()>xbrl.getMaxFieldSize()) {
			// we were running out of heap space. To prevent this we limit the content size of a field
			LOG.warn("The content is longer then "+xbrl.getMaxFieldSize()+" characters. The content is cut off");
			value.setLength(xbrl.getMaxFieldSize());
		}
	}

	public void endDocument() throws SAXException {
		value.setLength(0);
		factStack.clear();
		index = null;
		xbrl = null;
	}

}