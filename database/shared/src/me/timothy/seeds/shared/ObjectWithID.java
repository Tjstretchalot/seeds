package me.timothy.seeds.shared;

/**
 * Describes an object with a unique integer id associated to it.
 * 
 * @author Timothy
 */
public interface ObjectWithID {
	/**
	 * Fetch the id associated with this object. This operation is assumed to be very fast.
	 * @return the id associated with this object
	 */
	public int id();
}
