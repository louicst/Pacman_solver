package logic;
//import java.awt.*;
import java.util.ArrayList;

import data.*;
import view.*;


/**
 * Cette classe représente l'entité fantome et toute ses caractéristique
 *
 * @author RGM
 * @version 03/03/2017
 */
public class Ghost extends Entite {

	/** Tableau des figure composants le fantome */
	private GhostSkin figures;
	/** La couleur du fantome */
	private String couleur;
	/** Le mouvement que vient d'effectuer le fantome */
	private String previousMove;
	/** Compteur qui va aléatoirement faire faire demi-tour au fantome */
	//private int compteurInversionMove;
	/** Compteur du temps de peur des fantomes */
	private int compteurPeur;
	private int id;
	private ArrayList<BeliefState> visibleBeliefStateCopy;

	public static final int SPEED_GHOST = 10;//doit etre un multiple de taille de case
	public static final int SCORE_FANTOME = 100;
	public static final int TIME_PEUR = 60;


	/**
	 * Create a new ghost.
	 *
	 * @pre size >= 0
	 * @pre color different of ("white")
	 */
	public Ghost(int size, int x, int y, String color, Map map, int id) {
		this.previousMove = PacManLauncher.UP;
		//this.initCompteur();

		this.compteurPeur = 0;
		this.couleur = color;
		
		this.setMap(map);
		int xG = x / this.map.getTailleCase();
		int yG = y / this.map.getTailleCase();
		int xP = this.map.getPMX() / this.map.getTailleCase();
		int yP = this.map.getPMY() / this.map.getTailleCase();

		this.figures = new GhostSkin(size, x, y, color, this.map.isVisible(yG, xG, yP, xP));
		this.id = id;
	}

	/**
	 *	Choisir une direction aleatoire
	 */
	public int move (boolean with) {
		if (this.compteurPeur == 0) {
			this.setEtatNormal();
		}
		if (this.compteurPeur > 0) {
			this.compteurPeur--;
		}
		return checkCroisement(this.previousMove, with);
	}

	/**
	 * deplace l'entite dans la direction demander
	 *
	 * @param String toward direction demande
	 * @pre (toward.equals("UP") || toward.equals("DOWN") || toward.equals("LEFT") || toward.equals("RIGHT"))
	 */
	public boolean move (String toward) {
		this.previousMove = toward;
		int dx = 0;
		int dy = 0;

		int[] crossMap = this.crossMap(toward);
		dx = crossMap[0];
		dy = crossMap[1];

		crossMap = this.checkColision(toward, dx, dy);
		dx = crossMap[0];
		dy = crossMap[1];

		this.move(dx, dy);//move the ghost
		return false;
	}

	/**
	 * deplace l'entite d'une variation de (dx,dy)
	 * @param int dx le decalage x
	 * @param int dy le decalage y
	 */
	public void move (int dx, int dy) {
		for (Figure figure : this.getSkin()) {
			figure.move(dx, dy);
		}
	}

	private Figure[] getSkin () {
		return this.figures.getFigures();
	}

	/**
	 * Met le fantome en etat de peur
	 */
	public void setEtatPeur() {
		this.compteurPeur = Ghost.TIME_PEUR;
		Figure[] figures = this.getSkin();
		for (int i = 0; i < figures.length; i++) {
			figures[i].setColor("blue");
		}
	}

	public void setEtatNormal() {
		this.compteurPeur = 0;
		Figure[] figures = this.getSkin();
		for (int i = 0; i < figures.length; i++) {
			figures[i].setColor(this.couleur);
		}
	}

	/**
	 * retourne la position x de l'entite
	 * @return la position x de l'entite
	 */
	public int getX () {
		return this.figures.getX();
	}

	/**
	 * retourne la position y de l'entite
	 * @return la position y de l'entite
	 */
	public int getY () {
		return this.figures.getY();
	}

	/**
	 * retourne la taille de l'Entite fantome
	 * @return la taille de Ghost
	 */
	public int getWidth () {
		return this.figures.getWidth();
	}

	/**
	 * retourne la vitesse de deplacement d'un fantome
	 * @return la vitesse de Ghost
	 */
	public int getSpeed () {
		return Ghost.SPEED_GHOST;
	}

	/**
	 * retourne la vitesse de deplacement d'un fantome
	 * @return la vitesse de Ghost
	 */
	public int getPeur () {
		return this.compteurPeur;
	}

