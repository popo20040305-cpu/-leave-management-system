/*
Navicat MySQL Data Transfer

Source Server         : mysql8.1
Source Server Version : 80100
Source Host           : 127.0.0.1:3308
Source Database       : qingjia

Target Server Type    : MYSQL
Target Server Version : 80100
File Encoding         : 65001

Date: 2026-08-30 22:10:25
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for application_status
-- ----------------------------
DROP TABLE IF EXISTS `application_status`;
CREATE TABLE `application_status` (
  `id` tinyint NOT NULL AUTO_INCREMENT,
  `applicationStutas` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_unicode_ci;

-- ----------------------------
-- Records of application_status
-- ----------------------------
INSERT INTO `application_status` VALUES ('1', '待提交');
INSERT INTO `application_status` VALUES ('2', '待班主任审批');
INSERT INTO `application_status` VALUES ('3', '待辅导员审批');
INSERT INTO `application_status` VALUES ('4', '待二级学院领导审批');
INSERT INTO `application_status` VALUES ('5', '待销假');
INSERT INTO `application_status` VALUES ('6', '审批未通过');
INSERT INTO `application_status` VALUES ('7', '归档');

-- ----------------------------
-- Table structure for aprove_type
-- ----------------------------
DROP TABLE IF EXISTS `aprove_type`;
CREATE TABLE `aprove_type` (
  `id` int NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_unicode_ci;

-- ----------------------------
-- Records of aprove_type
-- ----------------------------
INSERT INTO `aprove_type` VALUES ('1', '同意');
INSERT INTO `aprove_type` VALUES ('2', '不同意');

-- ----------------------------
-- Table structure for class
-- ----------------------------
DROP TABLE IF EXISTS `class`;
CREATE TABLE `class` (
  `id` int NOT NULL AUTO_INCREMENT,
  `num` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_unicode_ci NOT NULL,
  `grade` int DEFAULT NULL,
  `belongToCollegeId` int NOT NULL,
  `banZhuRenId` int DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_unicode_ci;

-- ----------------------------
-- Records of class
-- ----------------------------
INSERT INTO `class` VALUES ('1', '03202003', '2020', '3', '2');
INSERT INTO `class` VALUES ('2', '03202004', '2020', '3', '2');
INSERT INTO `class` VALUES ('3', '03190401', '2019', '3', '2');
INSERT INTO `class` VALUES ('4', '03200415', '2020', '3', '2');
INSERT INTO `class` VALUES ('5', '02194011', '2024', '2', '2');
INSERT INTO `class` VALUES ('6', '02200419', '2020', '2', '2');
INSERT INTO `class` VALUES ('9', '20250101', '2025', '1', '2');

-- ----------------------------
-- Table structure for college
-- ----------------------------
DROP TABLE IF EXISTS `college`;
CREATE TABLE `college` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_unicode_ci NOT NULL,
  `num` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_unicode_ci NOT NULL,
  `tel` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_unicode_ci;

-- ----------------------------
-- Records of college
-- ----------------------------
INSERT INTO `college` VALUES ('1', '文学传媒学院', '01', '18972420584');
INSERT INTO `college` VALUES ('2', '理学院', '02', '13886779625');
INSERT INTO `college` VALUES ('3', '信息工程学院', '03', '0718 - 8437432');
INSERT INTO `college` VALUES ('5', '化学与环境学院', '04', '13477229167');
INSERT INTO `college` VALUES ('6', '医学院', '05', '0718-87659867');
INSERT INTO `college` VALUES ('12', '美术学院', '08', '13499998888');

-- ----------------------------
-- Table structure for grade
-- ----------------------------
DROP TABLE IF EXISTS `grade`;
CREATE TABLE `grade` (
  `id` int NOT NULL AUTO_INCREMENT,
  `grade` int DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_unicode_ci;

-- ----------------------------
-- Records of grade
-- ----------------------------
INSERT INTO `grade` VALUES ('1', '2019');
INSERT INTO `grade` VALUES ('2', '2020');
INSERT INTO `grade` VALUES ('3', '2021');
INSERT INTO `grade` VALUES ('4', '2022');
INSERT INTO `grade` VALUES ('5', '2023');
INSERT INTO `grade` VALUES ('6', '2024');
INSERT INTO `grade` VALUES ('7', '2025');

-- ----------------------------
-- Table structure for leave_application
-- ----------------------------
DROP TABLE IF EXISTS `leave_application`;
CREATE TABLE `leave_application` (
  `id` int NOT NULL AUTO_INCREMENT,
  `studentId` int DEFAULT NULL,
  `type` tinyint DEFAULT NULL,
  `path` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_unicode_ci DEFAULT NULL,
  `reason` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_unicode_ci DEFAULT NULL,
  `begintime` datetime DEFAULT NULL,
  `endtime` datetime DEFAULT NULL,
  `status` tinyint DEFAULT NULL,
  `xiaoJiaPhotoUrl` varchar(255) COLLATE utf8mb3_unicode_ci DEFAULT NULL,
  `xiaoJiaTime` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_unicode_ci;

-- ----------------------------
-- Records of leave_application
-- ----------------------------
INSERT INTO `leave_application` VALUES ('1', '1', '2', '学校到舞阳坝', '购物', '2021-06-13 11:04:30', '2021-06-13 15:04:30', '1', null, null);
INSERT INTO `leave_application` VALUES ('2', '1', '3', '学校到土桥坝', '逛街', '2021-06-13 14:04:30', '2021-06-13 19:04:30', '1', null, null);
INSERT INTO `leave_application` VALUES ('3', '1', '1', '学校到舞阳坝', '购物', '2021-06-15 12:29:47', '2021-06-15 16:29:47', '1', null, null);
INSERT INTO `leave_application` VALUES ('4', '1', '2', '学校到舞阳坝', '逛街', '2021-06-15 12:30:07', '2021-06-15 18:30:07', '1', null, null);
INSERT INTO `leave_application` VALUES ('5', '1', '3', '学校到舞阳坝', '购物', '2021-06-15 12:30:34', '2021-06-15 18:30:34', '1', null, null);
INSERT INTO `leave_application` VALUES ('6', '1', '4', '学校到土桥坝', '购物', '2021-06-15 12:30:51', '2021-06-15 16:30:51', '1', null, null);
INSERT INTO `leave_application` VALUES ('7', '26', '1', '学校——舞阳坝', '没鞋了，去买双鞋', '2026-06-24 16:58:00', '2026-06-24 22:58:00', '2', '', null);
INSERT INTO `leave_application` VALUES ('8', '26', '2', '学校--宣恩', '世界那么大，我想去看看宣恩', '2026-06-24 17:21:00', '2026-06-26 17:21:00', '2', null, null);
INSERT INTO `leave_application` VALUES ('10', '26', '3', '恩施-坪坝营', '好山好水好风，三天时间不够逛，四天时间刚刚好', '2026-06-25 18:08:00', '2026-06-29 18:08:00', '7', '/xiaojiaPhoto/xiaojia032004150510.jpg', '2026-06-27 08:53:27');
INSERT INTO `leave_application` VALUES ('11', '26', '1', '学校-舞阳坝', '买点好吃的，行不', '2026-06-24 18:54:00', '2026-06-24 21:54:00', '1', '', null);
INSERT INTO `leave_application` VALUES ('12', '26', '4', '恩施，武汉，洛阳，西安', '回家耍耍', '2026-06-27 19:40:00', '2026-07-02 19:40:00', '1', null, null);
INSERT INTO `leave_application` VALUES ('13', '26', '4', '恩施  新疆', '去看看美丽的新疆', '2026-06-28 11:53:00', '2026-07-12 11:53:00', '7', '/xiaojiaPhoto/xiaojia032004150513.jpg', '2026-06-28 17:03:58');

-- ----------------------------
-- Table structure for leave_application_shen_pi
-- ----------------------------
DROP TABLE IF EXISTS `leave_application_shen_pi`;
CREATE TABLE `leave_application_shen_pi` (
  `id` int NOT NULL AUTO_INCREMENT,
  `applicationId` int DEFAULT NULL,
  `teacherId` int DEFAULT NULL,
  `result` tinyint DEFAULT NULL,
  `opinion` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_unicode_ci DEFAULT NULL,
  `createTime` datetime DEFAULT NULL,
  `roleType` tinyint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=30 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_unicode_ci;

-- ----------------------------
-- Records of leave_application_shen_pi
-- ----------------------------
INSERT INTO `leave_application_shen_pi` VALUES ('24', '10', '2', '1', '审批通过', '2026-06-26 16:14:04', '1');
INSERT INTO `leave_application_shen_pi` VALUES ('25', '10', '2', '1', '审批通过', '2026-06-26 16:14:15', '2');
INSERT INTO `leave_application_shen_pi` VALUES ('26', '10', '2', '1', '审批通过', '2026-06-26 17:01:40', '3');
INSERT INTO `leave_application_shen_pi` VALUES ('27', '13', '2', '1', '审批通过', '2026-06-27 17:14:06', '1');
INSERT INTO `leave_application_shen_pi` VALUES ('28', '13', '2', '1', '审批通过', '2026-06-28 11:08:39', '2');
INSERT INTO `leave_application_shen_pi` VALUES ('29', '13', '2', '1', '审批通过', '2026-06-28 16:22:12', '3');

-- ----------------------------
-- Table structure for leave_application_type
-- ----------------------------
DROP TABLE IF EXISTS `leave_application_type`;
CREATE TABLE `leave_application_type` (
  `id` tinyint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_unicode_ci;

-- ----------------------------
-- Records of leave_application_type
-- ----------------------------
INSERT INTO `leave_application_type` VALUES ('1', '一天以内');
INSERT INTO `leave_application_type` VALUES ('2', '一天以外三天以内');
INSERT INTO `leave_application_type` VALUES ('3', '三天以外');
INSERT INTO `leave_application_type` VALUES ('4', '出恩施州');

-- ----------------------------
-- Table structure for menu
-- ----------------------------
DROP TABLE IF EXISTS `menu`;
CREATE TABLE `menu` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_unicode_ci DEFAULT NULL,
  `permission` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_unicode_ci DEFAULT NULL,
  `pid` int DEFAULT NULL,
  `pids` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_unicode_ci DEFAULT NULL,
  `url` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_unicode_ci DEFAULT NULL,
  `type` tinyint DEFAULT NULL,
  `icon` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_unicode_ci DEFAULT NULL,
  `order_num` int DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=60 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_unicode_ci;

-- ----------------------------
-- Records of menu
-- ----------------------------
INSERT INTO `menu` VALUES ('1', '基础信息', 'baseinfo:access', '0', '0,', '', '1', 'images/leftico01.png', '2');
INSERT INTO `menu` VALUES ('2', '学院管理', 'baseinfo:college:access', '1', '0,1,', '/college/index', '2', '', '1');
INSERT INTO `menu` VALUES ('3', '查询学院', 'baseinfo:college:query', '2', '0,1,2,', '', '3', '', '1');
INSERT INTO `menu` VALUES ('4', '添加学院', 'baseinfo:college:add', '2', '0,1,2,', '', '3', '', '2');
INSERT INTO `menu` VALUES ('5', '修改学院', 'baseinfo:college:update', '2', '0,1,2,', '', '3', '', '3');
INSERT INTO `menu` VALUES ('6', '删除学院', 'baseinfo:college:delete', '2', '0,1,2,', '', '3', '', '4');
INSERT INTO `menu` VALUES ('7', '班级管理', 'baseinfo:class:access', '1', '0,1,', '/class/index', '2', '', '2');
INSERT INTO `menu` VALUES ('8', '查询班级', 'baseinfo:class:query', '7', '0,1,7,', '', '3', '', '1');
INSERT INTO `menu` VALUES ('9', '添加班级', 'baseinfo:class:add', '7', '0,1,7,', '', '3', '', '2');
INSERT INTO `menu` VALUES ('10', '修改班级', 'baseinfo:class:update', '7', '0,1,7,', '', '3', '', '3');
INSERT INTO `menu` VALUES ('11', '删除班级', 'baseinfo:class:delete', '7', '0,1,7,', '', '3', '', '5');
INSERT INTO `menu` VALUES ('12', '学生管理', 'baseinfo:student:access', '1', '0,1,', '/student/index', '2', '', '3');
INSERT INTO `menu` VALUES ('13', '查询学生', 'baseinfo:student:query', '12', '0,1,12,', '', '3', '', '1');
INSERT INTO `menu` VALUES ('14', '添加学生', 'baseinfo:student:add', '12', '0,1,12,', '', '3', '', '2');
INSERT INTO `menu` VALUES ('15', '修改学生', 'baseinfo:student:update', '12', '0,1,12,', '', '3', '', '3');
INSERT INTO `menu` VALUES ('16', '学生详情', 'baseinfo:student:detail', '12', '0,1,12,', '', '3', '', '5');
INSERT INTO `menu` VALUES ('17', '修改学生密码', 'baseinfo:student:updatepassword', '12', '0,1,12,', '', '3', '', '4');
INSERT INTO `menu` VALUES ('18', '教师管理', 'baseinfo:teacher:access', '1', '0,1,', '/teacher/index', '2', '', '4');
INSERT INTO `menu` VALUES ('19', '查询教师', 'baseinfo:teacher:qurey', '18', '0,1,18,', '', '3', '', '5');
INSERT INTO `menu` VALUES ('20', '添加教师', 'baseinfo:teacher:add', '18', '0,1,18,', '', '3', '', '1');
INSERT INTO `menu` VALUES ('21', '修改教师', 'baseinfo:teacher:update', '18', '0,1,18,', '', '3', '', '2');
INSERT INTO `menu` VALUES ('22', '删除教师', 'baseinfo:teacher:delete', '18', '0,1,18,', '', '3', '', '3');
INSERT INTO `menu` VALUES ('23', '修改教师密码', 'baseinfo:teacher:updatepassword', '18', '0,1,18,', '', '3', '', '4');
INSERT INTO `menu` VALUES ('24', '请假管理', 'applicaton:access', '0', null, null, '1', 'images/leftico01.png', '4');
INSERT INTO `menu` VALUES ('25', '我的申请', 'leave:student:access', '24', '0,24,', '/leave/student/index?curPage=1', '2', '', '1');
INSERT INTO `menu` VALUES ('26', '请假查询', 'applicaton:myapplication:query', '25', null, null, '3', null, '3');
INSERT INTO `menu` VALUES ('27', '添加请假', 'leave:student:add', '25', '0,24,25,', '', '3', '', '2');
INSERT INTO `menu` VALUES ('28', '修改请假', 'leave:student::update', '25', '0,24,25,', '', '3', '', '4');
INSERT INTO `menu` VALUES ('29', '请假详情', 'leave:student:detail', '25', '0,24,25,', '', '3', '', '5');
INSERT INTO `menu` VALUES ('30', '班主任审批', 'leave:banzhuren:access', '24', '0,24,', '/leave/banzhuren/index', '2', '', '2');
INSERT INTO `menu` VALUES ('31', '辅导员审批', 'leave:fudaoyuan:access', '24', '0,24,', '/leave/fudaoyuan/index?curPage=1', '2', '', '3');
INSERT INTO `menu` VALUES ('32', '院领导审批', 'leave:yuanlingdao:access', '24', '0,24,', '/leave/yuanlingdao/index', '2', '', '4');
INSERT INTO `menu` VALUES ('34', '系统管理', 'system:access', '0', '0,', '', '1', 'images/leftico01.png', '3');
INSERT INTO `menu` VALUES ('35', '权限管理', 'system:menu:access', '34', '', '/menu/list', '2', null, '2');
INSERT INTO `menu` VALUES ('36', '查询菜单', 'system:menu:query', '35', '35,', '', '3', '', '1');
INSERT INTO `menu` VALUES ('37', '添加菜单', 'system:menu:add', '35', '35,', '', '3', '', '2');
INSERT INTO `menu` VALUES ('38', '修改菜单', 'system:menu:update', '35', '35,', '', '3', '', '3');
INSERT INTO `menu` VALUES ('41', '指定班主任', 'baseinfo:class:banzhuren', '7', '0,1,7,', '', '3', '', '4');
INSERT INTO `menu` VALUES ('42', '删除菜单', 'system:menu:delete', '35', '35,', '', '3', '', '6');
INSERT INTO `menu` VALUES ('45', '分页查询', 'baseinfo:college:page', '2', '0,1,2,', '', '3', '', '5');
INSERT INTO `menu` VALUES ('50', '角色管理', 'system:role:access', '34', '0,34,', '/role/index', '2', '', '1');
INSERT INTO `menu` VALUES ('51', '查询角色', 'system:role:query', '50', '0,34,50,', '', '3', '', '1');
INSERT INTO `menu` VALUES ('52', '添加角色', 'system:role:add', '50', '0,34,50,', '', '3', '', '2');
INSERT INTO `menu` VALUES ('53', '修改角色', 'system:role:update', '50', '0,34,50,', '', '3', '', '3');
INSERT INTO `menu` VALUES ('54', '指定角色权限', 'system:role:zdjsqx', '50', '0,34,50,', '', '3', '', '4');
INSERT INTO `menu` VALUES ('55', '删除角色', 'system:role:delete', '50', '0,34,50,', '', '3', '', '5');
INSERT INTO `menu` VALUES ('57', '指定教师角色', 'baseinfo:teacher:zdjs', '18', '0,1,18,', '', '3', '', '6');
INSERT INTO `menu` VALUES ('58', '学生销假', 'leave:student:xiaojia', '25', '0,24,25,', '', '3', '', '1');
INSERT INTO `menu` VALUES ('59', '删除申请', 'leave:student:delete', '25', '0,24,25,', '', '3', '', '6');

-- ----------------------------
-- Table structure for menu_type
-- ----------------------------
DROP TABLE IF EXISTS `menu_type`;
CREATE TABLE `menu_type` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_unicode_ci;

-- ----------------------------
-- Records of menu_type
-- ----------------------------
INSERT INTO `menu_type` VALUES ('1', '目录');
INSERT INTO `menu_type` VALUES ('2', '菜单');
INSERT INTO `menu_type` VALUES ('3', '按钮');

-- ----------------------------
-- Table structure for role
-- ----------------------------
DROP TABLE IF EXISTS `role`;
CREATE TABLE `role` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_unicode_ci DEFAULT NULL,
  `remark` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_unicode_ci DEFAULT NULL,
  `roleType` int DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_unicode_ci;

-- ----------------------------
-- Records of role
-- ----------------------------
INSERT INTO `role` VALUES ('1', '学生', '学生角色，类型是个人，只能查看个人信息', '1');
INSERT INTO `role` VALUES ('2', '班主任', '班主任', '2');
INSERT INTO `role` VALUES ('3', '辅导员', '辅导员', '3');
INSERT INTO `role` VALUES ('4', '学院领导', '学院领导', '3');
INSERT INTO `role` VALUES ('5', '系统管理员', '系统管理员', '4');
INSERT INTO `role` VALUES ('6', '开发人员', '开发人员，可以访问所有功能', '4');
INSERT INTO `role` VALUES ('15', '教师', '可以查看教师所属学院的学院、班级、教师、学生', '1');

-- ----------------------------
-- Table structure for role_menu
-- ----------------------------
DROP TABLE IF EXISTS `role_menu`;
CREATE TABLE `role_menu` (
  `roleId` int DEFAULT NULL,
  `menuId` int DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_unicode_ci;

-- ----------------------------
-- Records of role_menu
-- ----------------------------
INSERT INTO `role_menu` VALUES ('5', '1');
INSERT INTO `role_menu` VALUES ('5', '2');
INSERT INTO `role_menu` VALUES ('5', '3');
INSERT INTO `role_menu` VALUES ('5', '4');
INSERT INTO `role_menu` VALUES ('5', '5');
INSERT INTO `role_menu` VALUES ('5', '6');
INSERT INTO `role_menu` VALUES ('5', '45');
INSERT INTO `role_menu` VALUES ('5', '7');
INSERT INTO `role_menu` VALUES ('5', '8');
INSERT INTO `role_menu` VALUES ('5', '9');
INSERT INTO `role_menu` VALUES ('5', '10');
INSERT INTO `role_menu` VALUES ('5', '41');
INSERT INTO `role_menu` VALUES ('5', '11');
INSERT INTO `role_menu` VALUES ('5', '12');
INSERT INTO `role_menu` VALUES ('5', '13');
INSERT INTO `role_menu` VALUES ('5', '14');
INSERT INTO `role_menu` VALUES ('5', '15');
INSERT INTO `role_menu` VALUES ('5', '17');
INSERT INTO `role_menu` VALUES ('5', '16');
INSERT INTO `role_menu` VALUES ('5', '18');
INSERT INTO `role_menu` VALUES ('5', '20');
INSERT INTO `role_menu` VALUES ('5', '21');
INSERT INTO `role_menu` VALUES ('5', '22');
INSERT INTO `role_menu` VALUES ('5', '23');
INSERT INTO `role_menu` VALUES ('5', '19');
INSERT INTO `role_menu` VALUES ('5', '34');
INSERT INTO `role_menu` VALUES ('5', '50');
INSERT INTO `role_menu` VALUES ('5', '51');
INSERT INTO `role_menu` VALUES ('5', '52');
INSERT INTO `role_menu` VALUES ('5', '53');
INSERT INTO `role_menu` VALUES ('5', '54');
INSERT INTO `role_menu` VALUES ('6', '1');
INSERT INTO `role_menu` VALUES ('6', '2');
INSERT INTO `role_menu` VALUES ('6', '3');
INSERT INTO `role_menu` VALUES ('6', '4');
INSERT INTO `role_menu` VALUES ('6', '5');
INSERT INTO `role_menu` VALUES ('6', '6');
INSERT INTO `role_menu` VALUES ('6', '45');
INSERT INTO `role_menu` VALUES ('6', '7');
INSERT INTO `role_menu` VALUES ('6', '8');
INSERT INTO `role_menu` VALUES ('6', '9');
INSERT INTO `role_menu` VALUES ('6', '10');
INSERT INTO `role_menu` VALUES ('6', '41');
INSERT INTO `role_menu` VALUES ('6', '11');
INSERT INTO `role_menu` VALUES ('6', '12');
INSERT INTO `role_menu` VALUES ('6', '13');
INSERT INTO `role_menu` VALUES ('6', '14');
INSERT INTO `role_menu` VALUES ('6', '15');
INSERT INTO `role_menu` VALUES ('6', '17');
INSERT INTO `role_menu` VALUES ('6', '16');
INSERT INTO `role_menu` VALUES ('6', '18');
INSERT INTO `role_menu` VALUES ('6', '20');
INSERT INTO `role_menu` VALUES ('6', '21');
INSERT INTO `role_menu` VALUES ('6', '22');
INSERT INTO `role_menu` VALUES ('6', '23');
INSERT INTO `role_menu` VALUES ('6', '19');
INSERT INTO `role_menu` VALUES ('6', '57');
INSERT INTO `role_menu` VALUES ('6', '34');
INSERT INTO `role_menu` VALUES ('6', '50');
INSERT INTO `role_menu` VALUES ('6', '51');
INSERT INTO `role_menu` VALUES ('6', '52');
INSERT INTO `role_menu` VALUES ('6', '53');
INSERT INTO `role_menu` VALUES ('6', '54');
INSERT INTO `role_menu` VALUES ('6', '55');
INSERT INTO `role_menu` VALUES ('6', '35');
INSERT INTO `role_menu` VALUES ('6', '36');
INSERT INTO `role_menu` VALUES ('6', '37');
INSERT INTO `role_menu` VALUES ('6', '38');
INSERT INTO `role_menu` VALUES ('6', '42');
INSERT INTO `role_menu` VALUES ('6', '24');
INSERT INTO `role_menu` VALUES ('6', '43');
INSERT INTO `role_menu` VALUES ('6', '25');
INSERT INTO `role_menu` VALUES ('6', '27');
INSERT INTO `role_menu` VALUES ('6', '26');
INSERT INTO `role_menu` VALUES ('6', '28');
INSERT INTO `role_menu` VALUES ('6', '29');
INSERT INTO `role_menu` VALUES ('6', '30');
INSERT INTO `role_menu` VALUES ('6', '31');
INSERT INTO `role_menu` VALUES ('6', '32');
INSERT INTO `role_menu` VALUES ('14', '1');
INSERT INTO `role_menu` VALUES ('14', '2');
INSERT INTO `role_menu` VALUES ('14', '3');
INSERT INTO `role_menu` VALUES ('14', '45');
INSERT INTO `role_menu` VALUES ('14', '7');
INSERT INTO `role_menu` VALUES ('14', '8');
INSERT INTO `role_menu` VALUES ('14', '12');
INSERT INTO `role_menu` VALUES ('14', '13');
INSERT INTO `role_menu` VALUES ('14', '18');
INSERT INTO `role_menu` VALUES ('14', '19');
INSERT INTO `role_menu` VALUES ('15', '1');
INSERT INTO `role_menu` VALUES ('15', '2');
INSERT INTO `role_menu` VALUES ('15', '3');
INSERT INTO `role_menu` VALUES ('15', '7');
INSERT INTO `role_menu` VALUES ('15', '8');
INSERT INTO `role_menu` VALUES ('15', '12');
INSERT INTO `role_menu` VALUES ('15', '13');
INSERT INTO `role_menu` VALUES ('15', '18');
INSERT INTO `role_menu` VALUES ('15', '19');
INSERT INTO `role_menu` VALUES ('2', '1');
INSERT INTO `role_menu` VALUES ('2', '2');
INSERT INTO `role_menu` VALUES ('2', '3');
INSERT INTO `role_menu` VALUES ('2', '7');
INSERT INTO `role_menu` VALUES ('2', '8');
INSERT INTO `role_menu` VALUES ('2', '11');
INSERT INTO `role_menu` VALUES ('2', '12');
INSERT INTO `role_menu` VALUES ('2', '13');
INSERT INTO `role_menu` VALUES ('2', '14');
INSERT INTO `role_menu` VALUES ('2', '15');
INSERT INTO `role_menu` VALUES ('2', '17');
INSERT INTO `role_menu` VALUES ('2', '16');
INSERT INTO `role_menu` VALUES ('2', '18');
INSERT INTO `role_menu` VALUES ('2', '22');
INSERT INTO `role_menu` VALUES ('2', '19');
INSERT INTO `role_menu` VALUES ('2', '24');
INSERT INTO `role_menu` VALUES ('2', '30');
INSERT INTO `role_menu` VALUES ('3', '1');
INSERT INTO `role_menu` VALUES ('3', '2');
INSERT INTO `role_menu` VALUES ('3', '3');
INSERT INTO `role_menu` VALUES ('3', '7');
INSERT INTO `role_menu` VALUES ('3', '8');
INSERT INTO `role_menu` VALUES ('3', '9');
INSERT INTO `role_menu` VALUES ('3', '10');
INSERT INTO `role_menu` VALUES ('3', '41');
INSERT INTO `role_menu` VALUES ('3', '11');
INSERT INTO `role_menu` VALUES ('3', '12');
INSERT INTO `role_menu` VALUES ('3', '13');
INSERT INTO `role_menu` VALUES ('3', '14');
INSERT INTO `role_menu` VALUES ('3', '15');
INSERT INTO `role_menu` VALUES ('3', '17');
INSERT INTO `role_menu` VALUES ('3', '16');
INSERT INTO `role_menu` VALUES ('3', '18');
INSERT INTO `role_menu` VALUES ('3', '19');
INSERT INTO `role_menu` VALUES ('3', '24');
INSERT INTO `role_menu` VALUES ('3', '31');
INSERT INTO `role_menu` VALUES ('4', '1');
INSERT INTO `role_menu` VALUES ('4', '2');
INSERT INTO `role_menu` VALUES ('4', '3');
INSERT INTO `role_menu` VALUES ('4', '4');
INSERT INTO `role_menu` VALUES ('4', '5');
INSERT INTO `role_menu` VALUES ('4', '6');
INSERT INTO `role_menu` VALUES ('4', '45');
INSERT INTO `role_menu` VALUES ('4', '7');
INSERT INTO `role_menu` VALUES ('4', '8');
INSERT INTO `role_menu` VALUES ('4', '9');
INSERT INTO `role_menu` VALUES ('4', '10');
INSERT INTO `role_menu` VALUES ('4', '41');
INSERT INTO `role_menu` VALUES ('4', '11');
INSERT INTO `role_menu` VALUES ('4', '12');
INSERT INTO `role_menu` VALUES ('4', '13');
INSERT INTO `role_menu` VALUES ('4', '14');
INSERT INTO `role_menu` VALUES ('4', '15');
INSERT INTO `role_menu` VALUES ('4', '17');
INSERT INTO `role_menu` VALUES ('4', '16');
INSERT INTO `role_menu` VALUES ('4', '18');
INSERT INTO `role_menu` VALUES ('4', '20');
INSERT INTO `role_menu` VALUES ('4', '21');
INSERT INTO `role_menu` VALUES ('4', '22');
INSERT INTO `role_menu` VALUES ('4', '23');
INSERT INTO `role_menu` VALUES ('4', '19');
INSERT INTO `role_menu` VALUES ('4', '24');
INSERT INTO `role_menu` VALUES ('4', '32');
INSERT INTO `role_menu` VALUES ('1', '1');
INSERT INTO `role_menu` VALUES ('1', '2');
INSERT INTO `role_menu` VALUES ('1', '3');
INSERT INTO `role_menu` VALUES ('1', '7');
INSERT INTO `role_menu` VALUES ('1', '8');
INSERT INTO `role_menu` VALUES ('1', '12');
INSERT INTO `role_menu` VALUES ('1', '13');
INSERT INTO `role_menu` VALUES ('1', '24');
INSERT INTO `role_menu` VALUES ('1', '25');
INSERT INTO `role_menu` VALUES ('1', '58');
INSERT INTO `role_menu` VALUES ('1', '27');
INSERT INTO `role_menu` VALUES ('1', '26');
INSERT INTO `role_menu` VALUES ('1', '28');
INSERT INTO `role_menu` VALUES ('1', '29');
INSERT INTO `role_menu` VALUES ('1', '59');

-- ----------------------------
-- Table structure for role_type
-- ----------------------------
DROP TABLE IF EXISTS `role_type`;
CREATE TABLE `role_type` (
  `id` tinyint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_unicode_ci;

-- ----------------------------
-- Records of role_type
-- ----------------------------
INSERT INTO `role_type` VALUES ('1', '个人');
INSERT INTO `role_type` VALUES ('2', '班级');
INSERT INTO `role_type` VALUES ('3', '学院');
INSERT INTO `role_type` VALUES ('4', '系统');

-- ----------------------------
-- Table structure for sex_type
-- ----------------------------
DROP TABLE IF EXISTS `sex_type`;
CREATE TABLE `sex_type` (
  `id` tinyint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_unicode_ci;

-- ----------------------------
-- Records of sex_type
-- ----------------------------
INSERT INTO `sex_type` VALUES ('1', '男');
INSERT INTO `sex_type` VALUES ('2', '女');
INSERT INTO `sex_type` VALUES ('3', '未知');

-- ----------------------------
-- Table structure for student
-- ----------------------------
DROP TABLE IF EXISTS `student`;
CREATE TABLE `student` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_unicode_ci NOT NULL,
  `num` varchar(12) CHARACTER SET utf8mb3 COLLATE utf8mb3_unicode_ci NOT NULL,
  `password` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_unicode_ci DEFAULT NULL,
  `salt` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_unicode_ci DEFAULT NULL,
  `classId` int DEFAULT NULL,
  `sex` tinyint DEFAULT NULL,
  `mobile` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_unicode_ci DEFAULT NULL,
  `qq` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_unicode_ci DEFAULT NULL,
  `avatarUrl` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `num` (`num`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_unicode_ci;

-- ----------------------------
-- Records of student
-- ----------------------------
INSERT INTO `student` VALUES ('1', '西门吹雪', '031941501', '123456', null, '3', '1', null, null, null);
INSERT INTO `student` VALUES ('2', '东方不败', '0320041501', '123456', null, '4', '1', null, null, null);
INSERT INTO `student` VALUES ('4', '中原一点红', '0220041902', '0220041902', null, '6', '1', null, null, null);
INSERT INTO `student` VALUES ('5', '江小鱼', '0220041903', '0220041903', null, '6', '1', null, null, null);
INSERT INTO `student` VALUES ('17', '佛丹斯科', '0220041904', '0220041904', null, '6', '2', null, null, null);
INSERT INTO `student` VALUES ('18', '竹下登', '0220041905', '0220041905', null, '6', '1', null, null, null);
INSERT INTO `student` VALUES ('19', '奥巴马', '0320041509', '$shiro1$SHA-256$500000$ObSt5VK7vf+C4NLwSEY6jQ==$2ArsAkhvsQ6UhlJHAjXqhtWXBbg+2NmCf6QmuE34/KE=', 'lB58nJuR8bjuVyiSKvOLNA==', '4', '1', '18972420584', '41110999', '/studentPhoto/0320041509_1781839492105.jpg');
INSERT INTO `student` VALUES ('20', '特朗普', '0220041907', '0220041907', null, '6', '1', null, null, null);
INSERT INTO `student` VALUES ('21', '拜登', '0220041908', '0220041908', null, '6', '1', null, null, null);
INSERT INTO `student` VALUES ('22', '向顶天', '0220041910', '0220041910', null, '6', '1', null, null, null);
INSERT INTO `student` VALUES ('23', '张三丰', '0220041911', '0220041911', null, '6', '1', null, null, null);
INSERT INTO `student` VALUES ('24', '杨不悔', '0220041912', '$shiro1$SHA-256$500000$zvutLzOKqBch0cVMEpQeGw==$NJ0vojeJopq5VsXHqSFeXnDx10XVPrDsfImuiXriuC0=', 'MU3veLyT76e3ziPBjmtwZw==', '6', '1', null, null, null);
INSERT INTO `student` VALUES ('25', '罗成', '0320041512', '$shiro1$SHA-256$500000$FCFqjI8nk5+JiCpkjeNP/A==$Syl3K57t7K1gwNyhG77KRm3U9W2mqWjRDUsewvwqz7A=', 'JEasbwbRSbR/LpqbLUmTBA==', '5', '1', '18972420584', '41110999', '/studentPhoto/0320041512_1781965474854.jpg');
INSERT INTO `student` VALUES ('26', '秦琼', '0320041505', '$shiro1$SHA-256$500000$5CtKrxa+J6iHoeSZMPhSdg==$FUACIk7zxbcpOu20oRW4LOBX6vDAuH6jQQqQvru2nNA=', 'GAyLJDeqSooIygWmYRxegg==', '4', '1', '18972420584', '411109995', '/studentPhoto/0320041505_1781965589715.jpg');
INSERT INTO `student` VALUES ('27', '欧阳林玲', '2025010101', '$shiro1$SHA-256$500000$cCsM31tcky/233dLpfJRSg==$i0I+eJx/UwPLf9EOUaXpIzbI9Xzu9L1nzFmZZ16JyHs=', 'N1PkJUkoqEyLl9h6ofcw5g==', '9', '0', '13886779625', '134567788', '/studentPhoto/2025010101_1782183681996.jpg');

-- ----------------------------
-- Table structure for teacher
-- ----------------------------
DROP TABLE IF EXISTS `teacher`;
CREATE TABLE `teacher` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_unicode_ci DEFAULT NULL,
  `num` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_unicode_ci DEFAULT NULL,
  `password` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_unicode_ci DEFAULT NULL,
  `salt` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_unicode_ci DEFAULT NULL,
  `collegeId` int DEFAULT NULL,
  `mobile` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_unicode_ci DEFAULT NULL,
  `avatarUrl` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_unicode_ci;

-- ----------------------------
-- Records of teacher
-- ----------------------------
INSERT INTO `teacher` VALUES ('1', '陶勇', '1997012', '$shiro1$SHA-256$500000$75qHgpPqYBSfio+5ubkg7w==$DGraE/lGdbD+zRz+qJdi+CjZ9UCMcDXVZUcSHiJoRqQ=', 'dGqegl7Svc/+aUp3XiAd8Q==', '3', '18088889999', '/teacherPhoto/1781508601573_99212e7b.png');
INSERT INTO `teacher` VALUES ('2', '贺刚', '1997013', '$shiro1$SHA-256$500000$HGZQMpOLyD3/zMpPXsgfGg==$FYdkF+pmc5PbUOGO/J4pqt1BcHM0U/A6s4nPJgF0ciY=', 'llzPXp/FYgijsvTUsPkdMA==', '3', '13499998888', '/teacherPhoto/1781508412747_82269db0.png');
INSERT INTO `teacher` VALUES ('3', '陶子', '2022013', '$shiro1$SHA-256$500000$ilVE0ycemLIl95vi3vpBIw==$yPzjRb6zdvbEspEgdDb56q8VV70R2iOdeVbrK2x6zFA=', 'Gkfu+ri05jj1V/s/+roSKw==', '3', '1228889999', '/teacherPhoto/2022013.jpg');
INSERT INTO `teacher` VALUES ('4', '西门吹雪', '2021018', '$shiro1$SHA-256$500000$5+EoM9CwqjT+WGiP8V6gYQ==$aWWmYqUxZDUgHxIrnnNCEcOrftKvWEz1fb4GN8WiNkQ=', '3nE/QPKJwc0a+V1LHVhqgQ==', '3', '15688889999', '/teacherPhoto/1781578851462_d652093c.jpg');
INSERT INTO `teacher` VALUES ('5', '东方不败', '2021019', '$shiro1$SHA-256$500000$Q4ccNyiGmzPeFrODQO08CA==$AShR/uEBg7nebJAOQDBKKD/mPHiWTYUWU1caOz5ncUk=', 'kEmhpmfB+tubf7aL2LudtQ==', '3', '13499998888', '/teacherPhoto/1781507832634_be8a9364.jpeg');
INSERT INTO `teacher` VALUES ('11', '花满楼', '202608', '$shiro1$SHA-256$500000$br2Do+J7GtjSXD5p2KCLoA==$d93R92Jdhq4sqt/DLzlwLZBCcdnym9WODURo8xCvOtQ=', 'IsWDtIO4+PvSMYvWMkfuEQ==', '1', '13886769988', null);

-- ----------------------------
-- Table structure for user_role
-- ----------------------------
DROP TABLE IF EXISTS `user_role`;
CREATE TABLE `user_role` (
  `userid` int DEFAULT NULL,
  `userType` tinyint DEFAULT NULL,
  `roleid` int DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_unicode_ci;

-- ----------------------------
-- Records of user_role
-- ----------------------------
INSERT INTO `user_role` VALUES ('1', '2', '3');
INSERT INTO `user_role` VALUES ('1', '2', '4');
INSERT INTO `user_role` VALUES ('1', '2', '5');
INSERT INTO `user_role` VALUES ('6', '2', '3');
INSERT INTO `user_role` VALUES ('1', '2', '6');
INSERT INTO `user_role` VALUES ('1', '2', '7');
INSERT INTO `user_role` VALUES ('1', '1', '1');
INSERT INTO `user_role` VALUES ('5', '2', '3');
INSERT INTO `user_role` VALUES ('3', '2', '3');
INSERT INTO `user_role` VALUES ('2', '2', '2');
INSERT INTO `user_role` VALUES ('2', '2', '3');
INSERT INTO `user_role` VALUES ('2', '2', '4');

-- ----------------------------
-- Table structure for user_token
-- ----------------------------
DROP TABLE IF EXISTS `user_token`;
CREATE TABLE `user_token` (
  `userId` int DEFAULT NULL,
  `userType` tinyint DEFAULT NULL,
  `token` varchar(255) DEFAULT NULL,
  `expire_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of user_token
-- ----------------------------
INSERT INTO `user_token` VALUES ('1', '2', 'b93450a0effc4274906cdb76cf71fc87', '2026-08-30 08:52:21', '2026-08-25 08:52:21');
