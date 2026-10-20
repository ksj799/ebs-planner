-- =====================================================================
-- 품목 계획 속성 (Oracle 품목 화면의 일반 계획 / MPS·MRP 계획 / 리드타임 / 재공 탭)
-- H2 데모와 Oracle 포트폴리오(db/oracle/02_demo_reset.sql 의 demo_reset 안)에 같은 내용으로 들어간다.
-- 완제품 1001~1010, 구매부품 2001~2010 (모두 가상 데이터)
-- =====================================================================

-- ---------- 완제품 공통 ----------
UPDATE mtl_system_items_b
   SET inventory_planning_code = 6, mrp_safety_stock_code = 2, safety_stock_bucket_days = 10, mrp_safety_stock_percent = 5,
       minimum_order_quantity = 100, maximum_order_quantity = 5000, order_cost = 120000, carrying_cost = 10,
       fixed_days_supply = 5, fixed_lot_multiplier = 50,
       mrp_planning_code = 4, ato_forecast_control = 1, planning_exception_set = 'FG-STD', end_assembly_pegging_flag = 'Y',
       planned_inv_point_flag = 'N', create_supply_flag = 'Y', rounding_control_type = 1, shrinkage_rate = 0.01,
       acceptable_early_days = 2, mrp_calculate_atp_flag = 'Y', auto_reduce_mps = 3, repetitive_planning_flag = 'N',
       planning_time_fence_code = 1, demand_time_fence_code = 1, release_time_fence_code = 1,
       substitution_window_code = 4, substitution_window_days = 5, critical_component_flag = 2, preposition_point = 'N',
       preprocessing_lead_time = 0, full_lead_time = 3, postprocessing_lead_time = 1, fixed_lead_time = 0.5,
       variable_lead_time = 0.002, lead_time_lot_size = 1000, cum_manufacturing_lead_time = 3,
       build_in_wip_flag = 'Y', wip_supply_type = 1, wip_supply_subinventory = 'FG',
       overcompletion_tolerance_type = 1, overcompletion_tolerance_value = 3,
       inventory_carry_penalty = 10, operation_slack_penalty = 5
 WHERE inventory_item_id BETWEEN 1001 AND 1010;

-- 누적 총 리드타임 = 제조 LT + 가장 긴 구매부품 LT (BOM 기준으로 미리 계산한 값)
UPDATE mtl_system_items_b SET cumulative_total_lead_time = 13 WHERE inventory_item_id = 1001;
UPDATE mtl_system_items_b SET cumulative_total_lead_time = 22 WHERE inventory_item_id = 1002;
UPDATE mtl_system_items_b SET cumulative_total_lead_time = 8  WHERE inventory_item_id IN (1003, 1004, 1010);
UPDATE mtl_system_items_b SET cumulative_total_lead_time = 19 WHERE inventory_item_id IN (1005, 1007);
UPDATE mtl_system_items_b SET cumulative_total_lead_time = 10 WHERE inventory_item_id IN (1006, 1008, 1009);

-- 개별 특성: 스페이서는 고정 주문량, 브래킷은 반복 계획, 커버는 다른 보관장소에서 보충
UPDATE mtl_system_items_b SET fixed_order_quantity = 500, fixed_lot_multiplier = NULL WHERE inventory_item_id = 1010;
UPDATE mtl_system_items_b SET repetitive_planning_flag = 'Y', overrun_percentage = 10,
       acceptable_rate_increase = 20, acceptable_rate_decrease = 20 WHERE inventory_item_id = 1006;
UPDATE mtl_system_items_b SET source_type = 3, source_organization_id = 204, source_subinventory = 'FG'
 WHERE inventory_item_id = 1009;
UPDATE mtl_system_items_b SET inventory_planning_code = 2, min_minmax_quantity = 200, max_minmax_quantity = 1000
 WHERE inventory_item_id = 1009;

-- ---------- 구매부품 공통 ----------
UPDATE mtl_system_items_b
   SET inventory_planning_code = 6, mrp_safety_stock_code = 1, maximum_order_quantity = 20000, order_cost = 50000,
       carrying_cost = 12, source_type = 2, source_organization_id = NULL, source_subinventory = NULL, fixed_days_supply = 10,
       mrp_planning_code = 3, ato_forecast_control = 3, planning_exception_set = 'RM-STD', end_assembly_pegging_flag = 'N',
       planned_inv_point_flag = 'N', create_supply_flag = 'Y', rounding_control_type = 1, shrinkage_rate = 0,
       acceptable_early_days = 3, mrp_calculate_atp_flag = 'N', auto_reduce_mps = 1, repetitive_planning_flag = 'N',
       planning_time_fence_code = 4, planning_time_fence_days = 10, demand_time_fence_code = 4, demand_time_fence_days = 5,
       release_time_fence_code = 4, release_time_fence_days = 3, substitution_window_code = 4, substitution_window_days = 7,
       critical_component_flag = 2, preposition_point = 'N',
       fixed_lead_time = 0, variable_lead_time = 0, cum_manufacturing_lead_time = 0,
       cumulative_total_lead_time = NVL(preprocessing_lead_time, 0) + NVL(full_lead_time, 0) + NVL(postprocessing_lead_time, 0),
       lead_time_lot_size = 1,
       build_in_wip_flag = 'N', wip_supply_type = 2, wip_supply_subinventory = 'RM',
       overcompletion_tolerance_type = 1, overcompletion_tolerance_value = 0,
       inventory_carry_penalty = 0, operation_slack_penalty = 0
 WHERE inventory_item_id BETWEEN 2001 AND 2010;

-- 소모성 부품: 최소-최대 계획 + 벌크 공급
UPDATE mtl_system_items_b SET inventory_planning_code = 2, mrp_planning_code = 6, wip_supply_type = 4,
       min_minmax_quantity = 5000, max_minmax_quantity = 20000, fixed_order_quantity = 5000, planning_exception_set = 'BULK'
 WHERE inventory_item_id IN (2005, 2009);
-- 고무 부싱: 재주문점 계획
UPDATE mtl_system_items_b SET inventory_planning_code = 1, min_minmax_quantity = 1000, max_minmax_quantity = 4000
 WHERE inventory_item_id = 2007;
-- 장납기·핵심 부품: MRP 안전재고 + 핵심 구성품 + 소프트 페깅
UPDATE mtl_system_items_b SET critical_component_flag = 1, mrp_safety_stock_code = 2, safety_stock_bucket_days = 20,
       mrp_safety_stock_percent = 10, end_assembly_pegging_flag = 'A' WHERE inventory_item_id IN (2003, 2010);
-- 강재(KG): 절단 손실, 사전 배치 지점
UPDATE mtl_system_items_b SET shrinkage_rate = 0.02, preposition_point = 'Y' WHERE inventory_item_id IN (2004, 2006);
