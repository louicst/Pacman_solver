package logic;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
//import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.TreeSet;

import data.Map;
import view.Gomme;

/**
 * an object Position correspond to a position in the Pacman grid
 */
class Position implements Comparable{
	public int x, y;
	public char dir;

	/**
	 * construct a new Object position corresponding to the position of an entity (ghost or pacman) in the grid
	 * @param x row
	 * @param y column
	 * @param dir direction followed by the entity
	 */
	public Position(int x, int y, char dir) {
		this.x = x;
		this.y = y;
		this.dir = dir;
	}
	
	/**
	 * return the row index
	 * @return the row index
	 */
	int getRow() {
		return this.x;
	}
	
	/**
	 * return the column index
	 * @return the column index
	 */
	int getColumn() {
		return this.y;
	}
	
	/**
	 * return direction (among 'U', 'D', 'L', 'R')
	 * @return
	 */
	char getDirection() {
		return this.dir;
	}

	public String toString() {
		return "(" + this.x + "," + this.y + ") " + this.dir;
	}

	/**
	 * construct a copie of a given position
	 * @param pos
	 */
	public Position clone() {
		return new Position(this.x, this.y, this.dir);
	}
	
	
	/**
	 * used to compare two positions
	 * @return 0 if the two positions are the same
	 */
	public int compareTo(Object o) {
		Position pos = (Position)o;
		int comp = this.x - pos.x;
		if(comp != 0)
			return comp;
		comp = this.y - pos.y;
		if(comp != 0)
			return comp;
		comp = this.dir - pos.dir;
		if(comp != 0)
			return comp;
		return 0; 
	}
}

/**
 * an object BeliefState represents all relevant information about the game.
 */
public class BeliefState implements Comparable{
	private char map[][];
	private ArrayList<TreeSet<Position>> listPGhost;
	private Position pacmanPos, pacmanOldPos;
	private int nbrOfGommes, nbrOfSuperGommes, score, life;
	private ArrayList<Integer> compteurPeur;
	private static ArrayList<int[]> gamePositions;
	private static HashSet<String> visible;
	private static int pacmanXInit, pacmanYInit;
	private static ArrayList<int[]> listPGhostInit;
	private static int tailleCase;
	private static int taille;
	
	
	public static void setStaticVariables(ArrayList<int[]> gamePositions, HashSet<String> visible, int pacmanXInit, int pacmanYInit, ArrayList<int[]> listPGhostInit, int tailleCase, int taille) {
		BeliefState.gamePositions = gamePositions;
		BeliefState.visible = visible;
		BeliefState.pacmanXInit = pacmanXInit;
		BeliefState.pacmanYInit = pacmanYInit;
		BeliefState.listPGhostInit = listPGhostInit;
		BeliefState.tailleCase = tailleCase;
		BeliefState.taille = taille;
	}
	/**
	 * create a new BeliefState object
	 * @param theMap the current version of the Pacman man discribing the position of (super) gums
	 * @param score the current score
	 * @param life the number of remaining lifes for Pacman
	 */
	public BeliefState(int taille, int score, int life) {
		BeliefState.taille = taille;
		this.map = new char[BeliefState.taille][BeliefState.taille];
		this.pacmanPos = new Position(0,0,'U');
		this.pacmanOldPos = this.pacmanPos;
		this.listPGhost = new ArrayList<TreeSet<Position>>();
		this.nbrOfGommes = 0;
		this.score = score;
		this.compteurPeur = new ArrayList<Integer>();
		this.life = life;
	}
	
	/*public BeliefState(InputStream in) {
		Scanner scan = new Scanner(in);
		BeliefState.taille = scan.nextInt();
		scan.nextLine();
		this.map = new char[BeliefState.taille][BeliefState.taille];
		for(int i = 0; i < BeliefState.taille; i++) {
			String line = scan.nextLine();
			for(int j = 0; j < BeliefState.taille; j++) {
				this.map[i][j] = line.charAt(j);
			}
		}
		int pacmanPosX = scan.nextInt(), pacmanPosY = scan.nextInt();
		BeliefState.pacmanXInit = scan.nextInt();
		BeliefState.pacmanYInit = scan.nextInt();
		BeliefState.tailleCase = scan.nextInt();
		scan.nextLine();
		String line = scan.nextLine();
		this.pacmanPos = new Position(pacmanPosX, pacmanPosY, line.charAt(0));
		this.score = scan.nextInt();
		this.life = scan.nextInt();
		this.nbrOfGommes = scan.nextInt();
		this.nbrOfSuperGommes = scan.nextInt();
		int sizeListPGhost = scan.nextInt();
		this.listPGhost = new ArrayList<TreeSet<Position>>();
		this.compteurPeur = new ArrayList<Integer>();
		BeliefState.listPGhostInit = new ArrayList<int[]>();
		for(int i = 0; i < sizeListPGhost; i++) {
			TreeSet<Position> posGhost = new TreeSet<Position>();
			this.compteurPeur.add(scan.nextInt());
			int nbrPos = scan.nextInt();
			for(int index = 0; index < nbrPos; index++) {
				int x = scan.nextInt();
				int y = scan.nextInt();
				line = scan.nextLine();
				char dir = line.charAt(1);
				Position posG = new Position(x, y, dir);
				posGhost.add(posG);
			}
			this.listPGhost.add(posGhost);
			int[] posG = new int[2];
			posG[0] = scan.nextInt();
			posG[1] = scan.nextInt();
			BeliefState.listPGhostInit.add(posG);
		}
		int gamePositionSize = scan.nextInt();
		BeliefState.gamePositions = new ArrayList<int[]>();
		for(int index = 0; index < gamePositionSize; index++) {
			int[] posCell = new int[2];
			posCell[0] = scan.nextInt();
			posCell[1] = scan.nextInt();
			BeliefState.gamePositions.add(posCell);
		}
		while(scan.hasNext()) {
			line = scan.nextLine();
			BeliefState.visible.add(line);
		}
	}*/
	
	public int compareTo(Object o) {
		BeliefState bs = (BeliefState) o;
		int comp = this.pacmanPos.compareTo(bs.pacmanPos);
		if(comp != 0)
			return comp;
		comp = this.life - bs.life;
		if(comp != 0)
			return comp;
		comp = this.score - bs.score;
		if(comp != 0)
			return comp;
		comp = this.nbrOfGommes - bs.nbrOfGommes;
		if(comp != 0)
			return comp;
		comp = this.nbrOfSuperGommes - bs.nbrOfSuperGommes;
		if(comp != 0)
			return comp;
		for(int[] pos:BeliefState.gamePositions) {
			comp = this.map[pos[0]][pos[1]] - bs.map[pos[0]][pos[1]];
			if(comp != 0)
				return comp;
		}
		for(int i = 0; i < this.compteurPeur.size(); i++) {
			comp = this.compteurPeur.get(i) - bs.compteurPeur.get(i);
			if(comp != 0)
				return comp;
		}
		for(int i = 0; i < this.listPGhost.size(); i++) {
			TreeSet<Position> posGhost1 = this.listPGhost.get(i), posGhost2 = bs.listPGhost.get(i);
			comp = posGhost1.size() - posGhost2.size();
			if(comp != 0)
				return comp;
			Iterator<Position> iterPos1 = posGhost1.descendingIterator(), iterPos2 = posGhost2.descendingIterator();
			for(Position pos1 = iterPos1.next(), pos2 = iterPos2.next(); iterPos1.hasNext(); pos1 = iterPos1.next(), pos2 = iterPos2.next()) {
				comp = pos1.compareTo(pos2);
				if(comp != 0)
					return comp;
			}
		}
		return 0;
	}
	
