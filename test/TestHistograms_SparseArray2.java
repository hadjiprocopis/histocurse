import  java.util.Random;
import  java.util.HashMap;
import  java.util.ArrayList;
import  java.io.PrintWriter;

import  org.apache.commons.math3.distribution.NormalDistribution;
import  org.apache.commons.math3.distribution.BetaDistribution;
import  org.apache.commons.math3.distribution.AbstractRealDistribution;

import	ahp.org.Histograms.*;
import	ahp.org.Containers.*;

public class TestHistograms_SparseArray2 {
public static void main(String args[]) throws Exception {
	String	labels[/*dims*/][/*num bins/labels per dim*/] =
		new String[][]{
			new String[]{"a", "b"},
			new String[]{"a", "b"},
		}
	;

	Histogram ahist = new Histogram<SparseArray<Histobin>>(
		"a histogram1",
		labels,
		new int[]{2, 2}, // numbins per dim
		new double[]{1,1}, // bin widths
		new double[]{0,0}, // boundaries start
		// from: http://stackoverflow.com/questions/37231043/how-to-keep-generic-type-of-nested-generics-with-class-tokens
		(Class<SparseArray<Histobin>> )(Class<?> )SparseArray.class
	);

	ahist.increment_bin_count(new String[]{"a", "a"});
	ahist.increment_bin_count(new double[]{0.1, 0.3});
	ahist.increment_bin_count(new double[]{0.1, 0.3});
	ahist.increment_bin_count(new double[]{0.1, 0.3});
	ahist.increment_bin_count(new double[]{0.1, 0.3});
	ahist.increment_bin_count(new double[]{0.1, 0.3});
	ahist.increment_bin_count(new double[]{0.2, 0.5});
	ahist.increment_bin_count(new double[]{0.3, 1.2});
	ahist.increment_bin_count(new double[]{0.4, 1.5});
	ahist.increment_bin_count(new double[]{1.1, 0.1});
	ahist.increment_bin_count(new double[]{1.2, 1.4});
	
	System.out.println("*******************");
	System.out.println("Histogram with preset-labels: "+ahist);

	System.out.println("******************* DECREMENTING ");
	ahist.decrement_bin_count(new double[]{1.2, 1.4});
	ahist.decrement_bin_count(new double[]{0.2, 0.2});
	ahist.decrement_bin_count(new double[]{0.2, 0.2});
	ahist.decrement_bin_count(new double[]{0.2, 0.2});
	ahist.decrement_bin_count(new double[]{0.2, 0.2});
	System.out.println("after decrement Histogram with preset-labels: "+ahist);

	Random arng = new Random(1234);
	HistogramSampler asampler = ahist.sampler(arng);
	asampler.select_bins_using_wildcard_labels(new String[]{"a", "*"});
	System.out.println("random bin: with '0-*' : "+asampler.random_bin_from_selection());

	ahist.decrement_bin_count(new double[]{0.2, 0.2});
	asampler.select_bins_using_wildcard_labels(new String[]{"a", "*"});
	System.out.println("random bin: with '0-*' : "+asampler.random_bin_from_selection());

	ahist.decrement_bin_count(new double[]{0.2, 0.2});
	ahist.decrement_bin_count(new double[]{0.2, 0.2});
	ahist.decrement_bin_count(new double[]{0.2, 0.2});
	asampler.select_bins_using_wildcard_labels(new String[]{"a", "*"});
	System.out.println("random bin: with '0-*' : "+Histobin.array_toString(asampler.random_bin_from_selection_and_the_selection_itself()));

	ahist.decrement_bin_count(new double[]{0.2, 0.2});
	asampler.select_bins_using_wildcard_labels(new String[]{"a", "*"});
	System.out.println("random bin: with '0-*' : "+Histobin.array_toString(asampler.random_bin_from_selection_and_the_selection_itself()));

	ahist.decrement_bin_count(new double[]{0.2, 0.2});
	asampler.select_bins_using_wildcard_labels(new String[]{"a", "*"});
	System.out.println("random bin: with '0-*' : "+Histobin.array_toString(asampler.random_bin_from_selection_and_the_selection_itself()));

	ahist.decrement_bin_count(new double[]{0.2, 0.2});
	asampler.select_bins_using_wildcard_labels(new String[]{"a", "*"});
	System.out.println("random bin: with '0-*' : "+Histobin.array_toString(asampler.random_bin_from_selection_and_the_selection_itself()));

	ahist.increment_bin_count(new double[]{0.2, 0.2});
	ahist.increment_bin_count(new double[]{0.2, 0.2});
	ahist.increment_bin_count(new double[]{0.2, 0.2});
	ahist.increment_bin_count(new double[]{0.2, 0.2});
	ahist.increment_bin_count(new double[]{0.2, 0.2});
	asampler.select_bins_using_wildcard_labels(new String[]{"a", "*"});
	System.out.println("random bin: with '0-*' : "+Histobin.array_toString(asampler.random_bin_from_selection_and_the_selection_itself()));

	System.exit(0);

	HashMap<String,Integer> ahashLabelsDim1 = new HashMap<String,Integer>();
	ahashLabelsDim1.put("dim1-bin1", 0);
	ahashLabelsDim1.put("dim1-bin2", 1);
	HashMap<String,Integer> ahashLabelsDim2 = new HashMap<String,Integer>();
	ahashLabelsDim2.put("dim2-bin1", 0);
	ahashLabelsDim2.put("dim2-bin2", 1);
	ArrayList<HashMap<String,Integer>> dimslabelslist = new ArrayList<HashMap<String,Integer>>();
	dimslabelslist.add(ahashLabelsDim1);
	dimslabelslist.add(ahashLabelsDim2);
	ahist = new Histogram<SparseArray<Histobin>>(
		"a histogram2",
		dimslabelslist,
		new int[]{2, 2}, // numbins per dim
		new double[]{1,1}, // bin widths
		new double[]{0,0}, // boundaries start
		// from: http://stackoverflow.com/questions/37231043/how-to-keep-generic-type-of-nested-generics-with-class-tokens
		(Class<SparseArray<Histobin>> )(Class<?> )SparseArray.class
	);

	ahist.increment_bin_count(new String[]{"dim1-bin2", "dim2-bin1"});
	ahist.increment_bin_count(new double[]{0.1, 0.3});
	ahist.increment_bin_count(new double[]{0.2, 0.5});
	ahist.increment_bin_count(new double[]{0.3, 1.2});
	ahist.increment_bin_count(new double[]{0.4, 1.5});
	ahist.increment_bin_count(new double[]{1.1, 0.1});
	ahist.increment_bin_count(new double[]{1.2, 1.4});
		
	System.out.println("Histogram with default-labels: "+ahist);



}
}
