package	ahp.org.Histograms;

import	java.util.Random;
import	java.io.FileNotFoundException;
import	java.io.PrintWriter;
import	java.util.Comparator;
// so not thread-safe:
import	java.util.HashMap;
import	java.util.Arrays;

public class	Histobin3D implements Comparable<Histobin3D> {
	public	String	label;
	public	int	i, j, k;
	public	double	count = 0.0; // just the count
	public	double	norm_count = 0.0; // the normalised count
	public	double	cum_count = 0.0; // the normalised cumulative count
	public	double	left_boundaryX,
		right_boundaryX,
		left_boundaryY,
		right_boundaryY,
		left_boundaryZ,
		right_boundaryZ;

	public Histobin3D(
		int _i, int _j, int _k,
		double c, // count
		double lbX, double rbX, // ranges
		double lbY, double rbY,
		double lbZ, double rbZ
	){
		this.set(_i, _j, _k, c, lbX, rbX, lbY, rbY, lbZ, rbZ);
	}
	public	void	reset(){
		// resets the bin counts (not widths, number of bins etc.)
		this.count = 0;
		this.norm_count = 0.0;
		this.cum_count = 0.0;
	}
	public	void	set(
		int _i, int _j, int _k,
		double c, // count
		double lbX, double rbX, // ranges
		double lbY, double rbY,
		double lbZ, double rbZ
	){
		this.i = _i;
		this.j = _j;
		this.k = _k;
		this.count = c;
		this.left_boundaryX = lbX;
		this.right_boundaryX = rbX;
		this.left_boundaryY = lbY;
		this.right_boundaryY = rbY;
		this.left_boundaryZ = lbZ;
		this.right_boundaryZ = rbZ;
	}
	public	int	compareTo(Histobin3D another_histobin){
		double	shit = this.cum_count - another_histobin.cum_count;
		if( shit > 0.0 ){ return 1; }
		else if( shit < 0.0 ){ return -1; }
		return 0;
	}
	// copy all our values to the destination obj
	public	void	copy(Histobin3D destination_obj){
		destination_obj.i = this.i;
		destination_obj.j = this.j;
		destination_obj.k = this.k;
		destination_obj.count = this.count;
		destination_obj.left_boundaryX = this.left_boundaryX;
		destination_obj.right_boundaryX = this.right_boundaryX;
		destination_obj.left_boundaryY = this.left_boundaryY;
		destination_obj.right_boundaryY = this.right_boundaryY;
		destination_obj.left_boundaryZ = this.left_boundaryZ;
		destination_obj.right_boundaryZ = this.right_boundaryZ;

		destination_obj.label = new String(this.label);
		destination_obj.cum_count = this.cum_count;
		destination_obj.norm_count = this.norm_count;
	}
	public	Histobin3D clone(){
		Histobin3D ahistobin = new Histobin3D(
			this.i, this.j, this.k,
			this.count,
			this.left_boundaryX,
			this.right_boundaryX,
			this.left_boundaryY,
			this.right_boundaryY,
			this.left_boundaryZ,
			this.right_boundaryZ
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
		return new String(this.i+sep+this.j+sep+this.k+sep+this.count);
	}

}