	/**
	 * construct a copy of the state
	 * @param toCopy BeliefState object to be copied
	 * @param isDead if true then Pacman is dead and the status of the status should be updated accordingly
	 */

	public BeliefState(BeliefState toCopy, boolean isDead) {
		this(BeliefState.taille, toCopy.score, toCopy.life);
		this.pacmanPos.dir = toCopy.pacmanPos.dir;
		for(int i = 0; i < this.map.length; i++) {
			for(int j = 0; j < this.map[i].length; j++) {
				this.modifyMap(i, j, toCopy.map[i][j]);
			}
		}
		this.pacmanOldPos = toCopy.pacmanOldPos.clone();
		if(!isDead) {
			this.listPGhost.clear();
			for(TreeSet<Position> listP: toCopy.listPGhost) {
				TreeSet<Position> newListP = new TreeSet<Position>();
				for(Position pos: listP.descendingSet()) {
					newListP.add(pos.clone());
				}
				this.listPGhost.add(newListP);
			}
			for(int i = 0; i < toCopy.compteurPeur.size(); i++) {
				this.compteurPeur.set(i, toCopy.compteurPeur.get(i));
			}
		}
		else {
			this.life = toCopy.life - 1;
			this.moveTo(BeliefState.pacmanYInit / BeliefState.tailleCase, BeliefState.pacmanXInit / BeliefState.tailleCase, 'U');
		}
	}

	/**
	 * update the status of one square
	 * @param i row of the square
	 * @param j column of the square
	 * @param val value coressponding to the content of the square
	 */
	public void modifyMap(int i, int j, char val) {
		switch(val) {
		case '.': nbrOfGommes++; break;
		case '*': nbrOfGommes++; nbrOfSuperGommes++; break;
		case 'P': this.pacmanPos.x = i;this.pacmanPos.y = j; break;
		case 'F': TreeSet<Position> posGhost = new TreeSet<Position>(); Position pos = new Position(i, j, 'U'); posGhost.add(pos); this.listPGhost.add(posGhost); this.compteurPeur.add(0);  break;
		case 'B': this.pacmanPos.x = i;this.pacmanPos.y = j; TreeSet<Position> posGhost2 = new TreeSet<Position>(); Position pos2 = new Position(i, j, 'U'); posGhost2.add(pos2); this.listPGhost.add(posGhost2); this.compteurPeur.add(0); break;
		}
		this.map[i][j] = val;
	}

	/**
	 * returns the current score
	 * @return current score
	 */
	public int getScore() {
		return this.score;
	}

