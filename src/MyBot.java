import java.util.ArrayList;
import java.util.List;

public class MyBot {
	public static void main(String[] args) throws java.io.IOException {

		final InitPackage iPackage = Networking.getInit();
		final int myID = iPackage.myID;
		final GameMap gameMap = iPackage.map;

		Networking.sendInit("V10");

		int turns = 0;
		boolean phase1Completed = false;
		Location bestProductionLocation = null;

		while (true) {
			List<Move> moves = new ArrayList<Move>();

			Networking.updateFrame(gameMap);
			turns++;

			if (turns == 1) {
				for (int y = 0; y < gameMap.height; y++)
					for (int x = 0; x < gameMap.width; x++) {
						Location tempLocation = gameMap.getLocation(x, y);
						Site tempSite = tempLocation.getSite();
						if (tempSite.owner == myID) {
							Actions tempDecisionMaker = new Actions(gameMap, myID, x, y);
							bestProductionLocation = tempDecisionMaker.findBestProductionSite(x, y);
						}
					}
			}

			if (!phase1Completed)
				if (gameMap.getLocation(bestProductionLocation.getX(), bestProductionLocation.getY()).getSite().owner == myID)
					phase1Completed = true;


			for (int y = 0; y < gameMap.height; y++) {
				for (int x = 0; x < gameMap.width; x++) {
					Location location = gameMap.getLocation(x, y);
					Site site = location.getSite();
					if (site.owner == myID) {

						if (!phase1Completed) {
							Actions decisionMaker = new Actions(gameMap, myID, x, y);
							Direction moveDirection = decisionMaker.goForBestProductionSiteInEntryGame(bestProductionLocation.getX(), bestProductionLocation.getY());
							moves.add(new Move(location, moveDirection));
						} else {
							moves.add(new Move(location, Direction.randomDirection()));
						}
						//	moves.add(new Move(location, Direction.randomDirection()));
					}
				}
			}
			Networking.sendFrame(moves);
		}
	}
}


class Actions {
	int eastOwner;
	int southOwner;
	int westOwner;
	int northOwner;
	int PLAYER = 0;
	int NEUTRAL = 1;
	int ENEMY = 2;
	int x;
	int y;
	Location location;
	Location eastLocation;
	Location southLocation;
	Location westLocation;
	Location northLocation;
	Site site;
	Site eastSite;
	Site southSite;
	Site westSite;
	Site northSite;
	int locationStrength;
	int eastStrength;
	int southStrength;
	int westStrength;
	int northStrength;
	int locationProduction;
	int eastProduction;
	int southProduction;
	int westProduction;
	int northProduction;
	double MINVALUE = -99999;
	GameMap gameMap;
	int myID;
	int scanArea = 3; //TODO can be optimised
	boolean moveCommand;
	boolean possibleConquest;
	boolean eastDefeatable;
	boolean southDefeatable;
	boolean westDefeatable;
	boolean northDefeatable;

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
			this.eastOwner = PLAYER;
		else if (eastSite.owner != 0)
			this.eastOwner = ENEMY;
		else
			this.eastOwner = NEUTRAL;

		if (southSite.owner == myID)    // 0 = al meu, 1 = neutru, 2 = al adversarului
			this.southOwner = PLAYER;
		else if (southSite.owner != 0)
			this.southOwner = ENEMY;
		else
			this.southOwner = NEUTRAL;

		if (westSite.owner == myID)    // 0 = al meu, 1 = neutru, 2 = al adversarului
			this.westOwner = PLAYER;
		else if (westSite.owner != 0)
			this.westOwner = ENEMY;
		else
			this.westOwner = NEUTRAL;

		if (northSite.owner == myID)    // 0 = al meu, 1 = neutru, 2 = al adversarului
			this.northOwner = PLAYER;
		else if (northSite.owner != 0)
			this.northOwner = ENEMY;
		else
			this.northOwner = NEUTRAL;

