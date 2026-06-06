USE apartment_hub;

-- Admin user (password: admin123)
INSERT INTO sys_user (id, username, password, real_name, phone, email, status) VALUES
(1, 'admin', '$2a$10$ugPY849DF.bTDwfD63bJOeHobzYuNXRyG5OBPc2xJX076xzNFe7SS', 'System Admin', '13800000001', 'admin@apartmenthub.com', 1),
(2, 'manager', '$2a$10$ugPY849DF.bTDwfD63bJOeHobzYuNXRyG5OBPc2xJX076xzNFe7SS', 'Zhang Manager', '13800000002', 'manager@apartmenthub.com', 1),
(3, 'staff', '$2a$10$ugPY849DF.bTDwfD63bJOeHobzYuNXRyG5OBPc2xJX076xzNFe7SS', 'Li Staff', '13800000003', 'staff@apartmenthub.com', 1);

-- Roles
INSERT INTO sys_role (id, role_name, role_code, description, status) VALUES
(1, 'Administrator', 'ADMIN', 'System administrator with full access', 1),
(2, 'Manager', 'MANAGER', 'Apartment manager', 1),
(3, 'Staff', 'STAFF', 'Front desk staff', 1);

-- User-Role mapping
INSERT INTO sys_user_role (id, user_id, role_id) VALUES
(1, 1, 1), (2, 2, 2), (3, 3, 3);

-- Permissions
INSERT INTO sys_permission (id, parent_id, permission_name, permission_code, type, path, icon, sort_order) VALUES
(1, 0, 'Dashboard', 'dashboard', 0, '/dashboard', 'Odometer', 1),
(2, 0, 'Property Management', 'apartment', 0, NULL, 'OfficeBuilding', 2),
(3, 2, 'Apartment List', 'apartment:list', 0, '/apartment/list', NULL, 1),
(4, 2, 'Building List', 'apartment:building', 0, '/apartment/building', NULL, 2),
(5, 2, 'Room List', 'apartment:room', 0, '/apartment/room', NULL, 3),
(6, 2, 'Room Type', 'apartment:room-type', 0, '/apartment/room-type', NULL, 4),
(7, 0, 'Tenant Management', 'tenant', 0, NULL, 'User', 3),
(8, 7, 'Tenant List', 'tenant:list', 0, '/tenant/list', NULL, 1),
(9, 0, 'Contract Management', 'contract', 0, NULL, 'Document', 4),
(10, 9, 'Contract List', 'contract:list', 0, '/contract/list', NULL, 1),
(11, 9, 'New Contract', 'contract:create', 0, '/contract/create', NULL, 2),
(12, 0, 'Bill Management', 'bill', 0, NULL, 'Money', 5),
(13, 12, 'Bill List', 'bill:list', 0, '/bill/list', NULL, 1),
(14, 0, 'Checkout Management', 'checkout', 0, NULL, 'SwitchButton', 6),
(15, 14, 'Checkout List', 'checkout:list', 0, '/checkout/list', NULL, 1),
(16, 0, 'Repair Management', 'repair', 0, NULL, 'SetUp', 7),
(17, 16, 'Repair Orders', 'repair:list', 0, '/repair/list', NULL, 1),
(18, 0, 'Reports', 'report', 0, NULL, 'DataAnalysis', 8),
(19, 18, 'Revenue Report', 'report:revenue', 0, '/report/revenue', NULL, 1),
(20, 18, 'Occupancy Report', 'report:occupancy', 0, '/report/occupancy', NULL, 2),
(21, 0, 'System Management', 'system', 0, NULL, 'Setting', 9),
(22, 21, 'User Management', 'system:user', 0, '/system/user', NULL, 1),
(23, 21, 'Role Management', 'system:role', 0, '/system/role', NULL, 2),
(24, 21, 'Dict Management', 'system:dict', 0, '/system/dict', NULL, 3),
(25, 21, 'Operation Log', 'system:log', 0, '/system/log', NULL, 4),
(26, 0, 'Content Management', 'announcement', 0, NULL, 'Bell', 10),
(27, 26, 'Announcement List', 'announcement:list', 0, '/announcement/list', NULL, 1),
(28, 2, 'Room Discovery', 'apartment:discover', 0, '/apartment/discover', NULL, 5);

