import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MyBot {
	public static void main(String[] args) throws java.io.IOException {

		final InitPackage iPackage = Networking.getInit();
		final int myID = iPackage.myID;
		final GameMap gameMap = iPackage.map;
		Networking.sendInit("V17");

		int turn = 0;
		while (true) {
			List<Move> moves = new ArrayList<Move>();

			Networking.updateFrame(gameMap);

			turn++;
			for (int y = 0; y < gameMap.height; y++) {
				for (int x = 0; x < gameMap.width; x++) {
					final Location location = gameMap.getLocation(x, y);
					final Site site = location.getSite();
					if (site.owner == myID) {
						Actions decisionMaker = new Actions(gameMap, myID, x, y, turn);

						moves.add(new Move(location, decisionMaker.conquer()));
						//moves.add(new Move(location, Direction.randomDirection()));
					}
				}
			}
			Networking.sendFrame(moves);
		}
	}
}


class Actions {
	final int StrengthCap = 255;
	int x, y, turn;
	int eastOwner, southOwner, westOwner, northOwner;
	Location location, eastLocation, southLocation, westLocation, northLocation;
	Location eastTwoLocation, southTwoLocation, westTwoLocation, northTwoLocation;
	Location eastThreeLocation, southThreeLocation, westThreeLocation, northThreeLocation;
	Site site, eastSite, southSite, westSite, northSite;
	Site eastTwoSite, southTwoSite, westTwoSite, northTwoSite;
	Site eastThreeSite, southThreeSite, westThreeSite, northThreeSite;
	double locationStrength, eastStrength, southStrength, westStrength, northStrength;
	double locationProduction, eastProduction, southProduction, westProduction, northProduction;
	GameMap gameMap;
	int myID;
	boolean moveCommand;
	boolean possibleConquest, eastDefeatable, southDefeatable, westDefeatable, northDefeatable;

	Actions(GameMap gameMap, int myID, int x, int y, int turn) {
		this.gameMap = gameMap;
		this.myID = myID;
		this.x = x;
		this.y = y;
		// distanta de o celula
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
		this.eastOwner = eastSite.owner;
		this.southOwner = southSite.owner;
		this.westOwner = westSite.owner;
		this.northOwner = northSite.owner;

		//distanta de doua celule
		this.eastTwoLocation = gameMap.getLocation(eastLocation, Direction.EAST);
		this.southTwoLocation = gameMap.getLocation(southLocation, Direction.SOUTH);
		this.westTwoLocation = gameMap.getLocation(westLocation, Direction.WEST);
		this.northTwoLocation = gameMap.getLocation(northLocation, Direction.NORTH);
		this.eastTwoSite = eastTwoLocation.getSite();
		this.southTwoSite = southTwoLocation.getSite();
		this.westTwoSite = westTwoLocation.getSite();
		this.northTwoSite = northTwoLocation.getSite();

		//distanta de trei celule
		this.eastThreeLocation = gameMap.getLocation(eastTwoLocation, Direction.EAST);
		this.southThreeLocation = gameMap.getLocation(southTwoLocation, Direction.SOUTH);
		this.westThreeLocation = gameMap.getLocation(westTwoLocation, Direction.WEST);
		this.northThreeLocation = gameMap.getLocation(northTwoLocation, Direction.NORTH);
		this.eastThreeSite = eastThreeLocation.getSite();
		this.southThreeSite = southThreeLocation.getSite();
		this.westThreeSite = westThreeLocation.getSite();
		this.northThreeSite = northThreeLocation.getSite();

		//Setare celule alaturate ce pot fi cucerite

		if (locationStrength > eastStrength + eastProduction && eastOwner != myID)
			this.eastDefeatable = true;
		if (locationStrength > southStrength + southProduction && southOwner != myID)
			this.southDefeatable = true;
		if (locationStrength > westStrength + westProduction && westOwner != myID)
			this.westDefeatable = true;
		if (locationStrength > northStrength + northProduction && northOwner != myID)
			this.northDefeatable = true;

		if (eastDefeatable || southDefeatable || westDefeatable || northDefeatable)
			possibleConquest = true;

		if (locationStrength >= locationProduction * 6 || (checkIfEnemy(location) && possibleConquest))    //TODO can be optimized
			this.moveCommand = true;


	}

