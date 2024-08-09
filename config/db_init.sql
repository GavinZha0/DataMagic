CREATE DATABASE IF NOT EXISTS datapie6 DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;


# ----------------------------
# Table: system site
# these can be changed based on different customers
# ----------------------------
DROP TABLE IF EXISTS sys_site;
CREATE TABLE sys_site
(
    id          int            NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name        varchar(64)    NOT NULL comment 'Site name',
    owner       varchar(64)    NOT NULL comment 'Company name',
    partner     varchar(64)	   DEFAULT NULL,
    about       varchar(255)   DEFAULT NULL,
    logo        varchar(255)   DEFAULT '/default/profile/logo.png' comment 'logo file',
    created_by  varchar(64)    NOT NULL,
    created_at  timestamp      NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_by  varchar(64)    DEFAULT NULL,
    updated_at  timestamp      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB;

INSERT INTO sys_site (id, name, owner, partner, about, logo, created_by, created_at, updated_by, updated_at)
VALUES (1, 'DataPie', 'NineStar Tech', null, 'A big data platform for AI and BI!', '/ftp/sys/logo.png', 'Superman', null, 'Superman', null);


# ----------------------------
# Table: sys parameter
# system/user defined parameters
# ----------------------------

DROP TABLE IF EXISTS sys_param;
CREATE TABLE sys_param
(
    id          int            NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name        varchar(64)    NOT NULL,
    `desc`      varchar(128)   DEFAULT NULL comment 'description',
    `group`     varchar(64)    DEFAULT NULL comment 'parameter group',
    module      varchar(64)    NOT NULL comment 'module or feature',
    type        varchar(64)    NOT NULL comment 'integer, float, boolean, string, [string], json',
    value       varchar(255)   NOT NULL comment 'current value',
    previous    varchar(255)   DEFAULT NULL comment 'previous value',
	org_id      int            DEFAULT 1 comment 'free center by default',
    created_by  varchar(64)    NOT NULL,
    created_at  timestamp      NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_by  varchar(64)    DEFAULT NULL,
    updated_at  timestamp      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	CONSTRAINT fk_param_org    foreign key(org_id)     REFERENCES sys_org(id)
) ENGINE = InnoDB;

INSERT INTO sys_param (id, name, `desc`, `group`, module, type, value, previous, org_id, created_by, created_at, updated_by, updated_at)
VALUES (1, 'source_type', null, 'datasource', 'dataviz', '[string]', "['CSV', 'JSON', 'MySQL', 'MariaDB', 'Vertica']", '', 2, 'Superman', null, 'Superman', null),
(2, 'chart_lib', null, 'dataview', 'dataviz', '[json]', "[{name:'G2Plot',ver:['2.4']},{name:'S2',ver:['1.51']},{name:'ECharts',ver:['1.0']},{name:'AmCharts',ver:['5.0','4.1']},{name:'ApexCharts',ver:['2.0']},{name:'VegaLite', ver:['5.6']}]", null, 2, 'Superman', null, 'Superman', null);



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
VALUES (1, null, 'Free Center', 'Discover the production for free', null, true, null, false, 'Superman', null, 'Superman', null);

INSERT INTO sys_org (id, pid, name, `desc`, logo, active, exp_date, deleted, created_by, created_at, updated_by, updated_at)
VALUES (2, null, 'NineStar', 'A future AI company', null, true, null, false, 'Superman', null, 'Superman', null);

INSERT INTO sys_org (id, pid, name, `desc`, logo, active, exp_date, deleted, created_by, created_at, updated_by, updated_at)
VALUES (3, null, 'VIP Club', 'paying users', null, true, null, false, 'Superman', null, 'Superman', null);

INSERT INTO sys_org (id, pid, name, `desc`, logo, active, exp_date, deleted, created_by, created_at, updated_by, updated_at)
VALUES (4, null, 'Demo Center', 'demo users of company A', null, true, null, false, 'Superman', null, 'Superman', null);

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
VALUES (1, 'Superman', '$2a$10$7lSDEQ8pyopeE6MMInDQweXboiU8ZF/6CSN0x2.SCVeSz9z4CU57O', 'Gavin.Zhao', '', 'jichun.zhao@outlook.com', '18611815495', 2, null, null, true, false, null, false, 'Superman', null, 'Superman', null);

INSERT INTO sys_user (id, name, password, realname, `desc`, email, phone, org_id, avatar, social, active, sms_code, exp_date, deleted, created_by, created_at, updated_by, updated_at)
VALUES (2, 'Admin', '$2a$10$7lSDEQ8pyopeE6MMInDQweXboiU8ZF/6CSN0x2.SCVeSz9z4CU57O', 'Mr.Zhao', '', 'jichun.zhao@gmail.com', '7328902296', 2, 'cat.jpg', null, true, false, null, false, 'Superman', null, 'Superman', null);

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
    org_id      int             DEFAULT NULL comment 'global role when org is null',
    created_by  varchar(64)     NOT NULL,
    created_at  timestamp       NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_by  varchar(64)     DEFAULT NULL,
    updated_at  timestamp       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_role_org      foreign key(org_id)     REFERENCES sys_org(id)
) ENGINE = InnoDB;


INSERT INTO sys_role (id, name, `desc`, active, org_id, created_by, created_at, updated_by, updated_at)
VALUES (1, 'Guest', 'view only', true, null, 'Superman', null, 'Superman', null);

INSERT INTO sys_role (id, name, `desc`, active, org_id, created_by, created_at, updated_by, updated_at)
VALUES (2, 'Superuser', 'has all permissions', true, null, 'Superman', null, 'Superman', null);

INSERT INTO sys_role (id, name, `desc`, active, org_id, created_by, created_at, updated_by, updated_at)
VALUES (3, 'Administrator', 'has all permissions', true, null, 'Superman', null, 'Superman', null);

INSERT INTO sys_role (id, name, `desc`, active, org_id, created_by, created_at, updated_by, updated_at)
VALUES (4, 'Admin', 'control panel only', true, null, 'Superman', null, 'Superman', null);

INSERT INTO sys_role (id, name, `desc`, active, org_id, created_by, created_at, updated_by, updated_at)
VALUES (5, 'Tester', 'has all permissions', true, null, 'Superman', null, 'Superman', null);

INSERT INTO sys_role (id, name, `desc`, active, org_id, created_by, created_at, updated_by, updated_at)
VALUES (6, 'Viewer', 'view only', true, null, 'Superman', null, 'Superman', null);

INSERT INTO sys_role (id, name, `desc`, active, org_id, created_by, created_at, updated_by, updated_at)
VALUES (7, 'Publisher', 'has publish permissions', true, 2, 'Superman', null, 'Superman', null);



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
VALUES (1, 1, 2);
INSERT INTO sys_user_role (id, user_id, role_id)
VALUES (2, 2, 3);


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
    
    active       boolean       NOT NULL DEFAULT true,
    deleted      boolean       NOT NULL DEFAULT false comment 'true: it was deleted',
    created_by  varchar(64)    NOT NULL,
    created_at  timestamp      NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_by  varchar(64)    DEFAULT NULL,
    updated_at  timestamp      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB;

INSERT INTO sys_menu (id, pid, name, title, icon, pos, subreport, component, path, redirect, active, deleted, created_by, created_at, updated_by, updated_at)
VALUES (1, null, 'Home', '主页', 'ant-design:home-outlined', 0, false, '/home/index', '/home', null, true, false, 'Superman', null, 'Superman', null);

