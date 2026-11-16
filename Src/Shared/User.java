package Shared;

//Rappresenta il giocatore con le statistiche

public class User {
    //un utente può registrarsi indicando un nome utente e password (NON HO FATTO: sse il nome utente non è già registrato)
    private String username;
    private String password;
    //per ogni partita a cui partecipa un giocatore ottiene il seguente punteggio
    private int totalScore;
    
    private int currentStreak;//Il numero di vittorie consecutive. I puzzle devono essere risolti prima che scada il tempo per essere inclusi nella serie attuale.
    private int maxStreak;//La serie più lunga di vittorie consecutive che hai mai ottenuto
    private int perfectPuzzles; //Il numero di puzzle che hai risolto senza errori.
    private int gamesPlayed; //numero di partite giocate 
    private int gamesWon; //numero di partite vinte 
    private int gamesLost;//numero di partite perse
   
    private int[] istogrammaErrori; //indici: 0,1,2,3 -> vinte con quell errore

    public User(String username, String password){
        this.username=username;
        this.password=password;
        this.totalScore=0;
        this.gamesLost=0;
        this.gamesPlayed=0;
        this.gamesWon=0;
        this.currentStreak=0;
        this.maxStreak=0;
        this.perfectPuzzles=0;
        this.istogrammaErrori= new int[6];//0,1,2,3,4,non completata
    }
    
    public synchronized void updateUsername(String username){
        this.username=username;
    }

    public synchronized void updatePassword(String password){
        this.password=password;
    }

    //metodi get
    public synchronized String getUsername(){
        return username;
    }

    public synchronized String getPassword(){
        return password;
    }

    public synchronized int getTotalScore(){
        return totalScore;
    }

    //vittorie consecutive
    public synchronized int getCurrentStreak(){
        return currentStreak;
    } 

    //streak più elevato di sempre
    public synchronized int getMaxStreak(){
        return maxStreak;
    }

    //puzzle risolti con 0 errori
    public synchronized int getPerfectPuzzles(){
        return perfectPuzzles;
    }

    public synchronized int getGamesPlayed(){
        return gamesPlayed;
    }

    public synchronized int getGamesWon(){
        return gamesWon;
    }

    public synchronized int getGamesLost(){
        return gamesLost;
    }

    public synchronized void incrementaPartiteGiocate(){
        this.gamesPlayed++;
    }

    public synchronized void incrementaPartiteVinte(){
        this.gamesWon++;
    }

    public synchronized void incrementaPartitePerse(){
        this.gamesLost++;
    }

    public synchronized void aggiungiPunteggio(int punti){
        this.totalScore+=punti;
    }

    public synchronized void incrementaPuzzlePerfetti(){
        this.perfectPuzzles++;
    }

    public synchronized void incrementaStreak(){
        this.currentStreak++;
        if(this.currentStreak>this.maxStreak){
            this.maxStreak=this.currentStreak;
        }
    }

    public synchronized void resetStreak(){
        this.currentStreak=0;
    }

    // indice=0 partita vinta con 0 errori, indice=1 partite vinte con un errore,..[fino a 3], indice=4 partita persa, indice=5 partita non conclusa
    public synchronized void incrementaIstorgramma(int indice){
        this.istogrammaErrori[indice]++;
    }

    //istogramma degli errori, da 0 a 4
    public synchronized int[] getIstogrammaErrori(){
        return istogrammaErrori;
    }
    
}
