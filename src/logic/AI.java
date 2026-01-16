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
        
        // 1. UPDATE HISTORY
        Position currentPos = currentState.getPacmanPosition();
        String currentKey = currentPos.getRow() + "," + currentPos.getColumn();
        visited.put(currentKey, visited.getOrDefault(currentKey, 0) + 1);

        // 2. DEBUG FEEDBACK
        feedback(currentState);

        // --- NEW: REFLEX LAYER (INSTINCT DE TUEUR) ---
        // If a ghost is scared, visible, and 1 step away, KILL IT.
        String killMove = checkImmediateKill(currentState);
        if (killMove != null) {
            System.out.println(">>> KILL REFLEX ACTIVATED: " + killMove);
            return killMove; 
        }
        // ---------------------------------------------

        // 3. NORMAL SEARCH (AND-OR)
        Plans plans = currentState.extendsBeliefState();
        String bestAction = PacManLauncher.UP; 
        double maxScore = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < plans.size(); i++) {
            Result result = plans.getResult(i);
            if (plans.getAction(i).isEmpty()) continue;
            
            boolean startsWithUncertainty = (result.size() > 1);

            double score = evaluateANDNode(result, currentState, MAX_DEPTH - 1, startsWithUncertainty);

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
        try {
            JOptionPane.showMessageDialog(null, "Score calculated. Press OK for next move.", "AI Debugger", JOptionPane.PLAIN_MESSAGE);
        } catch (Exception e) {}
    }

    // --- NEW: IMMEDIATE KILL CHECKER ---
    private static String checkImmediateKill(BeliefState state) {
        Position pac = state.getPacmanPosition();
        
        for (int i = 0; i < state.getNbrOfGhost(); i++) {
            // Is he scared?
            if (state.getCompteurPeur(i) > 0) {
                TreeSet<Position> positions = state.getGhostPositions(i);
                
                // Is he visible (exactly 1 position)?
                if (positions.size() == 1) {
                    Position ghostPos = positions.first();
                    
                    // Is he adjacent?
                    int dRow = ghostPos.getRow() - pac.getRow();
                    int dCol = ghostPos.getColumn() - pac.getColumn();
                    
                    if (Math.abs(dRow) + Math.abs(dCol) == 1) {
                        // Return the direction immediately
                        if (dRow == -1) return PacManLauncher.UP;
                        if (dRow == 1)  return PacManLauncher.DOWN;
                        if (dCol == -1) return PacManLauncher.LEFT;
                        if (dCol == 1)  return PacManLauncher.RIGHT;
                    }
                }
            }
        }
        return null; // No kill found
    }

    // --- AND NODE ---
    private static double evaluateANDNode(Result result, BeliefState parent, int depth, boolean isInvisibleContext) {
        double minScore = Double.POSITIVE_INFINITY;
        boolean hasValidScenario = false;
        boolean currentContextIsInvisible = isInvisibleContext || (result.size() > 1);

        for (BeliefState nextState : result.getBeliefStates()) {
            hasValidScenario = true;
            double val;

            if (nextState.getLife() < parent.getLife() || nextState.getLife() <= 0) {
                if (currentContextIsInvisible) val = -500000.0; 
                else val = -1000000000.0; 
            } else {
                val = deepSearch(nextState, parent, depth, currentContextIsInvisible);
            }

            if (val < minScore) minScore = val;
        }
        return hasValidScenario ? minScore : -1000000000.0;
    }

    // --- OR NODE ---
    private static double deepSearch(BeliefState state, BeliefState parent, int depth, boolean isInvisibleContext) {
        if (depth == 0) return heuristic(state, parent, isInvisibleContext);

        Plans futurePlans = state.extendsBeliefState();
        if (futurePlans.size() == 0) return heuristic(state, parent, isInvisibleContext);

        double maxScore = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < futurePlans.size(); i++) {
            Result res = futurePlans.getResult(i);
            double score = evaluateANDNode(res, state, depth - 1, isInvisibleContext);
            if (score > maxScore) maxScore = score;
        }
        return maxScore;
    }

    private static double heuristic(BeliefState state, BeliefState parentState, boolean isUncertain) {
        return getObjectiveScore(state, parentState, isUncertain) - getDangerScore(state);
    }
