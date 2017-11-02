import	ahp.org.Histograms.*;
import	ahp.org.Containers.*;

import	java.util.Random;
import  java.io.PrintWriter;

import	org.apache.commons.math3.distribution.NormalDistribution;
import	org.apache.commons.math3.distribution.BetaDistribution;
import	org.apache.commons.math3.distribution.AbstractRealDistribution;

public	class	TestHistograms_SparseArray_randombins {
	public	static void main(String[] args){
		int	num_samples = 1000000,
			num_samples_repopulate = 1000000;

		// so many bins along each dimension:
		int	num_bins[] = new int[]{50,50,50};
		// the width of the bins along each dimesnion:
		double	bin_widths[] = new double[]{1, 1, 1};
		// the histogram's boundary start from here (along each dimension):
		double	boundaries_from[] = new double[]{0, 0, 0};

		int	num_dims = num_bins.length;

		int	i;

		// in larger dims, saving hists to file is not practical...
		boolean	doNotSaveDataToFiles = num_dims > 3;

		if( bin_widths.length != num_dims ){ System.err.println("TestHistograms_SparseArray_randombins.java : main() : bin_widths must have "+num_dims+" dimensions."); System.exit(1); }
		if( boundaries_from.length != num_dims ){ System.err.println("TestHistograms_SparseArray_randombins.java : main() : boundaries_from must have "+num_dims+" dimensions."); System.exit(1); }

		String	outfile;
		PrintWriter	shitprintw;

		long	nanotime_started = System.nanoTime();
		// here we create histograms and then we populate them from a distribution
		Histogram<SparseArray<Histobin>> hN;
		if( (hN=make_histogram_and_fill_from_NORMAL_distribution(
			"hN",
			num_bins, bin_widths, boundaries_from,
			num_samples,
			doNotSaveDataToFiles ? null : "test"+num_dims+"D_NORMALDistribution" // out basename
		)) == null ){ System.err.println("TestHistograms_SparseArray_randombins.java : main() : call to make_histogram_and_fill_from_NORMAL_distribution() has failed."); System.exit(1); }

		if( doNotSaveDataToFiles == false ){
			try {
				outfile = "test"+num_dims+"D_NORMALDistribution_histogram_dump.txt";
				shitprintw = new PrintWriter(outfile);
				shitprintw.println(hN.toString());
				shitprintw.close();
				System.out.println("TestHistograms_SparseArray_randombins.java : main() : first histogram written to file '"+outfile+"'.");
			} catch(Exception ex){ System.err.println("TestHistograms_SparseArray_randombins.java : main() : exception was caught:\n"+ex); ex.printStackTrace(); System.exit(1); }
		}

		// here we create a fresh histogram and then ask the old histograms
		// to give us a random value (based on their own distribution)
		// the new histogram should then exhibit the same distribution as the existing histogram
		Histogram<SparseArray<Histobin>> regeneratedHist;
		if( (regeneratedHist=populate_histogram_from_existing_histogram_and_save(
			"regeneratedHist",
			hN,
			num_samples_repopulate,
			doNotSaveDataToFiles ? null : "test"+num_dims+"D_repopulateFromNORMALHistogram"
		)) == null ){ System.err.println("TestHistograms_SparseArray_randombins.java : main() : call to populate_histogram_from_existing_histogram_and_save() has failed."); System.exit(1); }

		if( doNotSaveDataToFiles == false ){
			try {
				outfile = "test"+num_dims+"D_repopulateFromNormalHistogram_histogram_dump.txt";
				shitprintw = new PrintWriter(outfile);
				shitprintw.println(hN.toString());
				shitprintw.close();
				System.out.println("TestHistograms_SparseArray_randombins.java : main() : first histogram written to file '"+outfile+"'.");
			} catch(Exception ex){ System.err.println("TestHistograms_SparseArray_randombins.java : main() : exception was caught:\n"+ex); ex.printStackTrace(); System.exit(1); }
		}

		try {
			System.out.println("TestHistograms_SparseArray_randombins.java : similarity between original and created histograms is:"
			  +"\nwith 0 selectors: L2="+hN.difference_in_normalised_count_L2(regeneratedHist)
			  +"\nwith 0 selectors: KL(kullback)="+hN.difference_in_normalised_count_KULLBACK(regeneratedHist)
			  +"\nwith 0 selectors: KL-SYMMETRIC="+hN.difference_in_normalised_count_KULLBACK_SYMMETRIC(regeneratedHist)
			  +"\nWARNING: negative KL will occur when bin counts is zero..."
			);
		} catch(Exception ex){ System.err.println("TestHistograms_SparseArray_randombins.java : main() : exception was caught:\n"+ex); ex.printStackTrace(); System.exit(1); }
	

		// Test conditional probability, specify a 'GIVEN' as the first bin dimension
		Histogram<SparseArray<Histobin>>	regeneratedHist_fix1D = null,
						regeneratedHist_fix2D = null;
		Random myRNG = new Random(1234);
		int	NUM_MAX_TRIALS_SELECTION = 10000;
		try {
			HistogramSampler sampler_fix1D = new HistogramSampler(
				"sampler_fix1D",
				hN, // histogram to sample from
				myRNG
			);

			int trials = 0;
			String[] bin_selector_fix1D = new String[num_dims];
			for(i=0;i<num_dims;i++){ bin_selector_fix1D[i] = "*"; }

			do {
				// any bin really but in the middle you get the nice bell-curve
				bin_selector_fix1D[0] = Integer.toString((int )(myRNG.nextInt(num_bins[0])));
			} while( (trials++<NUM_MAX_TRIALS_SELECTION)
				&& (sampler_fix1D.select_bins_using_wildcard_labels(bin_selector_fix1D) == false) );
			if( trials >= NUM_MAX_TRIALS_SELECTION ){ throw new Exception("fix1D: failed to pick a random bin-selector with some bins in it after "+NUM_MAX_TRIALS_SELECTION+" trials !!!"); }

			HistogramSampler sampler_fix2D = new HistogramSampler(
				"sampler_fix2D",
				hN, // histogram to sample from
				myRNG
			);

			trials = 0;
			String[] bin_selector_fix2D = new String[num_dims];
			for(i=0;i<num_dims;i++){ bin_selector_fix2D[i] = "*"; }
			do {
				bin_selector_fix2D[0] = Integer.toString((int )(myRNG.nextInt(num_bins[0])));
				bin_selector_fix2D[1] = Integer.toString((int )(myRNG.nextInt(num_bins[1])));
			} while( (trials++<NUM_MAX_TRIALS_SELECTION) && (sampler_fix2D.select_bins_using_wildcard_labels(bin_selector_fix2D) == false) );
			if( trials >= NUM_MAX_TRIALS_SELECTION ){ throw new Exception("fix2D: failed to pick a random bin-selector with some bins in it after "+NUM_MAX_TRIALS_SELECTION+" trials !!!"); }

			System.out.println("Calling populate_histogram_from_existing_histogram_and_save() with this bin selector (1 dimension fixed): "+Histogram.print_bin_selector_spec(bin_selector_fix1D));
			if( (regeneratedHist_fix1D=populate_histogram_from_existing_histogram_and_save(
				"regeneratedHist_fix1D",
				hN,
				num_samples_repopulate,
				sampler_fix1D,
				"test"+num_dims+"D_repopulateFromNORMALHistogram_conditional_fix1Dimension"
			)) == null ){ System.err.println("TestHistograms_SparseArray_randombins.java : main() : call to populate_histogram_from_existing_histogram_and_save() has failed (2)."); System.exit(1); }

			System.out.println("Calling populate_histogram_from_existing_histogram_and_save() with this bin selector (2 dimensions fixed): "+Histogram.print_bin_selector_spec(bin_selector_fix2D));
			if( (regeneratedHist_fix2D=populate_histogram_from_existing_histogram_and_save(
				"regeneratedHist_fix2D",
				hN,
				num_samples_repopulate,
				sampler_fix2D,
				"test"+num_dims+"D_repopulateFromNORMALHistogram_conditional_fix2Dimensions"
			)) == null ){ System.err.println("TestHistograms_SparseArray_randombins.java : main() : call to populate_histogram_from_existing_histogram_and_save() has failed (2)."); System.exit(1); }

			System.out.println("TestHistograms_SparseArray_randombins.java : similarity between original and created histograms is:"
			  +"\nwith 0 selectors: L2="+hN.difference_in_normalised_count_L2(regeneratedHist)
			  +"\nwith 1 selector: L2="+hN.difference_in_normalised_count_L2(regeneratedHist_fix1D)
			  +(num_dims==3?"\nwith 2 selectors: L2="+hN.difference_in_normalised_count_L2(regeneratedHist_fix2D):"")
			  +"\nwith 0 selectors: KL(kullback)="+hN.difference_in_normalised_count_KULLBACK(regeneratedHist)
			  +"\nwith 1 selector: KL(kullback)="+hN.difference_in_normalised_count_KULLBACK(regeneratedHist_fix1D)
			  +(num_dims==3?"\nwith 2 selectors: KL(kullback)="+hN.difference_in_normalised_count_KULLBACK(regeneratedHist_fix2D):"")
			  +"\nwith 0 selectors: KL-SYMMETRIC="+hN.difference_in_normalised_count_KULLBACK_SYMMETRIC(regeneratedHist)
			  +"\nwith 1 selector: KL-SYMMETRIC="+hN.difference_in_normalised_count_KULLBACK_SYMMETRIC(regeneratedHist_fix1D)
			  +(num_dims==3?"\nwith 2 selectors: KL-SYMMETRIC="+hN.difference_in_normalised_count_KULLBACK_SYMMETRIC(regeneratedHist_fix2D):"")
			  +"\nWARNING: negative KL will occur when bin counts is zero..."
		);
		} catch(Exception ex){ System.out.println("TestHistograms_SparseArray_randombins.java : main() : exception was caught:\n"+ex); ex.printStackTrace(); System.exit(1); }
		System.out.println("TestHistograms_SparseArray_randombins.java : done, total time taken : "+((System.nanoTime()-nanotime_started)/1000000)+" milli seconds.");
	}