-- Role-Permission mapping (Admin has all)
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES
(1, 1, 1),(2, 1, 2),(3, 1, 3),(4, 1, 4),(5, 1, 5),(6, 1, 6),(7, 1, 7),(8, 1, 8),
(9, 1, 9),(10, 1, 10),(11, 1, 11),(12, 1, 12),(13, 1, 13),(14, 1, 14),(15, 1, 15),
(16, 1, 16),(17, 1, 17),(18, 1, 18),(19, 1, 19),(20, 1, 20),(21, 1, 21),(22, 1, 22),
(23, 1, 23),(24, 1, 24),(25, 1, 25),
(58, 1, 26),(59, 1, 27),(60, 1, 28);

-- Manager permissions (no system management)
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES
(26, 2, 1),(27, 2, 2),(28, 2, 3),(29, 2, 4),(30, 2, 5),(31, 2, 6),(32, 2, 7),(33, 2, 8),
(34, 2, 9),(35, 2, 10),(36, 2, 11),(37, 2, 12),(38, 2, 13),(39, 2, 14),(40, 2, 15),
(41, 2, 16),(42, 2, 17),(43, 2, 18),(44, 2, 19),(45, 2, 20),
(61, 2, 26),(62, 2, 27),(63, 2, 28);

-- Staff permissions (limited)
INSERT INTO sys_role_permission (id, role_id, permission_id) VALUES
(46, 3, 1),(47, 3, 2),(48, 3, 3),(49, 3, 5),(50, 3, 7),(51, 3, 8),
(52, 3, 9),(53, 3, 10),(54, 3, 12),(55, 3, 13),(56, 3, 16),(57, 3, 17);

-- Dict data
INSERT INTO sys_dict (id, dict_type, dict_code, dict_label, sort_order) VALUES
(1, 'room_status', '0', 'Vacant', 1),
(2, 'room_status', '1', 'Rented', 2),
(3, 'room_status', '2', 'Maintenance', 3),
(4, 'room_status', '3', 'Reserved', 4),
(5, 'bill_status', '0', 'Pending', 1),
(6, 'bill_status', '1', 'Paid', 2),
(7, 'bill_status', '2', 'Overdue', 3),
(8, 'bill_status', '3', 'Cancelled', 4),
(9, 'payment_method', '0', 'Cash', 1),
(10, 'payment_method', '1', 'Bank Transfer', 2),
(11, 'payment_method', '2', 'Alipay', 3),
(12, 'payment_method', '3', 'WeChat', 4),
(13, 'repair_type', '0', 'Plumbing', 1),
(14, 'repair_type', '1', 'Furniture', 2),
(15, 'repair_type', '2', 'Appliance', 3),
(16, 'repair_type', '3', 'Network', 4),
(17, 'repair_type', '4', 'Other', 5),
(18, 'contract_status', '0', 'Draft', 1),
(19, 'contract_status', '1', 'Active', 2),
(20, 'contract_status', '2', 'Expired', 3),
(21, 'contract_status', '3', 'Terminated', 4),
(22, 'payment_cycle', '1', 'Monthly', 1),
(23, 'payment_cycle', '3', 'Quarterly', 2),
(24, 'payment_cycle', '12', 'Yearly', 3),
(25, 'repair_priority', '0', 'Urgent', 1),
(26, 'repair_priority', '1', 'Normal', 2),
(27, 'repair_priority', '2', 'Low', 3);

-- Sample Apartment
INSERT INTO apartment (id, name, address, city, district, contact_name, contact_phone, total_buildings, description, status) VALUES
(1, 'Sunshine Garden', '123 Nanjing Road', 'Nanjing', 'Xuanwu', 'Wang Manager', '13900001111', 2, 'Premium apartment complex near metro', 1),
(2, 'Lakeside Residence', '456 Xuanwu Lake Road', 'Nanjing', 'Gulou', 'Chen Manager', '13900002222', 1, 'Lakeside luxury apartments', 1);

-- Sample Buildings
INSERT INTO building (id, apartment_id, name, floors, description, status) VALUES
(1, 1, 'Building A', 6, 'Main building', 1),
(2, 1, 'Building B', 6, 'East building', 1),
(3, 2, 'Tower 1', 8, 'Lake view tower', 1);

