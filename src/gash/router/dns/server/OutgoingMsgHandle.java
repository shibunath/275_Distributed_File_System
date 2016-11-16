package gash.router.dns.server;


import gash.router.client.CommConnection;
import gash.router.server.CommandInit;
import gash.router.server.CommonUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

public  class OutgoingMsgHandle { //create new channel for the leader
	private EventLoopGroup group;
	 private ChannelFuture channel;
	 private WorkInitDNS wi;
	 private Bootstrap b;
	
	public void OutgoingMessage(){
	
		group = new NioEventLoopGroup();
	try {
		wi = new WorkInitDNS(false);
		b = new Bootstrap();
		b.group(group).channel(NioSocketChannel.class).handler(wi);
		b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
		b.option(ChannelOption.TCP_NODELAY, true);
		b.option(ChannelOption.SO_KEEPALIVE, true);

		// Make the connection attempt.			

	} catch (Throwable ex) {
		System.out.println("failed to initialize the connection to leader" + ex);
		ex.printStackTrace();
	}
	
}
	
	
	public Channel createChannel(){
		Channel returnChannel = null ;
		if(IPList.getHostIP() != null){
		ChannelFuture chan = b.connect(IPList.getHostIP(), 
				 CommonUtils.COMMAND_PORT).syncUninterruptibly();
		 //connect to leader.. send message to read or write..
		 
		System.out.println("in chanelll"); 
		IPList.setLeaderChannel(chan.channel());

		// want to monitor the connection to the server s.t. if we loose the
		// connection, we can try to re-establish it.
		 ChannelClosedListen ccl = new ChannelClosedListen(this);
		returnChannel = chan.channel();
		System.out.println(" chanelll ----- "+returnChannel); 
		returnChannel.closeFuture().addListener(ccl);

//		System.out.println(chan.channel().localAddress() + " -> open: " + chan.channel().isOpen()
//				+ ", write: " + chan.channel().isWritable() + ", reg: " + chan.channel().isRegistered());
		
		
	}
		return returnChannel;}
	
	public static class ChannelClosedListen implements ChannelFutureListener {
		OutgoingMsgHandle omh;

		public ChannelClosedListen(OutgoingMsgHandle omh) {
			this.omh = omh;
		}

		@Override
		public void operationComplete(ChannelFuture future) throws Exception {
			// we lost the connection or have shutdown.
			System.out.println("--> client lost connection to the server");
			System.out.flush();

			// @TODO if lost, try to re-establish the connection
		}
	}
}
	
