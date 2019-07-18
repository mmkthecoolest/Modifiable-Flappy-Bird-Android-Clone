package com.mygdx.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Rectangle;

import java.util.Random;

//WARNING: ONLY USE POSITIVE NUMBERS FOR VALUES. NEGATIVE VALUES MAY BREAK THE GAME.
public class FlappyBirdClone extends ApplicationAdapter {
    private SpriteBatch batch;//a batch of sprites
    private Texture background;//background sprite
    private Texture ground;//ground texture
    private float[] groundX;//array of ground texture x-coordinates
    private Texture gameOver;//game over texture
    private ShapeRenderer shapeRenderer;//shaperenderer

    private Texture[] birds;//array of textures for the bird
    private boolean flapState = true;//flap state to determine which bird texture to use
    private int delay = 0;//the delay counter to change the flap state
    private float birdY;//y-position of the bird
    private float downwardVelocity = 0;//downward velocity of the bird
    private Circle birdCircle;//collision shape for the bird

    private Texture upperPipe;//texture for upper pipe
    private Texture lowerPipe;//texture for lower pipe
    private float[] pipeX;//array of pipe x-coordinates
    private final float pipesDistance = 700;//distance between each pipe
    private final float pipeStartDistance = 400;//distance before pipes start appearing onscreen
    private Rectangle[] topRectangles;//collision shapes used for upper pipes
    private Rectangle[] bottomRectangles;//collision shapes used for lower pipes
    private final int numberOfPipes = 4;//number of pipes per render
    private Random rand;
    private float[] pipeOffset;//offset of y-coordinates of lower pipes
    private final int offsetMax = 800;//the maximum value of each pipe's y-offset. Decrease this if upper pipes do not appear connected to the top of the screen

    private enum GameState{
        GAME_WAITING,//wait for user to start the game
        GAME_START,//game is active
        GAME_OVER//game is over
    }

    private GameState gameState = GameState.GAME_WAITING;
    private int score = 0;//current score
    private Preferences preferences;//used to load highscore
    private int highScore = 0;//highscore variable
    private BitmapFont font;

    //function to generate y-offset for each pipe. Number is random from 0 to offsetMax
    private float generateOffset() {
        return rand.nextFloat() * offsetMax;
    }

    @Override
    public void create() {
        final int fontSize = 5;//font size

        batch = new SpriteBatch();
        background = new Texture("bg.png");
        ground = new Texture("ground.png");
        gameOver = new Texture("gameover.png");
        birds = new Texture[2];
        birds[0] = new Texture("bird.png");
        birds[1] = new Texture("bird2.png");
        upperPipe = new Texture("toptube.png");
        lowerPipe = new Texture("bottomtube.png");

        //place bird in the center of the screen
        birdY = (float) ((Gdx.graphics.getHeight() - birds[0].getHeight()) / 2.0);

        rand = new Random();
        pipeOffset = new float[numberOfPipes];
        pipeX = new float[numberOfPipes];
        for (int i = 0; i < numberOfPipes; i++) {
            pipeOffset[i] = generateOffset();
            pipeX[i] = pipeStartDistance + Gdx.graphics.getWidth() + (i * pipesDistance);
        }
        groundX = new float[2];
        groundX[0] = 0;
        groundX[1] = Gdx.graphics.getWidth();

        birdCircle = new Circle();
        topRectangles = new Rectangle[numberOfPipes];
        bottomRectangles = new Rectangle[numberOfPipes];

        for (int i = 0; i < numberOfPipes; i++) {
            topRectangles[i] = new Rectangle();
            bottomRectangles[i] = new Rectangle();
        }
        shapeRenderer = new ShapeRenderer();

        font = new BitmapFont();
        font.setColor(Color.WHITE);
        font.getData().setScale(fontSize);

        //highscore is originally 0. If a value is saved, that one is loaded instead.
        preferences = Gdx.app.getPreferences("preferences");
        if(preferences.contains("highScore")){
            highScore = preferences.getInteger("highScore");
        }
    }

