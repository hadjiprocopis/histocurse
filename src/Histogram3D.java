package	ahp.org.Histograms;

import	java.util.Random;
import	java.io.FileNotFoundException;
import	java.io.PrintWriter;
import	java.util.Comparator;
// so not thread-safe:
import	java.util.HashMap;
import	java.util.Arrays;

public	class	Histogram3D {
	private	int		myNumBinsX,
				myNumBinsY,
				myNumBinsZ,
				myNumTotalBins;
	private	Histobin3D[][][]	myBins;
	private	double		myBinWidthX,
				myBinWidthY,
				myBinWidthZ;
	private HashMap<String, Histobin3D>	myLabels2Bins;

	// discreet cumulative density function
	// normalises the bins to Sum(counts) = 1 and then 
	// adds the counts
	private	Histobin3D[]	myCumBins;
	// the leftmost and rightmost bin coordinates
	// to define the range of the inputs to the bins
	private double		myBinsXLeftBoundary,
				myBinsXRightBoundary,
				myBinsYLeftBoundary,
				myBinsYRightBoundary,
				myBinsZLeftBoundary,
				myBinsZRightBoundary;
	// a flag which is set every time a value is added or removed
	// it indicates that the bin's predictor must be re-calculated.
	private	boolean		myNeedRecalculate;

	// comparator to sort cumbins array wrt to the cum_count content
	private	Histobin3D_Comparator myCumBinsComparator = new Histobin3D_Comparator();

	// this index will return the index in the cumbins array of the bin where the
	// search for specific cum_count value must start
	// the input should be an integer between 0-9 denoting cum_count value of 0.0 to 0.9
	// and the output will be an integer index - you should start search the cumbins array
	// from that index and below.
	private int[]		myCumBin_cumcount_index;
	// this is the size of the index. Use 1 for 0-9, 2 for 0-99, 3 for 0-999 etc.
	private final int	myCumBin_cumcount_index_num_digits_size = 2;
	private final int	myCumBin_cumcount_index_size = (int )(Math.pow(10, myCumBin_cumcount_index_num_digits_size));

	public	Histogram3D clone(){
		Histogram3D ahist = null;
		try {
			ahist =new Histogram3D(
				this.myNumBinsX, this.myNumBinsY, this.myNumBinsZ,
				this.myBinWidthX, this.myBinWidthY, this.myBinWidthZ,
				this.myBinsXLeftBoundary,
				this.myBinsYLeftBoundary,
				this.myBinsZLeftBoundary
			);
		} catch(Exception ex){ System.err.println("Histogram3D.java : clone() : exception was caught while creating cloned histogram."); return(null); }
		int	i, j, k;
		for(i=this.myNumBinsX;i-->0;){
			for(j=this.myNumBinsX;j-->0;){
				for(k=this.myNumBinsZ;k-->0;){
					this.myBins[i][j][k].copy(ahist.bin(i,j,k));
				}
			}
		}
		ahist.recalculate_predictors(true);
		return ahist;
	}
	public	Histobin3D	bin(int i, int j, int k){ return this.myBins[i][j][k]; }
	public	void	reset(){
		for(int q=this.myNumTotalBins;q-->0;){ this.myCumBins[q].reset(); }
	}
	public	Histogram3D(
		int	num_binsX,
		int	num_binsY,
		int	num_binsZ,
		double	bin_widthX,
		double	bin_widthY,
		double	bin_widthZ,
		double	start_fromX, // bins' leftmost boundary (not center) along X
		double	start_fromY, // bins' leftmost boundary (not center) along Y
		double	start_fromZ // bins' leftmost boundary (not center) along Z
	) throws Exception {
		System.out.println("Histogram3D() : creating ...");
		if( ! this._setup_bins(
			num_binsX, num_binsY, num_binsZ,
			bin_widthX, bin_widthY, bin_widthZ,
			start_fromX, start_fromY, start_fromZ
		) ){
			System.err.println("Histogram3D.java : constructor1 : call to _setup_bins() has failed.");
			throw new Exception("Histogram3D.java : constructor1 : call to _setup_bins() has failed.");
		}
		if( ! this._setup_labels(null) ){
			System.err.println("Histogram3D.java : constructor1 : call to _setup_labels() has failed.");
			throw new Exception("Histogram3D.java : constructor1 : call to _setup_labels() has failed.");
		}
	}
	public	Histogram3D(
		int	num_binsX,
		int	num_binsY,
		int	num_binsZ,
		double	bin_widthX,
		double	bin_widthY,
		double	bin_widthZ,
		double	start_fromX, // bins' leftmost boundary (not center)
		double	start_fromY, // bins' leftmost boundary (not center)
		double	start_fromZ, // bins' leftmost boundary (not center)
		String[][][] labels // optional
	) throws Exception {
		if( ! this._setup_bins(
			num_binsX, num_binsY, num_binsZ,
			bin_widthX, bin_widthY, bin_widthZ,
			start_fromX, start_fromY, start_fromZ
		) ){
			System.err.println("Histogram3D.java : constructor2 : call to _setup_bins() has failed.");
			throw new Exception("Histogram3D.java : constructor2 : call to _setup_bins() has failed.");
		}
		if( ! this._setup_labels(labels) ){
			System.err.println("Histogram3D.java : constructor2 : call to _setup_labels() has failed.");
			throw new Exception("Histogram3D.java : constructor2 : call to _setup_labels() has failed.");
		}
	}

