import	ahp.org.Histograms.Histogram1D;

import	java.util.Random;
import	org.apache.commons.math3.distribution.NormalDistribution;
import	org.apache.commons.math3.distribution.BetaDistribution;
import	org.apache.commons.math3.distribution.AbstractRealDistribution;

public	class	TestHistogram1D {
	public	static void main(String[] args){
		long	nanotime_started = System.nanoTime();

		int	num_bins = 100,
			num_samples = 100000,
			num_samples_repopulate = 100000;
		double	bin_width = 0.05,
			start_from = 0.0;

		// here we create histograms and then we populate them from a distribution
		Histogram1D	hN, hL, hB;
		if( (hN=make_histogram_and_fill_from_NORMAL_distribution(
			num_bins, bin_width, start_from, num_samples,
			"test1D_NORMALDistribution"
		)) == null ){ System.err.println("TestHistogram1D.java : main() : call to make_histogram_and_fill_from_NORMAL_distribution() has failed."); System.exit(1); }
//		hN.random_value(new Random());
//		System.exit(0);

/*		if( (hB=make_histogram_and_fill_from_BETA_distribution(
			num_bins, bin_width, start_from, num_samples,
			"test1D_BETADistribution"
		)) == null ){ System.err.println("TestHistogram1D.java : main() : call to make_histogram_and_fill_from_BETA_distribution() has failed."); System.exit(1); }
		if( (hL=make_histogram_and_fill_from_LEVY_distribution(
			num_bins, bin_width, start_from, num_samples,
			"test1D_LEVYDistribution"
		)) == null ){ System.err.println("TestHistogram1D.java : main() : call to make_histogram_and_fill_from_LEVY_distribution() has failed."); System.exit(1); }
*/
		// here we create a fresh histogram and then ask the old histograms
		// to give us a random value (based on their own distribution)
		// the new histogram should then exhibit the same distribution as the existing histogram
		Histogram1D newhN, newhB, newhL;
		if( (newhN=populate_histogram_from_existing_histogram_and_save(
			hN,
			num_samples_repopulate,
			"test1D_repopulateFromNORMALHistogram"
		)) == null ){ System.err.println("TestHistogram1D.java : main() : call to populate_histogram_from_existing_histogram_and_save() has failed."); System.exit(1); }
/*		if( (newhL=populate_histogram_from_existing_histogram_and_save(
			hL,
			num_samples_repopulate,
			"test1D_repopulateFromLEVYHistogram"
		)) == null ){ System.err.println("TestHistogram1D.java : main() : call to populate_histogram_from_existing_histogram_and_save() has failed."); System.exit(1); }
		if( (newhB=populate_histogram_from_existing_histogram_and_save(
			hB,
			num_samples_repopulate,
			"test1D_repopulateFromBETAHistogram"
		)) == null ){ System.err.println("TestHistogram1D.java : main() : call to populate_histogram_from_existing_histogram_and_save() has failed."); System.exit(1); }
*/
		System.out.println("TestHistogram1D.java : done, time taken : "+
((System.nanoTime()-nanotime_started)/1000000)+" milli seconds.");
	}

