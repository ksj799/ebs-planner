-- =====================================================================
-- 데모 샘플 데이터 (전부 가상 데이터, 회사 데이터 아님). 날짜는 실행일 기준 상대값.
-- 이 파일 + item_attributes.sql 이 원본이고, Oracle 포트폴리오 스크립트(db/oracle/02_demo_reset.sql)는
-- db/oracle/generate.sh 로 이 파일들에서 만들어진다. → H2 에서 검증한 데이터가 Oracle 에도 그대로 들어간다.
-- 규칙: 날짜는 "오늘 + n일" 형식(H2 날짜 더하기 함수)만 쓴다 – generate.sh 가 Oracle 의 d0 + n 으로 바꾼다
-- =====================================================================

-- ---------- 조직 ----------
INSERT INTO mtl_parameters VALUES (204, 'M01', 204, 'DEMO-CAL', -1);
INSERT INTO hr_all_organization_units VALUES (204, '데모 공장 (Demo Plant)');
INSERT INTO wip_parameters VALUES (204, 'Standard');

-- 교대: 1교대 08~17시(점심 제외 8h), 2교대 17~01시(8h). 단위 = 자정 기준 초
INSERT INTO bom_shift_times VALUES ('DEMO-CAL', 1, 28800, 43200);
INSERT INTO bom_shift_times VALUES ('DEMO-CAL', 1, 46800, 61200);
INSERT INTO bom_shift_times VALUES ('DEMO-CAL', 2, 61200, 3600);

-- ---------- 부서 / 자원 ----------
INSERT INTO bom_departments VALUES (11, 'ASSY',  '생산4팀',   204, NULL);
INSERT INTO bom_departments VALUES (12, 'MACH',  '생산1팀',   204, NULL);
INSERT INTO bom_departments VALUES (13, 'PACK',  '생산5팀',   204, NULL);
INSERT INTO bom_departments VALUES (14, 'PRESS', '생산2팀', 204, NULL);
INSERT INTO bom_departments VALUES (15, 'WELD',  '생산3팀',   204, NULL);
INSERT INTO bom_resources VALUES (101, 'ASSY-LN', '조립라인',     'HR',  204, NULL, 1);
INSERT INTO bom_resources VALUES (102, 'CNC',     'CNC 가공기',   'HR',  204, NULL, 1);
INSERT INTO bom_resources VALUES (103, 'PACKER',  '포장작업자',   'MIN', 204, NULL, 2);
INSERT INTO bom_resources VALUES (104, 'PRS-200', '200톤 프레스', 'HR',  204, NULL, 1);
INSERT INTO bom_resources VALUES (105, 'WELD-RB', '용접 로봇',    'HR',  204, NULL, 1);
INSERT INTO bom_department_resources VALUES (11, 101, 1, 2);
INSERT INTO bom_department_resources VALUES (12, 102, 2, 2);
INSERT INTO bom_department_resources VALUES (13, 103, 1, 2);
INSERT INTO bom_department_resources VALUES (14, 104, 1, 2);
INSERT INTO bom_department_resources VALUES (15, 105, 1, 2);
INSERT INTO bom_resource_shifts VALUES (11, 101, 1, NULL);
INSERT INTO bom_resource_shifts VALUES (11, 101, 2, NULL);
INSERT INTO bom_resource_shifts VALUES (12, 102, 1, NULL);
INSERT INTO bom_resource_shifts VALUES (13, 103, 1, NULL);
INSERT INTO bom_resource_shifts VALUES (14, 104, 1, NULL);
INSERT INTO bom_resource_shifts VALUES (14, 104, 2, NULL);
INSERT INTO bom_resource_shifts VALUES (15, 105, 1, NULL);

-- ---------- 품목 ----------
INSERT INTO mtl_planners VALUES ('PL01', 204, '김생산 (완제품)');
INSERT INTO mtl_planners VALUES ('PL02', 204, '이자재 (구매부품)');
INSERT INTO fnd_common_lookups VALUES ('ITEM_TYPE', 'FG', 'Finished Good');
INSERT INTO fnd_common_lookups VALUES ('ITEM_TYPE', 'P',  'Purchased Item');

