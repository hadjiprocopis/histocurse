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

import	ahp.org.Containers.*;
import	ahp.org.Cartesians.*;
import	ahp.org.Statistics.*;

// the fuking generics T is the CONTAINER where we store our Histobin objs
// e.g. new Histogram<DenseArray<Histobin>>
// e.g. new Histogram<SparseArray<Histobin>>
public	class	Histogram< T extends ahp.org.Containers.Container<Histobin> > implements Serializable {
	private static final long serialVersionUID = 1797271487L+3L;

	// java shit:
	Class<? extends T>	myTClass;

	private	String		myName; // a name for this histogram for various printing
	private	int		myNumDims;
	private	int[]		myNumBinsPerDimension;
	private	T		myBins;
	private	double[]	myBinWidths;

	// labels for each bin across each dimension
	// myBinDimLabels[adim][a_bin_coordinate_at_that_adim]
	private	String[][]	myBinCoordinates2Labels;

	// find the bin index at specified dimension given label at this dimension
	// the array is over all dimensions
	private	ArrayList<HashMap<String,Integer>>	myLabels2BinCoordinates;
	// results of 'labels2bin_coordinates()' use this pre-allocated array
	private	int	_my_labels2bin_coordinates_return[] = null;

	// the boundaries of the whole histogram (not centres) over each dimension
	public	double	myBoundariesFrom[];
	public	double	myBoundariesTo[];

	// a flag which is set every time a value is added or removed
	// it indicates that the bin's predictor must be re-calculated.
	private	boolean		myNeedRecalculate;

	// keep a total of all the bins' count
	private	double		myTotalBinCount = 0.0;
	// number of bins which contain at least 1 count... (in sparse we have problem delete bins)
	private	int		myNumNonemptyBins = 0;
	// Sparseness is the ratio of zero-count bins over total bins (0-1)
	private	double		mySparseness = 0.0;
	// Entropy of histo: it will be calculated only if asked and then
	// set here, if no recalculate is necessary then this will be returned
	private	double		myEntropy = -1.0;
	// this get statistics of how many bins are matched with
	// a bin selector whose last dim (only) is '*'
	// this is a measure to see the effectiveness of the histo
	// as a predictor. If many choices then you are fucked - everything is pretty much random
	// if 1,2 choices then you are fine as a predicting oracunt
	private	StatisticsContainer	myLastDimCountStatistics[] = null;

	// this index will return the index in the bins array of the bin where the
	// search for specific cum_count value must start
	// the input should be an integer between 0-9 denoting cum_count value of 0.0 to 0.9
	// and the output will be an integer index - you should start search the bins array
	// from that index and below.
	private int		myBin_cumcount_index[];
	// this is the size of the index. Use 1 for 0-9, 2 for 0-99, 3 for 0-999 etc.
	// now this of course does not make sense when the number of bins is less than 10^myBin_cumcount_index_num_digits_size
	// and that we will not know when we use sparsearrays. So, just leave it as is
	// basically a histogram with less than 10^myBin_cumcount_index_num_digits_size bins
	// will see no benefit from this (it will be slower ok)
	private final int	myBin_cumcount_index_num_digits_size = 2;
	private final int	myBin_cumcount_index_size = (int )(Math.pow(10, myBin_cumcount_index_num_digits_size));

	// values2coordinates() calculates the coordinates of the bin
	// which should receive the specified 'values' (an array of double).
	// The 'coordinates' of the bin is a int[]. How do we pass this data?
	// it has to allocate that int[] and return it.
	// Also it can use a pre-allocated (for each 'Histogram' object)
	// array of int[num_dims] and use that to pass the calculate coordinates.
	// the caller must read this pre-allocated array for the results.
	// this is pre-allocated
	private	int	_my_values2coordinates_return[];

	private static final double log2baseE = Math.log(2);

/* EXample of how to iterate over all bins:
			CartesianProductIteratorCompressed CPI = new CartesianProductIteratorCompressed(
				this.myNumBinsPerDimension
			);
			while( CPI.hasNext() ){
				acoordinates = CPI.next();
			}
*/

	public	static void save_to_file_object(
		String afilename,
		Histogram ahist
	) throws Exception {
		FileOutputStream fout = new FileOutputStream(afilename);
		ObjectOutputStream oos = new ObjectOutputStream(fout);
		oos.writeObject(ahist);
		oos.close();
		fout.close();
	}
	public	static Histogram load_from_file_object(
		String afilename
	) throws Exception {
		FileInputStream fis = new FileInputStream(afilename);
		ObjectInputStream ois = new ObjectInputStream(fis);
		Histogram result = (Histogram )ois.readObject();
		ois.close();
		fis.close();
		return result;
	}

	/* Constructor 1: No bin labels required.
	   Bin labels will be created as 'Default' meaning
	   that a label will be the String of the integer bin number.
	*/
	public	Histogram(
		// give a name to this histogram just for reference, nothing important
		String	histogram_name,
		int	num_bins[], // num of bins for each dimension
		double	bin_widths[], // for each dimension - bin widths can differ from one dim to another BUT within a dim, binsize is same.
		double	boundaries_start[], // left-most boundary in each dimension
		// Java shit - specify the class of the Container (e.g. SparseArray<Histobin> or DenseArray<Histobin>) etc.
		Class<? extends T>	tclazz
	) throws Exception {
		this.myTClass = tclazz;
		if( tclazz == null ){ throw new Exception("Class is required! because of the stupid idiots at Java."); }

		this.name(histogram_name);
		this.alloc(num_bins);
		this.init((String[][] )null, num_bins, bin_widths, boundaries_start);
	}

	/* Constructor 2: bin labels are given as a 2D array of String.
	   The first dimension denotes the dimension and the second dimension the bin number.
	   so labels[atDim][atBin].
	*/
	public	Histogram(
		// give a name to this histogram just for reference, nothing important
		String	histogram_name,
		// the label of each bin across each dimension
		// [dimension][labels]
		String[][] labels,
		int	num_bins[], // num of bins for each dimension
		double	bin_widths[], // for each dimension - bin widths can differ.
		double	boundaries_start[], // left-most boundary in each dimension
		// Java shit - specify the class of the Container (e.g. SparseArray<Histobin> or DenseArray<Histobin>) etc.
		Class<? extends T>	tclazz
	) throws Exception {
		this.myTClass = tclazz;
		if( tclazz == null ){ throw new Exception("Class is required! because of the stupid idiots at Java."); }

		this.name(histogram_name);
		this.alloc(num_bins);
		this.init(labels, num_bins, bin_widths, boundaries_start);
	}
	/* Constructor 3: bin labels are given as an ArrayList of Hashmaps.
	   Each Hashmap contains the label as KEY and the bin number as Value.
	   There is one Hashmap for each Dimension of the Histogram. The order
	   in the ArrayList is of increasing Dimension.
	*/
	public	Histogram(
		// give a name to this histogram just for reference, nothing important
		String	histogram_name,
		// the label of each bin across each dimension
		// [dimension] = HashMap<label, shit>
		// we need arraylist because shitty java will not allow
		// generic arrays
		ArrayList<HashMap<String,Integer>> binDimLabelsHashMaps,
		int	num_bins[], // num of bins for each dimension
		double	bin_widths[], // for each dimension - bin widths can differ.
		double	boundaries_start[], // left-most boundary in each dimension
		// Java shit - specify the class of the Container (e.g. SparseArray<Histobin> or DenseArray<Histobin>) etc.
		Class<? extends T>	tclazz
	) throws Exception {
		this.myTClass = tclazz;
		if( tclazz == null ){ throw new Exception("Class is required! because of the stupid idiots at Java."); }

		this.name(histogram_name);
		this.alloc(num_bins);
		this.init(binDimLabelsHashMaps, num_bins, bin_widths, boundaries_start);
	}
	/* Constructor 4: bin labels are assumed to be the SAME OVER EACH DIMENSION.
	   So, only one Hashmap is required to provide the label as KEY and the bin number as Value.
	   This Hashmap will be soft-copied (a fucking reference) to each Dimension of the Histogram.
	*/
	public	Histogram(
		// give a name to this histogram just for reference, nothing important
		String	histogram_name,
		// the label of each bin across each dimension
		// [dimension] = HashMap<label, shit>
		// we need arraylist because shitty java will not allow
		// generic arrays
		HashMap<String,Integer> binDimLabelsHashMap,
		int	num_bins[], // num of bins for each dimension
		double	bin_widths[], // for each dimension - bin widths can differ.
		double	boundaries_start[], // left-most boundary in each dimension
		// Java shit - specify the class of the Container (e.g. SparseArray<Histobin> or DenseArray<Histobin>) etc.
		Class<? extends T>	tclazz
	) throws Exception {
		this.myTClass = tclazz;
		if( tclazz == null ){ throw new Exception("Class is required! because of the stupid idiots at Java."); }

		this.name(histogram_name);
		this.alloc(num_bins);

		// make a soft-copy of the single Hashmap provided for all Dims and copy it to an ArrayList...
		ArrayList<HashMap<String,Integer>> crap = new ArrayList<HashMap<String,Integer>>(this.myNumDims);
		for(int i=0;i<this.myNumDims;i++){
			// now with this shitty container, if you want to overwrite at index
			// then USE 'set()' instead of 'add()'.
			crap.add(
				i,
				// same hashmapp over all dims - hopefully this will be a ref
				binDimLabelsHashMap
			);
		}
		this.init(crap, num_bins, bin_widths, boundaries_start);
	}
	/* End constructors */

