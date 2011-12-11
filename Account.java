package net.sourceforge.gnucashtoqif;

import java.util.ArrayList;
import java.util.List;

/**
 * An Account class that contains transactions
 */
class Account
{
    /** Unknown account type */
    public final static int TYPE_UNKNOWN = -1;

    /** Double-entry type of account, which becomes a Category in QIF */
    public final static int TYPE_DOUBLEENTRY = 0;

    /** Double-entry type of account for income, which becomes a Category in QIF */
    public final static int TYPE_DOUBLEENTRY_INCOME = 1;

    /** Double-entry type of account for expenses, which becomes a Category in QIF */
    public final static int TYPE_DOUBLEENTRY_EXPENSE = 2;
    
    /** BANK acount */
    public final static int TYPE_BANK = 3;

    /** INVST account */
    public final static int TYPE_INVST = 4;

    /** CREDIT card account */
    public final static int TYPE_CREDIT = 5;
    
    /** CASH account */
    public final static int TYPE_CASH = 6;
    
    /** ASSET account */
    public final static int TYPE_ASSET = 7;
    
    /** LIABILITY account */
    public final static int TYPE_LIABILITY = 8;

    /** The type of account, as specified by the TYPE_* constants */
    public int type = TYPE_UNKNOWN;

    /** The GnuCash type name */
    public String typeName;
    
    /** The GUID of this account */
    public String guid;

    /** The GUID of this account's parent */
    public String parentGuid;

    /** The name of this account */
    public String name;
    
    /** Notes for this account */
    public String notes;
    
    /** Description for this account */
    public String description;
    
    /** Account code */
    public String code;
    
    /** The full name of this account, or null if not known yet */
    public String fullName = null;

    /** The Transaction objects for this Account */
    public List trans = new ArrayList();
    
    /**
     * Returns the description of this account or, if missing, the given default
     * @param defaultDescription The default description to return if no description
     * exists
     * @return The description of this account or, if missing, the given default
     */
    public String getDescription(String defaultDescription) {
        if (description != null)
            return description;
        else if (notes != null)
            return notes;
        else
            return defaultDescription;
    }
    
    /**
     * Returns whether or not this account is a double-entry account
     * that will become a category
     * @return True if this account is a double-entry account or false
     * if it is not
     */
    public boolean isDoubleEntry() {
        return type == TYPE_DOUBLEENTRY || type == TYPE_DOUBLEENTRY_INCOME || type == TYPE_DOUBLEENTRY_EXPENSE;
    }
    
    /**
     * Returns the name of this account's type
     * @return The type name of this account, which can be Bank,
     * CCard, or Invst
     */
    public String getQIFTypeName() {
        String type;
        switch (this.type) {
            default :
            case Account.TYPE_BANK :
                type = "Bank";
                break;
            case Account.TYPE_CREDIT :
                type = "CCard";
                break;
            case Account.TYPE_INVST :
                type = "Invst";
                break;
            case Account.TYPE_CASH :
                type = "Cash";
                break;
            case Account.TYPE_ASSET :
                type = "Oth A";
                break;
        }
        return type;
    }
    
    /**
     * Returns the IIF name as mapped by the document at http://www.datablox.com/qb/qbaccnt.htm
     * @return The IIF name as mapped by the document at http://www.datablox.com/qb/qbaccnt.htm
     */
    public String getIIFTypeName()
    {
    	// Dispatch the account type
        if (typeName.equals("bank")) {
        	return "BANK";
        } else if (typeName.equals("savings"))
        {
        	// The closest mapping in QuickBooks is Bank
        	return "BANK";
        } else if (typeName.equals("receivable")) {
        	return "AR";
        } else if (typeName.equals("asset")) {
        	return "FIXASSET";
        } else if (typeName.equals("cash"))
        {
            // The closest mapping in QuickBooks is Other Current Asset
        	return "OCASSET";
        } else if (typeName.equals("currency"))
        {
            // The closest mapping in QuickBooks is Other Current Asset
        	return "OCASSET";
        } else if (typeName.equals("stock"))
        {
        	// The closest mapping in QuickBooks is Other Asset
        	return "OASSET";
        } else if (typeName.equals("mutual"))
        {
        	// The closest mapping in QuickBooks is Other Asset
        	return "OASSET";
        } else if (typeName.equals("moneymrkt"))
        {
        	// The closest mapping in QuickBooks is Other Asset
        	return "OASSET";
        } else if (typeName.equals("payable")) {
        	return "AP";
        } else if (typeName.equals("credit")) {
        	return "CCARD";
        } else if (typeName.equals("liability")) {
        	return "LTLIAB";
        } else if (typeName.equals("creditline")) {
        	return "OCLIAB";
        } else if (typeName.equals("equity")) {
        	return "EQUITY";
        } else if (typeName.equals("income")) {
        	return "INC";
        } else if (typeName.equals("expense")) {
        	return "EXP";
        } else
        	throw new RuntimeException("Unhandled account type: " + typeName);
        
        // QuickBooks account types not used:
        // OthIncome (income), COGS (expense), OthExpense (expense), NonPosting (expense)
    }
}
