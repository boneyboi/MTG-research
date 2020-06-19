/**
 * Description
 * @author Michael Bowling
 * @author Shaelyn Rivers
 * @author Deric Siglin
 * @since 17 June 2020
 */

package forge.game.research.decision.strategy.template;

import forge.game.card.Card;

public class TemplateCMC extends CardTemplate {

    int cmc;

    public TemplateCMC(int cmc) {
        this.cmc = cmc;
    }

    @Override
    public boolean matches(Card card){
        return (card.getCMC() == cmc);
    }
}
