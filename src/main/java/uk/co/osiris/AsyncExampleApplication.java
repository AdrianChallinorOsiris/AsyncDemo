package uk.co.osiris;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import lombok.extern.slf4j.Slf4j;

/**
 * This application is to demonstrate how to call asynchronously for a list of
 * items.   
 * 
 * This code is to demonstrate a principal, not to be used in production. Its
 * not optimal, and I'm not trying to prove any particular point. Its just to
 * show how to call something that will go asynchononous.
 * 
 * @author The Old Mancunian
 *
 */
@SpringBootApplication
@Slf4j
public class AsyncExampleApplication implements CommandLineRunner {

	String pathName = "../..";

	public static void main(String[] args) {
		SpringApplication.run(AsyncExampleApplication.class, args).close();
	}

	@Override
	public void run(String... args) throws Exception {
		sizeByStream(pathName);
		sizeByAsyncStream(pathName);
	}

	/**
	 * Generate the total directory size, but using async methods. This is more
	 * complex to set up, because we have to:
	 * 
	 * 1) List the top level directory 2) For each item, 2.1) if its a file, get its
	 * file size and add that to a running total 2.2) If its a directory, get the
	 * size of that directory a synchonously 3) Wait for all the async calls to
	 * complete 4) Add the resulting value to the computed call
	 * 
	 */
	private void sizeByAsyncStream(String pathName) {

		// This is a list to hold the future results
		ArrayList<CompletableFuture<Long>> futures = new ArrayList<>();

		// Process the files in the directory first, using a stream
		log.info("Starting Asynch processing walk... ");

		// Note we could do this whole thing recursively...
		// Find a file - Add its size
		// Find a directory and start a new thread to handle the directory.
		// That is an exercise left to the reader as home work!

		// This gets the top level directory size, asynchronously
		futures.add(CompletableFuture.supplyAsync(() -> dirOnlySize(pathName)));

		// Now get a list of the directories we have to process. This is the processing
		// of
		// a list of items aynschronously.
		Set<File> dirList = Stream.of(new File(pathName).listFiles()).filter(file -> file.isDirectory())
				.collect(Collectors.toSet());

		// For each directory, fire off an asynch thread
		for (File d : dirList) {
			futures.add(CompletableFuture.supplyAsync(() -> treesize(d.getAbsolutePath())));
		}

		// Convert the collection to an array, because CompletableFuture doesn't work
		// with collections
		// Then wait for all the futures to complete. I do this in one step because
		// otherwise the
		// Eclipse compiler starts to complain about missing type declarations. This is
		// a "spell"
		// that allows all of the threads to complete.
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));

		// By now all threads have ended so we can process the results. The only issue here
		// is that the code doesn't know that we already know that the threads have
		// completed, so it insists we trap the exceptions. I'm doing this in a conventional,
		// non-stream loop just to make the code more understandable. It can all be done 
		// in one line, with a stream and a map-reduce. 
		
		Long size = 0L;
		try {
			for (CompletableFuture<Long> f : futures) {
				size += f.get();
			}
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}

		log.info("Async calculated filesize = {} ", size);
	}

	/**
	 * This calculates the total directory size, including subsirectories using a
	 * tree walker, with a simple stream.
	 * 
	 * @param pathname
	 */

	public void sizeByStream(String pathname) {
		log.info("Starting tree walk... ");
		Long size = treesize(pathname);
		log.info("Stream calculated filesize = {} ", size);
	}

	// Setup async execution by telling Java we do expect to use Streams. 
	// You need to set this up for your CPU and thread capability. 
	@Bean
	public Executor taskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(12);
		executor.setMaxPoolSize(24);
		executor.setQueueCapacity(500);
		executor.setThreadNamePrefix("async-");
		executor.initialize();
		return executor;
	}

	/** 
	 * Given the name of a path, calculate the size of all the files
	 * and subdirectories, recursively, in the 
	 * directory tree. 
	 * 
	 * @param pathName
	 * @return
	 */
	public Long treesize(String pathName) {
		try {
			Path folder = Paths.get(pathName);
			Long size = Files.walk(folder).filter(p -> p.toFile().isFile()).mapToLong(p -> p.toFile().length()).sum();
			return size;
		} catch (IOException e) {
			log.error("Oops, we got an error there: {} ", e.getMessage());
			return null;
		}
	}
	
	
	/** 
	 * Give the name of a path, calculate the size of all the files. 
	 * @param pathName
	 * @return
	 */
	public long dirOnlySize(String pathName) {
		Long size = Stream.of(new File(pathName).listFiles()).filter(file -> !file.isDirectory())
				.mapToLong(p -> p.length()).sum();
		return size;
	}
}
