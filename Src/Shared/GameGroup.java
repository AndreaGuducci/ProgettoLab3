package Shared;

/*tema non appartiene ad una classe intera, ma ad un singolo gruppo.
Definisce un singolo gruppo tematico composto da un tema e 4 parole*/

public class GameGroup {
    private String theme;
    private String [] words; 

    public GameGroup(String tema, String []words){
        this.theme=tema;
        this.words=words;
    }

    //getter per sapere quali parole appartengono ad un gruppo o quale sia il tema
    public String getTema(){
        return theme;
    }

    public String[] getWords(){
        return words;
    }

}
