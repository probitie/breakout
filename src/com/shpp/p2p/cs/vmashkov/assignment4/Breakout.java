package com.shpp.p2p.cs.vmashkov.assignment4;

import acm.graphics.*;
import acm.util.RandomGenerator;
import com.shpp.cs.a.graphics.WindowProgram;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;


/**
 * The Breakout Game
 */


public class Breakout extends WindowProgram
{
    // Width and height of application window in pixels
    public static final int APPLICATION_WIDTH = 400;
    public static final int APPLICATION_HEIGHT = 600;

    /**
     * Dimensions of game board (usually the same)
     */
    private static final int WIDTH = APPLICATION_WIDTH;
    private static final int HEIGHT = APPLICATION_HEIGHT;
    // Dimensions of the paddle
    private static final int PADDLE_WIDTH = 60;
    private static final int PADDLE_HEIGHT = 10;

    // Offset of the paddle up from the bottom
    private static final int PADDLE_Y_OFFSET = 30;

    // Number of bricks per row
    private static final int NBRICKS_PER_ROW = 6;

    // Number of rows of bricks
    private static final int NBRICK_ROWS = 10;

    // Separation between bricks
    private static final int BRICK_SEP = 0;

    // Width of a brick
    private static final int BRICK_WIDTH = (WIDTH) / NBRICKS_PER_ROW;

    // Height of a brick
    private static final int BRICK_HEIGHT = 8;

    // brick internal padding for handling Y reflection
    private static final int INTERNAL_PADDING = 5; // px

    // Diameter of the ball in pixels
    private static final int DIAMETER = 20;

    // Offset of the top brick row from the top
    private static final int BRICK_Y_OFFSET = 70;

    // How many balls does the user have
    private static final int ATTEMPTS = 3;

    // speed of the game (not of the ball only)
    private final int PAUSE_TIME_MS = 5;
    private static final Color[] COLORS = new Color[]{Color.RED,
            Color.ORANGE,
            Color.YELLOW,
            Color.GREEN,
            Color.CYAN};

    private static final Color PADDLE_COLOR = Color.BLUE;

    GRect paddle;
    GOval ball;

    // while it false - program will not perform any actions
    boolean canPlayRound;

    // speed of the ball in both axles
    private double vx, vy;

    // bricks on the game board
    ArrayList<GRect> bricks;

    // actual paddle Y position (calculates after creating a window)
    double paddleY;

    /**
     * initialises and starts the game
     */
    public void run()
    {

        // set up paddle Y position -
        // PADDLE_Y_OFFSET is responding for padding from window bottom
        paddleY = getHeight() - PADDLE_Y_OFFSET;

        // waiting for user click before start the game
        canPlayRound = false;

        // adding a paddle to the game board
        drawPaddle();

        // adding bricks to game board and initialising bricks array (object field)
        drawBricks();

        // creating a ball
        drawBall();

        // setting up ball speed and direction (but it does not move now)
        setRandomSpeedAndDirection();

        // connect a user mouse to the game
        addMouseListeners();

        // and finally start our game
        startGame();

    }

    /**
     * launches the game and outputs "U're won/lose" after the end
     * <p>
     * Win/ Lose events are built by exceptions
     */
    private void startGame()
    {
        try
        {
            startRoundLoops(); // play the game
        } catch (GameWinException e)
        {
            alertGameWin();
        } catch (GameOverException e)
        {
            alertGameOver();
        }
    }

    /**
     * Starts game loop and repeats it *ATTEMPTS* times
     *
     * @throws GameOverException if user lose
     * @throws GameWinException  if user win
     */
    private void startRoundLoops() throws GameOverException, GameWinException
    {
        // attempts to win the game
        int attemptsLeft = ATTEMPTS;

        while (true)
        {
            // if you can not play the round - game freezes till it changes
            if (ball != null && canPlayRound)
            {
                try
                {

                    // if the ball touches bottom border - attempt will be failed
                    moveBall();
                } catch (AttemptOverException e)
                {

                    // wait till user click to continue
                    canPlayRound = false;

                    // game start is also an attempt
                    attemptsLeft--;

                    if (attemptsLeft <= 0)
                        throw new GameOverException();
                }

                // handle collisions
                GObject collisionObject = getCollisionObjectForBall();
                if (collisionObject != null)
                {
                    handleCollision(collisionObject);
                }
            } else
            {
                // if user can not play the round - put ball to the center
                if (ball != null)
                {
                    double centerX = getCenterCoordinate(DIAMETER, getWidth());
                    double centerY = getCenterCoordinate(DIAMETER, getHeight());

                    ball.setLocation(centerX, centerY);
                }
            }

            // check if user won
            checkIfBricksLeft();

            pause(PAUSE_TIME_MS);
        }
    }


