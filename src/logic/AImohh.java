package logic;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.HashSet;
import java.util.TreeSet;

/**
 * Classes Plans et Result pour l'algorithme AND-OR
 */
class Plans {
    ArrayList<Result> results;
    ArrayList<ArrayList<String>> actions;
    
    public Plans() {
        this.results = new ArrayList<Result>();
        this.actions = new ArrayList<ArrayList<String>>();
    }
    
    public void addPlan(Result beliefState, ArrayList<String> action) {
        this.results.add(beliefState);
        this.actions.add(action);
    }
    
    public int size() { return this.results.size(); }
    public Result getResult(int index) { return this.results.get(index); }
    public ArrayList<String> getAction(int index) { return this.actions.get(index); }
}

class Result {
    private ArrayList<BeliefState> beliefStates;

    public Result(ArrayList<BeliefState> states) { this.beliefStates = states; }
    public int size() { return this.beliefStates.size(); }
    public BeliefState getBeliefState(int index) { return this.beliefStates.get(index); }
    public ArrayList<BeliefState> getBeliefStates() { return this.beliefStates; }
}

/**
 * ========================================================================
 * IA PACMAN - Algorithme AND-OR Optimisé
 * ========================================================================
 * 
 * Stratégie: Collecter les gommes tout en évitant les fantômes
 * - Priorité 1: Ne pas mourir
 * - Priorité 2: Manger les gommes
 * - Priorité 3: Éviter les oscillations
 * 
 * ========================================================================
 */
public class AI {
    
    // ===== PARAMÈTRES AND-OR =====
    private static final int MAX_DEPTH = 2;
    private static final int MAX_AND_STATES = 4;
    
    // ===== ANTI-OSCILLATION RENFORCÉ =====
    private static String lastAction = null;
    private static String secondLastAction = null;
    private static int sameDirectionCount = 0;
    private static final int POSITION_HISTORY_SIZE = 15;
    private static LinkedList<String> positionHistory = new LinkedList<>();
    
    // ===== DIRECTIONS =====
    private static final int[][] DIRS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
    private static final String[] ACTIONS = {PacManLauncher.UP, PacManLauncher.DOWN, 
                                              PacManLauncher.LEFT, PacManLauncher.RIGHT};
    
    // ===== INFO FANTÔME EFFRAYÉ =====
    private static class ScaredGhostInfo {
        boolean exists = false;
        int row, col, distance, fearTime;
    }

    /**
     * Point d'entrée principal
     */
    public static String findNextMove(BeliefState state) {
        if (state.getLife() <= 0) return PacManLauncher.UP;
        
        // Enregistrer position actuelle
        Position pacPos = state.getPacmanPosition();
        String posKey = pacPos.getRow() + "," + pacPos.getColumn();
        
        // Choisir la meilleure action
        String bestAction = selectBestAction(state);
        
        // Mettre à jour l'historique
        updateHistory(posKey, bestAction);
        
        return bestAction;
    }
    