	private	static	Histogram<SparseArray<Histobin>> make_histogram_and_fill_from_NORMAL_distribution(
		String histogram_name,
		int[] numBins,
		double[]	binWidths,
		double[]	boundariesFrom,
		int num_samples,
		String	out_basename // leave null if don't want to write out data
	){
		System.out.println("make_histogram_and_fill_from_NORMAL_distribution : starting for '"+histogram_name+"' ...");

		int	num_dims = numBins.length, i;
		long	nanotime_started = System.nanoTime();
		Histogram<SparseArray<Histobin>> ahist = make_histogram(histogram_name, numBins, binWidths, boundariesFrom);
		org.apache.commons.math3.distribution.AbstractRealDistribution	myDistrs[] = new org.apache.commons.math3.distribution.AbstractRealDistribution[num_dims];
		for(i=0;i<num_dims;i++){
			double	from = ahist.boundaries_from(i);
			double	to = ahist.boundaries_to(i);
			myDistrs[i] = make_NORMAL_distribution(
				// mean: at the middle of the bins' span
				(to - from) / 2.0,
				// stdev: 15% of total histogram span
				0.15 * (to - from)
			);
		}
		fill_histogram_with_samples_from_distribution(ahist, myDistrs, 1.0, num_samples);

		if( out_basename != null ){
			String outfile = out_basename + ".txt";
			if( ! save_histogram_for_plotting(ahist, outfile) ){ System.err.println("TestHistograms_SparseArray_randombins.java: make_histogram_and_fill_from_NORMAL_distribution() : call to save_histogram_for_plotting() has failed for output file '"+outfile+"'."); return null; }
		}
		System.out.println("make_histogram_and_fill_from_NORMAL_distribution : done for '"+histogram_name+"', time taken: "+((System.nanoTime()-nanotime_started)/1000000)+" milli seconds.");
		return ahist;
	}

