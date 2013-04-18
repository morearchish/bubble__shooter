package com.bubbleshooter;

import java.util.Arrays;
import java.util.Random;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.animation.TranslateAnimation;

public class MainGame extends SurfaceView implements SurfaceHolder.Callback {

	GameLoop mainLoopThread;
	Point displayDims;
	static int ROWS = 15;
	static int COLS = 10;
	static int baseRow = 0; // the matching row with the footer
	static int startEmptyRows ;
	static int footerRatio = 20 ;//15% or screen height
	static int startEmptyRatio = 40; //40% of the screen height
	
	static int footerHeight ;
	static int gunWidth = 200;
	static int drawOffset ; // upper end of footer
	static int DIAM = 65; //bubble diameter
	
	Point bulletLoc;
	Point bulletInitLoc;
	Bitmap redBitmap;
	Bitmap bubblesResized; 
	
	int map[][];
	
	
	static final int ORANGE = 0;
	static final int RED = 1;
	static final int PINK = 2;
	static final int PURPLE = 3;
	static final int GREEN = 4;
	
	static final int supportedColors = 5;
	
	
	// 0 is the next to be fired 
	int nextBubbleColor[] = new int[3];
	Point nextBubbleLoc[] = new Point[3];
	
	//FIXME profile the performance for the background image
	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	void initGame(int rows){
		// adjusting vars
		DIAM = displayDims.x/COLS;
		footerHeight = (int) (displayDims.y*footerRatio/100.0);
		drawOffset = displayDims.y - footerHeight ;
		startEmptyRows  = (int) (displayDims.y*startEmptyRatio/100.0/DIAM);

		
		
		System.out.println(startEmptyRows);
		Random random = new Random();
		
		int sdk = android.os.Build.VERSION.SDK_INT;
		if(sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
			this.setBackgroundDrawable(getResources().getDrawable(R.drawable.background_1));
		} else {
		    this.setBackground(getResources().getDrawable(R.drawable.background_1));
		}
		
		// filling the map 
		map = new int [rows][COLS];
		for (int i = 0; i < startEmptyRows ; i++) 
			Arrays.fill(map[i], -1);
		for (int i = startEmptyRows; i < map.length; i++) {
			for (int j = 0; j < map[0].length; j++) {
				map[i][j] = random.nextInt(supportedColors); 
			}
		}	
		
		// adjust location and color of the 3 next bubbles to shoot
		int gap = displayDims.x/40;
		int y = drawOffset + (footerHeight-DIAM)/2;
		int x = (displayDims.x-gunWidth)/2 - 3*gap;
		for (int i = 0; i < nextBubbleColor.length; i++) {
			nextBubbleColor[i] = random.nextInt(supportedColors);
			nextBubbleLoc[i] = new Point(x - i*(DIAM+gap), y);
		}
		
		// initial bullet bubble location
		bulletInitLoc 	= new Point((displayDims.x - DIAM)/2, y);
		bulletLoc 		= new Point((displayDims.x - DIAM)/2, y);
	
	}
	
	public MainGame(Context context) {
		super(context);
		mainLoopThread = new GameLoop(this);
		getHolder().addCallback(this);
		setFocusable(true);
		
		redBitmap = BitmapFactory.decodeResource(getResources(),R.drawable.red);
		//FIXME
		DisplayMetrics metrics = context.getResources().getDisplayMetrics();
		displayDims = new Point(metrics.widthPixels, metrics.heightPixels);
		
		
		initGame(15);
		bubblesResized = Bitmap.createScaledBitmap(redBitmap, DIAM, DIAM, false);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub

	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		mainLoopThread.isRunning = true;
		mainLoopThread.start();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		mainLoopThread.isRunning = false ;
		boolean retry = true;
		// mainLoopThread.isRunning = false;
		while (retry) {
			try {
				mainLoopThread.join();
				retry = false;
			} catch (InterruptedException e) {

			}

		}

	}
	
	
	
	double slope;
	int delta = 1;
	boolean isfired = false ;
	int fireCnt ; 
	int v = 5; //firing velocity pixel/frame
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN){
			//touch down
			
			
			
			if (event.getY() > bulletInitLoc.y)
				return false;
			
			slope = (bulletInitLoc.y-event.getY())/(bulletInitLoc.x-event.getX())*1.0;
			System.out.println(slope);
			isfired = true;
			fireCnt = 1;
			bulletLoc.x = bulletInitLoc.x + ((bulletInitLoc.x > event.getX())? -v*fireCnt:v*fireCnt);
			bulletLoc.y = (int) ((bulletInitLoc.x-bulletLoc.x)* -slope) + bulletInitLoc.y;
			System.out.println("("+event.getX()+", "+event.getY()+")");
			System.out.println(bulletInitLoc);
			System.out.println(bulletLoc);
			return true ;
		}
		else if (event.getAction() == MotionEvent.ACTION_UP){
			//touch released;
		}
		
		
		return false;
	}	
	

	
	@Override
	protected void onDraw(Canvas canvas) {
		canvas.restore();
		//Draw the grid
		for (int i = baseRow; i < ROWS; i++){ 
			for (int j = 0; j < COLS -(i&1); j++){
				if (map[i][j] == -1)
					continue ;
				//TODO 	switch on map[i][j] to choose color 
				canvas.drawBitmap(bubblesResized,j*DIAM+((i&1)==1?DIAM/2:0), drawOffset - i*(DIAM-5),  null);
			}
		}
		
		// Draw the next to shoot bubbles
		for (int i = 0; i < nextBubbleColor.length; i++) 
			canvas.drawBitmap(bubblesResized, nextBubbleLoc[i].x, nextBubbleLoc[i].y, null);
		
		//TODO move to main thread: run
		if (isfired){
			bulletLoc.x = bulletLoc.x + ((bulletInitLoc.x > bulletLoc.x)? -v*fireCnt:v*fireCnt);
			bulletLoc.y = (int) ((bulletInitLoc.x-bulletLoc.x)* -slope) + bulletInitLoc.y;
			
			Log.v("onDraw", bulletLoc.toString());
			if (bulletLoc.x > displayDims.x-DIAM || bulletLoc.x < 0){
				isfired = false ;
				bulletLoc.x = bulletInitLoc.x;
				bulletLoc.y = bulletInitLoc.y;
			}
			
		}
		
		// Draw the bullet bubble
		canvas.drawBitmap(bubblesResized, bulletLoc.x,bulletLoc.y, null);
		
		
	}

}
