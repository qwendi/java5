import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        if (args.length > 1) {
            if (args[0].equals("server")) {
                new Server(Integer.parseInt(args[1]));
            } else if (args[0].equals("client")) {
                new Client(args[1], Integer.parseInt(args[2]));
            }
        }
    }
}
class ServerConnectionThread extends Thread {
    boolean connected = true;
    Socket socket;
    Server server;
    ServerConnectionThread(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }
    public void run() {
        System.out.println("Ожидание сообщений(" + this + ")");
        while (this.server.alive) {
            try {
                BufferedReader dis = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String msg = dis.readLine();
                if (msg == null) {
                    server.clients.remove(this);
                    this.connected = false;
                    this.server.onClientChange(this);
                    return;
                } else {
                    server.onClientMessage(this, msg);
                }
            } catch (IOException e) {
                server.clients.remove(this);
                this.connected = false;
                this.server.onClientChange(this);
                return;
            }
        }
    }
    public String toString() {
        return "Клиент: " + this.socket.getInetAddress().toString() + " порт:" +this.socket.getPort() + " подключен:" + this.connected;
    }
    public void sendMessage(String message) {
        try {
            PrintStream ps = new PrintStream(this.socket.getOutputStream());
            ps.println(message);
            ps.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
class ServerConnectionInitThread extends Thread {
    Server server;
    int port;
    ServerConnectionInitThread(int port, Server server) {
        this.server = server;
        this.port = port;
    }
    public void run() {
        try {
            ServerSocket ss = new ServerSocket(port);
            while (this.server.alive) {
                Socket socket = ss.accept();
                ServerConnectionThread sst = new ServerConnectionThread(socket, this.server);
                this.server.clients.add(sst);
                this.server.onClientChange(sst);
                sst.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
class Server {
    public boolean alive = true;
    public ArrayList<ServerConnectionThread> clients = new ArrayList<ServerConnectionThread>();
    Server(int port) {
        System.out.println("Запуск сервера: " + port);
        new ServerConnectionInitThread(port, this).start();
    }
    public void onClientChange(ServerConnectionThread clientThread) {
        System.out.println("onClientChange: " + clientThread);
        System.out.println("active client list:");
        String clientsList = "";
        for (int i=0; i<clients.size(); i++) {
            clientsList += i+". "+clients.get(i) + "\r\n";
        }
        for (ServerConnectionThread c: clients) {
            try {
                PrintStream ps = new PrintStream(c.socket.getOutputStream());
                c.sendMessage("Clients count="+clients.size());
                c.sendMessage(clientsList);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.print(clientsList);
    }
    public void onClientMessage(ServerConnectionThread clientThread, String message) {
        System.out.println("Cообщение от (" + clientThread + "): " + message);
        if (message.startsWith("send:")) {
            int msgPos = message.indexOf(":",5);
            int clientIndex = Integer.parseInt(message.substring(5, msgPos));
            clients.get(clientIndex).sendMessage(message.substring(msgPos+1));
        }
    }
}
class ClientConnectionThread extends Thread {
    Client client;
    Socket socket;
    ClientConnectionThread(Client client) {
        this.client = client;
    }
    public void run() {
        try {
            System.out.println("Подключение к серверу "+this.client.server_address+" порт:"+this.client.server_port);
            socket = new Socket(this.client.server_address, this.client.server_port);
            BufferedReader dis = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            while (this.client.alive) {
                String msg = dis.readLine();
                if (msg == null) return;
                System.out.println(msg);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class Client {
    public boolean alive = true;
    public String server_address;
    public int server_port;
    Client(String server_address, int server_port) {
        this.server_address = server_address;
        this.server_port = server_port;
        ClientConnectionThread cct = new ClientConnectionThread(this);
        cct.start();
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String cmd = scanner.nextLine();
            try {
                PrintStream ps = new PrintStream(cct.socket.getOutputStream());
                ps.println(cmd);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
    }
}