package Server;
/* il client handler è l'intermediario tra la logica di gioco e l'interfasccia utente
ogni client che si connette è unico, il Clienthandler deve tenere traccia di SocketChannel (canale aperto per parlare con client), 
Username per sapere chi è l'utente attualmente loggato e lp statp, ossia se è in attesa, sta giocando o è loggato.*/

import java.nio.channels.SocketChannel;
import com.google.gson.Gson;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import Shared.Game;
import Shared.GameGroup;
import Shared.PlayerGameState;
import Shared.User;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.net.InetSocketAddress;
import java.net.InetAddress;

public class ClientHandler implements Runnable{
    private final SocketChannel clientChannel;
    private final Gson gson;
    private String username; 
    private boolean isLogged = false; 
    private final userManager gestoreUtenti;
    private final GameManager gestorePartite;
    private final NotificatoreUDP notificatore;

    //costruttore
    public ClientHandler (SocketChannel clientChannel, userManager gestUtenti, GameManager gestPartite, NotificatoreUDP notificatore){
        this.clientChannel=clientChannel;
        this.gson=new Gson();
        this.gestoreUtenti=gestUtenti;
        this.gestorePartite=gestPartite;
        this.notificatore=notificatore;
    }

//run aspetta che il client mandi la richiesta, la legge, elabora, manda risposta e torna al punto iniziale.
    public void run(){
        ByteBuffer buffer= ByteBuffer.allocate(1024); 
        try{
            while (true){
                int letti=clientChannel.read(buffer);
                //se il clinet si disconnette
                if(letti==-1)break;
                
                //limit=position, position=0
                buffer.flip();

                while(true){
                    int indiceFineMessaggio=-1; 
                    //cerco la fine del messaggio
                    for(int i=buffer.position();i<buffer.limit();i++){
                        if(buffer.get(i)=='\n'){
                            //indica la fine del singolo messaggio
                            indiceFineMessaggio=i;
                            break;
                        }
                    }
                    //se non la trovo non è arrivato tutto il messaggio, faccio compact e mi rimetto in attesa
                    if(indiceFineMessaggio==-1){
                        break;
                    }

                    int lunghezzaMessaggio=indiceFineMessaggio-buffer.position();
                    
                    //perché new String accetta array di Byte e non ByteBuffer
                    byte[] datiMessaggio=new byte[lunghezzaMessaggio];
                    
                    //copio i byte del messaggio in byte[] e avanza position
                    buffer.get(datiMessaggio); 
                    //consumo anche '\n'
                    buffer.get(); 

                    //jsonRicevuto è il messaggio che è stato inviato sulla socketChannel dal client, decodificati in UTF8
                    String jsonRicevuto=new String(datiMessaggio, StandardCharsets.UTF_8);
                    String risposta;
                    try{
                        
                        //mappa i campi JSON sui campi dell’oggetto Richiesta tramite reflection.
                        Richiesta richiesta=gson.fromJson(jsonRicevuto,Richiesta.class);
                        if(richiesta==null ||richiesta.getOperation()==null){
                            risposta=gson.toJson(new Risposta("error","richiesta non valida"));
                        }
                        else{
                            risposta=processaOperazione(richiesta); 
                        }
                    }catch(RuntimeException ex){
                        System.err.println("richiesta non valida:"+ex.getMessage());
                        risposta=gson.toJson(new Risposta("error","richiesta non valida"));
                    }
                    //getByte converte in array di byte la risposta
                    ByteBuffer bufferScrittura= ByteBuffer.wrap((risposta+"\n").getBytes(StandardCharsets.UTF_8));

                    while(bufferScrittura.hasRemaining()){
                        clientChannel.write(bufferScrittura);
                    }
                }

                //sposto eventuali byte non consumati all'inizio
                buffer.compact();
            }
        }catch(IOException ex){
            System.err.println("errore di comunicazione: "+ex.getMessage());
        }
        finally{
            if(isLogged){
                gestoreUtenti.logout(this.username);
                notificatore.rimuoviClient(this.username);
            }
            try{
                clientChannel.close();
            }catch(IOException ex){
                System.err.println("errore in chiusura canale: "+ex.getMessage());
            }
        }
    }

