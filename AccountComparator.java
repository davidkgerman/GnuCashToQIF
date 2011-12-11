package net.sourceforge.gnucashtoqif;

import java.util.Comparator;

/**
 * Compares two Account object instances using their full names
 */
public class AccountComparator implements Comparator
{
	/**
	 * Compares the full account names of the two inputs
	 * @param o1 Input 1
	 * @param o2 Input 2
	 * @return -1 if o1 &lt; o2, 0 if o1 == o2, or 1 if o1 &gt; o2
	 */
	public int compare(Object o1, Object o2) {
        return ((Account) o1).fullName.compareTo(((Account) o2).fullName);
	}
}