	Direction conquer() {

		if (!moveCommand && turn < 30)
			return Direction.STILL;

		if (eastOwner == myID && southOwner == myID && westOwner == myID && northOwner == myID)
			return moveInnerPieces();

		ArrayList<Direction> lista = new ArrayList<>(4);
		lista.add(Direction.EAST);
		lista.add(Direction.SOUTH);
		lista.add(Direction.WEST);
		lista.add(Direction.NORTH);

		Collections.sort(lista, new Comparator<Direction>() {
			public int compare(Direction c1, Direction c2) {
				if (efficiencyFormula(c1) > efficiencyFormula(c2)) return 1;
				if (efficiencyFormula(c1) < efficiencyFormula(c2)) return -1;
				return 0;
			}
		});

		for (int i = 3; i >= 0; i--) {
			Site siteToAttack = gameMap.getLocation(location, lista.get(i)).getSite();
			if (turn < 30 && locationStrength > siteToAttack.strength)
				if (siteToAttack.owner != myID && !checkIfEnemy(gameMap.getLocation(gameMap.getLocation(location, lista.get(i)), lista.get(i))))
					return lista.get(i);
			if (locationStrength - locationStrength * 0.25 > siteToAttack.strength) {
				if (siteToAttack.owner != myID)
					return lista.get(i);
			}
		}

		return Direction.STILL;
	}

	double efficiencybySteps(Location tempLocation, double steps) {
		return ((tempLocation.getSite().production * tempLocation.getSite().production) + (locationStrength - tempLocation.getSite().strength)) / steps;
	}
	double efficiencyFormula(Direction direction) {

		if (direction == Direction.EAST) {
			if (eastStrength != 0 && eastTwoSite.strength != 0 && eastThreeSite.strength != 0)
				return (eastProduction * eastProduction) / eastStrength + 0.75 * (eastTwoSite.production * eastTwoSite.production) / eastTwoSite.strength + 0.5 * (eastThreeSite.production / eastThreeSite.strength);
			else
				return eastProduction + 0.75 * eastTwoSite.production + 0.5 * eastThreeSite.production;
		}

		if (direction == Direction.SOUTH) {
			if (southStrength != 0 && southTwoSite.strength != 0 && southThreeSite.strength != 0)
				return (southProduction * southProduction) / southStrength + 0.75 * (southTwoSite.production * southTwoSite.production) / southTwoSite.strength + 0.5 * (southThreeSite.production / southThreeSite.strength);
			else
				return southProduction + 0.75 * southTwoSite.production + 0.5 * southThreeSite.production;
		}

		if (direction == Direction.WEST) {
			if (westStrength != 0 && westTwoSite.strength != 0 && westThreeSite.strength != 0)
				return (westProduction * westProduction) / westStrength + 0.75 * (westTwoSite.production * westTwoSite.production) / westTwoSite.strength + 0.5 * (westThreeSite.production / westThreeSite.strength);
			else
				return westProduction + 0.75 * westTwoSite.production + 0.5 * westThreeSite.production;
		}

		if (direction == Direction.NORTH) {
			if (northStrength != 0 && northTwoSite.strength != 0 && northThreeSite.strength != 0)
				return (northProduction * northProduction) / northStrength + 0.75 * (northTwoSite.production * northTwoSite.production) / northTwoSite.strength + 0.5 * (northThreeSite.production / northThreeSite.strength);
			else
				return northProduction + 0.75 * northTwoSite.production + 0.5 * northThreeSite.production;
		}

		return 0;
	}

