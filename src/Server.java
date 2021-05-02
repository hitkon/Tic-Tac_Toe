
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.*;
import java.util.*;

class Pair<F, T>{
	public F first;
	public T second;
	Pair(F a, T b){
		this.first = a;
		this.second = b;
	}
}


class Game{
	public int id;
	public ServerListener Pl1;
	public ServerListener Pl2;
	public boolean isFirst;
	public int turn;
	public int map[][] = new int[3][3];
	Game(int id, ServerListener Pl1, ServerListener Pl2) {
		turn  =0;
		this.id = id;
		isFirst = true;
		this.Pl1 = Pl1;
		this.Pl2 = Pl2;
		for(int i = 0; i < 3; i++) {
			for(int j = 0; j < 3; j++) {
				map[i][j] =0;
			}
		}
	}
	public String toString() {
		String Ans = "";
		for(int i = 0; i < 3; i++) {
			for(int j = 0; j < 3; j++) {
				Ans += Integer.toString(map[i][j]);
			}
		}
		return Ans;
	}
	public boolean Check(int x) {
		this.turn++;
		for(int i = 0; i < 3; i++) {
			boolean p = true;
			for(int j = 0; j < 3; j++) {
				if(map[i][j] != x) {
					p = false;
				}
			}
			if(p == true) {
				return true;
			}
		}
		for(int i = 0; i < 3; i++) {
			boolean p = true;
			for(int j = 0; j < 3; j++) {
				if(map[j][i] != x) {
					p = false;
				}
			}
			if(p == true) {
				return true;
			}
		}
		boolean p = true;
		for(int i = 0; i < 3; i++) {
			if(map[i][i] != x)
				p = false;
		}
		if(p == true)
			return true;
		p = true;
		for(int i = 0; i < 3; i++) {
			if(map[i][3 - i - 1] != x)
				p = false;
		}		
		return p;
	}
	
}

public class Server {

	public static int ID = 0;
	private static Socket clientSocket;
	private static ServerSocket server;
	private static BufferedReader in;
	private static BufferedWriter out;
	public static LinkedList<ServerListener> serverList = new LinkedList<>();
	public static LinkedList<Pair<String, String> > requestList = new LinkedList<>();
	public static LinkedList<Game> gameList = new LinkedList<>();
	public static HashSet<String>Nicks = new HashSet<String>();
	
	
	public static void main(String[] args) {
		try {
			server = new ServerSocket(4444);
			System.out.println("Server started");
			
			try {
				while(true) {
					Socket sock = server.accept();
					try {
						System.out.println("New Connection");
						serverList.add(new ServerListener(sock));
					}
					catch(Exception e) {
						sock.close();
					}
				}
			}
			finally {
				server.close();
			}
		}
		catch(Exception e) {
			
		}
	}
}

class ServerListener extends Thread{
	private Socket sock;
	private String Nickname;
	private BufferedReader in;
	private BufferedWriter out;
	
	public ServerListener(Socket sock) throws IOException {
		this.sock = sock;
		in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
		out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
		start();
	}
	