    //render method gets run again and again
    @Override
    public void render() {
        final float gravity = 1.5f;//rate which downward velocity changes per render. Or just gravity
        final float shoveStrength = 25;//strength of the jump from each tap
        final float pipeGapHeight = 400;//gap between upper and lower pipes
        final float pipeSpeed = 10;//scrolling speed in a nutshell
        final int flapDelay = 5;//change flap state every 'flapDelay' renders
        final boolean collisionAction = true;//variable to enable or disable collision code
        final boolean collisionColor = false;//variable to enable or disable collision colors
        final int scoreYOffset = Gdx.graphics.getHeight() - 200;
        final int scoreXOffset = 200;//^these two variables are just determining the place of the text
        final float offsetGround = 100;//offset from ground to lowest possible distance of top of lowerPipe.
        //set it lower to lower the ground and vice versa

        //Gdx.app.log("Random number", String.valueOf(pipeOffset));

        if (Gdx.input.justTouched()) {
            //Gdx.app.log("Status", "touched");

            if (gameState != GameState.GAME_OVER) {
                downwardVelocity = -shoveStrength;
                gameState = GameState.GAME_START;
            } else {
                gameState = GameState.GAME_WAITING;
            }
        }

        int index;
        if (flapState && gameState != GameState.GAME_OVER) {
            index = 1;
        } else index = 0;

        if (gameState == GameState.GAME_START) {
            downwardVelocity += gravity;
            birdY -= downwardVelocity;
            if (birdY < lowerPipe.getHeight() - offsetMax - offsetGround) {
                birdY = lowerPipe.getHeight() - offsetMax - offsetGround;
                if(collisionAction) {
                    gameState = GameState.GAME_OVER;
                }
            } else if (birdY > Gdx.graphics.getHeight() - (birds[index].getHeight())) {
                birdY = Gdx.graphics.getHeight() - (birds[index].getHeight());
                downwardVelocity = 0;
            }

            for (int i = 0; i < numberOfPipes; i++) {
                pipeX[i] -= pipeSpeed;
            }

            for(int i = 0; i < 2; i++){
                groundX[i] -= pipeSpeed;
                if(groundX[i] <= -Gdx.graphics.getWidth()){
                    groundX[i] = Gdx.graphics.getWidth();
                }
            }

            if (pipeX[0] <= -lowerPipe.getWidth()) {
                pipeX[0] = pipeX[numberOfPipes - 1] + pipesDistance;
                pipeOffset[0] = generateOffset();
            }
            for (int i = 0; i < numberOfPipes - 1; i++) {
                if (pipeX[i + 1] <= -lowerPipe.getWidth()) {
                    pipeX[i + 1] = pipeX[i] + pipesDistance;
                    pipeOffset[i + 1] = generateOffset();
                }
            }


        } else if (gameState == GameState.GAME_WAITING) {
            for (int i = 0; i < numberOfPipes; i++) {
                pipeOffset[i] = generateOffset();
                pipeX[i] = pipeStartDistance + Gdx.graphics.getWidth() + (i * pipesDistance);
            }

            birdY = (float) ((Gdx.graphics.getHeight() - birds[index].getHeight()) / 2.0);
            downwardVelocity = 0;
            score = 0;
        }

        batch.begin();

        batch.draw(background, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        //draw background from origin to width and height of screen
        float birdXPosition = (float) ((Gdx.graphics.getWidth() - birds[index].getWidth()) / 2.0);
        //last two parameters are not needed if we just want the image at its original size
        batch.draw(birds[index], birdXPosition, birdY);

        for (int i = 0; i < numberOfPipes; i++) {
            batch.draw(upperPipe, pipeX[i], lowerPipe.getHeight() + pipeGapHeight - pipeOffset[i]);
            batch.draw(lowerPipe, pipeX[i], 0 - pipeOffset[i]);
        }

        for(int i = 0; i < 2; i++){
            batch.draw(ground, groundX[i], 0, Gdx.graphics.getWidth(),
                    lowerPipe.getHeight() - offsetMax - offsetGround);
        }



        if(gameState == GameState.GAME_OVER) {
            float gameOverX = (Gdx.graphics.getWidth() - gameOver.getWidth()) / 2.0f;
            float gameOverY = (Gdx.graphics.getHeight() - gameOver.getHeight()) / 2.0f;
            batch.draw(gameOver, gameOverX, gameOverY);

            if(score > highScore && collisionAction){
                highScore = score;
                preferences.putInteger("highScore", highScore);
                preferences.flush();
            }

            font.draw(batch,
                    "Current score: " + String.valueOf(score) + "\nHigh score: " + String.valueOf(highScore),
                    scoreXOffset, scoreYOffset);
        } else if (gameState == GameState.GAME_START){
            font.draw(batch, String.valueOf(score), scoreXOffset, scoreYOffset);
        } else {
            font.draw(batch, "Tap to start", scoreXOffset, scoreYOffset);
        }
        delay++;

        if (delay == flapDelay) {
            flapState = !(flapState);
            delay = 0;
        }

        batch.end();

        birdCircle.set(Gdx.graphics.getWidth() / 2.0f, birdY + (birds[index].getHeight() / 2.0f),
                birds[index].getHeight() / 2.0f);

        for (int i = 0; i < numberOfPipes; i++) {
            bottomRectangles[i].set(pipeX[i], 0 - pipeOffset[i], lowerPipe.getWidth(), lowerPipe.getHeight());
            topRectangles[i].set(pipeX[i], lowerPipe.getHeight() + pipeGapHeight - pipeOffset[i],
                    upperPipe.getWidth(), upperPipe.getHeight());
        }

        if (collisionColor) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(Color.RED);
            shapeRenderer.circle(birdCircle.x, birdCircle.y, birdCircle.radius);

            shapeRenderer.setColor(Color.BLUE);
            for (int i = 0; i < numberOfPipes; i++) {
                shapeRenderer.rect(bottomRectangles[i].x, bottomRectangles[i].y, bottomRectangles[i].width,
                        bottomRectangles[i].height);

                shapeRenderer.rect(topRectangles[i].x, topRectangles[i].y, topRectangles[i].width,
                        topRectangles[i].height);
            }

            shapeRenderer.end();
        }

        if (collisionAction) {

            for (int i = 0; i < numberOfPipes; i++) {
                if (Intersector.overlaps(birdCircle, topRectangles[i]) ||
                        Intersector.overlaps(birdCircle, bottomRectangles[i])) {
                    gameState = GameState.GAME_OVER;
                }
            }
        }

        for(int i = 0; i < numberOfPipes; i++){
            int intBirdXPosition = (int) birdXPosition;
            int intPipeX = (int) (pipeX[i] + upperPipe.getWidth());
            //Gdx.app.log("PipeX position", Integer.toString(intPipeX));

            if(intBirdXPosition > intPipeX && intBirdXPosition - intPipeX < pipesDistance
                && intBirdXPosition - intPipeX < pipeSpeed){
               // Gdx.app.log("Score", Integer.toString(++score));
                score++;
            }
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        background.dispose();
        birds[0].dispose();
        birds[1].dispose();
        upperPipe.dispose();
        lowerPipe.dispose();
        font.dispose();

        shapeRenderer.dispose();
    }
}