    /**
     * Sélection de la meilleure action avec AND-OR
     */
    private static String selectBestAction(BeliefState state) {
        Plans plans = state.extendsBeliefState();
        if (plans.size() == 0) return PacManLauncher.UP;
        
        Position pacPos = state.getPacmanPosition();
        int pacRow = pacPos.getRow();
        int pacCol = pacPos.getColumn();
        char[][] map = state.getMap();
        
        // Trouver le fantôme le plus proche
        int minGhostDist = getMinGhostDistance(state, pacRow, pacCol);
        
        String bestAction = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        
        for (int i = 0; i < plans.size(); i++) {
            String action = plans.getAction(i).get(0);
            Result result = plans.getResult(i);
            
            // Calculer nouvelle position
            int newRow = pacRow + DIRS[getActionIndex(action)][0];
            int newCol = pacCol + DIRS[getActionIndex(action)][1];
            
            if (!isValid(map, newRow, newCol)) continue;
            
            // ===== ÉVALUATION AND-OR =====
            double score = evaluateAndNode(result, MAX_DEPTH);
            
            // ===== BONUS/PÉNALITÉS IMMÉDIATES =====
            
            // 1. GOMMES (PRIORITÉ MAXIMALE!)
            char cell = map[newRow][newCol];
            if (cell == '.') score += 5000;      // Gomme normale - ENCORE AUGMENTÉ
            if (cell == '*') score += 8000;      // Super-gomme - ENCORE AUGMENTÉ
            
            // Bonus pour aller vers zone dense en gommes
            int gumsNear = countGumsNear(map, newRow, newCol, 5);
            score += gumsNear * 500;  // ENCORE AUGMENTÉ
            
            // 2. CHASSE AUX FANTÔMES EFFRAYÉS (PRIORITÉ SI ACTIF!)
            ScaredGhostInfo scaredInfo = findNearestScaredGhost(state, pacRow, pacCol);
            if (scaredInfo.exists && scaredInfo.fearTime > 3) {
                // Il y a un fantôme effrayé à chasser!
                int newScaredDist = manhattan(newRow, newCol, scaredInfo.row, scaredInfo.col);
                
                if (newScaredDist <= 1) {
                    score += 8000;  // On va le manger! GROS BONUS
                } else if (newScaredDist < scaredInfo.distance) {
                    score += 4000 + (scaredInfo.distance - newScaredDist) * 1000;  // Se rapprocher
                } else if (newScaredDist > scaredInfo.distance) {
                    score -= 2000;  // S'éloigner = mauvais
                }
            }
            
            // 3. DANGER DES FANTÔMES (seulement non-effrayés)
            int newGhostDist = getMinGhostDistanceFrom(state, newRow, newCol);
            
            if (newGhostDist <= 1) {
                score -= 100000;  // MORT CERTAINE
            } else if (newGhostDist == 2) {
                score -= 3000;    // Très dangereux (réduit)
            } else if (newGhostDist <= 4) {
                score -= 800;     // Dangereux (réduit)
            }
            
            // Bonus pour s'éloigner du fantôme (si pas de chasse en cours)
            if (minGhostDist <= 5 && (!scaredInfo.exists || scaredInfo.fearTime <= 3)) {
                score += (newGhostDist - minGhostDist) * 600;
            }
            
            // 4. ANTI-OSCILLATION (réduit si chasse en cours)
            if (scaredInfo.exists && scaredInfo.fearTime > 3 && scaredInfo.distance <= 5) {
                // Chasse en cours: réduire les pénalités d'oscillation
                score += getAntiOscillationScore(action, newRow, newCol, minGhostDist) * 0.3;
            } else {
                score += getAntiOscillationScore(action, newRow, newCol, minGhostDist);
            }
            
            // 4. ROUTES DE FUITE
            int routes = countRoutes(map, newRow, newCol);
            if (routes == 1 && minGhostDist <= 4) {
                score -= 3000;  // Impasse dangereuse
            }
            score += routes * 100;
            
            // 5. DISTANCE À LA GOMME LA PLUS PROCHE (TRÈS IMPORTANT)
            int distToGum = findNearestGumDist(map, newRow, newCol);
            score -= distToGum * 350;  // Plus proche = mieux - ENCORE AUGMENTÉ
            
            if (score > bestScore) {
                bestScore = score;
                bestAction = action;
            }
        }
        
        if (bestAction == null) {
            bestAction = findValidAction(state);
        }
        
        return bestAction;
    }
    
    /**
     * Nœud AND: évalue le pire cas (réponses des fantômes)
     */
    private static double evaluateAndNode(Result result, int depth) {
        if (result.size() == 0) return 0;
        
        double minScore = Double.POSITIVE_INFINITY;
        int limit = Math.min(result.size(), MAX_AND_STATES);
        
        for (int i = 0; i < limit; i++) {
            BeliefState s = result.getBeliefState(i);
            double score;
            
            if (depth <= 1 || s.getLife() <= 0) {
                score = evaluate(s);
            } else {
                score = evaluateOrNode(s, depth - 1);
            }
            
            if (score < minScore) minScore = score;
        }
        
        return minScore;
    }
    
    /**
     * Nœud OR: évalue le meilleur choix (actions de Pacman)
     */
    private static double evaluateOrNode(BeliefState state, int depth) {
        if (state.getLife() <= 0) return -100000;
        if (state.getNbrOfGommes() <= 0) return 100000;
        
        Plans plans = state.extendsBeliefState();
        if (plans.size() == 0) return evaluate(state);
        
        double maxScore = Double.NEGATIVE_INFINITY;
        
        for (int i = 0; i < plans.size(); i++) {
            double score = evaluateAndNode(plans.getResult(i), depth - 1);
            if (score > maxScore) maxScore = score;
        }
        
        return maxScore;
    }
    
