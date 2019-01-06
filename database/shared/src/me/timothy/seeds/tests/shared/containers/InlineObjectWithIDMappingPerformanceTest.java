package me.timothy.seeds.tests.shared.containers;

import java.io.File;
import java.text.DecimalFormat;

import me.timothy.seeds.shared.db.InlineObjectWithIDMapping;
import me.timothy.seeds.tests.shared.containers.InlineObjectWithIDMappingTests.*;

public class InlineObjectWithIDMappingPerformanceTest {
	public static void main(String[] args) {
		runInsertionTest();
	}
	
	public static void runInsertionTest() {
		final int timeSecondsPerTrial = 2;
		final int warmups = 7;
		final int trials = 10;
		final int maxSize = 1 << 17;
		int itersBetweenTimeCheck = 1;
		
		File f = new File("perf_test_inline_object_with_id.dat");
		if(f.exists())
			f.delete();
		
		InlineObjectWithIDMapping<TestClass> map = new InlineObjectWithIDMapping<>("perf_test_inline_object_with_id.dat", new TestClassSerializer(4096-9));
		map.open();
		
		DecimalFormat df = new DecimalFormat("#.000000000");
		
		for(int i = 0; i < warmups; i++) {
			long start = System.currentTimeMillis();
			long end = start + timeSecondsPerTrial * 1000;
			int counter = 0;
			long time;
			while((time = System.currentTimeMillis()) < end && counter < maxSize) {
				for(int j = 0; j < itersBetweenTimeCheck && counter < maxSize; j++) {
					try {
						map.put(new TestClass(map.suggestID(), ~counter));
					}catch(IllegalArgumentException e) {
						System.out.println("counter=" + counter);
						throw e;
					}
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
			while((time = System.currentTimeMillis()) < end && counter < maxSize) {
				for(int j = 0; j < itersBetweenTimeCheck && counter < maxSize; j++) {
					map.put(new TestClass(map.suggestID(), ~counter));
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
		
		map.close();
	}
}
