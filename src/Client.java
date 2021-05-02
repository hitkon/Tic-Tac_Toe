
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;
import java.io.*;

import java.nio.*;


public class Client {
	static int port = 4444;
	//static String addr= "25.37.240.51";
	static String addr= "localhost";
	public static void main(String[] args) {
		new ClientT(addr, port);
	}
	
}

class ClientT{
	private static Socket sock;
	private static BufferedReader read;
	private static BufferedReader in;
	private static BufferedWriter out;
	private int port;
	private String addr;
	private String Nickname;
//	private Date time;
//	private String dtime;
//	private SimpleDateFormat dt1;
	
	ClientT(String addr, int port) {
		this.port = port;
		this.addr = addr;
		try {
			this.sock = new Socket(addr, port);
		}catch(Exception e){
			System.err.println("Socket failed connection: " + e);
		}
		try {
			read = new BufferedReader(new InputStreamReader(System.in));
			in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
			pressNickName();
			ThreadPause TP = new ThreadPause(); 
			
			new ReadMsg(TP).start();
		}catch(Exception e){
			ClientT.this.downSock();
		}
	}
	private void pressNickName() {
		System.out.println("Enter your NickName:");
		try {
			while(true) {
			Nickname = read.readLine();
			out.write("n" + Nickname + "\n");
			out.flush();
			String s = in.readLine();
			if(s.equals("y"))
				break;
			System.out.println("Nickname is already used. Enter your NickName again:");
			}
		
		}
		catch(Exception e) {}
	}
	private void downSock() {
		try {
			if(!sock.isClosed()) {
				sock.close();
				in.close();
				out.close();
			}
				
		}
		catch(IOException ignored){}
		
	}
	class ThreadPause{
		public boolean isReadMsgPaused = false;
		public boolean isWriteMsgPaused = false;
		public String lastReadestString = null; 
	}
	
	private class ReadMsg extends Thread{
		ThreadPause TP = null;
		ReadMsg(ThreadPause TP){
			this.TP=TP;
		}
		//ReentrantLock lock;
		
		private void PrintMap(String map) {
			System.out.println("Map:");
			for(int i = 0; i < 3; i++) {
				for(int j = 0; j < 3; j++) {
					if(map.charAt(i * 3 + j) == '1')
						System.out.print("X");
					else if(map.charAt(i * 3 + j) == '2')
						System.out.print("0");
					else
						System.out.print(".");
				}
				System.out.println();
			}
		}
		private TreeMap<String, String> Opponents = new TreeMap<>();
		private String lastOpponentName = "";
		
		private int Check(String a) {
			if(a.equals("1"))
				return 1;
			if(a.equals("2"))
				return 2;
			if(a.equals("3"))
				return 3;
			return -1;
		}
		private String Turn(String map) {
			System.out.print("Enter your turn (xy).");
			if(isMyFirstTurn)
				System.out.println("You are X");
			else
				System.out.println("You are 0");
			String s = null;
			int X = -1;
			int Y = -1;
			try {
			while(!read.ready()) {};
			s = read.readLine();
			while(true) {
				String x = s.substring(0, 1);
				String y = s.substring(1);
				X = Check(x);
				Y = Check(y);
				if(X != -1 && Y != -1 && map.charAt((X - 1) * 3 + (Y-1)) == '0')
					break;
				System.out.println("Unexpected turn. Enter your turn (xy)");
				while(!read.ready()) {};
				s = read.readLine();
			}
			}
			catch(Exception e){}
			return "" + (X - 1) + (Y-1);
		}
		
