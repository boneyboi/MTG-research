package forge.game.ability.effects;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.GameCommand;
import forge.card.CardStateName;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameObject;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.*;
import forge.game.combat.Combat;
import forge.game.event.GameEventCombatChanged;
import forge.game.player.DelayedReveal;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.player.PlayerView;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.spellability.TargetRestrictions;
import forge.game.trigger.TriggerType;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;
import forge.util.Lang;
import forge.util.MessageUtil;
import forge.util.TextUtil;
import forge.util.collect.FCollection;
import forge.util.collect.FCollectionView;
import forge.util.Localizer;
import forge.util.CardTranslation;

import java.util.List;
import java.util.Map;

public class ChangeZoneEffect extends SpellAbilityEffect {
    
    private boolean isHidden(SpellAbility sa) {
        boolean hidden = sa.hasParam("Hidden");
        if (!hidden && sa.hasParam("Origin")) {
            hidden = ZoneType.isHidden(sa.getParam("Origin"));
        }
        return hidden;
    }

    @Override
    protected String getStackDescription(SpellAbility sa) {
        if (isHidden(sa)) {
            return changeHiddenOriginStackDescription(sa);
        }
        return changeKnownOriginStackDescription(sa);
    }

    /**
     * <p>
     * changeHiddenOriginStackDescription.
     * </p>
     *
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String changeHiddenOriginStackDescription(final SpellAbility sa) {
        // TODO build Stack Description will need expansion as more cards are
        // added

        final StringBuilder sb = new StringBuilder();
        final Card host = sa.getHostCard();

        if (!(sa instanceof AbilitySub)) {
            sb.append(" -");
        }

        sb.append(" ");

        // Player whose cards will change zones
        List<Player> fetchers = null;
        if (sa.hasParam("DefinedPlayer")) {
            fetchers = AbilityUtils.getDefinedPlayers(sa.getHostCard(), sa.getParam("DefinedPlayer"), sa);
        }
        if (fetchers == null && sa.hasParam("ValidTgts") && sa.usesTargeting()) {
            fetchers = Lists.newArrayList(sa.getTargets().getTargetPlayers());
        }
        if (fetchers == null) {
            fetchers = Lists.newArrayList(sa.getHostCard().getController());
        }

        final String fetcherNames = Lang.joinHomogenous(fetchers, Player.Accessors.FN_GET_NAME);

        // Player who chooses the cards to move
        List<Player> choosers = Lists.newArrayList();
        if (sa.hasParam("Chooser")) {
            choosers = AbilityUtils.getDefinedPlayers(sa.getHostCard(), sa.getParam("Chooser"), sa);
        }
        if (choosers.isEmpty()) {
            choosers.add(sa.getActivatingPlayer());
        }

        final StringBuilder chooserSB = new StringBuilder();
        for (int i = 0; i < choosers.size(); i++) {
            chooserSB.append(choosers.get(i).getName());
            chooserSB.append((i + 2) == choosers.size() ? " and " : (i + 1) == choosers.size() ? "" : ", ");
        }
        final String chooserNames = chooserSB.toString();

        String fetchPlayer = fetcherNames;
        if (chooserNames.equals(fetcherNames)) {
            fetchPlayer = "their";
        }

        String origin = "";
        if (sa.hasParam("Origin")) {
            origin = sa.getParam("Origin");
        }
        final String destination = sa.getParam("Destination");

        final String type = sa.hasParam("ChangeType") ? sa.getParam("ChangeType") : "Card";
        final int num = sa.hasParam("ChangeNum") ? AbilityUtils.calculateAmount(host,
                sa.getParam("ChangeNum"), sa) : 1;

        if (origin.equals("Library") && sa.hasParam("Defined")) {
            // for now, just handle the Exile from top of library case, but
            // this can be expanded...
            if (destination.equals("Exile")) {
                sb.append("Exile the top card of your library");
                if (sa.hasParam("ExileFaceDown")) {
                    sb.append(" face down");
                }
            } else if (destination.equals("Ante")) {
                sb.append("Add the top card of your library to the ante");
            }
            sb.append(".");
        } else if (origin.equals("Library")) {
            sb.append(chooserNames);
            sb.append(" search").append(choosers.size() > 1 ? " " : "es ");
            sb.append(fetchPlayer);
            sb.append("'s library for ").append(num).append(" ").append(type).append(" and ");

            if (destination.equals("Exile")) {
                if (num == 1) {
                    sb.append("exiles that card ");
                } else {
                    sb.append("exiles those cards ");
                }
            } else {
                if (num == 1) {
                    sb.append("puts that card ");
                } else {
                    sb.append("puts those cards ");
                }

                if (destination.equals("Battlefield")) {
                    sb.append("onto the battlefield");
                    if (sa.hasParam("Tapped")) {
                        sb.append(" tapped");
                    }
                    if (sa.hasParam("GainControl")) {
                        sb.append(" under ").append(chooserNames).append("'s control");
                    }

                    sb.append(".");

                }
                if (destination.equals("Hand")) {
                    sb.append("into its owner's hand.");
                }
                if (destination.equals("Graveyard")) {
                    sb.append("into its owners's graveyard.");
                }
            }
            sb.append(" Then shuffle that library.");
        } else if (origin.equals("Hand")) {
            sb.append(chooserNames);
            if (!chooserNames.equals(fetcherNames)) {
                sb.append(" looks at ").append(fetcherNames).append("'s hand and ");
                sb.append(destination.equals("Exile") ? "exiles " : "puts ");
                sb.append(num).append(" of those ").append(type).append(" card(s)");
            } else {
                sb.append(destination.equals("Exile") ? " exiles " : " puts ");
                if (type.equals("Card")) {
                    sb.append(num);
                } else {
                    sb.append(num).append(" ").append(type);
                }
                sb.append(" card(s) from ").append(fetchPlayer).append(" hand");
            }

            if (destination.equals("Battlefield")) {
                sb.append(" onto the battlefield");
                if (sa.hasParam("Tapped")) {
                    sb.append(" tapped");
                }
                if (sa.hasParam("GainControl")) {
                    sb.append(" under ").append(chooserNames).append("'s control");
                }
            }
            if (destination.equals("Library")) {
                final int libraryPos = sa.hasParam("LibraryPosition") ? AbilityUtils.calculateAmount(host, sa.getParam("LibraryPosition"), sa) : 0;

                if (libraryPos == 0) {
                    sb.append(" on top");
                }
                if (libraryPos == -1) {
                    sb.append(" on the bottom");
                }

                sb.append(" of ").append(fetchPlayer);
                if (!fetchPlayer.equals("their")) {
                    sb.append("'s");
                }
                sb.append(" library");
            }

            sb.append(".");
        } else if (origin.equals("Battlefield")) {
            // TODO Expand on this Description as more cards use it
            // for the non-targeted SAs when you choose what is returned on
            // resolution
            sb.append("Return ").append(num).append(" ").append(type).append(" card(s) ");
            sb.append(" to your ").append(destination);
        }

        return sb.toString();
    }

    /**
     * <p>
     * changeKnownOriginStackDescription.
     * </p>
     *
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private static String changeKnownOriginStackDescription(final SpellAbility sa) {

        final StringBuilder sb = new StringBuilder();
        final Card host = sa.getHostCard();

        if (!(sa instanceof AbilitySub)) {
            sb.append(host.getName()).append(" -");
        }

        sb.append(" ");

        final ZoneType destination = ZoneType.smartValueOf(sa.getParam("Destination"));
        ZoneType origin = null;
        if (sa.hasParam("Origin")) {
            origin = ZoneType.listValueOf(sa.getParam("Origin")).get(0);
        }

        final StringBuilder sbTargets = new StringBuilder();

        Iterable<Card> tgts;
        if (sa.usesTargeting()) {
            tgts = sa.getTargets().getTargetCards();
        } else {
            // otherwise add self to list and go from there
            tgts = sa.knownDetermineDefined(sa.getParam("Defined"));
        }

        for (final Card c : tgts) {
            sbTargets.append(" ").append(c);
        }

        final String targetname = sbTargets.toString();

        final String pronoun = Iterables.size(tgts) > 1 ? " their " : " its ";

        final String fromGraveyard = " from the graveyard";

        if (destination.equals(ZoneType.Battlefield)) {
            sb.append("Put").append(targetname);
            if (ZoneType.Graveyard.equals(origin)) {
                sb.append(fromGraveyard);
            }

            sb.append(" onto the battlefield");
            if (sa.hasParam("Tapped")) {
                sb.append(" tapped");
            }
            if (sa.hasParam("GainControl")) {
                sb.append(" under your control");
            }
            sb.append(".");
        }

        if (destination.equals(ZoneType.Hand)) {
            sb.append("Return").append(targetname);
            if (ZoneType.Graveyard.equals(origin)) {
                sb.append(fromGraveyard);
            }
            sb.append(" to").append(pronoun).append("owners hand.");
        }

        if (destination.equals(ZoneType.Library)) {
            if (sa.hasParam("Shuffle")) { // for things like Gaea's
                                          // Blessing
                sb.append("Shuffle").append(targetname);

                sb.append(" into").append(pronoun).append("owner's library.");
            } else {
                sb.append("Put").append(targetname);
                if (ZoneType.Graveyard.equals(origin)) {
                    sb.append(fromGraveyard);
                }

                // this needs to be zero indexed. Top = 0, Third = 2, -1 =
                // Bottom
                final int libraryPosition = sa.hasParam("LibraryPosition") ? AbilityUtils.calculateAmount(host, sa.getParam("LibraryPosition"), sa) : 0;

                if (libraryPosition == -1) {
                    sb.append(" on the bottom of").append(pronoun).append("owner's library.");
                } else if (libraryPosition == 0) {
                    sb.append(" on top of").append(pronoun).append("owner's library.");
                } else {
                    sb.append(" ").append(libraryPosition + 1).append(" from the top of");
                    sb.append(pronoun).append("owner's library.");
                }
            }
        }

        if (destination.equals(ZoneType.Exile)) {
            sb.append("Exile").append(targetname);
            if (ZoneType.Graveyard.equals(origin)) {
                sb.append(fromGraveyard);
            }
            sb.append(".");
        }

        if (destination.equals(ZoneType.Ante)) {
            sb.append("Ante").append(targetname);
            sb.append(".");
        }

        if (destination.equals(ZoneType.Graveyard)) {
            sb.append("Put").append(targetname);
            if (origin != null) {
                sb.append(" from ").append(origin);
            }
            sb.append(" into").append(pronoun).append("owner's graveyard.");
        }

        return sb.toString();
    }

    /**
     * <p>
     * changeZoneResolve.
     * </p>
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     */

