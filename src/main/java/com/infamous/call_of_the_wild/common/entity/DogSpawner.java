package com.infamous.call_of_the_wild.common.entity;

import com.infamous.call_of_the_wild.common.entity.dog.Dog;
import com.infamous.call_of_the_wild.common.registry.COTWEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.ForgeHooks;

import java.util.List;

public class DogSpawner implements CustomSpawner {
   private static final int TICK_DELAY = 1200;
   private int nextTick;

   public int tick(ServerLevel level, boolean spawnEnemies, boolean spawnFriendlies) {
      if (spawnFriendlies && level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) {
         --this.nextTick;
         if (this.nextTick <= 0) {
            this.nextTick = 1200;
            Player player = level.getRandomPlayer();
            if (player != null) {
               RandomSource random = level.random;
               int xOffset = (8 + random.nextInt(24)) * (random.nextBoolean() ? -1 : 1);
               int zOffset = (8 + random.nextInt(24)) * (random.nextBoolean() ? -1 : 1);
               BlockPos targetPos = player.blockPosition().offset(xOffset, 0, zOffset);
               int horizontalOffset = 10;
               if (level.hasChunksAt(targetPos.getX() - horizontalOffset, targetPos.getZ() - horizontalOffset, targetPos.getX() + horizontalOffset, targetPos.getZ() + horizontalOffset)) {
                  if (NaturalSpawner.isSpawnPositionOk(SpawnPlacements.Type.ON_GROUND, level, targetPos, COTWEntityTypes.DOG.get())) {
                     if (level.isCloseToVillage(targetPos, 2)) {
                        return this.spawnInVillage(level, targetPos);
                     }

                     /*
                     if (level.structureManager().getStructureWithPieceAt(targetPos, StructureTags.CATS_SPAWN_IN).isValid()) {
                        return this.spawnInHut(level, targetPos);
                     }
                      */
                  }

               }
            }
         }
      }
      return 0;
   }

   private int spawnInVillage(ServerLevel level, BlockPos blockPos) {
      int i = 48;
      if (level.getPoiManager().getCountInRange((h) -> h.is(PoiTypes.HOME), blockPos, 48, PoiManager.Occupancy.IS_OCCUPIED) > 4L) {
         List<Dog> nearbyDogs = level.getEntitiesOfClass(Dog.class, (new AABB(blockPos)).inflate(48.0D, 8.0D, 48.0D));
         if (nearbyDogs.size() < 5) {
            return this.spawnDog(blockPos, level);
         }
      }

      return 0;
   }

   private int spawnInHut(ServerLevel level, BlockPos blockPos) {
      int i = 16;
      List<Dog> nearbyDogs = level.getEntitiesOfClass(Dog.class, (new AABB(blockPos)).inflate(16.0D, 8.0D, 16.0D));
      return nearbyDogs.size() < 1 ? this.spawnDog(blockPos, level) : 0;
   }

   private int spawnDog(BlockPos blockPos, ServerLevel level) {
      Dog dog = COTWEntityTypes.DOG.get().create(level);
      if (dog == null) {
         return 0;
      } else {
         dog.moveTo(blockPos, 0.0F, 0.0F); // Fix MC-147659: Some witch huts spawn the incorrect cat
         if(ForgeHooks.canEntitySpawn(dog, level, blockPos.getX(), blockPos.getY(), blockPos.getZ(), null, MobSpawnType.NATURAL) == -1) return 0;
         dog.finalizeSpawn(level, level.getCurrentDifficultyAt(blockPos), MobSpawnType.NATURAL, (SpawnGroupData)null, (CompoundTag)null);
         level.addFreshEntityWithPassengers(dog);
         return 1;
      }
   }
}
