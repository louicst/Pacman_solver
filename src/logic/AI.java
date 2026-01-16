package logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JOptionPane;
import java.util.TreeSet;

public class AI {
    private static Map<String, Integer> visited = new HashMap<>();
    private static final int MAX_DEPTH = 3; 

    public static String findNextMove(BeliefState currentState) {

        feedback(currentState);

        Position currentPos = currentState.getPacmanPosition();
        String currentKey = currentPos.getRow() + "," + currentPos.getColumn();
        
        // On marque la case actuelle
        visited.put(currentKey, visited.getOrDefault(currentKey, 0) + 1);

        Plans plans = currentState.extendsBeliefState();
        String bestAction = PacManLauncher.UP; 
        double maxScore = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < plans.size(); i++) {
            Result result = plans.getResult(i);
            if (plans.getAction(i).isEmpty()) continue;
            
            double score = Double.POSITIVE_INFINITY; // Start with max possible
            for (BeliefState nextState : result.getBeliefStates()) {
                double val = deepSearch(nextState, currentState, MAX_DEPTH - 1);
                
                // In an AND node, the value is the WORST possible outcome (Min)
                if (val < score) {
                    score = val;
                }
            }
            // No division. The value of the action is defined by its weakest link.
            score += Math.random() * 0.01;

            if (score > maxScore) {
                maxScore = score;
                bestAction = plans.getAction(i).get(0);
            }
        }
        waitForUserPopup();
        