    private String processaOperazione(Richiesta richiesta){
       String operazione=richiesta.getOperation();
        switch(operazione){

            case "login":
                String usernameLogin=richiesta.getUsername();
                String pswLogin=richiesta.getPsw();
                
                if (usernameLogin==null||usernameLogin.trim().isEmpty()||pswLogin==null||pswLogin.isEmpty()) {
                    return gson.toJson(new Risposta("error","username e password obbligatori"));
                }

                if(gestoreUtenti.login(usernameLogin,pswLogin)){
                    
                    if(gestoreUtenti.provaLogin(usernameLogin)){
                    //se l'utente si è registrato e non era al momento loggato:
                        isLogged=true;
                        this.username=usernameLogin;
                        if(richiesta.getUdpPort()!=null){
                            InetAddress ip=clientChannel.socket().getInetAddress();
                            //registrazione per ricevere notifiche UDP
                            notificatore.registraClient(usernameLogin, new InetSocketAddress(ip, richiesta.getUdpPort()));
                        }

                        //effettuando il login si accede automaticamente alla partita in corso, ricevendone informazioni necessarie
                        return gson.toJson(costruisciRipostaLogin());
                    }
                    else{
                        //utente utente momentaneamente già loggato
                        return gson.toJson(new Risposta("error", "utente già connesso"));
                    }
                }   
                else{
                        return gson.toJson(new Risposta("error","credenziali errate"));
                }
                
                

            case "register":
               if(richiesta.getUsername()==null || richiesta.getUsername().trim().isEmpty()|| richiesta.getPsw()==null || richiesta.getPsw().isEmpty()){
                return gson.toJson(new Risposta("error","username e password obbligatori"));
               } 
               if(gestoreUtenti.register(richiesta.getUsername(),richiesta.getPsw())){
                return gson.toJson(new Risposta("ok","Registrazione ricevuta"));
               }
               return gson.toJson(new Risposta("error","Registrazione fallita"));
                 
               
           
            case "logout":
                if(!isLogged){
                    return gson.toJson(new Risposta("error", "utente non loggato"));
                }
                gestoreUtenti.logout(this.username);
                notificatore.rimuoviClient(this.username);
                this.username=null;
                this.isLogged=false;
                return gson.toJson(new Risposta("ok", "logout effettuato"));

            case "updateCredentials":
              
                //aggiornaCredenziali(String userAttuale,String passwordAttuale, String nuovoUser, String nuovaPassword)
                
                int modifica= gestoreUtenti.aggiornaCredenziali(richiesta.getOldUsername(), richiesta.getOldPsw(),richiesta.getNewUsername(), richiesta.getNewPsw());
                
                if (modifica==0){
                    if(richiesta.getOldUsername().equals(this.username))
                        this.username=richiesta.getNewUsername();
                    return gson.toJson(new Risposta("ok","modifica effettuata con successo"));
                }

                else if (modifica==1){
                    return gson.toJson(new Risposta("error","La vecchia password non corrisponde"));

                }

                else if (modifica==2){
                    return gson.toJson(new Risposta("error","Username già occupato"));
                }

                else return gson.toJson(new Risposta("error","Errore"));

            case "submitProposal":
                return gestisciSubmitProposal(richiesta);
            
            case "requestGameInfo":
                return gestisciRequestGameInfo(richiesta);
            case "requestGameStats":
                return gestisciRequestStats(richiesta);
            case "requestLeaderboard":
                return gestisciRequestLeaderboard(richiesta);
            case "requestPlayerStats":
                return gestisciRequestPlayerStats(richiesta);
            default:
                return gson.toJson(new Risposta("error","operazione sconosciuta"));

        }
    }

    private GameGroup[] listaGruppiToArray (List<GameGroup> lista){
        GameGroup[] risultato=new GameGroup[lista.size()];
        for(int i=0; i<lista.size();i++ ){
            risultato[i]=lista.get(i);
        }
        return risultato;
    }