		boolean isMyFirstTurn = false;
		
		
		public void run() {
			String str;
			WriteMsg WM = new WriteMsg(TP);
			WM.start();
			this.setPriority(MAX_PRIORITY);
			
			try {
				while(true) {
					if(TP.isReadMsgPaused == true)
						continue;
					if(in.ready()) {
						str = in.readLine();
					
					if(str.charAt(0) == 'p') {
						String s = str.substring(1);
						StringTokenizer st = new StringTokenizer(s, "|");
						System.out.println("Players List:");
						while(st.hasMoreTokens())
							System.out.println(st.nextToken());
					}
					else if(str.charAt(0) == 'q') {
						System.out.println("This player does not exist");
						TP.isWriteMsgPaused = false;
					}
					else if(str.charAt(0) == 'd') {
						System.out.println("Player declined your request");
						TP.isWriteMsgPaused = false;
					}
					else if(str.charAt(0) == 'r') {
						//WM.wait();
						TP.isWriteMsgPaused = true;
						String s = str.substring(1);
						System.out.println(s + " sent request to you\n Enter (a)ccept|(d)decline");

						while(!read.ready()) {};
						String Ans = read.readLine();
						
						while(Ans.charAt(0) != 'a' &&  Ans.charAt(0) != 'd') {
							while(!read.ready()) {};
							Ans = read.readLine();
							
							System.out.println("Unexpected answer. Enter again");
						}
						
//						read.close();
//						read = new BufferedReader(new InputStreamReader(System.in));
						
						if(Ans.charAt(0) == 'd') {
							TP.isWriteMsgPaused = false;
							//WM.notify();
							out.write("d\n");
						}
						else {
							lastOpponentName = s;
							out.write("a\n");
						}
						out.flush();
						//TP.isWriteMsgPaused = false;
						
					}
					else if(str.charAt(0) == 'y') {
						TP.isWriteMsgPaused = true;
						String s = str.substring(3);
						StringTokenizer st = new StringTokenizer(s, "|");
						String id = st.nextToken();
						Opponents.put(id, lastOpponentName);
						if(str.charAt(1) == '1') {
							String map = st.nextToken();
							isMyFirstTurn = true;
							PrintMap(map);
							String Ans = "t" + id + "|" + Turn(map) + "\n";
							out.write(Ans);
							out.flush();
						}
						else {
							isMyFirstTurn = false;
							System.out.println("Waiting for opponent turn");
							
						}
					}
					else if(str.charAt(0) == 'o') {
						String map = str.substring(2);
						if(str.charAt(1) == 'w') {
							System.out.println("You win");
						}
						else if(str.charAt(1) == 'l') {
							System.out.println("You loose");
						}
						else {
							System.out.println("Demand");
						}
						PrintMap(map);
						WM.PrintMenu();
						TP.isWriteMsgPaused = false;
					}
					else if(str.charAt(0) == 't') {
						String s = str.substring(1);
						StringTokenizer st = new StringTokenizer(s, "|");
						String id = st.nextToken();
						String map = st.nextToken();
						PrintMap(map);
						String Ans = "t" + id + "|" + Turn(map) + "\n";
						out.write(Ans);
						out.flush();
					}
					else if(str.equals("exit")) {
						ClientT.this.downSock();
						break;
					}
					//System.out.println(str);
					}
				}
			}
			catch(Exception e) {}
		}
	}
	private class WriteMsg extends Thread{
		ThreadPause TP = null;
		
		WriteMsg(ThreadPause TP){
			this.TP = TP;
		}
		
		private void PrintMenu() {
			System.out.println("Enter:");
			System.out.println("(p)layers|(r)equest|(e)xit");
		}
		
		public void run() {
			String userWord = null;
			PrintMenu();
			int x = 1;
			while(true) {
				//System.out.println(TP.isWriteMsgPaused);
//				x++;
//				if(x == 100000000) {
//					System.out.println(TP.isWriteMsgPaused);
//					x = 0;
//				}
				try {
				if(TP.isWriteMsgPaused == true) {
					this.sleep(1000);
					continue;
				}
				}
				catch(Exception e) {
					System.err.println(e);
				}
//				if(x == 1000000) {
//					System.out.println(TP.isWriteMsgPaused);
//					x = 0;
//				}
				
			try {
				//PrintMenu();
				
				if(read.ready()) {
					userWord = read.readLine();
					if(userWord.equals("")) {
						
						continue;
					}
					//System.out.println(userWord);
				
//				TP.lastReadestString = userWord;
//				if(TP.isWriteMsgPaused == true)
//					continue;
//				TP.lastReadestString = null;
				
				if(userWord.charAt(0) == 'p') {
					out.write("p\n");
					out.flush();
				}
				else if(userWord.charAt(0) == 'r') {
					System.out.println("Enter your opponent Nickname");
					while(!read.ready()) {};
					String s = read.readLine();
					while(s.equals(Nickname)) {
						System.out.println("You can't request yourself");
						while(!read.ready()) {};
						s=read.readLine();
					}
					out.write("r" + s + "\n");
					out.flush();
					System.out.println("Waiting for answer");
					TP.isWriteMsgPaused = true;
				}
				else if(userWord.charAt(0) == 'e') {
					out.write("e\n");
					out.flush();
					ClientT.this.downSock();
					break;
				}
				//PrintMenu();
				}
			}
			catch(Exception e) {
				ClientT.this.downSock();
			}
			}
		}
	}
}