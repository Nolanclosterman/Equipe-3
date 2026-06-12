package com.equipe3.backend.model;

/**
 * The three core indicators of a company, each ranging from 0 to 100.
 * Mirrors the dashboard gauges of the mockup: money, ecology, ethics.
 */
public class Scores {

    private int money;
    private int ecology;
    private int ethics;

    public Scores() {
        this(50, 50, 50);
    }

    public Scores(int money, int ecology, int ethics) {
        this.money = clamp(money);
        this.ecology = clamp(ecology);
        this.ethics = clamp(ethics);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    public int getMoney() {
        return money;
    }

    public void setMoney(int money) {
        this.money = clamp(money);
    }

    public int getEcology() {
        return ecology;
    }

    public void setEcology(int ecology) {
        this.ecology = clamp(ecology);
    }

    public int getEthics() {
        return ethics;
    }

    public void setEthics(int ethics) {
        this.ethics = clamp(ethics);
    }
}
