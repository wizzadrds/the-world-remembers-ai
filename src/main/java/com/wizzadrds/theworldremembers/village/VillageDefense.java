package com.wizzadrds.theworldremembers.village;
public record VillageDefense(int livingGolems,int armedVillagers,int recentDeaths){ public VillageDefense{if(livingGolems<0||armedVillagers<0||recentDeaths<0)throw new IllegalArgumentException();} public int score(){return livingGolems*3+armedVillagers-recentDeaths*2;} }
