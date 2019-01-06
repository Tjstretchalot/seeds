package me.timothy.seeds.tests.shared.containers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import me.timothy.seeds.shared.FixedSerializer;
import me.timothy.seeds.shared.ObjectWithID;
import me.timothy.seeds.shared.db.InlineObjectWithIDMapping;

public class InlineObjectWithIDMappingTests {
	public static class TestClass implements ObjectWithID {
		public int id;
		public int val;
		
		/** 
		 * Construct a test class with the given id
		 * 
		 * @param id the id
		 * @param val the value
		 */
		public TestClass(int id, int val) {
			this.id = id;
			this.val = val;
		}
		
		public int id() {
			return id;
		}
		
		@Override
		public String toString() {
			return "[TC id=" + id + ", val= " + val +"]";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + id;
			result = prime * result + val;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TestClass other = (TestClass) obj;
			if (id != other.id)
				return false;
			if (val != other.val)
				return false;
			return true;
		}
	}
	
	public static class TestClassSerializer implements FixedSerializer<TestClass> {
		private int maxSize;
		
		public TestClassSerializer(int maxSize) {
			this.maxSize = maxSize;
		}
		
		@Override
		public int write(TestClass a, ByteBuffer out) {
			out.putInt(a.val);
			return 4;
		}

		@Override
		public TestClass read(int id, ByteBuffer in) {
			int val = in.getInt();
			return new TestClass(id, val);
		}

		@Override
		public int maxSize() {
			return maxSize;
		}
		
	}
	
	private File file;
	private TestClassSerializer ser;
	private InlineObjectWithIDMapping<TestClass> map;
	
	private TestClass tc(int id, int val) {
		return new TestClass(id, val);
	}
	
	@Before
	public void setUp() {
		file = new File("test_inline_object_with_id.dat");
		if(file.exists())
			file.delete();
		
		ser = new TestClassSerializer(4);
	}
	
	@Test
	public void testPutGetSmall() {
		map = new InlineObjectWithIDMapping<TestClass>(file.getAbsolutePath(), ser);
		
		map.open();
		map.put(tc(3, 5));
		assertEquals(tc(3, 5), map.get(3));
		assertNull(map.get(5));
		assertNull(map.get(7));
		map.put(tc(5, 7));
		assertEquals(tc(3, 5), map.get(3));
		assertEquals(tc(5, 7), map.get(5));
		assertNull(map.get(7));
		map.put(tc(11, 13));
		assertEquals(tc(3, 5), map.get(3));
		assertEquals(tc(5, 7), map.get(5));
		assertNull(map.get(7));
		assertEquals(tc(11, 13), map.get(11));
	}
	
	@Test
	public void testPutGetDeleteSmall() {
		map = new InlineObjectWithIDMapping<TestClass>(file.getAbsolutePath(), ser);
		
		map.open();
		map.put(tc(32, 5));
		assertEquals(tc(32, 5), map.get(32));
		assertNull(map.get(5));
		map.put(tc(17, 3));
		assertEquals(tc(32, 5), map.get(32));
		assertEquals(tc(17, 3), map.get(17));
		assertNull(map.get(5));
		assertNull(map.remove(9));
		assertEquals(tc(32, 5), map.remove(32));
		assertNull(map.get(32));
		assertEquals(tc(17, 3), map.get(17));
		map.put(tc(17, 5));
		assertEquals(tc(17, 5), map.get(17));
		assertNull(map.remove(32));
		assertEquals(tc(17, 5), map.remove(17));
	}
	
	@Test
	public void testPutGetWithForcedCollisions() {
		ser = new TestClassSerializer(4096 - 9);
		map = new InlineObjectWithIDMapping<>(file.getAbsolutePath(), ser);
		
		// 16 long
		map.open();
		map.put(tc(5 + 16 * 0, 0));
		map.put(tc(5 + 16 * 1, 1));
		map.put(tc(0 + 16 * 3, 7));
		map.put(tc(5 + 16 * 2, 2));
		
		assertEquals(tc(5 + 16 * 0, 0), map.get(5 + 16 * 0));
		assertEquals(tc(5 + 16 * 1, 1), map.get(5 + 16 * 1));
		assertEquals(tc(5 + 16 * 2, 2), map.get(5 + 16 * 2));
		assertEquals(tc(0 + 16 * 3, 7), map.get(0 + 16 * 3)); 
	}
	
	
	@Test
	public void testHugeWorstCaseCollision() throws Exception {
		// this must be a power of 2 for it to match capacity, and we need to know capacity
		// to force collisions
		// This was originally 262144 but it turns out when your hashmap is acting like a terrible list it's really slow
		// so I dropped that down to 32mib
		final int entries = 8192;
		
		
		ser = new TestClassSerializer(4096 - 9);
		try(FileOutputStream fos = new FileOutputStream(file)) {
			byte[] block = new byte[4096];
			for(int i = 0; i < entries + 1; i++) {
				fos.write(block);
			}
		}

		map = new InlineObjectWithIDMapping<>(file.getAbsolutePath(), ser);
		map.setPreventResize(true);
		map.open();
		
		for(int i = 0; i < entries; i++) {
			map.put(tc(entries * i, i));
		}
		
		// we increment by a lot since this operation is expensive and we dont want this test to take forever
		// we dont increment by a power of 2 to increase the odds of catching every scenario
		for(int i = 0; i < entries; i += 1027) { 
			assertEquals(tc(entries * i, i), map.remove(entries * i));
			assertEquals(tc(entries * (i + 1), (i + 1)), map.get(entries * (i + 1)));
		}
	}
	
