package me.timothy.seeds.shared.containers;

/**
 * This map is tuned for maximum performance when integers which are very dense (as if by an 
 * auto-incrementing regime) to objects. Has a load capacity of 3/4.
 * 
 * After performance testing it looks like hashmaps are so fast that even after removing the
 * boxing/unboxing overhead, there is only a ~20% improvement and neither this nor the generic
 * implementation are very consistent. I beleive it's the memory allocation that accounts
 * for the high variation between runs
 * 
 * @author Timothy
 * @param <A> The type of the value
 */
@SuppressWarnings("unchecked")
public class IntObjectMap <A> {
	/**
	 * These are the nodes that we store in each bucket
	 * @author Timothy
	 *
	 * @param <A> the type for the value being stored
	 */
	private static final class BucketEntry<A> {
		/** The key which this entry was stored in. Will have multiple potential collisions, so needs to be rechecked */
		public int key;
		/** The value that was stored with key */
		public A value;
		/** The index + 1 for where to look if this is not the key you were expecting */
		public int next;
	}
	
	private BucketEntry<A>[] buckets;
	private int filledUpTo;
	private int length;
	
	/**
	 * Creates a map from integers to A's, with an initial capacity no less than
	 * the suggested size.
	 * 
	 * @param suggestedSize a suggestion for the capacity of the map.
	 */
	public IntObjectMap(int suggestedSize) {
		int power = 16; // skip the first few
		while(power < suggestedSize)
			power <<= 1;

		buckets = ((BucketEntry<A>[]) new BucketEntry[power]);
	}
	
	private boolean maybeExpand() {
		int capac = buckets.length;
		if(length + 1 < (capac - (capac >> 2)))
			return false;
		int newCapac = capac << 1;
		BucketEntry<A>[] oldBuckets = buckets;
		buckets = new BucketEntry[newCapac];
		filledUpTo = 0;
		
		for(int i = 0; i < capac; i++) {
			BucketEntry<A> entr = oldBuckets[i];
			if(entr != null) {
				entr.next = 0;
				put(entr);
			}
		}
		
		return true;
	}
	
	/**
	 * Returns the value associated with the given key, or null if not known
	 * 
	 * @param key the key of interest
	 * @return the value for that key
	 */
	public A get(int key) {
		int ind = key & (buckets.length - 1);
		BucketEntry<A> entr = buckets[ind];
		if(entr == null)
			return null;
		
		if(entr.key == key)
			return entr.value;
		
		while(entr.next != 0) {
			ind = entr.next - 1;
			entr = buckets[ind];
			if(entr.key == key)
				return entr.value;
		}
		
		return null;
	}
	
	/**
	 * Puts the given value and associates it with the given key. Returns null if the key was
	 * previously not associated with a value, otherwise returns the value previously associated
	 * with the key.
	 * 
	 * @param key the key
	 * @param value the value
	 * @return the value for the key before the put
	 */
	public A put(int key, A value) {
		BucketEntry<A> insEntry = new BucketEntry<>();
		insEntry.key = key;
		insEntry.value = value;
		
		int ind = key & (buckets.length - 1);
		BucketEntry<A> entr = buckets[ind];
		if(entr == null) {
			length++;
			buckets[ind] = insEntry;
			if(filledUpTo == ind) {
				if(buckets.length == filledUpTo + 1) {
					maybeExpand();
					return null;
				}
				
				while(buckets[++filledUpTo] != null);
			}
			return null;
		}
		
		if(maybeExpand())
			return put(key, value);

		if(entr.key == key) {
			final A result = entr.value;
			entr.value = value;
			return result;
		}
		while(entr.next != 0) {
			entr = buckets[entr.next - 1];
			if(entr.key == key) {
				final A result = entr.value;
				entr.value = value;
				return result;
			}
		}
		buckets[filledUpTo] = insEntry;
		entr.next = filledUpTo + 1;
		while(buckets[++filledUpTo] != null);
		length++;
		return null;
	}
	