    @Override
    public void resolve(SpellAbility sa) {
        if (isHidden(sa) && !sa.hasParam("Ninjutsu")) {
            changeHiddenOriginResolve(sa);
        } else {
            //else if (isKnown(origin) || sa.containsKey("Ninjutsu")) {
            // Why is this an elseif and not just an else?
            changeKnownOriginResolve(sa);
        }
    }

    /**
     * <p>
     * changeKnownOriginResolve.
     * </p>
     *
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     */
    private void changeKnownOriginResolve(final SpellAbility sa) {
        Iterable<Card> tgtCards = getTargetCards(sa);
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final Player player = sa.getActivatingPlayer();
        final Card hostCard = sa.getHostCard();
        final Game game = player.getGame();

        ZoneType destination = ZoneType.smartValueOf(sa.getParam("Destination"));
        final List<ZoneType> origin = Lists.newArrayList();
        if (sa.hasParam("Origin")) {
            origin.addAll(ZoneType.listValueOf(sa.getParam("Origin")));
        }

        boolean altDest = false;
        if (sa.hasParam("DestinationAlternative")) {
            final StringBuilder sb = new StringBuilder();
            sb.append(sa.getParam("AlternativeDestinationMessage"));
            Player alterDecider = player;
            if (sa.hasParam("AlternativeDecider")) {
                alterDecider = AbilityUtils.getDefinedPlayers(hostCard, sa.getParam("AlternativeDecider"), sa).get(0);
            }
            if (!alterDecider.getController().confirmAction(sa, PlayerActionConfirmMode.ChangeZoneToAltDestination, sb.toString())) {
                destination = ZoneType.smartValueOf(sa.getParam("DestinationAlternative"));
                altDest = true;
            }
        }

        final CardZoneTable triggerList = new CardZoneTable();
        // changing zones for spells on the stack
        for (final SpellAbility tgtSA : getTargetSpells(sa)) {
            if (!tgtSA.isSpell()) { // Catch any abilities or triggers that slip through somehow
                continue;
            }

            final SpellAbilityStackInstance si = game.getStack().getInstanceFromSpellAbility(tgtSA);
            if (si == null) {
                continue;
            }

            removeFromStack(tgtSA, sa, si, game, triggerList);
        } // End of change from stack

        final String remember = sa.getParam("RememberChanged");
        final String forget = sa.getParam("ForgetChanged");
        final String imprint = sa.getParam("Imprint");

        if (sa.hasParam("Unimprint")) {
            hostCard.clearImprintedCards();
        }

        if (sa.hasParam("ForgetOtherRemembered")) {
            hostCard.clearRemembered();
        }

        final boolean optional = sa.hasParam("Optional");
        final long ts = game.getNextTimestamp();
        
        for (final Card tgtC : tgtCards) {
            if (tgt != null && tgtC.isInPlay() && !tgtC.canBeTargetedBy(sa)) {
                continue;
            }
            if (sa.hasParam("RememberLKI")) {
                hostCard.addRemembered(CardUtil.getLKICopy(tgtC));
            }

            final String prompt = TextUtil.concatWithSpace(Localizer.getInstance().getMessage("lblDoYouWantMoveTargetFromOriToDest", CardTranslation.getTranslatedName(tgtC.getName()), Lang.joinHomogenous(origin, ZoneType.Accessors.GET_TRANSLATED_NAME), destination.getTranslatedName()));
            if (optional && !player.getController().confirmAction(sa, null, prompt) )
                continue;

            final Zone originZone = game.getZoneOf(tgtC);

            // if Target isn't in the expected Zone, continue

            if (originZone == null || (!origin.isEmpty() && !origin.contains(originZone.getZoneType()))) {
                continue;
            }

            Card movedCard = null;

            if (destination.equals(ZoneType.Library)) {
                // library position is zero indexed
                int libraryPosition = 0;
                if (!altDest) {
                    libraryPosition = sa.hasParam("LibraryPosition") ? AbilityUtils.calculateAmount(hostCard, sa.getParam("LibraryPosition"), sa) : 0;
                } else {
                    libraryPosition = sa.hasParam("LibraryPositionAlternative") ? Integer.parseInt(sa.getParam("LibraryPositionAlternative")) : 0;
                }

                // If a card is moved to library from the stack, remove its spells from the stack
                if (sa.hasParam("Fizzle")) {
                    if (tgtC.isInZone(ZoneType.Exile) || tgtC.isInZone(ZoneType.Hand) || tgtC.isInZone(ZoneType.Stack)) {
                        // This only fizzles spells, not anything else.
                        game.getStack().remove(tgtC);
                    }
                }

                movedCard = game.getAction().moveToLibrary(tgtC, libraryPosition, sa);

            } else {
                if (destination.equals(ZoneType.Battlefield)) {
                    if (sa.hasParam("Tapped") || sa.hasParam("Ninjutsu")) {
                        tgtC.setTapped(true);
                    }
                    if (sa.hasParam("Transformed")) {
                        if (tgtC.isDoubleFaced()) {
                            tgtC.changeCardState("Transform", null);
                        } else {
                            // If it can't Transform, don't change zones.
                            continue;
                        }
                    }
                    if (sa.hasParam("WithCounters")) {
                        String[] parse = sa.getParam("WithCounters").split("_");
                        tgtC.addEtbCounter(CounterType.getType(parse[0]), Integer.parseInt(parse[1]), player);
                    }
                    if (sa.hasParam("GainControl")) {
                        if (sa.hasParam("NewController")) {
                            final Player p = AbilityUtils.getDefinedPlayers(hostCard, sa.getParam("NewController"), sa).get(0);
                            tgtC.setController(p, game.getNextTimestamp());
                        } else {
                            tgtC.setController(player, game.getNextTimestamp());
                        }
                    }
                    if (sa.hasParam("AttachedTo")) {
                        CardCollection list = AbilityUtils.getDefinedCards(hostCard, sa.getParam("AttachedTo"), sa);
                        if (list.isEmpty()) {
                            list = CardLists.getValidCards(game.getCardsIn(ZoneType.Battlefield), sa.getParam("AttachedTo"), tgtC.getController(), tgtC);
                        }
                        if (!list.isEmpty()) {
                            Card attachedTo = player.getController().chooseSingleEntityForEffect(list, sa, Localizer.getInstance().getMessage("lblSelectACardAttachSourceTo", tgtC.toString()));
                            tgtC.attachToEntity(attachedTo);
                        } else { // When it should enter the battlefield attached to an illegal permanent it fails
                            continue;
                        }
                    }

                    if (sa.hasParam("AttachedToPlayer")) {
                        FCollectionView<Player> list = AbilityUtils.getDefinedPlayers(hostCard, sa.getParam("AttachedToPlayer"), sa);
                        if (!list.isEmpty()) {
                            Player attachedTo = player.getController().chooseSingleEntityForEffect(list, sa, Localizer.getInstance().getMessage("lblSelectAPlayerAttachSourceTo", tgtC.toString()));
                            tgtC.attachToEntity(attachedTo);
                        }
                        else { // When it should enter the battlefield attached to an illegal player it fails
                            continue;
                        }
                    }

                    Map<AbilityKey, Object> moveParams = Maps.newEnumMap(AbilityKey.class);

                    if (sa.hasAdditionalAbility("AnimateSubAbility")) {
                        // need LKI before Animate does apply
                        moveParams.put(AbilityKey.CardLKI, CardUtil.getLKICopy(tgtC));

                        hostCard.addRemembered(tgtC);
                        AbilityUtils.resolve(sa.getAdditionalAbility("AnimateSubAbility"));
                        hostCard.removeRemembered(tgtC);
                    }

                    // Auras without Candidates stay in their current
                    // location
                    if (tgtC.isAura()) {
                        final SpellAbility saAura = tgtC.getFirstAttachSpell();
                        if (saAura != null) {
                            saAura.setActivatingPlayer(sa.getActivatingPlayer());
                            if (!saAura.getTargetRestrictions().hasCandidates(saAura, false)) {
                                if (sa.hasAdditionalAbility("AnimateSubAbility")) {
                                    tgtC.removeChangedState();
                                }
                                continue;
                            }
                        }
                    }

                    movedCard = game.getAction().moveTo(
                            tgtC.getController().getZone(destination), tgtC, sa, moveParams);
                    if (sa.hasParam("Unearth")) {
                        movedCard.setUnearthed(true);
                        movedCard.addChangedCardKeywords(Lists.newArrayList("Haste"), null, false, false,
                                game.getNextTimestamp(), true);
                        registerDelayedTrigger(sa, "Exile", Lists.newArrayList(movedCard));
                        addLeaveBattlefieldReplacement(movedCard, sa, "Exile");
                    }
                    if (sa.hasParam("FaceDown")) {
                        movedCard.turnFaceDown(true);
                    }
                    if (sa.hasParam("Attacking")) {
                        final Combat combat = game.getCombat();
                        if ( null != combat ) {
                            final FCollectionView<GameEntity> e = combat.getDefenders();

                            GameEntity defender = null;
                            if (sa.hasParam("DefinedDefender")) {
                                FCollection<GameObject> objs = AbilityUtils.getDefinedObjects(sa.getHostCard(), sa.getParam("DefinedDefender"), sa);
                                for(GameObject obj : objs) {
                                    if (obj instanceof GameEntity) {
                                        defender = (GameEntity)obj;
                                        break;
                                    }
                                }
                            } else {
                                defender = player.getController().chooseSingleEntityForEffect(e, sa, Localizer.getInstance().getMessage("lblChooseDefenderToAttackWithCard", CardTranslation.getTranslatedName(movedCard.getName())));
                            }
                            if (defender != null) {
                                combat.addAttacker(movedCard, defender);
                                game.getCombat().getBandOfAttacker(movedCard).setBlocked(false);
                                game.fireEvent(new GameEventCombatChanged());
                            }
                        }
                    }
                    if (sa.hasParam("Ninjutsu")) {
                        // Ninjutsu need to get the Defender of the Returned Creature 
                        final Card returned = sa.getPaidList("Returned").getFirst();
                        final GameEntity defender = game.getCombat().getDefenderByAttacker(returned);
                        game.getCombat().addAttacker(tgtC, defender);
                        game.getCombat().getBandOfAttacker(tgtC).setBlocked(false);
                        game.fireEvent(new GameEventCombatChanged());
                    }
                    if (sa.hasParam("Tapped") || sa.hasParam("Ninjutsu")) {
                        tgtC.setTapped(true);
                    }
                    movedCard.setTimestamp(ts);
                } else {
                    // might set before card is moved only for nontoken
                    Card host = null;
                    if (destination.equals(ZoneType.Exile) && !tgtC.isToken()) {
                        host = sa.getOriginalHost();
                        if (host == null) {
                            host = sa.getHostCard();
                        }
                        tgtC.setExiledWith(host);
                    }
                    movedCard = game.getAction().moveTo(destination, tgtC, sa);
                    // If a card is Exiled from the stack, remove its spells from the stack
                    if (sa.hasParam("Fizzle")) {
                        if (tgtC.isInZone(ZoneType.Exile) || tgtC.isInZone(ZoneType.Hand)
                                || tgtC.isInZone(ZoneType.Stack) || tgtC.isInZone(ZoneType.Command)) {
                            // This only fizzles spells, not anything else.
                            game.getStack().remove(tgtC);
                        }
                    }

                    // might set after card is moved again if something has changed
                    if (destination.equals(ZoneType.Exile) && !movedCard.isToken()) {
                        movedCard.setExiledWith(host);
                    }

                    if (sa.hasParam("ExileFaceDown")) {
                        movedCard.turnFaceDown(true);
                    }

                    if (sa.hasParam("TrackDiscarded")) {
                        movedCard.setMadnessWithoutCast(true);
                    }
                }
            }
            if (!movedCard.getZone().equals(originZone)) {
                triggerList.put(originZone.getZoneType(), movedCard.getZone().getZoneType(), movedCard);

                if (remember != null) {
                    hostCard.addRemembered(movedCard);
                    // addRememberedFromCardState ?
                    if (tgtC.getMeldedWith() != null) {
                        Card meld = game.getCardState(tgtC.getMeldedWith(), null);
                        if (meld != null) {
                            hostCard.addRemembered(meld);
                        }
                    }
                }
                if (forget != null) {
                    hostCard.removeRemembered(movedCard);
                }
                if (imprint != null) {
                    hostCard.addImprintedCard(movedCard);
                }
            }
        }

        triggerList.triggerChangesZoneAll(game);

        // for things like Gaea's Blessing
        if (destination.equals(ZoneType.Library) && sa.hasParam("Shuffle") && "True".equals(sa.getParam("Shuffle"))) {
            FCollection<Player> pl = new FCollection<>();
            // use defined controller. it does need to work even without Targets.
            if (sa.hasParam("TargetsWithDefinedController")) {
                pl.addAll(AbilityUtils.getDefinedPlayers(hostCard, sa.getParam("TargetsWithDefinedController"), sa));
            } else {
                for (final Card tgtC : tgtCards) {
                    // FCollection already does use set.
                    pl.add(tgtC.getOwner());
                }
            }

            for (final Player p : pl) {
                p.shuffle(sa);
            }
        }
    }

