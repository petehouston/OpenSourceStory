package handling.channel.handler;


import client.MapleCharacter;
import client.MapleClient;
import client.MapleStat;
import client.PlayerStats;
import client.Skill;
import client.SkillFactory;
import constants.GameConstants;
import java.util.EnumMap;
import java.util.Map;
import server.Randomizer;
import tools.Pair;
import tools.data.LittleEndianAccessor;
import tools.packet.CWvsContext;

public class StatsHandling
{
  public static final void DistributeAP(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr)
  {
    Map statupdate = new EnumMap(MapleStat.class);
    c.getSession().write(CWvsContext.updatePlayerStats(statupdate, true, chr));
    slea.readInt();

    PlayerStats stat = chr.getStat();
    int job = chr.getJob();
    if (chr.getRemainingAp() > 0) {
      switch (slea.readInt()) {
      case 64:
        if (stat.getStr() >= 999) {
          return;
        }
        stat.setStr((short)(stat.getStr() + 1), chr);
        statupdate.put(MapleStat.STR, Long.valueOf(stat.getStr()));
        break;
      case 128:
        if (stat.getDex() >= 999) {
          return;
        }
        stat.setDex((short)(stat.getDex() + 1), chr);
        statupdate.put(MapleStat.DEX, Long.valueOf(stat.getDex()));
        break;
      case 256:
        if (stat.getInt() >= 999) {
          return;
        }
        stat.setInt((short)(stat.getInt() + 1), chr);
        statupdate.put(MapleStat.INT, Long.valueOf(stat.getInt()));
        break;
      case 512:
        if (stat.getLuk() >= 999) {
          return;
        }
        stat.setLuk((short)(stat.getLuk() + 1), chr);
        statupdate.put(MapleStat.LUK, Long.valueOf(stat.getLuk()));
        break;
      case 2048:
        int maxhp = stat.getMaxHp();
        if ((chr.getHpApUsed() >= 10000) || (maxhp >= 500000)) {
          return;
        }
        if (GameConstants.isBeginnerJob(job))
          maxhp += Randomizer.rand(8, 12);
        else if (((job >= 100) && (job <= 132)) || ((job >= 3200) && (job <= 3212)) || ((job >= 1100) && (job <= 1112)) || ((job >= 3100) && (job <= 3112)))
          maxhp += Randomizer.rand(36, 42);
        else if (((job >= 200) && (job <= 232)) || (GameConstants.isEvan(job)))
          maxhp += Randomizer.rand(10, 20);
        else if (((job >= 300) && (job <= 322)) || ((job >= 400) && (job <= 434)) || ((job >= 1300) && (job <= 1312)) || ((job >= 1400) && (job <= 1412)) || ((job >= 3300) && (job <= 3312)) || ((job >= 2300) && (job <= 2312)))
          maxhp += Randomizer.rand(16, 20);
        else if (((job >= 510) && (job <= 512)) || ((job >= 1510) && (job <= 1512)))
          maxhp += Randomizer.rand(28, 32);
        else if (((job >= 500) && (job <= 532)) || ((job >= 3500) && (job <= 3512)) || (job == 1500))
          maxhp += Randomizer.rand(18, 22);
        else if ((job >= 1200) && (job <= 1212))
          maxhp += Randomizer.rand(15, 21);
        else if ((job >= 2000) && (job <= 2112))
          maxhp += Randomizer.rand(38, 42);
        else {
          maxhp += Randomizer.rand(50, 100);
        }
        maxhp = Math.min(500000, Math.abs(maxhp));
        chr.setHpApUsed((short)(chr.getHpApUsed() + 1));
        stat.setMaxHp(maxhp, chr);
        statupdate.put(MapleStat.MAXHP, Long.valueOf(maxhp));
        break;
      case 8192:
        int maxmp = stat.getMaxMp();
        if ((chr.getHpApUsed() >= 10000) || (stat.getMaxMp() >= 500000)) {
          return;
        }
        if (GameConstants.isBeginnerJob(job)) {
          maxmp += Randomizer.rand(6, 8); } else {
          if ((job >= 3100) && (job <= 3112))
            return;
          if (((job >= 200) && (job <= 232)) || (GameConstants.isEvan(job)) || ((job >= 3200) && (job <= 3212)) || ((job >= 1200) && (job <= 1212)))
            maxmp += Randomizer.rand(38, 40);
          else if (((job >= 300) && (job <= 322)) || ((job >= 400) && (job <= 434)) || ((job >= 500) && (job <= 532)) || ((job >= 3200) && (job <= 3212)) || ((job >= 3500) && (job <= 3512)) || ((job >= 1300) && (job <= 1312)) || ((job >= 1400) && (job <= 1412)) || ((job >= 1500) && (job <= 1512)) || ((job >= 2300) && (job <= 2312)))
            maxmp += Randomizer.rand(10, 12);
          else if (((job >= 100) && (job <= 132)) || ((job >= 1100) && (job <= 1112)) || ((job >= 2000) && (job <= 2112)))
            maxmp += Randomizer.rand(6, 9);
          else
            maxmp += Randomizer.rand(50, 100);
        }
        maxmp = Math.min(500000, Math.abs(maxmp));
        chr.setHpApUsed((short)(chr.getHpApUsed() + 1));
        stat.setMaxMp(maxmp, chr);
        statupdate.put(MapleStat.MAXMP, Long.valueOf(maxmp));
        break;
      default:
        c.getSession().write(CWvsContext.enableActions());
        return;
      }
      chr.setRemainingAp((short)(chr.getRemainingAp() - 1));
      statupdate.put(MapleStat.AVAILABLEAP, Long.valueOf(chr.getRemainingAp()));
      c.getSession().write(CWvsContext.updatePlayerStats(statupdate, true, chr));
    }
  }

