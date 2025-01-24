CREATE DATABASE IF NOT EXISTS datapie DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS footmart DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS classicmodels DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS datastore DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS mlflow DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;


# ----------------------------
# Table: system site
# these can be changed based on different customers
# ----------------------------
DROP TABLE IF EXISTS sys_site;
CREATE TABLE sys_site
(
    id          int            NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name        varchar(64)    NOT NULL comment 'Site name',
    owner       varchar(64)    NOT NULL comment 'Owner name',
    partner     varchar(64)	   DEFAULT NULL,
    about       varchar(255)   DEFAULT NULL,
    logo        varchar(255)   DEFAULT 's3:/datapie/profile/logo.png' comment 'logo file',
    created_by  varchar(64)    NOT NULL,
    created_at  timestamp      NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_by  varchar(64)    DEFAULT NULL,
    updated_at  timestamp      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB;

INSERT INTO sys_site (id, name, owner, partner, about, logo, created_by, created_at, updated_by, updated_at)
VALUES (1, 'DataPie', 'NineStar', null, 'A data platform for AI and BI!', 's3:/datapie/profile/logo.png', 'Superman', now(), 'Superman', null);


# ----------------------------
# Table: organization
# It can be created by super user and expiration date should be given based on license
# all users which belong to this organization will be deactivated if it expires
# ----------------------------
DROP TABLE IF EXISTS sys_org;
CREATE TABLE sys_org
(
    id          int           NOT NULL AUTO_INCREMENT PRIMARY KEY,
	pid         int           DEFAULT NULL comment 'parent id',
    name        varchar(64)   NOT NULL,
    `desc`      varchar(128)  DEFAULT NULL comment 'description',
    logo        varchar(255)  DEFAULT NULL,
    active      boolean       NOT NULL DEFAULT false comment 'impact all user of this org',
    exp_date    date          NULL comment 'expiration date',
    deleted     boolean       NOT NULL DEFAULT false comment 'true: it was deleted',
    created_by  varchar(64)   NOT NULL,
    created_at  timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_by  varchar(64)   DEFAULT NULL,
    updated_at  timestamp     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB;

INSERT INTO sys_org (id, pid, name, `desc`, logo, active, exp_date, deleted, created_by, created_at, updated_by, updated_at)
VALUES (1, null, 'Playground', 'Discover the production for free', null, true, null, false, 'Superman', now(), 'Superman', null),
	   (2, null, 'NineStar', 'A future AI company', null, true, null, false, 'Superman', now(), 'Superman', null),
	   (3, null, 'Demo A', 'demo for company A', null, true, null, false, 'Superman', now(), 'Superman', null);



# ----------------------------
# Table: sys parameter
# system or user defined parameters
# ----------------------------
DROP TABLE IF EXISTS sys_param;
CREATE TABLE sys_param
(
    id          int            NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name        varchar(64)    NOT NULL,
    `desc`      varchar(128)   DEFAULT NULL comment 'description',
    `group`     varchar(64)    DEFAULT NULL comment 'parameter group',
    module      varchar(64)    NOT NULL comment 'module or feature',
    type        varchar(64)    NOT NULL comment 'int, float, bool, string, [string], json, [json]',
    value       varchar(255)   NOT NULL comment 'current value',
    previous    varchar(255)   DEFAULT NULL comment 'previous value',
	org_id      int            DEFAULT 1 comment 'organization id',
    created_by  varchar(64)    NOT NULL,
    created_at  timestamp      NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_by  varchar(64)    DEFAULT NULL,
    updated_at  timestamp      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	CONSTRAINT fk_param_org    foreign key(org_id)     REFERENCES sys_org(id)
) ENGINE = InnoDB;

INSERT INTO sys_param (id, name, `desc`, `group`, module, type, value, previous, org_id, created_by, created_at, updated_by, updated_at)
VALUES (1, 'source_type', null, 'datasource', 'datamgr', '[string]', "['CSV', 'JSON', 'MySQL', 'MariaDB', 'Vertica']", '', 2, 'Superman', now(), 'Superman', null),
       (2, 'chart_lib', null, 'dataview', 'dataviz', '[json]', "[{name:'G2Plot',ver:['2.4']},{name:'S2',ver:['1.51']},{name:'ECharts',ver:['1.0']}]", null, 2, 'Superman', now(), 'Superman', null);



# ----------------------------
# Table: user
# user can be managered/filtered by organization
# ----------------------------
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user
(
    id            int           NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name          varchar(64)   NOT NULL,
    password      varchar(128)  NOT NULL comment 'encrypted password',
    realname      varchar(64)   NOT NULL,
	`desc`        varchar(64)   DEFAULT NULL comment 'description',
    email         varchar(64)   DEFAULT NULL,
    phone         varchar(16)   DEFAULT NULL,
    org_id        int           DEFAULT 1 comment 'free center by default',
    avatar        varchar(255)  DEFAULT 'default.jpg',
    social        varchar(255)  DEFAULT NULL comment "json like {wechat:'Gavin', teams:'Alice'}",
    active        boolean       NOT NULL DEFAULT false,
	sms_code      boolean       DEFAULT false comment 'true:receive auth code by phone. false: email code',
    exp_date      date          NULL,
    deleted       boolean       DEFAULT false comment 'true: it was deleted', 
    created_by  varchar(64)     NOT NULL,
    created_at  timestamp       NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_by  varchar(64)     DEFAULT NULL,
    updated_at  timestamp       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_org      foreign key(org_id)     REFERENCES sys_org(id)
) ENGINE = InnoDB;
  
INSERT INTO sys_user (id, name, password, realname, `desc`, email, phone, org_id, avatar, social, active, sms_code, exp_date, deleted, created_by, created_at, updated_by, updated_at)
VALUES (1, 'Superman', '$2a$10$7lSDEQ8pyopeE6MMInDQweXboiU8ZF/6CSN0x2.SCVeSz9z4CU57O', 'Mr.Gavin', '', 'jichun.zhao@outlook.com', '18611815495', 2, null, null, true, false, null, false, 'Superman', now(), 'Superman', null),
       (2, 'Admin', '$2a$10$7lSDEQ8pyopeE6MMInDQweXboiU8ZF/6CSN0x2.SCVeSz9z4CU57O', 'Mr.Zhao', '', 'jichun.zhao@outlook.com', '7328902296', 2, null, null, true, false, null, false, 'Superman', now(), 'Superman', null),
       (3, 'visitor', '$2a$10$7lSDEQ8pyopeE6MMInDQweXboiU8ZF/6CSN0x2.SCVeSz9z4CU57O', 'Visitor', '', 'visitor@gmail.com', null, 1, null, null, true, false, null, false, 'Superman', now(), 'Superman', null),
       (4, 'GavinZ', '$2a$10$5UdWUefOL1ZQtXmwmcRwVunQlR278M/VtgxE59VydihhInziQ9Lua', 'Gavin.Zhao', '', 'jichun.zhao@outlook.com', null, 1, null, null, true, false, null, false, 'Superman', now(), 'Superman', null);

# ----------------------------
# Table: role
# Superuser, Administrator, Guest, Viewer, ...
# ----------------------------
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role
(
    id          int             NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name        varchar(64)     NOT NULL, 
    `desc`      varchar(128)    DEFAULT NULL comment 'description',
    active      boolean         NOT NULL DEFAULT false,
    org_id      int             DEFAULT NULL comment 'null or 0 for all org/users',
    created_by  varchar(64)     NOT NULL,
    created_at  timestamp       NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_by  varchar(64)     DEFAULT NULL,
    updated_at  timestamp       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_role_org      foreign key(org_id)     REFERENCES sys_org(id)
) ENGINE = InnoDB;


INSERT INTO sys_role (id, name, `desc`, active, org_id, created_by, created_at, updated_by, updated_at)
VALUES (1, 'Guest', 'build AI and BI', true, null, 'Superman', now(), 'Superman', null),
       (2, 'Superuser', 'has all permissions', true, null, 'Superman', now(), 'Superman', null),
       (3, 'Administrator', 'has all permissions', true, null, 'Superman', now(), 'Superman', null),
       (4, 'Admin', 'control panel only', true, null, 'Superman', now(), 'Superman', null),
       (5, 'Tester', 'has all permissions', true, null, 'Superman', now(), 'Superman', null),
       (6, 'Viewer', 'view only', true, null, 'Superman', now(), 'Superman', null);



# ----------------------------
# Table: sys_user_role
# user relationship
# ----------------------------
DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role
(
    id            int           NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id       int           NOT NULL,
    role_id       int           NOT NULL,
    CONSTRAINT fk_retive_user   foreign key(user_id)    REFERENCES sys_user(id),
    CONSTRAINT fk_retive_role   foreign key(role_id)    REFERENCES sys_role(id)
) ENGINE = InnoDB;



INSERT INTO sys_user_role (id, user_id, role_id)
VALUES (1, 1, 2),
       (2, 2, 3),
	   (3, 3, 1);


# ----------------------------
# Table: menu
# all items which can be controlled by a role
# ----------------------------
DROP TABLE IF EXISTS sys_menu;
CREATE TABLE sys_menu
(
    id           int           NOT NULL AUTO_INCREMENT PRIMARY KEY,
    pid          int           DEFAULT NULL comment 'parent id',
    name         varchar(64)   NOT NULL comment 'English title', 
	title        varchar(64)   NOT NULL comment 'Chinese title', 
    icon         varchar(64)   DEFAULT NULL comment 'menu icon',
	pos          int           DEFAULT NULL comment 'position/order of menu',
	subreport    boolean       DEFAULT false comment 'true: show report as submenu for dynamic menu',
	
	component    varchar(64)   DEFAULT NULL comment 'component of frontend',
    path         varchar(64)   DEFAULT NULL comment 'route path/url',
    redirect     varchar(64)   DEFAULT NULL comment 'route redirection',
    org_id       int           DEFAULT NULL,
    active       boolean       NOT NULL DEFAULT true,
    deleted      boolean       NOT NULL DEFAULT false comment 'true: it was deleted',
    created_by  varchar(64)    NOT NULL,
    created_at  timestamp      NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_by  varchar(64)    DEFAULT NULL,
    updated_at  timestamp      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB;

INSERT INTO sys_menu (id, pid, name, title, icon, pos, subreport, component, path, redirect, active, deleted, created_by, created_at, updated_by, updated_at)
VALUES (1, null, 'Home', '主页', 'ant-design:home-outlined', 0, false, '/home/index', '/home', null, true, false, 'Superman', now(), 'Superman', null),
(2, null, 'Dashboard', '仪表板', 'ant-design:dashboard-outlined', 1, false, 'BlankLayout', '/dashboard', null, true, false, 'Superman', now(), 'Superman', null),

(3, null, 'Visualization', '数据可视化', 'ant-design:appstore-outlined', 2, false, 'BlankLayout', '/dataviz', null, true, false, 'Superman', now(), 'Superman', null),
(4, 3, 'Dataset', '数据集', 'ant-design:database-outlined', 0, false, '/dataviz/dataset/index', '/dataviz/dataset', null, true, false, 'Superman', now(), 'Superman', null),
(5, 3, 'View', '视图', 'ant-design:line-chart-outlined', 1, false, '/dataviz/dataview/index', '/dataviz/dataview', null, true, false, 'Superman', now(), 'Superman', null),
(6, 3, 'Report', '报表', 'ant-design:appstore-add-outlined', 2, false, '/dataviz/report/index', '/dataviz/report', null, true, false, 'Superman', now(), 'Superman', null),

(7, null, 'Source Mgr', '数据管理', 'ant-design:money-collect-outlined', 3, false, 'BlankLayout', '/datamgr', null, true, false, 'Superman', now(), 'Superman', null),
(8, 7, 'Source', '数据源', 'ant-design:database-outlined', 3, false, '/datamgr/datasource/index', '/datamgr/datasource', null, true, false, 'Superman', now(), 'Superman', null),
(9, 7, 'Import', '导入数据', 'control', 0, false, '/datamgr/import/index', '/datamgr/import', null, true, false, 'Superman', now(), 'Superman', null),
(10, 7, 'Anchor', '采集点', 'control', 1, false, '/datamgr/anchor/index', '/datamgr/anchor', null, true, false, 'Superman', now(), 'Superman', null),
(11, 7, 'Kafka', 'Kafka', 'control', 2, false, '/datamgr/kafka/index', '/datamgr/kafka', null, true, false, 'Superman', now(), 'Superman', null),
(12, 7, 'ETL', 'ETL', 'control', 3, false, '/datamgr/etl/index', '/datamgr/etl', null, true, false, 'Superman', now(), 'Superman', null),
(13, 7, 'Web Magic', 'Web Magic', 'control', 4, false, '/datamgr/webmagic/index', '/datamgr/webmagic', null, true, false, 'Superman', now(), 'Superman', null),

(14, null, 'ML Dev', '机器学习', 'ant-design:car-outlined', 4, false, 'BlankLayout', '/ml', null, true, false, 'Superman', now(), 'Superman', null),
(15, 14, 'Dataset', '数据集', 'ant-design:database-outlined', 0, false, '/ml/dataset/index', '/ml/dataset', null, true, false, 'Superman', now(), 'Superman', null),
(16, 14, 'EDA', '数据探索', 'control', 1, false, '/ml/eda/index', '/ml/eda', null, true, false, 'Superman', now(), 'Superman', null),
(17, 14, 'Algorithm', '算法', 'control', 2, false, '/ml/algorithm/index', '/ml/algorithm', null, true, false, 'Superman', now(), 'Superman', null),
(18, 14, 'Model', '模型', 'control', 3, false, '/ml/model/index', '/ml/model', null, true, false, 'Superman', now(), 'Superman', null),
(19, 14, 'Workflow', '工作流', 'control', 4, false, '/ml/workflow/index', '/ml/workflow', null, true, false, 'Superman', now(), 'Superman', null),
(20, 14, 'Vis', '可视化', 'control', 5, false, '/ml/vis/index', '/ml/vis', null, true, false, 'Superman', now(), 'Superman', null),

(21, null, 'AI App', '人工智能', 'ant-design:coffee-outlined', 5, false, 'BlankLayout', '/ai', null, true, false, 'Superman', now(), 'Superman', null),
(22, 21, 'Market', '模型市场', 'control', 0, false, '/ai/market/index', '/ai/market', null, true, false, 'Superman', now(), 'Superman', null),
(23, 21, 'Image', '图像处理', 'control', 1, false, '/ai/image/index', '/ai/image', null, true, false, 'Superman', now(), 'Superman', null),
(24, 21, 'Video', '视频分析', 'control', 2, false, '/ai/video/index', '/ai/video', null, true, false, 'Superman', now(), 'Superman', null),
(25, 21, 'Audio', '语音处理', 'control', 3, false, '/ai/audio/index', '/ai/audio', null, true, false, 'Superman', now(), 'Superman', null),
(26, 21, 'Text', '文本分心', 'control', 4, false, '/ai/text/index', '/ai/text', null, true, false, 'Superman', now(), 'Superman', null),
(27, 21, 'DM', '数据挖掘', 'control', 5, false, '/ai/dm/index', '/ai/dm', null, true, false, 'Superman', now(), 'Superman', null),

(28, null, 'Admin', '控制面板', 'ant-design:setting-outlined', 6, false, 'BlankLayout', '/admin', null, true, false, 'Superman', now(), 'Superman', null),
(29, 28, 'User', '用户管理', 'control', 0, false, '/admin/user/index', '/admin/user', null, true, false, 'Superman', now(), 'Superman', null),
(30, 28, 'Role', '角色管理', 'control', 1, false, '/admin/role/index', '/admin/role', null, true, false, 'Superman', now(), 'Superman', null),
(31, 28, 'Menu', '菜单管理', 'control', 2, false, '/admin/menu/index', '/admin/menu', null, true, false, 'Superman', now(), 'Superman', null),
(32, 28, 'Parameter', '参数管理', 'control', 3, false, '/admin/param/index', '/admin/config', null, true, false, 'Superman', now(), 'Superman', null),
(33, 28, 'Organization', '组织管理', 'control', 0, false, '/admin/org/index', '/admin/org', null, true, false, 'Superman', now(), 'Superman', null),
(34, 28, 'Scheduler', '调度计划', 'control', 4, false, '/admin/scheduler/index', '/admin/scheduler', null, true, false, 'Superman', now(), 'Superman', null),
(35, 28, 'My Center', '个人中心', 'control', 4, false, '/admin/mycenter/index', '/admin/mycenter', null, true, false, 'Superman', now(), 'Superman', null),

(36, null, 'Monitor', '系统监控', 'ant-design:fund-projection-screen-outlined', 7, false, 'BlankLayout', '/monitor', null, true, false, 'Superman', now(), 'Superman', null),
(37, 36, 'Druid', 'Druid', 'control', 0, false, '/monitor/druid/index', '/monitor/druid', null, true, false, 'Superman', now(), 'Superman', null),
(38, 36, 'Knife4j', 'Knife4j', 'control', 1, false, '/monitor/knife4j/index', '/monitor/knife4j', null, true, false, 'Superman', now(), 'Superman', null),
(39, 36, 'Gateway', '网关代理', 'control', 2, false, '/monitor/gateway/index', '/monitor/gateway', null, true, false, 'Superman', now(), 'Superman', null),
(40, 36, 'Network', '网络', 'control', 3, false, '/monitor/network/index', '/monitor/network', null, true, false, 'Superman', now(), 'Superman', null),

(41, null, 'System', '系统管理', 'ant-design:apple-outlined', 8, false, 'BlankLayout', '/system', null, true, false, 'Superman', now(), 'Superman', null),
(42, 41, 'Log', '日志管理', 'control', 0, false, 'BlankLayout', '/system/log', null, true, false, 'Superman', now(), 'Superman', null),
(43, 41, 'Access', '登录日志', 'control', 0, false, '/system/log/access/index', '/system/log/access', null, true, false, 'Superman', now(), 'Superman', null),
(44, 41, 'Action', '操作日志', 'control', 1, false, '/system/log/action/index', '/system/log/action', null, true, false, 'Superman', now(), 'Superman', null);



# ----------------------------
# Table: permit
# a role has permission to visit a menu
# ----------------------------
DROP TABLE IF EXISTS sys_role_menu_permit;
CREATE TABLE sys_role_menu_permit
(
    id            int           NOT NULL AUTO_INCREMENT PRIMARY KEY,
    role_id       int           NOT NULL,
    menu_id       int           NOT NULL,
    permit        tinyint       DEFAULT 1 comment 'bit permit, viewable by default',
	view          boolean       NOT NULL DEFAULT true comment 'true: viewable',
    edit          boolean       NOT NULL DEFAULT false comment 'true: add/edit/delete',
    publish       boolean       NOT NULL DEFAULT false comment 'true: has permit to publish',
	subscribe     boolean       NOT NULL DEFAULT false comment 'true: has permit to subscribe',
	import        boolean       NOT NULL DEFAULT false comment 'true: has permit to import',
	export        boolean       NOT NULL DEFAULT false comment 'true: has permit to export',
    CONSTRAINT fk_permit_role   foreign key(role_id)      REFERENCES sys_role(id),
    CONSTRAINT fk_permit_menu   foreign key(menu_id)      REFERENCES sys_menu(id)
) ENGINE = InnoDB;


INSERT INTO sys_role_menu_permit (id, role_id, menu_id, permit, view, edit, publish, subscribe, import, export)
VALUES (1, 1, 1, 1, true, true, true, true, false, false),
(2, 1, 2, 1, true, true, true, true, false, false),
(3, 1, 3, 1, true, true, true, true, false, false),
(4, 1, 7, 1, true, true, true, true, false, false),
(5, 1, 14, 1, true, true, true, true, false, false),
(6, 1, 21, 1, true, true, true, true, false, false),

(null, 2, 1, 64, true, true, true, true, true, true),
(null, 2, 2, 64, true, true, true, true, true, true),
(null, 2, 3, 64, true, true, true, true, true, true),
(null, 2, 7, 64, true, true, true, true, true, true),
(null, 2, 14, 64, true, true, true, true, true, true),
(null, 2, 21, 64, true, true, true, true, true, true),
(null, 2, 28, 64, true, true, true, true, true, true),
(null, 2, 36, 64, true, true, true, true, true, true),
(null, 2, 41, 64, true, true, true, true, true, true),

(null, 3, 1, 64, true, true, true, true, true, true),
(null, 3, 2, 64, true, true, true, true, true, true),
(null, 3, 3, 64, true, true, true, true, true, true),
(null, 3, 7, 64, true, true, true, true, true, true),
(null, 3, 14, 64, true, true, true, true, true, true),
(null, 3, 21, 64, true, true, true, true, true, true),
(null, 3, 28, 64, true, true, true, true, true, true),
(null, 3, 36, 64, true, true, true, true, true, true),
(null, 3, 41, 64, true, true, true, true, true, true),

(null, 4, 1, 8, true, true, true, false, false, false),
(null, 4, 2, 8, true, true, true, false, false, false),
(null, 4, 3, 8, true, true, true, false, false, false),
(null, 4, 7, 8, true, true, true, false, false, false),
(null, 4, 14, 8, true, true, true, false, false, false),
(null, 4, 21, 8, true, true, true, false, false, false),
(null, 4, 28, 8, true, true, true, false, false, false),
(null, 4, 36, 8, true, true, true, false, false, false),
(null, 4, 41, 8, true, true, true, false, false, false);



# ----------------------------
# Table: sys_msg
# someone send msg to another user or a broadcast to a org
# ----------------------------
DROP TABLE IF EXISTS sys_msg;
CREATE TABLE sys_msg
(
    id             int          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    ts             timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    type           varchar(16)  NOT NULL DEFAULT 'msg' comment 'notice or msg',
	category	   varchar(16)  NOT NULL DEFAULT 'ml' comment 'ai, bi, ml, chat',
	code 		   varchar(64)  DEFAULT NULL comment 'unique code',
    from_id        int          DEFAULT NULL comment 'user id',
    to_id          int          NOT NULL comment 'user id when msg, org id when notice',
    content        text         NOT NULL,
    tid            int          DEFAULT NULL comment 'ml_algo.id = 15',
	read_users     text         DEFAULT NULL comment '[id]'
) ENGINE = InnoDB;


# ----------------------------
# Table: log_access
# 
# ----------------------------
DROP TABLE IF EXISTS log_access;
CREATE TABLE log_access
(
    id             int           NOT NULL AUTO_INCREMENT PRIMARY KEY,
    ts_utc         timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    username       varchar(64)   NOT NULL comment 'username',
	user_id        int           DEFAULT NULL,
	login          boolean       NOT NULL DEFAULT true,
    ip             varchar(64)   DEFAULT NULL comment 'IPV4 or IPV6',
    os             varchar(64)   DEFAULT NULL comment 'Window 11, MacOS',
	browser        varchar(16)   DEFAULT NULL comment 'Browser name',
	lang           varchar(16)   DEFAULT NULL comment 'browser language',
	time_zone      varchar(64)   DEFAULT NULL comment 'Time zone',
	location       varchar(64)   DEFAULT NULL comment 'Location based on ip',
    result         varchar(255)  DEFAULT 'ok' comment 'ok: success, others: failure info',
	CONSTRAINT fk_access_user    foreign key(user_id)    REFERENCES sys_user(id)
) ENGINE = InnoDB;

# ----------------------------
# Table: log_action
# ----------------------------
DROP TABLE IF EXISTS log_action;
CREATE TABLE log_action
(
    id             int           NOT NULL AUTO_INCREMENT PRIMARY KEY,
    ts_utc         timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
	username       varchar(64)   NOT NULL comment 'username',
    user_id        int           NOT NULL,
	type           varchar(64)   DEFAULT NULL comment 'view, add, update, delete, active, import, export, auth, other',
    url            varchar(64)   DEFAULT NULL comment 'http://x/y',
	module         varchar(64)   DEFAULT NULL comment 'class name',
    method         varchar(64)   DEFAULT NULL,
	tid            int           DEFAULT NULL comment 'target id, like user id, soruce id',
	param          text          DEFAULT NULL comment 'json, like {in:{}, out:{}}',
    result         varchar(255)  DEFAULT 'ok' comment 'ok: success, others: failure info',
    CONSTRAINT fk_act_user       foreign key(user_id)    REFERENCES sys_user(id)
) ENGINE = InnoDB;



# ----------------------------
# not use
# Table: gis_ip
# ip to country
# ----------------------------
DROP TABLE IF EXISTS gis_ip;
CREATE TABLE gis_ip
(
    id             int           NOT NULL AUTO_INCREMENT PRIMARY KEY,
	ip_from        int unsigned  NOT NULL,
	ip_to          int unsigned  NOT NULL,
	code           varchar(8)    comment 'country code',  
	country        varchar(64)   comment 'country name'
) ENGINE = InnoDB;



# ----------------------------
# Table: gis_point
# longitude and latitude of a point on map
# ----------------------------
DROP TABLE IF EXISTS gis_point;
CREATE TABLE gis_point
(
    id             int           NOT NULL AUTO_INCREMENT PRIMARY KEY,
	pid            int           NULL,
	name           varchar(64)   NOT NULL,
	abbr           varchar(32)   DEFAULT NULL comment 'abbreviation',
    type           varchar(64)   NULL comment 'village, city, county, state, province, area, country or region',
    code           int           DEFAULT NULL comment 'house number, street number, zip code or post code',	
    lat            float         NOT NULL comment 'latitude',
	lng            float         NOT NULL comment 'longitude' 
) ENGINE = InnoDB;


# ----------------------------
# Table: gis_area
# a area on map which is described by multiple points
# ----------------------------
DROP TABLE IF EXISTS gis_area;
CREATE TABLE gis_area
(
    id             int           NOT NULL AUTO_INCREMENT PRIMARY KEY,
	pid            int           NULL,
	name           varchar(64)   NOT NULL,
	loc            geometry      NOT NULL comment 'GeoJSON'
) ENGINE = InnoDB;



# ----------------------------
# Table: gis layer
# map base layers and over layers
# ----------------------------
DROP TABLE IF EXISTS gis_layer;
CREATE TABLE gis_layer
(
    id             int           NOT NULL AUTO_INCREMENT PRIMARY KEY,
	name           varchar(64)   NOT NULL,
	type           varchar(64)   NOT NULL comment 'tileLayer, tileLayer.wms, wmts',
	`group`    	   varchar(64)   NOT NULL comment 'baselayer, overlayer',
	icon           varchar(255)  NULL,
	args		   varchar(255)  NOT NULL comment 'server link',
	options        text          NULL comment 'json'
) ENGINE = InnoDB;


INSERT INTO gis_layer (id, name, type, `group`, icon, args, options)
VALUES (1, 'OSM.Mapnik', 'tileLayer', 'baselayer', 'http://b.tile.osm.org/1/0/0.png', 'http://{s}.tile.osm.org/{z}/{x}/{y}.png', '{ attribution: "" }'),
(2, 'OPNVKarte', 'tileLayer', 'baselayer', 'https://tileserver.memomaps.de/tilegen/5/8/11.png', 'https://tileserver.memomaps.de/tilegen/{z}/{x}/{y}.png', '{attribution:\'Map <a href="https://memomaps.de/">memomaps.de</a> <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, map data &copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors\'}"),
(3, 'OpenTopoMap', 'tileLayer', 'baselayer', 'https://tile.opentopomap.org/3/7/2.png', 'https://{s}.tile.opentopomap.org/{z}/{x}/{y}.png', '{attribution:\'Map data: &copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors, <a href="http://viewfinderpanoramas.org">SRTM</a> | Map style: &copy; <a href="https://opentopomap.org">OpenTopoMap</a> (<a href="https://creativecommons.org/licenses/by-sa/3.0/">CC-BY-SA</a>)\'}'),
(4, 'Stadia.Dark', 'tileLayer', 'baselayer', 'https://tiles.stadiamaps.com/tiles/alidade_smooth_dark/6/14/25.png', 'https://tiles.stadiamaps.com/tiles/alidade_smooth_dark/{z}/{x}/{y}{r}.png', '{ attribution:\'&copy; <a href="https://stadiamaps.com/">Stadia Maps</a>, &copy; <a href="https://openmaptiles.org/">OpenMapTiles</a> &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors\'}'),
(5, 'Stadia.Outdoors', 'tileLayer', 'baselayer', 'https://tiles.stadiamaps.com/tiles/outdoors/8/77/94.png', 'https://tiles.stadiamaps.com/tiles/outdoors/{z}/{x}/{y}{r}.png', '{ attribution:\'&copy; <a href="https://stadiamaps.com/">Stadia Maps</a>, &copy; <a href="https://openmaptiles.org/">OpenMapTiles</a> &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors\'}'),
(6, 'OpenCycleMap', 'tileLayer', 'baselayer', 'https://tiles.stadiamaps.com/tiles/outdoors/1/1/0.png', 'https://{s}.tile.thunderforest.com/cycle/{z}/{x}/{y}.png?apikey={apikey}', '{ attribution:\'&copy; <a href="http://www.thunderforest.com/">Thunderforest</a>, &copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors\'}'),
(7, 'CyclOSM', 'tileLayer', 'baselayer', 'https://c.tile.openstreetmap.fr/hot/6/17/24.png', 'https://{s}.tile-cyclosm.openstreetmap.fr/cyclosm/{z}/{x}/{y}.png', '{ attribution:\'<a href="https://github.com/cyclosm/cyclosm-cartocss-style/releases" title="CyclOSM - Open Bicycle render">CyclOSM</a> | Map data: &copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors\'}'),
(8, 'Stamen.Toner', 'tileLayer', 'baselayer', 'https://stamen-tiles-b.a.ssl.fastly.net/toner/1/1/0.png', 'https://stamen-tiles-{s}.a.ssl.fastly.net/toner/{z}/{x}/{y}{r}.{ext}', '{ subdomains: "abcd", ext: "png", attribution:\'Map tiles by <a href="http://stamen.com">Stamen Design</a>, <a href="http://creativecommons.org/licenses/by/3.0">CC BY 3.0</a> &mdash; Map data &copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors\'}'),
(9, 'Stamen.Watercolor', 'tileLayer', 'baselayer', 'https://stamen-tiles-a.a.ssl.fastly.net/watercolor/1/0/0.jpg', 'https://stamen-tiles-{s}.a.ssl.fastly.net/watercolor/{z}/{x}/{y}.{ext}', '{subdomains: "abcd",ext: "jpg", attribution:\'Map tiles by <a href="http://stamen.com">Stamen Design</a>, <a href="http://creativecommons.org/licenses/by/3.0">CC BY 3.0</a> &mdash; Map data &copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors\'}'),
(10, 'Stamen.Terrain', 'tileLayer', 'baselayer', 'https://stamen-tiles-d.a.ssl.fastly.net/terrain-background/2/2/1.png', 'https://stamen-tiles-{s}.a.ssl.fastly.net/terrain/{z}/{x}/{y}{r}.{ext}', '{subdomains: "abcd",ext: "jpg", attribution:\'Map tiles by <a href="http://stamen.com">Stamen Design</a>, <a href="http://creativecommons.org/licenses/by/3.0">CC BY 3.0</a> &mdash; Map data &copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors\'}'),
(11, 'Stamen.TonerBackground', 'tileLayer', 'baselayer', 'https://stamen-tiles-d.a.ssl.fastly.net/toner-background/2/2/1.png', 'https://stamen-tiles-{s}.a.ssl.fastly.net/toner-background/{z}/{x}/{y}{r}.{ext}', '{subdomains: "abcd",ext: "jpg", attribution:\'Map tiles by <a href="http://stamen.com">Stamen Design</a>, <a href="http://creativecommons.org/licenses/by/3.0">CC BY 3.0</a> &mdash; Map data &copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors\'}'),
(12, 'Stamen.TerrainLabels', 'tileLayer', 'baselayer', 'https://stamen-tiles-b.a.ssl.fastly.net/terrain-labels/4/3/6.png', 'https://stamen-tiles-{s}.a.ssl.fastly.net/terrain-labels/{z}/{x}/{y}{r}.{ext}', '{subdomains: "abcd",ext: "jpg", attribution:\'Map tiles by <a href="http://stamen.com">Stamen Design</a>, <a href="http://creativecommons.org/licenses/by/3.0">CC BY 3.0</a> &mdash; Map data &copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors\'}'),
(13, 'Esri.WorldStreetMap', 'tileLayer', 'baselayer', 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/4/6/3', 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/{z}/{y}/{x}', '{ attribution: "Tiles &copy; Esri &mdash; Source: Esri, DeLorme, NAVTEQ, USGS, Intermap, iPC, NRCAN, Esri Japan, METI, Esri China (Hong Kong), Esri (Thailand), TomTom, 2012" }'),
(14, 'Esri.WorldImagery', 'tileLayer', 'baselayer', 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/8/94/77', 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', '{ attribution: "Tiles &copy; Esri &mdash; Source: Esri, DeLorme, NAVTEQ, USGS, Intermap, iPC, NRCAN, Esri Japan, METI, Esri China (Hong Kong), Esri (Thailand), TomTom, 2012" }'),
(15, 'Esri.WorldGrayCanvas', 'tileLayer', 'baselayer', 'https://c.basemaps.cartocdn.com/light_all/8/74/96.png', 'https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Light_Gray_Base/MapServer/tile/{z}/{y}/{x}', '{ attribution: "Tiles &copy; Esri &mdash; Esri, DeLorme, NAVTEQ" }'),
(16, 'CartoDB.DarkMatter', 'tileLayer', 'baselayer', 'https://d.basemaps.cartocdn.com/dark_all/5/8/11.png', 'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', '{ attribution:\'&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a>\' }'),
(17, 'WMS-Layer-1', 'tileLayer', 'baselayer', 'https://ows.mundialis.de/services/service?&service=WMS&request=GetMap&layers=TOPO-WMS%2COSM-Overlay-WMS&styles=&format=image%2Fjpeg&transparent=false&version=1.1.1&width=256&height=256&srs=EPSG%3A3857', 'http://ows.mundialis.de/services/service?&service=WMS&request=GetMap&layers=TOPO-WMS%2COSM-Overlay-WMS', '{ attribution: "" }'),

(31, 'OpenRailway', 'tileLayer', 'overlayer', 'https://a.tiles.openrailwaymap.org/standard/4/10/5.png', 'https://{s}.tiles.openrailwaymap.org/standard/{z}/{x}/{y}.png', '{ attribution:\'Map data: &copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors | Map style: &copy; <a href="https://www.OpenRailwayMap.org">OpenRailwayMap</a> (<a href="https://creativecommons.org/licenses/by-sa/3.0/">CC-BY-SA</a>)\'}'),
(32, 'Stamen.TonerHybrid', 'tileLayer', 'overlayer', 'https://stamen-tiles-a.a.ssl.fastly.net/toner-hybrid/4/3/5.png', 'https://stamen-tiles-{s}.a.ssl.fastly.net/toner-hybrid/{z}/{x}/{y}{r}.{ext}', '{subdomains: "abcd",ext: "png", attribution:\'Map tiles by <a href="http://stamen.com">Stamen Design</a>, <a href="http://creativecommons.org/licenses/by/3.0">CC BY 3.0</a> &mdash; Map data &copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors\'}'),
(33, 'Stamen.TonerLines', 'tileLayer', 'overlayer', 'https://stamen-tiles-a.a.ssl.fastly.net/toner-lines/4/3/5.png', 'https://stamen-tiles-{s}.a.ssl.fastly.net/toner-lines/{z}/{x}/{y}{r}.{ext}', '{subdomains: "abcd",ext: "png", attribution:\'Map tiles by <a href="http://stamen.com">Stamen Design</a>, <a href="http://creativecommons.org/licenses/by/3.0">CC BY 3.0</a> &mdash; Map data &copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors\'}'),
(34, 'Stamen.TonerLabels', 'tileLayer', 'overlayer', 'https://stamen-tiles-b.a.ssl.fastly.net/toner-labels/4/3/6.png', 'https://stamen-tiles-{s}.a.ssl.fastly.net/toner-labels/{z}/{x}/{y}{r}.{ext}', '{subdomains: "abcd",ext: "png", attribution:\'Map tiles by <a href="http://stamen.com">Stamen Design</a>, <a href="http://creativecommons.org/licenses/by/3.0">CC BY 3.0</a> &mdash; Map data &copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors\'}'),
(35, 'Stamen.TopOSMFeatures', 'tileLayer', 'overlayer', 'https://stamen-tiles-c.a.ssl.fastly.net/toposm-features/6/16/25.png', 'https://stamen-tiles-{s}.a.ssl.fastly.net/toposm-features/{z}/{x}/{y}{r}.{ext}', '{subdomains: "abcd",ext: "png", attribution:\'Map tiles by <a href="http://stamen.com">Stamen Design</a>, <a href="http://creativecommons.org/licenses/by/3.0">CC BY 3.0</a> &mdash; Map data &copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors\'}');




# ----------------------------
# Table: datasource
# ----------------------------
DROP TABLE IF EXISTS data_source;
CREATE TABLE data_source
(
    id             int          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name           varchar(64)  NOT NULL,
    `desc`         varchar(128) DEFAULT NULL comment 'description',
    `group`        varchar(64)  NOT NULL DEFAULT 'default',
    type           varchar(16)  NOT NULL comment 's3, mysql, http url, build-in',
    url            varchar(255) NOT NULL comment 'host:port/db or s3 endpoint url',
    params         varchar(255) DEFAULT NULL comment "array like ['useUnicode=true']",
    username       varchar(64)  NOT NULL comment 'username or s3 access id',
    password       varchar(255) NOT NULL comment 'password or s3 secret key',
    version        varchar(64)  DEFAULT NULL comment 'db or s3 version',
    org_id         int          NOT NULL,
    `public`       boolean      NOT NULL DEFAULT false,
    locked         text         DEFAULT NULL comment "lock tables or files for protection, ['user', 'salary']",
    created_by  varchar(64)     NOT NULL,
    created_at  timestamp       NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_by  varchar(64)     DEFAULT NULL,
    updated_at  timestamp       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_source_org    foreign key(org_id)     REFERENCES sys_org(id)
) ENGINE = InnoDB;


INSERT INTO data_source (id, name,`desc`,`group`,`type`,url,params,username,password,version,org_id,public,locked,created_by,created_at,updated_by,updated_at) VALUES
	 (1, 'Footmart','Public source','demo','MySQL','datapie.cjiaoci4g12w.us-east-1.rds.amazonaws.com:3306/footmart','[{"name":"characterEncoding","value":"UTF-8"},{"name":"serverTimezone","value":"UTC"}]','admin','YWRtaW4jNTIw','8.0.35',1,1,NULL,'GavinZ','2024-09-02 00:22:42','GavinZ','2024-09-02 18:02:58'),
	 (2, 'classicmodels','Public source','demo','MySQL','datapie.cjiaoci4g12w.us-east-1.rds.amazonaws.com:3306/classicmodels','[{"name":"characterEncoding","value":"UTF-8"},{"name":"serverTimezone","value":"UTC"}]','admin','YWRtaW4jNTIw','8.0.35',1,1,NULL,'GavinZ','2024-09-02 17:45:07','GavinZ','2024-09-02 18:01:36'),
	 (3, 'Datastore','Public source','demo','MySQL','datapie.cjiaoci4g12w.us-east-1.rds.amazonaws.com:3306/datastore',NULL,'admin','YWRtaW4jNTIw','8.0.35',1,1,NULL,'GavinZ','2024-09-02 00:30:19','GavinZ','2024-09-02 18:01:36'),
	 (4, 'Foodmart','public sale data','AWS','MySQL','datapie.cjiaoci4g12w.us-east-1.rds.amazonaws.com:3306/foodmart','[{"name":"useUnicode","value":"true"},{"name":"characterEncoding","value":"UTF-8"},{"name":"serverTimezon","value":"UTC"}]','admin','YWRtaW4jNTIw','8.0.35',2,1,NULL,'Admin','2024-09-01 00:18:58','Admin','2024-09-02 18:01:36'),
	 (5, 'classicmodels','public sale data','AWS','MySQL','datapie.cjiaoci4g12w.us-east-1.rds.amazonaws.com:3306/classicmodels','[{"name":"useUnicode","value":"true"},{"name":"characterEncoding","value":"UTF-8"},{"name":"serverTimezon","value":"UTC"}]','admin','YWRtaW4jNTIw','8.0.35',2,1,NULL,'Admin','2024-09-02 17:47:56','Admin','2024-09-02 18:01:36'),
	 (6, 'Data Store','data for ML','AWS','MySQL','datapie.cjiaoci4g12w.us-east-1.rds.amazonaws.com:3306/datastore','[{"name":"useUnicode","value":"true"},{"name":"characterEncoding","value":"UTF-8"},{"name":"serverTimezon","value":"UTC"}]','admin','YWRtaW4jNTIw','8.0.35',2,1,NULL,'Admin','2024-08-31 19:03:10','Admin','2024-09-02 18:01:36');


# ----------------------------
# Table: data_import
# import files to database table or file server
# ----------------------------
DROP TABLE IF EXISTS data_import;
CREATE TABLE data_import
(
    id             int           NOT NULL AUTO_INCREMENT PRIMARY KEY,
    files          text          NOT NULL comment 'json array',
	type           varchar(16)   NOT NULL comment 'csv/json/xls',
	attrs          text          NOT NULL comment 'file attrs',
	fields		   text          NOT NULL comment 'field config',
	config         text          NOT NULL comment 'file config',
    source_id      int           NOT NULL comment 'datasource id',
    table_name     varchar(64)   NOT NULL comment 'table name',
	overwrite      boolean       NULL DEFAULT false comment 'overwrite: cleanup before load',
	`rows`         int           NULL comment 'total rows of files',
	records        int           NULL comment 'imported records',
	ftp_path       varchar(255)  NOT NULL comment 'temp path',
	status         varchar(16)   NULL DEFAULT 'waiting' comment 'success,processing,error,warning or waiting',
	detail         text          NULL comment 'failure info',
	`public`       boolean       NOT NULL DEFAULT false,
	org_id         int           NOT NULL,
    created_by     varchar(64)   NOT NULL,
    created_at     timestamp     DEFAULT CURRENT_TIMESTAMP,
    updated_by     varchar(64)   DEFAULT NULL,
    updated_at     timestamp     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	CONSTRAINT fk_import_org     foreign key(org_id)     REFERENCES sys_org(id)
) ENGINE = InnoDB;





# ----------------------------
# Table: dataset
# ----------------------------
DROP TABLE IF EXISTS viz_dataset;
CREATE TABLE viz_dataset
(
    id             int          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name           varchar(64)  NOT NULL,
    `desc`         varchar(128) DEFAULT NULL,
    `group`        varchar(64)  DEFAULT 'UnGrouped',
    variable       text         DEFAULT NULL comment 'json array', 
    content        text         DEFAULT NULL comment 'sql query or file name',
	final_query    text         DEFAULT NULL comment 'final query',
    field          text         DEFAULT NULL comment 'json array like [{name:"Name", type:"string", alias:"Username", metrics:true, hidden: true, order: -2}]',
    source_id      int          NOT NULL,
	org_id         int          NOT NULL,
    `public`       boolean      NOT NULL DEFAULT false,
    created_by  varchar(64)     NOT NULL,
    created_at  timestamp       NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_by  varchar(64)     DEFAULT NULL,
    updated_at  timestamp       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_dataset_src   foreign key(source_id)  REFERENCES data_source(id),
	CONSTRAINT fk_dataset_org    foreign key(org_id)     REFERENCES sys_org(id)
) ENGINE = InnoDB;


INSERT INTO viz_dataset (id, name,`desc`,`group`,variable,content,final_query,field,source_id,org_id,public,created_by,created_at,updated_by,updated_at) VALUES
	 (1, 'employee salary','Employee salary trend','footmart','[{"type":"date","name":"StartDay","value":"''1975-01-01''"}]','WITH
  xx AS (
    SELECT
      employee_id,
      position_title,
      birth_date,
      education_level,
      marital_status,
      gender,
      salary
    FROM
      employee
    WHERE
      birth_date > @StartDay
  )
SELECT
  SQL_CALC_FOUND_ROWS 
  employee_id,
  position_title,
  birth_date,
  education_level,
  marital_status,
  gender,
  salary
FROM
  xx
ORDER BY
  birth_date','',NULL,'[{"hidden":true,"type":"number","name":"employee_id"},{"type":"string","name":"position_title","alias":"Position"},{"type":"timestamp","name":"birth_date"},{"type":"string","name":"education_level","alias":"Education"},{"type":"string","name":"marital_status"},{"type":"string","name":"gender"},{"type":"number","name":"salary","metrics":true,"order":1}]',1,1,1,'GavinZ','2024-09-01 00:32:41','GavinZ','2024-09-02 18:42:16'),
	 (2, 'Store sales','sales analysis','footmart','[{"type":"timestamp","name":"StartDate","value":"''2011-01-01''"},{"type":"timestamp","name":"EndDate","value":"''2012-01-01''"}]','SELECT
  SQL_CALC_FOUND_ROWS DATE,
  MONTH,
  QUARTER,
  store,
  city,
  state,
  region,
  country,
  p_brand,
  p_cagegory,
  p_family,
  cost,
  sales,
  ''K'' AS unit,
  lat,
  lng,
  dir
FROM
  (
    SELECT
      DATE_FORMAT(THE_DATE, ''%Y-%m-%d'') AS DATE,
      THE_MONTH AS MONTH,
      QUARTER AS QUARTER,
      store_name AS store,
      STORE_CITY AS city,
      SALES_STATE AS state,
      STORE_country AS country,
      SALES_REGION AS region,
      BRAND_NAME AS p_brand,
      PRODUCT_CATEGORY AS p_cagegory,
      PRODUCT_FAMILY AS p_family,
      STORE_COST AS cost,
      STORE_SALES AS sales,
      lat,
      lng,
      CASE
        WHEN SALES_REGION LIKE ''%West%'' THEN ''left''
        ELSE ''right''
      END AS dir
    FROM
      sales_fact_sample AS sales
      JOIN store AS store ON sales.store_id = store.store_id
      JOIN time_by_day AS DATE ON sales.time_id = DATE.time_id
      JOIN product AS product ON sales.PRODUCT_ID = product.PRODUCT_ID
  ) x
WHERE
  DATE BETWEEN $StartDate AND $EndDate
ORDER BY
  DATE
  ','SELECT SQL_CALC_FOUND_ROWS DATE, MONTH, QUARTER, store, city
	, state, region, country, p_brand, p_cagegory
	, p_family, cost, sales, ''K'' AS unit, lat
	, lng, dir
FROM (
	SELECT DATE_FORMAT(THE_DATE, ''%Y-%m-%d'') AS DATE, THE_MONTH AS MONTH, QUARTER AS QUARTER
		, store_name AS store, STORE_CITY AS city, SALES_STATE AS state, STORE_country AS country, SALES_REGION AS region
		, BRAND_NAME AS p_brand, PRODUCT_CATEGORY AS p_cagegory, PRODUCT_FAMILY AS p_family, STORE_COST AS cost, STORE_SALES AS sales
		, lat, lng
		, CASE 
			WHEN SALES_REGION LIKE ''%West%'' THEN ''left''
			ELSE ''right''
		END AS dir
	FROM sales_fact_sample sales
		JOIN store store ON sales.store_id = store.store_id
		JOIN time_by_day DATE ON sales.time_id = DATE.time_id
		JOIN product product ON sales.PRODUCT_ID = product.PRODUCT_ID
) x
WHERE DATE BETWEEN ''2011-01-01'' AND ''2012-01-01''
ORDER BY DATE',NULL,'[{"name":"DATE","type":"string"},{"name":"MONTH","type":"string"},{"name":"QUARTER","type":"string"},{"name":"store","type":"string"},{"name":"city","type":"string"},{"name":"state","type":"string"},{"name":"region","type":"string"},{"name":"country","type":"string"},{"name":"p_brand","type":"string"},{"name":"p_cagegory","type":"string"},{"name":"p_family","type":"string"},{"name":"cost","type":"number","metrics":true},{"name":"sales","type":"number","metrics":true},{"name":"unit","type":"string"},{"name":"lat","type":"number"},{"name":"lng","type":"number"},{"name":"dir","type":"string"}]',1,1,1,'GavinZ','2024-09-02 18:39:45',NULL,'2024-09-02 18:43:44'),
	 (3, 'Order info','customer, order and products','footmart','[{"type":"timestamp","name":"startDate","value":"''2004-01-01''"},{"type":"timestamp","name":"endDate","value":"''2004-12-31''"}]','select 
orderDate,
  customerName,
  city,
  state,
  country,
 amount,
  productName,
  productLine,
  MSRP,
  buyPrice
from
(
SELECT
  orderDate,
  customerName,
  city,
  state,
  country,
  quantityOrdered * priceEach AS amount,
  productCode
FROM
  orders o
  JOIN customers c USING (customerNumber)
  JOIN orderdetails d USING (orderNumber)
  where orderDate >= $startDate and orderDate <= $endDate
  )x 
  join products using(productCode)
  ORDER BY
  orderDate','SELECT orderDate, customerName, city, state, country
	, amount, productName, productLine, MSRP, buyPrice
FROM (
	SELECT orderDate, customerName, city, state, country
		, quantityOrdered * priceEach AS amount, productCode
	FROM orders o
		JOIN customers c USING (customerNumber)
		JOIN orderdetails d USING (orderNumber)
	WHERE orderDate >= ''2004-01-01''
		AND orderDate <= ''2004-12-31''
) x
	JOIN products USING (productCode)
ORDER BY orderDate','[{"name":"orderDate","type":"timestamp"},{"name":"customerName","type":"string"},{"name":"city","type":"string"},{"name":"state","type":"string"},{"name":"country","type":"string"},{"name":"amount","type":"number","metrics":true},{"name":"productName","type":"string"},{"name":"productLine","type":"string"},{"name":"MSRP","type":"number","metrics":true},{"name":"buyPrice","type":"number","metrics":true}]',2,1,1,'GavinZ','2024-09-02 18:39:51',NULL,'2024-09-02 18:45:16');




# ----------------------------
# 动态sql支持变量，条件，循环等语法（Maybe Velocity, MyBatis）
# sql支持pipeline方式，顺序执行依次返回，前面的返回值可以影响后面的执行
# 用ACE替换codemirror，支持行号显示，语法高亮，输入提示，格式化等
# 预览数据量限制
# ----------------------------


# ----------------------------
# Table: data view
# ----------------------------
DROP TABLE IF EXISTS viz_view;
CREATE TABLE viz_view
(
    id             int          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name           varchar(64)  NOT NULL,
    `desc`         varchar(128) comment 'description',
    `group`        varchar(64)  DEFAULT 'UnGrouped',
    type           varchar(64)  NOT NULL comment 'view type like pie_chart, trend_chart',
    dim            varchar(255) NOT NULL comment '[string]',
	relation       varchar(255) DEFAULT NULL comment '[string]',
	location       varchar(255) DEFAULT NULL comment '[string]',
    metrics        varchar(255) NOT NULL comment '[string]',
	agg			   varchar(16)  comment 'metrics aggregation, like count/sum/avg',
	prec           int          DEFAULT NULL comment 'agg precision',
	filter         text         DEFAULT NULL comment 'json object, like {age:"age>10"}',
	sorter         text         DEFAULT NULL comment '[string], like ["age asc"]',
    variable       text         DEFAULT NULL comment 'json array',
	calculation    text         DEFAULT NULL comment 'json array, like [{name:"a",type:"string",value:"b+c"]',
    model          text         NOT NULL comment 'json object of view config',
    lib_name       varchar(16)  NOT NULL DEFAULT 'G2Plot' comment 'chart lib, like G2Plot/ECharts/AmCharts',
    lib_ver        varchar(16)  NULL comment 'major.minor',
    lib_cfg        text         NULL comment 'json config',
    dataset_id     int          NOT NULL,
	org_id         int          NOT NULL,
    `public`       boolean      NOT NULL DEFAULT false,
    created_by  varchar(64)     NOT NULL,
    created_at  timestamp       NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_by  varchar(64)     DEFAULT NULL,
    updated_at  timestamp       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_view_set      foreign key(dataset_id)        REFERENCES viz_dataset(id),
	CONSTRAINT fk_view_org      foreign key(org_id)     REFERENCES sys_org(id)
) ENGINE = InnoDB;


INSERT INTO viz_view (id, name,`desc`,`group`,`type`,dim,relation,location,metrics,agg,prec,`filter`,sorter,variable,calculation,model,lib_name,lib_ver,lib_cfg,dataset_id,org_id,public,created_by,created_at,updated_by,updated_at) VALUES
	 (1, 'Employee Salary Trend','Employee Salary Trending','footmart','line_chart','["birth_date"]',NULL,NULL,'["salary"]','sum',NULL,NULL,'["DATE ASC"]','[]',NULL,'{"legend":{"enabled":true},"tooltip":{"marker":true},"title":"","axis":{"slider":true},"advance":{"readonly":true},"auxiliary":{"annotations":[{"color":"black","start":"[''min'',''median'']","type":"line","end":"[''max'',''median'']","text":{"color":"blue","content":"Average"}},{"color":"red","start":"[''min'',''median'']","type":"regionFilter","end":"[''max'',''0'']"},{"type":"text","text":{"color":"green","content":"Salary Trend","fontSize":24,"position":"[''20%'',''10%'']"}},{"color":"gray","start":"[''1976-10-05'',''0'']","type":"region","end":"[''1978-07-02'',''max'']","opacity":0.1}]},"theme":"light","style":{"appear":[]},"metricsField":[{"type":"number","name":"salary","id":6}],"dimField":[{"type":"timestamp","name":"birth_date","id":2}]}','G2Plot','2.4','{"chartType":"Line","config":{"slider":{"trendCfg":{"isArea":true},"start":0,"end":1},"yField":"salary","annotations":[{"start":["min","median"],"type":"line","end":["max","median"],"style":{"stroke":"black","lineDash":[2,2]},"text":{"content":"Average","style":{"textBaseline":"bottom","fill":"blue"}},"id":"0"},{"color":"red","start":["min","median"],"type":"regionFilter","end":["max","0"],"id":"1"},{"type":"text","content":"Salary Trend","style":{"textBaseline":"bottom","fill":"green","fontSize":24},"position":["20%","10%"],"id":"2"},{"start":["1976-10-05","0"],"type":"region","end":["1978-07-02","max"],"style":{"fillOpacity":0.1,"fill":"yellow","lineDash":[2,2]},"id":"3"}],"xField":"birth_date","animation":{"update":{"animation":"path-in"}},"area":false,"isStack":false,"smooth":true,"theme":"default"}}',1,1,1,'GavinZ','2024-09-02 19:16:10','GavinZ','2024-09-02 19:59:20'),
	 (2, 'Region sales by Q',NULL,'footmart','heatmap','["region","QUARTER"]',NULL,NULL,'["sales"]','sum',NULL,NULL,NULL,'[]',NULL,'{}','G2Plot','2.4','{"chartType":"Heatmap","config":{"xField":"region","yField":"QUARTER","colorField":"sales"}}',2,1,1,'GavinZ','2024-09-02 19:18:25','GavinZ','2024-09-02 19:59:21'),
	 (3, 'Month sale comparation',NULL,'footmart','column_chart','["MONTH"]',NULL,NULL,'["sales"]','sum',NULL,NULL,NULL,'[]',NULL,'{}','G2Plot','2.4','{"chartType":"Column","config":{"xField":"MONTH","yField":"sales"}}',2,1,1,'GavinZ','2024-09-02 19:19:33','GavinZ','2024-09-02 19:59:22'),
	 (4, 'State sale pie',NULL,'footmart','pie_chart','["state"]',NULL,NULL,'["cost"]','sum',NULL,NULL,NULL,'[]',NULL,'{}','G2Plot','2.4','{"chartType":"Pie","config":{"angleField":"cost","colorField":"state"}}',2,1,1,'GavinZ','2024-09-02 19:20:35','GavinZ','2024-09-02 19:59:23'),
	 (5, 'Region stores',NULL,'footmart','pie_chart','["region"]',NULL,NULL,'["store"]','count',NULL,NULL,NULL,'[]',NULL,'{}','G2Plot','2.4','{"chartType":"Pie","config":{"angleField":"store","colorField":"region","innerRadius":0.5}}',2,1,1,'GavinZ','2024-09-02 19:41:35','GavinZ','2024-09-02 19:59:23'),
	 (6, 'family sales bar','','footmart','bar_chart','["p_family","p_cagegory"]',NULL,NULL,'["sales"]','sum',NULL,NULL,NULL,'[]',NULL,'{}','G2Plot','2.4','{"chartType":"Bar","config":{"xField":"sales","yField":"p_family","seriesField":"p_cagegory","isStack":true}}',2,1,1,'GavinZ','2024-09-02 19:49:25','GavinZ','2024-09-02 19:59:24'),
	 (7, 'state cost map',NULL,'footmart','choropleth_map','["country","region","state"]',NULL,NULL,'["cost"]','sum',NULL,NULL,NULL,'[]',NULL,'{}','Leaflet','1.7','{"chartType":"Choropleth","config":{"baselayer":["Stamen.TonerBackground"],"overlayer":[],"toolkit":{"fullscreen":false,"seek":false,"search":false,"export":false,"open":false,"locator":false,"coordinator":true,"scale":false,"player":false},"choropleth":{"refLayer":"us-stats.geojson","refJoin":["abbr","state"],"colorField":"cost","colorSteps":4,"colorScale":["#f5deb3","#f89477","#fc4a3c","#ff0000"],"fillOpacity":0.3,"borderColor":"gray","popup":"{=name}: {=sales}","colorMap":[{"color":"#f5deb3","value":[0,1],"label":"0~1"},{"color":"#f89477","value":[1,2],"label":"1~2"},{"color":"#fc4a3c","value":[2,3],"label":"2~3"},{"color":"#ff0000","value":[3,8],"label":"3~8"}]},"tooltip":"cost: {=cost}"}}',2,1,1,'GavinZ','2024-09-02 19:23:57','GavinZ','2024-09-02 19:59:25'),
	 (8, 'Region sales map',NULL,'footmart','marker_map','["region","lat","lng"]',NULL,NULL,'["sales"]','sum',NULL,NULL,NULL,'[]',NULL,'{}','Leaflet','1.7','{"chartType":"Marker","config":{"baselayer":["OSM.Mapnik"],"overlayer":[],"latField":"lat","lngField":"lng","toolkit":{"fullscreen":false,"seek":false,"search":false,"export":false,"open":false,"locator":false,"coordinator":true,"scale":false,"player":false},"marker":{"cluster":{"enabled":true},"shapeField":"region","shapeMap":[{"shape":"circle","value":"CenterWest","label":"CenterWest"},{"shape":"square","value":"WestCoast","label":"WestCoast"},{"shape":"star","value":"EastCost","label":"EastCost"}],"colorField":"sales","colorMap":[{"color":"#008000","value":[5,131],"label":"5~131"},{"color":"#ff0000","value":[131,226],"label":"131~226"}]},"tooltip":"sales: {=sales}"}}',2,1,1,'GavinZ','2024-09-02 19:25:53','GavinZ','2024-09-02 19:59:25'),
	 (9, 'cost migration map',NULL,'footmart','migration_map','["region","lat","lng"]',NULL,NULL,'["cost"]','sum',NULL,NULL,NULL,'[]',NULL,'{}','Leaflet','1.7','{"chartType":"Migration","config":{"baselayer":["OSM.Mapnik"],"overlayer":[],"latLngField":["lat","lng"],"toField":["toLat","toLng"],"labelFields":["country","store"],"toolkit":{"fullscreen":false,"seek":false,"search":false,"export":false,"open":false,"locator":false,"coordinator":true,"scale":false,"player":false},"marker":{"pulse":true,"textVisible":false,"pulseRadius":20},"line":{"width":2,"arrowSize":10,"colorField":"cost","colorMap":[{"color":"#008000","value":[0,1],"label":"0~1"},{"color":"#555500","value":[1,2],"label":"1~2"},{"color":"#aa2b00","value":[2,3],"label":"2~3"},{"color":"#ff0000","value":[3,8],"label":"3~8"}]}}}',2,1,1,'GavinZ','2024-09-02 19:28:32','GavinZ','2024-09-02 19:59:29'),
	 (10, 'Cost movement map',NULL,'footmart','movement_map','["lat","lng"]',NULL,NULL,'["cost"]','sum',NULL,NULL,NULL,'[]',NULL,'{}','Leaflet','1.7','{"chartType":"Movement","config":{"baselayer":["OSM.Mapnik"],"overlayer":[],"latField":"lat","lngField":"lng","toolkit":{"fullscreen":false,"seek":false,"search":false,"export":false,"open":false,"locator":false,"coordinator":true,"scale":false,"player":false},"movement":{"color":"#00ffff","pulseColor":"purple","weight":3,"delay":600,"dash":[5,50],"colorField":"cost","colorMap":[{"color":"#008000","value":[0,1],"label":"0~1"},{"color":"#555500","value":[1,2],"label":"1~2"},{"color":"#aa2b00","value":[2,3],"label":"2~3"},{"color":"#ff0000","value":[3,8],"label":"3~8"}]},"tooltip":"cost: {=cost}"}}',2,1,1,'GavinZ','2024-09-02 19:35:21','GavinZ','2024-09-02 19:59:30'),
	 (11, 'Region store topology',NULL,'footmart','tree_net','["region","state","store"]',NULL,NULL,'["cost"]','sum',NULL,NULL,NULL,'[]',NULL,'{}','Cytoscape','3.2','{"chartType":"Tree","config":{"nameField":[],"toolkit":{"fullscreen":true,"export":true},"layout":[{"alias":"Mrtree","name":"elk","options":{"fit":true,"animate":true,"elk":{"algorithm":"mrtree","edgeRoutingMode":"AVOID_OVERLAP","aspectRatio":0,"nodeNode":20}}}],"node":{"icon":{"shapeField":"nodeLevel","shapeMap":[{"shape":"star","value":0,"label":"region"},{"shape":"heart","value":1,"label":"state"},{"shape":"bell","value":2,"label":"store"}],"color":"#20B2AA","colorField":"cost","colorMap":[{"color":"#008000","value":[2,53],"label":"2~53"},{"color":"#ff0000","value":[53,92],"label":"53~92"}],"tooltip":"cost: {=cost}","labelSize":6},"body":{"shape":"ellipse","color":"#cccccc"}},"edge":{"line":{"style":"solid","color":"#cccccc"},"marker":{"width":1,"type":"haystack"}},"aux":{"animation":true,"collapse":false,"cluster":false,"compound":false,"cumsum":false,"highlight":"neighbors"},"dataMode":"FieldLevel"}}',2,1,1,'GavinZ','2024-09-02 19:38:11','GavinZ','2024-09-02 19:59:30'),
	 (12, 'Double tree stores',NULL,'footmart','tree_net','["country","state","store"]',NULL,NULL,'["sales"]','sum',NULL,NULL,NULL,'[]',NULL,'{}','Cytoscape','3.2','{"chartType":"Tree","config":{"nameField":[],"toolkit":{"fullscreen":true,"export":true},"layout":[{"alias":"DoubleTree","name":"DoubleTree","gojs":true,"options":{"widget":"gojs","balance":true,"dirField":"dir"},"checked":true}],"node":{"icon":{"shapeField":"nodeLevel","shapeMap":[{"shape":"star","value":0,"label":"country"},{"shape":"heart","value":1,"label":"state"},{"shape":"bell","value":2,"label":"store"}],"color":"#20B2AA","colorField":"sales","colorMap":[{"color":"#008000","value":[1,3],"label":"1~3"},{"color":"#555500","value":[3,5],"label":"3~5"},{"color":"#aa2b00","value":[5,8],"label":"5~8"},{"color":"#ff0000","value":[8,20],"label":"8~20"}],"tooltip":"sales: {=sales}","labelSize":6},"body":{"shape":"ellipse","color":"#cccccc"}},"edge":{"line":{"style":"solid","color":"#cccccc"},"marker":{"width":1,"type":"haystack"}},"aux":{"animation":true,"collapse":false,"cluster":false,"compound":false,"cumsum":false,"highlight":"neighbors"},"dataMode":"FieldLevel"}}',2,1,1,'GavinZ','2024-09-02 19:39:46','GavinZ','2024-09-02 19:59:31'),
	 (13, 'product topology',NULL,'footmart','star_net','["productLine","productName"]',NULL,NULL,'["amount"]','sum',NULL,NULL,NULL,'[]',NULL,'{}','Cytoscape','3.2','{"chartType":"Star","config":{"nameField":[],"toolkit":{"fullscreen":true,"export":true},"layout":[{"alias":"Fcose","name":"fcose","options":{"fit":true,"animate":true}}],"node":{"icon":{"tooltip":"amount: {=amount}"},"body":{"shapeField":"nodeLevel","shapeMap":[{"shape":"ellipse","value":0,"label":"productLine"},{"shape":"triangle","value":1,"label":"productName"}],"color":"#20B2AA","colorField":"amount","colorMap":[{"color":"#008000","value":[643,2018],"label":"643~2018"},{"color":"#555500","value":[2018,2790],"label":"2018~2790"},{"color":"#aa2b00","value":[2790,3905],"label":"2790~3905"},{"color":"#ff0000","value":[3905,8353],"label":"3905~8353"}],"shape":"ellipse"}},"edge":{"line":{"style":"solid","color":"gray"},"marker":{"width":1,"type":"haystack"}},"aux":{"animation":true,"collapse":false,"cluster":false,"compound":false,"cumsum":false},"dataMode":"FieldLevel"}}',3,1,1,'GavinZ','2024-09-02 19:54:18','GavinZ','2024-09-02 19:59:32');



# ----------------------------
# Table: data report
# ----------------------------
DROP TABLE IF EXISTS viz_report;
CREATE TABLE viz_report
(
    id             int          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name           varchar(64)  NOT NULL,
    `desc`         varchar(128) DEFAULT NULL,
    `group`        varchar(64)  DEFAULT 'UnGrouped',
    type           varchar(64)  NOT NULL comment 'like multiple, story',
    pages          text         NOT NULL comment 'json array of page config',
	org_id         int          NOT NULL,
    `public`       boolean      NOT NULL DEFAULT false,
    pub_pub        boolean      NOT NULL DEFAULT true comment 'publish public',
    menu_id        int          DEFAULT NULL,
	view_ids       text         NOT NULL comment 'view id list',
    created_by  varchar(64)     NOT NULL,
    created_at  timestamp       NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_by  varchar(64)     DEFAULT NULL,
    updated_at  timestamp       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_report_menu   foreign key(menu_id)        REFERENCES sys_menu(id),
	CONSTRAINT fk_report_org    foreign key(org_id)     REFERENCES sys_org(id)
) ENGINE = InnoDB;


INSERT INTO viz_report (id, name,`desc`,`group`,`type`,pages,org_id,public,pub_pub,menu_id,view_ids,created_by,created_at,updated_by,updated_at) VALUES
	 (1, 'Footmart analysis',NULL,'demo','report','[{"border":"dashed","filter":[],"label":true,"layout":"free","portrait":false,"toolbar":true,"grid":[{"id":1,"name":"footmart/Employee Salary Trend","type":"view","i":0,"h":8,"w":12,"x":12,"y":0},{"id":2,"name":"footmart/Region sales by Q","type":"view","i":1,"h":8,"w":12,"x":0,"y":0},{"id":11,"name":"footmart/Region store topology","type":"view","i":2,"h":10,"w":12,"x":12,"y":8},{"id":7,"name":"footmart/state cost map","type":"view","i":3,"h":10,"w":12,"x":0,"y":8}]}]',1,1,1,1,'[7]','GavinZ','2024-09-02 20:49:02','GavinZ','2024-09-02 20:59:28');
	 

# ----------------------------
# Table: dataset
# ----------------------------
DROP TABLE IF EXISTS ml_dataset;
CREATE TABLE ml_dataset
(
    id             int          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name           varchar(64)  NOT NULL,
    `desc`         varchar(128) DEFAULT NULL,
    `group`        varchar(64)  DEFAULT 'default',
    variable       text         DEFAULT NULL comment 'json array', 
	type		   varchar(16)  DEFAULT 'data' comment 'data, text, image, audio, timeseries',
    content        text         DEFAULT NULL comment 'file name or sql query',
	final_query    text         DEFAULT NULL comment 'final query',
    fields         text         NOT NULL comment 'json array like [{name:"age", type:"number", cat:"conti", weight:92, target:false, omit: false}]',
	target		   text         DEFAULT NULL comment 'target array',
	transform	   text         DEFAULT NULL comment '[{operation:"size", param:"300"}]',
	f_count        int          DEFAULT NULL comment'feature count',
	volume         int          DEFAULT NULL comment'data volume',
    source_id      int          NOT NULL,
	org_id         int          NOT NULL,
    `public`       boolean      NOT NULL DEFAULT false,
    created_by  varchar(64)     NOT NULL,
    created_at  timestamp       NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_by  varchar(64)     DEFAULT NULL,
    updated_at  timestamp       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_mldataset_src   foreign key(source_id)  REFERENCES data_source(id),
	CONSTRAINT fk_mldataset_org    foreign key(org_id)     REFERENCES sys_org(id)
) ENGINE = InnoDB;


INSERT INTO ml_dataset (id, name,`desc`,`group`,variable,type,query,final_query,fields,target,`transform`,f_count,source_id,org_id,public,created_by,created_at,updated_by,updated_at) VALUES
	 (1, 'iris',NULL,'demo','[]','data','select * from iris','SELECT *
FROM iris','[{"name":"petal_length","std":1.765,"type":"number","attr":"conti"},{"name":"petal_width","std":0.762,"type":"number","attr":"conti"},{"name":"sepal_length","std":0.828,"type":"number","attr":"conti"},{"name":"sepal_width","std":0.436,"type":"number","attr":"conti"},{"name":"target","std":0.819,"type":"number","attr":"disc","target":true},{"name":"uid","std":43.445,"type":"number","omit":true,"attr":"disc"}]','["target"]',NULL,NULL,3,1,1,'GavinZ','2024-09-02 21:07:33','GavinZ','2024-09-02 21:13:49'),
	 (2, 'house price',NULL,'demo','[]','data','select * from bostonhousing','SELECT *
FROM bostonhousing','[{"name":"age","std":28.149,"type":"number","attr":"conti"},{"name":"b","std":91.295,"type":"number","attr":"conti"},{"name":"chas","std":0.254,"type":"number","attr":"disc"},{"name":"crim","std":8.602,"type":"number","attr":"conti"},{"name":"dis","std":2.106,"type":"number","attr":"conti"},{"name":"indus","std":6.86,"type":"number","attr":"conti"},{"name":"lstat","std":7.141,"type":"number","attr":"conti"},{"name":"medv","std":9.197,"type":"number","attr":"conti","target":true,"miss":"drop"},{"name":"nox","std":0.116,"type":"number","attr":"conti"},{"name":"ptratio","std":2.165,"type":"number","attr":"conti"},{"name":"rad","std":8.707,"type":"number","attr":"disc"},{"name":"rm","std":0.703,"type":"number","attr":"conti"},{"name":"tax","std":168.537,"type":"number","attr":"disc"},{"name":"uid","std":146.214,"type":"number","omit":true,"attr":"disc"},{"name":"zn","std":23.335,"type":"number","attr":"disc"}]','["medv"]',NULL,NULL,3,1,1,'GavinZ','2024-09-02 21:12:57','GavinZ','2024-09-02 21:13:49');

# -------------------------
# Table: eda
# ----------------------------
DROP TABLE IF EXISTS ml_eda;
CREATE TABLE ml_eda
(
    id             int          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name           varchar(64)  NOT NULL,
    `desc`         varchar(128) DEFAULT NULL,
    `group`        varchar(64)  DEFAULT 'default',
    config         text         DEFAULT NULL comment 'single variable distribution',
    dataset_id     int          NOT NULL,
	org_id         int          NOT NULL,
    `public`       boolean      NOT NULL DEFAULT false,
    created_by  varchar(64)     NOT NULL,
    created_at  timestamp       NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_by  varchar(64)     DEFAULT NULL,
    updated_at  timestamp       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_eda_set      foreign key(dataset_id)        REFERENCES ml_dataset(id),
	CONSTRAINT fk_eda_org      foreign key(org_id)     REFERENCES sys_org(id)
) ENGINE = InnoDB;

INSERT INTO ml_eda (id, name,`desc`,`group`,config,dataset_id,org_id,public,created_by,created_at,updated_by,updated_at) VALUES
	 (1, 'Iris eda',NULL,'explorer','{"overall":{"pid":"stat"},"box":{"pid":"stat","outlier":"outliers"},"outlier":{"pid":"stat","method":"quantile","iqr":1.8}}',1,1,1,'GavinZ','2024-09-02 21:16:31','GavinZ','2024-09-02 21:16:39');



# ----------------------------
# Table: ml_algo
# ----------------------------
DROP TABLE IF EXISTS ml_algo;
CREATE TABLE ml_algo
(
    id             int           NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name           varchar(64)   NOT NULL,
    `desc`         varchar(128)  DEFAULT NULL comment 'description',
    `group`        varchar(64)   DEFAULT 'default',
    tags      	   varchar(128)  DEFAULT NULL comment 'like ["iris", "image", "pytorch"]',
	category       varchar(32)   NOT NULL comment 'classifier,regressor,vison,audio,inno...',
	algo_name      varchar(64)   comment 'base algo',
	data_cfg       text          DEFAULT NULL comment 'dataset config',
    train_cfg      text          DEFAULT NULL comment 'train and evaluation config',
	src_code       text          DEFAULT NULL comment 'source code',  
	org_id         int           NOT NULL,
    `public`       boolean       NOT NULL DEFAULT false,
    created_by  varchar(64)      NOT NULL,
    created_at  timestamp        NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_by  varchar(64)      DEFAULT NULL,
    updated_at  timestamp        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	CONSTRAINT fk_algo_org       foreign key(org_id)     REFERENCES sys_org(id)
) ENGINE = InnoDB;


INSERT INTO ml_algo (id, name,`desc`,`group`,framework,frame_ver,category,algo_name,data_cfg,train_cfg,src_code,org_id,public,created_by,created_at,updated_by,updated_at) VALUES
	 (1, 'RandomForestClassifier','','sklearn','sklearn','3.11','clf','RandomForestClassifier','{"datasetId":1,"evalRatio":0.2,"shuffle":false}','{"gpu":false,"strategy":"BasicVariantGenerator","trials":2,"epochs":1,"timeout":5,"params":{"n_estimators":"[5,10,15]","max_depth":"(4,10,2)"},"score":"accuracy","threshold":0.9}','
import ray
import mlflow
import matplotlib
from mlflow.utils.mlflow_tags import MLFLOW_RUN_NAME, MLFLOW_USER
from sklearn.ensemble import RandomForestClassifier
from sklearn import metrics

class CustomTrain:
  def train(config: dict, data: dict):
    setup_mlflow()

    estimator = RandomForestClassifier(n_estimators=config[''n_estimators''],max_depth=config[''max_depth''])
    for epoch in range(config.get("epochs", 1)):
      estimator.fit(data[''x''], data[''y''])
      accuracy_fn = metrics.get_scorer(''accuracy'')
      accuracy = accuracy_fn(estimator, data[''x''], data[''y''])
      ray.train.report({"accuracy": accuracy})
',1,0,'GavinZ','2024-09-02 01:48:57','GavinZ','2024-09-02 21:17:45'),
	 ('SVR','regression','sklearn','sklearn','3.11','reg','SVR','{"datasetId":2,"evalRatio":0.2,"shuffle":true}','{"gpu":false,"strategy":"BasicVariantGenerator","trials":3,"epochs":1,"timeout":5,"params":{"C":"[0.5,1,1.5]","gamma":"[\"auto\",\"rbf\",\"ploy\"]"},"score":"r2","threshold":0.5}','import ray
import mlflow
import matplotlib
from mlflow.utils.mlflow_tags import MLFLOW_RUN_NAME, MLFLOW_USER
from sklearn.svm import SVR
from sklearn import metrics

class CustomTrain:
  def train(config: dict, data: dict):
    setup_mlflow()

    estimator = SVR(C=config[''C''],gamma=config[''gamma''])
    for epoch in range(config.get("epochs", 1)):
      estimator.fit(data[''x''], data[''y''])
      r2_fn = metrics.get_scorer(''r2'')
      r2 = r2_fn(estimator, data[''x''], data[''y''])
      ray.train.report({"r2": r2})',1,0,'GavinZ','2024-09-02 22:07:26','GavinZ','2024-09-02 22:08:20');



# ----------------------------
# Table: ml_workflow
# ----------------------------
DROP TABLE IF EXISTS ml_workflow;
CREATE TABLE ml_workflow
( 
    id             int           NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name           varchar(64)   NOT NULL,
    `desc`         varchar(128)  DEFAULT NULL comment 'description',
    `group`        varchar(64)   DEFAULT 'UnGrouped',
	config         text          DEFAULT NULL comment 'ML training config', 
	workflow       text          DEFAULT NULL comment 'flow config with json', 
    canvas         text          DEFAULT NULL comment 'background and grid config',
    x6_ver         varchar(8)    DEFAULT NULL comment 'antvX6 version',
    version        varchar(8)    DEFAULT NULL comment 'workflow version',
	org_id         int           NOT NULL,
    `public`       boolean       NOT NULL DEFAULT false,
    created_by     varchar(64)   NOT NULL,
    created_at     timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_by     varchar(64)   DEFAULT NULL,
    updated_at     timestamp     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	CONSTRAINT fk_flow_org       foreign key(org_id)     REFERENCES sys_org(id)
) ENGINE = InnoDB;

INSERT INTO ml_workflow (id, name,`desc`,`group`,config,workflow,canvas,x6_ver,version,org_id,public,created_by,created_at,updated_by,updated_at) VALUES
	 (1, 'sk classifer','Classification','demo','{"timeout":0}','{"nodes":[{"id":"11482720","shape":"ExeNode","data":{"type":"source","kind":"dataset","title":"Dataset","data":{"id":1},"text":"demo/iris"},"position":{"x":200,"y":260}},{"id":"11482722","shape":"ExeNode","data":{"type":"proc","kind":"clean","title":"Cleaning","data":{"miss":"drop","duplicate":"drop","outlier":"drop"},"text":"Miss: Drop"},"position":{"x":580,"y":120}},{"id":"11482726","shape":"ExeNode","data":{"type":"proc","kind":"scale","title":"Scaling","data":[{"sf":"aaa","method":"std","title":"aaa : STD"}],"text":"1 fields"},"position":{"x":580,"y":260}},{"id":"11482757","shape":"ExeNode","data":{"type":"fe","kind":"select","title":"Feature Selection","data":{"feature_selection_threshold":1,"multicollinearity_threshold":0,"pca_method":0,"pca_components":0,"ignore_low_variance":1,"cluster_iter":0,"seed":123,"metric":"gain"},"text":"pyCaret"},"position":{"x":580,"y":440}},{"id":"11482761","shape":"ExeNode","data":{"type":"ml","kind":"clf","title":"Classification","data":{"method":"pycaret","blacklist":["lr","qda","catboost"],"metric":"accuracy","fold":5},"text":"pyCaret"},"position":{"x":949,"y":260}}],"edges":[{"id":"11482742","shape":"edge","source":{"cell":"11482720"},"target":{"cell":"11482722"}},{"id":"11482744","shape":"edge","source":{"cell":"11482722"},"target":{"cell":"11482726"}},{"id":"11482758","shape":"edge","source":{"cell":"11482726"},"target":{"cell":"11482757"}},{"id":"11482763","shape":"edge","source":{"cell":"11482757"},"target":{"cell":"11482761"}}]}','{"width":600,"height":600,"bg":{"color":"#212121"},"grid":{"type":"dot","size":20,"color":"#FFF59C","thickness":1},"edge":{"color":"#a9a9a9","router":"smooth"}}',NULL,'0',1,0,'GavinZ','2024-09-02 23:54:55','GavinZ','2024-09-02 23:57:19');



# ----------------------------
# Table: ml_experiment
# ----------------------------
DROP TABLE IF EXISTS ml_experiment;
CREATE TABLE ml_experiment
(
    id             int           NOT NULL AUTO_INCREMENT PRIMARY KEY,
	ml_id          int           NOT NULL,
	type		   varchar(16)   NOT NULL comment 'algo or workflow',
	name		   varchar(64)   NOT NULL comment 'runs name without trial NO.',
	`desc`         varchar(128)  DEFAULT NULL comment 'description',
	dataset		   text          NOT NULL comment '{id:12, fields:[]}',
	algo		   text          NOT NULL comment '{name,framework,frameVer,srcCode}',
	train		   text          NOT NULL comment '{params:[],evals:[]}',
	trials		   text          DEFAULT NULL comment '[{uuid:123,params:{},evals{}}]',
	status         int           DEFAULT 0 comment '0:waiting, 1~99:progress, 100:done, 101:canceled, -1:failed',
	user_id        int			 NOT NULL,
	org_id         int           NOT NULL,
    start_at  	   timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
	end_at         timestamp     DEFAULT NULL,
	CONSTRAINT fk_exper_user      foreign key(user_id)    REFERENCES sys_user(id),
	CONSTRAINT fk_exper_org       foreign key(org_id)     REFERENCES sys_org(id)
) ENGINE = InnoDB;

INSERT INTO ml_experiment (id, name, `desc`, algo_id, dataset, algo, train, trials, status, user_id, org_id, start_at, end_at)
VALUES (1, 17, 50, '[{id:123, params: {a:1, b:2}, eval:{loss:0.2}}]', 2, 0, 3, 1, null);




# ----------------------------
# Table: ai_model
# ----------------------------
DROP TABLE IF EXISTS ai_model;
CREATE TABLE ai_model
(
    id             int            NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name           varchar(64)    NOT NULL,
    `desc`         varchar(128)   DEFAULT NULL comment 'description',
    area           varchar(32)    NOT NULL comment 'image, video, audio, text, data, security',
    tags           varchar(128)    DEFAULT NULL comment 'used to search like ["image", "autopilot", "medicine"]',
	algo_id        int            DEFAULT NULL comment 'point to a ml algo like a foreign key',
	`schema`       text           DEFAULT NULL comment 'schema of model input and output',
	transform      text           DEFAULT NULL comment 'transform config which is from dataset fields',
    rate           int            DEFAULT NULL comment '0 to 10',
    price          varchar(16)    DEFAULT NULL comment '$10/year',
	org_id         int            NOT NULL,
    `public`       boolean        NOT NULL DEFAULT false,
	run_id         varchar(32)    NOT NULL comment 'run_id of mlflow model_versions',
	version		   int			  NOT NULL comment 'version of mlflow model_versions',
	deploy_to      varchar(64)    DEFAULT NULL comment 'mlflow, ray, docker segmaker or databricks',
	endpoint       varchar(64)    DEFAULT NULL comment 'http://IP:PORT/invocations',
	status		   int            NOT NULL DEFAULT 0 comment '0:ready;1:serving;2:exception',
    created_by  varchar(64)    NOT NULL comment 'registered_by',
    created_at  timestamp      NOT NULL DEFAULT CURRENT_TIMESTAMP comment 'registered_at',
	updated_by     varchar(64)   DEFAULT NULL,
    updated_at     timestamp     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	deployed_by    varchar(64)    DEFAULT NULL,
    deployed_at    timestamp      DEFAULT NULL,
	CONSTRAINT fk_ai_model_org    foreign key(org_id)     REFERENCES sys_org(id)
) ENGINE = InnoDB;


# ----------------------------
# Table: ai_data
# ----------------------------
DROP TABLE IF EXISTS ai_data;
CREATE TABLE ai_data
(
    id             int            NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name           varchar(64)    NOT NULL,
    `desc`         varchar(128)   DEFAULT NULL comment 'description',
    `group`        varchar(64)    DEFAULT 'default',
	area           varchar(32)    DEFAULT NULL comment 'image, video, audio, text, data, security',
    model_id       int            NOT NULL,
	field_map	   text           DEFAULT NULL comment 'field map like {"schemaName": "fieldName"}',
	org_id         int            NOT NULL,
    `public`       boolean        NOT NULL DEFAULT false,
    created_by     varchar(64)    NOT NULL,
    created_at     timestamp      NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_by     varchar(64)    DEFAULT NULL,
    updated_at     timestamp      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_aidata_model   foreign key(model_id)  REFERENCES ai_model(id),
	CONSTRAINT fk_ai_data_org    foreign key(org_id)     REFERENCES sys_org(id)
) ENGINE = InnoDB;



# ----------------------------
# Table: ai_image
# ----------------------------
DROP TABLE IF EXISTS ai_image;
CREATE TABLE ai_image
(
    id             int            NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name           varchar(64)    NOT NULL,
    `desc`         varchar(128)   DEFAULT NULL comment 'description',
    `group`        varchar(64)    DEFAULT 'UnGrouped',
    area           varchar(64)    DEFAULT NULL comment 'like autopilot, medicine',
    type           varchar(64)    NOT NULL comment 'come from model like clacification, regression, clustering, reduction',
    model_id       int            NOT NULL,
    platform       varchar(64)    DEFAULT 'DJL' comment 'running platform',
    platform_ver   varchar(16)    DEFAULT NULL,
    content        text           DEFAULT NULL comment '[{file: "/aaa/bbb/abc.jpg", prediction: {}}, {file: "/abc/bcd/fg.png", prediction: {}}]',
	org_id         int            NOT NULL,
    `public`       boolean        NOT NULL DEFAULT false,
    created_by     varchar(64)    NOT NULL,
    created_at     timestamp      NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_by     varchar(64)    DEFAULT NULL,
    updated_at     timestamp      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_aiimage_model   foreign key(model_id)  REFERENCES ai_model(id)
) ENGINE = InnoDB;

INSERT INTO ai_image (id, name, `desc`, `group`, type, field, model_id, platform, platform_ver, content, org_id, `public`, created_by, created_at, updated_by, updated_at)
VALUES (1, 'object detecting', 'Object classification and detection', 'demo', 'Classification', 'autopilot', 1, 'DJL', '2.0', '[{file: "/aaa/bbb/abc.jpg", prediction: {}}]', 1, true, 'GavinZ', null, null, null),
(2, 'pneumonia detecting', 'Pneumonia classification', 'demo', 'Classification', 'medicine', 4, 'DJL', '4.5', '[{file: "/aaa/bbb/abc.jpg", prediction: {}}]', 1, true, 'GavinZ', null, null, null);


# ----------------------------
# Table: ai_audio
# ----------------------------
DROP TABLE IF EXISTS ai_audio;
CREATE TABLE ai_audio
(
    id             int            NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name           varchar(64)    NOT NULL,
    `desc`         varchar(128)   DEFAULT NULL comment 'description',
    `group`        varchar(64)    DEFAULT 'UnGrouped',
    area           varchar(64)    DEFAULT NULL comment 'like autopilot, medicine',
    type           varchar(64)    NOT NULL comment 'come from model like clacification, regression, clustering, reduction',
    model_id       int            NOT NULL,
    platform       varchar(64)    DEFAULT 'DJL' comment 'running platform',
    platform_ver   varchar(16)    DEFAULT NULL,
    content        text           DEFAULT NULL comment '[{file: "/aaa/bbb/abc.jpg", prediction: {}}, {file: "/abc/bcd/fg.png", prediction: {}}]',
	org_id         int            NOT NULL,
    `public`       boolean        NOT NULL DEFAULT false,
    created_by     varchar(64)    NOT NULL,
    created_at     timestamp      NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_by     varchar(64)    DEFAULT NULL,
    updated_at     timestamp      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_aiaudio_model   foreign key(model_id)  REFERENCES ai_model(id)
) ENGINE = InnoDB;

INSERT INTO ai_audio (id, name, `desc`, `group`, type, field, model_id, platform, platform_ver, content, org_id, `public`, created_by, created_at, updated_by, updated_at)
VALUES (1, 'object detecting', 'Object classification and detection', 'demo', 'Classification', 'autopilot', 1, 'DJL', '2.0', '[{file: "/aaa/bbb/abc.jpg", prediction: {}}]', 1, true, 'GavinZ', null, null, null),
(2, 'pneumonia detecting', 'Pneumonia classification', 'demo', 'Classification', 'medicine', 4, 'DJL', '4.5', '[{file: "/aaa/bbb/abc.jpg", prediction: {}}]', 1, true, 'GavinZ', null, null, null);


# ----------------------------
# Table: ai_video
# ----------------------------
DROP TABLE IF EXISTS ai_video;
CREATE TABLE ai_video
(
    id             int            NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name           varchar(64)    NOT NULL,
    `desc`         varchar(128)   DEFAULT NULL comment 'description',
    `group`        varchar(64)    DEFAULT 'UnGrouped',
    area           varchar(64)    DEFAULT NULL comment 'like autopilot, medicine',
    type           varchar(64)    NOT NULL comment 'come from model like clacification, regression, clustering, reduction',
    model_id       int            NOT NULL,
    platform       varchar(64)    DEFAULT 'DJL' comment 'running platform',
    platform_ver   varchar(16)    DEFAULT NULL,
    content        text           DEFAULT NULL comment '[{file: "/aaa/bbb/abc.jpg", prediction: {}}, {file: "/abc/bcd/fg.png", prediction: {}}]',
	org_id         int            NOT NULL,
    `public`       boolean        NOT NULL DEFAULT false,
    created_by     varchar(64)    NOT NULL,
    created_at     timestamp      NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_by     varchar(64)    DEFAULT NULL,
    updated_at     timestamp      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_aivideo_model   foreign key(model_id)  REFERENCES ai_model(id)
) ENGINE = InnoDB;

INSERT INTO ai_video (id, name, `desc`, `group`, type, field, model_id, platform, platform_ver, content, org_id, `public`, created_by, created_at, updated_by, updated_at)
VALUES (1, 'object detecting', 'Object classification and detection', 'demo', 'Classification', 'autopilot', 1, 'DJL', '2.0', '[{file: "/aaa/bbb/abc.jpg", prediction: {}}]', 1, true, 'GavinZ', null, null, null),
(2, 'pneumonia detecting', 'Pneumonia classification', 'demo', 'Classification', 'medicine', 4, 'DJL', '4.5', '[{file: "/aaa/bbb/abc.jpg", prediction: {}}]', 1, true, 'GavinZ', null, null, null);


# ----------------------------
# Table: ai_text
# ----------------------------
DROP TABLE IF EXISTS ai_text;
CREATE TABLE ai_text
(
    id             int            NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name           varchar(64)    NOT NULL,
    `desc`         varchar(128)   DEFAULT NULL comment 'description',
    `group`        varchar(64)    DEFAULT 'UnGrouped',
    area           varchar(64)    DEFAULT NULL comment 'like autopilot, medicine',
    type           varchar(64)    NOT NULL comment 'come from model like clacification, regression, clustering, reduction',
    model_id       int            NOT NULL,
    platform       varchar(64)    DEFAULT 'DJL' comment 'running platform',
    platform_ver   varchar(16)    DEFAULT NULL,
    content        text           DEFAULT NULL comment '[{file: "/aaa/bbb/abc.jpg", prediction: {}}, {file: "/abc/bcd/fg.png", prediction: {}}]',
	org_id         int            NOT NULL,
    `public`       boolean        NOT NULL DEFAULT false,
    created_by     varchar(64)    NOT NULL,
    created_at     timestamp      NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_by     varchar(64)    DEFAULT NULL,
    updated_at     timestamp      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_aitext_model   foreign key(model_id)  REFERENCES ai_model(id)
) ENGINE = InnoDB;

INSERT INTO ai_text (id, name, `desc`, `group`, type, field, model_id, platform, platform_ver, content, org_id, `public`, created_by, created_at, updated_by, updated_at)
VALUES (1, 'object detecting', 'Object classification and detection', 'demo', 'Classification', 'autopilot', 1, 'DJL', '2.0', '[{file: "/aaa/bbb/abc.jpg", prediction: {}}]', 1, true, 'GavinZ', null, null, null),
(2, 'pneumonia detecting', 'Pneumonia classification', 'demo', 'Classification', 'medicine', 4, 'DJL', '4.5', '[{file: "/aaa/bbb/abc.jpg", prediction: {}}]', 1, true, 'GavinZ', null, null, null);



# ----------------------------
# Table: ai_security
# ----------------------------
DROP TABLE IF EXISTS ai_security;
CREATE TABLE ai_security
(
    id             int            NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name           varchar(64)    NOT NULL,
    `desc`         varchar(128)   DEFAULT NULL comment 'description',
    `group`        varchar(64)    DEFAULT 'UnGrouped',
    area           varchar(64)    DEFAULT NULL comment 'like autopilot, medicine',
    type           varchar(64)    NOT NULL comment 'come from model like clacification, regression, clustering, reduction',
    model_id       int            NOT NULL,
    platform       varchar(64)    DEFAULT 'DJL' comment 'running platform',
    platform_ver   varchar(16)    DEFAULT NULL,
    content        text           DEFAULT NULL comment '[{file: "/aaa/bbb/abc.jpg", prediction: {}}, {file: "/abc/bcd/fg.png", prediction: {}}]',
	org_id         int            NOT NULL,
    `public`       boolean        NOT NULL DEFAULT false,
    created_by     varchar(64)    NOT NULL,
    created_at     timestamp      NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_by     varchar(64)    DEFAULT NULL,
    updated_at     timestamp      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_aisecurity_model   foreign key(model_id)  REFERENCES ai_model(id)
) ENGINE = InnoDB;

INSERT INTO ai_security (id, name, `desc`, `group`, type, field, model_id, platform, platform_ver, content, org_id, `public`, created_by, created_at, updated_by, updated_at)
VALUES (1, 'object detecting', 'Object classification and detection', 'demo', 'Classification', 'autopilot', 1, 'DJL', '2.0', '[{file: "/aaa/bbb/abc.jpg", prediction: {}}]', 1, true, 'GavinZ', null, null, null),
(2, 'pneumonia detecting', 'Pneumonia classification', 'demo', 'Classification', 'medicine', 4, 'DJL', '4.5', '[{file: "/aaa/bbb/abc.jpg", prediction: {}}]', 1, true, 'GavinZ', null, null, null);




# ----------------------------
# Table: ai_history
# ----------------------------
DROP TABLE IF EXISTS ai_history;
CREATE TABLE ai_history
(
    id             int           NOT NULL AUTO_INCREMENT PRIMARY KEY,
	app_id          int           NOT NULL,
	area		   varchar(32)   NOT NULL comment 'image, video, audio, text, data, security',
	dataset		   text          NOT NULL comment '{file:"S3://aa/bb.csv", map:{a1:"b1",a2:"b2"}}',
	result		   text          NOT NULL comment '[1,2,3,1]',
	org_id         int           NOT NULL,
    created_by     varchar(64)    NOT NULL,
    created_at     timestamp      NOT NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT fk_ai_history_org       foreign key(org_id)     REFERENCES sys_org(id)
) ENGINE = InnoDB;




# ----------------------------
# not use
# Table: sys_menu_i18n
# support multiple languages
# ----------------------------
DROP TABLE IF EXISTS sys_menu_i18n;
CREATE TABLE sys_menu_title
(
    id            int          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    menu_id       int          DEFAULT NULL,
    zh-cn        varchar(64)   NOT NULL, 
    CONSTRAINT fk_title_menu    foreign key(menu_id)     REFERENCES sys_menu(id)
) ENGINE = InnoDB;







# ----------------------------
# Table: status dim
# user can be managered/filtered by organization
# ----------------------------
DROP TABLE IF EXISTS sys_status_dim;
CREATE TABLE sys_status_dim
(
    id            int           NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `group`      varchar(16)   NOT NULL,
    status        varchar(16)   NOT NULL
) ENGINE = InnoDB;

INSERT INTO sys_status_dim (id, `group`, status) VALUES (1, 'user', 'frozen');
INSERT INTO sys_status_dim (id, `group`, status) VALUES (2, 'user', 'active');
INSERT INTO sys_status_dim (id, `group`, status) VALUES (3, 'user', 'expired');
INSERT INTO sys_status_dim (id, `group`, status) VALUES (4, 'user', 'deleted');



# ----------------------------
# Table: sys_task
# user 
# ----------------------------
DROP TABLE IF EXISTS sys_task;
CREATE TABLE sys_task
(
    id            int           NOT NULL AUTO_INCREMENT PRIMARY KEY,
	name           varchar(64)    NOT NULL,
    `group`      varchar(16),
	job_class	 varchar(255) NOT NULL,
	exec_strategy	 varchar(16) NOT NULL,
	expression	 varchar(64) NOT NULL,
	remark	 varchar(16),
	start_date	 timestamp      default NULL,
	end_date	 timestamp      default NULL,
    created_by     varchar(64)    NOT NULL,
    created_at     timestamp      DEFAULT CURRENT_TIMESTAMP,
	updated_by     varchar(64)    DEFAULT NULL,
    updated_at     timestamp      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB;

INSERT INTO sys_task (id, name, `group`, job_class, exec_strategy, expression, remark, start_date, end_date, created_by, created_at, updated_by, updated_at)
VALUES (1, 'daily report', 'abc', '{job: 15}', 'once', '5 4 * * *', 'test', '2024-02-02 18:50:00', '2024-02-02 18:55:00', 'Admin', null, 'Admin', null);

# ----------------------------
# Table: sys_task_group
# user 
# ----------------------------
DROP TABLE IF EXISTS sys_task_group;
CREATE TABLE sys_task_group
(
    id            int           NOT NULL AUTO_INCREMENT PRIMARY KEY,
	name           varchar(64)    NOT NULL,
    `group`      varchar(16),
	job_class	 varchar(255) NOT NULL,
	exec_strategy	 varchar(16) NOT NULL,
	expression	 varchar(64) NOT NULL,
	remark	 varchar(16),
	start_date	 timestamp      default NULL,
	end_date	 timestamp      default NULL,
    created_by     varchar(64)    NOT NULL,
    created_at     timestamp      DEFAULT CURRENT_TIMESTAMP,
	updated_by     varchar(64)    DEFAULT NULL,
    updated_at     timestamp      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB;

INSERT INTO sys_task (id, name, `group`, job_class, exec_strategy, expression, remark, start_date, end_date, created_by, created_at, updated_by, updated_at)
VALUES (1, 'daily report', 'abc', '{job: 15}', 'once', '5 4 * * *', 'test', '2024-02-02 18:50:00', '2024-02-02 18:55:00', 'Admin', null, 'Admin', null);
