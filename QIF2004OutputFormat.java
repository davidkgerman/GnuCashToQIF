package net.sourceforge.gnucashtoqif;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.TreeSet;

import javax.swing.filechooser.FileFilter;

public class QIF2004OutputFormat extends QIF2003OutputFormat
{
	/**
	 * Write out QIF &gt;= 2004 data to the given writer using the input conversion parameters
	 * @param conversion The accounts and their transaction data
	 * @param writer The writer to output the converted data to
	 * @return Set of warnings, which is never null
	 */
	public void export(Conversion conversion, Writer writer) throws IOException
	{
		// Ensure that all accounts have getFullName() called
		updateFullNames(conversion);
		
		// Create a Set of sorted accounts
	    conversion.accountSet = new TreeSet(new AccountComparator());
	    conversion.accountSet.addAll(conversion.accounts.values());

        // Write out the Category list
        writeCategoryList(writer, conversion);            
        
        // Write out the Account list
        writeAccountList(writer, conversion);

        // Turn AutoSwitch back on for the transactions
        writer.write("!Option:AutoSwitch\n");
        
        // Loop through all the non-double-entry accounts
        Iterator accountIterator = conversion.accountSet.iterator();
        Account currentAccount;
        while (accountIterator.hasNext()) {
            currentAccount = (Account) accountIterator.next();
            if (!currentAccount.isDoubleEntry() && currentAccount.trans.size() > 0)
            {
                // Write out this Account's header and transactions
                writeAccountTransactionHeader(writer, currentAccount);
                writeAccountTransactions(writer, currentAccount, conversion, true);
            }
        }
	}

	/** Our cached FileFilter singleton instance */
	protected static FileFilter fileFilter = null;

	/**
	 * Returns the QIF &gt;= 2004 format FileFilter
	 * @return The QIF &gt;= 2004 format FileFilter
	 */
	public FileFilter getFileFilter() {
		if (fileFilter == null)
		    return fileFilter = new FileFilter() {
	            public boolean accept(File f) {
	                if (f.isDirectory() || f.getName().endsWith(".qif"))
	                    return true;
	                else
	                    return false;
	            }
	            public String getDescription() {
	                return "QIF File >= 2004";
	            }
	        };
	    else
	        return fileFilter;
	}

    /**
     * Writes out the account list to the given Writer object as QIF 2004
     * @param writer The Writer to write QIF data to
     * @throws IOException Thrown if an error comes up while
     * writing to the Writer
     */
    protected void writeAccountList(Writer writer, Conversion conversion) throws IOException
    {
        // Loop through all the accounts, writing out the names and descriptions
        // of the non-double-entry accounts. All other information came from
        // the output of a Quicken QIF Export, and I'm not sure exactly what
        // they mean (AutoSwitch, that is)
        writer.write("!Option:AutoSwitch\n");
        writer.write("!Account\n");
        Iterator iterator = conversion.accountSet.iterator();
        Account current;
        String name, description;
        while (iterator.hasNext()) {
            current = (Account) iterator.next();
            
            // Set the account type integer in Account for this Account
            setAccountType(current, conversion.warnings);

            // If this is not a double-entry account, then write out its information
            // if it has at least one transaction or if we're supposed to write out
            // all accounts
            if (!current.isDoubleEntry() && (current.trans.size() > 0 || !conversion.pruneUnusedAccounts)) {
                name = current.name;
                description = current.getDescription("");
                if (name.length() > 33)
                    conversion.warnings.add("Account \"" + name + "\" might import as \"" + name.substring(0, 33) + "\" in Quicken");
                writer.write("N" + name + "\n");
                writer.write("T" + current.getQIFTypeName() + "\n");
                if (description.length() > 64)
                    conversion.warnings.add("Description of account \"" + name + "\" is " + description.length() + " characters, which might crash Quicken while importing. Shrink to at most 64 characters");
                if (badAccountNames.matcher(name).find())
                	conversion.warnings.add("Account \"" + name + "\" might not import properly due to : or / in its name");
                writer.write("D" + description + "\n");
                
                // If this is a credit card, then write out a fake credit limit
                // (please feel free to implement this as a proper import from GnuCash)
                if (current.type == Account.TYPE_CREDIT)
                    writer.write("L0.00\n");
                
                writer.write("^\n");
            }
        }
        writer.write("!Clear:AutoSwitch\n");
	}

	/**
     * Writes out the header information for an account in the QIF 2004 format
     * @param writer The Writer to write QIF data to
     * @param account The Account to write to the Writer
     * @throws IOException Thrown if an error comes up while
     * writing to the Writer
     */
	protected void writeAccountTransactionHeader(Writer writer, Account account) throws IOException
	{
        // Write out the account type header in the QIF 2004 format
        String type = account.getQIFTypeName();
        writer.write("!Account\n");
        writer.write("N" + account.name + "\n");
        writer.write("T" + type + "\n");
        writer.write("^\n");
        writer.write("!Type:" + type + "\n");
	}
}