	@Test
	public void testPutRemoveWithForcedCollisions() {
		ser = new TestClassSerializer(4096 - 9);
		map = new InlineObjectWithIDMapping<>(file.getAbsolutePath(), ser);
		
		map.open();
		
		for(int i = 0; i < 4; i++) {
			map.put(tc(3 + 16 * 0, 0));
			map.put(tc(3 + 16 * 1, 1));
			map.put(tc(3 + 16 * 2, 2));
			map.put(tc(1 + 16 * 3, 7));
	
			assertEquals("i=" + i, tc(3 + 16 * 0, 0), map.get(3 + 16 * 0));
			assertEquals("i=" + i, tc(3 + 16 * 1, 1), map.get(3 + 16 * 1));
			assertEquals("i=" + i, tc(3 + 16 * 2, 2), map.get(3 + 16 * 2));
			assertEquals("i=" + i, tc(1 + 16 * 3, 7), map.get(1 + 16 * 3));
			
			switch(i) {
			case 0:
				assertEquals(tc(3 + 16 * 0, 0), map.remove(3 + 16 * 0));
				assertNull(map.get(3 + 16 * 0));
				assertEquals(tc(3 + 16 * 1, 1), map.get(3 + 16 * 1));
				assertEquals(tc(3 + 16 * 2, 2), map.get(3 + 16 * 2));
				assertEquals(tc(1 + 16 * 3, 7), map.get(1 + 16 * 3));
				break;
			case 1:
				assertEquals(tc(3 + 16 * 1, 1), map.remove(3 + 16 * 1));
				assertEquals(tc(3 + 16 * 0, 0), map.get(3 + 16 * 0));
				assertNull(map.get(3 + 16 * 1));
				assertEquals(tc(3 + 16 * 2, 2), map.get(3 + 16 * 2));
				assertEquals(tc(1 + 16 * 3, 7), map.get(1 + 16 * 3));
				break;
			case 2:
				assertEquals(tc(3 + 16 * 2, 2), map.remove(3 + 16 * 2));
				assertEquals(tc(3 + 16 * 0, 0), map.get(3 + 16 * 0));
				assertEquals(tc(3 + 16 * 1, 1), map.get(3 + 16 * 1));
				assertNull(map.get(3 + 16 * 2));
				assertEquals(tc(1 + 16 * 3, 7), map.get(1 + 16 * 3));
				break;
			case 3:
				assertEquals(tc(1 + 16 * 3, 7), map.remove(1 + 16 * 3));
				assertEquals(tc(3 + 16 * 0, 0), map.get(3 + 16 * 0));
				assertEquals(tc(3 + 16 * 1, 1), map.get(3 + 16 * 1));
				assertEquals(tc(3 + 16 * 2, 2), map.get(3 + 16 * 2));
				assertNull(map.get(1 + 16 * 3));
				break;
			}
			
			map.close();
			file.delete();
			map = new InlineObjectWithIDMapping<>(file.getAbsolutePath(), ser);
			map.open();
		}
	}
	
	@Test
	public void testRegression1() {
		map = new InlineObjectWithIDMapping<>(file.getAbsolutePath(), ser);
		map.open();
	
		map.put(tc(44768, 14));
		map.put(tc(22209, 26));
		map.put(tc(97897, 50));
	}
	
	private static class HistoryItem {
		public byte type; // 0 = add, 1 = delete, 2 = get
		public int id;
		public int val; // for deletes / gets this is the expected value, -1 for null
		
		public HistoryItem(byte type, int id, int val) {
			this.type = type;
			this.id = id;
			this.val = val;
		}
		
