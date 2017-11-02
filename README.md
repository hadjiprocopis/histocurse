# histocurse

author: andreas hadjiprocopis (andreashad2@gmail.com)

This is a module implementing a Histogram in N-dimensions.
Meaning that bins do not exist in a one-dimensional space
as they usually do but can be in N-dimensions.

N can be large. In the case of large N, it is better to
use the <SparseArray> back-store.

Here is an example on how to use this module:

```
import  java.util.Random;
import  java.util.HashMap;
import  java.util.ArrayList;
import  java.io.PrintWriter;

import  ahp.org.Histograms.*;
import  ahp.org.Containers.*;

public class TestHistograms_SparseArray {
public static void main(String args[]) throws Exception {
	// Here we create labels for each bin in the histogram.
	// We are going to create 2-dimensional histogram with
	// 2 bins in each dimension.
	// Labels to bins are optional.
	String  labels[/*dims*/][/*num bins/labels per dim*/] =
		new String[][]{
			new String[]{"a", "b"},
			new String[]{"a", "b"},
		}
	;

	// Here we create the 2-dimensional histogram
	Histogram ahist = new Histogram<SparseArray<Histobin>>(
		// histogram name
		"a histogram1",
		// labels (a string array) if any, otherwise will create default
		labels,
		// specify the number of dimensions and the number of bins in each dimension
		new int[]{2, 2}, // numbins per dim
		// bin widths for each dimension
		new double[]{1,1}, // bin widths
		// bins start from 0 in both dimensions
		new double[]{0,0}, // boundaries start
		// Some magic: specify what backing store you want
		// can also be DenseArray.class
		// from: http://stackoverflow.com/questions/37231043/how-to-keep-generic-type-of-nested-generics-with-class-tokens
		(Class<SparseArray<Histobin>> )(Class<?> )SparseArray.class
	);

	// and here we are adding data to the histogram
	// this says increment the count of the bin with this LABEL
	ahist.increment_bin_count(new String[]{"a", "a"});
	// whereas this one finds the bin given coordinates of the data
	// e.g. data (0.1,0.3) falls into the bin (0,0) (given that they
	// start from 0 and have a width of 1 (see above)
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

	// here we decrement bin counts
	System.out.println("******************* DECREMENTING ");
	ahist.decrement_bin_count(new double[]{1.2, 1.4});
	ahist.decrement_bin_count(new double[]{0.2, 0.2});
	ahist.decrement_bin_count(new double[]{0.2, 0.2});
	ahist.decrement_bin_count(new double[]{0.2, 0.2});
	ahist.decrement_bin_count(new double[]{0.2, 0.2});
	System.out.println("after decrement Histogram with preset-labels: "+ahist);

	// Here we are sampling from the histogram
	// basically we are asking for the content of a bin
	// it will return a Histobin object
	Histobin abin;
	// select by a coordinate inside a bin:
	abin = ahist.get_bin(new double[]{0.2, 0.2});
	System.out.println("Selected bin: "+abin);
	// select by bin-label
	abin = ahist.get_bin(new String[]{"a", "b"});
	System.out.println("Selected bin: "+abin);
	// select by bin-coordinate (integer)
	abin = ahist.get_bin(new int[]{0, 1});
	System.out.println("Selected bin: "+abin);
	
	// This is where the magic happens
	// we are asking for bins selected by a wildcard: first dim to be 'a' and second dim any (*)
	// (courtesy of the CARTESIAN module)
	Random arng = new Random(1234);
	HistogramSampler asampler = ahist.sampler(arng);
	// get all bins whose first coordinate is 'a' and second is anything (*)
	asampler.select_bins_using_wildcard_labels(new String[]{"a", "*"});
	// and print those bins:
	System.out.println("random bin: with '0-*' : "+asampler.random_bin_from_selection());
}
}
```

The above example can be run using
```
ant clean && ant && ant TestHistograms_SparseArray
```

There is more magic to this module which will be documented
in due time.

author: andreas hadjiprocopis (andreashad2@gmail.com)
