package com.example.PrincessMemoryGame;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.Random;

//-----------------------------------------------

public class GameView extends View {

    public static final long CALLBACK_WAIT_TIME = 1000;

    public enum State {
        UNSOLVED(-3),
        WIN(-2),
        EMPTY(0),
        SELECTED(1),
        SOLVED(2);

        private int mValue;

        private State(int value) {
            mValue = value;
        }

        public int getValue() {
            return mValue;
        }

        public static State fromInt(int i) {
            for (State s : values()) {
                if (s.getValue() == i) {
                    return s;
                }
            }
            return EMPTY;
        }
    }

    private int[] princesses = {R.drawable.ariel, R.drawable.aurora, R.drawable.belle, R.drawable.cinderella, R.drawable.mulan, R.drawable.rapunzel, R.drawable.pocahantas, R.drawable.snowwhite};

    private static final int MARGIN = 4;
    private static final int MSG_BLINK = 1;
    private static final int BOARD_SIDE = 4;

    private final Handler mHandler = new Handler(new MyHandler());

    private final Rect cellSquare = new Rect();
    private final Rect gameBoardSquare = new Rect();

    private int mSxy;
    private int mOffetX;
    private int mOffetY;
    private Paint mWinPaint;
    private Paint mLinePaint;
    private Paint mBmpPaint;
    private Bitmap bitmapUsedToGetCellSize;
    private Drawable drawableBackground;

    private ICellListener mCellListener;

    /**
     * Contains one of {@link State#EMPTY}, {@link State#SELECTED} or {@link State#SOLVED}.
     */
    private final Cell[] cells = new Cell[BOARD_SIDE * BOARD_SIDE];

    private int selectedCellIndex = -1;
    private State mSelectedValue = State.EMPTY;
    private State mCurrentPlayer = State.UNSOLVED;
    private State mWinner = State.EMPTY;

    private int mWinCol = -1;
    private int mWinRow = -1;
    private int mWinDiag = -1;

    private boolean mBlinkDisplayOff;
    private final Rect mBlinkRect = new Rect();

    private Random random = new Random();
    private int tries = 0;
    private int firstTryIndex = 0;

    public interface ICellListener {
        abstract void onCellSelected();
    }

    public GameView(Context context, AttributeSet attributes) {
        super(context, attributes);
        requestFocus();

        drawableBackground = getResources().getDrawable(R.drawable.background);
        setBackground(drawableBackground);

        bitmapUsedToGetCellSize = getResBitmap(R.drawable.belle);

        if (bitmapUsedToGetCellSize != null) {
            cellSquare.set(0, 0, bitmapUsedToGetCellSize.getWidth() - 1, bitmapUsedToGetCellSize.getHeight() - 1);
        }

        mBmpPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        mLinePaint = new Paint();
        mLinePaint.setColor(0xff00ffff);
        mLinePaint.setStrokeWidth(5);
        mLinePaint.setStyle(Style.STROKE);

        mWinPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mWinPaint.setColor(0xffff00ff);
        mWinPaint.setStrokeWidth(10);
        mWinPaint.setStyle(Style.STROKE);

        for (int i = 0; i < cells.length; i++) {
            int princessIndex = (int) Math.ceil((i + 1) / 2.0) - 1;
            cells[i] = new Cell(princessIndex);
        }
    }

    public void resetBoard() {
        tries = 0;
        for (int i = 0; i < cells.length; i++) {
            cells[i].setState(State.UNSOLVED);
        }
    }

    public void shuffleBoard() {
        int maxRandomNumber = cells.length;
        for (int i = 0; i < cells.length; i++) {
            int indexToSwap = random.nextInt(maxRandomNumber);
            int oldValue = cells[i].getPrincessesIndex();
            int newValue = cells[indexToSwap].getPrincessesIndex();
            cells[i].setPrincessesIndex(newValue);
            cells[indexToSwap].setPrincessesIndex(oldValue);
        }
        invalidate();
    }

    public Cell[] getCells() {
        return cells;
    }

    public void setCell(int cellIndex, Cell value) {
        cells[cellIndex] = value;
        invalidate();
    }

    public void setCellListener(ICellListener cellListener) {
        mCellListener = cellListener;
    }

    public int getSelection() {
        if (mSelectedValue == mCurrentPlayer) {
            return selectedCellIndex;
        }

        return -1;
    }

