/**
 * 
 */
package net.sourceforge.gnucashtoqif;

class Split
{
    /** The amount of the split */
    public double amount;

    /** The reconciliation state of this split */
    public char reconciliationStatus;

    /** The account GUID */
    public String accountGuid;
    
    /** The account object */
    public Account account;
    
    /** The memo for this split */
    public String memo = null;
    
    /** The lot GUID of this split */
    public String lotGuid;
    
    /** The action of this split */
    public String action;
}
