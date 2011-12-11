package net.sourceforge.gnucashtoqif;

import javax.swing.JPanel;
import java.awt.GridBagLayout;
import javax.swing.JCheckBox;
import java.awt.GridBagConstraints;

/**
 * Option panel for the file chooser. This was created and can be maintained with
 * Eclipse's visual editor
 * @author Steven Lawrance
 */
public class OptionPanel extends JPanel {

    private JCheckBox splitMemoFromDescription = null;
    private JCheckBox pruneUnusedAccounts = null;

    static final long serialVersionUID = 1;
    
    /**
     * Returns whether or not unused account pruning is enabled or disabled
     * @return Whether or not unused account pruning is enabled or disabled
     */
    public boolean isUnusedAccountPruningEnabled() {
        return pruneUnusedAccounts.isSelected();
    }
    
    /**
     * Sets whether or not unused account pruning is enabled or disabled
     * @param enabled True to enable unused account pruning or false otherwise
     */
    public void setUnusedAccountPruningEnabled(boolean enabled) {
        pruneUnusedAccounts.setSelected(enabled);
    }
    
    /**
     * Returns whether or not memo splitting is enabled or disabled
     * @return Whether or not memo splitting is enabled or disabled
     */
    public boolean isMemoSplittingEnabled() {
        return splitMemoFromDescription.isSelected();
    }
    
    /**
     * Sets whether or not memo splitting is enabled or disabled
     * @param enabled True to enable memo splitting or false otherwise
     */
    public void setMemoSplittingEnabled(boolean enabled) {
        splitMemoFromDescription.setSelected(enabled);
    }
    
    /**
     * This method initializes 
     * 
     */
    public OptionPanel() {
    	super();
    	initialize();
    }

    /**
     * This method initializes this
     * 
     */
    private void initialize() {
        GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
        gridBagConstraints1.gridx = 0;
        gridBagConstraints1.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints1.insets = new java.awt.Insets(5,5,0,5);
        gridBagConstraints1.gridy = 0;
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5,5,0,5);
        gridBagConstraints.gridy = 1;
        this.setLayout(new GridBagLayout());
        this.add(getSplitMemoFromDescription(), gridBagConstraints);
        this.add(getPruneUnusedAccounts(), gridBagConstraints1);
    }

    /**
     * This method initializes splitMemoFromDescription	
     * 	
     * @return javax.swing.JCheckBox	
     */
    private JCheckBox getSplitMemoFromDescription() {
        if (splitMemoFromDescription == null) {
            splitMemoFromDescription = new JCheckBox();
            splitMemoFromDescription.setText("Extract memos from descriptions");
            splitMemoFromDescription.setSelected(true);
        }
        return splitMemoFromDescription;
    }

    /**
     * This method initializes pruneUnusedAccounts	
     * 	
     * @return javax.swing.JCheckBox	
     */
    private JCheckBox getPruneUnusedAccounts() {
        if (pruneUnusedAccounts == null) {
            pruneUnusedAccounts = new JCheckBox();
            pruneUnusedAccounts.setText("Ignore unused accounts");
            pruneUnusedAccounts.setSelected(true);
        }
        return pruneUnusedAccounts;
    }
}