	protected	void	alloc(int num_bins[]){
		this.myNumDims = num_bins.length;
		this.myBoundariesFrom = new double[this.myNumDims];
		this.myBoundariesTo = new double[this.myNumDims];
		this.myBinWidths = new double[this.myNumDims];
		this.myNumBinsPerDimension = new int[this.myNumDims];
		this._my_values2coordinates_return = new int[this.myNumDims];

		// this is how we construct a ahp.org.Container to contain our bins
		// this is a generics shit so we need to go through all this magic:
		try {
			this.myBins = this.construct_fucking_generics_object(
				"container of "+this.name(),
				num_bins
			);
		} catch(Exception ex){
			System.err.println("Histogram.java : "+this.myName+" : alloc() : internal error 12\n"+ex);
			ex.printStackTrace();
			System.exit(1);
		}

		int	i;
		this.myBinCoordinates2Labels = new String[this.myNumDims][];
		for(i=this.myNumDims;i-->0;){
			this.myBinCoordinates2Labels[i] = new String[num_bins[i]];
		}

		// for each dimension a bin has a label
		// this hashmap is used to quickly find the bin index (at given dimension)
		// given a bin label (at given dimension)
		this.myLabels2BinCoordinates = new ArrayList<HashMap<String,Integer>>(this.myNumDims);
		for(i=0;i<this.myNumDims;i++){
			// this is added irrespective of having bins or not
			// if user supplies own then this will be overwritten
			this.myLabels2BinCoordinates.add(new HashMap<String,Integer>(num_bins[i]));
		}

		this._my_labels2bin_coordinates_return = new int[this.myNumDims]; // for method _my_labels2bin_coordinates_return
		this.myBin_cumcount_index = new int[this.myBin_cumcount_index_size];
		this.myLastDimCountStatistics = new StatisticsContainer[]{ new StatisticsContainer(), new StatisticsContainer() };
	}
	protected	void	init(
		// labels is a String[aDim][aBin] and tells us the label of of the specific
		// bin at specific dimension
		String[][]	labels, // if you want to use default labels, use the constructor with no labels - do not set labels to null
		int[]		num_bins, // num of bins for each dimension
		double[]	bin_widths, // for each dimension
		double[]	boundaries_start // left-most boundary in each dimension
	) throws Exception {
		if( (num_bins.length != bin_widths.length) || (num_bins.length != boundaries_start.length) ){ throw new Exception("Histogram.java : "+this.myName+" : init()/1 : the arrays 'labels', 'num_bins', 'bin_widths' and 'boundaries_start' must all have the same size on their first dimension: "+labels.length+", "+num_bins.length+", "+bin_widths.length+", "+boundaries_start.length); }
		if( labels != null ){
			if( (num_bins.length != labels.length) ){ throw new Exception("Histogram.java : "+this.myName+" : init()/1 : the arrays 'labels', 'num_bins', 'bin_widths' and 'boundaries_start' must all have the same size on their first dimension: "+labels.length+", "+num_bins.length+", "+bin_widths.length+", "+boundaries_start.length); }
		}

		// setup the histogram (boundaries, bin widths etc.)
		if( ! this._setup_histogram(num_bins, bin_widths, boundaries_start) ){
			System.err.println("Histogram.java : "+this.myName+" : init()/1 : '"+this.myName+"' : call to _setup_histogram() has failed.");
			throw new Exception("Histogram.java : "+this.myName+" : init()/1 : '"+this.myName+"' : call to _setup_histogram() has failed.");
		}

		// first setup the labels in our internal hashes
		if( this._setup_labels(labels) == false ){
			System.err.println("Histogram.java : "+this.myName+" : init()/1 : '"+this.myName+"' : call to _setup_labels() has failed.");
			throw new Exception("Histogram.java : "+this.myName+" : init()/1 : '"+this.myName+"' : call to _setup_labels() has failed.");
		}

		// then setup the bins, if we are flat we create them and set their labels
		if( this._setup_bins(num_bins, bin_widths, boundaries_start) == false ){
			System.err.println("Histogram.java : "+this.myName+" : init()/1 : '"+this.myName+"' : call to _setup_bins() has failed.");
			throw new Exception("Histogram.java : "+this.myName+" : init()/1 : call to _setup_bins() has failed.");
		}
	}
	protected	void	init(
		// the label of each bin across each dimension
		// [dimension] = HashMap<label, shit>
		ArrayList<HashMap<String,Integer>> binDimLabelsHashMaps,
		int[]		num_bins, // num of bins for each dimension
		double[]	bin_widths, // for each dimension
		double[]	boundaries_start // left-most boundary in each dimension
	) throws Exception {
		if( (num_bins.length != bin_widths.length) || (num_bins.length != boundaries_start.length) ){ throw new Exception("Histogram.java : "+this.myName+" : init()/2 : the arrays 'labels', 'num_bins', 'bin_widths' and 'boundaries_start' must all have the same size on their first dimension: "+num_bins.length+", "+bin_widths.length+", "+boundaries_start.length); }
		if( binDimLabelsHashMaps != null ){
			if( (num_bins.length != binDimLabelsHashMaps.size()) ){ throw new Exception("Histogram.java : "+this.myName+" : init()/1 : the arrays 'labels', 'num_bins', 'bin_widths' and 'boundaries_start' must all have the same size on their first dimension: "+binDimLabelsHashMaps.size()+", "+num_bins.length+", "+bin_widths.length+", "+boundaries_start.length); }
		}

		// setup the histogram (boundaries, bin widths etc.)
		if( ! this._setup_histogram(num_bins, bin_widths, boundaries_start) ){
			System.err.println("Histogram.java : "+this.myName+" : init()/2 : '"+this.myName+"' : call to _setup_histogram() has failed.");
			throw new Exception("Histogram.java : "+this.myName+" : init()/2 : '"+this.myName+"' : call to _setup_histogram() has failed.");
		}

		// first setup the labels in our internal hashes
		if( this._setup_labels(binDimLabelsHashMaps) == false ){
			System.err.println("Histogram.java : "+this.myName+" : init()/2 : '"+this.myName+"' : call to _setup_labels() has failed.");
			throw new Exception("Histogram.java : "+this.myName+" : init()/2 : '"+this.myName+"' : call to _setup_labels() has failed.");
		}

		// then setup the bins, if we are flat we create them and set their labels
		if( this._setup_bins(num_bins, bin_widths, boundaries_start) == false ){
			System.err.println("Histogram.java : "+this.myName+" : init()/2 : '"+this.myName+"' : call to _setup_bins() has failed.");
			throw new Exception("Histogram.java : "+this.myName+" : init()/2 : call to _setup_bins() has failed.");
		}
	}

