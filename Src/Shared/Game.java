package Shared;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collections;

//rappresenta una partita globale. Contiene ID, le 16 paorle, i 4 gruppi e il tempo di inizio per calcolare la scadenza

public class Game {
    private int gameId;
    private String [] parole;

    private GameGroup [] groups; //array di 4 gruppi
    
    //per sapere quando è iniziata la partita
    private long startTime; 
    
    //per dire se la partita è stata conclusa o no. Volatile perché la JVM altrimenti può tenere in cache una copia dela valore del thread che legge, e non vedere l'aggiornamentoi fatto da un altro thread
    private volatile boolean statoPartita; 
    
    //per ogni username il suo stato nella partita specifica
    private ConcurrentHashMap<String, PlayerGameState> statoGiocatori; 
    
    public Game(int gameID){
        this.gameId=gameID;
        parole=new String[16];
        groups=new GameGroup[4];
        this.startTime=System.currentTimeMillis();
        statoPartita=true;  //partita attiva
        //una mappa statoGiocatori per game
        statoGiocatori= new ConcurrentHashMap<>(); 
    }

    //per lo statoPartita
    public boolean isActive(){
        return statoPartita;
    }

    //raccolgo, mescolo e converto in array
    public void iniziaPartita(){
        this.startTime=System.currentTimeMillis();
        this.statoPartita=true;
        
        //viene creata una nuova mappa per i giocatori
        this.statoGiocatori=new ConcurrentHashMap<>();
        
        //viene creata un arrayList contenente tutte le parole
        List<String> tutteLeParole=new ArrayList<>();
        
        //scorro i gruppi e ogni parola del gruppo, e le inserisco in tutteLeParole
        for(GameGroup g:groups){
            for(String parola:g.getWords()){
                tutteLeParole.add(parola);
            }
        }
        //mescolo
        Collections.shuffle(tutteLeParole);
        //copio gli elementi di tutteLePArole in un array di string lungo quanto la lista
        this.parole=tutteLeParole.toArray(new String[tutteLeParole.size()]);

    }

    public void terminaPartita(){
        this.statoPartita=false;
    }

    //il server deve poter leggere il gameID per trovare la partita giusta quando un cliente la richiede
    public int getGameID(){ 
        return gameId;
    }

    //il server deve poter leggere startTime per calcolare quanto tempo è rimasto
    public long getStartTime(){ 
        return startTime;
    }

    public String[] getParole(){
        return parole;
    }

    public GameGroup[] getGruppi(){
        return groups;
    }

    public ConcurrentHashMap<String,PlayerGameState> getStatoGiocatori(){
        return statoGiocatori;
    }

    public long getTempoRimanente(long durataPartita){
        long trascorso= System.currentTimeMillis()-startTime;
        long rimanente= durataPartita-trascorso;
        return Math.max(rimanente,0);
    }
}