    /**
     * Fonction d'évaluation heuristique
     */
    private static double evaluate(BeliefState state) {
        if (state.getLife() <= 0) return -100000;
        if (state.getNbrOfGommes() <= 0) return 100000 + state.getScore();
        
        double score = 0;
        
        Position pacPos = state.getPacmanPosition();
        int pacRow = pacPos.getRow();
        int pacCol = pacPos.getColumn();
        char[][] map = state.getMap();
        
        // Score du jeu (TRÈS important!)
        score += state.getScore() * 80;  // ENCORE AUGMENTÉ
        
        // Vies
        score += state.getLife() * 5000;
        
        // Gommes restantes (moins = mieux) - ENCORE AUGMENTÉ
        score -= state.getNbrOfGommes() * 80;
        
        // Distance à la gomme la plus proche - ENCORE AUGMENTÉ
        int distGum = findNearestGumDist(map, pacRow, pacCol);
        score -= distGum * 350;
        
        // Danger des fantômes
        int minGhostDist = getMinGhostDistance(state, pacRow, pacCol);
        if (minGhostDist <= 1) score -= 50000;
        else if (minGhostDist == 2) score -= 3000;
        else if (minGhostDist == 3) score -= 1000;
        else if (minGhostDist <= 5) score -= 300;
        
        // Fantômes effrayés = GROS BONUS pour chasser (200-400 points par fantôme!)
        for (int i = 0; i < state.getNbrOfGhost(); i++) {
            int peur = state.getCompteurPeur(i);
            if (peur > 0) {
                for (Position gp : state.getGhostPositions(i)) {
                    int d = manhattan(pacRow, pacCol, gp.getRow(), gp.getColumn());
                    if (d <= 1) {
                        score += 5000;  // On va le manger!
                    } else if (d <= 3 && peur > 5) {
                        score += 3000 / d;  // Proche et temps suffisant
                    } else if (d <= 5 && peur > 8) {
                        score += 1500 / d;  // Atteignable
                    }
                }
            }
        }
        
        // Routes de fuite
        int routes = countRoutes(map, pacRow, pacCol);
        score += routes * 200;
        
        return score;
    }
    
    // ========================================================================
    // ANTI-OSCILLATION
    // ========================================================================
    
    private static double getAntiOscillationScore(String action, int newRow, int newCol, int ghostDist) {
        double score = 0;
        String newPosKey = newRow + "," + newCol;
        
        // Compter les visites récentes de cette position
        int visitCount = 0;
        for (String pos : positionHistory) {
            if (pos.equals(newPosKey)) visitCount++;
        }
        
        // PÉNALITÉ TRÈS FORTE pour revisiter une position
        if (visitCount > 0) {
            score -= visitCount * 6000;  // AUGMENTÉ
        }
        if (visitCount >= 2) {
            score -= 20000;  // Boucle détectée! AUGMENTÉ
        }
        
        // BLOQUER le demi-tour (sauf danger immédiat)
        if (isOpposite(action, lastAction)) {
            if (ghostDist > 2) {
                score -= 15000;  // AUGMENTÉ - presque interdit
            } else {
                score -= 2000;  // Même en danger, légère pénalité
            }
        }
        
        // Pattern A-B-A = oscillation → STRICTEMENT INTERDIT
        if (secondLastAction != null && action.equals(secondLastAction) 
            && isOpposite(lastAction, secondLastAction)) {
            score -= 30000;  // AUGMENTÉ
        }
        
        // GROS bonus pour continuer tout droit (évite les zigzags)
        if (action.equals(lastAction)) {
            score += 1500;  // AUGMENTÉ
        }
        
        // Bonus pour tourner (pas demi-tour) - encourage l'exploration
        if (!isOpposite(action, lastAction) && !action.equals(lastAction)) {
            score += 800;  // AUGMENTÉ
        }
        
        return score;
    }
    
    private static void updateHistory(String posKey, String action) {
        // Mettre à jour l'historique des positions
        positionHistory.addFirst(posKey);
        if (positionHistory.size() > POSITION_HISTORY_SIZE) {
            positionHistory.removeLast();
        }
        
        // Compteur de même direction
        if (action.equals(lastAction)) {
            sameDirectionCount++;
        } else {
            sameDirectionCount = 0;
        }
        
        // Mettre à jour l'historique des actions
        secondLastAction = lastAction;
        lastAction = action;
    }
    
    /**
     * Compte les gommes dans un rayon
     */
    private static int countGumsNear(char[][] map, int row, int col, int radius) {
        int count = 0;
        int size = map.length;
        for (int dr = -radius; dr <= radius; dr++) {
            for (int dc = -radius; dc <= radius; dc++) {
                if (Math.abs(dr) + Math.abs(dc) > radius) continue;
                int nr = row + dr, nc = col + dc;
                if (nr >= 0 && nr < size && nc >= 0 && nc < size) {
                    if (map[nr][nc] == '.') count++;
                    else if (map[nr][nc] == '*') count += 2;
                }
            }
        }
        return count;
    }
    