	// this function must be called after bins have been setup (_setup())
	protected	boolean	_setup_labels(
		// labels is [dim][bin_number]
		// IF labels==null then we setup default labels
		String[][] labels
	) throws Exception {
		// String[][] labels were given, we need to setup this.myLabels2BinCoordinates
		//if( debug ){ System.out.println("Histogram.java : "+this.myName+" : _setup_labels() : starting for "+this.myNumDims+" dimensions ..."); }
		long	nanotime_started = System.nanoTime();

		int	num_bins = this.myBins.capacity(), i, j;
		Histobin abin;
		HashMap<String,Integer> ahash;

		boolean	debug = false;

		// a label of a bin is a String[], over all dimensions:
		String	internal_labels_ref[];
		if( labels == null ){
			// setup our default labels which is the bin index per dimension (start-zero)
			for(i=this.myNumDims;i-->0;){
				internal_labels_ref = this.myBinCoordinates2Labels[i];
				ahash = this.myLabels2BinCoordinates.get(i);
				for(j=this.myNumBinsPerDimension[i];j-->0;){
					String Jstring = Integer.toString(j);
					ahash.put(
						Jstring,
						new Integer(j)
					);
					// update myBinCoordinates2Labels[i][j]:
					internal_labels_ref[j] = Jstring;
				}
			}
		} else {
			// labels array have been specified
			// check label sizes etc.
			if( labels.length != this.myNumDims ){
				System.err.println("Histogram.java: _setup() : '"+this.myName+"' : number of dimensions "+this.myNumDims+" must be the same as the number of labels, "+labels.length+". Can not continue.");
				return false;
			}
			for(i=this.myNumDims;i-->0;){
				if( labels[i].length != this.myNumBinsPerDimension[i] ){
					System.err.println("Histogram.java: _setup_labels() : '"+this.myName+"' : number of bins in dimension "+i+"(="+this.myNumBinsPerDimension[i]+") must be the same as the number of bin labels["+i+"], "+labels[i].length+". Can not continue.");
					return false;
				}
			}
			// setup the labels now
			String a_supplied_label_ref[];
			for(i=this.myNumDims;i-->0;){
				// get the ith element for faster access ...
				internal_labels_ref = this.myBinCoordinates2Labels[i];
				ahash = this.myLabels2BinCoordinates.get(i);
				a_supplied_label_ref = labels[i];
				for(j=this.myNumBinsPerDimension[i];j-->0;){
					ahash.put(a_supplied_label_ref[j], j);
					internal_labels_ref[j] = a_supplied_label_ref[j];
				}
			}
		} // if( labels == null )

		if( debug ){
			System.out.println("Histogram.java : "+this.myName+" : _setup_labels() : '"+this.myName+"' : debug info, array of labels per dimension:");
			for(i=this.myNumDims;i-->0;){
				ahash = this.myLabels2BinCoordinates.get(i);
				System.out.print(i+" => [");
				for(Map.Entry entry : ahash.entrySet()){
					System.out.print(entry.getKey()+"=>"+entry.getValue()+", ");
				}
				System.out.println("]");
			}
		}
		//if( debug ){ System.out.println("Histogram.java : "+this.myName+" : _setup_labels() : '"+this.myName+"' : _setup_labels() : done. Time taken: "+((System.nanoTime()-nanotime_started)/1000000)+" milli seconds."); }
		return true;
	}
	// this function must be called after bins have been setup (_setup())
	protected	boolean	_setup_labels(
		// the label of each bin across each dimension
		// [dimension] = HashMap<label, shit>
		ArrayList<HashMap<String,Integer>> labelsHashmaps
	) throws Exception {
		// Labels were given as 'ArrayList<HashMap<String,Integer>>' (labelsHashmaps)
		// therefore we need to setup this.myBinCoordinates2Labels (String[][]) only

		long	nanotime_started = System.nanoTime();
		//if( debug ){ System.out.println("Histogram.java : "+this.myName+" : _setup_labels() : setting specified HashMaps of labelsHashmaps ..."); }
		HashMap<String,Integer>	ahash;
		int i, j;
		boolean debug = false;

		String	key, internal_labels_ref[];
		for(i=this.myNumDims;i-->0;){
			// from the arraylist get the hash for dim i
			ahash = labelsHashmaps.get(i);
			// NOTE the ArrayList.set() !!! using add() will expand the fucking list!
			this.myLabels2BinCoordinates.set(i, ahash);

			// get the ith element for faster access ...
			internal_labels_ref = this.myBinCoordinates2Labels[i];
			for( Map.Entry<String, Integer> entry : ahash.entrySet() ){
				key = entry.getKey();
				j = entry.getValue().intValue();
				internal_labels_ref[j] = key;
			}
		}
		if( debug ){
			System.out.println("Histogram.java : "+this.myName+" : _setup_labels() : '"+this.myName+"' : debug info, array of labels per dimension:");
			for(i=this.myNumDims;i-->0;){
				ahash = this.myLabels2BinCoordinates.get(i);
				System.out.print(i+" => [");
				for(Map.Entry entry : ahash.entrySet()){
					System.out.print(entry.getKey()+"=>"+entry.getValue()+", ");
				}
				System.out.println("]");
			}
		}
		// if( debug ) System.out.println("Histogram.java : "+this.myName+" : _setup_labels() : '"+this.myName+"' : _setup_labels() : done. Time taken: "+((System.nanoTime()-nanotime_started)/1000000)+" milli seconds.");
		return true;
	}
	protected boolean _setup_histogram(
		int[]		num_bins, // num of bins for each dimension
		double[]	bin_widths, // for each dimension
		double[]	boundaries_start // left-most boundary in each dimension
	){
		for(int i=0;i<this.myNumDims;i++){
			this.myNumBinsPerDimension[i] = num_bins[i];
			this.myBinWidths[i] = bin_widths[i];
			this.myBoundariesFrom[i] = boundaries_start[i];
			this.myBoundariesTo[i] = this.myBoundariesFrom[i]
						+ this.myNumBinsPerDimension[i] * this.myBinWidths[i];
		}
		return true;
	}