	//
	// nothing to change below
	//
	private	static	void	print_distribution_to_file(
		org.apache.commons.math3.distribution.AbstractRealDistribution adist,
		int num_samples,
		String outfile
	){
		try {
			PrintWriter out =new PrintWriter(outfile);
			for(int i=num_samples;i-->0;){ out.println(adist.sample()); }
			out.close();
		} catch(Exception ex){ System.err.println("TestHistograms_SparseArray_randombins.java : print_distribution_to_file() : caught exception : "+ex); ex.printStackTrace(); }
	}

	private	static	void	print_histogram(Histogram<SparseArray<Histobin>> ahist){
		System.out.println(ahist.toString());
	}
	private	static	boolean	save_histogram_for_plotting(
		Histogram<SparseArray<Histobin>> ahist,
		String	outfile
	){
		try {
			ahist.save_to_file_txt(outfile);
		} catch(Exception ex){ System.err.println("TestHistograms_SparseArray_randombins.java: save_histogram_for_plotting() : call to save_to_file_txt() has failed:\n"+ex); ex.printStackTrace(); return false; }
		return true;
	}
	private	static	org.apache.commons.math3.distribution.AbstractRealDistribution	make_NORMAL_distribution(
		double mean, double stdev
	){
		return new org.apache.commons.math3.distribution.NormalDistribution(
			mean, stdev
		);
	}		
	private	static	org.apache.commons.math3.distribution.AbstractRealDistribution	make_BETA_distribution(
		double alpha, double beta
	){
		return new org.apache.commons.math3.distribution.BetaDistribution(
			alpha, beta
		);
	}		
	private	static	org.apache.commons.math3.distribution.AbstractRealDistribution	make_LEVY_distribution(
		double mu, double c
	){
		return new org.apache.commons.math3.distribution.BetaDistribution(
			mu, c
		);
	}		
	private	static	Histogram<SparseArray<Histobin>>	make_histogram(
		String histogram_name,
		int[] numBins,
		double[]	binWidths,
		double[]	boundariesFrom
	){
		System.out.println("make_histogram : starting for '"+histogram_name+"' ...");
		long	nanotime_started = System.nanoTime();
		Histogram<SparseArray<Histobin>> hist = null;

		try {
			hist = new Histogram<SparseArray<Histobin>>(
				histogram_name,
				// null labels (null means default labels=>coordinates)
				numBins,
				binWidths,
				boundariesFrom,
				(Class<SparseArray<Histobin>> )(Class<?> )SparseArray.class
			);
		} catch(Exception ex){
			System.err.println("TestHistograms_SparseArray_randombins.java : main() : exception was caught when called new HistogramNDSparseArray() : "+ex);
			ex.printStackTrace();
			return null;
		}
		System.out.println("make_histogram : done for '"+histogram_name+"', time taken: "+((System.nanoTime()-nanotime_started)/1000000)+" milli seconds.");
		return hist;
	}
	private	static	void	fill_histogram_with_samples_from_distribution(
		Histogram<SparseArray<Histobin>>	ahist,
		org.apache.commons.math3.distribution.AbstractRealDistribution myDistrs[],
		double	scale,
		int num_samples
	){
		System.out.println("fill_histogram_with_samples_from_distribution : starting for '"+ahist.name()+" ...");
		long	nanotime_started = System.nanoTime();
		double	aninputX, aninputY, aninputZ;
		int	num_dims = myDistrs.length;
		double	froms[] = new double[num_dims];
		double	tos[] = new double[num_dims];

		int	i, j;
		for(i=0;i<num_dims;i++){
			froms[i] = ahist.boundaries_from(i);
			tos[i] = ahist.boundaries_to(i);
		}
		org.apache.commons.math3.distribution.AbstractRealDistribution	adist;
		double rand_values_to_add[] = new double[num_dims];
		double	ashit, afrom, ato;
		for(i=num_samples;i-->0;){
			for(j=num_dims;j-->0;){
				adist = myDistrs[j];
				afrom = froms[j];
				ato = tos[j];
				do {
					ashit = adist.sample() * scale;
				} while(
					   (ashit<afrom)
					|| (ashit>ato)
				);
				rand_values_to_add[j] = ashit;
			}
			ahist.increment_bin_count(rand_values_to_add);
		}
		System.out.println("fill_histogram_with_samples_from_distribution : done for '"+ahist.name()+"', time taken: "+((System.nanoTime()-nanotime_started)/1000000)+" milli seconds.");
	}
	private static  Histogram<SparseArray<Histobin>>     populate_histogram_from_existing_histogram_and_save(
		String histogram_name,
		Histogram<SparseArray<Histobin>> ahist,
		int     num_samples,
		String	out_basename // leave null if don't want to write out data
	){
		System.out.println("populate_histogram_from_existing_histogram_and_save()/1 : starting for '"+histogram_name+"' ...");
		long	nanotime_started = System.nanoTime();
		Histogram<SparseArray<Histobin>> newhist = ahist.clone_only_structure(histogram_name);

		Random  arng = new Random(1234);
		double[] arv;
		for(int i=num_samples;i-->0;){
			arv = ahist.random_value(arng);
			newhist.increment_bin_count(arv);
		}
		if( out_basename != null ){
			String outfile = out_basename + ".txt";
			if( ! save_histogram_for_plotting(newhist, outfile) ){ System.err.println("TestHistograms_SparseArray_randombins.java: populate_histogram_from_existing_histogram_and_save() : call to save_histogram_for_plotting() has failed for output file '"+outfile+"'."); return null; }
		}
		System.out.println("populate_histogram_from_existing_histogram_and_save()/1 : done for '"+histogram_name+"', time taken: "+((System.nanoTime()-nanotime_started)/1000000)+" milli seconds.");
		return newhist;
	}
	// it specifies which bins from the original histogram to choose to get the requested random values 
	private static  Histogram<SparseArray<Histobin>>     populate_histogram_from_existing_histogram_and_save(
		String histogram_name,
		Histogram<SparseArray<Histobin>> ahist,
		int     num_samples,
		HistogramSampler	asampler,
		String	out_basename // leave null if don't want to write out data
	) throws Exception {
		System.out.println("populate_histogram_from_existing_histogram_and_save()/2 : starting for '"+histogram_name+"' ...");
		long	nanotime_started = System.nanoTime();
		Histogram<SparseArray<Histobin>> newhist = ahist.clone_only_structure(histogram_name);

		Random  arng = new Random(1234);
		for(int i=num_samples;i-->0;){
			newhist.increment_bin_count(
				asampler.random_value_from_selection()
			);
		}
		if( out_basename != null ){
			String outfile = out_basename + ".txt";
			if( ! save_histogram_for_plotting(newhist, outfile) ){ System.err.println("TestHistograms_SparseArray_randombins.java: populate_histogram_from_existing_histogram_and_save() : call to save_histogram_for_plotting() has failed for output file '"+outfile+"'."); return null; }
		}
		System.out.println("populate_histogram_from_existing_histogram_and_save()/2 : done for '"+histogram_name+"', time taken: "+((System.nanoTime()-nanotime_started)/1000000)+" milli seconds.");
		return newhist;
	}
}
