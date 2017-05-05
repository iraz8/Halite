import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MyBot {

	public static void main(String[] args) throws java.io.IOException {

		final InitPackage iPackage = Networking.getInit();
		final int myID = iPackage.myID;
		final GameMap gameMap = iPackage.map;

		Networking.sendInit("V1");

		while (true) {
			List<Move> moves = new ArrayList<Move>();

			Networking.updateFrame(gameMap);

			for (int y = 0; y < gameMap.height; y++) {
				for (int x = 0; x < gameMap.width; x++) {
					final Location location = gameMap.getLocation(x, y);
					final Site site = location.getSite();
					Actions decisionMaker = new Actions(gameMap, myID, x, y);
					if (site.owner == myID) {
						moves.add(new Move(location, decisionMaker.movebyBestProductionArea()));

						//	moves.add(new Move(location, Direction.randomDirection()));
					}
				}
			}
			Networking.sendFrame(moves);
		}
	}
}

class Actions {
	final int PLAYER = 0, NEUTRAL = 1, ENEMY = 2;
	GameMap gameMap;
	int myID;
	int x, y;
	Location location, eastLocation, southLocation, westLocation, northLocation;
	Site site, eastSite, southSite, westSite, northSite;
	int locationStrength, eastStrength, southStrength, westStrength, northStrength;
	int locationProduction, eastProduction, southProduction, westProduction, northProduction;
	int eastOwner, southOwner, westOwner, northOwner;
	boolean moveCommand;
	int waitBeforeMoveFactor = 6;

	Actions(GameMap gameMap, int myID, int x, int y) {
		this.gameMap = gameMap;
		this.myID = myID;
		this.x = x;
		this.y = y;
		this.location = gameMap.getLocation(x, y);
		this.eastLocation = gameMap.getLocation(location, Direction.EAST);
		this.southLocation = gameMap.getLocation(location, Direction.SOUTH);
		this.westLocation = gameMap.getLocation(location, Direction.WEST);
		this.northLocation = gameMap.getLocation(location, Direction.NORTH);
		this.site = location.getSite();
		this.eastSite = eastLocation.getSite();
		this.southSite = southLocation.getSite();
		this.westSite = westLocation.getSite();
		this.northSite = northLocation.getSite();
		this.locationStrength = site.strength;
		this.eastStrength = eastSite.strength;
		this.southStrength = southSite.strength;
		this.westStrength = westSite.strength;
		this.northStrength = northSite.strength;
		this.locationProduction = site.production;
		this.eastProduction = eastSite.production;
		this.southProduction = southSite.production;
		this.westProduction = westSite.production;
		this.northProduction = northSite.production;

		if (eastSite.owner == myID)    // 0 = al meu, 1 = neutru, 2 = al adversarului
			this.eastOwner = 0;
		else if (eastSite.owner != 0)
			this.eastOwner = 2;
		else
			this.eastOwner = 1;

		if (southSite.owner == myID)    // 0 = al meu, 1 = neutru, 2 = al adversarului
			this.southOwner = 0;
		else if (southSite.owner != 0)
			this.southOwner = 2;
		else
			this.southOwner = 1;

		if (westSite.owner == myID)    // 0 = al meu, 1 = neutru, 2 = al adversarului
			this.westOwner = 0;
		else if (westSite.owner != 0)
			this.westOwner = 2;
		else
			this.westOwner = 1;

		if (northSite.owner == myID)    // 0 = al meu, 1 = neutru, 2 = al adversarului
			this.northOwner = 0;
		else if (northSite.owner != 0)
			this.northOwner = 2;
		else
			this.northOwner = 1;

		if (locationStrength > locationProduction * 6)    //TODO can be optimized
			moveCommand = true;
	}

	public Direction movebyBestProductionArea() {
		int psumES = 0, psumSW = 0, psumWN = 0, psumNE = 0; //productions sums on regions
		int scanArea = 3;    //TODO can be optimized
		boolean possibleConquest = false;

		if (locationStrength > eastStrength || locationStrength > southStrength
			|| locationStrength > westStrength || locationStrength > northStrength) {
			possibleConquest = true;
		}

		if (possibleConquest == false)
			return Direction.STILL;

		for (int i = x + 1; i <= scanArea; i++) {
			for (int j = y - scanArea; j < y; j++) {
				Site tempSite = gameMap.getLocation(i, j).getSite();
				if (tempSite.owner != myID) {
					psumES = psumES + tempSite.production;
				}
			}
		}

		for (int i = x - scanArea; i < 0; i++) {
			for (int j = y - scanArea; j < 0; j++) {
				Site tempSite = gameMap.getLocation(i, j).getSite();
				if (tempSite.owner != myID) {
					psumSW = psumSW + tempSite.production;
				}
			}
		}

		for (int i = x - scanArea; i < 0; i++) {
			for (int j = y + 1; j <= scanArea; j++) {
				Site tempSite = gameMap.getLocation(i, j).getSite();
				if (tempSite.owner != myID) {
					psumWN = psumWN + tempSite.production;
				}
			}
		}

		for (int i = x + 1; i <= scanArea; i++) {
			for (int j = y + 1; j <= scanArea; j++) {
				Site tempSite = gameMap.getLocation(i, j).getSite();
				if (tempSite.owner != myID) {
					psumNE = psumNE + tempSite.production;
				}
			}
		}

		ArrayList<Integer> pAreas = new ArrayList<>(4);
		pAreas.add(psumES);
		pAreas.add(psumSW);
		pAreas.add(psumWN);
		pAreas.add(psumNE);

		Collections.sort(pAreas);

		for (int i = 3; i >= 0; i--) {
			int currentSum = pAreas.get(i);

			if (psumES == currentSum) {
				if (locationStrength > eastStrength && eastOwner == NEUTRAL) {
					return Direction.EAST;
				} else if (locationStrength > southStrength && southOwner == NEUTRAL) {
					return Direction.SOUTH;
				}
			}

			if (psumSW == currentSum) {
				if (locationStrength > southStrength && southOwner == NEUTRAL) {
					return Direction.SOUTH;
				} else if (locationStrength > westStrength && westOwner == NEUTRAL) {
					return Direction.WEST;
				}
			}

			if (psumWN == currentSum) {
				if (locationStrength > westStrength && westOwner == NEUTRAL) {
					return Direction.WEST;
				} else if (locationStrength > northStrength && northOwner == NEUTRAL) {
					return Direction.NORTH;
				}
			}

			if (psumNE == currentSum) {
				if (locationStrength > northStrength && northOwner == NEUTRAL) {
					return Direction.NORTH;
				} else if (locationStrength > eastStrength && eastOwner == NEUTRAL) {
					return Direction.EAST;
				}
			}
		}

		if (moveCommand == true) {
			if (eastOwner == PLAYER && eastStrength > eastProduction * waitBeforeMoveFactor)
				return Direction.EAST;
			if (southOwner == PLAYER && southStrength > southProduction * waitBeforeMoveFactor)
				return Direction.SOUTH;
			if (westOwner == PLAYER && westStrength > westProduction * waitBeforeMoveFactor)
				return Direction.WEST;
			if (northOwner == PLAYER && northStrength > northProduction * waitBeforeMoveFactor)
				return Direction.NORTH;
		}
		return Direction.STILL;
	}


}
