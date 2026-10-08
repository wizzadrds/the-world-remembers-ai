package com.wizzadrds.theworldremembers.client;
import net.fabricmc.loader.api.FabricLoader; import java.io.*; import java.nio.file.*; import java.util.*;
public final class TwrClientConfig {
 private static final Path FILE=FabricLoader.getInstance().getConfigDir().resolve("the-world-remembers.properties");
 public String providerUrl=""; public String apiKey=""; public boolean aiEnabled=false; public String micDevice="Default"; public String outputDevice="Default";
 public static TwrClientConfig load(){var c=new TwrClientConfig();var p=new Properties();try(InputStream in=Files.newInputStream(FILE)){p.load(in);c.providerUrl=p.getProperty("providerUrl","");c.apiKey=p.getProperty("apiKey","");c.aiEnabled=Boolean.parseBoolean(p.getProperty("aiEnabled","false"));c.micDevice=p.getProperty("micDevice","Default");c.outputDevice=p.getProperty("outputDevice","Default");}catch(IOException ignored){}return c;}
 public void save(){var p=new Properties();p.setProperty("providerUrl",providerUrl);p.setProperty("apiKey",apiKey);p.setProperty("aiEnabled",Boolean.toString(aiEnabled));p.setProperty("micDevice",micDevice);p.setProperty("outputDevice",outputDevice);try{Files.createDirectories(FILE.getParent());try(OutputStream out=Files.newOutputStream(FILE)){p.store(out,"The World Remembers client settings");}}catch(IOException ignored){}}
 public boolean hasAiCredentials(){return aiEnabled&&!providerUrl.isBlank()&&!apiKey.isBlank();}
 public static List<String> inputDevices(){var out=new ArrayList<String>();out.add("Default");try{for(var info:javax.sound.sampled.AudioSystem.getMixerInfo()){var m=javax.sound.sampled.AudioSystem.getMixer(info);if(m.getTargetLineInfo().length>0)out.add(info.getName());}}catch(Exception ignored){}return out;}
 public static List<String> outputDevices(){var out=new ArrayList<String>();out.add("Default");try{for(var info:javax.sound.sampled.AudioSystem.getMixerInfo()){var m=javax.sound.sampled.AudioSystem.getMixer(info);if(m.getSourceLineInfo().length>0)out.add(info.getName());}}catch(Exception ignored){}return out;}
}
