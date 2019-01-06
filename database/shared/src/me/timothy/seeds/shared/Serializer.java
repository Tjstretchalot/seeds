package me.timothy.seeds.shared;

import java.nio.ByteBuffer;

/**
 * Describes something that is capable of writing a specific type of object
 * 
 * @author Timothy
 *
 * @param <A> the type of object that can be written
 */
public interface Serializer<A> {
	/**
	 * Writes the given object to the given output. You do not need to write the id.
	 * @param a the thing to write 
	 * @param out where to write the thing
	 * @return the number of bytes written
	 */
	public int write(A a, ByteBuffer out);
	
	/**
	 * Read the result from write()
	 * 
	 * @param in the input 
	 * @return the object read
	 */
	public A read(int id, ByteBuffer in);
}
