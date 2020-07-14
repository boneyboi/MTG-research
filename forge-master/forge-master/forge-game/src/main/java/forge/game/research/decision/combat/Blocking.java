package forge.game.research.decision.combat;

import forge.game.card.Card;

import java.util.ArrayList;
import java.util.Map;

public class Blocking {

    public void Blocking(){

    }

    public Map<Card, ArrayList<Card>> getBlocks(ArrayList<Card> attackers, ArrayList<Card> blockers) {
        return getBlocksThird(getBlocksSecond(getBlocksFirst(attackers, blockers)));
    }

    public Map<Card, ArrayList<Card>> getBlocksFirst(ArrayList<Card> attackers, ArrayList<Card> blockers) {

    }

    public Map<Card, ArrayList<Card>> getBlocksSecond(Map<Card, ArrayList<Card>> list) {

    }

    public Map<Card, ArrayList<Card>> getBlocksThird(Map<Card, ArrayList<Card>> list) {

    }
}