        return bestAction;
    }

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

    private static double deepSearch(BeliefState state, BeliefState parent, int depth) {
        // La vie reste importante pour finir la map, mais on ne sur-réagit pas
        if (state.getLife() < parent.getLife() || state.getLife() <= 0) return -1000000.0;
        if (depth == 0) return heuristic(state, parent);

        Plans futurePlans = state.extendsBeliefState();
        if (futurePlans.size() == 0) return heuristic(state, parent);

        double bestFutureScore = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < futurePlans.size(); i++) {
            double avgScore = 0;
            Result res = futurePlans.getResult(i);
            for (BeliefState futureState : res.getBeliefStates()) {
                avgScore += deepSearch(futureState, state, depth - 1);
            }
            avgScore /= res.size();
            if (avgScore > bestFutureScore) bestFutureScore = avgScore;
        }
        return bestFutureScore;
    }

    private static double heuristic(BeliefState state, BeliefState parentState) {
        return getObjectiveScore(state, parentState) - getDangerScore(state);
    }

    private static double getObjectiveScore(BeliefState state, BeliefState parent) {
        double score = 0;
        Position pac = state.getPacmanPosition();
        String key = pac.getRow() + "," + pac.getColumn();

        // 1. BONUS EXPLORATION (Massif)
        // Si la case n'a JAMAIS été visitée, c'est un gain énorme
        if (visited.getOrDefault(key, 0) == 0) {
            score += 50000.0; 
        }

        // 2. SCORE DE JEU ET GOMMES
        // On récompense le fait de manger (différence de score réel)
        if (state.getScore() > parent.getScore()) {
            score += (state.getScore() - parent.getScore()) * 100.0;
        }

        // 3. CHASSE ET MODE BERGER
        int nbrGhosts = state.getNbrOfGhost();
        int ghostsFollowing = 0;
        for(int i = 0; i < nbrGhosts; i++) {
            TreeSet<Position> positions = state.getGhostPositions(i);
            if (positions.isEmpty() || positions.size() > 1) continue;
            
            Position gPos = positions.first();
            int dist = Math.abs(gPos.getRow() - pac.getRow()) + Math.abs(gPos.getColumn() - pac.getColumn());
            
            if (state.getCompteurPeur(i) > 0) {
                score += (200000.0 / (dist + 1)); // Attraction fatale pour les bleus
            } else if (dist < 6) {
                ghostsFollowing++;
            }
        }

        // 4. GRAVITÉ GLOBALE (Si on a tout mangé localement, on change de zone)
        score += getGlobalCoinGravity(state, pac) * 2.0;

        return score;
    }

    private static double getDangerScore(BeliefState state) {
        if (state.getLife() <= 0) return 1000000.0;
        
        double danger = 0;
        Position pac = state.getPacmanPosition();
        String key = pac.getRow() + "," + pac.getColumn();

        // 1. PÉNALITÉ DE LOOP (La plus importante ici)
        // Plus on a visité cette case, plus elle devient "interdite"
        int nbrVisits = visited.getOrDefault(key, 0);
        if (nbrVisits > 0) {
            danger += Math.pow(nbrVisits, 2) * 2000.0; 
        }

        // 2. DANGER DES FANTÔMES (Réduit pour favoriser le passage)
        for (int i = 0; i < state.getNbrOfGhost(); i++) {
            if (state.getCompteurPeur(i) > 0) continue;
            TreeSet<Position> positions = state.getGhostPositions(i);
            if (positions.isEmpty()) continue;
            
            double riskAversion = (positions.size() > 1) ? 1.5 : 1.0; // Moins peur de l'incertitude
            double proba = 1.0 / positions.size();

            for (Position gp : positions) {
                int dist = Math.abs(gp.getRow() - pac.getRow()) + Math.abs(gp.getColumn() - pac.getColumn());
                
                // On n'a peur que si le fantôme est VRAIMENT proche (< 4 cases)
                if (dist < 4) {
                    danger += (Math.pow(5 - dist, 3) * 500.0 * proba * riskAversion);
                }
            }
        }

        return danger;
    }

    private static double getGlobalCoinGravity(BeliefState state, Position pac) {
        double avgR = 0, avgC = 0, count = 0;
        char[][] map = state.getMap();
        for (int r = 0; r < map.length; r++)
            for (int c = 0; c < map[r].length; c++)
                if (map[r][c] == '.' || map[r][c] == '*') {
                    avgR += r; avgC += c; count++;
                }
        if (count == 0) return 0;
        avgR /= count; avgC /= count;
        double dist = Math.abs(avgR - pac.getRow()) + Math.abs(avgC - pac.getColumn());
        return (5000.0 / (dist + 1)); // Augmenté pour tirer Pac-Man vers les gommes
    }
    public static void feedback(BeliefState currentState) {
        System.out.println("\n=== ANALYSE FEEDBACK (Deep Search) ===");
        
        Position cur = currentState.getPacmanPosition();
        System.out.println("Position Pacman: " + cur.getRow() + "," + cur.getColumn());

        // Display Ghost Beliefs
        System.out.println("--- Croyances Fantômes ---");
        int nbrGhosts = currentState.getNbrOfGhost();
        for (int i = 0; i < nbrGhosts; i++) {
            System.out.print("Fantôme " + i + ": ");
            TreeSet<Position> positions = currentState.getGhostPositions(i);
            if (positions.isEmpty()) {
                System.out.print("Inconnu/Mort");
            } else {
                int count = 0;
                for (Position p : positions) {
                    System.out.print("(" + p.getRow() + "," + p.getColumn() + ") ");
                    count++;
                    if (count >= 5) { 
                        System.out.print("... [" + positions.size() + " pos]");
                        break; 
                    }
                }
            }
            if (currentState.getCompteurPeur(i) > 0) System.out.print(" [PEUR]");
            System.out.println(); 
        }
        System.out.println("--------------------------");

        Plans plans = currentState.extendsBeliefState();

        for (int i = 0; i < plans.size(); i++) {
            Result result = plans.getResult(i);
            ArrayList<String> actions = plans.getAction(i);

            if (actions.isEmpty()) continue;
            String direction = actions.get(0); 
            
            // --- CORRECTION: USE DEEP SEARCH LIKE THE MAIN LOOP ---
            double worstCaseScore = Double.POSITIVE_INFINITY;
            
            for (BeliefState nextState : result.getBeliefStates()) {
                // We use the same MAX_DEPTH as the real decision making
                double val = deepSearch(nextState, currentState, MAX_DEPTH - 1);
                
                if (val < worstCaseScore) {
                    worstCaseScore = val;
                }
            }
            // -----------------------------------------------------

            System.out.println(String.format("Action: %-6s | Score (Deep Min): %10.2f | Scénarios: %d", 
                direction, worstCaseScore, result.size()));
        }
        System.out.println("========================\n");
    }
}