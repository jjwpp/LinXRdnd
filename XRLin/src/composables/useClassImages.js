// 职业立绘图片映射 — 男性版（默认）
import barbarian from "../assets/classes/barbarian.jpg";
import bard from "../assets/classes/bard.jpg";
import cleric from "../assets/classes/cleric.jpg";
import druid from "../assets/classes/druid.jpg";
import fighter from "../assets/classes/fighter.jpg";
import monk from "../assets/classes/monk.jpg";
import paladin from "../assets/classes/paladin.jpg";
import ranger from "../assets/classes/ranger.jpg";
import rogue from "../assets/classes/rogue.jpg";
import sorcerer from "../assets/classes/sorcerer.jpg";
import warlock from "../assets/classes/warlock.jpg";
import wizard from "../assets/classes/wizard.jpg";

// 职业立绘图片映射 — 女性版
import barbarianF from "../assets/classes/barbarian_female.jpg";
import bardF from "../assets/classes/bard_female.jpg";
import clericF from "../assets/classes/cleric_female.jpg";
import druidF from "../assets/classes/druid_female.jpg";
import fighterF from "../assets/classes/fighter_female.jpg";
import monkF from "../assets/classes/monk_female.jpg";
import paladinF from "../assets/classes/paladin_female.jpg";
import rangerF from "../assets/classes/ranger_female.jpg";
import rogueF from "../assets/classes/rogue_female.jpg";
import sorcererF from "../assets/classes/sorcerer_female.jpg";
import warlockF from "../assets/classes/warlock_female.jpg";
import wizardF from "../assets/classes/wizard_female.jpg";

// 男性职业图片（默认）
const classImages = {
  barbarian,
  bard,
  cleric,
  druid,
  fighter,
  monk,
  paladin,
  ranger,
  rogue,
  sorcerer,
  warlock,
  wizard,
};

// 女性职业图片
const classImagesFemale = {
  barbarian: barbarianF,
  bard: bardF,
  cleric: clericF,
  druid: druidF,
  fighter: fighterF,
  monk: monkF,
  paladin: paladinF,
  ranger: rangerF,
  rogue: rogueF,
  sorcerer: sorcererF,
  warlock: warlockF,
  wizard: wizardF,
};

/**
 * 根据性别获取职业图片
 * @param {string} classId  职业ID
 * @param {string} gender   'male' | 'female'，默认 male
 * @returns {string} 图片路径
 */
function getClassImage(classId, gender = "male") {
  if (gender === "female") {
    return classImagesFemale[classId] || classImages[classId];
  }
  return classImages[classId];
}

export { classImages, classImagesFemale, getClassImage };
export default classImages;