	/**
	 * create all possible states resulting from a given action of Pacman
	 * @param toward describe the action performed by Pacman (PacmanLuncher.UP/DOWN/LEFT/RIGHT)
	 * @return list of possible states that can be the results of the action performed by Pacman
	 */
	public Result extendsBeliefState(String toward) {
		BeliefState stateRemoved = null;
		ArrayList<BeliefState> listAlternativeBeliefState = new ArrayList<BeliefState>();
		BeliefState currentBeliefState = null;
		char currentPos = this.map[this.pacmanPos.x][this.pacmanPos.y];
		switch(toward.charAt(0)) {
		case 'U': if(pacmanPos.x > 0) {
			char nextPos = this.map[this.pacmanPos.x - 1][this.pacmanPos.y];
			if(nextPos != '#') {
				currentBeliefState = this.move(-1, 0, nextPos, 'U');
				if(nextPos == '*') {
					for(int i = 0; i < currentBeliefState.compteurPeur.size(); i++) {
						currentBeliefState.compteurPeur.set(i, Ghost.TIME_PEUR);
					}
				}
			}
			else {
				currentBeliefState = this.move(0, 0, currentPos, 'U');
			}
		} else {
			currentBeliefState = this.move(0, 0, currentPos, 'U');
		} break;
		case 'D': if(this.pacmanPos.x + 1 < this.map.length) {
			char nextPos = this.map[this.pacmanPos.x + 1][this.pacmanPos.y];
			if(nextPos != '#') {
				currentBeliefState = this.move(1, 0, nextPos, 'D');
				if(nextPos == '*') {
					for(int i = 0; i < currentBeliefState.compteurPeur.size(); i++) {
						currentBeliefState.compteurPeur.set(i, Ghost.TIME_PEUR);
					}
				}
			}
			else {
				currentBeliefState = this.move(0, 0, currentPos, 'D');
			}
		}
		else{
			currentBeliefState = this.move(0, 0, currentPos, 'D');
		} break;
		case 'L': if(this.pacmanPos.y > 0) {
			char nextPos = this.map[this.pacmanPos.x][this.pacmanPos.y - 1];
			if(nextPos != '#') {
				currentBeliefState = this.move(0, -1, nextPos, 'L');
				if(nextPos == '*') {
					for(int i = 0; i < currentBeliefState.compteurPeur.size(); i++) {
						currentBeliefState.compteurPeur.set(i, Ghost.TIME_PEUR);
					}
				}
			}
			else {
				currentBeliefState = this.move(0, 0, currentPos, 'L');
			}
		}
		else{
			currentBeliefState = this.move(0, 0, currentPos, 'L');
		} break;
		case 'R': if(this.pacmanPos.y + 1 < this.map[0].length) {
			char nextPos = this.map[this.pacmanPos.x][this.pacmanPos.y + 1];
			if(nextPos != '#') {
				currentBeliefState = this.move(0, 1, nextPos, 'R');
				if(nextPos == '*') {
					for(int i = 0; i < currentBeliefState.compteurPeur.size(); i++) {
						currentBeliefState.compteurPeur.set(i, Ghost.TIME_PEUR);
					}
				}
			}
			else {
				currentBeliefState = this.move(0, 0, currentPos, 'R');
			}
		}
		else {
			currentBeliefState = this.move(0, 0, currentPos, 'R');
		} break;
		}

		boolean dead = false;
		int l = 0;
		for(TreeSet<Position> treeSet: this.listPGhost) {//test pour chaque ghost si il se trouve sur la case de PacMan et qu'il n'a pas peur (en gros PacMan mort)
			if(currentBeliefState.compteurPeur.get(l++) == 0 && treeSet.size() == 1) {//ghost n'a pas peur et une seule position possible
				Position pos = treeSet.first();
				if(pos.x == currentBeliefState.pacmanPos.x && pos.y == currentBeliefState.pacmanPos.y) {//si le PacMan s'est deplace a la place du ghost
					dead = true;//le PacMan est mort
					break;
				}
			}
		}
		if(dead) {
			listAlternativeBeliefState.add(new BeliefState(currentBeliefState, true));//ajoute un etat ou PacMan est mort
		}
		else {
			listAlternativeBeliefState.add(currentBeliefState);
			for(int k = 0; k < currentBeliefState.compteurPeur.size(); k++) {//pour chaque fantome
				ArrayList<BeliefState> tempListAlternativeBeliefState = new ArrayList<BeliefState>();

				for(int indexBeliefState = 0; indexBeliefState < listAlternativeBeliefState.size(); indexBeliefState++) {//pour chaque BeliefState deja trouve
					BeliefState state = listAlternativeBeliefState.get(indexBeliefState); 
					int compteurPeur = state.compteurPeur.get(k);
					if (compteurPeur > 0) {//decremente le compteur de peur
						state.compteurPeur.set(k, compteurPeur - 2);
					}
					TreeSet<Position> posGhost = state.listPGhost.get(k), newPosGhost = new TreeSet<Position>();
					Iterator<Position> itPos = posGhost.iterator();
					HashSet<String> hAlternativePos = new HashSet<String>();
					while(itPos.hasNext()) {//pour chauqe position possible du ghost
						Position posG = itPos.next();
						boolean haveMoved = false;
						if(BeliefState.isVisible(posG.x, posG.y, this.pacmanPos.x, this.pacmanPos.y) && compteurPeur == 0) {//si le ghost est visible et n'est pas effraye
							/*if(posGhost.size() > 1) {
								Position newPos = posG.clone();
								BeliefState actualBeliefState = new BeliefState(state, false);
								actualBeliefState.listPGhost.get(k).clear();
								boolean isDead = false;
								if(newPos.x > state.pacmanPos.x) {
									newPos.x--;
									newPos.dir = 'U';
									if(newPos.x == state.pacmanPos.x)
										isDead = true;
								}
								else {
									if(newPos.x < state.pacmanPos.x) {
										newPos.x++;
										newPos.dir = 'D';
										if(newPos.x == state.pacmanPos.x)
											isDead = true;
									}
									else {
										if(newPos.y < state.pacmanPos.y) {
											newPos.y++;
											newPos.dir = 'R';
											if(newPos.y == state.pacmanPos.y)
												isDead = true;
										}
										else {
											if(newPos.y > state.pacmanPos.y) {
												newPos.y--;
												newPos.dir = 'L';
												if(newPos.y == state.pacmanPos.y)
													isDead = true;
											}
											else
												isDead = true;
										}
									}
								}
								if(isDead) {
									if(stateRemoved == null)
										stateRemoved = new BeliefState(actualBeliefState, true);
								}
								else {
									actualBeliefState.listPGhost.get(k).add( newPos.clone());
									if(!hAlternativePos.contains(newPos.toString())) {
										tempListAlternativeBeliefState.add(actualBeliefState);
										hAlternativePos.add(newPos.toString());
									}
								}
							}
							else {*/
								Position newPos = posG.clone();
								if(newPos.x > this.pacmanPos.x) {//le ghost effectue son mouvement dans la direction de Pacman
									newPos.x--;
									newPos.dir = 'U';
								}
								else {
									if(newPos.x < this.pacmanPos.x) {
										newPos.x++;
										newPos.dir = 'D';
									}
									else {
										if(newPos.y < this.pacmanPos.y) {
											newPos.y++;
											newPos.dir = 'R';
										}
										else {
											newPos.y--;
											newPos.dir = 'L';
										}
									}
								}
								if(newPos.x == state.pacmanPos.x && newPos.y == state.pacmanPos.y) {//si apres deplacement le ghost se trouve sur la meme case que Pacman
									if(stateRemoved == null)
										stateRemoved = new BeliefState(state, true);//cree un etat ou Pacman est mort
								}
								else{
									newPosGhost.add(newPos);
								}
							//}
							haveMoved = true;//le ghost s'est deplace
						}
						else {//si le ghost n'est pas visible ou qu'il a peur
							ArrayList<Position> caseAround =  new ArrayList<Position>();//on regarde quelles sont les mouvement possibles pour le ghost
							boolean rightAvailable = false, leftAvailable = false, upAvailable = false, downAvailable = false;
							if(posG.x > 0 && state.map[posG.x - 1][posG.y] != '#') {
								caseAround.add(new Position(posG.x - 1, posG.y, 'U'));
								upAvailable = true;
							}
							if(posG.x + 1 < state.map.length && state.map[posG.x + 1][posG.y] != '#') {
								caseAround.add(new Position(posG.x + 1, posG.y, 'D'));
								downAvailable = true;
							}
							if(posG.y > 0 && state.map[posG.x][posG.y - 1] != '#') {
								caseAround.add(new Position(posG.x, posG.y - 1, 'L'));
								leftAvailable = true;
							}
							if(posG.y + 1 < state.map[0].length && state.map[posG.x][posG.y + 1] != '#') {
								caseAround.add(new Position(posG.x, posG.y + 1, 'R'));
								rightAvailable = true;
							}

							switch (posG.dir) {
							case 'U' :
								if (leftAvailable || rightAvailable) {
									if(downAvailable) {
										caseAround.remove(upAvailable?1:0);
									}
									for(Position newPos: caseAround) {
										if((newPos.x == state.pacmanPos.x && newPos.y == state.pacmanPos.y) || (posG.x == state.pacmanPos.x && posG.y == state.pacmanPos.y && newPos.x == this.pacmanPos.x && newPos.y == this.pacmanPos.y)) {//soit le ghost se retrouve sur la case du Pacman, soit le ghost et le Pacman se sont croises
											if(compteurPeur == 0) {//si le ghost n'etait pas dans un etat de peur alors Pacman est mort
												if(stateRemoved == null)
													stateRemoved = new BeliefState(state, true);
											}
											else {//si le ghost etait dans un etat de peur alors il a ete mange
												newPos = new Position(BeliefState.listPGhostInit.get(k)[1] / BeliefState.tailleCase, BeliefState.listPGhostInit.get(k)[0] / BeliefState.tailleCase,'U');
												BeliefState actualBeliefState = new BeliefState(state, false);
												actualBeliefState.listPGhost.get(k).clear();
												actualBeliefState.compteurPeur.set(k, 0);
												actualBeliefState.listPGhost.get(k).add(newPos);
												actualBeliefState.score += Ghost.SCORE_FANTOME;
												if(!hAlternativePos.contains(newPos.toString())) {
													tempListAlternativeBeliefState.add(actualBeliefState);
													hAlternativePos.add(newPos.toString());
												}
											}
										}
										else {
											if(BeliefState.isVisible(newPos.x, newPos.y, state.pacmanPos.x, state.pacmanPos.y)) {
												BeliefState actualBeliefState = new BeliefState(state, false);
												actualBeliefState.listPGhost.get(k).clear();
												actualBeliefState.listPGhost.get(k).add(newPos);
												if(!hAlternativePos.contains(newPos.toString())) {
													tempListAlternativeBeliefState.add(actualBeliefState);
													hAlternativePos.add(newPos.toString());
												}
											}
											else {
												newPosGhost.add(newPos);
											}
										}
									}
									haveMoved = true;
								} else if (!upAvailable) {
									for(Position newPos: caseAround) {
										if((newPos.x == state.pacmanPos.x && newPos.y == state.pacmanPos.y) || (posG.x == state.pacmanPos.x && posG.y == state.pacmanPos.y && newPos.x == this.pacmanPos.x && newPos.y == this.pacmanPos.y)) {//soit le ghost se retrouve sur la case du Pacman, soit le ghost et le Pacman se sont croises
											if(compteurPeur == 0) {//si le ghost n'etait pas dans un etat de peur alors Pacman est mort
												if(stateRemoved == null)
													stateRemoved = new BeliefState(state, true);
											}
											else {//si le ghost etait dans un etat de peur alors il a ete mange
												newPos = new Position(BeliefState.listPGhostInit.get(k)[1] / BeliefState.tailleCase, BeliefState.listPGhostInit.get(k)[0] / BeliefState.tailleCase,'U');
												BeliefState actualBeliefState = new BeliefState(state, false);
												actualBeliefState.listPGhost.get(k).clear();
												actualBeliefState.compteurPeur.set(k, 0);
												actualBeliefState.listPGhost.get(k).add(newPos);
												actualBeliefState.score += Ghost.SCORE_FANTOME;
												if(!hAlternativePos.contains(newPos.toString())) {
													tempListAlternativeBeliefState.add(actualBeliefState);
													hAlternativePos.add(newPos.toString());
												}
											}
										}
										else {
											if(BeliefState.isVisible(newPos.x, newPos.y, state.pacmanPos.x, state.pacmanPos.y)) {
												BeliefState actualBeliefState = new BeliefState(state, false);
												actualBeliefState.listPGhost.get(k).clear();
												actualBeliefState.listPGhost.get(k).add(newPos);
												if(!hAlternativePos.contains(newPos.toString())) {
													tempListAlternativeBeliefState.add(actualBeliefState);
													hAlternativePos.add(newPos.toString());
												}
											}
											else {
												newPosGhost.add(newPos);
											}
										}
									}
									haveMoved = true;
								}
								break;
							case 'D' :
								if (leftAvailable || rightAvailable) {
									if(upAvailable) {
										caseAround.remove(0);
									}
									for(Position newPos: caseAround) {
										if((newPos.x == state.pacmanPos.x && newPos.y == state.pacmanPos.y) || (posG.x == state.pacmanPos.x && posG.y == state.pacmanPos.y && newPos.x == this.pacmanPos.x && newPos.y == this.pacmanPos.y)) {//soit le ghost se retrouve sur la case du Pacman, soit le ghost et le Pacman se sont croises
											if(compteurPeur == 0) {//si le ghost n'etait pas dans un etat de peur alors Pacman est mort
												if(stateRemoved == null)
													stateRemoved = new BeliefState(state, true);
											}
											else {//si le ghost etait dans un etat de peur alors il a ete mange
												newPos = new Position(BeliefState.listPGhostInit.get(k)[1] / BeliefState.tailleCase, BeliefState.listPGhostInit.get(k)[0] / BeliefState.tailleCase,'U');
												BeliefState actualBeliefState = new BeliefState(state, false);
												actualBeliefState.listPGhost.get(k).clear();
												actualBeliefState.compteurPeur.set(k, 0);
												actualBeliefState.listPGhost.get(k).add(newPos);
												actualBeliefState.score += Ghost.SCORE_FANTOME;
												if(!hAlternativePos.contains(newPos.toString())) {
													tempListAlternativeBeliefState.add(actualBeliefState);
													hAlternativePos.add(newPos.toString());
												}
											}
										}
										else {
											if(BeliefState.isVisible(newPos.x, newPos.y, state.pacmanPos.x, state.pacmanPos.y)) {
												BeliefState actualBeliefState = new BeliefState(state, false);
												actualBeliefState.listPGhost.get(k).clear();
												actualBeliefState.listPGhost.get(k).add(newPos);
												if(!hAlternativePos.contains(newPos.toString())) {
													tempListAlternativeBeliefState.add(actualBeliefState);
													hAlternativePos.add(newPos.toString());
												}
											}
											else {
												newPosGhost.add(newPos);
											}
										}
									}
									haveMoved = true;
								} else if (!downAvailable) {
									for(Position newPos: caseAround) {
										if((newPos.x == state.pacmanPos.x && newPos.y == state.pacmanPos.y) || (posG.x == state.pacmanPos.x && posG.y == state.pacmanPos.y && newPos.x == this.pacmanPos.x && newPos.y == this.pacmanPos.y)) {//soit le ghost se retrouve sur la case du Pacman, soit le ghost et le Pacman se sont croises
											if(compteurPeur == 0) {//si le ghost n'etait pas dans un etat de peur alors Pacman est mort
												if(stateRemoved == null)
													stateRemoved = new BeliefState(state, true);
											}
											else {//si le ghost etait dans un etat de peur alors il a ete mange
												newPos = new Position(BeliefState.listPGhostInit.get(k)[1] / BeliefState.tailleCase, BeliefState.listPGhostInit.get(k)[0] / BeliefState.tailleCase,'U');
												BeliefState actualBeliefState = new BeliefState(state, false);
												actualBeliefState.listPGhost.get(k).clear();
												actualBeliefState.compteurPeur.set(k, 0);
												actualBeliefState.listPGhost.get(k).add(newPos);
												actualBeliefState.score += Ghost.SCORE_FANTOME;
												if(!hAlternativePos.contains(newPos.toString())) {
													tempListAlternativeBeliefState.add(actualBeliefState);
													hAlternativePos.add(newPos.toString());
												}
											}
										}
										else {
											if(BeliefState.isVisible(newPos.x, newPos.y, state.pacmanPos.x, state.pacmanPos.y)) {
												BeliefState actualBeliefState = new BeliefState(state, false);
												actualBeliefState.listPGhost.get(k).clear();
												actualBeliefState.listPGhost.get(k).add(newPos);
												if(!hAlternativePos.contains(newPos.toString())) {
													tempListAlternativeBeliefState.add(actualBeliefState);
													hAlternativePos.add(newPos.toString());
												}
											}
											else {
												newPosGhost.add(newPos);
											}
										}
									}
									haveMoved = true;
								}
								break;
							case 'L' :
								if (upAvailable || downAvailable) {
									if(rightAvailable) {
										caseAround.remove((upAvailable?1:0)+(downAvailable?1:0)+(leftAvailable?1:0));

									}
									for(Position newPos: caseAround) {
										if((newPos.x == state.pacmanPos.x && newPos.y == state.pacmanPos.y) || (posG.x == state.pacmanPos.x && posG.y == state.pacmanPos.y && newPos.x == this.pacmanPos.x && newPos.y == this.pacmanPos.y)) {//soit le ghost se retrouve sur la case du Pacman, soit le ghost et le Pacman se sont croises
											if(compteurPeur == 0) {//si le ghost n'etait pas dans un etat de peur alors Pacman est mort
												if(stateRemoved == null)
													stateRemoved = new BeliefState(state, true);
											}
											else {//si le ghost etait dans un etat de peur alors il a ete mange
												newPos = new Position(BeliefState.listPGhostInit.get(k)[1] / BeliefState.tailleCase, BeliefState.listPGhostInit.get(k)[0] / BeliefState.tailleCase,'U');
												BeliefState actualBeliefState = new BeliefState(state, false);
												actualBeliefState.listPGhost.get(k).clear();
												actualBeliefState.compteurPeur.set(k, 0);
												actualBeliefState.listPGhost.get(k).add(newPos);
												actualBeliefState.score += Ghost.SCORE_FANTOME;
												if(!hAlternativePos.contains(newPos.toString())) {
													tempListAlternativeBeliefState.add(actualBeliefState);
													hAlternativePos.add(newPos.toString());
												}
											}
										}
										else {
											if(BeliefState.isVisible(newPos.x, newPos.y, state.pacmanPos.x, state.pacmanPos.y)) {
												BeliefState actualBeliefState = new BeliefState(state, false);
												actualBeliefState.listPGhost.get(k).clear();
												actualBeliefState.listPGhost.get(k).add(newPos);
												if(!hAlternativePos.contains(newPos.toString())) {
													tempListAlternativeBeliefState.add(actualBeliefState);
													hAlternativePos.add(newPos.toString());
												}
											}
											else {
												newPosGhost.add(newPos);
											}
										}
									}
									haveMoved = true;
								} else if (!leftAvailable) {
									for(Position newPos: caseAround) {
										if((newPos.x == state.pacmanPos.x && newPos.y == state.pacmanPos.y) || (posG.x == state.pacmanPos.x && posG.y == state.pacmanPos.y && newPos.x == this.pacmanPos.x && newPos.y == this.pacmanPos.y)) {//soit le ghost se retrouve sur la case du Pacman, soit le ghost et le Pacman se sont croises
											if(compteurPeur == 0) {//si le ghost n'etait pas dans un etat de peur alors Pacman est mort
												if(stateRemoved == null)
													stateRemoved = new BeliefState(state, true);
											}
											else {//si le ghost etait dans un etat de peur alors il a ete mange
												newPos = new Position(BeliefState.listPGhostInit.get(k)[1] / BeliefState.tailleCase, BeliefState.listPGhostInit.get(k)[0] / BeliefState.tailleCase,'U');
												BeliefState actualBeliefState = new BeliefState(state, false);
												actualBeliefState.listPGhost.get(k).clear();
												actualBeliefState.compteurPeur.set(k, 0);
												actualBeliefState.listPGhost.get(k).add(newPos);
												actualBeliefState.score += Ghost.SCORE_FANTOME;
												if(!hAlternativePos.contains(newPos.toString())) {
													tempListAlternativeBeliefState.add(actualBeliefState);
													hAlternativePos.add(newPos.toString());
												}
											}
										}
										else {
											if(BeliefState.isVisible(newPos.x, newPos.y, state.pacmanPos.x, state.pacmanPos.y)) {
												BeliefState actualBeliefState = new BeliefState(state, false);
												actualBeliefState.listPGhost.get(k).clear();
												actualBeliefState.listPGhost.get(k).add(newPos);
												if(!hAlternativePos.contains(newPos.toString())) {
													tempListAlternativeBeliefState.add(actualBeliefState);
													hAlternativePos.add(newPos.toString());
												}
											}
											else {
												newPosGhost.add(newPos);
											}
										}
									}
									haveMoved = true;
								}
								break;
							case 'R' :
								if (upAvailable || downAvailable) {
									if(leftAvailable) {
										caseAround.remove((upAvailable?1:0)+(downAvailable?1:0));
									}
									for(Position newPos: caseAround) {
										if((newPos.x == state.pacmanPos.x && newPos.y == state.pacmanPos.y) || (posG.x == state.pacmanPos.x && posG.y == state.pacmanPos.y && newPos.x == this.pacmanPos.x && newPos.y == this.pacmanPos.y)) {//soit le ghost se retrouve sur la case du Pacman, soit le ghost et le Pacman se sont croises
											if(compteurPeur == 0) {//si le ghost n'etait pas dans un etat de peur alors Pacman est mort
												if(stateRemoved == null)
													stateRemoved = new BeliefState(state, true);
											}
											else {//si le ghost etait dans un etat de peur alors il a ete mange
												newPos = new Position(BeliefState.listPGhostInit.get(k)[1] / BeliefState.tailleCase, BeliefState.listPGhostInit.get(k)[0] / BeliefState.tailleCase,'U');
												BeliefState actualBeliefState = new BeliefState(state, false);
												actualBeliefState.listPGhost.get(k).clear();
												actualBeliefState.compteurPeur.set(k, 0);
												actualBeliefState.listPGhost.get(k).add(newPos);
												actualBeliefState.score += Ghost.SCORE_FANTOME;
												if(!hAlternativePos.contains(newPos.toString())) {
													tempListAlternativeBeliefState.add(actualBeliefState);
													hAlternativePos.add(newPos.toString());
												}
											}
										}
										else {
											if(BeliefState.isVisible(newPos.x, newPos.y, state.pacmanPos.x, state.pacmanPos.y)) {
												BeliefState actualBeliefState = new BeliefState(state, false);
												actualBeliefState.listPGhost.get(k).clear();
												actualBeliefState.listPGhost.get(k).add(newPos);
												if(!hAlternativePos.contains(newPos.toString())) {
													tempListAlternativeBeliefState.add(actualBeliefState);
													hAlternativePos.add(newPos.toString());
												}
											}
											else {
												newPosGhost.add(newPos);
											}
										}
									}
									haveMoved = true;
								} else if (!rightAvailable) {
									for(Position newPos: caseAround) {
										if((newPos.x == state.pacmanPos.x && newPos.y == state.pacmanPos.y) || (posG.x == state.pacmanPos.x && posG.y == state.pacmanPos.y && newPos.x == this.pacmanPos.x && newPos.y == this.pacmanPos.y)) {//soit le ghost se retrouve sur la case du Pacman, soit le ghost et le Pacman se sont croises
											if(compteurPeur == 0) {//si le ghost n'etait pas dans un etat de peur alors Pacman est mort
												if(stateRemoved == null)
													stateRemoved = new BeliefState(state, true);
											}
											else {//si le ghost etait dans un etat de peur alors il a ete mange
												newPos = new Position(BeliefState.listPGhostInit.get(k)[1] / BeliefState.tailleCase, BeliefState.listPGhostInit.get(k)[0] / BeliefState.tailleCase,'U');
												BeliefState actualBeliefState = new BeliefState(state, false);
												actualBeliefState.listPGhost.get(k).clear();
												actualBeliefState.compteurPeur.set(k, 0);
												actualBeliefState.listPGhost.get(k).add(newPos);
												actualBeliefState.score += Ghost.SCORE_FANTOME;
												if(!hAlternativePos.contains(newPos.toString())) {
													tempListAlternativeBeliefState.add(actualBeliefState);
													hAlternativePos.add(newPos.toString());
												}
											}
										}
										else {
											if(BeliefState.isVisible(newPos.x, newPos.y, state.pacmanPos.x, state.pacmanPos.y)) {
												BeliefState actualBeliefState = new BeliefState(state, false);
												actualBeliefState.listPGhost.get(k).clear();
												actualBeliefState.listPGhost.get(k).add(newPos);
												if(!hAlternativePos.contains(newPos.toString())) {
													tempListAlternativeBeliefState.add(actualBeliefState);
													hAlternativePos.add(newPos.toString());
												}
											}
											else {
												newPosGhost.add(newPos);
											}
										}
									}
									haveMoved = true;
								}
								break;
							}	
						}									
						if (!haveMoved) {
							Position newPos = posG.clone();
							switch(posG.dir) {
							case 'U': newPos.x--; break;
							case 'D': newPos.x++; break;
							case 'L': newPos.y--; break;
							case 'R': newPos.y++; break;
							}
							if(compteurPeur > 0) {//si le ghost est en etat de peur
								if((newPos.x == state.pacmanPos.x && newPos.y == state.pacmanPos.y) || (posG.x == state.pacmanPos.x && posG.y == state.pacmanPos.y && newPos.x == this.pacmanPos.x && newPos.y == this.pacmanPos.y)) {//si il se trouve sur la meme case que Pacman ou si ils se sont croises
									newPos = new Position(BeliefState.listPGhostInit.get(k)[1] / BeliefState.tailleCase, BeliefState.listPGhostInit.get(k)[0] / BeliefState.tailleCase,'U');//le ghost a ete mange
									BeliefState actualBeliefState = new BeliefState(state, false);
									actualBeliefState.listPGhost.get(k).clear();
									actualBeliefState.compteurPeur.set(k, 0);
									actualBeliefState.listPGhost.get(k).add(newPos);
									actualBeliefState.score += Ghost.SCORE_FANTOME;
									if(!hAlternativePos.contains(newPos.toString())) {
										tempListAlternativeBeliefState.add(actualBeliefState);
										hAlternativePos.add(newPos.toString());
									}
								}
								else {
									if(BeliefState.isVisible(newPos.x, newPos.y, state.pacmanPos.x, state.pacmanPos.y)) {
										BeliefState actualBeliefState = new BeliefState(state, false);
										actualBeliefState.listPGhost.get(k).clear();
										actualBeliefState.listPGhost.get(k).add(newPos);
										if(!hAlternativePos.contains(newPos.toString())) {
											tempListAlternativeBeliefState.add(actualBeliefState);
											hAlternativePos.add(newPos.toString());
										}
									}
									else {
										newPosGhost.add(newPos);
									}
								}
							}
							else {
								if((newPos.x == state.pacmanPos.x && newPos.y == state.pacmanPos.y) || (posG.x == state.pacmanPos.x && posG.y == state.pacmanPos.y && newPos.x == this.pacmanPos.x && newPos.y == this.pacmanPos.y)) {//soit le ghost se retrouve sur la case du Pacman, soit le ghost et le Pacman se sont croises
									if(stateRemoved == null)
										stateRemoved = new BeliefState(state, true);
								}
								else {
									if(BeliefState.isVisible(newPos.x, newPos.y, state.pacmanPos.x, state.pacmanPos.y)) {
										BeliefState actualBeliefState = new BeliefState(state, false);
										actualBeliefState.listPGhost.get(k).clear();
										actualBeliefState.listPGhost.get(k).add(newPos);
										if(!hAlternativePos.contains(newPos.toString())) {
											tempListAlternativeBeliefState.add(actualBeliefState);
											hAlternativePos.add(newPos.toString());
										}
									}
									else {
										newPosGhost.add(newPos);
									}
								}
							}
						}
					}
					if(newPosGhost.isEmpty()) {
						listAlternativeBeliefState.remove(indexBeliefState--);
					}
					else {
						state.listPGhost.set(k, newPosGhost);
					}
				}
				listAlternativeBeliefState.addAll(tempListAlternativeBeliefState);
			}
			if(stateRemoved != null) {
				listAlternativeBeliefState.add(stateRemoved);
			}
		}
		return new Result(listAlternativeBeliefState);
	}

