/**
 * 
 */
package net.sourceforge.gnucashtoqif;

import java.util.ArrayList;
import java.util.List;

class Transaction
{
    /** The posting date in YYYY-MM-DD format */
    public String datePosted;
    
    /** The due date in YYYY-MM-DD format */
    public String dateDue;

    /** The description of the transaction */
    public String description;

    /** The reference number, if any */
    public String ref;

    /** The splits in this transaction (usually two) */
    public List splits = new ArrayList(2);
    
    /** The GUID of this transaction */
    public String guid;
    
    /** The transaction type */
    public String transactionType;
    
    /** Export transaction type (used by IIF; not from GnuCash) */
    public String exportTransactionType;

    /**
     * Returns a stringified version of this object
     * @return A stringified version of this object
     */
    public String toString() {
    	return "\"" + description + "\" on " + datePosted + ((ref == null)? "": " with ref \"" + ref + "\"");
    }
}
