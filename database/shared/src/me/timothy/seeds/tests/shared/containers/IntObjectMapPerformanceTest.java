package me.timothy.seeds.tests.shared.containers;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Random;

import me.timothy.seeds.shared.containers.IntObjectMap;

public class IntObjectMapPerformanceTest {
	public static void main(String[] args) {
		//runRandomTest();
		//runRandomInsertionTest();
		//runIterativeInsertionTest();
		runIterativeInsertionTest2();
	}
	
	@SuppressWarnings("unused")
	private static void runRandomTest() {
		final int iters = 100000;
		final int warmups = 10;
		Random rand = new Random();
		
		IntObjectMap<Integer> map1 = new IntObjectMap<>(0);
		
		System.out.println("Warming up random test against my map...");
		for(int i = 0; i < warmups; i++) {
			int[] randomTest = new int[iters * 2];
			for(int j = 0; j < iters; j++) {
				randomTest[j * 2] = rand.nextBoolean() ? 1 : 0;
				randomTest[j * 2 + 1] = rand.nextInt(100);
			}

			long start = System.currentTimeMillis();
			for(int j = 0; j < iters; j++) {
				if(randomTest[j << 1] == 0) {
					map1.put(randomTest[(j << 1) + 1], j);
				}else {
					map1.remove(randomTest[(j << 1) + 1]);
				}
			}
			long time = System.currentTimeMillis() - start;
			System.out.println("  Warmup " + (i+1) + "/" + warmups + " - " + time + "ms (" + (time / (double)iters) + " ms / oper)");
			map1 = new IntObjectMap<>(0);
		}
		
		System.out.println("Running... ");
		int[] testRandomTest = new int[iters * 2];
		for(int j = 0; j < iters; j++) {
			testRandomTest[(j << 1)] = rand.nextBoolean() ? 1 : 0;
			testRandomTest[(j << 1) + 1] = rand.nextInt(100);
		}

		long start = System.currentTimeMillis();
		for(int j = 0; j < iters; j++) {
			if(testRandomTest[j << 1] == 0) {
				map1.put(testRandomTest[(j << 1) + 1], j);
			}else {
				map1.remove(testRandomTest[(j << 1) + 1]);
			}
		}
		long timeForMine = System.currentTimeMillis() - start;
		System.out.println("  Actual: " + timeForMine + "ms");
		map1 = null;

		System.out.println();
		System.out.println();
		
		System.out.println("Warming up random insertion test against generic map...");
		
		HashMap<Integer, Integer> map2 = new HashMap<>();
		for(int i = 0; i < warmups; i++) {
			int[] randomTest = new int[iters * 2];
			for(int j = 0; j < iters; j++) {
				randomTest[j * 2] = rand.nextBoolean() ? 1 : 0;
				randomTest[j * 2 + 1] = rand.nextInt(100);
			}

			start = System.currentTimeMillis();
			for(int j = 0; j < iters; j++) {
				if(randomTest[j << 1] == 0) {
					map2.put(randomTest[(j << 1) + 1], j);
				}else {
					map2.remove(randomTest[(j << 1) + 1]);
				}
			}
			long time = System.currentTimeMillis() - start;
			System.out.println("  Warmup " + (i+1) + "/" + warmups + " - " + time + "ms");
			map2 = new HashMap<>();
		}

		System.out.println("Running...");
		
		start = System.currentTimeMillis();
		for(int j = 0; j < iters; j++) {
			if(testRandomTest[j << 1] == 0) {
				map2.put(testRandomTest[(j << 1) + 1], j);
			}else {
				map2.remove(testRandomTest[(j << 1) + 1]);
			}
		}
		long timeForGen = System.currentTimeMillis() - start;
		System.out.println("  Actual: " + timeForGen + "ms");
		System.out.println("  Final size: " + map2.size());
		map2 = null;
		
		if(timeForMine < timeForGen) {
			double perc = ((timeForGen - timeForMine) / (double)timeForMine) * 100;
			System.out.println("Mines Better! Improvement: " + perc + "%");
		}else {
			double perc = ((timeForMine - timeForGen) / (double)timeForGen) * 100;
			System.out.println("Generics Better! Improvement: " + perc + "%");
		}
	}