	/**
	 * create all possible states resulting from all possible actions of Pacman
	 * @return a plan, which is a list of belief states, on per set of actions resulting to the same belief states
	 */
	public Plans extendsBeliefState() {
		Plans plans = new Plans();
		if(this.life <= 0)
			return plans;
		ArrayList<String> listNull = new ArrayList<String>();
		if(pacmanPos.x > 0) {
			char nextPos = this.map[this.pacmanPos.x - 1][this.pacmanPos.y];
			if(nextPos != '#') {
				ArrayList<String> listUp = new ArrayList<String>();
				listUp.add(PacManLauncher.UP);
				plans.addPlan(this.extendsBeliefState(PacManLauncher.UP), listUp);
			}
			else {
				listNull.add(PacManLauncher.UP);
			}
		}
		if(this.pacmanPos.x + 1 < this.map.length) {
			char nextPos = this.map[this.pacmanPos.x + 1][this.pacmanPos.y];
			if(nextPos != '#') {
				ArrayList<String> listDown = new ArrayList<String>();
				listDown.add(PacManLauncher.DOWN);
				plans.addPlan(this.extendsBeliefState(PacManLauncher.DOWN), listDown);
			}
			else {
				listNull.add(PacManLauncher.DOWN);
			}
		}
		if(this.pacmanPos.y > 0) {
			char nextPos = this.map[this.pacmanPos.x][this.pacmanPos.y - 1];
			if(nextPos != '#') {
				ArrayList<String> listLeft = new ArrayList<String>();
				listLeft.add(PacManLauncher.LEFT);
				plans.addPlan(this.extendsBeliefState(PacManLauncher.LEFT), listLeft);
			}
			else {
				listNull.add(PacManLauncher.LEFT);
			}
		}
		if(this.pacmanPos.y + 1 < this.map[0].length) {
			char nextPos = this.map[this.pacmanPos.x][this.pacmanPos.y + 1];
			if(nextPos != '#') {
				ArrayList<String> listRight = new ArrayList<String>();
				listRight.add(PacManLauncher.RIGHT);
				plans.addPlan(this.extendsBeliefState(PacManLauncher.RIGHT), listRight);
			}
			else {
				listNull.add(PacManLauncher.RIGHT);
			}
		}
		if(listNull.size() > 0)
			plans.addPlan(this.extendsBeliefState(listNull.get(0)), listNull);
		return plans;
	}

