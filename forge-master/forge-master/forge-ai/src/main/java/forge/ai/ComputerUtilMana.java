package forge.ai;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.*;
import forge.ai.ability.AnimateAi;
import forge.card.CardStateName;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.card.mana.ManaAtom;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostParser;
import forge.card.mana.ManaCostShard;
import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.*;
import forge.game.combat.CombatUtil;
import forge.game.cost.*;
import forge.game.mana.Mana;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.mana.ManaPool;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerPredicates;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementType;
import forge.game.spellability.AbilityManaPart;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;
import forge.util.TextUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class ComputerUtilMana {
    private final static boolean DEBUG_MANA_PAYMENT = false;

    public static boolean canPayManaCost(ManaCostBeingPaid cost, final SpellAbility sa, final Player ai) {
        cost = new ManaCostBeingPaid(cost); //check copy of cost so it doesn't modify the exist cost being paid
        return payManaCost(cost, sa, ai, true, true);
    }

    public static boolean payManaCost(ManaCostBeingPaid cost, final SpellAbility sa, final Player ai) {
        return payManaCost(cost, sa, ai, false, true);
    }

    public static boolean canPayManaCost(final SpellAbility sa, final Player ai, final int extraMana) {
        return payManaCost(sa, ai, true, extraMana, true);
    }
    
    /**
     * Return the number of colors used for payment for Converge
     */
    public static int getConvergeCount(final SpellAbility sa, final Player ai) {
    	ManaCostBeingPaid cost = ComputerUtilMana.calculateManaCost(sa, true, 0);
    	if (payManaCost(cost, sa, ai, true, true)) {
    		return cost.getSunburst();
    	} else {
    		return 0;
    	}
    }

    // Does not check if mana sources can be used right now, just checks for potential chance.
    public static boolean hasEnoughManaSourcesToCast(final SpellAbility sa, final Player ai) {
        if(ai == null || sa == null)
            return false;
        sa.setActivatingPlayer(ai);
        return payManaCost(sa, ai, true, 0, false);
    }

    public static boolean payManaCost(final Player ai, final SpellAbility sa) {
        return payManaCost(sa, ai, false, 0, true);
    }

    private static void refundMana(List<Mana> manaSpent, Player ai, SpellAbility sa) {
        if (sa.getHostCard() != null) {
            sa.getHostCard().setCanCounter(true);
        }
        for (final Mana m : manaSpent) {
            ai.getManaPool().addMana(m);
        }
        manaSpent.clear();
    }

    private static boolean payManaCost(final SpellAbility sa, final Player ai, final boolean test, final int extraMana, boolean checkPlayable) {
        ManaCostBeingPaid cost = ComputerUtilMana.calculateManaCost(sa, test, extraMana);
        return payManaCost(cost, sa, ai, test, checkPlayable);
    }

    private static Integer scoreManaProducingCard(final Card card) {
        int score = 0;

        for (SpellAbility ability : card.getSpellAbilities()) {
            ability.setActivatingPlayer(card.getController());
            if (ability.isManaAbility()) {
                score += ability.calculateScoreForManaAbility();
            }
            else if (!ability.isTrigger() && ability.isPossible()) {
                score += 13; //add 13 for any non-mana activated abilities
            }
        }

        if (card.isCreature()) {
            //treat attacking and blocking as though they're non-mana abilities
            if (CombatUtil.canAttack(card)) {
                score += 13;
            }
            if (CombatUtil.canBlock(card)) {
                score += 13;
            }
        }

        return score;
    }
    
    private static void sortManaAbilities(final Multimap<ManaCostShard, SpellAbility> manaAbilityMap) {
        final Map<Card, Integer> manaCardMap = Maps.newHashMap();
        final List<Card> orderedCards = Lists.newArrayList();
        
        for (final ManaCostShard shard : manaAbilityMap.keySet()) {
            for (SpellAbility ability : manaAbilityMap.get(shard)) {
            	final Card hostCard = ability.getHostCard();
                if (!manaCardMap.containsKey(hostCard)) {
                    manaCardMap.put(hostCard, scoreManaProducingCard(hostCard));
                    orderedCards.add(hostCard);
                }
            }
        }
        Collections.sort(orderedCards, new Comparator<Card>() {
            @Override
            public int compare(final Card card1, final Card card2) {
                return Integer.compare(manaCardMap.get(card1), manaCardMap.get(card2));
            }
        });

        if (DEBUG_MANA_PAYMENT) {
            System.out.print("Ordered Cards: " + orderedCards.size());
            for (Card card : orderedCards) {
                System.out.print(card.getName() + ", ");
            }
            System.out.println();
        }

        for (final ManaCostShard shard : manaAbilityMap.keySet()) {
            final Collection<SpellAbility> abilities = manaAbilityMap.get(shard);
            final List<SpellAbility> newAbilities = new ArrayList<>(abilities);

            if (DEBUG_MANA_PAYMENT) {
                System.out.println("Unsorted Abilities: " + newAbilities);
            }

            Collections.sort(newAbilities, new Comparator<SpellAbility>() {
                @Override
                public int compare(final SpellAbility ability1, final SpellAbility ability2) {
                    int preOrder = orderedCards.indexOf(ability1.getHostCard()) - orderedCards.indexOf(ability2.getHostCard());

                    if (preOrder == 0) {
                        // Mana abilities on the same card
                        String shardMana = shard.toString().replaceAll("\\{", "").replaceAll("\\}", "");

                        boolean payWithAb1 = ability1.getManaPart().mana().contains(shardMana);
                        boolean payWithAb2 = ability2.getManaPart().mana().contains(shardMana);

                        if (payWithAb1 && !payWithAb2) {
                            return -1;
                        } else if (payWithAb2 && !payWithAb1) {
                            return 1;
                        }

                        return ability1.compareTo(ability2);
                    } else {
                        return preOrder;
                    }
                }
            });

            if (DEBUG_MANA_PAYMENT) {
                System.out.println("Sorted Abilities: " + newAbilities);
            }

            manaAbilityMap.replaceValues(shard, newAbilities);
        }
    }
 
    public static SpellAbility chooseManaAbility(ManaCostBeingPaid cost, SpellAbility sa, Player ai, ManaCostShard toPay,
            Collection<SpellAbility> saList, boolean checkCosts) {
        for (final SpellAbility ma : saList) {
            if (ma.getHostCard() == sa.getHostCard()) {
                continue;
            }

            if (sa.getHostCard() != null) {
                if (sa.getApi() == ApiType.Animate) {
                    // For abilities like Genju of the Cedars, make sure that we're not activating the aura ability by tapping the enchanted card for mana
                    if (sa.getHostCard().isAura() && "Enchanted".equals(sa.getParam("Defined"))
                            && ma.getHostCard() == sa.getHostCard().getEnchantingCard()
                            && ma.getPayCosts().hasTapCost()) {
                        continue;
                    }

                    // If a manland was previously animated this turn, do not tap it to animate another manland
                    if (sa.getHostCard().isLand() && ma.getHostCard().isLand()
                            && ai.getController().isAI()
                            && AnimateAi.isAnimatedThisTurn(ai, ma.getHostCard())) {
                        continue;
                    }
                } else if (sa.getApi() == ApiType.Pump) {
                    if ((sa.getHostCard().isInstant() || sa.getHostCard().isSorcery())
                            && ma.getHostCard().isCreature()
                            && ai.getController().isAI()
                            && ma.getPayCosts().hasTapCost()
                            && sa.getTargets().getTargetCards().contains(ma.getHostCard())) {
                        // do not activate pump instants/sorceries targeting creatures by tapping targeted
                        // creatures for mana (for example, Servant of the Conduit)
                        continue;
                    }
                } else if (sa.getApi() == ApiType.Attach
                        && "AvoidPayingWithAttachTarget".equals(sa.getHostCard().getSVar("AIPaymentPreference"))) {
                    // For cards like Genju of the Cedars, make sure we're not attaching to the same land that will
                    // be tapped to pay its own cost if there's another untapped land like that available
                    if (ma.getHostCard().equals(sa.getTargetCard())) {
                        if (CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), Predicates.and(CardPredicates.nameEquals(ma.getHostCard().getName()), CardPredicates.Presets.UNTAPPED)).size() > 1) {
                            continue;
                        }
                    }
                }

            }

            SpellAbility paymentChoice = ma;

            // Exception: when paying generic mana with Cavern of Souls, prefer the colored mana producing ability
            // to attempt to make the spell uncounterable when possible.
            if (ComputerUtilAbility.getAbilitySourceName(ma).equals("Cavern of Souls")
                    && sa.getHostCard().getType().getCreatureTypes().contains(ma.getHostCard().getChosenType())) {
                if (toPay == ManaCostShard.COLORLESS && cost.getUnpaidShards().contains(ManaCostShard.GENERIC)) {
                    // Deprioritize Cavern of Souls, try to pay generic mana with it instead to use the NoCounter ability
                    continue;
                } else if (toPay == ManaCostShard.GENERIC || toPay == ManaCostShard.X) {
                    for (SpellAbility ab : saList) {
                        if (ab.isManaAbility() && ab.getManaPart().isAnyMana() && ab.hasParam("AddsNoCounter")) {
                            if (!ab.getHostCard().isTapped()) {
                                paymentChoice = ab;
                                break;
                            }
                        }
                    }
                }
            }

            final String typeRes = cost.getSourceRestriction();
            if (StringUtils.isNotBlank(typeRes) && !paymentChoice.getHostCard().getType().hasStringType(typeRes)) {
                continue;
            }

            if (canPayShardWithSpellAbility(toPay, ai, paymentChoice, sa, checkCosts)) {
                return paymentChoice;
            }
        }
        return null;
    }
    
    public static CardCollection getManaSourcesToPayCost(final ManaCostBeingPaid cost, final SpellAbility sa, final Player ai) {
        CardCollection manaSources = new CardCollection();

        adjustManaCostToAvoidNegEffects(cost, sa.getHostCard(), ai);
        List<Mana> manaSpentToPay = new ArrayList<>();

        List<ManaCostShard> unpaidShards = cost.getUnpaidShards();
        Collections.sort(unpaidShards); // most difficult shards must come first
        for (ManaCostShard part : unpaidShards) {
            if (part != ManaCostShard.X) {
                if (cost.isPaid()) {
                    continue;
                }

                // get a mana of this type from floating, bail if none available
                final Mana mana = getMana(ai, part, sa, cost.getSourceRestriction(), (byte) -1);
                if (mana != null) {
                    if (ai.getManaPool().tryPayCostWithMana(sa, cost, mana, false)) {
                        manaSpentToPay.add(0, mana);
                    }
                }
            }
        }

        if (cost.isPaid()) {
            // refund any mana taken from mana pool when test
            refundMana(manaSpentToPay, ai, sa);

            handleOfferingsAI(sa, true, cost.isPaid());
            return manaSources;
        }

        // arrange all mana abilities by color produced.
        final ListMultimap<Integer, SpellAbility> manaAbilityMap = ComputerUtilMana.groupSourcesByManaColor(ai, true);
        if (manaAbilityMap.isEmpty()) {
            refundMana(manaSpentToPay, ai, sa);

            handleOfferingsAI(sa, true, cost.isPaid());
            return manaSources;
        }

        // select which abilities may be used for each shard
        Multimap<ManaCostShard, SpellAbility> sourcesForShards = ComputerUtilMana.groupAndOrderToPayShards(ai, manaAbilityMap, cost);

        sortManaAbilities(sourcesForShards);

        ManaCostShard toPay;
        // Loop over mana needed
        while (!cost.isPaid()) {
            toPay = getNextShardToPay(cost);

            Collection<SpellAbility> saList = sourcesForShards.get(toPay);
            if (saList == null) {
                break;
            }

            SpellAbility saPayment = chooseManaAbility(cost, sa, ai, toPay, saList, true);
            if (saPayment == null) {
                boolean lifeInsteadOfBlack = toPay.isBlack() && ai.hasKeyword("PayLifeInsteadOf:B");
                if ((!toPay.isPhyrexian() && !lifeInsteadOfBlack) || !ai.canPayLife(2)) {
                    break; // cannot pay
                }

                if (toPay.isPhyrexian()) {
                    cost.payPhyrexian();
                } else if (lifeInsteadOfBlack) {
                    cost.decreaseShard(ManaCostShard.BLACK, 1);
                }

                continue;
            }

            manaSources.add(saPayment.getHostCard());
            setExpressColorChoice(sa, ai, cost, toPay, saPayment);

            String manaProduced = toPay.isSnow() ? "S" : GameActionUtil.generatedMana(saPayment);
            manaProduced = AbilityManaPart.applyManaReplacement(saPayment, manaProduced);
            //System.out.println(manaProduced);
            payMultipleMana(cost, manaProduced, ai);

            // remove from available lists
            /*
             * Refactoring this code to sourcesForShards.values().removeIf((SpellAbility srcSa) -> srcSa.getHostCard().equals(saPayment.getHostCard()));
             * causes Android build not to compile
             * */
            Iterator<SpellAbility> itSa = sourcesForShards.values().iterator();
            while (itSa.hasNext()) {
                SpellAbility srcSa = itSa.next();
                if (srcSa.getHostCard().equals(saPayment.getHostCard())) {
                    itSa.remove();
                }
            }
        }

        handleOfferingsAI(sa, true, cost.isPaid());

        refundMana(manaSpentToPay, ai, sa);
        
        return manaSources;
    } // getManaSourcesToPayCost()

    private static boolean payManaCost(final ManaCostBeingPaid cost, final SpellAbility sa, final Player ai, final boolean test, boolean checkPlayable) {
        adjustManaCostToAvoidNegEffects(cost, sa.getHostCard(), ai);
        List<Mana> manaSpentToPay = test ? new ArrayList<>() : sa.getPayingMana();
        boolean purePhyrexian = cost.containsOnlyPhyrexianMana();
        int testEnergyPool = ai.getCounters(CounterType.ENERGY);

        List<SpellAbility> paymentList = Lists.newArrayList();

        if (payManaCostFromPool(cost, sa, ai, test, manaSpentToPay)) {
            return true;	// paid all from floating mana
        }
        
        boolean hasConverge = sa.getHostCard().hasConverge();
        ListMultimap<ManaCostShard, SpellAbility> sourcesForShards = getSourcesForShards(cost, sa, ai, test,
				checkPlayable, manaSpentToPay, hasConverge);
        if (sourcesForShards == null && !purePhyrexian) {
        	return false;	// no mana abilities to use for paying
        }

        final ManaPool manapool = ai.getManaPool();
        ManaCostShard toPay = null;
        List<SpellAbility> saExcludeList = new ArrayList<>();

        // Loop over mana needed
        while (!cost.isPaid()) {
            toPay = getNextShardToPay(cost);

            boolean lifeInsteadOfBlack = toPay.isBlack() && ai.hasKeyword("PayLifeInsteadOf:B");

            Collection<SpellAbility> saList = null;
            if (hasConverge && 
            		(toPay == ManaCostShard.GENERIC || toPay == ManaCostShard.X)) {
            	final int unpaidColors = cost.getUnpaidColors() + cost.getColorsPaid() ^ ManaCostShard.COLORS_SUPERPOSITION;
            	for (final byte b : ColorSet.fromMask(unpaidColors)) {	// try and pay other colors for converge
            		final ManaCostShard shard = ManaCostShard.valueOf(b);
            		saList = sourcesForShards.get(shard);
            		if (saList != null && !saList.isEmpty()) {
            			toPay = shard;
            			break;
            		}
            	}
            	if (saList == null || saList.isEmpty()) {	// failed to converge, revert to paying generic
            		saList = sourcesForShards.get(toPay);
            		hasConverge = false;
            	}
            } else {
                if (!(sourcesForShards == null && purePhyrexian)) {
                    saList = sourcesForShards.get(toPay);
                } else {
                    saList = Lists.newArrayList(); // Phyrexian mana only: no valid mana sources, but can still pay life
                }
            }
            if (saList == null) {
                break;
            }

            saList.removeAll(saExcludeList);

            SpellAbility saPayment = saList.isEmpty() ? null : chooseManaAbility(cost, sa, ai, toPay, saList, checkPlayable || !test);

            if (saPayment != null && ComputerUtilCost.isSacrificeSelfCost(saPayment.getPayCosts())) {
                if (sa.getTargets() != null && sa.getTargets().isTargeting(saPayment.getHostCard())) {
                    saExcludeList.add(saPayment); // not a good idea to sac a card that you're targeting with the SA you're paying for
                    continue;
                }
            }

            if (saPayment != null && saPayment.hasParam("AILogic")) {
                boolean consider = false;
                if (saPayment.getParam("AILogic").equals("BlackLotus")) {
                    consider = SpecialCardAi.BlackLotus.consider(ai, sa, cost);
                    if (!consider) {
                        saExcludeList.add(saPayment); // since we checked this already, do not loop indefinitely checking again
                        continue;
                    }
                }
            }

            if (saPayment == null) {
                if ((!toPay.isPhyrexian() && !lifeInsteadOfBlack) || !ai.canPayLife(2)
                        || (ai.getLife() <= 2 && !ai.cantLoseForZeroOrLessLife())) {
                    break; // cannot pay
                }

                if (sa.hasParam("AIPhyrexianPayment")) {
                    if ("Never".equals(sa.getParam("AIPhyrexianPayment"))) {
                        break; // unwise to pay
                    } else if (sa.getParam("AIPhyrexianPayment").startsWith("OnFatalDamage.")) {
                        int dmg = Integer.parseInt(sa.getParam("AIPhyrexianPayment").substring(14));
                        if (ai.getOpponents().filter(PlayerPredicates.lifeLessOrEqualTo(dmg)).isEmpty()) {
                            break; // no one to finish with the gut shot
                        }
                    }
                }

                if (toPay.isPhyrexian()) {
                    cost.payPhyrexian();
                } else if (lifeInsteadOfBlack) {
                    cost.decreaseShard(ManaCostShard.BLACK, 1);
                }

                if (!test) {
                    ai.payLife(2, sa.getHostCard());
                }
                continue;
            }
            paymentList.add(saPayment);

            setExpressColorChoice(sa, ai, cost, toPay, saPayment);

            if (test) {
				// Check energy when testing
				CostPayEnergy energyCost = saPayment.getPayCosts().getCostEnergy();
				if (energyCost != null) {
					testEnergyPool -= Integer.parseInt(energyCost.getAmount());
					if (testEnergyPool < 0) {
						// Can't pay energy cost
						break;
					}
				}

                String manaProduced = toPay.isSnow() ? "S" : GameActionUtil.generatedMana(saPayment);
                manaProduced = AbilityManaPart.applyManaReplacement(saPayment, manaProduced);
                //System.out.println(manaProduced);
                payMultipleMana(cost, manaProduced, ai);

                // remove from available lists
                /*
                 * Refactoring this code to sourcesForShards.values().removeIf((SpellAbility srcSa) -> srcSa.getHostCard().equals(saPayment.getHostCard()));
                 * causes Android build not to compile
                 * */
                Iterator<SpellAbility> itSa = sourcesForShards.values().iterator();
                while (itSa.hasNext()) {
                    SpellAbility srcSa = itSa.next();
                    if (srcSa.getHostCard().equals(saPayment.getHostCard())) {
                        itSa.remove();
                    }
                }
            }
            else {
                final CostPayment pay = new CostPayment(saPayment.getPayCosts(), saPayment);
                if (!pay.payComputerCosts(new AiCostDecision(ai, saPayment))) {
                    saList.remove(saPayment);
                    continue;
                }

                ai.getGame().getStack().addAndUnfreeze(saPayment);
                // subtract mana from mana pool
                manapool.payManaFromAbility(sa, cost, saPayment);

                // no need to remove abilities from resource map,
                // once their costs are paid and consume resources, they can not be used again
                
                if (hasConverge) {	// hack to prevent converge re-using sources
                	// remove from available lists
                    /*
                    * Refactoring this code to sourcesForShards.values().removeIf((SpellAbility srcSa) -> srcSa.getHostCard().equals(saPayment.getHostCard()));
                    * causes Android build not to compile
                    * */
	                Iterator<SpellAbility> itSa = sourcesForShards.values().iterator();
	                while (itSa.hasNext()) {
	                    SpellAbility srcSa = itSa.next();
	                    if (srcSa.getHostCard().equals(saPayment.getHostCard())) {
	                        itSa.remove();
	                    }
	                }
                }
            }
        }

        handleOfferingsAI(sa, test, cost.isPaid());

//        if (DEBUG_MANA_PAYMENT) {
//            System.err.printf("%s > [%s] payment has %s (%s +%d) for (%s) %s:%n\t%s%n%n",
//                    FThreads.debugGetCurrThreadId(), test ? "test" : "PROD", cost.isPaid() ? "*PAID*" : "failed", originalCost,
//                    extraMana, sa.getHostCard(), sa.toUnsuppressedString(), StringUtils.join(paymentPlan, "\n\t"));
//        }

        // See if it's possible to pay with something that was left in the mana pool in corner cases,
        // e.g. Gemstone Caverns with a Luck counter on it generating colored mana (which fails to be
        // processed correctly on a per-ability basis, leaving floating mana in the pool)
        if (!cost.isPaid() && !manapool.isEmpty()) {
            for (byte color : MagicColor.WUBRGC) {
                manapool.tryPayCostWithColor(color, sa, cost);
            }
        }

        // The cost is still unpaid, so refund the mana and report
        if (!cost.isPaid()) {
            refundMana(manaSpentToPay, ai, sa);
            if (test) {
                resetPayment(paymentList);
                return false;
            }
            else {
                System.out.println("ComputerUtil : payManaCost() cost was not paid for " + sa.getHostCard().getName() + ". Didn't find what to pay for " + toPay);
                return false;
            }
        }

        // Note: manaSpentToPay shouldn't be cleared here, since it needs to remain
        // on the SpellAbility in order for effects that check mana spent cost to work.

        sa.getHostCard().setColorsPaid(cost.getColorsPaid());
        // if (sa instanceof Spell_Permanent) // should probably add this
        sa.getHostCard().setSunburstValue(cost.getSunburst());

        if (test) {
            refundMana(manaSpentToPay, ai, sa);
            resetPayment(paymentList);
        }

        return true;
    } // payManaCost()

    private static void resetPayment(List<SpellAbility> payments) {
        for(SpellAbility sa : payments) {
            sa.getManaPart().clearExpressChoice();
        }
    }


	/**
	 * Creates a mapping between the required mana shards and the available spell abilities to pay for them
	 */
	private static ListMultimap<ManaCostShard, SpellAbility> getSourcesForShards(final ManaCostBeingPaid cost,
			final SpellAbility sa, final Player ai, final boolean test, final boolean checkPlayable,
			List<Mana> manaSpentToPay, final boolean hasConverge) {
		// arrange all mana abilities by color produced.
        final ListMultimap<Integer, SpellAbility> manaAbilityMap = ComputerUtilMana.groupSourcesByManaColor(ai, checkPlayable);
        if (manaAbilityMap.isEmpty()) {	// no mana abilities, bailing out
            refundMana(manaSpentToPay, ai, sa);
            handleOfferingsAI(sa, test, cost.isPaid());
            return null;
        }
        if (DEBUG_MANA_PAYMENT) {
            System.out.println("DEBUG_MANA_PAYMENT: manaAbilityMap = " + manaAbilityMap);
        }

        // select which abilities may be used for each shard
        ListMultimap<ManaCostShard, SpellAbility> sourcesForShards = ComputerUtilMana.groupAndOrderToPayShards(ai, manaAbilityMap, cost);
        if (hasConverge) {	// add extra colors for paying converge
        	final int unpaidColors = cost.getUnpaidColors() + cost.getColorsPaid() ^ ManaCostShard.COLORS_SUPERPOSITION;
        	for (final byte b : ColorSet.fromMask(unpaidColors)) {
        		final ManaCostShard shard = ManaCostShard.valueOf(b);
        		if (!sourcesForShards.containsKey(shard)) {
        			if (ai.getManaPool().canPayForShardWithColor(shard, b)) {
                        for (SpellAbility saMana : manaAbilityMap.get((int)b)) {
                        	sourcesForShards.get(shard).add(sourcesForShards.get(shard).size(), saMana);
                        }
                    }
        		}
        	}
        }
        sortManaAbilities(sourcesForShards);
        if (DEBUG_MANA_PAYMENT) {
            System.out.println("DEBUG_MANA_PAYMENT: sourcesForShards = " + sourcesForShards);
        }
		return sourcesForShards;
	}

    /**
     * Checks if the given mana cost can be paid from floating mana.
     * @param cost mana cost to pay for
     * @param sa ability to pay for
     * @param ai activating player
     * @param test actual payment is made if this is false
     * @param manaSpentToPay list of mana spent
     * @return whether the floating mana is sufficient to pay the cost fully
     */
    private static boolean payManaCostFromPool(final ManaCostBeingPaid cost, final SpellAbility sa, final Player ai, 
            final boolean test, List<Mana> manaSpentToPay) {
    	final boolean hasConverge = sa.getHostCard().hasConverge();
        List<ManaCostShard> unpaidShards = cost.getUnpaidShards();
        Collections.sort(unpaidShards); // most difficult shards must come first
        for (ManaCostShard part : unpaidShards) {
            if (part != ManaCostShard.X) {
                if (cost.isPaid()) {
                    continue;
                }

                // get a mana of this type from floating, bail if none available
                final Mana mana = getMana(ai, part, sa, cost.getSourceRestriction(), hasConverge ? cost.getColorsPaid() : -1);
                if (mana != null) {
                    if (ai.getManaPool().tryPayCostWithMana(sa, cost, mana, test)) {
                        manaSpentToPay.add(0, mana);
                    }
                }
            }
        }

        if (cost.isPaid()) {
            // refund any mana taken from mana pool when test
            if (test) {
                refundMana(manaSpentToPay, ai, sa);
            }
            handleOfferingsAI(sa, test, cost.isPaid());
            return true;
        }
        return false;
    }

    /**
     * <p>
     * getManaFrom.
     * </p>
     *
     * @param saBeingPaidFor
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @return a {@link forge.game.mana.Mana} object.
     */
    private static Mana getMana(final Player ai, final ManaCostShard shard, final SpellAbility saBeingPaidFor, String restriction, final byte colorsPaid) {
        final List<Pair<Mana, Integer>> weightedOptions = selectManaToPayFor(ai.getManaPool(), shard, saBeingPaidFor, restriction, colorsPaid);

        // Exclude border case
        if (weightedOptions.isEmpty()) {
            return null; // There is no matching mana in the pool
        }

        // select equal weight possibilities
        List<Mana> manaChoices = new ArrayList<>();
        int bestWeight = Integer.MIN_VALUE;
        for (Pair<Mana, Integer> option : weightedOptions) {
            int thisWeight = option.getRight();
            Mana thisMana = option.getLeft();

            if (thisWeight > bestWeight) {
                manaChoices.clear();
                bestWeight = thisWeight;
            }

            if (thisWeight == bestWeight) {
                // add only distinct Mana-s
                boolean haveDuplicate = false;
                for (Mana m : manaChoices) {
                    if (m.equals(thisMana)) {
                        haveDuplicate = true;
                        break;
                    }
                }
                if (!haveDuplicate) {
                    manaChoices.add(thisMana);
                }
            }
        }

        // got an only one best option?
        if (manaChoices.size() == 1) {
            return manaChoices.get(0);
        }

        // if we are simulating mana payment for the human controller, use the first mana available (and avoid prompting the human player)
        if (!(ai.getController() instanceof PlayerControllerAi)) {
            return manaChoices.get(0); 
        }

        // Let them choose then
        return ai.getController().chooseManaFromPool(manaChoices);
    }

	private static List<Pair<Mana, Integer>> selectManaToPayFor(final ManaPool manapool, final ManaCostShard shard,
			final SpellAbility saBeingPaidFor, String restriction, final byte colorsPaid) {
        final List<Pair<Mana, Integer>> weightedOptions = new ArrayList<>();
        for (final Mana thisMana : manapool) {
            if (!manapool.canPayForShardWithColor(shard, thisMana.getColor())) {
                continue;
            }

            if (thisMana.getManaAbility() != null && !thisMana.getManaAbility().meetsSpellAndShardRestrictions(saBeingPaidFor, shard, thisMana.getColor())) {
                continue;
            }

            boolean canPay = manapool.canPayForShardWithColor(shard, thisMana.getColor());
            if (!canPay || (shard.isSnow() && !thisMana.isSnow())) {
                continue;
            }

            if (StringUtils.isNotBlank(restriction) && !thisMana.getSourceCard().getType().hasStringType(restriction)) {
                continue;
            }

            int weight = 0;
            if (colorsPaid == -1) {
            	// prefer colorless mana to spend
            	weight += thisMana.isColorless() ? 5 : 0;
            } else {
            	// get more colors for converge
            	weight += (thisMana.getColor() | colorsPaid) != colorsPaid ? 5 : 0;
            }

            // prefer restricted mana to spend
            if (thisMana.isRestricted()) {
                weight += 2;
            }

            // Spend non-snow mana first
            if (!thisMana.isSnow()) {
                weight += 1;
            }

            weightedOptions.add(Pair.of(thisMana, weight));
        }
        return weightedOptions;
    }
    
    private static void setExpressColorChoice(final SpellAbility sa, final Player ai, ManaCostBeingPaid cost,
            ManaCostShard toPay, SpellAbility saPayment) {

        AbilityManaPart m = saPayment.getManaPart();
        if (m.isComboMana()) {
            getComboManaChoice(ai, saPayment, sa, cost);
        }
        else if (saPayment.getApi() == ApiType.ManaReflected) {
            //System.out.println("Evaluate reflected mana of: " + saPayment.getHostCard());
            Set<String> reflected = CardUtil.getReflectableManaColors(saPayment);

            for (byte c : MagicColor.WUBRG) {
                if (ai.getManaPool().canPayForShardWithColor(toPay, c) && reflected.contains(MagicColor.toLongString(c))) {
                    m.setExpressChoice(MagicColor.toShortString(c));
                    return;
                }
            }
        }
        else if (m.isAnyMana()) {
            byte colorChoice = 0;
            if (toPay.isOr2Generic())
                colorChoice = toPay.getColorMask();
            else {
                for (byte c : MagicColor.WUBRG) {
                    if (ai.getManaPool().canPayForShardWithColor(toPay, c)) {
                        colorChoice = c;
                        break;
                    }
                }
            }
            m.setExpressChoice(MagicColor.toShortString(colorChoice));
        }
    }

    private static boolean canPayShardWithSpellAbility(ManaCostShard toPay, Player ai, SpellAbility ma, SpellAbility sa, boolean checkCosts) {
        final Card sourceCard = ma.getHostCard();

        if (isManaSourceReserved(ai, sourceCard, sa)) {
            return false;
        }
        
        if (toPay.isSnow() && !sourceCard.isSnow()) {
            return false;
        }

        AbilityManaPart m = ma.getManaPart();
        if (!m.meetsManaRestrictions(sa)) {
            return false;
        }

        if (ma.hasParam("ActivationLimit")) {
            if (ma.getActivationsThisTurn() >= AbilityUtils.calculateAmount(sourceCard, ma.getParam("ActivationLimit"), ma)) {
                return false;
            }
        }

        if (checkCosts) {
            // Check if AI can still play this mana ability
            ma.setActivatingPlayer(ai);
            // if the AI can't pay the additional costs skip the mana ability
            if (!CostPayment.canPayAdditionalCosts(ma.getPayCosts(), ma)) {
                return false;
            }
            else if (sourceCard.isTapped()) {
                return false;
            } else if (ma.getRestrictions() != null && ma.getRestrictions().isInstantSpeed()) {
                return false;
            }
        }

        if (m.isComboMana()) {
            for (String s : m.getComboColors().split(" ")) {
                if ("Any".equals(s) || ai.getManaPool().canPayForShardWithColor(toPay, ManaAtom.fromName(s)))
                    return true;
            }
            return false;
        }

        if (ma.getApi() == ApiType.ManaReflected) {
            Set<String> reflected = CardUtil.getReflectableManaColors(ma);

            for (byte c : MagicColor.WUBRG) {
                if (ai.getManaPool().canPayForShardWithColor(toPay, c) && reflected.contains(MagicColor.toLongString(c))) {
                    m.setExpressChoice(MagicColor.toShortString(c));
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    // isManaSourceReserved returns true if sourceCard is reserved as a mana source for payment
    // for the future spell to be cast in another phase. However, if "sa" (the spell ability that is
    // being considered for casting) is high priority, then mana source reservation will be
    // ignored.
    private static boolean isManaSourceReserved(Player ai, Card sourceCard, SpellAbility sa) {
        if (sa == null) {
            return false;
        }
        if (!(ai.getController() instanceof PlayerControllerAi)) {
            return false;
        }

        AiController aic = ((PlayerControllerAi)ai.getController()).getAi();
        int chanceToReserve = aic.getIntProperty(AiProps.RESERVE_MANA_FOR_MAIN2_CHANCE);

        // Mana reserved for spell synchronization
        if (AiCardMemory.isRememberedCard(ai, sourceCard, AiCardMemory.MemorySet.HELD_MANA_SOURCES_FOR_NEXT_SPELL)) {
            return true;
        }

        PhaseType curPhase = ai.getGame().getPhaseHandler().getPhase();

        // For combat tricks, always obey mana reservation
        if (curPhase == PhaseType.COMBAT_DECLARE_BLOCKERS || curPhase == PhaseType.CLEANUP) {
            if (!(ai.getGame().getPhaseHandler().isPlayerTurn(ai))) {
                AiCardMemory.clearMemorySet(ai, AiCardMemory.MemorySet.HELD_MANA_SOURCES_FOR_ENEMY_DECLBLK);
                AiCardMemory.clearMemorySet(ai, AiCardMemory.MemorySet.CHOSEN_FOG_EFFECT);
            }
            else
                AiCardMemory.clearMemorySet(ai, AiCardMemory.MemorySet.HELD_MANA_SOURCES_FOR_DECLBLK);
        } else {
            if ((AiCardMemory.isRememberedCard(ai, sourceCard, AiCardMemory.MemorySet.HELD_MANA_SOURCES_FOR_DECLBLK)) ||
                    (AiCardMemory.isRememberedCard(ai, sourceCard, AiCardMemory.MemorySet.HELD_MANA_SOURCES_FOR_ENEMY_DECLBLK))) {
                // This mana source is held elsewhere for a combat trick.
                return true;
            }
        }

        // If it's a low priority spell (it's explicitly marked so elsewhere in the AI with a SVar), always
        // obey mana reservations for Main 2; otherwise, obey mana reservations depending on the "chance to reserve"
        // AI profile variable.
        if (sa.getSVar("LowPriorityAI").equals("")) {
            if (chanceToReserve == 0 || MyRandom.getRandom().nextInt(100) >= chanceToReserve) {
                return false;
            }
        }

        if (curPhase == PhaseType.MAIN2 || curPhase == PhaseType.CLEANUP) {
            AiCardMemory.clearMemorySet(ai, AiCardMemory.MemorySet.HELD_MANA_SOURCES_FOR_MAIN2);
        }
        else {
            if (AiCardMemory.isRememberedCard(ai, sourceCard, AiCardMemory.MemorySet.HELD_MANA_SOURCES_FOR_MAIN2)) {
                // This mana source is held elsewhere for a Main Phase 2 spell.
                return true;
            }
        }

        return false;
    }


    private static ManaCostShard getNextShardToPay(ManaCostBeingPaid cost) {
        // mind the priorities
        // * Pay mono-colored first,curPhase == PhaseType.CLEANUP
        // * Pay 2/C with matching colors
        // * pay hybrids
        // * pay phyrexian, keep mana for colorless
        // * pay generic
        return cost.getShardToPayByPriority(cost.getDistinctShards(), ColorSet.ALL_COLORS.getColor());
    }

    private static void adjustManaCostToAvoidNegEffects(ManaCostBeingPaid cost, final Card card, Player ai) {
        // Make mana needed to avoid negative effect a mandatory cost for the AI
        for (String manaPart : card.getSVar("ManaNeededToAvoidNegativeEffect").split(",")) {
            // convert long color strings to short color strings
            if (manaPart.isEmpty()) {
                continue;
            }

            byte mask = ManaAtom.fromName(manaPart);

            // make mana mandatory for AI
            if (!cost.needsColor(mask, ai.getManaPool()) && cost.getGenericManaAmount() > 0) {
                ManaCostShard shard = ManaCostShard.valueOf(mask);
                cost.increaseShard(shard, 1);
                cost.decreaseGenericMana(1);
            }
        }
    }

    /**
     * <p>
     * getComboManaChoice.
     * </p>
     * 
     * @param manaAb
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param saRoot
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param cost
     *            a {@link forge.game.mana.ManaCostBeingPaid} object.
     */
    private static void getComboManaChoice(final Player ai, final SpellAbility manaAb, final SpellAbility saRoot, final ManaCostBeingPaid cost) {
        final StringBuilder choiceString = new StringBuilder();
        final Card source = manaAb.getHostCard();
        final AbilityManaPart abMana = manaAb.getManaPart();

        if (abMana.isComboMana()) {
            int amount = manaAb.hasParam("Amount") ? AbilityUtils.calculateAmount(source, manaAb.getParam("Amount"), saRoot) : 1;
            final ManaCostBeingPaid testCost = new ManaCostBeingPaid(cost);
            final String[] comboColors = abMana.getComboColors().split(" ");
            for (int nMana = 1; nMana <= amount; nMana++) {
                String choice = "";
                // Use expressChoice first
                if (!abMana.getExpressChoice().isEmpty()) {
                    choice = abMana.getExpressChoice();
                    abMana.clearExpressChoice();
                    byte colorMask = ManaAtom.fromName(choice);
                    if (abMana.canProduce(choice, manaAb) && testCost.isAnyPartPayableWith(colorMask, ai.getManaPool())) {
                        choiceString.append(choice);
                        payMultipleMana(testCost, choice, ai);
                        continue;
                    }
                }
                // check colors needed for cost
                if (!testCost.isPaid()) {
                    // Loop over combo colors
                    for (String color : comboColors) {
                        if (testCost.isAnyPartPayableWith(ManaAtom.fromName(color), ai.getManaPool())) {
                            payMultipleMana(testCost, color, ai);
                            if (nMana != 1) {
                                choiceString.append(" ");
                            }
                            choiceString.append(color);
                            choice = color;
                            break;
                        }
                    }
                    if (!choice.isEmpty()) {
                        continue;
                    }
                }
                // check if combo mana can produce most common color in hand
                String commonColor = ComputerUtilCard.getMostProminentColor(ai.getCardsIn(
                        ZoneType.Hand));
                if (!commonColor.isEmpty() && abMana.getComboColors().contains(MagicColor.toShortString(commonColor))) {
                    choice = MagicColor.toShortString(commonColor);
                }
                else {
                    // default to first color
                    choice = comboColors[0];
                }
                if (nMana != 1) {
                    choiceString.append(" ");
                }
                choiceString.append(choice);
            }
        }
        if (choiceString.toString().isEmpty()) {
            choiceString.append("0");
        }

        abMana.setExpressChoice(choiceString.toString());
    }

    /**
     * <p>
     * payMultipleMana.
     * </p>
     * @param mana
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    private static String payMultipleMana(ManaCostBeingPaid testCost, String mana, final Player p) {
        List<String> unused = new ArrayList<>(4);
        for (String manaPart : TextUtil.split(mana, ' ')) {
            if (StringUtils.isNumeric(manaPart)) {
                for (int i = Integer.parseInt(manaPart); i > 0; i--) {
                    boolean wasNeeded = testCost.ai_payMana("1", p.getManaPool());
                    if (!wasNeeded) {
                        unused.add(Integer.toString(i));
                        break;
                    }
                }
            }
            else {
                String color = MagicColor.toShortString(manaPart);
                boolean wasNeeded = testCost.ai_payMana(color, p.getManaPool());
                if (!wasNeeded) {
                    unused.add(color);
                }
            }
        }
        return unused.isEmpty() ? null : StringUtils.join(unused, ' ');
    }
    
    /**
     * Find all mana sources.
     * @param manaAbilityMap The map of SpellAbilities that produce mana.
     * @return Were all mana sources found?
     */
    private static ListMultimap<ManaCostShard, SpellAbility> groupAndOrderToPayShards(final Player ai, final ListMultimap<Integer, SpellAbility> manaAbilityMap,
            final ManaCostBeingPaid cost) {
        ListMultimap<ManaCostShard, SpellAbility> res = ArrayListMultimap.create();

        if (cost.getGenericManaAmount() > 0 && manaAbilityMap.containsKey(ManaAtom.GENERIC)) {
            res.putAll(ManaCostShard.GENERIC, manaAbilityMap.get(ManaAtom.GENERIC));
        }

        // loop over cost parts
        for (ManaCostShard shard : cost.getDistinctShards()) {
            if (DEBUG_MANA_PAYMENT) {
                System.out.println("DEBUG_MANA_PAYMENT: shard = " + shard);
            }
            if (shard == ManaCostShard.S) {
                res.putAll(shard, manaAbilityMap.get(ManaAtom.IS_SNOW));
                continue;
            }

            if (shard.isOr2Generic()) {
                Integer colorKey = (int) shard.getColorMask();
                if (manaAbilityMap.containsKey(colorKey))
                    res.putAll(shard, manaAbilityMap.get(colorKey));
                if (manaAbilityMap.containsKey(ManaAtom.GENERIC))
                    res.putAll(shard, manaAbilityMap.get(ManaAtom.GENERIC));
                continue;
            }
            
            if (shard == ManaCostShard.GENERIC) {
                continue;
            }

            for (Integer colorint : manaAbilityMap.keySet()) {
                // apply mana color change matrix here
                if (ai.getManaPool().canPayForShardWithColor(shard, colorint.byteValue())) {
                    for (SpellAbility sa : manaAbilityMap.get(colorint)) {
                        if (!res.get(shard).contains(sa)) {
                            res.get(shard).add(sa);
                        }
                    }
                }
            }
        }

        return res;
    }

    /**
     * Calculate the ManaCost for the given SpellAbility.
     * @param sa The SpellAbility to calculate for.
     * @param test test
     * @param extraMana extraMana
     * @return ManaCost
     */
    static ManaCostBeingPaid calculateManaCost(final SpellAbility sa, final boolean test, final int extraMana) {
    	Card card = sa.getHostCard();
        ZoneType castFromBackup = null;
        if (test && sa.isSpell()) {
            castFromBackup = card.getCastFrom();
            sa.getHostCard().setCastFrom(card.getZone() != null ? card.getZone().getZoneType() : null);
        }

        Cost payCosts = CostAdjustment.adjust(sa.getPayCosts(), sa);
        CostPartMana manapart = payCosts != null ? payCosts.getCostMana() : null;
        final ManaCost mana = payCosts != null ? ( manapart == null ? ManaCost.ZERO : manapart.getManaCostFor(sa) ) : ManaCost.NO_COST;

        String restriction = null;
        if (payCosts != null && payCosts.getCostMana() != null) {
            restriction = payCosts.getCostMana().getRestiction();
        }
        ManaCostBeingPaid cost = new ManaCostBeingPaid(mana, restriction);

        // Tack xMana Payments into mana here if X is a set value
        if (cost.getXcounter() > 0 || extraMana > 0) {
            int manaToAdd = 0;
            if (test && extraMana > 0) {
                final int multiplicator = Math.max(cost.getXcounter(), 1);
                manaToAdd = extraMana * multiplicator;
            } else {
                // For Count$xPaid set PayX in the AFs then use that here
                // Else calculate it as appropriate.
                final String xSvar = card.getSVar("X").startsWith("Count$xPaid") ? "PayX" : "X";
                if (!sa.getSVar(xSvar).isEmpty() || card.hasSVar(xSvar) || card.getState(CardStateName.Original).hasSVar(xSvar)) {
                    if (xSvar.equals("PayX") && (card.hasSVar(xSvar) || card.getState(CardStateName.Original).hasSVar(xSvar))) {
                         // X SVar may end up being an empty string when copying a spell with no cost (e.g. Jhoira Avatar)
                        String xValue = card.hasSVar(xSvar) ? card.getSVar(xSvar) : card.getState(CardStateName.Original).getSVar(xSvar);
                        manaToAdd = xValue.isEmpty() ? 0 : Integer.parseInt(xValue) * cost.getXcounter(); // X
                    } else {
                        manaToAdd = AbilityUtils.calculateAmount(card, xSvar, sa) * cost.getXcounter();
                    }
                }
            }

            String manaXColor = sa.getParam("XColor");
            ManaCostShard shardToGrow = ManaCostShard.parseNonGeneric(manaXColor == null ? "1" : manaXColor);
            cost.increaseShard(shardToGrow, manaToAdd);

            if (!test) {
                sa.setXManaCostPaid(manaToAdd / cost.getXcounter());
            }
        }
        
        CostAdjustment.adjust(cost, sa, null, test);

        int timesMultikicked = card.getKickerMagnitude();
        if (timesMultikicked > 0 && sa.hasParam("Announce") && sa.getParam("Announce").startsWith("Multikicker")) {
            ManaCost mkCost = sa.getMultiKickerManaCost();
            for (int i = 0; i < timesMultikicked; i++) {
            	cost.addManaCost(mkCost);
            }
            sa.setSVar("Multikicker", String.valueOf(timesMultikicked));
        }

        if (test && sa.isSpell()) {
            sa.getHostCard().setCastFrom(castFromBackup);
        }

        return cost;
    }

    // This method can be used to estimate the total amount of mana available to the player,
    // including the mana available in that player's mana pool
    public static int getAvailableManaEstimate(final Player p) {
        return getAvailableManaEstimate(p, true);
    }

    public static int getAvailableManaEstimate(final Player p, final boolean checkPlayable) {
        int availableMana = 0;

        final CardCollectionView list = new CardCollection(p.getCardsIn(ZoneType.Battlefield));
        final List<Card> srcs = CardLists.filter(list, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return !c.getManaAbilities().isEmpty();
            }
        });

        int maxProduced = 0;
        int producedWithCost = 0;
        boolean hasSourcesWithNoManaCost = false;

        for (Card src : srcs) {
            maxProduced = 0;

            for (SpellAbility ma : src.getManaAbilities()) {
                ma.setActivatingPlayer(p);
                if (!checkPlayable || ma.canPlay()) {
                    int costsToActivate = ma.getPayCosts().getCostMana() != null ? ma.getPayCosts().getCostMana().convertAmount() : 0;
                    int producedMana = ma.getParamOrDefault("Produced", "").split(" ").length;
                    int producedAmount = AbilityUtils.calculateAmount(src, ma.getParamOrDefault("Amount", "1"), ma);

                    int producedTotal = producedMana * producedAmount - costsToActivate;

                    if (costsToActivate > 0) {
                        producedWithCost += producedTotal;
                    } else if (!hasSourcesWithNoManaCost) {
                        hasSourcesWithNoManaCost = true;
                    }

                    if (producedTotal > maxProduced) {
                        maxProduced = producedTotal;
                    }
                }
            }

            availableMana += maxProduced;
        }

        availableMana += p.getManaPool().totalMana();

        if (producedWithCost > 0 && !hasSourcesWithNoManaCost) {
            availableMana -= producedWithCost; // probably can't activate them, no other mana available
        }

        return availableMana;
    }

    //This method is currently used by AI to estimate available mana
    public static CardCollection getAvailableManaSources(final Player ai, final boolean checkPlayable) {
        final CardCollectionView list = CardCollection.combine(ai.getCardsIn(ZoneType.Battlefield), ai.getCardsIn(ZoneType.Hand));
        final List<Card> manaSources = CardLists.filter(list, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                for (final SpellAbility am : getAIPlayableMana(c)) {
                    am.setActivatingPlayer(ai);
                    if (!checkPlayable || am.canPlay()) {
                        return true;
                    }
                }
                return false;
            }
        }); // CardListFilter

        final CardCollection sortedManaSources = new CardCollection();
        final CardCollection otherManaSources = new CardCollection();
        final CardCollection colorlessManaSources = new CardCollection();
        final CardCollection oneManaSources = new CardCollection();
        final CardCollection twoManaSources = new CardCollection();
        final CardCollection threeManaSources = new CardCollection();
        final CardCollection fourManaSources = new CardCollection();
        final CardCollection fiveManaSources = new CardCollection();
        final CardCollection anyColorManaSources = new CardCollection();

        // Sort mana sources
        // 1. Use lands that can only produce colorless mana without
        // drawback/cost first
        // 2. Search for mana sources that have a certain number of abilities
        // 3. Use lands that produce any color many
        // 4. all other sources (creature, costs, drawback, etc.)
        for (Card card : manaSources) {
            if (card.isCreature() || card.isEnchanted()) {
                otherManaSources.add(card);
                continue; // don't use creatures before other permanents
            }

            int usableManaAbilities = 0;
            boolean needsLimitedResources = false;
            boolean producesAnyColor = false;
            final List<SpellAbility> manaAbilities = getAIPlayableMana(card);

            for (final SpellAbility m : manaAbilities) {
                if (m.getManaPart().isAnyMana()) {
                    producesAnyColor = true;
                }

                final Cost cost = m.getPayCosts();
                if (cost != null) {
                    needsLimitedResources |= !cost.isReusuableResource();
                }

                // if the AI can't pay the additional costs skip the mana ability
                if (cost != null) {
                    m.setActivatingPlayer(ai);
                    if (!CostPayment.canPayAdditionalCosts(m.getPayCosts(), m)) {
                        continue;
                    }
                }

                // don't use abilities with dangerous drawbacks
                AbilitySub sub = m.getSubAbility();
                if (sub != null && !card.getName().equals("Pristine Talisman") && !card.getName().equals("Zhur-Taa Druid")) {
                    if (!SpellApiToAi.Converter.get(sub.getApi()).chkDrawbackWithSubs(ai, sub)) {
                        continue;
                    }
                    needsLimitedResources = true; // TODO: check for good drawbacks (gainLife)
                }
                usableManaAbilities++;
            }

            if (needsLimitedResources) {
                otherManaSources.add(card);
            } else if (producesAnyColor) {
                anyColorManaSources.add(card);
            } else if (usableManaAbilities == 1) {
                if (manaAbilities.get(0).getManaPart().mana().equals("C")) {
                    colorlessManaSources.add(card);
                } else {
                    oneManaSources.add(card);
                }
            } else if (usableManaAbilities == 2) {
                twoManaSources.add(card);
            } else if (usableManaAbilities == 3) {
                threeManaSources.add(card);
            } else if (usableManaAbilities == 4) {
                fourManaSources.add(card);
            } else {
                fiveManaSources.add(card);
            }
        }
        sortedManaSources.addAll(sortedManaSources.size(), colorlessManaSources);
        sortedManaSources.addAll(sortedManaSources.size(), oneManaSources);
        sortedManaSources.addAll(sortedManaSources.size(), twoManaSources);
        sortedManaSources.addAll(sortedManaSources.size(), threeManaSources);
        sortedManaSources.addAll(sortedManaSources.size(), fourManaSources);
        sortedManaSources.addAll(sortedManaSources.size(), fiveManaSources);
        sortedManaSources.addAll(sortedManaSources.size(), anyColorManaSources);
        //use better creatures later
        ComputerUtilCard.sortByEvaluateCreature(otherManaSources);
        Collections.reverse(otherManaSources);
        sortedManaSources.addAll(sortedManaSources.size(), otherManaSources);

        if (DEBUG_MANA_PAYMENT) {
            System.out.println("DEBUG_MANA_PAYMENT: sortedManaSources = " + sortedManaSources);
        }
        return sortedManaSources;
    } // getAvailableManaSources()

    //This method is currently used by AI to estimate mana available
    private static ListMultimap<Integer, SpellAbility> groupSourcesByManaColor(final Player ai, boolean checkPlayable) {
        final ListMultimap<Integer, SpellAbility> manaMap = ArrayListMultimap.create();
        final Game game = ai.getGame();

        List<ReplacementEffect> replacementEffects = new ArrayList<>();
        for (final Player p : game.getPlayers()) {
            for (final Card crd : p.getAllCards()) {
                for (final ReplacementEffect replacementEffect : crd.getReplacementEffects()) {
                    if (replacementEffect.requirementsCheck(game)
                            && replacementEffect.getMode() == ReplacementType.ProduceMana
                            && replacementEffect.hasParam("ManaReplacement")
                            && replacementEffect.zonesCheck(game.getZoneOf(crd))) {
                        replacementEffects.add(replacementEffect);
                    }
                }
            }
        }

        // Loop over all current available mana sources
        for (final Card sourceCard : getAvailableManaSources(ai, checkPlayable)) {
            if (DEBUG_MANA_PAYMENT) {
                System.out.println("DEBUG_MANA_PAYMENT: groupSourcesByManaColor sourceCard = " + sourceCard);
            }
            for (final SpellAbility m : getAIPlayableMana(sourceCard)) {
                if (DEBUG_MANA_PAYMENT) {
                    System.out.println("DEBUG_MANA_PAYMENT: groupSourcesByManaColor m = " + m);
                }
                m.setActivatingPlayer(ai);
                if (checkPlayable && !m.canPlay()) {
                    continue;
                }

                // don't kill yourself
                final Cost abCost = m.getPayCosts();
                if (!ComputerUtilCost.checkLifeCost(ai, abCost, sourceCard, 1, m)) {
                    continue;
                }

                // don't use abilities with dangerous drawbacks
                AbilitySub sub = m.getSubAbility();
                if (sub != null) {
                    if (!SpellApiToAi.Converter.get(sub.getApi()).chkDrawbackWithSubs(ai, sub)) {
                        continue;
                    }
                }

                manaMap.get(ManaAtom.GENERIC).add(m); // add to generic source list
                AbilityManaPart mp = m.getManaPart();

                // setup produce mana replacement effects
                final Map<AbilityKey, Object> repParams = AbilityKey.newMap();
                repParams.put(AbilityKey.Mana, mp.getOrigProduced());
                repParams.put(AbilityKey.Affected, sourceCard);
                repParams.put(AbilityKey.Player, ai);
                repParams.put(AbilityKey.AbilityMana, m);

                for (final ReplacementEffect replacementEffect : replacementEffects) {
                    if (replacementEffect.canReplace(repParams)) {
                        Card crd = replacementEffect.getHostCard();
                        String repType = crd.getSVar(replacementEffect.getParam("ManaReplacement"));
                        if (repType.contains("Chosen")) {
                            repType = TextUtil.fastReplace(repType, "Chosen", MagicColor.toShortString(crd.getChosenColor()));
                        }
                        mp.setManaReplaceType(repType);
                    }
                }

                Set<String> reflectedColors = CardUtil.getReflectableManaColors(m);
                // find possible colors
                if (mp.canProduce("W", m) || reflectedColors.contains(MagicColor.Constant.WHITE)) {
                    manaMap.get(ManaAtom.WHITE).add(m);
                }
                if (mp.canProduce("U", m) || reflectedColors.contains(MagicColor.Constant.BLUE)) {
                    manaMap.get(ManaAtom.BLUE).add(m);
                }
                if (mp.canProduce("B", m) || reflectedColors.contains(MagicColor.Constant.BLACK)) {
                    manaMap.get(ManaAtom.BLACK).add(m);
                }
                if (mp.canProduce("R", m) || reflectedColors.contains(MagicColor.Constant.RED)) {
                    manaMap.get(ManaAtom.RED).add(m);
                }
                if (mp.canProduce("G", m) || reflectedColors.contains(MagicColor.Constant.GREEN)) {
                    manaMap.get(ManaAtom.GREEN).add(m);
                }
                if (mp.canProduce("C", m) || reflectedColors.contains(MagicColor.Constant.COLORLESS)) {
                    manaMap.get(ManaAtom.COLORLESS).add(m);
                }
                if (mp.isSnow()) {
                    manaMap.get(ManaAtom.IS_SNOW).add(m);
                }
                if (DEBUG_MANA_PAYMENT) {
                    System.out.println("DEBUG_MANA_PAYMENT: groupSourcesByManaColor manaMap  = " + manaMap);
                }
            } // end of mana abilities loop
        } // end of mana sources loop

        return manaMap;
    }

    /**
     * <p>
     * determineLeftoverMana.
     * </p>
     * 
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @return a int.
     * @since 1.0.15
     */
    public static int determineLeftoverMana(final SpellAbility sa, final Player player) {
        for (int i = 1; i < 100; i++) {
            if (!canPayManaCost(sa.getRootAbility(), player, i)) {
                return i - 1;
            }
        }
        return 99;
    }

    /**
     * <p>
     * determineLeftoverMana.
     * </p>
     * 
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @param shardColor
     *            a mana shard to specifically test for.
     * @return a int.
     * @since 1.5.59
     */
    public static int determineLeftoverMana(final SpellAbility sa, final Player player, final String shardColor) {
        ManaCost origCost = sa.getRootAbility().getPayCosts().getTotalMana();

        String shardSurplus = shardColor;
        for (int i = 1; i < 100; i++) {
            ManaCost extra = new ManaCost(new ManaCostParser(shardSurplus));
            if (!canPayManaCost(new ManaCostBeingPaid(ManaCost.combine(origCost, extra)), sa, player)) {
                return i - 1;
            }
            shardSurplus += " " + shardColor;
        }
        return 99;
    }

    // Returns basic mana abilities plus "reflected mana" abilities
    /**
     * <p>
     * getAIPlayableMana.
     * </p>
     * 
     * @return a {@link java.util.List} object.
     */
    public static List<SpellAbility> getAIPlayableMana(Card c) {
        final List<SpellAbility> res = new ArrayList<>();
        for (final SpellAbility a : c.getManaAbilities()) {
            // if a mana ability has a mana cost the AI will miscalculate
            // if there is a parent ability the AI can't use it
            final Cost cost = a.getPayCosts();
            if (!cost.hasNoManaCost() || (a.getApi() != ApiType.Mana && a.getApi() != ApiType.ManaReflected)) {
                continue;
            }

            if (a.getRestrictions() != null &&  a.getRestrictions().isInstantSpeed()) {
                continue;
            }

            if (!res.contains(a)) {
                if (cost.isReusuableResource()) {
                    res.add(0, a);
                }
                else {
                    res.add(res.size(), a);
                }
            }
        }
        return res;
    }

    private static void handleOfferingsAI(final SpellAbility sa, boolean test, boolean costIsPaid) {
        if (sa.isOffering() && sa.getSacrificedAsOffering() != null) {
            final Card offering = sa.getSacrificedAsOffering();
            offering.setUsedToPay(false);
            if (costIsPaid && !test) {
                sa.getHostCard().getGame().getAction().sacrifice(offering, sa, null);
            }
            sa.resetSacrificedAsOffering();
        }
        if (sa.isEmerge() && sa.getSacrificedAsEmerge() != null) {
            final Card emerge = sa.getSacrificedAsEmerge();
            emerge.setUsedToPay(false);
            if (costIsPaid && !test) {
                sa.getHostCard().getGame().getAction().sacrifice(emerge, sa, null);
            }
            sa.resetSacrificedAsEmerge();
        }
    }
    
        
    /**
     * Matches list of creatures to shards in mana cost for convoking.
     * @param cost cost of convoked ability
     * @param list creatures to be evaluated
     * @param improvise
     * @return map between creatures and shards to convoke
     */
    public static Map<Card, ManaCostShard> getConvokeOrImproviseFromList(final ManaCost cost, List<Card> list, boolean improvise) {
        final Map<Card, ManaCostShard> convoke = new HashMap<>();
        Card convoked = null;
        if (!improvise) {
            for (ManaCostShard toPay : cost) {
                for (Card c : list) {
                    final int mask = c.determineColor().getColor() & toPay.getColorMask();
                    if (mask != 0) {
                        convoked = c;
                        convoke.put(c, toPay);
                        break;
                    }
                }
                if (convoked != null) {
                    list.remove(convoked);
                }
                convoked = null;
            }
        }
        for (int i = 0; i < list.size() && i < cost.getGenericCost(); i++) {
            convoke.put(list.get(i), ManaCostShard.GENERIC);
        }
        return convoke;
    }

    public static int determineMaxAffordableX(Player ai, SpellAbility sa) {
        if (sa.getPayCosts().getCostMana() == null) {
            return -1;
        }

        int numTgts = 0;
        int numX = sa.getPayCosts().getCostMana().getAmountOfX();

        if (numX == 0) {
            return -1;
        }

        int testX = 1;
        while (testX <= 100) {
            if (ComputerUtilMana.canPayManaCost(sa, ai, testX)) {
                numTgts++;
            } else {
                break;
            }
            testX++;
        }

        numTgts /= numX;

        return numTgts;
    }
}