		//Setare celule alaturate ce pot fi cucerite

		if (locationStrength > (eastStrength + eastProduction) && eastOwner != PLAYER)
			this.eastDefeatable = true;
		if (locationStrength > (southStrength + southProduction) && southOwner != PLAYER)
			this.southDefeatable = true;
		if (locationStrength > (westStrength + westProduction) && westOwner != PLAYER)
			this.westDefeatable = true;
		if (locationStrength > (northStrength + northProduction) && northOwner != PLAYER)
			this.northDefeatable = true;

		if (eastDefeatable || southDefeatable || westDefeatable || northDefeatable)
			possibleConquest = true;

		if (locationStrength > locationProduction * 6)    //TODO can be optimized
			this.moveCommand = true;


	}


	Location findBestProductionSite(int initialX, int initialY) {
		int minX = Math.max(0, initialX - scanArea), maxX = Math.min(gameMap.width - 1, initialX + scanArea);
		int minY = Math.max(0, initialY - scanArea), maxY = Math.min(gameMap.height - 1, initialY + scanArea);
		int bestX = initialX, bestY = initialY;
		int bestProduction = gameMap.getLocation(initialX, initialY).getSite().production;
		for (int i = minX; i <= maxX; i++)
			for (int j = minY; j <= maxY; j++) {
				Site tempSite = gameMap.getLocation(i, j).getSite();
				if (tempSite.owner != myID) {
					if (tempSite.production == bestProduction) {
						if (Math.abs(initialX - i) + Math.abs(initialY - j) < Math.abs(initialX - bestX) + Math.abs(initialY - bestY)) {
							bestX = i;
							bestY = j;
							bestProduction = tempSite.production;
						}
					}
					if (tempSite.production > bestProduction) {
						bestX = i;
						bestY = j;
						bestProduction = tempSite.production;
					}
				}
			}

		return gameMap.getLocation(bestX, bestY);

	}

	Direction goForBestProductionSiteInEntryGame(int bestX, int bestY) {


		if (x == bestX) {    //daca bestX e pe aceeasi coloana
			if (y > bestY) {
				if (northDefeatable || northOwner == PLAYER)
					return Direction.NORTH;
			}
			if (y < bestY) {
				if (southDefeatable || southOwner == PLAYER)
					return Direction.SOUTH;
			}
		}

		if (y == bestY) {    //daca bestY e pe aceelasi rand
			if (x < bestX) {
				if (eastDefeatable || eastOwner == PLAYER)
					return Direction.EAST;
			}
			if (x > bestX) {
				if (westDefeatable || westOwner == PLAYER)
					return Direction.WEST;
			}
		}


		if (x > bestX && y < bestY) {   //cadranul 1
			if (westStrength <= southStrength && (westDefeatable || westOwner == PLAYER))
				return Direction.WEST;
			if (southStrength <= westStrength && (southDefeatable || southOwner == PLAYER))
				return Direction.SOUTH;
		}

		if (x < bestX && y < bestY) {    //cadranul 2
			if (eastStrength <= southStrength && (eastDefeatable || eastOwner == PLAYER))
				return Direction.EAST;
			if (southStrength <= eastStrength && (southDefeatable || southOwner == PLAYER))
				return Direction.SOUTH;
		}

		if (x < bestX && y > bestY) {    //cadranul 3
			if (eastStrength <= northStrength && (eastDefeatable || eastOwner == PLAYER))
				return Direction.EAST;
			if (northStrength <= eastStrength && (northDefeatable || northOwner == PLAYER))
				return Direction.NORTH;
		}

		if (x > bestX && y > bestY) {    //cadranul 4
			if (westStrength <= northStrength && (westDefeatable || westOwner == PLAYER))
				return Direction.WEST;
			if (northStrength <= westStrength && (northDefeatable || northOwner == PLAYER))
				return Direction.NORTH;

		}

		return Direction.STILL;
	}
}