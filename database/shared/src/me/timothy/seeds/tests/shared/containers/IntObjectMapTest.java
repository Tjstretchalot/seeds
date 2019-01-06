package me.timothy.seeds.tests.shared.containers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.Random;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import me.timothy.seeds.shared.containers.IntObjectMap;

public class IntObjectMapTest {
	protected static class TestClass {
		public int id;

		public TestClass(int id) {
			super();
			this.id = id;
		}
		
		@Override
		public boolean equals(Object o) {
			if(!(o instanceof TestClass))
				return false;
			return id == ((TestClass)o).id;
		}
		
		@Override
		public String toString() {
			return "[TC id=" + id + "]";
		}
	}
	
	protected IntObjectMap<TestClass> map;
	protected IntObjectMap<Integer> map2;
	
	@Before
	public void setUp() {
		map = new IntObjectMap<>(0);
		map2 = new IntObjectMap<>(0);
	}
	
	@Test
	public void testPutNoConflicts() {
		assertNull(map.get(3));
		assertNull(map.get(3));
		
		assertNull(map.put(5, new TestClass(5)));
		assertEquals(5, map.get(5).id);
		assertNull(map.get(3));
		
		assertNull(map.put(3, new TestClass(3)));
		assertEquals(5, map.get(5).id);
		assertEquals(3, map.get(3).id);
		assertNull(map.get(7));
		
		assertNull(map.put(6, new TestClass(6)));
		assertEquals(3, map.get(3).id);
		assertNull(map.get(4));
		assertEquals(5, map.get(5).id);
		assertEquals(6, map.get(6).id);
		assertNull(map.get(7));
	}
	
	@Test
	public void testPutConflicts() {
		assertNull(map.put(3, new TestClass(1)));
		assertEquals(1, map.put(3, new TestClass(2)).id);
		assertEquals(2, map.put(3, new TestClass(3)).id);
		assertEquals(3, map.put(3, new TestClass(4)).id);
		assertNull(map.get(1));
		assertNull(map.get(2));
		assertEquals(4, map.get(3).id);
		assertNull(map.get(4));
	}
	
	@Test
	public void testPutMany() {
		for(int i = 0; i < 100; i++) {
			assertNull(map.put(i, new TestClass(i)));
		}
		
		for(int i = 0; i < 100; i++) {
			assertEquals(i, map.get(i).id);
		}
		for(int i = 100; i < 105; i++) {
			assertNull(map.get(i));
		}
	}
	
	@Test
	public void testRemove() {
		assertNull(map.remove(3));
		assertNull(map.put(3, new TestClass(3)));
		assertNull(map.remove(7));
		assertNull(map.put(7, new TestClass(7)));
		assertEquals(3, map.remove(3).id);
		assertNull(map.get(3));
		assertEquals(7, map.remove(7).id);
		assertNull(map.get(7));
	}
	
	@Test
	public void testRegression1() {
		assertNull(map2.put(81, 130));
		assertNull(map2.remove(30));
		assertNull(map2.remove(85));
		assertNull(map2.put(55, 774));
		assertNull(map2.remove(65));
		assertNull(map2.put(7, 81));
		assertNull(map2.put(13, 723));
		assertNull(map2.remove(4));
		assertNull(map2.remove(54));
		assertNull(map2.put(60, 535));
		assertNull(map2.remove(57));
		assertNull(map2.put(34, 886));
		assertNull(map2.put(85, 363));
		assertNull(map2.remove(37));
		assertNull(map2.put(54, 814));
		assertNull(map2.remove(99));
		assertNull(map2.put(36, 994));
		assertNull(map2.remove(50));
		assertNull(map2.put(96, 199));
		assertNull(map2.put(50, 860));
		assertEquals(Integer.valueOf(199), map2.get(96));
	}
	
	@Test
	public void testRegression2() {
		assertNull(map2.remove(64));
		assertNull(map2.remove(78));
		assertNull(map2.put(50, 492));
		assertNull(map2.remove(33));
		assertNull(map2.put(9, 383));
		assertNull(map2.remove(36));
		assertNull(map2.remove(67));
		assertNull(map2.remove(98));
		assertNull(map2.put(40, 40));
		assertNull(map2.put(14, 690));
		assertNull(map2.put(96, 551));
		assertNull(map2.remove(93));
		assertNull(map2.remove(26));
		assertNull(map2.put(88, 428));
		assertNull(map2.put(30, 767));
		assertEquals(Integer.valueOf(690), map2.remove(14));
		assertNull(map2.put(34, 65));
		assertNull(map2.remove(57));
		assertNull(map2.remove(16));
		assertNull(map2.remove(16));
		assertEquals(Integer.valueOf(767), map2.get(30));
	}
	