    public State getCurrentPlayer() {
        return mCurrentPlayer;
    }

    public void setCurrentPlayer(State player) {
        mCurrentPlayer = player;
        selectedCellIndex = -1;
    }

    public State getWinner() {
        return mWinner;
    }

    public void setWinner(State winner) {
        mWinner = winner;
    }

    /**
     * Sets winning mark on specified column or row (0..2) or diagonal (0..1).
     */
    public void setFinished(int col, int row, int diagonal) {
        mWinCol = col;
        mWinRow = row;
        mWinDiag = diagonal;
    }

    //-----------------------------------------


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int sxy = mSxy;
        int s3 = sxy * BOARD_SIDE;
        int x7 = mOffetX;
        int y7 = mOffetY;

        for (int i = 0, k = sxy; i < BOARD_SIDE - 1; i++, k += sxy) {
            canvas.drawLine(x7, y7 + k, x7 + s3 - 1, y7 + k, mLinePaint);
            canvas.drawLine(x7 + k, y7, x7 + k, y7 + s3 - 1, mLinePaint);
        }

        for (int j = 0, k = 0, y = y7; j < BOARD_SIDE; j++, y += sxy) {
            for (int i = 0, x = x7; i < BOARD_SIDE; i++, k++, x += sxy) {
                gameBoardSquare.offsetTo(MARGIN + x, MARGIN + y);
                if (cells[k].getState() == State.SOLVED || cells[k].getState() == State.SELECTED) {
                    Bitmap bitmap = getResBitmap(cells[k].getPrincessResourceId(princesses));
                    canvas.drawBitmap(bitmap, cellSquare, gameBoardSquare, mBmpPaint);
                }
            }
        }