-- Room Types
INSERT INTO room_type (id, type_name, area, orientation, base_price, facilities, description) VALUES
(1, 'Standard Single', 25.00, 'South', 1500.00, '{"wifi":true,"ac":true,"bathroom":true}', 'Standard single room'),
(2, 'Deluxe Double', 40.00, 'South', 2500.00, '{"wifi":true,"ac":true,"bathroom":true,"kitchen":true}', 'Deluxe double room'),
(3, 'Family Suite', 60.00, 'South', 3800.00, '{"wifi":true,"ac":true,"bathroom":true,"kitchen":true,"balcony":true}', 'Family suite with balcony');

-- Sample Rooms
INSERT INTO room (id, building_id, room_type_id, room_number, floor, rent_price, image, status) VALUES
(1, 1, 1, 'A-101', 1, 1500.00, 'https://picsum.photos/seed/apt-single-01/400/300', 0),
(2, 1, 1, 'A-102', 1, 1500.00, 'https://picsum.photos/seed/apt-single-01/400/300', 0),
(3, 1, 2, 'A-201', 2, 2500.00, 'https://picsum.photos/seed/apt-double-01/400/300', 0),
(4, 1, 2, 'A-202', 2, 2500.00, 'https://picsum.photos/seed/apt-double-01/400/300', 0),
(5, 1, 3, 'A-301', 3, 3800.00, 'https://picsum.photos/seed/apt-suite-01/400/300', 0),
(6, 2, 1, 'B-101', 1, 1500.00, 'https://picsum.photos/seed/apt-single-02/400/300', 0),
(7, 2, 1, 'B-102', 1, 1500.00, 'https://picsum.photos/seed/apt-single-02/400/300', 0),
(8, 2, 2, 'B-201', 2, 2500.00, 'https://picsum.photos/seed/apt-double-02/400/300', 0),
(9, 3, 2, 'T1-301', 3, 2800.00, 'https://picsum.photos/seed/apt-double-02/400/300', 0),
(10, 3, 3, 'T1-501', 5, 4200.00, 'https://picsum.photos/seed/apt-suite-01/400/300', 0);

-- Sample Tenants
INSERT INTO tenant (id, name, gender, phone, id_card, emergency_contact, emergency_phone, tag) VALUES
(1, 'Zhang San', 1, '13811111111', '320102199001011234', 'Zhang Father', '13800001111', 'white-collar'),
(2, 'Li Si', 0, '13822222222', '320102199502022345', 'Li Mother', '13800002222', 'student'),
(3, 'Wang Wu', 1, '13833333333', '320102198803033456', 'Wang Wife', '13800003333', 'family');

-- More demo tenants
INSERT INTO tenant (id, name, gender, phone, id_card, emergency_contact, emergency_phone, tag, remark) VALUES
(4, 'Chen Yu', 0, '13844444444', '320102199610104567', 'Chen Father', '13800004444', 'white-collar', 'Prefers south-facing room'),
(5, 'Liu Qiang', 1, '13855555555', '320102199211115678', 'Liu Mother', '13800005555', 'white-collar', 'Works near subway line 2'),
(6, 'Zhao Min', 0, '13866666666', '320102199807126789', 'Zhao Sister', '13800006666', 'student', 'Needs quiet floor'),
(7, 'Sun Lei', 1, '13877777777', '320102198912137890', 'Sun Wife', '13800007777', 'family', 'Family with one child');

-- Expand rooms so pages and reports have richer data
INSERT INTO room (id, building_id, room_type_id, room_number, floor, rent_price, image, status) VALUES
(11, 1, 1, 'A-401', 4, 1650.00, 'https://picsum.photos/seed/apt-single-03/400/300', 0),
(12, 1, 2, 'A-402', 4, 2600.00, 'https://picsum.photos/seed/apt-double-03/400/300', 0),
(13, 2, 1, 'B-301', 3, 1600.00, 'https://picsum.photos/seed/apt-single-04/400/300', 0),
(14, 2, 3, 'B-501', 5, 3950.00, 'https://picsum.photos/seed/apt-suite-02/400/300', 0),
(15, 3, 1, 'T1-201', 2, 1800.00, 'https://picsum.photos/seed/apt-single-05/400/300', 0),
(16, 3, 2, 'T1-401', 4, 3000.00, 'https://picsum.photos/seed/apt-double-04/400/300', 0);

