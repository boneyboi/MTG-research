package forge.game.research.decision;

import forge.game.card.Card;

public class TemplateName extends CardTemplate {
    String name;

    public TemplateName(String name) {
        this.name = name;
    }

    @Override
    public boolean matches(Card card){
        return card.getName().equals(name);
    }
}
