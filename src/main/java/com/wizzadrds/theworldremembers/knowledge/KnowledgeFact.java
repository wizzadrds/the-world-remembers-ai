package com.wizzadrds.theworldremembers.knowledge;
import java.util.UUID;
public record KnowledgeFact(UUID holder,String eventType,UUID subject,UUID source,KnowledgeProvenance provenance,long observedTick,double confidence,int hops){public KnowledgeFact{if(confidence<0||confidence>1)throw new IllegalArgumentException("confidence");if(hops<0)throw new IllegalArgumentException("hops");}}
