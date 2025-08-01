package org.example;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

public class BoidPanel extends JPanel {
    static JFrame frame;
    private final ArrayList<Boid> boids = new ArrayList<>();
    private final ArrayList<Point> averagePositions = new ArrayList<>();
    private final ArrayList<Point> averageVelocity = new ArrayList<>();

    public BoidPanel() {
        Random rand = new Random();

        for (int i = 0; i < 125; ++i) {
            int startX = rand.nextInt(2500);
            int startY = rand.nextInt(1400);
            double xVel = rand.nextInt(5) + 2;
            double yVel = rand.nextInt(5) + 2;
            this.boids.add(new Boid(startX, startY, xVel, yVel));
        }

        Timer timer = new Timer(16, (e) -> {
            for (Boid boid : this.boids) {
                boid.closeCounter = 0;
                boid.groupId = -1; // Reset group ID
            }

            // Find groups and calculate multiple centers
            identifyGroups();

            for (Boid boid : this.boids) {
                boid.move(this.getWidth(), this.getHeight());
            }

            this.repaint();
        });
        timer.start();
    }

    /**
     * Identify groups of boids based on proximity.
     */
    private void identifyGroups() {
        int nextGroupId = 0;
        HashSet<Integer> assignedBoids = new HashSet<>();
        averagePositions.clear(); // Clear previous frame’s data
        averageVelocity.clear();

        // Reset close status before assigning groups
        for (Boid boid : boids) {
            boid.close = false;
            boid.groupId = -1;
        }

        for (Boid boid : boids) {
            if (boid.groupId == -1) {  // If the boid is not yet assigned to a group
                ArrayList<Boid> group = new ArrayList<>();
                findGroup(boid, group, nextGroupId);
                nextGroupId++;

                // Only mark boids as "close" if there is more than one boid in the group
                if (group.size() > 1) {
                    for (Boid b : group) {
                        b.close = true;
                    }
                }

                // Calculate average position for the group
                double sumX = 0, sumY = 0;
                double sumXVel = 0, sumYVel = 0;
                for (Boid b : group) {
                    boid.separation(b.x, b.y);
                    sumX += b.x;
                    sumY += b.y;
                    sumXVel += b.x_velocity;
                    sumYVel += b.y_velocity;
                    assignedBoids.add(b.hashCode());

                }
                if (group.size() > 1) { // Ensure groups are meaningful
                    int avgX = (int) (sumX / group.size());
                    int avgY = (int) (sumY / group.size());
                    int avgXVel = (int) (sumXVel / group.size());
                    int avgYVel = (int) (sumYVel / group.size());
//                    System.out.println(avgXVel);
//                    System.out.println(avgYVel);
                    averagePositions.add(new Point(avgX, avgY));
                    averageVelocity.add(new Point(avgXVel, avgYVel));
                }
            }
            boid.move_towards_average(averagePositions);
            boid.alignment(averageVelocity);

        }
    }


    /**
     * Uses a flood-fill-like approach to group nearby boids recursively.
     */
    private void findGroup(Boid boid, ArrayList<Boid> group, int groupId) {
        if (boid.groupId != -1) return; // Already assigned
        boid.groupId = groupId;
        group.add(boid);

        for (Boid other : boids) {
            if (other.groupId == -1 && distance(boid, other) < 125) {
                findGroup(other, group, groupId);
            }
        }
    }

    /**
     * Calculate the Euclidean distance between two boids.
     */
    private double distance(Boid a, Boid b) {
        return Math.sqrt(Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2));
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.lightGray);
        g.fillRect(0, 0, this.getWidth(), this.getHeight());

        Graphics2D g2d = (Graphics2D) g;

        // Draw boids
        for (Boid boid : this.boids) {
            g2d.setColor(boid.close ? Color.BLACK : Color.BLACK);

            int size = 20;
            int x = boid.x;
            int y = boid.y;

            int[] xPoints = {0, size / 2, -size / 2};
            int[] yPoints = {-size / 2, size / 2, size / 2};

            double angle = Math.atan2(boid.y_velocity, boid.x_velocity) + Math.PI / 2;

            g2d.translate(x, y);
            g2d.rotate(angle);

            g2d.fillPolygon(xPoints, yPoints, 3);

            g2d.rotate(-angle);
            g2d.translate(-x, -y);
        }

        // Draw purple balls for each group's center
