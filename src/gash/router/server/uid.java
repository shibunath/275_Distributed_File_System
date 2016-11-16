package gash.router.server;

import java.util.UUID;

public class uid {
	 public static   void main(String args[]){
		    //generate random UUIDs
		    UUID id = UUID.randomUUID();   
		    System.out.println("UUID One: " + id);
		  //  Integer.valueOf(id);
		    id.getMostSignificantBits();
		    System.out.println(id.getMostSignificantBits());
		    System.out.println( (int) CommonUtils.UUIDGen());
		    
		    
		
		  }

}