private static double getObjectiveScore(BeliefState state, BeliefState parent, boolean isUncertain) {
        double score = 0;
        Position pac = state.getPacmanPosition();
        String key = pac.getRow() + "," + pac.getColumn();

        // 1. SCORE DIFFERENCE (Kill Confirmed > Super Gum > Gum)
        if (state.getScore() > parent.getScore()) {
            double diff = state.getScore() - parent.getScore();
            if (diff > 100) {
                score += diff * 10000.0; // Ghost Eaten
            } else {
                // Regular Coin
                score += diff * 20.0;
                
                // --- NEW: GREEDING LOGIC ---
                // If both ghosts are right behind us (Dist <= 2), eating coins is VERY valuable
                // because we know the rest of the map is safe.
                if (areAllGhostsClose(state)) {
                    score += diff * 500.0; // Massive bonus to clear the area
                }
            }
        }

        // 2. COIN DENSITY
        score += getCoinDensityScore(state, pac);
        
        // 3. SMART GHOST HUNTING
        if (!isUncertain) {
            score += getGhostHuntingScore(state, pac);
        }

        // 4. EXPLORATION
        if (isUncertain && visited.getOrDefault(key, 0) == 0) {
            score += 200.0; 
        }

        return score;
    }

    // --- NEW: CHECK IF ALL GHOSTS ARE CLOSE ---
    private static boolean areAllGhostsClose(BeliefState state) {
        Position pac = state.getPacmanPosition();
        int ghostCount = state.getNbrOfGhost();
        
        for(int i=0; i<ghostCount; i++) {
            // Ignore dead/scared ghosts for this check? 
            // Usually we care about the dangerous ones.
            if (state.getCompteurPeur(i) > 0) continue; 
            
            TreeSet<Position> positions = state.getGhostPositions(i);
            if (positions.isEmpty()) return false; // Unknown ghost = Not safe
            
            // Get nearest possible position
            int minDist = Integer.MAX_VALUE;
            for(Position p : positions) {
                int d = Math.abs(p.getRow() - pac.getRow()) + Math.abs(p.getColumn() - pac.getColumn());
                if(d < minDist) minDist = d;
            }
            
            // If any ghost is far away (> 2), we are NOT in the "All Close" scenario
            if (minDist > 2) return false;
        }
        return true; // All dangerous ghosts are within 2 steps
    }

    private static double getCoinDensityScore(BeliefState state, Position pac) {
        char[][] map = state.getMap();
        int radius = 3; 
        int coinCount = 0;
        double minDist = Double.MAX_VALUE;

        for (int r = pac.getRow() - radius; r <= pac.getRow() + radius; r++) {
            for (int c = pac.getColumn() - radius; c <= pac.getColumn() + radius; c++) {
                if (r >= 0 && r < map.length && c >= 0 && c < map[0].length) {
                    char cell = map[r][c];
                    if (cell == '.' || cell == '*') {
                        coinCount++;
                        double d = Math.abs(r - pac.getRow()) + Math.abs(c - pac.getColumn());
                        if (d < minDist) minDist = d;
                    }
                }
            }
        }

        double score = 0;
        if (coinCount > 0) score += coinCount * 100.0; 
        if (minDist != Double.MAX_VALUE) score += (2000.0 / (minDist + 1));
        return score;
    }

    private static double getGhostHuntingScore(BeliefState state, Position pac) {
        double score = 0;
        
        for (int i = 0; i < state.getNbrOfGhost(); i++) {
            int fearTimer = state.getCompteurPeur(i);
            
            if (fearTimer > 0) {
                TreeSet<Position> positions = state.getGhostPositions(i);
                
                // Only chase visible ghosts
                if (positions.size() == 1) {
                    Position ghostPos = positions.first(); 
                    int dist = Math.abs(ghostPos.getRow() - pac.getRow()) + Math.abs(ghostPos.getColumn() - pac.getColumn());
                    int requiredTime = dist + 2;

                    if (fearTimer >= requiredTime) {
                        score += 200000.0; 
                        score += (10000.0 / (dist + 1));
                    }
                }
            }
        }
        return score;
    }

    private static double getDangerScore(BeliefState state) {
        double danger = 0;
        Position pac = state.getPacmanPosition();
        String key = pac.getRow() + "," + pac.getColumn();

        int nbrVisits = visited.getOrDefault(key, 0);
        if (nbrVisits > 0) {
            danger += Math.pow(nbrVisits, 2) * 50.0; 
        }

        for (int i = 0; i < state.getNbrOfGhost(); i++) {
            if (state.getCompteurPeur(i) > 0) continue; 
            
            TreeSet<Position> positions = state.getGhostPositions(i);
            if (positions.isEmpty()) continue;

            for (Position gp : positions) {
                int dist = Math.abs(gp.getRow() - pac.getRow()) + Math.abs(gp.getColumn() - pac.getColumn());
                if (dist < 3) {
                    danger += 1000.0 / (dist + 1);
                }
            }
        }
        return danger;
    }

    public static void feedback(BeliefState currentState) {
        System.out.println("\n=== ANALYSE AND-OR (Greedy Flanker) ===");
        Position cur = currentState.getPacmanPosition();
        System.out.println("Pos Pacman: " + cur.getRow() + "," + cur.getColumn());

        System.out.println("--- Ghost Positions ---");
        int nbrGhosts = currentState.getNbrOfGhost();
        for (int i = 0; i < nbrGhosts; i++) {
            System.out.print("Ghost " + i + ": ");
            TreeSet<Position> positions = currentState.getGhostPositions(i);
            if (positions.isEmpty()) {
                System.out.print("Unknown/Dead");
            } else {
                int count = 0;
                for (Position p : positions) {
                    System.out.print("(" + p.getRow() + "," + p.getColumn() + ") ");
                    count++;
                    if (count >= 10) { 
                        System.out.print("... [" + positions.size() + " total]");
                        break; 
                    }
                }
            }
            if (currentState.getCompteurPeur(i) > 0) {
                System.out.print(" [AFRAID: " + currentState.getCompteurPeur(i) + "]");
            }
            System.out.println();
        }
        System.out.println("-----------------------");

        Plans plans = currentState.extendsBeliefState();

        for (int i = 0; i < plans.size(); i++) {
            Result result = plans.getResult(i);
            ArrayList<String> actions = plans.getAction(i);
            if (actions.isEmpty()) continue;
            String direction = actions.get(0); 
            
            boolean startsInvisible = (result.size() > 1);
            
            double minScore = Double.POSITIVE_INFINITY;
            int deathCount = 0;
            
            for (BeliefState nextState : result.getBeliefStates()) {
                if (nextState.getLife() <= 0) deathCount++;
            }
            
            boolean certainDeath = (result.size() > 0 && deathCount == result.size());

            for (BeliefState nextState : result.getBeliefStates()) {
                double val;
                if (nextState.getLife() <= 0) {
                    if (certainDeath) val = -1000000000.0;
                    else val = -500000.0;
                } else {
                    val = deepSearch(nextState, currentState, MAX_DEPTH - 1, startsInvisible);
                }
                if (val < minScore) minScore = val;
            }

            System.out.println(String.format("Action: %-6s | Score: %14.0f | Scénarios: %d | Type: %s", 
                direction, minScore, result.size(), startsInvisible ? "Invisible" : "Visible"));
        }
        System.out.println("============================================\n");
    }
}