    private Risposta costruisciRipostaLogin(){
        Risposta risposta=new Risposta ("ok","login effettuato");
        Game partita=gestorePartite.getPartitaInCorso();
        if(partita==null){
            return risposta;
        }
    
        partita.getStatoGiocatori().putIfAbsent(this.username, new PlayerGameState());
        PlayerGameState stato=partita.getStatoGiocatori().get(this.username);

        risposta.setGameId(partita.getGameID());
        risposta.setParole(partita.getParole());
        risposta.setGruppiTrovati(listaGruppiToArray(stato.getGroupGuess()));
        risposta.setErrori(stato.getError());
        risposta.setPunteggio(stato.getScore());
        risposta.setTempoRimanente(gestorePartite.getTempoRimanente(partita));

        return risposta;
    }

    //gestisce quando un giocatore invia una quadrupla di parole come tentativo. Recupera/crea lo stato del giocatore della partita corrente
    private String gestisciSubmitProposal(Richiesta richiesta){ 
        if(!isLogged){
            return gson.toJson(new Risposta("error", "devi effettuare il login."));
        }

        Game partitaAttuale=gestorePartite.getPartitaInCorso();

        if(partitaAttuale==null){
            return gson.toJson(new Risposta("error", "nessuna partita attualmente attiva"));
        }

        if(!partitaAttuale.isActive()){
            return gson.toJson(new Risposta("error", "partita già conclusa"));
        }

        //inserisco la coppia username-playerGameState in statoGiocatori se non c'è ancora
        partitaAttuale.getStatoGiocatori().putIfAbsent(this.username, new PlayerGameState());

        //stato è il playerGameState di quel giocatore
        PlayerGameState stato= partitaAttuale.getStatoGiocatori().get(this.username);

        if(stato.getWin()|| stato.getError()>=4){
            return gson.toJson(new Risposta( "error", "hai già concluso questa partita"));
        }    

        //Risultato= CORRETTA/ERRATA/MALFORMATA. Prende paroleProposte, gruppiReali e gruppiGiaTrovati
        RisultatoProposta risultato=ProposalEvaluator.valuta(richiesta.getWords(), partitaAttuale.getGruppi(),stato.getGroupGuess());

        boolean appenaVinta=false;
        boolean appenaPersa=false;

        if(risultato.getEsito()==RisultatoProposta.TipoEsito.CORRETTA){
            stato.registraRispostaCorretta(risultato.getGruppo());
            appenaVinta=stato.getWin();
        }
        else if( risultato.getEsito()==RisultatoProposta.TipoEsito.ERRATA){
            stato.registraRispostaErrata();
            appenaPersa= (stato.getError()==4);
        }

        else if( risultato.getEsito()==RisultatoProposta.TipoEsito.MALFORMATA){
            return gson.toJson(new Risposta("error","proposta malformata"));
        }
        
         if (appenaVinta || appenaPersa){
            gestoreUtenti.registraFinePartita(this.username, appenaVinta, stato.getScore(),stato.getError());
        }

        Risposta risposta= new Risposta("ok","Proposta Valutata"); 
        
        risposta.setEsito(risultato.getEsito().toString()); //getEsito() restituisce un enum, tramite toString lo converto in Stringa 
        risposta.setErrori(stato.getError());
        risposta.setPunteggio(stato.getScore());
        risposta.setVinta(stato.getWin());
        
        if(risultato.getEsito()==RisultatoProposta.TipoEsito.CORRETTA){
            //dico al CLient quale gruppo ha indovinato
            risposta.setTema(risultato.getGruppo().getTema());

        }

        return gson.toJson(risposta);
    }