	// assumes that _setup_labels was called first and internal structures wrt labels have been setup already
	protected	boolean	_setup_bins(
		int[]		num_bins, // num of bins for each dimension
		double[]	bin_widths, // for each dimension
		double[]	boundaries_start // left-most boundary in each dimension
	){
		//if( debug ){ System.out.println("Histogram.java : "+this.myName+" : _setup_bins() : '"+this.myName+"' : starting for "+this.myNumDims+" dimensions ..."); }
		long	nanotime_started = System.nanoTime();
		int	I;

		if( this.myBins.sparse() == false ){
			// create all the bins if we are not sparse:
			double	aboundaries[][] = new double[this.myNumDims][2];
			int	acoordinates[] = new int[this.myNumDims];
			Histobin	abin;
			int num_total_bins = this.myBins.size();
			//if( debug ){ System.out.print("Histogram.java : "+this.myName+" : _setup_bins() : '"+this.myName+"' : creating "+num_total_bins+" bins :"); System.out.flush(); }
			for(I=this.myBins.capacity();I-->0;){
				// get the coordinates for linear index I (and return it to acoordinates)
				this.myBins.linear_index2coordinates(I, acoordinates);
				// the bin created will also have labels as specified by user
				abin = this.create_bin(acoordinates);
				this.myBins.put_with_linear_index(abin, I);
				abin.id(I); // histobin id can be set to I and be done with this headache
				//if( debug ) if( I % 50000 == 0 ){ System.out.print(" "+I+" of "+num_total_bins+", "); System.out.flush(); }
			}
			this.mySparseness = 1.0; // all our bins are zero-count, we are not sparse, sparseness is MAX
			//if( debug ) System.out.println("Done ALL!");
		}
		this.myTotalBinCount = 0.0;
		this.myNeedRecalculate = false;

		//if( debug ){ System.out.println("Histogram.java : "+this.myName+" : _setup_bins() : '"+this.myName+"' : done. Time taken: "+((System.nanoTime()-nanotime_started)/1000000)+" milli seconds."); }
		return true;
	}
	public	Histogram<T> clone(String new_histogram_name){
		Histogram<T> anotherhist = this.clone_only_structure(new_histogram_name);
		// now copy the bin's content - hard copy
		try {
			Iterator<Histobin> itera = this.myBins.iterator();
			Histobin abin, anotherbin;
			while(itera.hasNext()){
				abin = itera.next();
				if( (anotherbin=anotherhist.get_bin(abin.coordinates())) == null ){ throw new Exception("Histogram.java : "+this.myName+" : clone() : histograms are not the same! coordinate "+Arrays.toString(abin.coordinates())+" does not exist in 'other' histogram."); }
				abin.copy(anotherbin);
			}
		} catch(Exception ex){ System.err.println("Histogram.java : "+this.myName+" : clone() : '"+this.myName+"' : exception was caught while calling copy():\n"+ex); return(null); }
		// this is not necessary:
		anotherhist.recalculate(true);
		return anotherhist;
	}
	@SuppressWarnings("unchecked")
	public  Histogram<T> clone_only_structure(String new_histogram_name){
		Histogram<T> ahist = null;
		try {
			// the supress warning refers to this shit.
			ahist = new Histogram(
				new_histogram_name==null ? this.myName+" (cloned)" : new_histogram_name,
				this.myBinCoordinates2Labels,
				this.myNumBinsPerDimension,
				this.myBinWidths,
				this.myBoundariesFrom,
				this.myTClass
			);
		} catch(Exception ex){ System.err.println("Histogram.java : "+this.myName+" : clone_only_structure() : '"+this.myName+"' : exception was caught while creating cloned histogram."); return(null); }
		//this.myNeedRecalculate = true; // i dont think so
		return ahist;
	}
	// GET the bin with specified coordinates
	public	Histobin	get_bin(int acoordinates[]){ return this.myBins.get(acoordinates); }
	// GET the bin with specified linear index
	public	Histobin	get_bin_with_linear_index(int a_linear_index){
		return this.myBins.get_with_linear_index(a_linear_index);
	}
	// PUT the bin with specified coordinates to 'abin'
	public	void	put_bin(Histobin abin, int acoordinates[]){
		int alinear_index = this.myBins.put(abin, acoordinates);
		abin.id(alinear_index);
		//System.out.println("put_bin(coords) : added bin : "+abin);
	}
	public	void	put_bin(Histobin abin){
		int alinear_index = this.myBins.put(abin, abin.coordinates());
		abin.id(alinear_index);
		//System.out.println("put_bin() : added bin : "+abin);
	}
	// PUT the bin with specified linear index to 'abin'
	public	void	put_bin_with_linear_index(Histobin abin, int a_linear_index){
		this.myBins.put_with_linear_index(abin, a_linear_index);
		abin.id(a_linear_index);
		//System.out.println("put_bin(linear_index) : added bin : "+abin);
	}
	public	void	reset(){
		Iterator<Histobin> itera = this.myBins.iterator();
		Histobin abin;
		while(itera.hasNext()){
			abin = itera.next();
			abin.reset_counts();
		}
	}