    // ========================================================================
    // FONCTIONS UTILITAIRES
    // ========================================================================
    
    private static int getMinGhostDistance(BeliefState state, int row, int col) {
        int minDist = Integer.MAX_VALUE;
        for (int i = 0; i < state.getNbrOfGhost(); i++) {
            if (state.getCompteurPeur(i) > 0) continue; // Ignorer fantômes effrayés
            for (Position gp : state.getGhostPositions(i)) {
                int d = manhattan(row, col, gp.getRow(), gp.getColumn());
                if (d < minDist) minDist = d;
            }
        }
        return minDist;
    }
    
    /**
     * Trouve le fantôme effrayé le plus proche
     */
    private static ScaredGhostInfo findNearestScaredGhost(BeliefState state, int row, int col) {
        ScaredGhostInfo info = new ScaredGhostInfo();
        info.distance = Integer.MAX_VALUE;
        
        for (int i = 0; i < state.getNbrOfGhost(); i++) {
            int peur = state.getCompteurPeur(i);
            if (peur > 0) {
                for (Position gp : state.getGhostPositions(i)) {
                    int d = manhattan(row, col, gp.getRow(), gp.getColumn());
                    if (d < info.distance) {
                        info.exists = true;
                        info.distance = d;
                        info.row = gp.getRow();
                        info.col = gp.getColumn();
                        info.fearTime = peur;
                    }
                }
            }
        }
        return info;
    }
    
    private static int getMinGhostDistanceFrom(BeliefState state, int row, int col) {
        return getMinGhostDistance(state, row, col);
    }
    
    private static int findNearestGumDist(char[][] map, int row, int col) {
        int size = map.length;
        // Recherche en spirale (rapide)
        for (int r = 0; r <= 15; r++) {
            for (int dr = -r; dr <= r; dr++) {
                for (int dc = -r; dc <= r; dc++) {
                    if (Math.abs(dr) + Math.abs(dc) != r) continue;
                    int nr = row + dr, nc = col + dc;
                    if (nr >= 0 && nr < size && nc >= 0 && nc < size) {
                        char c = map[nr][nc];
                        if (c == '.' || c == '*') return r;
                    }
                }
            }
        }
        return 20;
    }
    
    private static int countRoutes(char[][] map, int row, int col) {
        int count = 0;
        for (int[] d : DIRS) {
            if (isValid(map, row + d[0], col + d[1])) count++;
        }
        return count;
    }
    
    private static int manhattan(int r1, int c1, int r2, int c2) {
        return Math.abs(r1 - r2) + Math.abs(c1 - c2);
    }
    
    private static boolean isValid(char[][] map, int row, int col) {
        return row >= 0 && row < map.length && col >= 0 && col < map[0].length && map[row][col] != '#';
    }
    
    private static int getActionIndex(String action) {
        for (int i = 0; i < ACTIONS.length; i++) {
            if (ACTIONS[i].equals(action)) return i;
        }
        return 0;
    }
    
    private static boolean isOpposite(String a1, String a2) {
        if (a1 == null || a2 == null) return false;
        return (a1.equals(PacManLauncher.UP) && a2.equals(PacManLauncher.DOWN)) ||
               (a1.equals(PacManLauncher.DOWN) && a2.equals(PacManLauncher.UP)) ||
               (a1.equals(PacManLauncher.LEFT) && a2.equals(PacManLauncher.RIGHT)) ||
               (a1.equals(PacManLauncher.RIGHT) && a2.equals(PacManLauncher.LEFT));
    }
    
    private static String findValidAction(BeliefState state) {
        Position p = state.getPacmanPosition();
        char[][] map = state.getMap();
        
        // Éviter le demi-tour si possible
        for (int i = 0; i < 4; i++) {
            if (isOpposite(ACTIONS[i], lastAction)) continue;
            if (isValid(map, p.getRow() + DIRS[i][0], p.getColumn() + DIRS[i][1])) {
                return ACTIONS[i];
            }
        }
        // Sinon n'importe quelle direction valide
        for (int i = 0; i < 4; i++) {
            if (isValid(map, p.getRow() + DIRS[i][0], p.getColumn() + DIRS[i][1])) {
                return ACTIONS[i];
            }
        }
        return PacManLauncher.UP;
    }
}
