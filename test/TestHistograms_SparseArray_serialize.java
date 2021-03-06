import	ahp.org.Histograms.*;
import	ahp.org.Containers.*;

import	java.util.Random;
import  java.io.PrintWriter;

import	org.apache.commons.math3.distribution.NormalDistribution;
import	org.apache.commons.math3.distribution.BetaDistribution;
import	org.apache.commons.math3.distribution.AbstractRealDistribution;

public	class	TestHistograms_SparseArray_serialize {
	public	static void main(String[] args) throws Exception {
		int	num_samples = 10000,
			num_samples_repopulate = 5;

		// so many bins along each dimension:
		int	num_bins[] = new int[]{3,3};
		// the width of the bins along each dimesnion:
		double	bin_widths[] = new double[]{1, 1};
		// the histogram's boundary start from here (along each dimension):
		double	boundaries_from[] = new double[]{0, 0};

		int	num_dims = num_bins.length;

		int	i;

		// in larger dims, saving hists to file is not practical...
		boolean	doNotSaveDataToFiles = num_dims > 3;

		if( bin_widths.length != num_dims ){ System.err.println("TestHistograms_SparseArray_serialize.java : main() : bin_widths must have "+num_dims+" dimensions."); System.exit(1); }
		if( boundaries_from.length != num_dims ){ System.err.println("TestHistograms_SparseArray_serialize.java : main() : boundaries_from must have "+num_dims+" dimensions."); System.exit(1); }

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
		)) == null ){ System.err.println("TestHistograms_SparseArray_serialize.java : main() : call to make_histogram_and_fill_from_NORMAL_distribution() has failed."); System.exit(1); }

		if( doNotSaveDataToFiles == false ){
			try {
				outfile = "test"+num_dims+"D_NORMALDistribution_histogram_dump.txt";
				shitprintw = new PrintWriter(outfile);
				shitprintw.println(hN.toString());
				shitprintw.close();
				System.out.println("TestHistograms_SparseArray_serialize.java : main() : first histogram written to file '"+outfile+"'.");
			} catch(Exception ex){ System.err.println("TestHistograms_SparseArray_serialize.java : main() : exception was caught:\n"+ex); ex.printStackTrace(); System.exit(1); }
		}

		outfile = "a_histogram.obj";
		System.out.println("saving object to file: "+outfile);
		Histogram.save_to_file_object(outfile, hN);

		Histogram<SparseArray<Histobin>> newH = Histogram.load_from_file_object(outfile);

		double diff = hN.difference_in_normalised_count_L2(newH);
		if( diff == 0.0 ){
			System.out.println("GOOD, no difference in saved and re-loaded hists.");
		} else {
			throw new Exception("saved and re-loaded hists differ. this is a problem");
		}

		System.out.println("TestHistograms_SparseArray_serialize.java : done, total time taken : "+((System.nanoTime()-nanotime_started)/1000000)+" milli seconds.");
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
			if( ! save_histogram_for_plotting(ahist, outfile) ){ System.err.println("TestHistograms_SparseArray_serialize.java: make_histogram_and_fill_from_NORMAL_distribution() : call to save_histogram_for_plotting() has failed for output file '"+outfile+"'."); return null; }
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
		} catch(Exception ex){ System.err.println("TestHistograms_SparseArray_serialize.java : print_distribution_to_file() : caught exception : "+ex); ex.printStackTrace(); }
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
		} catch(Exception ex){ System.err.println("TestHistograms_SparseArray_serialize.java: save_histogram_for_plotting() : call to save_to_file_txt() has failed:\n"+ex); ex.printStackTrace(); return false; }
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
				// no labels, will be put later
				numBins,
				binWidths,
				boundariesFrom,
				(Class<SparseArray<Histobin>> )(Class<?> )SparseArray.class
			);
		} catch(Exception ex){
			System.err.println("TestHistograms_SparseArray_serialize.java : main() : exception was caught when called new HistogramNDSparseArray() : "+ex);
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

		Random  arng = new Random();
		double[] arv;
		for(int i=num_samples;i-->0;){
			arv = ahist.random_value(arng);
			newhist.increment_bin_count(arv);
		}
		if( out_basename != null ){
			String outfile = out_basename + ".txt";
			if( ! save_histogram_for_plotting(newhist, outfile) ){ System.err.println("TestHistograms_SparseArray_serialize.java: populate_histogram_from_existing_histogram_and_save() : call to save_histogram_for_plotting() has failed for output file '"+outfile+"'."); return null; }
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

		Random  arng = new Random();
		for(int i=num_samples;i-->0;){
			newhist.increment_bin_count(
				asampler.random_value_from_selection()
			);
		}
		if( out_basename != null ){
			String outfile = out_basename + ".txt";
			if( ! save_histogram_for_plotting(newhist, outfile) ){ System.err.println("TestHistograms_SparseArray_serialize.java: populate_histogram_from_existing_histogram_and_save() : call to save_histogram_for_plotting() has failed for output file '"+outfile+"'."); return null; }
		}
		System.out.println("populate_histogram_from_existing_histogram_and_save()/2 : done for '"+histogram_name+"', time taken: "+((System.nanoTime()-nanotime_started)/1000000)+" milli seconds.");
		return newhist;
	}
}