-- (품목ID, 조직, 품번, 설명, 단위, WIP생성, 단위명, 제조1/구매2, 전처리, 처리, 후처리, 최소주문, 주문배수, 유형, 계획자)
INSERT INTO mtl_system_items_b (inventory_item_id, organization_id, segment1, description, primary_uom_code, build_in_wip_flag, primary_unit_of_measure, planning_make_buy_code, preprocessing_lead_time, full_lead_time, postprocessing_lead_time, minimum_order_quantity, fixed_lot_multiplier, item_type, planner_code) VALUES (1001, 204, 'FG-MH100', '모터 하우징', 'EA', 'Y', 'Each', 1, NULL, NULL, NULL, NULL, NULL, 'FG', 'PL01');
INSERT INTO mtl_system_items_b (inventory_item_id, organization_id, segment1, description, primary_uom_code, build_in_wip_flag, primary_unit_of_measure, planning_make_buy_code, preprocessing_lead_time, full_lead_time, postprocessing_lead_time, minimum_order_quantity, fixed_lot_multiplier, item_type, planner_code) VALUES (1002, 204, 'FG-GB200', '기어 박스', 'EA', 'Y', 'Each', 1, NULL, NULL, NULL, NULL, NULL, 'FG', 'PL01');
INSERT INTO mtl_system_items_b (inventory_item_id, organization_id, segment1, description, primary_uom_code, build_in_wip_flag, primary_unit_of_measure, planning_make_buy_code, preprocessing_lead_time, full_lead_time, postprocessing_lead_time, minimum_order_quantity, fixed_lot_multiplier, item_type, planner_code) VALUES (1003, 204, 'FG-SH300', '샤프트', 'EA', 'Y', 'Each', 1, NULL, NULL, NULL, NULL, NULL, 'FG', 'PL01');
INSERT INTO mtl_system_items_b (inventory_item_id, organization_id, segment1, description, primary_uom_code, build_in_wip_flag, primary_unit_of_measure, planning_make_buy_code, preprocessing_lead_time, full_lead_time, postprocessing_lead_time, minimum_order_quantity, fixed_lot_multiplier, item_type, planner_code) VALUES (1004, 204, 'FG-FL400', '플랜지', 'EA', 'Y', 'Each', 1, NULL, NULL, NULL, NULL, NULL, 'FG', 'PL01');
INSERT INTO mtl_system_items_b (inventory_item_id, organization_id, segment1, description, primary_uom_code, build_in_wip_flag, primary_unit_of_measure, planning_make_buy_code, preprocessing_lead_time, full_lead_time, postprocessing_lead_time, minimum_order_quantity, fixed_lot_multiplier, item_type, planner_code) VALUES (1005, 204, 'FG-PL500', '풀리', 'EA', 'Y', 'Each', 1, NULL, NULL, NULL, NULL, NULL, 'FG', 'PL01');
INSERT INTO mtl_system_items_b (inventory_item_id, organization_id, segment1, description, primary_uom_code, build_in_wip_flag, primary_unit_of_measure, planning_make_buy_code, preprocessing_lead_time, full_lead_time, postprocessing_lead_time, minimum_order_quantity, fixed_lot_multiplier, item_type, planner_code) VALUES (1006, 204, 'FG-BR600', '브래킷', 'EA', 'Y', 'Each', 1, NULL, NULL, NULL, NULL, NULL, 'FG', 'PL01');
INSERT INTO mtl_system_items_b (inventory_item_id, organization_id, segment1, description, primary_uom_code, build_in_wip_flag, primary_unit_of_measure, planning_make_buy_code, preprocessing_lead_time, full_lead_time, postprocessing_lead_time, minimum_order_quantity, fixed_lot_multiplier, item_type, planner_code) VALUES (1007, 204, 'FG-CP700', '커플링', 'EA', 'Y', 'Each', 1, NULL, NULL, NULL, NULL, NULL, 'FG', 'PL01');
INSERT INTO mtl_system_items_b (inventory_item_id, organization_id, segment1, description, primary_uom_code, build_in_wip_flag, primary_unit_of_measure, planning_make_buy_code, preprocessing_lead_time, full_lead_time, postprocessing_lead_time, minimum_order_quantity, fixed_lot_multiplier, item_type, planner_code) VALUES (1008, 204, 'FG-HB800', '허브', 'EA', 'Y', 'Each', 1, NULL, NULL, NULL, NULL, NULL, 'FG', 'PL01');
INSERT INTO mtl_system_items_b (inventory_item_id, organization_id, segment1, description, primary_uom_code, build_in_wip_flag, primary_unit_of_measure, planning_make_buy_code, preprocessing_lead_time, full_lead_time, postprocessing_lead_time, minimum_order_quantity, fixed_lot_multiplier, item_type, planner_code) VALUES (1009, 204, 'FG-CV900', '커버 (라우팅 미등록)', 'EA', 'Y', 'Each', 1, NULL, NULL, NULL, NULL, NULL, 'FG', 'PL01');
INSERT INTO mtl_system_items_b (inventory_item_id, organization_id, segment1, description, primary_uom_code, build_in_wip_flag, primary_unit_of_measure, planning_make_buy_code, preprocessing_lead_time, full_lead_time, postprocessing_lead_time, minimum_order_quantity, fixed_lot_multiplier, item_type, planner_code) VALUES (1010, 204, 'FG-SP110', '스페이서', 'EA', 'Y', 'Each', 1, NULL, NULL, NULL, NULL, NULL, 'FG', 'PL01');
INSERT INTO mtl_system_items_b (inventory_item_id, organization_id, segment1, description, primary_uom_code, build_in_wip_flag, primary_unit_of_measure, planning_make_buy_code, preprocessing_lead_time, full_lead_time, postprocessing_lead_time, minimum_order_quantity, fixed_lot_multiplier, item_type, planner_code) VALUES (2001, 204, 'RM-AL01', '알루미늄 주물 하우징', 'EA', 'N', 'Each', 2, 1, 7, 1, 500, 100, 'P', 'PL02');
INSERT INTO mtl_system_items_b (inventory_item_id, organization_id, segment1, description, primary_uom_code, build_in_wip_flag, primary_unit_of_measure, planning_make_buy_code, preprocessing_lead_time, full_lead_time, postprocessing_lead_time, minimum_order_quantity, fixed_lot_multiplier, item_type, planner_code) VALUES (2002, 204, 'RM-BR02', '베어링 6204ZZ', 'EA', 'N', 'Each', 2, 0, 5, 1, 0, 500, 'P', 'PL02');
INSERT INTO mtl_system_items_b (inventory_item_id, organization_id, segment1, description, primary_uom_code, build_in_wip_flag, primary_unit_of_measure, planning_make_buy_code, preprocessing_lead_time, full_lead_time, postprocessing_lead_time, minimum_order_quantity, fixed_lot_multiplier, item_type, planner_code) VALUES (2003, 204, 'RM-GR03', '헬리컬 기어 세트', 'SET', 'N', 'Set', 2, 1, 15, 2, 200, 0, 'P', 'PL02');
INSERT INTO mtl_system_items_b (inventory_item_id, organization_id, segment1, description, primary_uom_code, build_in_wip_flag, primary_unit_of_measure, planning_make_buy_code, preprocessing_lead_time, full_lead_time, postprocessing_lead_time, minimum_order_quantity, fixed_lot_multiplier, item_type, planner_code) VALUES (2004, 204, 'RM-ST04', '환봉 S45C Ø40', 'KG', 'N', 'Kilogram', 2, 0, 3, 1, 1000, 0, 'P', 'PL02');
INSERT INTO mtl_system_items_b (inventory_item_id, organization_id, segment1, description, primary_uom_code, build_in_wip_flag, primary_unit_of_measure, planning_make_buy_code, preprocessing_lead_time, full_lead_time, postprocessing_lead_time, minimum_order_quantity, fixed_lot_multiplier, item_type, planner_code) VALUES (2005, 204, 'RM-BT05', '볼트 키트 M8', 'EA', 'N', 'Each', 2, 0, 2, 0, 0, 1000, 'P', 'PL02');
INSERT INTO mtl_system_items_b (inventory_item_id, organization_id, segment1, description, primary_uom_code, build_in_wip_flag, primary_unit_of_measure, planning_make_buy_code, preprocessing_lead_time, full_lead_time, postprocessing_lead_time, minimum_order_quantity, fixed_lot_multiplier, item_type, planner_code) VALUES (2006, 204, 'RM-PL06', '냉연 강판 1.6t', 'KG', 'N', 'Kilogram', 2, 0, 4, 1, 2000, 500, 'P', 'PL02');
INSERT INTO mtl_system_items_b (inventory_item_id, organization_id, segment1, description, primary_uom_code, build_in_wip_flag, primary_unit_of_measure, planning_make_buy_code, preprocessing_lead_time, full_lead_time, postprocessing_lead_time, minimum_order_quantity, fixed_lot_multiplier, item_type, planner_code) VALUES (2007, 204, 'RM-RB07', '고무 부싱', 'EA', 'N', 'Each', 2, 0, 6, 1, 1000, 500, 'P', 'PL02');
INSERT INTO mtl_system_items_b (inventory_item_id, organization_id, segment1, description, primary_uom_code, build_in_wip_flag, primary_unit_of_measure, planning_make_buy_code, preprocessing_lead_time, full_lead_time, postprocessing_lead_time, minimum_order_quantity, fixed_lot_multiplier, item_type, planner_code) VALUES (2008, 204, 'RM-PT08', '분체 도료', 'KG', 'N', 'Kilogram', 2, 1, 5, 0, 100, 50, 'P', 'PL02');
INSERT INTO mtl_system_items_b (inventory_item_id, organization_id, segment1, description, primary_uom_code, build_in_wip_flag, primary_unit_of_measure, planning_make_buy_code, preprocessing_lead_time, full_lead_time, postprocessing_lead_time, minimum_order_quantity, fixed_lot_multiplier, item_type, planner_code) VALUES (2009, 204, 'RM-KY09', '키 스톡 6x6', 'EA', 'N', 'Each', 2, 0, 3, 0, 0, 1000, 'P', 'PL02');
INSERT INTO mtl_system_items_b (inventory_item_id, organization_id, segment1, description, primary_uom_code, build_in_wip_flag, primary_unit_of_measure, planning_make_buy_code, preprocessing_lead_time, full_lead_time, postprocessing_lead_time, minimum_order_quantity, fixed_lot_multiplier, item_type, planner_code) VALUES (2010, 204, 'RM-CS10', '주강 블랭크', 'EA', 'N', 'Each', 2, 1, 12, 2, 300, 100, 'P', 'PL02');

