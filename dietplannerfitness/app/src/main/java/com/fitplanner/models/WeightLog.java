package com.fitplanner.models;

public class WeightLog {
    private int id;
    private double weight;
    private String date;

    public WeightLog(int id, double weight, String date) {
        this.id = id;
        this.weight = weight;
        this.date = date;
    }

    public int getId() { return id; }
    public double getWeight() { return weight; }
    public String getDate() { return date; }
}
