package ahp.org.Histograms;

import	java.lang.Class;
import	java.lang.reflect.*;
import	java.util.Random;
import	java.io.Serializable;
import	java.io.FileNotFoundException;
import	java.io.PrintWriter;
import	java.io.FileInputStream;
import	java.io.FileOutputStream;
import	java.io.ObjectInputStream;
import	java.io.ObjectOutputStream;
import	java.util.Comparator;
// so not thread-safe:
import	java.util.ArrayList;
import	java.util.HashMap;
import	java.util.List;
import	java.util.Map;
import	java.util.Arrays;
import	java.util.Iterator;

// a 1D plain histogram with a fixed-size array of doubles, increment, get bin contents, that's all
public	class	HistogramVanilla implements Serializable {
	private static final long serialVersionUID = 1797271487L+23L;

	private	String		myName;
	private	int		myBins[];
	private int		myNumBins = -1;
	private	double		myBinWidth = -1.0;

	// the boundaries of the whole histogram (not centres) over each dimension
	public	double	myBoundariesFrom = -1.0;
	public	double	myBoundariesTo = -1.0;

	public	HistogramVanilla(
		// give a name to this histogram just for reference, nothing important
		String	ahistogram_name,
		int	anum_bins, // num of bins
		double	abin_width,
		double	aboundaries_start // left-most boundary
	) throws Exception {
		this.name(ahistogram_name);
		this.alloc(anum_bins);
		this.boundaries_from(aboundaries_start);
		this.bin_width(abin_width);
	}
	public	HistogramVanilla(
		// give a name to this histogram just for reference, nothing important
		String	histogram_name,
		int	num_bins // num of bins
		// required boundaries from and bin width can be set later
	) throws Exception {
		this.name(histogram_name);
		this.alloc(num_bins);
	}
	public	static void save_to_file_object(
		String afilename,
		HistogramVanilla ahist
	) throws Exception {
		FileOutputStream fout = new FileOutputStream(afilename);
		ObjectOutputStream oos = new ObjectOutputStream(fout);
		oos.writeObject(ahist);
		oos.close();
		fout.close();
	}
	public	static HistogramVanilla load_from_file_object(
		String afilename
	) throws Exception {
		FileInputStream fis = new FileInputStream(afilename);
		ObjectInputStream ois = new ObjectInputStream(fis);
		HistogramVanilla result = (HistogramVanilla )ois.readObject();
		ois.close();
		fis.close();
		return result;
	}
	protected	void	alloc(int num_bins){
		this.myBins = new int[num_bins];
		this.myNumBins = num_bins;
	}
	// GET the bin with specified coordinates
	public	int	get_bin_count(int coordinate){ return this.myBins[coordinate]; }
	public	void	increment_bin_count(int coordinate){
		this.myBins[coordinate]++;
	}
	public	void	increment_bin_count(int coordinate, int by_amount){
		this.myBins[coordinate]+=by_amount;
	}
	public	void	decrement_bin_count(int coordinate){
		if( this.myBins[coordinate]==0 ){ return; }
		this.myBins[coordinate]--;
	}
	public	void	decrement_bin_count(int coordinate, int by_amount){
		if( this.myBins[coordinate]<=by_amount ){
			this.myBins[coordinate] = 0;
		} else { this.myBins[coordinate] -= by_amount; }
	}
	public	void	increment_bin_count(double avalue){
		this.increment_bin_count(this.binindex_from_value(avalue));
	}
	public	void	increment_bin_count(double avalue, int by_amount){
		this.increment_bin_count(this.binindex_from_value(avalue), by_amount);
	}
	public	void	decrement_bin_count(double avalue){
		this.decrement_bin_count(this.binindex_from_value(avalue));
	}
	public	void	decrement_bin_count(double avalue, int by_amount){
		this.decrement_bin_count(this.binindex_from_value(avalue), by_amount);
	}
	public	int	binindex_from_value(double value){
//		int idx = (int )((value-this.myBoundariesFrom)/this.myBinWidth);
		return (int )((value-this.myBoundariesFrom)/this.myBinWidth);
	}
	public	void	reset(){
		for(int i=this.myNumBins;i-->0;){ this.myBins[i] = 0; }
	}
	// getters and shit
	public	double	bin_width(){ return this.myBinWidth; }
	public	void	bin_width(double b){ this.myBinWidth = b; }
	public	double	boundaries_from(){ return this.myBoundariesFrom; }
	public	void	boundaries_from(double b){ this.myBoundariesFrom = b; }
	public	double	boundaries_to(){ return this.myBoundariesTo; }
	public	int	num_bins(){ return this.myNumBins; }
	public	String	name(){ return this.myName; }
	public	void	name(String n){ this.myName = n; }
	public	int[] bins(){ return this.myBins; }

	public	void	toFileInfo(String afilename) throws Exception {
		PrintWriter pw = new PrintWriter(afilename);
		pw.println(this.toStringInfo());
		pw.close();
	}
	public	String	toStringInfo(){
		StringBuilder	buffer = new StringBuilder();
		buffer.append("Histogram '"+this.name()+"':\n");
		buffer.append("num bins  : "+this.myNumBins+"\n");
		buffer.append("bin width : "+this.myBinWidth+"\n");
		buffer.append("bou-from  : "+this.myBoundariesFrom+"\n");

		return buffer.toString();
	}

	public	String	toString(){
		StringBuilder	buffer = new StringBuilder();

		buffer.append(this.toStringInfo());
		buffer.append("\nboundaries // count\n");
		double bound = this.myBoundariesFrom;
		for(int i=0;i<this.myNumBins;i++){
			buffer.append("[");
			buffer.append(bound);
			buffer.append(",");
			bound += this.myBinWidth;
			buffer.append(bound);
			buffer.append("] ");
			buffer.append(this.myBins[i]);
			buffer.append("\n");
		}
		return buffer.toString();
	}
	public	void save_to_file_txt(String afilename) throws Exception { this.save_to_file_txt(afilename, "\t"); }
	public	void save_to_file_txt(String afilename, String sep) throws Exception {
		PrintWriter out = new PrintWriter(afilename);
		out.print(this.toString());
		out.close();
	}
}

