// 怪物立绘图片映射
import goblin from "../assets/monsters/goblin.jpg";
import kobold from "../assets/monsters/kobold.jpg";
import skeleton from "../assets/monsters/skeleton.jpg";
import zombie from "../assets/monsters/zombie.jpg";
import owlbear from "../assets/monsters/owlbear.jpg";
import mimic from "../assets/monsters/mimic.jpg";
import werewolf from "../assets/monsters/werewolf.jpg";
import troll from "../assets/monsters/troll.jpg";
import basilisk from "../assets/monsters/basilisk.jpg";
import manticore from "../assets/monsters/manticore.jpg";
import displacerBeast from "../assets/monsters/displacer-beast.jpg";
import ogre from "../assets/monsters/ogre.jpg";
import chimera from "../assets/monsters/chimera.jpg";
import stoneGolem from "../assets/monsters/stone-golem.jpg";
import frostGiant from "../assets/monsters/frost-giant.jpg";
import fireGiant from "../assets/monsters/fire-giant.jpg";
import mindFlayer from "../assets/monsters/mind-flayer.jpg";
import medusa from "../assets/monsters/medusa.jpg";
import beholder from "../assets/monsters/beholder.jpg";
import redDragon from "../assets/monsters/red-dragon.jpg";
import lich from "../assets/monsters/lich.jpg";
import tarrasque from "../assets/monsters/tarrasque.jpg";

// 怪物图片映射
const monsterImages = {
  goblin,
  kobold,
  skeleton,
  zombie,
  owlbear,
  mimic,
  werewolf,
  troll,
  basilisk,
  manticore,
  "displacer-beast": displacerBeast,
  ogre,
  chimera,
  "stone-golem": stoneGolem,
  "frost-giant": frostGiant,
  "fire-giant": fireGiant,
  "mind-flayer": mindFlayer,
  medusa,
  beholder,
  "red-dragon": redDragon,
  lich,
  tarrasque,
};

/**
 * 根据怪物ID获取图片
 * @param {string} monsterId  怪物ID
 * @returns {string} 图片路径
 */
function getMonsterImage(monsterId) {
  return monsterImages[monsterId];
}

export { monsterImages, getMonsterImage };
export default monsterImages;
