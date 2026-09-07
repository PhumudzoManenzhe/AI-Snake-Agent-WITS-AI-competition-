import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Random;

import za.ac.wits.snake.DevelopmentAgent;

public class MyAgent extends DevelopmentAgent {
    int[][] map;
    int w, h;
    String[][] snakeState;
    int[][] snakeHeads;
    int mySnakeNum;
    int[] apple;
    int[] directions; 

    public static void main(String args[]) {
        MyAgent agent = new MyAgent();
        MyAgent.start(agent, args);
    }

    @Override
    public void run() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            String initString = br.readLine();
            String[] temp = initString.split(" ");
            int nSnakes = Integer.parseInt(temp[0]);
            w = Integer.parseInt(temp[1]);
            h = Integer.parseInt(temp[2]);
            map = new int[w][h];

            while (true) {
                String line = br.readLine();
                if (line.contains("Game Over")) break;

                // Read apple position
                String[] appleCoords = line.split(" ");
                apple = new int[]{Integer.parseInt(appleCoords[0]), Integer.parseInt(appleCoords[1])};

                
                
                
                
               String n= br.readLine();
               System.out.print(n);               

                snakeState = new String[4][];
                snakeHeads = new int[nSnakes][2];
                directions = new int[nSnakes];

                // Reset map
                for (int i = 0; i < w; i++)
                    Arrays.fill(map[i], 0);

                // Mark apple on map
                if (apple[0] >= 0 && apple[1] >= 0) map[apple[1]][apple[0]] = -1;
                mySnakeNum = Integer.parseInt(n);
                
                // Read all snakes
                for (int i = 0; i < nSnakes; i++) {
                    snakeState[i] = br.readLine().split(" ");
                    if (!snakeState[i][0].equals("dead")) {
                        String[] body = Arrays.copyOfRange(snakeState[i], 3, snakeState[i].length);
                        for (int j = 0; j < body.length; j++) {
                            String[] coord = body[j].split(",");
                            int x = Integer.parseInt(coord[0]);
                            int y = Integer.parseInt(coord[1]);
                            map[y][x] = 1;
                            if (j == 0) snakeHeads[i] = new int[]{x, y};
                            if (j < body.length - 1) {
                                // Fill in segments between kinks
                                String[] next = body[j + 1].split(",");
                                int nx = Integer.parseInt(next[0]);
                                int ny = Integer.parseInt(next[1]);
                                if (x == nx) {
                                    for (int k = Math.min(y, ny); k <= Math.max(y, ny); k++) map[k][x] = 1;
                                } else if (y == ny) {
                                    for (int k = Math.min(x, nx); k <= Math.max(x, nx); k++) map[y][k] = 1;
                                }
                            }
                        }

                        // Determine direction from head to first body segment
                        if (body.length > 1) {
                            String[] head = body[0].split(",");
                            String[] first = body[1].split(",");
                            int hx = Integer.parseInt(head[0]);
                            int hy = Integer.parseInt(head[1]);
                            int fx = Integer.parseInt(first[0]);
                            int fy = Integer.parseInt(first[1]);
                            if (hy < fy) directions[i] = 0; // Up
                            else if (hy > fy) directions[i] = 1; // Down
                            else if (hx < fx) directions[i] = 2; // Left
                            else directions[i] = 3; // Right
                        }
                    }
                }

                int move = makeMove(nSnakes);
                System.out.println(move);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int makeMove(int nSnakes) {
        int[] pos = snakeHeads[mySnakeNum];
        ArrayList<int[]> pathToApple = bfs(pos, apple, map);

        boolean safeApple = false;
        if (!pathToApple.isEmpty()) {
            int myDist = pathToApple.size();
            safeApple = true;
            // Compare my distance with opponents
            for (int i = 0; i < nSnakes; i++) {
                if (i != mySnakeNum && snakeState[i][0].equals("alive")) {
                    ArrayList<int[]> oppPath = bfs(snakeHeads[i], apple, map);
                    int oppDist = oppPath.size();
                    if (oppDist > 0 && oppDist <= myDist) {
                        safeApple = false;
                        break;
                    }
                }
            }
        }

        int move;
        if (safeApple && !pathToApple.isEmpty()) {
            int[] next = pathToApple.get(pathToApple.size() - 1);
            move = directionTo(next, pos);
        } else {
            move = safeMove(pos); // fallback
        }

        return move;
    }

    private int safeMove(int[] pos) {
        int x = pos[0], y = pos[1];

        // Possible directions: Up=0, Down=1, Left=2, Right=3
        int[][] moves = {
            {x, y - 1}, // Up
            {x, y + 1}, // Down
            {x - 1, y}, // Left
            {x + 1, y}  // Right
        };

        // Step 1: compute target = center of board
        int targetX = w/2 ;
        int targetY = h;

        // Step 2: try to move closer to center safely
        int bestDir = -1;
        int bestDist = Integer.MAX_VALUE;
        for (int dir = 0; dir < 4; dir++) {
            int nx = moves[dir][0];
            int ny = moves[dir][1];
            if (nx >= 0 && nx < w && ny >= 0 && ny < h && map[ny][nx] == 0) {
                int dist = Math.abs(nx - targetX) + Math.abs(ny - targetY);
                if (dist < bestDist) {
                    bestDist = dist;
                    bestDir = dir;
                }
            }
        }

        // Step 3: if no safe center move, try to block enemy
        if (bestDir == -1 && apple[0] >= 0) {
            for (int i = 0; i < snakeHeads.length; i++) {
                if (i == mySnakeNum) continue;
                int[] opp = snakeHeads[i];
                if (opp != null && opp[0] >= 0) {
                    // Find midpoint between opponent and apple
                    int midX = (opp[0] + apple[0]) / 2;
                    int midY = (opp[1] + apple[1]) / 2;
                    for (int dir = 0; dir < 4; dir++) {
                        int nx = moves[dir][0];
                        int ny = moves[dir][1];
                        if (nx >= 0 && nx < w && ny >= 0 && ny < h && map[ny][nx] == 0) {
                            int dist = Math.abs(nx - midX) + Math.abs(ny - midY);
                            if (dist < bestDist) {
                                bestDist = dist;
                                bestDir = dir;
                            }
                        }
                    }
                }
            }
        }

        // Step 4: fallback to original safe random-ish move
        if (bestDir != -1) return bestDir;
        for (int dir = 0; dir < 4; dir++) {
            int nx = moves[dir][0];
            int ny = moves[dir][1];
            if (nx >= 0 && nx < w && ny >= 0 && ny < h && map[ny][nx] == 0) return dir;
        }

        return 0; // no free space, go up
    }


    private int directionTo(int[] next, int[] pos) {
        if (next[1] - pos[1] == 1) return 1; // Down
        if (next[1] - pos[1] == -1) return 0; // Up
        if (next[0] - pos[0] == 1) return 3; // Right
        if (next[0] - pos[0] == -1) return 2; // Left
        return new Random().nextInt(4); // fallback random
    }

    private ArrayList<int[]> bfs(int[] root, int[] dest, int[][] map) {
        if (root[0] < 0 || root[1] < 0 || root[0] >= w || root[1] >= h) return new ArrayList<>();
        LinkedList<int[]> q = new LinkedList<>();
        int[][][] parent = new int[h][w][2];
        boolean[][] visited = new boolean[h][w];
        for (int i = 0; i < w; i++)
            for (int j = 0; j < h; j++) parent[j][i] = new int[]{-1, -1};
        visited[root[1]][root[0]] = true;
        q.add(root);
        int[] curr = root;

        while (!q.isEmpty() && !Arrays.equals(curr, dest)) {
            curr = q.remove();
            int[][] neighbors = {{curr[0], curr[1] - 1}, {curr[0], curr[1] + 1}, {curr[0] - 1, curr[1]}, {curr[0] + 1, curr[1]}};
            for (int[] n : neighbors) {
                int nx = n[0], ny = n[1];
                if (nx >= 0 && nx < w && ny >= 0 && ny < h && !visited[ny][nx] && (map[ny][nx] == 0 || map[ny][nx] == -1)) {
                    visited[ny][nx] = true;
                    parent[ny][nx] = curr;
                    q.add(n);
                }
            }
        }

        ArrayList<int[]> path = new ArrayList<>();
        if (visited[dest[1]][dest[0]]) {
            curr = dest;
            while (!Arrays.equals(curr, root)) {
                path.add(curr);
                curr = parent[curr[1]][curr[0]];
            }
        }
        return path;
    }
}