//        g2d.setColor(Color.WHITE);
//        int ballSize = 15;
//        for (Point p : averagePositions) {
//            g2d.fillOval(p.x - ballSize / 2, p.y - ballSize / 2, ballSize, ballSize);
//        }
    }

    public static void main(String[] args) {
        frame = new JFrame("Bouncing Boids with Groups");
        frame.setSize(2500, 1500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        BoidPanel boidPanel = new BoidPanel();
        frame.add(boidPanel);
        frame.setVisible(true);
    }
}

class Boid {
    static final int SIZE = 50;
    int x;
    int y;
    double x_velocity;
    double y_velocity;
    boolean close = false;
    int closeCounter = 0;
    int groupId = -1; // New: Keeps track of boid’s group

    public Boid(int startX, int startY, double xVel, double yVel) {
        this.x = startX;
        this.y = startY;
        this.x_velocity = xVel;
        this.y_velocity = yVel;
    }

    public void move(int width, int height) {
        this.x += (int) this.x_velocity;
        this.y += (int) this.y_velocity;

        if (this.x + SIZE >= width){
            this.x = 0;
        }
        if (this.x + SIZE <= 0) {
            this.x = width;
        }
        if (this.y + SIZE >= height){
            this.y = 0;
        }
        if (this.y + SIZE <= 0) {
            this.y = height;
        }

    }

    public void move_towards_average(ArrayList<Point> averagePositions) {
        if (averagePositions.isEmpty()) return; // Prevent crash

        // Find the closest average position
        Point closest = averagePositions.get(0);
        double minDistance = distance(this.x, this.y, closest.x, closest.y);

        for (Point p : averagePositions) {
            double d = distance(this.x, this.y, p.x, p.y);
            if (d < minDistance) {
                minDistance = d;
                closest = p;
            }
        }

        // Move gradually towards the closest average point
        double speedFactor = 0.0027; // Reduce speed for smoother movement
        x_velocity += (closest.x - this.x) * speedFactor;
        y_velocity += (closest.y - this.y) * speedFactor;

        // Limit max speed to avoid erratic movement
        double maxSpeed = 5;
        double speed = Math.sqrt(x_velocity * x_velocity + y_velocity * y_velocity);
        if (speed > maxSpeed) {
            x_velocity = (x_velocity / speed) * maxSpeed;
            y_velocity = (y_velocity / speed) * maxSpeed;
        }
    }
    private double distance(int x1, int y1, int x2, int y2) {
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }
    public void alignment(ArrayList<Point> averageVelocity) {
        if (averageVelocity.isEmpty()) return; // Prevent crash
        x_velocity+=averageVelocity.get(0).x/3;
        y_velocity+=averageVelocity.get(0).y/3;
    }
    public void separation(int pos_x, int pos_y) {
        double dx = this.x - pos_x;
        double dy = this.y - pos_y;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > 0 && distance < 150) { // Only separate if too close (e.g., < 100 pixels)
            double repulsionStrength = 130 / distance; // Stronger repulsion when closer

            // Normalize direction and scale it by repulsion strength
            x_velocity += (dx / distance) * repulsionStrength;
            y_velocity += (dy / distance) * repulsionStrength;

            // Limit max speed to avoid erratic movement
            double maxSpeed = 5;
            double speed = Math.sqrt(x_velocity * x_velocity + y_velocity * y_velocity);
            if (speed > maxSpeed) {
                x_velocity = (x_velocity / speed) * maxSpeed;
                y_velocity = (y_velocity / speed) * maxSpeed;
            }
        }
    }

}

