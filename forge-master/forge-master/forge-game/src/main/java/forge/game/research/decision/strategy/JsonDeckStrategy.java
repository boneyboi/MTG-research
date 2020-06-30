package forge.game.research.decision.strategy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import forge.game.research.decision.strategy.template.*;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

public class JsonDeckStrategy {

    private ArrayList<String> jsoncardtemplates;
    private String jsonname;
    private DeckStrategy deckStrategy;





    public JsonDeckStrategy(){

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
                    System.out.println(name);
                }
                layer++;
            }
            else if(data.equals(JsonToken.NAME)){
                System.out.println(data);
                String name = gsonreader.nextName();
                if(layer==1){stratindex++;}
                if(layer==2){
                    if (name.equals("TemplateLifeBuff")){
                        cardtemplate = new TemplateLifeBuff();
                    } else if (name.equals("TemplateLifelink")){
                        cardtemplate = new TemplateLifelink();
                    } else if (name.equals("TemplateName")){
                        cardtemplate = new TemplateName(gsonreader.nextString());
                    } else if (name.equals("TemplateNonPermanentCMC")){
                        cardtemplate = new TemplateNonPermanentCMC(gsonreader.nextInt());
                    } else if (name.equals("TemplatePermanentCMC")){
                        cardtemplate = new TemplatePermanentCMC(gsonreader.nextInt());
                    } else if (name.equals("TemplateSurveil")){
                        cardtemplate = new TemplateSurveil();
                    } else if (name.equals("TemplateSurveilBoost")){
                        cardtemplate = new TemplateSurveilBoost();
                    }
                    deckStrategyResult.addTemplateCard(stratindex, cardtemplate);
                    cardindex++;
                }
            } else if(data.equals(JsonToken.STRING)){
                System.out.println(data);
                gsonreader.skipValue();
                //deckStrategyResult.addTemplateCard(stratindex, new TemplateName(gsonreader.nextString()));
                cardindex++;
            } else if(data.equals(JsonToken.NUMBER)){
                System.out.println(data);
                gsonreader.skipValue();
                cardindex++;
            } else if(data.equals(JsonToken.END_OBJECT)){
                System.out.println();
                gsonreader.endObject();
                cardindex=0;
                layer--;
            } else if(data.equals(JsonToken.END_ARRAY)){
                System.out.println();
                gsonreader.endArray();
            } else if(data.equals(JsonToken.END_DOCUMENT)){
                gsonreader.skipValue();
            }

            System.out.println();

        }
        gsonreader.close();
        //also we need a for each loop that goes through each object in the deckstrategy,
        // so we get all the different strategies
        /*for(String s : jsoncardtemplates){
            //the string should look like "card : info"
            //split it by ":"
            //then see what template the first split contains with if statements
            //then create a new template with the info with the scond part by exluding the first charachter
        }*/
        return deckStrategyResult;
    }
}
