package handling.channel.handler;

import client.MapleBuffStat;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleJob;
import client.MapleStat;
import client.PlayerStats;
import client.Skill;
import client.SkillFactory;
import client.SkillMacro;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import constants.GameConstants;
import handling.channel.ChannelServer;
import java.awt.Point;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MaplePortal;
import server.MapleStatEffect;
import server.Randomizer;
import server.Timer.CloneTimer;
import server.life.MapleMonster;
import server.life.MobAttackInfo;
import server.life.MobSkill;
import server.life.MobSkillFactory;
import server.maps.FieldLimitType;
import server.maps.MapleMap;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.movement.LifeMovementFragment;
import server.quest.MapleQuest;
import tools.FileoutputUtil;
import tools.Pair;
import tools.data.LittleEndianAccessor;
import tools.packet.CField;
import tools.packet.CWvsContext;
import tools.packet.MTSCSPacket;
import tools.packet.MobPacket;

public class PlayerHandler {

    public static int xx = 0;

    public static int isFinisher(int skillid) {
        switch (skillid) {
            case 1111003:
                return GameConstants.GMS ? 1 : 10;
            case 1111005:
                return GameConstants.GMS ? 2 : 10;
            case 11111002:
                return GameConstants.GMS ? 1 : 10;
            case 11111003:
                return GameConstants.GMS ? 2 : 10;
        }
        return 0;
    }

    public static void ChangeSkillMacro(LittleEndianAccessor slea, MapleCharacter chr) {
        int num = slea.readByte();

        for (int i = 0; i < num; i++) {
            String name = slea.readMapleAsciiString();
            int shout = slea.readByte();
            int skill1 = slea.readInt();
            int skill2 = slea.readInt();
            int skill3 = slea.readInt();

            SkillMacro macro = new SkillMacro(skill1, skill2, skill3, name, shout, i);
            chr.updateMacros(i, macro);
        }
    }
 public static void updateSpecialStat(final LittleEndianAccessor slea, final MapleClient c) {
        String stat = slea.readMapleAsciiString();
        int array = slea.readInt();
        int mode = slea.readInt();
        switch (stat) {
            case "honorLeveling":
                c.getSession().write(CWvsContext.updateSpecialStat(stat, array, mode, c.getPlayer().getHonourNextExp()));
                break;
            case "hyper":
                c.getSession().write(CWvsContext.updateSpecialStat(stat, array, mode, 0));
                break;
        }
    }
    public static final void ChangeKeymap(LittleEndianAccessor slea, MapleCharacter chr) {
        if ((slea.available() > 8L) && (chr != null)) {
            slea.skip(4);
            int numChanges = slea.readInt();

            for (int i = 0; i < numChanges; i++) {
                int key = slea.readInt();
                byte type = slea.readByte();
                int action = slea.readInt();

                chr.changeKeybinding(key, type, action);

            }
        } else if (chr != null) {
            int type = slea.readInt();
            int data = slea.readInt();
            switch (type) {
                case 1:
                    if (data <= 0) {
                        chr.getQuestRemove(MapleQuest.getInstance(122221));
                    } else {
                        chr.getQuestNAdd(MapleQuest.getInstance(122221)).setCustomData(String.valueOf(data));
                    }
                    break;
                case 2:
                    if (data <= 0) {
                        chr.getQuestRemove(MapleQuest.getInstance(122223));
                    } else {
                        chr.getQuestNAdd(MapleQuest.getInstance(122223)).setCustomData(String.valueOf(data));
                    }
                    break;
            }
        }
    }

    public static final void ANGELICCHANGE(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        if ((chr == null) || (chr.getMap() == null)) {
            return;
        }
        chr.getMap().broadcastMessage(chr, CField.showAnelicbuster(chr.getId(), slea.readInt()), false);
        chr.getMap().broadcastMessage(chr, CField.updateCharLook(chr), false);
        c.getSession().write(CWvsContext.enableActions());
    }

    public static final void UseTitle(int itemId, MapleClient c, MapleCharacter chr) {
        if ((chr == null) || (chr.getMap() == null)) {
            return;
        }
        Item toUse = chr.getInventory(MapleInventoryType.SETUP).findById(itemId);
        if (toUse == null) {
            return;
        }
        if (itemId <= 0) {
            chr.getQuestRemove(MapleQuest.getInstance(124000));
        } else {
            chr.getQuestNAdd(MapleQuest.getInstance(124000)).setCustomData(String.valueOf(itemId));
        }
        chr.getMap().broadcastMessage(chr, CField.showTitle(chr.getId(), itemId), false);
        c.getSession().write(CWvsContext.enableActions());
    }

    public static final void UseChair(int itemId, MapleClient c, MapleCharacter chr) {
        if ((chr == null) || (chr.getMap() == null)) {
            return;
        }
        Item toUse = chr.getInventory(MapleInventoryType.SETUP).findById(itemId);
        if (toUse == null) {

            return;
        }
        if ((GameConstants.isFishingMap(chr.getMapId())) && (itemId == 3011000)) {
            chr.startFishingTask();
        }
        chr.setChair(itemId);
        chr.getMap().broadcastMessage(chr, CField.showChair(chr.getId(), itemId), false);
        c.getSession().write(CWvsContext.enableActions());
    }

    public static final void CancelChair(short id, MapleClient c, MapleCharacter chr) {
        if (id == -1) {
            chr.cancelFishingTask();
            chr.setChair(0);
            c.getSession().write(CField.cancelChair(-1));
            if (chr.getMap() != null) {
                chr.getMap().broadcastMessage(chr, CField.showChair(chr.getId(), 0), false);
            }
        } else {
            chr.setChair(id);
            c.getSession().write(CField.cancelChair(id));
        }
    }

    public static final void TrockAddMap(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        byte addrem = slea.readByte();
        byte vip = slea.readByte();

        if (vip == 1) {
            if (addrem == 0) {
                chr.deleteFromRegRocks(slea.readInt());
            } else if (addrem == 1) {
                if (!FieldLimitType.VipRock.check(chr.getMap().getFieldLimit())) {
                    chr.addRegRockMap();
                } else {
                    chr.dropMessage(1, "This map is not available to enter for the list.");
                }
            }
        } else if (vip == 2) {
            if (addrem == 0) {
                chr.deleteFromRocks(slea.readInt());
            } else if (addrem == 1) {
                if (!FieldLimitType.VipRock.check(chr.getMap().getFieldLimit())) {
                    chr.addRockMap();
                } else {
                    chr.dropMessage(1, "This map is not available to enter for the list.");
                }
            }
        } else if (vip == 3) {
            if (addrem == 0) {
                chr.deleteFromHyperRocks(slea.readInt());
            } else if (addrem == 1) {
                if (!FieldLimitType.VipRock.check(chr.getMap().getFieldLimit())) {
                    chr.addHyperRockMap();
                } else {
                    chr.dropMessage(1, "This map is not available to enter for the list.");
                }
            }
        }
        c.getSession().write(MTSCSPacket.OnMapTransferResult(chr, vip, addrem == 0));
    }

    public static void CharInfoRequest(final int objectid, final MapleClient c, final MapleCharacter chr) {
        if (c.getPlayer() == null || c.getPlayer().getMap() == null) {
            return;
        }
        final MapleCharacter player = c.getPlayer().getMap().getCharacterById(objectid);
        c.getSession().write(CWvsContext.enableActions());
        if (player != null) {
            c.getSession().write(CWvsContext.charInfo(player, c.getPlayer().getId() == objectid));
        }
    }

