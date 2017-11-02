 package	ahp.org.Histograms;

import	java.util.Random;
import	java.util.Arrays;
import	java.util.List;
import	java.util.ArrayList;
import	java.util.Map;
import	java.util.HashMap;
import	java.io.Serializable;

import	ahp.org.Cartesians.*;

/* select some (via wildcards) or all of the Histogram bins
   and then get random bins/values from them according to the
   distribution of the sample.
   Wildcard selection can be via bin labels(strings) or bin coordinates (integers).
   The selected bins's references will be stored internally!
*/
public	class	HistogramSampler implements Serializable {
	private static final long serialVersionUID = 1797271487L+6L;

	private	String		myName; // a name for this sampler
	private	Random		myRNG;
	private	String[]	mySelectionIndicesSpec = null;

	private	int		myNumDims;

	private	Histobin[]	mySelectedBins = null;
	private	int		mySelectedBinsTotalNum = 0;
	private	int		mySelectedBinsTotalCount = 0;
	private	int		mySelectedBinsTotalCountMinus1 = 0;

	// we get this from our histogram for easy access
	private	ArrayList<? extends Map<String,Integer>> myBinDimLabelsHashMaps;
	private	Histogram	myHistogram = null;

	public	HistogramSampler(
		String	aname,
		Histogram	ahist,
		Random	arng
	){
		this.name(aname);
		this.myHistogram = ahist;
		this.rng(arng);

		this.init();
	}

	@SuppressWarnings("unchecked")
	private	void	init(){
		this.myNumDims = this.myHistogram.num_dimensions();
                // bindims_labels_hashmaps():
                // public  ArrayList<HashMap<String,Integer>>      bindims_labels_hashmaps(){ return this.myBinDimLabelsHashMaps; }
                // so fuck off
		this.myBinDimLabelsHashMaps = this.myHistogram.bindims_labels_hashmaps();
	}

	// get the bin by next calling random_bin_from_selection()
	public	boolean	select_bins_using_wildcard_labels(
		String[] bin_dim_LABELS_spec
	) throws Exception {
		if( bin_dim_LABELS_spec.length == 0 ){
			throw new Exception("HistogramSampler.java : select_bins_using_wildcard_labels() : specified labels spec does not have the same number of dimensions as my histogram ("+this.myNumDims+") : "+HistogramSampler.print_bin_selector_spec(bin_dim_LABELS_spec));
		}
		if( bin_dim_LABELS_spec.length != this.myNumDims ){
			throw new Exception("HistogramSampler.java : select_bins_using_wildcard_labels() : specified labels spec does not have the same number of dimensions as my histogram ("+this.myNumDims+") : "+HistogramSampler.print_bin_selector_spec(bin_dim_LABELS_spec));
		}
		// given an array of string labels, convert it to an array of coordinates (integers) to access the bin
		String anindices[] = SpecsParser.labels2integer_indices_spec(
			bin_dim_LABELS_spec,
			this.myBinDimLabelsHashMaps
		);

//System.out.println("From labels: "+Arrays.toString(bin_dim_LABELS_spec)+" I got indices:"+Arrays.toString(anindices));
//System.out.println("bindim labels jashmap:\n"+this.myBinDimLabelsHashMaps);
//System.exit(1);
		if( anindices.length == 0 ){
			System.err.println("HistogramSampler.java : select_bins_using_wildcard_labels() : specified selection yieled zero indices back : "+HistogramSampler.print_bin_selector_spec(bin_dim_LABELS_spec));
			return false;
		}
		if( ! this.select_bins_using_wildcard_indices(anindices) ){
			//System.err.println("HistogramSampler.java : select_bins_using_wildcard_labels() : call to select_bins_using_wildcard_indices() has failed for (labels) selector : "+HistogramSampler.print_bin_selector_spec(bin_dim_LABELS_spec));
			return false;
		}
		return true;
	}
	// get the bin by next calling random_bin_from_selection()
	public	boolean	select_bins_using_wildcard_indices(
		String[] bin_dim_INDICES_spec
	) throws Exception {
		if( bin_dim_INDICES_spec.length == 0 ){
			throw new Exception("HistogramSampler.java : select_bins_using_wildcard_indices() : got empty indices back for labels: "+HistogramSampler.print_bin_selector_spec(bin_dim_INDICES_spec));
		}
		if( bin_dim_INDICES_spec.length != this.myNumDims ){
			throw new Exception("HistogramSampler.java : select_using_wildcard_indices() : specified labels spec does not have the same number of dimensions as my histogram ("+this.myNumDims+") : "+HistogramSampler.print_bin_selector_spec(bin_dim_INDICES_spec));
		}
		this.mySelectionIndicesSpec = bin_dim_INDICES_spec;
		if( this._apply_bin_selection_slow_and_slim() == 0 ){
			//System.err.println("HistogramSampler.java : select_using_wildcard_indices() : selection yielded zero bins : "+HistogramSampler.print_bin_selector_spec(bin_dim_INDICES_spec));
			return false;
		}
		return true;
	}

