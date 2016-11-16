package gash.router.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * @author Sarika Nitin Kale
 *
 * 
 */
public class ChunkTracker {

	private static HashMap<Integer, ArrayList> chunkTracker = new HashMap<Integer, ArrayList>();

	protected static Logger logger = LoggerFactory.getLogger("Chunk Tracker");
	public static HashMap<Integer, ArrayList> getChunkTracker() {

		return chunkTracker;

	}

	public static void setChunkTracker(HashMap<Integer, ArrayList> chunkTracker) {

		ChunkTracker.chunkTracker = chunkTracker;

	}

	public static void set(int id, int pos) {

		ArrayList<Integer> al = chunkTracker.get(id);

		al.add(pos, 1);

		chunkTracker.put(id, al);

	}

	public static void setInitial(int id, int length) {

		ArrayList<Integer> al = new ArrayList<Integer>();

		chunkTracker.put(id, al);
		
		al=chunkTracker.get(id);
		
		al.add(0,1);

	}

	public static boolean checkMissing(int id) {

		try {
			ArrayList al = chunkTracker.get(id);

			if (al.contains(0)) {

				return true; // chunks are missing

			}

			else {

				return false; // all values are 1

			}
		} catch (Exception e) {
			
			logger.error("MISSING CHUNKS ....ROLLBACK");
			
			return false;
		}

	}

}