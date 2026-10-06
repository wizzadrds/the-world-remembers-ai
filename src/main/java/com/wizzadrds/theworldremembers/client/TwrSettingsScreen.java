package com.wizzadrds.theworldremembers.client;
import net.minecraft.client.gui.GuiGraphics; import net.minecraft.client.gui.components.*; import net.minecraft.client.gui.screens.Screen; import net.minecraft.network.chat.Component; import java.util.*;
public final class TwrSettingsScreen extends Screen {
 private final Screen parent; private final TwrClientConfig config; private EditBox url,key,mic,out; private CycleButton<Boolean> enabled;
 public TwrSettingsScreen(Screen parent){super(Component.literal("The World Remembers"));this.parent=parent;this.config=TwrClientConfig.load();}
 @Override protected void init(){int cx=width/2,w=280; url=new EditBox(font,cx-w/2,55,w,20,Component.literal("AI provider URL"));url.setValue(config.providerUrl);addRenderableWidget(url);
 key=new EditBox(font,cx-w/2,85,w,20,Component.literal("API key"));key.setValue(config.apiKey);key.setSuggestion("API key (stored locally)");key.setResponder(s->{});addRenderableWidget(key);
 enabled=CycleButton.onOffBuilder(config.aiEnabled).withTooltip(v->Component.literal("Optional: AI is disabled by default")).create(cx-w/2,115,w,20,Component.literal("AI features"),(b,v)->config.aiEnabled=v);addRenderableWidget(enabled);
 mic=new EditBox(font,cx-w/2,145,w,20,Component.literal("Microphone"));mic.setValue(config.micDevice);addRenderableWidget(mic);
 out=new EditBox(font,cx-w/2,175,w,20,Component.literal("Headphones / output"));out.setValue(config.outputDevice);addRenderableWidget(out);
 addRenderableWidget(Button.builder(Component.literal("Save"),b->save()).bounds(cx-140,220,135,20).build());
 addRenderableWidget(Button.builder(Component.literal("Cancel"),b->minecraft.setScreen(parent)).bounds(cx+5,220,135,20).build());
 addRenderableWidget(Button.builder(Component.literal("Reset audio names"),b->{mic.setValue("Default");out.setValue("Default");}).bounds(cx-140,250,280,20).build());}
 private void save(){config.providerUrl=url.getValue().trim();config.apiKey=key.getValue();config.micDevice=mic.getValue().trim();config.outputDevice=out.getValue().trim();config.save();minecraft.setScreen(parent);}
 @Override public void render(GuiGraphics g,int mx,int my,float d){renderBackground(g);g.drawCenteredString(font,"THE WORLD REMEMBERS — SETTINGS",width/2,25,0xFFFFFFFF);g.drawString(font,"AI provider URL",width/2-140,45,0xFFAAAAAA);g.drawString(font,"API key (local only; never a mod unlock)",width/2-140,75,0xFFAAAAAA);g.drawString(font,"Microphone device name",width/2-140,135,0xFFAAAAAA);g.drawString(font,"Output device name",width/2-140,165,0xFFAAAAAA);g.drawCenteredString(font,"Voice device selection is stored; native audio routing depends on the engine.",width/2,280,0xFFAAAAAA);super.render(g,mx,my,d);}
}
