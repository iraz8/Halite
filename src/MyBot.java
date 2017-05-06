import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MyBot {

	public static void main(String[] args) throws java.io.IOException {

		final InitPackage iPackage = Networking.getInit();
		final int myID = iPackage.myID;
		final GameMap gameMap = iPackage.map;

		Networking.sendInit("V2");

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
	boolean eastDefeatable, southDefeatable, westDefeatable, northDefeatable;

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

		//Setare owneri
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

		//Setare celule alaturate ce pot fi cucerite
		if (locationStrength > eastStrength)
			eastDefeatable = true;
		if (locationStrength > southStrength)
			southDefeatable = true;
		if (locationStrength > westStrength)
			westDefeatable = true;
		if (locationStrength > northStrength)
			northDefeatable = true;

		if (locationStrength > locationProduction * 6)    //TODO can be optimized
			moveCommand = true;
	}

	public Direction movebyBestProductionArea() {
		int scanArea = 5;    //TODO can be optimized
		boolean possibleConquest = false;
		int minX = x, maxX = x, minY = y, maxY = y;
		if (locationStrength > eastStrength || locationStrength > southStrength
			|| locationStrength > westStrength || locationStrength > northStrength) {
			possibleConquest = true;
		}

		if (possibleConquest == false)
			return Direction.STILL;

		if (x - scanArea < 0)
			minX = gameMap.width - (x - scanArea);
		else
			minX = x - scanArea;

		if (x + scanArea > gameMap.width)
			maxX = 0 + ((x + scanArea) - gameMap.width);
		else
			maxX = x + scanArea;

		if (y - scanArea < 0)
			minY = gameMap.height - (y - scanArea);
		else
			minY = y - scanArea;

		if (y + scanArea > gameMap.height)
			maxY = 0 + ((y + scanArea) - gameMap.height);
		else
			maxX = y + scanArea;

		int bestX = 0, bestY = 0, bestEfficiency = 0;
		for (int i = minX; i <= maxX; i++) {
			if (x == i)
				continue;
			for (int j = minY; j <= maxY; j++) {
				if (y == j)
					continue;
				Site tempSite = gameMap.getLocation(i, j).getSite();
				if (tempSite.owner == myID)
					continue;

				int tempEfficiency = efficiencyFormula(tempSite.production, tempSite.strength, abs(x - i));
				if (tempEfficiency > bestEfficiency && locationStrength > tempSite.strength) {
					bestX = i;
					bestY = j;
					bestEfficiency = tempEfficiency;
				} else {
					int tempDistance = abs(x - i) + abs(y - j);
					int bestDistance = abs(x - bestX) + abs(y - bestY);
					if (tempEfficiency == bestEfficiency && locationStrength > tempSite.strength && tempDistance < bestDistance) {
						bestX = i;
						bestY = j;
						bestEfficiency = tempEfficiency;
					}
				}
			}
		}

		if (bestEfficiency > 0) {
			int efficiencyNorth = efficiencyFormula(northProduction, northStrength, 1);
			int efficiencyEast = efficiencyFormula(eastProduction, eastStrength, 1);
			int efficiencySouth = efficiencyFormula(southProduction, southStrength, 1);
			int efficiencyWest = efficiencyFormula(westProduction, westStrength, 1);
			if (bestX > x) {
				if (bestY > y) {    //N-E
					if (efficiencyNorth >= efficiencyEast && northOwner != PLAYER && northDefeatable)
						return Direction.NORTH;
					else if (eastOwner != PLAYER && eastDefeatable)
						return Direction.EAST;
				}
				if (bestY < y) {    //E-S
					if (efficiencyEast >= efficiencySouth && eastOwner != PLAYER && eastDefeatable)
						return Direction.EAST;
					else if (southOwner != PLAYER && southDefeatable)
						return Direction.SOUTH;
				}
			} else if (bestX < x) {
				if (bestY < y) {    //S-W
					if (efficiencySouth >= efficiencyWest && southOwner != PLAYER && southDefeatable)
						return Direction.SOUTH;
					else if (westOwner != PLAYER && westDefeatable)
						return Direction.WEST;
				}
				if (bestY > y) {    //W-N
					if (efficiencyWest >= efficiencyNorth && westOwner != PLAYER && westDefeatable)
						return Direction.WEST;
					else if (northOwner != PLAYER && northDefeatable)
						return Direction.NORTH;
				}
			}
		}
		return Direction.STILL;
	}

	int efficiencyFormula(int production, int strength, int steps) {
		return ((production * 37 - strength) * 5) / steps;
	}

	int abs(int number) {
		return (number < 0) ? -number : number;
	}

}
