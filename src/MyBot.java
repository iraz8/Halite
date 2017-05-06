import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MyBot {

	public static void main(String[] args) throws java.io.IOException {

		final InitPackage iPackage = Networking.getInit();
		final int myID = iPackage.myID;
		final GameMap gameMap = iPackage.map;

		Networking.sendInit("V5");

		final int PLAYER = 0;

		//Phase1
		Location bestCloseLocation = gameMap.getLocation(0, 0);
		int bestX = 0, bestY = 0;
		boolean phase1Analyzer = true;
		int initialX = 0, initialY = 0;
		boolean phase1 = true;

		//noinspection InfiniteLoopStatement
		while (true) {
			List<Move> moves = new ArrayList<>();

			Networking.updateFrame(gameMap);

			int conqueredPieces = 0;
			boolean enoughConquered = false;
			double percentEnoughConquered = 0.3;

			for (int y = 0; y < gameMap.height; y++) {
				for (int x = 0; x < gameMap.width; x++) {
					final Location location = gameMap.getLocation(x, y);
					final Site site = location.getSite();


					if (site.owner == myID) {
						conqueredPieces++;
						if (phase1Analyzer) {
							initialX = x;
							initialY = y;
						}
					}
				}
			}


			//PHASE 1 Analyzer
			if (phase1Analyzer) {
				Actions analyzer = new Actions(gameMap, myID, initialX, initialY);
				bestCloseLocation = analyzer.bestProductionArea();
				bestX = bestCloseLocation.getX();
				bestY = bestCloseLocation.getY();
				phase1Analyzer = false;
			}

			if (conqueredPieces >= ((double) (gameMap.height * gameMap.width)) * percentEnoughConquered)
				enoughConquered = true;

			for (int y = 0; y < gameMap.height; y++) {
				for (int x = 0; x < gameMap.width; x++) {
					final Location location = gameMap.getLocation(x, y);
					final Site site = location.getSite();

					boolean innerPiece = false;

					if (site.owner == myID) {
						Actions decisionMaker = new Actions(gameMap, myID, x, y);
						if (decisionMaker.eastOwner == PLAYER &&
							decisionMaker.southOwner == PLAYER &&
							decisionMaker.westOwner == PLAYER &&
							decisionMaker.northOwner == PLAYER)
							innerPiece = true;

					/*	if (site.strength < site.production * 5 && site.production > 8)
							moves.add(new Move(location, Direction.STILL));
						else if (!enoughConquered) {
							Direction move = decisionMaker.movebyEfficiency();
							moves.add(new Move(location, move));
						} else if (!innerPiece) {
							Direction move = decisionMaker.movebyBestProductionArea();
							if (move != Direction.STILL)
								moves.add(new Move(location, move));
							else {
								//	move = decisionMaker.movebyProduction();
								move = decisionMaker.movebyEfficiency();
								moves.add(new Move(location, move));
							}
						} else
							moves.add(new Move(location, decisionMaker.moveInnerPieces()));
						//	moves.add(new Move(location, Direction.randomDirection()));
						*/
						if (bestCloseLocation.getSite().owner == myID) {
							phase1 = false;
							System.out.println("222");
						}

						if (phase1)
							moves.add(new Move(location, decisionMaker.firstPhase(bestX, bestY)));
						else if (innerPiece)
							moves.add(new Move(location, decisionMaker.moveInnerPieces()));
						else
							moves.add(new Move(location, decisionMaker.closeNeutralPieces()));
					}
				}
			}
			Networking.sendFrame(moves);
		}
	}
}

