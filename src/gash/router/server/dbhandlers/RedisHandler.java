package gash.router.server.dbhandlers;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * @author Sarika Nitin Kale
 *
 * 
 */
public class RedisHandler implements DbHandler {
	Jedis jedis;
	static JedisPool jedisPool;
	protected static Logger logger = LoggerFactory.getLogger("Redis Handler");
	

	public RedisHandler() {
		
		this.jedis = new Jedis("localhost");

	}

	@Override
	public long encryptKey(String keyValue) {

		Checksum checksum = new CRC32();

		// update the current checksum with the specified array of bytes

		checksum.update(keyValue.getBytes(), 0, keyValue.length());

		// get the current checksum value

		return checksum.getValue();

	}

	public Jedis jedisPool() {
		try {
			if (jedisPool == null) {

				JedisPoolConfig config = new JedisPoolConfig();
				
				jedisPool = new redis.clients.jedis.JedisPool("localhost");

			}
		} catch (Exception e) {
			
			e.printStackTrace();
		}
		return jedisPool.getResource();

	}

	@Override
	public boolean insertData(String key, int sequenceId, String value) {

		try {
			jedis.hset(Long.toString(encryptKey(key)), Integer.toString(sequenceId), value);
			return true;			
		} catch (Exception e) {
			logger.error("Cannot Insert");
			return false;
		}
	}

	@Override
	public Map<String, String> retrieveData(String key) {

		Map<String, String> data = new HashMap<String, String>();
		
		try {
			if (jedis.exists(Long.toString(encryptKey(key)))) {

				long encryptedKey = encryptKey(key);

				Set<String> sequenceIds = jedis.hkeys(Long.toString(encryptedKey));
				
				for (String b : sequenceIds) {

					data.put(b, jedis.hget(Long.toString(encryptedKey), b));
					

				}

			}
		} catch (Exception e) {
			
			e.printStackTrace();
		}

		return data;

	}

	@Override
	public Map<String, String> removeData(String key) {
		Map<String, String> data = new HashMap<String, String>();

		if (jedis.exists(Long.toString(encryptKey(key)))) {

			long encryptedKey = encryptKey(key);

			Set<String> sequenceIds = jedis.hkeys(Long.toString(encryptedKey));

			for (String b : sequenceIds) {

				data.put(b, jedis.hget(Long.toString(encryptedKey), b));
				jedis.hdel(Long.toString(encryptedKey), b);
				System.out.println(data.size());
			}	

		}

		return data;
	}

	@Override
	public void closeConnection(){
		
		jedis.close();
	}
}
