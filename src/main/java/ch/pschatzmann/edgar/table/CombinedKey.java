package ch.pschatzmann.edgar.table;

/**
 * A combination of a Row and a Column Key which identifies a value
 * 
 * @author pschatzmann
 *
 */
public class CombinedKey implements Comparable<CombinedKey> {
	private Key rowKey;
	private Key colKey;
	private Integer hashCode=null;

	public CombinedKey(Key colKey, Key rowKey) {
		this.rowKey = rowKey;
		this.colKey = colKey;
	}

	public Key getColKey() {
		return colKey;
	}

	public Key getRowKey() {
		return rowKey;
	}

	@Override
	public int hashCode() {
		if (hashCode ==null) {
			hashCode = rowKey.hashCode() | colKey.hashCode();
		}
		return hashCode;
	}

	@Override
	public int compareTo(CombinedKey o) {			
		int result = this.getColKey().compareTo(o.getColKey());
		if (result == 0) {
			 result = this.getRowKey().compareTo(o.getRowKey());
		}
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final CombinedKey other = (CombinedKey) obj;
		if (this.hashCode()!=other.hashCode())
			return false;
		return compareTo(other) == 0;
	}

	@Override
	public String toString() {
		return this.colKey.toString() + "/" + this.rowKey.toString();
	}
}