	/**
	 * This is used for reslotting only. It assumes that our length no greater than the capacity less 1
	 * and that the key is unique. Does not change the length, but does update the filledUpTo value. it
	 * assumes insEntry.next == 0
	 * 
	 * @param insEntry the entry to reslot
	 */
	private void put(BucketEntry<A> insEntry) {
		int ind = insEntry.key & (buckets.length - 1);
		
		BucketEntry<A> entr = buckets[ind];
		if(entr == null) {
			buckets[ind] = insEntry;
			if(filledUpTo == ind) {
				while(buckets[++filledUpTo] != null);
			}
			return;
		}
		
		while(entr.next != 0) {
			entr = buckets[entr.next - 1];
		}
		buckets[filledUpTo] = insEntry;
		entr.next = filledUpTo + 1;
		while(buckets[++filledUpTo] != null);
	}
	
	/**
	 * Removes the value associated with the given key. If there was no value associated with the
	 * key, returns null. Otherwise, returns the old value associated with the given key.
	 * 
	 * @param key the key whose relationship should be removed
	 * @return the old value associated with the key, or null if there was none
	 */
	public A remove(int key) {
		int ind = key & (buckets.length - 1);
		final BucketEntry<A> entr = buckets[ind];
		if(entr == null)
			return null;
		
		if(entr.next == 0) {
			if(entr.key != key) { return null; }

			length--;
			buckets[ind] = null;
			if(filledUpTo > ind)
				filledUpTo = ind;
			return entr.value;
		}
		
		// If we are the end of the chain then it's safe to do a raw update, in all other cases we must reslot everything
		// since the chain may have multiple sources
		
		if(entr.key == key) {
			// We're first, so we need to reslot the remainder of the chain
			length--;
			buckets[ind] = null;
			if(filledUpTo > ind)
				filledUpTo = ind;
			
			int numToReslot = 1;
			BucketEntry<A> curr = buckets[entr.next - 1];
			while(curr.next != 0) {
				numToReslot++;
				curr = buckets[curr.next - 1];
			}
			
			BucketEntry<A>[] toReslot = new BucketEntry[numToReslot];
			int reslotIndex = 0;
			curr = buckets[entr.next - 1];
			buckets[entr.next - 1] = null;
			if(filledUpTo > entr.next - 1)
				filledUpTo = entr.next - 1;
			toReslot[reslotIndex++] = curr;
			while(curr.next != 0) {
				int tmp = curr.next;
				curr.next = 0;
				curr = buckets[tmp - 1];
				buckets[tmp - 1] = null;
				if(filledUpTo > tmp - 1)
					filledUpTo = tmp - 1;
				toReslot[reslotIndex++] = curr;
			}
			for(int i = 0; i < toReslot.length; i++) {
				put(toReslot[i]);
			}
			return entr.value;
		}
		
		int numToReslot = 0; // we never need to reslot the first one
		BucketEntry<A> prev = entr;
		BucketEntry<A> next = null;
		while(prev.next != 0) {
			next = buckets[prev.next - 1];
			if(next.key == key) {
				break;
			}
			prev = next;
			numToReslot++;
		}
		if(prev.next == 0) {
			// we're not in the chain
			return null;
		}
		
		// We are in the chain
		length--;
		
		if(next.next == 0) {
			// we are the end of the chain
			buckets[prev.next - 1] = null;
			if(filledUpTo > prev.next - 1)
				filledUpTo = prev.next - 1;
			prev.next = 0;
			return next.value;
		}
		
		// We are in the middle of the chain - we will figure out how long the chain is before we can update
		
		while(next.next != 0) {
			next = buckets[next.next - 1];
			numToReslot++;
		}
		
		// Now we reslot everyone past the first one that isn't us
		A result = null;
		BucketEntry<A>[] reslot = new BucketEntry[numToReslot];
		int reslotCounter = 0;
		int nextInd = entr.next;
		entr.next = 0;
		while(nextInd != 0) {
			next = buckets[nextInd - 1];
			if(next.key != key) {
				reslot[reslotCounter++] = next;
			}else {
				result = next.value;
			}
			buckets[nextInd - 1] = null;
			nextInd = next.next;
			next.next = 0;
		}
		
		for(int i = 0; i < reslot.length; i++) {
			put(reslot[i]);
		}
		return result;
	}
	
	/**
	 * Clears this map, not freeing the allocation table but freeing the references to the values
	 */
	public void clear() {
		for(int i = 0; i < buckets.length; i++) {
			buckets[i] = null;
		}
		length = 0;
		filledUpTo = 0;
	}
}
