package logic;
import data.*;
import view.*;

/**
 * Une entite qui se deplace dans l'environnement de jeu pacman
 * @author maxime,guillaume,remi
 * @version 2017.02.14
 * @pre
 * @inv position de entite comprise dans la taille de la fenetre
 *      largeur = hauteur
 *      speed <= largeurCase
 * @post
 */
abstract class Entite {

	/**
	 * La carte dans laquelle l'entite evolue
	 */
	protected Map map;

	/**
	 * dessine l'entite sur la carte
	 */
	public abstract void draw();

	/**
	 * retourne la position x de l'entite
	 * @return la position x de l'entite
	 */
	public abstract int getX();

	/**
	 * retourne la position y de l'entite
	 * @return la position y de l'entite
	 */
	public abstract int getY();

	/**
	 * retourne la taille (largeur = longueur)
	 * @return la taille de l'entite
	 */
	public abstract int getWidth();

	/**
	 * donne la carte a l'entite
	 * @param Map map la carte où évoluer
	 */
	public void setMap (Map map) {
		this.map = map;
	}

	/**
	 * deplace l'entite en position (x,y) en fonction de sa position actuelle
	 * @param int x la position où aller
	 * @param int y la position où aller
	 * @pre ((x>0) && (x<Canvas.WIDTH)) && ((y>0) && (y<Canvas.HEIGHT))
	 */
	public void setLocation (int x, int y) {
		int tmpx = x-this.getX();
		int tmpy = y-this.getY();

		this.move(tmpx, tmpy);
	}

	/**
	 * deplace l'entite dans la direction demander
	 *
	 * @param String toward direction demande
	 * @pre (toward.equals("UP") || toward.equals("DOWN") || toward.equals("LEFT") || toward.equals("RIGHT"))
	 */
	public abstract boolean move (String toward);

	/**
	 * deplace l'entite d'une variation de (dx,dy)
	 * @param int dx le decalage x
	 * @param int dy le decalage y
	 */
	public abstract void move (int dx, int dy);

	/**
	 * verifie que la figure est aussi d'un type correspondant
	 * pour une interaction avec l'entite
	 * @param  Figure f la figure avec qui faire potentiellement un interaction
	 * @return vrai si l'entite doit faire des actions avec la figure
	 */
	public abstract boolean typeCaseToCheck (Figure f);

	/**
	 * definie les actions que l'entite va devoir realiser avec un objet de type gomme
	 * qui est en position (i,j) sur la Map
	 * @param Figure[][] map la carte ayant les objets de type gomme
	 * @param int        i   position colonne pour la Map
	 * @param int        j   position ligne dans la Map
	 * *@pre (i>=0 && j>=0)
	 */
	protected abstract void actionWithGom (Figure[][] map, int i, int j);

	/**
	 * verifie les colisions entre l'entite et plusieurs Figure
	 * les Figure sont celles presente sur Map et autour de l'Entite
	 *
	 * @param toward
	 * @param dx
	 * @param dy
	 * @pre (toward.equals("UP") || toward.equals("DOWN") || toward.equals("LEFT") || toward.equals("RIGHT"))
	 * @post (ret.length==2)
	 */
	protected int[] checkColision (String toward, int dx, int dy) {
		int[] ret = new int[2];
		Figure[][] map = this.map.getMap();

		int[] colLign = this.getColLign();
		int colonne = colLign[0];
		int ligne = colLign[1];

		for (int i=colonne-1; i<=colonne+1; i++) {
			for (int j=ligne-1; j<=ligne+1; j++) {
				Figure f = map[j][i];
				if (this.checkOneColision(f, dx, dy)) {
					if (f instanceof Wall) {
						if (toward.equals(PacManLauncher.UP)) {
							//dy<0
							dy = this.getY()-(f.getY()+f.getHeight());
						} else if (toward.equals(PacManLauncher.DOWN)) {
							//dy>0
							dy = (this.getY()+this.getWidth())-f.getY();
						} else if (toward.equals(PacManLauncher.LEFT)) {
							//dx<0
							dx = this.getX()-(f.getX()+f.getWidth());
						} else if (toward.equals(PacManLauncher.RIGHT)) {
							//dy>0
							dx = (this.getX()+this.getWidth())-f.getX();
						}
					} else if (f instanceof Gomme) {
						this.actionWithGom(map, j, i);
					}
				}
			}
		}

		ret[0] = dx;
		ret[1] = dy;
		return ret;
	}