    private String gestisciRequestGameInfo(Richiesta richiesta){
        if(!isLogged){
            return gson.toJson(new Risposta("error","Devi effettuare login"));
        }
        Game partita;

        //verifico se gameID è presente e lo assegno a partita.
        if(richiesta.getGameId()==null){
            partita=gestorePartite.getPartitaInCorso();
        }
        else{
            partita=gestorePartite.getPartitaById(richiesta.getGameId());
        }

        if(partita==null) return gson.toJson(new Risposta("error", "partita inesistente"));
        
        //recupero lo stato di quel giocatore in partita
        PlayerGameState stato=partita.getStatoGiocatori().get(this.username);
       
        if(stato==null)stato=new PlayerGameState();

        Risposta risposta=new Risposta("ok","Informazioni partita");

        if(partita.isActive()){
            risposta.setPartitaConclusa(false);
            risposta.setTempoRimanente(gestorePartite.getTempoRimanente(partita));
            risposta.setErrori(stato.getError());
            risposta.setPunteggio(stato.getScore());
            risposta.setProposteCorrette(stato.getGroupGuess().size());
            risposta.setParoleRimanenti(paroleAncoraDaTrovare(partita,stato));
            risposta.setGruppiTrovati(listaGruppiToArray(stato.getGroupGuess()));
        }
        else{
            risposta.setPartitaConclusa(true);
            risposta.setGruppiReali(partita.getGruppi());
            risposta.setProposteCorrette(stato.getGroupGuess().size());
            risposta.setErrori(stato.getError());
            risposta.setPunteggio(stato.getScore());
        }

        return gson.toJson(risposta);
    }

    private String[] paroleAncoraDaTrovare(Game partita, PlayerGameState stato){
      //devo verificare una per una se le parole sono gia state trovate, e questo si fa velocemente con .contains su HashSet 
        Set<String> paroleTrovate=new HashSet<>(); 
        //scorro tutti i gruppi trovati dal giocatore che ha fatto richiesta
        for(GameGroup g : stato.getGroupGuess()){
           //scorro tutte le parole dei gruppi trovati
            for(String parola: g.getWords()){
                paroleTrovate.add(parola);
            }
        }

        //devo costruire collezione di parole, ma non so quante ce ne saranno. un ArrayList cresce dinamicamente, inoltre è ordinato
        List <String> rimanenti= new ArrayList<>(); 
        //scorro tutte le parole della partita attuale
        for(String parola:partita.getParole()){
            //se queste la parola non è presente in paroleTrovate significa che è rimanente
            if(!paroleTrovate.contains(parola)){
                rimanenti.add(parola);
            }
        }

        String[] risultato=new String[rimanenti.size()];
       
        for(int i=0;i<rimanenti.size();i++){
            risultato[i]=rimanenti.get(i);
        }

        return risultato;
    }

    //se è in corso voglio sapere quanti giocatori stanno giocando, quanti hanno gia concluso, quanti hanno vinto e il tempo rimanente
    //se è finita voglio sapere quanti hanno partecipato, quanti hanno concluso, quanti hanno vinto ed il punteggio medio
    private String gestisciRequestStats(Richiesta richiesta){
        if(!isLogged){
              return gson.toJson(new Risposta("error","Devi effettuare login"));
        }

        Game partita;

        if(richiesta.getGameId()==null){
            partita=gestorePartite.getPartitaInCorso();
        }
        else{
            partita=gestorePartite.getPartitaById(richiesta.getGameId());
        }

        if(partita==null) return gson.toJson(new Risposta("error", "partita inesistente"));

        int partecipanti=0;
        int conclusi=0;
        int vincitori=0;
        int punteggioTotale=0;

        //scorro tutti gli stati dei giocatori in partita
        for(PlayerGameState stato: partita.getStatoGiocatori().values()){
            partecipanti++;
            punteggioTotale+=stato.getScore();
            boolean  haConcluso=stato.getWin()||stato.getError()>=4;
            if(haConcluso)conclusi++;
            if(stato.getWin()) vincitori++;
        }
            Risposta risposta=new Risposta("ok", "statistiche partita");

            if(partita.isActive()){
                risposta.setPartitaConclusa(false);
                risposta.setTempoRimanente(gestorePartite.getTempoRimanente(partita));
                risposta.setNumGiocatoriInCorso(partecipanti-conclusi);
                risposta.setNumGiocatoriConclusi(conclusi);
                risposta.setNumGiocatoriVincitori(vincitori);
            }
            else{
                risposta.setPartitaConclusa(true);
                risposta.setNumPartecipanti(partecipanti);
                risposta.setNumGiocatoriConclusi(conclusi);
                risposta.setNumGiocatoriVincitori(vincitori);
                double media;
                if(partecipanti==0)media=0.0;
                else media=(double) punteggioTotale/partecipanti;
                risposta.setPunteggioMedio(media);
            }

        
     return gson.toJson(risposta);
    }