	/**
	 * remove from a list of states all the states where a given ghost is not (possibly) at a given position provided as input
	 * @param listBeliefState list of state to be updated
	 * @param gId Id of the ghost
	 * @param posG actual position of the ghost
	 */
	public static void filter(ArrayList<BeliefState> listBeliefState, int gId, Position posG) {
		ArrayList<BeliefState> copy = (ArrayList<BeliefState>)listBeliefState.clone();
		for(int i = 0; i < listBeliefState.size(); i++) {
			BeliefState state = listBeliefState.get(i);
			if(!state.listPGhost.get(gId).contains(posG)) {
				if(listBeliefState.size() == 1)
					System.out.println("problem");
				else {
					listBeliefState.remove(i);
					i--;
				}
			}
		}
	}

	/**
	 * move the Pacman at a given position
	 * @param i number of rows added to the current position of Pacman
	 * @param j number of columns added to the current position of Pacman
	 * @param nextPos content of the new position of Pacman
	 * @param move direction followed by Pacman ('U', 'D', 'L', 'R')
	 * @return the state resulting from the action of Pacman
	 */
	public BeliefState move(int i, int j, char nextPos, char move) {
		BeliefState nextBeliefState = new BeliefState(this, false);
		if(nextBeliefState.map[nextBeliefState.pacmanPos.x][nextBeliefState.pacmanPos.y] == 'B')
			nextBeliefState.map[nextBeliefState.pacmanPos.x][nextBeliefState.pacmanPos.y] = 'F';
		else
			nextBeliefState.map[nextBeliefState.pacmanPos.x][nextBeliefState.pacmanPos.y] = 'O';
		nextBeliefState.pacmanPos.x += i;
		nextBeliefState.pacmanPos.y += j;
		nextBeliefState.pacmanPos.dir = move;
		if(nextPos == '*' || nextPos == '.') {
			nextBeliefState.nbrOfGommes--;
			nextBeliefState.score += Gomme.SCORE_GOMME;
			if(nextPos == '*') {
				nextBeliefState.nbrOfSuperGommes--;
				for(int k = 0; k < nextBeliefState.compteurPeur.size(); k++) {
					nextBeliefState.compteurPeur.set(k, Ghost.TIME_PEUR);
				}
			}
		}
		if(nextBeliefState.map[nextBeliefState.pacmanPos.x][nextBeliefState.pacmanPos.y] == 'F')
			nextBeliefState.map[nextBeliefState.pacmanPos.x][nextBeliefState.pacmanPos.y] = 'B';
		else
			nextBeliefState.map[nextBeliefState.pacmanPos.x][nextBeliefState.pacmanPos.y] = 'P';
		nextBeliefState.pacmanOldPos = this.pacmanPos.clone();
		return nextBeliefState;
	}

