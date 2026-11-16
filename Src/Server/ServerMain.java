package Server;

import java.util.concurrent.Executors;
import java.io.FileInputStream;
import java.util.concurrent.ExecutorService;
import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.net.InetSocketAddress;

public class ServerMain {
    private int port;
    private String matchesFile;
    private long gameDurations;

    private ExecutorService threadPool;
    
    //memoria del server:
    private userManager gestoreUtenti;
    
    private GameManager gestorePartite; 
    private NotificatoreUDP notificatore;

    //costruttore
    public ServerMain(){
        //configurazione
        readConfig();
        this.threadPool=Executors.newCachedThreadPool();
        this.gestoreUtenti=new userManager();
        try{
            this.notificatore=new NotificatoreUDP();
        }catch(IOException ex){
            System.err.println("errore creazione notificatore UDP"+ex.getMessage());
        }
        this.gestorePartite=new GameManager(this.matchesFile,this.gameDurations, this.notificatore, this.gestoreUtenti);
    }

    public void readConfig(){
        Properties prop=new Properties();
        try(InputStream input= new FileInputStream("config/server.properties.txt")){ 
            prop.load(input); //prende i byte e capisce che sono scritti con chiave=valore, e riempie prop con tutte le coppie nel file 
            
            String valorePorta=prop.getProperty("server_port"); 
            this.port=Integer.parseInt(valorePorta);

            this.matchesFile=prop.getProperty("matches_file");

            String valoreDurata= prop.getProperty("durata_partita");
            //perche avevo letto testo ma mi serve un numero
            this.gameDurations=Long.parseLong(valoreDurata);

            System.out.println("Server configurato su porta: "+this.port);
        }catch(IOException ex){
            System.out.println("Errore");
            this.port=9999;//default
        }
    }
    
    public void start(){
            //creo canale per accettare connessioni
            try(ServerSocketChannel serverChannel=ServerSocketChannel.open()){ 
                
                //assegno la porta al canale
                serverChannel.bind(new InetSocketAddress(this.port)); 
                
                //manteniamo il canale bloccante, accept sospende il thread finché non arriva per davvero una connessione
                serverChannel.configureBlocking(true);
                System.out.println("Server in ascolto sulla porta: "+this.port);
                
                //tengo in vita il server
                while (true){
                    // il server si blocca qui finché un client non si collega, quando si collega nasce clientChannel
                    SocketChannel clientChannel=serverChannel.accept(); 
                    System.out.println("Client connesso: "+clientChannel.getRemoteAddress()); 
                    
                    //runnable
                    ClientHandler handler=new ClientHandler(clientChannel, gestoreUtenti, gestorePartite, notificatore);
                    //assegno un thread del pool all'handler e parte il run.
                    threadPool.execute(handler); 
                }
            }catch(IOException ex){
                System.err.println("Errore:"+ex.getMessage());
            }
        }
    
    public static void main(String[]args){
        //creo oggetto, parte il costruttore-> parte readConfig()
        ServerMain server=new ServerMain();
        server.start();
    }
    
}