	// store in this object all the bins selected from the wildcard specified
	// then everytime a random bin is asked, we respond fast
	// NOTE: this is fast but it can use large amounts of memory because
	// all the bin coordinates produced by the spec are returned in an array
	// and then this array is iterated. This is fast but if memory space is
	// more valuable to you then surely use _apply_bin_selection_slow_and_slim() which is not that slower at all
	private int	_apply_bin_selection_fast_and_fat() throws Exception {
		long time_started = System.nanoTime();
		// cartesian product enumerator needs the number of bins per dimension
		Cartesian acartesian = new Cartesian(this.myHistogram.num_bins());
		int     bin_indices[][] = acartesian.product(this.mySelectionIndicesSpec);
		acartesian = null; // we no longer need it, garbage it because it is huge...

		int L = bin_indices.length, i;
		ArrayList<Histobin> shit = new ArrayList<Histobin>();
		this.mySelectedBins = new Histobin[L];
		this.mySelectedBinsTotalNum = L;

		Histobin abin;
		this.mySelectedBinsTotalCount = 0;
		double acount;
		for(i=0;i<L;i++){
			// we will skip if bin=null (no selection matched this bins combi)
			// we will also skip if bin-count is zero!
			// so anything returned will be bincount>0

			abin = this.myHistogram.get_bin(bin_indices[i]);
			if( (abin==null)
			 || ((acount=abin.count())<1.0) ){ continue; }
			this.mySelectedBinsTotalCount += acount;
			shit.add(abin);
		}
		this.mySelectedBinsTotalCount = shit.size();
		this.mySelectedBins = shit.toArray(new Histobin[this.mySelectedBinsTotalCount]);
		this.mySelectedBinsTotalCountMinus1 = this.mySelectedBinsTotalCount - 1;
		return this.mySelectedBinsTotalCount;
	}
	// store in this object all the bins selected from the wildcard specified
	// then everytime a random bin is asked, we respond fast
	// NOTE: this version does not enumerate all indices selected by specs in an array
	// as the _apply_bin_selection_fast_and_fat() version above.
	// It creates a new set of indices on the fly as an iterator.
	// it does check if the bin is null or has count zero and ignores those
	private int	_apply_bin_selection_slow_and_slim() throws Exception {
		long time_started = System.nanoTime();

		// cartesian product enumerator needs the number of bins per dimension
		CartesianProductIteratorCompressed CPI = new CartesianProductIteratorCompressed(
			this.myHistogram.num_bins(),
			this.mySelectionIndicesSpec
		);
		int	bin_indices[] = new int[this.myNumDims];
		// NOTE WARNING: span() (num_states) IS THE MAX number of bins which can exist and matched
		// However, some bins can be null in the case sparse arrays, so we push matched non-null
		// bins into arraylist and then return an array with all non-null Histobins
		int	num_states = CPI.span();
		// push not-null bins here:
		ArrayList<Histobin> tmp_selected_bins = new ArrayList<Histobin>();
		Histobin abin;
		this.mySelectedBinsTotalCount = 0;
		double acount;
		while( CPI.hasNext() ){
			// result goes to pre-allocated bin_indices
			           //<--- bin_indices: is a ref to get results back
			CPI.next(bin_indices);
			// so now we have the nextbin's indices, and we query it from histo
			// we will skip if bin=null (no selection matched this bins combi)
			// we will also skip if bin-count is zero!
			// so anything returned will be bincount>0
			if( ((abin=this.myHistogram.get_bin(bin_indices))==null)
			   ||((acount=abin.count())<1.0)
			 ){
				// it is normal for sparse-array histograms not to contain all coordinates
				// at that coordinate there is no bin because there is no count!
				//throw new Exception("HistogramSampler.java : _apply_bin_selection_slow_and_slim() : call to get_bin() has failed for bin coordinates/indices: "+Arrays.toString(bin_indices)+", spec was: "+Arrays.toString(this.mySelectionIndicesSpec)+"\nand the CPI was:\n"+CPI);
				continue;
			}
			tmp_selected_bins.add(abin);
			this.mySelectedBinsTotalCount += acount;
		}
		this.mySelectedBinsTotalNum = tmp_selected_bins.size();
		// selected bins are guarenteed non-null in the case of sparse container.
		this.mySelectedBins = tmp_selected_bins.toArray(new Histobin[this.mySelectedBinsTotalNum]);

		this.mySelectedBinsTotalCountMinus1 = this.mySelectedBinsTotalCount - 1;
		return this.mySelectedBinsTotalCount;
	}
	public	Histobin	random_bin_from_selection() throws Exception {
		long time_started = System.nanoTime();
		boolean	debug = false;

		// random integer from 1 to the sum of all counts (not from zero!)
		// so we have a special case if total count is 1, then do not use RNG
		int count_to_stop =
			1 + (this.mySelectedBinsTotalCount==1 ?
				0 : this.myRNG.nextInt(this.mySelectedBinsTotalCountMinus1)
			)
		;
		// and then crawl the bins until this number is exhausted
		int j = 0;
		do {
			count_to_stop -= (int )(this.mySelectedBins[j].count());
			j++;
		} while( count_to_stop > 0 ); // this loop will not access beyond the array's end!
		j--;
		Histobin abin = this.mySelectedBins[j];
		if( (abin == null) || (abin.count() == 0) ){
			throw new Exception("HistogramSampler.java : random_bin_from_selection() : something wrong, count of selected bin is 0 : "+abin+" and total sum count="+this.mySelectedBinsTotalCount);
		}
		return abin;
	}
	// get our choice of a random bin (via montecarlo above) and also all the selected bins
	// returns an array.
	// The element at [0] is the random-bin (can be null) selected by RNG
	// all remaining elements are the selected bins forming the set of 'choices'
	// see also selected_bins() (does not select a random_bin whihc takes time)
	public	Histobin[]	random_bin_from_selection_and_the_selection_itself() throws Exception {
		Histobin ret[] = new Histobin[this.mySelectedBinsTotalNum+1];
		ret[0] = this.random_bin_from_selection(); // first element is a random bin of our own method
		// rest is filled with all candidate bins selected by this selector:
		for(int i=this.mySelectedBinsTotalNum+1;i-->1;){ ret[i] = this.mySelectedBins[i-1]; }
		return ret;
	}