		@Override
		public String toString() {
			if(type == 0) {
				return "map.put(tc(" + id + ", " + val + "));";
			}else if(type == 1) {
				if(val == -1) {
					return "assertNull(map.remove(" + id + "));";
				}else {
					return "assertEquals(tc(" + id + ", " + val + "), map.remove(" + id + "));";
				}
			}else {
				if(val == -1) {
					return "assertNull(map.get(" + id + "));";
				}else {
					return "assertEquals(tc(" + id + ", " + val + "), map.get(" + id + "));";
				}
			}
		}
	}
	
	private String errorStr(List<HistoryItem> hist) {
		return Stream.concat(Stream.of(
					"// Total Length: " + hist.size(),
					"map = new InlineObjectWithIDMapping<>(file.getAbsolutePath(), ser);", 
					"map.open();",
					""),
				hist.stream().map(HistoryItem::toString)).collect(Collectors.joining("\n"));
	}
	
	@SuppressWarnings("unused")
	@Test
	public void testRandom() {
		final boolean sanityCheck = false; // calls assertSane
		final boolean forceCheckEvery = false; // improves granularity but is unlikely to improve catching/not catching. good when you know theres a bug
		final boolean padToFullSector = true; // force padding to full sector, need many fewer operations to test resizing
		final int numRepeats = 100;
		final int opersPer = (padToFullSector ? 64 : 20000) * (forceCheckEvery ? 2 : 1);
		final Random rnd = new Random();
		final HashMap<Integer, Integer> std = new HashMap<Integer, Integer>();
		
		final double insertUpTo = 0.7;
		final double deleteUpTo = 0.995;
		
		if(padToFullSector)
			ser = new TestClassSerializer(4096 - 9);
		else
			ser = new TestClassSerializer(4);
		
		map = new InlineObjectWithIDMapping<>(file.getAbsolutePath(), ser);
		map.open();
		
		List<HistoryItem> hist = new ArrayList<>();
		for(int i = 0; i < numRepeats; i++) {
			try {
				boolean lastWasScan = false;
				for(int j = 0; j < opersPer; j++) {
					if(sanityCheck)
						map.assertSane();
					double choice = j == opersPer - 1 ? 1 : rnd.nextDouble();
					if(forceCheckEvery && !lastWasScan) {
						choice = 1;
					}
					int id = rnd.nextInt(opersPer * 5);
					
					if(choice < insertUpTo) {
						lastWasScan = false;
						int val = rnd.nextInt(100);
						std.put(id, val);
						hist.add(new HistoryItem((byte)0, id, val));
						map.put(tc(id, val));
						
						TestClass fetched = map.get(id);
						if(!tc(id, val).equals(fetched)) {
							hist.add(new HistoryItem((byte)2, id, val));
							assertEquals(errorStr(hist), tc(id, val), fetched);
						}
					}else if(choice < deleteUpTo) {
						lastWasScan = false;
						Integer exp = std.remove(id);
						hist.add(new HistoryItem((byte)1, id, exp == null ? -1 : exp.intValue()));
						TestClass ret = map.remove(id);
						
						if(exp == null && ret != null) {
							assertNull(errorStr(hist), ret);
						}else if(exp != null && !tc(id, exp.intValue()).equals(ret)) {
							assertEquals(errorStr(hist), tc(id, exp.intValue()), ret);						
						}
					}else if(!lastWasScan) {
						lastWasScan = true;
						Set<Integer> available = new HashSet<>();
						for(int k = 0; k < 100; k++) {
							available.add(rnd.nextInt(opersPer * 5));
						}
						
						for(Entry<Integer, Integer> exp : std.entrySet()) {
							TestClass ret = map.get(exp.getKey());
							if(!tc(exp.getKey(), exp.getValue()).equals(ret)) {
								hist.add(new HistoryItem((byte)2, exp.getKey(), exp.getValue()));
								assertEquals(errorStr(hist), tc(exp.getKey(), exp.getValue()), ret);
							}
							
							available.remove(exp.getKey());
						}
						
						for(int av : available) {
							TestClass ret = map.get(av);
							if(ret != null) {
								hist.add(new HistoryItem((byte)2, av, -1));
								assertNull(errorStr(hist), ret);
							}
						}
					}
				}
			} catch(Exception e) {
				throw new AssertionError(errorStr(hist), e);
			}
			std.clear();
			hist.clear();
			
			map.close();
			file.delete();
			map = new InlineObjectWithIDMapping<>(file.getAbsolutePath(), ser);
			map.open();
		}
	}
	
	@After
	public void cleanUp() {
		map.close();
		map = null;
	}
}