	@SuppressWarnings("unused")
	private static void runRandomInsertionTest() {
		final int iters = 100000;
		final int warmups = 10;
		Random rand = new Random();
		
		IntObjectMap<Integer> map1 = new IntObjectMap<>(0);
		
		System.out.println("Warming up random insertion test against my map...");
		for(int i = 0; i < warmups; i++) {
			int[] randomPoints = new int[iters];
			for(int j = 0; j < randomPoints.length; j++) {
				randomPoints[j] = rand.nextInt();
			}
			long start = System.currentTimeMillis();
			for(int j = 0; j < iters; j++) {
				map1.put(randomPoints[j], j);
			}
			long time = System.currentTimeMillis() - start;
			System.out.println("  Warmup " + (i+1) + "/" + warmups + " - " + time + "ms");
			map1 = new IntObjectMap<>(0);
		}
		
		System.out.println("Running... ");
		int[] testRandomPoints = new int[iters];
		for(int j = 0; j < testRandomPoints.length; j++) {
			testRandomPoints[j] = rand.nextInt();
		}
		long start = System.currentTimeMillis();
		for(int j = 0; j < iters; j++) {
			map1.put(testRandomPoints[j], j);
		}
		long timeForMine = System.currentTimeMillis() - start;
		System.out.println("  Actual: " + timeForMine + "ms");
		map1 = null;
		
		System.out.println();
		System.out.println();
		
		System.out.println("Warming up random insertion test against generic map...");
		
		HashMap<Integer, Integer> map2 = new HashMap<>();
		for(int i = 0; i < warmups; i++) {
			int[] randomPoints = new int[iters];
			for(int j = 0; j < randomPoints.length; j++) {
				randomPoints[j] = rand.nextInt();
			}
			start = System.currentTimeMillis();
			for(int j = 0; j < iters; j++) {
				map2.put(randomPoints[j], j);
			}
			long time = System.currentTimeMillis() - start;
			System.out.println("  Warmup " + (i+1) + "/" + warmups + " - " + time + "ms");
			map2 = new HashMap<>();
		}

		System.out.println("Running...");
		
		start = System.currentTimeMillis();
		for(int j = 0; j < iters; j++) {
			map2.put(testRandomPoints[j], j);
		}
		long timeForGen = System.currentTimeMillis() - start;
		System.out.println("  Actual: " + timeForGen + "ms");
		map2 = null;
		
		if(timeForMine < timeForGen) {
			double perc = ((timeForGen - timeForMine) / (double)timeForMine) * 100;
			System.out.println("Mines Better! Improvement: " + perc + "%");
		}else {
			double perc = ((timeForMine - timeForGen) / (double)timeForGen) * 100;
			System.out.println("Generics Better! Improvement: " + perc + "%");
		}
	}
	
