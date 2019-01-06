package me.timothy.seeds.shared.db;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel.MapMode;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import me.timothy.seeds.shared.FixedSerializer;
import me.timothy.seeds.shared.ObjectWithID;

/**
 * Handles mapping an object that is typically looked up by id. This uses a FixedSerializer to allow
 * inlining the maximum number of units per physical sector size (4096 bytes), padding the remaining.
 * It uses a MappedMemoryFile as the method of fetching the data, but still attempts to avoid page
 * faults where possible.
 * 
 * The implementation is effectively a padded HashMap on a memory mapped file, with overhead for
 * serializing/deserializing and size optimizations more appropriate for this type of backing.
 * 
 * @author Timothy
 *
 * @param <A>
 */
public class InlineObjectWithIDMapping<A extends ObjectWithID> implements Mapping<A> {
	/**
	 * The threshold for memory usage during a reslot where we swap to copying using
	 * a swap file.
	 */
	private static final long REMOVE_RESLOT_MEMORY_THRESHOLD = 1024 * 1024 * 16; // 16 megabytes 
	/**
	 * Number of bytes per physical sector
	 */
	private static final int SECTOR_SIZE = 4096;
	
	/**
	 * This is the minimum sectors that we allocate regardless of the size of each element.
	 */
	private static final int MINIMUM_SECTORS = 16;
	
	/** 
	 * The thing capable of serializing the object
	 */
	private final FixedSerializer<A> serializer;
	
	/**
	 * A path to the file
	 */
	private final String filePath;
	
	/**
	 * The swap file we use
	 */
	private final File swpFile;
	
	/**
	 * The actual random access file instance
	 */
	private RandomAccessFile file;
	
	/**
	 * The memory mapped file
	 */
	private MappedByteBuffer buffer;
	
	/**
	 * The size that we pad every single object to, in bytes. This includes the heading bytes
	 * which are (byte - exists, int - id, int - next)
	 */
	private final int paddedSizeEach;
	/**
	 * How many items we can fit into a single sector
	 */
	private final int numPerSector;
	
	/**
	 * The left-most index that is empty, such that all indexes smaller than it are not empty
	 */
	private int filledLeftOf;
	
	/**
	 * The number of items in the mapping
	 */
	private int length;
	
	/**
	 * How many sectors we have loaded
	 */
	private int numSectors;
	
	/**
	 * The capacity, which is the nearest power of 2 below numSectors * numPerSector
	 */
	private int capacity;
	
	/**
	 * Prevents this map from resizing.
	 */
	private boolean preventResize;
	
	public InlineObjectWithIDMapping(String filePath, FixedSerializer<A> serializer) {
		this.filePath = filePath;
		this.swpFile = new File(filePath + ".swp");
		this.serializer = serializer;
		
		if(swpFile.exists())
			throw new IllegalStateException("swap file already exists!");
		
		int actSizeEach = serializer.maxSize();
		if(actSizeEach > SECTOR_SIZE - 9)
			throw new IllegalArgumentException("Fixed serializer requires " + actSizeEach + " bytes, which is too many to fit in a single physical sector and thus should not be inlined");
		
		paddedSizeEach = actSizeEach + 9;
		numPerSector = SECTOR_SIZE / paddedSizeEach;
		
		file = null;
		buffer = null;
		length = 0;
		numSectors = 0;
		capacity = 0;
	}
	
	/**
	 * Sets if this map is allowed to resize. While false, arbitrary errors occur when it exceeds
	 * its capacity. This is only intended for testing.
	 */
	public void setPreventResize(boolean val) {
		this.preventResize = val;
		if(!val)
			maybeExpand();
	}

