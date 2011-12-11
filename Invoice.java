package net.sourceforge.gnucashtoqif;


/**
 * Contains a single invoice's data
 * @author Steven Lawrance
 */
public class Invoice
{
    /** The GUID of this invoice */
	public String guid;
	
	/** The date opened in YYYY-MM-DD format */
	public String dateOpened;
	
	/** The date posted in YYYY-MM-DD format */
	public String datePosted;
	
	/** The ID of the invoice */
	public String id;
	
	/** Whether or not this invoice is active */
	public boolean active;
	
	/** GUID of account that this invoice is on */
	public String accountGuid;
    
	/** The account that this invoice is on */
	public Account account;
	
	/** GUID of the transaction that this invoice generated */
	public String invoiceTransactionGuid;
	
	/** Transaction that this invoice generated */
	public Transaction invoiceTransaction;
}