	/**
	 * [checkCroisement description]
	 * @param String toward [description]
	 */
	public int checkCroisement (String toward, boolean moveBeliefState) {
		boolean haveMoved = false;
		int reInit = 0;
		Figure[][] map = this.map.getMap();

		if(this.getX() % this.map.getTailleCase() == 0 && this.getY() % this.map.getTailleCase() == 0) {//si le ghost se trouve sur une case
			int yG = this.getX() / this.map.getTailleCase();//position actuelle du ghost
			int xG = this.getY() / this.map.getTailleCase();
			Position pacmanOldPos = this.map.getBeliefState().getPacmanOldPosition();//position avant deplacement de Pacman
			int xP = pacmanOldPos.x, yP = pacmanOldPos.y;
			
			if(this.map.isVisible(xG, yG, xP, yP)) {
				this.figures.setVisible(true);
			}
			else
				this.figures.setVisible(false);
			if(this.map.isVisible(xG, yG, xP, yP) && this.compteurPeur == 0) {//si le ghost etait visible et n'avait pas peur
				if(xG > xP) {//il prend la direction du Pacman
					this.move(PacManLauncher.UP);
					if(moveBeliefState)
						reInit = this.map.getBeliefState().moveGhost(-1, 0, this.id, this.previousMove.charAt(0));
				}
				else {
					if(xG < xP) {
						this.move(PacManLauncher.DOWN);
						if(moveBeliefState)
							reInit = this.map.getBeliefState().moveGhost(1, 0, this.id, this.previousMove.charAt(0));
					}
					else {
						if(yG < yP) {

							this.move(PacManLauncher.RIGHT);
							if(moveBeliefState)
								reInit = this.map.getBeliefState().moveGhost(0, 1, this.id, this.previousMove.charAt(0));
						}
						else {
							if(yG > yP) {
								this.move(PacManLauncher.LEFT);
								if(moveBeliefState)
									reInit = this.map.getBeliefState().moveGhost(0, -1, this.id, this.previousMove.charAt(0));
							}
							else {
								System.out.println("problem");
								/*switch(this.map.getBeliefState().getPacmanPos().dir) {
								case 'U': this.move(PacManLauncher.DOWN); break;
								case 'D': this.move(PacManLauncher.UP); break;
								case 'L': this.move(PacManLauncher.RIGHT); break;
								case 'R': this.move(PacManLauncher.LEFT); break;
								}
								if(moveBeliefState)
									reInit = reInit || this.map.getBeliefState().moveGhost(0, 0, this.id, this.previousMove.charAt(0));*/
							}
						}
					}
				}
				haveMoved = true;
			}
			else {//si le ghost n'est pas visible ou qu'il a peur
				int[] colLign = this.getColLign();
				int colonne = colLign[0];
				int ligne = colLign[1];

				Figure fup = map[ligne-1][colonne];
				Figure fdown = map[ligne+1][colonne];
				Figure fleft = map[ligne][colonne-1];
				Figure fright = map[ligne][colonne+1];

				ArrayList<Figure> caseAround =  new ArrayList<Figure>();
				caseAround.add(fup);
				caseAround.add(fdown);
				caseAround.add(fleft);
				caseAround.add(fright);

				switch (toward) {
				case PacManLauncher.UP :
					if (fleft.getClass().getName().compareTo("view.Wall") != 0 || fright.getClass().getName().compareTo("view.Wall") != 0) {
						caseAround.remove(fdown);
						reInit = this.chooseMove(toward, caseAround, fup, fdown, fleft, fright, moveBeliefState);
						haveMoved = true;
					} else if (fup.getClass().getName().compareTo("view.Wall") == 0) {
						reInit = this.chooseMove(toward, caseAround, fup, fdown, fleft, fright, moveBeliefState);
						haveMoved = true;
					}
					break;
				case PacManLauncher.DOWN :
					if (fleft.getClass().getName().compareTo("view.Wall") != 0 || fright.getClass().getName().compareTo("view.Wall") != 0) {
						caseAround.remove(fup);
						reInit = this.chooseMove(toward, caseAround, fup, fdown, fleft, fright, moveBeliefState);
						haveMoved = true;
					} else if (fdown.getClass().getName().compareTo("view.Wall") == 0) {
						reInit = this.chooseMove(toward, caseAround, fup, fdown, fleft, fright, moveBeliefState);
						haveMoved = true;
					}
					break;
				case PacManLauncher.LEFT :
					if (fup.getClass().getName().compareTo("view.Wall") != 0 || fdown.getClass().getName().compareTo("view.Wall") != 0) {
						caseAround.remove(fright);
						reInit = this.chooseMove(toward, caseAround, fup, fdown, fleft, fright, moveBeliefState);
						haveMoved = true;
					} else if (fleft.getClass().getName().compareTo("view.Wall") == 0) {
						reInit = this.chooseMove(toward, caseAround, fup, fdown, fleft, fright, moveBeliefState);
						haveMoved = true;
					}
					break;
				case PacManLauncher.RIGHT :
					if (fup.getClass().getName().compareTo("view.Wall") != 0 || fdown.getClass().getName().compareTo("view.Wall") != 0) {
						caseAround.remove(fleft);
						reInit = this.chooseMove(toward, caseAround, fup, fdown, fleft, fright, moveBeliefState);
						haveMoved = true;
					} else if (fright.getClass().getName().compareTo("view.Wall") == 0) {
						reInit = this.chooseMove(toward, caseAround, fup, fdown, fleft, fright, moveBeliefState);
						haveMoved = true;
					}
					break;
				}
			}
			if(!haveMoved && moveBeliefState) {//si il n'a pas bouge il garde la meme direction
				switch(toward){
				case PacManLauncher.UP: reInit = this.map.getBeliefState().moveGhost(-1, 0, this.id, this.previousMove.charAt(0)); /*System.out.println("moveGhost(-1, 0, " + this.id + ", " + this.previousMove.charAt(0) + ")");*/ break;
				case PacManLauncher.DOWN: reInit = this.map.getBeliefState().moveGhost(1, 0, this.id, this.previousMove.charAt(0)); /*System.out.println("moveGhost(1, 0, " + this.id + ", " + this.previousMove.charAt(0) + ")");*/ break;
				case PacManLauncher.LEFT: reInit = this.map.getBeliefState().moveGhost(0, -1, this.id, this.previousMove.charAt(0)); /*System.out.println("moveGhost(0, -1, " + this.id + ", " + this.previousMove.charAt(0) + ")");*/ break;
				case PacManLauncher.RIGHT: reInit = this.map.getBeliefState().moveGhost(0, 1, this.id, this.previousMove.charAt(0)); /*System.out.println("moveGhost(0, 1, " + this.id + ", " + this.previousMove.charAt(0) + ")");*/ break;
				}
			}
			/*if(moveBeliefState && this.id == (this.map.getPGhost().size() - 1)) {
				this.visibleBeliefStateCopy = (ArrayList<BeliefState>)this.map.getVisibleBeliefState().clone();
			}*/
		}

		if (!haveMoved) {
			this.move(this.previousMove);
		}
		return reInit;
	}