INSERT INTO sys_menu (id, pid, name, title, icon, pos, subreport, component, path, redirect, active, deleted, created_by, created_at, updated_by, updated_at)
VALUES (2, null, 'Dashboard', '仪表板', 'ant-design:dashboard-outlined', 1, false, 'BlankLayout', '/dashboard', null, true, false, 'Superman', null, 'Superman', null);


INSERT INTO sys_menu (id, pid, name, title, icon, pos, subreport, component, path, redirect, active, deleted, created_by, created_at, updated_by, updated_at)
VALUES (3, null, 'Visualization', '数据可视化', 'ant-design:appstore-outlined', 2, false, 'BlankLayout', '/dataviz', null, true, false, 'Superman', null, 'Superman', null);

INSERT INTO sys_menu (id, pid, name, title, icon, pos, subreport, component, path, redirect, active, deleted, created_by, created_at, updated_by, updated_at)
VALUES (4, 3, 'Report', '报表', 'ant-design:appstore-add-outlined', 0, false, '/dataviz/report/index', '/dataviz/report', null, true, false, 'Superman', null, 'Superman', null);

INSERT INTO sys_menu (id, pid, name, title, icon, pos, subreport, component, path, redirect, active, deleted, created_by, created_at, updated_by, updated_at)
VALUES (5, 3, 'View', '视图', 'ant-design:line-chart-outlined', 1, false, '/dataviz/dataview/index', '/dataviz/dataview', null, true, false, 'Superman', null, 'Superman', null);

INSERT INTO sys_menu (id, pid, name, title, icon, pos, subreport, component, path, redirect, active, deleted, created_by, created_at, updated_by, updated_at)
VALUES (6, 3, 'Dataset', '数据集', 'ant-design:database-outlined', 2, false, '/dataviz/dataset/index', '/dataviz/dataset', null, true, false, 'Superman', null, 'Superman', null);



INSERT INTO sys_menu (id, pid, name, title, icon, pos, subreport, component, path, redirect, active, deleted, created_by, created_at, updated_by, updated_at)
VALUES (7, null, 'Source Mgr', '数据管理', 'ant-design:money-collect-outlined', 3, false, 'BlankLayout', '/datamgr', null, true, false, 'Superman', null, 'Superman', null);

INSERT INTO sys_menu (id, pid, name, title, icon, pos, subreport, component, path, redirect, active, deleted, created_by, created_at, updated_by, updated_at)
VALUES (8, 7, 'Source', '数据源', 'ant-design:database-outlined', 3, false, '/datamgr/datasource/index', '/datamgr/datasource', null, true, false, 'Superman', null, 'Superman', null);

INSERT INTO sys_menu (id, pid, name, title, icon, pos, subreport, component, path, redirect, active, deleted, created_by, created_at, updated_by, updated_at)
VALUES (9, 7, 'Import', '导入数据', 'control', 0, false, '/datamgr/import/index', '/datamgr/import', null, true, false, 'Superman', null, 'Superman', null);

INSERT INTO sys_menu (id, pid, name, title, icon, pos, subreport, component, path, redirect, active, deleted, created_by, created_at, updated_by, updated_at)
VALUES (10, 7, 'Anchor', '采集点', 'control', 1, false, '/datamgr/anchor/index', '/datamgr/anchor', null, true, false, 'Superman', null, 'Superman', null);

INSERT INTO sys_menu (id, pid, name, title, icon, pos, subreport, component, path, redirect, active, deleted, created_by, created_at, updated_by, updated_at)
VALUES (11, 7, 'Kafka', 'Kafka', 'control', 2, false, '/datamgr/kafka/index', '/datamgr/kafka', null, true, false, 'Superman', null, 'Superman', null);

INSERT INTO sys_menu (id, pid, name, title, icon, pos, subreport, component, path, redirect, active, deleted, created_by, created_at, updated_by, updated_at)
VALUES (12, 7, 'ETL', 'ETL', 'control', 3, false, '/datamgr/etl/index', '/datamgr/etl', null, true, false, 'Superman', null, 'Superman', null);

INSERT INTO sys_menu (id, pid, name, title, icon, pos, subreport, component, path, redirect, active, deleted, created_by, created_at, updated_by, updated_at)
VALUES (13, 7, 'Web Magic', 'Web Magic', 'control', 4, false, '/datamgr/webmagic/index', '/datamgr/webmagic', null, true, false, 'Superman', null, 'Superman', null);


INSERT INTO sys_menu (id, pid, name, title, icon, pos, subreport, component, path, redirect, active, deleted, created_by, created_at, updated_by, updated_at)
VALUES (14, null, 'ML Dev', '机器学习', 'ant-design:car-outlined', 4, false, 'BlankLayout', '/ml', null, true, false, 'Superman', null, 'Superman', null);

INSERT INTO sys_menu (id, pid, name, title, icon, pos, subreport, component, path, redirect, active, deleted, created_by, created_at, updated_by, updated_at)
VALUES (15, 14, 'Dataset', '数据集', 'ant-design:database-outlined', 2, false, '/ml/dataset/index', '/ml/dataset', null, true, false, 'Superman', null, 'Superman', null);

INSERT INTO sys_menu (id, pid, name, title, icon, pos, subreport, component, path, redirect, active, deleted, created_by, created_at, updated_by, updated_at)
VALUES (16, 14, 'EDA', '数据探索', 'control', 3, false, '/ml/eda/index', '/ml/eda', null, true, false, 'Superman', null, 'Superman', null);

INSERT INTO sys_menu (id, pid, name, title, icon, pos, subreport, component, path, redirect, active, deleted, created_by, created_at, updated_by, updated_at)
VALUES (17, 14, 'Algorithm', '算法', 'control', 0, false, '/ml/algorithm/index', '/ml/algorithm', null, true, false, 'Superman', null, 'Superman', null);

INSERT INTO sys_menu (id, pid, name, title, icon, pos, subreport, component, path, redirect, active, deleted, created_by, created_at, updated_by, updated_at)
VALUES (18, 14, 'Model', '模型', 'control', 1, false, '/ml/model/index', '/ml/model', null, true, false, 'Superman', null, 'Superman', null);

INSERT INTO sys_menu (id, pid, name, title, icon, pos, subreport, component, path, redirect, active, deleted, created_by, created_at, updated_by, updated_at)
VALUES (19, 14, 'Workflow', '工作流', 'control', 3, false, '/ml/workflow/index', '/ml/workflow', null, true, false, 'Superman', null, 'Superman', null);

INSERT INTO sys_menu (id, pid, name, title, icon, pos, subreport, component, path, redirect, active, deleted, created_by, created_at, updated_by, updated_at)
VALUES (20, 14, 'Vis', '可视化', 'control', 3, false, '/ml/vis/index', '/ml/vis', null, true, false, 'Superman', null, 'Superman', null);



INSERT INTO sys_menu (id, pid, name, title, icon, pos, subreport, component, path, redirect, active, deleted, created_by, created_at, updated_by, updated_at)
VALUES (21, null, 'AI App', '人工智能', 'ant-design:coffee-outlined', 5, false, 'BlankLayout', '/ai', null, true, false, 'Superman', null, 'Superman', null);

INSERT INTO sys_menu (id, pid, name, title, icon, pos, subreport, component, path, redirect, active, deleted, created_by, created_at, updated_by, updated_at)
VALUES (22, 21, 'Market', '模型市场', 'control', 0, false, '/ai/market/index', '/ai/market', null, true, false, 'Superman', null, 'Superman', null);

