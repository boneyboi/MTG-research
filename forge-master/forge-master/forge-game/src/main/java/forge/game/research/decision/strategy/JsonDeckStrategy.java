package forge.game.research.decision.strategy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import forge.game.research.DoublyLinkedList;
import forge.game.research.decision.strategy.template.*;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Function;

public class JsonDeckStrategy {

    private ArrayList<String> jsoncardtemplates;
    private String jsonname;
    private DeckStrategy deckStrategy;
    private HashMap<String, CardTemplate> templateconversion = new HashMap<String, CardTemplate>();
    private String cardname;
    private int permCMC;
    private int nonpermCMC;


    public JsonDeckStrategy(){
        //template converesion converts the card template string in json to the actual card template
        Function<String, CardTemplate> newtemplatename = (s1)->new TemplateName("");
        Function<String, CardTemplate> newtemplatepermcmc = (s1)->new TemplatePermanentCMC(permCMC);
        templateconversion.put("TemplateName", newtemplatename.apply(""));
        templateconversion.put("TemplatePermanentCMC", newtemplatepermcmc.apply(""));
        templateconversion.put("TemplateNonPermanentCMC", new TemplateNonPermanentCMC(nonpermCMC));
        templateconversion.put("TemplateLifeBuff", new TemplateLifeBuff());
        templateconversion.put("TemplateLifelink", new TemplateLifelink());
        templateconversion.put("TemplateSurveil", new TemplateSurveil());
        templateconversion.put("TemplateSurveilBoost", new TemplateSurveilBoost());
        templateconversion.put("TemplateRemoval", new TemplateRemoval());

    }

    /**
     * Converts the json file for a deck strategy into the deck strategy object and returns it for use
     * in other parts of the program
     * @return
     */
    public DeckStrategy createDeckStrategy(String path) throws IOException {
        Gson gson = new Gson();
        JsonReader gsonreader = gson.newJsonReader(new FileReader(path));
        DeckStrategy deckStrategyResult = new DeckStrategy();
        int layer = 0;
        int stratindex = -1; //start at -1 so this first strat found is indexed as 0
        int cardindex = 0;
        CardTemplate cardtemplate = null;
        while(gsonreader.hasNext()){
            JsonToken data = gsonreader.peek();
            if(data.equals(JsonToken.BEGIN_ARRAY)){
                System.out.println();
                gsonreader.beginArray();
            }
            else if (data.equals(JsonToken.BEGIN_OBJECT)){
                System.out.println();
                gsonreader.beginObject();
                if(layer==1){
                    String name = gsonreader.nextName();
                    deckStrategyResult.addStrategy(name);
                    stratindex++;
                    System.out.println(name);
                }
                layer++;
            }
            else if(data.equals(JsonToken.NAME)){
                System.out.println(data);
                String name = gsonreader.nextName();
                if(layer==2){stratindex++; deckStrategyResult.addStrategy(name);}
                if(layer==3){
                    //Strategy tempstrat = deckStrategyResult.getStrategies().get(stratindex);
                    /*tempstrat.pushFront(new StrategyNode());
                    tempstrat.reset();
                    tempstrat.pushCard(cardindex, templateconversion.get(name));
                    tempstrat.reset();*/
                    deckStrategyResult.getStrategies().get(stratindex).reset();
                    deckStrategyResult.getStrategies().get(stratindex).pushFront(new StrategyNode());
                    deckStrategyResult.getStrategies().get(stratindex).reset();
                    deckStrategyResult.getStrategies().get(stratindex).pushCard(cardindex,
                            templateconversion.get(name));
                    deckStrategyResult.getStrategies().get(stratindex).reset();
                    //if(cardindex>0){deckStrategyResult.addNode(stratindex, new StrategyNode());}
                    //deckStrategyResult.addNode(stratindex, new StrategyNode());
                    //if(cardindex==0){deckStrategyResult.getStrategies().get(stratindex).next();}
                    /*if(templateconversion.containsKey(name)){
                        deckStrategyResult.addTemplateCard(stratindex, templateconversion.get(name));
                    }*/
                    if (name.equals("TemplateName")){
                        TemplateName tempname =
                        (TemplateName)
                        deckStrategyResult.getStrategies().get(stratindex).next().cards.iterator().next();
                        tempname.setName(gsonreader.nextString());
                        //this.cardname = gsonreader.nextString();
                    } else if (name.equals("TemplateNonPermanentCMC")){
                        TemplateNonPermanentCMC tempnonperm =
                        (TemplateNonPermanentCMC)
                        deckStrategyResult.getStrategies().get(stratindex).next().cards.iterator().next();
                        tempnonperm.setCMC(gsonreader.nextInt());
                        //this.nonpermCMC = gsonreader.nextInt();
                    } else if (name.equals("TemplatePermanentCMC")){
                        //System.out.println(deckStrategyResult.getStrategies().get(stratindex).next());
                        //Strategy tempstrat = deckStrategyResult.getStrategies().get(0);
                        //tempstrat.reset();
                        //DoublyLinkedList<CardTemplate> templist = tempstrat.next().getCards();
                        //System.out.println(templist.peek_front());
                        //DoublyLinkedList<CardTemplate> ctemp = deckStrategyResult.getStrategies().get(stratindex).get(cardindex).getCards();
                        //int jkjkj = cardindex + 5;
                        TemplatePermanentCMC tempperm =
                        (TemplatePermanentCMC)
                        deckStrategyResult.getStrategies().get(stratindex).get(cardindex).nextCard();
                        tempperm.setCMC(gsonreader.nextInt());
                        //this.permCMC = gsonreader.nextInt();
                    }
                    cardindex++;
                }
            }
            else if(data.equals(JsonToken.STRING)){
                System.out.println(data);
                gsonreader.skipValue();
                //deckStrategyResult.addTemplateCard(stratindex, new TemplateName(gsonreader.nextString()));
                //cardindex++;
            }
            else if(data.equals(JsonToken.NUMBER)){
                System.out.println(data);
                gsonreader.skipValue();
                //cardindex++;
            }
            else if(data.equals(JsonToken.END_OBJECT)){
                System.out.println();
                gsonreader.endObject();
                cardindex=0;
                layer--;
            }
            else if(data.equals(JsonToken.END_ARRAY)){
                System.out.println();
                gsonreader.endArray();
            }
            else if(data.equals(JsonToken.END_DOCUMENT)){
                gsonreader.skipValue();
            }

            System.out.println();

        }
        gsonreader.close();
        return deckStrategyResult;
    }
}
