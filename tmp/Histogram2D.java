package	ahp.org.Histograms;

import	java.util.Random;
import  java.io.FileNotFoundException;
import  java.io.PrintWriter;
import	java.util.Comparator;
import	java.util.Collections;

public	class	Histogram2D {
	private	int		myNumBinsX,
				myNumBinsY,
				myNumTotalBins;
	private	double[][]	myBins;
	private	double		myBinWidthX,
				myBinWidthY;

	// discreet cumulative density function
	// normalises the bins to Sum(counts) = 1 and then 
	// adds the counts
	private	Histoflatbins2D[]	myCumBins_flat;
	private	double[][]		myCumBins;
	// the leftmost and rightmost bin coordinates
	// to define the range of the inputs to the bins
	private double		myBinsXLeftBoundary,
				myBinsXRightBoundary,
				myBinsYLeftBoundary,
				myBinsYRightBoundary;
	// optional labels for each bin:
	private	String[][]	myLabels;
	// a flag which is set every time a value is added or removed
	// it indicates that the bin's predictor must be re-calculated.
	private	boolean		myNeedRecalculate;

	// comparator to sort cumbins_flat array
	private	Histoflatbins2D_Comparator myCumBinsFlatComparator = new Histoflatbins2D_Comparator();

	public	Histogram2D(
		int	num_binsX,
		int	num_binsY,
		double	bin_widthX,
		double	bin_widthY,
		double	start_fromX, // bins' leftmost boundary (not center)
		double	start_fromY // bins' leftmost boundary (not center)
	) throws Exception {
		if( ! this._setup_bins(num_binsX, num_binsY, bin_widthX, bin_widthY, start_fromX, start_fromY) ){
			System.err.println("Histogram2D.java : constructor1 : call to _setup_bins() has failed.");
			throw new Exception("Histogram2D.java : constructor1 : call to _setup_bins() has failed.");
		}
		if( ! this._setup_labels(null) ){
			// just a warning
			System.err.println("Histogram2D.java : _setup() : call to _setup_labels() has failed. No labels will be setup but the bins are OK and will continue.");
		}
	}
	public	Histogram2D(
		int	num_binsX,
		int	num_binsY,
		double	bin_widthX,
		double	bin_widthY,
		double	start_fromX, // bins' leftmost boundary (not center)
		double	start_fromY, // bins' leftmost boundary (not center)
		String[] labels // optional
	) throws Exception {
		if( ! this._setup_bins(num_binsX, num_binsY, bin_widthX, bin_widthY, start_fromX, start_fromY) ){
			System.err.println("Histogram2D.java : constructor1 : call to _setup_bins() has failed.");
			throw new Exception("Histogram2D.java : constructor1 : call to _setup_bins() has failed.");
		}
		if( ! this._setup_labels(labels) ){
			// just a warning
			System.err.println("Histogram2D.java : _setup() : call to _setup_labels() has failed. No labels will be setup but the bins are OK and will continue.");
		}
	}

