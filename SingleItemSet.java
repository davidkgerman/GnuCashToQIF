package net.sourceforge.gnucashtoqif;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Set that contains one and only one item
 */
class SingleItemSet extends AbstractSet
{
	/** Our item */
	Object item;
	
	/**
	 * Creates a new immutable single item Set
	 * @param item The item to put into the Set
	 */
	public SingleItemSet(Object item) {
		this.item = item;
	}
	
	/**
	 * Returns the size of this Set, which is always 1
	 * @return The size of this Set, which is always 1
	 */
	public int size() {
		return 1;
	}
	
	/**
	 * Returns an Iterator for this set
	 */
	public Iterator iterator() {
		return new Iterator() {
			protected boolean next = true;
			public boolean hasNext() {
				return next;
			}
			public Object next() {
				if (next) {
					next = false;
					return item;
				} else
				    throw new NoSuchElementException("Only one element exists in this Set");
			}
			public void remove() {
				throw new UnsupportedOperationException("Read-only Set");
			}
		};
	}
}