package com.kandarp.snake;

import java.util.ArrayList;
import java.util.Random;

import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

public class SnakeView extends TileView {

	private int mMode = READY;
	public static final int PAUSE = 0;
	public static final int READY = 1;
	public static final int RUNNING = 2;
	public static final int LOSE = 3;

	/**
	 * Current direction the snake is headed.
	 */
	private int mDirection = NORTH;
	private int mNextDirection = NORTH;
	private static final int NORTH = 1;
	private static final int SOUTH = 2;
	private static final int EAST = 3;
	private static final int WEST = 4;

	public int mBoardSize = BS_NORMAL;
	public static final int BS_BIG = 0;
	public static final int BS_NORMAL = 1;
	public static final int BS_SMALL = 2;
	private int tileSizes[] = { 12, 24, 36 };

	/**
	 * Labels for the drawables that will be loaded into the TileView class
	 */
	private static final int BODY_TILE = 1;
	private static final int FOOD_TILE = 2;
	private static final int GREENFOOD_TILE = 3;
	private static final int REDFOOD_TILE = 4;
	private static final int WALL_TILE = 5;
	private static final int HEAD_TILE = 6;
	private static final int HEAD2_TILE = 7;
	private static final int HEADEAT_TILE = 8;
	private static final int HEADBAD_TILE = 9;

	private boolean mDrawHead2 = false;
	private boolean mDrawHeadEat = false;

	/**
	 * mScore: used to track the number of apples captured mMoveDelay: number of
	 * milliseconds between snake movements. This will decrease as apples are
	 * captured.
	 */
	private long mScore = 0;
	private long mMoveDelay = 350;
	private int numcollision = 0;
	public boolean firstTime = true;
	public boolean noSmallSize = false;
	private long mLastMove;
	private TextView mStatusText;
	private TextView mScoreText;
	public boolean mUseWalls = false;

	private ArrayList<Coordinate> mSnakeTrail = new ArrayList<Coordinate>();
	private ArrayList<Coordinate> mAppleList = new ArrayList<Coordinate>();
	private Coordinate mRedApple = new Coordinate(1, 1);
	private boolean mActiveRedApple = false;
	private Coordinate mGreenApple = new Coordinate(1, 1);
	private boolean mActiveGreenApple = false;

	private static final Random RNG = new Random();
	private Vibrator mVibrator;

	/**
	 * Create a simple handler that we can use to cause animation to happen. We
	 * set ourselves as a target and we can use the sleep() function to cause an
	 * update/invalidate to occur at a later date.
	 */
	private RefreshHandler mRedrawHandler = new RefreshHandler();

