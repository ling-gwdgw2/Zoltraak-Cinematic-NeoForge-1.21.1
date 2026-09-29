package com.frierenflight.zoltraakcinematic.client;

import com.frierenflight.zoltraakcinematic.entity.boss.QualBossEntity;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import io.redspace.ironsspellbooks.player.ClientMagicData;

/**
 * 📜 QualClientHelper — Client-side failover resolver for QualBossEntity.
 * Isolated in client package to prevent any NoClassDefFoundError on dedicated servers.
 */
public class QualClientHelper {

    public static int getSyncedSpellState(QualBossEntity entity) {
        try {
            SyncedSpellData synced = ClientMagicData.getSyncedSpellData(entity);
            if (synced != null && synced.isCasting()) {
                String id = synced.getCastingSpellId();
                if (id != null && id.contains("barrage")) {
                    return QualBossEntity.CAST_STATE_BARRAGE;
                }
                return QualBossEntity.CAST_STATE_BEAM;
            }
        } catch (Throwable ignored) {
            // Fail gracefully if Iron's Spells client data is unavailable
        }
        return QualBossEntity.CAST_STATE_IDLE;
    }
}