-- Active contracts
INSERT INTO contract (id, contract_no, tenant_id, room_id, start_date, end_date, rent_amount, deposit_amount, payment_cycle, status, remark) VALUES
(1, 'CT-2025-0001', 1, 3, '2025-09-01', '2026-08-31', 2500.00, 2500.00, 1, 1, 'Monthly rent, standard deposit'),
(2, 'CT-2025-0002', 2, 5, '2025-10-01', '2026-09-30', 3800.00, 3800.00, 1, 1, 'Family suite long stay'),
(3, 'CT-2025-0003', 3, 8, '2025-11-15', '2026-11-14', 2500.00, 2500.00, 1, 1, 'Company reimbursed rent'),
(4, 'CT-2026-0001', 4, 9, '2026-01-01', '2026-12-31', 2800.00, 2800.00, 1, 1, 'Lake view unit'),
(5, 'CT-2026-0002', 5, 10, '2026-02-01', '2027-01-31', 4200.00, 4200.00, 1, 1, 'Premium family suite'),
(6, 'CT-2026-0003', 6, 12, '2026-03-01', '2027-02-28', 2600.00, 2600.00, 1, 1, 'Near elevator'),
(7, 'CT-2026-0004', 7, 14, '2026-04-01', '2027-03-31', 3950.00, 3950.00, 1, 1, 'Reserved parking space');

UPDATE room SET status = 1 WHERE id IN (3, 5, 8, 9, 10, 12, 14);
UPDATE room SET status = 2 WHERE id IN (4, 15);
UPDATE room SET status = 3 WHERE id IN (16);

-- Bills across recent months
INSERT INTO bill (id, bill_no, contract_id, tenant_id, room_id, bill_type, amount, billing_month, due_date, status, paid_time, remark) VALUES
(1, 'BILL-202601-001', 1, 1, 3, 0, 2500.00, '2026-01', '2026-01-05', 1, '2026-01-03 10:20:00', 'January rent'),
(2, 'BILL-202601-002', 2, 2, 5, 0, 3800.00, '2026-01', '2026-01-05', 1, '2026-01-04 14:10:00', 'January rent'),
(3, 'BILL-202602-001', 1, 1, 3, 0, 2500.00, '2026-02', '2026-02-05', 1, '2026-02-02 09:18:00', 'February rent'),
(4, 'BILL-202602-002', 4, 4, 9, 1, 2800.00, '2026-02', '2026-02-05', 1, '2026-02-01 16:43:00', 'Deposit'),
(5, 'BILL-202603-001', 3, 3, 8, 0, 2500.00, '2026-03', '2026-03-05', 1, '2026-03-03 11:30:00', 'March rent'),
(6, 'BILL-202603-002', 5, 5, 10, 0, 4200.00, '2026-03', '2026-03-05', 1, '2026-03-04 13:05:00', 'March rent'),
(7, 'BILL-202604-001', 6, 6, 12, 0, 2600.00, '2026-04', '2026-04-05', 1, '2026-04-03 15:24:00', 'April rent'),
(8, 'BILL-202604-002', 7, 7, 14, 0, 3950.00, '2026-04', '2026-04-05', 1, '2026-04-05 08:40:00', 'April rent'),
(9, 'BILL-202605-001', 1, 1, 3, 0, 2500.00, '2026-05', '2026-05-05', 1, '2026-05-02 10:00:00', 'May rent'),
(10, 'BILL-202605-002', 2, 2, 5, 0, 3800.00, '2026-05', '2026-05-05', 1, '2026-05-03 19:42:00', 'May rent'),
(11, 'BILL-202605-003', 4, 4, 9, 2, 320.00, '2026-05', '2026-05-10', 1, '2026-05-09 12:05:00', 'Utilities'),
(12, 'BILL-202606-001', 1, 1, 3, 0, 2500.00, '2026-06', '2026-06-05', 0, NULL, 'June rent'),
(13, 'BILL-202606-002', 2, 2, 5, 0, 3800.00, '2026-06', '2026-06-05', 2, NULL, 'June rent overdue'),
(14, 'BILL-202606-003', 5, 5, 10, 0, 4200.00, '2026-06', '2026-06-05', 0, NULL, 'June rent'),
(15, 'BILL-202606-004', 7, 7, 14, 2, 460.00, '2026-06', '2026-06-03', 2, NULL, 'Utilities overdue');

