package gash.router.server.dbhandlers;

import java.util.Map;
/**
 * @author Sarika Nitin Kale
 *
 * 
 */
public interface DbHandler {

	long encryptKey(String key);

	boolean insertData(String key, int sequenceId, String value);
	
	public Map< String, String> retrieveData(String key);
	
	public Map<String, String> removeData(String key);
	
	public void closeConnection();

}