	private	void	_make_default_labels(){
		int	num_binsX = this.myNumBinsX;
		int	num_binsY = this.myNumBinsY, i, j;
		// automatic labels
		this.myLabels = new String[num_binsX][num_binsY];
		for(i=num_binsX;i-->0;){
			for(j=num_binsY;j-->0;){
				this.myLabels[i][j] = (i+1)+","+(j+1);
			}
		}
	}
	// this function must be called after bins have been setup (_setup())
	private	boolean	_setup_labels(String[][] labels){
		int	num_binsX = this.myNumBinsX;
		int	num_binsY = this.myNumBinsY, i, j;
		if( labels == null ){
			// make default labels
			this._make_default_labels();
		} else {
			if( (labels.length != num_binsX) || (labels[0].length != num_binsY) ){
				System.err.println("Histogram2D.java: _setup() : WARNING : number of bins "+num_binsX+" must be the same as the number of bin labels, "+labels.length+" and "+num_binsY+" the same as "+labels[0].length+". Bin labels will be ignored.");
				return false;
			} else {
				this.myLabels = new String[num_binsX][];
				for(i=num_binsX;i-->0;){
					this.myLabels[i] = new String[num_binsY];
					for(j=num_binsY;j-->0;){
						this.myLabels[i][j] = new String(labels[i][j]);
					}
				}
			}
		}
		return true;
	}
	private	boolean	_setup_bins(
		int	num_binsX,
		int	num_binsY,
		double	bin_widthX,
		double	bin_widthY,
		// this is not where the first bincentre is!
		// this is where the left boundary of the first bin is:
		double	start_fromX,
		double	start_fromY
	){
		this.myNumBinsX = num_binsX;
		this.myNumBinsY = num_binsY;
		this.myNumTotalBins = num_binsX * num_binsY;

		this.myBinsXLeftBoundary = start_fromX;
		this.myBinsYLeftBoundary = start_fromY;
		this.myBinsXRightBoundary = start_fromX + num_binsX * bin_widthX;
		this.myBinsYRightBoundary = start_fromY + num_binsY * bin_widthY;

		this.myBinWidthX = bin_widthX;
		this.myBinWidthY = bin_widthY;

		this.myBins = new double[num_binsX][];
		this.myCumBins = new double[num_binsX][];
		// flat cumbins it is a 1D array of all the cumbins (which is a 2D array)
		this.myCumBins_flat = new Histoflatbins2D[this.myNumTotalBins];

		int i, j, k = 0;
		for(i=num_binsX;i-->0;){
			this.myBins[i] = new double[num_binsY];
			this.myCumBins[i] = new double[num_binsY];
			for(j=num_binsY;j-->0;){
				this.myBins[i][j] = 0.0;
				this.myCumBins[i][j] = 0.0;
				this.myCumBins_flat[k++] = new Histoflatbins2D(i, j, 0.0);
			}
		}
		this.myNeedRecalculate = false;

		return true;
	}
	// given a value on the x-axis and one on the y-axis, find which bin it belongs to
	// returns the bin coordinates as a 2-array of int
	private	int[]	get_bin_index(double x, double y){
		return new int[]{
			(int )(Math.floor((x-this.myBinsXLeftBoundary)/this.myBinWidthX)),
			(int )(Math.floor((y-this.myBinsYLeftBoundary)/this.myBinWidthY))
		};
	}
	// add a value to the corresponding bin - increment its count
	public	void	add(double x, double y){
		int[] shit = this.get_bin_index(x, y);
		this.myBins[shit[0]][shit[1]]++;
		this.myNeedRecalculate = true;
	}
	public	void	remove(double x, double y){
		int[]	ind = this.get_bin_index(x, y);
		if( this.myBins[ind[0]][ind[1]] == 0 ){ return; }
		this.myBins[ind[0]][ind[1]]--;
		this.myNeedRecalculate = true;
	}
	// will construct the discreet cumulative density function
	// which will be used to output a random value based on the distribution
	// represented by this histogram. Every time a bin changes, this
	// needs to be recalculated
	// but it is better to have a flag and whenever a prediction is required
	// then this function maybe called.
	private	void	recalculate_predictors(){
		if( this.myNeedRecalculate == false ){ return; }
		double	totalsum = 0.0, asum;
		int	NX = this.myNumBinsX,
			NY = this.myNumBinsY,
			i, j, k;
		for(i=NX;i-->0;){
			for(j=NY;j-->0;){
				totalsum += this.myBins[i][j];
			}
		}

		k = 0;
		for(i=0;i<NX;i++){
			for(j=0;j<NY;j++){
				asum = 0.0;
				// the cumbins of i,j is formed by the sum of the bins of the rectangle(0,0,i,j)
				// this is the rectangle:
				for(I=i;I-->0;) for(J=j;J-->0;) asum += this.myBins[I][J];
				this.myCumBins[i][j] = asum / totalsum;
				this.myCumBins_flat[k++].set(i, j, this.myCumBins[i][j]);
			}
		}
		// and now sort the cumbins flat wrt to content (v)
		Collections.sort(this.myCumBins_flat, this.myCumBinsFlatComparator);

		this.myNeedRecalculate = false;
	}
	// returns a 2array of random bin index based on bins contents, i.e. most
	// popular bins will have more chance of being selected
	// this is basically an attempt to re-create the distribution
	public	int[]	random_bin_index(Random arng){
		this.recalculate_predictors();
		// RNG nextDouble : must be between 0 and 1:
		double anum = arng.nextDouble();
		int k = random_bin_flatbin_index(arng);
		return new int[]{
			this.myCumBins_flat[k].i,
			this.myCumBins_flat[k].j
		};
	}
	private	int	random_bin_flatbin_index(Random arng){
		for(int k=this.myNumTotalBins;k-->1;){
			if( (anum<=this.myCumBins_flat[k].v) &&
			    (anum>this.myCumBins_flat[k-1].v)
			){
				return k;
			}
		}
		return 0;
	}
	// get a random bin and then get a random number within the range of that bin
	// and return that
	public	double	random_value(Random arng){
		int	k = this.random_bin_flatbin_index(arng);
		int[]	binindex = this.random_bin_index(arng);
		double	binboundaryLeft = this.myBinsLeftBoundary + binindex*this.myBinWidth;

		// now get a random value between the bin's range
		// basically we need a random value from
		// boundary-left to boundary-right (which is boundary-left+width)
		return binboundaryLeft
			+ this.myBinWidth * arng.nextDouble();
	}
	public	double	bins_left_boundary(){ return this.myBinsLeftBoundary; }
	public	double	bins_right_boundary(){ return this.myBinsRightBoundary; }
	public	int	num_bins(){ return this.myNumBins; }
	public	String	toString(){
		StringBuilder	buffer = new StringBuilder();
		int	N = this.myNumBins, i;
		double	left = this.myBinsLeftBoundary, right = left + this.myBinWidth;
		for(i=0;i<N;i++){
			buffer.append(
				this.myLabels[i]
				+" ("+left+","+right+") = "
				+this.myBins[i]
				+"\n"
			);
			left = right;
			right += this.myBinWidth;
		}
		return buffer.toString();
	}
	public  boolean save_to_file_txt(String afilename) {
		try {
			PrintWriter out =new PrintWriter(afilename);
			int	N = this.myNumBins, i;
			for(i=0;i<N;i++){
				out.println(i+"\t"+this.myBins[i]);
			}
			out.close();
			return true;
		} catch(FileNotFoundException fed){
			System.err.println("Blob2D.save_to_file_txt() : error saving blob to file '"+afilename+"', exception was caught: "+fed);
			fed.printStackTrace();
		}
		return false;
	}
}
class	Histoflatbins2D implements Comparable<Histoflatbins2D> {
	// class representing a bin, i is it's 'number' and v its content/count
	// v is double because this class is used by the cumulative bins
	int	i, j;
	double	v;
	public	Histoflatbins2D(int _i, int _j, double _v){ this.set(_i,_j,_v); }
	public	int	compareTo(Histoflatbins2D another_histoflatbins){
		return this.v - another_histoflatbins.v;
	}
	public	void set(int _i, int _j, double _v){ this.i = _i; this.j = _j; this.v = _v; }
}
static	class Histoflatbins2D_Comparator implements Comparator<Histoflatbins2D> {
	@override
	public int compare(Histoflatbins2D o1, Histoflatbins2D o2) {
 		return o1.compareTo(o2);
	}
}