  public static void DistributeSP(int skillid, byte quantity, MapleClient c, MapleCharacter chr) {
    boolean isBeginnerSkill = false;

    if ((quantity <= 0) || (quantity > 30))
      return;
    int remainingSp;
    if ((GameConstants.isBeginnerJob(skillid / 10000)) && ((skillid % 10000 == 1000) || (skillid % 10000 == 1001) || (skillid % 10000 == 1002) || (skillid % 10000 == 2))) {
      boolean resistance = (skillid / 10000 == 3000) || (skillid / 10000 == 3001);
      int snailsLevel = chr.getSkillLevel(SkillFactory.getSkill(skillid / 10000 * 10000 + 1000));
      int recoveryLevel = chr.getSkillLevel(SkillFactory.getSkill(skillid / 10000 * 10000 + 1001));
      int nimbleFeetLevel = chr.getSkillLevel(SkillFactory.getSkill(skillid / 10000 * 10000 + (resistance ? 2 : 1002)));
      remainingSp = Math.min(chr.getLevel() - 1, resistance ? 9 : 6) - snailsLevel - recoveryLevel - nimbleFeetLevel;
      isBeginnerSkill = true; } else {
      if (GameConstants.isBeginnerJob(skillid / 10000)) {
        return;
      }
      remainingSp = chr.getRemainingSp(GameConstants.getSkillBookForSkill(skillid));
    }
    Skill skill = SkillFactory.getSkill(skillid);

    for (Pair ski : skill.getRequiredSkills()) {
      if (chr.getSkillLevel(SkillFactory.getSkill(((Integer)ski.left).intValue())) < ((Byte)ski.right).byteValue())
      {
        return;
      }
    }
    int maxlevel = skill.isFourthJob() ? chr.getMasterLevel(skill) : skill.getMaxLevel();
    int curLevel = chr.getSkillLevel(skill);

    if ((skill.isInvisible()) && (chr.getSkillLevel(skill) == 0) && (
      ((skill.isFourthJob()) && (chr.getMasterLevel(skill) == 0)) || ((!skill.isFourthJob()) && (maxlevel < 10) && (!isBeginnerSkill) && (chr.getMasterLevel(skill) <= 0)))) {
      c.getSession().write(CWvsContext.enableActions());

      return;
    }

    for (int i : GameConstants.blockedSkills) {
      if (skill.getId() == i) {
        c.getSession().write(CWvsContext.enableActions());
        chr.dropMessage(1, "This skill has been blocked and may not be added.");
        return;
      }
    }

    if ((remainingSp >= quantity) && (curLevel + quantity <= maxlevel) && (skill.canBeLearnedBy(chr.getJob()))) {
      if (!isBeginnerSkill) {
        int skillbook = GameConstants.getSkillBookForSkill(skillid);
        chr.setRemainingSp(chr.getRemainingSp(skillbook) - quantity, skillbook);
      }
                 chr.updateSingleStat(MapleStat.AVAILABLESP, 0); // we don't care the value here
      chr.changeSingleSkillLevel(skill, (byte)(curLevel + quantity), chr.getMasterLevel(skill));
    }
    else
    {
      c.getSession().write(CWvsContext.enableActions());
    }
  }

