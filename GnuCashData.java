package net.sourceforge.gnucashtoqif;

import java.io.IOException;
import java.io.Reader;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.xerces.jaxp.SAXParserFactoryImpl;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Parses GnuCash input into accounts and transactions
 */
public class GnuCashData
{
    /**
	 * Creates a new GnuCashParser that reads input from the given reader
	 * and builds an in-memory representation of most of GnuCash's elements
	 * @param input The Reader to read GnuCash data from
	 * @param conversion The conversion configuration to use
	 * @throws IOException Thrown if an IOException arose while reading the input
	 * @throws SAXException Thrown if the input GnuCash file has an XML format error
	 */
	public static void importGnuCash(Reader reader, Conversion conversion) throws SAXException, IOException
	{
        // Run the data conversion by having the XML parser provide
        // the file state events to us
        XMLReader parser;
        try {
        	parser = SAXParserFactoryImpl.newInstance().newSAXParser().getXMLReader();
        } catch (ParserConfigurationException e) {
        	throw new RuntimeException(e);
        }
        GnuCashHandler handler = new GnuCashHandler(conversion);
        parser.setContentHandler(handler);
        parser.setErrorHandler(handler);
        parser.setFeature("http://xml.org/sax/features/namespaces", true);
        parser.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
        try {
            parser.parse(new InputSource(reader));
        } catch (SAXException e) {
        	e.getException().printStackTrace();
        	throw e;
        }
	}
	
	/**
	 * XML file handler for the GnuCash file format
	 */
    protected static class GnuCashHandler extends DefaultHandler
	{
        /** The conversion object */
        protected Conversion conversion;

	    /** This data member will handle the incoming XML values */
	    protected StringBuffer buffer = new StringBuffer();

	    /**
	     * Creates a new GnuCashHandler with the given conversion object
	     * @param conversion The data conversion object to use and write to
	     */
	    public GnuCashHandler(Conversion conversion) {
	    	this.conversion = conversion;
	    }
	    
	    /** Start document */
	    public void startDocument() {
	    }

	    /** Start element */
	    public void startElement(String uri, String localName, String qName, Attributes attributes)
	    {
	        // Reset the buffer
	        buffer.setLength(0);

	        // Dispatch the type of element
	        String value;
	        if (qName.equalsIgnoreCase("gnc:account"))
	            currentAccount = new Account();
	        else if (qName.equalsIgnoreCase("gnc:transaction"))
	            currentTransaction = new Transaction();
	        else if (qName.equalsIgnoreCase("trn:date-posted") ||
	        		 qName.equalsIgnoreCase("invoice:opened") ||
	        		 qName.equalsIgnoreCase("invoice:posted") ||
	        		 (qName.equalsIgnoreCase("slot:value") && (value = attributes.getValue("type")) != null &&
	        		  value.equalsIgnoreCase("timespec")))
	            parentName = qName;
	        else if (qName.equalsIgnoreCase("trn:split"))
	            currentSplit = new Split();
	        else if (qName.equalsIgnoreCase("gnc:GncVendor"))
	        	currentVendor = new Vendor();
	        else if (qName.equalsIgnoreCase("gnc:GncInvoice"))
	        	currentInvoice = new Invoice();
	        else if (qName.equalsIgnoreCase("gnc:lot") && currentAccount != null) {
	        	currentLot = new Lot();
	        	currentLot.account = currentAccount;
	        } else if (qName.equalsIgnoreCase("gnc-v2")) {
	            // Add the namespace definitions to prevent SAX from not liking GnuCash
	            ((AttributesImpl) attributes).addAttribute("", "", "", "xmlns:act", "http://www.gnucash.org/act");
	            ((AttributesImpl) attributes).addAttribute("", "", "", "xmlns:gnc", "http://www.gnucash.org/gnc");
	            ((AttributesImpl) attributes).addAttribute("", "", "", "xmlns:trn", "http://www.gnucash.org/trn");
	            ((AttributesImpl) attributes).addAttribute("", "", "", "xmlns:ts", "http://www.gnucash.org/ts");
	            ((AttributesImpl) attributes).addAttribute("", "", "", "xmlns:split", "http://www.gnucash.org/split");
	            ((AttributesImpl) attributes).addAttribute("", "", "", "xmlns:cd", "http://www.gnucash.org/cd");
	            ((AttributesImpl) attributes).addAttribute("", "", "", "xmlns:cmdty", "http://www.gnucash.org/cmdty");
	            ((AttributesImpl) attributes).addAttribute("", "", "", "xmlns:slot", "http://www.gnucash.org/slot");
	        }
	    }

