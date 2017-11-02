package	ahp.org.Histograms;

import	java.util.Random;
import	java.io.FileNotFoundException;
import	java.io.Serializable;
import	java.io.PrintWriter;
import	java.util.Comparator;
// so not thread-safe:
import	java.util.HashMap;
import	java.util.Arrays;
import	java.util.List;
import	java.util.ArrayList;

public class	Histobin implements Comparable<Histobin>, Serializable {
	private static final long serialVersionUID = 1797271487L+2L;

	private	boolean	debug = false;

	public	static final int FROM = 0,
				 TO = 1;

	private	int	myNumDims;
	private	int[]	myCoordinates;
	private	int	myID = -1; // can be anything we give it - can use it for its linear index

	// the boundaries of the bin one for each dimension and then from|to
	// [X|Y|Z...numdims][FROM|TO]
	private	double[][] myBoundaries;

	// these are the OVERALL counts etc:
	private	double	myCountOverall = 0.0; // just the count
	private	double	myNormCountOverall = 0.0; // the normalised count
	private	double	myCumCountOverall = 0.0; // the normalised cumulative count

	// label associated with this bin
	// there is a String for each bin dimension
	private	String	myLabels[] = null;
	private	String	myLabelsAsString = null;
	private	boolean	myLabelsNeedRecalculate = false;

	public Histobin(
		int	num_dims,
		String[] alabels
	){
		this.alloc(num_dims);
		this.labels(alabels);
		this.reset_geography();
		this.reset_counts();
	}
	public Histobin(
		int	num_dims,
		String[]	alabels,
		int	acount,
		int[]	acoordinates,
		double[][] aboundaries
	){
		// allocates wrt num_dims
		// and then initialises first the coordinates
		// and if there are sufficient varargs, also the counts
		this.alloc(num_dims);

		this.reset_counts(); // reset the cum/norm counts
		this.init(acoordinates, acount, aboundaries);
		// labels after setting coordinates (in case we need to do default)!
		this.labels(alabels);
	}
	public	void	reset_geography(){
		// reset location and boundaries
		for(int i=this.myNumDims;i-->0;){
			this.myCoordinates[i] = 0;
			this.myBoundaries[i][FROM] = 0;
			this.myBoundaries[i][TO] = 0;
		}
	}
	public	void	reset_counts(){
		// resets the bin counts (not widths, number of bins etc.)
		this.myCountOverall = 0;
		this.myNormCountOverall = 0.0;
		this.myCumCountOverall = 0.0;
	}
	public	void	alloc(int num_dims){
		this.myNumDims = num_dims;
		this.myCoordinates = new int[num_dims];
		this.myBoundaries = new double[num_dims][2];
	}
	public	void	init(
		int[] acoordinates,
		int acount,
		double[][] aboundaries
	){
		int	i;
		for(i=0;i<this.myNumDims;i++){
			this.myCoordinates[i] = acoordinates[i];
		}
		this.myCountOverall = acount;
		for(i=this.myNumDims;i-->0;){
			// left:
			this.myBoundaries[i][Histobin.FROM] = aboundaries[i][FROM];
			// right:
			this.myBoundaries[i][Histobin.TO] = aboundaries[i][TO];
		}
	}
	// concatenate all labels into one string label for toString etc.
	public	String	labels_as_one_string(){
		this.labels_recalculate();
		return this.myLabelsAsString;
	}
	private	void	make_default_labels(){
		if( debug ){ System.out.println("Histobin.java : make_default_labels() : called, my coordinates are: "+Arrays.toString(this.myCoordinates)); }
		this.myLabels = new String[this.myNumDims];
		for(int i=0;i<this.myNumDims;i++){
			this.myLabels[i] = Integer.toString(this.myCoordinates[i]);
		}
		this.myLabelsNeedRecalculate = true;
	}
	public	String	pack_labels_into_one_string(int from, int to){
		StringBuilder sb = new StringBuilder();
		for(int i=from;i<to;i++){
			sb.append("<");
			sb.append(this.myLabels[i]);
			sb.append(">");
		}
		return sb.toString();
	}
	public	String	pack_labels_into_one_string(){
		return pack_labels_into_one_string(0, this.myNumDims);
	}
	private	void	labels_recalculate(){
		if( this.myLabelsNeedRecalculate == false ){ return; }
		this.myLabelsAsString = this.pack_labels_into_one_string();
	}		
	public	void	labels(String[] alabels){
		if( alabels == null ){
			this.make_default_labels();
		} else {
			this.myLabels = alabels;
		}
		this.myLabelsNeedRecalculate = true;
	}
	public	String[]	labels(){
		this.labels_recalculate();
		return this.myLabels;
	}
	public	List<String>	labels_as_list(){
		return Arrays.asList(this.labels());
	}
	public	String	label(int atDim){
		return this.myLabels[atDim];
	}

