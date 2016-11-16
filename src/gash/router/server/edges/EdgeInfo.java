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
package gash.router.server.edges;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

import gash.router.server.WorkInit;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

public class EdgeInfo {
	private int ref;
	private String host;
	private int port;
	private long lastHeartbeat = -1;
	private boolean active = false;
	private Channel channel;
	protected static Logger logger = LoggerFactory.getLogger("edge info");
	EdgeInfo(int ref, String host, int port) {
		this.ref = ref;
		this.host = host;
		this.port = port;
		
	/*	this.channel=newChannel();
		this.active=true;*/
		
		
	}

	public int getRef() {
		return ref;
	}

	public void setRef(int ref) {
		this.ref = ref;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public long getLastHeartbeat() {
		return lastHeartbeat;
	}

	public void setLastHeartbeat(long lastHeartbeat) {
		this.lastHeartbeat = lastHeartbeat;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public Channel getChannel() {
		return channel;
	}

	public void setChannel(Channel channel) {
		this.channel = channel;
	}
	public boolean newChannel(){
		ChannelFuture channel;
		Boolean temp=false;
		EventLoopGroup group = new NioEventLoopGroup();
		try {
			WorkInit si = new WorkInit(null, false);
			Bootstrap b = new Bootstrap();
			b.group(group).channel(NioSocketChannel.class).handler(si);
			b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 500);
			b.option(ChannelOption.TCP_NODELAY, true);
			b.option(ChannelOption.SO_KEEPALIVE, true);

			// Make the connection attempt.
			channel = b.connect(getHost(), getPort()).syncUninterruptibly();
			Channel chan=channel.channel();
			System.out.println("LOGGERRRRR"+chan);
			setChannel(chan);
			temp=true;
			setActive(chan.isActive());
			// want to monitor the connection to the server s.t. if we loose the
			// connection, we can try to re-establish it.
		//	ClientClosedListener ccl = new ClientClosedListener(this);
		//	channel.channel().closeFuture().addListener(ccl);

			System.out.println(channel.channel().localAddress() + " -> open: " + channel.channel().isOpen()
					+ ", write: " + channel.channel().isWritable() + ", reg: " + channel.channel().isRegistered());
			return temp;

		} catch (Throwable ex) {
			logger.error("failed to initialize the client connection"+ ex);
			ex.printStackTrace();
		}
		return temp;
	}
}