	/**
	 * move the Pacman at a given position
	 * @param i number of rows added to the current position of Pacman
	 * @param j number of columns added to the current position of Pacman
	 * @param move direction followed by Pacman ('U', 'D', 'L', 'R')
	 * @return true if Pacman is dead after performing the move
	 */
	public boolean move(int i, int j, char move) {
		this.pacmanOldPos = this.pacmanPos.clone();
		if(this.map[this.pacmanPos.x + i][this.pacmanPos.y + j] != '#') {
			if(this.map[this.pacmanPos.x][this.pacmanPos.y] == 'B')
				this.map[this.pacmanPos.x][this.pacmanPos.y] = 'F';
			else
				this.map[this.pacmanPos.x][this.pacmanPos.y] = 'O';
			this.pacmanPos.x += i;
			this.pacmanPos.y += j;
			this.pacmanPos.dir = move;
			int l = 0;
			char nextPos = this.map[this.pacmanPos.x][this.pacmanPos.y];
			if(nextPos != 'O' && nextPos != 'F') {
				this.nbrOfGommes--;
				this.score += Gomme.SCORE_GOMME;
				if(nextPos == '*') {
					this.nbrOfSuperGommes--;
					for(int k = 0; k < this.compteurPeur.size(); k++) {
						this.compteurPeur.set(k, Ghost.TIME_PEUR);
					}
				}
			}
			if(nextPos == 'F')
				this.map[this.pacmanPos.x][this.pacmanPos.y] = 'B';
			else
				this.map[this.pacmanPos.x][this.pacmanPos.y] = 'P';
			for(TreeSet<Position> treeSet: this.listPGhost) {
				if(this.compteurPeur.get(l++) == 0 && treeSet.size() == 1) {
					Position pos = treeSet.first();
					if(pos.x == this.pacmanPos.x && pos.y == this.pacmanPos.y) {
						return true;
					}
				}
			}
		}
		else {
			this.pacmanPos.dir = move;
		}
		return false;
	}

