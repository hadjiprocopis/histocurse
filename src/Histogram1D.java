package	ahp.org.Histograms;

import	java.util.Random;
import  java.io.FileNotFoundException;
import  java.io.PrintWriter;
import	java.util.Arrays;
import	java.util.Collections;
import	java.util.Hashtable;
import	java.util.Comparator;

public	class	Histogram1D {
	private	Histobin1D[]	myBins;
	private	double		myBinWidth;
	private	int		myNumBins;
	private	int		myNumTotalBins;
	private	Hashtable<String, Histobin1D>	myLabels2Bins;

       // discreet cumulative density function
	// normalises the bins to Sum(counts) = 1 and then 
	// adds the counts
	// this is just a reference to the bins in myBins array
	private Histobin1D[]    myCumBins;
	// an index to the cumbins cum_count. As cumbins maybe a huge array (at least in higher dimensions)
	// this index will return the index in the cumbins array of the bin where the
	// search for specific cum_count value must start
	// the input should be an integer between 0-9 denoting cum_count value of 0.0 to 0.9
	// and the output will be an integer index - you should start search the cumbins array
	// from that index and below.
	private int[]    myCumBin_cumcount_index;
	// this is the size of the index. Use 1 for 0-9, 2 for 0-99, 3 for 0-999 etc.
	private final int       myCumBin_cumcount_index_num_digits_size = 2;
	private final int       myCumBin_cumcount_index_size = (int )(Math.pow(10, myCumBin_cumcount_index_num_digits_size));

	// a flag which is set every time a value is added or removed
	// it indicates that the bin's predictor must be re-calculated.
	private boolean	myNeedRecalculate;

	// the leftmost and rightmost bin coordinates along each histogram dimension (this case is 1D)
	// to define the range of the inputs to the total bins
	private double		myBinsLeftXBoundary,
				myBinsRightXBoundary;

	// comparator to sort cumbins array wrt to the cum_count content
	private Histobin1D_Comparator myCumBinsComparator = new Histobin1D_Comparator();


	public	Histogram1D	clone() {
		Histogram1D ahist = null;
		try {
			ahist = new Histogram1D(
				this.myNumBins,
				this.myBinWidth,
				this.myBinsLeftXBoundary
			);
		} catch(Exception ex){ System.err.println("Histogram1D.java : clone() : exception was caught while creating cloned histogram."); return(null); }
		int	i;
		for(i=this.myNumBins;i-->0;){
			this.myBins[i].copy(ahist.bin(i));
		}
		ahist.recalculate_predictors(true);
		return ahist;
	}

	public	Histobin1D	bin(int i){ return this.myBins[i]; }
	public	void	reset(){
		for(int i=this.myNumBins;i-->0;){
			this.myBins[i].reset();
		}
		this.myNeedRecalculate = true;
	}
	public	Histogram1D(
		int	num_bins,
		double	bin_width,
		double	start_from // bins' leftmost boundary (not center)
	) throws Exception {
		System.out.println("Histogram1D() : creating ...");
		if( ! this._setup_bins(num_bins, bin_width, start_from) ){
			System.err.println("Histogram1D.java : constructor1 : call to _setup_bins() has failed.");
			throw new Exception("Histogram1D.java : constructor1 : call to _setup_bins() has failed.");
		}
		if( ! this._setup_labels(null) ){
			System.err.println("Histogram1D.java : constructor1 : call to _setup_labels() has failed.");
			throw new Exception("Histogram1D.java : constructor1 : call to _setup_labels() has failed.");
		}
	}
	public	Histogram1D(
		int	num_bins,
		double	bin_width,
		double	start_from, // bins' leftmost boundary (not center)
		String[] labels // optional
	) throws Exception {
		if( ! this._setup_bins(num_bins, bin_width, start_from) ){
			System.err.println("Histogram1D.java : constructor2 : call to _setup_bins() has failed.");
			throw new Exception("Histogram1D.java : constructor2 : call to _setup_bins() has failed.");
		}
		if( ! this._setup_labels(labels) ){
			System.err.println("Histogram1D.java : constructor2 : call to _setup_labels() has failed.");
			throw new Exception("Histogram1D.java : constructor2 : call to _setup_labels() has failed.");
		}
	}