	Direction moveInnerPieces() {
		int eastSteps = 0, southSteps = 0, westSteps = 0, northSteps = 0;
		Location eastTemp = location;
		Location southTemp = location;
		Location westTemp = location;
		Location northTemp = location;

		while (eastTemp.getSite().owner == myID || eastSteps < gameMap.width - 1) {
			eastSteps++;
			eastTemp = gameMap.getLocation(eastTemp, Direction.EAST);
		}

		while (southTemp.getSite().owner == myID || southSteps < gameMap.height - 1) {
			southSteps++;
			southTemp = gameMap.getLocation(southTemp, Direction.SOUTH);
		}

		while (westTemp.getSite().owner == myID || westSteps < gameMap.width - 1) {
			westSteps++;
			westTemp = gameMap.getLocation(westTemp, Direction.WEST);
		}

		while (northTemp.getSite().owner == myID || northSteps < gameMap.height - 1) {
			northSteps++;
			northTemp = gameMap.getLocation(northTemp, Direction.NORTH);
		}

/*
		ArrayList<Integer> lista = new ArrayList<>(4);
		lista.add(eastSteps);
		lista.add(southSteps);
		lista.add(westSteps);
		lista.add(northSteps);

		Collections.sort(lista);

*/

		ArrayList<Location> lista = new ArrayList<>(4);
		lista.add(eastTemp);
		lista.add(southTemp);
		lista.add(westTemp);
		lista.add(northTemp);
		Collections.sort(lista, new Comparator<Location>() {
			public int compare(Location c1, Location c2) {
				if (efficiencybySteps(c1, gameMap.getDistance(location, c1)) > efficiencybySteps(c2, gameMap.getDistance(location, c2)))
					return 1;
				if (efficiencybySteps(c1, gameMap.getDistance(location, c1)) < efficiencybySteps(c2, gameMap.getDistance(location, c2)))
					return -1;
				if (gameMap.getDistance(location, c1) > gameMap.getDistance(location, c2)) return 1;
				if (gameMap.getDistance(location, c1) < gameMap.getDistance(location, c2)) return -1;
				return 0;
			}
		});

		for (int i = 3; i >= 0; i--) {
			if (locationStrength > lista.get(i).getSite().strength) {
				if (lista.get(i) == eastTemp && (locationStrength + eastStrength < StrengthCap) && eastStrength < eastProduction * 3)
					return Direction.EAST;
				if (lista.get(i) == southTemp && (locationStrength + southStrength < StrengthCap) && southStrength < southProduction * 3)
					return Direction.SOUTH;
				if (lista.get(i) == westTemp && (locationStrength + westStrength < StrengthCap) && westStrength < westProduction * 3)
					return Direction.WEST;
				if (lista.get(i) == northTemp && (locationStrength + northStrength < StrengthCap) && northStrength < northProduction * 3)
					return Direction.NORTH;
			}
		}

		for (int i = 3; i >= 0; i--) {
			if (locationStrength > lista.get(i).getSite().strength) {
				if (lista.get(i) == eastTemp)
					return Direction.EAST;
				if (lista.get(i) == southTemp)
					return Direction.SOUTH;
				if (lista.get(i) == westTemp)
					return Direction.WEST;
				if (lista.get(i) == northTemp)
					return Direction.NORTH;
			}
		}

		/*
		if (lista.get(0) == eastSteps)
			return Direction.EAST;
		if (lista.get(0) == southSteps)
			return Direction.SOUTH;
		if (lista.get(0) == westSteps)
			return Direction.WEST;
		if (lista.get(0) == northSteps)
			return Direction.NORTH;

/*
		if (lista.get(0) <= 2) {
			if (eastStrength < eastTwoSite.strength * 1.5)
				return Direction.EAST;
			if (southStrength < southTwoSite.strength * 1.5)
				return Direction.SOUTH;
			if (westStrength < westTwoSite.strength * 1.5)
				return Direction.WEST;
			if (northStrength < northTwoSite.strength * 1.5)
				return Direction.NORTH;
			return Direction.STILL;
		}


		//in caz de adversar, trimite piese
		for (int i = 0; i < 2; i++) {
			if (lista.get(i) == eastSteps && (locationStrength + eastStrength + eastProduction <= StrengthCap) && checkIfEnemy(eastTemp) && locationStrength > eastTemp.getSite().strength)
				return Direction.EAST;
			if (lista.get(i) == southSteps && (locationStrength + southStrength + southProduction <= StrengthCap) && checkIfEnemy(southTemp) && locationStrength > southTemp.getSite().strength)
				return Direction.SOUTH;
			if (lista.get(i) == westSteps && (locationStrength + westStrength + westProduction <= StrengthCap) && checkIfEnemy(westTemp) && locationStrength > westTemp.getSite().strength)
				return Direction.WEST;
			if (lista.get(i) == northSteps && (locationStrength + northStrength + northProduction <= StrengthCap) && checkIfEnemy(northTemp) && locationStrength > northTemp.getSite().strength)
				return Direction.NORTH;
		}


		//Varianta ideala fara strength loss si cu numar acceptabil de pasi
		for (int i = 0; i < 2; i++) {
			if (lista.get(i) == eastSteps && (locationStrength + eastStrength + eastProduction <= StrengthCap) && checkIfMoveHere(eastLocation))
				return Direction.EAST;
			if (lista.get(i) == southSteps && (locationStrength + southStrength + southProduction <= StrengthCap) && checkIfMoveHere(southLocation))
				return Direction.SOUTH;
			if (lista.get(i) == westSteps && (locationStrength + westStrength + westProduction <= StrengthCap) && checkIfMoveHere(westLocation))
				return Direction.WEST;
			if (lista.get(i) == northSteps && (locationStrength + northStrength + northProduction <= StrengthCap) && checkIfMoveHere(northLocation))
				return Direction.NORTH;
		}


		for (int i = 0; i < 2; i++) {
			if (lista.get(i) == eastSteps && (locationStrength + eastStrength + eastProduction <= StrengthCap + 100))
				return Direction.EAST;
			if (lista.get(i) == southSteps && (locationStrength + southStrength + southProduction <= StrengthCap + 100))
				return Direction.SOUTH;
			if (lista.get(i) == westSteps && (locationStrength + westStrength + westProduction <= StrengthCap + 100))
				return Direction.WEST;
			if (lista.get(i) == northSteps && (locationStrength + northStrength + northProduction <= StrengthCap + 100))
				return Direction.NORTH;
		}

		if (lista.get(0) == eastSteps)
			return Direction.EAST;
		if (lista.get(0) == southSteps)
			return Direction.SOUTH;
		if (lista.get(0) == westSteps)
			return Direction.WEST;
		if (lista.get(0) == northSteps)
			return Direction.NORTH;
*/
		return Direction.STILL;
	}

	Boolean checkIfEnemy(Location tempLocation) {
		Site eastTempSite = gameMap.getLocation(tempLocation, Direction.EAST).getSite();
		Site southTempSite = gameMap.getLocation(tempLocation, Direction.SOUTH).getSite();
		Site westTempSite = gameMap.getLocation(tempLocation, Direction.SOUTH).getSite();
		Site northTempSite = gameMap.getLocation(tempLocation, Direction.SOUTH).getSite();
		if (tempLocation.getSite().owner != myID)
			if ((eastTempSite.owner != myID && eastTempSite.owner != 0) ||
				(southTempSite.owner != myID && southTempSite.owner != 0) ||
				(westTempSite.owner != myID && westTempSite.owner != 0) ||
				(northTempSite.owner != myID && northTempSite.owner != 0))
				return Boolean.TRUE;

		return Boolean.FALSE;
	}


	Boolean checkIfMoveHere(Location tempLocation) {
		Site tempSite = tempLocation.getSite();
		if (tempSite.strength < tempSite.production * 4 && tempSite.production > 4)
			return Boolean.FALSE;
		return Boolean.TRUE;
	}
}