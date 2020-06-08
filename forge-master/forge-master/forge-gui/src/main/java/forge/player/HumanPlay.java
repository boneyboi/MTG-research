package forge.player;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import forge.FThreads;
import forge.card.mana.ManaCost;
import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.GameEntityView;
import forge.game.GameEntityViewMap;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.ability.effects.CharmEffect;
import forge.game.ability.effects.FlipCoinEffect;
import forge.game.card.*;
import forge.game.card.CardPredicates.Presets;
import forge.game.cost.*;
import forge.game.mana.ManaConversionMatrix;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.player.Player;
import forge.game.player.PlayerController;
import forge.game.player.PlayerView;
import forge.game.spellability.*;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.match.input.InputPayMana;
import forge.match.input.InputPayManaOfCostPayment;
import forge.match.input.InputSelectCardsFromList;
import forge.util.TextUtil;
import forge.util.collect.FCollectionView;
import forge.util.gui.SGuiChoose;
import forge.util.Localizer;
import forge.util.CardTranslation;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class HumanPlay {

    private HumanPlay() {
    }

    /**
     * <p>
     * playSpellAbility.
     * </p>
     *
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     */
    public final static boolean playSpellAbility(final PlayerControllerHuman controller, final Player p, SpellAbility sa) {
        FThreads.assertExecutedByEdt(false);

        Card source = sa.getHostCard();

        if (sa instanceof LandAbility) {
            sa.setActivatingPlayer(p);
            if (sa.canPlay()) {
                sa.resolve();
                p.getGame().updateLastStateForCard(source);
            }
            return false;
        }

        boolean castFaceDown = sa.isCastFaceDown();

        sa.setActivatingPlayer(p);
        boolean flippedToCast = sa instanceof Spell && source.isFaceDown();

        source.setSplitStateToPlayAbility(sa);
        sa = chooseOptionalAdditionalCosts(p, sa);
        if (sa == null) {
            return false;
        }

        // extra play check
        if (sa.isSpell() && !sa.canPlay()) {
            return false;
        }

        if (flippedToCast && !castFaceDown) {
            source.turnFaceUp(false, false);
        }

        if (sa.getApi() == ApiType.Charm && !sa.isWrapper()) {
            CharmEffect.makeChoices(sa);
        }

        sa = AbilityUtils.addSpliceEffects(sa);

        if (sa.isBestow()) {
            source.animateBestow();
        }

        // System.out.println("Playing:" + sa.getDescription() + " of " + sa.getHostCard() +  " new = " + newAbility);
        final HumanPlaySpellAbility req = new HumanPlaySpellAbility(controller, sa);
        if (!req.playAbility(true, false, false)) {
            if (flippedToCast && !castFaceDown) {
                source.turnFaceDown(true);
            }
            return false;
        }
        return true;
    }

    /**
     * choose optional additional costs. For HUMAN only
     * @param p
     *
     * @param original
     *            the original sa
     * @return an ArrayList<SpellAbility>.
     */
    static SpellAbility chooseOptionalAdditionalCosts(Player p, final SpellAbility original) {
        if (!original.isSpell()) {
            return original;
        }

        PlayerController c = p.getController();

        // choose alternative additional cost
        final List<SpellAbility> abilities = GameActionUtil.getAdditionalCostSpell(original);

        final SpellAbility choosen = c.getAbilityToPlay(original.getHostCard(), abilities);

        List<OptionalCostValue> list =  GameActionUtil.getOptionalCostValues(choosen);
        if (!list.isEmpty()) {
            list = c.chooseOptionalCosts(choosen, list);
        }

        return GameActionUtil.addOptionalCosts(choosen, list);
        //final List<SpellAbility> abilities = GameActionUtil.getOptionalCosts(original);
    }

    /**
     * <p>
     * playSpellAbilityForFree.
     * </p>
     *
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     */
    public static final void playSaWithoutPayingManaCost(final PlayerControllerHuman controller, final Game game, SpellAbility sa, boolean mayChooseNewTargets) {
        FThreads.assertExecutedByEdt(false);
        final Card source = sa.getHostCard();

        source.setSplitStateToPlayAbility(sa);

        if (!sa.isCopied()) {
            if (sa.getApi() == ApiType.Charm && !sa.isWrapper()) {
                CharmEffect.makeChoices(sa);
            }
            sa = AbilityUtils.addSpliceEffects(sa);
        }

        final HumanPlaySpellAbility req = new HumanPlaySpellAbility(controller, sa);
        req.playAbility(mayChooseNewTargets, true, false);
    }

    /**
     * <p>
     * playSpellAbility_NoStack.
     * </p>
     *
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     */
    public final static void playSpellAbilityNoStack(final PlayerControllerHuman controller, final Player player, final SpellAbility sa) {
        playSpellAbilityNoStack(controller, player, sa, false);
    }

    public final static void playSpellAbilityNoStack(final PlayerControllerHuman controller, final Player player, final SpellAbility sa, boolean useOldTargets) {
        sa.setActivatingPlayer(player);

        final HumanPlaySpellAbility req = new HumanPlaySpellAbility(controller, sa);
        req.playAbility(!useOldTargets, false, true);
    }

    // ------------------------------------------------------------------------

    private static int getAmountFromPart(CostPart part, Card source, SpellAbility sourceAbility) {
        String amountString = part.getAmount();
        return StringUtils.isNumeric(amountString) ? Integer.parseInt(amountString) : AbilityUtils.calculateAmount(source, amountString, sourceAbility);
    }

    /**
     * TODO: Write javadoc for this method.
     * @param part
     * @param source
     * @param sourceAbility
     * @return
     */
    private static int getAmountFromPartX(CostPart part, Card source, SpellAbility sourceAbility) {
        String amountString = part.getAmount();
        // Probably should just be -
        return AbilityUtils.calculateAmount(source, amountString, sourceAbility);
    }

    /**
     * <p>
     * payCostDuringAbilityResolve.
     * </p>
     *
     * @param cost
     *            a {@link forge.game.cost.Cost} object.
     * @param sourceAbility TODO
     */
    public static boolean payCostDuringAbilityResolve(final PlayerControllerHuman controller, final Player p, final Card source, final Cost cost, SpellAbility sourceAbility, String prompt) {
        // Only human player pays this way
        Card current = null; // Used in spells with RepeatEach effect to distinguish cards, Cut the Tethers
        if (sourceAbility.hasParam("ShowCurrentCard")) {
            current = Iterables.getFirst(AbilityUtils.getDefinedCards(source, sourceAbility.getParam("ShowCurrentCard"), sourceAbility), null);
        }

        final List<CostPart> parts = CostAdjustment.adjust(cost, sourceAbility).getCostParts();
        final List<CostPart> remainingParts = new ArrayList<>(parts);
        CostPart costPart = null;
        if (!parts.isEmpty()) {
            costPart = parts.get(0);
        }
        String orString = prompt == null ? sourceAbility.getStackDescription().trim() : "";
        if (!orString.isEmpty()) {
            if (sourceAbility.hasParam("UnlessSwitched")) {
                orString = TextUtil.concatWithSpace(" (" + Localizer.getInstance().getMessage("lblIfYouDo") + ":", orString, ")");
            } else {
                orString = TextUtil.concatWithSpace(" (" + Localizer.getInstance().getMessage("lblOr") + ":", orString, ")");
            }
        }

        if (parts.isEmpty() || (costPart.getAmount().equals("0") && parts.size() < 2)) {
            return p.getController().confirmPayment(costPart, Localizer.getInstance().getMessage("lblDoYouWantPay") + " {0}?" + orString, sourceAbility);
        }
        // 0 mana costs were slipping through because CostPart.getAmount returns 1
        else if (costPart instanceof CostPartMana && parts.size() < 2) {
            if (((CostPartMana) costPart).getManaToPay().isZero()) {
                return p.getController().confirmPayment(costPart, Localizer.getInstance().getMessage("lblDoYouWantPay") + " {0}?" + orString, sourceAbility);
            }
        }

        final HumanCostDecision hcd = new HumanCostDecision(controller, p, sourceAbility, source);
        boolean mandatory = cost.isMandatory();

        //the following costs do not need inputs
        for (CostPart part : parts) {
            boolean mayRemovePart = true;

            if (part instanceof CostPayLife) {
                final int amount = getAmountFromPart(part, source, sourceAbility);
                if (!p.canPayLife(amount)) {
                    return false;
                }

                if (!p.getController().confirmPayment(part, Localizer.getInstance().getMessage("lblDoYouWantPayNLife", String.valueOf(amount)) + orString, sourceAbility)) {
                    return false;
                }

                p.payLife(amount, null);
            }
            else if (part instanceof CostDraw) {
                final int amount = getAmountFromPart(part, source, sourceAbility);
                List<Player> res = new ArrayList<>();
                String type = part.getType();
                for (Player player : p.getGame().getPlayers()) {
                    if (player.isValid(type, p, source, sourceAbility) && player.canDraw()) {
                        res.add(player);
                    }
                }

                if (res.isEmpty()) {
                    return false;
                }

                String message = null;
                if (res.contains(p)) {
                    message = Localizer.getInstance().getMessage("lblDoYouWantLetThatPlayerDrawNCardOrDoAction", String.valueOf(amount), orString);
                }
                else {
                    message = Localizer.getInstance().getMessage("lblDoYouWantDrawNCardOrDoAction", String.valueOf(amount), orString);
                }

                if (!p.getController().confirmPayment(part, message, sourceAbility)) {
                    return false;
                }

                for (Player player : res) {
                    player.drawCards(amount, sourceAbility);
                }
            }
            else if (part instanceof CostGainLife) {
                PaymentDecision pd = part.accept(hcd);

                if (pd == null)
                    return false;
                else
                    part.payAsDecided(p, pd, sourceAbility);
            }
            else if (part instanceof CostAddMana) {
                String desc = part.toString();
                desc = desc.substring(0, 1).toLowerCase() + desc.substring(1);

                if (!p.getController().confirmPayment(part, Localizer.getInstance().getMessage("lblDoyouWantTo") + " " + desc + "?" + orString, sourceAbility)) {
                    return false;
                }
                PaymentDecision pd = part.accept(hcd);

                if (pd == null)
                    return false;
                else
                    part.payAsDecided(p, pd, sourceAbility);
            }
            else if (part instanceof CostMill) {
                final int amount = getAmountFromPart(part, source, sourceAbility);
                final CardCollectionView list = p.getCardsIn(ZoneType.Library);
                if (list.size() < amount) { return false; }
                if (!p.getController().confirmPayment(part, Localizer.getInstance().getMessage("lblDoYouWantMillNCardsOrDoAction", String.valueOf(amount), orString), sourceAbility)) {
                    return false;
                }
                CardCollectionView listmill = p.getCardsIn(ZoneType.Library, amount);
                ((CostMill) part).payAsDecided(p, PaymentDecision.card(listmill), sourceAbility);
            }
            else if (part instanceof CostFlipCoin) {
                final int amount = getAmountFromPart(part, source, sourceAbility);
                if (!p.getController().confirmPayment(part, Localizer.getInstance().getMessage("lblDoYouWantFlipNCoinOrDoAction", String.valueOf(amount), orString), sourceAbility)) {
                    return false;
                }
                final int n = FlipCoinEffect.getFilpMultiplier(p);
                for (int i = 0; i < amount; i++) {
                    FlipCoinEffect.flipCoinCall(p, sourceAbility, n);
                }
            }
            else if (part instanceof CostDamage) {
                // not a pay life but damage!
                PaymentDecision pd = part.accept(hcd);

                if (pd == null)
                    return false;
                else
                    part.payAsDecided(p, pd, sourceAbility);
            }
            else if (part instanceof CostPutCounter) {
                PaymentDecision pd = part.accept(hcd);

                if (pd == null)
                    return false;
                else
                    part.payAsDecided(p, pd, sourceAbility);
            }
            else if (part instanceof CostRemoveCounter) {
                CounterType counterType = ((CostRemoveCounter) part).counter;
                int amount = getAmountFromPartX(part, source, sourceAbility);

                if (!part.canPay(sourceAbility, p)) {
                    return false;
                }

                if (!mandatory) {
                    if (!p.getController().confirmPayment(part, Localizer.getInstance().getMessage("lblDoYouWantRemoveNTargetTypeCounterFromCard", String.valueOf(amount), counterType.getName(), CardTranslation.getTranslatedName(source.getName())), sourceAbility)) {
                        return false;
                    }
                }

                source.subtractCounter(counterType, amount);
            }
            else if (part instanceof CostRemoveAnyCounter) {
                int amount = getAmountFromPartX(part, source, sourceAbility);
                CardCollectionView list = p.getCardsIn(ZoneType.Battlefield);
                int allCounters = 0;
                for (Card c : list) {
                    final Map<CounterType, Integer> tgtCounters = c.getCounters();
                    for (Integer value : tgtCounters.values()) {
                        allCounters += value;
                    }
                }
                if (allCounters < amount) { return false; }

                if (!mandatory) {
                    if (!p.getController().confirmPayment(part, Localizer.getInstance().getMessage("lblDoYouWantRemoveCountersFromCard", part.getDescriptiveType()), sourceAbility)) {
                        return false;
                    }
                }

                list = CardLists.getValidCards(list, part.getType().split(";"), p, source, sourceAbility);
                while (amount > 0) {
                    final CounterType counterType;
                    list = CardLists.filter(list, new Predicate<Card>() {
                        @Override
                        public boolean apply(final Card card) {
                            return card.hasCounters();
                        }
                    });
                    if (list.isEmpty()) { return false; }
                    InputSelectCardsFromList inp = new InputSelectCardsFromList(controller, 1, 1, list, sourceAbility);
                    inp.setMessage(Localizer.getInstance().getMessage("lblSelectRemoveCounterCard"));
                    inp.setCancelAllowed(true);
                    inp.showAndWait();
                    if (inp.hasCancelled()) {
                        continue;
                    }
                    Card selected = inp.getFirstSelected();
                    final Map<CounterType, Integer> tgtCounters = selected.getCounters();
                    final List<CounterType> typeChoices = new ArrayList<>();
                    for (CounterType key : tgtCounters.keySet()) {
                        if (tgtCounters.get(key) > 0) {
                            typeChoices.add(key);
                        }
                    }
                    if (typeChoices.size() > 1) {
                        String cprompt = Localizer.getInstance().getMessage("lblSelectRemoveCounterType");
                        counterType = controller.getGui().one(cprompt, typeChoices);
                    }
                    else {
                        counterType = typeChoices.get(0);
                    }
                    selected.subtractCounter(counterType, 1);
                    amount--;
                }
            }
            else if (part instanceof CostExile) {
                CostExile costExile = (CostExile) part;

                ZoneType from = ZoneType.Graveyard;
                if ("All".equals(part.getType())) {
                    if (!p.getController().confirmPayment(part, Localizer.getInstance().getMessage("lblDoYouWantExileAllCardYouGraveyard"), sourceAbility)) {
                        return false;
                    }

                    costExile.payAsDecided(p, PaymentDecision.card(p.getCardsIn(ZoneType.Graveyard)), sourceAbility);
                }
                else {
                    from = costExile.getFrom();
                    CardCollection list = CardLists.getValidCards(p.getCardsIn(from), part.getType().split(";"), p, source, sourceAbility);
                    final int nNeeded = getAmountFromPart(costPart, source, sourceAbility);
                    if (list.size() < nNeeded) {
                        return false;
                    }
                    if (from == ZoneType.Library) {
                        if (!p.getController().confirmPayment(part, Localizer.getInstance().getMessage("lblDoYouWantExileNCardsFromYourLibrary", String.valueOf(nNeeded)), sourceAbility)) {
                            return false;
                        }
                        list = list.subList(0, nNeeded);
                        costExile.payAsDecided(p, PaymentDecision.card(list), sourceAbility);
                    } else {
                        // replace this with input
                        CardCollection newList = new CardCollection();
                        GameEntityViewMap<Card, CardView> gameCacheList = GameEntityView.getMap(list);
                        for (int i = 0; i < nNeeded; i++) {
                            final CardView cv = SGuiChoose.oneOrNone(Localizer.getInstance().getMessage("lblExileFromZone", from.getTranslatedName()), gameCacheList.getTrackableKeys());
                            if (cv == null || !gameCacheList.containsKey(cv)) {
                                return false;
                            }
                            newList.add(gameCacheList.remove(cv));
                        }
                        costExile.payAsDecided(p, PaymentDecision.card(newList), sourceAbility);
                    }
                }
            }
            else if (part instanceof CostPutCardToLib) {
                int amount = Integer.parseInt(part.getAmount());
                final ZoneType from = ((CostPutCardToLib) part).getFrom();
                final boolean sameZone = ((CostPutCardToLib) part).isSameZone();
                CardCollectionView listView;
                if (sameZone) {
                    listView = p.getGame().getCardsIn(from);
                }
                else {
                    listView = p.getCardsIn(from);
                }
                CardCollection list = CardLists.getValidCards(listView, part.getType().split(";"), p, source, sourceAbility);

                if (sameZone) { // Jotun Grunt
                    FCollectionView<Player> players = p.getGame().getPlayers();
                    List<Player> payableZone = new ArrayList<>();
                    for (Player player : players) {
                        CardCollectionView enoughType = CardLists.filter(list, CardPredicates.isOwner(player));
                        if (enoughType.size() < amount) {
                            list.removeAll(enoughType);
                        } else {
                            payableZone.add(player);
                        }
                    }
                    GameEntityViewMap<Player, PlayerView> gameCachePlayer = GameEntityView.getMap(payableZone);
                    PlayerView pv = SGuiChoose.oneOrNone(Localizer.getInstance().getMessage("lblPutCardFromWhoseZone", from.getTranslatedName()), gameCachePlayer.getTrackableKeys());
                    if (pv == null || !gameCachePlayer.containsKey(pv)) {
                        return false;
                    }
                    Player chosen = gameCachePlayer.get(pv);

                    List<Card> typeList = CardLists.filter(list, CardPredicates.isOwner(chosen));

                    GameEntityViewMap<Card, CardView> gameCacheTypeList = GameEntityView.getMap(typeList);
                    for (int i = 0; i < amount; i++) {
                        if (gameCacheTypeList.isEmpty()) {
                            return false;
                        }
                        final CardView cv = SGuiChoose.oneOrNone(Localizer.getInstance().getMessage("lblPutCardToLibrary"), gameCacheTypeList.getTrackableKeys());
                        if (cv == null || !gameCacheTypeList.containsKey(cv)) {
                            return false;
                        }
                        final Card c = gameCacheTypeList.get(cv);

                        gameCacheTypeList.remove(c);
                        p.getGame().getAction().moveToLibrary(c, Integer.parseInt(((CostPutCardToLib) part).getLibPos()), null);
                    }
                }
                else { // Tainted Specter, Gurzigost, etc.
                    boolean hasPaid = payCostPart(controller, p, sourceAbility, (CostPartWithList)part, amount, list, Localizer.getInstance().getMessage("lblPutIntoLibrary") + orString);
                    if (!hasPaid) {
                        return false;
                    }
                }
                return true;
            }
            else if (part instanceof CostSacrifice) {
                PaymentDecision pd = part.accept(hcd);
                if (pd == null)
                    return false;
                else
                    part.payAsDecided(p, pd, sourceAbility);
            }
            else if (part instanceof CostGainControl) {
                int amount = Integer.parseInt(part.getAmount());
                CardCollectionView list = CardLists.getValidCards(p.getGame().getCardsIn(ZoneType.Battlefield), part.getType(), p, source);
                boolean hasPaid = payCostPart(controller, p, sourceAbility, (CostPartWithList)part, amount, list, Localizer.getInstance().getMessage("lblGainControl") + orString);
                if (!hasPaid) { return false; }
            }
            else if (part instanceof CostReturn) {
                CardCollectionView list = CardLists.getValidCards(p.getCardsIn(ZoneType.Battlefield), part.getType(), p, source);
                int amount = getAmountFromPartX(part, source, sourceAbility);
                boolean hasPaid = payCostPart(controller, p, sourceAbility, (CostPartWithList)part, amount, list, Localizer.getInstance().getMessage("lblReturnToHand") + orString);
                if (!hasPaid) { return false; }
            }
            else if (part instanceof CostDiscard) {
                if ("Hand".equals(part.getType())) {
                    if (!p.getController().confirmPayment(part, Localizer.getInstance().getMessage("lblDoYouWantDiscardYourHand"), sourceAbility)) {
                        return false;
                    }

                    ((CostDiscard)part).payAsDecided(p, PaymentDecision.card(p.getCardsIn(ZoneType.Hand)), sourceAbility);
                } else {
                    CardCollectionView list = CardLists.getValidCards(p.getCardsIn(ZoneType.Hand), part.getType(), p, source);
                    int amount = getAmountFromPartX(part, source, sourceAbility);
                    boolean hasPaid = payCostPart(controller, p, sourceAbility, (CostPartWithList)part, amount, list, Localizer.getInstance().getMessage("lbldiscard") + orString);
                    if (!hasPaid) { return false; }
                }
            }
            else if (part instanceof CostReveal) {
                CostReveal costReveal = (CostReveal) part;
                CardCollectionView list = CardLists.getValidCards(p.getCardsIn(costReveal.getRevealFrom()), part.getType(), p, source);
                int amount = getAmountFromPartX(part, source, sourceAbility);
                boolean hasPaid = payCostPart(controller, p, sourceAbility, (CostPartWithList)part, amount, list, Localizer.getInstance().getMessage("lblReveal") + orString);
                if (!hasPaid) { return false; }
            }
            else if (part instanceof CostTapType) {
                CardCollectionView list = CardLists.getValidCards(p.getCardsIn(ZoneType.Battlefield), part.getType(), p, source);
                list = CardLists.filter(list, Presets.UNTAPPED);
                int amount = getAmountFromPartX(part, source, sourceAbility);
                boolean hasPaid = payCostPart(controller, p, sourceAbility, (CostPartWithList)part, amount, list, Localizer.getInstance().getMessage("lblTap") + orString);
                if (!hasPaid) { return false; }
            }
            else if (part instanceof CostPartMana) {
                if (!((CostPartMana) part).getManaToPay().isZero()) { // non-zero costs require input
                    mayRemovePart = false;
                }
            }
            else if (part instanceof CostPayEnergy) {
                CounterType counterType = CounterType.ENERGY;
                int amount = getAmountFromPartX(part, source, sourceAbility);

                if (!part.canPay(sourceAbility, p)) {
                    return false;
                }

                if (!mandatory) {
                    if (!p.getController().confirmPayment(part, Localizer.getInstance().getMessage("lblDoYouWantSpendNTargetTypeCounter", String.valueOf(amount), counterType.getName()), sourceAbility)) {
                        return false;
                    }
                }

                p.payEnergy(amount, source);
            }

            else {
                throw new RuntimeException("GameActionUtil.payCostDuringAbilityResolve - An unhandled type of cost was met: " + part.getClass());
            }

            if (mayRemovePart) {
                remainingParts.remove(part);
            }
        }

        if (remainingParts.isEmpty()) {
            return true;
        }
        if (remainingParts.size() > 1) {
            throw new RuntimeException("GameActionUtil.payCostDuringAbilityResolve - Too many payment types - " + source);
        }
        costPart = remainingParts.get(0);
        // check this is a mana cost
        if (!(costPart instanceof CostPartMana)) {
            throw new RuntimeException("GameActionUtil.payCostDuringAbilityResolve - The remaining payment type is not Mana.");
        }

        if (prompt == null) {
            String promptCurrent = current == null ? "" : Localizer.getInstance().getMessage("lblCurrentCard") + ": " + current;
            prompt = source + "\n" + promptCurrent;
        }

        sourceAbility.clearManaPaid();
        boolean paid = p.getController().payManaCost(cost.getCostMana(), sourceAbility, prompt, false);
        if (!paid) {
            p.getManaPool().refundManaPaid(sourceAbility);
        }
        return paid;
    }

    private static boolean payCostPart(final PlayerControllerHuman controller, Player p, SpellAbility sourceAbility, CostPartWithList cpl, int amount, CardCollectionView list, String actionName) {
        if (list.size() < amount) { return false; } // unable to pay (not enough cards)

        InputSelectCardsFromList inp = new InputSelectCardsFromList(controller, amount, amount, list, sourceAbility);
        inp.setMessage(Localizer.getInstance().getMessage("lblSelectNSpecifyTypeCardsToAction", cpl.getDescriptiveType(), actionName));
        inp.setCancelAllowed(true);

        inp.showAndWait();
        if (inp.hasCancelled() || inp.getSelected().size() != amount) {
            return false;
        }

        cpl.payAsDecided(p, PaymentDecision.card(inp.getSelected()), sourceAbility);

        return true;
    }


    private static boolean handleOfferingConvokeAndDelve(final SpellAbility ability, CardCollection cardsToDelve, boolean manaInputCancelled) {
        Card hostCard = ability.getHostCard();
        final Game game = hostCard.getGame();

        final CardZoneTable table = new CardZoneTable();
        if (!manaInputCancelled && !cardsToDelve.isEmpty()) {
            for (final Card c : cardsToDelve) {
                hostCard.addDelved(c);
                final ZoneType o = c.getZone().getZoneType();
                final Card d = game.getAction().exile(c, null);
                table.put(o, d.getZone().getZoneType(), d);
            }
        }
        if (ability.isOffering() && ability.getSacrificedAsOffering() != null) {
            final Card offering = ability.getSacrificedAsOffering();
            offering.setUsedToPay(false);
            if (!manaInputCancelled) {
                game.getAction().sacrifice(offering, ability, table);
            }
            ability.resetSacrificedAsOffering();
        }
        if (ability.isEmerge() && ability.getSacrificedAsEmerge() != null) {
            final Card emerge = ability.getSacrificedAsEmerge();
            emerge.setUsedToPay(false);
            if (!manaInputCancelled) {
                game.getAction().sacrifice(emerge, ability, table);
            }
            ability.resetSacrificedAsEmerge();
        }
        if (ability.getTappedForConvoke() != null) {
            for (final Card c : ability.getTappedForConvoke()) {
                c.setTapped(false);
                if (!manaInputCancelled) {
                    c.tap();
                }
            }
            ability.clearTappedForConvoke();
        }
        if (!table.isEmpty() && !manaInputCancelled) {
            table.triggerChangesZoneAll(game);
        }
        return !manaInputCancelled;
    }

    public static boolean payManaCost(final PlayerControllerHuman controller, final ManaCost realCost, final CostPartMana mc, final SpellAbility ability, final Player activator, String prompt, ManaConversionMatrix matrix, boolean isActivatedSa) {
        final Card source = ability.getHostCard();
        ManaCostBeingPaid toPay = new ManaCostBeingPaid(realCost, mc.getRestiction());

        String xInCard = source.getSVar("X");
        if (mc.getAmountOfX() > 0 && !"Count$xPaid".equals(xInCard)) { // announce X will overwrite whatever was in card script
            int xPaid = AbilityUtils.calculateAmount(source, "X", ability);
            toPay.setXManaCostPaid(xPaid, ability.getParam("XColor"));
            ability.setXManaCostPaid(xPaid);
        }
        else if (ability.getXManaCostPaid() != null) { //ensure pre-announced X value retained
            toPay.setXManaCostPaid(ability.getXManaCostPaid(), ability.getParam("XColor"));
        }

        int timesMultikicked = source.getKickerMagnitude();
        if (timesMultikicked > 0 && ability.isAnnouncing("Multikicker")) {
            ManaCost mkCost = ability.getMultiKickerManaCost();
            for (int i = 0; i < timesMultikicked; i++) {
                toPay.addManaCost(mkCost);
            }
        }

        int timesPseudokicked = source.getPseudoKickerMagnitude();
        if (timesPseudokicked > 0 && ability.isAnnouncing("Pseudo-multikicker")) {
            ManaCost mkCost = ability.getMultiKickerManaCost();
            for (int i = 0; i < timesPseudokicked; i++) {
                toPay.addManaCost(mkCost);
            }
        }

        Integer replicate = ability.getSVarInt("Replicate");
        if (replicate != null) {
            ManaCost rCost = source.getManaCost();
            for (int i = 0; i < replicate; i++) {
                toPay.addManaCost(rCost);
            }
        }

        CardCollection cardsToDelve = new CardCollection();
        if (isActivatedSa) {
            CostAdjustment.adjust(toPay, ability, cardsToDelve, false);
        }

        Card offering = null;
        Card emerge = null;

        InputPayMana inpPayment;
        if (ability.isOffering()) {
            if (ability.getSacrificedAsOffering() == null) {
                System.out.println("Sacrifice input for Offering cancelled");
                return false;
            } else {
                offering = ability.getSacrificedAsOffering();
            }
        }
        if (ability.isEmerge()) {
            if (ability.getSacrificedAsEmerge() == null) {
                System.out.println("Sacrifice input for Emerge cancelled");
                return false;
            } else {
                emerge = ability.getSacrificedAsEmerge();
            }
        }
        if (!toPay.isPaid()) {
            // Input is somehow clearing out the offering card?
            inpPayment = new InputPayManaOfCostPayment(controller, toPay, ability, activator, matrix);
            inpPayment.setMessagePrefix(prompt);
            inpPayment.showAndWait();
            if (!inpPayment.isPaid()) {
                return handleOfferingConvokeAndDelve(ability, cardsToDelve, true);
            }

            source.setXManaCostPaidByColor(toPay.getXManaCostPaidByColor());
            source.setColorsPaid(toPay.getColorsPaid());
            source.setSunburstValue(toPay.getSunburst());
        }

        // Handle convoke and offerings
        if (ability.isOffering()) {
            if (ability.getSacrificedAsOffering() == null && offering != null) {
                ability.setSacrificedAsOffering(offering);
            }
            if (ability.getSacrificedAsOffering() != null) {
                System.out.println("Finishing up Offering");
                offering.setUsedToPay(false);
                activator.getGame().getAction().sacrifice(offering, ability, null);
                ability.resetSacrificedAsOffering();
            }
        }
        if (ability.isEmerge()) {
            if (ability.getSacrificedAsEmerge() == null && emerge != null) {
                ability.setSacrificedAsEmerge(emerge);
            }
            if (ability.getSacrificedAsEmerge() != null) {
                System.out.println("Finishing up Emerge");
                emerge.setUsedToPay(false);
                activator.getGame().getAction().sacrifice(emerge, ability, null);
                ability.resetSacrificedAsEmerge();
            }
        }
        if (ability.getTappedForConvoke() != null) {
            activator.getGame().getTriggerHandler().suppressMode(TriggerType.Taps);
            for (final Card c : ability.getTappedForConvoke()) {
                c.setTapped(false);
                c.tap();
            }
            activator.getGame().getTriggerHandler().clearSuppression(TriggerType.Taps);
            ability.clearTappedForConvoke();
        }
        return handleOfferingConvokeAndDelve(ability, cardsToDelve, false);
    }
}