INSERT INTO sys_menu (id, pid, name, title, icon, pos, subreport, component, path, redirect, active, deleted, created_by, created_at, updated_by, updated_at)
VALUES (23, 21, 'Image', '图像处理', 'control', 1, false, '/ai/image/index', '/ai/image', null, true, false, 'Superman', null, 'Superman', null);

INSERT INTO sys_menu (id, pid, name, title, icon, pos, subreport, component, path, redirect, active, deleted, created_by, created_at, updated_by, updated_at)
VALUES (24, 21, 'Video', '视频分析', 'control', 2, false, '/ai/video/index', '/ai/video', null, true, false, 'Superman', null, 'Superman', null);

INSERT INTO sys_menu (id, pid, name, title, icon, pos, subreport, component, path, redirect, active, deleted, created_by, created_at, updated_by, updated_at)
VALUES (25, 21, 'Audio', '语音处理', 'control', 3, false, '/ai/audio/index', '/ai/audio', null, true, false, 'Superman', null, 'Superman', null);

INSERT INTO sys_menu (id, pid, name, title, icon, pos, subreport, component, path, redirect, active, deleted, created_by, created_at, updated_by, updated_at)
VALUES (26, 21, 'Text', '文本分心', 'control', 4, false, '/ai/text/index', '/ai/text', null, true, false, 'Superman', null, 'Superman', null);

INSERT INTO sys_menu (id, pid, name, title, icon, pos, subreport, component, path, redirect, active, deleted, created_by, created_at, updated_by, updated_at)
VALUES (27, 21, 'DM', '数据挖掘', 'control', 5, false, '/ai/dm/index', '/ai/dm', null, true, false, 'Superman', null, 'Superman', null);


INSERT INTO sys_menu (id, pid, name, title, icon, pos, subreport, component, path, redirect, active, deleted, created_by, created_at, updated_by, updated_at)
VALUES (28, null, 'Admin', '控制面板', 'ant-design:setting-outlined', 6, false, 'BlankLayout', '/admin', null, true, false, 'Superman', null, 'Superman', null);

INSERT INTO sys_menu (id, pid, name, title, icon, pos, subreport, component, path, redirect, active, deleted, created_by, created_at, updated_by, updated_at)
VALUES (29, 28, 'User', '用户管理', 'control', 0, false, '/admin/user/index', '/admin/user', null, true, false, 'Superman', null, 'Superman', null);

INSERT INTO sys_menu (id, pid, name, title, icon, pos, subreport, component, path, redirect, active, deleted, created_by, created_at, updated_by, updated_at)
VALUES (30, 28, 'Role', '角色管理', 'control', 1, false, '/admin/role/index', '/admin/role', null, true, false, 'Superman', null, 'Superman', null);

INSERT INTO sys_menu (id, pid, name, title, icon, pos, subreport, component, path, redirect, active, deleted, created_by, created_at, updated_by, updated_at)
VALUES (31, 28, 'Menu', '菜单管理', 'control', 2, false, '/admin/menu/index', '/admin/menu', null, true, false, 'Superman', null, 'Superman', null);

INSERT INTO sys_menu (id, pid, name, title, icon, pos, subreport, component, path, redirect, active, deleted, created_by, created_at, updated_by, updated_at)
VALUES (32, 28, 'Parameter', '参数管理', 'control', 3, false, '/admin/param/index', '/admin/config', null, true, false, 'Superman', null, 'Superman', null);

INSERT INTO sys_menu (id, pid, name, title, icon, pos, subreport, component, path, redirect, active, deleted, created_by, created_at, updated_by, updated_at)
VALUES (33, 28, 'Organization', '组织管理', 'control', 0, false, '/admin/org/index', '/admin/org', null, true, false, 'Superman', null, 'Superman', null);

INSERT INTO sys_menu (id, pid, name, title, icon, pos, subreport, component, path, redirect, active, deleted, created_by, created_at, updated_by, updated_at)
VALUES (34, 28, 'Scheduler', '调度计划', 'control', 4, false, '/admin/scheduler/index', '/admin/scheduler', null, true, false, 'Superman', null, 'Superman', null);

INSERT INTO sys_menu (id, pid, name, title, icon, pos, subreport, component, path, redirect, active, deleted, created_by, created_at, updated_by, updated_at)
VALUES (35, 28, 'My Center', '个人中心', 'control', 4, false, '/admin/mycenter/index', '/admin/mycenter', null, true, false, 'Superman', null, 'Superman', null);


INSERT INTO sys_menu (id, pid, name, title, icon, pos, subreport, component, path, redirect, active, deleted, created_by, created_at, updated_by, updated_at)
VALUES (36, null, 'Monitor', '系统监控', 'ant-design:fund-projection-screen-outlined', 7, false, 'BlankLayout', '/monitor', null, true, false, 'Superman', null, 'Superman', null);

INSERT INTO sys_menu (id, pid, name, title, icon, pos, subreport, component, path, redirect, active, deleted, created_by, created_at, updated_by, updated_at)
VALUES (37, 36, 'Druid', 'Druid', 'control', 0, false, '/monitor/druid/index', '/monitor/druid', null, true, false, 'Superman', null, 'Superman', null);

INSERT INTO sys_menu (id, pid, name, title, icon, pos, subreport, component, path, redirect, active, deleted, created_by, created_at, updated_by, updated_at)
VALUES (38, 36, 'Knife4j', 'Knife4j', 'control', 1, false, '/monitor/knife4j/index', '/monitor/knife4j', null, true, false, 'Superman', null, 'Superman', null);

INSERT INTO sys_menu (id, pid, name, title, icon, pos, subreport, component, path, redirect, active, deleted, created_by, created_at, updated_by, updated_at)
VALUES (39, 36, 'Gateway', '网关代理', 'control', 2, false, '/monitor/gateway/index', '/monitor/gateway', null, true, false, 'Superman', null, 'Superman', null);

INSERT INTO sys_menu (id, pid, name, title, icon, pos, subreport, component, path, redirect, active, deleted, created_by, created_at, updated_by, updated_at)
VALUES (40, 36, 'Network', '网络', 'control', 3, false, '/monitor/network/index', '/monitor/network', null, true, false, 'Superman', null, 'Superman', null);


INSERT INTO sys_menu (id, pid, name, title, icon, pos, subreport, component, path, redirect, active, deleted, created_by, created_at, updated_by, updated_at)
VALUES (41, null, 'System', '系统管理', 'ant-design:apple-outlined', 8, false, 'BlankLayout', '/system', null, true, false, 'Superman', null, 'Superman', null);

INSERT INTO sys_menu (id, pid, name, title, icon, pos, subreport, component, path, redirect, active, deleted, created_by, created_at, updated_by, updated_at)
VALUES (42, 41, 'Log', '日志管理', 'control', 0, false, 'BlankLayout', '/system/log', null, true, false, 'Superman', null, 'Superman', null);

INSERT INTO sys_menu (id, pid, name, title, icon, pos, subreport, component, path, redirect, active, deleted, created_by, created_at, updated_by, updated_at)
VALUES (43, 41, 'Access', '登录日志', 'control', 0, false, '/system/log/access/index', '/system/log/access', null, true, false, 'Superman', null, 'Superman', null);

INSERT INTO sys_menu (id, pid, name, title, icon, pos, subreport, component, path, redirect, active, deleted, created_by, created_at, updated_by, updated_at)
VALUES (44, 41, 'Action', '操作日志', 'control', 1, false, '/system/log/action/index', '/system/log/action', null, true, false, 'Superman', null, 'Superman', null);



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
VALUES (1, 1, 1, 1, true, false, false, false, false, false);