-- ---------- 라우팅: 공정순서 = 품목ID*100+공정번호, 자원 basis 1=개당(h), 2=로트당 셋업(h) ----------
INSERT INTO bom_operational_routings VALUES (501, 501, 1001, 204, NULL);
INSERT INTO bom_operational_routings VALUES (502, 502, 1002, 204, NULL);
INSERT INTO bom_operational_routings VALUES (503, 503, 1003, 204, NULL);
INSERT INTO bom_operational_routings VALUES (504, 504, 1004, 204, NULL);
INSERT INTO bom_operational_routings VALUES (505, 505, 1005, 204, NULL);
INSERT INTO bom_operational_routings VALUES (506, 506, 1006, 204, NULL);
INSERT INTO bom_operational_routings VALUES (507, 507, 1007, 204, NULL);
INSERT INTO bom_operational_routings VALUES (508, 508, 1008, 204, NULL);
INSERT INTO bom_operational_routings VALUES (510, 510, 1010, 204, NULL);
-- 모터 하우징: 가공 → 조립 → 포장
INSERT INTO bom_operation_sequences VALUES (100110, 501, 10, 12, DATE '2020-01-01', NULL);
INSERT INTO bom_operation_sequences VALUES (100120, 501, 20, 11, DATE '2020-01-01', NULL);
INSERT INTO bom_operation_sequences VALUES (100130, 501, 30, 13, DATE '2020-01-01', NULL);
INSERT INTO bom_operation_resources VALUES (100110, 10, 102, 0.010, 1);
INSERT INTO bom_operation_resources VALUES (100120, 10, 101, 0.0167, 1);
INSERT INTO bom_operation_resources VALUES (100120, 20, 101, 0.5, 2);
INSERT INTO bom_operation_resources VALUES (100130, 10, 103, 0.3, 1);
-- 기어 박스: 가공 → 조립
INSERT INTO bom_operation_sequences VALUES (100210, 502, 10, 12, DATE '2020-01-01', NULL);
INSERT INTO bom_operation_sequences VALUES (100220, 502, 20, 11, DATE '2020-01-01', NULL);
INSERT INTO bom_operation_resources VALUES (100210, 10, 102, 0.012, 1);
INSERT INTO bom_operation_resources VALUES (100220, 10, 101, 0.025, 1);
INSERT INTO bom_operation_resources VALUES (100220, 20, 101, 0.75, 2);
-- 샤프트: 가공
INSERT INTO bom_operation_sequences VALUES (100310, 503, 10, 12, DATE '2020-01-01', NULL);
INSERT INTO bom_operation_resources VALUES (100310, 10, 102, 0.0125, 1);
INSERT INTO bom_operation_resources VALUES (100310, 20, 102, 0.33, 2);
-- 플랜지: 가공 → 포장
INSERT INTO bom_operation_sequences VALUES (100410, 504, 10, 12, DATE '2020-01-01', NULL);
INSERT INTO bom_operation_sequences VALUES (100420, 504, 20, 13, DATE '2020-01-01', NULL);
INSERT INTO bom_operation_resources VALUES (100410, 10, 102, 0.02, 1);
INSERT INTO bom_operation_resources VALUES (100410, 20, 102, 0.5, 2);
INSERT INTO bom_operation_resources VALUES (100420, 10, 103, 0.2, 1);
-- 풀리: 가공 → 프레스
INSERT INTO bom_operation_sequences VALUES (100510, 505, 10, 12, DATE '2020-01-01', NULL);
INSERT INTO bom_operation_sequences VALUES (100520, 505, 20, 14, DATE '2020-01-01', NULL);
INSERT INTO bom_operation_resources VALUES (100510, 10, 102, 0.008, 1);
INSERT INTO bom_operation_resources VALUES (100520, 10, 104, 0.015, 1);
INSERT INTO bom_operation_resources VALUES (100520, 20, 104, 0.4, 2);
-- 브래킷: 프레스 → 용접
INSERT INTO bom_operation_sequences VALUES (100610, 506, 10, 14, DATE '2020-01-01', NULL);
INSERT INTO bom_operation_sequences VALUES (100620, 506, 20, 15, DATE '2020-01-01', NULL);
INSERT INTO bom_operation_resources VALUES (100610, 10, 104, 0.006, 1);
INSERT INTO bom_operation_resources VALUES (100610, 20, 104, 0.6, 2);
INSERT INTO bom_operation_resources VALUES (100620, 10, 105, 0.02, 1);
INSERT INTO bom_operation_resources VALUES (100620, 20, 105, 0.3, 2);
-- 커플링: 가공 → 조립
INSERT INTO bom_operation_sequences VALUES (100710, 507, 10, 12, DATE '2020-01-01', NULL);
INSERT INTO bom_operation_sequences VALUES (100720, 507, 20, 11, DATE '2020-01-01', NULL);
INSERT INTO bom_operation_resources VALUES (100710, 10, 102, 0.009, 1);
INSERT INTO bom_operation_resources VALUES (100720, 10, 101, 0.012, 1);
INSERT INTO bom_operation_resources VALUES (100720, 20, 101, 0.25, 2);
-- 허브: 가공 → 용접
INSERT INTO bom_operation_sequences VALUES (100810, 508, 10, 12, DATE '2020-01-01', NULL);
INSERT INTO bom_operation_sequences VALUES (100820, 508, 20, 15, DATE '2020-01-01', NULL);
INSERT INTO bom_operation_resources VALUES (100810, 10, 102, 0.015, 1);
INSERT INTO bom_operation_resources VALUES (100810, 20, 102, 0.4, 2);
INSERT INTO bom_operation_resources VALUES (100820, 10, 105, 0.01, 1);
-- 스페이서: 프레스
INSERT INTO bom_operation_sequences VALUES (101010, 510, 10, 14, DATE '2020-01-01', NULL);
INSERT INTO bom_operation_resources VALUES (101010, 10, 104, 0.004, 1);
INSERT INTO bom_operation_resources VALUES (101010, 20, 104, 0.3, 2);
-- (FG-CV900 커버는 라우팅 미등록 사례)