        if (mWinRow >= 0) {
            int y = y7 + mWinRow * sxy + sxy / (BOARD_SIDE - 1);
            canvas.drawLine(x7 + MARGIN, y, x7 + s3 - 1 - MARGIN, y, mWinPaint);

        } else if (mWinCol >= 0) {
            int x = x7 + mWinCol * sxy + sxy / (BOARD_SIDE - 1);
            canvas.drawLine(x, y7 + MARGIN, x, y7 + s3 - 1 - MARGIN, mWinPaint);

        } else if (mWinDiag == 0) {
            // diagonal 0 is from (0,0) to (2,2)

            canvas.drawLine(x7 + MARGIN, y7 + MARGIN,
                            x7 + s3 - 1 - MARGIN, y7 + s3 - 1 - MARGIN, mWinPaint);

        } else if (mWinDiag == 1) {
            // diagonal 1 is from (0,2) to (2,0)

            canvas.drawLine(x7 + MARGIN, y7 + s3 - 1 - MARGIN,
                            x7 + s3 - 1 - MARGIN, y7 + MARGIN, mWinPaint);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Keep the view squared
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = MeasureSpec.getSize(heightMeasureSpec);
        int d = w == 0 ? h : h == 0 ? w : w < h ? w : h;
        setMeasuredDimension(d, d);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        int sx = (w - (BOARD_SIDE - 1) * MARGIN) / BOARD_SIDE;
        int sy = (h - (BOARD_SIDE - 1) * MARGIN) / BOARD_SIDE;

        int size = sx < sy ? sx : sy;

        mSxy = size;
        mOffetX = (w - BOARD_SIDE * size) / (BOARD_SIDE - 1);
        mOffetY = (h - BOARD_SIDE * size) / (BOARD_SIDE - 1);

        gameBoardSquare.set(MARGIN, MARGIN, size - MARGIN, size - MARGIN);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();

        if (action == MotionEvent.ACTION_DOWN) {
            return true;

        } else if (action == MotionEvent.ACTION_UP) {
            int x = (int) event.getX();
            int y = (int) event.getY();

            int sxy = mSxy;
            x = (x - MARGIN) / sxy;
            y = (y - MARGIN) / sxy;
            int cell = x + BOARD_SIDE * y;

            if (/*isEnabled() && */ cells[cell].getState() == State.UNSOLVED && x >= 0 && x < BOARD_SIDE && y >= 0 & y < BOARD_SIDE) {
                incrementTries();
                if (tries == 1) {
                    selectedCellIndex = cell;
                    cells[cell].setState(State.SELECTED);
                } else {
                    if (cells[cell].getPrincessesIndex() == cells[selectedCellIndex].getPrincessesIndex()) {
                        cells[cell].setState(State.SOLVED);
                        cells[selectedCellIndex].setState(State.SOLVED);
                    } else {
                        cells[cell].setState(State.SELECTED);
//                        cells[selectedCellIndex].setState(State.UNSOLVED);
                    }
                }
                mHandler.sendEmptyMessageDelayed(MSG_BLINK, CALLBACK_WAIT_TIME);
                invalidate();
            }

            return true;
        }
        return true;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();

        Parcelable s = super.onSaveInstanceState();
        bundle.putParcelable("gv_super_state", s);

        bundle.putBoolean("gv_en", isEnabled());

        int[] data = new int[cells.length];
        for (int i = 0; i < data.length; i++) {
            data[i] = cells[i].getState().getValue();
        }
        bundle.putIntArray("gv_data", data);

        bundle.putInt("gv_sel_cell", selectedCellIndex);
        bundle.putInt("gv_sel_val", mSelectedValue.getValue());
        bundle.putInt("gv_curr_play", mCurrentPlayer.getValue());
        bundle.putInt("gv_winner", mWinner.getValue());

        bundle.putInt("gv_win_col", mWinCol);
        bundle.putInt("gv_win_row", mWinRow);
        bundle.putInt("gv_win_diag", mWinDiag);

        bundle.putBoolean("gv_blink_off", mBlinkDisplayOff);
        bundle.putParcelable("gv_blink_rect", mBlinkRect);

        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {

        if (!(state instanceof Bundle)) {
            // Not supposed to happen.
            super.onRestoreInstanceState(state);
            return;
        }

        Bundle bundle = (Bundle) state;
        Parcelable superState = bundle.getParcelable("gv_super_state");

        setEnabled(bundle.getBoolean("gv_en", true));

        int[] data = bundle.getIntArray("gv_data");
        if (data != null && data.length == cells.length) {
            for (int i = 0; i < data.length; i++) {
                cells[i] = new Cell(data[i]);
            }
        }

        selectedCellIndex = bundle.getInt("gv_sel_cell", -1);
        mSelectedValue = State.fromInt(bundle.getInt("gv_sel_val", State.EMPTY.getValue()));
        mCurrentPlayer = State.fromInt(bundle.getInt("gv_curr_play", State.EMPTY.getValue()));
        mWinner = State.fromInt(bundle.getInt("gv_winner", State.EMPTY.getValue()));

        mWinCol = bundle.getInt("gv_win_col", -1);
        mWinRow = bundle.getInt("gv_win_row", -1);
        mWinDiag = bundle.getInt("gv_win_diag", -1);

        mBlinkDisplayOff = bundle.getBoolean("gv_blink_off", false);
        Rect r = bundle.getParcelable("gv_blink_rect");
        if (r != null) {
            mBlinkRect.set(r);
        }

        // let the blink handler decide if it should blink or not
        mHandler.sendEmptyMessage(MSG_BLINK);

        super.onRestoreInstanceState(superState);
    }

    //-----

    private class MyHandler implements Callback {
        public boolean handleMessage(Message msg) {
            if (msg.what == MSG_BLINK) {
                if (tries == 2) {
                    for (int i = 0; i < cells.length; i++) {
                        if (cells[i].getState() == State.SELECTED) {
                            cells[i].setState(State.UNSOLVED);
                        }
                    }
                    invalidate();
                }
                return true;
            }
            return false;
        }
    }

    private void incrementTries() {
        ++tries;
        if (tries > 2) {
            tries = 1;
        }
    }

    private Bitmap getResBitmap(int bmpResId) {
        Options opts = new Options();
        opts.inDither = false;

        Resources res = getResources();
        Bitmap bmp = BitmapFactory.decodeResource(res, bmpResId, opts);

        if (bmp == null && isInEditMode()) {
            // BitmapFactory.decodeResource doesn't work from the rendering
            // library in Eclipse's Graphical Layout Editor. Use this workaround instead.

            Drawable d = res.getDrawable(bmpResId);
            int w = d.getIntrinsicWidth();
            int h = d.getIntrinsicHeight();
            bmp = Bitmap.createBitmap(w, h, Config.ARGB_8888);
            Canvas c = new Canvas(bmp);
            d.setBounds(0, 0, w - 1, h - 1);
            d.draw(c);
        }

        return bmp;
    }
}