    /**
     * creates paddle object and adds it to this/paddle field
     */
    private void drawPaddle()
    {
        GRect paddle = new GRect(getWidth() / 2.0 - PADDLE_WIDTH / 2.0,
                paddleY, PADDLE_WIDTH, PADDLE_HEIGHT);
        paddle.setFilled(true);
        paddle.setFillColor(PADDLE_COLOR);

        // add to the object field and to the canvas
        this.paddle = paddle;
        add(paddle);
    }

    /**
     * draws rows of the bricks on the game board
     */
    private void drawBricks()
    {
        // init the object field
        bricks = new ArrayList<>();

        // add brick lines with different colors
        for (int i = 0; i < NBRICK_ROWS; i++)
        {
            // offset for each row
            double offsetY = BRICK_Y_OFFSET + i * (BRICK_HEIGHT + BRICK_SEP);

            // distribute color to all rows
            Color color = getColorForRow(i);
            drawBricksLine(offsetY, color);
        }

    }


    /**
     * draws a centered By X axis row of bricks
     *
     * @param offsetY where to start build the row
     * @param color   color of the row bricks
     */
    private void drawBricksLine(double offsetY, Color color)
    {
        for (int i = 0; i < NBRICKS_PER_ROW; i++)
        {
            double padding = getCenterCoordinate(NBRICKS_PER_ROW * (BRICK_WIDTH + BRICK_SEP) - BRICK_SEP, getWidth());

            // x offset for each brick
            double offsetX = padding + i * (BRICK_WIDTH + BRICK_SEP);

            drawBrick(offsetX, offsetY, color);
        }
    }


    /**
     * compares row index to color amount
     * Example: 5 colors, 9 rows - each 2 row will have the same color[0-3],
     * and last row will have color [4]
     *
     * @param rowNumber number of the brick row
     * @return color for the current row
     */
    private Color getColorForRow(int rowNumber)
    {
        return COLORS[(rowNumber / 2) % COLORS.length];
    }

    /**
     * @param x     X coordinate of the brick
     * @param y     Y coordinate of the brick
     * @param color brick fill color
     */
    private void drawBrick(double x, double y, Color color)
    {
        GRect brick = new GRect(x, y, BRICK_WIDTH, BRICK_HEIGHT);
        brick.setFilled(true);
        brick.setColor(Color.BLACK);
        brick.setFillColor(color);

        // add to brick field
        bricks.add(brick);
        add(brick);
    }

    /**
     * draws a ball on the game board
     */
    private void drawBall()
    {
        // draw at the center
        double centerX = getCenterCoordinate(DIAMETER, getWidth());
        double centerY = getCenterCoordinate(DIAMETER, getHeight());

        GOval oval = new GOval(centerX, centerY, DIAMETER, DIAMETER);
        oval.setFilled(true);

        // also set up "ball" field
        ball = oval;
        add(oval);
    }

    /**
     * sets for fields vx and vy random values that together means speed and direction
     */
    private void setRandomSpeedAndDirection()
    {
        RandomGenerator rgen = RandomGenerator.getInstance();

        // I think there is no sense move this to discrete method
        // but anyone must keep in mind that
        // this and five-rows-below pieces of code are responsible for the same
        vx = rgen.nextDouble(1.0, 3.0);
        if (rgen.nextBoolean(0.5))
            vx = -vx;

        vy = rgen.nextDouble(1.0, 3.0);
    }


    /**
     * checks "User win" condition
     *
     * @throws GameWinException if no bricks left on the game board - it means user won
     */
    private void checkIfBricksLeft() throws GameWinException
    {
        if (bricks.size() == 0)
            throw new GameWinException();
    }

    /**
     * finds collision object for the ball
     *
     * @return object that the ball collided with
     */
    private GObject getCollisionObjectForBall()
    {
        ArrayList<GPoint> vertexes = getBallVertices();

        return findObject(vertexes);

    }

    /**
     * @return 4 ball "hitbox" vertexes - it is needed for handle collision
     */
    private ArrayList<GPoint> getBallVertices()
    {
        ArrayList<GPoint> vertexes = new ArrayList<>();
        // collision object is bordered by a rectangle where
        // top left vertex
        vertexes.add(new GPoint(ball.getX(), ball.getY()));

        // top right vertex
        vertexes.add(new GPoint(ball.getX() + DIAMETER, ball.getY()));

        // bottom left vertex
        vertexes.add(new GPoint(ball.getX(), ball.getY() + DIAMETER));

        // bottom right vertex
        vertexes.add(new GPoint(ball.getX() + DIAMETER, ball.getY() + DIAMETER));

        return vertexes;
    }

    /**
     * finds first object that has any of these points
     *
     * @param points a collection of the points which the method needs to check
     * @return found object or null
     */
    private GObject findObject(ArrayList<GPoint> points)
    {
        for (GPoint point : points)
        {
            GObject element = getElementAt(point);
            if (element != null)
                return element;
        }
        return null;
    }