	private	static	Histogram1D	make_histogram_and_fill_from_BETA_distribution(
		int num_bins, double bin_width, double start_from,
		int num_samples,
		String	out_basename
	){
		long	nanotime_started = System.nanoTime();
		Histogram1D ahist = make_histogram(num_bins, bin_width, start_from);
		double	left_boundary = ahist.bins_left_boundary(),
			right_boundary = ahist.bins_right_boundary();
		org.apache.commons.math3.distribution.AbstractRealDistribution adist = make_BETA_distribution(
			// alpha
			2.0,
			//beta
			5.0
		);
		fill_histogram_with_samples_from_distribution(ahist, adist, 100.0, num_samples);
		print_histogram(ahist);
		String outfile = out_basename + ".txt";
		if( ! save_histogram_for_plotting(ahist, outfile) ){ System.err.println("TestHistogram1D.java: make_histogram_and_fill_from_BETA_distribution() : call to save_histogram_for_plotting() has failed for output file '"+outfile+"'."); return null; }
		System.out.println("make_histogram_and_fill_from_BETA_distribution : time taken: "+((System.nanoTime()-nanotime_started)/1000000)+" milli seconds.");
		return ahist;
	}
	private	static	Histogram1D	make_histogram_and_fill_from_LEVY_distribution(
		int num_bins, double bin_width, double start_from, int num_samples,
		String	out_basename
	){
		long	nanotime_started = System.nanoTime();
		Histogram1D ahist = make_histogram(num_bins, bin_width, start_from);
		double	left_boundary = ahist.bins_left_boundary(),
			right_boundary = ahist.bins_right_boundary();
		org.apache.commons.math3.distribution.AbstractRealDistribution adist = make_LEVY_distribution(
			// mu
			0.5,
			//c
			1.0
		);
		fill_histogram_with_samples_from_distribution(ahist, adist, 50.0, num_samples);
		print_histogram(ahist);
		String outfile = out_basename + ".txt";
		if( ! save_histogram_for_plotting(ahist, outfile) ){ System.err.println("TestHistogram1D.java: make_histogram_and_fill_from_BETA_distribution() : call to save_histogram_for_plotting() has failed for output file '"+outfile+"'."); return null; }
		System.out.println("make_histogram_and_fill_from_LEVY_distribution : time taken: "+((System.nanoTime()-nanotime_started)/1000000)+" milli seconds.");
		return ahist;
	}
	private	static	Histogram1D make_histogram_and_fill_from_NORMAL_distribution(
		int num_bins, double bin_width, double start_from, int num_samples,
		String	out_basename
	){
		long	nanotime_started = System.nanoTime();
		Histogram1D ahist = make_histogram(num_bins, bin_width, start_from);
		double	left_boundary = ahist.bins_left_boundary(),
			right_boundary = ahist.bins_right_boundary();
		org.apache.commons.math3.distribution.AbstractRealDistribution adist = make_NORMAL_distribution(
			// mean: at the middle of the bins' span
			(right_boundary - left_boundary) / 2.0,
			// stdev: 15% of total histogram span
			0.15 * (right_boundary - left_boundary)
		);
		fill_histogram_with_samples_from_distribution(ahist, adist, 1.0, num_samples);

		print_histogram(ahist);
		String outfile = out_basename + ".txt";
		if( ! save_histogram_for_plotting(ahist, outfile) ){ System.err.println("TestHistogram1D.java: make_histogram_and_fill_from_NORMAL_distribution() : call to save_histogram_for_plotting() has failed for output file '"+outfile+"'."); return null; }
		System.out.println("make_histogram_and_fill_from_NORMAL_distribution : time taken: "+((System.nanoTime()-nanotime_started)/1000000)+" milli seconds.");
		return ahist;
	}

	//
	// nothing to change below
	//
	private	static	void	print_histogram(Histogram1D ahist){
		System.out.println(ahist.toString());
	}
	private	static	boolean	save_histogram_for_plotting(
		Histogram1D ahist,
		String	outfile
	){
		try {
			ahist.save_to_file_txt(outfile);
		} catch(Exception ex){ System.err.println("TestHistogram1D.java: save_histogram_for_plotting() : call to save_to_file_txt() has failed:\n"+ex); ex.printStackTrace(); return false; }
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
	private	static	Histogram1D	make_histogram(
		int num_bins, double bin_width, double start_from
	){
		Histogram1D hist = null;

		try {
			hist = new Histogram1D(num_bins, bin_width, start_from);
		} catch(Exception ex){
			System.err.println("TestHistogram1D.java : main() : exception was caught when called new Histogram1D() : "+ex);
			ex.printStackTrace();
			return null;
		}
		return hist;
	}
	private	static	void	fill_histogram_with_samples_from_distribution(
		Histogram1D	ahist,
		org.apache.commons.math3.distribution.AbstractRealDistribution adist,
		double	scale,
		int num_samples
	){
		long	nanotime_started = System.nanoTime();
		double	aninput;
		double	left_boundary = ahist.bins_left_boundary(),
			right_boundary = ahist.bins_right_boundary();
		for(int i=num_samples;i-->0;){
			do {
				aninput = adist.sample() * scale;
			} while(
				   (aninput<left_boundary)
				|| (aninput>right_boundary)
			);
			ahist.add(aninput);
		}
		System.out.println("fill_histogram_with_samples_from_distribution : time taken: "+((System.nanoTime()-nanotime_started)/1000000)+" milli seconds.");
	}
	private	static	Histogram1D	populate_histogram_from_existing_histogram_and_save(
		Histogram1D ahist,
		int	num_samples,
		String	out_basename
	){
		long	nanotime_started = System.nanoTime();
		Histogram1D newhist = ahist.clone();
		newhist.reset();
		Random	arng = new Random();
		for(int i=num_samples;i-->0;){
			newhist.add(ahist.random_value(arng));
		}
		print_histogram(newhist);
		String outfile = out_basename + ".txt";
		if( ! save_histogram_for_plotting(newhist, outfile) ){ System.err.println("TestHistogram1D.java: populate_histogram_from_existing_histogram_and_save() : call to save_histogram_for_plotting() has failed for output file '"+outfile+"'."); return null; }
		System.out.println("populate_histogram_from_existing_histogram_and_save : time taken: "+((System.nanoTime()-nanotime_started)/1000000)+" milli seconds.");
		return newhist;
	}
}