	// will construct the discreet cumulative density function
	// which will be used to output a random value based on the distribution
	// represented by this histogram. Every time a bin changes, this
	// needs to be recalculated
	// but it is better to have a flag and whenever a prediction is required
	// then this function maybe called.
	protected	void	recalculate(boolean force){
		if( (this.myNeedRecalculate == false) && (force == false) ){ return; }
		// if size==0, we are sparse and nothing was inserted yet...
		if( this.myBins.size() == 0 ){ return; }
		long	time_started = System.nanoTime();
		//System.out.println("Histogram.java : "+this.myName+" : recalculate("+force+") : '"+this.myName+"' starting ...");
		boolean	debug = false;
		double	asum, acount;
		int	j, I;
		this.mySparseness = 0.0;
		Histobin abin;
		Iterator<Histobin> itera = this.myBins.iterator();
		double totalsum = 0.0;
		int N = 0;
		int num_nonempty_b = 0;
		while( itera.hasNext() ){
			abin = itera.next();
			acount = abin.count();
			totalsum += acount;
			if( acount == 0.0 ){ this.mySparseness++; }
			else { num_nonempty_b++; }
			N++;
		}
		this.mySparseness /= N;

		this.myTotalBinCount = totalsum;
		this.myNumNonemptyBins = num_nonempty_b;

		// basically what we do in order to get a random bin proportional to the histogram's PDF
		// is to put all the bins side-by-side their length representing their count
		// just like sticks of wood.
		// then throw a uniform-random stone on them and see where it lands.
		// most of the times will land on the most-counts stick of wood (or bin)
		// monte carlo.
		// but we have a bit of a problem in finding where the stone lands (in computers only)
		// so we put all the bins side-by-side and order is not important (e.g. a simple i-j loop over all bins)
		// this creates the bins array.
		// then in this array we sum the normalised counts (norm_count) to get the cumulative count
		// each bin with all its previous (on the left). Cum count finally arrives at 1.
		// and so get a uniform-random number (from 0 to 1), then start going
		// down the array of bins checking if that number is > than the current bin cum bin
		// and when it is then we have the bin we have landed to, and so then get its
		// value or a random value within its bounds.

		itera = this.myBins.iterator();
		double cumulative_count = 0.0,
			anormcount, entrop = 0.0;
		if( itera.hasNext() ){
			abin = itera.next();
			if( (acount=abin.count()) > 0.0 ){
				anormcount = acount / totalsum;
				cumulative_count += anormcount;
				entrop += anormcount * Math.log(anormcount);
				abin.normalisedCount(anormcount);
			} else {
				abin.normalisedCount(0.0);
			}
			abin.cumulativeCount(cumulative_count);
		}
		this.myEntropy = - entrop / Histogram.log2baseE;

		// now build an index of cum_count values for the bins array
		// so that given a cumcount value we get an index in bins array
		// and search only from that index downwards.
		// indices are start-0
		int	ci, oldci = 0;
		itera = this.myBins.iterator();
		while( itera.hasNext() ){
			abin = itera.next();
			I = abin.id(); // when we create a bin (in here) we set its ID to the linear index
			//if( debug ) System.out.println("cumcount: "+I+" = "+abin.cumulativeCount());
			// get the first digits
			ci = (int )Math.floor(abin.cumulativeCount() * this.myBin_cumcount_index_size);
			if( ci > oldci ){
				for(j=oldci;j<ci;j++){
					this.myBin_cumcount_index[j] = I;
					//if( debug ) System.out.println("adding bin index "+I+" to myBin_cumcount_index["+j+"]");
				}
				oldci = ci;
			}
		}
		this.myBin_cumcount_index[this.myBin_cumcount_index_size-1] = N-1;

		// do last dims statistics : this needs to be thought over because it might be heavy:
		this.count_num_choices_at_last_dims();

		/*if( debug ){
			for(I=0;I<this.myBin_cumcount_index_size;I++){ System.out.println(I+" = "+this.myBin_cumcount_index[I]); }
			System.out.println("Cumbins Index (1st decimal digit => ends at bin index) are shown above");
		}*/
		// and now sort the bins flat wrt to content (v)
		this.myNeedRecalculate = false;
		//if( debug ) System.out.println("Histogram.java : "+this.myName+" : recalculate("+force+") : '"+this.myName+"' done in "+((System.nanoTime()-time_started)/1E06)+" milli seconds.");
	}
	// given a value on the x-axis and one on the y-axis, find which bin it belongs to
	// returns the bin coordinates as a 3-array of int
	// SLOW (it has to allocate int[])
/*	protected	int[]	values2coordinates(double ... values){
		int ret[] = new int[this.myNumDims];
		for(int i=this.myNumDims;i-->0;){
			ret[i] = (int )(Math.floor((values[i]-this.myBoundariesFrom[i])/this.myBinWidths[i]));
		}
		return ret;
	}
*/	// same as above
	// but fills 'ret' reference so it's faster provided you allocate ret before calling
	// FASTER provided passed ret[] is allocated once
	protected	void	values2coordinates(int ret[], double ... values){
		for(int i=this.myNumDims;i-->0;){
			ret[i] = (int )(Math.floor((values[i]-this.myBoundariesFrom[i])/this.myBinWidths[i]));
		}
	}
	// same as above
	// but uses a pre-allocated array (_my_values2coordinates_return) to store the results
	// FASTEST (i think) : results are passed by internal array allocated once.
	protected	void	values2coordinates(double ... values){
		for(int i=this.myNumDims;i-->0;){
			this._my_values2coordinates_return[i] =
				(int )(Math.floor((values[i]-this.myBoundariesFrom[i])/this.myBinWidths[i]))
			;
		}
	}
	// given a value on the x-axis and one on the y-axis, find which bin it belongs to
	// and return that bin object
	public	Histobin	get_bin(double ... values){
		// this.values2coordinates() writes its results to this._my_values2coordinates_return[]
		// which is already pre-allocated:
		this.values2coordinates(values);
		return this.get_bin(this._my_values2coordinates_return);
	}
	// given a bin-label (a String array, one for each bin dimension),
	//it returns the bin object
	public	Histobin	get_bin(List<String> alabels) throws Exception {
		// NOTE: duplicate code with get_bin(String alabels[]) for efficiency
		// if you make changes make them also there
		int shit[];
		if( (shit=this.labels2bin_coordinates(alabels)) == null ){ throw new Exception("Histogram.java : "+this.myName+" : get_bin() : could not find label '"+alabels.toString()+"' ([] are not part of the label!) in any of the bins/1 - skipping just this one."); }
		return this.get_bin(shit);
	}
	public	Histobin	get_bin(String alabels[]) throws Exception {
		// NOTE: duplicate code with get_bin(List<String> alabels) for efficiency
		// if you make changes make them also there
		int shit[];
		if( (shit=this.labels2bin_coordinates(alabels)) == null ){ throw new Exception("Histogram.java : "+this.myName+" : get_bin() : could not find label '"+alabels.toString()+"' ([] are not part of the label!) in any of the bins/1 - skipping just this one."); }
		return this.get_bin(shit);
	}
	protected	int[]	labels2bin_coordinates(List<String> alabels){
		// NOTE: duplicate code with labels2bin_coordinates(String alabels[]) for efficiency
		// if you make changes make them also there
		Integer shit;
		for(int atDim=this.myNumDims;atDim-->0;){
			if( (shit=this.myLabels2BinCoordinates.get(atDim).get(alabels.get(atDim))) == null ){
//System.out.println("arDim="+atDim+" my this.myLabels2BinCoordinates.get(atDim: "+this.myLabels2BinCoordinates.get(atDim));
//yyy
 return null; }
			this._my_labels2bin_coordinates_return[atDim] = shit.intValue();
		}
		return this._my_labels2bin_coordinates_return;
	}
	protected	int[]	labels2bin_coordinates(String alabels[]){
		// NOTE: duplicate code with labels2bin_coordinates(List<String> alabels) for efficiency
		// if you make changes make them also there
		Integer shit;
		for(int atDim=this.myNumDims;atDim-->0;){
			if( (shit=this.myLabels2BinCoordinates.get(atDim).get(alabels[atDim])) == null ){
//System.out.println("arDim="+atDim+" my this.myLabels2BinCoordinates.get(atDim: "+this.myLabels2BinCoordinates.get(atDim));
//yyy
 return null; }
			this._my_labels2bin_coordinates_return[atDim] = shit.intValue();
		}
		return this._my_labels2bin_coordinates_return;
	}
	// given an array of bin-indices labels and wildcards, it returns the bin object(s)
	protected	Histobin[]	get_bins_by_wildcard(
		String bin_dim_indices_spec[] // 1 or * or 1:5 or 1,2,3
	) throws Exception {
		long time_started = System.nanoTime();
		Cartesian acartesian = new Cartesian(this.myNumBinsPerDimension);
		int	bin_indices[][] = acartesian.product(bin_dim_indices_spec);
		acartesian = null;

		int		L = bin_indices.length;
		Histobin	ret[] = new Histobin[L];
		for(int i=L;i-->0;){
			ret[i] = this.get_bin(bin_indices[i]);
		}
		return ret;
	}
	// INCREMENT a bin count
	// CREATE BIN IF BIN IS NOT THERE (in case of sparse container)
	public	int	increment_bin_count(double ... values){
		// if our bin-container (myBins) is sparse, then it maybe
		// the case that this bin is not yet inserted into the histogram
		// and must be inserted for the first time here...

		// so first, get the coordinates for this bin which may or may not exist there
		// this.values2coordinates() writes its results to this._my_values2coordinates_return[]
		// which is already pre-allocated (int[]):
		this.values2coordinates(values);

		// is the bin there (only in case of sparse container it will not be there)?
		Histobin abin = this.get_bin(this._my_values2coordinates_return);
		if( abin == null ){
			abin = this.create_bin(this._my_values2coordinates_return);
			this.put_bin(abin);
		}
		this.myNeedRecalculate = true;
		return (int )(abin.count_increment());
	}
	// add a value to the corresponding bin - increment its count
	// CREATE BIN IF BIN IS NOT THERE (in case of sparse container)
	public	int	increment_bin_count(String alabels[]){
		// NOTE: duplicate code with increment_bin_count(List<String> alabels) for efficiency
		// if you make changes make them also there
		try {
			// is the bin there (only in case of sparse container it will not be there)?
			Histobin abin = this.get_bin(alabels);
			if( abin == null ){
				abin = this.create_bin(alabels);
				this.put_bin(abin);
			}
			this.myNeedRecalculate = true;
			return (int )(abin.count_increment());
		} catch(Exception ex){
			System.err.print("Histogram.java : "+this.myName+" : increment_bin_count() : exception was caught when called get_bin() for label '"+Arrays.toString(alabels)+"':\n"+ex);
			ex.printStackTrace();
			return -1; // error return
		}
	}
	// add a value to the corresponding bin - increment its count
	// CREATE BIN IF BIN IS NOT THERE (in case of sparse container)
	public	int	increment_bin_count(List<String> alabels){
		// NOTE: duplicate code with increment_bin_count(String alabels[]) for efficiency
		// if you make changes make them also there
		try {
			// is the bin there (only in case of sparse container it will not be there)?
			Histobin abin = this.get_bin(alabels);
			if( abin == null ){
				abin = this.create_bin(alabels);
				this.put_bin(abin);
			}
			this.myNeedRecalculate = true;
			return (int )(abin.count_increment());
		} catch(Exception ex){
			System.err.print("Histogram.java : "+this.myName+" : increment_bin_count() : exception was caught when called get_bin() for label '"+Histogram.toString_List(alabels)+"':\n"+ex);
			ex.printStackTrace();
			return -1; // error return
		}
	}
	// decrement bin count but if bin does not exist (in sparse containers)
	// DONT CREATE IT (and have it there with a zero count)
	public	int	decrement_bin_count(double ... values){
		this.values2coordinates(values);
		Histobin abin = this.get_bin(this._my_values2coordinates_return);
		if( abin == null ){
			// bin has not yet been inserted (because we are sparse)
			return 0;
		}
		if( abin.is_count_zero() ){ return 0; }
		this.myNeedRecalculate = true;
		return (int )(abin.count_decrement());
	}
	public	int	decrement_bin_count(String alabels[]){
		// NOTE: duplicate code with decrement_bin_count(List<String> alabels) for efficiency
		// if you make changes make them also there
		try {
			Histobin abin = this.get_bin(alabels);
			//System.err.println("before decrement entropy: "+this.entropy());
			if( abin.is_count_zero() ){ return 0; }
			this.myNeedRecalculate = true;
			//abin.count_decrement();
			//System.err.println("after decrement "+this.entropy());
			return (int )(abin.count_decrement());
		} catch(Exception ex){
			System.err.print("Histogram.java : "+this.myName+" : decrement_bin_count() : exception was caught when called get_bin() for label '"+Arrays.toString(alabels)+"':\n"+ex);
			ex.printStackTrace();
			return -1; // error return
		}
	}
	public	int	decrement_bin_count(List<String> alabels){
		// NOTE: duplicate code with decrement_bin_count(String alabels[]) for efficiency
		// if you make changes make them also there
		try {
			Histobin abin = this.get_bin(alabels);
			if( abin.is_count_zero() ){ return 0; }
			this.myNeedRecalculate = true;
			return (int )(abin.count_decrement());
		} catch(Exception ex){
			System.err.print("Histogram.java : "+this.myName+" : decrement_bin_count() : exception was caught when called get_bin() for label '"+Histogram.toString_List(alabels)+"':\n"+ex);
			ex.printStackTrace();
			return -1; // error return
		}
	}
	// returns the bin coordinates of a random based on bins counts, i.e. most
	// popular bins will have more chance of being selected
	// this is basically an attempt to re-create the distribution
	public	int[]	random_bin_index(Random arng){
		Histobin abin = this.random_bin(arng);
		return abin.coordinates();
	}
	public	Histobin	random_bin(Random arng){
		this.recalculate(false);
		boolean	debug = false;
		// RNG nextDouble : must be between 0 and 1:
		double anum = arng.nextDouble();
		int	ci = (int )(anum * this.myBin_cumcount_index_size),
			civ = this.myBin_cumcount_index[ci], civ_final, i;
		Histobin abin;
		// civ tells us that the cumcount we are looking for is BELOW
		// (i.e. it stops at civ)
		//if( debug ) System.out.println("Indices (are start-0) ci="+ci+" and civ="+civ);
		// indices in the index are start-0, so inclusive:
		for(i=civ;i-->0;){
			abin = this.get_bin_with_linear_index(i);
			if( abin == null ){
				// this is not a problem if we are in a sparse array because it simply
				// neans a bin with zero counts.
				//if( debug ) System.out.println("random_bin() : Bin index="+i+" gives null bin");
				continue;
			}
			//if( debug ) System.out.println("Searching for "+anum+" < "+this.get_bin_with_linear_index(i).cumulativeCount()+" at bin="+i+", civ="+civ);
			if( anum >= abin.cumulativeCount() ){
				// now we are in here one too-many
				civ_final = i+1;
				//if( debug ) { System.out.println("search : for anum="+anum+" found bin="+civ_final+" (starts from 0), cum_count="+this.get_bin_with_linear_index(civ_final).cumulativeCount()+". Anum must be greater than the next bin down: "+this.get_bin_with_linear_index( (civ_final-1)<=0?0:(civ_final-1) ).cumulativeCount()); }
				return this.get_bin_with_linear_index(civ_final);
			}
		}
		return this.get_bin_with_linear_index(0);
	}
	// get a random bin and then get a random number within the range of that bin
	// which of course is 2D so the returned value is a Narray of (x,y, z values)
	public	double[]	random_value(Random arng){
		// get the random bin
		Histobin	abin = this.random_bin(arng);
		return abin.random_value(arng);
	}