	class RefreshHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			SnakeView.this.update();
			SnakeView.this.invalidate();
		}

		public void sleep(long delayMillis) {
			this.removeMessages(0);
			sendMessageDelayed(obtainMessage(0), delayMillis);
		}
	};

	public SnakeView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initSnakeView();
	}

	public SnakeView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initSnakeView();
	}

	public void setTileSizes(int w, int h) {
		if (h < 300 || w < 300)
			noSmallSize = true; // qvga device
		if (h < 400 || w < 400) { // qvga & hvga devices
			tileSizes[0] = 12;
			tileSizes[1] = 24;
			tileSizes[2] = 36;
		}
	}

	private void initSnakeView() {
		setFocusable(true);

		Resources r = this.getContext().getResources();

		resetBitmapTiles(10);
		loadBitmapTile(BODY_TILE, r.getDrawable(R.drawable.bodytile));
		loadBitmapTile(FOOD_TILE, r.getDrawable(R.drawable.appletile));
		loadBitmapTile(GREENFOOD_TILE, r.getDrawable(R.drawable.peppertile));
		loadBitmapTile(REDFOOD_TILE, r.getDrawable(R.drawable.redpeppertile));
		loadBitmapTile(WALL_TILE, r.getDrawable(R.drawable.walltile2));
		loadBitmapTile(HEAD_TILE, r.getDrawable(R.drawable.headtile));
		loadBitmapTile(HEAD2_TILE, r.getDrawable(R.drawable.head2tile));
		loadBitmapTile(HEADEAT_TILE, r.getDrawable(R.drawable.headeattile));
		loadBitmapTile(HEADBAD_TILE, r.getDrawable(R.drawable.headbadtile));

	}

	public void initNewGame() {
		mSnakeTrail.clear();
		mAppleList.clear();

		// For now we're just going to load up a short default eastbound snake
		// that's just turned north
		mSnakeTrail.add(new Coordinate(7, 7));
		mSnakeTrail.add(new Coordinate(6, 7));
		mSnakeTrail.add(new Coordinate(5, 7));
		mSnakeTrail.add(new Coordinate(4, 7));
		mSnakeTrail.add(new Coordinate(3, 7));
		mSnakeTrail.add(new Coordinate(2, 7));
		mNextDirection = NORTH;

		// Two apples to start with
		mActiveGreenApple = false;
		mActiveRedApple = false;
		addRandomApple();
		addRandomApple();

		mScore = 0;
	}

	public void setVibrator(Vibrator v) {
		mVibrator = v;
	}

	/*
	 * handles key events in the game. Update the direction our snake is
	 * traveling based on the DPAD. Ignore events that would cause the snake to
	 * immediately turn back on itself.
	 * 
	 * (non-Javadoc)
	 * 
	 * @see android.view.View#onKeyDown(int, android.os.KeyEvent)
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent msg) {

		if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
			if (mMode == READY) {
				// no initNewGame() because we have initiated it in setMode
				setMode(RUNNING);
				return (true);
			}

			if (mMode == LOSE) {
				initNewGame();
				setMode(RUNNING);
				return (true);
			}

			if (mMode == PAUSE) {
				setMode(RUNNING);
				return (true);
			}

			setMode(PAUSE);
			return (true);
		}

		if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
			if (mDirection != SOUTH) {
				mNextDirection = NORTH;
			}
			return (true);
		}

		if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
			if (mDirection != NORTH) {
				mNextDirection = SOUTH;
			}
			return (true);
		}

		if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
			if (mDirection != EAST) {
				mNextDirection = WEST;
			}
			return (true);
		}

		if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
			if (mDirection != WEST) {
				mNextDirection = EAST;
			}
			return (true);
		}

		return super.onKeyDown(keyCode, msg);
	}

	private float savedX;
	private float savedY;
	private int savedMode;

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			savedX = event.getX();
			savedY = event.getY();
			savedMode = mMode;
			return (true);
		}

		if (event.getAction() != MotionEvent.ACTION_UP)
			return (true);

		if (savedMode != mMode)
			return (true);

		float distX = savedX - event.getX();
		float distY = savedY - event.getY();
		float adistX = Math.abs(distX);
		float adistY = Math.abs(distY);

		if (adistX + adistY >= 10) {
			if (adistX > adistY) {
				if (distX > 0) {
					if (mDirection != EAST)
						mNextDirection = WEST;
				} else {
					if (mDirection != WEST)
						mNextDirection = EAST;
				}
			} else {
				if (distY > 0) {
					if (mDirection != SOUTH)
						mNextDirection = NORTH;
				} else {
					if (mDirection != NORTH)
						mNextDirection = SOUTH;
				}
			}
		}

		if (mMode == READY) {
			// no initNewGame() because we have initiated it in setMode
			setMode(RUNNING);
			// return (true);
		} else if (mMode == LOSE) {
			initNewGame();
			setMode(RUNNING);
			// return (true);
		} else if (mMode == PAUSE) {
			setMode(RUNNING);
			// return (true);
		} else if (mMode == RUNNING) {
			if (adistX + adistY < 10) {
				setMode(PAUSE);
				// return (true);
			}
		}

		return (true);
	}

	public void setTextView(TextView newView) {
		mStatusText = newView;
	}

	public void setScoreView(TextView newView) {
		mScoreText = newView;
	}

	/**
	 * Updates the current mode of the application (RUNNING or PAUSED or the
	 * like) as well as sets the visibility of textview for notification
	 * 
	 * @param newMode
	 */
	public void setMode(int newMode) {
		int oldMode = mMode;
		mMode = newMode;
		updateScore();

		if (newMode == RUNNING & oldMode != RUNNING) {
			mStatusText.setVisibility(View.INVISIBLE);
			update();
			invalidate();
			return;
		}

		Resources res = getContext().getResources();
		CharSequence str = "";
		if (newMode == PAUSE) {
			str = res.getText(R.string.mode_pause);
		}
		if (newMode == READY) {
			if (!firstTime) {
				initNewGame();
				updateElements();
				invalidate();
			}
			str = res.getText(R.string.mode_ready);
		}
		if (newMode == LOSE) {
			if (mScore > 0) {
				str = res.getString(R.string.mode_lose_prefix_cr) + mScore
						+ res.getString(R.string.mode_lose_suffix);
			} else {
				str = res.getString(R.string.mode_lose_prefix) + mScore
						+ res.getString(R.string.mode_lose_suffix);
			}
		}

		mStatusText.setText(str);
		mStatusText.setVisibility(View.VISIBLE);
	}

	public int getMode() {
		return mMode;
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		if (firstTime) {
			if (mMode == READY) {
				initNewGame();
				updateElements();
			} else if (mMode == PAUSE) {
				updateElements();
			}
		}
		firstTime = false;
	}

	/**
	 * Selects a random location within the garden that is not currently covered
	 * by the snake. Currently _could_ go into an infinite loop if the snake
	 * currently fills the garden, but we'll leave discovery of this prize to a
	 * truly excellent snake-player.
	 * 
	 */
	private Coordinate findFreeCoordinate() {
		Coordinate newCoord = null;
		boolean found = false;
		while (!found) {
			// Choose a new location for our apple
			int newX = 1 + RNG.nextInt(mXTileCount - 2);
			int newY = 1 + RNG.nextInt(mYTileCount - 2);
			newCoord = new Coordinate(newX, newY);

			// Make sure it's not already under the snake
			boolean collision = false;
			int snakelength = mSnakeTrail.size();
			for (int index = 0; index < snakelength; index++) {
				if (mSnakeTrail.get(index).equals(newCoord)) {
					collision = true;
					numcollision++;
				}
			}
			int applelength = mAppleList.size();
			for (int index = 0; index < applelength; index++) {
				if (mAppleList.get(index).equals(newCoord)) {
					collision = true;
					numcollision++;
				}
			}
			if (mActiveRedApple && mRedApple.equals(newCoord)) {
				collision = true;
				numcollision++;
			}
			if (mActiveGreenApple && mGreenApple.equals(newCoord)) {
				collision = true;
				numcollision++;
			}
			// if we're here and there's been no collision, then we have
			// a good location for an apple. Otherwise, we'll circle back
			// and try again
			found = !collision;
		}
		return newCoord;
	}

	private void addRandomApple() {
		Coordinate newCoord = null;

		newCoord = findFreeCoordinate();
		mAppleList.add(newCoord);
	}

	private void addRedApple() {
		mRedApple = findFreeCoordinate();
		mActiveRedApple = true;
	}

	private void addGreenApple() {
		mGreenApple = findFreeCoordinate();
		mActiveGreenApple = true;
	}

	/**
	 * Handles the basic update loop, checking to see if we are in the running
	 * state, determining if a move should be made, updating the snake's
	 * location.
	 */
	public void update() {
		if (mMode == RUNNING) {
			long now = System.currentTimeMillis();

			if (now - mLastMove >= mMoveDelay) {
				updateElements();
				mLastMove = now;
			}
			mRedrawHandler.sleep(mMoveDelay);
		}
	}

	public void updateElements() {
		clearTiles();
		updateWalls();
		updateSnake();
		updateApples();
	}

	/**
	 * Draws some walls.
	 * 
	 */
	private void updateWalls() {
		if (!mUseWalls)
			return;
		for (int x = 0; x < mXTileCount; x++) {
			setTile(WALL_TILE, x, 0);
			setTile(WALL_TILE, x, mYTileCount - 1);
		}
		for (int y = 1; y < mYTileCount - 1; y++) {
			setTile(WALL_TILE, 0, y);
			setTile(WALL_TILE, mXTileCount - 1, y);
		}
	}

	/**
	 * Draws some apples.
	 * 
	 */
	private void updateApples() {
		for (Coordinate c : mAppleList) {
			setTile(FOOD_TILE, c.x, c.y);
		}
		if (mActiveRedApple) {
			setTile(REDFOOD_TILE, mRedApple.x, mRedApple.y);
		}
		if (mActiveGreenApple) {
			setTile(GREENFOOD_TILE, mGreenApple.x, mGreenApple.y);
		}
	}

	/**
	 * Figure out which way the snake is going, see if he's run into anything
	 * (the walls, himself, or an apple). If he's not going to die, we then add
	 * to the front and subtract from the rear in order to simulate motion. If
	 * we want to grow him, we don't subtract from the rear.
	 * 
	 */
	private void updateSnake() {
		boolean growSnake = false;
		boolean mustAddRandomApple = false;
		boolean mustAddGreenApple = false;

		// grab the snake by the head
		Coordinate head = mSnakeTrail.get(0);
		Coordinate newHead = new Coordinate(1, 1);

		mDirection = mNextDirection;

		switch (mDirection) {
		case EAST: {
			newHead = new Coordinate(head.x + 1, head.y);
			break;
		}
		case WEST: {
			newHead = new Coordinate(head.x - 1, head.y);
			break;
		}
		case NORTH: {
			newHead = new Coordinate(head.x, head.y - 1);
			break;
		}
		case SOUTH: {
			newHead = new Coordinate(head.x, head.y + 1);
			break;
		}
		}

		// Collision detection with walls if we are suing them or adjust head
		// position
		if (mUseWalls) {
			if ((newHead.x < 1) || (newHead.y < 1)
					|| (newHead.x > mXTileCount - 2)
					|| (newHead.y > mYTileCount - 2)) {
				mVibrator.vibrate(300);
				setMode(LOSE);
				drawSnakeBad();
				return;
			}
		} else {
			if (newHead.x < 0)
				newHead.x = mXTileCount - 1;
			if (newHead.x > mXTileCount - 1)
				newHead.x = 0;
			if (newHead.y < 0)
				newHead.y = mYTileCount - 1;
			if (newHead.y > mYTileCount - 1)
				newHead.y = 0;
		}

		// Look for collisions with itself
		int snakelength = mSnakeTrail.size();
		for (int snakeindex = 0; snakeindex < snakelength; snakeindex++) {
			Coordinate c = mSnakeTrail.get(snakeindex);
			if (c.equals(newHead)) {
				mVibrator.vibrate(400);
				setMode(LOSE);
				drawSnakeBad();
				return;
			}
		}

		// Look for apples
		int applecount = mAppleList.size();
		for (int appleindex = 0; appleindex < applecount; appleindex++) {
			Coordinate c = mAppleList.get(appleindex);
			if (c.equals(newHead)) {
				mAppleList.remove(c);
				mustAddRandomApple = true;
				mScore++;
				mMoveDelay *= 0.95;
				updateScore();
				mVibrator.vibrate(100);
				mDrawHeadEat = true;
				growSnake = true;
				break;
			}
		}
		// Look for red apple
		if (mActiveRedApple && mRedApple.equals(newHead)) {
			mActiveRedApple = false;
			mScore += 2;
			mMoveDelay = 400;
			updateScore();
			mVibrator.vibrate(100);
			mDrawHeadEat = true;
			mustAddGreenApple = true;
		}
		// Look for green apple
		if (mActiveGreenApple && mGreenApple.equals(newHead)) {
			mActiveGreenApple = false;
			mScore += 3;
			mMoveDelay = 200;
			updateScore();
			mVibrator.vibrate(100);
			mDrawHeadEat = true;
			growSnake = true;
		}

		// push a new head onto the ArrayList and pull off the tail
		mSnakeTrail.add(0, newHead);
		// except if we want the snake to grow
		if (!growSnake) {
			mSnakeTrail.remove(mSnakeTrail.size() - 1);
		}

		int index = 0;
		for (Coordinate c : mSnakeTrail) {
			if (index == 0) {
				if (mDrawHeadEat)
					setTile(HEADEAT_TILE, c.x, c.y);
				else if (mDrawHead2)
					setTile(HEAD2_TILE, c.x, c.y);
				else
					setTile(HEAD_TILE, c.x, c.y);
				mDrawHeadEat = false;
				mDrawHead2 = !mDrawHead2;
			} else {
				setTile(BODY_TILE, c.x, c.y);
			}
			index++;
		}

		if (mustAddRandomApple)
			addRandomApple();
		if (mustAddGreenApple)
			addGreenApple();

		int limit = 90;
		if (mMoveDelay < limit) {
			if (!mActiveRedApple) {
				addRedApple();
				mActiveGreenApple = false;
			}
		} else {
			if (mActiveRedApple) {
				mActiveRedApple = false;
			}
		}
	}

	void drawSnakeBad() {
		int index = 0;
		for (Coordinate c : mSnakeTrail) {
			if (index == 0) {
				setTile(HEADBAD_TILE, c.x, c.y);
			} else {
				setTile(BODY_TILE, c.x, c.y);
			}
			index++;
		}
	}

	/**
     * 
     */
	private void updateScore() {
		CharSequence str = "";
		Resources res = getContext().getResources();

		str = " " + res.getString(R.string.score) + " " + mScore;
		mScoreText.setText(str);

	}

	/**
	 * Simple class containing two integer values and a comparison function.
	 * There's probably something I should use instead, but this was quick and
	 * easy to build.
	 * 
	 */
	private class Coordinate {
		public int x;
		public int y;

		public Coordinate(int newX, int newY) {
			x = newX;
			y = newY;
		}

		public boolean equals(Coordinate other) {
			if (x == other.x && y == other.y) {
				return true;
			}
			return false;
		}

		@Override
		public String toString() {
			return "Coordinate: [" + x + "," + y + "]";
		}
	}

}