class Actions {
	final int eastOwner;
	final int southOwner;
	final int westOwner;
	final int northOwner;
	private final int PLAYER = 0;
	private final int NEUTRAL = 1;
	private final int ENEMY = 2;
	private final GameMap gameMap;
	private final int myID;
	private final int x;
	private final int y;
	private final Location location;
	private final Location eastLocation;
	private final Location southLocation;
	private final Location westLocation;
	private final Location northLocation;
	private final Site site;
	private final Site eastSite;
	private final Site southSite;
	private final Site westSite;
	private final Site northSite;
	private final int locationStrength;
	private final int eastStrength;
	private final int southStrength;
	private final int westStrength;
	private final int northStrength;
	private final int locationProduction;
	private final int eastProduction;
	private final int southProduction;
	private final int westProduction;
	private final int northProduction;
	private final double MINVALUE = -99999;
	private final int scanArea = 2; //TODO can be optimised
	private boolean moveCommand;
	private boolean possibleConquest;
	private boolean eastDefeatable;
	private boolean southDefeatable;
	private boolean westDefeatable;
	private boolean northDefeatable;
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

		if (locationStrength > eastStrength && eastOwner != PLAYER)
			this.eastDefeatable = true;
		if (locationStrength > southStrength && southOwner != PLAYER)
			this.southDefeatable = true;
		if (locationStrength > westStrength && westOwner != PLAYER)
			this.westDefeatable = true;
		if (locationStrength > northStrength && northOwner != PLAYER)
			this.northDefeatable = true;

		if (eastDefeatable || southDefeatable || westDefeatable || northDefeatable)
			possibleConquest = true;

