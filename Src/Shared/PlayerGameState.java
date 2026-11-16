package Shared;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

//gestiste lo stato di un giocatore in una singola partita

public class PlayerGameState {
    private int error; 
    private int score;
    private boolean win;
    private List<GameGroup> groupGuess; //lista dinamica

    public PlayerGameState(){
        this.error=0;
        this.score=0;
        this.win=false;
        groupGuess=new CopyOnWriteArrayList<>();// lista dei gruppi che questo specifico utente ha già indovinato
    }

    //synchronized perche viene usato da thread diversi, per registrare risposta corrett/lo scheduler a fine partuta calcola le statistiche, qualche clientHandler potrebbe voler chiedere le statistiche di questo giocatore
    //syncronized mi permette di usare un metodo alla volta su ogni utente 
    public synchronized void registraRispostaCorretta(GameGroup gruppo){
        this.groupGuess.add(gruppo);
        this.score+=6;
        if(this.groupGuess.size()==3){
            this.win=true;
        }
    }

    public synchronized void registraRispostaErrata(){
        this.error+=1;
        this.score-=4;
    }

    //metodi get
    public synchronized int getError(){
        return error;
    }
    
    public synchronized int getScore(){
        return score;
    }
    
    public synchronized boolean getWin(){
        return win;
    }

    public List<GameGroup>getGroupGuess(){
        return groupGuess;
    }

}
