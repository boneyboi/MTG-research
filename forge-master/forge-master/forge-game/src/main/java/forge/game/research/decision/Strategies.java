package forge.game.research.decision;


import java.util.ArrayList;

public class Strategies {
    public ArrayList<Strategy> lifelinkstrats;
    public ArrayList<Strategy> monoredStrats;

    public Strategies(){
        lifelinkstrats = new ArrayList<Strategy>();
        lifelinkstrats.add(new Strategy("LifeLink"));
        lifelinkstrats.get(0).pushCard(new TemplateName("Daxos"));


        monoredStrats = new ArrayList<Strategy>();
        monoredStrats.add(new Strategy("Monored"));
        monoredStrats.get(0).pushCard(new TemplateCMC(1));


    }

}