-- ---------- BOM 1단계: 구성품순서 = 상위ID*100 + 구성품ID-2000 ----------
INSERT INTO bom_structures_b VALUES (601, 601, 1001, 204, NULL);
INSERT INTO bom_structures_b VALUES (602, 602, 1002, 204, NULL);
INSERT INTO bom_structures_b VALUES (603, 603, 1003, 204, NULL);
INSERT INTO bom_structures_b VALUES (604, 604, 1004, 204, NULL);
INSERT INTO bom_structures_b VALUES (605, 605, 1005, 204, NULL);
INSERT INTO bom_structures_b VALUES (606, 606, 1006, 204, NULL);
INSERT INTO bom_structures_b VALUES (607, 607, 1007, 204, NULL);
INSERT INTO bom_structures_b VALUES (608, 608, 1008, 204, NULL);
INSERT INTO bom_structures_b VALUES (609, 609, 1009, 204, NULL);
INSERT INTO bom_structures_b VALUES (610, 610, 1010, 204, NULL);
-- (구성품순서, BOM, 구성품, 단위당 수량, 수율, 유효시작, 유효종료, 구현일, WIP 공급유형)
INSERT INTO bom_components_b VALUES (100101, 601, 2001, 1,    1,    DATE '2020-01-01', NULL, DATE '2020-01-01', 1);
INSERT INTO bom_components_b VALUES (100102, 601, 2002, 2,    1,    DATE '2020-01-01', NULL, DATE '2020-01-01', 1);
INSERT INTO bom_components_b VALUES (100105, 601, 2005, 1,    1,    DATE '2020-01-01', NULL, DATE '2020-01-01', 4);
INSERT INTO bom_components_b VALUES (100202, 602, 2002, 2,    1,    DATE '2020-01-01', NULL, DATE '2020-01-01', 1);
INSERT INTO bom_components_b VALUES (100203, 602, 2003, 1,    1,    DATE '2020-01-01', NULL, DATE '2020-01-01', 1);
INSERT INTO bom_components_b VALUES (100205, 602, 2005, 1,    1,    DATE '2020-01-01', NULL, DATE '2020-01-01', 4);
INSERT INTO bom_components_b VALUES (100304, 603, 2004, 0.8,  0.95, DATE '2020-01-01', NULL, DATE '2020-01-01', 2);
INSERT INTO bom_components_b VALUES (100309, 603, 2009, 1,    1,    DATE '2020-01-01', NULL, DATE '2020-01-01', 4);
INSERT INTO bom_components_b VALUES (100404, 604, 2004, 1.2,  0.95, DATE '2020-01-01', NULL, DATE '2020-01-01', 2);
INSERT INTO bom_components_b VALUES (100405, 604, 2005, 4,    1,    DATE '2020-01-01', NULL, DATE '2020-01-01', 4);
INSERT INTO bom_components_b VALUES (100510, 605, 2010, 1,    1,    DATE '2020-01-01', NULL, DATE '2020-01-01', 1);
INSERT INTO bom_components_b VALUES (100509, 605, 2009, 1,    1,    DATE '2020-01-01', NULL, DATE '2020-01-01', 4);
INSERT INTO bom_components_b VALUES (100606, 606, 2006, 0.6,  1,    DATE '2020-01-01', NULL, DATE '2020-01-01', 2);
INSERT INTO bom_components_b VALUES (100608, 606, 2008, 0.05, 1,    DATE '2020-01-01', NULL, DATE '2020-01-01', 2);
INSERT INTO bom_components_b VALUES (100605, 606, 2005, 2,    1,    DATE '2020-01-01', NULL, DATE '2020-01-01', 4);
INSERT INTO bom_components_b VALUES (100710, 607, 2010, 1,    1,    DATE '2020-01-01', NULL, DATE '2020-01-01', 1);
INSERT INTO bom_components_b VALUES (100707, 607, 2007, 2,    1,    DATE '2020-01-01', NULL, DATE '2020-01-01', 2);
INSERT INTO bom_components_b VALUES (100804, 608, 2004, 1.5,  0.95, DATE '2020-01-01', NULL, DATE '2020-01-01', 2);
INSERT INTO bom_components_b VALUES (100802, 608, 2002, 1,    1,    DATE '2020-01-01', NULL, DATE '2020-01-01', 1);
INSERT INTO bom_components_b VALUES (100906, 609, 2006, 0.4,  1,    DATE '2020-01-01', NULL, DATE '2020-01-01', 2);
INSERT INTO bom_components_b VALUES (100908, 609, 2008, 0.03, 1,    DATE '2020-01-01', NULL, DATE '2020-01-01', 2);
INSERT INTO bom_components_b VALUES (101004, 610, 2004, 0.2,  0.95, DATE '2020-01-01', NULL, DATE '2020-01-01', 2);

