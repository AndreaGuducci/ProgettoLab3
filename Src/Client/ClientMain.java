package Client;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Properties;
import java.util.Scanner;
import java.util.HashMap;
import java.util.Map;
import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;
import java.net.InetSocketAddress; 
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class ClientMain {

    //il valore arriva da client.properties.txt
    private int serverPort;
    private String serverAddress;
    private Gson gson=new Gson();
    
    //per ricevere notifiche
    private DatagramSocket udpSocket;
    
    //buffer in cui si accumulano i byte letti dal canale TCP
    private final ByteBuffer buffer=ByteBuffer.allocate(1024);
     
    //costruttore
    public ClientMain(){
        //auto-configurazione
        readConfig();
    }

    public void readConfig(){
        Properties prop=new Properties();
        try(InputStream input=new FileInputStream("config/client.properties.txt")){
            prop.load(input); //fa la decodifica da se, senza bisogno di InputStreamReader
            //la macchina
            serverAddress=prop.getProperty("server_address");
            //la porta
            String server_Port=prop.getProperty("server_port");
            this.serverPort=Integer.parseInt(server_Port);
            }
        catch(Exception ex){
            System.err.println("Errore");
            this.serverPort=9999;
            this.serverAddress="localhost";
        }
    }

    private class RicevitoreUDP implements Runnable{

        public void run(){
            byte[] bufferRicezione=new byte[4096];
            while(true){
                try{
                    DatagramPacket pacchetto=new DatagramPacket(bufferRicezione, bufferRicezione.length);
                    //si blocca in attesa. Quando arrivano receive mette i byte in bufferRicezione
                    udpSocket.receive(pacchetto);
                    //converto i byte in UTF_8, getLength mi permette di prendere solo ciò che è arrivato e non tutto il pcchetto
                    String messaggio=new String(pacchetto.getData(),0,pacchetto.getLength(), StandardCharsets.UTF_8);
                    System.out.println("\n [Notifica Server]: "+messaggio);
                }catch(IOException ex){
                    break;
                }
            }
        }
    }

    //creo thread con un ricevitoreUDP come compito, imposta come setDaemon e lo fa partire
    private void avviaRicevitoreUDP(){
        Thread ricevitore=new Thread(new RicevitoreUDP());
        ricevitore.setDaemon(true); //altrimenti questo thread terrebbe vivo il programma anche dopo aver chiuso il client dal menu
        //parte il thread che esegue il run di runnable
        ricevitore.start();
    }
    
    //per TCP
    private void inviaMessaggio(SocketChannel channel, String message)throws IOException{
        String messaggioConLinea=message+"\n";

        //incapsulo byte del messaggio in bufferInvio
        ByteBuffer bufferInvio= ByteBuffer.wrap(messaggioConLinea.getBytes(StandardCharsets.UTF_8)); 
        while(bufferInvio.hasRemaining()){
            //scrive sul canale di comunicazione col server finché il buffer non è vuoto
            channel.write(bufferInvio); 
        }
    }

    private String riceviMessaggio(SocketChannel channel) throws IOException{
       while(true){
        //cerchiamo'\n' tra i byte già scritti fin'ora, da 0 a buffer.position()
        int indiceFineMessaggio=-1;
        for(int i=0; i<buffer.position();i++){
            if(buffer.get(i)=='\n'){
                indiceFineMessaggio=i;
                break;
            }
        }
        if(indiceFineMessaggio!=-1){
            byte[] datiMessaggio= new byte[indiceFineMessaggio];
            buffer.flip();
            buffer.get(datiMessaggio);
            buffer.get();
            buffer.compact();

            return new String(datiMessaggio,StandardCharsets.UTF_8).trim();

        }

        
        int letti=channel.read(buffer);
        //chiusura connessione
        if(letti==-1){
            throw new IOException("connessione chiusa lato server");
        }
       }
      
    }

    public void start(){
        Scanner scanner=new Scanner(System.in);
        
        //provo ad aprire socketChannel per TCP
        try(SocketChannel socketChannel=SocketChannel.open(new InetSocketAddress(this.serverAddress, this.serverPort))){
            System.out.println("connesso al server utilizzando NIO");
           
            //il sistema assegna una porta libera
            this.udpSocket=new DatagramSocket(0);
           //un thrad si mette ad aspettare pacchetti su quel socket
            avviaRicevitoreUDP();
           
            while(true){
                System.out.println("1)Registrati");
                System.out.println("2)Login");
                System.out.println("3)Logout");
                System.out.println("4)Esci");
                System.out.println("5)Aggiorna Credenziali");
                System.out.println("6) Invia Proposta");
                System.out.println("7)Informazioni Partita");
                System.out.println("8)Statistiche Partita");
                System.out.println("9)Classifica");
                System.out.println("10) Statistiche Personali");

                String scelta=scanner.nextLine();//per catturare input
                
                if (scelta.equals("4")){
                    System.out.println("Uscita...");
                    break;//esco dal while
                }
                
                Map <String, Object> dati= new HashMap<>(); 

                switch (scelta){
                    case "1":
                        System.out.println("Inserisci username: ");
                        dati.put("username", scanner.nextLine());
                        
                        System.out.println("inserisci password: ");
                        dati.put("psw", scanner.nextLine());
                        
                        dati.put("operation","register");

                        inviaMessaggio(socketChannel, gson.toJson(dati));
                        System.out.println("Risposta dal server: "+ riceviMessaggio(socketChannel));
                        break;

                    case "2":
                        System.out.println("Inserisci username: ");
                        dati.put("username", scanner.nextLine());
                        
                        System.out.println("inserisci password: ");
                        dati.put("psw", scanner.nextLine());
                        
                        dati.put("operation","login");
                        
                        //comunivca porta al server 
                        dati.put("udpPort", udpSocket.getLocalPort());
                       
                        inviaMessaggio(socketChannel, gson.toJson(dati));
                        System.out.println("Risposta dal server: " + riceviMessaggio(socketChannel));
                        break;
                    
                    case "3":
                        dati.put("operation", "logout");
                        
                        inviaMessaggio(socketChannel, gson.toJson(dati));
                        System.out.println("Server: "+ riceviMessaggio(socketChannel));
                        break;
                    
                    case "5":
                        System.out.println("Inserisci username attuale: ");
                        dati.put("oldUsername", scanner.nextLine());
                        
                        System.out.print("Inserisci password attuale: ");
                        dati.put("oldPsw", scanner.nextLine());
                       
                        System.out.print("Inserisci nuovo username (lascia vuoto se non lo vuoi cambiare)");
                        dati.put("newUsername", scanner.nextLine());

                        System.out.print("Inserisci Nuova password (lascia inalterata se si vuole modificare solo username): ");                        
                        dati.put("newPsw", scanner.nextLine()); 
                        
                        dati.put("operation", "updateCredentials"); 
                        
                        inviaMessaggio(socketChannel, gson.toJson(dati));
                        System.out.println("Server: " + riceviMessaggio(socketChannel));
                        break;

                        case "6":
                        String[] parole=new String[4];

                        //array di parole creato dinamicamente con gli inserimenti dell'utente
                        for(int i=0;i<4;i++){
                            System.out.println("Parola "+(i+1)+":");
                            parole[i]=scanner.nextLine().trim().toUpperCase();
                        }

                        dati.put("words", parole);
                        dati.put("operation","submitProposal");
                        inviaMessaggio(socketChannel, gson.toJson(dati));
                        System.out.println("Risposta dal server: "+riceviMessaggio(socketChannel));
                        break;
                
                    case "7":
                        System.out.println("Id partita (vuoto per partita corrente): ");
                        //id della partita
                        String gameIdInput=scanner.nextLine().trim();
                       
                        if(!gameIdInput.isEmpty()){
                            dati.put("gameId", Integer.parseInt(gameIdInput));
                        }
                        //se non entro in if significa che il campo gameID sarà vuoto
                        
                        dati.put("operation","requestGameInfo");
                        inviaMessaggio(socketChannel, gson.toJson(dati));
                        System.out.println("Risposta dal server: "+riceviMessaggio(socketChannel));
                        break;
                    
                    case "8":
                        System.out.println("Id partita (vuoto per paritta corrente): ");
                        //id partita
                        String gameIdInput2=scanner.nextLine().trim();
                        
                        if(!gameIdInput2.isEmpty()){
                            dati.put("gameId",Integer.parseInt(gameIdInput2));
                        }
                        dati.put("operation","requestGameStats");
                        inviaMessaggio(socketChannel, gson.toJson(dati));
                        System.out.println("Risposta dal server: "+riceviMessaggio(socketChannel));
                        break;

                    case "9":
                        System.out.println("Cerca un giocatore specifico (lascia vuoto per classifica generale): ");
                        String nomeGiocatore=scanner.nextLine().trim();
                        if(!nomeGiocatore.isEmpty()){
                            dati.put("playerName",nomeGiocatore);
                        }
                        else{
                            System.out.println("numero di giocatori che vuoi vedere (vuoto per tutti):");
                            String topInput=scanner.nextLine().trim();
                            if(!topInput.isEmpty()){
                                dati.put("topPlayers",Integer.parseInt(topInput));
                            }
                        }

                        dati.put("operation","requestLeaderboard");
                        inviaMessaggio(socketChannel, gson.toJson(dati));
                        System.out.println("Risposta dal server: "+riceviMessaggio(socketChannel));
                        break;

                    case "10":
                        dati.put("operation","requestPlayerStats");
                        inviaMessaggio(socketChannel, gson.toJson(dati));
                        System.out.println("Risposta dal server: "+riceviMessaggio(socketChannel));
                        break;

                    default:
                        System.out.println("Operazione non valida");
                        break;
                    }
            }
        }catch(IOException ex){
                System.err.println("Errore: "+ex.getMessage());
            }
        }
    

    public static void main(String[] args){
        ClientMain client=new ClientMain();
        client.start();

    }
}
