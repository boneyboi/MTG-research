/**
 * Class that holds a sorted list of cards on the opponents field
 * Sorted by card value, least to greatest
 * @author Michael Bowlin
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since July 07, 2020
 */
package forge.game.research.decision.infosupport.removal;

import forge.game.card.Card;
import forge.game.research.card.Front;

import java.util.ArrayList;

import static java.lang.Double.POSITIVE_INFINITY;

public class RemovalList {

    private ArrayList<Card> removalList = new ArrayList<Card>();
    private Front f = new Front();

    public RemovalList() {
    }

    /**
     * Sorts the incoming battlefield list into a list that is organized from minimum value to max value
     * @param list - list of cards on the opponent's battlefield
     * @return removalList
     */
    public ArrayList sortList(ArrayList<Card> list) {
        int count = 0;

        //battlefield list is not empty/there is something on your opponent's side of the field
        if (list.size() > 0) {

            //sorting algorithm
            do {
                //assures that we initially always have a min
                double minVal = POSITIVE_INFINITY;
                Card cardToAdd = null;

                for (Card card : list) {
                    //iterates and find a card with the least value, which is added first
                    //makes sure duplicates cannot be added
                    if (f.chooser(card) <= minVal && !removalList.contains(card)) {
                        cardToAdd = card;
                        minVal = f.chooser(card);
                    }
                }
                //may remove this clause since 0 is protected against
                if (cardToAdd != null) {
                    removalList.add(cardToAdd);
                }
                count++;

            //as many times as there are cards
            } while (count < list.size());
        }

        return removalList;
    }

    /**
     * Allows for removalList to be obtained outside of class
     * @return removalList
     */
    public ArrayList getList() {
        return removalList;
    }

    /**
     * Method that obtains the highest value card or "threat" on the opponent's battlefield
     * @return the card that has the highest value on opponent's board
     */
    public Card getBiggestThreat(ArrayList<Card> cards) {
        sortList(cards);
        Card bigThreat = null;

        //makes sure list is not empty
        if (removalList.size() > 0) {
            bigThreat = removalList.get(removalList.size() - 1);
        }

        return bigThreat;
    }
}
