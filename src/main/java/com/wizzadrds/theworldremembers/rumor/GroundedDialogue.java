package com.wizzadrds.theworldremembers.rumor;
public final class GroundedDialogue {
 private GroundedDialogue() {}
 public static String render(KnowledgeFact f) {
  String certainty;
  switch (f.origin()) {
   case DIRECT -> certainty = "I saw it myself";
   case REPORTED -> certainty = "I heard it from someone";
   case RUMORED -> certainty = "People say";
   default -> certainty = "I am not sure";
  }
  return certainty + ": " + f.eventType().name().toLowerCase().replace('_',' ') + ". (" + f.confidence() + "% certain)";
 }
}