    //si può vedere classifica completa, classifica dei primi k giocatori, posizione di un giocatore
    private String gestisciRequestLeaderboard(Richiesta richiesta){
         if(!isLogged){
              return gson.toJson(new Risposta("error","Devi effettuare login"));
        }

        List<User> classificaCompleta=gestoreUtenti.getClassificaCompleta(); 
        
        //caso in cui cerco username, punteggio, posizione di uno specifico giocatore
        if(richiesta.getPlayerName()!=null){
            String nomeCercato=richiesta.getPlayerName();
            
            //array di 1 posizione perche ho un solo giocatore da restituire, e lo faccio array per avere nei 3 casi il solito tipo di risposta da restituire.
            VoceClassifica[] risultato=new VoceClassifica[1];
            boolean trovato=false;

            for(int i=0; i<classificaCompleta.size(); i++){
                User u=classificaCompleta.get(i);

                if(u.getUsername().equals(nomeCercato)){
                    risultato[0]=new VoceClassifica(u.getUsername(), u.getTotalScore(), i+1);
                    trovato=true;
                    break;
                }
            }
            if(!trovato){
                return gson.toJson(new Risposta ("error", "giocatore inesistente"));
            }

            Risposta risposta=new Risposta ("ok", "posizione in classifica");
            
            //inserisco il giocatore trovato in risposta
            risposta.setClassifica(risultato);

            return gson.toJson(risposta);
        }

        //caso in cui restituisco username,punteggio,posizione dei primi k giocatori 
        int quantiGiocatoriRestituire;

        //giocatori richiesti=null oppure <=0
        if(richiesta.getTopPlayers()==null||richiesta.getTopPlayers()<=0){
            quantiGiocatoriRestituire=classificaCompleta.size();
        }
        else{
            quantiGiocatoriRestituire=Math.min(richiesta.getTopPlayers(), classificaCompleta.size());
        }

        VoceClassifica[] classificaRisposta=new VoceClassifica[quantiGiocatoriRestituire];

        for(int i=0; i<quantiGiocatoriRestituire; i++){
            User u=classificaCompleta.get(i);
            classificaRisposta[i]=new VoceClassifica(u.getUsername(),u.getTotalScore(), i+1);
        }

        Risposta risposta= new Risposta ("ok", "Classifica");
        
        //inserisco i giocatori trovati in risposta
        risposta.setClassifica(classificaRisposta);
        return gson.toJson(risposta);
    }

    private String gestisciRequestPlayerStats(Richiesta richiesta){
        if(!isLogged){
            return gson.toJson(new Risposta("error", "devi effettuare il login"));
        }

        User utente=gestoreUtenti.getUser(this.username);
        if (utente==null){
            return gson.toJson(new Risposta("error", "utente non trovato"));
        }

        //partite giocate da un utente
        int giocate=utente.getGamesPlayed();
        double winRate;
        double lossRate;

        //calcolo le percentuali di vittoria e perdita
        if(giocate==0){
            winRate=0.0;
            lossRate=0.0;
        }

        else{
            winRate=(double) (utente.getGamesWon()/giocate)*100.0;
            lossRate=(double) (utente.getGamesLost()/giocate)*100.0;
        }

        Risposta risposta= new Risposta ("ok", "statistiche personali");
        risposta.setPuzzleCompletati(giocate);
        risposta.setWinRate(winRate);
        risposta.setLossRate(lossRate);
        risposta.setStreakCorrente(utente.getCurrentStreak());
        risposta.setStreakMassima(utente.getMaxStreak());
        risposta.setPuzzlePerfetti(utente.getPerfectPuzzles());
        risposta.setIstogrammaErrori(utente.getIstogrammaErrori());

        return gson.toJson(risposta);


    }
}