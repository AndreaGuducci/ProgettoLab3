package Server;

import Shared.GameGroup;
//POJO
public class Risposta {
    private String status;
    private String message;
    private String esito;
    private String tema;
    private int errori;
    private int punteggio; 
    private boolean vinta; 

    private Long tempoRimanente;
    private String[] paroleRimanenti;
    private Integer proposteCorrette;
    private GameGroup[] gruppiReali;
    private Boolean partitaConclusa;

    private Integer numGiocatoriInCorso; 
    private Integer numGiocatoriConclusi;
    private Integer numGiocatoriVincitori;
    private Integer numPartecipanti;
    private Double punteggioMedio;

    private VoceClassifica[] classifica;

    private Integer puzzleCompletati;
    private Double winRate;
    private Double lossRate;
    private Integer streakCorrente; //num vittorie consecutive
    private Integer streakMassima;
    private Integer puzzlePerfetti;
    private int[] istogrammaErrori;

    private Integer gameId;

    private String[] parole;
    private GameGroup[] gruppiTrovati;

    public Risposta(String status, String message){
        this.status=status;
        this.message=message;
    }

    public void setParole(String[] parole){
        this.parole=parole;
    }

    public void setGruppiTrovati(GameGroup[] gruppiTrovati){
        this.gruppiTrovati=gruppiTrovati;
    }

    public void setEsito(String esito){
        this.esito=esito;
    }
   
    public void setTema(String tema){
        this.tema=tema;
    }

    public void setErrori(int errori){
        this.errori=errori;
    }
   
    public void setPunteggio(int punteggio){
        this.punteggio=punteggio;
    }
   
    public void setVinta(boolean vinta){
        this.vinta=vinta;
    }

    public void setTempoRimanente(long tempoRimanente){
        this.tempoRimanente=tempoRimanente;
    }

    public void setParoleRimanenti(String[] paroleRimanenti){
        this.paroleRimanenti=paroleRimanenti;
    }

    public void setProposteCorrette(int proposteCorrette){
        this.proposteCorrette=proposteCorrette;
    }

    public void setGruppiReali(GameGroup[] gruppiReali){
        this.gruppiReali=gruppiReali;
    }
    
    public void setPartitaConclusa(boolean partitaConclusa){
        this.partitaConclusa=partitaConclusa;
    }

    public void setNumGiocatoriInCorso(int n){
        this.numGiocatoriInCorso=n;
    }

    public void setNumGiocatoriConclusi(int n){
        this.numGiocatoriConclusi=n;
    }

    public void setNumGiocatoriVincitori(int n){
        this.numGiocatoriVincitori=n;
    }

    public void setNumPartecipanti(int n){
        this.numPartecipanti=n;
    }

    public void setPunteggioMedio(double media){
        this.punteggioMedio=media;
    }

    public void setClassifica(VoceClassifica[] classifica){
        this.classifica=classifica;
    }

    public void setPuzzleCompletati(int n){
        this.puzzleCompletati=n;
    }

    public void setWinRate(double n){
        this.winRate=n;
    }
    
    public void setLossRate(double n){
        this.lossRate=n;
    }

    public void setStreakCorrente(int n){
        this.streakCorrente=n;
    }

    public void setStreakMassima(int n){
        this.streakMassima=n;
    }

    public void setPuzzlePerfetti(int n){
        this.puzzlePerfetti=n;
    }

    public void setIstogrammaErrori(int[]arr){
        this.istogrammaErrori=arr;
    }

    public void setGameId(int gameId){
        this.gameId=gameId;
    }
}