-- Payments for paid bills
INSERT INTO payment (id, payment_no, bill_id, tenant_id, amount, payment_method, payment_time, remark, operator_id) VALUES
(1, 'PAY-202601-001', 1, 1, 2500.00, 2, '2026-01-03 10:20:00', 'Alipay payment', 2),
(2, 'PAY-202601-002', 2, 2, 3800.00, 3, '2026-01-04 14:10:00', 'WeChat payment', 2),
(3, 'PAY-202602-001', 3, 1, 2500.00, 1, '2026-02-02 09:18:00', 'Bank transfer', 2),
(4, 'PAY-202602-002', 4, 4, 2800.00, 2, '2026-02-01 16:43:00', 'Deposit paid', 2),
(5, 'PAY-202603-001', 5, 3, 2500.00, 3, '2026-03-03 11:30:00', 'WeChat payment', 3),
(6, 'PAY-202603-002', 6, 5, 4200.00, 1, '2026-03-04 13:05:00', 'Bank transfer', 2),
(7, 'PAY-202604-001', 7, 6, 2600.00, 2, '2026-04-03 15:24:00', 'Alipay payment', 3),
(8, 'PAY-202604-002', 8, 7, 3950.00, 3, '2026-04-05 08:40:00', 'WeChat payment', 2),
(9, 'PAY-202605-001', 9, 1, 2500.00, 1, '2026-05-02 10:00:00', 'Bank transfer', 2),
(10, 'PAY-202605-002', 10, 2, 3800.00, 2, '2026-05-03 19:42:00', 'Alipay payment', 3),
(11, 'PAY-202605-003', 11, 4, 320.00, 3, '2026-05-09 12:05:00', 'Utilities paid', 3);

-- Repair orders with JSON image references
INSERT INTO repair_order (id, order_no, room_id, tenant_id, type, priority, description, images, status, assignee_id, resolve_time, resolve_remark) VALUES
(1, 'RO-202606-001', 3, 1, 0, 0, 'Bathroom faucet leaking, water pressure unstable', '["/images/room-standard.svg"]', 2, 3, NULL, NULL),
(2, 'RO-202606-002', 4, NULL, 2, 1, 'Air conditioner inspection before listing', '["/images/room-standard.svg"]', 1, 3, NULL, NULL),
(3, 'RO-202605-003', 10, 5, 3, 1, 'Router signal weak in bedroom', '["/images/room-suite.svg"]', 4, 3, '2026-05-22 17:30:00', 'Router replaced and verified'),
(4, 'RO-202605-004', 15, NULL, 1, 2, 'Wardrobe hinge needs replacement', '["/images/room-suite.svg"]', 0, NULL, NULL, NULL);

-- Operation logs for dashboard/system pages
INSERT INTO operation_log (id, user_id, username, module, operation, method, params, ip, duration, status, create_time) VALUES
(1, 1, 'admin', 'Auth', 'Login', 'POST /api/auth/login', '{}', '127.0.0.1', 142, 1, '2026-06-01 09:00:00'),
(2, 2, 'manager', 'Contract', 'Create contract', 'POST /api/contracts', '{"contractNo":"CT-2026-0004"}', '127.0.0.1', 230, 1, '2026-06-01 10:18:00'),
(3, 3, 'staff', 'Repair', 'Assign order', 'PUT /api/repair-orders/1', '{"assigneeId":3}', '127.0.0.1', 165, 1, '2026-06-02 14:21:00'),
(4, 2, 'manager', 'Bill', 'Check overdue', 'POST /api/bills/check-overdue', '{}', '127.0.0.1', 98, 1, '2026-06-05 08:30:00');

-- Announcements
INSERT INTO announcement (id, title, content, summary, publisher_id, status, top_flag) VALUES
(1, 'Notice on Water Supply Maintenance', 'Dear residents, the water supply system will undergo maintenance on June 10th from 9:00 AM to 3:00 PM. Please store water in advance. We apologize for any inconvenience.', 'Water supply maintenance on June 10th, 9AM-3PM', 1, 1, 1),
(2, 'Summer Electricity Saving Tips', 'With the arrival of summer, electricity consumption increases significantly. Here are some tips: 1. Set AC to 26°C or above. 2. Turn off lights when leaving. 3. Unplug unused appliances. Let us work together to save energy and reduce costs.', 'Energy saving tips for summer season', 1, 1, 0),
(3, 'Community Activity: Weekend BBQ Party', 'We are organizing a community BBQ party this Saturday at the garden area from 5PM to 8PM. All residents are welcome! Please sign up at the front desk by Friday.', 'Weekend BBQ party this Saturday, sign up by Friday', 2, 1, 0);
