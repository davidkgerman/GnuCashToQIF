package net.sourceforge.gnucashtoqif;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * GnuCashToQIF conversion configuration object
 */
public class Conversion
{
    /** Unused account pruning configuration option */
    public boolean pruneUnusedAccounts = true;
    
    /** Split memos from descriptions */
    public boolean splitMemoFromDescription = true;

    /** The accounts, using their GUIDs as the key */
    public Map accounts = new HashMap();
    
    /** The transactions, using their GUIDs as the key */
    public Map transactions = new HashMap();
    
    /** Set of warnings that come up during processing */
    public SortedSet warnings = new TreeSet();

    /** Sorted Set of accounts */
    public SortedSet accountSet = null;
    
    /** Set of vendors */
    public Set vendors = new HashSet();
    
    /** Map of lot GUIDs to Lot objects */
    public Map lots = new HashMap();
    
    /** Map of invoice GUIDs to invoices */
    public Map invoices = new HashMap();
}
