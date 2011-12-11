package net.sourceforge.gnucashtoqif;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Iterator;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

/**
 * <p>Title: GnuCashToQIF</p>
 * <p>Description: This program converts GnuCash files into multiple
 * QIF files</p>
 * <p>License: Public Domain</p>
 * @author Steven Lawrance
 * @version 1.6_pre1
 */
public class GnuCashToQIF
{
    /**
     * Run the GnuCash to QIF Data Conversion program
     * @param args The arguments from the command line
     */
    public static void main(String[] args) {
        GnuCashToQIF program = new GnuCashToQIF();
        
        // Construct the valid command-line parameters
        LongOpt options[] = new LongOpt[4];
        options[0] = new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h');
        options[1] = new LongOpt("output-format", LongOpt.REQUIRED_ARGUMENT, null, 't');
        options[2] = new LongOpt("ignore-unused", LongOpt.OPTIONAL_ARGUMENT, null, 'i');
        options[3] = new LongOpt("extract-memos", LongOpt.OPTIONAL_ARGUMENT, null, 'm');

        // Parse the command-line arguments
        Getopt opt = new Getopt(GnuCashToQIF.class.getName(), args, "t:i::m::", options);
        int c;
        while ((c = opt.getopt()) != -1) {
            switch (c)
            {
                case '?' :  // Option error; display the help
                case 'h' :
                {
                    // Write out our help page
                    String launchString = "    java -jar GnuCashToQIF-1.6_pre1.jar ";
                    System.out.println("GnuCashToQIF command line options");
                    System.out.print(GnuCashToQIF.class.getName());
                    System.out.println(" [options] [source [output]]");
                    System.out.println();
                    System.out.println("Options:");
                    System.out.println("--extract-memos     Extract a memo from a description when");
                    System.out.println("                    a memo does not exist.");
                    System.out.println("                    Values: yes, no");
                    System.out.println("                    Default: yes");
                    System.out.println("--ignore-unused     Ignore unused accounts and categories,");
                    System.out.println("                    leaving them out of the output file.");
                    System.out.println("                    Values: yes, no");
                    System.out.println("                    Default: yes");
                    System.out.println("--output-format     Set the output file compatibility format.");
                    System.out.println("                    Values: 2003, 2004, iif");
                    System.out.println("                    Default: 2004");
                    System.out.println();
                    System.out.println("Source: The source GnuCash file name or - for stdin");
                    System.out.println();
                    System.out.println("Output: The destination GnuCash file name or - for stdout");
                    System.out.println();
                    System.out.println("Examples:");
                    System.out.println("Use the GUI, but disable memo extraction by default:");
                    System.out.print(launchString);
                    System.out.println("--extract-memos=no");
                    System.out.println("Use the GUI, but disable memo extraction and unused account ignoring by default:");
                    System.out.print(launchString);
                    System.out.println("--extract-memos=no --ignore-unused=no");
                    System.out.println("Use the GUI, but set the input file and Quicken <= 2003 file format:");
                    System.out.print(launchString);
                    System.out.println("--output-format=2003 my.gnucash");
                    System.out.println("Read a GnuCash file and output the QIF file in the 2004 format to stdout:");
                    System.out.print(launchString);
                    System.out.println("--output-format=2004 my.gnucash -");
                    System.out.println("Read a GnuCash file from stdin and write to stdout:");
                    System.out.print(launchString);
                    System.out.println("- -");
                    System.out.println();
                    System.out.println("Author: Steven Lawrance <slawrance@yahoo.com>");
                    System.out.println();
                    System.out.println("Licenses:");
                    System.out.println("  GnuCashToQIF 1.6_pre1:    Public Domain");
                    System.out.println("  Apache Xerces 1.4.4: Apache Software License, Version 1.1");
                    System.out.println("  GNU Getopt 1.0.12:   GNU Library General Public License, Version 2");
                    
                    // Exit normally, though indicate error if getopt gave us '?'
                    System.exit((c == '?')? 1: 0);
                    break;
                }
                case 't' :
                {
                    // Get the specified default file format
                    int format;
                    try {
                        String val = opt.getOptarg();
                        if (val.equalsIgnoreCase("iif"))
                        	format = -1;
                        else {
                            if (val != null)
                                format = Integer.parseInt(val, 10);
                            else
                                format = 2004;
                        }
                    } catch (NumberFormatException e) {
                        format = 2004;
                    }
                    
                    // Adjust two-digit years, assuming that <= 69 is really 2069
                    if (format < 70)
                        format += 2000;
                    else if (format < 100)
                        format += 1900;
                    else if (format < 1970) {
                        System.out.println("Unknown year " + format + "; please specify 2003, 2004, or IIF");
                        System.exit(1);
                    }
                    
                    // Set the default file format
                    if (format <= 2003)
                        program.outputFileFormat = qifFile2003Type;
                    else
                        program.outputFileFormat = qifFile2004Type;
                    break;
                }
                case 'i' :
                {
                    // Whether or not we should ignore unused accounts
                    String value = opt.getOptarg();
                    if (value != null && (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("no") || value.equals("0")))
                        program.conversion.pruneUnusedAccounts = false;
                    else
                        program.conversion.pruneUnusedAccounts = true;
                    break;
                }
                case 'm' :
                {
                    // Whether or not we should extract memos from descriptions
                    String value = opt.getOptarg();
                    if (value != null && (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("no") || value.equals("0")))
                        program.conversion.splitMemoFromDescription = false;
                    else
                        program.conversion.splitMemoFromDescription = true;
                    break;
                }
            }
        }
        
        // Get the non-option argument start element
        c = opt.getOptind();

        // Get the source file
        File sourceFile;
        if (args.length - c >= 1) {
            sourceFile = new File(args[c]);
            if ((!sourceFile.exists() || !sourceFile.canRead()) && !sourceFile.getName().equals("-")) {
                System.out.println("File \"" + sourceFile.getPath() + "\" not found or could not be read; exiting");
                sourceFile = null;
            }
        } else
            sourceFile = program.getSourceFile();
        if (sourceFile == null) {
            System.exit(1);
            return;
        }
        
        // Get the destination file
        File destFile;
        if (args.length - c >= 2) {
            destFile = new File(args[c + 1]);
            if (!destFile.getName().equals("-")) {
                try {
                    destFile.createNewFile();
                } catch (IOException e) {
                    System.out.println("Could not create \"" + destFile.getPath() + "\"; exiting");
                    destFile = null;
                }
            }
        } else
            destFile = program.getDestinationFile(sourceFile);
        if (destFile == null) {
            System.exit(1);
            return;
        }
        
        // Read the source file
        try {
            Reader fileReader;
            if (sourceFile.getName().equals("-"))
                fileReader = new InputStreamReader(System.in);
            else
                fileReader = new FileReader(sourceFile);
            GnuCashData.importGnuCash(fileReader, program.conversion);
            if (fileReader instanceof FileReader)
                fileReader.close();
        } catch (java.io.IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                null,
                "File read error during XML parsing; check stderr",
                "GnuCash->QIF Data Conversion",
                JOptionPane.ERROR_MESSAGE);
            System.exit(2);
            return;
        } catch (org.xml.sax.SAXException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                null,
                "Corrupt GnuCash file: Invalid XML; check stderr",
                "GnuCash->QIF Data Conversion",
                JOptionPane.ERROR_MESSAGE);
            System.exit(2);
            return;
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                null,
                "General Exception " + e.toString() + "; check stderr",
                "GnuCash->QIF Data Conversion",
                JOptionPane.ERROR_MESSAGE);
            System.exit(2);
            return;
        }
        
        // Write the destination file
        if (!program.writeDestinationFile(destFile)) {
            String message = "One or more files could not be written; consult the source code or ask for help";
            if (args.length - c > 0)
                System.out.println(message);
            else
                JOptionPane.showMessageDialog(null, message, "GnuCash->QIF Data Conversion", JOptionPane.WARNING_MESSAGE);
            System.exit(3);
            return;
        }
        
        // We're done! Write out any warnings that might have been gathered
        if (args.length - c >= 2) {
            Iterator warning = program.conversion.warnings.iterator();
            while (warning.hasNext()) {
                System.err.print("Warning: ");
                System.err.println(warning.next().toString());
            }
        } else {
            Iterator warning = program.conversion.warnings.iterator();
            StringBuffer messages = new StringBuffer();
            while (warning.hasNext()) {
                messages.append("  ");
                messages.append(warning.next().toString());
                messages.append('\n');
            }
            
            // Finish constructing the completion message
            String completionMessage = "Data Conversion Successful :-)!";
            if (messages.length() > 0)
                completionMessage += "\n\nThe following warnings came up during the conversion:\n" + messages;
            
            // Notify the user
            JOptionPane.showMessageDialog(
                null,
                completionMessage,
                "Data Conversion",
                JOptionPane.INFORMATION_MESSAGE);
        }
        
        // Close the virtual machine
        System.exit(0);
    }
    
    /**
     * Returns the QIF File of the passed-in GnuCash File
     * @param gnucashFile The GnuCash File to get the QIF File of
     * @return The QIF File representation of the given GnuCash File
     */
    protected static File getQIFFileFromGnuCashFile(File gnucashFile)
    {
        // Chop off the text after the last period (don't do that if the
        // file begins with a period and has no extension, though). If we
        // could not chop, then simply add .qif
        String name = gnucashFile.getName();
        int pos = name.lastIndexOf('.');
        if (pos > 0)
            return new File(gnucashFile.getParent(), name.substring(0, pos) + ".qif");
        else
            return new File(gnucashFile.getParent(), name + ".qif");
    }

    /**
     * Returns the destination QIF file
     * @param gnucashFile The GnuCash File, which we will extract path information
     * from
     * @return The File describing the target QIF File, or null
     * if the user did not select or type in a new file or if the parent
     * directory does not have write permissions.
     */
    protected File getDestinationFile(File gnucashFile)
    {
        // Ask the user for the destination file
        JFileChooser chooser = createOutputChooser(gnucashFile.getParentFile());
        chooser.setDialogType(JFileChooser.SAVE_DIALOG);
        chooser.setDialogTitle("Select Destination File");
        chooser.setSelectedFile(getQIFFileFromGnuCashFile(gnucashFile));
        if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION)
            return null;
        
        // Get the selected QIF format and remember it
        FileFilter fileFilter = chooser.getFileFilter();
        if (fileFilter == qifFile2003Type.getFileFilter())
        	outputFileFormat = qifFile2003Type;
        else if (fileFilter == qifFile2004Type.getFileFilter())
        	outputFileFormat = qifFile2004Type;
        else if (fileFilter == iifFileType.getFileFilter())
        	outputFileFormat = iifFileType;

        // If the file exists, ask if we should overwrite
        File file = chooser.getSelectedFile();
        if (file.exists()) {
            if (JOptionPane.showConfirmDialog(
                    null,
                    "File \"" + file.getName() + "\" already exists. Overwrite?",
                    "Destination File",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION)
                return getDestinationFile(gnucashFile);
        } else
        {
            // File does not exist; ensure that the permissions are correct and, if so,
            // attempt to create the file
            try
            {
                // Create the new file
                file.createNewFile();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(
                    null,
                    "Could not create the destination file; check the parent directory's permissions",
                    "Destination File",
                    JOptionPane.ERROR_MESSAGE);
                return null;                
            }
        }
        
        // Set our configuration from the option panel
        OptionPanel config = (OptionPanel) chooser.getAccessory();
        conversion.pruneUnusedAccounts = config.isUnusedAccountPruningEnabled();
        conversion.splitMemoFromDescription = config.isMemoSplittingEnabled();

        // Return the selected directory
        return file;
    }

    /**
     * Returns the source File object as selected by the user.
     * This method ensures that the file can be read, returning
     * null if this process cannot read it after informing
     * the user.
     * @return The File that the user selected, or null if the
     * user did not select a file
     */
    protected File getSourceFile()
    {
        // Ask the user for the source file
        JFileChooser chooser = createGnuCashChooser(new File(System.getProperty("user.home")));
        chooser.setDialogTitle("Select Source GnuCash File");
        if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
            return null;

        // Create a File and ensure that it can be read
        File file = chooser.getSelectedFile();
        if (!file.canRead()) {
            JOptionPane.showMessageDialog(
                null,
                "Selected file cannot be read; check its permissions",
                "Source GnuCash File",
                JOptionPane.ERROR_MESSAGE);
            return null;
        }

        // Return the file
        return file;
    }

    /**
     * Creates a new file chooser
     * @param startingFolder The folder to start in when browsing
     * @return The JFileChooser requested
     */
    protected JFileChooser createGnuCashChooser(File startingFolder)
    {
        // Create a file chooser
        JFileChooser chooser = new JFileChooser(startingFolder);

        // Add the Accept-All File Filter
        chooser.setAcceptAllFileFilterUsed(true);

        // Add the GnuCash File Filter
        FileFilter gnucashFileType = new FileFilter() {
            public boolean accept(File f)
            {
                // Accept directories while excluding OS X packages
                if (f.isDirectory()) {
                    if (new File(f, "Contents/PkgInfo").exists())
                        return false;
                    else
                        return true;
                }
                
                // See if the file is a GnuCash XML file
                FileInputStream fis = null;
                BufferedReader reader = null;
                try {
                    fis = new FileInputStream(f);
                    byte data[] = new byte[5];
                    if (fis.read(data) == 5 && data[0] == '<' && data[1] == '?' && data[2] == 'x' && data[3] == 'm' && data[4] == 'l')
                    {
                        // This is probably an XML file, so get the second line and compare
                        reader = new BufferedReader(new InputStreamReader(fis));
                        reader.readLine();
                        if (reader.readLine().startsWith("<gnc-v2")) {
                            reader.close();
                            return true;
                        }
                    }
                } catch (IOException e) { }

                // Close the appropriate stream
                try {
                    if (reader != null)
                        reader.close();
                    else if (fis != null)
                        fis.close();
                } catch (IOException e) { }
                
                // We're not a GnuCash file
                return false;
            }
            public String getDescription() {
                return "GnuCash XML File";
            }
        };

        // Add the file extensions with the default set to the compressed version
        chooser.addChoosableFileFilter(gnucashFileType);
        chooser.setFileFilter(gnucashFileType);

        // Return this file chooser
        return chooser;
    }

    /**
     * Creates a new folder chooser
     * @param startingFolder The folder to start in when browsing
     * @return The JFileChooser requested
     */
    protected JFileChooser createOutputChooser(File startingFolder)
    {
        // Create a file chooser using the starting folder
        JFileChooser chooser = new JFileChooser(startingFolder);

        // Ensure that Accept-All is in the list
        chooser.setAcceptAllFileFilterUsed(true);
        
        // Add the QIF file extensions and set the default format as the default
        chooser.addChoosableFileFilter(iifFileType.getFileFilter());
        chooser.addChoosableFileFilter(qifFile2004Type.getFileFilter());
        chooser.addChoosableFileFilter(qifFile2003Type.getFileFilter());
        chooser.setFileFilter(outputFileFormat.getFileFilter());
        
        // Set the accessory to our option panel after setting its configuration
        OptionPanel config = new OptionPanel();
        config.setUnusedAccountPruningEnabled(conversion.pruneUnusedAccounts);
        config.setMemoSplittingEnabled(conversion.splitMemoFromDescription);
        chooser.setAccessory(config);

        // Return this file chooser
        return chooser;
    }
    
    /**
     * Writes out the QIF file to the given File object
     * @param destFile The File to write the QIF data to
     * @return True if the file was written; false if it failed
     */
    protected boolean writeDestinationFile(File destFile)
    {
        // Go through every account, writing out the data for those with
        // at least one transaction
        boolean failure = false;
        try
        {
            // Create the output file
            Writer writer;
            if (destFile.getName().equals("-"))
                writer = new OutputStreamWriter(System.out);
            else
                writer = new FileWriter(destFile);
            
            // Write out the proper format, defaulting to 2004 if All Files was selected
            outputFileFormat.export(conversion, writer);

            // Close this file or, if stdout, flush the buffers
            if (writer instanceof FileWriter)
                writer.close();
            else
                writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                null,
                "Error while writing the output file " + destFile.getPath() + "; check stderr",
                "Exporting File",
                JOptionPane.ERROR_MESSAGE);
            failure = true;
        } catch (RuntimeException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                null,
                "A problem came up while exporting the file: " + e.getMessage() + "; check stderr",
                "Exporting File",
                JOptionPane.ERROR_MESSAGE);
            failure = true;
        }

        // Return whether or not all files were written successfully
        return !failure;
    }

    
    /**
     * Returns a String with the first letter of the input String capitalized
     * @param input The String to process
     * @return The String representation of the input String with the first
     * letter capitalized
     */
    protected static String capitalizeFirstLetter(String input) {
        int length = input.length();
        if (length == 0)
            return input;
        else if (length == 1)
            return input.toUpperCase();
        else {
            char str[] = input.toCharArray();
            int first = str[0];
            if (first >= 'a' && first <= 'z')
                first = 'A' + (first - 'a');
            str[0] = (char) first;
            return new String(str);
        }
    }
    
    /** The QIF format of the destination file */
    protected OutputFormat outputFileFormat = qifFile2004Type;
    
    /** The QIF file <= 2003 format */
    protected static OutputFormat qifFile2003Type = new QIF2003OutputFormat();

    /** The QIF file >= 2004 format */
    protected static OutputFormat qifFile2004Type = new QIF2004OutputFormat();
    
    /** The IFF file format */
    protected static OutputFormat iifFileType = new IIFOutputFormat();
    
    /** Our conversion configuration */
    protected Conversion conversion = new Conversion();
}