	public void run() {
		String word;
		try {
			while(true) {
//				if(this.sock.isClosed()) {
//					System.out.println("Client Crash");
//					Server.Nicks.remove(this.Nickname);
//					this.interrupt();
//					Server.serverList.remove(this);
//					break;
//				}
				word = in.readLine();
				System.out.println(word);
				if(word == null)
					break;
				if(word.charAt(0) == 'n') {
					String Nick = word.substring(1);
					if(Server.Nicks.contains(Nick)) {
						this.send("n");
						continue;
					}
					Server.Nicks.add(Nick);
					this.Nickname = Nick;
					this.send("y");
				}
				if(word.charAt(0) == 'p') {
					String Ans = "p";
					for(ServerListener sv : Server.serverList) {
						Ans = Ans + sv.Nickname + "|";
					}
					for(ServerListener sv : Server.serverList) {
						if(sv == this) {
							sv.send(Ans);
						}
					}
				}
				if(word.charAt(0) == 'e') {
					//String Nick = word.substring(1);
					Server.Nicks.remove(this.Nickname);
					this.interrupt();
					Server.serverList.remove(this);
					return;
				}
				if(word.charAt(0) == 'r') {
					String Nick = word.substring(1);
					boolean isFind = false;
					for(ServerListener sv : Server.serverList) {
						if(sv.Nickname.equals(Nick)) {
							Server.requestList.add(new Pair<>(Nickname, sv.Nickname));
							sv.send("r" + this.Nickname);
							isFind = true;
							break;
						}
					}
					if(isFind == false) {
						this.send("q");
					}
				}
				if(word.charAt(0) == 'a') {
					int id = Server.ID;
					Game g = null;
					int r = (new Random()).nextInt() % 2;
					String Nick = "";
					for(Pair<String, String> V : Server.requestList) {
						if(V.second == Nickname) {
							Nick = V.first;
							Server.requestList.remove(V);
							break;
						}
					}
					
					if(r == 0) {
						this.send("y1|" + id + "|" + "000000000");
						for(ServerListener sv : Server.serverList) {
							if(sv.Nickname == Nick) {
								sv.send("y2|" + id);
								g = new Game(Server.ID++, this, sv);
							}
						}
					}
					else {
						this.send("y2|" + id);
						for(ServerListener sv : Server.serverList) {
							if(sv.Nickname == Nick) {
								sv.send("y1|" + id + "|" + "000000000");
								g = new Game(Server.ID++, sv, this);
							}
						}
					}
					Server.gameList.add(g);
				}
				if(word.charAt(0) == 'd') {
					String Nick = "";
					for(Pair<String, String> V : Server.requestList) {
						if(V.second == Nickname) {
							Nick = V.first;
							Server.requestList.remove(V);
							break;
						}
					}
					for(ServerListener sv : Server.serverList) {
						if(sv.Nickname == Nick) {
							sv.send("d");
							break;
						}
					}
				}
				if(word.charAt(0) == 't') {
					String id = word.substring(1, word.indexOf('|', 2));
					int id2 = Integer.parseInt(id, 10);
					String t = word.substring(word.indexOf('|')+1);
					for(Game g : Server.gameList) {
						if(g.id == id2) {
							if(g.isFirst) {
								g.map[Integer.parseInt(t.substring(0, 1))][Integer.parseInt(t.substring(1))] = 1;
								if(g.Check(1)) {
									g.Pl1.send("ow" + g.toString());
									g.Pl2.send("ol" + g.toString());
									Server.gameList.remove(g);
									break;
								}
								if(g.turn == 9) {
									g.Pl1.send("od" + g.toString());
									g.Pl2.send("od" + g.toString());
									Server.gameList.remove(g);
									break;
								}
								g.Pl2.send("t" + id + "|" + g.toString());
							}
							else {
								g.map[Integer.parseInt(t.substring(0, 1))][Integer.parseInt(t.substring(1))] = 2;
								if(g.Check(2)) {
									g.Pl1.send("ol" + g.toString());
									g.Pl2.send("ow" + g.toString());
									Server.gameList.remove(g);
									break;
								}
								if(g.turn == 9) {
									g.Pl1.send("od" + g.toString());
									g.Pl2.send("od" + g.toString());
									Server.gameList.remove(g);
									break;
								}
								g.Pl1.send("t" + id + "|" + g.toString());
							}
							
							g.isFirst = !g.isFirst;
							break;
						}
					}
					
				}
				
				
			}
		}
		catch(Exception e) {
			System.err.println(e);
			Server.Nicks.remove(this.Nickname);
			this.interrupt();
			Server.serverList.remove(this);	
		}
		Server.Nicks.remove(this.Nickname);
		this.interrupt();
		Server.serverList.remove(this);
	}
	private void send(String word) {
		try {
		System.out.println(word);
		out.write(word + "\n");
		out.flush();
		}
		catch(Exception e){
			
		}
	}
	
}