-- ---------- 보관장소 / 로케이터 / 현재고 ----------
INSERT INTO mtl_secondary_inventories VALUES ('FG',  204, 1, '완제품 창고');
INSERT INTO mtl_secondary_inventories VALUES ('RM',  204, 1, '원자재 창고');
INSERT INTO mtl_secondary_inventories VALUES ('MRB', 204, 2, '부적합품 보류');
INSERT INTO mtl_item_locations_kfv VALUES (1, 204, 'FG.01.01');
INSERT INTO mtl_item_locations_kfv VALUES (2, 204, 'RM.A.01');
INSERT INTO mtl_item_locations_kfv VALUES (3, 204, 'RM.A.02');
INSERT INTO mtl_item_locations_kfv VALUES (4, 204, 'RM.B.01');
-- (조직, 품목, 보관장소, 수량, 로케이터, 로트, 리비전)
INSERT INTO mtl_onhand_quantities_detail VALUES (204, 1001, 'FG',  150,   1,    NULL,        NULL);
INSERT INTO mtl_onhand_quantities_detail VALUES (204, 1004, 'FG',  400,   1,    NULL,        NULL);
INSERT INTO mtl_onhand_quantities_detail VALUES (204, 1004, 'MRB', 100,   NULL, NULL,        NULL);
INSERT INTO mtl_onhand_quantities_detail VALUES (204, 1006, 'FG',  250,   1,    NULL,        NULL);
INSERT INTO mtl_onhand_quantities_detail VALUES (204, 1010, 'FG',  600,   1,    NULL,        NULL);
INSERT INTO mtl_onhand_quantities_detail VALUES (204, 2001, 'RM',  800,   2,    'AL2609-01', NULL);
INSERT INTO mtl_onhand_quantities_detail VALUES (204, 2001, 'RM',  300,   3,    'AL2609-02', NULL);
INSERT INTO mtl_onhand_quantities_detail VALUES (204, 2002, 'RM',  8000,  2,    'BR-0901',   NULL);
INSERT INTO mtl_onhand_quantities_detail VALUES (204, 2002, 'MRB', 200,   NULL, 'BR-0815',   NULL);
INSERT INTO mtl_onhand_quantities_detail VALUES (204, 2003, 'RM',  900,   4,    'GR-0922',   'A');
INSERT INTO mtl_onhand_quantities_detail VALUES (204, 2004, 'RM',  6000,  4,    'ST-A1',     NULL);
INSERT INTO mtl_onhand_quantities_detail VALUES (204, 2004, 'RM',  2000,  4,    'ST-A2',     NULL);
INSERT INTO mtl_onhand_quantities_detail VALUES (204, 2004, 'RM',  6000,  4,    'ST-A3',     NULL);
INSERT INTO mtl_onhand_quantities_detail VALUES (204, 2005, 'RM',  30000, 3,    NULL,        NULL);
INSERT INTO mtl_onhand_quantities_detail VALUES (204, 2006, 'RM',  4500,  4,    'PL-1001',   NULL);
INSERT INTO mtl_onhand_quantities_detail VALUES (204, 2007, 'RM',  2500,  3,    'RB-0930',   NULL);
INSERT INTO mtl_onhand_quantities_detail VALUES (204, 2008, 'RM',  300,   3,    'PT-0925',   NULL);
INSERT INTO mtl_onhand_quantities_detail VALUES (204, 2009, 'RM',  12000, 3,    NULL,        NULL);
INSERT INTO mtl_onhand_quantities_detail VALUES (204, 2010, 'RM',  1500,  2,    'CS-0918',   'B');

