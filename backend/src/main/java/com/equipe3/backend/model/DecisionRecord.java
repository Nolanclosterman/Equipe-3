package com.equipe3.backend.model;

/**
 * One entry of the decision history: the problem the player faced, the solution
 * they chose, and the narrative consequence. Fed back into event generation so
 * that "previous decisions bring consequences" (Design §4.1).
 */
public record DecisionRecord(String problem, String solution, String narrative) {
}
