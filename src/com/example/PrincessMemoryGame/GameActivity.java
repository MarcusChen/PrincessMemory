/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.PrincessMemoryGame;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import com.example.PrincessMemoryGame.GameView.ICellListener;
import com.example.PrincessMemoryGame.GameView.State;

import java.util.Random;


public class GameActivity extends Activity {
    private static final int MSG_COMPUTER_TURN = 1;
    private static final long COMPUTER_DELAY_MS = 500;
    private Handler mHandler = new Handler(new MyHandlerCallback());
    private GameView gameView;
    private TextView infoView;
    private Button newGameButton;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.game);

        gameView = (GameView) findViewById(R.id.game_view);
        infoView = (TextView) findViewById(R.id.move_feedback);
        newGameButton = (Button) findViewById(R.id.new_game);

        gameView.shuffleBoard();
        gameView.setFocusable(true);
        gameView.setFocusableInTouchMode(true);
        gameView.setCellListener(new MyCellListener());
//        gameView.setEnabled(true);

        newGameButton.setOnClickListener(new NewGameButtonListener());
    }

    @Override
    protected void onResume() {
        super.onResume();
        mHandler.sendEmptyMessageDelayed(MSG_COMPUTER_TURN, COMPUTER_DELAY_MS);
    }

    private class MyCellListener implements ICellListener {
        public void onCellSelected() {
            if (checkGameFinished()) {
                gameView.setEnabled(false);
                newGameButton.setEnabled(true);
                infoView.setText("Blythe, you win!!!");
            }
        }
    }

    private class NewGameButtonListener implements OnClickListener {

        public void onClick(View v) {
            gameView.resetBoard();
            gameView.shuffleBoard();
        }
    }

    private class MyHandlerCallback implements Callback {
        public boolean handleMessage(Message msg) {
            if (checkGameFinished()) {
                infoView.setText("Blythe, you win!!!");
                return true;
            }
            return false;
        }
    }

    public boolean checkGameFinished() {
        Cell[] cells = gameView.getCells();
        boolean allSolved = true;
        for (int i = 0; i < cells.length; i++) {
            if (!cells[i].getState().equals(State.SOLVED)) {
                allSolved = false;
                break;
            }
        }
        return allSolved;
    }
}
