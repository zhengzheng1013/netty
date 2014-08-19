package netty.echo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class EchoClient {
	
	private String host;
	
	private int port;
	
	private ByteBuf buffer;
	
	public EchoClient(String host, int port) {
		this.host = host;
		this.port = port;
		
		buffer = Unpooled.buffer(1024);
	}
	
	public void start() {
		EventLoopGroup elGroup = new NioEventLoopGroup();
		
		Bootstrap boot = new Bootstrap();
		boot.group(elGroup)
			.channel(NioSocketChannel.class)
			.option(ChannelOption.TCP_NODELAY, true)
			.handler(new ChannelInitializer<SocketChannel>() {
				@Override
				protected void initChannel(SocketChannel channel) throws Exception {
					ChannelPipeline pipeline = channel.pipeline();
					pipeline.addLast(new EchoClientHandler());
				}
			});
		
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		try {
			Channel channel = boot.connect(host, port).sync().channel();
			String line;
			ChannelFuture lastWriterFuture = null;
			while((line = in.readLine()) != null) {
				buffer.clear();
				buffer.retain();
				buffer.writeBytes(line.getBytes());
				
				// 和Mina的EchoServer配合时，Mina的TextLineCodecFactory需要发送内容最后为'\n'
				buffer.writeByte('\n');
				
				lastWriterFuture = channel.writeAndFlush(buffer);
				if("bye".equalsIgnoreCase(line)) {
					channel.closeFuture().sync();
					break;
				}
			}
			
			if(lastWriterFuture != null) {
				lastWriterFuture.sync();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			elGroup.shutdownGracefully();
		}
	}
	
	public static void main(String[] args) {
		EchoClient client = new EchoClient("localhost", 9999);
		client.start();
	}
}