-- ---------- 고객 / 수주 ----------
INSERT INTO hz_parties VALUES (1, '한빛전자');
INSERT INTO hz_parties VALUES (2, '대성기계');
INSERT INTO hz_parties VALUES (3, '세진산업');
INSERT INTO hz_parties VALUES (4, '태양모터스');
INSERT INTO hz_parties VALUES (5, '그린로보틱스');
INSERT INTO hz_parties VALUES (6, '미래정밀');
INSERT INTO hz_cust_accounts VALUES (11, 1);
INSERT INTO hz_cust_accounts VALUES (12, 2);
INSERT INTO hz_cust_accounts VALUES (13, 3);
INSERT INTO hz_cust_accounts VALUES (14, 4);
INSERT INTO hz_cust_accounts VALUES (15, 5);
INSERT INTO hz_cust_accounts VALUES (16, 6);
INSERT INTO oe_order_headers_all VALUES (7001, 10501, 11);
INSERT INTO oe_order_headers_all VALUES (7002, 10502, 12);
INSERT INTO oe_order_headers_all VALUES (7003, 10503, 13);
INSERT INTO oe_order_headers_all VALUES (7004, 10504, 14);
INSERT INTO oe_order_headers_all VALUES (7005, 10505, 15);
INSERT INTO oe_order_headers_all VALUES (7006, 10506, 16);
INSERT INTO oe_order_headers_all VALUES (7007, 10507, 11);
INSERT INTO oe_order_headers_all VALUES (7008, 10508, 12);
INSERT INTO oe_order_headers_all VALUES (7009, 10509, 13);
INSERT INTO oe_order_headers_all VALUES (7010, 10510, 14);
INSERT INTO oe_order_headers_all VALUES (7011, 10511, 15);
INSERT INTO oe_order_headers_all VALUES (7012, 10512, 16);
INSERT INTO oe_order_headers_all VALUES (7013, 10513, 11);
INSERT INTO oe_order_headers_all VALUES (7014, 10514, 12);
INSERT INTO oe_order_headers_all VALUES (7015, 10515, 13);
INSERT INTO oe_order_headers_all VALUES (7016, 10516, 14);
INSERT INTO oe_order_headers_all VALUES (7017, 10517, 15);
INSERT INTO oe_order_headers_all VALUES (7018, 10518, 16);
-- (라인ID, 헤더, 라인, 출하분할, 품목, 조직, 주문량, 출하량, 단위, 출하예정일, 요청일, 미결, 예약, 유형)
INSERT INTO oe_order_lines_all VALUES (8001, 7001, 1, 1, 1001, 204, 1200, 0,   'EA', DATEADD('DAY', 4, CURRENT_DATE),  DATEADD('DAY', 4, CURRENT_DATE),  'Y', 'Y', 'ORDER');
INSERT INTO oe_order_lines_all VALUES (8002, 7002, 1, 1, 1003, 204, 1500, 0,   'EA', DATEADD('DAY', 6, CURRENT_DATE),  DATEADD('DAY', 6, CURRENT_DATE),  'Y', 'Y', 'ORDER');
INSERT INTO oe_order_lines_all VALUES (8003, 7002, 2, 1, 1008, 204, 600,  0,   'EA', DATEADD('DAY', 9, CURRENT_DATE),  DATEADD('DAY', 9, CURRENT_DATE),  'Y', 'Y', 'ORDER');
INSERT INTO oe_order_lines_all VALUES (8004, 7003, 1, 1, 1002, 204, 2500, 0,   'EA', DATEADD('DAY', 12, CURRENT_DATE), DATEADD('DAY', 12, CURRENT_DATE), 'Y', 'Y', 'ORDER');
INSERT INTO oe_order_lines_all VALUES (8005, 7004, 1, 1, 1004, 204, 2200, 200, 'EA', DATEADD('DAY', 9, CURRENT_DATE),  DATEADD('DAY', 9, CURRENT_DATE),  'Y', 'Y', 'ORDER');
INSERT INTO oe_order_lines_all VALUES (8006, 7004, 2, 1, 1009, 204, 300,  0,   'EA', DATEADD('DAY', 7, CURRENT_DATE),  DATEADD('DAY', 7, CURRENT_DATE),  'Y', 'Y', 'ORDER');
INSERT INTO oe_order_lines_all VALUES (8007, 7005, 1, 1, 1001, 204, 1800, 0,   'EA', DATEADD('DAY', 15, CURRENT_DATE), DATEADD('DAY', 15, CURRENT_DATE), 'Y', 'Y', 'ORDER');
INSERT INTO oe_order_lines_all VALUES (8008, 7006, 1, 1, 1003, 204, 9000, 0,   'EA', DATEADD('DAY', 8, CURRENT_DATE),  DATEADD('DAY', 8, CURRENT_DATE),  'Y', 'Y', 'ORDER');
INSERT INTO oe_order_lines_all VALUES (8009, 7006, 2, 1, 1005, 204, 800,  0,   'EA', DATEADD('DAY', 11, CURRENT_DATE), DATEADD('DAY', 11, CURRENT_DATE), 'Y', 'Y', 'ORDER');
INSERT INTO oe_order_lines_all VALUES (8010, 7007, 1, 1, 1006, 204, 1500, 0,   'EA', DATEADD('DAY', 5, CURRENT_DATE),  DATEADD('DAY', 5, CURRENT_DATE),  'Y', 'Y', 'ORDER');
INSERT INTO oe_order_lines_all VALUES (8011, 7008, 1, 1, 1007, 204, 700,  0,   'EA', DATEADD('DAY', 13, CURRENT_DATE), DATEADD('DAY', 13, CURRENT_DATE), 'Y', 'Y', 'ORDER');
INSERT INTO oe_order_lines_all VALUES (8012, 7008, 2, 1, 1010, 204, 2000, 0,   'EA', DATEADD('DAY', 3, CURRENT_DATE),  DATEADD('DAY', 3, CURRENT_DATE),  'Y', 'Y', 'ORDER');
INSERT INTO oe_order_lines_all VALUES (8013, 7009, 1, 1, 1002, 204, 500,  0,   'EA', DATEADD('DAY', 20, CURRENT_DATE), DATEADD('DAY', 20, CURRENT_DATE), 'Y', 'N', 'ORDER');
INSERT INTO oe_order_lines_all VALUES (8014, 7010, 1, 1, 1008, 204, 900,  0,   'EA', DATEADD('DAY', 18, CURRENT_DATE), DATEADD('DAY', 18, CURRENT_DATE), 'Y', 'Y', 'ORDER');
INSERT INTO oe_order_lines_all VALUES (8015, 7010, 2, 1, 1006, 204, 1200, 400, 'EA', DATEADD('DAY', 16, CURRENT_DATE), DATEADD('DAY', 16, CURRENT_DATE), 'Y', 'Y', 'ORDER');
INSERT INTO oe_order_lines_all VALUES (8016, 7011, 1, 1, 1005, 204, 1500, 0,   'EA', DATEADD('DAY', 22, CURRENT_DATE), DATEADD('DAY', 22, CURRENT_DATE), 'Y', 'Y', 'ORDER');
INSERT INTO oe_order_lines_all VALUES (8017, 7012, 1, 1, 1004, 204, 1000, 0,   'EA', DATEADD('DAY', 25, CURRENT_DATE), DATEADD('DAY', 25, CURRENT_DATE), 'Y', 'Y', 'ORDER');
INSERT INTO oe_order_lines_all VALUES (8018, 7012, 2, 1, 1001, 204, 600,  0,   'EA', DATEADD('DAY', 27, CURRENT_DATE), DATEADD('DAY', 27, CURRENT_DATE), 'Y', 'Y', 'ORDER');
INSERT INTO oe_order_lines_all VALUES (8019, 7013, 1, 1, 1007, 204, 1300, 0,   'EA', DATEADD('DAY', 19, CURRENT_DATE), DATEADD('DAY', 19, CURRENT_DATE), 'Y', 'Y', 'ORDER');
INSERT INTO oe_order_lines_all VALUES (8020, 7014, 1, 1, 1003, 204, 2400, 0,   'EA', DATEADD('DAY', 24, CURRENT_DATE), DATEADD('DAY', 24, CURRENT_DATE), 'Y', 'Y', 'ORDER');
INSERT INTO oe_order_lines_all VALUES (8021, 7014, 2, 1, 1010, 204, 1500, 0,   'EA', DATEADD('DAY', 14, CURRENT_DATE), DATEADD('DAY', 14, CURRENT_DATE), 'Y', 'Y', 'ORDER');
INSERT INTO oe_order_lines_all VALUES (8022, 7015, 1, 1, 1009, 204, 400,  0,   'EA', DATEADD('DAY', 30, CURRENT_DATE), DATEADD('DAY', 30, CURRENT_DATE), 'Y', 'Y', 'ORDER');
INSERT INTO oe_order_lines_all VALUES (8023, 7016, 1, 1, 1002, 204, 1200, 0,   'EA', DATEADD('DAY', 33, CURRENT_DATE), DATEADD('DAY', 33, CURRENT_DATE), 'Y', 'Y', 'ORDER');
INSERT INTO oe_order_lines_all VALUES (8024, 7016, 2, 1, 1005, 204, 700,  0,   'EA', DATEADD('DAY', 29, CURRENT_DATE), DATEADD('DAY', 29, CURRENT_DATE), 'Y', 'Y', 'ORDER');
INSERT INTO oe_order_lines_all VALUES (8025, 7017, 1, 1, 1006, 204, 900,  0,   'EA', DATEADD('DAY', 35, CURRENT_DATE), DATEADD('DAY', 35, CURRENT_DATE), 'Y', 'Y', 'ORDER');
INSERT INTO oe_order_lines_all VALUES (8026, 7018, 1, 1, 1008, 204, 1100, 0,   'EA', DATEADD('DAY', 31, CURRENT_DATE), DATEADD('DAY', 31, CURRENT_DATE), 'Y', 'Y', 'ORDER');
INSERT INTO oe_order_lines_all VALUES (8027, 7018, 2, 1, 1004, 204, 800,  0,   'EA', DATEADD('DAY', 38, CURRENT_DATE), DATEADD('DAY', 38, CURRENT_DATE), 'Y', 'Y', 'ORDER');

