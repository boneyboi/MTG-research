    package forge.game.replacement;

    import forge.game.ability.AbilityKey;
    import forge.game.card.Card;

    import java.util.Map;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class ReplaceGameLoss extends ReplacementEffect {

    /**
     * Instantiates a new replace gain life.
     *
     * @param map the map
     * @param host the host
     */
    public ReplaceGameLoss(Map<String, String> map, Card host, boolean intrinsic) {
        super(map, host, intrinsic);
    }

    /* (non-Javadoc)
     * @see forge.card.replacement.ReplacementEffect#canReplace(java.util.HashMap)
     */
    @Override
    public boolean canReplace(Map<AbilityKey, Object> runParams) {
        if (hasParam("ValidPlayer")) {
            if (!matchesValid(runParams.get(AbilityKey.Affected), getParam("ValidPlayer").split(","), this.getHostCard())) {
                return false;
            }
        }

        return true;
    }

}