    /**
     * <p>
     * changeHiddenOriginResolve.
     * </p>
     *
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     */
    private static void changeHiddenOriginResolve(final SpellAbility sa) {
        List<Player> fetchers;

        if (sa.hasParam("DefinedPlayer")) {
            fetchers = AbilityUtils.getDefinedPlayers(sa.getHostCard(), sa.getParam("DefinedPlayer"), sa);
        } else {
            fetchers = AbilityUtils.getDefinedPlayers(sa.getHostCard(), sa.getParam("Defined"), sa);
        }

        // handle case when Defined is for a Card
        if (fetchers.isEmpty()) {
            fetchers.add(sa.getHostCard().getController());
        }

        Player chooser = null;
        if (sa.hasParam("Chooser")) {
            final String choose = sa.getParam("Chooser");
            if (choose.equals("Targeted") && sa.getTargets().isTargetingAnyPlayer()) {
                chooser = sa.getTargets().getFirstTargetedPlayer();
            } else {
                chooser = AbilityUtils.getDefinedPlayers(sa.getHostCard(), choose, sa).get(0);
            }
        }

        for (final Player player : fetchers) {
            Player decider = chooser;
            if (decider == null) {
                decider = player;
            }
            changeZonePlayerInvariant(decider, sa, player);
        }
    }