INSERT INTO sys_role_menu_permit (id, role_id, menu_id, permit, view, edit, publish, subscribe, import, export)
VALUES (2, 2, 1, 64, true, true, true, true, true, true);
INSERT INTO sys_role_menu_permit (id, role_id, menu_id, permit, view, edit, publish, subscribe, import, export)
VALUES (3, 2, 2, 64, true, true, true, true, true, true);
INSERT INTO sys_role_menu_permit (id, role_id, menu_id, permit, view, edit, publish, subscribe, import, export)
VALUES (4, 2, 3, 64, true, true, true, true, true, true);
INSERT INTO sys_role_menu_permit (id, role_id, menu_id, permit, view, edit, publish, subscribe, import, export)
VALUES (5, 2, 7, 64, true, true, true, true, true, true);
INSERT INTO sys_role_menu_permit (id, role_id, menu_id, permit, view, edit, publish, subscribe, import, export)
VALUES (6, 2, 14, 64, true, true, true, true, true, true);
INSERT INTO sys_role_menu_permit (id, role_id, menu_id, permit, view, edit, publish, subscribe, import, export)
VALUES (7, 2, 19, 64, true, true, true, true, true, true);
INSERT INTO sys_role_menu_permit (id, role_id, menu_id, permit, view, edit, publish, subscribe, import, export)
VALUES (8, 2, 26, 64, true, true, true, true, true, true);
INSERT INTO sys_role_menu_permit (id, role_id, menu_id, permit, view, edit, publish, subscribe, import, export)
VALUES (9, 2, 34, 64, true, true, true, true, true, true);
INSERT INTO sys_role_menu_permit (id, role_id, menu_id, permit, view, edit, publish, subscribe, import, export)
VALUES (10, 2, 39, 64, true, true, true, true, true, true);

INSERT INTO sys_role_menu_permit (id, role_id, menu_id, permit, view, edit, publish, subscribe, import, export)
VALUES (11, 3, 1, 64, true, true, true, true, true, true);
INSERT INTO sys_role_menu_permit (id, role_id, menu_id, permit, view, edit, publish, subscribe, import, export)
VALUES (12, 3, 2, 64, true, true, true, true, true, true);
INSERT INTO sys_role_menu_permit (id, role_id, menu_id, permit, view, edit, publish, subscribe, import, export)
VALUES (13, 3, 3, 64, true, true, true, true, true, true);
INSERT INTO sys_role_menu_permit (id, role_id, menu_id, permit, view, edit, publish, subscribe, import, export)
VALUES (14, 3, 7, 64, true, true, true, true, true, true);
INSERT INTO sys_role_menu_permit (id, role_id, menu_id, permit, view, edit, publish, subscribe, import, export)
VALUES (15, 3, 14, 64, true, true, true, true, true, true);
INSERT INTO sys_role_menu_permit (id, role_id, menu_id, permit, view, edit, publish, subscribe, import, export)
VALUES (16, 3, 19, 64, true, true, true, true, true, true);
INSERT INTO sys_role_menu_permit (id, role_id, menu_id, permit, view, edit, publish, subscribe, import, export)
VALUES (17, 3, 26, 64, true, true, true, true, true, true);
INSERT INTO sys_role_menu_permit (id, role_id, menu_id, permit, view, edit, publish, subscribe, import, export)
VALUES (18, 3, 34, 64, true, true, true, true, true, true);
INSERT INTO sys_role_menu_permit (id, role_id, menu_id, permit, view, edit, publish, subscribe, import, export)
VALUES (19, 3, 39, 64, true, true, true, true, true, true);

INSERT INTO sys_role_menu_permit (id, role_id, menu_id, permit, view, edit, publish, subscribe, import, export)
VALUES (20, 4, 1, 8, true, true, true, false, false, false);
INSERT INTO sys_role_menu_permit (id, role_id, menu_id, permit, view, edit, publish, subscribe, import, export)
VALUES (21, 4, 2, 8, true, true, true, false, false, false);
INSERT INTO sys_role_menu_permit (id, role_id, menu_id, permit, view, edit, publish, subscribe, import, export)
VALUES (22, 4, 3, 8, true, true, true, false, false, false);
INSERT INTO sys_role_menu_permit (id, role_id, menu_id, permit, view, edit, publish, subscribe, import, export)
VALUES (23, 4, 7, 8, true, true, true, false, false, false);
INSERT INTO sys_role_menu_permit (id, role_id, menu_id, permit, view, edit, publish, subscribe, import, export)
VALUES (24, 4, 14, 8, true, true, true, false, false, false);
INSERT INTO sys_role_menu_permit (id, role_id, menu_id, permit, view, edit, publish, subscribe, import, export)
VALUES (25, 4, 19, 8, true, true, true, false, false, false);
INSERT INTO sys_role_menu_permit (id, role_id, menu_id, permit, view, edit, publish, subscribe, import, export)
VALUES (26, 4, 26, 8, true, true, true, false, false, false);
INSERT INTO sys_role_menu_permit (id, role_id, menu_id, permit, view, edit, publish, subscribe, import, export)
VALUES (27, 4, 34, 8, true, true, true, false, false, false);
INSERT INTO sys_role_menu_permit (id, role_id, menu_id, permit, view, edit, publish, subscribe, import, export)
VALUES (28, 4, 39, 8, true, true, true, false, false, false);



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
    type           varchar(16)  NOT NULL comment 'csv, json, excel, mysql, vertica...',
    url            varchar(255) NOT NULL comment 'host:port/db',
    params         varchar(255) DEFAULT NULL comment "array like ['useUnicode=true']",
    username       varchar(64)  NOT NULL comment 'db username',
    password       varchar(255) NOT NULL comment 'db password',
    version        varchar(64)  DEFAULT NULL comment 'db version',
    org_id         int          NOT NULL,
    `public`       boolean      NOT NULL DEFAULT false,
    locked_table   text         DEFAULT NULL comment "array like ['user', 'salary']",
    created_by  varchar(64)     NOT NULL,
    created_at  timestamp       NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_by  varchar(64)     DEFAULT NULL,
    updated_at  timestamp       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_source_org    foreign key(org_id)     REFERENCES sys_org(id)
) ENGINE = InnoDB;


INSERT INTO data_source (id, name, `group`, `desc`, type, url, params, username, password, version, `public`, org_id, locked_table, created_by, created_at, updated_by, updated_at)
VALUES (1, 'AWS Maria', 'AWS', 'test of aws MariaDB', 'MySQL', 'datapie.c34q1kuwepfw.us-east-1.rds.amazonaws.com:3306/foodmart2', '[{"name":"useUnicode","value":"true"},{"name":"characterEncoding","value":"UTF-8"},{"name":"serverTimezon","value":"UTC"}]', 'admin', 'cm9vdDEyMw==', '10.6.3', true, 2, null, 'Admin', null, 'Admin', null);


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

INSERT INTO data_import (id, files, `type`, attrs, fields, config, source_id, table_name, overwrite, `rows`, records, ftp_path, status, detail, `public`, org_id, created_by, created_at, updated_by, updated_at)
VALUES (1, '["abc.csv"]', 'CSV', '{"header":true}', '[{"name":"cat","type":"string"}]', '{"ts":"UTC"}', 1, 'abc', false, null, null, '20230222173548', 'success', null, false, 1, 'Superman', null, 'Superman', null);





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
    query          text         DEFAULT NULL comment 'sql query',
	final_query    text         DEFAULT NULL comment 'final query',
	error          varchar(255) DEFAULT NULL,
    field          text         DEFAULT NULL comment 'json array like [{name:"Name", type:"string", alias:"Username", metrics:true, hidden: true, order: -2}]',
    graph          text         DEFAULT NULL comment 'ER graph, json object',
    graph_ver      varchar(8)   DEFAULT NULL,
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


