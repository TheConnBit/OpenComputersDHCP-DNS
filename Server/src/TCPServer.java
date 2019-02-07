import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

class Data {
	public int MapSize;
	public String[][] IPMap;
	public HashMap<String, String[]> PortMap;
	public String[] domains = new String[] {"ru", "net", "server"};
	Data(String[] domainlist) {
		MapSize = 8192;
		IPMap = new String[MapSize][2];
		PortMap = new HashMap<String, String[]>(); 
		domains = domainlist;
	}
}

class Client extends Thread {
	Socket ClientConn;
	String IDClient;
	String IPClient;
	int ID;
	static Data Map;
	Timer timer;
    boolean connected = true;
    boolean ping = true;
    static String dnsfolder = new File(".").getAbsolutePath()+"\\dns";
    Client(Socket connectionSocket, Data data){
        ClientConn = connectionSocket;
        Map = data;
        timer = new Timer();
        timer.schedule(check, 35000, 35000);
    }
     
    TimerTask check = new TimerTask() {
        public void run()
        {
        	if (ping == false) {
        		connected = false;
        	} else {
        		ping = false;
        	}
        }
      };
      
	public void run(){
		String[] IDC;
		String IDCD = "";
		do {
			IDCD = Recv(ClientConn);
			IDC = IDCD.split("FF");
		} while (!IDC[0].equals("register"));
		IDClient = IDC[1];
		IDC = null;
		IDCD = null;
	    System.out.println("[INFO] [DHCP] Client ID: " + IDClient + " connected");
	    IPClient = ClientRegister(IDClient);
	    if (IPClient == "isUsed") {
	    	Trans(ClientConn, "isused");
	    	connected = false;
	    } else {
	    	if (IPClient == "") {
		    	Trans(ClientConn, "failed");
		    	connected = false;
	    	} else {
	    		Trans(ClientConn, IPClient);
				for (int i = 0; i < Map.MapSize; i++) {
					if (Map.IPMap[i][0].equals(IPClient)) {
						ID = i;
					}
				}
	    		System.out.println("[INFO] [DHCP] Client ID: " + IDClient + " issued IP: " + IPClient);
	    		Map.PortMap.put(IDClient, new String[255]);
	    		for (int i = 0; i < 255; i++) {
	    			Map.PortMap.get(IDClient)[i] = "#";
	    		}
	    	}
	    }
	    while (connected) {
				String data = RecvND(ClientConn);
				if (data != "" & data != null) {
					if (data.equals("exit#")) {
						connected = false;
					}
					if (data.equals("ping#")) {
						ping = true;
						Trans(ClientConn, "pong");
					}
					String[] Sdata = data.split("FF");
					if (Sdata[0].equals("registerdomain")) {
						if (Sdata.length == 2) {
							String response = RegisterDomain(Sdata[1], IDClient);
							Trans(ClientConn, response);
						} else {
							Trans(ClientConn, "0");
						}
					}
					if (Sdata[0].equals("unregisterdomain")) {
						if (Sdata.length == 2) {
							String response = UnRegisterDomain(Sdata[1], IDClient);
							Trans(ClientConn, response);
						} else {
							Trans(ClientConn, "0");
						}
					}
					if (Sdata[0].equals("reslove")) {
						if (Sdata.length == 2) {
							String response = Resolve(Sdata[1]);
							Trans(ClientConn, response);
						} else {
							Trans(ClientConn, "0");
						}
					}
					if (Sdata[0].equals("send")) {
						if (Sdata.length == 4) {
							Send(IPClient, Sdata[1], Integer.parseInt(Sdata[2]), Sdata[3]);
							System.out.println("[INFO] [RET] Data ("+Sdata[3]+") received from IP "+IPClient+" to IP "+Sdata[1]+" is written to the buffer port "+Sdata[2]);
						}
					}
					if (Sdata[0].equals("getbuffer#")) {
						if (!Map.PortMap.get(IDClient)[Integer.parseInt(Sdata[1])].equals("#")) { //The data from the buffer port 234 has been requested by the recipient
							System.out.println("[INFO] [RET] The data ("+Map.PortMap.get(IDClient)[Integer.parseInt(Sdata[1])]+") from the buffer port "+Integer.parseInt(Sdata[1])+" has been requested by the recipient IP "+IPClient);
							Trans(ClientConn, Map.PortMap.get(IDClient)[Integer.parseInt(Sdata[1])]);
							Map.PortMap.get(IDClient)[Integer.parseInt(Sdata[1])] = "#";
						} else {
							Trans(ClientConn, "empity");
						}
					}
				}
				
	    }
	    if (IPClient == "isUsed") {
	    	System.out.println("[INFO] [DHCP] Client ID: " + IDClient + " already used");
	    } else {
	    	System.out.println("[INFO] [DHCP] Client ID: " + IDClient + " IP: " + IPClient + " disconnected!");
	    }
	    try {
			ClientConn.close();
		} catch (IOException e) {
			//e.printStackTrace();
		}
	    ClientUnRegister(IDClient);
	    timer.cancel();
	    this.stop();
    }
	
