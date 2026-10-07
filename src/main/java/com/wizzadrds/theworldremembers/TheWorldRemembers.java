            }
        }
    }

    private static void linkBabyToNearbyParents(ServerLevel world, Villager child, com.wizzadrds.theworldremembers.family.FamilyManager families, MemoryManager memories) {
        java.util.List<Villager> adults=nearbyVillagers(world, child, 8).stream()
                .filter(v -> !v.isBaby() && !v.getUUID().equals(child.getUUID()))
                .toList();
        if(adults.size()!=2) return;
        families.addParentChild(adults.get(0).getUUID(), child.getUUID());
        families.addParentChild(adults.get(1).getUUID(), child.getUUID());
        if (families.parentsOf(child.getUUID()).size() == 2) {
            long time=world.getGameTime();
            memories.inheritFamilyHistory(adults.get(0).getUUID(), child.getUUID(), time);
            memories.inheritFamilyHistory(adults.get(1).getUUID(), child.getUUID(), time);
            memories.rememberEvent(child.getUUID(), adults.get(0).getUUID(), MemoryEventType.NPC_BORN, time, MemoryImportance.IMPORTANT);
            memories.rememberEvent(child.getUUID(), adults.get(1).getUUID(), MemoryEventType.NPC_BORN, time, MemoryImportance.IMPORTANT);
            families.linkSiblingsFromSharedParent(child.getUUID());
        }
    }
    private static void processCourtship(ServerLevel world, Villager villager, FamilyManager families, FamilyCourtshipManager courtship, MemoryManager memories, NpcAgeManager ages) {
        java.util.List<Villager> candidates = nearbyVillagers(world, villager, 4).stream()
            .filter(other -> !other.isBaby() && !other.getUUID().equals(villager.getUUID())
                && ages.get(other.getUUID()) != null && ages.get(other.getUUID()).isAdult()
                && !families.hasSpouse(other.getUUID()) && !families.areRelated(villager.getUUID(), other.getUUID())
                && marriageCompatible(villager, other, ages))
            .toList();
        if (candidates.isEmpty()) return;
        Villager partner = candidates.stream().min(java.util.Comparator.comparingDouble(villager::distanceToSqr)).orElse(null);
        if (partner == null || villager.getUUID().compareTo(partner.getUUID()) > 0) return;
        int progress = courtship.advance(villager.getUUID(), partner.getUUID(), 20);
        if (progress < 1200) return;
        if (!families.addSpouses(villager.getUUID(), partner.getUUID())) { courtship.clear(villager.getUUID(), partner.getUUID()); return; }
        long time = world.getGameTime();
        memories.rememberEvent(villager.getUUID(), partner.getUUID(), MemoryEventType.NPC_MARRIED, time, MemoryImportance.IMPORTANT);
        memories.rememberEvent(partner.getUUID(), villager.getUUID(), MemoryEventType.NPC_MARRIED, time, MemoryImportance.IMPORTANT);
        courtship.clear(villager.getUUID(), partner.getUUID());
    }

    private static java.util.List<Villager> nearbyVillagers(ServerLevel world, Villager origin, double radius) {
        long now = world.getGameTime();
        VillagerSpatialIndex index = VILLAGER_SPATIAL_INDEX.get(world);
        if (index == null || now - index.gameTime() >= VILLAGER_SPATIAL_INDEX_TICKS) {
            java.util.Map<Long,java.util.List<Villager>> buckets = new java.util.HashMap<>();
            java.util.List<UUID> ids = VILLAGER_REGISTRY.get(world);
            if (ids != null) for (UUID id : ids) {
                Entity entity = world.getEntity(id);
                if (!(entity instanceof Villager villager) || !villager.isAlive() || villager.isRemoved()) continue;
                int x = villager.blockPosition().getX(), z = villager.blockPosition().getZ();
                long key = (((long)(x >> 4)) << 32) ^ ((z >> 4) & 0xffffffffL);
                buckets.computeIfAbsent(key, ignored -> new java.util.ArrayList<>()).add(villager);
            } else {
                // Deterministic/full GameTest execution can call the helpers without the
                // live scheduler ever having populated its rotating registry.
                for (Villager villager : world.getEntitiesOfClass(
                        Villager.class,
                        new net.minecraft.world.phys.AABB(-30_000_000,-2048,-30_000_000,30_000_000,2048,30_000_000),
                        v -> v.isAlive() && !v.isRemoved())) {
                    int x = villager.blockPosition().getX(), z = villager.blockPosition().getZ();
                    long key = (((long)(x >> 4)) << 32) ^ ((z >> 4) & 0xffffffffL);
                    buckets.computeIfAbsent(key, ignored -> new java.util.ArrayList<>()).add(villager);
                }
            }
            index = new VillagerSpatialIndex(now, buckets);
            VILLAGER_SPATIAL_INDEX.put(world, index);
        }
        return index.nearby(origin, radius);
    }

    private static boolean marriageCompatible(Villager first, Villager second, NpcAgeManager ages) {
        var firstAge = ages.get(first.getUUID());
        var secondAge = ages.get(second.getUUID());