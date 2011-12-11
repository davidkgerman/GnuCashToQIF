package net.sourceforge.gnucashtoqif;

/**
 * Lot objects
 * @author Steven Lawrance
 */
public class Lot
{
	/** The GUID of this lot */
	public String guid;
	
	/** The GUID of the invoice that this lot belongs to */
	public String invoiceGuid;
	
	/** The account that this lot belongs to */
	public Account account;
}