INSERT INTO viz_dataset (id, name, `group`, `desc`, variable, query, field, graph, graph_ver, source_id, org_id, `public`, created_by, created_at, updated_by, updated_at)
VALUES (1, 'employee salary', 'first', 'test of local MariaDB', '[{name:"aaa", type:"number", value:"35"},{name:"bbb", type:"string", value:"hello"},{name:"ccc", type:"timestamp", value:"2021-10-10"}]', 'select * from employee limit 20', '[{name:"FULL_NAME", title:"name"}, {name:"SALARY", metrics:true, orderPri:0, orderDir:"Asc"}, {name:"DEPARTMENT_ID", hidden:true},{name:"EDUCATION_LEVEL", title:"Education", dim:true},{name:"MARITAL_STATUS", filter:"=\'M\'"},{name:"GENDER"}, {name:"POSITION_TITLE"}, {name:"EMPLOYEE_ID"}]', null, null, 1, 1, true, 'Admin', null, null, null);

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


INSERT INTO viz_view (id, name, `desc`, `group`, type, dim, metrics, agg, filter, sorter, variable, calculation, model, lib_name, lib_ver, lib_cfg, dataset_id, org_id, `public`, created_by, created_at, updated_by, updated_at)
VALUES (1, 'employee salary', 'test of employee', 'first', 'line_chart', 'aa, bb, cc', 'dd, ee', 'count', null, null, '[{name:"aaa", type:"number", value:"35"},{name:"bbb", type:"string", value:"hello"},{name:"ccc", type:"timestamp", value:"2021-10-10"}]', null, '[{name:"FULL_NAME", title:"name"}, {name:"SALARY", metrics:true, orderPri:0, orderDir:"Asc"}, {name:"DEPARTMENT_ID", hidden:true},{name:"EDUCATION_LEVEL", title:"Education", dim:true},{name:"MARITAL_STATUS", filter:"=\'M\'"},{name:"GENDER"}, {name:"POSITION_TITLE"}, {name:"EMPLOYEE_ID"}]', 'antvG2Plot', '2.2', '[{name:"FULL_NAME", title:"name"}, {name:"SALARY", metrics:true, orderPri:0, orderDir:"Asc"}, {name:"DEPARTMENT_ID", hidden:true},{name:"EDUCATION_LEVEL", title:"Education", dim:true},{name:"MARITAL_STATUS", filter:"=\'M\'"},{name:"GENDER"}, {name:"POSITION_TITLE"}, {name:"EMPLOYEE_ID"}]', 1, 1, true, 'Admin', null, 'Admin', null);




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


INSERT INTO viz_report (id, name, `desc`, `group`, type, pages, org_id, `public`, pub_pub, menu_id, created_by, created_at, updated_by, updated_at)
VALUES (1, 'Salary Distribution', 'employee salary', 'first', 'single', '[{id: 1, name:"aaa", layout:"2x2", dataviews:[1,2,3,4], filter:{label: "FULL_NAME", value: "Gavin"}}]', 1, true, true, null, 'Superman', null, null, null);



# ----------------------------
# Table: sys_notice
# someone send notification to a org
# ----------------------------
DROP TABLE IF EXISTS sys_notice;
CREATE TABLE sys_notice
(
    id             int          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    ts             timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    type           varchar(16)  NOT NULL DEFAULT 'notice' comment 'notice, inquiry, ad, warning',
    from_id        int          DEFAULT NULL comment 'user id',
	to_id          int          NOT NULL comment 'org id',
    content        text         NOT NULL,
    tid            int          DEFAULT NULL comment 'ml_algo.id = 15',
	`read`         int          DEFAULT 0 comment 'count read user'
) ENGINE = InnoDB;

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
    from_id        int          DEFAULT NULL comment 'user id',
    to_id          int          NOT NULL comment 'user id when msg, org id when notice',
    content        text         NOT NULL,
    tid            int          DEFAULT NULL comment 'ml_algo.id = 15',
	read_users     text         DEFAULT '[]' comment 'user list'
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
# Table: log_perf
# ----------------------------
DROP TABLE IF EXISTS log_perf;
CREATE TABLE log_perf
(
    id             int           NOT NULL AUTO_INCREMENT PRIMARY KEY,
    ts_utc         timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
	username       varchar(64)   NOT NULL comment 'username',
    user_id        int           NOT NULL,
    url            varchar(64)   DEFAULT NULL comment 'http://x/y',
	module         varchar(64)   DEFAULT NULL comment 'class name',
    method         varchar(64)   DEFAULT NULL,
	tid            int           DEFAULT NULL comment 'target id, like user id, soruce id',
	param          text          DEFAULT NULL comment 'json, like {in:{}, out:{}}',
    duration       int           comment 'query performance',
    CONSTRAINT fk_perf_user      foreign key(user_id)    REFERENCES sys_user(id)
) ENGINE = InnoDB;


