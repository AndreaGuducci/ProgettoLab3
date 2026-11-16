package Server;


import Shared.GameGroup;
public class RisultatoProposta {
    
    public enum TipoEsito{
        CORRETTA,
        ERRATA,
        MALFORMATA
    }

    private TipoEsito esito;
    private GameGroup gruppo;

    public RisultatoProposta(TipoEsito esito, GameGroup gruppo){
        this.esito=esito;
        this.gruppo=gruppo;
    }

    public TipoEsito getEsito(){
        return esito;
    }

    public GameGroup getGruppo(){
        return gruppo;
    }

   
}


