package me.timothy.seeds.shared;

/**
 * A serializer than additionally guarrantees a max capacity
 * 
 * @author Timothy
 */
public interface FixedSerializer<A> extends Serializer<A> {
	/**
	 * Returns the maximum size in bytes required for any write call.
	 * 
	 * @return maximum size in bytes for a write call
	 */
	public int maxSize();
}
