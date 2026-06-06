CREATE DATABASE IF NOT EXISTS apartment_hub DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_general_ci;
USE apartment_hub;

-- System tables
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    real_name VARCHAR(50),
    phone VARCHAR(20),
    email VARCHAR(100),
    avatar VARCHAR(255),
    status TINYINT DEFAULT 1 COMMENT '0=disabled 1=enabled',
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL,
    role_code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    status TINYINT DEFAULT 1,
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT PRIMARY KEY,
    parent_id BIGINT DEFAULT 0,
    permission_name VARCHAR(100) NOT NULL,
    permission_code VARCHAR(100) NOT NULL UNIQUE,
    type TINYINT DEFAULT 0 COMMENT '0=menu 1=button 2=api',
    path VARCHAR(255),
    icon VARCHAR(100),
    sort_order INT DEFAULT 0,
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    INDEX idx_role_id (role_id),
    INDEX idx_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_dict (
    id BIGINT PRIMARY KEY,
    dict_type VARCHAR(50) NOT NULL,
    dict_code VARCHAR(50) NOT NULL,
    dict_label VARCHAR(100) NOT NULL,
    sort_order INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Business tables
CREATE TABLE IF NOT EXISTS apartment (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    address VARCHAR(255),
    city VARCHAR(50),
    district VARCHAR(50),
    contact_name VARCHAR(50),
    contact_phone VARCHAR(20),
    total_buildings INT DEFAULT 0,
    description TEXT,
    status TINYINT DEFAULT 1 COMMENT '0=inactive 1=active',
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS building (
    id BIGINT PRIMARY KEY,
    apartment_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    floors INT DEFAULT 1,
    description VARCHAR(255),
    status TINYINT DEFAULT 1,
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_apartment_id (apartment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS room_type (
    id BIGINT PRIMARY KEY,
    type_name VARCHAR(50) NOT NULL,
    area DECIMAL(8,2),
    orientation VARCHAR(20),
    base_price DECIMAL(10,2),
    facilities JSON,
    description VARCHAR(255),
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS room (
    id BIGINT PRIMARY KEY,
    building_id BIGINT NOT NULL,
    room_type_id BIGINT,
    room_number VARCHAR(20) NOT NULL,
    floor INT,
    rent_price DECIMAL(10,2),
    image VARCHAR(500) COMMENT 'Room image URL',
    status TINYINT DEFAULT 0 COMMENT '0=vacant 1=rented 2=maintenance 3=reserved',
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_building_room (building_id, room_number),
    INDEX idx_building_id (building_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS tenant (
    id BIGINT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    gender TINYINT COMMENT '0=female 1=male',
    phone VARCHAR(20) NOT NULL UNIQUE,
    id_card VARCHAR(18) NOT NULL UNIQUE,
    id_card_front VARCHAR(255),
    id_card_back VARCHAR(255),
    emergency_contact VARCHAR(50),
    emergency_phone VARCHAR(20),
    tag VARCHAR(50) COMMENT 'student/white-collar/family',
    remark VARCHAR(255),
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS contract (
    id BIGINT PRIMARY KEY,
    contract_no VARCHAR(50) NOT NULL UNIQUE,
    tenant_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    rent_amount DECIMAL(10,2) NOT NULL,
    deposit_amount DECIMAL(10,2) NOT NULL,
    payment_cycle TINYINT DEFAULT 1 COMMENT '1=monthly 3=quarterly 12=yearly',
    status TINYINT DEFAULT 0 COMMENT '0=draft 1=active 2=expired 3=terminated',
    terminate_reason VARCHAR(255),
    remark TEXT,
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_room_id (room_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bill (
    id BIGINT PRIMARY KEY,
    bill_no VARCHAR(50) NOT NULL UNIQUE,
    contract_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    bill_type TINYINT DEFAULT 0 COMMENT '0=rent 1=deposit 2=utility 3=property',
    amount DECIMAL(10,2) NOT NULL,
    billing_month VARCHAR(7) COMMENT '2026-06',
    due_date DATE,
    status TINYINT DEFAULT 0 COMMENT '0=pending 1=paid 2=overdue 3=cancelled',
    paid_time DATETIME,
    remark VARCHAR(255),
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_contract_id (contract_id),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_status (status),
    INDEX idx_billing_month (billing_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS payment (
    id BIGINT PRIMARY KEY,
    payment_no VARCHAR(50) NOT NULL UNIQUE,
    bill_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    payment_method TINYINT DEFAULT 0 COMMENT '0=cash 1=bank_transfer 2=alipay 3=wechat',
    payment_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(255),
    operator_id BIGINT,
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_bill_id (bill_id),
    INDEX idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS repair_order (
    id BIGINT PRIMARY KEY,
    order_no VARCHAR(50) NOT NULL UNIQUE,
    room_id BIGINT NOT NULL,
    tenant_id BIGINT,
    type TINYINT DEFAULT 0 COMMENT '0=plumbing 1=furniture 2=appliance 3=network 4=other',
    priority TINYINT DEFAULT 1 COMMENT '0=urgent 1=normal 2=low',
    description TEXT,
    images JSON,
    status TINYINT DEFAULT 0 COMMENT '0=pending 1=assigned 2=in_progress 3=completed 4=verified',
    assignee_id BIGINT,
    resolve_time DATETIME,
    resolve_remark TEXT,
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_room_id (room_id),
    INDEX idx_status (status),
    INDEX idx_assignee_id (assignee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS operation_log (
    id BIGINT PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(50),
    module VARCHAR(50),
    operation VARCHAR(100),
    method VARCHAR(255),
    params TEXT,
    ip VARCHAR(50),
    duration BIGINT,
    status TINYINT DEFAULT 1 COMMENT '0=fail 1=success',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS announcement (
    id BIGINT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    summary VARCHAR(500),
    publisher_id BIGINT,
    status TINYINT DEFAULT 1 COMMENT '0=draft 1=published',
    top_flag TINYINT DEFAULT 0 COMMENT '0=normal 1=pinned',
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_publisher_id (publisher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
