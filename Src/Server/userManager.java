package Server;
/*gestisce tutti gli utenti lato server:
- registrazione/login/logout
- aggiornamento credenziali
- statistiche di fine partita
- classifica ordinata per punteggio
- salvataggio e caricamento utenti su users.json
*/

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import Shared.User; 
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FileReader;
import java.io.File;
import java.util.HashMap;
import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;
import java.util.Set;

public class userManager {
    
    private final ConcurrentHashMap<String,User> users;
    //percorso del file sul disco dove vengono salvati e riletti gli utenti 
    private final String JSON_PATH="users.json"; 

    private final Gson gson =new GsonBuilder().setPrettyPrinting().create(); 
     //mappa condivisa tra tutti i clientHandler che vede se un utente è gia loggato
    private final ConcurrentHashMap<String,Boolean> utentiLoggati=new ConcurrentHashMap<>();
   
    public userManager(){
        this.users= new ConcurrentHashMap<>(); //appena chiudo il programma svanisce tutto, temporaneo
        File file=new File(JSON_PATH);
        if(file.exists()){ // verifico se il file esiste già su disco
            loadUsers(file);
        }
        else{
            System.out.println("File utenti non trovato, ne verrà creato uno al primo salvataggio");
        }
    }

    //registrare utente
    public boolean register(String username, String password){
        
        User user=new User(username,password);
        User precedente=users.putIfAbsent(username, user);
        if(precedente!=null){
            return false;
        }
        saveUsers();
        return true;
    }

    //due utenti non possono essere loggati due volte
    public boolean provaLogin(String username){
        return utentiLoggati.putIfAbsent(username,true)==null;
    }

    public boolean login(String username,String password){

        User u=users.get(username);
        if (u==null)
            return false; //utente non registrato
        if(u.getPassword().equals(password))
            return true;
        return false;
    }

    public void logout(String username){
        utentiLoggati.remove(username);
    }

    //creo funzione che quando si registra un utente scrive dalla ram al file
    synchronized private void saveUsers(){

        try(FileWriter writer=new FileWriter(JSON_PATH)){ 
            //prendo users completo e la riscrivo all'interno di writer in json
            gson.toJson(users,writer); 
        }
        catch(IOException ex){
            System.out.println("Errore: "+ ex.getMessage());
        }
    }

    //quando accendo il server legge il file dalla ram
    private void loadUsers(File file){  //users.json
            try(FileReader reader= new FileReader(file)){ //filereader(file) restituisce un oggetto di tipo filereader,ossia uno strumento di lettura. Reader sarà la variabile che leggerà il contenuto. Possiede il metodo read()
                Type tipoMappa= new TypeToken<HashMap<String,User>>(){}.getType();//crea oggetto che si ricorda il tipo completo che  altrimenti JVM cancellerebbe.
                HashMap <String,User> temp = gson.fromJson(reader, tipoMappa );  //gson.FromJson legge il file Reader e json sa che i valori devono essere di tipo User.
                if (temp!=null){
                    //copiamo i dati caricati nella nostra mappa principale
                    this.users.putAll(temp); //prendo tutti gli elementi della memoria temporanea e li metto nella mappa principale della classe
                    System.out.println("dati caricati con successo");
                }
            }catch(IOException ex){
                System.out.println("Errore durante il caricamento: "+ ex.getMessage());
            }
    }

    //ritorno 0 se ho successo, 1 se la vecchia password non corrisponde e 2 se l'username è già occupato
    synchronized public int aggiornaCredenziali(String userAttuale,String passwordAttuale, String nuovoUser, String nuovaPassword){
       
        //estraggo user dalla mappa
        User utenteTrovato=this.users.get(userAttuale);  
       
        //se User=null o le password non corrispondono
        if(utenteTrovato==null || !utenteTrovato.getPassword().equals(passwordAttuale)){
            return 1; 
        }

        //per capire cosa l'utente voglia cambiare:
        boolean cambiaUser= (nuovoUser!=null && !nuovoUser.trim().isEmpty() && !nuovoUser.equals(userAttuale)); //vorremmo cambiare user
        boolean cambiaPassword=(nuovaPassword!=null && !nuovaPassword.trim().isEmpty()); //vorremmo cambiare la password
        
        if(cambiaUser){
            //verifico se c'è gia un usename con quella chiave
            if(this.users.containsKey(nuovoUser)){
                return 2; //username già esistente
            }

            //modifico il nome
            utenteTrovato.updateUsername(nuovoUser); 
            
            //elimino vecchio user
            this.users.remove(userAttuale);

            //inserisco chiave= nuovoUser e valore=utenteTrovato
            this.users.put(nuovoUser,utenteTrovato);
           
        } 

        if(cambiaPassword){
            utenteTrovato.updatePassword(nuovaPassword);
        }

        saveUsers();

        return 0; //successo
    }

    public synchronized void registraFinePartita(String username, boolean vinta, int punteggio, int errori){
        User utente=users.get(username);
        if(utente==null)return;

        utente.incrementaPartiteGiocate();
        utente.aggiungiPunteggio(punteggio);

        if(vinta){
            utente.incrementaPartiteVinte();
            utente.incrementaStreak();
            utente.incrementaIstorgramma(errori);
            if(errori==0){
                utente.incrementaPuzzlePerfetti();
            }
         } 
         else{
            utente.incrementaPartitePerse();
            utente.resetStreak();
            utente.incrementaIstorgramma(4);
            }
        saveUsers();
        
    }

    //chi non termnina la partita entro il tempo ha perso
    public synchronized void registraPartitaNonConclusa(String username,int punteggio){
        User utente=users.get(username);
        if(utente==null)return;

        utente.incrementaPartiteGiocate();
        utente.aggiungiPunteggio(punteggio);
        utente.resetStreak();
        utente.incrementaIstorgramma(5);

        saveUsers();
    }

    //ordino tutti gli utenti per punteggio, dal più alto al più basso
    public List <User> getClassificaCompleta(){
       
        //creo arrayList e lo riempio con tutti i valori della mappa user 
        List<User> lista=new ArrayList<>(users.values());
       
        //bubble sort per ordinare
        for(int i=0;i<lista.size()-1;i++){
            for(int j=0; j<lista.size()-1; j++){
                User a= lista.get(j);
                User b= lista.get(j+1);
                if(a.getTotalScore()<b.getTotalScore()){
                    lista.set(j, b);
                    lista.set(j+1,a);
                }
            }
        }
        return lista;
    }

    public User getUser(String username){
        return users.get(username);
    }

    public Set<String> getUtentiLoggati(){
        //ritorno tutte le chiavi della mappa
        return utentiLoggati.keySet();
    }
}
