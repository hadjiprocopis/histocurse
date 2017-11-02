package	ahp.org.Histograms;

import	java.util.Random;
import	java.io.FileNotFoundException;
import	java.io.PrintWriter;
import	java.util.Comparator;
// so not thread-safe:
import	java.util.HashMap;
import	java.util.Arrays;

public class	Histobin2D implements Comparable<Histobin2D> {
	public	String	label;
	public	int	i, j;
	public	double	count = 0.0; // just the count
	public	double	norm_count = 0.0; // the normalised count
	public	double	cum_count = 0.0; // the normalised cumulative count
	public	double	left_boundaryX,
		right_boundaryX,
		left_boundaryY,
		right_boundaryY;

	public Histobin2D(int _i, int _j, double c, double lbX, double rbX, double lbY, double rbY){
		this.set(_i, _j, c, lbX, rbX, lbY, rbY);
	}
	public	void	reset(){
		// resets the bin counts (not widths, number of bins etc.)
		this.count = 0;
		this.norm_count = 0.0;
		this.cum_count = 0.0;
	}
	public	void	set(int _i, int _j, double c, double lbX, double rbX, double lbY, double rbY){
		this.i = _i;
		this.j = _j;
		this.count = c;
		this.left_boundaryX = lbX;
		this.right_boundaryX = rbX;
		this.left_boundaryY = lbY;
		this.right_boundaryY = rbY;
	}
	public	int	compareTo(Histobin2D another_histobin){
		double	shit = this.cum_count - another_histobin.cum_count;
		if( shit > 0.0 ){ return 1; }
		else if( shit < 0.0 ){ return -1; }
		return 0;
	}
	// copy all our values to the destination obj
	public	void	copy(Histobin2D destination_obj){
		destination_obj.i = this.i;
		destination_obj.j = this.j;
		destination_obj.count = this.count;
		destination_obj.left_boundaryX = this.left_boundaryX;
		destination_obj.right_boundaryX = this.right_boundaryX;
		destination_obj.left_boundaryY = this.left_boundaryY;
		destination_obj.right_boundaryY = this.right_boundaryY;

		destination_obj.label = new String(this.label);
		destination_obj.cum_count = this.cum_count;
		destination_obj.norm_count = this.norm_count;
	}
	public	Histobin2D clone(){
		Histobin2D ahistobin = new Histobin2D(
			this.i,
			this.j,
			this.count,
			this.left_boundaryX,
			this.right_boundaryX,
			this.left_boundaryY,
			this.right_boundaryY
		);
		ahistobin.label = this.label;
		ahistobin.cum_count = this.cum_count;
		ahistobin.norm_count = this.norm_count;

		return ahistobin;
	}
	public	String	toString(){
		return new String(this.label+" = "+this.count+", "+this.norm_count+", "+this.cum_count);
	}
	public  String  toString_as_histoplot(String sep){
		return new String(this.i+sep+this.j+sep+this.count);
	}
}