	// returns the values of a bin (selected randomly from within its boundaries)
	// throws exception if something went wrong in selecting the bin
	public	double[]	random_value_from_selection() throws Exception {
		Histobin abin = this.random_bin_from_selection();
		return abin.random_value(this.myRNG);
	}

	// accepts: {"a", "a:r", "*", "d,c,v"}
	// and will return {"1", "1:2" ... }
	// i.e. it takes wildcarded labels and provides wildcarded bin-indices
	// to be used in selecting bins using wildcards.
	private  String[] make_a_bin_selector_indices_spec_from_labels(
		String[] alabels
	) throws Exception {
		String[] out = new String[alabels.length];
		String  ashit, anout;
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
					ahash = this.myBinDimLabelsHashMaps.get(aDim);
					if( ahash.containsKey(ashit) == false ){
						throw new Exception("HistogramSampler.java: make_a_bin_selector_indices_spec_from_labels() : label '"+ashit+"' does not map to any bin in dimension "+aDim);
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
	// getters
	// return a copy of the (internal) selected bins after a call to, e.g.
	//    select_bins_using_wildcard_indices()
	// returns null if num of bins selected is zero
	public	Histobin[]	selected_bins(){
		return (this.mySelectedBinsTotalNum == 0) ?
			new Histobin[]{} // in case bins selected is zero
			:
			Arrays.copyOf(
				this.mySelectedBins, // src
				mySelectedBinsTotalNum // how many elements
			)
		;
	}

	public	void	rng(Random r){ this.myRNG = r; }
	public	void	name(String a){ this.myName = a; }
	public	String	name(){ return this.myName; }
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
}
