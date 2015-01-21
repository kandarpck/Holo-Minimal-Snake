package com.kandarp.snake;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class Snake extends Activity {

	private SnakeView mSnakeView;

	static final int DIALOG_ABOUT_ID = 0;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.snake_layout);

		mSnakeView = (SnakeView) findViewById(R.id.snake);
		mSnakeView.setTextView((TextView) findViewById(R.id.text));
		mSnakeView.setScoreView((TextView) findViewById(R.id.textscore));

		int dHeight = getWindowManager().getDefaultDisplay().getHeight();
		int dWidth = getWindowManager().getDefaultDisplay().getWidth();
		mSnakeView.setTileSizes(dWidth, dHeight);

		mSnakeView.setMode(SnakeView.READY);
	}

	@Override
	protected void onResume() {
		super.onResume();
		Vibrator mvibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		mSnakeView.setVibrator(mvibrator);
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mSnakeView.getMode() == SnakeView.RUNNING) {
			mSnakeView.setMode(SnakeView.PAUSE);
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.snake_menu, menu);
		menu.findItem(R.id.menu_about).setIcon(
				getResources().getDrawable(
						android.R.drawable.ic_menu_info_details));
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (mSnakeView.getMode() == SnakeView.RUNNING)
			mSnakeView.setMode(SnakeView.PAUSE);
		return true;
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.menu_about:
			showDialog(DIALOG_ABOUT_ID);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = null;

		switch (id) {
		case DIALOG_ABOUT_ID:
			LayoutInflater inflatera = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
			View layouta = inflatera.inflate(R.layout.about_layout,
					(ViewGroup) findViewById(R.id.about_layout_root));
			AlertDialog.Builder buildera = new AlertDialog.Builder(this);
			buildera.setTitle(R.string.about_title);
			buildera.setIcon(R.drawable.icon);
			buildera.setView(layouta);

			buildera.setPositiveButton("OK",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					});

			dialog = buildera.create();
			dialog.setCancelable(true);
			dialog.setCanceledOnTouchOutside(true);
			break;
		}
		return dialog;
	}

	@Override
	public void onBackPressed() {
		if (mSnakeView.getMode() == SnakeView.RUNNING) {
			mSnakeView.setMode(SnakeView.PAUSE);
		} else
			finish();
	}
}