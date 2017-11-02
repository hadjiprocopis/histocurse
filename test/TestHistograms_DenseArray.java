import  java.util.Random;
import  java.io.PrintWriter;

import  org.apache.commons.math3.distribution.NormalDistribution;
import  org.apache.commons.math3.distribution.BetaDistribution;
import  org.apache.commons.math3.distribution.AbstractRealDistribution;

import	ahp.org.Histograms.*;
import	ahp.org.Containers.*;

public class TestHistograms_DenseArray {
@SuppressWarnings("unchecked")
public static void main(String args[]) throws Exception {
	String	labels[/*dims*/][/*num bins/labels per dim*/] =
		new String[][]{
			new String[]{"d1-a", "d1-b"}, // dim1 labels
			new String[]{"d2-a", "d2-b"}, // dim2 labels for each bin
		}
	;
	Histogram ahist = new Histogram<DenseArray<Histobin>>(
		"a histogram",
		labels,
		new int[]{2, 2}, // numbins per dim
		new double[]{1,1}, // bin widths
		new double[]{0,0}, // boundaries start
		// from: http://stackoverflow.com/questions/37231043/how-to-keep-generic-type-of-nested-generics-with-class-tokens
		(Class<DenseArray<Histobin>> )(Class<?> )DenseArray.class
	);
	ahist.increment_bin_count(new String[]{"d1-a", "d2-b"}); 
	ahist.increment_bin_count(new double[]{0.1, 0.3});

	System.out.println("Histogram: "+ahist);	
}
}
