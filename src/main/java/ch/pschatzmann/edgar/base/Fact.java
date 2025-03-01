package ch.pschatzmann.edgar.base;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;

import ch.pschatzmann.edgar.utils.Utils;

/**
 * Representation of a XML/XLS tag with its attribute values. All attribute
 * values are indexed so that the content can be found quickly. Facts are
 * represented as Tree which represents the XML tag level structure.
 * 
 * @author pschatzmann
 *
 */

public class Fact implements Serializable, Comparable<Fact> {
	private static final Logger LOG = Logger.getLogger(Fact.class);
    @JsonIgnore
	private List<Fact> explodedFacts = null;
	
	public enum Type {
		ROOT, xbrl, value, importX, context, entity, arcroleRef, calculationArc, calculationLink, definition, definitionLink, definitionArc, element, loc, label, labelArc, labelLink, linkbase, linkbaseRef, presentationArc, presentationLink, roleRef, roleType, schema, schemaRef, unit, usedOn, period, startDate, endDate, identifier, segment, explicitMember, instant, divide, unitNumerator, unitDenominator, measure, annotation, appinfo, footnote, footnoteLink, footnoteArc, reference, documentation, DefinitionAndReference, header, resources, hidden, html, continuation, typedMember, UNDEFINED
	};

	public enum Attribute {
		value, contextRef, instant, form, file, parameterName, label, role, from, to, order, priority, preferredLabel, href, roleURI, id, explicitMember, date, segment, identifier, companyName, tradingSymbol, incorporation, location, sicCode, sicDescription, dateLabel, segmentDimension, dimension, numberOfMonths, unitRef, scale
	};
	
	public enum DataType {
		string, number, html, undefined
	}

    @JsonIgnore
	private XBRL xbrl;
    @JsonIgnore
	private Type type;
    @JsonIgnore
	protected Map<String, String> attributes = new HashMap();
    @JsonIgnore
	private List<Fact> children = new ArrayList();
    @JsonIgnore
	private List<Fact> parents = new ArrayList();
	private int level = 0;
	private long line = 0;
    @JsonIgnore
	private EdgarFiling filingInfo;

	/**
	 * Default Constructor
	 * 
	 * @param xbrl
	 * @param type
	 * @param level
	 * @param line
	 */
	public Fact(XBRL xbrl, Type type, int level, long line) {
		this.xbrl = xbrl;
		this.type = type;
		this.line = line;
		this.level = level;
		if (xbrl!=null)
			this.filingInfo = xbrl.getFilingInfo();
	}
	
	/**
	 * Clone existing node
	 * @param source
	 */
	public Fact(Fact source) {
		this.explodedFacts = source.explodedFacts;
		this.xbrl = source.xbrl;
		this.type = source.type;
		this.attributes = source.attributes;
		this.children = source.children;
		this.parents = source.parents;		
		this.line = source.line;
		this.level = source.level;	
		if (xbrl!=null)
			this.filingInfo = xbrl.getFilingInfo();
	}
	
    @JsonIgnore
	protected XBRL getXBRL() {
		return this.xbrl;
	}

	/**
	 * type of the fact (usaully the xml tag name)
	 * 
	 * @return
	 */
	public Type getType() {
		return type;
	}

	/**
	 * Returns the xbrl attribute information
	 * 
	 * @param factName
	 * @return
	 */
	public String getAttribute(String factName) {
		String result = getAttributes().get(factName);
		return result;
	}

	/**
	 * Returns the xbrl attribute information
	 * 
	 * @param att
	 * @return
	 */
	public String getAttribute(Attribute att) {
		return getAttribute(att.name());
	}

	/**
	 * Adds attribute information
	 * 
	 * @param key
	 * @param value
	 */
	public void put(String key, String value) {
		this.attributes.put(key, value);
		xbrl.getIndex().add(value, this);
	}

	/**
	 * Adds the attribute information from multiple attributes
	 * 
	 * @param values
	 */
	public void putAll(Map<String, String> values) {
		for (Entry<String,String> e :values.entrySet()){
			this.put(e.getKey(),e.getValue());
		}
	}

	/**
	 * Returns the attribute/value map
	 * 
	 * @return
	 */
    @JsonIgnore
	public Map<String, String> getAttributes() {
		return this.attributes;
	}