	/**
	 * retourne la position
	 * en fonction de la map
	 * et de la taille des cases
	 * la position fictive de l'Entite sur cette map
	 */
	protected int[] getColLign () {
		Figure[][] map = this.map.getMap();
		int colonne = this.getX()/this.map.getTailleCase();
		int ligne = this.getY()/this.map.getTailleCase();
		if (colonne <= 0) {//gestion bord de map droite/gauche
			colonne = 1;
		} else if (colonne >= map.length-1) {
			colonne = map.length-2;
		}
		if (ligne <= 0) {//gestion bord de map bas/haut
			ligne = 1;
		} else if (ligne >= map.length-1) {
			ligne = map.length-2;
		}

		int[] ret = {colonne,ligne};
		return ret;
	}

	/**
	 * renvoi la vitesse de deplacement de l'entite
	 * @return la vitesse de deplacement de l'entite
	 */
	public abstract int getSpeed();

	/**
	 * verifie si l'entite va sortir de la Map
	 * sur un bord de cette Map
	 * retourne la variation de deplacement dx,dy a faire dans un tableau d'entier de 2 cases
	 * quand entite va sortir sur un bord, l'entite reapparait sur le bord opposé
	 *
	 * @param toward la direction dans laquelle se dirige
	 * @return ret {dx, dy}
	 * @pre (toward.equals("UP") || toward.equals("DOWN") || toward.equals("LEFT") || toward.equals("RIGHT"))
	 * @post (ret.length==2)
	 */
	protected int[] crossMap (String toward) {
		int[] ret = new int[2];
		int dx = 0;
		int dy = 0;
		int x = this.getX();
		int y = this.getY();

		int speed = this.getSpeed();
		int width = this.getWidth()/4;
		int heightMap = Canvas.HEIGHT;
		int widthMap = Canvas.WIDTH;
		if (toward.equals(PacManLauncher.UP)) {
			//pacman is out the map of a part of his body
			if ((y-speed) <= (-width)) {
				//so he spawn at the bottom with a part of his body visible
				dy = heightMap-speed;
			} else {
				dy = -speed;
			}
		} else if (toward.equals(PacManLauncher.DOWN)) {
			if ((y+speed) > (heightMap-width)) {
				dy = -heightMap+speed;
			} else {
				dy = speed;
			}
		} else if (toward.equals(PacManLauncher.LEFT)) {
			if ((x-speed) <= (-width)) {
				dx = widthMap-speed;
			} else {
				dx = -speed;
			}
		} else if (toward.equals(PacManLauncher.RIGHT)) {
			if ((x+speed) > (widthMap-width)) {
				dx = -widthMap+speed;
			} else {
				dx = speed;
			}
		}

		ret[0] = dx;
		ret[1] = dy;
		assert (ret.length==2) : "nombre de coordonnee doit etre egale a 2";
		return ret;
	}


	/**
	 *	verifie si une colision entre l'entite et une Figure (avec laquelle l'entite doit interagir)
	 *	va se réaliser cad si les coordonnees des 4 points de Entite rentre en partie
	 *	dans les coordonnees des 4 points de Figure
	 * @param f la Figure etant l'obstacle
	 * @param dx le deplacement x qui va se faire
	 * @param dy le deplacement y qui va se faire
	 * @pre (f!=null)
	 */
	protected boolean checkOneColision (Figure f, int dx, int dy) {
		boolean ret = false;

		assert (f!=null) : "Figure is null";
		if (f != null) {
			if (this.typeCaseToCheck(f)) {
				int xf = f.getX();//x de f
				int yf = f.getY();//y de f
				int wf = f.getWidth();//largeur f
				int hf = f.getHeight();//hauteur f

				int xt = this.getX()+dx;//x
				int yt = this.getY()+dy;//y
				int st = this.getWidth();//size

				boolean posMinX = (xt < (xf+wf)) || ((xt+st) < (xf+wf));//inferieur bord droit
				boolean posMaxX = (xt > xf) || (xt+st > xf);//superieur bord gauche
				boolean posMinY = (yt < (yf+hf)) || (yt+st < (yf+hf));//inferieur bord bas
				boolean posMaxY = (yt > yf) || (yt+st > yf);//superieur bord haut

				if (posMinX && posMaxX && posMinY && posMaxY) {
					ret = true;
				}
			}
		}

		return ret;
	}


}
