package	ahp.org.Histograms;

import	java.util.Random;
import  java.io.FileNotFoundException;
import  java.io.PrintWriter;
import	java.util.Arrays;
import	java.util.Collections;
import	java.util.Hashtable;
import	java.util.Comparator;

public class   Histobin1D implements Comparable<Histobin1D> {
	public	int     i;
	public	double  left_boundary,
		right_boundary;
	public	String  label;
	public	int     count = 0;
	public	double  norm_count = 0.0,
		cum_count = 0.0;

	public Histobin1D(int _i, int c, double lb, double rb){ set(_i, c, lb, rb); }

	public  void    set(int _i, int c, double lb, double rb){
		this.i = _i;
		this.count = c;
		this.left_boundary = lb;
		this.right_boundary = rb;
	}
	public  Histobin1D clone(){
		Histobin1D ahistobin = new Histobin1D(
			this.i,
			this.count,
			this.left_boundary,
			this.right_boundary
		);
		ahistobin.label = new String(this.label);
		ahistobin.cum_count = this.cum_count;
		ahistobin.norm_count = this.norm_count;

		return ahistobin;
	}
	// copy all our values to this destination obj
	public  void    copy(Histobin1D destination_obj){
		destination_obj.i = this.i;
		destination_obj.count = this.count;
		destination_obj.left_boundary = this.left_boundary;
		destination_obj.right_boundary = this.right_boundary;
		destination_obj.cum_count = this.cum_count;
		destination_obj.norm_count = this.norm_count;
		destination_obj.label = new String(this.label);
	}
	public  int     compareTo(Histobin1D another_histobin){ 
		double shit = this.cum_count - another_histobin.cum_count;
		if( shit > 0.0 ){ return 1; }
		else if( shit < 0.0 ){ return -1; }
		return 0;
	}
	public  void    reset(){
		// resets the bin counts (not widths, number of bins etc.)
		this.count = 0;
		this.norm_count = 0.0;
		this.cum_count = 0.0;
	}
	public  String  toString(){
		return new String(this.label+" => "+this.count+", "+this.norm_count+", "+this.cum_count);
	}
	public  String  toString_as_histoplot(String sep){
		return new String(this.i+sep+this.count);
	}
}
