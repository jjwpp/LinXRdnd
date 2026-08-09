// 冒险地图图片映射
import forest from "../assets/maps/forest.jpg";
import dungeon from "../assets/maps/dungeon.jpg";
import ruins from "../assets/maps/ruins.jpg";
import mountain from "../assets/maps/mountain.jpg";
import town from "../assets/maps/town.jpg";
import swamp from "../assets/maps/swamp.jpg";

// 地图图片映射
const mapImages = {
  forest,
  dungeon,
  ruins,
  mountain,
  town,
  swamp,
};

// 地图列表（用于随机/轮换选择）
const mapList = [forest, dungeon, ruins, mountain, town, swamp];

// 关键词到地图的映射规则
const keywordMap = [
  { keys: ["森林", "树林", "林地", "树丛", "丛林"], map: forest },
  { keys: ["地下", "洞穴", "洞窟", "地牢", " dungeon", "墓穴", "矿坑"], map: dungeon },
  { keys: ["遗迹", "废墟", "神殿", "古堡", "废墟", "断壁"], map: ruins },
  { keys: ["山", "山脉", "山峰", "悬崖", "山口", "高地"], map: mountain },
  { keys: ["城镇", "村庄", "镇", "村", "酒馆", "集市", "城"], map: town },
  { keys: ["沼泽", "泥潭", "湿地", "水域", "河流", "湖泊"], map: swamp },
];

/**
 * 根据故事文本关键词匹配地图
 * @param {string} text  故事文本
 * @returns {string} 地图图片路径
 */
function getMapByContext(text) {
  if (!text) return mapList[0];
  const lowerText = text.toLowerCase();
  for (const rule of keywordMap) {
    if (rule.keys.some((k) => lowerText.includes(k.toLowerCase()))) {
      return rule.map;
    }
  }
  return mapList[0];
}

/**
 * 根据索引获取地图（用于轮换）
 * @param {number} index
 * @returns {string} 地图图片路径
 */
function getMapByIndex(index) {
  return mapList[index % mapList.length];
}

export { mapImages, mapList, getMapByContext, getMapByIndex };
export default mapImages;