	// convenience method to create a new bin (but not insert it)
	// given coordinates. The labels are taken from our internal labels set at init()
	// so labels of this bin will be correct as user specified.
	private	Histobin create_bin(int acoordinates[]){
		double aboundaries[][] = new double[this.myNumDims][2];
		String[] alabels = new String[this.myNumDims];
		for(int atDim=this.myNumDims;atDim-->0;){
			aboundaries[atDim][Histobin.FROM] = this.myBoundariesFrom[atDim]
				+ acoordinates[atDim] * this.myBinWidths[atDim];
			aboundaries[atDim][Histobin.TO] = aboundaries[atDim][Histobin.FROM]
				+ this.myBinWidths[atDim];
			alabels[atDim] = this.myBinCoordinates2Labels[atDim][acoordinates[atDim]];
		}
		return new Histobin(
			this.myNumDims,
			alabels,
			0, // the count (initially)
			acoordinates, // the coordinates
			aboundaries
		);
		// now this bin does not have its ID set to a linear index because it is not inserted yet.
	}
	// create a bin (but not insert it) by giving it a list of String labels
	// this will get coordinates and call the above.
	private	Histobin create_bin(List<String> alabels){
		int acoordinates[] = this.labels2bin_coordinates(alabels);
		if( acoordinates == null ){
			System.err.println("Histogram.java : "+this.myName+" : create_bin()/1 : could not find bin coordinates from labels: "+Arrays.toString(alabels.toArray()));
			return null;
		}
		// this is basically copy-paste of above create_bin but without the labels
		double aboundaries[][] = new double[this.myNumDims][2];
		for(int atDim=this.myNumDims;atDim-->0;){
			aboundaries[atDim][Histobin.FROM] = this.myBoundariesFrom[atDim]
				+ acoordinates[atDim] * this.myBinWidths[atDim];
			aboundaries[atDim][Histobin.TO] = aboundaries[atDim][Histobin.FROM]
				+ this.myBinWidths[atDim];
		}
		return new Histobin(
			this.myNumDims,
			(String[] )alabels.toArray(),
			0, // the count (initially)
			acoordinates, // the coordinates
			aboundaries
		);
		// now this bin does not have its ID set to a linear index because it is not inserted yet.
	}
	private	Histobin create_bin(String alabels[]){
		int acoordinates[] = this.labels2bin_coordinates(alabels);
		if( acoordinates == null ){
			System.err.println("Histogram.java : "+this.myName+" : create_bin()/1 : could not find bin coordinates from labels: "+Arrays.toString(alabels));
			return null;
		}
		// this is basically copy-paste of above create_bin but without the labels
		double aboundaries[][] = new double[this.myNumDims][2];
		for(int atDim=this.myNumDims;atDim-->0;){
			aboundaries[atDim][Histobin.FROM] = this.myBoundariesFrom[atDim]
				+ acoordinates[atDim] * this.myBinWidths[atDim];
			aboundaries[atDim][Histobin.TO] = aboundaries[atDim][Histobin.FROM]
				+ this.myBinWidths[atDim];
		}
		return new Histobin(
			this.myNumDims,
			alabels,
			0, // the count (initially)
			acoordinates, // the coordinates
			aboundaries
		);
		// now this bin does not have its ID set to a linear index because it is not inserted yet.
	}

	// getters and shit
	public	double	sparseness(){ return this.mySparseness; }
	public	double	boundaries_from(int adim){ return this.myBoundariesFrom[adim]; }
	public	double	boundaries_to(int adim){ return this.myBoundariesTo[adim]; }
	public	int	num_bins(int adim){ return this.myNumBinsPerDimension[adim]; }
	public	int[]	num_bins(){ return this.myNumBinsPerDimension; }
	public	int	num_total_bins(){ return this.myBins.size(); }
	public	int	num_total_bins_nonempty(){ this.recalculate(false); return this.myNumNonemptyBins; }
	public	int	bins_capacity(){ return this.myBins.capacity(); }
	public	int	num_dimensions(){ return this.myNumDims; }
	public	double	total_bin_count(){ this.recalculate(false); return this.myTotalBinCount; }
//	public	Histobin[]	bins(){ return this.myBins; }
	public	boolean	is_total_count_zero(){ return this.total_bin_count() == 0; }
	public	String	name(){ return this.myName; }
	public	void	name(String n){ this.myName = n; }
	public	ArrayList<HashMap<String,Integer>> bindims_labels_hashmaps(){ return this.myLabels2BinCoordinates; }

	// shannon entropy
	public	double	entropy(){ this.recalculate(false); return this.myEntropy; }
	// this needs to be thought over again...
	public	StatisticsContainer[] num_choices_at_last_dims(){ return this.myLastDimCountStatistics; }

	// comparisons with another hist:

	// compares two histograms by taking the sum of the square of the
	// difference in NORMALISED COUNT over each bin (pair)
	// Histograms must have the same number of bins over each dimension,
	// we don't care about labels
	// returns the metric IF HISTOGRAMS HAVE SAME SIZE (using compare_size())
	// or -1.0 if histograms differ in size
	// a value of 0.0 indicates that histograms are identical wrt bin count.
	public	double	difference_in_normalised_count_L2(
		Histogram another_histogram
	) throws Exception {
		if( this.compare_size(another_histogram) != 0 ){ return -1.0; }

		this.recalculate(false);

		// histograms have same size and also same number of bins per dimension etc.
		// so their flattened array of bins must run in the same order
		// so we take the metric by iterating on that array
		double sum = 0.0, diff;
		Histobin abin, anotherbin;
		Iterator<Histobin> itera = this.myBins.iterator();
		while( itera.hasNext() ){
			abin = itera.next();
			if( (anotherbin=another_histogram.get_bin(abin.coordinates())) == null ){
				// if it is a sparse, then null bin means zero count
				// so we will consider null bin a zero-count bin
				// throw new Exception("Histogram.java : "+this.myName+" : difference_in_normalised_count_L2() : histograms are not the same! coordinate "+Arrays.toString(abin.coordinates())+" does not exist in 'other' histogram.\nThis histogram:\n"+this+"\nAnother histogram:\n"+another_histogram); }
				diff = abin.normalisedCount();
			} else {
				// bin exists in both histograms and has positive count
				diff = abin.normalisedCount()
					- another_histogram.get_bin(abin.coordinates()).normalisedCount()
				;
			}
			sum += diff * diff;
		}
		return Math.sqrt(sum);
	}
	public	double	difference_in_normalised_count_KULLBACK(
		Histogram another_histogram
	) throws Exception {
// WARNING: you will get negative KL (which should not happen)
// if you have a zero in either distribution.
// KL is defined only when all bins norm count sum to 1
// however when one bin's count is zero and the other's is not
// then you ignore them and so the sum to 1 breaks down!
		if( this.compare_size(another_histogram) != 0 ){ return -1.0; }
		// histograms have same size and also same number of bins per dimension etc.
		// so their flattened array of bins must run in the same order
		// so we take the metric by iterating on that array
		double KL = 0.0, mycount, othercount;
		Histobin abin, anotherbin;
		Iterator<Histobin> itera = this.myBins.iterator();
		while( itera.hasNext() ){
			abin = itera.next();
			if( (anotherbin=another_histogram.get_bin(abin.coordinates())) == null ){
				// if it is a sparse, then null bin means zero count
				// so we will consider null bin a zero-count bin
				// throw new Exception("Histogram.java : "+this.myName+" : difference_in_normalised_count_KULLBACK() : histograms are not the same! coordinate "+Arrays.toString(abin.coordinates())+" does not exist in 'other' histogram.\nThis histogram:\n"+this+"\nAnother histogram:\n"+another_histogram); }
				// basically we skip because zero-count and that totally fucks up KL
				continue;
			}
			mycount = abin.normalisedCount();
			othercount = anotherbin.normalisedCount();
			if( (othercount==0.0) || (mycount==0.0) ){ continue; }
			KL += mycount * Math.log(mycount / othercount);
		}
// change base of log to 2 (for the unit to be bits)
// this should be inside the loop at the Math.log
// but for efficiency we take it out of the sum
// log_newbase(x) = log_oldbase(x) / log_oldbase(newbase)
// log_2(x) = log_e(x) / log_e(2)
// and Math.log is base e

		return KL / Histogram.log2baseE;
	}
	// KULLBACK is not symmetric (and so it is not a metric)
	// make it symmetric...
	public	double	difference_in_normalised_count_KULLBACK_SYMMETRIC(
		Histogram another_histogram
	) throws Exception {
		// KL of (us,them)
		double	myKL = this.difference_in_normalised_count_KULLBACK(another_histogram);
		if( myKL == -1.0 ){ return -1.0; } // differ in sizes
		// KL of (them, us)
		double otherKL = another_histogram.difference_in_normalised_count_KULLBACK(this);
		return myKL + otherKL;
	}
	// checks if the size (i.e. total number of bins and bins in each dimension)
	// is the same (returns 0)
	// if our total num bins is less than the other histograms, then it returns -1
	// else it returns +1
	public	int	compare_size(Histogram another_histogram){
		int our_num_bins = this.num_total_bins();
		int other_num_bins = another_histogram.num_total_bins();
		int return_in_case_of_mismatch = our_num_bins < other_num_bins ? -1 : +1;

		// check total num bins firstly
		if( our_num_bins != other_num_bins ){ return return_in_case_of_mismatch; }

		// check num dimensions secondly
		int our_num_dims = this.num_dimensions();
		int other_num_dims = another_histogram.num_dimensions();
		if( our_num_dims != other_num_dims ){ return return_in_case_of_mismatch; }

		// finally check the number of bins per dimension
		int[] other_bin_dims = another_histogram.num_bins();
		for(int i=our_num_dims;i-->0;){
			if( this.myNumBinsPerDimension[i] != other_bin_dims[i] ){ return return_in_case_of_mismatch; }
		}
		// all the same!
		return 0;
	}
	public	void	toFileInfo(String afilename) throws Exception {
		PrintWriter pw = new PrintWriter(afilename);
		pw.println(this.toStringInfo());
		pw.close();
	}
	public	String	toStringInfo(){
		StringBuilder	buffer = new StringBuilder();
		this.recalculate(false);
		buffer.append("Histogram '"+this.name()+"':\n");
		buffer.append("num dims : "+this.num_dimensions()+"\n");
		buffer.append("capacity : "+this.myBins.capacity()+"\n");
		buffer.append("num bins : "+this.myBins.size()+"\n");
		buffer.append("num non-empty bins : "+this.num_total_bins_nonempty()+"\n");
		buffer.append("type     : "+(this.myBins.sparse()?"sparse\n":"flat\n"));
		buffer.append("total bin count: "+this.total_bin_count()+"\n");
		buffer.append("sparsness: "+this.sparseness()+"\n");
		buffer.append("entropy  : "+this.entropy()+"\n");
		buffer.append("num choices for last-but-one dim : "+this.myLastDimCountStatistics[0]+"\n");
		buffer.append("num choices for last dim : "+this.myLastDimCountStatistics[1]+"\n");
		return buffer.toString();
	}