	@Test
	public void testRegression3() {
		assertNull(map2.put(65, 405));
		assertNull(map2.put(71, 202));
		assertNull(map2.put(67, 136));
		assertNull(map2.put(79, 478));
		assertNull(map2.put(82, 683));
		assertEquals(Integer.valueOf(683), map2.remove(82));
		assertNull(map2.put(18, 696));
		assertNull(map2.put(15, 262));
		assertEquals(Integer.valueOf(262), map2.put(15, 400));
	}
	
	@Test
	public void testRegression4() {
		assertNull(map2.put(78, 469));
		assertNull(map2.remove(81));
		assertNull(map2.remove(4));
		assertNull(map2.put(53, 368));
		assertNull(map2.remove(52));
		assertNull(map2.put(88, 786));
		assertNull(map2.remove(57));
		assertNull(map2.put(97, 523));
		assertNull(map2.put(2, 100));
		assertNull(map2.put(58, 803));
		assertNull(map2.put(9, 739));
		assertNull(map2.remove(15));
		assertNull(map2.put(90, 286));
		assertNull(map2.remove(27));
		assertNull(map2.remove(99));
		assertNull(map2.remove(17));
		assertNull(map2.put(99, 92));
		assertNull(map2.put(64, 73));
		assertNull(map2.put(86, 299));
		assertEquals(Integer.valueOf(803), map2.remove(58));
		assertEquals(Integer.valueOf(73), map2.get(64));
	}
	
