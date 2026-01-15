package logic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.JOptionPane; // <--- Import for the Popup

// ... (Keep your existing Plans and Result classes exactly as they are) ...
class Plans {
    ArrayList<Result> results;
    ArrayList<ArrayList<String>> actions;
    public Plans() { results = new ArrayList<>(); actions = new ArrayList<>(); }
    public void addPlan(Result r, ArrayList<String> a) { results.add(r); actions.add(a); }
    public int size() { return results.size(); }
    public Result getResult(int i) { return results.get(i); }
    public ArrayList<String> getAction(int i) { return actions.get(i); }
}

class Result {
    private ArrayList<BeliefState> beliefStates;
    public Result(ArrayList<BeliefState> s) { beliefStates = s; }
    public int size() { return beliefStates.size(); }
    public BeliefState getBeliefState(int i) { return beliefStates.get(i); }
    public ArrayList<BeliefState> getBeliefStates() { return beliefStates; }
}

// --- AI CLASS ---

public class AI {
    
    private static java.util.Map<String, Integer> visited = new java.util.HashMap<>();

    public static String findNextMove(BeliefState currentState) {
        
        // 1. Debug Feedback
        feedback(currentState);

        // 2. Memory
        Position currentPos = currentState.getPacmanPosition();
        String key = currentPos.getRow() + "," + currentPos.getColumn();
        visited.put(key, visited.getOrDefault(key, 0) + 1);

        Plans plans = currentState.extendsBeliefState();
        String bestAction = PacManLauncher.UP; 
        double maxScore = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < plans.size(); i++) {
            Result result = plans.getResult(i);
            ArrayList<String> actions = plans.getAction(i);
            
            if (actions.isEmpty()) continue;
            
            double score = evaluateResult(result, currentState);

            // Random tie-breaker
            score += Math.random() * 0.01;

            if (score > maxScore) {
                maxScore = score;
                bestAction = actions.get(0);
            }
        }
        
        // --- DEBUG CONTROL ---
        // Choose ONE of the following methods:
        
        //waitForUserPopup(); // OPTION 1: You must press ENTER on the popup
        // playSlowMotion();   // OPTION 2: Game plays automatically but slowly
        
        // ---------------------

        return bestAction;
    }

    // --- OPTION 1: STEP-BY-STEP (Popup) ---
    private static void waitForUserPopup() {
        // This opens a tiny dialog window. 
        // It pauses the code here, but allows the Game Window background to keep rendering.
        // You can just hit SPACE or ENTER to dismiss it quickly.
        try {
            JOptionPane.showMessageDialog(null, "Score calculated. Press OK for next move.", "AI Debugger", JOptionPane.PLAIN_MESSAGE);
        } catch (Exception e) {
            // Ignore UI errors
        }
    }

    // --- EXISTING HELPER METHODS (Keep your logic) ---

    private static double evaluateResult(Result result, BeliefState parentState) {
        if (result.size() == 0) return Double.NEGATIVE_INFINITY;
        double totalScore = 0;
        for (BeliefState state : result.getBeliefStates()) {
            totalScore += heuristic(state, parentState);
        }
        return totalScore / result.size();
    }

    private static double heuristic(BeliefState state, BeliefState parentState) {
        Position pac = state.getPacmanPosition();

        
        double objectiveScore = getObjectiveScore(state, parentState);
        double dangerScore = getDangerScore(state);

        return objectiveScore - dangerScore;
    }

    private static double getObjectiveScore(BeliefState state, BeliefState parent) {
        double score = 0;
        Position pac = state.getPacmanPosition();
        
        // 1. BASE GAME SCORE
        score += state.getScore() * 10.0;

        // 2. PASSIVE NAVIGATION (Distance to nearest)
        int distGum = state.distanceMinToGum();
        if (distGum != Integer.MAX_VALUE && distGum > 0) {
            score += (150.0 / distGum); // increased weight
        }

        // 3. GLOBAL FLOW (Solves side-switching)
        // If there are no coins nearby, look at the big picture
        if (distGum > 10 || distGum == Integer.MAX_VALUE) {
            score += getGlobalCoinGravity(state, pac);
        }

        // 4. GHOST ANALYSIS
        int nbrGhosts = state.getNbrOfGhost();
        int nearbyVisibleGhosts = 0;
        for(int i = 0; i < nbrGhosts; i++) {
            TreeSet<Position> positions = state.getGhostPositions(i);
            if (positions.isEmpty()) continue;
            Position gPos = positions.first();
            int dist = Math.abs(gPos.getRow() - pac.getRow()) + Math.abs(gPos.getColumn() - pac.getColumn());

            if (state.getCompteurPeur(i) > 0) {
                if (dist > 0) score += (5000.0 / dist);
                else score += 10000.0; 
            } else if (positions.size() == 1 && dist < 6) {
                nearbyVisibleGhosts++;
            }
        }

        // 5. SUPER GUM STRATEGY (The "Stalking" fix)
        // We look for the distance to the nearest Super Gum
        int distToSuper = getDistanceToNearestSuperGum(state, pac);
        if (distToSuper == 1 && nearbyVisibleGhosts < 2) {
            // Right next to it but ghosts aren't ready? PAIN.
            score -= 15000.0; 
        } else if (distToSuper >= 2 && distToSuper <= 3 && nearbyVisibleGhosts < 2) {
            // This is the "Sweet Spot". Pacman will try to stay here.
            score += 200.0; 
        }

        // 6. NORMAL GUM CONSUMPTION
        int normalGumsParent = parent.getNbrOfGommes() - parent.getNbrOfSuperGommes();
        int normalGumsChild = state.getNbrOfGommes() - state.getNbrOfSuperGommes();
        if (normalGumsChild < normalGumsParent) score += 300.0;

        return score;
    }

    // Helper to find the nearest Super Gum position
    private static int getDistanceToNearestSuperGum(BeliefState state, Position pac) {
        int minDist = Integer.MAX_VALUE;
        char[][] map = state.getMap();
        for (int r = 0; r < map.length; r++) {
            for (int c = 0; c < map[r].length; c++) {
                if (map[r][c] == '*') {
                    int d = Math.abs(r - pac.getRow()) + Math.abs(c - pac.getColumn());
                    if (d < minDist) minDist = d;
                }
            }
        }
        return minDist;
    }

    // Solves the "Switching Sides" problem by looking at the average coin position
    private static double getGlobalCoinGravity(BeliefState state, Position pac) {
        double avgR = 0, avgC = 0, count = 0;
        char[][] map = state.getMap();
        for (int r = 0; r < map.length; r++) {
            for (int c = 0; c < map[r].length; c++) {
                if (map[r][c] == '.' || map[r][c] == '*') {
                    avgR += r; avgC += c; count++;
                }
            }
        }
        if (count == 0) return 0;
        avgR /= count; avgC /= count;
        double distToMass = Math.abs(avgR - pac.getRow()) + Math.abs(avgC - pac.getColumn());
        return (200.0 / (distToMass + 1)); // Pulls Pacman towards the high-density areas
    }

    private static double getDangerScore(BeliefState state) {
        double danger = 0;
        Position pac = state.getPacmanPosition();
        int nbrGhosts = state.getNbrOfGhost();

        if (state.getLife() <= 0) {
            return 1000000.0;
        }


        for (int i = 0; i < nbrGhosts; i++) {
            if (state.getCompteurPeur(i) > 0) continue;

            TreeSet<Position> positions = state.getGhostPositions(i);
            if (positions.isEmpty()) continue;

            double probability = 1.0 / positions.size();

            for (Position ghostPos : positions) {
                int dist = Math.abs(ghostPos.getRow() - pac.getRow()) + Math.abs(ghostPos.getColumn() - pac.getColumn());
                if (state.getLife() <= 0) return -10000.0*probability*probability;
                else if (dist < 1) danger += (200.0 * probability); 
                else if (dist < 5) danger += (((5-dist)*30) * probability); // Adjusted heuristic per your code
                boolean isAligned = (ghostPos.getRow() == pac.getRow()) || (ghostPos.getColumn() == pac.getColumn());
                if (isAligned) {
                    // On ajoute 30 au danger (donc -30 au score final)
                    // On peut ajouter une condition de distance (ex: && dist < 10) si tu trouves qu'il est trop peureux de loin.
                    danger += (30.0 * probability);
                }
            }
                
        }

        String key = pac.getRow() + "," + pac.getColumn();
        int timesVisited = visited.getOrDefault(key, 0);

        if (timesVisited > 0) {
            // La pénalité augmente à chaque passage (70, 140, 210, etc.)
            danger += 70.0 * timesVisited;
        }
        return danger;
    }