	/**
	 * move the Pacman at a given position
	 * @param i new row position
	 * @param j new culumn position
	 * @param move direction of the pacman
	 */
	public void moveTo(int i, int j, char move) {
		if(this.map[this.pacmanPos.x][this.pacmanPos.y] == 'B')
			this.map[this.pacmanPos.x][this.pacmanPos.y] = 'F';
		else
			this.map[this.pacmanPos.x][this.pacmanPos.y] = 'O';
		this.pacmanPos.x = i;
		this.pacmanPos.y = j;
		this.pacmanPos.dir = move;
		if(this.map[this.pacmanPos.x][this.pacmanPos.y] == 'F')
			this.map[this.pacmanPos.x][this.pacmanPos.y] = 'B';
		else
			this.map[this.pacmanPos.x][this.pacmanPos.y] = 'P';
		this.pacmanOldPos = this.pacmanPos.clone();
	}

	/**
	 * move one of the ghost to a given position
	 * @param i number of rows added to the current position of the ghost
	 * @param j number of columns added to the current position of the ghost
	 * @param k Id of the ghost
	 * @param dir direction followed by the ghost ('U', 'D', 'L', 'R')
	 * @return true if the move performed by the ghost kill Pacman
	 */
	public int moveGhost(int i, int j, int k, char dir) {
		Position posGhost = this.listPGhost.get(k).first();

		int compteurPeur = this.compteurPeur.get(k);
		Position posPcopy = this.pacmanPos.clone();
		switch(posPcopy.dir) {
		case 'U': posPcopy.x++; break;
		case 'D': posPcopy.x--; break;
		case 'L': posPcopy.y++; break;
		case 'R': posPcopy.y--; break;
		}
		if(compteurPeur > 0) {//si le ghost est en etat de peur
			if((posGhost.x + i == this.pacmanPos.x && posGhost.y + j == this.pacmanPos.y) || (posGhost.x == this.pacmanPos.x && posGhost.y == this.pacmanPos.y && posPcopy.x == posGhost.x + i && posPcopy.y == posGhost.y + j)) {//si le ghost et le Pacman se sont croise ou que le ghost va sur la case du Pacman
				int[] initPosG = BeliefState.listPGhostInit.get(k);//le ghost est mange
				this.moveGhostTo(initPosG[1] /  BeliefState.tailleCase, initPosG[0] / BeliefState.tailleCase, k, 'U');
				this.score += Ghost.SCORE_FANTOME;
				return -1;
			}			
			this.compteurPeur.set(k, compteurPeur - 2);
			this.listPGhost.get(k).clear();
			posGhost.x += i;
			posGhost.y += j;
			posGhost.dir = dir;
			this.listPGhost.get(k).add(posGhost);
			return 0;
		}
		else {//si le ghost n'est pas en etat de peur
			if((posGhost.x + i == this.pacmanPos.x && posGhost.y + j == this.pacmanPos.y) || (posGhost.x == this.pacmanPos.x && posGhost.y == this.pacmanPos.y && posPcopy.x == posGhost.x + i && posPcopy.y == posGhost.y + j)) {//si le ghost et le Pacman se sont croise ou que le ghost va sur la case du Pacman
				this.life--;//alors Pacman meurt
				this.moveTo(BeliefState.pacmanYInit / BeliefState.tailleCase, BeliefState.pacmanXInit / BeliefState.tailleCase, 'U');
				for(int l = 0; l < BeliefState.listPGhostInit.size(); l++) {
					int[] initPosG = BeliefState.listPGhostInit.get(l);
					this.moveGhostTo(initPosG[1] / BeliefState.tailleCase, initPosG[0] / BeliefState.tailleCase, l, 'U');
				}
				return 1;
			}
			this.listPGhost.get(k).clear();
			posGhost.x += i;
			posGhost.y += j;
			posGhost.dir = dir;
			this.listPGhost.get(k).add(posGhost);
			return 0;
		}
	}

