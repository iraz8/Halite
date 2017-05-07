import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MyBot {
	public static void main(String[] args) throws java.io.IOException {

		final InitPackage iPackage = Networking.getInit();
		final int myID = iPackage.myID;
		final GameMap gameMap = iPackage.map;

		Networking.sendInit("V11");

		int turns = 0;
		boolean phase1Completed = false;
		Location bestProductionLocation = null;

		final int PLAYER = 0;

		boolean NAP = true; // non-aggression pact
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
							//daca diferentele de productie nu sunt mari, nu merita sa foloseasca strategia de tunneling
							if (bestProductionLocation.getSite().production < tempSite.production + 5)
								phase1Completed = true;
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

					boolean innerPiece = false;


					if (site.owner == myID) {
						Actions decisionMaker = new Actions(gameMap, myID, x, y);
						if (decisionMaker.eastOwner == PLAYER &&
							decisionMaker.southOwner == PLAYER &&
							decisionMaker.westOwner == PLAYER &&
							decisionMaker.northOwner == PLAYER)
							innerPiece = true;


						if (!phase1Completed) {
							Direction moveDirection = decisionMaker.goForBestProductionSiteInEntryGame(bestProductionLocation.getX(), bestProductionLocation.getY());
							moves.add(new Move(location, moveDirection));
						} else if (decisionMaker.closeEnemy()) {
							NAP = false;
							System.out.println("aaalllert");
							System.exit(1);
							moves.add(new Move(location, decisionMaker.attack()));
						} else {
							if (NAP && decisionMaker.closeEnemyTwoCells()) {
								moves.add(new Move(location, decisionMaker.noAttackPact()));
							} else if ((x + y + turns) % 2 == 0) {
								if (!innerPiece)
									moves.add(new Move(location, decisionMaker.movebyEfficiencyFormula()));
								else
									moves.add(new Move(location, decisionMaker.moveInnerPieces()));
							} else
								moves.add(new Move(location, Direction.STILL));
						}


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
	GameMap gameMap;
	int myID;
	int scanArea = 4; //TODO can be optimised
	int decentProduction = 5; //TODO can be optimised
	int goodProduction = 10; // TODO can be optimised
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

		if (locationStrength >= locationProduction * 5)    //TODO can be optimized
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
				if (tempSite.owner != myID && (tempSite.strength < locationStrength || tempSite.strength < locationStrength * 5)) {
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

		if (!possibleConquest && locationStrength < locationProduction * 4)
			return Direction.STILL;

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

	Direction movebyEfficiencyFormula() {

		if (locationProduction > goodProduction && locationStrength < locationProduction * 9)
			return Direction.STILL;

		if (locationProduction < decentProduction && possibleConquest) {
			ArrayList<Integer> tempStrengthList = new ArrayList<>();
			tempStrengthList.add(eastStrength);
			tempStrengthList.add(southStrength);
			tempStrengthList.add(westStrength);
			tempStrengthList.add(northStrength);

			int minStrength = Collections.min(tempStrengthList);

			if (locationStrength > minStrength) {
				if (eastDefeatable && eastStrength == minStrength)
					return Direction.EAST;
				if (southDefeatable && southStrength == minStrength)
					return Direction.SOUTH;
				if (westDefeatable && westStrength == minStrength)
					return Direction.WEST;
				if (northDefeatable && northStrength == minStrength)
					return Direction.NORTH;
			}
		}

		if (!moveCommand)
			return Direction.STILL;
		double efficiencyNorth = efficiencyFormula(northProduction, northStrength);
		double efficiencyEast = efficiencyFormula(eastProduction, eastStrength);
		double efficiencySouth = efficiencyFormula(southProduction, southStrength);
		double efficiencyWest = efficiencyFormula(westProduction, westStrength);

		ArrayList<Double> efficiencyList = new ArrayList<Double>();
		efficiencyList.add(efficiencyEast);
		efficiencyList.add(efficiencySouth);
		efficiencyList.add(efficiencyWest);
		efficiencyList.add(efficiencyNorth);

		Collections.sort(efficiencyList);

		for (int i = 3; i >= 2; i--) {
			if (efficiencyList.get(i) == efficiencyEast && eastDefeatable)
				return Direction.EAST;
			if (efficiencyList.get(i) == efficiencySouth && southDefeatable)
				return Direction.SOUTH;
			if (efficiencyList.get(i) == efficiencyWest && westDefeatable)
				return Direction.WEST;
			if (efficiencyList.get(i) == efficiencyNorth && northDefeatable)
				return Direction.NORTH;
		}

		if (possibleConquest) {
			ArrayList<Integer> strengthList = new ArrayList<>();
			strengthList.add(eastStrength);
			strengthList.add(southStrength);
			strengthList.add(westStrength);
			strengthList.add(northStrength);

			Collections.sort(strengthList);
			for (int i = 0; i < strengthList.size(); i++) {
				if (eastDefeatable && eastStrength == strengthList.get(i))
					return Direction.EAST;
				if (southDefeatable && southStrength == strengthList.get(i))
					return Direction.SOUTH;
				if (westDefeatable && westStrength == strengthList.get(i))
					return Direction.WEST;
				if (northDefeatable && northStrength == strengthList.get(i))
					return Direction.NORTH;
			}
		}
		return Direction.STILL;
	}

	double efficiencyFormula(double production, double strength) {
		if (strength > 0)
			return (production * production) / strength;
		else
			return production;
	}

	Direction moveInnerPieces() {
		int eastSteps = 0, southSteps = 0, westSteps = 0, northSteps = 0;
		Location goEast = location, goSouth = location, goWest = location, goNorth = location;

		if (!moveCommand)
			return Direction.STILL;

		while (goEast.getSite().owner != PLAYER && eastSteps < gameMap.width - 1) {
			eastSteps++;
			goEast = gameMap.getLocation(goEast, Direction.EAST);
		}
		while (goSouth.getSite().owner != PLAYER && southSteps < gameMap.height - 1) {
			southSteps++;
			goSouth = gameMap.getLocation(goSouth, Direction.SOUTH);
		}
		while (goWest.getSite().owner != PLAYER && westSteps < gameMap.width - 1) {
			westSteps++;
			goWest = gameMap.getLocation(goWest, Direction.WEST);
		}
		while (goNorth.getSite().owner != PLAYER && northSteps < gameMap.height - 1) {
			northSteps++;
			goNorth = gameMap.getLocation(goNorth, Direction.NORTH);
		}

		//Daca exista adversar in apropiere, se intareste frontiera
		if ((eastSteps == 3 || eastSteps == 4 || eastSteps == 5) && goEast.getSite().owner == ENEMY && locationStrength + eastStrength < 280)
			return Direction.EAST;
		if ((southSteps == 3 || southSteps == 4 || southSteps == 5) && goSouth.getSite().owner == ENEMY && locationStrength + southStrength < 280)
			return Direction.SOUTH;
		if ((westSteps == 3 || westSteps == 4 || westSteps == 5) && goWest.getSite().owner == ENEMY && locationStrength + westStrength < 280)
			return Direction.WEST;
		if ((northSteps == 3 || northSteps == 4 || northSteps == 5) && goNorth.getSite().owner == ENEMY && locationStrength + northStrength < 280)
			return Direction.NORTH;

		//Mareste strength-ul frontierei
		if (eastSteps == 2 && locationStrength + eastStrength + eastProduction < 255)
			return Direction.EAST;
		if (southSteps == 2 && locationStrength + southStrength + southProduction < 255)
			return Direction.SOUTH;
		if (westSteps == 2 && locationStrength + westStrength + westProduction < 255)
			return Direction.WEST;
		if (northSteps == 2 && locationStrength + northStrength + northProduction < 255)
			return Direction.NORTH;

		ArrayList<Integer> stepsList = new ArrayList<>();
		stepsList.add(eastSteps);
		stepsList.add(southSteps);
		stepsList.add(westSteps);
		stepsList.add(northSteps);

		Collections.sort(stepsList);

		for (int i = 0; i < stepsList.size(); i++) {
			if (eastSteps == stepsList.get(i) && locationStrength + eastStrength + eastProduction < 255)
				return Direction.EAST;
			if (southSteps == stepsList.get(i) && locationStrength + southStrength + southProduction < 255)
				return Direction.SOUTH;
			if (westSteps == stepsList.get(i) && locationStrength + westStrength + westProduction < 255)
				return Direction.WEST;
			if (northSteps == stepsList.get(i) && locationStrength + northStrength + northProduction < 255)
				return Direction.NORTH;
		}

		return Direction.STILL;
	}


	boolean closeEnemyTwoCells() {
		Location eastTwoLocation = gameMap.getLocation(eastLocation, Direction.EAST);
		Location southTwoLocation = gameMap.getLocation(southLocation, Direction.SOUTH);
		Location westTwoLocation = gameMap.getLocation(westLocation, Direction.WEST);
		Location northTwoLocation = gameMap.getLocation(northLocation, Direction.NORTH);
		Site eastTwoSite = eastTwoLocation.getSite();
		Site southTwoSite = southTwoLocation.getSite();
		Site westTwoSite = westTwoLocation.getSite();
		Site northTwoSite = northTwoLocation.getSite();
		if (eastTwoSite.owner == ENEMY || southTwoSite.owner == ENEMY || westTwoSite.owner == ENEMY || northTwoSite.owner == ENEMY)
			return true;
		return false;
	}

	Direction noAttackPact() {
		Location eastTwoLocation = gameMap.getLocation(eastLocation, Direction.EAST);
		Location southTwoLocation = gameMap.getLocation(southLocation, Direction.SOUTH);
		Location westTwoLocation = gameMap.getLocation(westLocation, Direction.WEST);
		Location northTwoLocation = gameMap.getLocation(northLocation, Direction.NORTH);
		Site eastTwoSite = eastTwoLocation.getSite();
		Site southTwoSite = southTwoLocation.getSite();
		Site westTwoSite = westTwoLocation.getSite();
		Site northTwoSite = northTwoLocation.getSite();
		if (!moveCommand)
			return Direction.STILL;
		if (eastTwoSite.owner == ENEMY) {
			if (locationStrength < 200)
				return Direction.STILL;
			else if (northStrength < southStrength)
				return Direction.NORTH;
			else
				return Direction.SOUTH;

		}

		if (southTwoSite.owner == ENEMY) {
			if (locationStrength < 200)
				return Direction.STILL;
			else if (eastStrength < westStrength && eastStrength < 200)
				return Direction.EAST;
			else if (westStrength < 200)
				return Direction.WEST;
		}

		if (westTwoSite.owner == ENEMY) {
			if (locationStrength < 200)
				return Direction.STILL;
			else if (southStrength < northStrength && southStrength < 200)
				return Direction.SOUTH;
			else if (northStrength < 200)
				return Direction.NORTH;
		}

		if (northTwoSite.owner == ENEMY) {
			if (locationStrength < 200)
				return Direction.STILL;
			else if (westStrength < eastStrength && westStrength < 200)
				return Direction.WEST;
			else if (eastStrength < 200)
				return Direction.EAST;
		}

		return Direction.STILL;
	}


	boolean closeEnemy() {
		if (eastSite.owner == ENEMY || southSite.owner == ENEMY || westSite.owner == ENEMY || northSite.owner == ENEMY)
			return true;
		return false;
	}

	Direction attack() {
		if (eastOwner == ENEMY && locationStrength > 1.5 * (double) eastStrength)
			return Direction.EAST;
		if (southOwner == ENEMY && locationStrength > 1.5 * (double) southStrength)
			return Direction.SOUTH;
		if (westOwner == ENEMY && locationStrength > 1.5 * (double) westStrength)
			return Direction.WEST;
		if (northOwner == ENEMY && locationStrength > 1.5 * (double) northStrength)
			return Direction.NORTH;
		return Direction.STILL;
	}


}