public static void feedback(BeliefState currentState) {
        System.out.println("\n=== ANALYSE FEEDBACK ===");
        
        // 1. Infos Pacman
        Position cur = currentState.getPacmanPosition();
        System.out.println("Position Pacman: " + cur.getRow() + "," + cur.getColumn());

        // 2. Infos Fantômes (Positions possibles)
        System.out.println("--- Croyances Fantômes ---");
        int nbrGhosts = currentState.getNbrOfGhost();
        for (int i = 0; i < nbrGhosts; i++) {
            System.out.print("Fantôme " + i + ": ");
            
            TreeSet<Position> positions = currentState.getGhostPositions(i);
            
            // Si la liste est vide, le fantôme est mort ou pas encore spawn
            if (positions.isEmpty()) {
                System.out.print("Inconnu/Mort");
            } else {
                // On affiche les coordonnées
                // Si la liste est très longue (brouillard), on limite l'affichage pour ne pas spammer
                int count = 0;
                for (Position p : positions) {
                    System.out.print("(" + p.getRow() + "," + p.getColumn() + ") ");
                    count++;
                    if (count >= 10) { 
                        System.out.print("... [" + positions.size() + " pos possibles]");
                        break; 
                    }
                }
            }

            // Affiche si le fantôme a peur (c'est bon à savoir pour le score)
            if (currentState.getCompteurPeur(i) > 0) {
                System.out.print(" [PEUR: " + currentState.getCompteurPeur(i) + "]");
            }
            System.out.println(); // Saut de ligne après chaque fantôme
        }
        System.out.println("--------------------------");

        // 3. Calcul des scores pour les mouvements
        Plans plans = currentState.extendsBeliefState();

        for (int i = 0; i < plans.size(); i++) {
            Result result = plans.getResult(i);
            ArrayList<String> actions = plans.getAction(i);

            if (actions.isEmpty()) continue;

            String direction = actions.get(0); 
            
            // Note: assure-toi que ta méthode evaluateResult accepte bien (Result, Position)
            double scoreMoyen = evaluateResult(result, currentState);

            System.out.println(String.format("Action: %-6s | Score: %10.2f | Scénarios possibles: %d", 
                direction, scoreMoyen, result.size()));
        }
        System.out.println("========================\n");
    }
}