	/**
	 * move one of the ghost to a given position
	 * @param i new row position of the ghost
	 * @param j new column position of the ghost
	 * @param k Id of the ghost
	 * @param dir direction followed by the ghost ('U', 'D', 'L', 'R')
	 */
	public void moveGhostTo(int i, int j, int k, char dir) {
		Position posGhost = this.listPGhost.get(k).first();
		this.listPGhost.get(k).clear();
		posGhost.x = i;
		posGhost.y = j;
		this.compteurPeur.set(k, 0);
		posGhost.dir = dir;
		this.listPGhost.get(k).add(/*posGhost.toString(),*/ posGhost);
	}

	public String toString() {
		String s = new String();
		for(int i = 0; i < this.map.length; i++) {
			for(int j = 0; j < this.map[0].length; j++) {
				s += this.map[i][j];
			}
			s += '\n';
		}
		s += "Pacman (" + this.pacmanPos.x + ", " + this.pacmanPos.y + ", " + this.pacmanPos.dir + ") "+ this.score +"\n";
		for(int i = 0; i < this.listPGhost.size(); i++) {
			s += "Ghost " + i + " (" + this.listPGhost.get(i).size() + ") [" + this.compteurPeur.get(i) + "]";
			Iterator<Position> itPos = this.listPGhost.get(i).iterator();
			while(itPos.hasNext()) {
				Position posG = itPos.next();
				s += "(" + posG.x + ", " + posG.y + ") " + posG.dir + " ";
			}
			s += "\n";
		}
		return s + "distanceMinToGum= " + this.distanceMinToGum() + "\n";
	}
	
	/*private static HashSet<String> visible;*/
	
	/*public void save(PrintStream out) {
		out.println(BeliefState.taille);
		for(int i = 0; i < this.map.length; i++) {
			for(int j = 0; j < this.map[0].length; j++) {
				out.print(this.map[i][j]);
			}
			out.println("");
		}
		out.println(this.pacmanPos.x);
		out.println(this.pacmanPos.y);
		out.println(BeliefState.pacmanXInit);
		out.println(BeliefState.pacmanYInit);
		out.println(BeliefState.tailleCase);
		out.println(this.pacmanPos.dir);
		out.println(this.score);
		out.println(this.life);
		out.println(this.nbrOfGommes);
		out.println(this.nbrOfSuperGommes);
		out.println(this.listPGhost.size());
		for(int i = 0; i < this.listPGhost.size(); i++) {
			out.println(this.compteurPeur.get(i));
			out.println(this.listPGhost.get(i).size());
			Iterator<Position> itPos = this.listPGhost.get(i).iterator();
			while(itPos.hasNext()) {
				Position posG = itPos.next();
				out.println(posG.x + " " + posG.y + " " + posG.dir);
			}
			int[] pos = this.listPGhostInit.get(i);
			out.println(pos[0] + " " + pos[1]);
		}
		out.println(BeliefState.gamePositions.size());
		for(int[] pos:BeliefState.gamePositions) {
			out.println(pos[0] + " " + pos[1]);
		}
		for(String visiblePos: BeliefState.visible) {
			out.println(visiblePos);
		}
		
	}*/

	/**
	 * return the position of one of the ghost
	 * @param i Id of the ghost
	 * @return the position of the ghost
	 */
	public Position getPGhost(int i) {
		return this.listPGhost.get(i).first();
	}

	/**
	 * return Pacman position
	 * @return the position of Pacman
	 */
	public Position getPacmanPos() {
		return this.pacmanPos;
	}
	
	/**
	 * return the number of remaining lifes
	 * @return the number of remaining lifes
	 */
	public int getLife() {
		return this.life;
	}
	
	/**
	 * return the number of remaining gums in the map
	 * @return the number of remaining gums in the map
	 */
	public int getNbrOfGommes() {
		return this.nbrOfGommes;
	}
	
	/**
	 * return the number of remaining super gums in the map
	 * @return the number of remaining super gums in the map
	 */
	public int getNbrOfSuperGommes() {
		return this.nbrOfSuperGommes;
	}
	
	/**
	 * return the number of ghosts
	 * @return number of ghosts
	 */
	public int getNbrOfGhost() {
		return this.compteurPeur.size();
	}
	
	public int getCompteurPeur(int i) {
		return this.compteurPeur.get(i);
	}
	
	public char getMap(int i, int j) {
		return this.map[i][j];
	}
	
	public char[][] getMap(){
		return this.map;
	}
	
	public Position getPacmanPosition() {
		return this.pacmanPos;
	}
	
	public Position getPacmanOldPosition() {
		return this.pacmanOldPos;
	}
	
	public TreeSet<Position> getGhostPositions(int i){
		return this.listPGhost.get(i);
	}
	public static boolean isVisible(int row1, int column1, int row2, int column2) {
		//System.out.println("isVisible " + row1 + "," + column1 + ";" + row2 + "," + column2);
		if(row1 == row2) {
			//System.out.println("row " + row1 + " columns " + column1 + "," + column2);
			return BeliefState.visible.contains(row1+","+Math.min(column1,column2)+";"+row2+","+Math.max(column1, column2));
		}
		if(column1 == column2) {
			//System.out.println("column " + column1 + " rows " + row1 + "," + row2);
			return BeliefState.visible.contains(Math.min(row1, row2)+","+column1+";"+Math.max(row1, row2)+","+column2);
		}
		return false;
	}
	
	public int distanceMinToGum() {
		LinkedList<int[]> queue = new LinkedList<int[]>();
		HashSet<String> visited = new HashSet<String>();
		int[] posP = new int[3];
		posP[0] = this.pacmanPos.x;
		posP[1] = this.pacmanPos.y;
		posP[2] = 0;
		queue.add(posP);
		while(!queue.isEmpty()) {
			int[] next = queue.pollFirst();
			visited.add(next[0] + "," + next[1]);
			if(next[0] > 0) {
				if(!visited.contains((next[0] - 1) + "," + next[1])) {
					char content = this.map[next[0] - 1][next[1]];
					switch(content) {
					case '.':
					case '*': return next[2] + 1;
					case '#': break;
					default: int[] neighbor = {next[0] - 1, next[1], next[2] + 1}; queue.add(neighbor);
					}
				}
			}
			if(next[0] < BeliefState.taille - 1) {
				if(!visited.contains((next[0] + 1) + "," + next[1])) {
					char content = this.map[next[0] + 1][next[1]];
					switch(content) {
					case '.':
					case '*': return next[2] + 1;
					case '#': break;
					default: int[] neighbor = {next[0] + 1, next[1], next[2] + 1}; queue.add(neighbor);
					}
				}
			}
			if(next[1] > 0) {
				if(!visited.contains(next[0] + "," + (next[1] - 1))) {
					char content = this.map[next[0]][next[1] - 1];
					switch(content) {
					case '.':
					case '*': return next[2] + 1;
					case '#': break;
					default: int[] neighbor = {next[0], next[1] - 1, next[2] + 1}; queue.add(neighbor);
					}
				}
			}
			if(next[1] < BeliefState.taille - 1) {
				if(!visited.contains(next[0] + "," + (next[1] + 1))) {
					char content = this.map[next[0]][next[1] + 1];
					switch(content) {
					case '.':
					case '*': return next[2] + 1;
					case '#': break;
					default: int[] neighbor = {next[0], next[1] + 1, next[2] + 1}; queue.add(neighbor);
					}
				}
			}
		}
		return Integer.MAX_VALUE;
	}
}
