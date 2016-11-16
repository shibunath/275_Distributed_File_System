package gash.router.dns.server;

import java.util.HashMap;


import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class MappingServer {  // running for incoming events...
	protected static HashMap<Integer, ServerBootstrap> bootstrap = new HashMap<Integer, ServerBootstrap>();

   public MappingServer(){
	   StartHostCommunication shc = new StartHostCommunication();
	   
	   shc.run();
   }

	private static class StartHostCommunication implements Runnable{

		@Override
		public  void run() {
			
			
			// TODO Auto-generated method stub
			EventLoopGroup bossGroup = new NioEventLoopGroup();
			EventLoopGroup workerGroup = new NioEventLoopGroup();

			try {
				ServerBootstrap b = new ServerBootstrap();
				bootstrap.put(4000, b);
				b.group(bossGroup, workerGroup);
				b.channel(NioServerSocketChannel.class);
				b.option(ChannelOption.SO_BACKLOG, 100);
				b.option(ChannelOption.TCP_NODELAY, true);
				b.option(ChannelOption.SO_KEEPALIVE, true);
				
				boolean compressComm = false;
				b.childHandler(new DNSServerHandler());
				
				ChannelFuture f = b.bind(4000).syncUninterruptibly();
				
				// block until the server socket is closed.
				f.channel().closeFuture().sync();
			} catch (Exception ex) {

			}
			finally {
				// Shut down all event loops to terminate all threads.
				bossGroup.shutdownGracefully();
				workerGroup.shutdownGracefully();


			}
		}
	}
}

