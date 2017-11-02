import	ahp.org.Histograms.Histogram3D;

import	java.util.Random;
import  java.io.PrintWriter;

import	org.apache.commons.math3.distribution.NormalDistribution;
import	org.apache.commons.math3.distribution.BetaDistribution;
import	org.apache.commons.math3.distribution.AbstractRealDistribution;

public	class	TestHistogram3D {
	public	static void main(String[] args){
		int	num_binsX = 50,
			num_binsY = 50,
			num_binsZ = 50,
			num_samples = 50000,
			num_samples_repopulate = 50000;
		double	bin_widthX = 1,
			bin_widthY = 1,
			bin_widthZ = 1,
			start_fromX = 0.0,
			start_fromY = 0.0,
			start_fromZ = 0.0;

		long	nanotime_started = System.nanoTime();
		// here we create histograms and then we populate them from a distribution
		Histogram3D hN, hB, hL;
		if( (hN=make_histogram_and_fill_from_NORMAL_distribution(
			num_binsX, num_binsY, num_binsZ,
			bin_widthX, bin_widthY, bin_widthZ,
			start_fromX, start_fromY, start_fromZ,
			num_samples,
			"test3D_NORMALDistribution"
		)) == null ){ System.err.println("TestHistogram3D.java : main() : call to make_histogram_and_fill_from_NORMAL_distribution() has failed."); System.exit(1); }
//		hN.dump_random_values_to_file("fuck.txt", 50000);
//System.exit(1);
/*		if( (hB=make_histogram_and_fill_from_BETA_distribution(
			num_binsX, num_binsY, num_binsZ,
			bin_widthX, bin_widthY, bin_widthZ,
			start_fromX, start_fromY, start_fromZ,
			num_samples,
			"test3D_BETADistribution"
		)) == null ){ System.err.println("TestHistogram3D.java : main() : call to make_histogram_and_fill_from_BETA_distribution() has failed."); System.exit(1); }
		if( (hL=make_histogram_and_fill_from_LEVY_distribution(
			num_binsX, num_binsY, num_binsZ,
			bin_widthX, bin_widthY, bin_widthZ,
			start_fromX, start_fromY, start_fromZ,
			num_samples,
			"test3D_LEVYDistribution"
		)) == null ){ System.err.println("TestHistogram3D.java : main() : call to make_histogram_and_fill_from_LEVY_distribution() has failed."); System.exit(1); }
*/
		// here we create a fresh histogram and then ask the old histograms
		// to give us a random value (based on their own distribution)
		// the new histogram should then exhibit the same distribution as the existing histogram
		Histogram3D newhN, newhB, newhL;
		if( (newhN=populate_histogram_from_existing_histogram_and_save(
			hN,
			num_samples_repopulate,
			"test3D_repopulateFromNORMALHistogram"
		)) == null ){ System.err.println("TestHistogram1D.java : main() : call to populate_histogram_from_existing_histogram_and_save() has failed."); System.exit(1); }
/*		if( (newhL=populate_histogram_from_existing_histogram_and_save(
			hL,
			num_samples_repopulate,
			"test3D_repopulateFromLEVYHistogram"
		)) == null ){ System.err.println("TestHistogram1D.java : main() : call to populate_histogram_from_existing_histogram_and_save() has failed."); System.exit(1); }
		if( (newhB=populate_histogram_from_existing_histogram_and_save(
			hB,
			num_samples_repopulate,
			"test3D_repopulateFromBETAHistogram"
		)) == null ){ System.err.println("TestHistogram1D.java : main() : call to populate_histogram_from_existing_histogram_and_save() has failed."); System.exit(1); }
*/

		// Test conditional probability, specify a 'GIVEN' as the first bin dimension
		Histogram3D newhN2, newhB2, newhL2;
		Histogram3D newhN3, newhB3, newhL3;
		int     given_bin_index1 = (int )(num_binsX/2);
		int     given_bin_index2 = (int )(num_binsY/2);
		if( (newhN2=populate_histogram_from_existing_histogram_and_save(
			hN,
			num_samples_repopulate,
			given_bin_index1,
			"test3D_repopulateFromNORMALHistogram_conditional_fix1Dimension"
		)) == null ){ System.err.println("TestHistogram1D.java : main() : call to populate_histogram_from_existing_histogram_and_save() has failed (2)."); System.exit(1); }
		if( (newhN3=populate_histogram_from_existing_histogram_and_save(
			hN,
			num_samples_repopulate,
			given_bin_index1, given_bin_index2,
			"test3D_repopulateFromNORMALHistogram_conditional_fix2Dimensions"
		)) == null ){ System.err.println("TestHistogram1D.java : main() : call to populate_histogram_from_existing_histogram_and_save() has failed (2)."); System.exit(1); }

		System.out.println("TestHistogram3D.java : done, time taken : "+((System.nanoTime()-nanotime_started)/1000000)+" milli seconds.");
	}

