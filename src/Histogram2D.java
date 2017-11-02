package	ahp.org.Histograms;

import	java.util.Random;
import	java.io.FileNotFoundException;
import	java.io.PrintWriter;
import	java.util.Comparator;
// so not thread-safe:
import	java.util.HashMap;
import	java.util.Arrays;

public	class	Histogram2D {
	private	int		myNumBinsX,
				myNumBinsY,
				myNumTotalBins;
	private	Histobin2D[][]	myBins;
	private	double		myBinWidthX,
				myBinWidthY;
	// It maps labels to bins. It can be used to find a bin
	// given its label.
	// this helps to add/remove using a bin-label instead of a value
	// which finds which bin to go to.
	private HashMap<String, Histobin2D>   myLabels2Bins;

	// discreet cumulative density function
	// normalises the bins to Sum(counts) = 1 and then 
	// adds the counts
	private	Histobin2D[]	myCumBins;
	// the leftmost and rightmost bin coordinates
	// to define the range of the inputs to the bins
	private double		myBinsXLeftBoundary,
				myBinsXRightBoundary,
				myBinsYLeftBoundary,
				myBinsYRightBoundary;
	// a flag which is set every time a value is added or removed
	// it indicates that the bin's predictor must be re-calculated.
	private	boolean		myNeedRecalculate;

	// comparator to sort cumbins array wrt to the cum_count content
	private	Histobin2D_Comparator myCumBinsComparator = new Histobin2D_Comparator();

	// this index will return the index in the cumbins array of the bin where the
	// search for specific cum_count value must start
	// the input should be an integer between 0-9 denoting cum_count value of 0.0 to 0.9
	// and the output will be an integer index - you should start search the cumbins array
	// from that index and below.
	private int[]		myCumBin_cumcount_index;
	// this is the size of the index. Use 1 for 0-9, 2 for 0-99, 3 for 0-999 etc.
	private final int	myCumBin_cumcount_index_num_digits_size = 2;
	private final int	myCumBin_cumcount_index_size = (int )(Math.pow(10, myCumBin_cumcount_index_num_digits_size));

	public	Histogram2D clone(){
		Histogram2D ahist = null;
		try {
			ahist =new Histogram2D(
				this.myNumBinsX, this.myNumBinsY,
				this.myBinWidthX, this.myBinWidthY,
				this.myBinsXLeftBoundary, myBinsYLeftBoundary
			);
		} catch(Exception ex){ System.err.println("Histogram2D.java : clone() : exception was caught while creating cloned histogram."); return(null); }
		int	i, j;
		for(i=this.myNumBinsX;i-->0;){
			for(j=this.myNumBinsX;j-->0;){
				this.myBins[i][j].copy(ahist.bin(i,j));
			}
		}
		ahist.recalculate_predictors(true);
		return ahist;
	}
	public	Histobin2D	bin(int i, int j){ return this.myBins[i][j]; }
	public	void	reset(){
		for(int i=this.myNumTotalBins;i-->0;){ this.myCumBins[i].reset(); }
	}
	public	Histogram2D(
		int	num_binsX,
		int	num_binsY,
		double	bin_widthX,
		double	bin_widthY,
		double	start_fromX, // bins' leftmost boundary (not center)
		double	start_fromY // bins' leftmost boundary (not center)
	) throws Exception {
		System.out.println("Histogram1D() : creating ...");
		if( ! this._setup_bins(num_binsX, num_binsY, bin_widthX, bin_widthY, start_fromX, start_fromY) ){
			System.err.println("Histogram2D.java : constructor1 : call to _setup_bins() has failed.");
			throw new Exception("Histogram2D.java : constructor1 : call to _setup_bins() has failed.");
		}
		if( ! this._setup_labels(null) ){
			System.err.println("Histogram2D.java : constructor1 : call to _setup_labels() has failed.");
			throw new Exception("Histogram2D.java : constructor1 : call to _setup_labels() has failed.");
		}
	}
	public	Histogram2D(
		int	num_binsX,
		int	num_binsY,
		double	bin_widthX,
		double	bin_widthY,
		double	start_fromX, // bins' leftmost boundary (not center)
		double	start_fromY, // bins' leftmost boundary (not center)
		String[][] labels // optional
	) throws Exception {
		if( ! this._setup_bins(num_binsX, num_binsY, bin_widthX, bin_widthY, start_fromX, start_fromY) ){
			System.err.println("Histogram2D.java : constructor2 : call to _setup_bins() has failed.");
			throw new Exception("Histogram2D.java : constructor2 : call to _setup_bins() has failed.");
		}
		if( ! this._setup_labels(labels) ){
			System.err.println("Histogram2D.java : constructor2 : call to _setup_labels() has failed.");
			throw new Exception("Histogram2D.java : constructor2 : call to _setup_labels() has failed.");
		}
	}

