package Server;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import Shared.Game;
import Shared.PlayerGameState;
import java.util.Map;
import java.util.concurrent.TimeUnit;

//Questa classe si occuperà di leggere il file una sola volta e fornire le partite al resto del server.

public class GameManager {

    private ConcurrentHashMap<Integer,Game> partite; //contiene partite (corrente o passate)
    private final long durataPartita;
    
    //tramite .schedule() programmo la fine 
    private final ScheduledExecutorService scheduler;
    private Game partitaCorrente;

    private final NotificatoreUDP notificatore;
    private final userManager gestoreUtenti;
    private final Gson gson=new Gson();
    private JsonReader reader;

    private class TaskFinePartita implements Runnable{
        public void run(){
            finePartita();
        }
    }

    //costruttore 
    public GameManager(String path, long durataPartita, NotificatoreUDP notificatore, userManager gestoreUtenti){
        this.partite= new ConcurrentHashMap<>();
        this.durataPartita=durataPartita;
        //Avvia e termina partite
        this.scheduler= Executors.newSingleThreadScheduledExecutor();
        this.notificatore=notificatore;
        this.gestoreUtenti=gestoreUtenti;
        apriFilePartite(path);
        avviaProssimaPartita();
    }

    private void apriFilePartite(String path){
        try{
            reader=new JsonReader(new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8));
            
            //legge dal file "[" e sposta il puntatore alla posizione subito dopo
            reader.beginArray();
        }catch(IOException ex){
            ex.printStackTrace();
        }
    }

    private Game leggiProssimaPartita(){
        try{
            if(reader!=null && reader.hasNext()){
                //leggo dal file la prossima partita, la converto in game e la restituisco (reader avanza)
                return gson.fromJson(reader,Game.class);
            }
        }catch(IOException ex){
            ex.printStackTrace();
        }
        return null; //non ci sono piu partite nel file
    }

    public synchronized Game getPartitaInCorso(){
        return partitaCorrente;
    }

    private synchronized void avviaProssimaPartita(){
        
        Game prossima= leggiProssimaPartita();
        
        if(prossima!=null){
            prossima.iniziaPartita();
            partitaCorrente=prossima;
            //inserisco chiave valore in partite
            partite.put(prossima.getGameID(),prossima); 
            
            //scorro le chiavi degli utenti loggati
            for(String username:gestoreUtenti.getUtentiLoggati()){
                //nella mappa dei giocatori della nuova partita se non c'è già username lo aggiungo con stato vuoto
                prossima.getStatoGiocatori().putIfAbsent(username, new PlayerGameState());
                
                Risposta notifica=new Risposta ("ok","nuova partita iniziata");
                
                notifica.setGameId(prossima.getGameID());
                notifica.setParole(prossima.getParole());
                notifica.setTempoRimanente(getTempoRimanente(prossima));
                notificatore.inviaAUno(username, gson.toJson(notifica));
            }
            scheduler.schedule(new TaskFinePartita(), durataPartita,TimeUnit.MILLISECONDS);
        }
        else{
            //file delle partite finito, fermiamo lo scheduler
            System.out.println("Partite terminate, nessuna nuova partita da avviare");
            //quando le partite sono finite con ci sono task da scartare
            scheduler.shutdown();
        }
    }

    private synchronized void finePartita(){
        Game partitaTerminata=partitaCorrente;
        partitaCorrente.terminaPartita();
        notificaFinePartita(partitaTerminata);
        avviaProssimaPartita();
    }

    public synchronized Game getPartitaById(int id){
        return partite.get(id);
    }
    
    public long getTempoRimanente(Game g){
        return g.getTempoRimanente(this.durataPartita);
    }

    private void notificaFinePartita(Game partita){
        //stati è solo un puntatore alla concurrentHashMap statoGiocatori, usata solo dal thread scheduler
        Map<String,PlayerGameState> stati=partita.getStatoGiocatori();
       
        int partecipanti=0;
        int conclusi=0;
        int vincitori=0;
        int punteggioTotale=0;
        
        //scorro le chiavi(username), calcolo statistiche partita e registro partite non concluse
        for(String username:stati.keySet()){
            
            PlayerGameState stato= stati.get(username);

            partecipanti++;
            punteggioTotale+=stato.getScore();
            
            boolean haCocluso=stato.getWin() ||stato.getError()>=4;
            
            if(haCocluso)conclusi++;

            if(!haCocluso){
                gestoreUtenti.registraPartitaNonConclusa(username, stato.getScore());
            }

            if(stato.getWin()){
                vincitori++;
            }
        }

        double punteggioMedio;
        if(partecipanti==0){
            punteggioMedio=0.0;
        }
        else{
            punteggioMedio=(double)punteggioTotale/partecipanti;
        }

        //per ciascun giocatore creo una notifica con le proprie statistiche ed invia la notifica a tutti
        for(String username:stati.keySet()){
            
            PlayerGameState stato=stati.get(username);
            
            Risposta notifica=new Risposta("ok","partita conclusa");

            notifica.setPartitaConclusa(true);
            notifica.setGruppiReali(partita.getGruppi());
            notifica.setProposteCorrette(stato.getGroupGuess().size());
            notifica.setErrori(stato.getError());
            notifica.setPunteggio(stato.getScore());
            notifica.setNumPartecipanti(partecipanti);
            notifica.setNumGiocatoriConclusi(conclusi);
            notifica.setNumGiocatoriVincitori(vincitori);
            notifica.setPunteggioMedio(punteggioMedio);

            notificatore.inviaAUno(username, gson.toJson(notifica));
        }
    }
}
