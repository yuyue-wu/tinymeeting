package com.nsn.lib.pushserver;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class Server{
	public final static  int port	= 4000;//port 服务器监听的端口号
	public final static  long channelLife	= 1200;//channelLife 长连接的寿命
	public final static  int refreshPeriod	= 1000*60;//refreshPeriod 检查过期连接的周期
	
	private static Server server=new Server();
	
	private Selector serverSelector;
	private Selector clientSelector;
	private ServerSocket serverSocket;
	private final AtomicBoolean addNewConnect = new AtomicBoolean(false);
	private final SocketChannelPool socketChannelPool = new SocketChannelPool();
	
	
	private ListenThread 	listen;
	private HeatbeatThread 	heatbeat;
	private Timer	timeOutChecker;
	
	private Server(){
		
	}
	public static Server getInstance(){
		return  server;
	}
	/**
	 * 启动pushserver
	 * @throws IOException
	 */
	public void startUp() throws IOException{
		serverSelector = Selector.open();
		clientSelector = Selector.open();
		
		//开启端口
		ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.configureBlocking(false);
		serverSocket = serverSocketChannel.socket();
		InetSocketAddress address = new InetSocketAddress(port);
		
		serverSocket.bind(address);
		serverSocketChannel.register(serverSelector, SelectionKey.OP_ACCEPT);
		
		listen 		= new ListenThread(serverSelector, clientSelector);
		heatbeat 	= new HeatbeatThread(clientSelector);
		timeOutChecker = new Timer();
		
		listen.start();
		heatbeat.start();
		timeOutChecker.schedule(new TimeoutChecker(),0, refreshPeriod);
	}
	
	/**
	 * 关闭pushserver，释放资源
	 */
	@SuppressWarnings("deprecation")
	public void shutDown(){
		listen.stop();
		heatbeat.stop();
		timeOutChecker.cancel();
		try {
			serverSocket.close();
			serverSelector.close();
			clientSelector.close();
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
	private void push(String message){
		if (message == null){
			return;
		}
		ByteBuffer buffer;
		for(SocketChannel sc:socketChannelPool.getChannels()){
			try {
				buffer = ByteBuffer.wrap(message.getBytes());
				sc.write(buffer);
			} catch(IOException e) {
				try {
					if(sc.keyFor(clientSelector) != null){
						sc.keyFor(clientSelector).cancel();
					}
					sc.close();
					socketChannelPool.remove(sc);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			} 
		}
	}
	/**
	 * 推送消息
	 */
	public void pushOnLineClients(){
		int count=socketChannelPool.getChannels().size();
		System.out.println(count+" alive connect remain");
		if(count>0){
			String message=socketChannelPool.getChannelsInfo();
			System.out.println("push message: "+message);
			push(message);
		}
		
	}
	
	/**
	 * 监听连接的进程。
	 * 监听到连接后把socketchannel注册到clientSelector，
	 * 并且把对应的channel添加到socketChannelPool里。
	 */
	class ListenThread extends Thread{
		Selector serverSelector;
		Selector clientSelector;
		
		ListenThread(Selector serverSelector,Selector clientSelector){
			this.serverSelector = serverSelector;
			this.clientSelector = clientSelector;
		}
		
		public void run(){
			while(true){
				try {
					serverSelector.select();
					Set<SelectionKey> 		selectionKeys 	= serverSelector.selectedKeys();
					Iterator<SelectionKey> 	iter 			= selectionKeys.iterator();
					SocketChannel 			socketChannel;
					ServerSocketChannel 	serverChannel;
					while (iter.hasNext()){
						SelectionKey key = iter.next();
						if (key.isAcceptable()){
							serverChannel = (ServerSocketChannel) key.channel();
							
							
							
							socketChannel = serverChannel.accept();
							socketChannel.configureBlocking(false);
							
							
							
							
							addNewConnect.set(true);
							clientSelector.wakeup();
							socketChannel.register(clientSelector, SelectionKey.OP_READ);
							socketChannelPool.put(socketChannel, channelLife);
							addNewConnect.set(false);
							
							
							System.out.println("a new client connect.");
							

							iter.remove();
						}
					}
				} catch (IOException e) {
					System.out.println("pushserver occur an error!");
				}
			}
		}
	}

	/**
	 * 心跳进程。接受用户发送过来的心跳，更新对应channel的状态。
	 */
	class HeatbeatThread extends Thread {
		Selector clientSelector;
		Channel channel;
		
		HeatbeatThread(Selector clientSelector){
			this.clientSelector = clientSelector;
		}
		
		public void run(){
			Set<SelectionKey> 		selectionKeys;
			Iterator<SelectionKey> 	iter;
			ByteBuffer 				buf = ByteBuffer.allocate(512);
			SocketChannel 			socketChannel;
			SelectionKey 			key;
			while(true){
				try {
					if(!addNewConnect.get()){
						clientSelector.select();
						selectionKeys	= clientSelector.selectedKeys();
						iter 			= selectionKeys.iterator();
						while (iter.hasNext()){
							key = iter.next();
							if (key.isReadable()){
								socketChannel = (SocketChannel) key.channel();
								try {
									buf.clear();
									int count=socketChannel.read(buf);
									if(count!=-1){
										if (count==1){
											System.out.println("get beat from "+socketChannel.socket().getInetAddress().getHostAddress() );
											socketChannelPool.UpdateListTime(socketChannel);
										}
										else{
											System.out.println("get user info "+socketChannel.socket().getInetAddress().getHostAddress() );
											socketChannelPool.UpdateUserName(socketChannel,new String( buf.array(),0,count));
											server.pushOnLineClients();
						
										}
									}
									
								} catch (IOException e) {
									try {
										socketChannel.close();
										key.cancel();
										socketChannelPool.remove(socketChannel);
										System.out.println("timeout connect be finded.finish cleaning.");
										server.pushOnLineClients();
									} catch (Exception e1) {
										System.out.println("failed to clean timeout connect");
									}
								}
								iter.remove();
							}
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 *
	 */
	class TimeoutChecker extends TimerTask{
		
		public void run(){
			for(SocketChannel sc : socketChannelPool.getChannels()){
				try {
					if(socketChannelPool.isTimeout(sc)){
						socketChannelPool.remove(sc);
						
						if(sc.keyFor(clientSelector) != null){
							sc.keyFor(clientSelector).cancel();
						}
						sc.close();
						socketChannelPool.remove(sc);
						server.pushOnLineClients();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			System.out.println(socketChannelPool.getChannels().size()+" alive connect remain~~~~~~~~~~~~~");
			
		}
	}
	  public static void main(String args[]) throws Exception {
	        Server.getInstance().startUp();
	    }

}