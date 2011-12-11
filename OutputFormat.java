package net.sourceforge.gnucashtoqif;

import java.io.IOException;
import java.io.Writer;

import javax.swing.filechooser.FileFilter;

/**
 * Interface that output file formats implement
 */
public interface OutputFormat
{
	/**
	 * Write out a file format to the given writer using the input account Map
	 * @param conversion The conversion configuration to use
	 * @param writer The writer to output the converted data to
	 */
    public void export(Conversion conversion, Writer writer) throws IOException;
    
    /**
     * Returns this output format's FileFilter
     * @return This output format's FileFilter
     */
    public FileFilter getFileFilter();
}