	@SuppressWarnings("unused")
	private static void runIterativeInsertionTest1() {
		final long iters = 100000;
		final int warmups = 10;
		IntObjectMap<Integer> map1 = new IntObjectMap<>(0);
		
		System.out.println("Warming up iterative insertion test against my map...");
		for(int i = 0; i < warmups; i++) {
			long start = System.currentTimeMillis();
			for(int j = 0; j < iters; j++) {
				map1.put(j, j);
			}
			long time = System.currentTimeMillis() - start;
			System.out.println("  Warmup " + (i+1) + "/" + warmups + " - " + time + "ms");
			map1 = new IntObjectMap<>(0);
		}
		
		System.out.println("Running...");

		long start = System.currentTimeMillis();
		for(int j = 0; j < iters; j++) {
			map1.put(j, j);
		}
		long timeForMine = System.currentTimeMillis() - start;
		System.out.println("  Actual: " + timeForMine + "ms");
		map1 = null;
		
		System.out.println();
		System.out.println();
		
		System.out.println("Warming up iterative insertion test against generic map...");
		
		HashMap<Integer, Integer> map2 = new HashMap<>();
		for(int i = 0; i < warmups; i++) {
			start = System.currentTimeMillis();
			for(int j = 0; j < iters; j++) {
				map2.put(j, j);
			}
			long time = System.currentTimeMillis() - start;
			System.out.println("  Warmup " + (i+1) + "/" + warmups +" - " + time + "ms");
			map2 = new HashMap<>(0);
		}
		
		System.out.println("Running...");
		
		start = System.currentTimeMillis();
		for(int j = 0; j < iters; j++) {
			map2.put(j, j);
		}
		long timeForGen = System.currentTimeMillis() - start;
		System.out.println("  Actual: " + timeForGen + "ms");
		map2 = null;
		
		if(timeForMine < timeForGen) {
			double perc = ((timeForGen - timeForMine) / (double)timeForMine) * 100;
			System.out.println("Mines Better! Improvement: " + perc + "%");
		}else {
			double perc = ((timeForMine - timeForGen) / (double)timeForGen) * 100;
			System.out.println("Generics Better! Improvement: " + perc + "%");
		}
	}
	
	private static void runIterativeInsertionTest2() {
		final int timeSecondsPerTrial = 2;
		final int warmups = 7;
		final int trials = 10;
		int itersBetweenTimeCheck = 1;

		IntObjectMap<Integer> map = new IntObjectMap<>(0);
		//HashMap<Integer, Integer> map = new HashMap<>();
		
		DecimalFormat df = new DecimalFormat("#.000000000");
		
		for(int i = 0; i < warmups; i++) {
			long start = System.currentTimeMillis();
			long end = start + timeSecondsPerTrial * 1000;
			int counter = 0;
			long time;
			while((time = System.currentTimeMillis()) < end) {
				for(int j = 0; j < itersBetweenTimeCheck; j++) {
					map.put(counter, ~counter);
					counter++;
				}
			}
			
			long duration = time - start;
			double msPerInsert = duration / ((double)counter);
			System.out.println("Warmup " + (i+1) + ": " + df.format(msPerInsert) + "ms / insert (took " + duration + "ms to do " + counter + " inserts)");
			map.clear();
			
			double errorOnIters = Math.abs(timeSecondsPerTrial * 1000 - duration) / (double)(timeSecondsPerTrial * 2000);
			if(errorOnIters > 0.1 || counter > 7 * itersBetweenTimeCheck) {
				// scale to try to get 5 loops
				itersBetweenTimeCheck = (int)Math.round(((timeSecondsPerTrial * 1000) / msPerInsert) * 0.2);
				System.out.println("Updated iters / check to " + itersBetweenTimeCheck);
			}
		}
		

		double[] trialsMSPerInsert = new double[trials];
		double sumMSPerInsert = 0;
		for(int i = 0; i < trials; i++) {
			long start = System.currentTimeMillis();
			long end = start + timeSecondsPerTrial * 1000;
			int counter = 0;
			long time;
			while((time = System.currentTimeMillis()) < end) {
				for(int j = 0; j < itersBetweenTimeCheck; j++) {
					map.put(counter, ~counter);
					counter++;
				}
			}
			
			double duration = time - start;
			double msPerInsert = duration / ((double)counter);
			System.out.println("Trial " + (i+1) + ": " + df.format(msPerInsert) + "ms / insert");
			sumMSPerInsert += msPerInsert;
			trialsMSPerInsert[i] = msPerInsert;
			map.clear();
		}
		
		double avgMSPerInsert = sumMSPerInsert / trials;
		
		double squaredDevSum = 0;
		for(int i = 0; i < trials; i++) {
			squaredDevSum += Math.pow(trialsMSPerInsert[i] - avgMSPerInsert, 2);
		}
		
		double stdDev = Math.sqrt(squaredDevSum / (trials - 1));
		double stdError = stdDev / Math.sqrt(trials);
		
		System.out.println("MS / Insert: " + df.format(avgMSPerInsert) + " ± " + df.format(stdError));
	}
}