    private static void changeZonePlayerInvariant(Player decider, SpellAbility sa, Player player) {
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        if (tgt != null) {
            final List<Player> players = Lists.newArrayList(sa.getTargets().getTargetPlayers());
            player = sa.hasParam("DefinedPlayer") ? player : players.get(0);
            if (players.contains(player) && !player.canBeTargetedBy(sa)) {
                return;
            }
        }

        List<ZoneType> origin = Lists.newArrayList();
        if (sa.hasParam("Origin")) {
            origin = ZoneType.listValueOf(sa.getParam("Origin"));
        }
        ZoneType destination = ZoneType.smartValueOf(sa.getParam("Destination"));
        final Card source = sa.getHostCard();

        // this needs to be zero indexed. Top = 0, Third = 2
        int libraryPos = sa.hasParam("LibraryPosition") ? AbilityUtils.calculateAmount(source, sa.getParam("LibraryPosition"), sa) : 0;

        if (sa.hasParam("OriginChoice")) {
            // Currently only used for Mishra, but may be used by other things
            // Improve how this message reacts for other cards
            final List<ZoneType> alt = ZoneType.listValueOf(sa.getParam("OriginAlternative"));
            CardCollectionView altFetchList = AbilityUtils.filterListByType(player.getCardsIn(alt), sa.getParam("ChangeType"), sa);

            final StringBuilder sb = new StringBuilder();
            sb.append(sa.getParam("AlternativeMessage")).append(" ");
            sb.append(altFetchList.size()).append(" " + Localizer.getInstance().getMessage("lblCardMatchSearchingTypeInAlternateZones"));

            if (!decider.getController().confirmAction(sa, PlayerActionConfirmMode.ChangeZoneFromAltSource, sb.toString())) {
                origin = alt;
            }
        }

        if (sa.hasParam("DestinationAlternative")) {
            final StringBuilder sb = new StringBuilder();
            sb.append(sa.getParam("AlternativeDestinationMessage"));

            if (!decider.getController().confirmAction(sa, PlayerActionConfirmMode.ChangeZoneToAltDestination, sb.toString())) {
                destination = ZoneType.smartValueOf(sa.getParam("DestinationAlternative"));
                libraryPos = sa.hasParam("LibraryPositionAlternative") ? Integer.parseInt(sa.getParam("LibraryPositionAlternative")) : 0;
            }
        }

        final boolean defined = sa.hasParam("Defined");
        int changeNum = sa.hasParam("ChangeNum") ? AbilityUtils.calculateAmount(source, sa.getParam("ChangeNum"), sa) : 1;

        final boolean optional = sa.hasParam("Optional");
        if (optional) {
            String prompt;
            if (defined) {
                prompt = Localizer.getInstance().getMessage("lblPutThatCardFromPlayerOriginToDestination", "{player's}", Lang.joinHomogenous(origin, ZoneType.Accessors.GET_TRANSLATED_NAME).toLowerCase(), destination.getTranslatedName().toLowerCase());
            }
            else {
                prompt = Localizer.getInstance().getMessage("lblSearchPlayerZoneConfirm", "{player's}", Lang.joinHomogenous(origin, ZoneType.Accessors.GET_TRANSLATED_NAME).toLowerCase());
            }
            String message = MessageUtil.formatMessage(prompt , decider, player);
            if (!decider.getController().confirmAction(sa, PlayerActionConfirmMode.ChangeZoneGeneral, message)) {
                return;
            }
        }

        String changeType = sa.getParam("ChangeType"); 

        CardCollection fetchList;
        boolean shuffleMandatory = true;
        boolean searchedLibrary = false;
        if (defined) {
            fetchList = new CardCollection(AbilityUtils.getDefinedCards(source, sa.getParam("Defined"), sa));
            if (!sa.hasParam("ChangeNum")) {
                changeNum = fetchList.size();
            }
        }
        else if (!origin.contains(ZoneType.Library) && !origin.contains(ZoneType.Hand)
                && !sa.hasParam("DefinedPlayer")) {
            fetchList = new CardCollection(player.getGame().getCardsIn(origin));
        }
        else {
            fetchList = new CardCollection(player.getCardsIn(origin));
            if (origin.contains(ZoneType.Library) && !sa.hasParam("NoLooking")) {
                searchedLibrary = true;
                if (decider.hasKeyword("LimitSearchLibrary")) { // Aven Mindcensor
                    fetchList.removeAll(player.getCardsIn(ZoneType.Library));
                    final int fetchNum = Math.min(player.getCardsIn(ZoneType.Library).size(), 4);
                    if (fetchNum == 0) {
                        searchedLibrary = false;
                    }
                    else {
                        fetchList.addAll(player.getCardsIn(ZoneType.Library, fetchNum));
                    }
                }
                if (!decider.canSearchLibraryWith(sa, player)) {
                    fetchList.removeAll(player.getCardsIn(ZoneType.Library));
                    // "if you do/sb does, shuffle" is not mandatory (usually a triggered ability), should has this param. 
                    // "then shuffle" is mandatory
                    shuffleMandatory = !sa.hasParam("ShuffleNonMandatory");
                    searchedLibrary = false;
                }
            }
        }

        //determine list of all cards to reveal to player in addition to those that can be chosen
        DelayedReveal delayedReveal = null;
        if (!defined && !sa.hasParam("AlreadyRevealed")) {
            if (origin.contains(ZoneType.Library) && searchedLibrary) {
                final int fetchNum = Math.min(player.getCardsIn(ZoneType.Library).size(), 4);
                CardCollectionView shown = !decider.hasKeyword("LimitSearchLibrary") ? player.getCardsIn(ZoneType.Library) : player.getCardsIn(ZoneType.Library, fetchNum);
                // Look at whole library before moving onto choosing a card
                delayedReveal = new DelayedReveal(shown, ZoneType.Library, PlayerView.get(player), CardTranslation.getTranslatedName(source.getName()) + " - " + Localizer.getInstance().getMessage("lblLookingCardIn") + " ");
            }
            else if (origin.contains(ZoneType.Hand) && player.isOpponentOf(decider)) {
                delayedReveal = new DelayedReveal(player.getCardsIn(ZoneType.Hand), ZoneType.Hand, PlayerView.get(player), CardTranslation.getTranslatedName(source.getName()) + " - " + Localizer.getInstance().getMessage("lblLookingCardIn") + " ");
            }
        }

        if (searchedLibrary) {
            if (decider.equals(player)) {
                // should only count the number of searching player's own library
                decider.incLibrarySearched();
                // Panglacial Wurm
                CardCollection canCastWhileSearching = CardLists.getKeyword(fetchList,
                        "While you're searching your library, you may cast CARDNAME from your library.");
                for (final Card tgtCard : canCastWhileSearching) {
                    List<SpellAbility> sas = AbilityUtils.getBasicSpellsFromPlayEffect(tgtCard, decider);
                    if (sas.isEmpty()) {
                        continue;
                    }
                    SpellAbility tgtSA = decider.getController().getAbilityToPlay(tgtCard, sas);
                    if (!decider.getController().confirmAction(tgtSA, null, Localizer.getInstance().getMessage("lblDoYouWantPlayCard", CardTranslation.getTranslatedName(tgtCard.getName())))) {
                        continue;
                    }
                    // if played, that card cannot be found
                    if (decider.getController().playSaFromPlayEffect(tgtSA)) {
                        fetchList.remove(tgtCard);
                    }
                }
            }
            final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
            runParams.put(AbilityKey.Player, decider);
            runParams.put(AbilityKey.Target, Lists.newArrayList(player));
            decider.getGame().getTriggerHandler().runTrigger(TriggerType.SearchedLibrary, runParams, false);
        }

        if (!defined && changeType != null) {
            fetchList = (CardCollection)AbilityUtils.filterListByType(fetchList, sa.getParam("ChangeType"), sa);
        }

        if (sa.hasParam("NoShuffle")) {
            shuffleMandatory = false;
        }

        final Game game = player.getGame();
        if (sa.hasParam("Unimprint")) {
            source.clearImprintedCards();
        }

        final boolean remember = sa.hasParam("RememberChanged");
        final boolean champion = sa.hasParam("Champion");
        final boolean forget = sa.hasParam("ForgetChanged");
        final boolean imprint = sa.hasParam("Imprint");
        String selectPrompt = sa.hasParam("SelectPrompt") ? sa.getParam("SelectPrompt") : MessageUtil.formatMessage(Localizer.getInstance().getMessage("lblSelectCardFromPlayerZone", "{player's}", Lang.joinHomogenous(origin, ZoneType.Accessors.GET_TRANSLATED_NAME).toLowerCase()), decider, player);
        final String totalcmc = sa.getParam("WithTotalCMC");
        int totcmc = AbilityUtils.calculateAmount(source, totalcmc, sa);

        fetchList.sort();

        CardCollection chosenCards = new CardCollection();
        // only multi-select if player can select more than one
        if (changeNum > 1 && allowMultiSelect(decider, sa)) {
            List<Card> selectedCards;
            if (! sa.hasParam("SelectPrompt")) {
                // new default messaging for multi select
                if (fetchList.size() > changeNum) {
                    //Select up to %changeNum cards from %players %origin
                    selectPrompt = MessageUtil.formatMessage(Localizer.getInstance().getMessage("lblSelectUpToNumCardFromPlayerZone", String.valueOf(changeNum), "{player's}", Lang.joinHomogenous(origin, ZoneType.Accessors.GET_TRANSLATED_NAME).toLowerCase()), decider, player);
                } else {
                    selectPrompt = MessageUtil.formatMessage(Localizer.getInstance().getMessage("lblSelectCardsFromPlayerZone", "{player's}", Lang.joinHomogenous(origin, ZoneType.Accessors.GET_TRANSLATED_NAME).toLowerCase()), decider, player);
                }
            }
            // ensure that selection is within maximum allowed changeNum
            do {
                selectedCards = decider.getController().chooseCardsForZoneChange(destination, origin, sa, fetchList, 0, changeNum, delayedReveal, selectPrompt, decider);
            } while (selectedCards != null && selectedCards.size() > changeNum);
            if (selectedCards != null) {
                chosenCards.addAll(selectedCards);
            }
            // maybe prompt the user if they selected fewer than the maximum possible?
        } else {
            // one at a time
            for (int i = 0; i < changeNum && destination != null; i++) {
                if (sa.hasParam("DifferentNames")) {
                    for (Card c : chosenCards) {
                        fetchList = CardLists.filter(fetchList, Predicates.not(CardPredicates.sharesNameWith(c)));
                    }
                }
                if (sa.hasParam("DifferentCMC")) {
                    for (Card c : chosenCards) {
                        fetchList = CardLists.filter(fetchList, Predicates.not(CardPredicates.sharesCMCWith(c)));
                    }
                }
                if (sa.hasParam("ShareLandType")) {
                    // After the first card is chosen, check if the land type is shared
                    for (final Card card : chosenCards) {
                        fetchList = CardLists.filter(fetchList, new Predicate<Card>() {
                            @Override
                            public boolean apply(final Card c) {
                                return c.sharesLandTypeWith(card);
                            }

                        });
                    }
                }
                if (totalcmc != null) {
                    if (totcmc >= 0) {
                        fetchList = CardLists.getValidCards(fetchList, "Card.cmcLE" + totcmc, source.getController(), source);
                    }
                }

                // If we're choosing multiple cards, only need to show the reveal dialog the first time through.
                boolean shouldReveal = (i == 0);
                Card c = null;
                if (sa.hasParam("AtRandom")) {
                    if (shouldReveal && delayedReveal != null) {
                        decider.getController().reveal(delayedReveal.getCards(), delayedReveal.getZone(), delayedReveal.getOwner(), delayedReveal.getMessagePrefix());
                    }
                    c = Aggregates.random(fetchList);
                } else if (defined && !sa.hasParam("ChooseFromDefined")) {
                    c = Iterables.getFirst(fetchList, null);
                } else {
                    String title = selectPrompt;
                    if (changeNum > 1) { //indicate progress if multiple cards being chosen
                        title += " (" + (i + 1) + " / " + changeNum + ")";
                    }
                    c = decider.getController().chooseSingleCardForZoneChange(destination, origin, sa, fetchList, shouldReveal ? delayedReveal : null, title, !sa.hasParam("Mandatory"), decider);
                }

                if (c == null) {
                    final int num = Math.min(fetchList.size(), changeNum - i);
                    String message = Localizer.getInstance().getMessage("lblCancelSearchUpToSelectNumCards", String.valueOf(num));

                    if (fetchList.isEmpty() || decider.getController().confirmAction(sa, PlayerActionConfirmMode.ChangeZoneGeneral, message)) {
                        break;
                    }
                    i--;
                    continue;
                }

                fetchList.remove(c);
                if (delayedReveal != null) {
                    delayedReveal.remove(CardView.get(c));
                }
                chosenCards.add(c);

                if (totalcmc != null) {
                    totcmc -= c.getCMC();
                }
            }
        }

        if (sa.hasParam("ShuffleChangedPile")) {
            CardLists.shuffle(chosenCards);
        }
        // do not shuffle the library once we have placed a fetched
        // card on top.
        if (origin.contains(ZoneType.Library) && (destination == ZoneType.Library) && !"False".equals(sa.getParam("Shuffle"))) {
            player.shuffle(sa);
        }

        CardCollection movedCards = new CardCollection();
        long ts = game.getNextTimestamp();
        final CardZoneTable triggerList = new CardZoneTable();
        for (final Card c : chosenCards) {
            Card movedCard = null;
            final Zone originZone = game.getZoneOf(c);
            if (destination.equals(ZoneType.Library)) {
                movedCard = game.getAction().moveToLibrary(c, libraryPos, sa);
            }
            else if (destination.equals(ZoneType.Battlefield)) {
                if (sa.hasParam("Tapped")) {
                    c.setTapped(true);
                }

                Map<AbilityKey, Object> moveParams = Maps.newEnumMap(AbilityKey.class);

                if (sa.hasAdditionalAbility("AnimateSubAbility")) {
                    // need LKI before Animate does apply
                    moveParams.put(AbilityKey.CardLKI, CardUtil.getLKICopy(c));

                    source.addRemembered(c);
                    AbilityUtils.resolve(sa.getAdditionalAbility("AnimateSubAbility"));
                    source.removeRemembered(c);
                }

                if (sa.hasParam("GainControl")) {
                    Player newController = sa.getActivatingPlayer();
                    if (sa.hasParam("NewController")) {
                        newController = AbilityUtils.getDefinedPlayers(sa.getHostCard(), sa.getParam("NewController"), sa).get(0);
                    } 
                    c.setController(newController, game.getNextTimestamp());
                }
                if (sa.hasParam("WithCounters")) {
                    String[] parse = sa.getParam("WithCounters").split("_");
                    c.addEtbCounter(CounterType.getType(parse[0]), Integer.parseInt(parse[1]), player);
                }
                if (sa.hasParam("Transformed")) {
                    if (c.isDoubleFaced()) {
                        c.changeCardState("Transform", null);
                    } else {
                        // If it can't Transform, don't change zones.
                        continue;
                    }
                }

                if (sa.hasParam("AttachedTo")) {
                    CardCollection list = AbilityUtils.getDefinedCards(source, sa.getParam("AttachedTo"), sa);
                    if (list.isEmpty()) {
                        list = CardLists.getValidCards(game.getCardsIn(ZoneType.Battlefield), sa.getParam("AttachedTo"), c.getController(), c);
                    }
                    if (!list.isEmpty()) {
                        Card attachedTo = null;
                        if (list.size() > 1) {
                            attachedTo = decider.getController().chooseSingleEntityForEffect(list, sa, Localizer.getInstance().getMessage("lblSelectACardAttachSourceTo", CardTranslation.getTranslatedName(c.getName())));
                        }
                        else {
                            attachedTo = list.get(0);
                        }

                        if (c.isAttachment()) {
                            c.attachToEntity(attachedTo);
                        }
                    }
                    else { // When it should enter the battlefield attached to an illegal permanent it fails
                        continue;
                    }
                }

                if (sa.hasParam("AttachedToPlayer")) {
                    FCollectionView<Player> list = AbilityUtils.getDefinedPlayers(source, sa.getParam("AttachedToPlayer"), sa);
                    if (!list.isEmpty()) {
                        Player attachedTo = player.getController().chooseSingleEntityForEffect(list, sa, Localizer.getInstance().getMessage("lblSelectACardAttachSourceTo", CardTranslation.getTranslatedName(c.getName())));
                        c.attachToEntity(attachedTo);
                    }
                    else { // When it should enter the battlefield attached to an illegal permanent it fails
                        continue;
                    }
                }

                if (sa.hasParam("Attacking")) {
                    final Combat combat = game.getCombat();
                    if ( null != combat ) {
                        final FCollectionView<GameEntity> e = combat.getDefenders();

                        GameEntity defender = null;
                        if (sa.hasParam("DefinedDefender")) {
                            FCollection<GameObject> objs = AbilityUtils.getDefinedObjects(source, sa.getParam("DefinedDefender"), sa);
                            for(GameObject obj : objs) {
                                if (obj instanceof GameEntity) {
                                    defender = (GameEntity)obj;
                                    break;
                                }
                            }
                        } else {
                            defender = player.getController().chooseSingleEntityForEffect(e, sa, Localizer.getInstance().getMessage("lblChooseDefenderToAttackWithCard", CardTranslation.getTranslatedName(c.getName())));
                        }
                        if (defender != null) {
                            combat.addAttacker(c, defender);
                            game.getCombat().getBandOfAttacker(c).setBlocked(false);
                            game.fireEvent(new GameEventCombatChanged());
                        }
                    }
                }
                if (sa.hasParam("Blocking")) {
                    final Combat combat = game.getCombat();
                    if ( null != combat ) {
                        CardCollection attackers = AbilityUtils.getDefinedCards(source, sa.getParam("Blocking"), sa);
                        if (!attackers.isEmpty()) {
                            Card attacker = attackers.get(0);
                            if (combat.isAttacking(attacker)) {
                                combat.addBlocker(attacker, c);
                                combat.orderAttackersForDamageAssignment(c);
                                game.fireEvent(new GameEventCombatChanged());
                            }
                        }
                    }
                }
                // need to be facedown before it hits the battlefield in case of Replacement Effects or Trigger
                if (sa.hasParam("FaceDown") && ZoneType.Battlefield.equals(destination)) {
                    c.turnFaceDown(true);

                    // set New Pt doesn't work because this values need to be copyable for clone effects
                    if (sa.hasParam("FaceDownPower")) {
                        c.setBasePower(AbilityUtils.calculateAmount(
                                source, sa.getParam("FaceDownPower"), sa));
                    }
                    if (sa.hasParam("FaceDownToughness")) {
                        c.setBaseToughness(AbilityUtils.calculateAmount(
                                source, sa.getParam("FaceDownToughness"), sa));
                    }

                    if (sa.hasParam("FaceDownAddType")) {
                        for (String type : sa.getParam("FaceDownAddType").split(",")) {
                            c.addType(type);
                        }
                    }

                    if (sa.hasParam("FaceDownPower") || sa.hasParam("FaceDownToughness")
                            || sa.hasParam("FaceDownAddType")) {
                        final GameCommand unanimate = new GameCommand() {
                            private static final long serialVersionUID = 8853789549297846163L;

                            @Override
                            public void run() {
                                c.clearStates(CardStateName.FaceDown, true);
                            }
                        };

                        c.addFaceupCommand(unanimate);
                    }
                }
                movedCard = game.getAction().moveTo(c.getController().getZone(destination), c, sa, moveParams);
                if (sa.hasParam("Tapped")) {
                    movedCard.setTapped(true);
                }

                // need to do that again?
                if (sa.hasParam("FaceDown") && !ZoneType.Battlefield.equals(destination)) {
                    movedCard.turnFaceDown(true);
                }
                movedCard.setTimestamp(ts);
            }
            else if (destination.equals(ZoneType.Exile)) {
                movedCard = game.getAction().exile(c, sa);
                if (!c.isToken()) {
                    Card host = sa.getOriginalHost();
                    if (host == null) {
                        host = sa.getHostCard();
                    }
                    movedCard.setExiledWith(host);
                }
                if (sa.hasParam("ExileFaceDown")) {
                    movedCard.turnFaceDown(true);
                }
            }
            else {
                movedCard = game.getAction().moveTo(destination, c, sa);
            }
            
            movedCards.add(movedCard);

            if (originZone != null) {
                triggerList.put(originZone.getZoneType(), movedCard.getZone().getZoneType(), movedCard);
            }

            if (champion) {
                final Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(source);
                runParams.put(AbilityKey.Championed, c);
                game.getTriggerHandler().runTrigger(TriggerType.Championed, runParams, false);
            }
            
            if (remember) {
                source.addRemembered(movedCard);
                // addRememberedFromCardState ?
                if (c.getMeldedWith() != null) {
                    Card meld = game.getCardState(c.getMeldedWith(), null);
                    if (meld != null) {
                        source.addRemembered(meld);
                    }
                }
            }
            if (forget) {
                source.removeRemembered(movedCard);
            }
            // for imprinted since this doesn't use Target
            if (imprint) {
                source.addImprintedCard(movedCard);
            }
        }

        if (((!ZoneType.Battlefield.equals(destination) && changeType != null && !defined && !changeType.equals("Card"))
                || (sa.hasParam("Reveal") && !movedCards.isEmpty())) && !sa.hasParam("NoReveal")) {
            game.getAction().reveal(movedCards, player);
        }
        
        if ((origin.contains(ZoneType.Library) && !destination.equals(ZoneType.Library) && !defined && shuffleMandatory)
                || (sa.hasParam("Shuffle") && "True".equals(sa.getParam("Shuffle")))) {
            player.shuffle(sa);
        }

        triggerList.triggerChangesZoneAll(game);
    }

