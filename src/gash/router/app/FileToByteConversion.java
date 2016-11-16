package gash.router.app;

import java.io.File;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
/**
 * @author Sarika Nitin Kale
 *
 * 
 */
public class FileToByteConversion {

	public byte[] fileToByteconvert(String filename) {

		File file = new File(filename);
		byte imageData[] = null;
		try {
			// Reading a a file from file system and convert it into bytes
			FileInputStream imageInFile = new FileInputStream(file);
			long leng=file.length();
			System.out.println(leng);
			if(leng < 2147483647){
				System.out.println(leng);
			imageData=new byte[ (int) file.length()];
			imageInFile.read(imageData);
			imageInFile.close();
			return imageData;
			}else
				return imageData;		
			
		} catch (FileNotFoundException e) {
			System.out.println("Image not found" + e);
		} catch (IOException ioe) {
			System.out.println("Exception while reading the Image " + ioe);
		}
		return null;
	}

	
}