-- ---------- 기존 작업지시 (3=해제, 1=미해제, 6=보류) ----------
INSERT INTO wip_entities VALUES (20001, 'J-20001', 204, 1, 1001);
INSERT INTO wip_entities VALUES (20002, 'J-20002', 204, 1, 1003);
INSERT INTO wip_entities VALUES (20003, 'J-20003', 204, 1, 1002);
INSERT INTO wip_entities VALUES (20004, 'J-20004', 204, 1, 1005);
INSERT INTO wip_entities VALUES (20005, 'J-20005', 204, 1, 1008);
INSERT INTO wip_entities VALUES (20006, 'J-20006', 204, 1, 1006);
INSERT INTO wip_entities VALUES (20007, 'J-20007', 204, 1, 1010);
INSERT INTO wip_entities VALUES (20008, 'J-20008', 204, 1, 1007);
-- (ID, 조직, 품목, 상태, 시작수량, 완료, 폐기, 시작일, 완료예정일, 출처, 출처라인, 클래스)
INSERT INTO wip_discrete_jobs VALUES (20001, 204, 1001, 3, 800,  300, 0, DATEADD('DAY', -2, CURRENT_DATE), DATEADD('DAY', 2, CURRENT_DATE), NULL, NULL, 'Standard');
INSERT INTO wip_discrete_jobs VALUES (20002, 204, 1003, 1, 600,  0,   0, DATEADD('DAY', 1, CURRENT_DATE),  DATEADD('DAY', 3, CURRENT_DATE), NULL, NULL, 'Standard');
INSERT INTO wip_discrete_jobs VALUES (20003, 204, 1002, 3, 1000, 400, 0, DATEADD('DAY', -3, CURRENT_DATE), DATEADD('DAY', 2, CURRENT_DATE), NULL, NULL, 'Standard');
INSERT INTO wip_discrete_jobs VALUES (20004, 204, 1005, 3, 500,  100, 0, DATEADD('DAY', -1, CURRENT_DATE), DATEADD('DAY', 3, CURRENT_DATE), NULL, NULL, 'Standard');
INSERT INTO wip_discrete_jobs VALUES (20005, 204, 1008, 3, 700,  0,   0, DATEADD('DAY', 0, CURRENT_DATE),  DATEADD('DAY', 4, CURRENT_DATE), NULL, NULL, 'Standard');
INSERT INTO wip_discrete_jobs VALUES (20006, 204, 1006, 1, 400,  0,   0, DATEADD('DAY', 2, CURRENT_DATE),  DATEADD('DAY', 5, CURRENT_DATE), NULL, NULL, 'Standard');
INSERT INTO wip_discrete_jobs VALUES (20007, 204, 1010, 1, 1500, 0,   0, DATEADD('DAY', 1, CURRENT_DATE),  DATEADD('DAY', 2, CURRENT_DATE), NULL, NULL, 'Standard');
INSERT INTO wip_discrete_jobs VALUES (20008, 204, 1007, 6, 300,  0,   0, DATEADD('DAY', -4, CURRENT_DATE), DATEADD('DAY', 1, CURRENT_DATE), NULL, NULL, 'Standard');
-- 작업지시 자재 소요 = BOM × 시작수량, 해제된 작업지시는 40% 출고
INSERT INTO wip_requirement_operations
SELECT wdj.wip_entity_id, wdj.organization_id, bcb.component_item_id, wdj.scheduled_start_date,
       ROUND(wdj.start_quantity * bcb.component_quantity / bcb.component_yield_factor, 2),
       CASE WHEN wdj.status_type = 3 THEN ROUND(wdj.start_quantity * bcb.component_quantity / bcb.component_yield_factor * 0.4, 2) ELSE 0 END,
       bcb.wip_supply_type
  FROM wip_discrete_jobs wdj
  JOIN bom_structures_b bsb ON bsb.assembly_item_id = wdj.primary_item_id AND bsb.organization_id = wdj.organization_id
  JOIN bom_components_b bcb ON bcb.bill_sequence_id = bsb.common_bill_sequence_id;

