import	ahp.org.Histograms.HistogramVanilla;

import	java.util.Random;

public	class	TestHistogramVanilla {
	public	static void main(String[] args) throws Exception {
		long	nanotime_started = System.nanoTime();

		int	num_bins = 100,
			num_samples = 100000, i;
		double	bin_width = 1.0/num_bins,
			start_from = 0.0, avalue;

		// here we create histograms and then we populate them from a distribution
		HistogramVanilla	ahisto = new HistogramVanilla(
			"fuck",
			num_bins,
			bin_width,
			start_from
		);
		Random myRNG = new Random(1234);
		for(i=num_samples;i-->0;){
			avalue = myRNG.nextDouble();
			ahisto.increment_bin_count(avalue);
		}
		System.out.println(ahisto);
		System.out.println("TestHistogramVanilla.java : done, time taken : "+((System.nanoTime()-nanotime_started)/1000000)+" milli seconds.");

		num_bins = 10;
		bin_width = 0.1;
		start_from = 12.3;
		HistogramVanilla	ahisto2 = new HistogramVanilla(
			"fuck",
			num_bins,
			bin_width,
			start_from
		);
		ahisto2.increment_bin_count(12.31);
		ahisto2.increment_bin_count(12.32);
		ahisto2.increment_bin_count(12.33);
		ahisto2.increment_bin_count(12.41);
		
		System.out.println(ahisto2);
		System.out.println("count of bin[0]: "+ahisto2.get_bin_count(0));
	}
}