	private	void	_make_default_labels(){
		int	num_binsX = this.myNumBinsX;
		int	num_binsY = this.myNumBinsY, i, j;
		// automatic labels
		for(i=num_binsX;i-->0;){
			for(j=num_binsY;j-->0;){
				this.myBins[i][j].label = ""+(i+1)+(j+1);
			}
		}
	}
	// this function must be called after bins have been setup (_setup())
	private	boolean	_setup_labels(String[][] labels){
		int	num_binsX = this.myNumBinsX;
		int	num_binsY = this.myNumBinsY, i, j;
		int	num_bins = this.myNumTotalBins;
		Histobin2D	abin;
		if( labels == null ){
			// make default labels
			this._make_default_labels();
		} else {
			if( (labels.length != num_binsX) || (labels[0].length != num_binsY) ){
				System.err.println("Histogram2D.java: _setup() : WARNING : number of bins "+num_binsX+" must be the same as the number of bin labels, "+labels.length+" and "+num_binsY+" the same as "+labels[0].length+". Bin labels will be ignored.");
				return false;
			} else {
				for(i=num_binsX;i-->0;){
					for(j=num_binsY;j-->0;){
						this.myBins[i][j].label = new String(labels[i][j]);
					}
				}
			}
		}
		// this is a HashMap mapping labels to bin
		// used for adding to a bin usin a label instead of using bin number
                this.myLabels2Bins = new HashMap<String, Histobin2D>();
		for(i=num_bins;i-->0;){
			abin = this.myCumBins[i];
			this.myLabels2Bins.put(abin.label, abin);
		}
		if( this.myLabels2Bins.size() != num_bins ){
			System.err.println("Histogram2D.java : _setup_labels() : labels must be unique! but this is not the case with specified labels: "+labels);
			return false;
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

		this.myBins = new Histobin2D[num_binsX][];
		// flat cumbins it is a 1D array of all the cumbins (which is a 2D array)
		// it contains references to the already existing bins
		this.myCumBins = new Histobin2D[this.myNumTotalBins];
		this.myCumBin_cumcount_index = new int[this.myCumBin_cumcount_index_size];

		int i, j, k;
		double	lbX = start_fromX,
			rbX = lbX + bin_widthX,
			lbY, rbY;
		for(i=0,k=0;i<num_binsX;i++){
			this.myBins[i] = new Histobin2D[num_binsY];
			lbY = start_fromY;
			rbY = lbY + bin_widthY;
			for(j=0;j<num_binsY;j++){
				this.myBins[i][j] = new Histobin2D(
					i, j,
					0,
					lbX, rbX,
					lbY, rbY
				);
				// the cumbins is a 1D array (as opposed to the 2D of bins)
				// and it holds references to the myBins
				// we do that in order to sort it wrt cum_count
				this.myCumBins[k++] = this.myBins[i][j];
				lbY += bin_widthY;
				rbY += bin_widthY;
			}
			lbX += bin_widthX;
			rbX += bin_widthX;
		}
		this.myNeedRecalculate = false;

		return true;
	}
	// will construct the discreet cumulative density function
	// which will be used to output a random value based on the distribution
	// represented by this histogram. Every time a bin changes, this
	// needs to be recalculated
	// but it is better to have a flag and whenever a prediction is required
	// then this function maybe called.
	private	void	recalculate_predictors(boolean force){
		if( (this.myNeedRecalculate == false) && (force == false) ){ return; }
		boolean	debug = false;
		double	totalsum, asum;
		int	NX = this.myNumBinsX,
			NY = this.myNumBinsY,
			N = this.myNumTotalBins,
			i, j, I, J, k;
		for(totalsum=0.0,k=N;k-->0;){
			totalsum += this.myCumBins[k].count;
		}

		// basically what we do in order to get a random bin proportional to the histogram's PDF
		// is to put all the bins side-by-side their length representing their count
		// just like sticks of wood.
		// then throw a uniform-random stone on them and see where it lands.
		// most of the times will land on the most-counts stick of wood (or bin)
		// monte carlo.
		// but we have a bit of a problem in finding where the stone lands (in computers only)
		// so we put all the bins side-by-side and order is not important (e.g. a simple i-j loop over all bins)
		// this creates the CumBins array.
		// then in this array we sum the normalised counts (norm_count) to get the cumulative count
		// each bin with all its previous (on the left). Cum count finally arrives at 1.
		// and so get a uniform-random number (from 0 to 1), then start going
		// down the array of CumBins checking if that number is > than the current bin cum bin
		// and when it is then we have the bin we have landed to, and so then get its
		// value or a random value within its bounds.
		Histobin2D abin = this.myCumBins[0];
		// the case for k=0:
		abin.norm_count = abin.cum_count = abin.count / totalsum;
		for(k=1;k<N;k++){
			abin = this.myCumBins[k];
			abin.norm_count = abin.count / totalsum;
			abin.cum_count = abin.norm_count + this.myCumBins[k-1].cum_count;
		}
		// now build an index of cum_count values for the cumbins array
		// so that given a cumcount value we get an index in cumbins array
		// and search only from that index downwards.
		// indices are start-0
		int	ci, oldci = 0;
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

/*		for(i=0;i<NX;i++){
			for(j=0;j<NY;j++){
				// THIS calculates cum_count using the usual method of summing the
				// square before. This is OK but then the order of the cumbins must
				// match the order of the cum summing. and that does not work below:
				abin = this.myBins[i][j];
				// normalised count:
				abin.norm_count = abin.count / totalsum;

				// the cumbins of i,j is formed by the sum of the bins of the rectangle(0,0,i,j) incl.
				// this is the rectangle:
				for(asum=0.0,I=0;I<=i;I++) for(J=0;J<=j;J++) asum += this.myBins[I][J].count;
				abin.cum_count = asum / totalsum;
			}
		}
*/
		// and now sort the cumbins flat wrt to content (v)
//		Arrays.sort(this.myCumBins, this.myCumBinsComparator);
//		for(Histobin2D shit : this.myCumBins){ System.out.println("shit:"+shit); }
//		System.exit(1);
		this.myNeedRecalculate = false;
	}
	// given a value on the x-axis and one on the y-axis, find which bin it belongs to
	// returns the bin coordinates as a 2-array of int
	private	int[]	get_bin_index(double x, double y){
		return new int[]{
			(int )(Math.floor((x-this.myBinsXLeftBoundary)/this.myBinWidthX)),
			(int )(Math.floor((y-this.myBinsYLeftBoundary)/this.myBinWidthY))
		};
	}
	// given a value on the x-axis and one on the y-axis, find which bin it belongs to
	// and return that bin object
	private	Histobin2D	get_bin(double x, double y){
		return this.myBins
			[(int )(Math.floor((x-this.myBinsXLeftBoundary)/this.myBinWidthX))]
			[(int )(Math.floor((y-this.myBinsYLeftBoundary)/this.myBinWidthY))]
		;
	}
	// given a bin-label, it returns the bin object
	private	Histobin2D	get_bin(String alabel) throws Exception {
		if( this.myLabels2Bins.containsKey(alabel) == false ){ throw new Exception("Histogram2D.java : get_bin() : could not find label '"+alabel+"' in any of the bins/1 - skipping just this one."); }
		return this.myLabels2Bins.get(alabel);
	}
	// if bin labels are 1 char each (e.g. an alphabet)
	// then it forms the string label and returns the bin object
	private	Histobin2D	get_bin(char label1, char label2) throws Exception {
		String	alabel = String.valueOf(label1)+String.valueOf(label2);
		if( this.myLabels2Bins.containsKey(alabel) == false ){ throw new Exception("Histogram2D.java : get_bin() : could not find label '"+alabel+"' (formed by the 2 chars: '"+label1+"', '"+label2+"') in any of the bins/2 - skipping just this one."); }
		return this.myLabels2Bins.get(alabel);
	}
	// add a value to the corresponding bin - increment its count
	public	void	add(double x, double y){
		Histobin2D abin = this.get_bin(x,y);
		abin.count++;
		this.myNeedRecalculate = true;
	}
	public	void	remove(double x, double y){
		Histobin2D abin = this.get_bin(x,y);
		if( abin.count == 0 ){ return; }
		abin.count--;
		this.myNeedRecalculate = true;
	}
	// add a value to the corresponding bin - increment its count
	public	boolean	add(String alabel){
		try {
			this.get_bin(alabel).count++;
			this.myNeedRecalculate = true;
			return true;
		} catch(Exception ex){
			System.err.println("Histogram2D.java : add() : exception was caught when called get_bin() for label '"+alabel+"':\n"+ex);
			return false;
		}
	}
	public	boolean	remove(String alabel){
		try {
			Histobin2D abin = this.get_bin(alabel);
			if( abin.count == 0 ){ return true; }
			abin.count--;
			this.myNeedRecalculate = true;
			return true;
		} catch(Exception ex){
			System.err.println("Histogram2D.java : remove() : exception was caught when called get_bin() for label '"+alabel+"':\n"+ex);
			return false;
		}
	}
	// this is the case where a single char is used as a label, ok shit but...
	public	boolean	add(char alabel1, char alabel2){
		try {
			this.get_bin(alabel1, alabel2).count++;
			this.myNeedRecalculate = true;
			return true;
		} catch(Exception ex){
			System.err.println("Histogram2D.java : add() : exception was caught when called get_bin() for label '"+String.valueOf(alabel1)+String.valueOf(alabel2)+"':\n"+ex);
			return false;
		}
	}
	public	boolean	remove(char alabel1, char alabel2){
		try {
			Histobin2D abin = this.get_bin(alabel1, alabel2);
			if( abin.count == 0 ){ return true; }
			abin.count--;
			this.myNeedRecalculate = true;
			return true;
		} catch(Exception ex){
			System.err.println("Histogram2D.java : remove() : exception was caught when called get_bin() for label '"+String.valueOf(alabel1)+String.valueOf(alabel2)+"':\n"+ex);
			return false;
		}
	}
	// returns a 2array of random bin index based on bins contents, i.e. most
	// popular bins will have more chance of being selected
	// this is basically an attempt to re-create the distribution
	public	int[]	random_bin_index(Random arng){
		// RNG nextDouble : must be between 0 and 1:
		double anum = arng.nextDouble();
		Histobin2D abin = this.random_bin(arng);
		return new int[]{ abin.i, abin.j };
	}
	// return a 2array of random bin index given that the first index is the
	// one specified
	public	int[]	random_bin_index(Random arng, int given_bin_index){
		// RNG nextDouble : must be between 0 and 1:
		double anum = arng.nextDouble();
		Histobin2D abin = this.random_bin(arng, given_bin_index);
		return new int[]{ abin.i, abin.j };
	}
	// returns a bin2D given that the first bin is the one specified
	// tries to simulate a conditional probability...
	public	Histobin2D	random_bin(Random arng, int given_bin_index){
		Histobin2D	abin = null;
		do {
			abin = this.random_bin(arng);
//			System.out.println("Checking if "+abin.i+"=="+given_bin_index);
		} while( abin.i != given_bin_index );
		return abin;
	}
	public	Histobin2D	random_bin(Random arng){
		this.recalculate_predictors(false);
		boolean	debug = false;
		// RNG nextDouble : must be between 0 and 1:
		double anum = arng.nextDouble();

	       int     ci = (int )(anum * this.myCumBin_cumcount_index_size),
			civ = this.myCumBin_cumcount_index[ci], civ_final, i;
		// civ tells us that the cumcount we are looking for is BELOW (i.e. it stops at civ)
		if( debug ) System.out.println("Indices (are start-0) ci="+ci+" and civ="+civ);
		// indices in the index are start-0, so inclusive:
		for(i=civ;i-->0;){
			if( debug ) System.out.println("Searching for "+anum+" < "+this.myCumBins[i].cum_count+" at bin="+i+", civ="+civ);
			if( anum >= this.myCumBins[i].cum_count ){
				// now we are in here one too-many
				civ_final = i+1;
				if( debug ) System.out.println("search : for anum="+anum+" found bin="+civ_final+" (starts from 0), cum_count="+this.myCumBins[civ_final].cum_count+". Anum must be greater than the next bin down: "+this.myCumBins[((civ_final-1)<=0?0:(civ_final-1))].cum_count);
				return this.myCumBins[civ_final];
			}
		}
		return this.myCumBins[0];
	}
	// get a random bin and then get a random number within the range of that bin
	// which of course is 2D so the returned value is a 2array of (x,y)
	public	double[]	random_value(Random arng){
		Histobin2D	abin = this.random_bin(arng);
		double	valX, valY;
		valX = abin.left_boundaryX + this.myBinWidthX * arng.nextDouble();
		valY = abin.left_boundaryY + this.myBinWidthY * arng.nextDouble();
		return new double[]{valX, valY};
	}
	// get a random bin and then get a random number within the range of that bin
	// which of course is 2D so the returned value is a 2array of (x,y)
	// GIVEN that the first bin index is the one specified,
	// poor attempt to simulate conditional probability
	public	double[]	random_value(Random arng, int given_bin_index){
		Histobin2D	abin = this.random_bin(arng, given_bin_index);
		double	valX, valY;
		valX = abin.left_boundaryX + this.myBinWidthX * arng.nextDouble();
		valY = abin.left_boundaryY + this.myBinWidthY * arng.nextDouble();
		return new double[]{valX, valY};
	}
	public	double	bins_left_boundaryX(){ return this.myBinsXLeftBoundary; }
	public	double	bins_right_boundaryX(){ return this.myBinsXRightBoundary; }
	public	double	bins_left_boundaryY(){ return this.myBinsYLeftBoundary; }
	public	double	bins_right_boundaryY(){ return this.myBinsYRightBoundary; }
	public	int	num_binsX(){ return this.myNumBinsX; }
	public	int	num_binsY(){ return this.myNumBinsY; }
	public	int	num_total_bins(){ return this.myNumTotalBins; }
	public	String	toString(){
		StringBuilder	buffer = new StringBuilder();
		int	NX = this.myNumBinsX,
			NY = this.myNumBinsY,
			i, j;
		Histobin2D	abin;
		this.recalculate_predictors(false);
		for(i=0;i<NX;i++){
			for(j=0;j<NY;j++){
				abin = this.myBins[i][j];
				buffer.append(
					abin.label
					+" ("
						+abin.left_boundaryX+","+abin.right_boundaryX
						+","+abin.left_boundaryY+","+abin.right_boundaryY
					+") = "
					+abin.count
					+"\n"
				);
			}
		}
		return buffer.toString();
	}
	public	void save_to_file_txt(String afilename) throws Exception { this.save_to_file_txt(afilename, "\t"); }
	public	void save_to_file_txt(String afilename, String sep) throws Exception {
		this.recalculate_predictors(false);
		Histobin2D	abin;
		PrintWriter out =new PrintWriter(afilename);
		int     NX = this.myNumBinsX,
			NY = this.myNumBinsY,
			i, j;
		for(i=0;i<NX;i++){
			for(j=0;j<NY;j++){
				abin = this.myBins[i][j];
				out.println(abin.toString_as_histoplot(sep));
			}
		}
		out.close();
	}
	public	void dump_random_values_to_file(String afilename, int num_values) throws Exception {
		this.recalculate_predictors(false);

		PrintWriter out =new PrintWriter(afilename);
		Random arng = new Random();
		double[] arv;
		for(int i=num_values;i-->0;){
			arv = this.random_value(arng);
			out.println(arv[0]+"\t"+arv[1]);
		}
		out.close();
	}
}
class Histobin2D_Comparator implements Comparator<Histobin2D> {
	public int compare(Histobin2D o1, Histobin2D o2) {
 		return o1.compareTo(o2);
	}
}