	public	String	toString(){
		StringBuilder	buffer = new StringBuilder();
		Histobin	abin;

		this.recalculate(false);
		int	I, j;
		buffer.append(this.toStringInfo());
		buffer.append("\nlabel // boundaries // count\n");
		Iterator<Histobin> itera = this.myBins.iterator();
		while( itera.hasNext() ){
			abin = itera.next();
			buffer.append(abin.labels_as_one_string());
			buffer.append(" (");
			for(j=0;j<this.myNumDims;j++){
				buffer.append("[");
				buffer.append(abin.boundary_from(j));
				buffer.append(",");
				buffer.append(abin.boundary_to(j));
				buffer.append("] ");
			}
			buffer.append(") = ");
			buffer.append(abin.count());
			buffer.append("\n");
		}
		return buffer.toString();
	}
	public	void save_to_file_txt(String afilename) throws Exception { this.save_to_file_txt(afilename, "\t"); }
	public	void save_to_file_txt(String afilename, String sep) throws Exception {
		this.recalculate(false);

		PrintWriter out =new PrintWriter(afilename);
		Histobin abin;
		Iterator<Histobin> itera = this.myBins.iterator();
		while( itera.hasNext() ){
			abin = itera.next();
			// plot normalised counts
			out.println(abin.toString_as_histoplot_normalised_counts(sep));
			// ... and not total counts!
			//out.println(abin.toString_as_histoplot_counts(sep));
		}
		out.close();
	}
	public	void dump_random_values_to_file(String afilename, int num_values) throws Exception {
		this.recalculate(false);

		PrintWriter out =new PrintWriter(afilename);
		Random arng = new Random();
		double[] arv;
		for(int i=num_values;i-->0;){
			arv = this.random_value(arng);
			out.println(arv[0]+"\t"+arv[1]);
		}
		out.close();
	}
	// accepts: {"a", "a:r", "*", "d,c,v"}
	// and will return {"1", "1:2" ... }
	// i.e. it takes wildcarded labels and provides wildcarded bin-indices
	// to be used in selecting bins using wildcards.
	public	String[] make_a_bin_selector_indices_spec_from_labels(
		String[] alabels
	) throws Exception {
		String[] out = new String[alabels.length];
		String	ashit, anout;
		int aDim = 0, j, anindex;
		Map<String,Integer> ahash;
		for(String alabel : alabels){
			if( alabel.equals("*") ){ out[aDim++] = alabel; continue; }
			anout = "";
			String shit[] = alabel.split("((?<=:)|(?=:))|((?<=,)|(?=,))");
			for(j=0;j<shit.length;j++){
				ashit=shit[j];
				if( ashit.isEmpty() ){ continue; }
				ashit.trim();
				if( ashit.equals(":") || ashit.equals(",") ){
					anout += ashit; // wildcard character
				} else {
					// a bin label, get its index
					ahash = this.myLabels2BinCoordinates.get(aDim);
					if( ahash.containsKey(ashit) == false ){
						throw new Exception("Histogram.java: make_a_bin_selector_indices_spec_from_labels() : label '"+ashit+"' does not map to any bin in dimension "+aDim);
					}
					anindex = ahash.get(ashit);
					anout += "<"+anindex+">";
				}
			}
			out[aDim] = anout;
			aDim++;
		}
		return out;
	}
	public	static String	toString_List(List<String> alist){
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for(String shit:alist){
			sb.append(shit);
			sb.append(",");
		}
		sb.setCharAt(sb.length()-1, ']');
		return sb.toString();
	}
	public	static	String	print_bin_selector_spec(String[] aspec){
		StringBuilder sb = new StringBuilder("[");
		int L = aspec.length;
		for(int i=0;i<L;i++){
			sb.append("\"");
			sb.append(aspec[i]);
			sb.append("\",");
		}
		sb.setCharAt(sb.length()-1, ']');
		return sb.toString();
	}
	// make a list of coordinates (integers) a String
	// this is not the same as constructing a bin's labels given coordinates!!!
	// this just gets the integer coordinates and forms a String with them
	public static String	coordinates2string(int acoordinates[]){
		StringBuilder sb = new StringBuilder();
		int L = acoordinates.length, atDim;
		for(atDim=0;atDim<L;atDim++){
			sb.append('<');
			sb.append(acoordinates[atDim]);
			sb.append('>');
		}
		return sb.toString();
	}
	// given a set of bin coordinates construct the bin's label (as a List<String>)
	// by enquiring 'this.myBinCoordinates2Labels' which holds labels for each dimension
	public List<String>	coordinates2list_of_labels(int acoordinates[]){
		int L = acoordinates.length, atDim;
		List<String> shit = new ArrayList<String>(L);
		for(atDim=0;atDim<L;atDim++){
			shit.add(
				this.myBinCoordinates2Labels[atDim][acoordinates[atDim]]
			);
		}
		return shit;
	}
	// given a set of bin coordinates construct the bin's label (as a List<String>)
	// by enquiring 'this.myBinCoordinates2Labels' which holds labels for each dimension
	public String[]	coordinates2array_of_labels(int acoordinates[]){
		int L = acoordinates.length, atDim;
		String shit[] = new String[L];
		for(atDim=0;atDim<L;atDim++){
			shit[atDim] =  this.myBinCoordinates2Labels[atDim][acoordinates[atDim]];
		}
		return shit;
	}
	public	HistogramSampler sampler(Random arng){
		return new HistogramSampler(
			this.name()+"_sampler",
			this,
			arng
		);
	}
	// counts and returns mean and stdev of the number of choices when the bin selector
	// is a '*' at the last bin only
	// returns an array for each of last dim and last-but-one dim

	private	void count_num_choices_at_last_dims(){
		this.myLastDimCountStatistics[0].reset();
		this.myLastDimCountStatistics[1].reset();

		HashMap<String,IntegerCounter> acuntsLastDim = new HashMap<String,IntegerCounter>();
		HashMap<String,IntegerCounter> acuntsLastButOneDim = new HashMap<String,IntegerCounter>();
		String	labels[];
		Histobin abin;
		Iterator<Histobin> itera = this.myBins.iterator();
		int	N1 = this.myNumDims - 1;
		int	N2 = N1 - 1;
		String bin_selectorLastDim, bin_selectorLastButOneDim;
		IntegerCounter amatch;
		while( itera.hasNext() ){
			abin = itera.next();

			// get a bin-selector which is the labels of the bin up to dim N1
			// each dim is enclosed in <> then this string is the key for the hashmap
			// and then we count how many we hit.
			// e.g. for 3 dims a lab will be '<a><b>' for '<a><b><c>' and '<a><b><d>'
			// so the num choices for that <a><b>* is 2
			bin_selectorLastDim = abin.pack_labels_into_one_string(0, N1);
			if( (amatch=acuntsLastDim.get(bin_selectorLastDim)) == null ){
				amatch = new IntegerCounter(1);
				acuntsLastDim.put(bin_selectorLastDim, amatch);
			} else {
				amatch.increment();
			}

			// get a bin-selector which is the labels of the bin up to dim N2
			bin_selectorLastButOneDim = abin.pack_labels_into_one_string(0, N2);
			if( (amatch=acuntsLastButOneDim.get(bin_selectorLastButOneDim)) == null ){
				amatch = new IntegerCounter(1);
				acuntsLastButOneDim.put(bin_selectorLastButOneDim, amatch);
			} else {
				amatch.increment();
			}
		}
		int n, acountervalue, i;

		double sum, sumsquare, min, max, mean;
		Iterator<IntegerCounter> it;
		IntegerCounter ac;
		for(i=0;i<2;i++){ // for the last-but-one and last dims
			if( i == 0 ){
				it = acuntsLastButOneDim.values().iterator();
				n = acuntsLastButOneDim.size();
			} else {
				it = acuntsLastDim.values().iterator();
				n = acuntsLastDim.size();
			}
			sum = 0.0;
			sumsquare = 0.0;
			min = 0.0;
			max = 0.0;

			if( ! it.hasNext() ){ return; /* no samples! */}

			ac = it.next();
			min = max = ac.myCounterValue;
			do {
				acountervalue = ac.myCounterValue;
				if( min > acountervalue ){ min = acountervalue; }
				if( max < acountervalue ){ max = acountervalue; }
				sum += acountervalue;
				sumsquare += acountervalue * acountervalue;
			} while( it.hasNext() && ((ac=it.next())!=null) );

			mean = sum / n;
			this.myLastDimCountStatistics[i].set(
				mean, /*mean*/
				Math.sqrt(sumsquare/n - (mean * mean)), /* running stdev */
				sum,
				min, max,
				n /* num samples */
			);
		}
	}

	@SuppressWarnings("unchecked")
	private final T construct_fucking_generics_object(
		String param1,
		int param2[]
	) throws Exception {
//		Constructor shit = this.myTClass.getDeclaredConstructors()[0];
		Constructor shit = this.myTClass.getConstructor(
			param1.getClass(), param2.getClass(),
			Class.class
		);
		if( shit == null ){ throw new Exception("Histogram.java : "+this.myName+" : construct_fucking_generics_object() : could not find constructor for Container<T> matching signature 'String,Integer,Histobin'"); }
		//if( debug ) System.out.println("Histogram.java : "+this.myName+" : construct_fucking_generics_object() : instantiating via constructor: "+shit);
		return (T )shit.newInstance(param1, param2, Histobin.class);
	}
}
class	IntegerCounter {
	public int myCounterValue = 0;
	public	IntegerCounter(int av){ this.myCounterValue = av; }
	public	IntegerCounter(){ this.myCounterValue = 0; }
	public	void	increment(){ this.myCounterValue++; }
	public	int	value(){ return this.myCounterValue; }
}

