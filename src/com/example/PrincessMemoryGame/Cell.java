package com.example.PrincessMemoryGame;

public class Cell {
    private GameView.State state = GameView.State.UNSOLVED;
    private int princessesIndex;

    public Cell(int princess) {
        this.princessesIndex = princess;
    }

    public int getPrincessResourceId(int[] princesses) {
        return princesses[this.getPrincessesIndex()];
    }

    public GameView.State getState() {
        return state;
    }

    public void setState(GameView.State state) {
        this.state = state;
    }

    public int getPrincessesIndex() {
        return princessesIndex;
    }

    public void setPrincessesIndex(int princessesIndex) {
        this.princessesIndex = princessesIndex;
    }
}