-- ---------- 공급사 / 미입고 PO ----------
INSERT INTO ap_suppliers VALUES (1, '대한금속');
INSERT INTO ap_suppliers VALUES (2, '한일베어링');
INSERT INTO ap_suppliers VALUES (3, '정밀기어산업');
INSERT INTO ap_suppliers VALUES (4, '동방고무');
INSERT INTO ap_suppliers VALUES (5, '코리아코팅');
INSERT INTO po_headers_all VALUES (9001, '4500001', 1, 'APPROVED');
INSERT INTO po_headers_all VALUES (9002, '4500002', 2, 'APPROVED');
INSERT INTO po_headers_all VALUES (9003, '4500003', 3, 'APPROVED');
INSERT INTO po_headers_all VALUES (9004, '4500004', 1, 'IN PROCESS');
INSERT INTO po_headers_all VALUES (9005, '4500005', 1, 'APPROVED');
INSERT INTO po_headers_all VALUES (9006, '4500006', 1, 'APPROVED');
INSERT INTO po_headers_all VALUES (9007, '4500007', 4, 'APPROVED');
INSERT INTO po_headers_all VALUES (9008, '4500008', 5, 'APPROVED');
INSERT INTO po_headers_all VALUES (9009, '4500009', 1, 'APPROVED');
INSERT INTO po_headers_all VALUES (9010, '4500010', 1, 'REQUIRES REAPPROVAL');
INSERT INTO po_headers_all VALUES (9011, '4500011', 3, 'APPROVED');
-- (라인ID, 헤더, 라인번호, 품목, 단위)
INSERT INTO po_lines_all VALUES (9101, 9001, 1, 2001, 'Each');
INSERT INTO po_lines_all VALUES (9102, 9001, 2, 2010, 'Each');
INSERT INTO po_lines_all VALUES (9103, 9002, 1, 2002, 'Each');
INSERT INTO po_lines_all VALUES (9104, 9003, 1, 2003, 'Set');
INSERT INTO po_lines_all VALUES (9105, 9004, 1, 2004, 'Kilogram');
INSERT INTO po_lines_all VALUES (9106, 9005, 1, 2005, 'Each');
INSERT INTO po_lines_all VALUES (9107, 9006, 1, 2006, 'Kilogram');
INSERT INTO po_lines_all VALUES (9108, 9007, 1, 2007, 'Each');
INSERT INTO po_lines_all VALUES (9109, 9008, 1, 2008, 'Kilogram');
INSERT INTO po_lines_all VALUES (9110, 9009, 1, 2009, 'Each');
INSERT INTO po_lines_all VALUES (9111, 9010, 1, 2001, 'Each');
INSERT INTO po_lines_all VALUES (9112, 9011, 1, 2003, 'Set');
-- (납품일정ID, 헤더, 라인, 분할번호, 입고조직, 유형, 수량, 입고, 취소, 필요일, 약속일, 취소여부, 마감상태)
INSERT INTO po_line_locations_all VALUES (9201, 9001, 9101, 1, 204, 'STANDARD', 2000,  0,    0, DATEADD('DAY', 3, CURRENT_DATE),  DATEADD('DAY', 5, CURRENT_DATE),  'N', 'OPEN');
INSERT INTO po_line_locations_all VALUES (9202, 9001, 9102, 1, 204, 'STANDARD', 600,   0,    0, DATEADD('DAY', 9, CURRENT_DATE),  DATEADD('DAY', 12, CURRENT_DATE), 'N', 'OPEN');
INSERT INTO po_line_locations_all VALUES (9203, 9001, 9102, 2, 204, 'STANDARD', 2400,  0,    0, DATEADD('DAY', 22, CURRENT_DATE), NULL,                             'N', 'OPEN');
INSERT INTO po_line_locations_all VALUES (9204, 9002, 9103, 1, 204, 'STANDARD', 5000,  0,    0, DATEADD('DAY', 2, CURRENT_DATE),  NULL,                             'N', 'OPEN');
INSERT INTO po_line_locations_all VALUES (9205, 9002, 9103, 2, 204, 'STANDARD', 3000,  0,    0, DATEADD('DAY', 12, CURRENT_DATE), NULL,                             'N', 'OPEN');
INSERT INTO po_line_locations_all VALUES (9206, 9003, 9104, 1, 204, 'STANDARD', 1000,  0,    0, DATEADD('DAY', -3, CURRENT_DATE), DATEADD('DAY', -2, CURRENT_DATE), 'N', 'OPEN');
INSERT INTO po_line_locations_all VALUES (9207, 9004, 9105, 1, 204, 'STANDARD', 8000,  2000, 0, DATEADD('DAY', 4, CURRENT_DATE),  DATEADD('DAY', 6, CURRENT_DATE),  'N', 'OPEN');
INSERT INTO po_line_locations_all VALUES (9208, 9005, 9106, 1, 204, 'STANDARD', 10000, 0,    0, DATEADD('DAY', 6, CURRENT_DATE),  NULL,                             'N', 'OPEN');
INSERT INTO po_line_locations_all VALUES (9209, 9006, 9107, 1, 204, 'STANDARD', 6000,  0,    0, DATEADD('DAY', 5, CURRENT_DATE),  DATEADD('DAY', 7, CURRENT_DATE),  'N', 'OPEN');
INSERT INTO po_line_locations_all VALUES (9210, 9006, 9107, 2, 204, 'STANDARD', 4000,  0,    0, DATEADD('DAY', 18, CURRENT_DATE), NULL,                             'N', 'OPEN');
INSERT INTO po_line_locations_all VALUES (9211, 9007, 9108, 1, 204, 'STANDARD', 2000,  0,    0, DATEADD('DAY', 8, CURRENT_DATE),  NULL,                             'N', 'OPEN');
INSERT INTO po_line_locations_all VALUES (9212, 9008, 9109, 1, 204, 'STANDARD', 400,   0,    0, DATEADD('DAY', 4, CURRENT_DATE),  DATEADD('DAY', 4, CURRENT_DATE),  'N', 'OPEN');
INSERT INTO po_line_locations_all VALUES (9213, 9009, 9110, 1, 204, 'STANDARD', 5000,  0,    0, DATEADD('DAY', 10, CURRENT_DATE), NULL,                             'N', 'OPEN');
INSERT INTO po_line_locations_all VALUES (9214, 9010, 9111, 1, 204, 'STANDARD', 1500,  0,    0, DATEADD('DAY', 16, CURRENT_DATE), NULL,                             'N', 'OPEN');
INSERT INTO po_line_locations_all VALUES (9215, 9011, 9112, 1, 204, 'STANDARD', 800,   0,    0, DATEADD('DAY', 20, CURRENT_DATE), DATEADD('DAY', 24, CURRENT_DATE), 'N', 'OPEN');
