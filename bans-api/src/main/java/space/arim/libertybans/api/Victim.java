/* 
 * LibertyBans-api
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-api is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-api is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-api. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.api;

public class Victim {
	
	private final VictimType type;
	
	Victim(VictimType type) {
		this.type = type;
	}
	
	public VictimType getType() {
		return type;
	}

	public enum VictimType {
		
		PLAYER,
		ADDRESS;
		
		/**
		 * Gets a VictimType from an ordinal, or {@code null} if no such
		 * ordinal exists in the enum
		 * 
		 * @param ordinal the ordinal, 0 or 1
		 * @return the corresponding victim type, or {@code null}
		 */
		public static VictimType fromOrdinal(int ordinal) {
			switch (ordinal) {
			case 0:
				return PLAYER;
			case 1:
				return ADDRESS;
			default:
				return null;
			}
		}
		
	}
	
}