	private	static	Histogram3D	make_histogram_and_fill_from_BETA_distribution(
		int num_binsX, int num_binsY, int num_binsZ,
		double bin_widthX, double bin_widthY, double bin_widthZ,
		double start_fromX, double start_fromY, double start_fromZ,
		int num_samples,
		String	out_basename
	){
		long	nanotime_started = System.nanoTime();
		Histogram3D ahist = make_histogram(
			num_binsX, num_binsY, num_binsZ,
			bin_widthX, bin_widthY, bin_widthZ,
			start_fromX, start_fromY, start_fromZ
		);
		org.apache.commons.math3.distribution.AbstractRealDistribution adistX = make_BETA_distribution(
			// alpha
			2.0,
			//beta
			5.0
		);
		org.apache.commons.math3.distribution.AbstractRealDistribution adistY = make_BETA_distribution(
			// alpha
			2.0,
			//beta
			5.0
		);
		org.apache.commons.math3.distribution.AbstractRealDistribution adistZ = make_BETA_distribution(
			// alpha
			2.0,
			//beta
			5.0
		);
		fill_histogram_with_samples_from_distribution(ahist, adistX, adistY, adistZ, 100.0, num_samples);
		print_histogram(ahist);
		String outfile = out_basename + ".txt";
		if( ! save_histogram_for_plotting(ahist, outfile) ){ System.err.println("TestHistogram3D.java: make_histogram_and_fill_from_BETA_distribution() : call to save_histogram_for_plotting() has failed for output file '"+outfile+"'."); return null; }
		System.out.println("make_histogram_and_fill_from_BETA_distribution : time taken: "+((System.nanoTime()-nanotime_started)/1000000)+" milli seconds.");
		return ahist;
	}
	private	static	Histogram3D	make_histogram_and_fill_from_LEVY_distribution(
		int num_binsX, int num_binsY, int num_binsZ,
		double bin_widthX, double bin_widthY, double bin_widthZ,
		double start_fromX, double start_fromY, double start_fromZ,
		int num_samples,
		String	out_basename
	){
		long	nanotime_started = System.nanoTime();
		Histogram3D ahist = make_histogram(
			num_binsX, num_binsY, num_binsZ,
			bin_widthX, bin_widthY, bin_widthZ,
			start_fromX, start_fromY, start_fromZ
		);
		org.apache.commons.math3.distribution.AbstractRealDistribution adistX = make_LEVY_distribution(
			// mu
			0.5,
			//c
			1.0
		);
		org.apache.commons.math3.distribution.AbstractRealDistribution adistY = make_LEVY_distribution(
			// mu
			0.5,
			//c
			1.0
		);
		org.apache.commons.math3.distribution.AbstractRealDistribution adistZ = make_LEVY_distribution(
			// mu
			0.5,
			//c
			1.0
		);
		fill_histogram_with_samples_from_distribution(ahist, adistX, adistY, adistZ, 50.0, num_samples);
		print_histogram(ahist);
		String outfile = out_basename + ".txt";
		if( ! save_histogram_for_plotting(ahist, outfile) ){ System.err.println("TestHistogram3D.java: make_histogram_and_fill_from_BETA_distribution() : call to save_histogram_for_plotting() has failed for output file '"+outfile+"'."); return null; }
		System.out.println("make_histogram_and_fill_from_LEVY_distribution : time taken: "+((System.nanoTime()-nanotime_started)/1000000)+" milli seconds.");
		return ahist;
	}
	private	static	Histogram3D make_histogram_and_fill_from_NORMAL_distribution(
		int num_binsX, int num_binsY, int num_binsZ,
		double bin_widthX, double bin_widthY, double bin_widthZ,
		double start_fromX, double start_fromY, double start_fromZ,
		int num_samples,
		String	out_basename
	){
		long	nanotime_started = System.nanoTime();
		Histogram3D ahist = make_histogram(
			num_binsX, num_binsY, num_binsZ,
			bin_widthX, bin_widthY, bin_widthZ,
			start_fromX, start_fromY, start_fromZ
		);
		double	left_boundaryX = ahist.bins_left_boundaryX(),
			right_boundaryX = ahist.bins_right_boundaryX(),
			left_boundaryY = ahist.bins_left_boundaryY(),
			right_boundaryY = ahist.bins_right_boundaryY(),
			left_boundaryZ = ahist.bins_left_boundaryZ(),
			right_boundaryZ = ahist.bins_right_boundaryZ();
		org.apache.commons.math3.distribution.AbstractRealDistribution adistX = make_NORMAL_distribution(
			// mean: at the middle of the bins' span
			(right_boundaryX - left_boundaryX) / 2.0,
			// stdev: 15% of total histogram span
			0.15 * (right_boundaryX - left_boundaryX)
		);
		org.apache.commons.math3.distribution.AbstractRealDistribution adistY = make_NORMAL_distribution(
			// mean: at the middle of the bins' span
			(right_boundaryY - left_boundaryY) / 2.0,
			// stdev: 15% of total histogram span
			0.15 * (right_boundaryY - left_boundaryY)
		);
		org.apache.commons.math3.distribution.AbstractRealDistribution adistZ = make_NORMAL_distribution(
			// mean: at the middle of the bins' span
			(right_boundaryZ - left_boundaryZ) / 2.0,
			// stdev: 15% of total histogram span
			0.15 * (right_boundaryZ - left_boundaryZ)
		);
		fill_histogram_with_samples_from_distribution(ahist, adistX, adistY, adistZ, 1.0, num_samples);

		print_histogram(ahist);
		String outfile = out_basename + ".txt";
		if( ! save_histogram_for_plotting(ahist, outfile) ){ System.err.println("TestHistogram3D.java: make_histogram_and_fill_from_NORMAL_distribution() : call to save_histogram_for_plotting() has failed for output file '"+outfile+"'."); return null; }
		System.out.println("make_histogram_and_fill_from_NORMAL_distribution : time taken: "+((System.nanoTime()-nanotime_started)/1000000)+" milli seconds.");
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
		} catch(Exception ex){ System.err.println("TestHistogram3D.java : print_distribution_to_file() : caught exception : "+ex); ex.printStackTrace(); }
	}

	private	static	void	print_histogram(Histogram3D ahist){
		System.out.println(ahist.toString());
	}
	private	static	boolean	save_histogram_for_plotting(
		Histogram3D ahist,
		String	outfile
	){
		try {
			ahist.save_to_file_txt(outfile);
		} catch(Exception ex){ System.err.println("TestHistogram3D.java: save_histogram_for_plotting() : call to save_to_file_txt() has failed:\n"+ex); ex.printStackTrace(); return false; }
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
	private	static	Histogram3D	make_histogram(
		int num_binsX, int num_binsY, int num_binsZ,
		double bin_widthX, double bin_widthY, double bin_widthZ,
		double start_fromX, double start_fromY, double start_fromZ
	){
		long	nanotime_started = System.nanoTime();
		Histogram3D hist = null;

		try {
			hist = new Histogram3D(
				num_binsX, num_binsY, num_binsZ,
				bin_widthX, bin_widthY, bin_widthZ,
				start_fromX, start_fromY, start_fromZ
			);
		} catch(Exception ex){
			System.err.println("TestHistogram3D.java : main() : exception was caught when called new Histogram3D() : "+ex);
			ex.printStackTrace();
			return null;
		}
		System.out.println("make_histogram : time taken: "+((System.nanoTime()-nanotime_started)/1000000)+" milli seconds.");
		return hist;
	}
	private	static	void	fill_histogram_with_samples_from_distribution(
		Histogram3D	ahist,
		org.apache.commons.math3.distribution.AbstractRealDistribution adistX,
		org.apache.commons.math3.distribution.AbstractRealDistribution adistY,
		org.apache.commons.math3.distribution.AbstractRealDistribution adistZ,
		double	scale,
		int num_samples
	){
		long	nanotime_started = System.nanoTime();
		double	aninputX, aninputY, aninputZ;
		double	left_boundaryX = ahist.bins_left_boundaryX(),
			right_boundaryX = ahist.bins_right_boundaryX(),
			left_boundaryY = ahist.bins_left_boundaryX(),
			right_boundaryY = ahist.bins_right_boundaryX(),
			left_boundaryZ = ahist.bins_left_boundaryZ(),
			right_boundaryZ = ahist.bins_right_boundaryZ();

		for(int i=num_samples;i-->0;){
			do {
				aninputX = adistX.sample() * scale;
			} while(
				   (aninputX<left_boundaryX)
				|| (aninputX>right_boundaryX)
			);
			do {
				aninputY = adistY.sample() * scale;
			} while(
				   (aninputY<left_boundaryY)
				|| (aninputY>right_boundaryY)
			);
			do {
				aninputZ = adistZ.sample() * scale;
			} while(
				   (aninputZ<left_boundaryZ)
				|| (aninputZ>right_boundaryZ)
			);
			ahist.add(aninputX, aninputY, aninputZ);
		}
		System.out.println("fill_histogram_with_samples_from_distribution : time taken: "+((System.nanoTime()-nanotime_started)/1000000)+" milli seconds.");
	}
	private static  Histogram3D     populate_histogram_from_existing_histogram_and_save(
		Histogram3D ahist,
		int     num_samples,
		String  out_basename
	){
		long	nanotime_started = System.nanoTime();
		Histogram3D newhist = ahist.clone();
		newhist.reset();

		Random  arng = new Random();
		double[] arv;
		for(int i=num_samples;i-->0;){
			arv = ahist.random_value(arng);
			newhist.add(arv[0], arv[1], arv[2]);
		}
		print_histogram(newhist);
		String outfile = out_basename + ".txt";
		if( ! save_histogram_for_plotting(newhist, outfile) ){ System.err.println("TestHistogram3D.java: populate_histogram_from_existing_histogram_and_save() : call to save_histogram_for_plotting() has failed for output file '"+outfile+"'."); return null; }
		System.out.println("populate_histogram_from_existing_histogram_and_save : time taken: "+((System.nanoTime()-nanotime_started)/1000000)+" milli seconds.");
		return newhist;
	}
	private static  Histogram3D     populate_histogram_from_existing_histogram_and_save(
		Histogram3D ahist,
		int     num_samples,
		int	given_bin_index1,
		String  out_basename
	){
		long	nanotime_started = System.nanoTime();
		Histogram3D newhist = ahist.clone();
		newhist.reset();

		Random  arng = new Random();
		double[] arv;
		for(int i=num_samples;i-->0;){
			arv = ahist.random_value(arng, given_bin_index1);
			newhist.add(arv[0], arv[1], arv[2]);
		}
		print_histogram(newhist);
		String outfile = out_basename + ".txt";
		if( ! save_histogram_for_plotting(newhist, outfile) ){ System.err.println("TestHistogram3D.java: populate_histogram_from_existing_histogram_and_save() : call to save_histogram_for_plotting() has failed for output file '"+outfile+"'."); return null; }
		System.out.println("populate_histogram_from_existing_histogram_and_save : time taken: "+((System.nanoTime()-nanotime_started)/1000000)+" milli seconds.");
		return newhist;
	}
	private static  Histogram3D     populate_histogram_from_existing_histogram_and_save(
		Histogram3D ahist,
		int     num_samples,
		int	given_bin_index1, int given_bin_index2,
		String  out_basename
	){
		long	nanotime_started = System.nanoTime();
		Histogram3D newhist = ahist.clone();
		newhist.reset();

		Random  arng = new Random();
		double[] arv;
		for(int i=num_samples;i-->0;){
			arv = ahist.random_value(arng, given_bin_index1, given_bin_index2);
			newhist.add(arv[0], arv[1], arv[2]);
		}
		print_histogram(newhist);
		String outfile = out_basename + ".txt";
		if( ! save_histogram_for_plotting(newhist, outfile) ){ System.err.println("TestHistogram3D.java: populate_histogram_from_existing_histogram_and_save() : call to save_histogram_for_plotting() has failed for output file '"+outfile+"'."); return null; }
		System.out.println("populate_histogram_from_existing_histogram_and_save : time taken: "+((System.nanoTime()-nanotime_started)/1000000)+" milli seconds.");
		return newhist;
	}
}