	@Test
	public void testRegression5() {
		assertNull(map2.put(91, 816));
		assertNull(map2.put(88, 710));
		assertNull(map2.put(4, 99));
		assertNull(map2.remove(68));
		assertNull(map2.remove(56));
		assertNull(map2.put(76, 388));
		assertNull(map2.remove(55));
		assertNull(map2.put(87, 466));
		assertNull(map2.put(30, 741));
		assertNull(map2.remove(70));
		assertNull(map2.remove(42));
		assertNull(map2.remove(13));
		assertNull(map2.remove(8));
		assertNull(map2.put(13, 830));
		assertNull(map2.remove(7));
		assertEquals(Integer.valueOf(388), map2.remove(76));
		assertNull(map2.put(99, 345));
		assertNull(map2.put(47, 33));
		assertNull(map2.put(51, 200));
		assertNull(map2.put(66, 806));
		assertNull(map2.put(80, 139));
		assertNull(map2.put(22, 846));
		assertNull(map2.put(83, 775));
		assertNull(map2.put(42, 217));
		assertNull(map2.remove(56));
		assertEquals(Integer.valueOf(806), map2.remove(66));
		assertNull(map2.put(43, 685));
		assertNull(map2.put(7, 492));
		assertNull(map2.put(84, 395));
		assertNull(map2.remove(36));
		assertEquals(Integer.valueOf(492), map2.put(7, 801));
		assertNull(map2.put(55, 801));
		assertNull(map2.put(25, 656));
		assertNull(map2.remove(82));
		assertNull(map2.remove(86));
		assertNull(map2.put(24, 112));
		assertNull(map2.put(32, 508));
		assertNull(map2.put(67, 342));
		assertNull(map2.remove(54));
		assertEquals(Integer.valueOf(685), map2.put(43, 158));
		assertNull(map2.put(68, 548));
		assertNull(map2.put(36, 63));
		assertNull(map2.put(1, 891));
		assertEquals(Integer.valueOf(342), map2.put(67, 312));
		assertNull(map2.remove(31));
		assertNull(map2.remove(35));
		assertNull(map2.put(60, 630));
		assertNull(map2.remove(98));
		assertNull(map2.put(15, 5));
		assertEquals(Integer.valueOf(312), map2.put(67, 324));
		assertNull(map2.remove(35));
		assertNull(map2.remove(40));
		assertEquals(Integer.valueOf(508), map2.remove(32));
		assertNull(map2.remove(2));
		assertNull(map2.put(63, 559));
		assertNull(map2.put(9, 725));
		assertNull(map2.remove(45));
		assertNull(map2.remove(28));
		assertNull(map2.remove(72));
		assertEquals(Integer.valueOf(112), map2.remove(24));
		assertNull(map2.remove(11));
		assertNull(map2.remove(97));
		assertNull(map2.put(72, 157));
		assertNull(map2.remove(59));
		assertEquals(Integer.valueOf(801), map2.remove(7));
		assertEquals(Integer.valueOf(158), map2.put(43, 464));
		assertNull(map2.remove(81));
		assertNull(map2.put(92, 71));
		assertNull(map2.put(2, 112));
		assertNull(map2.remove(85));
		assertNull(map2.put(18, 985));
		assertEquals(Integer.valueOf(725), map2.put(9, 708));
		assertNull(map2.remove(35));
		assertNull(map2.remove(93));
		assertNull(map2.put(98, 498));
		assertNull(map2.remove(46));
		assertEquals(Integer.valueOf(99), map2.remove(4));
		assertNull(map2.remove(12));
		assertNull(map2.put(70, 63));
		assertNull(map2.put(74, 404));
		assertNull(map2.remove(49));
		assertEquals(Integer.valueOf(801), map2.remove(55));
		assertNull(map2.put(96, 882));
		assertEquals(Integer.valueOf(63), map2.put(36, 387));
		assertNull(map2.put(76, 997));
		assertEquals(Integer.valueOf(741), map2.remove(30));
		assertNull(map2.put(24, 869));
		assertNull(map2.remove(82));
		assertNull(map2.remove(78));
		assertNull(map2.remove(20));
		assertNull(map2.remove(20));
		assertEquals(Integer.valueOf(816), map2.put(91, 334));
		assertEquals(Integer.valueOf(846), map2.remove(22));
		assertEquals(Integer.valueOf(775), map2.remove(83));
		assertNull(map2.remove(6));
		assertNull(map2.put(50, 951));
		assertNull(map2.put(90, 178));
		assertNull(map2.put(31, 88));
		assertEquals(Integer.valueOf(548), map2.remove(68));
		assertEquals(Integer.valueOf(324), map2.put(67, 800));
		assertNull(map2.put(22, 510));
		assertNull(map2.put(30, 0));
		assertNull(map2.put(4, 550));
		assertNull(map2.put(58, 746));
		assertNull(map2.put(81, 661));
		assertNull(map2.remove(23));
		assertEquals(Integer.valueOf(498), map2.put(98, 985));
		assertNull(map2.put(94, 326));
		assertNull(map2.remove(45));
		assertNull(map2.remove(0));
		assertNull(map2.put(14, 79));
		assertNull(map2.remove(69));
		assertNull(map2.put(53, 460));
		assertNull(map2.put(78, 592));
		assertEquals(Integer.valueOf(710), map2.put(88, 710));
		assertNull(map2.remove(48));
		assertNull(map2.put(75, 97));
		assertEquals(Integer.valueOf(891), map2.remove(1));
		assertNull(map2.remove(57));
		assertEquals(Integer.valueOf(33), map2.put(47, 924));
		assertNull(map2.remove(59));
		assertEquals(Integer.valueOf(924), map2.remove(47));
		assertNull(map2.remove(0));
		assertNull(map2.remove(27));
		assertNull(map2.put(6, 556));
		assertNull(map2.remove(82));
		assertNull(map2.put(12, 970));
		assertEquals(Integer.valueOf(71), map2.put(92, 360));
		assertEquals(Integer.valueOf(460), map2.put(53, 394));
		assertNull(map2.remove(1));
		assertNull(map2.remove(10));
		assertNull(map2.put(77, 258));
		assertEquals(Integer.valueOf(5), map2.remove(15));
		assertEquals(Integer.valueOf(550), map2.remove(4));
		assertNull(map2.put(8, 526));
		assertNull(map2.remove(49));
		assertNull(map2.remove(54));
		assertEquals(Integer.valueOf(869), map2.put(24, 998));
		assertEquals(Integer.valueOf(404), map2.put(74, 396));
		assertEquals(Integer.valueOf(394), map2.put(53, 175));
		assertNull(map2.remove(59));
		assertEquals(Integer.valueOf(998), map2.remove(24));
		assertNull(map2.remove(39));
		assertEquals(Integer.valueOf(396), map2.put(74, 951));
		assertEquals(Integer.valueOf(97), map2.put(75, 204));
		assertEquals(Integer.valueOf(175), map2.remove(53));
		assertNull(map2.put(71, 892));
		assertEquals(Integer.valueOf(830), map2.remove(13));
		assertNull(map2.put(27, 336));
		assertEquals(Integer.valueOf(258), map2.remove(77));
		assertEquals(Integer.valueOf(710), map2.remove(88));
		assertNull(map2.put(7, 102));
		assertNull(map2.remove(83));
		assertNull(map2.put(79, 802));
		assertNull(map2.remove(57));
		assertNull(map2.remove(59));
		assertNull(map2.remove(95));
		assertNull(map2.remove(88));
		assertNull(map2.remove(56));
		assertEquals(Integer.valueOf(345), map2.remove(99));
		assertNull(map2.remove(40));
		assertNull(map2.remove(64));
		assertNull(map2.put(82, 200));
		assertNull(map2.put(57, 645));
		assertEquals(Integer.valueOf(466), map2.remove(87));
		assertEquals(Integer.valueOf(951), map2.remove(74));
		assertNull(map2.put(77, 871));
		assertNull(map2.put(15, 639));
		assertEquals(Integer.valueOf(985), map2.put(98, 805));
		assertEquals(Integer.valueOf(871), map2.put(77, 182));
		assertNull(map2.remove(19));
		assertNull(map2.remove(0));
		assertNull(map2.remove(13));
		assertNull(map2.remove(48));
		assertNull(map2.remove(16));
		assertNull(map2.remove(4));
		assertNull(map2.put(88, 296));
		assertNull(map2.remove(55));
		assertEquals(Integer.valueOf(200), map2.put(51, 694));
		assertNull(map2.remove(10));
		assertNull(map2.remove(33));
		assertNull(map2.put(37, 893));
		assertNull(map2.remove(17));
		assertEquals(Integer.valueOf(708), map2.remove(9));
		assertNull(map2.put(47, 235));
		assertEquals(Integer.valueOf(139), map2.put(80, 821));
		assertNull(map2.put(21, 85));
		assertEquals(Integer.valueOf(630), map2.remove(60));
		assertNull(map2.put(54, 297));
		assertEquals(Integer.valueOf(217), map2.remove(42));
		assertNull(map2.put(61, 881));
		assertEquals(Integer.valueOf(892), map2.put(71, 124));
		assertNull(map2.put(38, 589));
		assertEquals(Integer.valueOf(881), map2.put(61, 851));
		assertNull(map2.remove(1));
		assertEquals(Integer.valueOf(200), map2.put(82, 494));
		assertNull(map2.put(83, 732));
		assertEquals(Integer.valueOf(102), map2.remove(7));
	}
	
