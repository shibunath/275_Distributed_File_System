package gash.router.server.dbhandlers;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
/**
 * @author Sarika Nitin Kale
 *
 * 
 */
public class MongoHandler implements DbHandler
{

	private DB db;
	private DBCollection col;
	private MongoClient mongoClient;
	protected static Logger logger = LoggerFactory.getLogger("Mongo Handler");
	
	
	public MongoHandler() throws UnknownHostException {
		mongoClient = new MongoClient("localhost", 27017);
		db = mongoClient.getDB("imagedb");
		 this.col = db.getCollection("images");
	}


	@Override
	public long encryptKey(String key) {
		// TODO Auto-generated method stub
		Checksum checksum = new CRC32();
		// update the current checksum with the specified array of bytes
		checksum.update(key.getBytes(), 0, key.length());
		// get the current checksum value
		return checksum.getValue();	
	}

	@Override
	public boolean insertData(String key, int sequenceId, String value) {
		try {
			DBObject doc=new BasicDBObject();	
			doc.put("Filename", encryptKey(key));
			doc.put("ChunkNumber", sequenceId);
			doc.put("FileData", value);
			col.insert(doc);
			return true;
		} catch (Exception e) {
			logger.error("Cannot insert data into Mongo");
			return false;
		}
		
	}

	@Override
	public Map<String, String> retrieveData(String key) {
		// TODO Auto-generated method stub
		
		Map<String, String> data = new HashMap<String, String>();
		long encryptKey=encryptKey(key);
		DBObject doc=new BasicDBObject();
		String seq;
		String val;
		doc.put("Filename", encryptKey(key));
		
		DBCursor cursor=col.find(doc);
		
		while(cursor.hasNext())
		{
			DBObject t=cursor.next();
			seq=t.get("ChunkNumber").toString();
			val=t.get("FileData").toString();
			data.put(seq, val);	
		}
		
		return data;
	}

	@Override
	public Map<String, String> removeData(String key) {
		// TODO Auto-generated method stub
		long encryptKey=encryptKey(key);
		
		Map<String, String> data = new HashMap<String, String>();
		DBObject doc=new BasicDBObject();
		doc.put("Filename", encryptKey(key));
		String seq;
		String val;
		
		DBCursor cursor=col.find(doc);
		
		while(cursor.hasNext())
		{
			DBObject t=cursor.next();
			seq=t.get("ChunkNumber").toString();
			val=t.get("FileData").toString();
			data.put(encryptKey(val));	
		}
		
		col.remove(doc);
		
		return data;
	}

	
	@Override
	public void closeConnection() {
		mongoClient.close();
		
	}
}