	private	void	_make_default_labels(){
		int	num_bins = this.myNumBins;
		// automatic labels
		for(int i=num_bins;i-->0;){ this.myBins[i].label = ""+(i+1); }
	}
	// this function must be called after bins have been setup (_setup())
	private	boolean	_setup_labels(String[] labels){
		int	num_bins = this.myNumBins, i;
		Histobin1D abin;
		if( labels == null ){
			// make default labels
			this._make_default_labels();
		} else {
			if( labels.length != num_bins ){
				System.err.println("Histogram1D.java: _setup() : WARNING : number of bins "+num_bins+" must be the same as the number of bin labels, "+labels.length+". Bin labels will be ignored.");
				return false;
			} else {
				for(i=num_bins;i-->0;){ this.myBins[i].label = new String(labels[i]); }
			}
		}
		// this is a hashtable mapping labels to bin
		// used for adding to a bin usin a label instead of using bin number
		this.myLabels2Bins = new Hashtable<String, Histobin1D>();
		for(i=num_bins;i-->0;){
			abin = this.myCumBins[i];
			this.myLabels2Bins.put(abin.label, abin);
		}
		if( this.myLabels2Bins.size() != num_bins ){
			System.err.println("Histogram1D.java : _setup_labels() : labels must be unique! but this is not the case with specified labels: "+labels);
			return false;
		}
		return true;
	}
	private	boolean	_setup_bins(
		int	num_bins,
		double	bin_width,
		// this is not where the first bincentre is!
		// this is where the left boundary of the first bin is:
		double	start_from
	){
		this.myNumBins = num_bins;
		this.myBins = new Histobin1D[num_bins];
		this.myCumBins = new Histobin1D[num_bins];
		this.myCumBin_cumcount_index = new int[this.myCumBin_cumcount_index_size];

		this.myBinsLeftXBoundary = start_from;
		this.myBinsRightXBoundary = start_from + num_bins * bin_width;
		this.myBinWidth = bin_width;
		double lb = start_from;
		double rb = lb + bin_width;
		for(int i=0;i<num_bins;i++){
			this.myBins[i] = new Histobin1D(i, 0, lb, rb);
			lb += bin_width;
			rb += bin_width;
			this.myCumBins[i] = this.myBins[i];
		}
		this.myNeedRecalculate = false;

		return true;
	}
	// given a value on the x-axis, find which bin it belongs to
	private	int	get_bin_index(double x){
		return (int )(Math.floor((x-this.myBinsLeftXBoundary)/this.myBinWidth));
	}
	// given a value on the x-axis, find which bin it belongs to and return bin object
	private	Histobin1D	get_bin(double x){
		return this.myBins[this.get_bin_index(x)];
	}
	// given a label, return the binobj with that label
	private	Histobin1D	get_bin(String alabel) throws Exception {
		if( this.myLabels2Bins.containsKey(alabel) == false ){
			throw new Exception("Histogram1D.java : get_bin() : label '"+alabel+"' was not found in the bins.");
		}
		return this.myLabels2Bins.get(alabel);
	}
	// add a value to the corresponding bin
	public	void	add(double x){
		//System.out.println("adding "+x+" at bin "+this.get_bin_index(x));
		this.myBins[this.get_bin_index(x)].count++;
		this.myNeedRecalculate = true;
	}
	public	void	remove(double x){
		int	ind = this.get_bin_index(x);
		if( this.myBins[ind].count == 0 ){ return; }
		this.myBins[ind].count--;
		this.myNeedRecalculate = true;
	}
	// add a value to the corresponding bin
	public	boolean	add(String alabel) {
		try {
			this.get_bin(alabel).count++;
			this.myNeedRecalculate = true;
			return true;
		} catch(Exception ex){
			System.err.println("Histogram1D.java : add() : Exception was caught while calling get_bin() with label '"+alabel+"':\n"+ex);
			ex.printStackTrace();
			return false;
		}
	}
	public	boolean	remove(String alabel){
		try {
			Histobin1D abin = this.get_bin(alabel);
			if( abin.count == 0 ){ return true; }
			abin.count--;
			this.myNeedRecalculate = true;
			return true;
		} catch(Exception ex){
			System.err.println("Histogram1D.java : remove() : Exception was caught while calling get_bin() with label '"+alabel+"':\n"+ex);
			ex.printStackTrace();
			return false;
		}
	}
	public	boolean	add(char alabel){
		try {
			this.get_bin(String.valueOf(alabel)).count++;
			this.myNeedRecalculate = true;
			return true;
		} catch(Exception ex){
			System.err.println("Histogram1D.java : add() : Exception was caught while calling get_bin() with label '"+String.valueOf(alabel)+"':\n"+ex);
			ex.printStackTrace();
			return false;
		}
	}
	public	boolean	remove(char alabel){
		try {
			Histobin1D abin = this.get_bin(String.valueOf(alabel));
			if( abin.count == 0 ){ return true; }
			abin.count--;
			this.myNeedRecalculate = true;
			return true;
		} catch(Exception ex){
			System.err.println("Histogram1D.java : add() : Exception was caught while calling get_bin() with label '"+String.valueOf(alabel)+"':\n"+ex);
			ex.printStackTrace();
			return false;
		}
	}
	public  double  random_value(Random arng){
		Histobin1D abin = this.random_bin(arng);
		double  binboundaryLeft = abin.left_boundary;

		// now get a random value between the random bin's range
		// basically we need a random value from
		// bin-boundary-left to bin-boundary-right (which is boundary-left+width)
		return binboundaryLeft
			+ this.myBinWidth * arng.nextDouble();
	}
	// returns a random bin index based on bins contents, i.e. most
	// popular bins will have more chance of being selected
	// this is basically an attempt to re-create the distribution
	public  int     random_bin_index(Random arng){  
		this.recalculate_predictors(false);
		// RNG nextDouble : must be between 0 and 1:
		double anum = arng.nextDouble();
		Histobin1D abin = this.random_bin(arng);
		return abin.i;
	}
	public  double  bins_left_boundary(){ return this.myBinsLeftXBoundary; }
	public  double  bins_right_boundary(){ return this.myBinsRightXBoundary; }
	public  int     num_bins(){ return this.myNumBins; }
	public	Histobin1D	random_bin(Random arng){
		boolean debug = false;
		this.recalculate_predictors(false);

		// RNG nextDouble : must be between 0 and 1:
		double anum = arng.nextDouble();

		int     ci = (int )(anum * this.myCumBin_cumcount_index_size),
			civ = this.myCumBin_cumcount_index[ci], civ_final, i;
		// civ tells us that the cumcount we are looking for is BELOW (i.e. it stops at civ)
		if( debug ) System.out.println("Indices (are start-0) ci="+ci+" and civ="+civ);
		// indices in the index are start-0, so inclusive:
		for(i=civ;i-->0;){
			if( debug ) System.out.println("Searching for "+anum+" >= "+this.myCumBins[i].cum_count+" at bin="+i+", civ="+civ);
			if( anum >= this.myCumBins[i].cum_count ){
				// now we are in here one too-many
				civ_final = i+1;
				if( debug ) System.out.println("search : for anum="+anum+" found bin="+civ_final+" (starts from 0), cum_count="+this.myCumBins[civ_final].cum_count+". Anum must be greater than the next bin down: "+this.myCumBins[((civ_final-1)<=0?0:(civ_final-1))].cum_count);
				return this.myCumBins[civ_final];
			}
		}
		return this.myCumBins[0];
	}
	public  String  toString(){
		StringBuilder   buffer = new StringBuilder();
		int     N = this.myNumBins, i;
		Histobin1D abin;
		this.recalculate_predictors(false);
		for(i=0;i<N;i++){
			abin = this.myBins[i];
			buffer.append(abin.toString()+"\n");
		}
		return buffer.toString();
	}
	public  void save_to_file_txt(String afilename) throws Exception { this.save_to_file_txt("\t"); }
	public  void save_to_file_txt(String afilename, String sep) throws Exception {
		this.recalculate_predictors(false);

		PrintWriter out = new PrintWriter(afilename);
		int     N = this.myNumBins, i;
		Histobin1D abin;
		for(i=0;i<N;i++){
			abin = this.myBins[i];
			out.println(abin.toString_as_histoplot(sep));
		}
		out.close();
	}
	public  void dump_random_values_to_file(String afilename, int num_values) throws Exception {
		this.recalculate_predictors(false);

		PrintWriter out =new PrintWriter(afilename);
		Random arng = new Random();
		double arv;
		for(int i=num_values;i-->0;){
			arv = this.random_value(arng);
			out.println(arv);
		}
		out.close();
	}
	// will construct the discreet cumulative density function
	// which will be used to output a random value based on the distribution
	// represented by this histogram. Every time a bin changes, this
	// needs to be recalculated
	// but it is better to have a flag and whenever a prediction is required
	// then this function maybe called.
	private void    recalculate_predictors(boolean force){
		boolean debug = false;
		if( (this.myNeedRecalculate == false) && (force == false) ){ return; }
		System.out.println("I am recalculating predictors...");
		double  sum = 0.0;
		int     N = this.myNumBins, i, j;
		for(i=N;i-->0;){ sum += this.myBins[i].count; }

		Histobin1D abin = this.myCumBins[0];
		abin.norm_count = abin.count / sum;
		abin.cum_count = abin.norm_count;
		for(i=1;i<N;i++){
			abin = this.myCumBins[i];
			abin.norm_count = abin.count / sum;
			abin.cum_count = this.myCumBins[i-1].cum_count + abin.norm_count;
		}
		// now build an index of cum_count values for the cumbins array
		// so that given a cumcount value we get an index in cumbins array
		// and search only from that index downwards.
		// indices are start-0
		int     ci, oldci = 0;
		for(i=0;i<N;i++){
			abin = this.myCumBins[i];
			if( debug ) System.out.println("cumcount: "+i+" = "+abin.cum_count);
			// get the first digits
			ci = (int )Math.floor(abin.cum_count * this.myCumBin_cumcount_index_size);
			if( ci > oldci ){
				for(j=oldci;j<ci;j++){ this.myCumBin_cumcount_index[j] = i; }
				oldci = ci;
			}
		}
		this.myCumBin_cumcount_index[this.myCumBin_cumcount_index_size-1] = N-1;

		if( debug ){ System.out.println("Cumbins Index (1st decimal digit => ends at bin index):"); for(i=0;i<this.myCumBin_cumcount_index_size;i++){ System.out.println(i+" = "+this.myCumBin_cumcount_index[i]); } }

		//Arrays.sort(this.myCumBins, this.myCumBinsComparator);
		this.myNeedRecalculate = false;
	}
}
class Histobin1D_Comparator implements Comparator<Histobin1D> {
	public int compare(Histobin1D o1, Histobin1D o2) {
		return o1.compareTo(o2);
	}
}