    public static final void TakeDamage(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        try {
            slea.skip(4);
            slea.readInt();
            byte type = slea.readByte();
            slea.skip(1);
            int damage = slea.readInt();
            slea.skip(2);
            boolean isDeadlyAttack = false;
            boolean pPhysical = false;
            int oid = 0;
            int monsteridfrom = 0;
            int fake = 0;
            int mpattack = 0;
            int skillid = 0;
            int pID = 0;
            int pDMG = 0;
            byte direction = 0;
            byte pType = 0;
            Point pPos = new Point(0, 0);
            MapleMonster attacker = null;
            if ((chr == null) || (chr.isHidden()) || (chr.getMap() == null)) {
                c.getSession().write(CWvsContext.enableActions());
                return;
            }
            if ((chr.isGM()) && (chr.isInvincible())) {
                c.getSession().write(CWvsContext.enableActions());
                return;
            }
            PlayerStats stats = chr.getStat();
            if ((type != -2) && (type != -3) && (type != -4)) {
                monsteridfrom = slea.readInt();
                oid = slea.readInt();
                attacker = chr.getMap().getMonsterByOid(oid);
                direction = slea.readByte();

                if ((attacker == null) || (attacker.getId() != monsteridfrom) || (attacker.getLinkCID() > 0) || (attacker.isFake()) || (attacker.getStats().isFriendly())) {
                    return;
                }
                if ((type != -1) && (damage > 0)) {
                    MobAttackInfo attackInfo = attacker.getStats().getMobAttack(type);
                    if (attackInfo != null) {
                        if (attackInfo.isDeadlyAttack()) {
                            isDeadlyAttack = true;
                            mpattack = stats.getMp() - 1;
                        } else {
                            mpattack += attackInfo.getMpBurn();
                        }
                        MobSkill skill = MobSkillFactory.getMobSkill(attackInfo.getDiseaseSkill(), attackInfo.getDiseaseLevel());
                        if ((skill != null) && ((damage == -1) || (damage > 0))) {
                            skill.applyEffect(chr, attacker, false);
                        }
                        attacker.setMp(attacker.getMp() - attackInfo.getMpCon());
                    }
                }
                skillid = slea.readInt();
                pDMG = slea.readInt();
                byte defType = slea.readByte();
                slea.skip(1);
                if (defType == 1) {
                    Skill bx = SkillFactory.getSkill(31110008);
                    int bof = chr.getTotalSkillLevel(bx);
                    if (bof > 0) {
                        MapleStatEffect eff = bx.getEffect(bof);
                        if (Randomizer.nextInt(100) <= eff.getX()) {
                            chr.handleForceGain(oid, 31110008, eff.getZ());
                        }
                    }
                }
                if (skillid != 0) {
                    pPhysical = slea.readByte() > 0;
                    pID = slea.readInt();
                    pType = slea.readByte();
                    slea.skip(4);
                    pPos = slea.readPos();
                }
            }
            if ((GameConstants.isluminous(chr.getJob())) && (chr.getBuffedValue(MapleBuffStat.Black_Blessing) != null)) {
                if (chr.getBuffedValue(MapleBuffStat.Black_Blessing) != null) {
                    chr.applyBlackBlessingBuff(-1);
                }
                chr.checkLunarTide();
            }
            if (damage == -1) {
                fake = 4020002 + (chr.getJob() / 10 - 40) * 100000;
                if ((fake != 4120002) && (fake != 4220002)) {
                    fake = 4120002;
                }
                if ((type == -1) && (chr.getJob() == 122) && (attacker != null) && (chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -10) != null) && (chr.getTotalSkillLevel(1220006) > 0)) {
                    MapleStatEffect eff = SkillFactory.getSkill(1220006).getEffect(chr.getTotalSkillLevel(1220006));
                    attacker.applyStatus(chr, new MonsterStatusEffect(MonsterStatus.STUN, Integer.valueOf(1), 1220006, null, false), false, eff.getDuration(), true, eff);
                    fake = 1220006;
                }

                if (chr.getTotalSkillLevel(fake) > 0);
            } else if ((damage < -1) || (damage > 200000)) {
                c.getSession().write(CWvsContext.enableActions());
                return;
            }
            if ((chr.getStat().dodgeChance > 0) && (Randomizer.nextInt(100) < chr.getStat().dodgeChance)) {
                c.getSession().write(CField.EffectPacket.showForeignEffect(20));
                return;
            }
            if ((pPhysical) && (skillid == 1201007) && (chr.getTotalSkillLevel(1201007) > 0)) {
                damage -= pDMG;
                if (damage > 0) {
                    MapleStatEffect eff = SkillFactory.getSkill(1201007).getEffect(chr.getTotalSkillLevel(1201007));
                    long enemyDMG = Math.min(damage * (eff.getY() / 100), attacker.getMobMaxHp() / 2L);
                    if (enemyDMG > pDMG) {
                        enemyDMG = pDMG;
                    }
                    if (enemyDMG > 1000L) {
                        enemyDMG = 1000L;
                    }
                    attacker.damage(chr, enemyDMG, true, 1201007);
                } else {
                    damage = 1;
                }
            }

            Pair modify = chr.modifyDamageTaken(damage, attacker);
            damage = ((Double) modify.left).intValue();
            if (damage > 0) {


                if (chr.getBuffedValue(MapleBuffStat.MORPH) != null) {
                    chr.cancelMorphs();
                }

                boolean mpAttack = (chr.getBuffedValue(MapleBuffStat.MECH_CHANGE) != null) && (chr.getBuffSource(MapleBuffStat.MECH_CHANGE) != 35121005);
                if (chr.getBuffedValue(MapleBuffStat.MAGIC_GUARD) != null) {
                    int hploss = 0, mploss = 0;
                    if (isDeadlyAttack) {
                        if (stats.getHp() > 1) {
                            hploss = stats.getHp() - 1;
                        }
                        if (stats.getMp() > 1) {
                            mploss = stats.getMp() - 1;
                        }
                        if (chr.getBuffedValue(MapleBuffStat.INFINITY) != null) {
                            mploss = 0;
                        }
                        chr.addMPHP(-hploss, -mploss);
                        //} else if (mpattack > 0) {
                        //    chr.addMPHP(-damage, -mpattack);
                    } else {
                        mploss = (int) (damage * (chr.getBuffedValue(MapleBuffStat.MAGIC_GUARD).doubleValue() / 100.0)) + mpattack;
                        //  c.getPlayer().dropMessage(6, "Magicguard MP damage: " + mploss + " Guard value: " + (chr.getBuffedValue(MapleBuffStat.MAGIC_GUARD).doubleValue() / 100.0));
                        hploss = damage - mploss;
                        if (chr.getBuffedValue(MapleBuffStat.INFINITY) != null) {
                            mploss = 0;
                        } else if (mploss > stats.getMp()) {
                            mploss = stats.getMp();
                            hploss = damage - mploss + mpattack;
                        }
                        chr.addMPHP(-hploss, -mploss);
                    }
                }
                if (chr.getTotalSkillLevel(SkillFactory.getSkill(27000003)) > 0) {
                    int hploss = 0;
                    int mploss = 0;
                    if (isDeadlyAttack) {
                        if (stats.getHp() > 1) {
                            hploss = stats.getHp() - 1;
                        }
                        if (stats.getMp() > 1) {
                            mploss = stats.getMp() - 1;
                        }
                        chr.addMPHP(-hploss, -mploss);
                    } else {
                        double lost = SkillFactory.getSkill(27000003).getEffect(chr.getTotalSkillLevel(SkillFactory.getSkill(27000003))).getX() / 100.0D;
                        mploss = (int) (damage * lost + mpattack);
                        hploss = damage - mploss;
                        if (mploss > stats.getMp()) {
                            mploss = stats.getMp();
                            hploss = damage - mploss + mpattack;
                        }
                        chr.addMPHP(-hploss, -mploss);
                    }
                } else if (chr.getStat().mesoGuardMeso > 0.0D) {
                    int mesoloss = (int) (damage * (chr.getStat().mesoGuardMeso / 100.0D));
                    if (chr.getMeso() < mesoloss) {
                        chr.gainMeso(-chr.getMeso(), false);
                        chr.cancelBuffStats(new MapleBuffStat[]{MapleBuffStat.MESOGUARD});
                    } else {
                        chr.gainMeso(-mesoloss, false);
                    }
                    if ((isDeadlyAttack) && (stats.getMp() > 1)) {
                        mpattack = stats.getMp() - 1;
                    }
                    chr.addMPHP(-damage, -mpattack);
                } else if (isDeadlyAttack) {
                    chr.addMPHP(stats.getHp() > 1 ? -(stats.getHp() - 1) : 0, (stats.getMp() > 1) && (!mpAttack) ? -(stats.getMp() - 1) : 0);
                } else {
                    chr.addMPHP(-damage, mpAttack ? 0 : -mpattack);
                }

                if (!GameConstants.GMS) {
                    chr.handleBattleshipHP(-damage);
                }
                if ((chr.inPVP()) && (chr.getStat().getHPPercent() <= 20)) {
                    chr.getStat();
                    SkillFactory.getSkill(PlayerStats.getSkillByJob(93, chr.getJob())).getEffect(1).applyTo(chr);
                }
            }
            byte offset = 0;
            int offset_d = 0;
            if (slea.available() == 1L) {
                offset = slea.readByte();
                if ((offset == 1) && (slea.available() >= 4L)) {
                    offset_d = slea.readInt();
                }
                if ((offset < 0) || (offset > 2)) {
                    offset = 0;
                }
            }

            chr.getMap().broadcastMessage(chr, CField.damagePlayer(chr.getId(), type, damage, monsteridfrom, direction, skillid, pDMG, pPhysical, pID, pType, pPos, offset, offset_d, fake), false);
        } catch (ArrayIndexOutOfBoundsException dd) {
        }
    }

    public static final void AranCombo(MapleClient c, MapleCharacter chr, int toAdd) {
        if ((chr != null) && (chr.getJob() >= 2000) && (chr.getJob() <= 2112)) {
            short combo = chr.getCombo();
            long curr = System.currentTimeMillis();

            if ((combo > 0) && (curr - chr.getLastCombo() > 7000L)) {
                combo = 0;
            }
            combo = (short) Math.min(30000, combo + toAdd);
            chr.setLastCombo(curr);
            chr.setCombo(combo);

            c.getSession().write(CField.testCombo(combo));

            switch (combo) {
                case 10:
                case 20:
                case 30:
                case 40:
                case 50:
                case 60:
                case 70:
                case 80:
                case 90:
                case 100:
                    if (chr.getSkillLevel(21000000) >= combo / 10) {
                        SkillFactory.getSkill(21000000).getEffect(combo / 10).applyComboBuff(chr, combo);
                    }
                    break;
            }
        }
    }

    public static final void UseItemEffect(int itemId, MapleClient c, MapleCharacter chr) {
        Item toUse = chr.getInventory((itemId == 4290001) || (itemId == 4290000) ? MapleInventoryType.ETC : MapleInventoryType.CASH).findById(itemId);
        if ((toUse == null) || (toUse.getItemId() != itemId) || (toUse.getQuantity() < 1)) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        if (itemId != 5510000) {
            chr.setItemEffect(itemId);
        }
        chr.getMap().broadcastMessage(chr, CField.itemEffect(chr.getId(), itemId), false);
    }

    public static final void CancelItemEffect(int id, MapleCharacter chr) {
        chr.cancelEffect(MapleItemInformationProvider.getInstance().getItemEffect(-id), false, -1L);
    }

    public static final void CancelBuffHandler(int sourceid, MapleCharacter chr) {
        if ((chr == null) || (chr.getMap() == null)) {
            return;
        }
        Skill skill = SkillFactory.getSkill(sourceid);
        switch (sourceid) {
//               // case 33001001: //��Ծ� ���̵�
//              //  chr.send(CWvsContext.cancelJaguarRiding());
//              //  break;
            case 11101022: //��
                chr.send(CField.cancelPollingMoon());
                chr.getMap().broadcastMessage(chr, CField.cancelPollingMoon(), false);
                break;
            case 11111022: //����¡ ��
                chr.send(CField.cancelLizingSun());
                chr.getMap().broadcastMessage(chr, CField.cancelLizingSun(), false);
                break;
            case 4341052: 
            chr.getStat().setHp(0, chr);
           chr.updateSingleStat(MapleStat.HP, 0);
           chr.getClient().getSession().write(CWvsContext.enableActions());
                break;
            default: {
                if (skill.isChargeSkill()) {
                    chr.setKeyDownSkill_Time(0L);
                    chr.getMap().broadcastMessage(chr, CField.skillCancel(chr, sourceid), false);
                } else {
                    if (skill.getEffect(1).isMonsterRiding()) {
                        chr.cancelEffect(skill.getEffect(1), false, -1);
                        chr.getClient().getSession().write(CWvsContext.BuffPacket.cancelRiding());
                        if (chr.getBuffedValue(MapleBuffStat.SOARING) != null) {
                            chr.cancelEffectFromBuffStat(MapleBuffStat.SOARING);
                        }
                        
                    } else {
                        chr.cancelEffect(skill.getEffect(1), false, -1);
                    }
                }
                break;
            }
        }

    }

    public static final void CancelMech(LittleEndianAccessor slea, MapleCharacter chr) {
        if (chr == null) {
            return;
        }
        int sourceid = slea.readInt();
        if ((sourceid % 10000 < 1000) && (SkillFactory.getSkill(sourceid) == null)) {
            sourceid += 1000;
        }
        Skill skill = SkillFactory.getSkill(sourceid);
        if (skill == null) {
            return;
        }
        if (skill.isChargeSkill()) {
            chr.setKeyDownSkill_Time(0L);
            chr.getMap().broadcastMessage(chr, CField.skillCancel(chr, sourceid), false);
        } else {
            chr.cancelEffect(skill.getEffect(slea.readByte()), false, -1L);
        }
    }

    public static final void QuickSlot(LittleEndianAccessor slea, MapleCharacter chr) {
        if ((slea.available() == 32L) && (chr != null)) {
            StringBuilder ret = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                ret.append(slea.readInt()).append(",");
            }
            ret.deleteCharAt(ret.length() - 1);
            chr.getQuestNAdd(MapleQuest.getInstance(123000)).setCustomData(ret.toString());
        }
    }

    public static final void SkillEffect(LittleEndianAccessor slea, MapleCharacter chr) {
        slea.readInt(); //v143
        int skillId = slea.readInt();
        if (skillId >= 91000000 && skillId <= 100000000) {
            chr.getClient().getSession().write(CWvsContext.enableActions());
            return;
        }
        byte level = slea.readByte();
        short direction = slea.readShort();
        byte unk = slea.readByte();

        Skill skill = SkillFactory.getSkill(GameConstants.getLinkedAranSkill(skillId));
        if ((chr == null) || (skill == null) || (chr.getMap() == null)) {
            return;
        }
        int skilllevel_serv = chr.getTotalSkillLevel(skill);

        if ((skilllevel_serv > 0) && (skilllevel_serv == level) && ((skillId == 33101005) || (skill.isChargeSkill()))) {
            chr.setKeyDownSkill_Time(System.currentTimeMillis());
            if (skillId == 33101005) {
                chr.setLinkMid(slea.readInt(), 0);
            }
            chr.getMap().broadcastMessage(chr, CField.skillEffect(chr, skillId, level, direction, unk), false);
        }
    }

    public static final void SpecialMove(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        try {
            if ((chr == null) || (chr.hasBlockedInventory()) || (chr.getMap() == null) || (slea.available() < 9L)) {
                c.getSession().write(CWvsContext.enableActions());
                return;
            }
                             if (chr.getBuffedValue(MapleBuffStat.SOARING) != null) {
            c.getPlayer().dropMessage(6, "Can't attack while soaring.");
            c.getPlayer().getClient().getSession().write(CWvsContext.enableActions());
            return;
        }
            slea.skip(4);
            int skillid = slea.readInt();
            if (skillid >= 91000000 && skillid <= 100000000) {
                c.getSession().write(CWvsContext.enableActions());
                return;
            }
            if (skillid == 23111008) {
                skillid += Randomizer.nextInt(2);
            }
            if (skillid == 42101001) {
                skillid = 42100010;
            }
            int skillLevel = slea.readByte();
            Skill skill = SkillFactory.getSkill(skillid);
            if ((skill == null) || ((GameConstants.isAngel(skillid)) && (chr.getStat().equippedSummon % 10000 != skillid % 10000)) || ((chr.inPVP()) && (skill.isPVPDisabled()))) {
                c.getSession().write(CWvsContext.enableActions());
                return;
            }
            int levelCheckSkill = 0;
            if ((GameConstants.isPhantom(chr.getJob())) && (!MapleJob.getById(skillid / 10000).isPhantom())) {
                int skillJob = skillid / 10000;
                if (skillJob % 100 == 0) {
                    levelCheckSkill = 24001001;
                } else if (skillJob % 10 == 0) {
                    levelCheckSkill = 24101001;
                } else if (skillJob % 10 == 1) {
                    levelCheckSkill = 24111001;
                } else {
                    levelCheckSkill = 24121001;
                }
            }
            if (chr.getSkillLevel(skill) != skillLevel) {
                skill = SkillFactory.getSkill(GameConstants.getLinkedBuffSkill(skillid));
                if (SkillFactory.getSkill(skillid) == null) {
                    skillLevel = 1;
                } else {
                    skillLevel = chr.getSkillLevel(skill);
                }
            }
            if ((skillid != 42100010) && (!GameConstants.isAngel(skillid)) && (!GameConstants.kaiser(skillid / 10000)) && (levelCheckSkill == 0) && ((chr.getTotalSkillLevel(GameConstants.getLinkedAranSkill(skillid)) <= 0) || (chr.getTotalSkillLevel(GameConstants.getLinkedAranSkill(skillid)) != skillLevel))) {
                if ((!GameConstants.isMulungSkill(skillid)) && (!GameConstants.isPyramidSkill(skillid)) && (chr.getTotalSkillLevel(GameConstants.getLinkedAranSkill(skillid)) <= 0)) {
                    return;
                }
                if (GameConstants.isMulungSkill(skillid)) {
                    if (chr.getMapId() / 10000 != 92502) {
                        return;
                    }
                    if (chr.getMulungEnergy() < 10000) {
                        return;
                    }
                    chr.mulung_EnergyModify(false);
                } else if ((GameConstants.isPyramidSkill(skillid)) && (chr.getMapId() / 10000 != 92602) && (chr.getMapId() / 10000 != 92601)) {
                    return;
                }
            }



            skillLevel = chr.getTotalSkillLevel(GameConstants.getLinkedAranSkill(skillid));
            MapleStatEffect effect = chr.inPVP() ? skill.getPVPEffect(skillLevel) : skill.getEffect(skillLevel);
            if ((effect.isMPRecovery()) && (chr.getStat().getHp() < chr.getStat().getMaxHp() / 100 * 10)) {
                c.getPlayer().dropMessage(5, "You do not have the HP to use this skill.");
                c.getSession().write(CWvsContext.enableActions());
                return;
            }
            if ((effect.getCooldown(chr) > 0) && (!chr.isGM())) {
                if (chr.skillisCooling(skillid)) {
                    c.getSession().write(CWvsContext.enableActions());
                    return;
                }

                c.getSession().write(CField.skillCooldown(skillid, effect.getCooldown(chr)));
                chr.addCooldown(skillid, System.currentTimeMillis(), effect.getCooldown(chr) * 1000);
            }
            int mobID;
            MapleMonster mob;
            switch (skillid) {
                case 1121001:
                case 1221001:
                case 1321001:
                case 9001020:
                case 9101020:
                case 31111003:
                    byte number_of_mobs = slea.readByte();
                    slea.skip(3);
                    for (int i = 0; i < number_of_mobs; i++) {
                        int mobId = slea.readInt();
                        mob = chr.getMap().getMonsterByOid(mobId);
                        if (mob != null) {
                            mob.switchController(chr, mob.isControllerHasAggro());
                            mob.applyStatus(chr, new MonsterStatusEffect(MonsterStatus.STUN, Integer.valueOf(1), skillid, null, false), false, effect.getDuration(), true, effect);
                        }
                    }
                    chr.getMap().broadcastMessage(chr, CField.EffectPacket.showBuffeffect(chr.getId(), skillid, 1, chr.getLevel(), skillLevel, slea.readByte()), chr.getTruePosition());
                    c.getSession().write(CWvsContext.enableActions());
                    break;
                case 30001061:
                    mobID = slea.readInt();
                    mob = chr.getMap().getMonsterByOid(mobID);
                    if (mob != null) {
                        boolean success = (mob.getHp() <= mob.getMobMaxHp() / 2L) && (mob.getId() >= 9304000) && (mob.getId() < 9305000);
                        chr.getMap().broadcastMessage(chr, CField.EffectPacket.showBuffeffect(chr.getId(), skillid, 1, chr.getLevel(), skillLevel, (byte) (success ? 1 : 0)), chr.getTruePosition());
                        if (success) {
                            chr.getQuestNAdd(MapleQuest.getInstance(111112)).setCustomData(String.valueOf((mob.getId() - 9303999) * 10));
                            chr.getMap().killMonster(mob, chr, true, false, (byte) 1);
                            chr.cancelEffectFromBuffStat(MapleBuffStat.MONSTER_RIDING);
                            c.getSession().write(CWvsContext.updateJaguar(chr));
                        } else {
                            chr.dropMessage(5, "The monster has too much physical strength, so you cannot catch it.");
                        }
                    }
                    c.getSession().write(CWvsContext.enableActions());
                    break;
                case 30001062:
                    chr.dropMessage(5, "No monsters can be summoned. Capture a monster first.");
                    c.getSession().write(CWvsContext.enableActions());
                    break;
                case 33101005:
                    mobID = chr.getFirstLinkMid();
                    mob = chr.getMap().getMonsterByOid(mobID);
                    chr.setKeyDownSkill_Time(0L);
                    chr.getMap().broadcastMessage(chr, CField.skillCancel(chr, skillid), false);
                    if (mob != null) {
                        boolean success = (mob.getStats().getLevel() < chr.getLevel()) && (mob.getId() < 9000000) && (!mob.getStats().isBoss());
                        if (success) {
                            chr.getMap().broadcastMessage(MobPacket.suckMonster(mob.getObjectId(), chr.getId()));
                            chr.getMap().killMonster(mob, chr, false, false, (byte) -1);
                        } else {
                            chr.dropMessage(5, "The monster has too much physical strength, so you cannot catch it.");
                        }
                    } else {
                        chr.dropMessage(5, "No monster was sucked. The skill failed.");
                    }
                    c.getSession().write(CWvsContext.enableActions());
                    break;
                case 4341003:
                    chr.setKeyDownSkill_Time(0L);
                    chr.getMap().broadcastMessage(chr, CField.skillCancel(chr, skillid), false);
                    break;
                case 36121054:
                    chr.setxenoncombo((short) 20);
                    c.getSession().write(CWvsContext.enableActions());
                    break;
                case 65111100: //�ҿ��Ŀ
                    int xy1 = slea.readShort();
                    int xy2 = slea.readShort();
                    int soulnum = slea.readByte();
                    int scheck = 0;
                    int scheck2 = 0;
                    if (soulnum == 1) {
                        scheck = slea.readInt();
                    } else if (soulnum == 4) {
                        scheck = slea.readInt();
                        scheck2 = slea.readInt();
                    }
                    c.getSession().write(CField.SoulSeeker(chr, skillid, soulnum, scheck, scheck2));
                    //   c.getSession().write(CField.unlockSkill());
                    // c.getSession().write(CField.RechargeEffect());
                    break;
                case 31221001:
                case 36001005: {
                    List<Integer> moblist = new ArrayList<Integer>();
                    byte count = slea.readByte();
                    for (byte i = 1; i <= count; i++) {
                        moblist.add(slea.readInt());
                    }
                    if (skillid == 31221001 || skillid == 2121052) {

                        c.getSession().write(CField.ShieldChacing(chr.getId(), moblist, skillid));


                    } else if (skillid == 36001005) {

                        for (int i = 0; i < 3; i++) {
                            c.getSession().write(CField.PinPointRocket(chr.getId(), moblist));
                        }
                    }
                    break;
                }
                case 4341052:
                                      
                                        EnumMap<MapleBuffStat, Integer> statups;
                         statups = new EnumMap(MapleBuffStat.class);
                statups.put(MapleBuffStat.ASURA, 1);
                c.getPlayer().getClient().getSession().write(CWvsContext.BuffPacket.giveBuff(4341052, 90000, statups, SkillFactory.getSkill(4341052).getEffect(1)));
                           c.getPlayer().getClient().getSession().write(CWvsContext.BuffPacket.giveForeignBuff(chr.getId(), statups, SkillFactory.getSkill(4341052).getEffect(1)));
                chr.getClient().getSession().write(CWvsContext.enableActions());
                    
                    break;
                case 2121054:
                case 2121052:
                    ArrayList<MapleMapObject> surroundingMonsters = new ArrayList<MapleMapObject>();


                    if (skillid == 2121054) {
                        for (MapleMapObject m : c.getPlayer().getMap().getMapObjectsInRange(c.getPlayer().getPosition(), 14990.0, Arrays.asList(MapleMapObjectType.MONSTER))) {

                            surroundingMonsters.add(m);

                        }
                    }
                    if (skillid == 2121052) {
                        for (MapleMapObject m : c.getPlayer().getMap().getMapObjectsInRange(c.getPlayer().getPosition(), 14990.0, Arrays.asList(MapleMapObjectType.MONSTER))) {

                            surroundingMonsters.add(m);

                        }
                    }

                    int damage = 100000 * c.getPlayer().getReborns();

                    if (c.getPlayer().getReborns() == 0) {
                        damage = 50000;
                    }



                    for (MapleMapObject e : surroundingMonsters) {

                        c.getPlayer().getMap().broadcastMessage(MobPacket.damageMonster(e.getObjectId(), damage));
                        MapleMonster bleh = (MapleMonster) e;
                        c.getPlayer().getClient().getSession().write(MobPacket.showMonsterHP(e.getObjectId(), (int) Math.ceil((bleh.getHp() * 100.0) / bleh.getMobMaxHp())));
                        bleh.damage(c.getPlayer(), damage, false);

                    }

                    c.getSession().write(CWvsContext.enableActions());
                    break;

                case 36111008:
                    if (chr.getxenoncombo() + 10 >= 20) {
                        chr.setxenoncombo((short) 20);
                        c.getSession().write(CWvsContext.enableActions());
                        break;
                    }
                    chr.setxenoncombo((short) (chr.getxenoncombo() + 10));
                    c.getSession().write(CWvsContext.enableActions());
                    break;
                case 3101004:
                case 3201004:
                    System.out.print("handing soul arrow");
                    effect.applyTo(c.getPlayer());
                    break;
                default:
                    Point pos = null;
                    if ((slea.available() == 5L) || (slea.available() == 7L)) {
                        pos = slea.readPos();
                    }
                    if (effect.isMagicDoor()) {
                        if (!FieldLimitType.MysticDoor.check(chr.getMap().getFieldLimit())) {
                            effect.applyTo(c.getPlayer(), pos);
                        } else {
                            c.getSession().write(CWvsContext.enableActions());
                        }
                    } else {
                        final int mountid = MapleStatEffect.parseMountInfo(c.getPlayer(), skill.getId());
                        if (mountid != 0 && mountid != GameConstants.getMountItem(skill.getId(), c.getPlayer()) && !c.getPlayer().isIntern() && c.getPlayer().getBuffedValue(MapleBuffStat.MONSTER_RIDING) == null && c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -122) == null) {
                            if (!GameConstants.isMountItemAvailable(mountid, c.getPlayer().getJob())) {
                                c.getSession().write(CWvsContext.enableActions());
                                return;
                            }
                        }
                        effect.applyTo(c.getPlayer(), pos);
                    }
                    break;
            }
        } catch (NullPointerException npe) {
        }
    }

    public static void closeRangeAttack(LittleEndianAccessor slea, MapleClient c, final MapleCharacter chr, final boolean energy) {
  
        if ((chr == null) || ((energy) && (chr.getBuffedValue(MapleBuffStat.ENERGY_CHARGE) == null) && (chr.getBuffedValue(MapleBuffStat.ASURA) == null) && (chr.getBuffedValue(MapleBuffStat.BODY_PRESSURE) == null) && (chr.getBuffedValue(MapleBuffStat.DARK_AURA) == null) && (chr.getBuffedValue(MapleBuffStat.TORNADO) == null) && (chr.getBuffedValue(MapleBuffStat.SUMMON) == null) && (chr.getBuffedValue(MapleBuffStat.RAINING_MINES) == null) && (chr.getBuffedValue(MapleBuffStat.TELEPORT_MASTERY) == null))) {
        c.getPlayer().getClient().getSession().write(CWvsContext.enableActions());
            return;
        }
        if (chr.getBuffedValue(MapleBuffStat.SOARING) != null) {
            c.getPlayer().dropMessage(6, "Can't attack while soaring.");
            c.getPlayer().getClient().getSession().write(CWvsContext.enableActions());
            return;
        }
      
        if ((chr.hasBlockedInventory()) || (chr.getMap() == null)) {
                   c.getPlayer().getClient().getSession().write(CWvsContext.enableActions());
            return;
        }
     
        try {
        
            AttackInfo attack = DamageParse.parseDmgM(slea, chr);
            if (attack == null) {
                c.getSession().write(CWvsContext.enableActions());
                return;
            }
            final boolean mirror = chr.getBuffedValue(MapleBuffStat.SHADOWPARTNER) != null;
            double maxdamage = chr.getStat().getCurrentMaxBaseDamage();
            Item shield = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -10);
            int attackCount = (shield != null) && (shield.getItemId() / 10000 == 134) ? 2 : 1;
            int skillLevel = 0;
            MapleStatEffect effect = null;
            Skill skill = null;

            if (attack.skill != 0) {
                skill = SkillFactory.getSkill(GameConstants.getLinkedAranSkill(attack.skill));
                if ((skill == null) || ((GameConstants.isAngel(attack.skill)) && (chr.getStat().equippedSummon % 10000 != attack.skill % 10000))) {
                    c.getSession().write(CWvsContext.enableActions());
                    return;
                }
                skillLevel = chr.getTotalSkillLevel(skill);
                effect = attack.getAttackEffect(chr, skillLevel, skill);
                if (effect == null) {
                    return;
                }

                maxdamage *= (effect.getDamage() + chr.getStat().getDamageIncrease(attack.skill)) / 100.0D;
                attackCount = effect.getAttackCount();

                if ((effect.getCooldown(chr) > 0) && (!chr.isGM()) && (!energy)) {
                    if (chr.skillisCooling(attack.skill)) {
                        c.getSession().write(CWvsContext.enableActions());
                        return;
                    }
                    c.getSession().write(CField.skillCooldown(attack.skill, effect.getCooldown(chr)));
                    chr.addCooldown(attack.skill, System.currentTimeMillis(), effect.getCooldown(chr) * 1000);
                }
            }
            attack = DamageParse.Modify_AttackCrit(attack, chr, 1, effect);
            attackCount *= (mirror ? 2 : 1);
            if (!energy) {
         
                int numFinisherOrbs = 0;
                Integer comboBuff = chr.getBuffedValue(MapleBuffStat.COMBO);

                if (isFinisher(attack.skill) > 0) {
                    if (comboBuff != null) {
                        numFinisherOrbs = comboBuff.intValue() - 1;
                    }
                    if (numFinisherOrbs <= 0) {
                        return;
                    }
                    chr.handleOrbconsume(isFinisher(attack.skill));
                    if (!GameConstants.GMS) {
                        maxdamage *= numFinisherOrbs;
                    }
                }
            }
            chr.checkFollow();
            if (!chr.isHidden()) {
                chr.getMap().broadcastMessage(chr, CField.closeRangeAttack(chr.getId(), attack.tbyte, attack.skill, skillLevel, attack.display, attack.speed, attack.allDamage, energy, chr.getLevel(), chr.getStat().passive_mastery(), attack.animation, attack.charge), chr.getTruePosition());
            } else {
                chr.getMap().broadcastGMMessage(chr, CField.closeRangeAttack(chr.getId(), attack.tbyte, attack.skill, skillLevel, attack.display, attack.speed, attack.allDamage, energy, chr.getLevel(), chr.getStat().passive_mastery(), attack.animation, attack.charge), false);
            }
            if (chr.getBuffedValue(MapleBuffStat.moon1) != null) {

                xx++;
                if (xx % 2 == 0) {
                    SkillFactory.getSkill(11111022).getEffect(30).applyTo(c.getPlayer());

                } else {
                    SkillFactory.getSkill(11101022).getEffect(30).applyTo(c.getPlayer());

                }
            }
            DamageParse.applyAttack(attack, skill, c.getPlayer(), attackCount, maxdamage, effect, mirror ? AttackType.NON_RANGED_WITH_MIRROR : AttackType.NON_RANGED);
            WeakReference<MapleCharacter>[] clones = chr.getClones();
            for (int i = 0; i < clones.length; i++) {
                if (clones[i].get() != null) {
                    final MapleCharacter clone = clones[i].get();

                    // CloneTimer.getInstance().schedule(new Runnable() {

                    // public void run() {
                    if (!chr.isHidden()) {
                        chr.getMap().broadcastMessage(clone, CField.closeRangeAttack(clone.getId(), attack.tbyte, attack.skill, skillLevel, attack.display, attack.speed, attack.allDamage, energy, chr.getLevel(), chr.getStat().passive_mastery(), attack.animation, attack.charge), clone.getTruePosition());
                    } else {
                        chr.getMap().broadcastGMMessage(clone, CField.closeRangeAttack(clone.getId(), attack.tbyte, attack.skill, skillLevel, attack.display, attack.speed, attack.allDamage, energy, chr.getLevel(), chr.getStat().passive_mastery(), attack.animation, attack.charge), false);
                    }
                    DamageParse.applyAttack(attack, skill, c.getPlayer(), attackCount, maxdamage, effect, mirror ? AttackType.NON_RANGED_WITH_MIRROR : AttackType.NON_RANGED);

                }
            }
        } catch (ArrayIndexOutOfBoundsException dd) {
        }
    }

    public static void rangedAttack(LittleEndianAccessor slea, MapleClient c, final MapleCharacter chr) {
        try {
            
        if (chr == null) {
              c.getPlayer().getClient().getSession().write(CWvsContext.enableActions());
            return;
        }
        if ((chr.hasBlockedInventory()) || (chr.getMap() == null)) {
              c.getPlayer().getClient().getSession().write(CWvsContext.enableActions());
            return;
        }
                if (chr.getBuffedValue(MapleBuffStat.SOARING) != null) {
            c.getPlayer().dropMessage(6, "Can't attack while soaring.");
            c.getPlayer().getClient().getSession().write(CWvsContext.enableActions());
            return;
        }
      
        AttackInfo attack = DamageParse.parseDmgR(slea, chr);
        if (attack == null) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        int bulletCount = 1;
        int skillLevel = 0;
        MapleStatEffect effect = null;
        Skill skill = null;
        boolean AOE = attack.skill == 4111004;
        boolean noBullet = ((chr.getJob() >= 3500) && (chr.getJob() <= 3512)) || (GameConstants.isCannon(chr.getJob())) || (GameConstants.angelic(chr.getJob())) || (GameConstants.xenon(chr.getJob())) || (GameConstants.demon_AV(chr.getJob())) || (GameConstants.kaiser(chr.getJob())) || (GameConstants.isJett(chr.getJob())) || (GameConstants.isPhantom(chr.getJob())) || (GameConstants.isMercedes(chr.getJob())) || (chr.getJob() >= 300) && (chr.getJob() <= 322);
        if (attack.skill != 0) {
            skill = SkillFactory.getSkill(GameConstants.getLinkedAranSkill(attack.skill));
            if ((skill == null) || ((GameConstants.isAngel(attack.skill)) && (chr.getStat().equippedSummon % 10000 != attack.skill % 10000))) {
                c.getSession().write(CWvsContext.enableActions());
                return;
            }
            skillLevel = chr.getTotalSkillLevel(skill);
            effect = attack.getAttackEffect(chr, skillLevel, skill);
            if (effect == null) {
                return;
            }
            switch (attack.skill) {
                case 1077:
                case 1078:
                case 1079:
                case 11077:
                case 11078:
                case 11079:
                case 4111013:
                case 4121003:
                case 4221003:
                case 5121002:
                case 5121013:
                case 5121016:
                case 5201001:
                case 5211008:
                case 5221013:
                case 5221016:
                case 5221017:
                case 5321001:
                case 5711000:
                case 5721001:
                case 5721003:
                case 5721004:
                case 5721006:
                case 5721007:
                case 5921002:
                case 11101004:
                case 13101005:
                case 13111007:
                case 14101006:
                case 14111008:
                case 15111007:
                case 21000004:
                case 21100004:
                case 21100007:
                case 21110004:
                case 21110011:
                case 21120006:
                case 33101002:
                case 33101007:
                case 33121001:
                case 33121002:
                case 13101020:
                case 51001004:
                case 51111007:
                case 42121002:
                case 51121008:
                    AOE = true;
                    bulletCount = effect.getAttackCount();
                    break;
                case 35111004:
                case 35121005:
                                    case 13001020:
                case 35121013:
                    AOE = true;
                    bulletCount = 6;
                    break;
                default:
                    bulletCount = effect.getBulletCount();
            }

            if ((noBullet) && (effect.getBulletCount() < effect.getAttackCount())) {
                bulletCount = effect.getAttackCount();
            }
            if ((noBullet) && (effect.getBulletCount() < effect.getAttackCount())) {
                bulletCount = effect.getAttackCount();
            }
            if ((effect.getCooldown(chr) > 0) && (!chr.isGM()) && (((attack.skill != 35111004) && (attack.skill != 35121013)) || (chr.getBuffSource(MapleBuffStat.MECH_CHANGE) != attack.skill))) {
                if (chr.skillisCooling(attack.skill)) {
                    c.getSession().write(CWvsContext.enableActions());
                    return;
                }
                c.getSession().write(CField.skillCooldown(attack.skill, effect.getCooldown(chr)));
                chr.addCooldown(attack.skill, System.currentTimeMillis(), effect.getCooldown(chr) * 1000);
            }
        }


        attack = DamageParse.Modify_AttackCrit(attack, chr, 2, effect);
        Integer ShadowPartner = chr.getBuffedValue(MapleBuffStat.SHADOWPARTNER);
        if (ShadowPartner != null) {
            bulletCount *= 2;
        }
        int projectile = 0;
        int visProjectile = 0;
        if ((!AOE) && (chr.getBuffedValue(MapleBuffStat.SOULARROW) == null) && (!noBullet)) {
            Item ipp = chr.getInventory(MapleInventoryType.USE).getItem((short) attack.slot);
            if (ipp == null) {
                return;
            }
            projectile = ipp.getItemId();

            if (attack.csstar > 0) {
                if (chr.getInventory(MapleInventoryType.CASH).getItem((short) attack.csstar) == null) {
                    return;
                }
                visProjectile = chr.getInventory(MapleInventoryType.CASH).getItem((short) attack.csstar).getItemId();
            } else {
                visProjectile = projectile;
            }

            if (chr.getBuffedValue(MapleBuffStat.SPIRIT_CLAW) == null) {
                int bulletConsume = bulletCount;
                if ((effect != null) && (effect.getBulletConsume() != 0)) {
                    bulletConsume = effect.getBulletConsume() * (ShadowPartner != null ? 2 : 1);
                }
                if ((chr.getJob() == 412) && (bulletConsume > 0) && (ipp.getQuantity() < MapleItemInformationProvider.getInstance().getSlotMax(projectile))) {
                    Skill expert = SkillFactory.getSkill(4120010);
                    if (chr.getTotalSkillLevel(expert) > 0) {
                        MapleStatEffect eff = expert.getEffect(chr.getTotalSkillLevel(expert));
                        if (eff.makeChanceResult()) {
                            ipp.setQuantity((short) (ipp.getQuantity() + 1));
                            c.getSession().write(CWvsContext.InventoryPacket.updateInventorySlot(MapleInventoryType.USE, ipp, false));
                            bulletConsume = 0;
                            c.getSession().write(CWvsContext.InventoryPacket.getInventoryStatus());
                        }
                    }
                }
                if ((bulletConsume > 0) && (!MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, projectile, bulletConsume, false, true))) {
                    chr.dropMessage(5, "You do not have enough arrows/bullets/stars.");
                    return;
                }
            }
        } else if ((chr.getJob() >= 3500) && (chr.getJob() <= 3512)) {
            visProjectile = 2333000;
        } else if (GameConstants.isCannon(chr.getJob())) {
            visProjectile = 2333001;
        }

        int projectileWatk = 0;
        if (projectile != 0) {
            projectileWatk = MapleItemInformationProvider.getInstance().getWatkForProjectile(projectile);
        }
        PlayerStats statst = chr.getStat();
        double basedamage;
        switch (attack.skill) {
            case 4001344:
            case 4121007:
            case 14001004:
            case 14111005:
                basedamage = Math.max(statst.getCurrentMaxBaseDamage(), statst.getTotalLuk() * 5.0F * (statst.getTotalWatk() + projectileWatk) / 100.0F);
                break;
            case 4111004:
                basedamage = 53000.0D;
                break;
            default:
                basedamage = statst.getCurrentMaxBaseDamage();
                switch (attack.skill) {
                    case 3101005:
                        basedamage *= effect.getX() / 100.0D;
                }
                break;
        }

        if (effect != null) {
            basedamage *= (effect.getDamage() + statst.getDamageIncrease(attack.skill)) / 100.0D;

            long money = effect.getMoneyCon();
            if (money != 0L) {
                if (money > chr.getMeso()) {
                    money = chr.getMeso();
                }
                chr.gainMeso(-money, false);
            }
        }
        chr.checkFollow();
        if (!chr.isHidden()) {
            if (attack.skill == 3211006) {
                chr.getMap().broadcastMessage(chr, CField.strafeAttack(chr.getId(), attack.tbyte, attack.skill, skillLevel, attack.display, attack.speed, visProjectile, attack.allDamage, attack.position, chr.getLevel(), chr.getStat().passive_mastery(), attack.animation, chr.getTotalSkillLevel(3220010)), chr.getTruePosition());
            } else {
                chr.getMap().broadcastMessage(chr, CField.rangedAttack(chr.getId(), attack.tbyte, attack.skill, skillLevel, attack.display, attack.speed, visProjectile, attack.allDamage, attack.position, chr.getLevel(), chr.getStat().passive_mastery(), attack.animation), chr.getTruePosition());
            }
        } else if (attack.skill == 3211006) {
            chr.getMap().broadcastGMMessage(chr, CField.strafeAttack(chr.getId(), attack.tbyte, attack.skill, skillLevel, attack.display, attack.speed, visProjectile, attack.allDamage, attack.position, chr.getLevel(), chr.getStat().passive_mastery(), attack.animation, chr.getTotalSkillLevel(3220010)), false);
        } else {
            chr.getMap().broadcastGMMessage(chr, CField.rangedAttack(chr.getId(), attack.tbyte, attack.skill, skillLevel, attack.display, attack.speed, visProjectile, attack.allDamage, attack.position, chr.getLevel(), chr.getStat().passive_mastery(), attack.animation), false);
        }

        DamageParse.applyAttack(attack, skill, chr, bulletCount, basedamage, effect, ShadowPartner != null ? AttackType.RANGED_WITH_SHADOWPARTNER : AttackType.RANGED);
        WeakReference<MapleCharacter>[] clones = chr.getClones();
        for (int i = 0; i < clones.length; i++) {
            if (clones[i].get() != null) {
                final MapleCharacter clone = clones[i].get();
                if (!chr.isHidden()) {
                    if (attack.skill == 3211006) {
                        chr.getMap().broadcastMessage(clone, CField.strafeAttack(clone.getId(), attack.tbyte, attack.skill, skillLevel, attack.display, attack.speed, visProjectile, attack.allDamage, attack.position, chr.getLevel(), chr.getStat().passive_mastery(), attack.animation, chr.getTotalSkillLevel(3220010)), chr.getTruePosition());
                    } else {
                        chr.getMap().broadcastMessage(clone, CField.rangedAttack(clone.getId(), attack.tbyte, attack.skill, skillLevel, attack.display, attack.speed, visProjectile, attack.allDamage, attack.position, chr.getLevel(), chr.getStat().passive_mastery(), attack.animation), chr.getTruePosition());
                    }
                } else if (attack.skill == 3211006) {
                    chr.getMap().broadcastGMMessage(clone, CField.strafeAttack(clone.getId(), attack.tbyte, attack.skill, skillLevel, attack.display, attack.speed, visProjectile, attack.allDamage, attack.position, chr.getLevel(), chr.getStat().passive_mastery(), attack.animation, chr.getTotalSkillLevel(3220010)), false);
                } else {
                    chr.getMap().broadcastGMMessage(clone, CField.rangedAttack(clone.getId(), attack.tbyte, attack.skill, skillLevel, attack.display, attack.speed, visProjectile, attack.allDamage, attack.position, chr.getLevel(), chr.getStat().passive_mastery(), attack.animation), false);
                }

                DamageParse.applyAttack(attack, skill, chr, bulletCount, basedamage, effect, ShadowPartner != null ? AttackType.RANGED_WITH_SHADOWPARTNER : AttackType.RANGED);


            }
        
            
        } 
            
        } catch(NullPointerException npe) {
            
        }
    }

    public static final void MagicDamage(LittleEndianAccessor slea, MapleClient c, final MapleCharacter chr) {
        if ((chr == null) || (chr.hasBlockedInventory()) || (chr.getMap() == null)) {
                       c.getPlayer().getClient().getSession().write(CWvsContext.enableActions());
            return;
        }
                  if (chr.getBuffedValue(MapleBuffStat.SOARING) != null) {
            c.getPlayer().dropMessage(6, "Can't attack while soaring.");
            c.getPlayer().getClient().getSession().write(CWvsContext.enableActions());
            return;
        }
      
        int bulletCount = 1;
        AttackInfo attack = DamageParse.parseDmgMa(slea, chr);
        if (attack == null) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        Skill skill = SkillFactory.getSkill(GameConstants.getLinkedAranSkill(attack.skill));
        if ((skill == null) || ((GameConstants.isAngel(attack.skill)) && (chr.getStat().equippedSummon % 10000 != attack.skill % 10000))) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        int skillLevel = chr.getTotalSkillLevel(skill);
        MapleStatEffect effect = attack.getAttackEffect(chr, skillLevel, skill);
        if (effect == null) {
                       c.getPlayer().getClient().getSession().write(CWvsContext.enableActions());
            return;
        }
        attack = DamageParse.Modify_AttackCrit(attack, chr, 3, effect);
        double maxdamage = chr.getStat().getCurrentMaxBaseDamage() * (effect.getDamage() + chr.getStat().getDamageIncrease(attack.skill)) / 100.0D;
        if (GameConstants.isPyramidSkill(attack.skill)) {
            maxdamage = 1.0D;
        } else if ((GameConstants.isBeginnerJob(skill.getId() / 10000)) && (skill.getId() % 10000 == 1000)) {
            maxdamage = 40.0D;
        }
        if ((effect.getCooldown(chr) > 0) && (!chr.isGM())) {
            if (chr.skillisCooling(attack.skill)) {
                c.getSession().write(CWvsContext.enableActions());
                return;
            }
            c.getSession().write(CField.skillCooldown(attack.skill, effect.getCooldown(chr)));
            chr.addCooldown(attack.skill, System.currentTimeMillis(), effect.getCooldown(chr) * 1000);
        }
        chr.checkFollow();
        if (!chr.isHidden()) {
            chr.getMap().broadcastMessage(chr, CField.magicAttack(chr.getId(), attack.tbyte, attack.skill, skillLevel, attack.display, attack.speed, attack.allDamage, attack.charge, chr.getLevel(), attack.animation), chr.getTruePosition());
        } else {
            chr.getMap().broadcastGMMessage(chr, CField.magicAttack(chr.getId(), attack.tbyte, attack.skill, skillLevel, attack.display, attack.speed, attack.allDamage, attack.charge, chr.getLevel(), attack.animation), false);
        }
        switch (attack.skill) {
            case 27101100: // ���ǵ� ����
            case 27101202: // ���̵� ������
            case 27111100: // ����Ʈ�� ����Ʈ
            case 27111202: // �콺�Ǿ�
            case 27121100: // ����Ʈ ���÷���
            case 27121202: // ����Į����
            case 2121006:
            case 2221003:
            case 2221006:
            case 32111003:
            case 2221007:
            //   case 2321054:
            case 2221012:
            case 2321007:
            case 2111003: // ������ �̽�Ʈ
            case 2121003: // �̽�Ʈ �̷���

            case 22181002: //��ũ����
                bulletCount = effect.getAttackCount();
                DamageParse.applyAttack(attack, skill, chr, bulletCount, maxdamage, effect, AttackType.RANGED);
                break;
            default:
                DamageParse.applyAttackMagic(attack, skill, c.getPlayer(), effect, maxdamage);
                break;
        }
        WeakReference<MapleCharacter>[] clones = chr.getClones();
        for (int i = 0; i < clones.length; i++) {
            if (clones[i].get() != null) {
                final MapleCharacter clone = clones[i].get();


                if (!chr.isHidden()) {
                    chr.getMap().broadcastMessage(clone, CField.magicAttack(clone.getId(), attack.tbyte, attack.skill, skillLevel, attack.display, attack.speed, attack.allDamage, attack.charge, chr.getLevel(), attack.animation), chr.getTruePosition());
                } else {
                    chr.getMap().broadcastGMMessage(clone, CField.magicAttack(clone.getId(), attack.tbyte, attack.skill, skillLevel, attack.display, attack.speed, attack.allDamage, attack.charge, chr.getLevel(), attack.animation), false);
                }
                switch (attack.skill) {
                    case 27101100: // ���ǵ� ����
                    case 27101202: // ���̵� ������
                    case 27111100: // ����Ʈ�� ����Ʈ
                    case 27111202: // �콺�Ǿ�
                    case 27121100: // ����Ʈ ���÷���
                    case 27121202: // ����Į����
                    case 2121006:
                    case 2221003:
                    case 2221006:
                    case 32111003:
                    case 2221007:
                    //   case 2321054:
                    case 2221012:
                    case 2321007:
                    case 2111003: // ������ �̽�Ʈ
                    case 2121003: // �̽�Ʈ �̷���

                    case 22181002: //��ũ����
                        bulletCount = effect.getAttackCount();
                        DamageParse.applyAttack(attack, skill, chr, bulletCount, maxdamage, effect, AttackType.RANGED);
                        break;
                    default:
                        DamageParse.applyAttackMagic(attack, skill, c.getPlayer(), effect, maxdamage);
                        break;
                }

            }
        }
    }

    public static final void DropMeso(int meso, MapleCharacter chr) {
        if ((!chr.isAlive()) || (meso < 10) || (meso > 50000) || (meso > chr.getMeso())) {
            chr.getClient().getSession().write(CWvsContext.enableActions());
            return;
        }
        chr.gainMeso(-meso, false, true);
        chr.getMap().spawnMesoDrop(meso, chr.getTruePosition(), chr, chr, true, (byte) 0);

    }

    public static final void ChangeAndroidEmotion(int emote, MapleCharacter chr) {
        if ((emote > 0) && (chr != null) && (chr.getMap() != null) && (!chr.isHidden()) && (emote <= 17) && (chr.getAndroid() != null)) {
            chr.getMap().broadcastMessage(CField.showAndroidEmotion(chr.getId(), emote));
        }
    }

    public static void MoveAndroid(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        slea.skip(12);
        List res = MovementParse.parseMovement(slea, 3);

        if ((res != null) && (chr != null) && (!res.isEmpty()) && (chr.getMap() != null) && (chr.getAndroid() != null)) {
            Point pos = new Point(chr.getAndroid().getPos());
            chr.getAndroid().updatePosition(res);
            chr.getMap().broadcastMessage(chr, CField.moveAndroid(chr.getId(), pos, res), false);
        }
    }

    public static void MoveHaku(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        slea.skip(17);
        List res = MovementParse.parseMovement(slea, 6);

        if ((res != null) && (chr != null) && (!res.isEmpty()) && (chr.getMap() != null) && (chr.getHaku() != null)) {
            Point pos = new Point(chr.getHaku().getPosition());
            chr.getHaku().updatePosition(res);
            chr.getMap().broadcastMessage(chr, CField.moveHaku(chr.getId(), chr.getHaku().getObjectId(), pos, res), false);
        }
    }

    public static void ChangeHaku(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        int oid = slea.readInt();
        if (chr.getHaku() != null) {
            chr.getHaku().sendstats();
            chr.getMap().broadcastMessage(chr, CField.spawnHaku_change0(chr.getId()), true);
            chr.getMap().broadcastMessage(chr, CField.spawnHaku_change1(chr.getHaku()), true);
            chr.getMap().broadcastMessage(chr, CField.spawnHaku_bianshen(chr.getId(), oid, chr.getHaku().getstats()), true);
        }
    }

    public static final void ChangeEmotion(final int emote, MapleCharacter chr) {
        if (emote > 7) {
            int emoteid = 5159992 + emote;
            MapleInventoryType type = GameConstants.getInventoryType(emoteid);
            if (chr.getInventory(type).findById(emoteid) == null) {

                return;
            }
        }
        if ((emote > 0) && (chr != null) && (chr.getMap() != null) && (!chr.isHidden())) {
            chr.getMap().broadcastMessage(chr, CField.facialExpression(chr, emote), false);
            WeakReference<MapleCharacter>[] clones = chr.getClones();
            for (int i = 0; i < clones.length; i++) {
                if (clones[i].get() != null) {
                    final MapleCharacter clone = clones[i].get();
                    CloneTimer.getInstance().schedule(new Runnable() {
                        @Override
                        public void run() {
                            clone.getMap().broadcastMessage(CField.facialExpression(clone, emote));
                        }
                    }, 500 * i + 500);
                }
            }
        }
    }

    public static final void Heal(LittleEndianAccessor slea, MapleCharacter chr) {
        if (chr == null) {
            return;
        }
        slea.readInt();
        if (slea.available() >= 8L) {
            slea.skip((slea.available() >= 12L) && (GameConstants.GMS) ? 8 : 4);
        }
        int healHP = slea.readShort();
        int healMP = slea.readShort();

        PlayerStats stats = chr.getStat();

        if (stats.getHp() <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        if ((healHP != 0) && (chr.canHP(now + 1000L))) {
            if (healHP > stats.getHealHP()) {
                healHP = (int) stats.getHealHP();
            }
            chr.addHP(healHP);
        }
        if ((healMP != 0) && (!GameConstants.isDemon(chr.getJob())) && (chr.canMP(now + 1000L))) {
            if (healMP > stats.getHealMP()) {
                healMP = (int) stats.getHealMP();
            }
            chr.addMP(healMP);
        }
    }

    public static final void MovePlayer(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        slea.skip(22);
        if (chr == null) {
            return;

        }
        final Point Original_Pos = chr.getPosition();
        List res;
        try {
            res = MovementParse.parseMovement(slea, 1);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println(new StringBuilder().append("AIOBE Type1:\n").append(slea.toString(true)).toString());
            return;
        }
        if ((res != null) && (c.getPlayer().getMap() != null)) {
            if (slea.available() != 18L) {
                return;
            }
            final MapleMap map = c.getPlayer().getMap();

            if (chr.isHidden()) {
                chr.setLastRes(res);
                c.getPlayer().getMap().broadcastGMMessage(chr, CField.movePlayer(chr.getId(), res, Original_Pos), false);
            } else {
                c.getPlayer().getMap().broadcastMessage(c.getPlayer(), CField.movePlayer(chr.getId(), res, Original_Pos), false);
            }

            MovementParse.updatePosition(res, chr, 0);
            final Point pos = chr.getTruePosition();
            map.movePlayer(chr, pos);
            if ((chr.getFollowId() > 0) && (chr.isFollowOn()) && (chr.isFollowInitiator())) {
                MapleCharacter fol = map.getCharacterById(chr.getFollowId());
                if (fol != null) {
                    Point original_pos = fol.getPosition();
                    fol.getClient().getSession().write(CField.moveFollow(Original_Pos, original_pos, pos, res));
                    MovementParse.updatePosition(res, fol, 0);
                    map.movePlayer(fol, pos);
                    map.broadcastMessage(fol, CField.movePlayer(fol.getId(), res, original_pos), false);
                } else {
                    chr.checkFollow();
                }
            }
            WeakReference<MapleCharacter>[] clones = chr.getClones();
            for (int i = 0; i < clones.length; i++) {
                if (clones[i].get() != null) {
                    final MapleCharacter clone = clones[i].get();
                    final List<LifeMovementFragment> res3 = res;
                    CloneTimer.getInstance().schedule(new Runnable() {
                        public void run() {
                            try {
                                if (clone.getMap() == map) {
                                    if (clone.isHidden()) {
                                        map.broadcastGMMessage(clone, CField.movePlayer(clone.getId(), res3, Original_Pos), false);
                                    } else {
                                        map.broadcastMessage(clone, CField.movePlayer(clone.getId(), res3, Original_Pos), false);
                                    }
                                    MovementParse.updatePosition(res3, clone, 0);
                                    map.movePlayer(clone, pos);
                                }
                            } catch (Exception e) {
                                //very rarely swallowed
                            }
                        }
                    }, 50 * i + 150);
                }
            }
            int count = c.getPlayer().getFallCounter();
            boolean samepos = (pos.y > c.getPlayer().getOldPosition().y) && (Math.abs(pos.x - c.getPlayer().getOldPosition().x) < 5);
            if ((samepos) && ((pos.y > map.getBottom() + 250) || (map.getFootholds().findBelow(pos) == null))) {
                if (count > 5) {
                    c.getPlayer().changeMap(map, map.getPortal(0));
                    c.getPlayer().setFallCounter(0);
                } else {
                    count++;
                    c.getPlayer().setFallCounter(count);
                }
            } else if (count > 0) {
                c.getPlayer().setFallCounter(0);
            }
            c.getPlayer().setOldPosition(pos);
            if ((!samepos) && (c.getPlayer().getBuffSource(MapleBuffStat.DARK_AURA) == 32120000)) {
                c.getPlayer().getStatForBuff(MapleBuffStat.DARK_AURA).applyMonsterBuff(c.getPlayer());
            } else if ((!samepos) && (c.getPlayer().getBuffSource(MapleBuffStat.YELLOW_AURA) == 32120001)) {
                c.getPlayer().getStatForBuff(MapleBuffStat.YELLOW_AURA).applyMonsterBuff(c.getPlayer());
            }
        }
    }

    public static final void ChangeMapSpecial(String portal_name, MapleClient c, MapleCharacter chr) {
        if ((chr == null) || (chr.getMap() == null)) {
            return;
        }
        MaplePortal portal = chr.getMap().getPortal(portal_name);

        if ((portal != null) && (!chr.hasBlockedInventory())) {
            portal.enterPortal(c);
        } else {
            c.getSession().write(CWvsContext.enableActions());
        }
    }

    public static final void ChangeMap(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        if ((chr == null) || (chr.getMap() == null)) {
            return;
        }
        if (slea.available() != 0L) {
            slea.readByte();
            int targetid = slea.readInt();
            if (GameConstants.GMS) {
                slea.readInt();
            }
            MaplePortal portal = chr.getMap().getPortal(slea.readMapleAsciiString());
            if (slea.available() >= 7L) {
                slea.readInt();
            }
            slea.skip(1);
            boolean wheel = (slea.readShort() > 0) && (!GameConstants.isEventMap(chr.getMapId())) && (chr.haveItem(5510000, 1, false, true)) && (chr.getMapId() / 1000000 != 925);

            if ((targetid != -1) && (!chr.isAlive())) {
                chr.setStance(0);
                if ((chr.getEventInstance() != null) && (chr.getEventInstance().revivePlayer(chr)) && (chr.isAlive())) {
                    return;
                }
                if (chr.getPyramidSubway() != null) {
                    chr.getStat().setHp(50, chr);
                    chr.getPyramidSubway().fail(chr);
                    return;
                }

                if (!wheel) {
                    chr.getStat().setHp(50, chr);

                    MapleMap to = chr.getMap().getReturnMap();
                    chr.changeMap(to, to.getPortal(0));
                } else {
                    c.getSession().write(CField.EffectPacket.useWheel((byte) (chr.getInventory(MapleInventoryType.CASH).countById(5510000) - 1)));
                    chr.getStat().setHp(chr.getStat().getMaxHp() / 100 * 40, chr);
                    MapleInventoryManipulator.removeById(c, MapleInventoryType.CASH, 5510000, 1, true, false);

                    MapleMap to = chr.getMap();
                    chr.changeMap(to, to.getPortal(0));
                }
            } else if ((targetid != -1) && (chr.isIntern())) {
                MapleMap to = ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(targetid);
                if (to != null) {
                    chr.changeMap(to, to.getPortal(0));
                } else {
                    chr.dropMessage(5, "Map is NULL. Use !warp <mapid> instead.");
                }
            } else if ((targetid != -1) && (!chr.isIntern())) {
                int divi = chr.getMapId() / 100;
                boolean unlock = false;
                boolean warp = false;
                if (divi == 9130401) {
                    warp = (targetid / 100 == 9130400) || (targetid / 100 == 9130401);
                    if (targetid / 10000 != 91304) {
                        warp = true;
                        unlock = true;
                        targetid = 130030000;
                    }
                } else if (divi == 9130400) {
                    warp = (targetid / 100 == 9130400) || (targetid / 100 == 9130401);
                    if (targetid / 10000 != 91304) {
                        warp = true;
                        unlock = true;
                        targetid = 130030000;
                    }
                } else if (divi == 9140900) {
                    warp = (targetid == 914090011) || (targetid == 914090012) || (targetid == 914090013) || (targetid == 140090000);
                } else if ((divi == 9120601) || (divi == 9140602) || (divi == 9140603) || (divi == 9140604) || (divi == 9140605)) {
                    warp = (targetid == 912060100) || (targetid == 912060200) || (targetid == 912060300) || (targetid == 912060400) || (targetid == 912060500) || (targetid == 3000100);
                    unlock = true;
                } else if (divi == 9101500) {
                    warp = (targetid == 910150006) || (targetid == 101050010);
                    unlock = true;
                } else if ((divi == 9140901) && (targetid == 140000000)) {
                    unlock = true;
                    warp = true;
                } else if ((divi == 9240200) && (targetid == 924020000)) {
                    unlock = true;
                    warp = true;
                } else if ((targetid == 980040000) && (divi >= 9800410) && (divi <= 9800450)) {
                    warp = true;
                } else if ((divi == 9140902) && ((targetid == 140030000) || (targetid == 140000000))) {
                    unlock = true;
                    warp = true;
                } else if ((divi == 9000900) && (targetid / 100 == 9000900) && (targetid > chr.getMapId())) {
                    warp = true;
                } else if ((divi / 1000 == 9000) && (targetid / 100000 == 9000)) {
                    unlock = (targetid < 900090000) || (targetid > 900090004);
                    warp = true;
                } else if ((divi / 10 == 1020) && (targetid == 1020000)) {
                    unlock = true;
                    warp = true;
                } else if ((chr.getMapId() == 900090101) && (targetid == 100030100)) {
                    unlock = true;
                    warp = true;
                } else if ((chr.getMapId() == 2010000) && (targetid == 104000000)) {
                    unlock = true;
                    warp = true;
                } else if ((chr.getMapId() == 106020001) || (chr.getMapId() == 106020502)) {
                    if (targetid == chr.getMapId() - 1) {
                        unlock = true;
                        warp = true;
                    }
                } else if ((chr.getMapId() == 0) && (targetid == 10000)) {
                    unlock = true;
                    warp = true;
                } else if ((chr.getMapId() == 931000011) && (targetid == 931000012)) {
                    unlock = true;
                    warp = true;
                } else if ((chr.getMapId() == 931000021) && (targetid == 931000030)) {
                    unlock = true;
                    warp = true;
                }
                if (unlock) {
                   /* c.getSession().write(CField.UIPacket.IntroDisableUI(false));
                    c.getSession().write(CField.UIPacket.IntroLock(false)); */
                    c.getSession().write(CWvsContext.enableActions());
                }
                if (warp) {
                    MapleMap to = ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(targetid);
                    chr.changeMap(to, to.getPortal(0));
                }
            } else if ((portal != null) && (!chr.hasBlockedInventory())) {
                portal.enterPortal(c);
            } else {
                c.getSession().write(CWvsContext.enableActions());
            }
        }
    }

    public static final void InnerPortal(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        if ((chr == null) || (chr.getMap() == null)) {
            return;
        }
        MaplePortal portal = chr.getMap().getPortal(slea.readMapleAsciiString());
        int toX = slea.readShort();
        int toY = slea.readShort();

        if (portal == null) {
            return;
        }
        if ((portal.getPosition().distanceSq(chr.getTruePosition()) > 22500.0D) && (!chr.isGM())) {

            return;
        }
        chr.getMap().movePlayer(chr, new Point(toX, toY));
        chr.checkFollow();
    }


    public static void ReIssueMedal(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        try {
            final MapleQuest q = MapleQuest.getInstance(slea.readShort());
            final int itemid = q.getMedalItem();
            if (itemid != slea.readInt() || itemid <= 0 || q == null || chr.getQuestStatus(q.getId()) != 2) {
                c.getSession().write(CField.UIPacket.reissueMedal(itemid, 4));
                return;
            }
            if (chr.haveItem(itemid, 1, true, true)) {
                c.getSession().write(CField.UIPacket.reissueMedal(itemid, 3));
                return;
            }
            if (!MapleInventoryManipulator.checkSpace(c, itemid, (short) 1, "")) {
                c.getSession().write(CField.UIPacket.reissueMedal(itemid, 2));
                return;
            }
            if (chr.getMeso() < 100) {
                c.getSession().write(CField.UIPacket.reissueMedal(itemid, 1));
                return;
            }
            chr.gainMeso(-100, true, true);
            MapleInventoryManipulator.addById(c, itemid, (short) 1, "Redeemed item through medal quest " + q.getId() + " on " + FileoutputUtil.CurrentReadable_Date());
            c.getSession().write(CField.UIPacket.reissueMedal(itemid, 0));
        } catch (ArrayIndexOutOfBoundsException ee) {
        }
    }
}