	public static String Recv(Socket Conn) {
		String data = "";
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(Conn.getInputStream()));
			data = reader.readLine();
		} catch (IOException e) {
			//e.printStackTrace();
		}
		return data;
	}
	
	public static String RecvND(Socket Conn) {
		String data = "";
		try {
			InputStream readerS = Conn.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(readerS));
			if (readerS.available() > 0) {
				data = reader.readLine();
			} else {
				data = null;
			}
		} catch (IOException e) {
			//e.printStackTrace();
		}
		return data;
	}
	
	public static void Trans(Socket Conn, String data) {
		try {
			DataOutputStream writer = new DataOutputStream(Conn.getOutputStream());
			writer.writeBytes(data + "\n");
		} catch (IOException e) {
			//e.printStackTrace();
		}
	}
	
    public static String ClientRegister(String ID) {
    	String IP = null;
    	for (int i = 0; i < Map.MapSize; i++) {
    		if (Map.IPMap[i][1].equals("#")) {
	    		if (!Map.IPMap[i][0].equals("0.0")) {
	    			Map.IPMap[i][1] = ID;
		    		IP = Map.IPMap[i][0];
		    		break;
	        	}
    		}
			if (Map.IPMap[i][1].equals(ID)) {
				IP = "isUsed";
				i = Map.MapSize;
				break;
			}
    	}
		return IP;
    }
    
    public static void ClientUnRegister(String ID) {
    	for (int i = 0; i < Map.MapSize; i++) {
    		if (Map.IPMap[i][1].equals(ID)) {
    			Map.IPMap[i][1] = "#";
    			break;
    		}
    	}
    }
    
    public static void Send(String SenderIP, String IP, int port, String data) {
		for (int i = 0; i < Map.MapSize; i++) {
			if (Map.IPMap[i][0].equals(IP)) {
				if (Map.PortMap.get(Map.IPMap[i][1])[port].equals("#")) {
					Map.PortMap.get(Map.IPMap[i][1])[port] = SenderIP + "FF" + data;
					break;
				} else {
					Map.PortMap.get(Map.IPMap[i][1])[port] = Map.PortMap.get(Map.IPMap[i][1])[port] + "DD" + SenderIP + "FF" + data;
					break;
				}
			}
		}
    }
    
    public static String RegisterDomain(String domain, String hwaddress) {
    	String[] d = domain.split("\\.");
    	if (d.length == 2) {
    		boolean flag = false;
    		for (int i = 0; i < Map.domains.length; i++) {
    			if (d[1].equals(Map.domains[i])) {
    				flag = true;
    				break;
    			} else {
    				flag = false;
    			}
    		}
	    	if (flag) {
				File domainfile = new File(dnsfolder+"\\"+d[1]+"\\"+d[0]+".domain");
				if (!domainfile.exists()) {
					try {
						if (domainfile.createNewFile()) {
							FileOutputStream dfstream = new FileOutputStream(domainfile);
							dfstream.write(hwaddress.getBytes());
							dfstream.close();
							System.out.println("[INFO] [DNS] Domain "+domain+" successfully created");
							return "OK";
						} else {
							System.out.println("[WARN] [DNS] Error create domain file");
							return "3";
						}
					} catch (IOException e) {
						//e.printStackTrace();
						System.out.println("[ERROR] [SYS] Input/Output error");
						return "0";
					}
				} else {
					System.out.println("[WARN] [DNS] Domain "+domain+" already created");
					return "2";
				}
	    	} else {
	    		System.out.println("[WARN] [DNS] Incorrect domain name");
	    		return "1";
	    	}
		} else {
			System.out.println("[WARN] [DNS] Incorrect domain name");
			return "1";
		}
    }
    
    public static String UnRegisterDomain(String domain, String hwaddress) {
    	String[] d = domain.split("\\.");
    	if (d.length == 2) {
    		boolean flag = false;
    		for (int i = 0; i < Map.domains.length; i++) {
    			if (d[1].equals(Map.domains[i])) {
    				flag = true;
    				break;
    			} else {
    				flag = false;
    			}
    		}
	    	if (flag) {
				File domainfile = new File(dnsfolder+"\\"+d[1]+"\\"+d[0]+".domain");
				if (domainfile.exists()) {
					try {
						FileReader dfstream = new FileReader(domainfile);
						int c;
						String data = "";
						while ((c = dfstream.read())!=-1) {
							data = data + (char)c;
						}
						dfstream.close();
						if (data.equals(hwaddress)) {
							domainfile.delete();
							System.out.println("[INFO] [DNS] Domain "+domain+" successfully removed");
							return "OK";
						} else {
							System.out.println("[WARN] [DNS] Access error to "+domain+" domain file");
							return "4";
						}
					} catch (IOException e) {
						//e.printStackTrace();
						System.out.println("[ERROR] [SYS] Input/Output error");
						return "0";
					}
				} else {
					System.out.println("[WARN] [DNS] Domain "+domain+" not already created");
					return "2";
				}
	    	} else {
	    		System.out.println("[WARN] [DNS] Incorrect domain name");
	    		return "1";
	    	}
		} else {
			System.out.println("[WARN] [DNS] Incorrect domain name");
			return "1";
		}
    }
    
    public static String Resolve(String domain) {
    	String[] d = domain.split("\\.");
    	if (d.length == 2) {
    		boolean flag = false;
    		for (int i = 0; i < Map.domains.length; i++) {
    			if (d[1].equals(Map.domains[i])) {
    				flag = true;
    				break;
    			} else {
    				flag = false;
    			}
    		}
	    	if (flag) {
				File domainfile = new File(dnsfolder+"\\"+d[1]+"\\"+d[0]+".domain");
				if (domainfile.exists()) {
					try {
						FileReader dfstream = new FileReader(domainfile);
						int c;
						String data = "";
						while ((c = dfstream.read())!=-1) {
							data = data + (char)c;
						}
						dfstream.close();
						for (int i = 0; i < Map.MapSize; i++) {
							if (Map.IPMap[i][1].equals(data)) {
								System.out.println("[INFO] [DNS] DNS request successfully processed");
								return Map.IPMap[i][0];
							}
						}
						System.out.println("[INFO] [DNS] DNS request not processed! Reason: Host is down");
						return "5";
					} catch (IOException e) {
						//e.printStackTrace();
						System.out.println("[ERROR] [SYS] Input/Output error");
						return "0";
					}
				} else {
					System.out.println("[WARN] [DNS] Domain not already created");
					return "2";
				}
	    	} else {
	    		System.out.println("[WARN] [DNS] Incorrect domain name");
	    		return "1";
	    	}
    	} else {
    		System.out.println("[WARN] [DNS] Incorrect domain name");
    		return "1";
    	}
    }
}