	@Test
	public void testAgainstStandard() {
		HashMap<Integer, TestClass> std = new HashMap<>();
		Random rand = new Random();
		
		for(int j = 0; j < 1000; j++) {
			List<String> history = new ArrayList<>();
			map = new IntObjectMap<>(0);
			std.clear();
			try {
				for(int i = 0; i < 200; i++) {
					int rnd = rand.nextInt(100);
					
					TestClass expected = null;
					if(!std.containsKey(rnd)) {
						TestClass res = map.get(rnd);
						if(res != null) {
							history.add("assertNull(map2.get(" + rnd + "));");
							assertNull(history.stream().collect(Collectors.joining("\n")), res);
						}
					}else {
						expected = std.get(rnd);
						TestClass res = map.get(rnd);
						if(!expected.equals(res)) {
							history.add("assertEquals(Integer.valueOf(" + expected.id + "), map2.get(" + rnd + "));");
							assertEquals(history.stream().collect(Collectors.joining("\n")), expected, res);
						}
					}
					
					if(rand.nextBoolean()) {
						int val = rand.nextInt(1000);
						TestClass tc = new TestClass(val);
						TestClass res = map.put(rnd, tc);
						if(expected == null) {
							history.add("assertNull(map2.put(" + rnd + ", " + val + "));");
						}else {
							history.add("assertEquals(Integer.valueOf(" + expected.id + "), map2.put(" + rnd + ", " + val + "));");
						}
						if((expected == null && res != null) || (expected != null && !expected.equals(res)))
							assertEquals(history.stream().collect(Collectors.joining("\n")), expected, res);
						std.put(rnd, tc);
					}else {
						if(expected == null) {
							history.add("assertNull(map2.remove(" + rnd + "));");
						}else {
							history.add("assertEquals(Integer.valueOf(" + expected.id + "), map2.remove(" + rnd + "));");
						}
						
						TestClass res = map.remove(rnd);
						if((expected == null && res != null) || (expected != null && !expected.equals(res)))
							assertEquals(history.stream().collect(Collectors.joining("\n")), expected, res);
						std.remove(rnd);
					}
				}
				
				Set<Integer> missing = new HashSet<>();
				for(int i = 0; i < 100; i++) {
					missing.add(i);
				}
				for(Entry<Integer, TestClass> e : std.entrySet()) {
					TestClass mapVal = map.get(e.getKey());
					if(!e.getValue().equals(mapVal)) {
						history.add("assertEquals(Integer.valueOf(" + e.getValue().id + "), map2.get(" + e.getKey() + "));");
						assertEquals(history.stream().collect(Collectors.joining("\n")), e.getValue(), map.get(e.getKey()));
					}
					missing.remove(e.getKey());
				}
				for(int i : missing) {
					TestClass mapVal = map.get(i);
					if(mapVal != null) {
						history.add("assertNull(map2.get(" + i + "));");
						assertNull(history.stream().collect(Collectors.joining("\n")), map.get(i));
					}
				}
			}catch(Exception e) {
				history.add(e.getMessage());
				StringWriter errors = new StringWriter();
				e.printStackTrace(new PrintWriter(errors));
				history.add(errors.toString());
				throw new AssertionError(history.stream().collect(Collectors.joining("\n")));
			}
		}
	}
	
	@After
	public void cleanUp() {
		map = null;
		map2 = null;
	}
}
