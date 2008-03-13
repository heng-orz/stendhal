package games.stendhal.server.maps;

import games.stendhal.server.core.engine.PlayerList;
import games.stendhal.server.core.engine.StendhalRPRuleProcessor;
import games.stendhal.server.entity.player.Player;

public class MockStendhalRPRuleProcessor extends StendhalRPRuleProcessor {
 protected MockStendhalRPRuleProcessor() {
	
}
	public static MockStendhalRPRuleProcessor get() {
		if (!(instance instanceof MockStendhalRPRuleProcessor)) {
			instance = new MockStendhalRPRuleProcessor();
		}

		return (MockStendhalRPRuleProcessor) instance;
	}

	@Override
	public void addGameEvent(String source, String event, String... params) {
		// do not log to database during test
	}

	@Override
	public int getTurn() {
		return 0;
	}

	/**
	 * Adds a player object to the list of players.
	 *
	 * @param player Player
	 */
	public void addPlayer(Player player) {
		this.onlinePlayers.add(player);
	}

	/**
	 * reset the list of online players.
	 */
	public void clearPlayers() {
		onlinePlayers = new PlayerList();
	}
}