	public int chooseMove(String toward, ArrayList<Figure> listF, Figure fup, Figure fdown, Figure fleft, Figure fright, boolean moveBeliefState) {
		ArrayList<Figure> toGo =  new ArrayList<Figure>();
		int returnedValue = 0;
		for (Figure f : listF) {
			if (f.getClass().getName().compareTo("view.Wall") != 0) {
				toGo.add(f);
			}
		}

		Figure nextMove = toGo.get((int)Math.round(Math.floor(Math.random()*toGo.size())));

		if (nextMove == null) {
			this.move(toward);
			if(moveBeliefState) {
				switch(toward){
				case PacManLauncher.UP: returnedValue = this.map.getBeliefState().moveGhost(-1, 0, this.id, this.previousMove.charAt(0)); break;
				case PacManLauncher.DOWN: returnedValue = this.map.getBeliefState().moveGhost(1, 0, this.id, this.previousMove.charAt(0)); break;
				case PacManLauncher.LEFT: returnedValue = this.map.getBeliefState().moveGhost(0, -1, this.id, this.previousMove.charAt(0)); break;
				case PacManLauncher.RIGHT: returnedValue = this.map.getBeliefState().moveGhost(0, 1, this.id, this.previousMove.charAt(0)); break;
				}
			}
		} else if (nextMove == fup) {
			this.move(PacManLauncher.UP);
			if(moveBeliefState)
				returnedValue = this.map.getBeliefState().moveGhost(-1, 0, this.id, this.previousMove.charAt(0));
		} else if (nextMove == fdown) {
			this.move(PacManLauncher.DOWN);
			if(moveBeliefState)
				returnedValue = this.map.getBeliefState().moveGhost(1, 0, this.id, this.previousMove.charAt(0));
		} else if (nextMove == fleft) {
			this.move(PacManLauncher.LEFT);
			if(moveBeliefState)
				returnedValue = this.map.getBeliefState().moveGhost(0, -1, this.id, this.previousMove.charAt(0));
		} else if (nextMove == fright) {
			this.move(PacManLauncher.RIGHT);
			if(moveBeliefState)
				returnedValue = this.map.getBeliefState().moveGhost(0, 1, this.id, this.previousMove.charAt(0));
		}
		return returnedValue;
	}

	/**
	 *	les fantomes agissent sur les murs
	 * mais pas les gommes
	 */
	public boolean typeCaseToCheck (Figure f) {
		return (f instanceof Wall);
	}

	protected void actionWithGom (Figure[][] map, int i, int j) {
		//no interaction
	}

	/**
	 * Draw the figure with current specifications on screen.
	 */
	public void draw() {
		this.figures.draw();
	}
	
	public void setPreviousMove(String toward) {
		this.previousMove = toward;
	}

}
