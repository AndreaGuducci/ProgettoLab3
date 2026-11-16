package Server;
//POJO
public class Richiesta {
    
    private String operation;
    private String username;
    private String psw;
    private String oldUsername;
    private String oldPsw;
    private String newUsername;
    private String newPsw;
    private String[] words;
    private Integer gameId;
    private String playerName;
    private Integer topPlayers;

    private Integer udpPort;

    public String getOperation(){
        return operation;
    }
    public String getUsername(){
        return username;
    }
    public String getPsw(){
        return psw;
    }
    public String getOldUsername(){
        return oldUsername;
    }
    public String getOldPsw(){
        return oldPsw;
    }
    public String getNewUsername(){
        return newUsername;
    }
    public String getNewPsw(){
        return newPsw;
    }
    public String[] getWords(){
        return words;
    }
    public Integer getGameId(){
        return gameId;
    }
    public String getPlayerName(){
        return playerName;
    }
    public Integer getTopPlayers(){
        return topPlayers;
    }

    public Integer getUdpPort(){
        return udpPort;
    }
}
