package games.stendhal.server.entity.npc.action;

import java.util.List;

import games.stendhal.server.entity.item.Item;
import games.stendhal.server.entity.npc.ChatAction;
import games.stendhal.server.entity.npc.SpeakerNPC;
import games.stendhal.server.entity.npc.parser.Sentence;
import games.stendhal.server.entity.player.Player;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.log4j.Logger;

/**
 * Drops the specified item with the specified infostring
 */
public class DropInfostringItemAction implements ChatAction {
	private static Logger logger = Logger.getLogger(DropItemAction.class);
	private final String itemName;
	private final String infostring;

	/**
	 * Creates a new DropItemAction.
	 * 
	 * @param itemName
	 *            name of item
	 * @param amount
	 *            for StackableItems
	 */
	public DropInfostringItemAction(final String itemName, final String infostring) {
		this.itemName = itemName;
		this.infostring = infostring;
	}

	public void fire(final Player player, final Sentence sentence, final SpeakerNPC npc) {
		final List<Item> items = player.getAllEquipped(itemName);
		boolean res = false;
		for (final Item item : items) {
			if (infostring.equalsIgnoreCase(item.getInfoString())) {
				res = player.drop(item);
				break;
			}
		}
		
		if (!res) {
			logger.error("Cannot drop " + itemName, new Throwable());
		}
		player.notifyWorldAboutChanges();
	}

	@Override
	public String toString() {
		return "drop item <" + itemName + "> with infostring <" + infostring + ">";
	}

	@Override
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final DropInfostringItemAction other = (DropInfostringItemAction) obj;
		if (!infostring.equals(other.infostring)) {
			return false;
		}
		if (itemName == null) {
			if (other.itemName != null) {
				return false;
			}
		} else if (!itemName.equals(other.itemName)) {
			return false;
		}
		return true;
	}

}
