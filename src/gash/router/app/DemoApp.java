/**
 * Copyright 2016 Gash.
 *
 * This file and intellectual content is protected under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package gash.router.app;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gash.router.client.CommConnection;
import gash.router.client.CommListener;
import gash.router.client.DNSConnectionInit;
import gash.router.client.IPList;
import gash.router.client.MessageClient;

import routing.Pipe.CommandMessage;

public class DemoApp implements CommListener {
	private MessageClient mc;
	protected static Logger logger = LoggerFactory.getLogger("connect");

	protected static List<CommandMessage> listOfMessages = new ArrayList<CommandMessage>();
	private StringBuilder builder = new StringBuilder();
	private FileOutputStream fos;

	public DemoApp(MessageClient mc) {
		init(mc);
	}

	private void init(MessageClient mc) {
		this.mc = mc;
		this.mc.addListener(this);
	}

	private void ping(int N) {
		// test round-trip overhead (note overhead for initial connection)
		final int maxN = 10;
		long[] dt = new long[N];
		long st = System.currentTimeMillis(), ft = 0;
		for (int n = 0; n < N; n++) {
			mc.ping();
			ft = System.currentTimeMillis();
			dt[n] = ft - st;
			st = ft;
		}

		System.out.println("Round-trip ping times (msec)");
		for (int n = 0; n < N; n++)
			System.out.print(dt[n] + " ");
		System.out.println("");
	}

	/**
	 * sample application (client) use of our messaging service
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		boolean temp = true;

		String host = "127.0.0.1";
		int port = 4668;

		// while (temp)
		{
			try {
				// RequestClientFacingServer.initConnection();

				// temp = false;
				DNSConnectionInit dnsConnectionInit = new DNSConnectionInit();
				MessageClient mc;
			
					Thread.sleep(1000);
					if(MessageClient.leaderIp!=null){
						System.out.println("will connect to leader.."+MessageClient.leaderIp);
						mc = new MessageClient(MessageClient.leaderIp, 3000);
					}
					else{
					    mc = new MessageClient("127.0.0.1", 3000);
					}

				DemoApp da = new DemoApp(mc);

				// do stuff w/ the connection
				// da.ping(2);
				if (args[0].equalsIgnoreCase("upload")) {
					// upload the file
					da.upload(args[1]);
				} else {
					// reading a file
					if (args[0].equalsIgnoreCase("read")) {
						da.read(args[1]);
					} else {
						logger.error("Please input a valid request");
					}
				}

				System.out.println("\n** exiting in 10 seconds. **");
				System.out.flush();
				Thread.sleep(10 * 1000);
			} catch (Exception e) {
				// e.printStackTrace();
			} finally {

			}

		}
	}

	private void read(String filename) {
		mc.read(filename);
	}

	private void upload(String fileName) {
		// TODO Auto-generated method stub

		mc.upload(fileName);

	}

	@Override
	public String getListenerId() {
		return "client";
	}

	@Override
	public void onMessage(CommandMessage msg) {
		System.out.println("----->" + msg);

	}

	@Override
	public void addMessage(CommandMessage msg) {
		listOfMessages.add(msg);
	}

	@Override
	public void mergeMessage(CommandMessage msg) {
		// TODO Auto-generated method stub

		try {
			if (msg.hasChunk()) {
				if (!listOfMessages.isEmpty()) {

					Collections.sort(listOfMessages, new Comparator<CommandMessage>() {
						@Override
						public int compare(CommandMessage msg1, CommandMessage msg2) {
							return msg1.getChunk().getChunkId() - msg2.getChunk().getChunkId();
						}
					});

					// custom output
					fos = new FileOutputStream("F:/images/ashwa.jpg");

					for (int i = 1; i <= listOfMessages.size(); i++) {

						if (msg.hasMessageId()) {
							if (msg.getMessageId() == 12345 && i == msg.getChunk().getChunkId()) {

								builder.append(msg.getReadResponse().getReadData());
							}

						} else
							logger.info("Invalid message chunk" + msg);
						return;
					}

					byte fileByte[] = builder.toString().getBytes();
					fos.write(fileByte);
					fos.close();

				}

			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