	    /** The current account */
	    protected Account currentAccount = null;

	    /** The current transaction */
	    protected Transaction currentTransaction = null;

	    /** The current split */
	    protected Split currentSplit = null;
	    
	    /** The current vendor name */
	    protected Vendor currentVendor = null;
	    
	    /** The current invoice */
	    protected Invoice currentInvoice = null;
	    
	    /** The current lot */
	    protected Lot currentLot = null;
	    
	    /** The current key, which is used for some values */
	    protected String currentKey = null;

	    /** The current parent element name */
	    protected String parentName = null;
	    
	    /** End element */
	    public void endElement(String uri, String localName, String qName)
	    {
	        // Trim the sides of the value
	        String value = buffer.toString().trim();

	        // Dispatch the type of element
	        if (qName.equalsIgnoreCase("gnc:account"))
	        {
	            // Put this account into the list
	            conversion.accounts.put(currentAccount.guid, currentAccount);
	            currentAccount = null;
	        } else if (qName.equalsIgnoreCase("act:name"))
	            currentAccount.name = value;
	        else if (qName.equalsIgnoreCase("act:description"))
	            currentAccount.description = value;
	        else if (qName.equalsIgnoreCase("act:code"))
	        	currentAccount.code = value;
	        else if (qName.equalsIgnoreCase("act:id"))
	            currentAccount.guid = value;
	        else if (qName.equalsIgnoreCase("act:parent"))
	            currentAccount.parentGuid = value;
	        else if (qName.equalsIgnoreCase("act:type")) {
	        	currentAccount.typeName = value.toLowerCase();
	        } else if (qName.equalsIgnoreCase("ts:date") && parentName != null && parentName.equalsIgnoreCase("trn:date-posted"))
	            currentTransaction.datePosted = value.substring(0, 10);
	        else if (qName.equalsIgnoreCase("ts:date") && parentName != null && currentTransaction != null && currentKey != null &&
	        		 parentName.equalsIgnoreCase("slot:value") && currentKey.equalsIgnoreCase("trans-date-due"))
	        	currentTransaction.dateDue = value.substring(0, 10);
	        else if (qName.equalsIgnoreCase("trn:description"))
	            currentTransaction.description = value;
	        else if (qName.equalsIgnoreCase("trn:num"))
	            currentTransaction.ref = value;
	        else if (qName.equalsIgnoreCase("trn:id"))
	        	currentTransaction.guid = value;
	        else if (qName.equalsIgnoreCase("split:reconciled-state"))
	            currentSplit.reconciliationStatus = value.charAt(0);
	        else if (qName.equalsIgnoreCase("lot:id") && currentLot != null)
	        	currentLot.guid = value;
	        else if (qName.equalsIgnoreCase("slot:value") && currentLot != null && currentKey != null &&
	        		 currentKey.equalsIgnoreCase("invoice-guid"))
	        	currentLot.invoiceGuid = value;
	        else if (qName.equalsIgnoreCase("split:lot"))
	        	currentSplit.lotGuid = value;
	        else if (qName.equalsIgnoreCase("slot:key"))
	            currentKey = value;
	        else if (qName.equalsIgnoreCase("slot:value") && currentTransaction != null && currentKey != null && currentKey.equalsIgnoreCase("trans-txn-type"))
	        	currentTransaction.transactionType = value;
	        else if (qName.equalsIgnoreCase("slot:value") && currentAccount != null && currentKey != null && currentKey.equalsIgnoreCase("notes"))
	            currentAccount.notes = value;
	        else if (qName.equalsIgnoreCase("split:quantity")) {
	            int pos = value.indexOf("/");
	            if (pos > 0)
	                currentSplit.amount = Double.parseDouble(value.substring(0, pos)) / Double.parseDouble(value.substring(pos + 1, value.length()));
	            else
	                currentSplit.amount = Double.parseDouble(value);
	        } else if (qName.equalsIgnoreCase("split:account"))
	            currentSplit.accountGuid = value;
	        else if (qName.equalsIgnoreCase("split:memo"))
	            currentSplit.memo = value;
	        else if (qName.equalsIgnoreCase("split:action"))
	        	currentSplit.action = value;
	        else if (qName.equalsIgnoreCase("trn:split"))
	        {
	            // Look up the referenced account and add the current transaction
	            // if found
	            Account acct = (Account) conversion.accounts.get(currentSplit.accountGuid);
	            if (acct != null)
	                acct.trans.add(currentTransaction);
	            
	            // Add this transaction to the transaction map
	            conversion.transactions.put(currentTransaction.guid, currentTransaction);

	            // Add the current split to the transaction
	            currentTransaction.splits.add(currentSplit);
	            currentSplit = null;
	        } else if (qName.equalsIgnoreCase("gnc:transaction"))
	            currentTransaction = null;
	        else if (qName.equalsIgnoreCase("vendor:name"))
	        	currentVendor.name = value;
	        else if (qName.equalsIgnoreCase("vendor:id"))
	        	currentVendor.id = value;
	        else if (qName.equalsIgnoreCase("addr:name") && currentVendor != null)
	        	currentVendor.addressName = value;
	        else if (qName.equalsIgnoreCase("addr:addr1") && currentVendor != null)
	        	currentVendor.addressLine1 = value;
	        else if (qName.equalsIgnoreCase("addr:addr2") && currentVendor != null)
	        	currentVendor.addressLine2 = value;
	        else if (qName.equalsIgnoreCase("vendor:active") && currentVendor != null)
	        	currentVendor.active = value.equals("1");
	        else if (qName.equalsIgnoreCase("gnc:GncVendor")) {
	        	conversion.vendors.add(currentVendor);
	        	currentVendor = null;
	        } else if (qName.equalsIgnoreCase("ts:date") && parentName != null && parentName.equalsIgnoreCase("invoice:opened"))
	        	currentInvoice.dateOpened = value.substring(0, 10);
	        else if (qName.equalsIgnoreCase("ts:date") && parentName != null && parentName.equalsIgnoreCase("invoice:posted"))
	        	currentInvoice.datePosted = value.substring(0, 10);
	        else if (qName.equalsIgnoreCase("invoice:id"))
	        	currentInvoice.id = value;
	        else if (qName.equalsIgnoreCase("invoice:guid"))
	        	currentInvoice.guid = value;
	        else if (qName.equalsIgnoreCase("invoice:active"))
	        	currentInvoice.active = value.equals("1");
	        else if (qName.equalsIgnoreCase("invoice:postacc"))
	        	currentInvoice.accountGuid = value;
	        else if (qName.equalsIgnoreCase("invoice:posttxn"))
	        	currentInvoice.invoiceTransactionGuid = value;
	        else if (qName.equalsIgnoreCase("gnc:GncInvoice")) {
	        	conversion.invoices.put(currentInvoice.guid, currentInvoice);
	        	currentInvoice = null;
	        } else if (parentName != null && (qName.equalsIgnoreCase("trn:date-posted") ||
	        		   qName.equalsIgnoreCase("invoice:opened") ||
	        		   qName.equalsIgnoreCase("invoice:posted") ||
	        		   qName.equalsIgnoreCase("slot:value")))
	        	parentName = null;
	        else if (qName.equalsIgnoreCase("gnc:lot") && currentLot != null) {
	        	conversion.lots.put(currentLot.guid, currentLot);
	        	currentLot = null;
	        }
	    }

	    /** Characters */
	    public void characters(char ch[], int start, int length) {
	        buffer.append(ch, start, length);
	    }	
	}
}
