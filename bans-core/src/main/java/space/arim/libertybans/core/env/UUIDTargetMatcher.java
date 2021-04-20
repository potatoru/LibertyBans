/* 
 * LibertyBans-core
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-core. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.core.env;

import java.net.InetAddress;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import space.arim.api.env.annote.PlatformPlayer;

public class UUIDTargetMatcher extends AbstractTargetMatcher {

	private final Set<UUID> uuids;
	
	public UUIDTargetMatcher(Set<UUID> uuids, Consumer<@PlatformPlayer Object> callback) {
		super(callback);
		this.uuids = Set.copyOf(uuids);
	}
	
	@Override
	public boolean matches(UUID uuid, InetAddress address) {
		return uuids.contains(uuid);
	}

	@Override
	public String toString() {
		return "UUIDTargetMatcher{" +
				"uuids=" + uuids +
				'}';
	}
}
