package net.sourceforge.gnucashtoqif;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import javax.swing.filechooser.FileFilter;

/**
 * QIF &lt;= 2003 format filter
 */
public class QIF2003OutputFormat implements OutputFormat
{
    /** A number formatter for the currency that gets written to the QIF file */
    protected static final NumberFormat currencyFormat = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.US));

	/**
	 * Write out QIF &lt;= 2003 data to the given writer using the input conversion parameters
	 * @param conversion The accounts and their transaction data
	 * @param writer The writer to output the converted data to
	 */
	public void export(Conversion conversion, Writer writer) throws IOException
	{
		// Ensure that all accounts have getFullName() called
		updateFullNames(conversion);
		
		// Create a Set of sorted accounts
	    conversion.accountSet = new TreeSet(new AccountComparator());
	    conversion.accountSet.addAll(conversion.accounts.values());

        // Write out a blank line (not sure if this is really necessary, though; it's to
        // exactly match Quicken's own QIF export output)
        writer.write("\n");
        
        // Write out the Account list
        writeAccountList(writer, conversion);

        // Write out the Category list
        writeCategoryList(writer, conversion);
        
        // Loop through all the non-double-entry accounts
        Iterator accountIterator = conversion.accountSet.iterator();
        Account currentAccount;
        while (accountIterator.hasNext()) {
            currentAccount = (Account) accountIterator.next();
            if (!currentAccount.isDoubleEntry() && currentAccount.trans.size() > 0)
            {
                // Write out this Account's header and transactions
                writeAccountTransactionHeader(writer, currentAccount);
                writeAccountTransactions(writer, currentAccount, conversion, false);
            }
        }
	}
	
	/** Our cached FileFilter singleton instance */
	protected static FileFilter fileFilter = null;

	/**
	 * Returns the QIF &lt;= 2003 format FileFilter
	 * @return The QIF &lt;= 2003 format FileFilter
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
	                return "QIF File <= 2003";
	            }
	        };
	    else
	        return fileFilter;
	}
	
	/** Matcher that matches possibly invalid account names */
	protected static Pattern badAccountNames = Pattern.compile("[:/]");

    /**
     * Writes out the account list to the given Writer object as QIF 2003
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
            
            // If this is not a double-entry account, then write out its information
            // if it has at least one transaction or if we're supposed to write out
            // all accounts
            if (!current.isDoubleEntry() && (current.trans.size() > 0 || !conversion.pruneUnusedAccounts)) {
                name = current.name;
                description = current.getDescription("");
                if (name.length() > 33)
                    conversion.warnings.add("Account \"" + name + "\" might import as \"" + name.substring(0, 33) + "\" in Quicken");
                writer.write("N" + name + "\n");
                if (description.length() > 64)
                    conversion.warnings.add("Description of account \"" + name + "\" is " + description.length() + " characters, which might crash Quicken while importing. Shrink to at most 64 characters");
                if (badAccountNames.matcher(name).find())
                	conversion.warnings.add("Account \"" + name + "\" might not import properly due to : or / in its name");
                writer.write("D" + description + "\n");
                writer.write("X\n");
                writer.write("T" + current.getQIFTypeName() + "\n");
                writer.write("^\n");
            }
        }
        writer.write("!Clear:AutoSwitch\n");
    }
    
    /**
     * Writes out the header information for an account in the QIF 2003 format
     * @param writer The Writer to write QIF data to
     * @param account The Account to write to the Writer
     * @throws IOException Thrown if an error comes up while
     * writing to the Writer
     */
    protected void writeAccountTransactionHeader(Writer writer, Account account) throws IOException
    {
        // Write out the account type header in the QIF 2003 format
        String type = account.getQIFTypeName();
        writer.write("!Account\n");
        writer.write("N" + account.name + "\n");
        writer.write("D\n");
        writer.write("X\n");
        writer.write("T" + type + "\n");
        writer.write("^\n");
        writer.write("!Type:" + type + "\n");
    }

    /**
     * Writes out the category list to the given Writer object
     * @param writer The Writer to write QIF data to
     * @throws IOException Thrown if an error comes up while
     * writing to the Writer
     */
    protected void writeCategoryList(Writer writer, Conversion conversion) throws IOException
    {
        // Loop through all the accounts, writing out the names and descriptions
        // of the double-entry accounts
        writer.write("!Type:Cat\n");
        Iterator iterator = conversion.accountSet.iterator();
        Account current;
        String name, description;
        while (iterator.hasNext()) {
            current = (Account) iterator.next();
            
            // Set the account type integer in Account for this Account
            setAccountType(current, conversion.warnings);

            // If this is a double-entry account, then write out its information
            // if it has at least one transaction or if we're supposed to write out
            // all accounts
            if (current.isDoubleEntry() && (current.trans.size() > 0 || !conversion.pruneUnusedAccounts)) {
                name = getFullName(current, conversion.accounts);
                description = current.getDescription("");
                if (name.length() > 33)
                    conversion.warnings.add("Category \"" + name + "\" might import as \"" + name.substring(0, 33) + "\" in Quicken");
                writer.write("N" + name + "\n");
                if (description.length() > 64)
                    conversion.warnings.add("Description of category \"" + name + "\" is " + description.length() + " characters, which might crash Quicken while importing. Shrink to at most 64 characters");
                writer.write("D" + description + "\n");
                if (current.type == Account.TYPE_DOUBLEENTRY_INCOME)
                    writer.write("I\n");
                else if (current.type == Account.TYPE_DOUBLEENTRY_EXPENSE)
                    writer.write("E\n");
                writer.write("^\n");
            }
        }
    }

    /**
     * Writes out the given Account and all of its transactions
     * to the given Writer object. Split transactions are
     * handled accordingly depending on whether or not their
     * account GUIDs exist in the accounts object.
     * @param writer The Writer to write QIF data to
     * @param account The Account to write to the Writer
     * @param duplicate When true, transactions are listed in all affected
     * accounts, though in a way that lists the split in only the primary
     * account; when false, transactions are only listed in the primary account
     * @throws IOException Thrown if an error comes up while
     * writing to the Writer
     */
    protected void writeAccountTransactions(Writer writer, Account account, Conversion conversion, boolean duplicate) throws IOException
    {
        // Write out every transaction
        Iterator transIterator = account.trans.iterator();
        Object splits[] = new Object[2];
        int i, acctSplit;
        String payee, memo, splitMemo, alternativeMemo;
        StringBuffer category = new StringBuffer();
        Account lastAccount = null;
        boolean splitTransaction, firstSplit, primaryAccount;
        Transaction currentTransaction;
        while (transIterator.hasNext()) {
            currentTransaction = (Transaction) transIterator.next();
            
            // Discover if this account is the last account listed in the splits
            splits = currentTransaction.splits.toArray(splits);
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
            
            // If we are not supposed to duplicate transactions, then continue to
            // the next transaction if this is not the primary account
            if (!primaryAccount && !duplicate)
                continue;
            
            // Write out the post date and the reference
            writer.write("D" + gnucashDateToQIFDate(currentTransaction.datePosted) + "\n");
            if (currentTransaction.ref != null)
                writer.write("N" + currentTransaction.ref + "\n");
            
            // Reset the memo field so that we can attempt to get it from the primary split
            memo = "";
            
            // Also reset the alternative memo, which is a memo attached to a complementary
            // record in a simple two-account transaction
            alternativeMemo = null;

            // If this is the transaction's primary account, then write out the full detail
            category.setLength(0);
            if (primaryAccount)
            {
                // Find out what the target account is through the splits
                if (currentTransaction.splits.size() > 2)
                    splitTransaction = true;
                else
                    splitTransaction = false;
                firstSplit = true;
                for (i = 0; i < splits.length && splits[i] != null; i++)
                {
                    // Get the split memo
                    splitMemo = ((Split) splits[i]).memo;
                    
                    // If this is split is the current account's split, then use
                    // its data for the overall transaction details
                    if (((Split) splits[i]).accountGuid.equalsIgnoreCase(account.guid)) {
                        acctSplit = i;

                        // Use this split's memo as the main memo if one exists
                        if (splitMemo != null && splitMemo.length() > 0)
                            memo = splitMemo;
                    } else
                    {
                        // If this is a split transaction, then write out split detail;
                        // otherwise, write out the non-split detail
                        if (splitTransaction) {
                            Account target = (Account) conversion.accounts.get(((Split) splits[i]).accountGuid);
                            if (target != null) {
                                if (target.isDoubleEntry()) {
                                    if (firstSplit)
                                        category.append("L" + getFullName(target, conversion.accounts) + "\n");
                                    category.append("S" + getFullName(target, conversion.accounts) + "\n");
                                } else {
                                    if (firstSplit)
                                        category.append("L[" + target.name + "]\n");
                                    category.append("S[" + target.name + "]\n");
                                }
                            } else {
                                if (firstSplit)
                                    category.append("LUnknown\n");
                                category.append("SUnknown\n");
                            }
                            
                            // Include a memo if one exists
                            if (splitMemo != null && splitMemo.length() > 0)
                                category.append("E" + splitMemo + "\n");

                            category.append("$" + currencyFormat.format(0 - ((Split) splits[i]).amount) + "\n");
                            firstSplit = false;
                        } else {
                            Account target = (Account) conversion.accounts.get(((Split) splits[i]).accountGuid);
                            if (target != null) {
                                if (target.isDoubleEntry())
                                    category.append("L" + getFullName(target, conversion.accounts) + "\n");
                                else
                                    category.append("L[" + target.name + "]\n");
                            } else
                                category.append("LUnknown\n");

                            // Use this split's memo as the alternative memo
                            if (splitMemo != null && splitMemo.length() > 0)
                                alternativeMemo = splitMemo;
                        }
                    }
                }
            }
            
            // Otherwise, write out the linked detail to the master transaction
            else
            {
                // Set the link
                category.append("L[" + lastAccount.name + "]\n");

                // Look up which split specifies this account so that the proper amount gets written
                acctSplit = -1;
                for (i = 0; i < splits.length && splits[i] != null; i++) {
                    if (((Split) splits[i]).accountGuid.equalsIgnoreCase(account.guid)) {
                        acctSplit = i;

                        // Get the split memo
                        splitMemo = ((Split) splits[i]).memo;

                        // Use this split's memo as the main memo if one exists
                        if (splitMemo != null && splitMemo.length() > 0)
                            memo = splitMemo;
                    }
                }
            }
            
            // If we don't have a memo but an alternative memo exists, then use the alternative memo
            if (alternativeMemo != null && memo.length() == 0)
                memo = alternativeMemo;

            // If memo extraction from GnuCash descriptions is enabled and no memo exists yet,
            // then proceed with that. Otherwise, simply leave the description alone
            if (conversion.splitMemoFromDescription && memo.length() == 0)
            {
                // The following code converted "my" way of using GnuCash into QIF.
                // YMMV, so please feel free to modify this code to suit your data
                // conversion needs :-)

                // Payee = text after "at" or "from", or desc if not exist
                // Memo = header of Payee if split as per above
                // Category = acct name

                // Split out the "at" or "from", if it's there
                int split = currentTransaction.description.indexOf(" at ");
                int descLength = currentTransaction.description.length();
                if (split > 0 && descLength > split + 4) {
                    payee = GnuCashToQIF.capitalizeFirstLetter(currentTransaction.description.substring(split + 4));
                    memo = currentTransaction.description.substring(0, split);
                } else if ((split = currentTransaction.description.indexOf(" from ")) > 0 && descLength > split + 6) {
                    payee = GnuCashToQIF.capitalizeFirstLetter(currentTransaction.description.substring(split + 6));
                    memo = currentTransaction.description.substring(0, split);
                } else if ((split = currentTransaction.description.indexOf(" via ")) > 0 && descLength > split + 5) {
                    payee = GnuCashToQIF.capitalizeFirstLetter(currentTransaction.description.substring(split + 5));
                    memo = currentTransaction.description.substring(0, split);
                } else {
                    payee = currentTransaction.description;
                }
            } else
                payee = currentTransaction.description;

            // Write out the amount, our cleared status, and the categories/splits
            writer.write("U" + currencyFormat.format(((Split) splits[acctSplit]).amount) + "\n");
            writer.write("T" + currencyFormat.format(((Split) splits[acctSplit]).amount) + "\n");
            writer.write("P" + payee + "\n");
            if (memo.length() > 0)
                writer.write("M" + memo + "\n");
            if (((Split) splits[acctSplit]).reconciliationStatus == 'c' || ((Split) splits[acctSplit]).reconciliationStatus == 'y')
                writer.write("C*\n");
            writer.write(category.toString());

            // Write out the end of this transaction record
            writer.write("^\n");
        }
    }
    
    /**
     * Sets the account type by its type name
     * @param typeName The name of the type to set this account to
     * @param warnings Set of that warnings encountered while setting the
     * account type will get placed into
     */
    protected static void setAccountType(Account account, Set warnings)
    {
        // Handle our known types and assume that all others are
        // double-entry accounts
        if (account.typeName.equals("bank")) {
        	account.type = Account.TYPE_BANK;
        } else if (account.typeName.equals("credit")) {
        	account.type = Account.TYPE_CREDIT;
        } else if (account.typeName.equals("cash") || account.typeName.equals("currency")) {
        	account.type = Account.TYPE_CASH;
        } else if (account.typeName.equals("asset")) {
        	account.type = Account.TYPE_ASSET;
        } else if (account.typeName.equals("mutual") || account.typeName.equals("stock") ||
        		account.typeName.equals("liability") || account.typeName.equals("receivable")) {
        	account.type = Account.TYPE_BANK;
            warnings.add(GnuCashToQIF.capitalizeFirstLetter(account.typeName) + " accounts are currently imported as regular bank accounts");
        } else if (account.typeName.equals("payable")) {
        	account.type = Account.TYPE_CREDIT;
        	warnings.add(GnuCashToQIF.capitalizeFirstLetter(account.typeName) + " accounts are currently imported as credit card accounts");
        } else if (account.typeName.equals("income")) {
        	account.type = Account.TYPE_DOUBLEENTRY_INCOME;
        } else if (account.typeName.equals("expense")) {
        	account.type = Account.TYPE_DOUBLEENTRY_EXPENSE;
        } else {
        	account.type = Account.TYPE_DOUBLEENTRY;
        }
    }
    
    /**
     * Returns the full name of this account by looking at our parents
     * and prepending their names to our name while delimiting with colons
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
            
            // Omit the top-level name; it's "Expense" or "Income" or something
            // and is unnecessary in Quicken
            if (null == parent.parentGuid) {
                break;
            }
            account.fullName = parent.name + ":" + account.fullName;
            p = parent.parentGuid;
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
    
    /**
     * Converts a GnuCash date into a QIF date
     * @param gnucashDate The GnuCash date to convert
     * @return The QIF-friendly date (4-digit year)
     */
    protected static String gnucashDateToQIFDate(String gnucashDate) {
        if (gnucashDate == null || gnucashDate.length() < 10)
            return "";
        return gnucashDate.substring(5, 7) + "/" + gnucashDate.substring(8, 10) + "/" + gnucashDate.substring(0, 4);
    }
}
