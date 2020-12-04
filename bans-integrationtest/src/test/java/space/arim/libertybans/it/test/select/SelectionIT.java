/* 
 * LibertyBans-integrationtest
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-integrationtest is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * LibertyBans-integrationtest is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-integrationtest. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.it.test.select;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.DraftPunishmentBuilder;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.punish.PunishmentDrafter;
import space.arim.libertybans.api.scope.ScopeManager;
import space.arim.libertybans.api.select.PunishmentSelector;
import space.arim.libertybans.api.select.SelectionOrderBuilder;
import space.arim.libertybans.it.DontInject;
import space.arim.libertybans.it.InjectionInvocationContextProvider;
import space.arim.libertybans.it.resolver.RandomOperatorResolver;
import space.arim.libertybans.it.resolver.RandomPunishmentTypeResolver;
import space.arim.libertybans.it.resolver.RandomPunishmentTypeResolver.SingularPunishment;
import space.arim.libertybans.it.resolver.RandomReasonResolver;
import space.arim.libertybans.it.resolver.RandomVictimResolver;
import space.arim.libertybans.it.util.TestingUtil;

@ExtendWith(InjectionInvocationContextProvider.class)
@ExtendWith({RandomPunishmentTypeResolver.class, RandomOperatorResolver.class,
	RandomVictimResolver.class, RandomReasonResolver.class})
public class SelectionIT {

	private final PunishmentDrafter drafter;
	private final PunishmentSelector selector;
	private final ScopeManager scopeManager;

	private static final Duration ONE_SECOND = Duration.ofSeconds(1L);

	public SelectionIT(PunishmentDrafter drafter, PunishmentSelector selector,
			ScopeManager scopeManager) {
		this.drafter = drafter;
		this.selector = selector;
		this.scopeManager = scopeManager;
	}

	private SelectionOrderBuilder selectionBuilder(PunishmentType type) {
		return selector.selectionBuilder().type(type);
	}

	private static Punishment getSinglePunishment(SelectionOrderBuilder selectionBuilder) {
		return selectionBuilder.build().getFirstSpecificPunishment().toCompletableFuture().join().orElseThrow();
	}

	private static List<Punishment> getPunishments(SelectionOrderBuilder selectionBuilder) {
		return selectionBuilder.build().getAllSpecificPunishments().toCompletableFuture().join();
	}

	private static void assertEmpty(SelectionOrderBuilder selectionBuilder) {
		List<Punishment> punishments = getPunishments(selectionBuilder);
		assertTrue(punishments.isEmpty(), "Non-empty punishments, retrieved " + punishments);
	}

	@TestTemplate
	public void testSelectNothing(@DontInject PunishmentType type, @DontInject Victim victim, @DontInject Operator operator) {
		assertEmpty(selectionBuilder(type));
		assertEmpty(selectionBuilder(type).operator(operator));
		assertEmpty(selectionBuilder(type).victim(victim));
		assertEmpty(selectionBuilder(type).operator(operator).victim(victim));
		assertEmpty(selectionBuilder(type).scope(scopeManager.specificScope("servername")));
		assertEmpty(selectionBuilder(type).victim(victim).scope(scopeManager.specificScope("servername")));
		assertEmpty(selectionBuilder(type).maximumToRetrieve(20));
		assertEmpty(selectionBuilder(type).operator(operator).maximumToRetrieve(20));
		assertEmpty(selectionBuilder(type).victim(victim).skipFirstRetrieved(10).maximumToRetrieve(30));
		assertEmpty(selectionBuilder(type).selectAll().victim(victim).operator(operator));
		assertEmpty(selectionBuilder(type).selectAll().scope(scopeManager.specificScope("otherserver")));
	}

	private DraftPunishmentBuilder draftBuilder(PunishmentType type, Victim victim, String reason) {
		return drafter.draftBuilder().type(type).victim(victim).reason(reason);
	}

	private Punishment getPunishment(DraftPunishmentBuilder draftBuilder) {
		CentralisedFuture<Optional<Punishment>> future = draftBuilder.build().enactPunishment().toCompletableFuture();
		Optional<Punishment> optPunishment;
		try {
			optPunishment = future.join();
		} catch (CompletionException ex) {
			throw Assertions.<RuntimeException>fail("Drafting punishment to later select failed", ex);
			//throw new TestAbortedException("Drafting punishment to later select failed", ex);
		}
		//assumeTrue
		assertTrue(optPunishment.isPresent(), "Drafting punishment to later select failed");
		return optPunishment.get();
	}

	@TestTemplate
	public void testSelectBanMuteForVictim(@DontInject @SingularPunishment PunishmentType type, @DontInject Victim victim) {
		Punishment banOrMute = getPunishment(
				draftBuilder(type, victim, "some reason").scope(scopeManager.specificScope("someserver")));
		assertEquals(banOrMute, getSinglePunishment(selectionBuilder(type).victim(victim)));
	}

	@TestTemplate
	public void testSelectMultipleWarnsForVictim(@DontInject Victim victim) {
		// Kicks would not be considered active, whereas bans and mutes are singular
		final PunishmentType type = PunishmentType.WARN;

		Punishment pun1 = getPunishment(
				draftBuilder(type, victim, "and kicked/warned on top of that"));
		// Required to have punishment start times be correctly ordered
		TestingUtil.sleepUnchecked(ONE_SECOND);

		Punishment pun2 = getPunishment(
				draftBuilder(type, victim, "Yet another punishment"));
		TestingUtil.sleepUnchecked(ONE_SECOND);

		Punishment pun3 = getPunishment(
				draftBuilder(type, victim, "More punishments all around"));

		assertEquals(
				List.of(pun3, pun2, pun1),
				getPunishments(selectionBuilder(type).victim(victim)));
		assertEquals(
				List.of(pun2, pun1),
				getPunishments(selectionBuilder(type).victim(victim).skipFirstRetrieved(1)));
		assertEquals(
				List.of(pun3),
				getPunishments(selectionBuilder(type).victim(victim).maximumToRetrieve(1)));
	}

	@TestTemplate
	public void testSelectHistoricalBansMutes(@DontInject @SingularPunishment PunishmentType type,
			@DontInject Victim victim) {
		Duration twoSeconds = Duration.ofSeconds(2L);

		Punishment expired1 = getPunishment(
				draftBuilder(type, victim, "the first punishment").duration(ONE_SECOND));
		TestingUtil.sleepUnchecked(twoSeconds);

		Punishment expired2 = getPunishment(
				draftBuilder(type, victim, "the second punishment").duration(ONE_SECOND));
		TestingUtil.sleepUnchecked(twoSeconds);

		Punishment active = getPunishment(
				draftBuilder(type, victim, "the third punishment"));

		assertEquals(
				List.of(active),
				getPunishments(selectionBuilder(type).victim(victim)));
		assertEquals(
				List.of(active, expired2, expired1),
				getPunishments(selectionBuilder(type).victim(victim).selectAll()));
		assertEquals(
				List.of(expired1),
				getPunishments(selectionBuilder(type).victim(victim).selectAll().skipFirstRetrieved(2)));
		assertEquals(
				List.of(active, expired2),
				getPunishments(selectionBuilder(type).victim(victim).selectAll().maximumToRetrieve(2)));
	}

}