  public static final void AutoAssignAP(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
    slea.readInt();
    slea.skip(4);
    if (slea.available() < 16L) {
      return;
    }
    int PrimaryStat = GameConstants.GMS ? (int)slea.readLong() : slea.readInt();
    int amount = slea.readInt();
    int SecondaryStat = GameConstants.GMS ? (int)slea.readLong() : slea.readInt();
    int amount2 = slea.readInt();
    if ((amount < 0) || (amount2 < 0)) {
      return;
    }

    PlayerStats playerst = chr.getStat();

    Map statupdate = new EnumMap(MapleStat.class);
    c.getSession().write(CWvsContext.updatePlayerStats(statupdate, true, chr));

    if (chr.getRemainingAp() == amount + amount2) {
      switch (PrimaryStat) {
      case 64:
        if (playerst.getStr() + amount > 999) {
          return;
        }
        playerst.setStr((short)(playerst.getStr() + amount), chr);
        statupdate.put(MapleStat.STR, Long.valueOf(playerst.getStr()));
        break;
      case 128:
        if (playerst.getDex() + amount > 999) {
          return;
        }
        playerst.setDex((short)(playerst.getDex() + amount), chr);
        statupdate.put(MapleStat.DEX, Long.valueOf(playerst.getDex()));
        break;
      case 256:
        if (playerst.getInt() + amount > 999) {
          return;
        }
        playerst.setInt((short)(playerst.getInt() + amount), chr);
        statupdate.put(MapleStat.INT, Long.valueOf(playerst.getInt()));
        break;
      case 512:
        if (playerst.getLuk() + amount > 999) {
          return;
        }
        playerst.setLuk((short)(playerst.getLuk() + amount), chr);
        statupdate.put(MapleStat.LUK, Long.valueOf(playerst.getLuk()));
        break;
      default:
        c.getSession().write(CWvsContext.enableActions());
        return;
      }
      switch (SecondaryStat) {
      case 64:
        if (playerst.getStr() + amount2 > 999) {
          return;
        }
        playerst.setStr((short)(playerst.getStr() + amount2), chr);
        statupdate.put(MapleStat.STR, Long.valueOf(playerst.getStr()));
        break;
      case 128:
        if (playerst.getDex() + amount2 > 999) {
          return;
        }
        playerst.setDex((short)(playerst.getDex() + amount2), chr);
        statupdate.put(MapleStat.DEX, Long.valueOf(playerst.getDex()));
        break;
      case 256:
        if (playerst.getInt() + amount2 > 999) {
          return;
        }
        playerst.setInt((short)(playerst.getInt() + amount2), chr);
        statupdate.put(MapleStat.INT, Long.valueOf(playerst.getInt()));
        break;
      case 512:
        if (playerst.getLuk() + amount2 > 999) {
          return;
        }
        playerst.setLuk((short)(playerst.getLuk() + amount2), chr);
        statupdate.put(MapleStat.LUK, Long.valueOf(playerst.getLuk()));
        break;
      default:
        c.getSession().write(CWvsContext.enableActions());
        return;
      }
      chr.setRemainingAp((short)(chr.getRemainingAp() - (amount + amount2)));
      statupdate.put(MapleStat.AVAILABLEAP, Long.valueOf(chr.getRemainingAp()));
      c.getSession().write(CWvsContext.updatePlayerStats(statupdate, true, chr));
    }
  }
}