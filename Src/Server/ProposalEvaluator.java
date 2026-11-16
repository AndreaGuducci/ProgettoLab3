package Server;
//verifica la proposta
import Shared.GameGroup;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;

public class ProposalEvaluator{
   
    public static RisultatoProposta valuta(String[] paroleProposte, GameGroup[] gruppiReali, List<GameGroup>gruppiGiaTrovati){
        
        //CONTROLLO 1:verifico se ci sono 4 parole
        if(paroleProposte.length!=4){ 
            return new RisultatoProposta(RisultatoProposta.TipoEsito.MALFORMATA,null); //null poiché una risposta malformata non ha un GameGroup associato.
        }

        //CONTROLLO 2: verifico se ci sono  parole uguali
        //hashset non ha duplicati
        Set <String> paroleSenzaDuplicati= new HashSet<>(Arrays.asList(paroleProposte)); 
        if (paroleSenzaDuplicati.size()!=4){
             return new  RisultatoProposta(RisultatoProposta.TipoEsito.MALFORMATA, null);
        }

        //CONTROLLO 3: verifico se le parole proposte sono tra le 16 parole totali
        Set <String> paroleValide= new HashSet<>();
        for(GameGroup g : gruppiReali){
            for(String parola: g.getWords()){
                paroleValide.add(parola);
            }
        }
        
        //se le parole valide non contengono tutte le parole proposte -> MALFORMATA 
        if (!paroleValide.containsAll(Arrays.asList(paroleProposte))){
            return new RisultatoProposta(RisultatoProposta.TipoEsito.MALFORMATA, null);
        }

        //CONTROLLO 4: verifico che nessuna delle parole proposte appartenga a un gruppo che il giocatore ha già trovato
        Set<String> paroleGiaTrovate= new HashSet<>();
        for(GameGroup g : gruppiGiaTrovati){
            for(String parola:g.getWords()){
                paroleGiaTrovate.add(parola);
            }
        }

        for(String parola: paroleProposte){
            if(paroleGiaTrovate.contains(parola)){
                return new RisultatoProposta(RisultatoProposta.TipoEsito.MALFORMATA, null);
            }
        }
        
        //CONTROLLO 5: verifico che le parole proposte (senza duplicati) siano esattamente quelle del gruppo reale
        for(GameGroup g: gruppiReali){
            Set <String> paroleDelGruppo=new HashSet<>(Arrays.asList(g.getWords()));
            if(paroleDelGruppo.equals(paroleSenzaDuplicati)){
                return new RisultatoProposta(RisultatoProposta.TipoEsito.CORRETTA, g);
            }
        }
        return new RisultatoProposta(RisultatoProposta.TipoEsito.ERRATA, null);

    }
}