	@Override
	public void open() {
		if(buffer != null)
			throw new IllegalStateException("Called open() when already open!");
		
		File actualFile = new File(filePath);
		if(!actualFile.exists())
		{
			try {
				allocateFile(actualFile, MINIMUM_SECTORS);
				file = new RandomAccessFile(actualFile, "rw");
				buffer = file.getChannel().map(MapMode.READ_WRITE, 0, MINIMUM_SECTORS * SECTOR_SIZE);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			
			length = 0;
			numSectors = MINIMUM_SECTORS;
			calculateCapacity();
			return;
		}
		
		long numBytesL = actualFile.length();
		if(numBytesL > Integer.MAX_VALUE)
			throw new IllegalStateException("actualFile is too big to mmap!");
		
		int numBytes = (int) numBytesL;
		numSectors = numBytes / SECTOR_SIZE;
		if(numSectors * SECTOR_SIZE != numBytes)
			throw new IllegalStateException("actualFile is not a round number of sectors; can't be from us!");
		calculateCapacity();
		
		try {
			file = new RandomAccessFile(actualFile, "rw");
			buffer = file.getChannel().map(MapMode.READ_WRITE, 0, numBytesL);
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
		
		filledLeftOf = -1;
		length = 0;
		for(int index = 0; index < capacity; index++) {
			int sector = index / numPerSector;
			int indInsideSector = index - sector * numPerSector;
			buffer.position(sector * SECTOR_SIZE + indInsideSector * paddedSizeEach);
			boolean exists = buffer.get() == 1;
			if(exists) {
				length++;
			}else if(filledLeftOf == -1) {
				filledLeftOf = index;
			}
		}
		
		if(filledLeftOf == -1) {
			throw new IllegalStateException("the entire map is full? That doesn't make sense!");
		}
	}
	
	private void calculateCapacity() { 
		int power = 16;
		int maxCapac = numSectors * numPerSector;
		while(power < maxCapac) {
			power <<= 1;
		}
		if(power == maxCapac) {
			capacity = power;
		}else {
			capacity = power >> 1;
		}
	}
	
	/**
	 * Creates the specified file for the first time and allocates the given number of bytes to it
	 * 
	 * @param file the file to allocate
	 * @param numSectors the number of sectors to allocate
	 * @throws IOException if one occurs
	 */
	private void allocateFile(File file, int numSectors) throws IOException {
		byte[] block = new byte[SECTOR_SIZE];
		try(FileOutputStream out = new FileOutputStream(file)) {
			for(int i = 0; i < numSectors; i++) {
				out.write(block);
			}
		}
	}

	@Override
	public void flush() {
		if(buffer == null) 
			throw new IllegalStateException("Cannot force when not opened!");
		
		buffer.force();
	}

	@Override
	public void close() {
		if(buffer == null)
			throw new IllegalStateException("Cannot close when not opened!");
		
		try {
			buffer.force();
			file.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			file = null;
			buffer = null;
		}
		
		/* 
		 * Memory mapped files don't like to respect file closing. However, it's a serious pain for 
		 * other classes to work around if our close function doesn't close correctly, so we force
		 * the issue.
		 */
		boolean releasedLock = false;
		File f = new File(filePath);
		for(int i = 0; i < 10; i++) {
			System.gc();
			if(f.renameTo(f)) {
				releasedLock = true;
				break;
			}
			Thread.yield();
		}
		
		if(!releasedLock) {
			// one last desperate attempt
			try {
				Thread.sleep(1000);
				System.gc();
				Thread.sleep(1000);

				if(f.renameTo(f)) {
					releasedLock = true;
				}
			}catch(InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		if(!releasedLock) {
			throw new IllegalStateException("Failed to release file lock!");
		}
	}
	
	/**
	 * Determines if we need to expand and does so
	 * @return true if we expanded, false otherwise
	 */
	private boolean maybeExpand() {
		if(preventResize || (length + 1) < (capacity - (capacity >> 2)))
			return false; // 3/4 load factor
		
		// This is far from ideal, but presumably it doesn't happen very often
		try {
			final int oldNumSectors = numSectors;
			final File tmpFile = new File(filePath + ".tmp");
			
			close();
			Files.move(Paths.get(filePath), tmpFile.toPath());
			
			capacity <<= 1;
			numSectors = (int)Math.ceil(capacity / (double)numPerSector);

			byte[] block = new byte[SECTOR_SIZE];
			File actualFile = new File(filePath);
			try(FileOutputStream fos = new FileOutputStream(actualFile)) {
				for(int i = 0; i < numSectors; i++) {
					fos.write(block);
				}
			}

			file = new RandomAccessFile(actualFile, "rw");
			buffer = file.getChannel().map(MapMode.READ_WRITE, 0, numSectors * SECTOR_SIZE);
			filledLeftOf = 0;
			length = 0;
			
			ByteBuffer blockWr = ByteBuffer.wrap(block);
			try(FileInputStream fis = new FileInputStream(tmpFile)) {
				for(int sector = 0; sector < oldNumSectors; sector++) {
					fis.read(block);
					for(int i = 0; i < block.length; i += paddedSizeEach) {
						blockWr.position(i);
						if(blockWr.get() == 1) { // exists
							final int id = blockWr.getInt(); // id
							blockWr.getInt(); // next
							final A val = serializer.read(id, blockWr);
							put(val);
						}
					}
				}
			}
			tmpFile.delete();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		return true;
	}
	
	/**
	 * This increments filled left of until we get to an empty spot. This will
	 * move the buffer!
	 */
	private void incrementFilledLeftOf() {
		while(true) {
			filledLeftOf++;
			int sector = filledLeftOf / numPerSector;
			int inIndSec = filledLeftOf - sector * numPerSector;
			
			buffer.position(sector * SECTOR_SIZE + inIndSec * paddedSizeEach);
			if(buffer.get() == 0)
				break;
		}
	}
	
	/**
	 * For debugging only
	 */
	public void assertSane() {
		HashSet<Integer> ids = new HashSet<>();
		HashSet<Integer> filledIndexes = new HashSet<>();
		HashSet<Integer> shouldBeFilledIndexes = new HashSet<>();
		for(int i = 0; i < capacity; i++) {
			int sector = i / numPerSector;
			int inIndSec = i - sector * numPerSector;
			
			buffer.position(sector * SECTOR_SIZE + inIndSec * paddedSizeEach);
			boolean exists = buffer.get() == 1;
			
			if(exists) {
				shouldBeFilledIndexes.remove(i);
				filledIndexes.add(i);
				int id = buffer.getInt();
				if(ids.contains(id))
					throw new IllegalStateException("duplicate id");
				ids.add(id);
				
				int next = buffer.getInt();
				if(next != 0) {
					if(next < i) {
						if(!filledIndexes.contains(next - 1))
							throw new IllegalStateException("index " + (next - 1) + " should have been filled; i=" + i + ", next=" + next);
					}else {
						shouldBeFilledIndexes.add(next - 1);
					}
				}
			}else if(shouldBeFilledIndexes.contains(i)) {
				throw new IllegalStateException("index " + i + " should be filled");
			}
		}
	}
	
	/**
	 * Returns an interator over all of the elements in this mapping. Not thread-safe,
	 * not safe to changes, not fail-fast, O(n) performance with a high constant factor
	 * 
	 * @return an iterator over the entire mapping
	 */
	public Iterator<A> iterAll() {
		return new InlineObjectWithIDMappingIter();
	}
	
	/**
	 * Puts the given object into the mapping. Overwrites the existing value if there is
	 * one.
	 * 
	 * @param a the object to write
	 */
	public void put(A a) {
		final int ind = a.id() & (capacity - 1);
		final int sector = ind / numPerSector;
		final int inIndSec = ind - sector * numPerSector;
		
		buffer.position(sector * SECTOR_SIZE + inIndSec * paddedSizeEach);
		buffer.mark();
		if(buffer.get() == 0) {
			length++;
			buffer.reset();
			buffer.put((byte)1); // exists
			buffer.putInt(a.id()); // id
			buffer.putInt(0); // next
			serializer.write(a, buffer);
			
			if(filledLeftOf == ind) {
				if(capacity == filledLeftOf + 1) {
					maybeExpand();
					return;
				}
				
				incrementFilledLeftOf();
			}
			return;
		}
		
		final int ogCollisionInd = buffer.getInt();
		if(ogCollisionInd == a.id()) {
			buffer.getInt(); // next
			serializer.write(a, buffer);
			return;
		}
		
		if(maybeExpand()) {
			put(a);
			return;
		}
		
		/* We will use the marker to keep track of the NEXT to overwrite */
		buffer.mark();
		int next = buffer.getInt();
		while(next != 0) {
			int nextSector = (next - 1) / numPerSector;
			int nextIndInSec = (next - 1) - nextSector * numPerSector;
			
			buffer.position(nextSector * SECTOR_SIZE + nextIndInSec * paddedSizeEach + 1); // we skip exists; next guarantees exists
			int nextID = buffer.getInt();
			if(nextID == a.id()) {
				buffer.getInt(); // next
				serializer.write(a, buffer);
				return;
			}
			buffer.mark();
			next = buffer.getInt();
		}
		
		buffer.reset();
		buffer.putInt(filledLeftOf + 1);
		
		int filledLeftOfSector = filledLeftOf / numPerSector;
		int filledLeftOfIndInSec = filledLeftOf - filledLeftOfSector * numPerSector;
		
		buffer.position(filledLeftOfSector * SECTOR_SIZE + filledLeftOfIndInSec * paddedSizeEach);
		buffer.put((byte)1); // exists
		buffer.putInt(a.id()); // id
		buffer.putInt(0); // next
		serializer.write(a, buffer);
		length++;
		incrementFilledLeftOf();
	}
	
	/**
	 * This is a reslotting function. It assumes the raw buffer is such that from position
	 * to limit is the serialized object. Does not change the length but does update the 
	 * filledLeftOf value
	 * 
	 * @param id the id of the object to reslot
	 * @param raw the serialized object to reslot
	 */
	private void reslotRaw(int id, ByteBuffer raw) {
		final int ind = id & (capacity - 1);
		final int sector = ind / numPerSector;
		final int indInSec = ind - sector * numPerSector;
		
		buffer.position(sector * SECTOR_SIZE + indInSec * paddedSizeEach);
		buffer.mark();
		if(buffer.get() == 0) { // exists
			buffer.reset();
			buffer.put((byte)1); // exists
			buffer.putInt(id); // id
			buffer.putInt(0); // next
			buffer.put(raw); // object

			if(filledLeftOf == ind) {
				incrementFilledLeftOf();
			}
			return;
		}
		
		buffer.getInt(); // id, irrelevant since we assume id is unique
		
		/* we will use the mark to keep track of the NEXT to overwrite */
		buffer.mark();
		int next = buffer.getInt();
		while(next != 0) {
			int nextSector = (next - 1) / numPerSector;
			int nextIndInSec = (next - 1) - nextSector * numPerSector;
			
			buffer.position(nextSector * SECTOR_SIZE + nextIndInSec * paddedSizeEach + 5); // skip exists, id
			buffer.mark();
			next = buffer.getInt();
		}
		
		buffer.reset();
		buffer.putInt(filledLeftOf + 1);

		int filledLeftOfSector = filledLeftOf / numPerSector;
		int filledLeftOfIndInSec = filledLeftOf - filledLeftOfSector * numPerSector;
		
		buffer.position(filledLeftOfSector * SECTOR_SIZE + filledLeftOfIndInSec * paddedSizeEach);
		buffer.put((byte)1); // exists
		buffer.putInt(id); // id
		buffer.putInt(0); // next
		buffer.put(raw); // object
		incrementFilledLeftOf();
	}
	
	/**
	 * Gets the object with the given id if it is in the mapping, otherwise returns null
	 * 
	 * @param id the id to lookup
	 * @return the object with that id
	 */
	public A get(int id) {
		final int ind = id & (capacity - 1);
		final int sector = ind / numPerSector;
		final int indInSec = ind - sector * numPerSector;
		
		buffer.position(sector * SECTOR_SIZE + indInSec * paddedSizeEach);
		if(buffer.get() == 0) { // exists
			return null;
		}
		
		final int buckID = buffer.getInt(); // id
		if(buckID == id) {
			buffer.getInt(); // next
			A res = serializer.read(id, buffer);
			return res;
		}
		
		int next = buffer.getInt(); // next
		
		while(next != 0) {
			final int nextSector = (next - 1) / numPerSector;
			final int nextIndInSec = (next - 1) - nextSector * numPerSector;
			buffer.position(nextSector * SECTOR_SIZE + nextIndInSec * paddedSizeEach + 1); // skip exists ; next guarantees existence
			int nextID = buffer.getInt(); // id
			if(nextID == id) {
				buffer.getInt(); // next
				A res = serializer.read(id, buffer);
				return res;
			}
			next = buffer.getInt(); // next
		}
		
		return null;
	}
	
	/**
	 * Uses a swap file to reslot all the parts of the chain. Assumes that upToNow contains
	 * byte arrays that each correspond to (first 4 bytes are an integer) followed by the
	 * remaining bytes are a serialized "A" object.
	 * 
	 * This must not check the existing of the first object it checks.
	 * 
	 * @param upToNow corresponds with the ones we already pushed into memory before deciding to use a swap file
	 * @param next the index+1 that we are currently at when we decided to use a swap file. Must NOT be in upToNow
	 * @param skipID if set, the index to NOT reslot (just delete)
	 */
	private void reslotChainWithSwapFile(List<byte[]> upToNow, int next, Integer skipIndex) {
		final int ogLimit = buffer.limit();
		final boolean haveSkip = skipIndex != null;
		final int skipUnwrapped = haveSkip ? skipIndex : -1;
		
		try {
			try (BufferedOutputStream swpOut = new BufferedOutputStream(new FileOutputStream(swpFile))) {
				for(byte[] arr : upToNow) {
					swpOut.write(arr);
				}
				upToNow.clear();
				upToNow = null;
				
				WritableByteChannel chan = Channels.newChannel(swpOut);
				while(next != 0) {
					int nextSector = (next - 1) / numPerSector;
					int nextIndInSec = (next - 1) - nextSector * numPerSector;
					
					buffer.position(nextSector * SECTOR_SIZE + nextIndInSec * paddedSizeEach);
					buffer.put((byte)0); // delete the entry
					if(haveSkip) {
						buffer.mark(); // we will need the id again
						int id = buffer.getInt(); // read id
						if(id == skipUnwrapped) {
							next = buffer.getInt(); // read next
							continue; // skip saving it
						}
						buffer.reset(); // go back to the id
					}
					buffer.limit(buffer.position() + 4); // write just the id
					chan.write(buffer);
					buffer.limit(ogLimit); // go back to the original limit
					next = buffer.getInt(); // update next 
					buffer.limit(buffer.position() + paddedSizeEach - 9); // write out just the item 
					chan.write(buffer);
					buffer.limit(ogLimit); // return to original limit
				}
			}
			
			ByteBuffer inBuff = ByteBuffer.allocate(paddedSizeEach - 5); // we dont have exists / next
			try(ReadableByteChannel swpIn = Channels.newChannel(new BufferedInputStream(new FileInputStream(swpFile)))) {
				while(swpIn.read(inBuff) > 0) {
					while(inBuff.position() != paddedSizeEach - 5) {
						if(swpIn.read(inBuff) <= 0)
							throw new IllegalStateException("partial read!");
					}
					
					inBuff.position(0);
					int id = inBuff.getInt();
					reslotRaw(id, inBuff);
					inBuff.clear();
				}
			}
			
			swpFile.delete();
		}catch(IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Delete the object with the given id and return the object deleted, or null if there
	 * was no object with that id.
	 * 
	 * @param id the id to delete
	 * @return the object deleted 
	 */
	public A remove(int id) { 
		final int ind = id & (capacity - 1);
		final int sector = ind / numPerSector;
		final int indInSec = ind - sector * numPerSector;
		
		buffer.position(sector * SECTOR_SIZE + indInSec * paddedSizeEach);
		buffer.mark();
		if(buffer.get() == 0) { // exists
			return null;
		}
		
		
		final int ogFoundID = buffer.getInt();
		final int ogFoundNext = buffer.getInt();
		
		if(ogFoundNext == 0) {
			if(ogFoundID != id) { return null; }
			
			length--;
			if(filledLeftOf > ind)
				filledLeftOf = ind;
			final A ogValue = serializer.read(ogFoundID, buffer);
			buffer.reset();
			buffer.put((byte)0);
			return ogValue;
		}
		
		// If we are the end of the chain it's safe to do a raw update, in all other cases we must reslot everything.
		// If this is performing well the chain should be short, so given that we're not the beginning the odds are
		// reasonable that we're at the end, so we assume that case
		
		if(ogFoundID == id) {
			// We're the beginning of the chain, we need to reslot the remainder. As we go through, we'll delete and
			// retrieve every block, storing them. If we exceed a threshold of memory, we use a swap file for this.
			length--;
			
			final A ogValue = serializer.read(ogFoundID, buffer);
			buffer.reset();
			buffer.put((byte)0);
			
			if(filledLeftOf > ind)
				filledLeftOf = ind;
			
			long bytesUsed = 0;
			List<byte[]> slices = new ArrayList<>();
			
			int next = ogFoundNext;
			while(next != 0) {
				int nextSector = (next - 1) / numPerSector;
				int nextIndInSector = (next - 1) - nextSector * numPerSector;
				
				buffer.position(nextSector * SECTOR_SIZE + nextIndInSector * paddedSizeEach);
				buffer.put((byte)0); // delete the entry
				
				if(filledLeftOf > (next - 1))
					filledLeftOf = next - 1;
				
				buffer.mark(); // we'll need the id again to put it in the slice
				buffer.getInt(); // id
				int nextNext = buffer.getInt();
				
				if(nextNext != 0 // never use a swap file for the final part; that's a waste
					&& bytesUsed + paddedSizeEach - 5 > REMOVE_RESLOT_MEMORY_THRESHOLD) {
					reslotChainWithSwapFile(slices, next, null);
					return ogValue;
				}
				
				byte[] slice = new byte[paddedSizeEach - 5];
				
				// copy the id over
				buffer.reset();
				buffer.get(slice, 0, 4);
				
				// skip next
				buffer.getInt();
				
				// copy the serialized object over
				buffer.get(slice, 4, paddedSizeEach - 9);
				
				// add to slices
				slices.add(slice);
				
				// update loop counters
				next = nextNext;
				bytesUsed += paddedSizeEach - 5;
			}
			
			for(int i = 0; i < slices.size(); i++) {
				ByteBuffer wrapped = ByteBuffer.wrap(slices.get(i));
				int sliceID = wrapped.getInt();
				reslotRaw(sliceID, wrapped);
			}
			
			return ogValue;
		}
		
		// If we're in the middle of the chain we need to reslot, otherwise we're at the end of the chain
		// and don't need to do any work. As in the starting case, we will use a swap file if we are in
		// the middle of the chain and the memory usage to reslot exceeds a threshold.
		// Of course if we're not in the chain we don't need to do anything.

		int prevInd = ind;
		int next = ogFoundNext;
		boolean nextIsUS = false; // if set to true, we have just read the id, so next corresponds to us and buffer.getInt() corresponds to our next
		while(next != 0) { // ogFoundNext is not 0, so this loop always enters, so we always set the mark.
			int nextSector = (next - 1) / numPerSector;
			int nextIndInSec = (next - 1) - nextSector * numPerSector;
			
			buffer.position(nextSector * SECTOR_SIZE + nextIndInSec * paddedSizeEach); // keep exists for the mark
			buffer.mark();
			buffer.get(); // exists
			int nextID = buffer.getInt();
			if(nextID == id) {
				nextIsUS = true;
				break;
			}
			
			prevInd = next - 1;
			next = buffer.getInt();
		}
		
		if(!nextIsUS) {
			return null; // We're not in the chain at all
		}

		final int indOfPointerToUs = prevInd;
		final int ourInd = next - 1;
		final int ourNext = buffer.getInt();

		length--;
		if(filledLeftOf > ourInd)
			filledLeftOf = ourInd;
		
		if(ourNext == 0) { 
			// We're at the end of the chain! The mark is at our exists
			buffer.reset();
			buffer.put((byte)0);
			buffer.getInt(); // id
			buffer.getInt(); // next
			final A val = serializer.read(id, buffer);
			
			// remove us from the chain
			int pointerToUsSector = indOfPointerToUs / numPerSector;
			int pointerToUsIndInSector = indOfPointerToUs - pointerToUsSector * numPerSector;
			
			buffer.position(pointerToUsSector * SECTOR_SIZE + pointerToUsIndInSector * paddedSizeEach + 5); // skip exists, id
			buffer.putInt(0); // next
			return val;
		}
		
		// We're in the middle of the chain! We need to go back and reslot everything but us and the first one.
		// we do have to update the next of the first one to be 0
		// We will use a swap file if memory exceeds a threshold
		
		final A val = serializer.read(id, buffer);
		
		buffer.position(sector * SECTOR_SIZE + indInSec * paddedSizeEach + 5); // skip exists, id
		buffer.putInt(0); // next
		
		long bytesUsed = 0;
		List<byte[]> slices = new ArrayList<>();
		boolean foundUs = false; // this is true if & only if we would be in slices if we weren't removing ourself
		
		next = ogFoundNext;
		while(next != 0) { // enters at least once; ogFoundNext != 0
			int nextSector = (next - 1) / numPerSector;
			int nextIndInSec = (next - 1) - nextSector * numPerSector;
			
			buffer.position(nextSector * SECTOR_SIZE + nextIndInSec * paddedSizeEach);
			buffer.put((byte)0); // delete the entry
			
			if(filledLeftOf > (next - 1))
				filledLeftOf = next - 1;
			
			buffer.mark();
			buffer.getInt(); // id
			int nextNext = buffer.getInt();

			if(nextNext != 0 // never use a swap file for the final part; that's a waste
				&& bytesUsed + paddedSizeEach - 5 > REMOVE_RESLOT_MEMORY_THRESHOLD) {
				reslotChainWithSwapFile(slices, next, foundUs ? null : ourInd);
				return val;
			}
			

			if(next == ourInd + 1) {
				foundUs = true;
			}else {
				byte[] slice = new byte[paddedSizeEach - 5]; // take away exists, next
				buffer.reset();
				buffer.get(slice, 0, 4); // id
				buffer.getInt(); // next
				buffer.get(slice, 4, paddedSizeEach - 9); // object
				
				slices.add(slice);
				bytesUsed += paddedSizeEach - 5;
			}
			
			next = nextNext;
		}
		
		for(int i = 0; i < slices.size(); i++) {
			ByteBuffer wrapped = ByteBuffer.wrap(slices.get(i));
			int sliceID = wrapped.getInt();
			reslotRaw(sliceID, wrapped);
		}
		
		return val;
	}
	
	/**
	 * Suggests an id that is not in the mapping for a new object. The id is selected to avoid hash collisions. This will re-use deleted
	 * ids aggressively unless there are already collisions
	 * 
	 * @return a suggested unique id
	 */
	public int suggestID() {
		return filledLeftOf;
	}
	
	/**
	 * Removes everything from the mapping
	 */
	public void clear() {
		length = 0;
		filledLeftOf = 0;
		for(int i = 0; i < capacity; i++) {
			int sector = i / numPerSector;
			int indInSec = i - sector * numPerSector;
			
			buffer.position(sector * SECTOR_SIZE + indInSec * paddedSizeEach);
			buffer.put((byte)0);
		}
	}
	
	/**
	 * An iterator over the entire mapping. Caches by physical sector. 
	 * 
	 * @author Timothy
	 *
	 * @param <A> the resulting object
	 */
	public class InlineObjectWithIDMappingIter implements Iterator<A> {
		private int sectorIndex;
		private int indexInsideSector;
		private A[] aBuffer;
		
		@SuppressWarnings("unchecked")
		public InlineObjectWithIDMappingIter() {
			if(length != 0) {
				aBuffer = (A[]) new Object[numPerSector];
				bufferSector();
			}
		}
		
		@Override
		public boolean hasNext() {
			return aBuffer != null;
		}

		@Override
		public A next() {
			final A result = aBuffer[indexInsideSector];
			while((++indexInsideSector) < aBuffer.length && aBuffer[indexInsideSector] == null);
			if(indexInsideSector == aBuffer.length) {
				sectorIndex++;
				bufferSector();
			}
			return result;
		}
		
		private void bufferSector() {
			if(sectorIndex >= numSectors) {
				aBuffer = null;
				return;
			}
			
			indexInsideSector = -1;
			final int sectorOffset = SECTOR_SIZE * sectorIndex;
			for(int i = 0; i < numPerSector; i++) {
				buffer.position(sectorOffset + paddedSizeEach * i);
				boolean hasEntry = buffer.get() == 1;
				if(hasEntry) {
					int id = buffer.getInt();
					buffer.getInt(); // next
					aBuffer[i] = serializer.read(id, buffer);
				}else {
					aBuffer[i] = null;
				}
			}
		}
	}
}