# ----------------------------
# Table: log_system
# ----------------------------
DROP TABLE IF EXISTS log_system;
CREATE TABLE log_system
(
    id             int          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    ts             timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    severity       varchar(8)   NOT NULL comment 'Critical, Error, Warnning, Info, Debug',
    module         varchar(64)  NOT NULL comment 'internal module',
	content        text         NULL,
	`repeat`        int          DEFAULT 1 comment 'times of repetition'
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
# Table: gis_entry
# a area on map which is described by multiple points
# ----------------------------
DROP TABLE IF EXISTS gis_entry;
CREATE TABLE gis_entry
(
    id             int           NOT NULL AUTO_INCREMENT PRIMARY KEY,
	name           varchar(64)   NOT NULL,
	type           varchar(16)   NOT NULL comment 'type',
	`group`        varchar(64)   DEFAULT 'UnGrouped',
	attrs          varchar(128)  DEFAULT NULL comment 'array like [market, hot]',   
    points         text          DEFAULT NULL comment 'city or county',   
    areas          text          DEFAULT NULL comment 'province or state',      
	visible        boolean       NOT NULL DEFAULT true,
	created_by  varchar(64)     NOT NULL,
    created_at  timestamp       NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_by  varchar(64)     DEFAULT NULL,
    updated_at  timestamp       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
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
VALUES (1, 'OSM.Mapnik', 'tileLayer', 'baselayer', 'http://b.tile.osm.org/1/0/0.png', 'http://{s}.tile.osm.org/{z}/{x}/{y}.png', '{ attribution: "" }');

INSERT INTO gis_layer (id, name, type, `group`, icon, args, options)
VALUES (2, 'OPNVKarte', 'tileLayer', 'baselayer', 'https://tileserver.memomaps.de/tilegen/5/8/11.png', 'https://tileserver.memomaps.de/tilegen/{z}/{x}/{y}.png', '{attribution:\'Map <a href="https://memomaps.de/">memomaps.de</a> <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, map data &copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors\'}");

INSERT INTO gis_layer (id, name, type, `group`, icon, args, options)
VALUES (3, 'OpenTopoMap', 'tileLayer', 'baselayer', 'https://tile.opentopomap.org/3/7/2.png', 'https://{s}.tile.opentopomap.org/{z}/{x}/{y}.png', '{attribution:\'Map data: &copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors, <a href="http://viewfinderpanoramas.org">SRTM</a> | Map style: &copy; <a href="https://opentopomap.org">OpenTopoMap</a> (<a href="https://creativecommons.org/licenses/by-sa/3.0/">CC-BY-SA</a>)\'}');

INSERT INTO gis_layer (id, name, type, `group`, icon, args, options)
VALUES (4, 'Stadia.Dark', 'tileLayer', 'baselayer', 'https://tiles.stadiamaps.com/tiles/alidade_smooth_dark/6/14/25.png', 'https://tiles.stadiamaps.com/tiles/alidade_smooth_dark/{z}/{x}/{y}{r}.png', '{ attribution:\'&copy; <a href="https://stadiamaps.com/">Stadia Maps</a>, &copy; <a href="https://openmaptiles.org/">OpenMapTiles</a> &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors\'}');

INSERT INTO gis_layer (id, name, type, `group`, icon, args, options)
VALUES (5, 'Stadia.Outdoors', 'tileLayer', 'baselayer', 'https://tiles.stadiamaps.com/tiles/outdoors/8/77/94.png', 'https://tiles.stadiamaps.com/tiles/outdoors/{z}/{x}/{y}{r}.png', '{ attribution:\'&copy; <a href="https://stadiamaps.com/">Stadia Maps</a>, &copy; <a href="https://openmaptiles.org/">OpenMapTiles</a> &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors\'}');

INSERT INTO gis_layer (id, name, type, `group`, icon, args, options)
VALUES (6, 'OpenCycleMap', 'tileLayer', 'baselayer', 'https://tiles.stadiamaps.com/tiles/outdoors/1/1/0.png', 'https://{s}.tile.thunderforest.com/cycle/{z}/{x}/{y}.png?apikey={apikey}', '{ attribution:\'&copy; <a href="http://www.thunderforest.com/">Thunderforest</a>, &copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors\'}');

INSERT INTO gis_layer (id, name, type, `group`, icon, args, options)
VALUES (7, 'CyclOSM', 'tileLayer', 'baselayer', 'https://c.tile.openstreetmap.fr/hot/6/17/24.png', 'https://{s}.tile-cyclosm.openstreetmap.fr/cyclosm/{z}/{x}/{y}.png', '{ attribution:\'<a href="https://github.com/cyclosm/cyclosm-cartocss-style/releases" title="CyclOSM - Open Bicycle render">CyclOSM</a> | Map data: &copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors\'}');

INSERT INTO gis_layer (id, name, type, `group`, icon, args, options)
VALUES (8, 'Stamen.Toner', 'tileLayer', 'baselayer', 'https://stamen-tiles-b.a.ssl.fastly.net/toner/1/1/0.png', 'https://stamen-tiles-{s}.a.ssl.fastly.net/toner/{z}/{x}/{y}{r}.{ext}', '{ subdomains: "abcd", ext: "png", attribution:\'Map tiles by <a href="http://stamen.com">Stamen Design</a>, <a href="http://creativecommons.org/licenses/by/3.0">CC BY 3.0</a> &mdash; Map data &copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors\'}');

INSERT INTO gis_layer (id, name, type, `group`, icon, args, options)
VALUES (9, 'Stamen.Watercolor', 'tileLayer', 'baselayer', 'https://stamen-tiles-a.a.ssl.fastly.net/watercolor/1/0/0.jpg', 'https://stamen-tiles-{s}.a.ssl.fastly.net/watercolor/{z}/{x}/{y}.{ext}', '{subdomains: "abcd",ext: "jpg", attribution:\'Map tiles by <a href="http://stamen.com">Stamen Design</a>, <a href="http://creativecommons.org/licenses/by/3.0">CC BY 3.0</a> &mdash; Map data &copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors\'}');

INSERT INTO gis_layer (id, name, type, `group`, icon, args, options)
VALUES (10, 'Stamen.Terrain', 'tileLayer', 'baselayer', 'https://stamen-tiles-d.a.ssl.fastly.net/terrain-background/2/2/1.png', 'https://stamen-tiles-{s}.a.ssl.fastly.net/terrain/{z}/{x}/{y}{r}.{ext}', '{subdomains: "abcd",ext: "jpg", attribution:\'Map tiles by <a href="http://stamen.com">Stamen Design</a>, <a href="http://creativecommons.org/licenses/by/3.0">CC BY 3.0</a> &mdash; Map data &copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors\'}');

INSERT INTO gis_layer (id, name, type, `group`, icon, args, options)
VALUES (11, 'Stamen.TonerBackground', 'tileLayer', 'baselayer', 'https://stamen-tiles-d.a.ssl.fastly.net/toner-background/2/2/1.png', 'https://stamen-tiles-{s}.a.ssl.fastly.net/toner-background/{z}/{x}/{y}{r}.{ext}', '{subdomains: "abcd",ext: "jpg", attribution:\'Map tiles by <a href="http://stamen.com">Stamen Design</a>, <a href="http://creativecommons.org/licenses/by/3.0">CC BY 3.0</a> &mdash; Map data &copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors\'}');

INSERT INTO gis_layer (id, name, type, `group`, icon, args, options)
VALUES (12, 'Stamen.TerrainLabels', 'tileLayer', 'baselayer', 'https://stamen-tiles-b.a.ssl.fastly.net/terrain-labels/4/3/6.png', 'https://stamen-tiles-{s}.a.ssl.fastly.net/terrain-labels/{z}/{x}/{y}{r}.{ext}', '{subdomains: "abcd",ext: "jpg", attribution:\'Map tiles by <a href="http://stamen.com">Stamen Design</a>, <a href="http://creativecommons.org/licenses/by/3.0">CC BY 3.0</a> &mdash; Map data &copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors\'}');

INSERT INTO gis_layer (id, name, type, `group`, icon, args, options)
VALUES (13, 'Esri.WorldStreetMap', 'tileLayer', 'baselayer', 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/4/6/3', 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/{z}/{y}/{x}', '{ attribution: "Tiles &copy; Esri &mdash; Source: Esri, DeLorme, NAVTEQ, USGS, Intermap, iPC, NRCAN, Esri Japan, METI, Esri China (Hong Kong), Esri (Thailand), TomTom, 2012" }');

INSERT INTO gis_layer (id, name, type, `group`, icon, args, options)
VALUES (14, 'Esri.WorldImagery', 'tileLayer', 'baselayer', 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/8/94/77', 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', '{ attribution: "Tiles &copy; Esri &mdash; Source: Esri, DeLorme, NAVTEQ, USGS, Intermap, iPC, NRCAN, Esri Japan, METI, Esri China (Hong Kong), Esri (Thailand), TomTom, 2012" }');

INSERT INTO gis_layer (id, name, type, `group`, icon, args, options)
VALUES (15, 'Esri.WorldGrayCanvas', 'tileLayer', 'baselayer', 'https://c.basemaps.cartocdn.com/light_all/8/74/96.png', 'https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Light_Gray_Base/MapServer/tile/{z}/{y}/{x}', '{ attribution: "Tiles &copy; Esri &mdash; Esri, DeLorme, NAVTEQ" }');

INSERT INTO gis_layer (id, name, type, `group`, icon, args, options)
VALUES (16, 'CartoDB.DarkMatter', 'tileLayer', 'baselayer', 'https://d.basemaps.cartocdn.com/dark_all/5/8/11.png', 'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', '{ attribution:\'&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a>\' }');

INSERT INTO gis_layer (id, name, type, `group`, icon, args, options)
VALUES (17, 'WMS-Layer-1', 'tileLayer', 'baselayer', 'https://ows.mundialis.de/services/service?&service=WMS&request=GetMap&layers=TOPO-WMS%2COSM-Overlay-WMS&styles=&format=image%2Fjpeg&transparent=false&version=1.1.1&width=256&height=256&srs=EPSG%3A3857', 'http://ows.mundialis.de/services/service?&service=WMS&request=GetMap&layers=TOPO-WMS%2COSM-Overlay-WMS', '{ attribution: "" }');



INSERT INTO gis_layer (id, name, type, `group`, icon, args, options)
VALUES (31, 'OpenRailway', 'tileLayer', 'overlayer', 'https://a.tiles.openrailwaymap.org/standard/4/10/5.png', 'https://{s}.tiles.openrailwaymap.org/standard/{z}/{x}/{y}.png', '{ attribution:\'Map data: &copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors | Map style: &copy; <a href="https://www.OpenRailwayMap.org">OpenRailwayMap</a> (<a href="https://creativecommons.org/licenses/by-sa/3.0/">CC-BY-SA</a>)\'}');

INSERT INTO gis_layer (id, name, type, `group`, icon, args, options)
VALUES (32, 'Stamen.TonerHybrid', 'tileLayer', 'overlayer', 'https://stamen-tiles-a.a.ssl.fastly.net/toner-hybrid/4/3/5.png', 'https://stamen-tiles-{s}.a.ssl.fastly.net/toner-hybrid/{z}/{x}/{y}{r}.{ext}', '{subdomains: "abcd",ext: "png", attribution:\'Map tiles by <a href="http://stamen.com">Stamen Design</a>, <a href="http://creativecommons.org/licenses/by/3.0">CC BY 3.0</a> &mdash; Map data &copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors\'}');

INSERT INTO gis_layer (id, name, type, `group`, icon, args, options)
VALUES (33, 'Stamen.TonerLines', 'tileLayer', 'overlayer', 'https://stamen-tiles-a.a.ssl.fastly.net/toner-lines/4/3/5.png', 'https://stamen-tiles-{s}.a.ssl.fastly.net/toner-lines/{z}/{x}/{y}{r}.{ext}', '{subdomains: "abcd",ext: "png", attribution:\'Map tiles by <a href="http://stamen.com">Stamen Design</a>, <a href="http://creativecommons.org/licenses/by/3.0">CC BY 3.0</a> &mdash; Map data &copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors\'}');

INSERT INTO gis_layer (id, name, type, `group`, icon, args, options)
VALUES (34, 'Stamen.TonerLabels', 'tileLayer', 'overlayer', 'https://stamen-tiles-b.a.ssl.fastly.net/toner-labels/4/3/6.png', 'https://stamen-tiles-{s}.a.ssl.fastly.net/toner-labels/{z}/{x}/{y}{r}.{ext}', '{subdomains: "abcd",ext: "png", attribution:\'Map tiles by <a href="http://stamen.com">Stamen Design</a>, <a href="http://creativecommons.org/licenses/by/3.0">CC BY 3.0</a> &mdash; Map data &copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors\'}');

INSERT INTO gis_layer (id, name, type, `group`, icon, args, options)
VALUES (35, 'Stamen.TopOSMFeatures', 'tileLayer', 'overlayer', 'https://stamen-tiles-c.a.ssl.fastly.net/toposm-features/6/16/25.png', 'https://stamen-tiles-{s}.a.ssl.fastly.net/toposm-features/{z}/{x}/{y}{r}.{ext}', '{subdomains: "abcd",ext: "png", attribution:\'Map tiles by <a href="http://stamen.com">Stamen Design</a>, <a href="http://creativecommons.org/licenses/by/3.0">CC BY 3.0</a> &mdash; Map data &copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors\'}');


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
    query          text         DEFAULT NULL comment 'sql query',
	final_query    text         DEFAULT NULL comment 'final query',
    fields         text         NOT NULL comment 'json array like [{name:"age", type:"number", cat:"conti", weight:92, target:false, omit: false}]',
	target		   text         DEFAULT NULL comment 'target array',
	transform	   text         DEFAULT NULL comment 'json array like [{field:"age", scale:"minmax", na:"mean"}]',
	f_count        int          DEFAULT NULL comment'feature count',
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


INSERT INTO ml_dataset (id, name, `group`, `desc`, variable, query, final_query, features, target, transform, f_count, source_id, org_id, `public`, created_by, created_at, updated_by, updated_at)
VALUES (1, 'Iris', 'first', 'test', '[{name:"aaa", type:"number", value:"35"}]', 'select * from iris', null, '[{name:"sepal_length", type:"number"}, {name:"sepal_width", type:"number"}, {name:"petal_length", type:"number"},{name:"petal_width", type:"number"},{name:"uid", hidden:true}]', '{name:"variety", type:"string", unique:["v1", "v2", "v3"]}', '[{field:"age", scale:"minmax", na:"mean"}]', 4, 1, 1, true, 'Admin', null, 'Admin', null);


# ----------------------------
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

INSERT INTO ml_eda (id, name, `group`, `desc`, config, dataset_id, org_id, `public`, created_by, created_at, updated_by, updated_at)
VALUES (1, 'Iris_eda', 'eda', 'test', '[{type:"hist", kde:true}]', 1, 1, true, 'Admin', null, 'Admin', null);



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
    framework      varchar(16)   DEFAULT 'python' comment 'python, pytorch, tensorflow, paddle, dl4j, JDL, keras, auto-sklearn',
    frame_ver      varchar(8)    DEFAULT '3.10',
	category       varchar(16)   NOT NULL comment 'classification, regression, clustering, reduction',
	algo_name      varchar(64)   comment 'algorithm name',
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


INSERT INTO ml_algo (id, name, `desc`, `group`, framework, frame_ver, category, algo_name, data_cfg, train_cfg, src_code, org_id, `public`, created_by, created_at, updated_by, updated_at)
VALUES (1, 'svm', 'new svm algorithm', 'first', 'python', '3.10', 'clf', 'svm', 5, 'svm() return data', '{dataset: 1}', '{timeout:5, trials:3, epochs: 2}', 1, true, 'Superman', null, null, null);


# ----------------------------
# Table: ml_experiment
# ----------------------------
DROP TABLE IF EXISTS ml_exper;
CREATE TABLE ml_exper
(
    id             int           NOT NULL AUTO_INCREMENT PRIMARY KEY,
    algo_id        int           NOT NULL,
	exper_id	   int			 NOT NULL comment 'experiment id in mlflow',
	trials		   text          DEFAULT NULL comment '[{id:2,params:{},eval:{}}]',
    duration       int           DEFAULT NULL comment 'unit minute',
	status         int           DEFAULT 1 comment '0:succ, 1:scheduled, 2:inprogress, 3:failed, 4:interrupted, 5:timeout, 6:except, 7:canceled',
	user_id        int			 NOT NULL,
	org_id         int           NOT NULL,
    created_at  timestamp        NOT NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT fk_exper_algo      foreign key(algo_id)     REFERENCES ml_algo(id),
	CONSTRAINT fk_exper_user      foreign key(user_id)    REFERENCES sys_user(id),
	CONSTRAINT fk_exper_org       foreign key(org_id)     REFERENCES sys_org(id)
) ENGINE = InnoDB;

INSERT INTO ml_exper (id, algo_id, exper_id, trials, duration, status, user_id, org_id, created_at)
VALUES (1, 17, 50, '[{id:123, params: {a:1, b:2}, eval:{loss:0.2}}]', 2, 0, 3, 1, null);



# ----------------------------
# Table: ml_flow
# ----------------------------
DROP TABLE IF EXISTS ml_flow;
CREATE TABLE ml_flow
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

INSERT INTO ml_flow (id, pid, name, `desc`, `group`, config, workflow, canvas, flow_ver, version, last_run, duration, status, error, org_id, `public`, created_by, created_at, updated_by, updated_at)
VALUES (1, null, 'Svm train', 'New svm train', 'first', '{timeout: 10}', '{nodes:[{id:"node1", shape:"rect", width: 20, height: 10}], edges:[]}', '{gbColor:"#123456"}', '2.0', '0', '2024-04-10 05:30:00', 88, 'success', null, 1, true, 'GavinZ', null, 'GavinZ', null);

# ----------------------------
# Table: ml_flow_history
# ----------------------------
DROP TABLE IF EXISTS ml_flow_history;
CREATE TABLE ml_flow_history
(
    id             int           NOT NULL AUTO_INCREMENT PRIMARY KEY,
    flow_id        int           NOT NULL,
	config         text          DEFAULT NULL comment 'ML training config', 
	workflow       text          DEFAULT NULL comment 'flow config with json', 
    x6_ver         varchar(8)    DEFAULT NULL comment 'antvX6 version',
    duration       int           DEFAULT NULL comment 'unit minute',
	status         int           DEFAULT 5 comment '0:succ, 1:failure, 3:interruption, 4:timeout, 5:exception',
    result         text          DEFAULT NULL comment 'json result, {acu: 0.95}',
	org_id         int           NOT NULL,
    created_by  varchar(64)      NOT NULL,
    created_at  timestamp        NOT NULL DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT fk_flow_h_flow       foreign key(flow_id)     REFERENCES ml_flow(id),
	CONSTRAINT fk_flow_h_org       foreign key(org_id)     REFERENCES sys_org(id)
) ENGINE = InnoDB;


# ----------------------------
# Table: ai_model
# ----------------------------
DROP TABLE IF EXISTS ai_model;
CREATE TABLE ai_model
(
    id             int            NOT NULL AUTO_INCREMENT PRIMARY KEY,
    sid            int            DEFAULT NULL comment 'history version',
    name           varchar(64)    NOT NULL,
    `desc`         varchar(128)   DEFAULT NULL comment 'description',
    category       varchar(64)    NOT NULL comment 'image, video, audio, text, data, security',
    type           varchar(32)    NOT NULL comment 'clacification, regression, clustering, reduction',
    tags           varchar(64)    DEFAULT NULL comment 'used to search like ["image", "autopilot", "medicine"]',
    version        varchar(16)    NOT NULL comment 'model version',
    network        varchar(16)    DEFAULT NULL comment 'CNN, RNN, Resnet, Letnet',
    framework      varchar(16)    NOT NULL comment 'pytorch, tensorflow, paddle, dj4j, JDL, auto-sklearn', 
    frame_ver      varchar(16)    DEFAULT NULL,
    trainset       varchar(64)    DEFAULT NULL comment 'dataset of training',
    files          text           NOT NULL comment 'array like ["aaa.pkl", "bbb.json", "ccc.txt"]',
    input          text           NOT NULL comment 'json object like {batch:true, grayscale:false, size: [224,224], normalize: true}',
    output         text           NOT NULL comment 'json object like {boundary,accuracy}',
    eval           text           DEFAULT NULL comment 'json object like {precision: 98}',
    score          int            DEFAULT NULL comment '0 to 10',
    price          varchar(16)    DEFAULT NULL comment '$10/year',
    detail         text           DEFAULT NULL, 
    weblink        varchar(64)    DEFAULT NULL comment 'demo link or home page',
    model_id       int            DEFAULT NULL comment 'point to a ml model like a foreign key',
	org_id         int            NOT NULL,
    `public`       boolean        NOT NULL DEFAULT false,
    created_by     varchar(64)    NOT NULL,
    created_at     timestamp      NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_by     varchar(64)    DEFAULT NULL,
    updated_at     timestamp      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	CONSTRAINT fk_aimodel_org       foreign key(org_id)     REFERENCES sys_org(id)
) ENGINE = InnoDB;

INSERT INTO ai_model (id, sid, name, `desc`, category, type, tags, version, network, framework, frame_ver, trainset, files, input, output, eval, score, price, detail, weblink, model_id, org_id, `public`, created_by, created_at, updated_by, updated_at)
VALUES (1, null, 'resnet50', 'Detect daily objects', 'image', 'Object detection', '["image", "autopilot"]', '1.0', 'Resnet50', 'DJL', '0.17.0', 'ImageNet', '{model: "null"}', '{batch:true, grayscale: false, size: [224,224], normalize: true}', '{boundary: boundaryBox, accuracy: accuracy}', '{precision: 98}', 9, '0', null, 'www.baidu.com', null, 1, true, 'GavinZ', null, 'GavinZ', null),
(2, null, 'resnet18', 'Recognize daily supplies', 'image', 'Classification', '["image"]', '1.0', 'Resnet18', 'pytorch', '1.11', null, '{file:["resnet18.pt","synset.txt"]}', '{batch:true, grayscale: false, size: [224,224], normalize: true}', '{boundary: boundaryBox, accuracy: accuracy}', '{precision: 95}', 8, '10', null, 'www.baidu.com', null, 1, true, 'GavinZ', null, null, null),
(3, null, 'resnet18_v1', 'Recognize daily supplies', 'image', 'Classification', '["image"]', '1.0', 'Resnet18', 'mxnet', '0.17.0', null, '{file:["resnet18_v1-0000.params","resnet18_v1-symbol.json","synset.txt"]}', '{batch:true, grayscale: false, size: [224,224], normalize: true}', '{boundary: boundaryBox, accuracy: accuracy}', '{precision: 95}', 8, '5', null, 'www.baidu.com', null, 1, true, 'GavinZ', null, null, null),
(4, null, 'hwr_mnist', 'Handwriting numerals recognition', 'image', 'Classification', '["handwriting","number", "image"]', '1.0', 'MPL', 'mxnet', '0.17.0', null, '{file:["hwr_mnist.params"]}', '{batch:true, grayscale: false, size: [224,224], normalize: true}', '{boundary: boundaryBox, accuracy: accuracy}', '{precision: 95}', 8, '10', null, 'www.baidu.com', null, 1, true, 'GavinZ', null, null, null),
(5, null, 'pneumonia', 'Pneumonia recognition', 'image', 'Classification', '["pneumonia", "image"]', '1.0', 'Resnet', 'tensorflow', '0.17.0', null, '{file:["pneumonia.pb"]}', '{batch:true, grayscale: false, size: [224,224], normalize: true}', '{boundary: boundaryBox, accuracy: accuracy}', '{precision: 95}', 8, '0', null, 'www.baidu.com', null, 1, true, 'GavinZ', null, null, null);




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
# Table: ai_data
# ----------------------------
DROP TABLE IF EXISTS ai_data;
CREATE TABLE ai_data
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
    CONSTRAINT fk_aidata_model   foreign key(model_id)  REFERENCES ai_model(id)
) ENGINE = InnoDB;

INSERT INTO ai_data (id, name, `desc`, `group`, type, field, model_id, platform, platform_ver, content, org_id, `public`, created_by, created_at, updated_by, updated_at)
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
