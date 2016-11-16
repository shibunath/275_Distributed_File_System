package gash.router.client;

import gash.router.server.CommonUtils;
import global.Global.GlobalCommandMessage;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import pipe.common.Common.Header;
import storage.Storage;
import storage.Storage.Action;


public  class DNSConnectionInit {
	private  EventLoopGroup group;
	private  String host = "192.168.1.31";
	//private  String host = CommonUtils.DNS_HOST;// ip of dns server
	private  int port = CommonUtils.DNS_PORT; //port of the dns server
	private  ChannelFuture channel;

	public  DNSConnectionInit() {
		init();
	}

	public  void init(){
		group = new NioEventLoopGroup();
		try{
		ClientDNSHandleInit cdi = new ClientDNSHandleInit( false);
		Bootstrap b = new Bootstrap();
		b.group(group).channel(NioSocketChannel.class).handler(cdi);
		b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
		b.option(ChannelOption.TCP_NODELAY, true);
		b.option(ChannelOption.SO_KEEPALIVE, true);
		System.out.println("in init my");

		// Make the connection attempt.
		channel = b.connect(host, port).syncUninterruptibly();
		Header.Builder hb = Header.newBuilder();
		hb.setNodeId(32); // tmp
		hb.setDestination(-1);
		hb.setTime(1);
		
		GlobalCommandMessage.Builder gcb =  GlobalCommandMessage.newBuilder();
		gcb.setHeader(hb);
		
		Storage.Query.Builder sqb = Storage.Query.newBuilder();
		sqb.setAction(Action.WHICHSERVER);
		gcb.setQuery(sqb);
		channel.channel().writeAndFlush(gcb.build());

		}catch(Exception e){
			System.out.println(e);
		}
	}
	

}