	private	void	_make_default_labels(){
		int	num_binsX = this.myNumBinsX,
			num_binsY = this.myNumBinsY,
			num_binsZ = this.myNumBinsZ,
			i, j, k;
		// automatic labels
		for(i=num_binsX;i-->0;){
			for(j=num_binsY;j-->0;){
				for(k=num_binsZ;k-->0;){
					this.myBins[i][j][k].label = ""+(i+1)+(j+1)+(k+1);
				}
			}
		}
	}
	// this function must be called after bins have been setup (_setup())
	private	boolean	_setup_labels(String[][][] labels){
		int	num_binsX = this.myNumBinsX,
			num_binsY = this.myNumBinsY,
			num_binsZ = this.myNumBinsZ,
			num_bins = this.myNumTotalBins,
			i, j, k;
		Histobin3D	abin;
		if( labels == null ){
			// make default labels
			this._make_default_labels();
		} else {
			if( (labels.length != num_binsX) || (labels[0].length != num_binsY) || (labels[0][0].length != num_binsZ) ){
				System.err.println("Histogram3D.java: _setup() : WARNING : number of bins "+num_binsX+" must be the same as the number of bin labels, "+labels.length+" and "+num_binsY+" the same as "+labels[0].length+". Bin labels will be ignored.");
				return false;
			} else {
				for(i=num_binsX;i-->0;){
					for(j=num_binsY;j-->0;){
						for(k=num_binsZ;k-->0;){
							this.myBins[i][j][k].label = new String(labels[i][j][k]);
						}
					}
				}
			}
		}
		// this is a hashtable mapping labels to bin
		// used for adding to a bin usin a label instead of using bin number
		this.myLabels2Bins = new HashMap<String, Histobin3D>();
		for(i=num_bins;i-->0;){
			abin = this.myCumBins[i];
			this.myLabels2Bins.put(abin.label, abin);
		}
		if( this.myLabels2Bins.size() != num_bins ){
			System.err.println("Histogram3D.java : _setup_labels() : labels must be unique! but this is not the case with specified labels: "+labels);
			return false;
		}
		return true;
	}
	private	boolean	_setup_bins(
		int	num_binsX,
		int	num_binsY,
		int	num_binsZ,
		double	bin_widthX,
		double	bin_widthY,
		double	bin_widthZ,
		// this is not where the first bincentre is!
		// this is where the left boundary of the first bin is:
		double	start_fromX,
		double	start_fromY,
		double	start_fromZ
	){
		this.myNumBinsX = num_binsX;
		this.myNumBinsY = num_binsY;
		this.myNumBinsZ = num_binsZ;
		this.myNumTotalBins = num_binsX * num_binsY * num_binsZ;

		this.myBinsXLeftBoundary = start_fromX;
		this.myBinsYLeftBoundary = start_fromY;
		this.myBinsZLeftBoundary = start_fromZ;
		this.myBinsXRightBoundary = start_fromX + num_binsX * bin_widthX;
		this.myBinsYRightBoundary = start_fromY + num_binsY * bin_widthY;
		this.myBinsZRightBoundary = start_fromZ + num_binsZ * bin_widthZ;

		this.myBinWidthX = bin_widthX;
		this.myBinWidthY = bin_widthY;
		this.myBinWidthZ = bin_widthZ;

		this.myBins = new Histobin3D[num_binsX][][];
		// flat cumbins it is a 1D array of all the cumbins (which is a 2D array)
		// it contains references to the already existing bins
		this.myCumBins = new Histobin3D[this.myNumTotalBins];
		this.myCumBin_cumcount_index = new int[this.myCumBin_cumcount_index_size];

		int i, j, k, q;
		double	lbX = start_fromX,
			rbX = lbX + bin_widthX,
			lbY, rbY, lbZ, rbZ;
		for(i=0,q=0;i<num_binsX;i++){
			this.myBins[i] = new Histobin3D[num_binsY][];
			lbY = start_fromY;
			rbY = lbY + bin_widthY;
			for(j=0;j<num_binsY;j++){
				this.myBins[i][j] = new Histobin3D[num_binsZ];
				lbZ = start_fromY;
				rbZ = lbZ + bin_widthZ;
				for(k=0;k<num_binsZ;k++){
					this.myBins[i][j][k] = new Histobin3D(
						i, j, k,
						0, // the count (initially)
						lbX, rbX,
						lbY, rbY,
						lbZ, rbZ
					);
					// the cumbins is a 1D array (as opposed to the 2D of bins)
					// and it holds references to the myBins
					// we do that in order to sort it wrt cum_count
					this.myCumBins[q++] = this.myBins[i][j][k];
					lbZ += bin_widthZ;
					rbZ += bin_widthZ;
				}
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
		System.out.println("Histogram3D.java : recalculate_predictors(false) starting ...");
		double	totalsum, asum;
		int	NX = this.myNumBinsX,
			NY = this.myNumBinsY,
			NZ = this.myNumBinsZ,
			N = this.myNumTotalBins,
			i, j, k, q;
		for(totalsum=0.0,q=N;q-->0;){
			totalsum += this.myCumBins[q].count;
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
		Histobin3D abin = this.myCumBins[0];
		// the case for k=0:
		abin.norm_count = abin.cum_count = abin.count / totalsum;
		for(q=1;q<N;q++){
			abin = this.myCumBins[q];
			abin.norm_count = abin.count / totalsum;
			abin.cum_count = abin.norm_count + this.myCumBins[q-1].cum_count;
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
				for(k=0;k<NZ;k++){
					// THIS calculates cum_count using the usual method of summing the
					// square before. This is OK but then the order of the cumbins must
					// match the order of the cum summing. and that does not work below:
					abin = this.myBins[i][j];
					// normalised count:
					abin.norm_count = abin.count / totalsum;

					// the cumbins of i,j is formed by the sum of the bins of the rectangle(0,0,i,j) incl.
					// this is the rectangle:
					for(asum=0.0,I=0;I<=i;I++) for(J=0;J<=j;J++) for(K=0;K<=k;K++) asum += this.myBins[I][J][K].count;
					abin.cum_count = asum / totalsum;
				}
			}
		}
*/
		// and now sort the cumbins flat wrt to content (v)
//		Arrays.sort(this.myCumBins, this.myCumBinsComparator);
//		for(Histobin3D shit : this.myCumBins){ System.out.println("shit:"+shit); }
//		System.exit(1);
		this.myNeedRecalculate = false;
	}
	// given a value on the x-axis and one on the y-axis, find which bin it belongs to
	// returns the bin coordinates as a 3-array of int
	private	int[]	get_bin_index(double x, double y, double z){
		return new int[]{
			(int )(Math.floor((x-this.myBinsXLeftBoundary)/this.myBinWidthX)),
			(int )(Math.floor((y-this.myBinsYLeftBoundary)/this.myBinWidthY)),
			(int )(Math.floor((z-this.myBinsZLeftBoundary)/this.myBinWidthZ))
		};
	}
	// given a value on the x-axis and one on the y-axis, find which bin it belongs to
	// and return that bin object
	private	Histobin3D	get_bin(double x, double y, double z){
		return this.myBins
			[(int )(Math.floor((x-this.myBinsXLeftBoundary)/this.myBinWidthX))]
			[(int )(Math.floor((y-this.myBinsYLeftBoundary)/this.myBinWidthY))]
			[(int )(Math.floor((z-this.myBinsZLeftBoundary)/this.myBinWidthZ))]
		;
	}
	// given a bin-label, it returns the bin object
	private	Histobin3D	get_bin(String alabel) throws Exception {
		if( this.myLabels2Bins.containsKey(alabel) == false ){ throw new Exception("Histogram3D.java : get_bin() : could not find label '"+alabel+"' in any of the bins/1 - skipping just this one."); }
		return this.myLabels2Bins.get(alabel);
	}
	// if bin labels are 1 char each (e.g. an alphabet)
	// then it forms the string label and returns the bin object
	private	Histobin3D	get_bin(char label1, char label2, char label3) throws Exception {
		String alabel = String.valueOf(label1)+String.valueOf(label2)+String.valueOf(label3);
		if( this.myLabels2Bins.containsKey(alabel) == false ){ throw new Exception("Histogram3D.java : get_bin() : could not find label '"+alabel+"' (formed by the 3 chars: '"+label1+"', '"+label2+"', "+label3+"') in any of the bins/2 - skipping just this one."); }
		return this.myLabels2Bins.get(alabel);
	}
	// add a value to the corresponding bin - increment its count
	public	void	add(double x, double y, double z){
		this.get_bin(x,y,z).count++;
		this.myNeedRecalculate = true;
	}
	public	void	remove(double x, double y, double z){
		Histobin3D abin = this.get_bin(x,y,z);
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
			System.err.println("Histogram3D.java : add() : exception was caught when called get_bin() for label '"+alabel+"':\n"+ex);
			ex.printStackTrace();
			return false;
		}
	}
	public	boolean	remove(String alabel){
		try {
			Histobin3D abin = this.get_bin(alabel);
			if( abin.count == 0 ){ return true; }
			abin.count--;
			this.myNeedRecalculate = true;
			return true;
		} catch(Exception ex){
			System.err.println("Histogram3D.java : remove() : exception was caught when called get_bin() for label '"+alabel+"':\n"+ex);
			ex.printStackTrace();
			return false;
		}
	}
	// add a value to the corresponding bin - increment its count
	// this is the case where a single char is used as a label, ok shit but...
	public	boolean	add(char alabel1, char alabel2, char alabel3){
		try {
			this.get_bin(alabel1, alabel2, alabel3).count++;
			this.myNeedRecalculate = true;
			return true;
		} catch(Exception ex){
			System.err.println("Histogram3D.java : add() : exception was caught when called get_bin() for label '"+alabel1+alabel2+alabel3+"':\n"+ex);
			ex.printStackTrace();
			return false;
		}		
	}
	public	boolean	remove(char alabel1, char alabel2, char alabel3){
		try {
			Histobin3D abin = this.get_bin(alabel1, alabel2, alabel3);
			if( abin.count == 0 ){ return true; }
			abin.count--;
			this.myNeedRecalculate = true;
			return true;
		} catch(Exception ex){
			System.err.println("Histogram3D.java : remove() : exception was caught when called get_bin() for label '"+alabel1+alabel2+alabel3+"':\n"+ex);
			ex.printStackTrace();
			return false;
		}
	}
	// returns a 2array of random bin index based on bins contents, i.e. most
	// popular bins will have more chance of being selected
	// this is basically an attempt to re-create the distribution
	public	int[]	random_bin_index(Random arng){
		// RNG nextDouble : must be between 0 and 1:
		double anum = arng.nextDouble();
		Histobin3D abin = this.random_bin(arng);
		return new int[]{ abin.i, abin.j, abin.k };
	}
	// return a 3array of a random bin index GIVEN that
	// 1st and 2nd bin coordinates are the ones specified.
	// tries to emulate conditional probability
	public	int[]	random_bin_index(Random arng, int given_bin_index1, int given_bin_index2){
		// RNG nextDouble : must be between 0 and 1:
		double anum = arng.nextDouble();
		Histobin3D abin = this.random_bin(arng, given_bin_index1, given_bin_index2);
		return new int[]{ abin.i, abin.j, abin.k };
	}
	// return a 3array of a random bin index GIVEN that
	// 1st bin coordinates are the ones specified.
	// tries to emulate conditional probability
	public	int[]	random_bin_index(Random arng, int given_bin_index1){
		// RNG nextDouble : must be between 0 and 1:
		double anum = arng.nextDouble();
		Histobin3D abin = this.random_bin(arng, given_bin_index1);
		return new int[]{ abin.i, abin.j, abin.k };
	}
	public	Histobin3D	random_bin(Random arng){
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
	// returns a bin3D given that the first and second bins are the one specified
	// tries to simulate a conditional probability...
	public  Histobin3D      random_bin(Random arng, int given_bin_index1, int given_bin_index2){
		Histobin3D      abin = null;
// PROBLEM! what if combination does not exist? endless loop
		do {
			abin = this.random_bin(arng);
System.out.println("random_bin() : checking if "+abin.i+"=="+given_bin_index1+", "+abin.j+"=="+given_bin_index2);
		} while( (abin.i != given_bin_index1) || (abin.j != given_bin_index2) );
		return abin;
	}
	// returns a bin3D given that the first and second bins are the one specified
	// tries to simulate a conditional probability...
	public  Histobin3D      random_bin(Random arng, int given_bin_index1){
		Histobin3D      abin = null;
		do {
			abin = this.random_bin(arng);
		} while( abin.i != given_bin_index1 );
		return abin;
	}
	// get a random bin and then get a random number within the range of that bin
	// which of course is 2D so the returned value is a 2array of (x,y)
	public	double[]	random_value(Random arng){
		Histobin3D	abin = this.random_bin(arng);
		double	valX, valY, valZ;
		valX = abin.left_boundaryX + this.myBinWidthX * arng.nextDouble();
		valY = abin.left_boundaryY + this.myBinWidthY * arng.nextDouble();
		valZ = abin.left_boundaryZ + this.myBinWidthZ * arng.nextDouble();
		return new double[]{valX, valY, valZ};
	}
	// get a random bin and then get a random number within the range of that bin
	// which of course is 2D so the returned value is a 2array of (x,y)
	// get a random bin and then get a random number within the range of that bin
	// which of course is 2D so the returned value is a 2array of (x,y)
	// GIVEN that the first bin index is the one specified,
	// poor attempt to simulate conditional probability
	public	double[]	random_value(Random arng, int given_bin_index){
		Histobin3D	abin = this.random_bin(arng, given_bin_index);
		double	valX, valY, valZ;
		valX = abin.left_boundaryX + this.myBinWidthX * arng.nextDouble();
		valY = abin.left_boundaryY + this.myBinWidthY * arng.nextDouble();
		valZ = abin.left_boundaryZ + this.myBinWidthZ * arng.nextDouble();
		return new double[]{valX, valY, valZ};
	}
	// get a random bin and then get a random number within the range of that bin
	// which of course is 2D so the returned value is a 2array of (x,y)
	// get a random bin and then get a random number within the range of that bin
	// which of course is 2D so the returned value is a 2array of (x,y)
	// GIVEN that the first AND second bin index are the ones specified,
	// poor attempt to simulate conditional probability
	public	double[]	random_value(Random arng, int given_bin_index1, int given_bin_index2){
		Histobin3D	abin = this.random_bin(arng, given_bin_index1, given_bin_index2);
		double	valX, valY, valZ;
		valX = abin.left_boundaryX + this.myBinWidthX * arng.nextDouble();
		valY = abin.left_boundaryY + this.myBinWidthY * arng.nextDouble();
		valZ = abin.left_boundaryZ + this.myBinWidthZ * arng.nextDouble();
		return new double[]{valX, valY, valZ};
	}
	public	double	bins_left_boundaryX(){ return this.myBinsXLeftBoundary; }
	public	double	bins_right_boundaryX(){ return this.myBinsXRightBoundary; }
	public	double	bins_left_boundaryY(){ return this.myBinsYLeftBoundary; }
	public	double	bins_right_boundaryY(){ return this.myBinsYRightBoundary; }
	public	double	bins_left_boundaryZ(){ return this.myBinsZLeftBoundary; }
	public	double	bins_right_boundaryZ(){ return this.myBinsZRightBoundary; }
	public	int	num_binsX(){ return this.myNumBinsX; }
	public	int	num_binsY(){ return this.myNumBinsY; }
	public	int	num_binsZ(){ return this.myNumBinsZ; }
	public	int	num_total_bins(){ return this.myNumTotalBins; }
	public	String	toString(){
		StringBuilder	buffer = new StringBuilder();
		int	NX = this.myNumBinsX,
			NY = this.myNumBinsY,
			NZ = this.myNumBinsZ,
			i, j, k;
		Histobin3D	abin;
		this.recalculate_predictors(false);
		for(i=0;i<NX;i++){
			for(j=0;j<NY;j++){
				for(k=0;k<NZ;k++){
					abin = this.myBins[i][j][k];
					buffer.append(
						abin.label
						+" ("
							+abin.left_boundaryX+","+abin.right_boundaryX
							+","+abin.left_boundaryY+","+abin.right_boundaryY
							+","+abin.left_boundaryZ+","+abin.right_boundaryZ
						+") = "
						+abin.count
						+"\n"
					);
				}
			}
		}
		return buffer.toString();
	}
	public	void save_to_file_txt(String afilename) throws Exception { this.save_to_file_txt(afilename, "\t"); }
	public	void save_to_file_txt(String afilename, String sep) throws Exception {
		this.recalculate_predictors(false);

		PrintWriter out =new PrintWriter(afilename);
		int	NX = this.myNumBinsX,
			NY = this.myNumBinsY,
			NZ = this.myNumBinsZ,
			i, j, k;
		Histobin3D	abin;
		for(i=0;i<NX;i++){
			for(j=0;j<NY;j++){
				for(k=0;k<NZ;k++){
					abin = this.myBins[i][j][k];
					out.println(abin.toString_as_histoplot(sep));
				}
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
class Histobin3D_Comparator implements Comparator<Histobin3D> {
	public int compare(Histobin3D o1, Histobin3D o2) {
 		return o1.compareTo(o2);
	}
}