/*
Error codes:
0 - Unknown error
1 - Incorrect domain name
2 - Domain already created/Domain not already created
3 - Error create domain file
4 - Access error to domain file
5 - Host is down
 */

class TCPServer {
	public static final String PATH_TO_PROPERTIES = new File(".").getAbsolutePath()+"\\config.properties";
	static String dnsfolder = new File(".").getAbsolutePath()+"\\dns";
	@SuppressWarnings("resource")
	public static void main(String argv[]) throws Exception 

	{
		System.out.println("DHCP/DNS Server for OpenComputers and other devices by Bit");
		System.out.println("Contscts:");
		System.out.println("-- ");
		System.out.print("[INFO] Initializating...");
        Properties prop = new Properties();
        String IP = "0.0.0.0";
        int port = 9999;
        int maxconnections = 200;
        String[] domainlist = new String[] {"ru", "net", "server"};
        try {
        	File F = new File(PATH_TO_PROPERTIES);
	        if (F.exists()) {
	        	System.out.println("");
	        	System.out.println("|-- [PROP] config.properties found!");
	        	FileInputStream fileInputStream = new FileInputStream(F);
	            prop.load(fileInputStream);
	            
	            IP = prop.getProperty("IP");
	            System.out.println("|-- [PROP] Set server IP to "+IP);
	            port = Integer.parseInt(prop.getProperty("port"));
	            System.out.println("|-- [PROP] Set server port to "+port);
	            maxconnections = Integer.parseInt(prop.getProperty("maxconnections"));
	            System.out.println("|-- [PROP] Set server maxconnections to "+maxconnections);
	            domainlist = prop.getProperty("domains").split(",");
	            for (int i = 0; i < domainlist.length; i++) {
	            	domainlist[i].replace(" ", "");
	            }
	            fileInputStream.close();
	            prop = null;
        	} else {
        		System.out.println("");
        		System.out.println("|-- [PROP] config.properties not found!");
        		System.out.println("|-- [PROP] Create config.properties and load default settings!");
        		FileWriter FW = new FileWriter(F);
        		FW.write("IP = "+IP+"\n");
        		FW.write("port = "+port+"\n");
        		FW.write("maxconnections = "+maxconnections+"\n");
        		String str = "";
        		for (int i = 0; i < domainlist.length; i++) {
        			if (i < (domainlist.length - 1)) {
        				str = str + domainlist[i] + ",";
        			} else {
        				str = str + domainlist[i];
        			}
        		}
        		FW.write("domains = "+str);
        		FW.close();
        	}
        } catch (IOException e) {
            //e.printStackTrace();
        }
		Data Map = new Data(domainlist);
		int n = 0, j = 0;
		for (int i = 0; i < Map.MapSize; i++) {
			Map.IPMap[i][0] = n + "." + j;
			Map.IPMap[i][1] = "#";
			j++;
			if (j == 256) {
				j = 0;
				n++;
			}
		}
		System.out.println("|-- [PROP] OK");
		System.out.println("[INFO] [DNS] Checking DNS folder...");
        File dir;
        dir = new File(dnsfolder);
        if (!dir.exists()) {
        	System.out.println("|-- [DNS] DNS folder not found!");
        	System.out.print("|-- [DNS] Create DNS folders...");
        	dir.mkdir();
        	System.out.println("OK");
        }
        for (int i = 0; i < Map.domains.length; i++) {
	        dir = new File(dnsfolder+"\\"+Map.domains[i]);
	        if (!dir.exists()) {
	        	dir.mkdir();
	        }
	        System.out.println("|-- [DNS] [Domain '."+Map.domains[i]+"' active]");
        }
        System.out.println("|-- [DNS] OK");
		System.out.print("[INFO] Starting socket...");
		ServerSocket welcomeSocket = null;
		try {
			welcomeSocket = new ServerSocket(port, maxconnections, InetAddress.getByName(IP));
		} catch (SocketException c) {
			//c.printStackTrace();
			System.out.println("");
			System.out.print("[FATAL ERROR] [SYS] Address already in use!");
			System.exit(0);
		}
		System.out.println("OK");
		System.out.println("[INFO] Socket started on external IP: "+welcomeSocket.getInetAddress());
		System.out.println("[INFO] Wait connections...");
		Socket connectionSocket = welcomeSocket.accept();
		while (true) {
			if (connectionSocket.isConnected()) {
				System.out.println("[INFO] New connection on "+connectionSocket.getRemoteSocketAddress());
				new Client(connectionSocket, Map).start();
				connectionSocket = welcomeSocket.accept();
			}
		}
	}
}
