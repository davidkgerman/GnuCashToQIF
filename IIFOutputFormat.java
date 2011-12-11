package net.sourceforge.gnucashtoqif;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

import javax.swing.filechooser.FileFilter;

/**
 * Output format handler for Intuit's QuickBooks IIF. This file is written using
 * MS-DOS/Windows newlines
 * @author Steven Lawrance
 */
public class IIFOutputFormat implements OutputFormat
{
    /** A number formatter for the currency that gets written to the IIF file */
    protected static final NumberFormat currencyFormat = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.US));

    /**
	 * Writes out the state in the given Conversion object to the given Writer
	 * using the IIF format
	 * @param conversion The input conversion parameters
	 * @param writer The output Writer
	 * @throws IOException Thrown when an IOException occurs during the conversion
	 */
	public void export(Conversion conversion, Writer writer) throws IOException
	{
		// Ensure that all accounts have getFullName() called
		updateFullNames(conversion);
		
		// Create a Set of sorted accounts
	    conversion.accountSet = new TreeSet(new AccountComparator());
	    conversion.accountSet.addAll(conversion.accounts.values());
	    
	    // Validate the transactions
	    validateTransactions(conversion);

		// Write out the account list
		writeAccountList(conversion, writer);
		
		// Write out the vendor list
		writeVendorList(conversion, writer);
		
		// Write out the transactions for each account
		writeTransactions(conversion, writer);
	}
	
	/**
	 * Writes out the account list in the given Conversion object to the given Writer
	 * using the IIF format given at http://www.datablox.com/qb/qbaccnt.htm
	 * @param conversion The input conversion parameters
	 * @param writer The output Writer
	 * @throws IOException Thrown when an IOException occurs during the conversion
	 */
	protected static void writeAccountList(Conversion conversion, Writer writer) throws IOException
	{
		// Write the table header
		writer.write("!ACCNT\tACCNTTYPE\tACCNUM\tNAME\tDESC\tBANKNUM\r\n");
		
		// Write out the account list
		Iterator accountIterator = conversion.accountSet.iterator();
		Account currentAccount;
		while (accountIterator.hasNext()) {
			currentAccount = (Account) accountIterator.next();

			// Set this account's type
			setAccountType(currentAccount);
			
			// Write the account line in the above column order
			writer.write("ACCNT\t");
			writer.write(currentAccount.getIIFTypeName());
			writer.write('\t');
			if (currentAccount.code != null)
			    writer.write(currentAccount.code);
			writer.write('\t');
			writer.write(getFullName(currentAccount, conversion.accounts));
			writer.write('\t');
			if (currentAccount.description != null)
			    writer.write(currentAccount.description);
			writer.write('\t');
			if (currentAccount.notes != null)
				writer.write(currentAccount.notes);  // FIXME: filter out tabs and other invalid characters!
			writer.write("\r\n");
		}
	}
	
	/**
	 * Writes out the transactions in the given Conversion object for the given
	 * Account to the given Writer
	 * using the IIF format
	 * http://www.datablox.com/qb/qbtran.htm
	 * @param conversion The input conversion parameters
	 * @param writer The output Writer
	 * @param invoice The invoice to write transaction information for, which is
	 * only non-null for invoices
	 * @param account The account to write transaction information for
	 * @param transIterator The transaction iterator to use in the main loop
	 * @param export The export strategy to use, which defines the output format
	 * @throws IOException Thrown when an IOException occurs during the conversion
	 */
	protected static void writeTransactions(Conversion conversion, Writer writer, Invoice invoice, Account account, Iterator transIterator, TransactionExport export) throws IOException
	{
        // Write out every transaction
        Split splits[] = new Split[2];
        int i, acctSplit;
        String memo, splitMemo, alternativeMemo;
        StringBuffer category = new StringBuffer();
        Account lastAccount = null;
        boolean primaryAccount;
        Transaction currentTransaction;
        Map uniqueSplits = new HashMap();
        Split currentSplit, existingSplit;
        Iterator splitIterator;
        while (transIterator.hasNext()) {
            currentTransaction = (Transaction) transIterator.next();
            
            // Combine splits to the same account together
            splitIterator = currentTransaction.splits.iterator();
            uniqueSplits.clear();
            while (splitIterator.hasNext()) {
            	currentSplit = (Split) splitIterator.next();
            	
            	// Add splits that affect a unique account
            	if ((existingSplit = (Split) uniqueSplits.get(currentSplit.accountGuid)) == null)
            		uniqueSplits.put(currentSplit.accountGuid, currentSplit);
            	
            	// Combine duplicate splits and notify the user
            	else {
            		conversion.warnings.add("Combined multiple splits in transaction " + currentTransaction + " that all pointed to " + ((Account) conversion.accounts.get(existingSplit.accountGuid)).name);
            		existingSplit.amount += currentSplit.amount;
            		splitIterator.remove();
            	}
            }
            
            // Discover if this account is the last account listed in the splits
            splits = (Split[]) currentTransaction.splits.toArray(splits);
            acctSplit = -1;
            for (i = currentTransaction.splits.size() - 1; acctSplit == -1 && i >= 0; i--) {
                lastAccount = (Account) conversion.accounts.get(((Split) splits[i]).accountGuid);
                if (lastAccount != null && !lastAccount.isDoubleEntry())
                    acctSplit = i;
            }
            if (acctSplit >= 0 && !lastAccount.guid.equalsIgnoreCase(account.guid))
                primaryAccount = false;
            else
                primaryAccount = true;
            
            // Reset the memo field so that we can attempt to get it from the primary split
            memo = "";
            
            // Also reset the alternative memo, which is a memo attached to a complementary
            // record in a simple two-account transaction
            alternativeMemo = null;

            // Set the primary split's account
            if (acctSplit >= 0)
            	((Split) splits[acctSplit]).account = (Account) conversion.accounts.get(((Split) splits[acctSplit]).accountGuid);
            
            // If this is the transaction's primary account, then write out the full detail
            category.setLength(0);
            if (primaryAccount)
            {
                for (i = 0; i < splits.length && splits[i] != null; i++)
                {
                    // Get the split memo
                    splitMemo = ((Split) splits[i]).memo;
                    
                    // If this is split is the current account's split, then use
                    // its data for the overall transaction details
                    if (((Split) splits[i]).accountGuid.equalsIgnoreCase(account.guid)) {
                        acctSplit = i;
                        
                        // Set the account if it's not already set
                        if (((Split) splits[acctSplit]).account == null)
                    	    ((Split) splits[acctSplit]).account = (Account) conversion.accounts.get(((Split) splits[acctSplit]).accountGuid);

                        // Use this split's memo as the main memo if one exists
                        if (splitMemo != null && splitMemo.length() > 0)
                            memo = splitMemo;
                    } else
                    {
                        // Set the account if it's not already set
                        if (((Split) splits[i]).account == null)
                    	    ((Split) splits[i]).account = (Account) conversion.accounts.get(((Split) splits[i]).accountGuid);

                        // Write out the split detail
                    	export.exportSplitInformation(conversion, category, currentTransaction, (acctSplit >= 0)? (Split) splits[acctSplit]: null, (Split) splits[i], i + 1);

                        // Use this split's memo as the alternative memo if less than two splits exist
                        if (splits.length <= 2 && splitMemo != null && splitMemo.length() > 0)
                            alternativeMemo = splitMemo;
                    }
                }
            }
            
            // Otherwise, ignore this transaction
            else
            	continue;
            
            // If we have no splits, then the transaction "moved" money from an account to
            // the same account. Warn the user and don't convert this transaction
            if (category.length() == 0) {
            	conversion.warnings.add("Ignoring transaction to/from same account " + getFullName(((Split) splits[acctSplit]).account, conversion.accounts) + ": " + currentTransaction);
            	continue;
            }
            
            // If we don't have a memo but an alternative memo exists, then use the alternative memo
            if (alternativeMemo != null && memo.length() == 0)
                memo = alternativeMemo;
            
            // Write the transaction information
            export.writeTransactionInformation(conversion, writer, currentTransaction, memo, (Split) splits[acctSplit], category.toString());
        }
	}
	
	/**
	 * Interface used by the exportTransactions() method to specify the export format
	 */
	protected static interface TransactionExport
	{
		/**
		 * Export split information into the given output StringBuffer
		 * @param conversion The conversion information object
		 * @param output The destination of the export
      	 * @param invoice The invoice to write transaction information for, which is
	     * only non-null for invoices
		 * @param account The account containing the split
		 * @param transaction The transaction containing the split
		 * @param primarySplit The primary split of the given transaction
		 * @param split The split to export, which should never equal primarySplit
		 * (the primary split is exported in writeTransactionInformation)
		 * @param splitID The ID of the split to export
		 */
		public void exportSplitInformation(Conversion conversion, StringBuffer output, Transaction transaction, Split primarySplit, Split split, int splitID);
		
		/**
		 * Writes the transaction information to the given Writer
		 * @param conversion The conversion information object
		 * @param writer The Writer to write the transaction information to
      	 * @param invoice The invoice to write transaction information for, which is
	     * only non-null for invoices
		 * @param account The account that the transaction belongs to
		 * @param transaction The transaction to export
		 * @param memo The memo to use if not null
		 * @param primarySplit The primary split of the given transaction
		 * @param splits The exported String of splits that resulted from calls
		 * to exportSplitInformation on every split
		 * @throws IOException Thrown if an IOException arises during export
		 */
		public void writeTransactionInformation(Conversion conversion, Writer writer, Transaction transaction, String memo, Split primarySplit, String splits) throws IOException;
	}
	
	/**
	 * Exports a simple general journal transfer
	 */
	protected static class TransferExport implements TransactionExport
	{
		/**
		 * Returns the transaction type given the transaction and primary
		 * split. This method caches its value for fast subsequent lookups
		 * @param transaction The transaction to get the type of
		 * @param primarySplit The primary split of the given transaction
		 * @return The IIF transaction type for this transaction
		 */
		public String getTransactionType(Transaction transaction, Split primarySplit)
		{
			// Return the cached value if present
			if (transaction.exportTransactionType != null)
				return transaction.exportTransactionType;
			
			// Perform action-based checks if we have an action on this split
			if (primarySplit.action != null)
			{
				// Handle bill payments (they have to be CC or CHECK due to a
				// bug in QuickBooks >= 2003 :-(
				if (primarySplit.action.equalsIgnoreCase("payment")) {
					if (primarySplit.account.typeName.equalsIgnoreCase("credit"))
						return transaction.exportTransactionType = "CC";
					else
					    return transaction.exportTransactionType = "CHECK";
				}

				// Handle bills
				else if (primarySplit.action.equalsIgnoreCase("bill"))
				{
					// If the money is going into the primary, then it's a refund;
					// otherwise, it's a bill
					if (primarySplit.amount > 0)
						return transaction.exportTransactionType = "BILL REFUND";
					else
					    return transaction.exportTransactionType = "BILL";
				}
			}
			
			// Otherwise, look at the account type of the primary account and the
			// direction of the money flow
			else
			{
				// Checking and savings accounts
				if (primarySplit.account.typeName.equalsIgnoreCase("bank") ||
					primarySplit.account.typeName.equalsIgnoreCase("savings"))
				{
					// Money out is a check and money in is a deposit
				    if (primarySplit.amount > 0)
				    	return transaction.exportTransactionType = "DEP";
				    else
				    	return transaction.exportTransactionType = "CHECK";
				}
				
				// Credit card accounts
				else if (primarySplit.account.typeName.equalsIgnoreCase("credit"))
				{
					// An increase is a charge and a decrease is a credit
					if (primarySplit.amount > 0)
						return transaction.exportTransactionType = "CC";
					else
						return transaction.exportTransactionType = "CC CRED";
				}
			}
			
			// By default, this is a general journal entry
			return transaction.exportTransactionType = "GENJRNL";
		}
		
		/**
		 * Export split information into the given output StringBuffer
		 * @param conversion The conversion information object
		 * @param output The destination of the export
      	 * @param invoice The invoice to write transaction information for, which is
	     * only non-null for invoices
		 * @param account The account containing the split
		 * @param currentTransaction The transaction containing the split
		 * @param primarySplit The primary split of the given transaction
		 * @param split The split to export, which should never equal primarySplit
		 * (the primary split is exported in writeTransactionInformation)
		 * @param splitID The ID of the split to export
		 */
		public void exportSplitInformation(Conversion conversion, StringBuffer output, Transaction currentTransaction, Split primarySplit, Split split, int splitID)
		{
			output.append("SPL\t");
			output.append(splitID + "\t");
			output.append(getTransactionType(currentTransaction, primarySplit));
			output.append('\t');
			output.append(gnucashDateToIIFDate(currentTransaction.datePosted));
			output.append('\t');
			output.append(getFullName(split.account, conversion.accounts));
			output.append('\t');
			// NAME
			output.append('\t');
			output.append(currencyFormat.format(split.amount));
			output.append('\t');
            
        	// If this has a lot GUID, ...
			String memo = split.memo;
        	if (split.lotGuid != null)
        	{
        		// Resolve the lot GUID to an invoice
        		Invoice invoice = (Invoice) conversion.invoices.get(((Lot) conversion.lots.get(split.lotGuid)).invoiceGuid);
        		
        		// If the invoice was found, ...
        		if (invoice != null)
        		{
        			// Set the document number on this split to the
        			// document number of the bill to help QuickBooks
        			// establish the link between the two
        			output.append(invoice.invoiceTransaction.ref);
        			
        			// If the memo is blank, use the bill reference
        			if (memo == null)
        				memo = invoice.invoiceTransaction.ref;
        		}
        		
        		// If the invoice was not found, give the user a warning
        		else
        			conversion.warnings.add("Invoice not found in transaction " + currentTransaction + " in account " + primarySplit.account.name);
        	}
        	
			output.append('\t');
			if (memo != null)
				output.append(memo);  // FIXME: remove \n, \t, and other invalid chars!
			output.append('\t');
            if (split.reconciliationStatus == 'c' || split.reconciliationStatus == 'y')
            	output.append('T');
            output.append("\r\n");
		}

		/**
		 * Writes the transaction information to the given Writer
		 * @param conversion The conversion information object
		 * @param writer The Writer to write the transaction information to
      	 * @param invoice The invoice to write transaction information for, which is
	     * only non-null for invoices
		 * @param account The account that the transaction belongs to
		 * @param currentTransaction The transaction to export
		 * @param memo The memo to use if not null
		 * @param primarySplit The primary split of the given transaction
		 * @param splits The exported String of splits that resulted from calls
		 * to exportSplitInformation on every split
		 * @throws IOException Thrown if an IOException arises during export
		 */
		public void writeTransactionInformation(Conversion conversion, Writer writer, Transaction currentTransaction, String memo, Split primarySplit, String splits) throws IOException
		{
			// Write out the transaction information
			writer.write("TRNS\t");
			writer.write(currentTransaction.guid + "\t");
			writer.write(getTransactionType(currentTransaction, primarySplit));
			writer.write('\t');
			writer.write(gnucashDateToIIFDate(currentTransaction.datePosted));
			writer.write('\t');
			writer.write(getFullName(primarySplit.account, conversion.accounts));
			writer.write('\t');
			writer.write(currentTransaction.description);
			writer.write('\t');
            writer.write(currencyFormat.format(primarySplit.amount));
			writer.write('\t');
			if (currentTransaction.ref != null)
		        writer.write(currentTransaction.ref);
			writer.write('\t');
			if (memo != null)
				writer.write(memo);
			writer.write('\t');
            if (primarySplit.reconciliationStatus == 'c' || primarySplit.reconciliationStatus == 'y')
            	writer.write('T');
            writer.write('\t');
            if (currentTransaction.dateDue != null)
            	writer.write(gnucashDateToIIFDate(currentTransaction.dateDue));
            writer.write("\tN\t");
            
            // If the primary split's lot's invoice has been posted, then this
            // bill has been paid in full
            Lot lot;
            Invoice invoice;
            if (primarySplit.lotGuid != null && (lot = (Lot) conversion.lots.get(primarySplit.lotGuid)) != null &&
                (invoice = (Invoice) conversion.invoices.get(lot.invoiceGuid)) != null &&
                invoice.datePosted != null && !invoice.datePosted.equals(""))
            	writer.write('Y');
            else
            	writer.write('N');
			writer.write("\r\n");
			
			// Write out the split information
			writer.write(splits);

			// Terminate this transaction
			writer.write("ENDTRNS\r\n");
		}
	}
	
	/**
	 * Writes out the transactions in the given Conversion object to the given Writer
	 * using the IIF format
	 * http://www.datablox.com/qb/qbtran.htm
	 * @param conversion The input conversion parameters
	 * @param writer The output Writer
	 * @throws IOException Thrown when an IOException occurs during the conversion
	 */
	protected static void writeTransactions(Conversion conversion, Writer writer) throws IOException
	{
		// Write the table header
		writer.write("!TRNS\tTRNSID\tTRNSTYPE\tDATE\tACCNT\tNAME\tAMOUNT\tDOCNUM\tMEMO\tCLEAR\tDUEDATE\tTOPRINT\tPAID\r\n");
		writer.write("!SPL\tSPLID\tTRNSTYPE\tDATE\tACCNT\tNAME\tAMOUNT\tDOCNUM\tMEMO\tCLEAR\r\n");
		writer.write("!ENDTRNS\r\n");
		
		// Write out the transaction list for each account
		Iterator accountIterator = conversion.accountSet.iterator();
		Account currentAccount;
		TransactionExport export = new TransferExport();
		while (accountIterator.hasNext()) {
			currentAccount = (Account) accountIterator.next();
			
			// Write general journal transactions
			writeTransactions(conversion, writer, null, currentAccount, currentAccount.trans.iterator(), export);
		}
	}

	/**
	 * Writes out the vendor list using the format at http://www.datablox.com/qb/qbvend.htm
	 * @param conversion The input conversion parameters
	 * @param writer The output Writer
	 * @throws IOException Thrown when an IOException occurs during the conversion
	 */
	protected static void writeVendorList(Conversion conversion, Writer writer) throws IOException
	{
		// Write out the vendor list header
		writer.append("!VEND\tNAME\tREFNUM\tCOMPANYNAME\tPRINTAS\tADDR1\tADDR2\tHIDDEN\r\n");
		
		// Loop through all the vendors
		Iterator vendorIterator = conversion.vendors.iterator();
		Vendor currentVendor;
		while (vendorIterator.hasNext()) {
			currentVendor = (Vendor) vendorIterator.next();
			
			// Write out this vendor's detail
			writer.append("VEND\t");
			writer.append(currentVendor.name);
			writer.append('\t');
			if (currentVendor.id != null)
			    writer.append(currentVendor.id);
			writer.append('\t');
			if (currentVendor.addressName != null)
			    writer.append(currentVendor.addressName);
			writer.append('\t');
			if (currentVendor.addressName != null)   // Assume address name is the company name
			    writer.append(currentVendor.addressName);
			writer.append('\t');
			if (currentVendor.addressLine1 != null)
			    writer.append(currentVendor.addressLine1);
			writer.append('\t');
			if (currentVendor.addressLine2 != null)
			    writer.append(currentVendor.addressLine2);
			writer.append('\t');
			writer.append((currentVendor.active)? 'N': 'Y');
			writer.append("\r\n");
		}
	}
	
	/**
	 * Separates the transactions into their proper buckets so that they
	 * can be exported to QuickBooks properly
	 * @param conversion The conversion object to use
	 */
	protected static void validateTransactions(Conversion conversion)
	{
		// Loop through all the invoices
		Iterator invoiceIterator = conversion.invoices.values().iterator();
		Invoice currentInvoice;
		while (invoiceIterator.hasNext()) {
			currentInvoice = (Invoice) invoiceIterator.next();
			
			// Set the invoice transaction
			currentInvoice.invoiceTransaction = (Transaction) conversion.transactions.get(currentInvoice.invoiceTransactionGuid);
			
			// Set the invoice account object
			currentInvoice.account = (Account) conversion.accounts.get(currentInvoice.accountGuid);
		}
		
		// Loop through all the accounts
//		Iterator accountIterator = conversion.accountSet.iterator();
//		Account currentAccount;
//		while (accountIterator.hasNext()) {
//			currentAccount = (Account) accountIterator.next();
//			
//			// Loop through all the transactions
//	        Iterator transIterator = currentAccount.trans.iterator();
//	        Transaction currentTransaction;
//	        while (transIterator.hasNext()) {
//	            currentTransaction = (Transaction) transIterator.next();
//	            
//	            // Find which split has a lot GUID
//	            Iterator splitIterator = currentTransaction.splits.iterator();
//	            Split currentSplit;
//	            while (splitIterator.hasNext()) {
//	            	currentSplit = (Split) splitIterator.next();
//	            	
//	            	// If this has a lot GUID, ...
//	            	if (currentSplit.lotGuid != null)
//	            	{
//	            		// Resolve the lot GUID to an invoice
//	            		currentInvoice = (Invoice) conversion.invoices.get(((Lot) conversion.lots.get(currentSplit.lotGuid)).invoiceGuid);
//	            		
//	            		// If the invoice was found, ...
//	            		if (currentInvoice != null)
//	            		{
//	            			// Set the document number on this split to the
//	            			// document number of the bill to help QuickBooks
//	            			// establish the link between the two
//	            			currentTransaction.
//	            		}
//	            		
//	            		// If the invoice was not found, throw a warning
//	            		else
//	            			conversion.warnings.add("Invoice not found in transaction " + currentTransaction);
//	            	}
//	            }
	            
//	            // If this is an invoice or invoice payment with one and only one
//	            // lot GUID, then process further
//	            if (invoice != null && currentTransaction.transactionType != null)
//	            {
//	            	// If this is an invoice, remove it after ensuring that it is set
//	            	if (currentTransaction.transactionType.equals("I")) {
//	            		if (invoice.invoiceTransaction != currentTransaction)
//	            			conversion.warnings.add("One or more invoice transactions are not set properly!");
//	            		else
//	            			transIterator.remove();
//	            	}
	            	
//	            	// If this is an invoice payment, then remove it after adding it
//	            	// to the invoice transactions
//	            	else if (currentTransaction.transactionType.equals("P")) {
//	            	    invoice.invoicePayments.add(currentTransaction);
//	            	    transIterator.remove();
//	            	}
//	            }
//	        }
//		}
	}

    /**
     * Returns the full name of this account by looking at our parents
     * and prepending their names to our name while delimiting with colons.
     * For IIF files, accounts that are not the same as their parents are
     * made top-level
     * @param accounts The map of account GUIDs to Account object instances to
     * use when building this account's full name
     * @return The full name of this account
     */
    protected static String getFullName(Account account, Map accounts)
    {
        // If we know the full name, then return it; otherwise, construct it
        if (account.fullName != null)
            return account.fullName;

        // Follow chain of parents, pre-pending their names,
        // so we get "Grandparent:Parent:Name"
        String p = account.parentGuid;
        account.fullName = account.name;
        while (null != p) {
            Account parent = (Account) accounts.get(p);
            
            // If this is the same type as the parent, then proceed
            if (account.typeName.equalsIgnoreCase(parent.typeName)) {
                account.fullName = parent.name + ":" + account.fullName;
                p = parent.parentGuid;
            }
            
            // Otherwise, we have to not loop anymore and, instead,
            // make this account name gathered thus far top-level
            else {
            	break;
            }
        }
        return account.fullName;
    }
    
    /**
     * Updates the full names of the accounts in the given conversion
     * @param conversion The conversion to update the full names in
     */
    protected static void updateFullNames(Conversion conversion)
    {
		// Ensure that all accounts have getFullName() called
		Iterator accountIterator = conversion.accounts.values().iterator();
        Account currentAccount;
        while (accountIterator.hasNext()) {
        	currentAccount = (Account) accountIterator.next();
        	currentAccount.fullName = getFullName(currentAccount, conversion.accounts);
        }
    }
    
	/** Our cached FileFilter singleton instance */
	protected static FileFilter fileFilter = null;

	/**
	 * Returns the IIF format FileFilter
	 * @return The IIF format FileFilter
	 */
	public FileFilter getFileFilter() {
		if (fileFilter == null)
		    return fileFilter = new FileFilter() {
	            public boolean accept(File f) {
	                if (f.isDirectory() || f.getName().endsWith(".iif"))
	                    return true;
	                else
	                    return false;
	            }
	            public String getDescription() {
	                return "IIF (Intuit QuickBooks)";
	            }
	        };
	    else
	        return fileFilter;
	}

    /**
     * Converts a GnuCash date into a IIF date
     * @param gnucashDate The GnuCash date to convert
     * @return The IIF-friendly date (4-digit year)
     */
    protected static String gnucashDateToIIFDate(String gnucashDate) {
        if (gnucashDate == null || gnucashDate.length() < 10)
            return "";
        return gnucashDate.substring(5, 7) + "/" + gnucashDate.substring(8, 10) + "/" + gnucashDate.substring(0, 4);
    }

    /**
     * Sets the account type by its type name
     * @param typeName The name of the type to set this account to
     */
    protected static void setAccountType(Account account)
    {
        // Set income and expense accounts, not caring about others
        if (account.typeName.equals("income")) {
        	account.type = Account.TYPE_DOUBLEENTRY_INCOME;
        } else if (account.typeName.equals("expense")) {
        	account.type = Account.TYPE_DOUBLEENTRY_EXPENSE;
        }
    }
}
