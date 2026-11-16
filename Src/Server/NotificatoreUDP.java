package Server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

public class NotificatoreUDP {
    private final ConcurrentHashMap <String,InetSocketAddress> indirizziClient;
    private final DatagramSocket socket;
    
    //puo fallire se il SO non riesce a dare socket o porta libera
    public NotificatoreUDP() throws IOException{
        this.indirizziClient=new ConcurrentHashMap<>();
        this.socket= new DatagramSocket();
    }

    //quando faccio login
    public void registraClient(String username, InetSocketAddress indirizzo){
        indirizziClient.put(username,indirizzo);
    }

    //quando faccio logout o quando quando mi disconnetto senza logout (eseguito da finally in run)
    public void rimuoviClient(String username){
        indirizziClient.remove(username);
    }

    //Invio messaggi asincroni
    public void inviaAUno(String username, String messaggioJson){
      
        //assegno ad indirizzo il valore della chiave username
        InetSocketAddress indirizzo=indirizziClient.get(username);
        
        //l'utente non ha registrato un indirizzo UDP
        if (indirizzo==null)return; 
       
        byte[] dati=messaggioJson.getBytes(StandardCharsets.UTF_8);
       
        try{
            DatagramPacket pacchetto=new DatagramPacket(dati, dati.length, indirizzo);
           // pacchetto parte dalla socket e va verso ip e porta scritti nel pacchetto
            socket.send(pacchetto);
        }catch(IOException ex){
            System.err.println("Errore invio UDP a "+username+": "+ex.getMessage());
        }

    }
}