    private static boolean allowMultiSelect(Player decider, SpellAbility sa) {
        return decider.getController().isGuiPlayer()        // limit mass selection to human players for now
                && !sa.hasParam("Mandatory")                // only handle optional decisions, for now
                && !sa.hasParam("ShareLandType")
                && !sa.hasParam("DifferentNames")
                && !sa.hasParam("DifferentCMC")
                && !sa.hasParam("AtRandom")
                && (!sa.hasParam("Defined") || sa.hasParam("ChooseFromDefined"))
                && sa.getParam("WithTotalCMC") == null;
    }

    /**
     * <p>
     * removeFromStack.
     * </p>
     *
     * @param tgtSA
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param srcSA
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param si
     *            a {@link forge.game.spellability.SpellAbilityStackInstance}
     *            object.
     * @param game 
     */
    private static void removeFromStack(final SpellAbility tgtSA, final SpellAbility srcSA, final SpellAbilityStackInstance si, final Game game, CardZoneTable triggerList) {
        final Card tgtHost = tgtSA.getHostCard();
        final Zone originZone = tgtHost.getZone();
        game.getStack().remove(si);
        
        Map<AbilityKey,Object> params = AbilityKey.newMap();
        params.put(AbilityKey.StackSa, tgtSA);
        params.put(AbilityKey.StackSi, si);

        Card movedCard = null;
        if (srcSA.hasParam("Destination")) {
            final boolean remember = srcSA.hasParam("RememberChanged");
            if (tgtSA.isAbility()) {
                // Shouldn't be able to target Abilities but leaving this in for now
            } else if (srcSA.getParam("Destination").equals("Graveyard")) {
                movedCard = game.getAction().moveToGraveyard(tgtHost, srcSA, params);
            } else if (srcSA.getParam("Destination").equals("Exile")) {
                Card host = srcSA.getOriginalHost();
                if (host == null) {
                    host = srcSA.getHostCard();
                }
                movedCard = game.getAction().exile(tgtHost, srcSA, params);
                movedCard.setExiledWith(host);
            } else if (srcSA.getParam("Destination").equals("TopOfLibrary")) {
                movedCard = game.getAction().moveToLibrary(tgtHost, srcSA, params);
            } else if (srcSA.getParam("Destination").equals("Hand")) {
                movedCard = game.getAction().moveToHand(tgtHost, srcSA, params);
            } else if (srcSA.getParam("Destination").equals("BottomOfLibrary")) {
                movedCard = game.getAction().moveToBottomOfLibrary(tgtHost, srcSA, params);
            } else if (srcSA.getParam("Destination").equals("Library")) {
                movedCard = game.getAction().moveToBottomOfLibrary(tgtHost, srcSA, params);
                if (srcSA.hasParam("Shuffle") && "True".equals(srcSA.getParam("Shuffle"))) {
                    tgtHost.getOwner().shuffle(srcSA);
                }
            } else {
                throw new IllegalArgumentException("AbilityFactory_ChangeZone: Invalid Destination argument for card "
                        + srcSA.getHostCard().getName());
            }

            if (remember) {
                srcSA.getHostCard().addRemembered(tgtHost);
                // TODO or remember moved?
            }

            if (!tgtSA.isAbility()) {
                System.out.println("Moving spell to " + srcSA.getParam("Destination"));
            }
            if (originZone != null && movedCard != null) {
                triggerList.put(originZone.getZoneType(), movedCard.getZone().getZoneType(), movedCard);
            }
        }
    }
}
