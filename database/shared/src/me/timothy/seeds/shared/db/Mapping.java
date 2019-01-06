package me.timothy.seeds.shared.db;

/**
 * Describes something which maps a given type of object.
 * 
 * @author Timothy
 *
 * @param <A> the type of object mapped
 */
public interface Mapping <A> {
	/**
	 * Open the mapping
	 */
	public void open();
	
	/**
	 * Flush the mapping to file
	 */
	public void flush();
	
	/**
	 * Close the mapping
	 */
	public void close();
}
