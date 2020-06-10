package forge.game.player;

import java.util.Collections;
import java.util.Comparator;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.research.CardEvaluator;
import forge.game.research.CreatureEval;
import forge.game.research.Front;
import forge.game.research.ZoneEvaluator;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;
import forge.util.collect.FCollection;

public class PlayerCollection extends FCollection<Player> {

    private static final long serialVersionUID = -4374566955977201748L;

    public PlayerCollection() {
    }
    
    public PlayerCollection(Iterable<Player> players) {
        this.addAll(players); 
    }

    // card collection functions
    public final CardCollection getCardsIn(ZoneType zone) {
        CardCollection result = new CardCollection();
        for (Player p : this) {
            result.addAll(p.getCardsIn(zone));
            //ZoneEvaluator evaluator = new ZoneEvaluator(ZoneType.Hand, p);
            for(Card card : p.getCardsIn(ZoneType.Hand)){
                //ignores cards that are not creatures -- research
                Front frontC = new Front(card);
                frontC.chooser();
            }
        }
        return result;
    }
    
    public final CardCollection getCreaturesInPlay() {
        CardCollection result = new CardCollection();
        for (Player p : this) {
            result.addAll(p.getCreaturesInPlay());
        }
        return result;
    }
    
    // filter functions with predicate
    public PlayerCollection filter(Predicate<Player> pred) {
        return new PlayerCollection(Iterables.filter(this, pred));
    }
    
    // sort functions with Comparator
    public Player min(Comparator<Player> comp) {
        return Collections.min(this, comp);
    }
    public Player max(Comparator<Player> comp) {
        return Collections.max(this, comp);
    }
    
    // value functions with Function
    public Integer min(Function<Player, Integer> func) {
        return Aggregates.min(this, func);
    }
    public Integer max(Function<Player, Integer> func) {
        return Aggregates.max(this, func);
    }
    public Integer sum(Function<Player, Integer> func) {
        return Aggregates.sum(this, func);
    }
}