	// other getters
	public	double	boundary(int adim, int alefttoright){ return this.myBoundaries[adim][alefttoright]; }
	public	double	boundary_from(int adim){ return this.myBoundaries[adim][Histobin.FROM]; }
	public	double	boundary_to(int adim){ return this.myBoundaries[adim][Histobin.TO]; }
	public	double[]	boundaries(int adim){ return this.myBoundaries[adim]; }
	public	void		boundaries(int adim, double[] avalues){
		this.myBoundaries[adim][FROM] = avalues[FROM];
		this.myBoundaries[adim][TO] = avalues[TO];
	}
	public	double[][]	boundaries(){ return this.myBoundaries; }
	public	void	boundaries(double [][] avalues){
		for(int i=avalues.length;i-->0;){
			this.myBoundaries[i][FROM] = avalues[i][FROM];
			this.myBoundaries[i][TO] = avalues[i][TO];
		}
	}
	public	int	coordinate(int adim){ return this.myCoordinates[adim]; }
	public	void	coordinate(int adim, int avalue){ this.myCoordinates[adim] = avalue; }
	public	int[]	coordinates(){ return this.myCoordinates; }
	public	void	coordinates(int acoordinates[])	throws Exception {
		if( acoordinates.length != this.myNumDims ){ throw new Exception("Histobin.java: coordinates() : length of input coordinates specified is not the same as the number of this bin's dimensions : "+acoordinates.length+" != "+this.myNumDims); }
		for(int i=this.myNumDims;i-->0;){
			acoordinates[i] = this.myCoordinates[i];
		}
	}

	public	int	id(){ return this.myID; }
	public	void	id(int i){ this.myID = i; }

/*	public	double	cumulativeCount(int adim){ return this.myCumCounts[adim]; }
	public	void	cumulativeCount(int adim, double avalue){ this.myCumCounts[adim] = avalue; }
	public	double[] cumulativeCounts(){ return this.myCumCounts; }
*/
	public	double	cumulativeCount(){ return this.myCumCountOverall; }
	public	void	cumulativeCount(double avalue){ this.myCumCountOverall = avalue; }

	public	double	normalisedCount(){ return this.myNormCountOverall; }
	public	void	normalisedCount(double avalue){ this.myNormCountOverall = avalue; }

	public	double	count(){ return this.myCountOverall; }
	public	void	count(double avalue){ this.myCountOverall = avalue; }

	public	double	count_increment(){ return ++this.myCountOverall;}
	// you should make sure it does not become negative!
	public	double	count_decrement(){ return --this.myCountOverall; }
	public	boolean	is_count_zero(){ return this.myCountOverall == 0; }

	public	int	N(){ return this.myNumDims; }

	// return the center of the bin (as an N-vector)
	public double[]	center(
	){
		double ret[] = new double[this.myNumDims];
		for(int i=this.myNumDims;i-->0;){
			ret[i] = (this.myBoundaries[i][0] + this.myBoundaries[i][1]) / 2.0;
		}
		return ret;
	}
	// return the center of the bin (as an N-vector)
	public void center(
		double center_ref[]
	){
		for(int i=this.myNumDims;i-->0;){
			center_ref[i] = (this.myBoundaries[i][0] + this.myBoundaries[i][1]) / 2.0;
		}
	}
	// select a random value (N-vector) from within this bin's boundaries
	public double[]	random_value(
		Random arng
	){
		double ret[] = new double[this.myNumDims];
		for(int i=this.myNumDims;i-->0;){
			// nextDouble() must be between 0-1
			ret[i] = this.myBoundaries[i][0]
				  + (this.myBoundaries[i][1] - this.myBoundaries[i][0]) * arng.nextDouble();
		}
		return ret;
	}
	public	int	compareTo(Histobin another_histobin){
		double	shit = this.myCumCountOverall - another_histobin.cumulativeCount();
		if( shit > 0.0 ){ return 1; }
		else if( shit < 0.0 ){ return -1; }
		return 0;
	}
	// copy all our values to the destination obj
	// WE ARE THE SOURCE!
	public	void	copy(Histobin destination_obj) throws Exception {
		if( destination_obj.N() != this.N() ){
			throw new Exception("Histobin.java : copy() : source and destination Histobins do not have the same number of dimensions, "+this.N()+" != "+destination_obj.N());
		}
		destination_obj.coordinates(this.coordinates());
		destination_obj.count(this.count());
		destination_obj.cumulativeCount(this.cumulativeCount());
		destination_obj.normalisedCount(this.normalisedCount());
		destination_obj.boundaries(this.boundaries());
		destination_obj.labels(this.labels());
	}
	public	Histobin clone(){
		Histobin ahistobin = new Histobin(
			this.myNumDims,
			this.labels()
		);
		try {
			this.copy(ahistobin);
			return ahistobin;
		} catch(Exception ex){
			System.err.println("Histobin.java: clone() : exception was caught while calling copy():\n"+ex);
			ex.printStackTrace();
			return null;
		}
	}
	public	static String	array_toString(Histobin arr[]){
		StringBuilder sb = new StringBuilder();
		for(int i=0;i<arr.length;i++){
			sb.append(arr[i].toString());
			sb.append("\n");
		}
		return sb.toString();
	}
	public	String	toString(){
		return new String("[l='"+this.labels_as_one_string()+"', c="+this.myCountOverall+", nc="+this.myNormCountOverall+", cc="+this.myCumCountOverall+", id="+this.id()+"]");
	}
	public  String  toString_as_histoplot_counts(String sep){
		StringBuilder sb = new StringBuilder();
		for(int i=0;i<this.myNumDims;i++){
			sb.append(this.myCoordinates[i]);
			sb.append(sep);
		}
		sb.append(this.myCountOverall);
		return sb.toString();
	}
	public  String  toString_as_histoplot_normalised_counts(String sep){
		StringBuilder sb = new StringBuilder();
		for(int i=0;i<this.myNumDims;i++){
			sb.append(this.myCoordinates[i]);
			sb.append(sep);
		}
		sb.append(this.myNormCountOverall);
		return sb.toString();
	}
}