		if (locationStrength > locationProduction * 6)    //TODO can be optimized
			this.moveCommand = true;
	}

	Location bestProductionArea() {
		int minX, maxX, minY, maxY;

		if (x - scanArea < 0)
			minX = gameMap.width - (x - scanArea);
		else
			minX = x - scanArea;

		if (x + scanArea > gameMap.width)
			maxX = ((x + scanArea) - gameMap.width);
		else
			maxX = x + scanArea;

		if (y - scanArea < 0)
			minY = gameMap.height - (y - scanArea);
		else
			minY = y - scanArea;

		if (y + scanArea > gameMap.height)
			maxY = ((y + scanArea) - gameMap.height);
		else
			maxY = y + scanArea;

		int bestX = 0, bestY = 0, bestProduction = 0;

		for (int i = minX; i < maxX; i++) {
			if (x == i)
				continue;
			for (int j = minY; j < maxY; j++) {
				if (y == j)
					continue;
				Site tempSite = gameMap.getLocation(i, j).getSite();
				if (tempSite.owner == myID)
					continue;

				if (tempSite.production > bestProduction) {
					bestX = i;
					bestY = j;
					bestProduction = tempSite.production;
				} else {
					int tempDistance = abs(x - i) + abs(y - j);
					int bestDistance = abs(x - bestX) + abs(y - bestY);
					if (tempSite.production == bestProduction && tempDistance < bestDistance) {
						bestX = i;
						bestY = j;
						bestProduction = tempSite.production;
					}
				}
			}
		}
		return gameMap.getLocation(bestX, bestY);
	}

	Direction firstPhase(int bestX, int bestY) {
		if (locationStrength < locationProduction * 3)
			return Direction.STILL;

		if (bestX > x) {
			if (bestY > y) {    //N-E
				if (northDefeatable)
					return Direction.NORTH;
				else if (eastDefeatable)
					return Direction.EAST;
			}
			if (bestY < y) {    //E-S
				if (eastDefeatable)
					return Direction.EAST;
				else if (southDefeatable)
					return Direction.SOUTH;
			}
		} else if (bestX < x) {
			if (bestY < y) {    //S-W
				if (southDefeatable)
					return Direction.SOUTH;
				else if (westDefeatable)
					return Direction.WEST;
			}
			if (bestY > y) {    //W-N
				if (westDefeatable)
					return Direction.WEST;
				else if (northDefeatable)
					return Direction.NORTH;
			}
		}

		if (bestX > x) {
			if (bestY > y) {    //N-E
				if (northOwner == PLAYER)
					return Direction.NORTH;
				else if (eastOwner == PLAYER)
					return Direction.EAST;
			}
			if (bestY < y) {    //E-S
				if (eastOwner == PLAYER)
					return Direction.EAST;
				else if (southOwner == PLAYER)
					return Direction.SOUTH;
			}
		} else if (bestX < x) {
			if (bestY < y) {    //S-W
				if (southOwner == PLAYER)
					return Direction.SOUTH;
				else if (westOwner == PLAYER)
					return Direction.WEST;
			}
			if (bestY > y) {    //W-N
				if (westOwner == PLAYER)
					return Direction.WEST;
				else if (northOwner == PLAYER)
					return Direction.NORTH;
			}
		}

		if (possibleConquest) {
			if (eastDefeatable)
				return Direction.EAST;
			if (southDefeatable)
				return Direction.SOUTH;
			if (westDefeatable)
				return Direction.WEST;
			if (northDefeatable)
				return Direction.NORTH;
		}

		return Direction.STILL;
	}


	Direction closeNeutralPieces() {
		if (possibleConquest) {
			double eastCloseEfficiency = closeEfficiencyFormula(eastProduction, eastStrength + eastProduction);
			double southCloseEfficiency = closeEfficiencyFormula(southProduction, southStrength + southProduction);
			double westCloseEfficiency = closeEfficiencyFormula(westProduction, westStrength + westProduction);
			double northCloseEfficiency = closeEfficiencyFormula(northProduction, northStrength + northProduction);

			ArrayList<Double> closePiecesList = new ArrayList<>();
			closePiecesList.add(eastCloseEfficiency);
			closePiecesList.add(southCloseEfficiency);
			closePiecesList.add(westCloseEfficiency);
			closePiecesList.add(northCloseEfficiency);

			Collections.sort(closePiecesList);
			for (int i = closePiecesList.size() - 1; i >= 0; i--) {
				if (closePiecesList.get(i) > MINVALUE) {
					if (closePiecesList.get(i) == eastCloseEfficiency && eastDefeatable)
						return Direction.EAST;
					if (closePiecesList.get(i) == southCloseEfficiency && southDefeatable)
						return Direction.SOUTH;
					if (closePiecesList.get(i) == westCloseEfficiency && westDefeatable)
						return Direction.WEST;
					if (closePiecesList.get(i) == northCloseEfficiency && northDefeatable)
						return Direction.NORTH;
				}
			}
		}
		return Direction.STILL;

	}

	Direction movebyBestProductionArea(int scanArea) {
		int minX, maxX, minY, maxY;

		if (!possibleConquest)
			return Direction.STILL;

		if (x - scanArea < 0)
			minX = gameMap.width - (x - scanArea);
		else
			minX = x - scanArea;

		if (x + scanArea > gameMap.width)
			maxX = ((x + scanArea) - gameMap.width);
		else
			maxX = x + scanArea;

		if (y - scanArea < 0)
			minY = gameMap.height - (y - scanArea);
		else
			minY = y - scanArea;

		if (y + scanArea > gameMap.height)
			maxY = ((y + scanArea) - gameMap.height);
		else
			maxY = y + scanArea;

		int bestX = 0, bestY = 0, bestProduction = 0;
		for (int i = minX; i < maxX; i++) {
			if (x == i)
				continue;
			for (int j = minY; j < maxY; j++) {
				if (y == j)
					continue;
				Site tempSite = gameMap.getLocation(i, j).getSite();
				if (tempSite.owner == myID)
					continue;

				if (tempSite.production > bestProduction) {
					bestX = i;
					bestY = j;
					bestProduction = tempSite.production;
				} else {
					int tempDistance = abs(x - i) + abs(y - j);
					int bestDistance = abs(x - bestX) + abs(y - bestY);
					if (tempSite.production == bestProduction && tempDistance < bestDistance) {
						bestX = i;
						bestY = j;
						bestProduction = tempSite.production;
					}
				}
			}
		}

		//Daca gaseste adversar sa il poata bate
		if (bestX > x) {
			if (bestY > y) {    //N-E
				if (northDefeatable)
					return Direction.NORTH;
				else if (eastDefeatable)
					return Direction.EAST;
			}
			if (bestY < y) {    //E-S
				if (eastDefeatable)
					return Direction.EAST;
				else if (southDefeatable)
					return Direction.SOUTH;
			}
		} else if (bestX < x) {
			if (bestY < y) {    //S-W
				if (southDefeatable)
					return Direction.SOUTH;
				else if (westDefeatable)
					return Direction.WEST;
			}
			if (bestY > y) {    //W-N
				if (westDefeatable)
					return Direction.WEST;
				else if (northDefeatable)
					return Direction.NORTH;
			}
		}

		ArrayList<Integer> strengthList = new ArrayList<>();
		strengthList.add(eastStrength);
		strengthList.add(southStrength);
		strengthList.add(westStrength);
		strengthList.add(northStrength);
		Collections.sort(strengthList);

		for (int i = 3; i >= 0; i--) {
			if (strengthList.get(i) == eastStrength && eastOwner == PLAYER)
				return Direction.EAST;
			if (strengthList.get(i) == southStrength && southOwner == PLAYER)
				return Direction.SOUTH;
			if (strengthList.get(i) == westStrength && westOwner == PLAYER)
				return Direction.WEST;
			if (strengthList.get(i) == northStrength && northOwner == PLAYER)
				return Direction.NORTH;
		}
		return Direction.STILL;
	}

	private double closeEfficiencyFormula(double production, double strength) {
		if (locationStrength > strength)
			return production / strength;
		else
			return MINVALUE;
	}

	private int efficiencyFormula(int production, int strength, int steps) {
		return (production * 37 - (locationStrength - strength) * 5) / steps;
	}

	private int abs(int number) {
		return (number < 0) ? -number : number;
	}

	Direction movebyProduction() {
		if (possibleConquest) {
			ArrayList<Integer> productionList = new ArrayList<>();
			if (eastDefeatable)
				productionList.add(eastProduction);
			if (southDefeatable)
				productionList.add(southProduction);
			if (westDefeatable)
				productionList.add(westProduction);
			if (northDefeatable)
				productionList.add(northProduction);

			Collections.sort(productionList);
			int maxProduction = productionList.get(productionList.size() - 1);

			if (maxProduction == eastProduction && eastDefeatable)
				return Direction.EAST;
			if (maxProduction == southProduction && southDefeatable)
				return Direction.SOUTH;
			if (maxProduction == westProduction && westDefeatable)
				return Direction.WEST;
			if (maxProduction == northProduction && northDefeatable)
				return Direction.NORTH;
		}
		return Direction.STILL;
	}

	Direction movebyEfficiency() {
		if (possibleConquest) {
			ArrayList<Integer> efficiencyList = new ArrayList<>();
			int northEfficiency = efficiencyFormula(northProduction, northStrength, 1);
			int eastEfficiency = efficiencyFormula(eastProduction, eastStrength, 1);
			int southEfficiency = efficiencyFormula(southProduction, southStrength, 1);
			int westEfficiency = efficiencyFormula(westProduction, westStrength, 1);
			if (eastDefeatable)
				efficiencyList.add(eastEfficiency);
			if (southDefeatable)
				efficiencyList.add(southEfficiency);
			if (westDefeatable)
				efficiencyList.add(westEfficiency);
			if (northDefeatable)
				efficiencyList.add(northEfficiency);

			Collections.sort(efficiencyList);

			for (int i = efficiencyList.size() - 1; i >= 0; i--) {
				if (efficiencyList.get(i) == eastEfficiency && eastDefeatable)
					return Direction.EAST;
				if (efficiencyList.get(i) == southEfficiency && southDefeatable)
					return Direction.SOUTH;
				if (efficiencyList.get(i) == westEfficiency && westDefeatable)
					return Direction.WEST;
				if (efficiencyList.get(i) == northEfficiency && northDefeatable)
					return Direction.NORTH;
			}
		}
		return Direction.STILL;
	}

	Direction moveInnerPieces() {
		Integer eastSteps = 0, southSteps = 0, westSteps = 0, northSteps = 0;
		Location goEast = location, goSouth = location, goWest = location, goNorth = location;

		if (!moveCommand)
			return Direction.STILL;

		while (goEast.getSite().owner == PLAYER && eastSteps <= gameMap.width) {
			eastSteps++;
			goEast = gameMap.getLocation(goEast, Direction.EAST);
		}
		while (goSouth.getSite().owner == PLAYER && southSteps <= gameMap.height) {
			southSteps++;
			goSouth = gameMap.getLocation(goSouth, Direction.SOUTH);
		}
		while (goWest.getSite().owner == PLAYER && westSteps <= gameMap.width) {
			westSteps++;
			goWest = gameMap.getLocation(goWest, Direction.WEST);
		}
		while (goNorth.getSite().owner == PLAYER && northSteps <= gameMap.height) {
			northSteps++;
			goNorth = gameMap.getLocation(goNorth, Direction.NORTH);
		}

	/*	//Daca exista adversar in apropiere, se intareste frontiera
		if ((eastSteps == 3 || eastSteps == 4) && goEast.getSite().owner == ENEMY && (locationStrength + eastStrength + eastProduction < 255))
			return Direction.EAST;
		if ((southSteps == 3 || southSteps == 4) && goSouth.getSite().owner == ENEMY && (locationStrength + southStrength + southProduction < 255))
			return Direction.SOUTH;
		if ((westSteps == 3 || westSteps == 4) && goWest.getSite().owner == ENEMY && (locationStrength + westStrength + westProduction < 255))
			return Direction.WEST;
		if ((northSteps == 3 || northSteps == 4) && goNorth.getSite().owner == ENEMY && (locationStrength + northStrength + northProduction < 255))
			return Direction.NORTH;
*/

		//Daca exista adversar in apropiere, se intareste frontiera
		if (eastSteps == 3 && goEast.getSite().owner == ENEMY && (locationStrength + eastStrength + eastProduction < 255))
			return Direction.EAST;
		if (southSteps == 3 && goSouth.getSite().owner == ENEMY && (locationStrength + southStrength + southProduction < 255))
			return Direction.SOUTH;
		if (westSteps == 3 && goWest.getSite().owner == ENEMY && (locationStrength + westStrength + westProduction < 255))
			return Direction.WEST;
		if (northSteps == 3 && goNorth.getSite().owner == ENEMY && (locationStrength + northStrength + northProduction < 255))
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

		ArrayList<Integer> stepsList = new ArrayList<>(4);
		stepsList.add(eastSteps);
		stepsList.add(southSteps);
		stepsList.add(westSteps);
		stepsList.add(northSteps);

		Collections.sort(stepsList);

		for (int i = 0; i < stepsList.size(); i++) {
			if (eastSteps == stepsList.get(i) && (locationStrength + eastStrength + eastProduction < 255))
				return Direction.EAST;
			if (southSteps == stepsList.get(i) && (locationStrength + southStrength + southProduction < 255))
				return Direction.SOUTH;
			if (westSteps == stepsList.get(i) && (locationStrength + westStrength + westProduction < 255))
				return Direction.WEST;
			if (northSteps == stepsList.get(i) && (locationStrength + northStrength + northProduction < 255))
				return Direction.NORTH;
		}
/*
		ArrayList <Integer> minStrengthLoseList = new ArrayList<>();
		minStrengthLoseList.add(locationStrength + eastStrength + eastProduction);
		minStrengthLoseList.add(locationStrength + southStrength + southProduction);
		minStrengthLoseList.add(locationStrength + westStrength + westProduction);
		minStrengthLoseList.add(locationStrength + northStrength + northProduction);
		int minStrengthLose = Collections.min(minStrengthLoseList);

			if (eastSteps == minStrengthLose)
				return Direction.EAST;
			if (southSteps == minStrengthLose)
				return Direction.SOUTH;
			if (westSteps == minStrengthLose)
				return Direction.WEST;
			if (northSteps == minStrengthLose)
				return Direction.NORTH;
*/

		for (int i = 0; i < stepsList.size(); i++) {
			if (eastSteps == stepsList.get(i))
				return Direction.EAST;
			if (southSteps == stepsList.get(i))
				return Direction.SOUTH;
			if (westSteps == stepsList.get(i))
				return Direction.WEST;
			if (northSteps == stepsList.get(i))
				return Direction.NORTH;
		}
		return Direction.STILL;
	}


}