	/**
	 * Returns the child nodes of all types
	 * 
	 * @return
	 */
	public List<Fact> getChildren() {
		return children;
	}
	
	/**
	 * Returns the children of the inidcated type
	 * @param type
	 * @return
	 */
	public List<Fact> getChildren(Type type) {
		List<Fact> result = new ArrayList();
		for (Fact f: getChildren()) {
			if (f.getType()== type) {
				result.add(f);
			}
		}
		return result;
	}


	/**
	 * Returns the parent nodes
	 * 
	 * @return
	 */
    @JsonIgnore
	public List<Fact> getParents() {
		return parents;
	}

	/**
	 * Adds a child node
	 * 
	 * @param fact
	 */
	public void addChild(Fact fact) {
		children.add(fact);
	}

	/**
	 * Adds a parent node
	 * 
	 * @param fact
	 */
	public void addParent(Fact fact) {
		parents.add(fact);
	}

	/**
	 * Returns the tree level. The root is on level 0
	 * 
	 * @return
	 */
	@JsonIgnore
	public int getLevel() {
		return level;
	}

	/**
	 * Defines the tree level
	 * 
	 * @param level
	 */
	public void setLevel(int level) {
		this.level = level;
	}

	/**
	 * Clears the attributes and children
	 */
	public void clear() {
		attributes.clear();
		children.clear();
		parents.clear();
		explodedFacts = null;
	}

	/**
	 * Returns the exploded list of facts using the inorder sequence
	 * 
	 * @return
	 */

    @JsonIgnore
	public List<Fact> getFacts() {
		if (explodedFacts==null) {
			explodedFacts = new ArrayList();
			explodeList(this, explodedFacts);
		}
		return explodedFacts;
	}
	
	public List<Fact> getFacts(Type type) {
		return getFacts(Arrays.asList(type),true,0, Integer.MAX_VALUE);
	}

	/**
	 * Finds the fact of the indicated type and the indicated level range
	 * 
	 * @param types
	 * @param equals
	 * @param fromLevel
	 * @param toLevel
	 * @return
	 */
	public List<Fact> getFacts(Collection<Type> types, boolean equals, int fromLevel, int toLevel) {
		List<Fact> result = new ArrayList();
		for (Fact f : getFacts()) {
			int factLevel = f.getLevel();
			if (factLevel >= fromLevel && factLevel <= toLevel && types.contains(f.getType())) {
				result.add(f);
			}
		}
		return result;
	}

	protected void explodeList(Fact fact, List<Fact> list) {
		list.add(fact);
		for (Fact f : fact.getChildren()) {
			explodeList(f, list);
		}
	}

	/**
	 * For debugging: returns the string representation with the attributes
	 */
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(this.getType());
		sb.append("(");
		sb.append(this.getLevel());
		sb.append(")");
		sb.append("[");
		sb.append(this.children.size());
		sb.append("]");

		sb.append(this.getAttributes().toString());
		return sb.toString();
	}

	/**
	 * Sort by type and line number
	 */
	@Override
	public int compareTo(Fact o) {
		int result = this.getType().compareTo(o.getType());
		if (result == 0) {
			result = Long.valueOf(this.line).compareTo(o.getLine());
		}
		return result;
	}

	/**
	 * Returns the line number. 
	 * @return
	 */
	protected Long getLine() {
		return this.line;
	}

	/**
	 * Retruns the parameterName value
	 * @return
	 */
	public String getParameterName() {
		return this.getAttribute(Attribute.parameterName);
	}

	/**
	 * Determines if the value content is numeric, a string or a html string
	 * @return
	 */
	public DataType getDataType() {
		String value = this.getAttribute(Attribute.value);
		return getDataType(value);
	}

	public DataType getDataType(String value) {
		DataType result = DataType.undefined;
		if (Utils.isEmpty(value)) {
			result = DataType.undefined;
		} else if (value.startsWith("<") || value.startsWith("&gt;")) {
			result = DataType.html;
		} else if (Utils.isNumber(value, false)) {
			result = DataType.number;
		} else  {
			result = DataType.string;
		}
		return result;
	}

	public void index() {
		this.getXBRL().getIndex().add(this);
	}
	
    @JsonIgnore
	protected EdgarFiling getFilingInfo() {
		return this.filingInfo;
	}

}
