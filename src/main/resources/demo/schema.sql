-- demo 전용: 프로그램이 사용하는 EBS 테이블의 필요한 컬럼만 흉내 냄 (실제 EBS 에는 실행하지 마세요)
CREATE TABLE mtl_parameters (organization_id NUMBER PRIMARY KEY, organization_code VARCHAR2(3), master_organization_id NUMBER,
  calendar_code VARCHAR2(10), calendar_exception_set_id NUMBER);
CREATE TABLE hr_all_organization_units (organization_id NUMBER PRIMARY KEY, name VARCHAR2(240));
CREATE TABLE wip_parameters (organization_id NUMBER PRIMARY KEY, default_discrete_class VARCHAR2(10));
CREATE TABLE bom_calendar_dates (calendar_code VARCHAR2(10), exception_set_id NUMBER, calendar_date DATE, seq_num NUMBER);
CREATE TABLE bom_shift_times (calendar_code VARCHAR2(10), shift_num NUMBER, from_time NUMBER, to_time NUMBER);
CREATE TABLE bom_departments (department_id NUMBER PRIMARY KEY, department_code VARCHAR2(10), description VARCHAR2(240),
  organization_id NUMBER, disable_date DATE);
CREATE TABLE bom_resources (resource_id NUMBER PRIMARY KEY, resource_code VARCHAR2(10), description VARCHAR2(240),
  unit_of_measure VARCHAR2(3), organization_id NUMBER, disable_date DATE);
CREATE TABLE bom_department_resources (department_id NUMBER, resource_id NUMBER, capacity_units NUMBER, available_24_hours_flag NUMBER);
CREATE TABLE bom_resource_shifts (department_id NUMBER, resource_id NUMBER, shift_num NUMBER, capacity_units NUMBER);
CREATE TABLE mtl_system_items_b (inventory_item_id NUMBER, organization_id NUMBER, segment1 VARCHAR2(40), description VARCHAR2(240),
  primary_uom_code VARCHAR2(3), build_in_wip_flag VARCHAR2(1), primary_unit_of_measure VARCHAR2(25) DEFAULT 'Each',
  planning_make_buy_code NUMBER DEFAULT 1, preprocessing_lead_time NUMBER, full_lead_time NUMBER, postprocessing_lead_time NUMBER,
  minimum_order_quantity NUMBER, fixed_lot_multiplier NUMBER,
  item_type VARCHAR2(30), inventory_item_status_code VARCHAR2(10) DEFAULT 'Active',
  inventory_planning_code NUMBER DEFAULT 6, planner_code VARCHAR2(10), min_minmax_quantity NUMBER, max_minmax_quantity NUMBER,
  mrp_safety_stock_code NUMBER DEFAULT 1, safety_stock_bucket_days NUMBER, mrp_safety_stock_percent NUMBER,
  maximum_order_quantity NUMBER, order_cost NUMBER, carrying_cost NUMBER, source_type NUMBER, source_organization_id NUMBER,
  source_subinventory VARCHAR2(10), fixed_order_quantity NUMBER, fixed_days_supply NUMBER,
  mrp_planning_code NUMBER DEFAULT 3, ato_forecast_control NUMBER DEFAULT 3, planning_exception_set VARCHAR2(10),
  end_assembly_pegging_flag VARCHAR2(1) DEFAULT 'N', planned_inv_point_flag VARCHAR2(1) DEFAULT 'N', create_supply_flag VARCHAR2(1) DEFAULT 'Y',
  rounding_control_type NUMBER DEFAULT 1, shrinkage_rate NUMBER, acceptable_early_days NUMBER, mrp_calculate_atp_flag VARCHAR2(1) DEFAULT 'N',
  auto_reduce_mps NUMBER DEFAULT 1, repetitive_planning_flag VARCHAR2(1) DEFAULT 'N', overrun_percentage NUMBER,
  acceptable_rate_increase NUMBER, acceptable_rate_decrease NUMBER,
  planning_time_fence_code NUMBER, planning_time_fence_days NUMBER, demand_time_fence_code NUMBER, demand_time_fence_days NUMBER,
  release_time_fence_code NUMBER, release_time_fence_days NUMBER, substitution_window_code NUMBER, substitution_window_days NUMBER,
  critical_component_flag NUMBER, preposition_point VARCHAR2(1),
  fixed_lead_time NUMBER, variable_lead_time NUMBER, cum_manufacturing_lead_time NUMBER, cumulative_total_lead_time NUMBER,
  lead_time_lot_size NUMBER, wip_supply_type NUMBER DEFAULT 1, wip_supply_subinventory VARCHAR2(10),
  overcompletion_tolerance_type NUMBER, overcompletion_tolerance_value NUMBER, inventory_carry_penalty NUMBER, operation_slack_penalty NUMBER);
CREATE TABLE mtl_planners (planner_code VARCHAR2(10), organization_id NUMBER, description VARCHAR2(50));
CREATE TABLE fnd_common_lookups (lookup_type VARCHAR2(30), lookup_code VARCHAR2(30), meaning VARCHAR2(80));
CREATE TABLE bom_operational_routings (routing_sequence_id NUMBER PRIMARY KEY, common_routing_sequence_id NUMBER,
  assembly_item_id NUMBER, organization_id NUMBER, alternate_routing_designator VARCHAR2(10));