    /**
     * process all sort of the collisions
     *
     * @param collider object that collided with the ball
     */
    private void handleCollision(GObject collider)
    {
        // if ball in paddle - do not change direction
        if (isPaddle(collider))
        {
            if (isBallInPaddle())
                moveToPaddleTop();
            horizontalReflection();

        } else if (isBrick(collider))
        {
            // when the ball comes from the left/right
            if (isSideCollisionWithBall(collider))
            {
                // delete brick after a collision
                getGCanvas().remove(collider);
                bricks.remove(collider);
                verticalReflection();

            } else
            {
                // delete brick after a collision
                getGCanvas().remove(collider);
                bricks.remove(collider);
                horizontalReflection();
            }
        }
    }

    /**
     * when ball touches a brick from the left/right side
     *
     * @param brick a brick
     * @return is it collided
     */
    private boolean isSideCollisionWithBall(GObject brick)
    {
        double ballCenterX = ball.getX() + DIAMETER / 2;
        double leftBrickSideX = brick.getX();
        double rightBrickSideX = brick.getX() + brick.getWidth();

        // if ball center x not in brick center x - that`s it
        return !(leftBrickSideX < ballCenterX) || !(ballCenterX < rightBrickSideX); // it is under or above the brick
    }

    /**
     * moves the ball to top of the paddle
     */
    private void moveToPaddleTop()
    {
        ball.setLocation(ball.getX(), paddleY - DIAMETER - 1);
    }

    /**
     * @return check if ball hit the paddle
     */
    private boolean isBallInPaddle()
    {
        // right bottom point
        double ballX = ball.getX() + DIAMETER;
        double ballY = ball.getY() + DIAMETER;

        //     left bottom                                  right bottom
        return getElementAt(ball.getX(), ballY) == paddle || getElementAt(ballX, ballY) == paddle;
    }

    /**
     * responsible for moving the ball and bounce it off the walls
     *
     * @throws AttemptOverException if it hit bottom wall
     */
    private void moveBall() throws AttemptOverException
    {

        // top
        if (ball.getY() <= 0)
            horizontalReflection();

        // bottom
        if (ball.getY() >= getHeight() - DIAMETER)
        {
            throw new AttemptOverException();
        }

        // left or right
        if (ball.getX() <= 0 || ball.getX() >= getWidth() - DIAMETER)
            verticalReflection();


        ball.move(vx, vy);
    }

    /**
     * bounce ball off the vertical wall
     */
    private void verticalReflection()
    {
        vx = -vx;
    }

    /**
     * bounce ball off the horizontal wall
     */
    private void horizontalReflection()
    {
        vy = -vy;
    }


    /**
     * checks if a collision object is in a bricks array
     *
     * @param collider a collision object
     * @return is a collision object in a bricks array
     */
    private boolean isBrick(GObject collider)
    {
        for (GRect brick : bricks)
            if (collider == brick)
                return true;
        return false;
    }

    /**
     * checks if a collision object is a paddle
     *
     * @param collider a collision object
     * @return is a collision object a paddle
     */
    private boolean isPaddle(GObject collider)
    {
        return collider == paddle;
    }

    /**
     * connects user mouse to start the round
     *
     * @param e the event to be processed
     */
    @Override
    public void mouseClicked(MouseEvent e)
    {
        canPlayRound = true;
    }

    /**
     * connects user mouse to move the paddle
     *
     * @param e the event to be processed
     */
    @Override
    public void mouseMoved(MouseEvent e)
    {
        // If there is something to drag at all, go move it
        if (paddle != null)
        {
            // because a paddle can not cross window borders
            if (mouseInAllowedRange(e))
            {
                double newX = e.getX() - PADDLE_WIDTH / 2.0;

                paddle.setLocation(newX, paddleY);
            }

        }
    }

    /**
     * checks whether mouse cursor is in allowed coordinates range
     *
     * @param e current mouse event
     * @return is mouse cursor in allowed coordinates
     */
    private boolean mouseInAllowedRange(MouseEvent e)
    {
        return PADDLE_WIDTH / 2 < e.getX()  // half of the racket before the left side
                &&
                e.getX() < getWidth() - (PADDLE_WIDTH / 2);// half of the racket before the right side
    }

    /**
     * draws label "You win"
     */
    private void alertGameWin()
    {
        alertGameMessage("You win");
    }

    /**
     * draws label "Game over"
     */
    private void alertGameOver()
    {
        alertGameMessage("Game over");
    }

    /**
     * draws big red label on a center of the screen
     */
    private void alertGameMessage(String message)
    {
        GLabel label = new GLabel(message);
        label.setColor(Color.RED);
        label.setFont("Serif-50");
        double centerX = getCenterCoordinate(label.getWidth(), getWidth());
        double centerY = getCenterCoordinate(label.getHeight(), getHeight());
        add(label, centerX, centerY);
    }

    /**
     * gets a coordinate that can be used to center the object
     *
     * @param base object projection on that coordinate axis
     * @param max  max axis range (commonly - window width or height)
     * @return coordinate that can be used to center the object
     */
    static double getCenterCoordinate(double base, double max)
    {
        return (max - base) / 2;
    }
}

