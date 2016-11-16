package gash.router.client;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;

import java.nio.channels.FileChannel;
import java.util.ArrayList;


public class Filechunk {


	public ArrayList<byte[]> fileChunking(String fileName) throws IOException {

		final long CHUNKSIZE = 1048576 ; // 1 Megabytes file chunks
		final int BUFFERSZE =  2 * 1048576; // 2 Megabyte memory buffer for
											// reading source file

		long splitSize = CHUNKSIZE;
		int bufferSize = BUFFERSZE;
		String source = fileName;
		String directory="chunks";
		String output = System.getProperty("user.dir").replace("\\", "/");
		ArrayList<byte[]> byteArray;
		FileChannel sourceChannel = null;
		try {
			sourceChannel = new FileInputStream(source).getChannel();

			ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);

			long totalBytesRead = 0; // total bytes read from channel
			long totalBytesWritten = 0; // total bytes written to output

			double numberOfChunks = Math.ceil(sourceChannel.size() / (double) splitSize);
			int padSize = (int) Math.floor(Math.log10(numberOfChunks) + 1);
			String outputFileFormat = "%s.%0" + padSize + "d";

			// setting buffers length
			byteArray=new ArrayList<>((int)numberOfChunks);
			
			FileChannel outputChannel = null; // output channel (split file) we
												// are currently writing
			long outputChunkNumber = 0; // the split file / chunk number
			long outputChunkBytesWritten = 0; // number of bytes written to
												// chunk so far

			try {

				for (int bytesRead = sourceChannel.read(buffer); bytesRead != -1; 
						bytesRead = sourceChannel.read(buffer)) {
					totalBytesRead += bytesRead;
					buffer.flip(); // convert the buffer from writing data to
									// buffer from disk to reading mode

					int bytesWrittenFromBuffer = 0; // number of bytes written
													// from buffer

					while (buffer.hasRemaining()) {
						if (outputChannel == null) {
							outputChunkNumber++;
							outputChunkBytesWritten = 0;
							// put enqueueing here
							String outputName = String.format(outputFileFormat, output, outputChunkNumber);
							System.out.println(String.format("Creating new output channel %s", outputName));
							outputChannel = new FileOutputStream(outputName).getChannel();
						}

						long chunkBytesFree = (splitSize - outputChunkBytesWritten); // maxmimum
																						// free
																						// space
																						// in
																						// chunk
						int bytesToWrite = (int) Math.min(buffer.remaining(), chunkBytesFree); // maximum
																								// bytes
																								// that
																								// should
																								// be
																								// read
																								// from
																								// current
																								// byte
																								// buffer

						buffer.limit(bytesWrittenFromBuffer + bytesToWrite); // set
																				// limit
																				// in
																				// buffer
																				// up
																				// to
																				// where
																				// bytes
																				// can
																				// be
																				// read

						int bytesWritten = outputChannel.write(buffer);
						System.out.println("ByteWritten"+bytesWritten);
						buffer.flip();
						byte b[]=new byte[buffer.remaining()];
						buffer.get(b);
						//Adding bytes from buffer into byteArray
						byteArray.add(b);
						outputChunkBytesWritten += bytesWritten;
						bytesWrittenFromBuffer += bytesWritten;
						totalBytesWritten += bytesWritten;	
						buffer.limit(bytesRead); // reset limit

						if (totalBytesWritten == sourceChannel.size()) {
							System.out.println("Finished writing last chunk");
							closeChannel(outputChannel);
							outputChannel = null;

							break;

						} else if (outputChunkBytesWritten == splitSize) {
							System.out.println("Chunk at capacity; closing()");
							closeChannel(outputChannel);
							outputChannel = null;
						}
					}

					buffer.clear();
				
					
				}

				return byteArray;	
			} finally {
				
				closeChannel(outputChannel);
			}
		} finally {
			closeChannel(sourceChannel);
		}
		

	}

	private static void closeChannel(FileChannel channel) {
		if (channel != null) {
			try {
				channel.close();
			} catch (Exception ignore) {

			}
		}
	}
}