CREATE TABLE bom_operation_sequences (operation_sequence_id NUMBER PRIMARY KEY, routing_sequence_id NUMBER, operation_seq_num NUMBER,
  department_id NUMBER, effectivity_date DATE, disable_date DATE);
CREATE TABLE bom_operation_resources (operation_sequence_id NUMBER, resource_seq_num NUMBER, resource_id NUMBER,
  usage_rate_or_amount NUMBER, basis_type NUMBER);
CREATE TABLE hz_parties (party_id NUMBER PRIMARY KEY, party_name VARCHAR2(360));
CREATE TABLE hz_cust_accounts (cust_account_id NUMBER PRIMARY KEY, party_id NUMBER);
CREATE TABLE oe_order_headers_all (header_id NUMBER PRIMARY KEY, order_number NUMBER, sold_to_org_id NUMBER);
CREATE TABLE oe_order_lines_all (line_id NUMBER PRIMARY KEY, header_id NUMBER, line_number NUMBER, shipment_number NUMBER,
  inventory_item_id NUMBER, ship_from_org_id NUMBER, ordered_quantity NUMBER, shipped_quantity NUMBER, order_quantity_uom VARCHAR2(3),
  schedule_ship_date DATE, request_date DATE, open_flag VARCHAR2(1), booked_flag VARCHAR2(1), line_category_code VARCHAR2(30));
CREATE TABLE wip_entities (wip_entity_id NUMBER PRIMARY KEY, wip_entity_name VARCHAR2(240), organization_id NUMBER,
  entity_type NUMBER, primary_item_id NUMBER);
CREATE TABLE wip_discrete_jobs (wip_entity_id NUMBER PRIMARY KEY, organization_id NUMBER, primary_item_id NUMBER, status_type NUMBER,
  start_quantity NUMBER, quantity_completed NUMBER, quantity_scrapped NUMBER, scheduled_start_date DATE, scheduled_completion_date DATE,
  source_code VARCHAR2(30), source_line_id NUMBER, class_code VARCHAR2(10));
CREATE TABLE wip_job_schedule_interface (last_update_date DATE, last_updated_by NUMBER, creation_date DATE, created_by NUMBER,
  group_id NUMBER, interface_id NUMBER PRIMARY KEY, source_code VARCHAR2(30), source_line_id NUMBER,
  process_phase NUMBER, process_status NUMBER, organization_id NUMBER, load_type NUMBER, status_type NUMBER,
  primary_item_id NUMBER, start_quantity NUMBER, net_quantity NUMBER, first_unit_start_date DATE, last_unit_completion_date DATE,
  scheduling_method NUMBER, class_code VARCHAR2(10), description VARCHAR2(240), job_name VARCHAR2(240), wip_entity_id NUMBER);
CREATE TABLE wip_interface_errors (interface_id NUMBER, error_type NUMBER, error VARCHAR2(500));
CREATE TABLE mtl_secondary_inventories (secondary_inventory_name VARCHAR2(10), organization_id NUMBER, availability_type NUMBER,
  description VARCHAR2(50));
CREATE TABLE mtl_onhand_quantities_detail (organization_id NUMBER, inventory_item_id NUMBER, subinventory_code VARCHAR2(10),
  primary_transaction_quantity NUMBER, locator_id NUMBER, lot_number VARCHAR2(80), revision VARCHAR2(3));
CREATE TABLE mtl_item_locations_kfv (inventory_location_id NUMBER, organization_id NUMBER, concatenated_segments VARCHAR2(200));
CREATE TABLE bom_structures_b (bill_sequence_id NUMBER PRIMARY KEY, common_bill_sequence_id NUMBER, assembly_item_id NUMBER,
  organization_id NUMBER, alternate_bom_designator VARCHAR2(10));
CREATE TABLE bom_components_b (component_sequence_id NUMBER PRIMARY KEY, bill_sequence_id NUMBER, component_item_id NUMBER,
  component_quantity NUMBER, component_yield_factor NUMBER, effectivity_date DATE, disable_date DATE, implementation_date DATE,
  wip_supply_type NUMBER);
CREATE TABLE ap_suppliers (vendor_id NUMBER PRIMARY KEY, vendor_name VARCHAR2(240));
CREATE TABLE po_headers_all (po_header_id NUMBER PRIMARY KEY, segment1 VARCHAR2(20), vendor_id NUMBER, authorization_status VARCHAR2(25));
CREATE TABLE po_lines_all (po_line_id NUMBER PRIMARY KEY, po_header_id NUMBER, line_num NUMBER, item_id NUMBER,
  unit_meas_lookup_code VARCHAR2(25));
CREATE TABLE po_line_locations_all (line_location_id NUMBER PRIMARY KEY, po_header_id NUMBER, po_line_id NUMBER, shipment_num NUMBER,
  ship_to_organization_id NUMBER, shipment_type VARCHAR2(25), quantity NUMBER, quantity_received NUMBER, quantity_cancelled NUMBER,
  need_by_date DATE, promised_date DATE, cancel_flag VARCHAR2(1), closed_code VARCHAR2(25));
CREATE TABLE wip_requirement_operations (wip_entity_id NUMBER, organization_id NUMBER, inventory_item_id NUMBER, date_required DATE,
  required_quantity NUMBER, quantity_issued NUMBER, wip_supply_type NUMBER);
CREATE SEQUENCE wip_job_schedule_interface_s START WITH 5001;
CREATE SEQUENCE wip_interface_s START WITH 90001;
CREATE SEQUENCE wip_entities_s START WITH 30001;
