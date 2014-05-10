package com.nsn.lib.pushserver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
//import java.util.Observable;
//import java.util.Observer;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;



public class Client extends java.util.Observable {
	private static Client client=new Client();
	private StringBuilder latestResponse = new StringBuilder();
	private boolean online=false;
	private final AtomicBoolean addNewConnect = new AtomicBoolean(false);
	
	private ListenThread 	listen;
	private HeatbeatThread heatbeat;
	
	private Selector serverSelector;
	private Selector clientSelector;
	private Client(){
		
	}
	public static Client getInstance(){
		return  client;
	}
	public void connectToServer(String serverIP, int serverPort, String userName) throws IOException{

		
			InetSocketAddress SERVER_ADDRESS = new InetSocketAddress(serverIP,serverPort);

			SocketChannel socketChannel = SocketChannel.open();
			socketChannel.configureBlocking(false);
			serverSelector = Selector.open();

			socketChannel.register(serverSelector, SelectionKey.OP_CONNECT);
			
			socketChannel.connect(SERVER_ADDRESS);

			clientSelector = Selector.open();
			heatbeat = new HeatbeatThread(clientSelector);
			heatbeat.start();

			listen= new ListenThread(serverSelector, clientSelector,userName);
			listen.start();
			

		

	}
	/**
	 * 关闭pushserver，释放资源
	 */
	@SuppressWarnings("deprecation")
	public void shutDown(){
		listen.stop();
		heatbeat.stop();
		try {
			serverSelector.close();
			clientSelector.close();
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			
		}
	}
	/**
	 * 监听连接的进程。
	 */
	class ListenThread extends Thread{
		Selector serverSelector;
		Selector clientSelector;
		String userName;
		
		ListenThread(Selector serverSelector,Selector clientSelector,String userName){
			this.serverSelector = serverSelector;
			this.clientSelector = clientSelector;
			this.userName=userName;
		}
		
		public void run(){
			while(true){
				Set<SelectionKey> selectionKeys;
				Iterator<SelectionKey> iterator;
				SelectionKey selectionKey;
				SocketChannel client;
				int count = 0;
				try{
					serverSelector.select();
					selectionKeys = serverSelector.selectedKeys();
					iterator = selectionKeys.iterator();
					while (iterator.hasNext()) {
						selectionKey = iterator.next();
						try {
							if (selectionKey.isConnectable()) {
//								System.out.println("client connect");
								client = (SocketChannel) selectionKey.channel();

								if (client.isConnectionPending()) {
									client.finishConnect();
									online=true;
									System.out.println("完成连接!");
									ByteBuffer sendbuffer = ByteBuffer.allocate(userName.getBytes().length);
									sendbuffer.clear();
									sendbuffer.put(userName.getBytes());
									sendbuffer.flip();
									client.write(sendbuffer);
								}
								client.register(serverSelector, SelectionKey.OP_READ);

							} else if (selectionKey.isReadable()) {
//								System.out.println("TT");
								client = (SocketChannel) selectionKey.channel();
								ByteBuffer receivebuffer = ByteBuffer.allocate(10);
								
								receivebuffer.clear();
								addNewConnect.set(true);
								clientSelector.wakeup();
								count = client.read(receivebuffer);
								addNewConnect.set(false);
								if (count > 0) {
									String temp = new String(receivebuffer.array(), 0,
											count);
									if (latestResponse.toString().contains("\n"))
										latestResponse = new StringBuilder();
									latestResponse.append(temp);
									if (temp.toCharArray()[temp.length() - 1] == '\n') {

										System.out.println(latestResponse.toString());
										setChanged();  
										notifyObservers();
										addNewConnect.set(true);
										clientSelector.wakeup();
										client.register(clientSelector, SelectionKey.OP_WRITE);
										addNewConnect.set(false);

									}

								}
								

								
							}
						}catch(Exception e){
							e.printStackTrace();
							online=false;
							shutDown();
							
						}
						
					}
					selectionKeys.clear();
				
				}catch(Exception e){
					online=false;
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

		HeatbeatThread(Selector clientSelector) {
			this.clientSelector = clientSelector;
		}

		public void run() {
			Set<SelectionKey> selectionKeys;
			Iterator<SelectionKey> iter;
			SocketChannel client;
			SelectionKey selectionKey;
			try {
				
				while (true) {
					try {
						if (!addNewConnect.get()) {
							clientSelector.select();
							selectionKeys = clientSelector.selectedKeys();
							iter = selectionKeys.iterator();

							while (iter.hasNext()) {
								selectionKey = iter.next();
								if (selectionKey.isWritable()) {
									
									try {
										client = (SocketChannel) selectionKey.channel();
										ByteBuffer sendbuffer = ByteBuffer.allocate(1);
										sendbuffer.clear();
										sendbuffer.put((byte) 0xFF);
										sendbuffer.flip();
										addNewConnect.set(true);
										client.write(sendbuffer);
										addNewConnect.set(false);
										System.out.println("send beat to server");
										Thread.sleep(5000);
									} catch (InterruptedException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									
									} catch (IOException e) {
										e.printStackTrace();
										online=false;
										shutDown();
									}
									
									// client.register(selector,
									// SelectionKey.OP_WRITE);

								}
								iter.remove();
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
						online=false;
						shutDown();

					}
				}
			}
			catch(Exception e){
				e.printStackTrace();
				online=false;
				shutDown();

			}
			
			
		}
	}
	@SuppressWarnings("unused")
	private String getLatestResponse(){
		return latestResponse.toString();
	}
	public String getLatestResponseStr(){
		return latestResponse.substring(0, latestResponse.length()-1);
	}
	public boolean getOnline(){
		return online;
	}
	public static void main(String args[]) {
		Client client=Client.getInstance();
		try {
			client.connectToServer("localhost", 4000, "huoling2");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			client.shutDown();
		}
//		System.out.println(client.getLatestResponse());
//		Client.getInstance().addObserver(new Observer(){
//
//			@Override
//			public void update(Observable o, Object arg) {
//				System.out.println("update");
//				
//			}
//			
//		});
	}
	

}
